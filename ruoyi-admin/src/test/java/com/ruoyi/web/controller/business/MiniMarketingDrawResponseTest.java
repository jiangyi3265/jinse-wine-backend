package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MiniMarketingDrawResponseTest
{
    private static final ObjectMapper JSON = new ObjectMapper();

    public static String jsonExtract(String value, String path) throws Exception
    {
        if (value == null || !path.startsWith("$.")) return null;
        JsonNode node = JSON.readTree(value).path(path.substring(2));
        return node.isMissingNode() || node.isNull() ? null : node.toString();
    }

    public static String jsonUnquote(String value) throws Exception
    {
        return value == null ? null : JSON.readValue(value, String.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void couponReplayReturnsEntitlementAndCurrentPointBalance() throws Exception
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:draw_response_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE ALIAS JSON_EXTRACT FOR 'com.ruoyi.web.controller.business.MiniMarketingDrawResponseTest.jsonExtract'");
        jdbc.execute("CREATE ALIAS JSON_UNQUOTE FOR 'com.ruoyi.web.controller.business.MiniMarketingDrawResponseTest.jsonUnquote'");
        jdbc.execute("CREATE TABLE biz_point_account (member_id BIGINT PRIMARY KEY, balance INT)");
        jdbc.execute("CREATE TABLE biz_lottery_prize (prize_id BIGINT PRIMARY KEY, prize_name VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_lottery_record (record_id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT, request_id VARCHAR(64), prize_id BIGINT, code_id BIGINT, "
                + "result_type VARCHAR(16), result_value VARCHAR(64), create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_lottery_code_outcome (code_id BIGINT, result_snapshot VARCHAR(500))");
        jdbc.execute("CREATE TABLE biz_coupon_template (coupon_template_id BIGINT PRIMARY KEY, template_code VARCHAR(64), "
                + "template_name VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_member_coupon (member_coupon_id BIGINT PRIMARY KEY, member_id BIGINT, "
                + "coupon_template_id BIGINT, status VARCHAR(16), expires_at TIMESTAMP)");
        jdbc.update("INSERT INTO biz_point_account VALUES (7, 320)");
        jdbc.update("INSERT INTO biz_lottery_prize VALUES (1, 'Festival coupon')");
        jdbc.update("INSERT INTO biz_lottery_record (member_id, request_id, prize_id, result_type, result_value, create_time) "
                + "VALUES (7, 'draw-001', 1, 'coupon', 'Festival coupon', CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO biz_coupon_template VALUES (4, 'festival90', 'Festival coupon')");
        jdbc.update("INSERT INTO biz_member_coupon VALUES (33, 7, 4, 'unused', CURRENT_TIMESTAMP)");

        MiniMarketingService service = new MiniMarketingService(jdbc, new PointExpiryService(jdbc), null, null);
        Method replayMethod = MiniMarketingService.class.getDeclaredMethod("storedDrawResult", long.class, Map.class);
        replayMethod.setAccessible(true);
        Method balanceMethod = MiniMarketingService.class.getDeclaredMethod("drawResult", long.class, Map.class);
        balanceMethod.setAccessible(true);
        Map<String, Object> request = new HashMap<>();
        request.put("request_id", "draw-001");
        request.put("result_type", "coupon");
        request.put("result_value", "Festival coupon");
        request.put("member_coupon_id", 33L);

        Map<String, Object> replay = (Map<String, Object>) replayMethod.invoke(service, 7L, request);
        replay = (Map<String, Object>) balanceMethod.invoke(service, 7L, replay);
        assertEquals("33", replay.get("id"));
        assertEquals("festival90", replay.get("templateId"));
        assertEquals("coupon", replay.get("kind"));
        assertEquals(320, ((Number) replay.get("pointBalance")).intValue());
        assertEquals("unused", replay.get("status"));

        Map<String, Object> first = new HashMap<>();
        first.put("id", "33");
        first = (Map<String, Object>) balanceMethod.invoke(service, 7L, first);
        assertEquals(320, ((Number) first.get("pointBalance")).intValue());
    }
}
