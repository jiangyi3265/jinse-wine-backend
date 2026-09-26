package com.ruoyi.web.controller.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/** 微信身份与小程序会话。管理员 JWT 和会员令牌互不通用。 */
@Service
public class MiniMemberAuthService
{
    private static final String WECHAT_SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    private static final int SESSION_DAYS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final RestTemplate wechat = new RestTemplate();

    @Value("${mini.wechat.app-id:}")
    private String appId;

    @Value("${mini.wechat.app-secret:}")
    private String appSecret;

    public MiniMemberAuthService(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> login(String code)
    {
        if (appId.isEmpty() || appSecret.isEmpty())
        {
            throw new MiniBusinessException(503, "微信小程序凭据未配置，暂无法登录");
        }
        if (code == null || code.trim().isEmpty() || code.length() > 128)
        {
            throw new MiniBusinessException(400, "微信登录 code 无效");
        }
        JsonNode response;
        try
        {
            response = wechat.getForObject(WECHAT_SESSION_URL, JsonNode.class, appId, appSecret, code.trim());
        }
        catch (RestClientException exception)
        {
            throw new MiniBusinessException(502, "微信登录服务暂不可用");
        }
        // 只有微信服务端返回的 OpenID 可以创建会员，不接受客户端提供身份。
        String openid = response == null ? "" : response.path("openid").asText("");
        if (response == null || response.path("errcode").asInt(0) != 0 || openid.isEmpty())
        {
            throw new MiniBusinessException(401, "微信登录 code 已失效，请重试");
        }
        String unionId = response.path("unionid").asText("");
        jdbc.update("INSERT IGNORE INTO biz_member (wechat_openid, union_id) VALUES (?, ?)", openid,
                unionId.isEmpty() ? null : unionId);
        Map<String, Object> member = first(jdbc.queryForList(
                "SELECT member_id, nickname, avatar, phone, vip_level, staff_store_id, status FROM biz_member WHERE wechat_openid = ?",
                openid));
        if (member == null || !"0".equals(member.get("status")))
        {
            throw new MiniBusinessException(403, "会员账号已停用");
        }
        long memberId = ((Number) member.get("member_id")).longValue();
        String token = newToken();
        Instant expires = Instant.now().plus(SESSION_DAYS, ChronoUnit.DAYS);
        jdbc.update("INSERT INTO biz_member_session (token_hash, member_id, expires_at) VALUES (?, ?, ?)",
                sha256(token), memberId, Timestamp.from(expires));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("expiresAt", expires.toEpochMilli());
        result.put("user", toUser(member));
        return result;
    }

    public long requireMember(HttpServletRequest request)
    {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer "))
        {
            throw new MiniBusinessException(401, "请先登录");
        }
        String token = authorization.substring(7).trim();
        if (token.length() < 40 || token.length() > 128)
        {
            throw new MiniBusinessException(401, "登录状态无效");
        }
        Map<String, Object> session = first(jdbc.queryForList(
                "SELECT s.member_id FROM biz_member_session s JOIN biz_member m ON m.member_id = s.member_id "
                        + "WHERE s.token_hash = ? AND s.expires_at > NOW() AND m.status = '0'",
                sha256(token)));
        if (session == null)
        {
            throw new MiniBusinessException(401, "登录已过期，请重新登录");
        }
        return ((Number) session.get("member_id")).longValue();
    }

    public Map<String, Object> member(long memberId)
    {
        Map<String, Object> row = first(jdbc.queryForList(
                "SELECT member_id, nickname, avatar, phone, vip_level, staff_store_id, status "
                        + "FROM biz_member WHERE member_id = ?", memberId));
        if (row == null || !"0".equals(row.get("status")))
        {
            throw new MiniBusinessException(401, "会员账号无效");
        }
        return toUser(row);
    }

    private Map<String, Object> toUser(Map<String, Object> member)
    {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", String.valueOf(member.get("member_id")));
        user.put("name", member.get("nickname"));
        user.put("avatar", member.get("avatar"));
        user.put("phone", member.get("phone"));
        user.put("vip", ((Number) member.get("vip_level")).intValue() > 0);
        user.put("role", member.get("staff_store_id") == null ? "member" : "staff");
        user.put("groupMember", false);
        return user;
    }

    private static String newToken()
    {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value)
    {
        try
        {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte item : bytes)
            {
                hex.append(Character.forDigit((item >>> 4) & 15, 16));
                hex.append(Character.forDigit(item & 15, 16));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new IllegalStateException("JVM 缺少 SHA-256", exception);
        }
    }

    private static Map<String, Object> first(List<Map<String, Object>> rows)
    {
        return rows.isEmpty() ? null : rows.get(0);
    }
}
