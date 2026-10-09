package com.ruoyi.web.controller.business;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WechatLogisticsClient
{
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    @Value("${mini.wechat.app-id:}") private String appId;
    @Value("${mini.wechat.app-secret:}") private String secret;
    private String accessToken;
    private long expiresAt;
    private List<Map<String, String>> cachedCompanies;
    private long companiesExpireAt;

    public WechatLogisticsClient(ObjectMapper json) { this.json = json; }
    public boolean configured() { return appId != null && !appId.isBlank() && secret != null && !secret.isBlank(); }

    public JsonNode post(String action, Map<String, Object> payload)
    {
        if (!List.of("trace_waybill", "query_trace").contains(action)) throw new IllegalArgumentException("物流接口无效");
        return authorized("https://api.weixin.qq.com/cgi-bin/express/delivery/open_msg/" + action, payload);
    }

    public synchronized List<Map<String, String>> companies()
    {
        if (cachedCompanies != null && System.currentTimeMillis() < companiesExpireAt) return cachedCompanies;
        JsonNode response = authorized("https://api.weixin.qq.com/cgi-bin/express/delivery/open_msg/get_delivery_list", Map.of());
        List<Map<String, String>> result = new ArrayList<>();
        for (JsonNode item : response.path("delivery_list"))
            if (!item.path("delivery_id").asText().isBlank() && !item.path("delivery_name").asText().isBlank())
                result.add(Map.of("code", item.path("delivery_id").asText(), "name", item.path("delivery_name").asText()));
        if (result.isEmpty()) throw new MiniBusinessException(502, "微信未返回可用快递公司，请检查物流查询服务开通状态");
        cachedCompanies = result;
        companiesExpireAt = System.currentTimeMillis() + 3600000;
        return result;
    }

    private JsonNode authorized(String url, Map<String, Object> payload)
    {
        JsonNode response = request(url + "?access_token=" + token(), payload);
        int code = response.path("errcode").asInt(0);
        if (code == 40001 || code == 40014 || code == 42001)
        {
            synchronized (this) { accessToken = null; expiresAt = 0; }
            response = request(url + "?access_token=" + token(), payload);
            code = response.path("errcode").asInt(0);
        }
        if (code != 0) throw new MiniBusinessException(502, "微信物流查询失败（" + code + "），请检查服务开通、运单和额度");
        return response;
    }

    private synchronized String token()
    {
        if (!configured()) throw new MiniBusinessException(503, "微信小程序凭据未配置");
        if (accessToken != null && System.currentTimeMillis() < expiresAt) return accessToken;
        JsonNode response = request("https://api.weixin.qq.com/cgi-bin/stable_token", Map.of("grant_type", "client_credential", "appid", appId, "secret", secret));
        String value = response.path("access_token").asText();
        if (value.isBlank()) throw new MiniBusinessException(502, "微信物流服务凭据获取失败（" + response.path("errcode").asInt(-1) + "）");
        accessToken = value;
        expiresAt = System.currentTimeMillis() + Math.max(60, response.path("expires_in").asLong(7200) - 300) * 1000;
        return value;
    }

    private JsonNode request(String url, Map<String, Object> payload)
    {
        try
        {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new MiniBusinessException(502, "微信物流服务暂时不可用");
            return json.readTree(response.body());
        }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new MiniBusinessException(502, "微信物流查询已中断"); }
        catch (MiniBusinessException e) { throw e; }
        catch (Exception e) { throw new MiniBusinessException(502, "微信物流服务暂时无法连接"); }
    }
}
