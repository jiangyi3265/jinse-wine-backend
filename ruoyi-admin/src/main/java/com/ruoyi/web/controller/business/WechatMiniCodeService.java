package com.ruoyi.web.controller.business;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.web.core.storage.MinioStorageService;
import org.springframework.beans.factory.annotation.Autowired;

/** Official WeChat mini-program code generation for referral attribution. */
@Service
public class WechatMiniCodeService
{
    private static final Logger log = LoggerFactory.getLogger(WechatMiniCodeService.class);
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String CODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token={token}";

    private final RestTemplate http = WechatHttpClient.create();
    private final ObjectMapper json;

    @Autowired(required = false)
    private MinioStorageService storage;

    @Value("${mini.wechat.app-id:}") private String appId;
    @Value("${mini.wechat.app-secret:}") private String appSecret;
    @Value("${mini.wechat.code-env-version:release}") private String codeEnvVersion;
    @Value("${mini.wechat.code-check-path:true}") private boolean codeCheckPath;

    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    public WechatMiniCodeService(ObjectMapper json)
    {
        this.json = json;
    }

    public String referralCode(String inviteCode)
    {
        return generate(inviteCode, "pages/index/index");
    }

    public String productReferralCode(long memberId, long productId)
    {
        return generate(PromotionProductScene.encode(memberId, productId), "pages/product/index");
    }

    public String lotteryCode(String drawCode)
    {
        return generate(drawCode, "pages/scan/index");
    }

    public String pickupCode(String orderNo)
    {
        return generate(orderNo, "pages/verification/index");
    }

    public String redemptionCode(long rewardId, String exchangeCode)
    {
        return generate(RedemptionScene.encode(rewardId, exchangeCode), "pages/verification/index");
    }

    private String generate(String scene, String page)
    {
        if (blank(appId) || blank(appSecret)) throw new MiniBusinessException(503, "微信小程序凭据未配置，无法生成官方小程序码");
        if (blank(scene) || scene.length() > 32 || !scene.matches("[A-Za-z0-9_-]+"))
            throw new MiniBusinessException(409, "小程序码场景值不符合长度或字符限制");
        if (!"develop".equals(codeEnvVersion) && !"trial".equals(codeEnvVersion) && !"release".equals(codeEnvVersion))
            throw new MiniBusinessException(500, "小程序码环境配置无效");
        try
        {
            String fileName = codeFileName(appId, codeEnvVersion, codeCheckPath, page, scene);
            String objectKey = "profile/mini-qrcode/" + fileName;
            if (storage != null && storage.isEnabled())
            {
                if (!storage.exists(objectKey))
                {
                    byte[] png = createCode(scene, page);
                    try (ByteArrayInputStream input = new ByteArrayInputStream(png))
                    {
                        storage.put(objectKey, input, png.length);
                    }
                }
                return "/" + objectKey;
            }
            Path directory = Paths.get(RuoYiConfig.getProfile(), "mini-qrcode").toAbsolutePath().normalize();
            Path image = directory.resolve(fileName).normalize();
            if (!image.startsWith(directory)) throw new IOException("invalid qr path");
            if (!Files.isRegularFile(image) || Files.size(image) == 0)
            {
                Files.createDirectories(directory);
                byte[] png = createCode(scene, page);
                Path temporary = Files.createTempFile(directory, fileName, ".tmp");
                try
                {
                    Files.write(temporary, png);
                    try { Files.move(temporary, image, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                    catch (AtomicMoveNotSupportedException e) { Files.move(temporary, image, StandardCopyOption.REPLACE_EXISTING); }
                }
                finally { Files.deleteIfExists(temporary); }
            }
            return "/profile/mini-qrcode/" + fileName;
        }
        catch (MiniBusinessException e)
        {
            log.warn("Official mini-program code generation failed: {}", e.getMessage());
            throw e;
        }
        catch (Exception e)
        {
            log.warn("Official mini-program code generation failed for page {}", page, e);
            throw new MiniBusinessException(502, "微信官方推广码生成失败");
        }
    }

    private byte[] createCode(String scene, String page) throws Exception
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("scene", scene);
        body.put("page", page);
        body.put("check_path", codeCheckPath);
        body.put("env_version", codeEnvVersion);
        body.put("width", 430);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<byte[]> response = http.postForEntity(CODE_URL, new HttpEntity<>(json.writeValueAsBytes(body), headers), byte[].class, token());
        byte[] bytes = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || bytes == null || bytes.length == 0)
            throw new IOException("empty WeChat code response");
        if (bytes[0] == '{')
        {
            JsonNode error = json.readTree(bytes);
            throw new MiniBusinessException(502, "微信官方推广码生成失败：" + error.path("errmsg").asText("未知错误"));
        }
        return bytes;
    }

    private synchronized String token()
    {
        if (!blank(accessToken) && System.currentTimeMillis() < tokenExpiresAt) return accessToken;
        try
        {
            JsonNode result = http.getForObject(TOKEN_URL, JsonNode.class, appId, appSecret);
            if (result == null || result.has("errcode") || blank(result.path("access_token").asText("")))
                throw new MiniBusinessException(502, "微信 access_token 获取失败");
            accessToken = result.path("access_token").asText();
            long seconds = Math.max(300, result.path("expires_in").asLong(7200) - 300);
            tokenExpiresAt = System.currentTimeMillis() + seconds * 1000;
            return accessToken;
        }
        catch (MiniBusinessException e) { throw e; }
        catch (RestClientException e) { throw new MiniBusinessException(502, "微信 access_token 服务暂不可用"); }
    }

    private static String sha256(String value) throws Exception
    {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) result.append(String.format("%02x", item));
        return result.toString();
    }

    static String codeFileName(String appId, String envVersion, boolean checkPath, String page, String scene) throws Exception
    {
        return sha256(String.join("\n", "v1", appId, envVersion, String.valueOf(checkPath), page, scene, "430")) + ".png";
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
