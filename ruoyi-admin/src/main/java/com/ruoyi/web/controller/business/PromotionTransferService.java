package com.ruoyi.web.controller.business;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Durable commission payouts: reserve locally, commit, then contact WeChat with a stable bill number. */
@Service
public class PromotionTransferService
{
    private static final Logger LOG = LoggerFactory.getLogger(PromotionTransferService.class);
    private final JdbcTemplate jdbc;
    private final WechatPayService pay;
    private final MiniBusinessExtensionService promotions;
    private final TransactionTemplate tx;
    @Value("${mini.promotion.auto-transfer-enabled:true}") private boolean enabled = true;

    public PromotionTransferService(JdbcTemplate jdbc, WechatPayService pay, MiniBusinessExtensionService promotions, PlatformTransactionManager transactions)
    {
        this.jdbc = jdbc; this.pay = pay; this.promotions = promotions; this.tx = new TransactionTemplate(transactions);
    }

    @Scheduled(fixedDelayString="${mini.promotion.transfer-interval-ms:60000}", initialDelayString="${mini.promotion.transfer-initial-delay-ms:60000}")
    public void dispatch()
    {
        if (!enabled || !pay.isCommissionTransferConfigured()) return;
        promotions.settleMaturedPromotions();
        for (Map<String, Object> member : eligibleMembers())
        {
            try { tx.execute(status -> reserve(((Number) member.get("member_id")).longValue())); }
            catch (RuntimeException e) { LOG.warn("Commission reservation deferred for member {}", member.get("member_id")); }
        }
        for (Map<String, Object> row : jdbc.queryForList("SELECT settlement_id FROM biz_promotion_transfer WHERE status NOT IN ('SUCCESS','FAIL','CANCELLED') ORDER BY update_time,settlement_id LIMIT 50"))
            reconcile(((Number) row.get("settlement_id")).longValue());
    }

    List<Map<String, Object>> eligibleMembers()
    {
        // Unpayable accounts must not occupy every slot and starve later members.
        return jdbc.queryForList("SELECT r.member_id FROM biz_promotion_record r JOIN biz_member m ON m.member_id=r.member_id "
                + "WHERE r.status='settled' AND m.status='0' AND TRIM(COALESCE(m.wechat_openid,''))<>'' "
                + "AND NOT EXISTS (SELECT 1 FROM biz_promotion_record active WHERE active.member_id=r.member_id AND active.status='processing') "
                + "AND EXISTS (SELECT 1 FROM biz_promotion_config WHERE status='0') "
                + "GROUP BY r.member_id HAVING SUM(CASE WHEN r.amount_cent<200000 THEN r.amount_cent ELSE 0 END)>="
                + "COALESCE((SELECT NULLIF(min_withdraw_cent,0) FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1),1000) ORDER BY r.member_id LIMIT 50");
    }

    Map<String, Object> reserve(long memberId)
    {
        Map<String, Object> config = first("SELECT min_withdraw_cent FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1");
        if (config == null) return null;
        int configuredMinimum = config.get("min_withdraw_cent") == null ? 0 : ((Number) config.get("min_withdraw_cent")).intValue();
        int minimum = configuredMinimum > 0 ? configuredMinimum : 1000;
        Map<String, Object> member = first("SELECT wechat_openid FROM biz_member WHERE member_id=? AND status='0' FOR UPDATE", memberId);
        if (member == null || member.get("wechat_openid") == null || String.valueOf(member.get("wechat_openid")).trim().isEmpty()) return null;
        if (jdbc.queryForObject("SELECT COUNT(*) FROM biz_promotion_record WHERE member_id=? AND status='processing'", Long.class, memberId) > 0) return null;
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT promotion_record_id,amount_cent FROM biz_promotion_record WHERE member_id=? AND status='settled' ORDER BY amount_cent,promotion_record_id FOR UPDATE", memberId);
        List<Long> selected = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> row : rows)
        {
            long amount = ((Number) row.get("amount_cent")).longValue();
            // This integration has no verified real-name collection; do not bypass WeChat's >= 2000 yuan verification.
            if (amount == 0 || amount >= 200000 || total + amount >= 200000) continue;
            total += amount;
            selected.add(((Number) row.get("promotion_record_id")).longValue());
        }
        if (total < minimum || selected.isEmpty()) return null;
        String requestId = "auto-commission-" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO biz_promotion_settlement(member_id,amount_cent,status,request_id) VALUES(?,?,'processing',?)", memberId, total, requestId);
        Long id = jdbc.queryForObject("SELECT settlement_id FROM biz_promotion_settlement WHERE member_id=? AND request_id=?", Long.class, memberId, requestId);
        for (Long record : selected)
        {
            if (jdbc.update("UPDATE biz_promotion_record SET status='processing' WHERE promotion_record_id=? AND member_id=? AND status='settled'", record, memberId) != 1)
                throw new MiniBusinessException(409, "佣金状态发生变化");
            jdbc.update("INSERT INTO biz_promotion_settlement_item(settlement_id,promotion_record_id) VALUES(?,?)", id, record);
        }
        jdbc.update("INSERT INTO biz_promotion_transfer(settlement_id,member_id,out_bill_no,amount_cent,openid,status) VALUES(?,?,?,?,?,'SUBMITTING')", id, memberId, "XSCP" + id, total, member.get("wechat_openid"));
        return first("SELECT * FROM biz_promotion_transfer WHERE settlement_id=?", id);
    }

    void reconcile(long id)
    {
        if (!pay.isCommissionTransferConfigured()) return;
        Map<String, Object> row = first("SELECT * FROM biz_promotion_transfer WHERE settlement_id=?", id);
        if (row == null || Arrays.asList("SUCCESS", "FAIL", "CANCELLED").contains(row.get("status"))) return;
        try
        {
            Map<String, Object> result = pay.queryCashTransfer(String.valueOf(row.get("out_bill_no")));
            if (result == null)
                result = pay.createCommissionTransfer(String.valueOf(row.get("out_bill_no")), ((Number) row.get("amount_cent")).intValue(), String.valueOf(row.get("openid")));
            final Map<String, Object> verified = result;
            tx.execute(status -> {
                Map<String, Object> locked = first("SELECT * FROM biz_promotion_transfer WHERE settlement_id=? FOR UPDATE", id);
                PromotionTransferLedger.apply(jdbc, locked, verified); return null;
            });
        }
        catch (RuntimeException e)
        {
            jdbc.update("UPDATE biz_promotion_transfer SET status='UNKNOWN',failure_reason='微信结果待查询，请勿重复付款',update_time=NOW() WHERE settlement_id=? AND status NOT IN ('SUCCESS','FAIL','CANCELLED')", id);
        }
    }

    public Map<String, Object> list(long memberId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("automatic", enabled);
        result.put("configured", pay.isCommissionTransferConfigured());
        result.put("rows", jdbc.queryForList("SELECT settlement_id AS id,amount_cent AS amount,status,failure_reason AS failureReason,create_time AS createdAt FROM biz_promotion_transfer WHERE member_id=? ORDER BY settlement_id DESC LIMIT 50", memberId));
        return result;
    }

    public Map<String, Object> detail(long memberId, long id)
    {
        if (first("SELECT settlement_id FROM biz_promotion_transfer WHERE member_id=? AND settlement_id=?", memberId, id) == null)
            throw new MiniBusinessException(404, "佣金转账不存在");
        reconcile(id);
        Map<String, Object> result = first("SELECT settlement_id AS id,amount_cent AS amount,status AS state,package_info AS packageInfo,failure_reason AS failureReason FROM biz_promotion_transfer WHERE member_id=? AND settlement_id=?", memberId, id);
        if ("WAIT_USER_CONFIRM".equals(result.get("state"))) result.putAll(pay.cashTransferClientInfo());
        return result;
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args); return rows.isEmpty() ? null : rows.get(0);
    }
}
