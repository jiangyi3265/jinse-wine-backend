package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/** Sends VIP campaign text to configured WeCom customer groups with durable delivery state. */
@Service
public class WechatWorkGroupMessageService
{
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={corpId}&corpsecret={secret}";
    private static final String SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/groupchat/send?access_token={token}";
    private static final int MAX_ATTEMPTS = 3;

    private final JdbcTemplate jdbc;
    private final RestTemplate http = WechatHttpClient.create();
    private final TransactionTemplate transaction;
    @Value("${mini.wechat.work.corp-id:}") private String corpId;
    @Value("${mini.wechat.work.secret:}") private String secret;
    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    public WechatWorkGroupMessageService(JdbcTemplate jdbc, PlatformTransactionManager manager)
    {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
    }

    public boolean configured()
    {
        return !blank(corpId) && !blank(secret) && !groupIds().isEmpty();
    }

    public int enqueue(long campaignId, String content)
    {
        if (!configured()) throw new MiniBusinessException(503, "企业微信群消息未配置 CorpID、Secret 或目标群 ID");
        String message = truncate(content, 2000);
        if (message.isEmpty()) throw new MiniBusinessException(400, "社群消息内容不能为空");
        int queued = 0;
        for (String chatId : groupIds())
        {
            queued += jdbc.update("INSERT IGNORE INTO biz_social_group_delivery(campaign_id,chat_id,status) VALUES(?,?, 'pending')", campaignId, chatId);
        }
        return queued;
    }

    @Scheduled(fixedDelayString = "${mini.wechat.work.group-message-interval-ms:10000}")
    public void dispatch()
    {
        if (!configured()) return;
        recoverExpiredClaims();
        for (int i = 0; i < 10; i++)
        {
            Map<String, Object> task = claim();
            if (task == null) return;
            deliver(task);
        }
    }

    private Map<String, Object> claim()
    {
        return transaction.execute(status -> {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT g.delivery_id,g.campaign_id,g.chat_id,c.content "
                    + "FROM biz_social_group_delivery g JOIN biz_social_campaign c ON c.campaign_id=g.campaign_id "
                    + "WHERE g.status IN ('pending','retry') AND COALESCE(g.next_retry_at,NOW())<=NOW() "
                    + "AND COALESCE(g.locked_until,NOW())<=NOW() AND COALESCE(c.scheduled_at,NOW())<=NOW() "
                    + "ORDER BY g.delivery_id LIMIT 1 FOR UPDATE");
            if (rows.isEmpty()) return null;
            Map<String, Object> task = rows.get(0);
            jdbc.update("UPDATE biz_social_group_delivery SET status='sending',attempt_count=attempt_count+1,locked_until=DATE_ADD(NOW(),INTERVAL 2 MINUTE) WHERE delivery_id=?", task.get("delivery_id"));
            jdbc.update("UPDATE biz_social_campaign SET status='sending' WHERE campaign_id=? AND status='queued'", task.get("campaign_id"));
            return task;
        });
    }

    private void deliver(Map<String, Object> task)
    {
        String outcome = "unknown";
        String error = "";
        int errcode = -1;
        try
        {
            Map<String, Object> body = new LinkedHashMap<>();
            List<String> chatIds = new ArrayList<>();
            chatIds.add(String.valueOf(task.get("chat_id")));
            body.put("chat_id_list", chatIds);
            body.put("msgtype", "text");
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("content", truncate(String.valueOf(task.get("content")), 2000));
            body.put("text", text);
            JsonNode response = http.postForObject(SEND_URL, body, JsonNode.class, token());
            errcode = response == null ? -1 : response.path("errcode").asInt(-1);
            error = response == null ? "企业微信接口未返回结果" : response.path("errmsg").asText("");
            outcome = errcode == 0 ? "sent" : "retry";
            if (errcode == 40001 || errcode == 42001) { accessToken = null; tokenExpiresAt = 0; }
        }
        catch (RestClientException exception)
        {
            outcome = "unknown";
            error = "企业微信请求结果未知，需人工核查";
        }
        catch (Exception exception)
        {
            outcome = "retry";
            error = "企业微信群消息发送失败";
        }
        finish(task, outcome, errcode, error);
    }

    private void finish(Map<String, Object> task, String outcome, int errcode, String error)
    {
        transaction.executeWithoutResult(status -> {
            long deliveryId = number(task.get("delivery_id"));
            long campaignId = number(task.get("campaign_id"));
            int attempts = jdbc.queryForObject("SELECT attempt_count FROM biz_social_group_delivery WHERE delivery_id=? FOR UPDATE", Integer.class, deliveryId);
            if ("sent".equals(outcome))
            {
                jdbc.update("UPDATE biz_social_group_delivery SET status='sent',sent_at=NOW(),locked_until=NULL,error_message='' WHERE delivery_id=?", deliveryId);
            }
            else if ("unknown".equals(outcome))
            {
                jdbc.update("UPDATE biz_social_group_delivery SET status='unknown',locked_until=NULL,error_message=? WHERE delivery_id=?", shorten(error), deliveryId);
            }
            else if (attempts < MAX_ATTEMPTS)
            {
                int delay = attempts == 1 ? 30 : 120;
                jdbc.update("UPDATE biz_social_group_delivery SET status='retry',next_retry_at=DATE_ADD(NOW(),INTERVAL ? SECOND),locked_until=NULL,error_message=? WHERE delivery_id=?", delay, shorten(error), deliveryId);
            }
            else
            {
                jdbc.update("UPDATE biz_social_group_delivery SET status='failed',locked_until=NULL,error_message=? WHERE delivery_id=?", shorten(error), deliveryId);
            }
            refreshCampaign(campaignId);
        });
    }

    private void recoverExpiredClaims()
    {
        transaction.executeWithoutResult(status -> {
            List<Long> affectedCampaigns = jdbc.queryForList(
                    "SELECT DISTINCT campaign_id FROM biz_social_group_delivery "
                            + "WHERE status='sending' AND locked_until<NOW()", Long.class);
            jdbc.update("UPDATE biz_social_group_delivery SET status='unknown',locked_until=NULL,error_message='发送进程中断，企业微信结果未知，需人工核查' WHERE status='sending' AND locked_until<NOW()");
            // A campaign can use both subscription and group channels. Recompute
            // from both delivery tables so an expired group claim cannot hide an
            // unfinished subscription delivery.
            for (Long campaignId : affectedCampaigns) refreshCampaign(campaignId);
        });
    }

    private void refreshCampaign(long campaignId)
    {
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
            jdbc.update("UPDATE biz_social_campaign SET status=? WHERE campaign_id=?", delivered == 0 ? "failed" : failed == 0 ? "sent" : "partial", campaignId);
        }
    }

    private synchronized String token()
    {
        if (!blank(accessToken) && System.currentTimeMillis() < tokenExpiresAt) return accessToken;
        JsonNode response = http.getForObject(TOKEN_URL, JsonNode.class, corpId, secret);
        if (response == null || response.path("errcode").asInt(-1) != 0 || blank(response.path("access_token").asText("")))
            throw new MiniBusinessException(502, "企业微信 access_token 获取失败");
        accessToken = response.path("access_token").asText();
        tokenExpiresAt = System.currentTimeMillis() + Math.max(300, response.path("expires_in").asLong(7200) - 300) * 1000;
        return accessToken;
    }

    private List<String> groupIds()
    {
        String value = config("wechat.work.groupChatIds");
        Set<String> unique = new LinkedHashSet<>();
        for (String part : value.split(","))
        {
            String id = part.trim();
            if (id.isEmpty()) continue;
            if (!id.matches("[A-Za-z0-9_-]{1,128}")) throw new MiniBusinessException(400, "企业微信群 ID 格式无效");
            unique.add(id);
        }
        if (unique.size() > 5) throw new MiniBusinessException(400, "企业微信群最多配置5个群");
        return new ArrayList<>(unique);
    }

    private String config(String key)
    {
        List<String> values = jdbc.query("SELECT config_value FROM biz_channel_config WHERE config_key=?", (rs, row) -> rs.getString(1), key);
        return values.isEmpty() ? "" : values.get(0);
    }

    /** 企业微信群文本上限按 UTF-8 字节数计算，截断时保持完整 Unicode 代码点。 */
    private static String truncate(String value, int maxBytes)
    {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (text.getBytes(StandardCharsets.UTF_8).length <= maxBytes) return text;
        StringBuilder result = new StringBuilder();
        int bytes = 0;
        for (int offset = 0; offset < text.length(); )
        {
            int codePoint = text.codePointAt(offset);
            int width = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + width > maxBytes) break;
            result.appendCodePoint(codePoint);
            bytes += width;
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static String shorten(String value) { return value == null ? "" : value.substring(0, Math.min(255, value.length())); }
    private static long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
