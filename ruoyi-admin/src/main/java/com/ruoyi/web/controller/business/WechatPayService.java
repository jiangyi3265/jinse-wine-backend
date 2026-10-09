package com.ruoyi.web.controller.business;

import java.io.IOException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 微信支付 v3 下单、退款、回调验签和回调解密；配置不完整时拒绝生成伪成功状态。 */
@Service
public class WechatPayService
{
    private static final Logger log = LoggerFactory.getLogger(WechatPayService.class);
    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final Pattern TRANSFER_BILL_NO = Pattern.compile("^[A-Za-z0-9]{1,32}$");
    private static final String LATE_PAYMENT_REASON = "系统自动退款：订单取消后收到支付成功通知";
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final OrderRefundService refunds;
    private final RestTemplate http = new RestTemplate();

    @Value("${mini.wechat.pay-enabled:false}") private boolean enabled;
    @Value("${mini.wechat.app-id:}") private String appId;
    @Value("${mini.wechat.merchant-id:}") private String merchantId;
    @Value("${mini.wechat.merchant-serial:}") private String merchantSerial;
    @Value("${mini.wechat.private-key:}") private String privateKeyPem;
    @Value("${mini.wechat.api-v3-key:}") private String apiV3Key;
    @Value("${mini.wechat.pay-notify-url:}") private String notifyUrl;
    @Value("${mini.wechat.refund-notify-url:}") private String refundNotifyUrl;
    @Value("${mini.wechat.transfer-enabled:false}") private boolean transferEnabled;
    @Value("${mini.wechat.transfer-notify-url:}") private String transferNotifyUrl;
    @Value("${mini.wechat.transfer-scene-id:}") private String transferSceneId;
    @Value("${mini.wechat.transfer-scene-report-name:活动名称}") private String transferSceneReportName;
    @Value("${mini.wechat.transfer-scene-report-value:金色酒业抽奖}") private String transferSceneReportValue;
    @Value("${mini.wechat.platform-cert:}") private String platformCertPem;
    @Value("${mini.wechat.platform-serial:}") private String platformSerial;

    public WechatPayService(JdbcTemplate jdbc, ObjectMapper json, OrderRefundService refunds) { this.jdbc = jdbc; this.json = json; this.refunds = refunds; }

    @Transactional
    public Map<String, Object> createJsapiPay(long memberId, String orderNo, String requestId)
    {
        if (requestId == null || !REQUEST_ID.matcher(requestId).matches()) throw new MiniBusinessException(400, "支付请求号无效");
        Map<String, Object> order = first("SELECT o.order_no, o.total_cent, o.status, m.wechat_openid FROM biz_order o JOIN biz_member m ON m.member_id=o.member_id WHERE o.order_no=? AND o.member_id=? FOR UPDATE", orderNo, memberId);
        if (order == null) throw new MiniBusinessException(404, "订单不存在");
        Map<String, Object> previous = first("SELECT order_no,response_json FROM biz_order_pay_request WHERE member_id=? AND request_id=? FOR UPDATE", memberId, requestId);
        if (previous != null) {
            if (!orderNo.equals(String.valueOf(previous.get("order_no")))) throw new MiniBusinessException(409, "支付请求号已用于其他订单");
            try { return json.readValue(String.valueOf(previous.get("response_json")), Map.class); }
            catch (Exception ignored) { throw new MiniBusinessException(409, "支付请求记录无效，请重新发起支付"); }
        }
        // Only a new request needs live merchant configuration. A saved response must remain replayable
        // after a short configuration outage so the client can safely retry an unknown network result.
        requireConfigured();
        if (!"pending".equals(String.valueOf(order.get("status")))) throw new MiniBusinessException(409, "当前订单不可支付");
        if (order.get("wechat_openid") == null || String.valueOf(order.get("wechat_openid")).trim().isEmpty()) throw new MiniBusinessException(409, "当前会员缺少微信身份");
        try {
            Map<String, Object> body = new LinkedHashMap<>(); body.put("appid", appId); body.put("mchid", merchantId); body.put("description", "金色酒业商城订单"); body.put("out_trade_no", orderNo); body.put("notify_url", notifyUrl);
            Map<String, Object> amount = new LinkedHashMap<>(); amount.put("total", ((Number) order.get("total_cent")).intValue()); amount.put("currency", "CNY"); body.put("amount", amount);
            Map<String, Object> payer = new LinkedHashMap<>(); payer.put("openid", order.get("wechat_openid")); body.put("payer", payer);
            String response = request("POST", "/v3/pay/transactions/jsapi", json.writeValueAsString(body));
            String prepayId = json.readTree(response).path("prepay_id").asText("");
            if (prepayId.isEmpty()) throw new MiniBusinessException(502, "微信支付未返回预支付单");
            String timestamp = String.valueOf(Instant.now().getEpochSecond()); String nonce = nonce();
            String paySign = sign(appId + "\n" + timestamp + "\n" + nonce + "\n" + "prepay_id=" + prepayId + "\n");
            Map<String, Object> result = new LinkedHashMap<>(); result.put("timeStamp", timestamp); result.put("nonceStr", nonce); result.put("package", "prepay_id=" + prepayId); result.put("signType", "RSA"); result.put("paySign", paySign);
            jdbc.update("INSERT INTO biz_order_pay_request(member_id,order_no,request_id,response_json) VALUES(?,?,?,?)", memberId, orderNo, requestId, json.writeValueAsString(result));
            return result;
        } catch (MiniBusinessException e) { throw e; }
        catch (WechatApiException e)
        {
            log.warn("微信支付下单被平台拒绝，orderNo={} status={} body={}", orderNo, e.statusCode, truncate(e.responseBody, 500));
            throw new MiniBusinessException(502, paymentApiError(e));
        }
        catch (Exception e)
        {
            log.warn("微信支付下单请求异常，orderNo={}", orderNo, e);
            throw new MiniBusinessException(502, "微信支付服务暂时不可用，请稍后重试");
        }
    }

    @Transactional
    public void handleNotify(HttpServletRequest request, String body)
    {
        requireConfigured();
        try {
            verifyNotify(request, body);
            JsonNode resource = decryptResource(json.readTree(body).path("resource"));
            if (!"SUCCESS".equals(resource.path("trade_state").asText())) return;
            applyPaymentSuccess(resource);
        } catch (MiniBusinessException e) { throw e; }
        catch (Exception e) { throw new MiniBusinessException(400, "支付回调校验失败"); }
    }

    @Transactional
    public String syncPaymentStatus(long memberId, String orderNo)
    {
        if (first("SELECT order_id FROM biz_order WHERE order_no=? AND member_id=?", orderNo, memberId) == null)
            throw new MiniBusinessException(404, "订单不存在");
        requireConfigured();
        try {
            JsonNode trade = json.readTree(request("GET", "/v3/pay/transactions/out-trade-no/" + orderNo + "?mchid=" + merchantId, ""));
            if (!orderNo.equals(trade.path("out_trade_no").asText())
                    || !appId.equals(trade.path("appid").asText())
                    || !merchantId.equals(trade.path("mchid").asText()))
                throw new MiniBusinessException(409, "微信支付查询结果与订单不匹配");
            String state = trade.path("trade_state").asText("");
            if (!Arrays.asList("SUCCESS", "NOTPAY", "USERPAYING", "CLOSED", "REVOKED", "PAYERROR", "REFUND").contains(state))
                throw new MiniBusinessException(502, "微信支付查询未返回可识别状态");
            if ("SUCCESS".equals(state)) applyPaymentSuccess(trade);
            return state;
        } catch (WechatApiException e) {
            if (e.statusCode == 404) return "NOT_FOUND";
            throw new MiniBusinessException(502, "微信支付状态暂时无法查询，请稍后重试");
        } catch (MiniBusinessException e) { throw e; }
        catch (Exception e) {
            log.warn("微信支付商户查单处理失败，orderNo={}", orderNo, e);
            throw new MiniBusinessException(502, "微信支付状态暂时无法查询，请稍后重试");
        }
    }

    private void applyPaymentSuccess(JsonNode resource)
    {
            if (!appId.equals(resource.path("appid").asText()) || !merchantId.equals(resource.path("mchid").asText()))
                throw new MiniBusinessException(409, "支付回调商户信息不匹配");
            String orderNo = resource.path("out_trade_no").asText(""); String transactionId = resource.path("transaction_id").asText(""); int paid = resource.path("amount").path("total").asInt(-1);
            if (orderNo.isEmpty() || transactionId.isEmpty() || paid < 0 || !"CNY".equals(resource.path("amount").path("currency").asText())) throw new MiniBusinessException(400, "支付回调内容不完整或币种无效");
            Map<String, Object> order = first("SELECT order_id,member_id,total_cent,status,wechat_transaction_id FROM biz_order WHERE order_no=? FOR UPDATE", orderNo);
            if (order == null) throw new MiniBusinessException(404, "订单不存在");
            if (((Number) order.get("total_cent")).intValue() != paid) throw new MiniBusinessException(409, "支付金额与订单不一致");
            String status = String.valueOf(order.get("status"));
            String storedTransaction = String.valueOf(order.get("wechat_transaction_id") == null ? "" : order.get("wechat_transaction_id"));
            if ("pending".equals(status))
            {
                jdbc.update("UPDATE biz_order SET status='paid', paid_at=NOW(), wechat_transaction_id=? WHERE order_id=? AND status='pending'", transactionId, order.get("order_id"));
            }
            else if ("cancelled".equals(status))
            {
                if (!storedTransaction.isEmpty() && !storedTransaction.equals(transactionId))
                    throw new MiniBusinessException(409, "已取消订单收到不同的支付流水，请人工核对商户账单");
                long orderId = ((Number) order.get("order_id")).longValue();
                Map<String, Object> refund = first("SELECT refund_id FROM biz_order_refund WHERE order_id=? AND reason=? ORDER BY refund_id LIMIT 1", orderId, LATE_PAYMENT_REASON);
                if (refund == null)
                {
                    jdbc.update("UPDATE biz_order SET paid_at=COALESCE(paid_at,NOW()),wechat_transaction_id=? WHERE order_id=? AND status='cancelled'", transactionId, orderId);
                    String refundNo = "RF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
                    jdbc.update("INSERT INTO biz_order_refund(order_id,member_id,refund_no,amount_cent,reason,status,channel_status) VALUES(?,?,?,?,?,'approved','PENDING_AUTO')",
                            orderId, order.get("member_id"), refundNo, paid, LATE_PAYMENT_REASON);
                    log.warn("微信支付成功通知晚于订单取消，已创建自动原路退款任务，orderNo={} transactionId={}", orderNo, transactionId);
                }
            }
            else if (storedTransaction.isEmpty() && Arrays.asList("paid", "shipping", "receiving", "complete", "refunded").contains(status))
            {
                jdbc.update("UPDATE biz_order SET wechat_transaction_id=? WHERE order_id=? AND status=? AND wechat_transaction_id IS NULL", transactionId, order.get("order_id"), status);
            }
            else if (!storedTransaction.isEmpty() && !storedTransaction.equals(transactionId))
            {
                throw new MiniBusinessException(409, "订单收到不同的支付流水，请人工核对商户账单");
            }
    }

    @Scheduled(fixedDelayString = "${mini.wechat.late-payment-refund-interval-ms:30000}", initialDelayString = "${mini.wechat.late-payment-refund-initial-delay-ms:15000}")
    public void refundLatePayments()
    {
        if (!enabled) return;
        Timestamp due = Timestamp.from(Instant.now().minusSeconds(120));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT refund_id,channel_status FROM biz_order_refund WHERE reason=? AND status='approved' "
                        + "AND (channel_status='PENDING_AUTO' OR (channel_status IN ('SUBMITTING','UNKNOWN','PROCESSING','SUCCESS') "
                        + "AND update_time<=?)) ORDER BY update_time,refund_id LIMIT 20", LATE_PAYMENT_REASON, due);
        for (Map<String, Object> row : rows)
        {
            long refundId = ((Number) row.get("refund_id")).longValue();
            String channelStatus = String.valueOf(row.get("channel_status"));
            try
            {
                if ("SUCCESS".equals(channelStatus)) refunds.markRefunded(refundId, "wechat");
                else if ("PENDING_AUTO".equals(channelStatus)) initiateRefund(refundId);
                else queryRefundStatus(refundId);
            }
            catch (MiniBusinessException e)
            {
                if ("SUCCESS".equals(channelStatus))
                {
                    jdbc.update("UPDATE biz_order_refund SET update_time=NOW() WHERE refund_id=? AND status='approved' AND channel_status='SUCCESS'", refundId);
                    log.error("微信已确认退款但本地退款落账失败，refundId={}", refundId, e);
                    continue;
                }
                if (e.getCode() == 404 && ("SUBMITTING".equals(channelStatus) || "UNKNOWN".equals(channelStatus)))
                {
                    // Reuse out_refund_no: WeChat treats a repeated refund request with that number as idempotent.
                    jdbc.update("UPDATE biz_order_refund SET channel_status='PENDING_AUTO' WHERE refund_id=? AND status='approved' AND channel_status=?",
                            refundId, channelStatus);
                }
                else if (e.getCode() == 404 || e.getCode() == 409)
                {
                    jdbc.update("UPDATE biz_order_refund SET channel_status='REVIEW' WHERE refund_id=? AND status='approved' AND channel_status=?",
                            refundId, channelStatus);
                }
                else if (!"PENDING_AUTO".equals(channelStatus))
                {
                    jdbc.update("UPDATE biz_order_refund SET update_time=NOW() WHERE refund_id=? AND status='approved' AND channel_status=?",
                            refundId, channelStatus);
                }
                log.warn("迟到支付自动退款暂未完成，refundId={}，原因={}", refundId, e.getMessage());
            }
            catch (RuntimeException e)
            {
                log.warn("迟到支付自动退款处理失败，refundId={}", refundId, e);
            }
        }
    }

    public Map<String, Object> initiateRefund(long refundId)
    {
        requireRefundConfigured();
        Map<String, Object> row = first("SELECT r.refund_id,r.refund_no,r.amount_cent,r.reason,r.status,r.channel_status,r.channel_refund_id,o.order_no,o.total_cent,o.wechat_transaction_id FROM biz_order_refund r JOIN biz_order o ON o.order_id=r.order_id WHERE r.refund_id=?", refundId);
        if (row == null) throw new MiniBusinessException(404, "退款申请不存在");
        if (!"approved".equals(String.valueOf(row.get("status")))) throw new MiniBusinessException(409, "仅审核通过的退款申请可发起微信退款");
        String channelStatus = String.valueOf(row.get("channel_status") == null ? "" : row.get("channel_status"));
        if ("SUCCESS".equals(channelStatus)) {
            refunds.markRefunded(refundId, "wechat");
            return refundResult(refundId, "SUCCESS", row.get("channel_refund_id") == null ? "" : String.valueOf(row.get("channel_refund_id")));
        }
        boolean autoPending = "PENDING_AUTO".equals(channelStatus);
        if (!blank(channelStatus) && !autoPending) throw new MiniBusinessException(409, "该退款已有微信渠道状态，请先核对微信退款单后再处理");
        if (row.get("wechat_transaction_id") == null || blank(String.valueOf(row.get("wechat_transaction_id")))) throw new MiniBusinessException(409, "订单缺少微信支付流水号，不能发起微信退款");
        String claimSql = autoPending
                ? "UPDATE biz_order_refund SET channel_status='SUBMITTING' WHERE refund_id=? AND status='approved' AND channel_status='PENDING_AUTO'"
                : "UPDATE biz_order_refund SET channel_status='SUBMITTING' WHERE refund_id=? AND status='approved' AND channel_status IS NULL";
        if (jdbc.update(claimSql, refundId) != 1)
            throw new MiniBusinessException(409, "退款渠道状态已变化，请刷新后再处理");
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("transaction_id", row.get("wechat_transaction_id")); body.put("out_refund_no", row.get("refund_no"));
            String reason = truncate(String.valueOf(row.get("reason")), 80);
            if (!blank(reason)) body.put("reason", reason);
            body.put("notify_url", refundNotifyUrl);
            Map<String, Object> amount = new LinkedHashMap<>(); amount.put("refund", ((Number) row.get("amount_cent")).intValue()); amount.put("total", ((Number) row.get("total_cent")).intValue()); amount.put("currency", "CNY"); body.put("amount", amount);
            JsonNode response = json.readTree(request("POST", "/v3/refund/domestic/refunds", json.writeValueAsString(body)));
            verifyRefundResponse(response, row);
            String status = response.path("status").asText("");
            if (!("SUCCESS".equals(status) || "PROCESSING".equals(status) || "CLOSED".equals(status) || "ABNORMAL".equals(status))) throw new MiniBusinessException(502, "微信退款未返回可识别状态");
            jdbc.update("UPDATE biz_order_refund SET channel_status=?,channel_refund_id=? WHERE refund_id=? AND status='approved' AND channel_status='SUBMITTING'", status, response.path("refund_id").asText(""), refundId);
            if ("SUCCESS".equals(status)) refunds.markRefunded(refundId, "wechat");
            return refundResult(refundId, status, response.path("refund_id").asText(""));
        } catch (MiniBusinessException e) {
            markRefundSubmissionResult(refundId, e.getCode() == 400 || e.getCode() == 409 ? "REVIEW" : "UNKNOWN");
            throw e;
        }
        catch (WechatApiException e) {
            boolean rejected = e.statusCode >= 400 && e.statusCode < 500 && e.statusCode != 429;
            markRefundSubmissionResult(refundId, rejected ? "REVIEW" : "UNKNOWN");
            throw new MiniBusinessException(502, rejected ? "微信拒绝退款申请，请核对商户配置与退款单" : "微信退款响应未知，系统将按退款单号继续查单");
        }
        catch (Exception e) {
            markRefundSubmissionResult(refundId, "UNKNOWN");
            throw new MiniBusinessException(502, "微信退款响应未知，系统将按退款单号继续查单");
        }
    }

    private void markRefundSubmissionResult(long refundId, String status)
    {
        jdbc.update("UPDATE biz_order_refund SET channel_status=? WHERE refund_id=? AND status='approved' AND channel_status='SUBMITTING'", status, refundId);
    }

    public Map<String, Object> queryRefundStatus(long refundId)
    {
        requireConfigured();
        Map<String, Object> row = first("SELECT r.refund_id,r.refund_no,r.amount_cent,r.status,r.channel_refund_id,o.order_no,o.total_cent,o.wechat_transaction_id FROM biz_order_refund r JOIN biz_order o ON o.order_id=r.order_id WHERE r.refund_id=?", refundId);
        if (row == null) throw new MiniBusinessException(404, "退款申请不存在");
        if (!"approved".equals(String.valueOf(row.get("status")))) throw new MiniBusinessException(409, "当前退款业务状态不需要查询微信渠道");
        try {
            JsonNode response = json.readTree(request("GET", "/v3/refund/domestic/refunds/" + row.get("refund_no"), ""));
            verifyRefundResponse(response, row);
            String status = response.path("status").asText("");
            if (!("SUCCESS".equals(status) || "PROCESSING".equals(status) || "CLOSED".equals(status) || "ABNORMAL".equals(status))) throw new MiniBusinessException(502, "微信退款查询未返回可识别状态");
            jdbc.update("UPDATE biz_order_refund SET channel_status=?,channel_refund_id=? WHERE refund_id=? AND status='approved'", status, response.path("refund_id").asText(""), refundId);
            if ("SUCCESS".equals(status)) refunds.markRefunded(refundId, "wechat");
            return refundResult(refundId, status, response.path("refund_id").asText(""));
        } catch (MiniBusinessException e) { throw e; }
        catch (WechatApiException e) {
            if (e.statusCode == 404) throw new MiniBusinessException(404, "微信退款单不存在");
            throw new MiniBusinessException(502, "暂时无法查询微信退款状态，请稍后重试");
        }
        catch (Exception e) { throw new MiniBusinessException(502, "暂时无法查询微信退款状态，请稍后重试"); }
    }

    static void verifyRefundResponse(JsonNode response, Map<String, Object> row)
    {
        String refundBillId = response.path("refund_id").asText("");
        String storedBillId = row.get("channel_refund_id") == null ? "" : String.valueOf(row.get("channel_refund_id"));
        if (refundBillId.isEmpty() || (!storedBillId.isEmpty() && !storedBillId.equals(refundBillId))
                || !String.valueOf(row.get("refund_no")).equals(response.path("out_refund_no").asText())
                || !String.valueOf(row.get("order_no")).equals(response.path("out_trade_no").asText())
                || !String.valueOf(row.get("wechat_transaction_id")).equals(response.path("transaction_id").asText())
                || ((Number) row.get("amount_cent")).intValue() != response.path("amount").path("refund").asInt(-1)
                || ((Number) row.get("total_cent")).intValue() != response.path("amount").path("total").asInt(-1)
                || !"CNY".equals(response.path("amount").path("currency").asText()))
            throw new MiniBusinessException(409, "微信退款结果与本地申请不匹配");
    }

    private Map<String, Object> refundResult(long refundId, String status, String wechatRefundId)
    {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("refundId", refundId); result.put("status", status); result.put("wechatRefundId", wechatRefundId); return result;
    }

    @Transactional
    public void handleRefundNotify(HttpServletRequest request, String body)
    {
        requireRefundConfigured();
        try {
            verifyNotify(request, body);
            JsonNode envelope = json.readTree(body);
            JsonNode resource = decryptResource(envelope.path("resource"));
            String eventType = envelope.path("event_type").asText("");
            String status = resource.path("refund_status").asText("");
            if (!("REFUND.SUCCESS".equals(eventType) || "REFUND.ABNORMAL".equals(eventType) || "REFUND.CLOSED".equals(eventType))) return;
            if (("REFUND.SUCCESS".equals(eventType) && !"SUCCESS".equals(status))
                    || ("REFUND.ABNORMAL".equals(eventType) && !"ABNORMAL".equals(status))
                    || ("REFUND.CLOSED".equals(eventType) && !"CLOSED".equals(status))) throw new MiniBusinessException(409, "微信退款回调事件与退款状态不匹配");
            String refundNo = resource.path("out_refund_no").asText("");
            Map<String, Object> row = first("SELECT r.refund_id,r.amount_cent,r.status,r.channel_status,r.channel_refund_id,o.total_cent,o.order_no,o.wechat_transaction_id FROM biz_order_refund r JOIN biz_order o ON o.order_id=r.order_id WHERE r.refund_no=? FOR UPDATE", refundNo);
            if (row == null) throw new MiniBusinessException(404, "退款申请不存在");
            verifyRefundNotification(resource, row, merchantId, appId);
            if (shouldKeepConfirmedRefund(row, status))
            {
                log.warn("忽略已确认成功退款的迟到状态通知，refundNo={} incoming={}", refundNo, status);
                return;
            }
            String refundId = resource.path("refund_id").asText("");
            jdbc.update("UPDATE biz_order_refund SET channel_status=?,channel_refund_id=? WHERE refund_id=?", status, refundId, row.get("refund_id"));
            if ("SUCCESS".equals(status) && "approved".equals(String.valueOf(row.get("status")))) refunds.markRefunded(((Number) row.get("refund_id")).longValue(), "wechat");
        } catch (MiniBusinessException e) { throw e; }
        catch (Exception e) { throw new MiniBusinessException(400, "微信退款回调校验失败"); }
    }

    static boolean shouldKeepConfirmedRefund(Map<String, Object> row, String incomingStatus)
    {
        return ("refunded".equals(String.valueOf(row.get("status")))
                || "SUCCESS".equals(String.valueOf(row.get("channel_status"))))
                && !"SUCCESS".equals(incomingStatus);
    }

    static void verifyRefundNotification(JsonNode resource, Map<String, Object> row, String merchantId, String appId)
    {
        // The signed refund notification can omit appid and mchid; reject either if supplied with a wrong value.
        if ((resource.has("mchid") && !merchantId.equals(resource.path("mchid").asText()))
                || (resource.has("appid") && !appId.equals(resource.path("appid").asText())))
            throw new MiniBusinessException(409, "退款回调商户信息不匹配");
        String refundBillId = resource.path("refund_id").asText("");
        String storedBillId = row.get("channel_refund_id") == null ? "" : String.valueOf(row.get("channel_refund_id"));
        if (refundBillId.isEmpty() || (!storedBillId.isEmpty() && !storedBillId.equals(refundBillId))
                || !String.valueOf(row.get("order_no")).equals(resource.path("out_trade_no").asText())
                || !String.valueOf(row.get("wechat_transaction_id")).equals(resource.path("transaction_id").asText()))
            throw new MiniBusinessException(409, "退款回调订单流水不匹配");
        if (((Number) row.get("amount_cent")).intValue() != resource.path("amount").path("refund").asInt(-1)
                || ((Number) row.get("total_cent")).intValue() != resource.path("amount").path("total").asInt(-1)
                || !"CNY".equals(resource.path("amount").path("currency").asText()))
            throw new MiniBusinessException(409, "退款回调金额或币种不匹配");
    }

    public Map<String, Object> createCashTransfer(String outBillNo, int amountCent, String openid)
    {
        requireTransferConfigured();
        if (amountCent <= 0 || blank(openid) || outBillNo == null || !TRANSFER_BILL_NO.matcher(outBillNo).matches())
            throw new MiniBusinessException(409, "红包金额、商户单号或微信会员身份无效");
        try
        {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appid", appId);
            body.put("out_bill_no", outBillNo);
            body.put("transfer_scene_id", transferSceneId);
            body.put("openid", openid);
            body.put("transfer_amount", amountCent);
            body.put("transfer_remark", "金色酒业活动红包");
            body.put("notify_url", transferNotifyUrl);
            body.put("user_recv_perception", "活动奖励");
            Map<String, String> report = new LinkedHashMap<>();
            report.put("info_type", transferSceneReportName);
            report.put("info_content", transferSceneReportValue);
            List<Map<String, String>> reports = new ArrayList<>();
            reports.add(report);
            body.put("transfer_scene_report_infos", reports);
            JsonNode response = json.readTree(request("POST", "/v3/fund-app/mch-transfer/transfer-bills", json.writeValueAsString(body)));
            if (!outBillNo.equals(response.path("out_bill_no").asText())
                    || response.path("transfer_bill_no").asText("").isEmpty()
                    || (response.hasNonNull("openid") && !openid.equals(response.path("openid").asText()))
                    || (response.hasNonNull("transfer_amount") && response.path("transfer_amount").asInt(-1) != amountCent))
                throw new MiniBusinessException(409, "微信转账响应与本地红包不匹配");
            Map<String, Object> result = transferResult(response);
            // The create API only echoes the bill numbers and state. Keep the submitted
            // amount/OpenID for local matching; the query API verifies its own fields.
            result.put("openid", openid);
            result.put("amountCent", amountCent);
            return result;
        }
        catch (MiniBusinessException e) { throw e; }
        catch (Exception e) { throw new MiniBusinessException(502, "微信转账响应未知，请查询原商户单号后再处理"); }
    }

    public Map<String, Object> queryCashTransfer(String outBillNo)
    {
        requireTransferConfigured();
        try
        {
            JsonNode response = json.readTree(request("GET", "/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/" + outBillNo + "?appid=" + appId, ""));
            if (!outBillNo.equals(response.path("out_bill_no").asText())
                    || !merchantId.equals(response.path("mch_id").asText())
                    || !appId.equals(response.path("appid").asText())
                    || response.path("transfer_bill_no").asText("").isEmpty()
                    || response.path("transfer_amount").asInt(-1) <= 0)
                throw new MiniBusinessException(409, "微信转账查询结果与本地单号或商户不匹配");
            return transferResult(response);
        }
        catch (MiniBusinessException e) { throw e; }
        catch (WechatApiException e)
        {
            if (e.statusCode == 404) return null;
            throw new MiniBusinessException(502, "暂时无法查询微信转账状态，请稍后重试");
        }
        catch (Exception e) { throw new MiniBusinessException(502, "暂时无法查询微信转账状态，请稍后重试"); }
    }

    public Map<String, Object> cashTransferClientInfo()
    {
        requireTransferConfigured();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appId", appId);
        result.put("mchId", merchantId);
        return result;
    }

    /**
     * Used by the background reward dispatcher to avoid creating a local
     * transfer record when the official merchant-transfer capability is not
     * configured. A false value means the reward remains pending and can be
     * retried after configuration is completed.
     */
    public boolean isTransferConfigured()
    {
        if (!transferEnabled || blank(appId) || blank(merchantId) || blank(merchantSerial)
                || blank(privateKeyPem) || blank(apiV3Key) || !platformVerificationConfigured()
                || blank(transferSceneId) || blank(transferNotifyUrl)
                || !transferNotifyUrl.startsWith("https://")) return false;
        return apiV3Key.getBytes(StandardCharsets.UTF_8).length == 32;
    }

    @Transactional
    public void handleTransferNotify(HttpServletRequest request, String body)
    {
        requireTransferConfigured();
        try
        {
            verifyNotify(request, body);
            JsonNode envelope = json.readTree(body);
            if (!"MCHTRANSFER.BILL.FINISHED".equals(envelope.path("event_type").asText())) return;
            JsonNode resource = decryptResource(envelope.path("resource"));
            String outBillNo = resource.path("out_bill_no").asText("");
            String state = resource.path("state").asText("");
            Map<String, Object> row = first("SELECT transfer_id,reward_id,transfer_bill_no,amount_cent,openid,status FROM biz_reward_transfer WHERE out_bill_no=? FOR UPDATE", outBillNo);
            if (row == null) throw new MiniBusinessException(404, "微信转账单不存在");
            verifyTransferNotification(resource, row, merchantId, appId);
            updateCashTransfer(row, resource);
        }
        catch (MiniBusinessException e) { throw e; }
        catch (Exception e) { throw new MiniBusinessException(400, "微信转账回调验签或解密失败"); }
    }

    static void verifyTransferNotification(JsonNode resource, Map<String, Object> row, String merchantId, String appId)
    {
        String billNo = resource.path("transfer_bill_no").asText("");
        String storedBillNo = row.get("transfer_bill_no") == null ? "" : String.valueOf(row.get("transfer_bill_no"));
        if (!appId.equals(resource.path("appid").asText()) || !merchantId.equals(resource.path("mch_id").asText())
                || billNo.isEmpty() || (!storedBillNo.isEmpty() && !storedBillNo.equals(billNo))
                || !String.valueOf(row.get("openid")).equals(resource.path("openid").asText())
                || ((Number) row.get("amount_cent")).intValue() != resource.path("transfer_amount").asInt(-1))
            throw new MiniBusinessException(409, "微信转账回调与本地转账信息不匹配");
    }

    private Map<String, Object> transferResult(JsonNode response)
    {
        String state = response.path("state").asText("");
        if (!("ACCEPTED".equals(state) || "PROCESSING".equals(state) || "WAIT_USER_CONFIRM".equals(state)
                || "TRANSFERING".equals(state) || "CANCELING".equals(state)
                || "SUCCESS".equals(state) || "FAIL".equals(state) || "CANCELLED".equals(state)))
            throw new MiniBusinessException(502, "微信转账返回了无法识别的状态");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appId", appId);
        result.put("mchId", merchantId);
        result.put("outBillNo", response.path("out_bill_no").asText(""));
        result.put("transferBillNo", response.path("transfer_bill_no").asText(""));
        result.put("openid", response.path("openid").asText(""));
        result.put("amountCent", response.path("transfer_amount").asInt(-1));
        result.put("state", state);
        result.put("packageInfo", response.path("package_info").asText(""));
        result.put("failReason", response.path("fail_reason").asText(""));
        return result;
    }

    private void updateCashTransfer(Map<String, Object> row, JsonNode response)
    {
        Map<String, Object> result = transferResult(response);
        String state = String.valueOf(result.get("state"));
        long transferId = ((Number) row.get("transfer_id")).longValue();
        int changed = jdbc.update("UPDATE biz_reward_transfer SET transfer_bill_no=NULLIF(?,''),status=?,package_info=NULLIF(?,''),fail_reason=? "
                        + "WHERE transfer_id=? AND status NOT IN ('SUCCESS','FAIL','CANCELLED')",
                result.get("transferBillNo"), state, result.get("packageInfo"), result.get("failReason"), transferId);
        if (changed != 1) return;
        long rewardId = ((Number) row.get("reward_id")).longValue();
        if ("SUCCESS".equals(state)) jdbc.update("UPDATE biz_reward SET status='received' WHERE reward_id=? AND status IN ('pending','processing','failed','closed')", rewardId);
        else if ("FAIL".equals(state) || "CANCELLED".equals(state)) jdbc.update("UPDATE biz_reward SET status='failed' WHERE reward_id=? AND status IN ('pending','processing')", rewardId);
        else jdbc.update("UPDATE biz_reward SET status='processing' WHERE reward_id=? AND status='pending'", rewardId);
    }

    private void requireTransferConfigured()
    {
        if (!isTransferConfigured())
            throw new MiniBusinessException(503, "微信商户转账能力或凭据尚未配置");
    }

    private void requireConfigured()
    {
        if (!enabled || blank(appId) || blank(merchantId) || blank(merchantSerial) || blank(privateKeyPem) || blank(apiV3Key) || blank(notifyUrl) || blank(platformCertPem)) throw new MiniBusinessException(503, "微信支付商户凭据尚未完整配置");
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) throw new MiniBusinessException(503, "微信支付 API v3 密钥必须为32字节");
    }

    public boolean isPayConfigured()
    {
        try { requireConfigured(); return true; }
        catch (MiniBusinessException e) { return false; }
    }

    private void requireRefundConfigured()
    {
        requireConfigured();
        if (blank(refundNotifyUrl) || !refundNotifyUrl.startsWith("https://")) throw new MiniBusinessException(503, "微信退款回调地址未配置为公网 HTTPS 地址");
    }

    protected String request(String method, String path, String body) throws Exception
    {
        String timestamp = String.valueOf(Instant.now().getEpochSecond()); String nonce = nonce();
        String authorization = "WECHATPAY2-SHA256-RSA2048 mchid=\"" + merchantId + "\",nonce_str=\"" + nonce + "\",timestamp=\"" + timestamp + "\",serial_no=\"" + merchantSerial + "\",signature=\"" + sign(method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n") + "\"";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try
        {
            ResponseEntity<byte[]> response = http.exchange("https://api.mch.weixin.qq.com" + path,
                    HttpMethod.valueOf(method), new HttpEntity<byte[]>(body.getBytes(StandardCharsets.UTF_8), headers), byte[].class);
            String responseBody = response.getBody() == null ? "" : new String(response.getBody(), StandardCharsets.UTF_8);
            HttpHeaders signedHeaders = response.getHeaders();
            verifyWechatSignature(signedHeaders.getFirst("Wechatpay-Signature"), signedHeaders.getFirst("Wechatpay-Timestamp"),
                    signedHeaders.getFirst("Wechatpay-Nonce"), signedHeaders.getFirst("Wechatpay-Serial"),
                    responseBody, platformPublicKey(), platformVerificationSerial());
            return responseBody;
        }
        catch (HttpStatusCodeException e)
        {
            throw new WechatApiException(e.getRawStatusCode(), e.getResponseBodyAsString(StandardCharsets.UTF_8));
        }
        catch (RestClientException e)
        {
            throw new IOException("微信接口请求失败", e);
        }
    }

    static final class WechatApiException extends IOException
    {
        private static final long serialVersionUID = 1L;
        private final int statusCode;
        private final String responseBody;
        WechatApiException(int statusCode) { this(statusCode, ""); }
        WechatApiException(int statusCode, String responseBody) { this.statusCode = statusCode; this.responseBody = responseBody == null ? "" : responseBody; }
    }

    private String paymentApiError(WechatApiException exception)
    {
        if (exception.statusCode == 401 || exception.statusCode == 403)
            return "微信支付商户签名校验失败，请检查商户号、商户证书序列号和私钥配置";
        try
        {
            JsonNode node = json.readTree(exception.responseBody);
            String message = node.path("message").asText("").trim();
            String code = node.path("code").asText("").trim();
            if (!message.isEmpty()) return code.isEmpty() ? "微信支付下单失败：" + truncate(message, 120) : "微信支付下单失败（" + code + "）：" + truncate(message, 120);
        }
        catch (Exception ignored) { }
        return "微信支付下单失败，请检查线上支付配置后重试";
    }

    private void verifyNotify(HttpServletRequest request, String body) throws Exception
    {
        String signature = request.getHeader("Wechatpay-Signature"); String timestamp = request.getHeader("Wechatpay-Timestamp"); String nonce = request.getHeader("Wechatpay-Nonce");
        String serial = request.getHeader("Wechatpay-Serial");
        verifyNotifyTimestamp(timestamp, Instant.now().getEpochSecond());
        verifyWechatSignature(signature, timestamp, nonce, serial, body,
                platformPublicKey(), platformVerificationSerial());
    }

    static void verifyNotifyTimestamp(String timestamp, long now) throws IOException
    {
        try
        {
            long signedAt = Long.parseLong(timestamp);
            if (signedAt < now - 300 || signedAt > now + 300) throw new IOException("WeChat notification timestamp expired");
        }
        catch (NumberFormatException e) { throw new IOException("invalid WeChat notification timestamp", e); }
    }

    static void verifyWechatSignature(String signature, String timestamp, String nonce, String serial, String body,
            PublicKey publicKey, String expectedSerial) throws Exception
    {
        if (blank(signature) || blank(timestamp) || blank(nonce)) throw new IOException("missing WeChat signature headers");
        if (blank(serial) || !expectedSerial.equalsIgnoreCase(serial)) throw new IOException("platform certificate serial mismatch");
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update((timestamp + "\n" + nonce + "\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
        if (!verifier.verify(Base64.getDecoder().decode(signature))) throw new IOException("invalid WeChat signature");
    }

    private JsonNode decryptResource(JsonNode resource) throws Exception
    {
        byte[] cipherText = Base64.getDecoder().decode(resource.path("ciphertext").asText());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES"), new GCMParameterSpec(128, resource.path("nonce").asText().getBytes(StandardCharsets.UTF_8))); cipher.updateAAD(resource.path("associated_data").asText().getBytes(StandardCharsets.UTF_8));
        return json.readTree(new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8));
    }

    private X509Certificate platformCertificate() throws Exception
    {
        String pem = readPem(platformCertPem); return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new java.io.ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }

    /** Supports both the legacy platform certificate and WeChat Pay platform public-key mode. */
    private PublicKey platformPublicKey() throws Exception
    {
        String pem = readPem(platformCertPem).trim();
        if (pem.contains("-----BEGIN PUBLIC KEY-----")) {
            String value = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(value)));
        }
        return platformCertificate().getPublicKey();
    }

    private String platformVerificationSerial() throws Exception
    {
        if (!blank(platformSerial)) return platformSerial.trim();
        String pem = readPem(platformCertPem).trim();
        if (pem.contains("-----BEGIN CERTIFICATE-----")) return platformCertificate().getSerialNumber().toString(16);
        throw new IOException("微信支付平台公钥 ID 未配置");
    }

    private boolean platformVerificationConfigured()
    {
        if (blank(platformCertPem)) return false;
        if (!blank(platformSerial)) return true;
        try { return readPem(platformCertPem).contains("-----BEGIN CERTIFICATE-----"); }
        catch (Exception ignored) { return false; }
    }

    String verificationMaterialFingerprint()
    {
        if (blank(privateKeyPem) || blank(platformCertPem)) return null;
        try
        {
            PrivateKey merchantKey = privateKey();
            PublicKey platformKey = platformPublicKey();
            String serial = platformVerificationSerial();
            return ProductionVerificationService.fingerprint("wechat-pay-material",
                    Base64.getEncoder().encodeToString(merchantKey.getEncoded()),
                    Base64.getEncoder().encodeToString(platformKey.getEncoded()), serial);
        }
        catch (Exception e) { log.warn("微信支付验收材料指纹计算失败: {}", e.toString()); return null; }
    }

    private PrivateKey privateKey() throws Exception { String value = readPem(privateKeyPem).replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", ""); return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(value))); }
    private String sign(String value) throws Exception { Signature signer = Signature.getInstance("SHA256withRSA"); signer.initSign(privateKey()); signer.update(value.getBytes(StandardCharsets.UTF_8)); return Base64.getEncoder().encodeToString(signer.sign()); }
    private static String readPem(String value) throws IOException { if (value.contains("-----BEGIN")) return value; return new String(Files.readAllBytes(Paths.get(value)), StandardCharsets.UTF_8); }
    private static String nonce() { return UUID.randomUUID().toString().replace("-", ""); }
    private static String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private Map<String, Object> first(String sql, Object... args) { java.util.List<Map<String, Object>> rows = jdbc.queryForList(sql, args); return rows.isEmpty() ? null : rows.get(0); }
}
