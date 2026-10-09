package com.ruoyi.web.controller.business;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Counts point grants after the member account row has been locked. */
final class PointDailyAllowance
{
    private PointDailyAllowance() { }

    static boolean available(JdbcTemplate jdbc, long memberId, String sourceType, int dailyLimit)
    {
        if (dailyLimit < 0) return true;
        if (dailyLimit == 0) return false;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        List<Map<String, Object>> awards = jdbc.queryForList(
                "SELECT point_record_id FROM biz_point_record WHERE member_id=? AND source_type=? "
                        + "AND change_amount>0 AND create_time>=? AND create_time<? FOR UPDATE",
                memberId, sourceType, Timestamp.valueOf(today.atStartOfDay()),
                Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
        return awards.size() < dailyLimit;
    }
}
