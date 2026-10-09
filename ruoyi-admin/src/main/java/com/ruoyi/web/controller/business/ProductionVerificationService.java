package com.ruoyi.web.controller.business;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.SecurityUtils;

@Service
public class ProductionVerificationService
{
    private static final Set<String> CHECK_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("mini", "pay", "transfer", "social", "work", "video")));

    private final JdbcTemplate jdbc;

    public ProductionVerificationService(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    public Map<String, Object> matching(String key, String fingerprint)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT config_fingerprint, verified_by, verified_at FROM biz_production_verification "
                + "WHERE check_key=? ORDER BY verification_id DESC LIMIT 1", key);
        if (rows.isEmpty() || !fingerprint.equals(rows.get(0).get("config_fingerprint"))) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("verifiedBy", rows.get(0).get("verified_by"));
        result.put("verifiedAt", rows.get(0).get("verified_at"));
        return result;
    }

    public Map<String, Object> record(String key, String fingerprint, String evidence)
    {
        if (!CHECK_KEYS.contains(key)) throw new MiniBusinessException(400, "不支持的上线验收项目");
        String note = evidence == null ? "" : evidence.trim();
        if (note.length() < 10 || note.length() > 1000)
            throw new MiniBusinessException(400, "请填写10至1000字的平台验收证据");
        jdbc.update("INSERT INTO biz_production_verification(check_key,config_fingerprint,evidence,verified_by) VALUES(?,?,?,?)",
                key, fingerprint, note, SecurityUtils.getUsername());
        return matching(key, fingerprint);
    }

    public static String fingerprint(String key, String... parts)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, key);
            for (String part : parts) update(digest, part == null ? "" : part);
            byte[] hash = digest.digest();
            char[] hex = new char[hash.length * 2];
            for (int i = 0; i < hash.length; i++)
            {
                hex[i * 2] = Character.forDigit((hash[i] >>> 4) & 15, 16);
                hex[i * 2 + 1] = Character.forDigit(hash[i] & 15, 16);
            }
            return new String(hex);
        }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    private static void update(MessageDigest digest, String value)
    {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
