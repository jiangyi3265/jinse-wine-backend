package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class DashboardMetricAccessTest
{
    @Test
    public void operatorCannotReadFinancialAggregatesThroughPromotionPermission()
    {
        Set<String> metrics = DashboardMetricAccess.visibleMetrics(Arrays.asList(
            "business:dashboard:query", "business:order:list", "business:member:list",
            "business:lottery:list", "business:redemption:list", "business:promotion:query"));
        assertEquals(4, metrics.size());
        assertTrue(metrics.contains("orders"));
        assertTrue(metrics.contains("members"));
        assertTrue(metrics.contains("draws"));
        assertTrue(metrics.contains("redemptions"));
        assertFalse(metrics.contains("salesCent"));
        assertFalse(metrics.contains("commissionCent"));
    }

    @Test
    public void financeAndSuperAdminCanReadFinancialAggregates()
    {
        Set<String> finance = DashboardMetricAccess.visibleMetrics(Arrays.asList(
            "business:dashboard:query", "business:order:list", "business:settlement:list"));
        assertTrue(finance.contains("salesCent"));
        assertTrue(finance.contains("commissionCent"));
        assertFalse(finance.contains("draws"));

        Set<String> admin = DashboardMetricAccess.visibleMetrics(Collections.singleton("*:*:*"));
        assertEquals(6, admin.size());
    }

    @Test
    public void dashboardPermissionAloneDoesNotGrantBusinessMetrics()
    {
        assertTrue(DashboardMetricAccess.visibleMetrics(Collections.singleton("business:dashboard:query")).isEmpty());
    }
}
