package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class ExchangeLimitTest
{
    @Test
    public void countsOnlyCompletedRequestsForExactItem()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:exchange_limit;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_mini_idempotency (member_id BIGINT, request_id VARCHAR(64), "
                + "action_type VARCHAR(32), payload_hash CHAR(64), reward_id BIGINT, member_coupon_id BIGINT)");
        String itemHash = MiniMemberAuthService.sha256("exchange|CUP_1");
        String otherHash = MiniMemberAuthService.sha256("exchange|CUPA1");
        jdbc.update("INSERT INTO biz_mini_idempotency VALUES(1,'r1','exchange',?,NULL,11)", itemHash);
        jdbc.update("INSERT INTO biz_mini_idempotency VALUES(1,'r2','exchange',?,NULL,NULL)", itemHash);
        jdbc.update("INSERT INTO biz_mini_idempotency VALUES(1,'r3','exchange',?,12,NULL)", otherHash);
        jdbc.update("INSERT INTO biz_mini_idempotency VALUES(2,'r4','exchange',?,NULL,13)", itemHash);
        jdbc.update("INSERT INTO biz_mini_idempotency VALUES(1,'r5','draw',?,NULL,14)", itemHash);
        assertEquals(1, ExchangeLimit.completed(jdbc, 1, "CUP_1"));
        assertEquals(1, ExchangeLimit.completed(jdbc, 1, "CUPA1"));
        assertEquals(0, ExchangeLimit.completed(jdbc, 1, "CUP%1"));
    }
}
