package com.ruoyi.web.controller.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 营销域原子服务：积分、兑换、活动码和门店核销均在数据库事务中完成。 */
@Service
public class MiniMarketingService
{
    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{3,128}$");
    private static final String CASH = "cash";
    private static final String PENDING = "pending";
    private static final String REDEEMED = "redeemed";

    private final JdbcTemplate jdbc;

    @Value("${mini.marketing.cash-enabled:false}")
    private boolean cashEnabled;

    public MiniMarketingService(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    public Map<String, Object> points(long memberId)
    {
        ensureAccount(memberId);
        Map<String, Object> row = first("SELECT balance FROM biz_point_account WHERE member_id = ?", memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", number(row.get("balance")));
        return result;
    }

    public List<Map<String, Object>> pointRecords(long memberId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT point_record_id, title, change_amount, "
                + "create_time FROM biz_point_record WHERE member_id = ? ORDER BY point_record_id DESC LIMIT 200", memberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(row.get("point_record_id")));
            item.put("title", row.get("title"));
            item.put("change", number(row.get("change_amount")));
            item.put("createdAt", timestamp(row.get("create_time")));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> exchange(long memberId, String code, String requestId)
    {
        requireCode(code);
        requireRequestId(requestId);
        Map<String, Object> existing = idempotency(memberId, requestId, "exchange", code);
        if (existing.get("reward_id") != null || existing.get("member_coupon_id") != null)
        {
            return existingResult(memberId, existing);
        }
        Map<String, Object> item = first("SELECT exchange_item_id, exchange_code, item_name, item_type, "
                + "coupon_template_id, spec, image_url, points, stock, per_user_limit FROM biz_exchange_item "
                + "WHERE exchange_code = ? AND status = '0' FOR UPDATE", code);
        if (item == null) throw new MiniBusinessException(404, "兑换项不存在或已下架");
        ensureAccount(memberId);
        Map<String, Object> account = first("SELECT balance FROM biz_point_account WHERE member_id = ? FOR UPDATE", memberId);
        if (number(account.get("balance")) < number(item.get("points"))) throw new MiniBusinessException(409, "积分不足");
        if (number(item.get("stock")) == 0) throw new MiniBusinessException(409, "该奖品已兑完");
        int used = "coupon".equals(item.get("item_type"))
                ? jdbc.queryForObject("SELECT COUNT(*) FROM biz_member_coupon WHERE member_id = ? AND coupon_template_id = ?",
                        Integer.class, memberId, item.get("coupon_template_id"))
                : jdbc.queryForObject("SELECT COUNT(*) FROM biz_reward WHERE member_id = ? AND exchange_code LIKE ?",
                        Integer.class, memberId, "EX-" + code + "-%");
        if (used >= number(item.get("per_user_limit"))) throw new MiniBusinessException(409, "已达到该奖品兑换上限");
        changePoints(memberId, -number(item.get("points")), "exchange", code, "兑换" + item.get("item_name"));
        if (number(item.get("stock")) > 0)
        {
            jdbc.update("UPDATE biz_exchange_item SET stock = stock - 1 WHERE exchange_item_id = ?", item.get("exchange_item_id"));
        }
        long rewardId = 0;
        Long couponId = null;
        if ("coupon".equals(item.get("item_type")))
        {
            if (item.get("coupon_template_id") == null) throw new MiniBusinessException(409, "兑换券配置无效");
            couponId = createCoupon(memberId, number(item.get("coupon_template_id")));
        }
        else
        {
            rewardId = createReward(memberId, "goods", String.valueOf(item.get("item_name")), 0, 0,
                    "EX-" + code + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    String.valueOf(item.get("spec")), String.valueOf(item.get("image_url")));
        }
        jdbc.update("UPDATE biz_mini_idempotency SET reward_id = ?, member_coupon_id = ? WHERE member_id = ? "
                + "AND request_id = ?", rewardId == 0 ? null : rewardId, couponId, memberId, requestId);
        return rewardId == 0 ? coupon(memberId, couponId) : reward(memberId, rewardId);
    }

    public List<Map<String, Object>> rewards(long memberId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT w.reward_id, w.reward_type, w.reward_name, w.amount_cent, "
                + "w.points, w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url "
                + "FROM biz_reward w LEFT JOIN biz_exchange_item e ON w.exchange_code LIKE CONCAT('EX-', e.exchange_code, '-%') "
                + "WHERE w.member_id = ? AND w.reward_type <> 'cash' ORDER BY w.reward_id DESC", memberId);
        return rewardList(rows);
    }

    public List<Map<String, Object>> cashRewards(long memberId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT reward_id, reward_type, reward_name, amount_cent, "
                + "points, exchange_code, image_url, status, expires_at, create_time, NULL AS spec, "
                + "image_url AS display_image_url FROM biz_reward "
                + "WHERE member_id = ? AND reward_type = 'cash' ORDER BY reward_id DESC", memberId);
        return rewardList(rows);
    }

    public Map<String, Object> cashReward(long memberId, String id)
    {
        Map<String, Object> row = rewardRow(memberId, id, CASH);
        if (row == null) throw new MiniBusinessException(404, "红包不存在");
        return toReward(row, "");
    }

    @Transactional
    public Map<String, Object> claimCash(long memberId, String id, String requestId)
    {
        requireRequestId(requestId);
        if (!cashEnabled) throw new MiniBusinessException(503, "微信商户转账凭据尚未配置，无法发放红包");
        Map<String, Object> row = rewardRow(memberId, id, CASH);
        if (row == null) throw new MiniBusinessException(404, "红包不存在");
        if (!PENDING.equals(row.get("status"))) throw new MiniBusinessException(409, "红包状态已更新");
        // 真正的商户转账回调接入前不允许直接改为 received，避免资金状态与微信不一致。
        throw new MiniBusinessException(503, "微信商户转账服务尚未配置");
    }

    @Transactional
    public Map<String, Object> draw(long memberId, String activityCode, String requestId)
    {
        requireCode(activityCode);
        requireRequestId(requestId);
        Map<String, Object> existing = idempotency(memberId, requestId, "draw", activityCode);
        if (existing.get("reward_id") != null) return reward(memberId, number(existing.get("reward_id")));
        if (existing.get("result_type") != null) return storedDrawResult(memberId, existing);
        // 演示规则可通过活动码稳定复现：5%现金、50%积分、25%代金券、20%实物。
        int bucket = Math.floorMod(hash(activityCode), 100);
        long rewardId;
        Map<String, Object> result = new LinkedHashMap<>();
        if (bucket < 5)
        {
            rewardId = createReward(memberId, CASH, "现金红包", 888, 0,
                    "DR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), "", "");
            result = reward(memberId, rewardId);
        }
        else if (bucket < 55)
        {
            int points = 50;
            changePoints(memberId, points, "draw", activityCode, "扫码活动积分");
            result.put("id", "draw-" + requestId);
            result.put("title", "积分奖励");
            result.put("value", points + "积分");
            result.put("kind", "points");
            result.put("createdAt", System.currentTimeMillis());
        }
        else if (bucket < 80)
        {
            long couponId = createCoupon(memberId, couponTemplateId("voucher20"));
            result.put("id", String.valueOf(couponId));
            result.put("title", "20元代金券");
            result.put("value", "¥20");
            result.put("kind", "coupon");
            result.put("createdAt", System.currentTimeMillis());
        }
        else
        {
            rewardId = createReward(memberId, "goods", "品鉴杯", 0, 0,
                    "DR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                    "精美陶瓷品鉴杯", "/mini-assets/design/cup.jpg");
            result = reward(memberId, rewardId);
        }
        Object rewardIdValue = result.get("kind") == null || !(CASH.equals(result.get("kind")) || "goods".equals(result.get("kind")))
                ? null : result.get("id");
        jdbc.update("UPDATE biz_mini_idempotency SET reward_id = ?, member_coupon_id = ?, result_type = ?, result_value = ? "
                + "WHERE member_id = ? AND request_id = ?", rewardIdValue,
                "coupon".equals(result.get("kind")) ? result.get("id") : null,
                result.get("kind"), result.get("value"), memberId, requestId);
        return result;
    }

    public List<Map<String, Object>> redemptions(long verifierMemberId)
    {
        requireStaff(verifierMemberId);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.redemption_id, r.reward_id, r.member_id, "
                + "r.store_id, r.verifier_member_id, r.status, r.request_id, r.create_time, w.reward_name, w.exchange_code "
                + "FROM biz_redemption r JOIN biz_reward w ON w.reward_id = r.reward_id "
                + "WHERE r.verifier_member_id = ? ORDER BY r.redemption_id DESC LIMIT 100", verifierMemberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(row.get("redemption_id")));
            item.put("name", row.get("reward_name"));
            item.put("code", row.get("exchange_code"));
            item.put("requestId", row.get("request_id"));
            item.put("status", row.get("status"));
            item.put("createdAt", timestamp(row.get("create_time")));
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> lookup(long verifierMemberId, String code)
    {
        requireStaff(verifierMemberId);
        Map<String, Object> row = first("SELECT w.reward_id, w.reward_type, w.reward_name, w.amount_cent, w.points, "
                + "w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url "
                + "FROM biz_reward w LEFT JOIN biz_exchange_item e "
                + "ON w.exchange_code LIKE CONCAT('EX-', e.exchange_code, '-%') "
                + "WHERE w.exchange_code = ?", code);
        if (row == null || CASH.equals(row.get("reward_type"))) throw new MiniBusinessException(404, "未找到对应奖品");
        return toReward(row, "");
    }

    @Transactional
    public Map<String, Object> confirm(long verifierMemberId, String code, String requestId)
    {
        requireStaff(verifierMemberId);
        requireCode(code);
        requireRequestId(requestId);
        // 先按核销人和请求号回放已完成结果，避免重复请求因奖品已核销而误报冲突。
        Map<String, Object> existing = first("SELECT w.reward_id, w.reward_type, w.reward_name, w.amount_cent, w.points, "
                + "w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url "
                + "FROM biz_redemption r JOIN biz_reward w ON w.reward_id = r.reward_id "
                + "LEFT JOIN biz_exchange_item e ON w.exchange_code LIKE CONCAT('EX-', e.exchange_code, '-%') "
                + "WHERE r.verifier_member_id = ? AND r.request_id = ?", verifierMemberId, requestId);
        if (existing != null)
        {
            if (!code.equals(existing.get("exchange_code")))
            {
                throw new MiniBusinessException(409, "请求号已用于其他核销");
            }
            Map<String, Object> result = toReward(existing, "");
            result.put("status", REDEEMED);
            return result;
        }
        Map<String, Object> staff = first("SELECT staff_store_id FROM biz_member WHERE member_id = ?", verifierMemberId);
        Map<String, Object> reward = first("SELECT w.reward_id, w.member_id, w.reward_type, w.reward_name, w.amount_cent, w.points, "
                + "w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url FROM biz_reward w "
                + "LEFT JOIN biz_exchange_item e ON w.exchange_code LIKE CONCAT('EX-', e.exchange_code, '-%') "
                + "WHERE w.exchange_code = ? FOR UPDATE", code);
        if (reward == null || CASH.equals(reward.get("reward_type"))) throw new MiniBusinessException(404, "未找到对应奖品");
        if (!PENDING.equals(reward.get("status"))) throw new MiniBusinessException(409, "该奖品已核销，请勿重复操作");
        int inserted = jdbc.update("INSERT IGNORE INTO biz_redemption (reward_id, member_id, store_id, "
                + "verifier_member_id, status, request_id) VALUES (?, ?, ?, ?, 'confirmed', ?)", reward.get("reward_id"),
                reward.get("member_id"), staff.get("staff_store_id"), verifierMemberId, requestId);
        if (inserted == 0) return first("SELECT redemption_id AS id, reward_id AS rewardId, member_id AS memberId, "
                + "status, request_id AS requestId, create_time AS createdAt FROM biz_redemption "
                + "WHERE verifier_member_id = ? AND request_id = ?", verifierMemberId, requestId);
        jdbc.update("UPDATE biz_reward SET status = 'redeemed' WHERE reward_id = ?", reward.get("reward_id"));
        Map<String, Object> result = toReward(reward, "");
        result.put("status", REDEEMED);
        return result;
    }

    public Map<String, Object> promotion(long memberId)
    {
        ensurePromotion(memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> sums = first("SELECT COALESCE(SUM(amount_cent),0) AS total, "
                + "COALESCE(SUM(CASE WHEN status='pending' THEN amount_cent ELSE 0 END),0) AS pending, "
                + "COALESCE(SUM(CASE WHEN status='settled' THEN amount_cent ELSE 0 END),0) AS settled "
                + "FROM biz_promotion_record WHERE member_id = ?", memberId);
        result.put("total", number(sums.get("total")));
        result.put("pending", number(sums.get("pending")));
        result.put("settled", number(sums.get("settled")));
        result.put("teamCount", jdbc.queryForObject("SELECT COUNT(*) FROM biz_promotion_relation WHERE inviter_member_id = ? AND status='0'", Integer.class, memberId));
        result.put("records", promotionRecords(memberId));
        return result;
    }

    public List<Map<String, Object>> team(long memberId)
    {
        ensurePromotion(memberId);
        return jdbc.queryForList("SELECT m.member_id AS id, m.nickname AS name, m.avatar, r.create_time AS createdAt "
                + "FROM biz_promotion_relation r JOIN biz_member m ON m.member_id = r.member_id "
                + "WHERE r.inviter_member_id = ? AND r.status = '0' ORDER BY r.relation_id DESC", memberId);
    }

    public List<Map<String, Object>> ranking(long memberId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT p.member_id AS id, m.nickname AS name, "
                + "COALESCE(SUM(p.amount_cent),0) AS amount FROM biz_promotion_record p JOIN biz_member m "
                + "ON m.member_id = p.member_id GROUP BY p.member_id, m.nickname ORDER BY amount DESC LIMIT 20");
        for (Map<String, Object> row : rows)
        {
            String name = String.valueOf(row.get("name"));
            row.put("initial", name.isEmpty() ? "?" : name.substring(0, 1));
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> promotionCode(long memberId)
    {
        ensurePromotion(memberId);
        Map<String, Object> row = first("SELECT invite_code FROM biz_promotion_relation WHERE member_id = ?", memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", row.get("invite_code"));
        return result;
    }

    private List<Map<String, Object>> promotionRecords(long memberId)
    {
        return jdbc.queryForList("SELECT promotion_record_id AS id, title, amount_cent AS amount, status, "
                + "create_time AS createdAt FROM biz_promotion_record WHERE member_id = ? ORDER BY promotion_record_id DESC LIMIT 100", memberId);
    }

    private void ensurePromotion(long memberId)
    {
        jdbc.update("INSERT IGNORE INTO biz_promotion_relation (member_id, invite_code) VALUES (?, ?)", memberId, "JS" + memberId + "PROMO");
    }

    private void ensureAccount(long memberId)
    {
        jdbc.update("INSERT IGNORE INTO biz_point_account (member_id, balance) VALUES (?, 0)", memberId);
    }

    private void changePoints(long memberId, int change, String source, String sourceId, String title)
    {
        ensureAccount(memberId);
        int updated = jdbc.update("UPDATE biz_point_account SET balance = balance + ? WHERE member_id = ? AND balance + ? >= 0",
                change, memberId, change);
        if (updated != 1) throw new MiniBusinessException(409, "积分不足");
        int balance = jdbc.queryForObject("SELECT balance FROM biz_point_account WHERE member_id = ?", Integer.class, memberId);
        jdbc.update("INSERT INTO biz_point_record (member_id, change_amount, balance_after, source_type, source_id, title) "
                + "VALUES (?, ?, ?, ?, ?, ?)", memberId, change, balance, source, sourceId, title);
    }

    private long createReward(long memberId, String type, String name, int amount, int points, String code,
            String spec, String image)
    {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO biz_reward "
                    + "(member_id, reward_type, reward_name, spec, amount_cent, points, exchange_code, image_url) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "reward_id" });
            statement.setLong(1, memberId);
            statement.setString(2, type);
            statement.setString(3, name);
            statement.setString(4, spec == null ? "" : spec);
            statement.setInt(5, amount);
            statement.setInt(6, points);
            statement.setString(7, code);
            statement.setString(8, image);
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    private long createCoupon(long memberId, long templateId)
    {
        Map<String, Object> template = first("SELECT template_code, template_name, coupon_type, amount_cent, discount_rate, "
                + "min_amount_cent, min_quantity, max_discount_cent, end_time, stock FROM biz_coupon_template "
                + "WHERE coupon_template_id = ? AND status = '0' FOR UPDATE", templateId);
        if (template == null) throw new MiniBusinessException(409, "兑换券配置无效");
        if (number(template.get("stock")) == 0) throw new MiniBusinessException(409, "兑换券已领完");
        if (number(template.get("stock")) > 0)
        {
            jdbc.update("UPDATE biz_coupon_template SET stock = stock - 1 WHERE coupon_template_id = ?", templateId);
        }
        String snapshot = "{\"coupon_type\":\"" + template.get("coupon_type") + "\",\"amount_cent\":"
                + number(template.get("amount_cent")) + ",\"discount_rate\":" + number(template.get("discount_rate"))
                + ",\"min_amount_cent\":" + number(template.get("min_amount_cent")) + ",\"min_quantity\":"
                + number(template.get("min_quantity")) + ",\"max_discount_cent\":" + number(template.get("max_discount_cent")) + "}";
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO biz_member_coupon "
                    + "(member_id, coupon_template_id, coupon_code, rule_snapshot, expires_at) VALUES (?, ?, ?, ?, ?)",
                    new String[] { "member_coupon_id" });
            statement.setLong(1, memberId);
            statement.setLong(2, templateId);
            statement.setString(3, "CP" + UUID.randomUUID().toString().replace("-", ""));
            statement.setString(4, snapshot);
            statement.setTimestamp(5, template.get("end_time") == null ? null : Timestamp.valueOf(String.valueOf(template.get("end_time"))));
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    private Map<String, Object> coupon(long memberId, long couponId)
    {
        Map<String, Object> row = first("SELECT c.member_coupon_id AS id, t.template_code AS templateId, t.template_name AS name, "
                + "c.status, c.expires_at AS expiresAt FROM biz_member_coupon c JOIN biz_coupon_template t "
                + "ON t.coupon_template_id = c.coupon_template_id WHERE c.member_id = ? AND c.member_coupon_id = ?", memberId, couponId);
        if (row == null) throw new MiniBusinessException(404, "优惠券不存在");
        row.put("id", String.valueOf(row.get("id")));
        return row;
    }

    private Map<String, Object> reward(long memberId, long rewardId)
    {
        Map<String, Object> row = first("SELECT w.reward_id, w.reward_type, w.reward_name, w.amount_cent, w.points, w.exchange_code, "
                + "w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url FROM biz_reward w "
                + "LEFT JOIN biz_exchange_item e ON w.exchange_code LIKE CONCAT('EX-', e.exchange_code, '-%') "
                + "WHERE w.member_id = ? AND w.reward_id = ?", memberId, rewardId);
        if (row == null) throw new MiniBusinessException(404, "奖品不存在");
        return toReward(row, "");
    }

    private List<Map<String, Object>> rewardList(List<Map<String, Object>> rows)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) result.add(toReward(row, ""));
        return result;
    }

    private Map<String, Object> toReward(Map<String, Object> row, String image)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(row.get("reward_id")));
        result.put("name", row.get("reward_name"));
        String spec = row.get("spec") == null ? "" : String.valueOf(row.get("spec"));
        result.put("spec", CASH.equals(row.get("reward_type")) ? "微信现金红包" : (spec.isEmpty() ? "请到指定门店兑换" : spec));
        Object displayImage = row.get("display_image_url");
        String imageUrl = displayImage != null && !String.valueOf(displayImage).isEmpty()
                ? String.valueOf(displayImage)
                : (row.get("image_url") == null ? image : String.valueOf(row.get("image_url")));
        result.put("image", imageUrl);
        result.put("kind", row.get("reward_type"));
        result.put("status", row.get("status"));
        result.put("code", row.get("exchange_code"));
        result.put("amount", row.get("amount_cent"));
        result.put("points", row.get("points"));
        result.put("createdAt", timestamp(row.get("create_time")));
        if (row.get("expires_at") != null) result.put("expiresAt", timestamp(row.get("expires_at")));
        return result;
    }

    private Map<String, Object> rewardRow(long memberId, String id, String type)
    {
        try
        {
            long rewardId = Long.parseLong(id);
            return first("SELECT reward_id, reward_type, reward_name, spec, amount_cent, points, exchange_code, image_url, status, expires_at, create_time "
                    + "FROM biz_reward WHERE member_id = ? AND reward_id = ? AND reward_type = ?", memberId, rewardId, type);
        }
        catch (Exception ignored)
        {
            throw new MiniBusinessException(400, "奖品 ID 无效");
        }
    }

    private Map<String, Object> idempotency(long memberId, String requestId, String action, String payload)
    {
        String hash = MiniMemberAuthService.sha256(action + "|" + payload);
        jdbc.update("INSERT IGNORE INTO biz_mini_idempotency (member_id, request_id, action_type, payload_hash) VALUES (?, ?, ?, ?)",
                memberId, requestId, action, hash);
        Map<String, Object> row = first("SELECT request_id, action_type, payload_hash, reward_id, member_coupon_id, result_type, result_value FROM biz_mini_idempotency "
                + "WHERE member_id = ? AND request_id = ? FOR UPDATE", memberId, requestId);
        if (!action.equals(row.get("action_type")) || !hash.equals(row.get("payload_hash")))
            throw new MiniBusinessException(409, "请求号已用于其他业务");
        return row;
    }

    private Map<String, Object> storedDrawResult(long memberId, Map<String, Object> row)
    {
        String type = String.valueOf(row.get("result_type"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", "coupon".equals(type) ? String.valueOf(row.get("member_coupon_id")) : "draw-" + row.get("request_id"));
        result.put("kind", type);
        result.put("createdAt", System.currentTimeMillis());
        if ("points".equals(type))
        {
            result.put("title", "积分奖励");
            String value = String.valueOf(row.get("result_value"));
            result.put("value", value.endsWith("积分") ? value : value + "积分");
        }
        else if ("coupon".equals(type))
        {
            result.put("title", "20元代金券");
            result.put("value", "¥20");
            result.put("rewardId", row.get("member_coupon_id"));
        }
        else if (row.get("reward_id") != null)
        {
            return reward(memberId, number(row.get("reward_id")));
        }
        return result;
    }

    private Map<String, Object> existingResult(long memberId, Map<String, Object> row)
    {
        if (row.get("reward_id") != null) return reward(memberId, number(row.get("reward_id")));
        return coupon(memberId, number(row.get("member_coupon_id")));
    }

    private long couponTemplateId(String code)
    {
        Map<String, Object> row = first("SELECT coupon_template_id FROM biz_coupon_template WHERE template_code = ? AND status = '0'", code);
        if (row == null) throw new MiniBusinessException(409, "活动券尚未配置");
        return number(row.get("coupon_template_id"));
    }

    private void requireStaff(long memberId)
    {
        Map<String, Object> row = first("SELECT staff_store_id FROM biz_member WHERE member_id = ? AND status = '0'", memberId);
        if (row == null || row.get("staff_store_id") == null) throw new MiniBusinessException(403, "当前账号没有核销权限");
    }

    private static void requireRequestId(String value)
    {
        if (value == null || !REQUEST_ID.matcher(value).matches()) throw new MiniBusinessException(400, "请求号无效");
    }

    private static void requireCode(String value)
    {
        if (value == null || !CODE.matcher(value).matches()) throw new MiniBusinessException(400, "活动码或兑换码无效");
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static int number(Object value)
    {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static long timestamp(Object value)
    {
        if (value instanceof java.util.Date) return ((java.util.Date) value).getTime();
        if (value instanceof LocalDateTime)
            return ((LocalDateTime) value).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (value instanceof Instant) return ((Instant) value).toEpochMilli();
        return Instant.now().toEpochMilli();
    }

    private static int hash(String value)
    {
        try
        {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return (bytes[0] & 0xff) * 256 + (bytes[1] & 0xff);
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new IllegalStateException("JVM 缺少 SHA-256", exception);
        }
    }
}
