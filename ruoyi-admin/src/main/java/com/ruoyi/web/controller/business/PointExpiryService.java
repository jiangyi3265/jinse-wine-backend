package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Expires due point grants after accounting for FIFO spending and legacy balances. */
@Service
public class PointExpiryService
{
    private final JdbcTemplate jdbc;

    public PointExpiryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Timestamp expirationFor(String source)
    {
        String ruleCode = "order".equals(source) ? "order_paid" : source;
        List<Map<String, Object>> rules = jdbc.queryForList("SELECT validity_days FROM biz_point_rule WHERE rule_code=? AND status='0' LIMIT 1", ruleCode);
        if (rules.isEmpty()) return null;
        int days = number(rules.get(0).get("validity_days"));
        if (days <= 0) return null;
        return new Timestamp(System.currentTimeMillis() + days * 86400000L);
    }

    @Transactional
    public int expireMember(long memberId)
    {
        List<Map<String, Object>> accounts = jdbc.queryForList("SELECT balance FROM biz_point_account WHERE member_id=? FOR UPDATE", memberId);
        if (accounts.isEmpty()) return 0;
        int balance = number(accounts.get(0).get("balance"));
        List<Map<String, Object>> records = jdbc.queryForList("SELECT point_record_id, change_amount, source_type, source_id, expires_at FROM biz_point_record WHERE member_id=? ORDER BY point_record_id FOR UPDATE", memberId);
        long ledgerNet = 0;
        for (Map<String, Object> record : records) ledgerNet += number(record.get("change_amount"));

        List<PointLot> lots = new ArrayList<>();
        Map<Long, PointLot> lotsById = new LinkedHashMap<>();
        long legacyBalance = Math.max(0L, balance - ledgerNet);
        if (legacyBalance > 0) lots.add(new PointLot(0, (int) Math.min(Integer.MAX_VALUE, legacyBalance), null));
        for (Map<String, Object> record : records)
        {
            long recordId = number(record.get("point_record_id"));
            int change = number(record.get("change_amount"));
            if (change > 0)
            {
                PointLot lot = new PointLot(recordId, change, record.get("expires_at"));
                lots.add(lot);
                lotsById.put(recordId, lot);
            }
            else if (change < 0)
            {
                int debit = -change;
                String sourceType = String.valueOf(record.get("source_type"));
                if ("expiry".equals(sourceType) || "order_refund".equals(sourceType))
                {
                    PointLot lot = lotsById.get(parseLong(record.get("source_id")));
                    if (lot != null)
                    {
                        int take = Math.min(debit, lot.remaining);
                        lot.remaining -= take;
                        debit -= take;
                    }
                    if (debit > 0) consumeOldest(lots, debit);
                }
                else debit = consumeOldest(lots, debit);
            }
        }

        long outstanding = 0;
        for (PointLot lot : lots) outstanding += lot.remaining;
        if (outstanding > balance)
        {
            long excess = outstanding - balance;
            for (PointLot lot : lots)
            {
                int take = (int) Math.min(excess, lot.remaining);
                lot.remaining -= take;
                excess -= take;
                if (excess == 0) break;
            }
        }
        else if (outstanding < balance)
        {
            lots.add(new PointLot(0, (int) Math.min(Integer.MAX_VALUE, balance - outstanding), null));
        }

        int expired = 0;
        List<PointLot> due = new ArrayList<>();
        for (PointLot lot : lots)
        {
            if (lot.recordId > 0 && lot.remaining > 0 && lot.expiresAt != null && !expirationAfterNow(lot.expiresAt))
            {
                int amount = Math.min(lot.remaining, balance - expired);
                if (amount > 0) { lot.remaining = amount; due.add(lot); expired += amount; }
            }
        }
        if (expired == 0) return 0;
        int updated = jdbc.update("UPDATE biz_point_account SET balance=balance-? WHERE member_id=? AND balance>=?", expired, memberId, expired);
        if (updated != 1) throw new IllegalStateException("积分余额并发变更，请重试");
        int after = balance;
        for (PointLot lot : due)
        {
            after -= lot.remaining;
            jdbc.update("INSERT INTO biz_point_record(member_id,change_amount,balance_after,source_type,source_id,title) VALUES(?, ?, ?, 'expiry', ?, '积分到期扣减')",
                    memberId, -lot.remaining, after, String.valueOf(lot.recordId));
        }
        return expired;
    }

    private static int consumeOldest(List<PointLot> lots, int debit)
    {
        for (PointLot lot : lots)
        {
            int take = Math.min(debit, lot.remaining);
            lot.remaining -= take;
            debit -= take;
            if (debit == 0) break;
        }
        return debit;
    }

    private static int number(Object value) { return value == null ? 0 : ((Number) value).intValue(); }
    private static long parseLong(Object value) { try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return -1; } }
    private static boolean expirationAfterNow(Object value)
    {
        if (value instanceof java.util.Date) return ((java.util.Date) value).after(new java.util.Date());
        try { return java.time.LocalDateTime.parse(String.valueOf(value).replace(' ', 'T')).isAfter(java.time.LocalDateTime.now()); }
        catch (Exception ignored) { return false; }
    }

    private static final class PointLot
    {
        private final long recordId;
        private int remaining;
        private final Object expiresAt;
        private PointLot(long recordId, int points, Object expiresAt) { this.recordId = recordId; this.remaining = points; this.expiresAt = expiresAt; }
    }
}
