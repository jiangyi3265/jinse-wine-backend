package com.ruoyi.web.controller.business;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Creates and rotates official WeCom customer-group joining QR codes. */
@Service
public class WechatWorkGroupQrService
{
    private static final Logger log = LoggerFactory.getLogger(WechatWorkGroupQrService.class);
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={corpId}&corpsecret={secret}";
    private static final String ADD_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/groupchat/add_join_way?access_token={token}";
    private static final String DELETE_URL = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/groupchat/del_join_way?access_token={token}";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json = new ObjectMapper();
    private final RestTemplate http;
    @Value("${mini.wechat.work.corp-id:}") private String corpId;
    @Value("${mini.wechat.work.secret:}") private String secret;
    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    @Autowired
    public WechatWorkGroupQrService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager)
    {
        this(jdbc, transactionManager, WechatHttpClient.create());
    }

    WechatWorkGroupQrService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, RestTemplate http)
    {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
        this.http = http;
    }

    public Map<String, Object> create(Object input)
    {
        List<String> chatIds = chatIds(input);
        return rotate(chatIds);
    }

    public Map<String, Object> groupChoices(String cursor, String selectedIds)
    {
        if (blank(corpId) || blank(secret)) throw new MiniBusinessException(503, "请先在后端配置企业微信 CorpID 和 Secret");
        List<String> ids = new ArrayList<>();
        String nextCursor = "";
        if (!blank(selectedIds)) ids.addAll(chatIds(selectedIds));
        else
        {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("limit", 10); if (!blank(cursor)) body.put("cursor", cursor);
            JsonNode list = readGroup("list", body);
            for (JsonNode item : list.path("group_chat_list")) ids.add(item.path("chat_id").asText());
            nextCursor = list.path("next_cursor").asText("");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String id : ids)
        {
            Map<String, Object> body = new LinkedHashMap<>(); body.put("chat_id", id); body.put("need_name", 0);
            JsonNode detail = readGroup("get", body).path("group_chat");
            Map<String, Object> row = new LinkedHashMap<>(); row.put("value", id); row.put("label", detail.path("name").asText("未命名客户群")); rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>(); result.put("rows", rows); result.put("nextCursor", nextCursor); result.put("hasMore", !nextCursor.isEmpty()); return result;
    }

    private JsonNode readGroup(String operation, Map<String, Object> body)
    {
        try
        {
            String url = "https://qyapi.weixin.qq.com/cgi-bin/externalcontact/groupchat/" + operation + "?access_token={token}";
            JsonNode result = http.postForObject(url, body, JsonNode.class, token());
            if (result != null && (result.path("errcode").asInt() == 40014 || result.path("errcode").asInt() == 42001))
            {
                tokenExpiresAt = 0;
                result = http.postForObject(url, body, JsonNode.class, token());
            }
            if (result == null || result.path("errcode").asInt(-1) != 0)
                throw new MiniBusinessException(502, "企业微信群读取失败，请检查客户联系权限和可见范围");
            return result;
        }
        catch (RestClientException e) { throw new MiniBusinessException(502, "企业微信群接口暂不可用"); }
    }

    public synchronized Map<String, Object> confirmRevoked(String configId, String evidence)
    {
        String id = configId == null ? "" : configId.trim();
        String note = evidence == null ? "" : evidence.trim();
        if (!id.matches("[A-Za-z0-9_-]{1,128}")) throw new MiniBusinessException(400, "旧二维码配置 ID 无效");
        if (note.length() < 10 || note.length() > 500) throw new MiniBusinessException(400, "请填写10至500字的企业微信核验证据");
        if (id.equals(config("wechat.work.groupQrConfigId"))) throw new MiniBusinessException(400, "当前生效二维码不能标记为已撤销");
        List<String> pending = pendingConfigIds();
        if (!pending.remove(id)) throw new MiniBusinessException(404, "该旧二维码不在待核验清单中");
        String pendingValue = pendingJson(pending);
        String statusText = pending.isEmpty() ? "旧二维码已人工核验撤销" : revocationStatus(pending.size());
        transactions.execute(status -> {
            save("wechat.work.groupQrPreviousConfigId", pendingValue);
            save("wechat.work.groupQrRevocationStatus", statusText);
            return null;
        });
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pendingRevocations", pending.size());
        response.put("revocationStatus", statusText);
        return response;
    }

    @Scheduled(cron = "${mini.wechat.work.group-qr-refresh-cron:0 0 3 * * *}")
    public synchronized void refreshExpiringCode()
    {
        if (blank(corpId) || blank(secret)) return;
        try
        {
            retryPendingRevocations();
            String chatIdConfig = config("wechat.work.groupChatIds");
            String expiresAt = config("wechat.work.groupQrExpiresAt");
            if (chatIdConfig.isEmpty() || expiresAt.isEmpty()) return;
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, TIME);
            if (expiry.isAfter(LocalDateTime.now().plusDays(7))) return;
            rotate(chatIds(chatIdConfig));
        }
        catch (Exception e) { log.warn("Enterprise WeChat group QR refresh failed: {}", e.getMessage()); }
    }

    private synchronized Map<String, Object> rotate(List<String> chatIds)
    {
        if (blank(corpId) || blank(secret)) throw new MiniBusinessException(503, "企业微信 CorpID/Secret 未在服务端配置");
        String oldConfigId = config("wechat.work.groupQrConfigId");
        List<String> pending = pendingConfigIds();
        try
        {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("scene", 2);
            body.put("remark", "金色酒业小程序社群");
            body.put("auto_create_room", 0);
            body.put("chat_id_list", chatIds);
            JsonNode result = http.postForObject(ADD_URL, body, JsonNode.class, token());
            if (result == null || result.path("errcode").asInt(-1) != 0 || blank(result.path("config_id").asText("")) || blank(result.path("qr_code").asText("")))
                throw new MiniBusinessException(502, "企业微信未能生成群二维码：" + (result == null ? "无接口响应" : result.path("errmsg").asText("未知错误")));
            String configId = result.path("config_id").asText();
            String qrCode = result.path("qr_code").asText();
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime expiresAt = createdAt.plusDays(364);
            if (!oldConfigId.isEmpty() && !oldConfigId.equals(configId) && !pending.contains(oldConfigId)) pending.add(oldConfigId);
            String pendingValue = pendingJson(pending);
            try
            {
                transactions.execute(status -> {
                    save("wechat.work.groupChatIds", String.join(",", chatIds));
                    save("wechat.work.groupQrConfigId", configId);
                    save("wechat.work.groupQr", qrCode);
                    save("wechat.work.groupQrCreatedAt", createdAt.format(TIME));
                    save("wechat.work.groupQrExpiresAt", expiresAt.format(TIME));
                    save("wechat.work.groupQrPreviousConfigId", pendingValue);
                    save("wechat.work.groupQrRevocationStatus", revocationStatus(pending.size()));
                    return null;
                });
            }
            catch (Exception e)
            {
                if (!deleteJoinWay(configId)) log.warn("New WeCom group QR requires manual revocation: {}", configId);
                throw new MiniBusinessException(502, "群二维码配置保存失败，原二维码未替换");
            }
            int pendingCount;
            try { pendingCount = retryPendingRevocations(); }
            catch (Exception e)
            {
                log.warn("WeCom group QR saved but revocation status could not be refreshed: {}", e.getMessage());
                try { pendingCount = pendingConfigIds().size(); }
                catch (Exception ignored) { pendingCount = pending.size(); }
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("qrUrl", qrCode);
            response.put("expiresAt", expiresAt.format(TIME));
            response.put("configId", configId);
            response.put("chatCount", chatIds.size());
            response.put("oldCodeRevoked", pendingCount == 0);
            response.put("pendingRevocations", pendingCount);
            response.put("revocationStatus", revocationStatus(pendingCount));
            return response;
        }
        catch (MiniBusinessException e) { throw e; }
        catch (RestClientException e) { throw new MiniBusinessException(502, "企业微信服务暂不可用，二维码未更新"); }
        catch (Exception e) { throw new MiniBusinessException(502, "企业微信群二维码生成失败，旧二维码仍保留"); }
    }

    private boolean deleteJoinWay(String configId)
    {
        try
        {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("config_id", configId);
            JsonNode result = http.postForObject(DELETE_URL, body, JsonNode.class, token());
            if (result == null || result.path("errcode").asInt(-1) != 0) { log.warn("Previous WeCom group QR could not be revoked: {}", configId); return false; }
            return true;
        }
        catch (Exception e) { log.warn("Previous WeCom group QR revocation failed: {}", configId); return false; }
    }

    private synchronized int retryPendingRevocations()
    {
        List<String> pending = pendingConfigIds();
        if (pending.isEmpty()) return 0;
        for (String configId : new ArrayList<>(pending))
        {
            if (!deleteJoinWay(configId)) continue;
            List<String> remaining = new ArrayList<>(pending);
            remaining.remove(configId);
            String pendingValue = pendingJson(remaining);
            try
            {
                transactions.execute(status -> {
                    save("wechat.work.groupQrPreviousConfigId", pendingValue);
                    save("wechat.work.groupQrRevocationStatus", revocationStatus(remaining.size()));
                    return null;
                });
            }
            catch (Exception e)
            {
                log.error("WeCom group QR {} was revoked but local confirmation failed; verify it before clearing the pending record", configId, e);
                throw e;
            }
            pending = remaining;
        }
        return pending.size();
    }

    private List<String> pendingConfigIds()
    {
        String stored = config("wechat.work.groupQrPreviousConfigId").trim();
        if (stored.isEmpty()) return new ArrayList<>();
        if (!stored.startsWith("[")) return new ArrayList<>(Collections.singletonList(stored));
        try
        {
            JsonNode array = json.readTree(stored);
            if (!array.isArray()) throw new MiniBusinessException(500, "旧群二维码撤销记录无效");
            List<String> ids = new ArrayList<>();
            for (JsonNode item : array)
            {
                String id = item.asText("").trim();
                if (!id.isEmpty() && !ids.contains(id)) ids.add(id);
            }
            return ids;
        }
        catch (JsonProcessingException e) { throw new MiniBusinessException(500, "旧群二维码撤销记录无效"); }
    }

    private String pendingJson(List<String> ids)
    {
        if (ids.isEmpty()) return "";
        try { return json.writeValueAsString(ids); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize pending WeCom QR revocations", e); }
    }

    private String revocationStatus(int pendingCount)
    {
        return pendingCount == 0 ? "已撤销旧二维码" : pendingCount + " 个旧二维码撤销待重试";
    }

    private synchronized String token()
    {
        if (!blank(accessToken) && System.currentTimeMillis() < tokenExpiresAt) return accessToken;
        JsonNode result = http.getForObject(TOKEN_URL, JsonNode.class, corpId, secret);
        if (result == null || result.path("errcode").asInt(-1) != 0 || blank(result.path("access_token").asText("")))
            throw new MiniBusinessException(502, "企业微信 access_token 获取失败");
        accessToken = result.path("access_token").asText();
        tokenExpiresAt = System.currentTimeMillis() + Math.max(300, result.path("expires_in").asLong(7200) - 300) * 1000;
        return accessToken;
    }

    private List<String> chatIds(Object input)
    {
        String value = input == null ? "" : String.valueOf(input).trim();
        List<String> ids = new ArrayList<>();
        for (String id : value.split("[,，\\s]+")) if (!id.trim().isEmpty()) ids.add(id.trim());
        if (ids.isEmpty() || ids.size() > 5) throw new MiniBusinessException(400, "请填写1至5个已创建的企业微信群 ID");
        for (String id : ids) if (!id.matches("[A-Za-z0-9_-]{1,128}")) throw new MiniBusinessException(400, "企业微信群 ID 格式无效");
        if (ids.stream().distinct().count() != ids.size()) throw new MiniBusinessException(400, "企业微信群 ID 不能重复");
        return ids;
    }

    private String config(String key)
    {
        List<String> values = jdbc.query("SELECT config_value FROM biz_channel_config WHERE config_key=?", (rs, row) -> rs.getString(1), key);
        return values.isEmpty() ? "" : values.get(0);
    }

    private void save(String key, String value)
    {
        jdbc.update("INSERT INTO biz_channel_config(config_key,config_value) VALUES(?,?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)", key, value);
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
