package com.ruoyi.web.controller.business;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Recovers payments whose WeChat notification was lost, including cancelled orders needing a refund. */
@Component
public class PendingPaymentReconciliationJob
{
    private static final Logger log = LoggerFactory.getLogger(PendingPaymentReconciliationJob.class);
    private final JdbcTemplate jdbc;
    private final WechatPayService pay;

    public PendingPaymentReconciliationJob(JdbcTemplate jdbc, WechatPayService pay)
    {
        this.jdbc = jdbc;
        this.pay = pay;
    }

    @Scheduled(fixedDelayString = "${mini.wechat.pay-reconcile-interval-ms:60000}",
            initialDelayString = "${mini.wechat.pay-reconcile-initial-delay-ms:60000}")
    public void reconcile()
    {
        if (!pay.isPayConfigured()) return;
        Timestamp cutoff = Timestamp.from(Instant.now().minusSeconds(20));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT p.member_id,p.order_no,COALESCE(r.attempt_count,0) AS attempt_count "
                        + "FROM biz_order_pay_request p JOIN biz_order o ON o.member_id=p.member_id AND o.order_no=p.order_no "
                        + "LEFT JOIN biz_order_pay_reconcile r ON r.order_no=p.order_no "
                        + "WHERE p.pay_request_id=(SELECT MIN(p2.pay_request_id) FROM biz_order_pay_request p2 "
                        + "WHERE p2.member_id=p.member_id AND p2.order_no=p.order_no) "
                        + "AND p.create_time<=? AND o.status IN ('pending','cancelled') AND o.paid_at IS NULL "
                        + "AND COALESCE(r.last_trade_state,'') NOT IN ('SUCCESS','CLOSED','REVOKED','PAYERROR','REFUND','REVIEW') "
                        + "AND (r.next_query_at IS NULL OR r.next_query_at<=NOW()) "
                        + "ORDER BY COALESCE(r.next_query_at,p.create_time),p.pay_request_id LIMIT 20", cutoff);
        for (Map<String, Object> row : rows)
        {
            try { reconcileRow(row); }
            catch (RuntimeException e) { log.error("微信支付补查台账写入失败，orderNo={}", row.get("order_no"), e); }
        }
    }

    public Map<String, Object> reconcileOne(String orderNo)
    {
        if (orderNo == null || orderNo.trim().isEmpty() || orderNo.length() > 64)
            throw new MiniBusinessException(400, "订单号无效");
        if (!pay.isPayConfigured()) throw new MiniBusinessException(503, "微信支付商户凭据尚未完整配置");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT p.member_id,p.order_no,COALESCE(r.attempt_count,0) AS attempt_count "
                        + "FROM biz_order_pay_request p LEFT JOIN biz_order_pay_reconcile r ON r.order_no=p.order_no "
                        + "WHERE p.order_no=? ORDER BY p.pay_request_id LIMIT 1", orderNo);
        if (rows.isEmpty()) throw new MiniBusinessException(404, "订单没有微信支付请求记录");
        return reconcileRow(rows.get(0));
    }

    private Map<String, Object> reconcileRow(Map<String, Object> row)
    {
            String orderNo = String.valueOf(row.get("order_no"));
            long memberId = ((Number) row.get("member_id")).longValue();
            int attempt = ((Number) row.get("attempt_count")).intValue() + 1;
            String state;
            String error = "";
            try
            {
                state = pay.syncPaymentStatus(memberId, orderNo);
                if ("REFUND".equals(state))
                {
                    error = "微信交易已退款，请核对本地订单及商户账单";
                    log.error("微信支付补查发现平台已退款但本地未确认支付，orderNo={}", orderNo);
                }
            }
            catch (RuntimeException e)
            {
                state = e instanceof MiniBusinessException && (((MiniBusinessException) e).getCode() == 400
                        || ((MiniBusinessException) e).getCode() == 409) ? "REVIEW" : "ERROR";
                error = String.valueOf(e.getMessage());
                log.warn("微信支付补查未完成，orderNo={} state={} reason={}", orderNo, state, error);
            }
            if (error.length() > 240) error = error.substring(0, 240);
            Timestamp next = needsRetry(state)
                    ? Timestamp.from(Instant.now().plusSeconds(retryDelaySeconds(attempt))) : null;
            jdbc.update("INSERT INTO biz_order_pay_reconcile(order_no,member_id,last_trade_state,attempt_count,last_query_at,next_query_at,last_error) "
                            + "VALUES(?,?,?,1,NOW(),?,?) ON DUPLICATE KEY UPDATE member_id=VALUES(member_id),"
                            + "last_trade_state=VALUES(last_trade_state),attempt_count=attempt_count+1,"
                            + "last_query_at=VALUES(last_query_at),next_query_at=VALUES(next_query_at),last_error=VALUES(last_error)",
                    orderNo, memberId, state, next, error);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderNo", orderNo);
            result.put("tradeState", state);
            result.put("lastError", error);
            result.put("nextQueryAt", next);
            return result;
    }

    static boolean needsRetry(String state)
    {
        return "ERROR".equals(state) || "NOT_FOUND".equals(state)
                || "NOTPAY".equals(state) || "USERPAYING".equals(state);
    }

    static long retryDelaySeconds(int attempt)
    {
        return attempt <= 3 ? 60 : attempt <= 12 ? 300 : 3600;
    }
}
