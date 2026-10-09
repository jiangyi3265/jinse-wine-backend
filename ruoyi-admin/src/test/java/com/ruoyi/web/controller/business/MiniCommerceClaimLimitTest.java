package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MiniCommerceClaimLimitTest
{
    @Test
    public void twoClaimsReplayAndLimitShareOneAuthoritativeCount()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:coupon_claim_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_coupon_claim_request (member_id BIGINT, request_id VARCHAR(64), "
                + "template_code VARCHAR(64), member_coupon_id BIGINT, PRIMARY KEY (member_id, request_id))");
        jdbc.execute("CREATE TABLE biz_coupon_template (coupon_template_id BIGINT PRIMARY KEY, template_code VARCHAR(64), "
                + "template_name VARCHAR(128), coupon_type VARCHAR(16), amount_cent INT, discount_rate INT, "
                + "min_amount_cent INT, min_quantity INT, max_discount_cent INT, member_only BOOLEAN, points_only BOOLEAN, "
                + "stock INT, claim_limit INT, start_time TIMESTAMP, end_time TIMESTAMP, status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_member_coupon (member_coupon_id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT, "
                + "coupon_template_id BIGINT, coupon_code VARCHAR(64), rule_snapshot VARCHAR(2000), "
                + "status VARCHAR(16) DEFAULT 'unused', expires_at TIMESTAMP)");
        jdbc.update("INSERT INTO biz_coupon_template VALUES (4,'multi2','Two coupons','fixed',1500,100,20000,1,0,FALSE,FALSE,2,2,NULL,NULL,'0')");
        MiniCommerceService service = new MiniCommerceService(jdbc, new ObjectMapper(), null, null, null);

        Map<String, Object> first = service.claim(7, "multi2", "claim-0001");
        Map<String, Object> replay = service.claim(7, "multi2", "claim-0001");
        assertEquals(first.get("id"), replay.get("id"));
        assertEquals("multi2", replay.get("templateId"));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM biz_member_coupon", Integer.class));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT stock FROM biz_coupon_template WHERE coupon_template_id=4", Integer.class));

        Map<String, Object> second = service.claim(7, "multi2", "claim-0002");
        assertEquals("multi2", second.get("templateId"));
        try
        {
            service.claim(7, "multi2", "claim-0003");
            fail("A third claim must exceed the per-member limit");
        }
        catch (MiniBusinessException error)
        {
            assertEquals(409, error.getCode());
        }
        assertEquals(Integer.valueOf(2), jdbc.queryForObject("SELECT COUNT(*) FROM biz_member_coupon", Integer.class));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT stock FROM biz_coupon_template WHERE coupon_template_id=4", Integer.class));
    }
}
