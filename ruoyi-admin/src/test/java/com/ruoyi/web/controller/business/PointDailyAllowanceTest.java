package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class PointDailyAllowanceTest
{
    @Test
    public void countsOnlyTodaysAwardsForTheSameMemberAndSource()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:point_daily;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_point_record (point_record_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "member_id BIGINT, source_type VARCHAR(30), change_amount INT, create_time TIMESTAMP)");
        assertTrue(PointDailyAllowance.available(jdbc, 1, "invite_register", -1));
        assertFalse(PointDailyAllowance.available(jdbc, 1, "invite_register", 0));
        jdbc.update("INSERT INTO biz_point_record(member_id,source_type,change_amount,create_time) VALUES(1,'invite_register',10,DATEADD('DAY',-1,CURRENT_TIMESTAMP()))");
        jdbc.update("INSERT INTO biz_point_record(member_id,source_type,change_amount,create_time) VALUES(1,'order',10,CURRENT_TIMESTAMP())");
        assertTrue(PointDailyAllowance.available(jdbc, 1, "invite_register", 1));
        jdbc.update("INSERT INTO biz_point_record(member_id,source_type,change_amount,create_time) VALUES(1,'invite_register',10,CURRENT_TIMESTAMP())");
        assertFalse(PointDailyAllowance.available(jdbc, 1, "invite_register", 1));
        assertTrue(PointDailyAllowance.available(jdbc, 2, "invite_register", 1));
        assertTrue(PointDailyAllowance.available(jdbc, 1, "order", 2));
    }
}
