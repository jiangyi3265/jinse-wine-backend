package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class InvitationPointLimitTest
{
    @Test
    public void bindingCanContinueAfterDailyBonusLimitIsReached()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:invitation_points;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_point_rule (rule_code VARCHAR(30), status CHAR(1), validity_days INT)");
        jdbc.execute("CREATE TABLE biz_point_account (member_id BIGINT PRIMARY KEY, balance INT)");
        jdbc.execute("CREATE TABLE biz_point_record (point_record_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "member_id BIGINT, change_amount INT, balance_after INT, source_type VARCHAR(30), "
                + "source_id VARCHAR(64), title VARCHAR(128), expires_at TIMESTAMP, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO biz_point_rule(rule_code,status,validity_days) VALUES('invite_register','0',0)");
        MiniBusinessExtensionService service = new MiniBusinessExtensionService(jdbc, new ObjectMapper(), new PointExpiryService(jdbc), null);

        service.awardInvitationPoints(1, 100, 1, "2", "邀请新会员奖励");
        service.awardInvitationPoints(1, 100, 1, "3", "邀请新会员奖励");
        service.awardInvitationPoints(2, 100, 0, "1", "绑定推广关系奖励");

        assertEquals(Integer.valueOf(100), jdbc.queryForObject("SELECT balance FROM biz_point_account WHERE member_id=1", Integer.class));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT balance FROM biz_point_account WHERE member_id=2", Integer.class));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM biz_point_record WHERE member_id=1", Integer.class));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT COUNT(*) FROM biz_point_record WHERE member_id=2", Integer.class));
    }
}
