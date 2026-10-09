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
import java.util.Locale;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final MiniBusinessExtensionService businessExtension;
    private final ObjectMapper objectMapper;

    @Value("${mini.wechat.app-id:}")
    private String appId;

    @Value("${mini.wechat.app-secret:}")
    private String appSecret;

    @Value("${mini.wechat.local-login-enabled:false}")
    private boolean localLoginEnabled;

    @Value("${app.environment:production}")
    private String appEnvironment;

    @Value("${mini.wechat.local-login-code:}")
    private String localLoginCode;

    @Value("${mini.wechat.local-openid:local-h5-member}")
    private String localOpenid;

    /** Optional second identity used only by the local H5 verifier workflow. */
    @Value("${mini.wechat.local-staff-login-code:}")
    private String localStaffLoginCode;

    @Value("${mini.wechat.local-staff-openid:local-h5-staff}")
    private String localStaffOpenid;

    @Value("${mini.wechat.local-staff-store-id:1}")
    private long localStaffStoreId;

    public MiniMemberAuthService(JdbcTemplate jdbc, MiniBusinessExtensionService businessExtension, ObjectMapper objectMapper)
    {
        this.jdbc = jdbc;
        this.businessExtension = businessExtension;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> login(String code)
    {
        return login(code, null);
    }

    @Transactional
    public Map<String, Object> login(String code, String inviteCode)
    {
        if (localLoginAllowed() && !blank(localStaffLoginCode) && localStaffLoginCode.equals(code == null ? "" : code.trim()))
        {
            ensureLocalStaff();
            return issueSession(localStaffOpenid, "", inviteCode);
        }
        if (localLoginAllowed() && !blank(localLoginCode) && localLoginCode.equals(code == null ? "" : code.trim()))
        {
            return issueSession(localOpenid, "", inviteCode);
        }
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
            // 微信接口可能返回 Content-Type: text/plain，即使正文是 JSON。读取为字符串后再解析，
            // 避免 RestTemplate 因消息头不是 application/json 而抛 UnknownContentTypeException。
            String responseBody = wechat.getForObject(WECHAT_SESSION_URL, String.class, appId, appSecret, code.trim());
            response = objectMapper.readTree(responseBody == null ? "" : responseBody);
        }
        catch (RestClientException | JsonProcessingException exception)
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
        return issueSession(openid, unionId, inviteCode);
    }

    /** Creates the member session after either a real WeChat exchange or an explicitly enabled local exchange. */
    private Map<String, Object> issueSession(String openid, String unionId, String inviteCode)
    {
        if (blank(openid) || openid.length() > 128)
        {
            throw new MiniBusinessException(401, "微信身份无效");
        }
        jdbc.update("INSERT IGNORE INTO biz_member (wechat_openid, union_id) VALUES (?, ?)", openid,
                unionId.isEmpty() ? null : unionId);
        Map<String, Object> member = first(jdbc.queryForList(
                "SELECT member_id, member_no, nickname, avatar, phone, vip_level, staff_store_id, status FROM biz_member WHERE wechat_openid = ?",
                openid));
        if (member == null || !"0".equals(member.get("status")))
        {
            throw new MiniBusinessException(403, "会员账号已停用");
        }
        long memberId = ((Number) member.get("member_id")).longValue();
        member.put("member_no", ensureMemberNumber(memberId));
        jdbc.update("INSERT IGNORE INTO biz_promotion_relation(member_id, invite_code, status) VALUES(?,?, '0')",
                memberId, "JS" + memberId + "PROMO");
        if (inviteCode != null && inviteCode.matches("[A-Za-z0-9_-]{3,128}"))
        {
            Map<String, Object> inviter = first(jdbc.queryForList("SELECT member_id FROM biz_promotion_relation WHERE invite_code=? AND status='0'", inviteCode));
            if (inviter != null && ((Number) inviter.get("member_id")).longValue() != memberId)
            {
                Map<String, Object> relation = first(jdbc.queryForList(
                        "SELECT inviter_member_id FROM biz_promotion_relation WHERE member_id=? FOR UPDATE", memberId));
                if (relation != null && relation.get("inviter_member_id") == null)
                    businessExtension.bind(memberId, inviteCode);
            }
        }
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

    private static boolean blank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private boolean localLoginAllowed()
    {
        return localLoginEnabled && "development".equalsIgnoreCase(appEnvironment == null ? "" : appEnvironment.trim());
    }

    /** Creates the local-only verifier fixture on a fresh database without affecting real WeChat logins. */
    private void ensureLocalStaff()
    {
        if (localStaffStoreId <= 0 || first(jdbc.queryForList(
                "SELECT store_id FROM biz_store WHERE store_id = ? AND status = '0'", localStaffStoreId)) == null)
        {
            throw new MiniBusinessException(503, "本地演示核销门店不存在或未启用");
        }
        jdbc.update("INSERT IGNORE INTO biz_member (wechat_openid, nickname, avatar, phone, vip_level, staff_store_id, status) VALUES (?, '演示核销员', '', '', 0, ?, '0')",
                localStaffOpenid, localStaffStoreId);
        jdbc.update("UPDATE biz_member SET staff_store_id = ? WHERE wechat_openid = ? AND status = '0'",
                localStaffStoreId, localStaffOpenid);
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
                "SELECT m.member_id, m.member_no, m.nickname, m.avatar, m.phone, m.vip_level, m.staff_store_id, m.status, "
                        + "st.store_name AS staff_store_name, st.address AS staff_store_address "
                        + "FROM biz_member m LEFT JOIN biz_store st ON st.store_id = m.staff_store_id "
                        + "WHERE m.member_id = ?", memberId));
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
        user.put("memberNo", member.get("member_no"));
        String nickname = member.get("nickname") == null ? "" : String.valueOf(member.get("nickname"));
        user.put("name", blank(nickname) ? "微信用户" : nickname);
        user.put("avatar", member.get("avatar"));
        user.put("phone", member.get("phone"));
        user.put("vip", ((Number) member.get("vip_level")).intValue() > 0);
        user.put("role", member.get("staff_store_id") == null ? "member" : "staff");
        user.put("staffStoreId", member.get("staff_store_id"));
        user.put("staffStoreName", member.get("staff_store_name"));
        user.put("staffStoreAddress", member.get("staff_store_address"));
        user.put("groupMember", false);
        return user;
    }

    private String ensureMemberNumber(long memberId)
    {
        for (int attempt = 0; attempt < 5; attempt++)
        {
            String number = "HY" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT);
            try
            {
                jdbc.update("UPDATE biz_member SET member_no=?,update_time=update_time WHERE member_id=? AND (member_no IS NULL OR member_no='')", number, memberId);
                // A locking read sees the winner when concurrent logins assigned the same member.
                return jdbc.queryForObject("SELECT member_no FROM biz_member WHERE member_id=? FOR UPDATE", String.class, memberId);
            }
            catch (DuplicateKeyException collision) { /* Generate another public number. */ }
        }
        throw new MiniBusinessException(503, "会员编号生成失败，请重试登录");
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
