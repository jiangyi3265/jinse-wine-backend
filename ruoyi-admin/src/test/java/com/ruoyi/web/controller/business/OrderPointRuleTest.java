package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class OrderPointRuleTest
{
    private final ObjectMapper json = new ObjectMapper();

    @Test
    public void earnsPerWholeYuanAndUsesDefaultRate() throws Exception
    {
        assertEquals(1, OrderPointRule.pointsPerYuan(json.readTree("{}")));
        assertEquals(10, OrderPointRule.points(1099, 1));
        assertEquals(30, OrderPointRule.points(1099, OrderPointRule.pointsPerYuan(json.readTree("{\"per_cent\":3}"))));
        assertEquals(0, OrderPointRule.points(99, 1000));
    }

    @Test
    public void rejectsInvalidRates() throws Exception
    {
        for (String raw : new String[] { "[]", "{\"per_cent\":-1}", "{\"per_cent\":1001}",
                "{\"per_cent\":0.5}", "{\"per_cent\":\"2\"}" })
        {
            try { OrderPointRule.pointsPerYuan(json.readTree(raw)); fail(raw); }
            catch (IllegalArgumentException expected) { }
        }
    }

}
