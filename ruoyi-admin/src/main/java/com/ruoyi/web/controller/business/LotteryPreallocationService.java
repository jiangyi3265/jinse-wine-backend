package com.ruoyi.web.controller.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persist the outcome and reserve stock in the same transaction as code creation. */
@Service
public class LotteryPreallocationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public LotteryPreallocationService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional
    public Map<String, Object> generate(String activityCode, int count, String prefix, String batchId,
            String linkPrefix, String operator) {
        if (count < 1 || count > 100) throw new MiniBusinessException(400, "生成数量必须为1至100的整数");
        if (batchId == null || !batchId.matches("[A-Za-z0-9_-]{16,64}"))
            throw new MiniBusinessException(400, "生成请求号无效，请刷新页面");
        if (linkPrefix.isEmpty()) throw new MiniBusinessException(409, "抽奖链接尚未配置");
        Map<String, Object> activity = first("SELECT *, JSON_UNQUOTE(JSON_EXTRACT(rule_json,'$.rewardValidityDays')) AS validity_days "
                + "FROM biz_lottery_activity WHERE activity_code=? FOR UPDATE", activityCode);
        if (activity == null) throw new MiniBusinessException(404, "活动不存在");
        Map<String, Object> existing = first("SELECT * FROM biz_lottery_batch WHERE batch_id=?", batchId);
        if (existing != null) {
            if (n(existing.get("activity_id")) != n(activity.get("activity_id")) || n(existing.get("code_count")) != count
                    || !prefix.equals(existing.get("code_prefix")))
                throw new MiniBusinessException(409, "请求号已用于其他生成参数");
            return batch(batchId, linkPrefix);
        }
        long now = System.currentTimeMillis();
        Timestamp activityEnd = timestamp(activity.get("end_time"));
        if (!"0".equals(activity.get("status")) || (activityEnd != null && activityEnd.getTime() <= now))
            throw new MiniBusinessException(409, "活动已停用或结束");
        long outstanding = jdbc.queryForObject("SELECT COUNT(*) FROM biz_lottery_code_outcome o JOIN biz_lottery_code c "
                + "ON c.code_id=o.code_id WHERE c.activity_id=? AND o.status='reserved' "
                + "AND (o.expires_at IS NULL OR o.expires_at>NOW()) FOR UPDATE", Long.class, activity.get("activity_id"));
        long limit = n(activity.get("total_limit"));
        if (limit >= 0 && n(activity.get("used_count")) + outstanding + count > limit)
            throw new MiniBusinessException(409, "生成数量超过活动剩余名额（含已生成未领取的码）");
        List<Map<String, Object>> prizes = jdbc.queryForList("SELECT * FROM biz_lottery_prize WHERE activity_id=? "
                + "AND status='0' ORDER BY prize_id FOR UPDATE", activity.get("activity_id"));
        long totalProbability = prizes.stream().mapToLong(p -> n(p.get("probability_bp"))).sum();
        if (totalProbability > 10000 || prizes.stream().anyMatch(p -> n(p.get("probability_bp")) < 0))
            throw new MiniBusinessException(409, "奖项概率配置无效");
        jdbc.update("INSERT INTO biz_lottery_batch(batch_id,activity_id,code_count,code_prefix,create_by) VALUES(?,?,?,?,?)",
                batchId, activity.get("activity_id"), count, prefix, operator);
        int wins = 0;
        for (int i = 0; i < count; i++) {
            int bucket = RANDOM.nextInt(10000);
            long cumulative = 0;
            Map<String, Object> selected = null;
            for (Map<String, Object> prize : prizes) {
                // An exhausted prize retains its probability interval; it never inflates another prize's chance.
                cumulative += n(prize.get("probability_bp"));
                if (bucket < cumulative) { selected = prize; break; }
            }
            if (selected != null && (n(selected.get("stock")) == 0 || "none".equals(selected.get("prize_type")))) selected = null;
            Map<String, Object> coupon = null;
            if (selected != null && "coupon".equals(selected.get("prize_type"))) {
                coupon = first("SELECT * FROM biz_coupon_template WHERE coupon_template_id=? FOR UPDATE", selected.get("coupon_template_id"));
                Timestamp start = coupon == null ? null : timestamp(coupon.get("start_time"));
                Timestamp end = coupon == null ? null : timestamp(coupon.get("end_time"));
                if (coupon == null || !"0".equals(coupon.get("status")) || n(coupon.get("stock")) == 0
                        || (start != null && start.getTime() > now) || (end != null && end.getTime() <= now)) selected = null;
            }
            Integer fixedAmount = null;
            if (selected != null && "cash".equals(selected.get("prize_type"))) {
                fixedAmount = reserveFixedAmount(selected);
                if (fixedAmount != null && fixedAmount < 1) selected = null;
            }
            Map<String, Object> ticket = null;
            if (selected != null && "ticket".equals(selected.get("prize_type"))) {
                ticket = new ExternalTicketService(jdbc).pick(selected.get("prize_id"));
                if (ticket == null) selected = null;
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("prize_type", "none");
            snapshot.put("prize_name", "谢谢参与");
            snapshot.put("reward_validity_days", n(activity.get("validity_days")));
            boolean prizeReserved = false, couponReserved = false;
            Timestamp expires = activityEnd;
            if (selected != null) {
                wins++;
                for (String key : Arrays.asList("prize_id", "prize_type", "prize_name", "points", "image_url", "redeem_store_id", "coupon_template_id"))
                    snapshot.put(key, selected.get(key));
                if (ticket != null) {
                    snapshot.put("ticket_id", ticket.get("ticket_id"));
                    Timestamp ticketEnd = timestamp(ticket.get("valid_until"));
                    if (expires == null || ticketEnd.before(expires)) expires = ticketEnd;
                }
                if ("cash".equals(selected.get("prize_type"))) {
                    if (fixedAmount == null) {
                        long min = n(selected.get("amount_min_cent")), max = n(selected.get("amount_max_cent"));
                        if (min < 1 || max < min || max > Integer.MAX_VALUE) throw new MiniBusinessException(409, "红包金额配置无效，最低为0.01元");
                        fixedAmount = (int) (min + RANDOM.nextInt((int) (max - min + 1)));
                    }
                    snapshot.put("amount_cent", fixedAmount);
                }
                if ("points".equals(selected.get("prize_type")) && n(selected.get("points")) <= 0)
                    throw new MiniBusinessException(409, "积分奖项配置无效");
                prizeReserved = ticket == null && n(selected.get("stock")) > 0;
                if (prizeReserved) {
                    jdbc.update("UPDATE biz_lottery_prize SET stock=stock-1 WHERE prize_id=?", selected.get("prize_id"));
                    selected.put("stock", n(selected.get("stock")) - 1);
                }
                if ("coupon".equals(selected.get("prize_type"))) {
                    Map<String, Object> rules = new LinkedHashMap<>();
                    for (String key : Arrays.asList("coupon_type", "amount_cent", "discount_rate", "min_amount_cent", "min_quantity", "max_discount_cent"))
                        rules.put(key, coupon.get(key));
                    snapshot.put("coupon_rules", rules);
                    Timestamp end = timestamp(coupon.get("end_time"));
                    snapshot.put("coupon_expires_at", end == null ? null : end.toString());
                    if (end != null && (expires == null || end.before(expires))) expires = end;
                    couponReserved = n(coupon.get("stock")) > 0;
                    if (couponReserved) jdbc.update("UPDATE biz_coupon_template SET stock=stock-1 WHERE coupon_template_id=?", selected.get("coupon_template_id"));
                }
            }
            // Code contents never reveal the preallocated outcome.
            String code = prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 28);
            jdbc.update("INSERT INTO biz_lottery_code(activity_id,draw_code,status,qr_url,mini_path) VALUES(?,?,'unused','',?)",
                    activity.get("activity_id"), code, "pages/scan/index?code=" + code);
            Long codeId = jdbc.queryForObject("SELECT code_id FROM biz_lottery_code WHERE draw_code=?", Long.class, code);
            if (ticket != null) new ExternalTicketService(jdbc).reserve(ticket.get("ticket_id"), codeId);
            jdbc.update("INSERT INTO biz_lottery_code_outcome(code_id,batch_id,prize_id,coupon_template_id,redeem_store_id,result_type,result_snapshot,"
                    + "prize_reserved,coupon_reserved,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?)", codeId, batchId,
                    snapshot.get("prize_id"), snapshot.get("coupon_template_id"), snapshot.get("redeem_store_id"), snapshot.get("prize_type"),
                    encode(snapshot), prizeReserved, couponReserved, expires);
        }
        jdbc.update("UPDATE biz_lottery_batch SET win_count=? WHERE batch_id=?", wins, batchId);
        return batch(batchId, linkPrefix);
    }

    public Map<String, Object> batches(String activityCode, int pageNum, int pageSize) {
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.max(1, Math.min(100, pageSize));
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM biz_lottery_batch b JOIN biz_lottery_activity a "
                + "ON a.activity_id=b.activity_id WHERE a.activity_code=?", Long.class, activityCode);
        List<Map<String, Object>> batches = jdbc.queryForList("SELECT b.batch_id AS batchId,b.code_count AS codeCount,b.win_count AS winCount,"
                + "b.create_time AS createTime,b.create_by AS createBy FROM biz_lottery_batch b JOIN biz_lottery_activity a "
                + "ON a.activity_id=b.activity_id WHERE a.activity_code=? ORDER BY b.create_time DESC,b.batch_id DESC LIMIT ? OFFSET ?",
                activityCode, safeSize, (long) (safePage - 1) * safeSize);
        Map<String, Object> page = new HashMap<>();
        page.put("rows", batches);
        page.put("total", total);
        if (batches.isEmpty()) return page;

        String placeholders = String.join(",", Collections.nCopies(batches.size(), "?"));
        Object[] batchIds = batches.stream().map(batch -> batch.get("batchId")).toArray();
        List<Map<String, Object>> prizeCounts = jdbc.queryForList("SELECT o.batch_id AS batchId,o.result_type AS resultType,"
                + "COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(o.result_snapshot,'$.prize_name')),''),'谢谢参与') AS prizeName,"
                + "COUNT(*) AS codeCount FROM biz_lottery_code_outcome o WHERE o.batch_id IN (" + placeholders + ") "
                + "GROUP BY o.batch_id,o.result_type,COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(o.result_snapshot,'$.prize_name')),''),'谢谢参与') "
                + "ORDER BY o.batch_id,CASE WHEN o.result_type='none' THEN 1 ELSE 0 END,prizeName", batchIds);
        Map<Object, List<Map<String, Object>>> byBatch = new HashMap<>();
        for (Map<String, Object> prizeCount : prizeCounts)
            byBatch.computeIfAbsent(prizeCount.get("batchId"), key -> new ArrayList<>()).add(prizeCount);
        for (Map<String, Object> batch : batches)
            batch.put("prizeCounts", byBatch.getOrDefault(batch.get("batchId"), Collections.emptyList()));
        return page;
    }

    public Map<String, Object> batch(String batchId, String linkPrefix) {
        Map<String, Object> batch = first("SELECT b.batch_id AS batchId,b.code_count AS codeCount,b.win_count AS winCount,"
                + "a.activity_code AS activityCode FROM biz_lottery_batch b JOIN biz_lottery_activity a ON a.activity_id=b.activity_id WHERE b.batch_id=?", batchId);
        if (batch == null) throw new MiniBusinessException(404, "生成批次不存在");
        List<Map<String, Object>> codes = jdbc.queryForList("SELECT c.draw_code AS code,c.mini_path AS miniPath,o.result_type AS resultType,"
                + "JSON_UNQUOTE(JSON_EXTRACT(o.result_snapshot,'$.prize_name')) AS prizeName,"
                + "JSON_EXTRACT(o.result_snapshot,'$.amount_cent') AS amountCent,"
                + "JSON_EXTRACT(o.result_snapshot,'$.points') AS points,o.status,o.expires_at AS expiresAt "
                + "FROM biz_lottery_code c JOIN biz_lottery_code_outcome o ON o.code_id=c.code_id WHERE o.batch_id=? ORDER BY c.code_id", batchId);
        codes.forEach(c -> c.put("linkUrl", linkPrefix.isEmpty() ? "" : linkPrefix + c.get("code")));
        batch.put("codes", codes);
        return batch;
    }

    public Map<String, Object> outcome(Object codeId) {
        return first("SELECT *, (expires_at IS NOT NULL AND expires_at<=NOW()) AS expired FROM biz_lottery_code_outcome WHERE code_id=? FOR UPDATE", codeId);
    }

    public Map<String, Object> snapshot(Map<String, Object> outcome) {
        if (!"reserved".equals(outcome.get("status")) || n(outcome.get("expired")) != 0)
            throw new MiniBusinessException(409, "二维码已领取或已过期");
        try { return json.readValue(String.valueOf(outcome.get("result_snapshot")), new TypeReference<Map<String, Object>>() {}); }
        catch (Exception e) { throw new MiniBusinessException(409, "二维码结果数据异常，请联系管理员"); }
    }

    public void claim(Object codeId, Map<String, Object> snapshot) {
        if (jdbc.update("UPDATE biz_lottery_code_outcome SET status='claimed' WHERE code_id=? AND status='reserved'", codeId) != 1)
            throw new MiniBusinessException(409, "二维码已领取");
        if (snapshot.get("prize_id") != null)
            jdbc.update("UPDATE biz_lottery_prize SET issued_count=issued_count+1 WHERE prize_id=?", snapshot.get("prize_id"));
    }

    @Transactional
    public void expire(long codeId) {
        // Activity -> code -> outcome -> prize -> coupon: same order as generation and draw.
        Map<String, Object> code = first("SELECT activity_id FROM biz_lottery_code WHERE code_id=?", codeId);
        if (code == null) return;
        first("SELECT activity_id FROM biz_lottery_activity WHERE activity_id=? FOR UPDATE", code.get("activity_id"));
        first("SELECT code_id FROM biz_lottery_code WHERE code_id=? FOR UPDATE", codeId);
        Map<String, Object> outcome = outcome(codeId);
        if (outcome == null || !"reserved".equals(outcome.get("status")) || n(outcome.get("expired")) == 0) return;
        if (n(outcome.get("prize_reserved")) == 1)
            jdbc.update("UPDATE biz_lottery_prize SET stock=stock+1 WHERE prize_id=? AND stock>=0", outcome.get("prize_id"));
        if (n(outcome.get("coupon_reserved")) == 1)
            jdbc.update("UPDATE biz_coupon_template SET stock=stock+1 WHERE coupon_template_id=? AND stock>=0", outcome.get("coupon_template_id"));
        if ("ticket".equals(outcome.get("result_type"))) new ExternalTicketService(jdbc).release(codeId);
        jdbc.update("UPDATE biz_lottery_code_outcome SET status='expired' WHERE code_id=?", codeId);
        jdbc.update("UPDATE biz_lottery_code SET status='disabled' WHERE code_id=? AND status='unused'", codeId);
    }

    public List<Long> expiredCodes() {
        return jdbc.queryForList("SELECT code_id FROM biz_lottery_code_outcome WHERE status='reserved' AND expires_at<=NOW() ORDER BY code_id LIMIT 200", Long.class);
    }

    String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new MiniBusinessException(409, "奖项快照保存失败"); }
    }

    // MySQL DATETIME may be mapped to LocalDateTime or Timestamp by the JDBC driver.
    private static Timestamp timestamp(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp) return (Timestamp) value;
        if (value instanceof java.time.LocalDateTime) return Timestamp.valueOf((java.time.LocalDateTime) value);
        if (value instanceof java.util.Date) return new Timestamp(((java.util.Date) value).getTime());
        return Timestamp.valueOf(String.valueOf(value).replace('T', ' '));
    }

    /** Reserves one configured fixed amount while the activity row is locked. */
    private Integer reserveFixedAmount(Map<String, Object> prize)
    {
        String raw = String.valueOf(prize.get("amount_rules_json") == null ? "" : prize.get("amount_rules_json"));
        if (raw.trim().isEmpty()) return null;
        try {
            List<Map<String, Object>> rules = json.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
            int total = rules.stream().mapToInt(rule -> (int) n(rule.get("count"))).sum();
            if (total <= 0) return -1;
            int pick = RANDOM.nextInt(total);
            for (Map<String, Object> rule : rules) {
                int count = (int) n(rule.get("count"));
                if (pick < count) {
                    int amount = (int) n(rule.get("amountCent"));
                    rule.put("count", count - 1);
                    String updatedRules = json.writeValueAsString(rules);
                    jdbc.update("UPDATE biz_lottery_prize SET amount_rules_json=? WHERE prize_id=?", updatedRules, prize.get("prize_id"));
                    prize.put("amount_rules_json", updatedRules);
                    return amount < 1 ? -1 : amount;
                }
                pick -= count;
            }
            return -1;
        } catch (Exception e) { throw new MiniBusinessException(409, "固定红包金额配置无效"); }
    }

    private static long n(Object value) { return value == null ? 0 : value instanceof Boolean ? ((Boolean) value ? 1 : 0) : Long.parseLong(String.valueOf(value)); }
    private Map<String, Object> first(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
