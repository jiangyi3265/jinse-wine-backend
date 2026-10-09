package com.ruoyi.web.controller.business;

import org.springframework.jdbc.core.JdbcTemplate;

/** Counts only completed redemptions for one exact exchange item code. */
final class ExchangeLimit
{
    private ExchangeLimit() { }

    static int completed(JdbcTemplate jdbc, long memberId, String exchangeCode)
    {
        String payloadHash = MiniMemberAuthService.sha256("exchange|" + exchangeCode);
        return jdbc.queryForList("SELECT request_id FROM biz_mini_idempotency "
                        + "WHERE member_id=? AND action_type='exchange' AND payload_hash=? "
                        + "AND (reward_id IS NOT NULL OR member_coupon_id IS NOT NULL) FOR UPDATE",
                memberId, payloadHash).size();
    }
}
