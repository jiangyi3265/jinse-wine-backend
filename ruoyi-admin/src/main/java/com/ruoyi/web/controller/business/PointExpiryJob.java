package com.ruoyi.web.controller.business;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily safety net; point reads and spends also expire the member lazily. */
@Component
public class PointExpiryJob
{
    private final JdbcTemplate jdbc;
    private final PointExpiryService expiry;

    public PointExpiryJob(JdbcTemplate jdbc, PointExpiryService expiry) { this.jdbc = jdbc; this.expiry = expiry; }

    @Scheduled(cron = "${mini.points.expiry-cron:0 15 2 * * *}")
    public void expireDuePoints()
    {
        long cursor = 0;
        while (true)
        {
            List<Map<String, Object>> members = jdbc.queryForList("SELECT DISTINCT member_id FROM biz_point_record WHERE member_id>? AND change_amount>0 AND expires_at<=NOW() ORDER BY member_id LIMIT 500", cursor);
            for (Map<String, Object> row : members)
            {
                cursor = ((Number) row.get("member_id")).longValue();
                expiry.expireMember(cursor);
            }
            if (members.size() < 500) return;
        }
    }
}
