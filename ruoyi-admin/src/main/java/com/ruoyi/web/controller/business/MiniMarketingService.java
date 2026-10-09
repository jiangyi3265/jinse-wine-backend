package com.ruoyi.web.controller.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
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
    private static final String EXCHANGE_REWARD_JOIN = "LEFT JOIN biz_exchange_item e ON "
            + "w.exchange_code = CONCAT('EX-', e.exchange_code, '-', RIGHT(w.exchange_code, 12)) ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final LotteryPreallocationService lotteryPreallocation;
    private final PointExpiryService pointExpiry;
    private final WechatMiniCodeService miniCode;
    private final WechatPayService wechatPay;

    public MiniMarketingService(JdbcTemplate jdbc, PointExpiryService pointExpiry, WechatMiniCodeService miniCode,
            WechatPayService wechatPay) {
        this(jdbc, pointExpiry, miniCode, wechatPay,
                new LotteryPreallocationService(jdbc, new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MiniMarketingService(JdbcTemplate jdbc, PointExpiryService pointExpiry, WechatMiniCodeService miniCode,
            WechatPayService wechatPay, LotteryPreallocationService lotteryPreallocation)
    {
        this.jdbc = jdbc;
        this.lotteryPreallocation = lotteryPreallocation;
        this.pointExpiry = pointExpiry;
        this.miniCode = miniCode;
        this.wechatPay = wechatPay;
    }

    public Map<String, Object> points(long memberId)
    {
        pointExpiry.expireMember(memberId);
        ensureAccount(memberId);
        Map<String, Object> row = first("SELECT balance FROM biz_point_account WHERE member_id = ?", memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", number(row.get("balance")));
        return result;
    }

    public List<Map<String, Object>> pointRecords(long memberId)
    {
        return pointRecords(memberId, MiniListPage.all());
    }

    @Transactional
    public Object pointRecords(long memberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        return page.result(pointRecords(memberId, page), jdbc,
                "SELECT COUNT(*) FROM biz_point_record WHERE member_id=?", memberId);
    }

    private List<Map<String, Object>> pointRecords(long memberId, MiniListPage page)
    {
        pointExpiry.expireMember(memberId);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT point_record_id, title, change_amount, "
                + "create_time FROM biz_point_record WHERE member_id = ? ORDER BY point_record_id DESC" + page.suffix(), memberId);
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
        pointExpiry.expireMember(memberId);
        requireCode(code);
        requireRequestId(requestId);
        Map<String, Object> existing = idempotency(memberId, requestId, "exchange", code);
        if (existing.get("reward_id") != null || existing.get("member_coupon_id") != null)
        {
            return exchangeResult(memberId, existingResult(memberId, existing));
        }
        Map<String, Object> item = first("SELECT exchange_item_id, exchange_code, item_name, item_type, "
                + "coupon_template_id, spec, image_url, redeem_store_id, points, stock, per_user_limit FROM biz_exchange_item "
                + "WHERE exchange_code = ? AND status = '0' FOR UPDATE", code);
        if (item == null) throw new MiniBusinessException(404, "兑换项不存在或已下架");
        ensureAccount(memberId);
        Map<String, Object> account = first("SELECT balance FROM biz_point_account WHERE member_id = ? FOR UPDATE", memberId);
        if (number(account.get("balance")) < number(item.get("points"))) throw new MiniBusinessException(409, "积分不足");
        if (number(item.get("stock")) == 0) throw new MiniBusinessException(409, "该奖品已兑完");
        int used = ExchangeLimit.completed(jdbc, memberId, code);
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
                    String.valueOf(item.get("spec")), String.valueOf(item.get("image_url")), item.get("redeem_store_id"));
        }
        jdbc.update("UPDATE biz_mini_idempotency SET reward_id = ?, member_coupon_id = ? WHERE member_id = ? "
                + "AND request_id = ?", rewardId == 0 ? null : rewardId, couponId, memberId, requestId);
        return exchangeResult(memberId, rewardId == 0 ? coupon(memberId, couponId) : reward(memberId, rewardId));
    }

    public List<Map<String, Object>> rewards(long memberId)
    {
        return rewards(memberId, MiniListPage.all(), "");
    }

    @Transactional
    public Object rewards(long memberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        String status = MiniListPage.status(params, "pending", "redeemed", "closed");
        String filter = status.isEmpty() ? "" : " AND w.status='" + status + "'";
        return page.result(rewards(memberId, page, filter), jdbc,
                "SELECT COUNT(*) FROM biz_reward w WHERE w.member_id=? AND w.reward_type<>'cash'" + filter, memberId);
    }

    private List<Map<String, Object>> rewards(long memberId, MiniListPage page, String filter)
    {
        closeExpiredRewards(memberId);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT w.reward_id, w.reward_type, w.reward_name, w.amount_cent, "
                + "w.points, w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, w.redeem_store_id, s.store_name AS redeem_store_name, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url "
                + "FROM biz_reward w " + EXCHANGE_REWARD_JOIN
                + "LEFT JOIN biz_store s ON s.store_id = w.redeem_store_id "
                + "WHERE w.member_id = ? AND w.reward_type <> 'cash'" + filter + " ORDER BY w.reward_id DESC" + page.suffix(), memberId);
        return rewardList(rows);
    }

    public Map<String, Object> rewardRedeemCode(long memberId, String id)
    {
        Map<String, Object> row = rewardRow(memberId, id, "goods");
        if (row == null) throw new MiniBusinessException(404, "实物奖品不存在");
        ensureRewardNotExpired(row);
        if (!PENDING.equals(row.get("status"))) throw new MiniBusinessException(409, "该奖品已完成核销");
        String code = String.valueOf(row.get("exchange_code"));
        if (blank(code)) throw new MiniBusinessException(409, "奖品兑换码尚未生成");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rewardId", String.valueOf(row.get("reward_id")));
        result.put("qrCode", miniCode.redemptionCode(number(row.get("reward_id")), code));
        result.put("qrCodeStatus", "ready");
        return result;
    }

    public List<Map<String, Object>> cashRewards(long memberId)
    {
        return cashRewards(memberId, MiniListPage.all());
    }

    @Transactional
    public Object cashRewards(long memberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        return page.result(cashRewards(memberId, page), jdbc,
                "SELECT COUNT(*) FROM biz_reward WHERE member_id=? AND reward_type='cash'", memberId);
    }

    private List<Map<String, Object>> cashRewards(long memberId, MiniListPage page)
    {
        closeExpiredRewards(memberId);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT reward_id, reward_type, reward_name, amount_cent, "
                + "points, exchange_code, image_url, status, expires_at, create_time, NULL AS spec, "
                + "image_url AS display_image_url FROM biz_reward "
                + "WHERE member_id = ? AND reward_type = 'cash' ORDER BY reward_id DESC" + page.suffix(), memberId);
        return rewardList(rows);
    }

    @Transactional
    public Map<String, Object> cashReward(long memberId, String id)
    {
        Map<String, Object> row = rewardRow(memberId, id, CASH);
        if (row == null) throw new MiniBusinessException(404, "红包不存在");
        refreshCashTransfer(number(row.get("reward_id")));
        restoreClosedCashReward(number(row.get("reward_id")));
        row = rewardRow(memberId, id, CASH);
        ensureRewardNotExpired(row);
        return toReward(row, "");
    }

    @Transactional
    public Map<String, Object> claimCash(long memberId, String id, String requestId)
    {
        requireRequestId(requestId);
        long rewardId;
        try { rewardId = Long.parseLong(id); }
        catch (Exception e) { throw new MiniBusinessException(400, "红包 ID 无效"); }
        Map<String, Object> row = first("SELECT w.reward_id,w.member_id,w.amount_cent,w.status,m.wechat_openid FROM biz_reward w "
                + "JOIN biz_member m ON m.member_id=w.member_id WHERE w.reward_id=? AND w.member_id=? AND w.reward_type='cash' FOR UPDATE",
                rewardId, memberId);
        if (row == null) throw new MiniBusinessException(404, "红包不存在");
        // 过期红包不能再创建新的商户转账尝试；已到账红包仍允许查看结果。
        Map<String, Object> expiry = first("SELECT expires_at FROM biz_reward WHERE reward_id=?", rewardId);
        if (expiry != null) { row.put("expires_at", expiry.get("expires_at")); ensureRewardNotExpired(row); }
        if ("received".equals(row.get("status"))) return cashTransferResult(latestCashTransfer(rewardId));
        if (row.get("wechat_openid") == null || blank(String.valueOf(row.get("wechat_openid"))))
            throw new MiniBusinessException(409, "会员缺少微信身份，无法发放红包");

        Map<String, Object> transfer = first("SELECT * FROM biz_reward_transfer WHERE member_id=? AND request_id=?", memberId, requestId);
        if (transfer != null && number(transfer.get("reward_id")) != rewardId)
            throw new MiniBusinessException(409, "请求号已用于其他红包发放");
        boolean created = false;
        if (transfer == null)
        {
            transfer = first("SELECT * FROM biz_reward_transfer WHERE reward_id=? AND status IN "
                    + "('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN') ORDER BY transfer_id DESC LIMIT 1 FOR UPDATE", rewardId);
        }
        if (transfer != null && ("SUBMITTING".equals(transfer.get("status")) || "ACCEPTED".equals(transfer.get("status"))
                || "PROCESSING".equals(transfer.get("status")) || "WAIT_USER_CONFIRM".equals(transfer.get("status"))
                || "TRANSFERING".equals(transfer.get("status")) || "CANCELING".equals(transfer.get("status"))
                || "UNKNOWN".equals(transfer.get("status"))))
        {
            restoreClosedCashReward(rewardId);
            row.put("status", "processing");
        }
        if (transfer == null && "closed".equals(row.get("status")))
            throw new MiniBusinessException(409, "红包已关闭，不能发起新的转账");
        if (transfer == null)
        {
            if (!wechatPay.isTransferConfigured())
                throw new MiniBusinessException(503, "微信商户转账能力或凭据尚未配置，红包保持待发放");
            long attempts = scalar("SELECT COUNT(*) FROM biz_reward_transfer WHERE reward_id=?", rewardId);
            if (attempts >= Integer.MAX_VALUE) throw new MiniBusinessException(409, "红包发放尝试次数已达上限");
            int attempt = (int) attempts + 1;
            String outBillNo = cashBillNo(rewardId, attempt);
            jdbc.update("INSERT INTO biz_reward_transfer(reward_id,member_id,request_id,out_bill_no,amount_cent,openid,status) "
                            + "VALUES(?,?,?,?,?,?,'SUBMITTING')",
                    rewardId, memberId, requestId, outBillNo, row.get("amount_cent"), row.get("wechat_openid"));
            transfer = first("SELECT * FROM biz_reward_transfer WHERE out_bill_no=?", outBillNo);
            jdbc.update("UPDATE biz_reward SET status='processing' WHERE reward_id=? AND status IN ('pending','failed')", rewardId);
            created = true;
        }

        String stateValue = String.valueOf(transfer.get("status"));
        if ("SUCCESS".equals(stateValue) || "FAIL".equals(stateValue) || "CANCELLED".equals(stateValue))
        {
            if ("SUCCESS".equals(stateValue))
                jdbc.update("UPDATE biz_reward SET status='received' WHERE reward_id=? AND status IN ('pending','processing','failed','closed')", rewardId);
            return cashTransferResult(transfer);
        }
        Map<String, Object> result;
        try
        {
            if (!created)
            {
                result = wechatPay.queryCashTransfer(String.valueOf(transfer.get("out_bill_no")));
                if (result == null)
                {
                    // Older local bill numbers used hyphens, which WeChat rejects. Only
                    // replace one after the merchant query confirms it does not exist.
                    String oldBillNo = String.valueOf(transfer.get("out_bill_no"));
                    if (!oldBillNo.matches("[A-Za-z0-9]{1,32}"))
                    {
                        int attempt = (int) scalar("SELECT COUNT(*) FROM biz_reward_transfer WHERE reward_id=? AND transfer_id<=?",
                                rewardId, transfer.get("transfer_id"));
                        String newBillNo = cashBillNo(rewardId, attempt);
                        jdbc.update("UPDATE biz_reward_transfer SET out_bill_no=? WHERE transfer_id=? AND out_bill_no=?",
                                newBillNo, transfer.get("transfer_id"), oldBillNo);
                        transfer.put("out_bill_no", newBillNo);
                    }
                    result = wechatPay.createCashTransfer(String.valueOf(transfer.get("out_bill_no")),
                            number(transfer.get("amount_cent")), String.valueOf(transfer.get("openid")));
                }
            }
            else result = wechatPay.createCashTransfer(String.valueOf(transfer.get("out_bill_no")),
                    number(transfer.get("amount_cent")), String.valueOf(transfer.get("openid")));
            if (!cashTransferMatches(transfer, result)) throw new MiniBusinessException(409, "微信转账查询与本地红包不匹配");
        }
        catch (MiniBusinessException e)
        {
            if (e.getCode() == 503) throw e;
            jdbc.update("UPDATE biz_reward_transfer SET status='UNKNOWN',fail_reason=? WHERE transfer_id=? AND status NOT IN ('SUCCESS','FAIL','CANCELLED')",
                    truncate(e.getMessage(), 255), transfer.get("transfer_id"));
            return cashTransferResult(latestCashTransfer(rewardId));
        }
        saveCashTransferResult(transfer, result);
        return cashTransferResult(latestCashTransfer(rewardId));
    }

    @Transactional
    public Map<String, Object> draw(long memberId, String drawCode, String requestId)
    {
        pointExpiry.expireMember(memberId);
        requireCode(drawCode);
        requireRequestId(requestId);
        Map<String, Object> existing = idempotency(memberId, requestId, "draw", drawCode);
        if (existing.get("reward_id") != null) return drawResult(memberId, reward(memberId, number(existing.get("reward_id"))));
        if (existing.get("result_type") != null) return drawResult(memberId, storedDrawResult(memberId, existing));
        Map<String, Object> activity = first("SELECT a.activity_id, a.daily_limit, a.total_limit, a.used_count, "
                + "JSON_UNQUOTE(JSON_EXTRACT(a.rule_json, '$.codeReusable')) AS code_reusable, "
                + "JSON_UNQUOTE(JSON_EXTRACT(a.rule_json, '$.rewardValidityDays')) AS reward_validity_days "
                + "FROM biz_lottery_code c JOIN biz_lottery_activity a ON a.activity_id=c.activity_id "
                + "WHERE c.draw_code=? AND (c.status='unused' OR (c.status='used' "
                + "AND NOT EXISTS (SELECT 1 FROM biz_lottery_code_outcome o WHERE o.code_id=c.code_id) "
                + "AND JSON_UNQUOTE(JSON_EXTRACT(a.rule_json, '$.codeReusable'))='true')) AND a.status='0' "
                + "AND (a.start_time IS NULL OR a.start_time<=NOW()) AND (a.end_time IS NULL OR a.end_time>NOW()) FOR UPDATE", drawCode);
        if (activity == null) throw new MiniBusinessException(409, "抽奖二维码无效、已使用或活动已结束");
        boolean reusableCode = "true".equalsIgnoreCase(String.valueOf(activity.get("code_reusable")));
        if (number(activity.get("total_limit")) >= 0 && number(activity.get("used_count")) >= number(activity.get("total_limit")))
            throw new MiniBusinessException(409, "活动名额已用完");
        if (number(activity.get("daily_limit")) >= 0) {
            Integer today = jdbc.queryForObject("SELECT COUNT(*) FROM biz_lottery_record WHERE activity_id = ? AND member_id = ? AND create_time >= CURDATE() FOR UPDATE",
                    Integer.class, activity.get("activity_id"), memberId);
            if (today != null && today >= number(activity.get("daily_limit"))) throw new MiniBusinessException(409, "今日抽奖次数已用完");
        }
        Map<String, Object> code = first("SELECT code_id, status FROM biz_lottery_code WHERE activity_id = ? AND draw_code = ? FOR UPDATE",
                activity.get("activity_id"), drawCode);
        if (code == null || (!"unused".equals(code.get("status")) && !(reusableCode && "used".equals(code.get("status")))))
            throw new MiniBusinessException(409, "二维码已使用或已失效");
        Map<String, Object> allocation = lotteryPreallocation.outcome(code.get("code_id"));
        boolean preallocated = allocation != null;
        Map<String, Object> selected = null;
        if (preallocated) {
            reusableCode = false;
            Map<String, Object> snapshot = lotteryPreallocation.snapshot(allocation);
            if (!"none".equals(snapshot.get("prize_type"))) selected = snapshot;
            activity.put("reward_validity_days", snapshot.get("reward_validity_days"));
        } else {
            long reserved = jdbc.queryForObject("SELECT COUNT(*) FROM biz_lottery_code_outcome o JOIN biz_lottery_code c ON c.code_id=o.code_id "
                    + "WHERE c.activity_id=? AND o.status='reserved' AND (o.expires_at IS NULL OR o.expires_at>NOW()) FOR UPDATE", Long.class, activity.get("activity_id"));
            if (number(activity.get("total_limit")) >= 0 && number(activity.get("used_count")) + reserved >= number(activity.get("total_limit")))
                throw new MiniBusinessException(409, "活动剩余名额已分配给预生成码");
            List<Map<String, Object>> prizes = jdbc.queryForList("SELECT p.prize_id, p.prize_type, p.prize_name, p.probability_bp, p.amount_min_cent, p.amount_max_cent, p.points, p.coupon_template_id, p.stock, p.image_url, p.redeem_store_id FROM biz_lottery_prize p LEFT JOIN biz_coupon_template ct ON ct.coupon_template_id=p.coupon_template_id AND p.prize_type='coupon' WHERE p.activity_id = ? AND p.status = '0' AND p.stock <> 0 AND (p.prize_type <> 'coupon' OR (ct.coupon_template_id IS NOT NULL AND ct.status='0' AND ct.stock <> 0 AND (ct.start_time IS NULL OR ct.start_time<=NOW()) AND (ct.end_time IS NULL OR ct.end_time>NOW()))) ORDER BY p.prize_id FOR UPDATE", activity.get("activity_id"));
            int bucket = RANDOM.nextInt(10000);
            int cumulative = 0;
            for (Map<String, Object> prize : prizes) {
                cumulative += number(prize.get("probability_bp"));
                if (bucket < cumulative) { selected = prize; break; }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        long rewardId = 0;
        Long couponId = null;
        String resultType = "none";
        String resultValue = "谢谢参与";
        if (selected != null) {
            resultType = String.valueOf(selected.get("prize_type"));
            if ("cash".equals(resultType)) {
                int min = number(selected.get("amount_min_cent"));
                int max = Math.max(min, number(selected.get("amount_max_cent")));
                int amount = preallocated ? number(selected.get("amount_cent")) : (min == max ? min : min + RANDOM.nextInt(max - min + 1));
                rewardId = createReward(memberId, CASH, String.valueOf(selected.get("prize_name")), amount, 0,
                        "DR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), "", String.valueOf(selected.get("image_url")), selected.get("redeem_store_id"));
                applyRewardExpiry(rewardId, activity.get("reward_validity_days"));
                result = reward(memberId, rewardId);
                resultValue = String.valueOf(amount);
            } else if ("goods".equals(resultType)) {
                rewardId = createReward(memberId, "goods", String.valueOf(selected.get("prize_name")), 0, 0,
                        "DR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), "", String.valueOf(selected.get("image_url")), selected.get("redeem_store_id"));
                applyRewardExpiry(rewardId, activity.get("reward_validity_days"));
                result = reward(memberId, rewardId);
                resultValue = String.valueOf(selected.get("prize_name"));
            } else if ("points".equals(resultType)) {
                int points = number(selected.get("points"));
                changePoints(memberId, points, "draw", drawCode, String.valueOf(selected.get("prize_name")));
                result.put("id", "draw-" + requestId); result.put("title", selected.get("prize_name")); result.put("value", points + "积分"); result.put("kind", "points"); result.put("createdAt", System.currentTimeMillis()); resultValue = String.valueOf(points);
            } else if ("coupon".equals(resultType)) {
                couponId = preallocated ? createReservedCoupon(memberId, selected) : createCoupon(memberId, number(selected.get("coupon_template_id")));
                result = coupon(memberId, couponId);
                result.put("title", selected.get("prize_name")); result.put("value", selected.get("prize_name")); result.put("kind", "coupon"); result.put("createdAt", System.currentTimeMillis()); resultValue = String.valueOf(selected.get("prize_name"));
            }
            if (!preallocated && number(selected.get("stock")) > 0) jdbc.update("UPDATE biz_lottery_prize SET stock = stock - 1, issued_count = issued_count + 1 WHERE prize_id = ? AND stock > 0", selected.get("prize_id"));
        }
        if (selected == null || "none".equals(resultType)) {
            result.put("id", "draw-" + requestId); result.put("title", "谢谢参与"); result.put("value", resultValue); result.put("kind", "none"); result.put("createdAt", System.currentTimeMillis());
        }
        if (preallocated) lotteryPreallocation.claim(code.get("code_id"), selected == null ? java.util.Collections.emptyMap() : selected);
        if (!reusableCode)
            jdbc.update("UPDATE biz_lottery_code SET status = 'used', member_id = ?, used_at = NOW() WHERE code_id = ? AND status = 'unused'", memberId, code.get("code_id"));
        jdbc.update("UPDATE biz_lottery_activity SET used_count = used_count + 1 WHERE activity_id = ?", activity.get("activity_id"));
        jdbc.update("INSERT INTO biz_lottery_record (activity_id, code_id, member_id, prize_id, result_type, result_value, request_id) VALUES (?, ?, ?, ?, ?, ?, ?)", activity.get("activity_id"), code.get("code_id"), memberId, selected == null ? null : selected.get("prize_id"), resultType, resultValue, requestId);
        Object rewardIdValue = result.get("kind") == null || !(CASH.equals(result.get("kind")) || "goods".equals(result.get("kind")))
                ? null : result.get("id");
        jdbc.update("UPDATE biz_mini_idempotency SET reward_id = ?, member_coupon_id = ?, result_type = ?, result_value = ? "
                + "WHERE member_id = ? AND request_id = ?", rewardIdValue,
                couponId, result.get("kind"), result.get("value"), memberId, requestId);
        // Keep the event timestamp stable across replay; the account balance
        // appended to the response reflects the member's current balance.
        Map<String, Object> created = first("SELECT create_time FROM biz_lottery_record WHERE member_id=? AND request_id=? ORDER BY record_id DESC LIMIT 1", memberId, requestId);
        if (created != null && created.get("create_time") != null)
            result.put("createdAt", timestamp(created.get("create_time")));
        return drawResult(memberId, result);
    }

    public List<Map<String, Object>> drawRecords(long memberId)
    {
        return drawRecords(memberId, MiniListPage.all());
    }

    @Transactional(readOnly = true)
    public Object drawRecords(long memberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        return page.result(drawRecords(memberId, page), jdbc,
                "SELECT COUNT(*) FROM biz_lottery_record WHERE member_id=?", memberId);
    }

    private List<Map<String, Object>> drawRecords(long memberId, MiniListPage page)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.record_id, r.result_type, r.result_value, "
                + "r.request_id, r.create_time, a.activity_name, "
                + "w.reward_id, w.reward_name, w.amount_cent, w.status AS reward_status, i.member_coupon_id "
                + "FROM biz_lottery_record r "
                + "LEFT JOIN biz_lottery_activity a ON a.activity_id = r.activity_id "
                + "LEFT JOIN biz_mini_idempotency i ON i.member_id = r.member_id AND i.request_id = r.request_id "
                + "AND i.action_type = 'draw' "
                + "LEFT JOIN biz_reward w ON w.reward_id = i.reward_id AND w.member_id = r.member_id "
                + "WHERE r.member_id = ? ORDER BY r.record_id DESC" + page.suffix(), memberId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            String type = String.valueOf(row.get("result_type"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(row.get("record_id")));
            item.put("title", drawTitle(type, row));
            item.put("kind", type);
            item.put("value", drawValue(type, row));
            item.put("createdAt", timestamp(row.get("create_time")));
            if (row.get("reward_id") != null)
            {
                item.put("rewardId", String.valueOf(row.get("reward_id")));
                item.put("amount", row.get("amount_cent"));
                item.put("status", row.get("reward_status"));
            }
            if (row.get("member_coupon_id") != null) item.put("couponId", String.valueOf(row.get("member_coupon_id")));
            result.add(item);
        }
        return result;
    }

    private String drawTitle(String type, Map<String, Object> row)
    {
        if (row.get("reward_name") != null) return String.valueOf(row.get("reward_name"));
        if ("points".equals(type)) return "积分奖励";
        if ("coupon".equals(type)) return String.valueOf(row.get("result_value"));
        if ("none".equals(type)) return "谢谢参与";
        return String.valueOf(row.get("activity_name"));
    }

    private String drawValue(String type, Map<String, Object> row)
    {
        if (CASH.equals(type) && row.get("amount_cent") != null)
            return "¥" + String.format(java.util.Locale.ROOT, "%.2f", number(row.get("amount_cent")) / 100.0);
        if ("points".equals(type)) return String.valueOf(row.get("result_value")) + "积分";
        if ("none".equals(type)) return "谢谢参与";
        return String.valueOf(row.get("result_value"));
    }

    @Transactional
    public Map<String, Object> signIn(long memberId, String requestId)
    {
        requireRequestId(requestId);
        pointExpiry.expireMember(memberId);
        ensureAccount(memberId);
        String today = LocalDate.now().toString();
        Map<String, Object> request = idempotency(memberId, requestId, "signin", today);
        if ("signin".equals(request.get("result_type"))) return points(memberId);
        jdbc.queryForObject("SELECT balance FROM biz_point_account WHERE member_id=? FOR UPDATE", Integer.class, memberId);
        Map<String, Object> rule = first("SELECT points,daily_limit FROM biz_point_rule WHERE rule_code='daily_sign' AND status='0'");
        int points = rule == null ? 0 : number(rule.get("points"));
        if (points <= 0) throw new MiniBusinessException(409, "签到规则未配置");
        int dailyLimit = rule == null ? 1 : number(rule.get("daily_limit"));
        if (dailyLimit >= 0) {
            Integer signed = jdbc.queryForObject("SELECT COUNT(*) FROM biz_point_record WHERE member_id=? AND source_type='daily_sign' AND create_time>=CURDATE()", Integer.class, memberId);
            if (signed != null && signed >= dailyLimit) throw new MiniBusinessException(409, dailyLimit == 1 ? "今天已经签到" : "今日签到次数已用完");
        }
        changePoints(memberId, points, "daily_sign", java.time.LocalDate.now().toString(), "每日签到");
        jdbc.update("UPDATE biz_mini_idempotency SET result_type='signin', result_value=? WHERE member_id=? AND request_id=?",
                String.valueOf(points), memberId, requestId);
        return points(memberId);
    }

    public List<Map<String, Object>> redemptions(long verifierMemberId)
    {
        return redemptions(verifierMemberId, MiniListPage.all());
    }

    @Transactional(readOnly = true)
    public Object redemptions(long verifierMemberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        return page.result(redemptions(verifierMemberId, page), jdbc,
                "SELECT COUNT(*) FROM biz_redemption r JOIN biz_reward w ON w.reward_id=r.reward_id WHERE r.verifier_member_id=?", verifierMemberId);
    }

    private List<Map<String, Object>> redemptions(long verifierMemberId, MiniListPage page)
    {
        requireStaff(verifierMemberId);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.redemption_id, r.reward_id, r.member_id, "
                + "r.store_id, r.verifier_member_id, r.status, r.request_id, r.create_time, w.reward_name, w.exchange_code "
                + "FROM biz_redemption r JOIN biz_reward w ON w.reward_id = r.reward_id "
                + "WHERE r.verifier_member_id = ? ORDER BY r.redemption_id DESC" + page.suffix(), verifierMemberId);
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
        long storeId = requireStaff(verifierMemberId);
        code = resolveRedemptionCode(code);
        Map<String, Object> row = first("SELECT w.reward_id, w.member_id, w.reward_type, w.reward_name, w.amount_cent, w.points, "
                + "w.redeem_store_id, s.store_name AS redeem_store_name, w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url, "
                + "m.nickname AS member_name, m.phone AS member_phone "
                + "FROM biz_reward w JOIN biz_member m ON m.member_id = w.member_id " + EXCHANGE_REWARD_JOIN
                + "LEFT JOIN biz_store s ON s.store_id = w.redeem_store_id "
                + "WHERE w.exchange_code = ?", code);
        if (row == null || CASH.equals(row.get("reward_type"))) throw new MiniBusinessException(404, "未找到对应奖品");
        if (row.get("redeem_store_id") != null && number(row.get("redeem_store_id")) != storeId)
            throw new MiniBusinessException(403, "该奖品不属于当前核销门店");
        ensureRewardNotExpired(row);
        return toReward(row, "");
    }

    @Transactional
    public Map<String, Object> confirm(long verifierMemberId, String code, String requestId)
    {
        requireStaff(verifierMemberId);
        requireCode(code);
        requireRequestId(requestId);
        code = resolveRedemptionCode(code);
        // 先按核销人和请求号回放已完成结果，避免重复请求因奖品已核销而误报冲突。
            Map<String, Object> existing = first("SELECT w.reward_id, w.member_id, w.reward_type, w.reward_name, w.amount_cent, w.points, "
                + "w.redeem_store_id, s.store_name AS redeem_store_name, w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url, "
                + "m.nickname AS member_name, m.phone AS member_phone "
                + "FROM biz_redemption r JOIN biz_reward w ON w.reward_id = r.reward_id JOIN biz_member m ON m.member_id = w.member_id "
                + EXCHANGE_REWARD_JOIN
                + "LEFT JOIN biz_store s ON s.store_id = w.redeem_store_id "
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
                + "w.redeem_store_id, s.store_name AS redeem_store_name, "
                + "w.exchange_code, w.image_url, w.status, w.expires_at, w.create_time, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url, "
                + "m.nickname AS member_name, m.phone AS member_phone FROM biz_reward w "
                + "JOIN biz_member m ON m.member_id = w.member_id " + EXCHANGE_REWARD_JOIN
                + "LEFT JOIN biz_store s ON s.store_id = w.redeem_store_id "
                + "WHERE w.exchange_code = ? FOR UPDATE", code);
        if (reward == null || CASH.equals(reward.get("reward_type"))) throw new MiniBusinessException(404, "未找到对应奖品");
        ensureRewardNotExpired(reward);
        if (reward.get("redeem_store_id") != null && number(reward.get("redeem_store_id")) != number(staff.get("staff_store_id")))
            throw new MiniBusinessException(403, "该奖品不属于当前核销门店");
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

    private String resolveRedemptionCode(String code)
    {
        Long rewardId = RedemptionScene.rewardId(code);
        if (rewardId == null) return code;
        Map<String, Object> reward = first("SELECT exchange_code FROM biz_reward WHERE reward_id=?", rewardId);
        if (reward == null || reward.get("exchange_code") == null) throw new MiniBusinessException(404, "未找到对应奖品");
        String exchangeCode = String.valueOf(reward.get("exchange_code"));
        if (!RedemptionScene.encode(rewardId, exchangeCode).equals(code))
            throw new MiniBusinessException(404, "未找到对应奖品");
        return exchangeCode;
    }

    public List<Map<String, Object>> drawPrizes()
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT a.activity_id, a.activity_name, a.start_time, a.end_time, "
                + "p.prize_id, p.prize_type, p.prize_name, p.amount_min_cent, p.amount_max_cent, p.points, p.image_url, p.stock, p.redeem_store_id, s.store_name AS redeem_store_name "
                + "FROM biz_lottery_activity a JOIN biz_lottery_prize p ON p.activity_id = a.activity_id "
                + "LEFT JOIN biz_coupon_template ct ON ct.coupon_template_id = p.coupon_template_id AND p.prize_type = 'coupon' "
                + "LEFT JOIN biz_store s ON s.store_id = p.redeem_store_id "
                + "WHERE a.status = '0' AND (a.start_time IS NULL OR a.start_time <= NOW()) "
                + "AND (a.end_time IS NULL OR a.end_time > NOW()) AND p.status = '0' AND p.stock <> 0 "
                + "AND (p.prize_type <> 'coupon' OR (ct.coupon_template_id IS NOT NULL AND ct.status = '0' "
                + "AND ct.stock <> 0 AND (ct.start_time IS NULL OR ct.start_time <= NOW()) "
                + "AND (ct.end_time IS NULL OR ct.end_time > NOW()))) "
                + "ORDER BY a.activity_id DESC, p.prize_id", new Object[0]);
        Map<Long, Map<String, Object>> activities = new LinkedHashMap<>();
        for (Map<String, Object> row : rows)
        {
            long activityId = number(row.get("activity_id"));
            Map<String, Object> activity = activities.computeIfAbsent(activityId, id -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("activityId", String.valueOf(id));
                item.put("activityName", row.get("activity_name"));
                item.put("startTime", row.get("start_time") == null ? null : timestamp(row.get("start_time")));
                item.put("endTime", row.get("end_time") == null ? null : timestamp(row.get("end_time")));
                item.put("prizes", new ArrayList<Map<String, Object>>());
                return item;
            });
            Map<String, Object> prize = new LinkedHashMap<>();
            prize.put("prizeId", String.valueOf(row.get("prize_id")));
            prize.put("kind", row.get("prize_type"));
            prize.put("name", row.get("prize_name"));
            prize.put("amountMinCent", row.get("amount_min_cent"));
            prize.put("amountMaxCent", row.get("amount_max_cent"));
            prize.put("points", row.get("points"));
            prize.put("image", row.get("image_url"));
            prize.put("stock", row.get("stock"));
            prize.put("redeemStoreId", row.get("redeem_store_id"));
            prize.put("redeemStoreName", row.get("redeem_store_name"));
            ((List<Map<String, Object>>) activity.get("prizes")).add(prize);
        }
        return new ArrayList<>(activities.values());
    }

    public Map<String, Object> promotion(long memberId)
    {
        preparePromotion(memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> sums = first("SELECT COALESCE(SUM(amount_cent),0) AS total, "
                + "COALESCE(SUM(CASE WHEN status='pending' THEN amount_cent ELSE 0 END),0) AS pending, "
                + "COALESCE(SUM(CASE WHEN status='settled' THEN amount_cent ELSE 0 END),0) AS settled, "
                + "COALESCE(SUM(CASE WHEN status='paid' THEN amount_cent ELSE 0 END),0) AS paid "
                + "FROM biz_promotion_record WHERE member_id = ?", memberId);
        result.put("total", number(sums.get("total")));
        result.put("pending", number(sums.get("pending")));
        result.put("settled", number(sums.get("settled")));
        result.put("paid", number(sums.get("paid")));
        Integer minWithdraw = jdbc.queryForObject(
                "SELECT min_withdraw_cent FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1",
                Integer.class);
        result.put("minWithdrawCent", minWithdraw == null ? 0 : minWithdraw);
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

    public Map<String, Object> ranking(long memberId)
    {
        Map<String, Object> config = first("SELECT period_type,start_time,end_time FROM biz_ranking_config WHERE status='0' ORDER BY config_id LIMIT 1");
        LocalDateTime[] period = rankingPeriod(config);
        Timestamp from = Timestamp.valueOf(period[0]), to = Timestamp.valueOf(period[1]);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT p.member_id AS id, m.nickname AS name, "
                + "COALESCE(SUM(p.amount_cent),0) AS amount FROM biz_promotion_record p JOIN biz_member m "
                + "ON m.member_id = p.member_id WHERE p.status IN ('pending','settled','processing','paid') AND p.create_time>=? AND p.create_time<? "
                + "GROUP BY p.member_id, m.nickname HAVING SUM(p.amount_cent) > 0 ORDER BY amount DESC,p.member_id ASC LIMIT 20", from, to);
        for (Map<String, Object> row : rows)
        {
            String name = String.valueOf(row.get("name"));
            row.put("initial", name.isEmpty() ? "?" : name.substring(0, 1));
        }
        Map<String, Object> mine = first("SELECT COALESCE(SUM(amount_cent),0) AS amount FROM biz_promotion_record WHERE member_id=? AND status IN ('pending','settled','processing','paid') AND create_time>=? AND create_time<?", memberId, from, to);
        long myAmount = mine == null ? 0 : ((Number) mine.get("amount")).longValue();
        long rank = myAmount <= 0 ? 0 : scalar("SELECT COUNT(*)+1 FROM (SELECT member_id,SUM(amount_cent) AS amount FROM biz_promotion_record WHERE status IN ('pending','settled','processing','paid') AND create_time>=? AND create_time<? GROUP BY member_id HAVING SUM(amount_cent)>? OR (SUM(amount_cent)=? AND member_id<?)) ranked", from, to, myAmount, myAmount, memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", rows);
        Map<String, Object> me = new LinkedHashMap<>(); me.put("rank", rank == 0 ? null : rank); me.put("amount", myAmount); result.put("me", me);
        return result;
    }

    private static LocalDateTime[] rankingPeriod(Map<String, Object> config)
    {
        String type = config == null ? "month" : String.valueOf(config.get("period_type"));
        LocalDateTime start = dateTime(config == null ? null : config.get("start_time"));
        LocalDateTime end = dateTime(config == null ? null : config.get("end_time"));
        if (start == null) {
            LocalDate today = LocalDate.now();
            if ("week".equals(type)) start = today.with(DayOfWeek.MONDAY).atStartOfDay();
            else if ("quarter".equals(type)) start = today.withMonth(((today.getMonthValue()-1)/3)*3+1).withDayOfMonth(1).atStartOfDay();
            else start = today.withDayOfMonth(1).atStartOfDay();
        }
        if (end == null) end = "week".equals(type) ? start.plusWeeks(1) : "quarter".equals(type) ? start.plusMonths(3) : start.plusMonths(1);
        return new LocalDateTime[] { start, end };
    }

    private static LocalDateTime dateTime(Object value)
    {
        if (value == null) return null;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof java.util.Date) return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), java.time.ZoneId.systemDefault());
        try { return LocalDateTime.parse(String.valueOf(value).replace(' ','T')); } catch (Exception ignored) { return null; }
    }

    public Map<String, Object> promotionCode(long memberId, String productCode)
    {
        Long productId = null;
        if (productCode != null)
        {
            Map<String, Object> product = first("SELECT product_id FROM biz_product WHERE product_code=? AND status='0'", productCode);
            if (product == null) throw new MiniBusinessException(404, "推广商品不存在或已下架");
            productId = ((Number) product.get("product_id")).longValue();
        }
        ensurePromotion(memberId);
        Map<String, Object> row = first("SELECT invite_code FROM biz_promotion_relation WHERE member_id = ? AND status='0'", memberId);
        if (row == null) throw new MiniBusinessException(409, "推广关系已停用，请联系平台处理");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", row.get("invite_code"));
        try
        {
            result.put("qrCode", productId == null
                    ? miniCode.referralCode(String.valueOf(row.get("invite_code")))
                    : miniCode.productReferralCode(memberId, productId));
            result.put("qrCodeStatus", "ready");
        }
        catch (MiniBusinessException e)
        {
            // Keep link-based attribution available while the official QR is not deployable yet.
            result.put("qrCode", "");
            result.put("qrCodeStatus", "pending");
        }
        return result;
    }

    @Transactional
    public Object promotionRecords(long memberId, Map<String, String> params)
    {
        MiniListPage page = new MiniListPage(params);
        preparePromotion(memberId);
        return page.result(promotionRecords(memberId, page.suffix()), jdbc,
                "SELECT COUNT(*) FROM biz_promotion_record WHERE member_id=?", memberId);
    }

    private List<Map<String, Object>> promotionRecords(long memberId)
    {
        return promotionRecords(memberId, " LIMIT 100");
    }

    private List<Map<String, Object>> promotionRecords(long memberId, String suffix)
    {
        return jdbc.queryForList("SELECT promotion_record_id AS id, order_id AS orderId, title, amount_cent AS amount, status, refund_recovery_required AS refundRecoveryRequired, "
                + "create_time AS createdAt FROM biz_promotion_record WHERE member_id = ? ORDER BY promotion_record_id DESC" + suffix, memberId);
    }

    private void preparePromotion(long memberId)
    {
        ensurePromotion(memberId);
        jdbc.update("UPDATE biz_promotion_record p JOIN biz_promotion_config c SET p.status='settled' WHERE p.status='pending' AND p.create_time <= TIMESTAMPADD(DAY, -c.settlement_days, NOW()) AND c.status='0'");
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
        jdbc.update("INSERT INTO biz_point_record (member_id, change_amount, balance_after, source_type, source_id, title, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", memberId, change, balance, source, sourceId, title,
                change > 0 ? pointExpiry.expirationFor(source) : null);
    }

    private long createReward(long memberId, String type, String name, int amount, int points, String code,
            String spec, String image)
    {
        return createReward(memberId, type, name, amount, points, code, spec, image, null);
    }

    private long createReward(long memberId, String type, String name, int amount, int points, String code,
            String spec, String image, Object redeemStoreId)
    {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO biz_reward "
                    + "(member_id, reward_type, reward_name, spec, amount_cent, points, exchange_code, image_url, redeem_store_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "reward_id" });
            statement.setLong(1, memberId);
            statement.setString(2, type);
            statement.setString(3, name);
            statement.setString(4, spec == null ? "" : spec);
            statement.setInt(5, amount);
            statement.setInt(6, points);
            statement.setString(7, code);
            statement.setString(8, image);
            if (redeemStoreId == null) statement.setObject(9, null);
            else statement.setLong(9, ((Number) redeemStoreId).longValue());
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    /** Applies the activity-level validity rule to cash and physical rewards. */
    private void applyRewardExpiry(long rewardId, Object configuredDays)
    {
        int days;
        try { days = configuredDays == null ? 0 : Integer.parseInt(String.valueOf(configuredDays)); }
        catch (NumberFormatException ignored) { days = 0; }
        if (days > 0 && days <= 3650)
        {
            jdbc.update("UPDATE biz_reward SET expires_at=? WHERE reward_id=?", Timestamp.from(Instant.now().plus(days, ChronoUnit.DAYS)), rewardId);
        }
    }

    private long createCoupon(long memberId, long templateId)
    {
        Map<String, Object> template = first("SELECT template_code, template_name, coupon_type, amount_cent, discount_rate, "
                + "min_amount_cent, min_quantity, max_discount_cent, end_time, stock FROM biz_coupon_template "
                + "WHERE coupon_template_id = ? AND status = '0' "
                + "AND (start_time IS NULL OR start_time<=NOW()) AND (end_time IS NULL OR end_time>NOW()) FOR UPDATE", templateId);
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

    private long createReservedCoupon(long memberId, Map<String, Object> snapshot) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO biz_member_coupon "
                    + "(member_id,coupon_template_id,coupon_code,rule_snapshot,expires_at) VALUES(?,?,?,?,?)",
                    new String[] { "member_coupon_id" });
            statement.setLong(1, memberId);
            statement.setLong(2, Long.parseLong(String.valueOf(snapshot.get("coupon_template_id"))));
            statement.setString(3, "CP" + UUID.randomUUID().toString().replace("-", ""));
            statement.setString(4, lotteryPreallocation.encode(snapshot.get("coupon_rules")));
            statement.setTimestamp(5, snapshot.get("coupon_expires_at") == null ? null : Timestamp.valueOf(String.valueOf(snapshot.get("coupon_expires_at"))));
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(row.get("id")));
        result.put("templateId", row.get("templateId"));
        result.put("name", row.get("name"));
        result.put("status", row.get("status"));
        result.put("expiresAt", row.get("expiresAt") == null ? null : timestamp(row.get("expiresAt")));
        return result;
    }

    private Map<String, Object> reward(long memberId, long rewardId)
    {
        Map<String, Object> row = first("SELECT w.reward_id, w.reward_type, w.reward_name, w.amount_cent, w.points, w.exchange_code, "
                + "w.image_url, w.status, w.expires_at, w.create_time, w.redeem_store_id, s.store_name AS redeem_store_name, "
                + "COALESCE(NULLIF(w.spec, ''), e.spec) AS spec, "
                + "COALESCE(NULLIF(w.image_url, ''), e.image_url) AS display_image_url FROM biz_reward w "
                + EXCHANGE_REWARD_JOIN
                + "LEFT JOIN biz_store s ON s.store_id = w.redeem_store_id "
                + "WHERE w.member_id = ? AND w.reward_id = ?", memberId, rewardId);
        if (row == null) throw new MiniBusinessException(404, "奖品不存在");
        ensureRewardNotExpired(row);
        return toReward(row, "");
    }

    /** Mark expired pending rewards closed before exposing or mutating them. */
    private void closeExpiredRewards(long memberId)
    {
        jdbc.update("UPDATE biz_reward w SET status='received' WHERE w.member_id=? AND w.reward_type='cash' "
                + "AND w.status IN ('pending','processing','failed','closed') AND EXISTS (SELECT 1 FROM biz_reward_transfer t "
                + "WHERE t.reward_id=w.reward_id AND t.status='SUCCESS' AND t.transfer_id=(SELECT MAX(latest_t.transfer_id) "
                + "FROM biz_reward_transfer latest_t WHERE latest_t.reward_id=w.reward_id))", memberId);
        jdbc.update("UPDATE biz_reward w SET status='processing' WHERE w.member_id=? AND w.reward_type='cash' "
                + "AND w.status='closed' AND EXISTS (SELECT 1 FROM biz_reward_transfer t WHERE t.reward_id=w.reward_id "
                + "AND t.status IN ('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN'))", memberId);
        jdbc.update("UPDATE biz_reward w SET status='closed' WHERE w.member_id=? AND w.expires_at IS NOT NULL "
                + "AND w.expires_at<=NOW() AND w.status IN ('pending','failed') AND (w.reward_type<>'cash' OR NOT EXISTS "
                + "(SELECT 1 FROM biz_reward_transfer t WHERE t.reward_id=w.reward_id AND t.status IN "
                + "('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN','SUCCESS'))) ", memberId);
    }

    private void ensureRewardNotExpired(Map<String, Object> reward)
    {
        Object expiresAt = reward.get("expires_at");
        if (expiresAt == null) return;
        long expires = timestamp(expiresAt);
        if (expires <= 0 || expires > System.currentTimeMillis()) return;
        jdbc.update("UPDATE biz_reward w SET status='closed' WHERE w.reward_id=? AND w.status IN ('pending','failed') "
                        + "AND (w.reward_type<>'cash' OR NOT EXISTS (SELECT 1 FROM biz_reward_transfer t WHERE t.reward_id=w.reward_id "
                        + "AND t.status IN ('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN','SUCCESS'))) ",
                reward.get("reward_id"));
        Map<String, Object> current = first("SELECT status, reward_type AS rewardType FROM biz_reward WHERE reward_id=?",
                reward.get("reward_id"));
        boolean unsettledCashTransfer = current != null && CASH.equals(current.get("rewardType"))
                && scalar("SELECT COUNT(*) FROM biz_reward_transfer WHERE reward_id=? AND status IN "
                        + "('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN','SUCCESS')",
                        reward.get("reward_id")) > 0;
        if (current != null && !"received".equals(current.get("status")) && !"redeemed".equals(current.get("status"))
                && !unsettledCashTransfer)
            throw new MiniBusinessException(409, "奖品已过期");
    }

    private void restoreClosedCashReward(long rewardId)
    {
        jdbc.update("UPDATE biz_reward SET status='processing' WHERE reward_id=? AND reward_type='cash' AND status='closed' "
                + "AND EXISTS (SELECT 1 FROM biz_reward_transfer WHERE reward_id=? AND status IN "
                + "('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN'))", rewardId, rewardId);
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
        result.put("spec", CASH.equals(row.get("reward_type")) ? "微信现金红包" : (spec.isEmpty()
                ? (row.get("redeem_store_id") == null ? "请到合作门店兑换" : "请到指定门店兑换") : spec));
        Object displayImage = row.get("display_image_url");
        String imageUrl = displayImage != null && !String.valueOf(displayImage).isEmpty()
                ? String.valueOf(displayImage)
                : (row.get("image_url") == null ? image : String.valueOf(row.get("image_url")));
        result.put("image", imageUrl);
        result.put("kind", row.get("reward_type"));
        if (row.get("redeem_store_id") != null) result.put("redeemStoreId", String.valueOf(row.get("redeem_store_id")));
        if (row.get("redeem_store_name") != null && !String.valueOf(row.get("redeem_store_name")).isEmpty()) result.put("redeemStoreName", row.get("redeem_store_name"));
        result.put("redeemStoreScope", row.get("redeem_store_id") == null ? "all" : "specified");
        result.put("status", row.get("status"));
        result.put("code", row.get("exchange_code"));
        if (row.get("member_name") != null) result.put("memberName", row.get("member_name"));
        if (row.get("member_phone") != null) result.put("memberPhone", maskPhone(String.valueOf(row.get("member_phone"))));
        result.put("amount", row.get("amount_cent"));
        result.put("points", row.get("points"));
        result.put("createdAt", timestamp(row.get("create_time")));
        if (row.get("expires_at") != null) result.put("expiresAt", timestamp(row.get("expires_at")));
        if (CASH.equals(row.get("reward_type")))
        {
            Map<String, Object> transfer = latestCashTransfer(number(row.get("reward_id")));
            result.put("transferStatus", transfer == null ? "PENDING" : transfer.get("status"));
            if (transfer != null)
            {
                result.put("transferFailReason", transfer.get("fail_reason"));
                if (transfer.get("package_info") != null) result.put("packageInfo", transfer.get("package_info"));
                if (transfer.get("out_bill_no") != null) result.put("outBillNo", transfer.get("out_bill_no"));
                if (transfer.get("transfer_bill_no") != null) result.put("transferBillNo", transfer.get("transfer_bill_no"));
            }
        }
        return result;
    }

    private Map<String, Object> rewardRow(long memberId, String id, String type)
    {
        try
        {
            long rewardId = Long.parseLong(id);
            return first("SELECT reward_id, reward_type, reward_name, spec, amount_cent, points, exchange_code, image_url, redeem_store_id, status, expires_at, create_time "
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
        Map<String, Object> record = first("SELECT r.result_value, r.result_type, r.create_time, COALESCE(JSON_UNQUOTE(JSON_EXTRACT(o.result_snapshot,'$.prize_name')),p.prize_name) AS prize_name FROM biz_lottery_record r "
                + "LEFT JOIN biz_lottery_prize p ON p.prize_id = r.prize_id "
                + "LEFT JOIN biz_lottery_code_outcome o ON o.code_id=r.code_id "
                + "WHERE r.member_id = ? AND r.request_id = ? ORDER BY r.record_id DESC LIMIT 1",
                memberId, row.get("request_id"));
        String storedValue = record == null || record.get("result_value") == null
                ? String.valueOf(row.get("result_value")) : String.valueOf(record.get("result_value"));
        String replayValue = row.get("result_value") == null || String.valueOf(row.get("result_value")).isEmpty()
                ? storedValue : String.valueOf(row.get("result_value"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", "coupon".equals(type) ? String.valueOf(row.get("member_coupon_id")) : "draw-" + row.get("request_id"));
        result.put("kind", type);
        // Replay the original event timestamp as well as its business value;
        // an idempotent response should not look like a newly-created prize.
        result.put("createdAt", record == null || record.get("create_time") == null
                ? System.currentTimeMillis() : timestamp(record.get("create_time")));
        if ("points".equals(type))
        {
            String title = record == null || record.get("prize_name") == null
                    || String.valueOf(record.get("prize_name")).isEmpty() ? "积分奖励" : String.valueOf(record.get("prize_name"));
            result.put("title", title);
            result.put("value", replayValue.endsWith("积分") ? replayValue : replayValue + "积分");
        }
        else if ("coupon".equals(type))
        {
            result.putAll(coupon(memberId, number(row.get("member_coupon_id"))));
            result.put("kind", "coupon");
            result.put("title", storedValue);
            result.put("value", storedValue);
        }
        else if ("none".equals(type))
        {
            // Keep the replay business result consistent with the original no-prize result.
            result.put("title", "谢谢参与");
            result.put("value", storedValue.isEmpty() ? "谢谢参与" : storedValue);
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

    private Map<String, Object> exchangeResult(long memberId, Map<String, Object> result)
    {
        result.put("pointBalance", jdbc.queryForObject(
                "SELECT balance FROM biz_point_account WHERE member_id=?", Integer.class, memberId));
        return result;
    }

    private Map<String, Object> drawResult(long memberId, Map<String, Object> result)
    {
        Map<String, Object> account = first("SELECT balance FROM biz_point_account WHERE member_id=?", memberId);
        result.put("pointBalance", account == null ? 0 : number(account.get("balance")));
        return result;
    }

    private long couponTemplateId(String code)
    {
        Map<String, Object> row = first("SELECT coupon_template_id FROM biz_coupon_template WHERE template_code = ? AND status = '0'", code);
        if (row == null) throw new MiniBusinessException(409, "活动券尚未配置");
        return number(row.get("coupon_template_id"));
    }

    private long requireStaff(long memberId)
    {
        // A verifier is usable only while both the account and its assigned
        // store remain active. This keeps prize redemption consistent with
        // the separate pickup-order verifier path.
        Map<String, Object> row = first("SELECT m.staff_store_id FROM biz_member m JOIN biz_store s "
                + "ON s.store_id = m.staff_store_id AND s.status = '0' "
                + "WHERE m.member_id = ? AND m.status = '0'", memberId);
        if (row == null || row.get("staff_store_id") == null) throw new MiniBusinessException(403, "当前账号没有核销权限");
        return number(row.get("staff_store_id"));
    }

    private static void requireRequestId(String value)
    {
        if (value == null || !REQUEST_ID.matcher(value).matches()) throw new MiniBusinessException(400, "请求号无效");
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String truncate(String value, int max) { if (value == null) return ""; return value.length() <= max ? value : value.substring(0, max); }

    private static void requireCode(String value)
    {
        if (value == null || !CODE.matcher(value).matches()) throw new MiniBusinessException(400, "活动码或兑换码无效");
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private long scalar(String sql, Object... args)
    {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private Map<String, Object> latestCashTransfer(long rewardId)
    {
        return first("SELECT transfer_id,reward_id,member_id,request_id,out_bill_no,transfer_bill_no,amount_cent,openid,status,package_info,fail_reason "
                + "FROM biz_reward_transfer WHERE reward_id=? ORDER BY transfer_id DESC LIMIT 1", rewardId);
    }

    private void refreshCashTransfer(long rewardId)
    {
        Map<String, Object> transfer = latestCashTransfer(rewardId);
        if (transfer == null) return;
        String stateValue = String.valueOf(transfer.get("status"));
        if ("SUCCESS".equals(stateValue))
        {
            jdbc.update("UPDATE biz_reward SET status='received' WHERE reward_id=? AND status IN ('pending','processing','failed','closed')", rewardId);
            return;
        }
        if (!("SUBMITTING".equals(stateValue) || "ACCEPTED".equals(stateValue) || "PROCESSING".equals(stateValue)
                || "WAIT_USER_CONFIRM".equals(stateValue) || "TRANSFERING".equals(stateValue)
                || "CANCELING".equals(stateValue) || "UNKNOWN".equals(stateValue))) return;
        try
        {
            Map<String, Object> result = wechatPay.queryCashTransfer(String.valueOf(transfer.get("out_bill_no")));
            if (result != null && cashTransferMatches(transfer, result)) saveCashTransferResult(transfer, result);
            else touchCashTransfer(transfer.get("transfer_id"));
        }
        catch (MiniBusinessException ignored)
        {
            // Keep the last known state visible while the channel is unavailable.
            touchCashTransfer(transfer.get("transfer_id"));
        }
    }

    private void saveCashTransferResult(Map<String, Object> transfer, Map<String, Object> result)
    {
        String stateValue = String.valueOf(result.get("state"));
        int changed = jdbc.update("UPDATE biz_reward_transfer SET transfer_bill_no=NULLIF(?,''),status=?,package_info=NULLIF(?,''),fail_reason=? "
                        + "WHERE transfer_id=? AND status NOT IN ('SUCCESS','FAIL','CANCELLED')",
                result.get("transferBillNo"), stateValue, result.get("packageInfo"), result.get("failReason"), transfer.get("transfer_id"));
        if (changed != 1) return;
        long rewardId = number(transfer.get("reward_id"));
        if ("SUCCESS".equals(stateValue)) jdbc.update("UPDATE biz_reward SET status='received' WHERE reward_id=? AND status IN ('pending','processing','failed','closed')", rewardId);
        else if ("FAIL".equals(stateValue) || "CANCELLED".equals(stateValue)) jdbc.update("UPDATE biz_reward SET status='failed' WHERE reward_id=? AND status IN ('pending','processing')", rewardId);
        else jdbc.update("UPDATE biz_reward SET status='processing' WHERE reward_id=? AND status='pending'", rewardId);
    }

    private void touchCashTransfer(Object transferId)
    {
        jdbc.update("UPDATE biz_reward_transfer SET update_time=NOW() WHERE transfer_id=? AND status IN "
                + "('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN')", transferId);
    }

    private Map<String, Object> cashTransferResult(Map<String, Object> transfer)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        if (transfer == null)
        {
            result.put("state", "PENDING");
            return result;
        }
        result.put("transferId", String.valueOf(transfer.get("transfer_id")));
        result.put("outBillNo", transfer.get("out_bill_no"));
        result.put("transferBillNo", transfer.get("transfer_bill_no"));
        result.put("state", transfer.get("status"));
        result.put("packageInfo", transfer.get("package_info"));
        result.put("failReason", transfer.get("fail_reason"));
        if (transfer.get("package_info") != null && !blank(String.valueOf(transfer.get("package_info"))))
        {
            try { result.putAll(wechatPay.cashTransferClientInfo()); }
            catch (MiniBusinessException ignored) { }
        }
        return result;
    }

    private static boolean cashTransferMatches(Map<String, Object> transfer, Map<String, Object> result)
    {
        String openid = String.valueOf(result.get("openid") == null ? "" : result.get("openid"));
        return String.valueOf(transfer.get("out_bill_no")).equals(String.valueOf(result.get("outBillNo")))
                && (openid.isEmpty() || String.valueOf(transfer.get("openid")).equals(openid))
                && number(transfer.get("amount_cent")) == number(result.get("amountCent"));
    }

    static String cashBillNo(long rewardId, int attempt)
    {
        return String.format(Locale.ROOT, "JW%019d%010d", rewardId, attempt);
    }

    private static int number(Object value)
    {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static String maskPhone(String value)
    {
        if (value == null || value.trim().isEmpty()) return "";
        String phone = value.trim();
        return phone.length() >= 7 ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4) : phone;
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
