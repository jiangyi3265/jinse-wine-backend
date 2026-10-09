package com.ruoyi.web.controller.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/** Resolve complete store addresses to GCJ-02; credentials never leave the server. */
@Service
public class StoreGeocodingService
{
    private static final String PATH = "/ws/geocoder/v1/";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper json;
    @Value("${store.geocoding.tencent-key:${TENCENT_MAP_KEY:}}") private String key;
    @Value("${store.geocoding.tencent-sk:${TENCENT_MAP_SK:}}") private String sk;

    public StoreGeocodingService(ObjectMapper json) { this.json = json; }

    public Map<String, Object> resolve(List<String> region, String address)
    {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("门店地址解析服务尚未配置，请联系管理员配置腾讯位置服务 WebService Key");
        String prefix = region.get(0) + (region.get(0).equals(region.get(1)) ? "" : region.get(1)) + region.get(2);
        String fullAddress = address.startsWith(prefix) ? address : prefix + address;
        Map<String, String> params = new TreeMap<>();
        params.put("address", fullAddress);
        params.put("key", key.trim());
        params.put("region", region.get(1));
        String encodedQuery = queryString(params);
        if (sk != null && !sk.isBlank())
        {
            // Tencent signs the exact encoded query string sent over the wire.
            params.put("sig", DigestUtils.md5DigestAsHex((PATH + "?" + encodedQuery + sk.trim()).getBytes(StandardCharsets.UTF_8)));
            encodedQuery = queryString(params);
        }
        JsonNode body;
        try
        {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://apis.map.qq.com" + PATH + "?" + encodedQuery))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new java.io.IOException("Map service unavailable");
            body = json.readTree(response.body());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("地址解析已中断，请重试保存");
        }
        catch (Exception e)
        {
            // Do not propagate request URLs or provider messages containing credentials.
            throw new IllegalArgumentException("地图服务暂时无法连接，门店未保存，请稍后重试");
        }
        int status = body == null ? -1 : body.path("status").asInt(-1);
        if (status != 0)
            throw new IllegalArgumentException("地址解析失败（地图服务状态 " + status + "），请检查详细地址或联系管理员检查地图服务配置和额度");
        JsonNode result = body.path("result");
        String expectedCode = RegionCatalog.districtCode(region);
        if (expectedCode == null || !expectedCode.equals(result.path("ad_info").path("adcode").asText()))
            throw new IllegalArgumentException("解析出的地址与所选省市区不一致，请检查地区和详细地址");
        // Tencent recommends reliability >= 7 and level >= 9 for a precise point.
        if (result.path("reliability").asInt(0) < 7 || result.path("level").asInt(0) < 9)
            throw new IllegalArgumentException("地址定位不够准确，请补充街道、门牌号或具体建筑名称后保存");
        JsonNode location = result.path("location");
        if (!location.path("lat").isNumber() || !location.path("lng").isNumber()
                || !validCoordinates(location.path("lat").asText(), location.path("lng").asText()))
            throw new IllegalArgumentException("地图服务未返回有效位置，请检查详细地址后重试");
        return Map.of("latitude", location.path("lat").decimalValue().setScale(7, RoundingMode.HALF_UP),
                "longitude", location.path("lng").decimalValue().setScale(7, RoundingMode.HALF_UP));
    }

    private static String queryString(Map<String, String> params)
    {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("&"));
    }

    static boolean validCoordinates(Object latitude, Object longitude)
    {
        try
        {
            double lat = new BigDecimal(String.valueOf(latitude)).doubleValue();
            double lng = new BigDecimal(String.valueOf(longitude)).doubleValue();
            return Double.isFinite(lat) && Double.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180
                    && (lat != 0 || lng != 0);
        }
        catch (NumberFormatException e) { return false; }
    }
}
