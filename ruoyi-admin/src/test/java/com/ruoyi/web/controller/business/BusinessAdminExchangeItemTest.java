package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class BusinessAdminExchangeItemTest
{
    @Test
    public void validatesExchangeFieldsAndPreservesUsedCodes() throws Exception
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:admin_exchange_item;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_exchange_item (exchange_item_id BIGINT PRIMARY KEY, exchange_code VARCHAR(64), item_type VARCHAR(16), coupon_template_id BIGINT, redeem_store_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_coupon_template (coupon_template_id BIGINT PRIMARY KEY, status VARCHAR(1))");
        jdbc.execute("CREATE TABLE biz_point_record (source_type VARCHAR(32), source_id VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_mini_idempotency (action_type VARCHAR(32), payload_hash CHAR(64), "
                + "reward_id BIGINT, member_coupon_id BIGINT)");
        jdbc.update("INSERT INTO biz_exchange_item VALUES(1,'CUP_1','goods',NULL,5)");
        BusinessAdminController controller = new BusinessAdminController(jdbc, new BusinessDataScopeService(jdbc));

        assertNull(validate(controller, fields("exchange_code", "CUP_2"), 1L));
        assertNotNull(validate(controller, fields("exchange_code", "bad code"), 1L));
        assertNotNull(validate(controller, fields("item_type", "cash"), 1L));
        assertNotNull(validate(controller, fields("points", 0), 1L));
        assertNotNull(validate(controller, fields("stock", -2), 1L));
        assertNotNull(validate(controller, fields("per_user_limit", 0), 1L));
        assertNull(validate(controller, fields("stock", -1), 1L));
        assertEquals("请选择有效的优惠券模板", validate(controller, fields("item_type", "coupon"), null));
        Map<String, Object> coupon = fields("item_type", "coupon");
        coupon.put("coupon_template_id", 9L);
        assertEquals("优惠券模板不存在或已停用", validate(controller, coupon, null));
        jdbc.update("INSERT INTO biz_coupon_template VALUES(9,'0')");
        assertNull(validate(controller, coupon, null));
        assertNull(coupon.get("redeem_store_id"));
        Map<String, Object> switched = fields("item_type", "coupon");
        switched.put("coupon_template_id", 9L);
        assertNull(validate(controller, switched, 1L));
        assertNull(switched.get("redeem_store_id"));
        switched.put("redeem_store_id", 5L);
        assertEquals("优惠券兑换项不能指定兑换门店", validate(controller, switched, 1L));
        Map<String, Object> goods = fields("item_type", "goods");
        goods.put("coupon_template_id", 9L);
        assertNull(validate(controller, goods, 1L));
        assertNull(goods.get("coupon_template_id"));

        jdbc.update("INSERT INTO biz_mini_idempotency VALUES('exchange',?,11,NULL)",
                MiniMemberAuthService.sha256("exchange|CUP_1"));
        assertEquals("已有兑换记录的兑换编码不能修改，请新建兑换项",
                validate(controller, fields("exchange_code", "CUP_2"), 1L));
        assertNull(validate(controller, fields("exchange_code", "CUP_1"), 1L));

        jdbc.update("DELETE FROM biz_mini_idempotency");
        jdbc.update("INSERT INTO biz_point_record VALUES('exchange','CUP_1')");
        assertEquals("已有兑换记录的兑换编码不能修改，请新建兑换项",
                validate(controller, fields("exchange_code", "CUP_2"), 1L));
    }

    private static Map<String, Object> fields(String key, Object value)
    {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(key, value);
        return fields;
    }

    private static String validate(BusinessAdminController controller, Map<String, Object> values, Long id)
            throws Exception
    {
        Method method = BusinessAdminController.class.getDeclaredMethod("validateExchangeItem",
                String.class, Map.class, Long.class);
        method.setAccessible(true);
        return (String) method.invoke(controller, "exchangeItem", values, id);
    }
}
