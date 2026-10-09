package com.ruoyi.web.controller.business;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Delivers only to members who explicitly authorized the configured mini-program template. */
@Service
public class WechatSocialDeliveryService
{
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token={token}";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RestTemplate http = WechatHttpClient.create();
    private final TransactionTemplate transaction;
    @Value("${mini.wechat.app-id:}") private String appId;
    @Value("${mini.wechat.app-secret:}") private String appSecret;
    @Value("${mini.wechat.code-env-version:release}") private String codeEnvVersion;
    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    public WechatSocialDeliveryService(JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager manager)
    {
        this.jdbc = jdbc;
        this.json = json;
        this.transaction = new TransactionTemplate(manager);
    }

    public Map<String, Object> subscriptionConfig()
    {
        String templateId = config("wechat.mini.socialTemplateId");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", templateId);
        result.put("enabled", !blank(templateId) && !blank(appId) && !blank(appSecret));
        return result;
    }

    public boolean recordSubscription(long memberId, String templateId, String result)
    {
        return recordSubscription(memberId, templateId, result, null);
    }

    public boolean recordSubscription(long memberId, String templateId, String result, String requestId)
    {
        String configured = config("wechat.mini.socialTemplateId");
        if (blank(configured) || !configured.equals(templateId)) throw new MiniBusinessException(409, "社群通知模板未配置或已更新");
        if (!"accept".equals(result)) return false;
        String normalized = normalizeRequestId(requestId);
        return transaction.execute(status -> {
            if (normalized != null)
            {
                Map<String, Object> existing = first("SELECT template_id,status FROM biz_social_subscription WHERE member_id=? AND request_id=? FOR UPDATE", memberId, normalized);
                if (existing != null)
                {
                    if (!configured.equals(String.valueOf(existing.get("template_id")))) throw new MiniBusinessException(409, "请求号已用于其他订阅模板");
                    return true;
                }
                jdbc.update("INSERT IGNORE INTO biz_social_subscription(member_id,template_id,status,request_id) VALUES(?,?,'available',?)", memberId, configured, normalized);
                return true;
            }
            // Older clients did not send a request ID; preserve their behavior while new clients are idempotent.
            jdbc.update("INSERT INTO biz_social_subscription(member_id,template_id,status) VALUES(?,?,'available')", memberId, configured);
            return true;
        });
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static String normalizeRequestId(String requestId)
    {
        if (blank(requestId)) return null;
        String value = requestId.trim();
        if (!value.matches("[A-Za-z0-9_-]{8,64}")) throw new MiniBusinessException(400, "订阅请求号无效");
        return value;
    }

    public List<Map<String, Object>> campaigns()
    {
        return jdbc.queryForList("SELECT c.campaign_id AS campaignId,c.campaign_type AS campaignType,c.title,c.content,c.coupon_template_id AS couponTemplateId,"
                + "JSON_UNQUOTE(JSON_EXTRACT(c.target_json,'$.audience')) AS audience,"
                + "COALESCE(JSON_UNQUOTE(JSON_EXTRACT(c.target_json,'$.channels')),'subscribe') AS channels,c.scheduled_at AS scheduledAt,c.status,c.sent_count AS sentCount,c.create_time AS createTime,"
                + "COALESCE(d.pendingCount,0)+COALESCE(g.pendingCount,0) AS pendingCount,COALESCE(d.sendingCount,0)+COALESCE(g.sendingCount,0) AS sendingCount,"
                + "COALESCE(d.deliveredCount,0)+COALESCE(g.deliveredCount,0) AS deliveredCount,COALESCE(d.failedCount,0)+COALESCE(g.failedCount,0) AS failedCount "
                + "FROM biz_social_campaign c "
                + "LEFT JOIN (SELECT campaign_id,SUM(status IN ('pending','retry')) pendingCount,SUM(status='sending') sendingCount,SUM(status='sent') deliveredCount,SUM(status IN ('failed','unknown')) failedCount FROM biz_social_delivery GROUP BY campaign_id) d ON d.campaign_id=c.campaign_id "
                + "LEFT JOIN (SELECT campaign_id,SUM(status IN ('pending','retry')) pendingCount,SUM(status='sending') sendingCount,SUM(status='sent') deliveredCount,SUM(status IN ('failed','unknown')) failedCount FROM biz_social_group_delivery GROUP BY campaign_id) g ON g.campaign_id=c.campaign_id "
                + "ORDER BY c.campaign_id DESC LIMIT 100");
    }

    /** Returns delivery-level evidence for operator reconciliation without exposing member contact data. */
    public List<Map<String, Object>> deliveries(long campaignId)
    {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM biz_social_campaign WHERE campaign_id=?", Long.class, campaignId) == 0)
            throw new MiniBusinessException(404, "社群活动不存在");
        return jdbc.queryForList("SELECT delivery_id AS deliveryId,'subscribe' AS channel,d.member_id AS memberId,COALESCE(m.nickname,'') AS target,d.status,d.attempt_count AS attemptCount,d.error_message AS errorMessage,d.next_retry_at AS nextRetryAt,d.sent_at AS sentAt "
                + "FROM biz_social_delivery d LEFT JOIN biz_member m ON m.member_id=d.member_id WHERE d.campaign_id=? "
                + "UNION ALL SELECT delivery_id AS deliveryId,'group' AS channel,NULL AS memberId,chat_id AS target,status,attempt_count AS attemptCount,error_message AS errorMessage,next_retry_at AS nextRetryAt,sent_at AS sentAt "
                + "FROM biz_social_group_delivery WHERE campaign_id=? ORDER BY channel,deliveryId LIMIT 500", campaignId, campaignId);
    }

    @Scheduled(fixedDelayString = "${mini.wechat.social-delivery-interval-ms:10000}")
    public void dispatch()
    {
        if (blank(appId) || blank(appSecret) || blank(config("wechat.mini.socialTemplateId"))) return;
        recoverExpiredClaims();
        for (int i = 0; i < 20; i++)
        {
            Map<String, Object> task = claim();
            if (task == null) return;
            deliver(task);
        }
    }

    private Map<String, Object> claim()
    {
        return transaction.execute(status -> {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT d.delivery_id,d.campaign_id,d.member_id,d.subscription_id,s.template_id,m.wechat_openid,c.title,c.content "
                    + "FROM biz_social_delivery d JOIN biz_social_subscription s ON s.subscription_id=d.subscription_id AND s.status='reserved' AND s.delivery_id=d.delivery_id "
                    + "JOIN biz_member m ON m.member_id=d.member_id AND m.status='0' JOIN biz_social_campaign c ON c.campaign_id=d.campaign_id "
                    + "WHERE d.status IN ('pending','retry') AND COALESCE(d.next_retry_at,NOW())<=NOW() AND COALESCE(d.locked_until,NOW())<=NOW() AND COALESCE(c.scheduled_at,NOW())<=NOW() "
                    + "ORDER BY d.delivery_id LIMIT 1 FOR UPDATE");
            if (rows.isEmpty()) return null;
            Map<String, Object> task = rows.get(0);
            jdbc.update("UPDATE biz_social_delivery SET status='sending',attempt_count=attempt_count+1,locked_until=DATE_ADD(NOW(),INTERVAL 2 MINUTE) WHERE delivery_id=?", task.get("delivery_id"));
            jdbc.update("UPDATE biz_social_campaign SET status='sending' WHERE campaign_id=? AND status='queued'", task.get("campaign_id"));
            return task;
        });
    }

    private void deliver(Map<String, Object> task)
    {
        String result = "unknown";
        String error = "";
        int errcode = -1;
        String currentToken;
        try { currentToken = token(); }
        catch (Exception e)
        {
            finish(task, "retry", -1, "微信凭据暂不可用，尚未提交消息");
            return;
        }
        try
        {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", task.get("wechat_openid"));
            body.put("template_id", task.get("template_id"));
            body.put("page", "pages/vip/index");
            body.put("miniprogram_state", subscriptionPageState());
            String titleField = field("wechat.mini.socialTitleField", "thing1");
            String contentField = field("wechat.mini.socialContentField", "thing2");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put(titleField, value(task.get("title")));
            data.put(contentField, value(task.get("content")));
            body.put("data", data);
            JsonNode response = http.postForObject(SEND_URL, body, JsonNode.class, currentToken);
            errcode = response == null ? -1 : response.path("errcode").asInt(-1);
            error = response == null ? "微信接口未返回结果" : response.path("errmsg").asText("");
            result = errcode == 0 ? "sent" : "retry";
            if (errcode == 40001 || errcode == 42001) { accessToken = null; tokenExpiresAt = 0; }
        }
        catch (RestClientException e)
        {
            // The request may have reached WeChat before a network timeout; do not resend blindly.
            result = "unknown";
            error = "微信接口请求结果未知，需人工核查";
        }
        catch (Exception e)
        {
            result = "retry";
            error = "微信消息发送失败";
        }
        finish(task, result, errcode, error);
    }

    private void finish(Map<String, Object> task, String outcome, int errcode, String error)
    {
        transaction.executeWithoutResult(status -> {
            long deliveryId = number(task.get("delivery_id"));
            long subscriptionId = number(task.get("subscription_id"));
            int attempts = jdbc.queryForObject("SELECT attempt_count FROM biz_social_delivery WHERE delivery_id=? FOR UPDATE", Integer.class, deliveryId);
            if ("sent".equals(outcome))
            {
                jdbc.update("UPDATE biz_social_delivery SET status='sent',sent_at=NOW(),locked_until=NULL,error_message='',provider_message_id='' WHERE delivery_id=?", deliveryId);
                jdbc.update("UPDATE biz_social_subscription SET status='consumed',consumed_at=NOW(),reserved_until=NULL WHERE subscription_id=? AND delivery_id=?", subscriptionId, deliveryId);
            }
            else if ("unknown".equals(outcome))
            {
                jdbc.update("UPDATE biz_social_delivery SET status='unknown',locked_until=NULL,error_message=? WHERE delivery_id=?", shorten(error), deliveryId);
                jdbc.update("UPDATE biz_social_subscription SET status='consumed',consumed_at=NOW(),reserved_until=NULL WHERE subscription_id=? AND delivery_id=?", subscriptionId, deliveryId);
            }
            else if (errcode == 43101)
            {
                jdbc.update("UPDATE biz_social_delivery SET status='failed',locked_until=NULL,error_message=? WHERE delivery_id=?", "会员未授权该模板消息", deliveryId);
                jdbc.update("UPDATE biz_social_subscription SET status='rejected',reserved_until=NULL WHERE subscription_id=? AND delivery_id=?", subscriptionId, deliveryId);
            }
            else if (attempts < 3)
            {
                int delay = attempts == 1 ? 30 : 120;
                jdbc.update("UPDATE biz_social_delivery SET status='retry',next_retry_at=DATE_ADD(NOW(),INTERVAL ? SECOND),locked_until=NULL,error_message=? WHERE delivery_id=?", delay, shorten(error), deliveryId);
            }
            else
            {
                jdbc.update("UPDATE biz_social_delivery SET status='failed',locked_until=NULL,error_message=? WHERE delivery_id=?", shorten(error), deliveryId);
                jdbc.update("UPDATE biz_social_subscription SET status='available',delivery_id=NULL,reserved_until=NULL WHERE subscription_id=? AND delivery_id=?", subscriptionId, deliveryId);
            }
            long campaignId = number(task.get("campaign_id"));
            Map<String, Object> counts = jdbc.queryForMap("SELECT "
                    + "(SELECT COUNT(*) FROM biz_social_delivery WHERE campaign_id=? AND status IN ('pending','sending','retry')) + "
                    + "(SELECT COUNT(*) FROM biz_social_group_delivery WHERE campaign_id=? AND status IN ('pending','sending','retry')) AS active, "
                    + "(SELECT COUNT(*) FROM biz_social_delivery WHERE campaign_id=? AND status='sent') + "
                    + "(SELECT COUNT(*) FROM biz_social_group_delivery WHERE campaign_id=? AND status='sent') AS delivered, "
                    + "(SELECT COUNT(*) FROM biz_social_delivery WHERE campaign_id=? AND status IN ('failed','unknown')) + "
                    + "(SELECT COUNT(*) FROM biz_social_group_delivery WHERE campaign_id=? AND status IN ('failed','unknown')) AS failed",
                    campaignId, campaignId, campaignId, campaignId, campaignId, campaignId);
            if (number(counts.get("active")) == 0)
            {
                long delivered = number(counts.get("delivered"));
                long failed = number(counts.get("failed"));
                String campaignStatus = delivered == 0 ? "failed" : failed == 0 ? "sent" : "partial";
                jdbc.update("UPDATE biz_social_campaign SET status=? WHERE campaign_id=?", campaignStatus, campaignId);
            }
        });
    }

    private void recoverExpiredClaims()
    {
        transaction.executeWithoutResult(status -> {
            jdbc.update("UPDATE biz_social_delivery SET status='unknown',locked_until=NULL,error_message='发送进程中断，微信结果未知，需人工核查' WHERE status='sending' AND locked_until<NOW()");
            jdbc.update("UPDATE biz_social_subscription s JOIN biz_social_delivery d ON d.subscription_id=s.subscription_id SET s.status='consumed',s.consumed_at=NOW(),s.reserved_until=NULL WHERE d.status='unknown' AND s.status='reserved'");
            jdbc.update("UPDATE biz_social_campaign c SET c.status=CASE WHEN EXISTS (SELECT 1 FROM biz_social_delivery sent WHERE sent.campaign_id=c.campaign_id AND sent.status='sent') OR EXISTS (SELECT 1 FROM biz_social_group_delivery sent WHERE sent.campaign_id=c.campaign_id AND sent.status='sent') THEN 'partial' ELSE 'failed' END "
                    + "WHERE c.status='sending' AND NOT EXISTS (SELECT 1 FROM biz_social_delivery active WHERE active.campaign_id=c.campaign_id AND active.status IN ('pending','sending','retry')) "
                    + "AND NOT EXISTS (SELECT 1 FROM biz_social_group_delivery active WHERE active.campaign_id=c.campaign_id AND active.status IN ('pending','sending','retry'))");
        });
    }

    private String token()
    {
        if (!blank(accessToken) && System.currentTimeMillis() < tokenExpiresAt) return accessToken;
        JsonNode response = http.getForObject(TOKEN_URL, JsonNode.class, appId, appSecret);
        if (response == null || response.has("errcode") || blank(response.path("access_token").asText(""))) throw new IllegalStateException("微信 access_token 获取失败");
        accessToken = response.path("access_token").asText();
        tokenExpiresAt = System.currentTimeMillis() + Math.max(300, response.path("expires_in").asLong(7200) - 300) * 1000;
        return accessToken;
    }

    private String config(String key)
    {
        List<String> values = jdbc.query("SELECT config_value FROM biz_channel_config WHERE config_key=?", (rs, row) -> rs.getString(1), key);
        return values.isEmpty() ? "" : values.get(0);
    }

    private String field(String key, String fallback)
    {
        String value = config(key);
        return value.matches("[A-Za-z][A-Za-z0-9_]{0,31}") ? value : fallback;
    }

    private String subscriptionPageState()
    {
        // Subscription messages use different environment names from the QR API.
        switch (codeEnvVersion)
        {
            case "develop": return "developer";
            case "trial": return "trial";
            case "release": return "formal";
            default: throw new IllegalStateException("Unsupported mini-program environment");
        }
    }

    private static String value(Object input)
    {
        String text = input == null ? "" : String.valueOf(input).trim().replaceAll("\\s+", " ");
        int end = text.offsetByCodePoints(0, Math.min(20, text.codePointCount(0, text.length())));
        return text.substring(0, end);
    }

    private static String shorten(String value) { return value == null ? "" : value.substring(0, Math.min(255, value.length())); }
    private static long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
