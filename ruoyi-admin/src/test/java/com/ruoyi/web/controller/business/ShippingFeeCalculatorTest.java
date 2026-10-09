package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class ShippingFeeCalculatorTest
{
    private JdbcTemplate database()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:shipping_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_shipping_template(template_id BIGINT PRIMARY KEY, template_name VARCHAR(64), "
                + "free_threshold_cent INT, base_fee_cent INT, extra_fee_cent INT, extra_quantity INT, status CHAR(1))");
        jdbc.update("INSERT INTO biz_shipping_template VALUES(1,'Default',10000,1000,200,2,'0'),"
                + "(2,'Alternate',20000,2000,500,1,'0'),(3,'Disabled',0,0,0,1,'1')");
        return jdbc;
    }

    private Map<String, Object> item(Object template, int price, int quantity)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("shipping_template_id", template);
        item.put("price_cent", price);
        item.put("quantity", quantity);
        return item;
    }

    @Test
    public void selectsProductTemplateAndFallsBackForMissingOrDisabledBindings()
    {
        JdbcTemplate jdbc = database();
        assertEquals(2L, ((Number) ShippingFeeCalculator.rule(jdbc, 2L).get("template_id")).longValue());
        for (Object id : new Object[] { null, 3L, 999L })
            assertEquals(1L, ((Number) ShippingFeeCalculator.rule(jdbc, id).get("template_id")).longValue());
    }

    @Test
    public void chargesFirstItemThenRoundsUpExtraGroups()
    {
        Map<String, Object> rule = ShippingFeeCalculator.rule(database(), 1L);
        assertEquals(1000, ShippingFeeCalculator.amount(rule, 100, 0, 1));
        assertEquals(1200, ShippingFeeCalculator.amount(rule, 200, 0, 2));
        assertEquals(1200, ShippingFeeCalculator.amount(rule, 300, 0, 3));
        assertEquals(1400, ShippingFeeCalculator.amount(rule, 400, 0, 4));
    }

    @Test
    public void evaluatesFreeThresholdAfterCouponDiscount()
    {
        Map<String, Object> rule = ShippingFeeCalculator.rule(database(), 1L);
        assertEquals(0, ShippingFeeCalculator.amount(rule, 10000, 0, 1));
        assertEquals(1000, ShippingFeeCalculator.amount(rule, 10000, 1, 1));
    }

    @Test
    public void zeroThresholdAndMissingRulesAreFree()
    {
        JdbcTemplate jdbc = database();
        jdbc.update("UPDATE biz_shipping_template SET free_threshold_cent=0 WHERE template_id=1");
        assertEquals(0, ShippingFeeCalculator.amount(ShippingFeeCalculator.rule(jdbc, 1L), 0, 0, 10));
        jdbc.update("DELETE FROM biz_shipping_template");
        assertNull(ShippingFeeCalculator.rule(jdbc, null));
        assertEquals(0, ShippingFeeCalculator.amount(null, 100, 0, 1));
    }

    @Test
    public void combinesQuantitiesWithinOneTemplate()
    {
        JdbcTemplate jdbc = database();
        assertEquals(1200, ShippingFeeCalculator.cart(jdbc,
                Arrays.asList(item(1L, 100, 1), item(1L, 200, 2)), 500, 0));
    }

    @Test
    public void addsFeesAcrossDifferentTemplates()
    {
        JdbcTemplate jdbc = database();
        assertEquals(3700, ShippingFeeCalculator.cart(jdbc,
                Arrays.asList(item(1L, 100, 2), item(2L, 200, 2)), 600, 0));
    }

    @Test
    public void mergesDefaultAndDisabledBindingsIntoTheEffectiveTemplate()
    {
        JdbcTemplate jdbc = database();
        assertEquals(1200, ShippingFeeCalculator.cart(jdbc,
                Arrays.asList(item(null, 100, 1), item(3L, 100, 1), item(1L, 100, 1)), 300, 0));
    }

    @Test
    public void checksFreeThresholdSeparatelyForEachTemplateGroup()
    {
        JdbcTemplate jdbc = database();
        assertEquals(2000, ShippingFeeCalculator.cart(jdbc,
                Arrays.asList(item(1L, 10000, 1), item(2L, 10000, 1)), 20000, 0));
    }

    @Test
    public void allocatesEveryDiscountCentBeforeCheckingGroupThresholds()
    {
        JdbcTemplate jdbc = database();
        jdbc.update("UPDATE biz_shipping_template SET free_threshold_cent=100 WHERE template_id IN(1,2)");
        assertEquals(2000, ShippingFeeCalculator.cart(jdbc,
                Arrays.asList(item(1L, 100, 1), item(2L, 100, 1)), 200, 1));
        assertEquals(3000, ShippingFeeCalculator.cart(jdbc,
                Arrays.asList(item(1L, 100, 1), item(2L, 100, 1)), 200, 2));
    }

    @Test
    public void rejectsFeeOverflow()
    {
        JdbcTemplate jdbc = database();
        jdbc.update("UPDATE biz_shipping_template SET base_fee_cent=?,extra_fee_cent=1,extra_quantity=1 WHERE template_id=1", Integer.MAX_VALUE);
        try
        {
            ShippingFeeCalculator.amount(ShippingFeeCalculator.rule(jdbc, 1L), 100, 0, 2);
            fail("Freight must fit in integer cents");
        }
        catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
    }
}
