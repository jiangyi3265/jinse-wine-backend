package com.ruoyi.web.controller.business;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import com.ruoyi.common.utils.SecurityUtils;

/** Decides which aggregate dashboard fields may leave the server. */
final class DashboardMetricAccess
{
    private DashboardMetricAccess() {}

    static Set<String> visibleMetrics(Collection<String> permissions)
    {
        Set<String> visible = new HashSet<>();
        if (SecurityUtils.hasPermi(permissions, "business:order:list")) visible.add("orders");
        if (SecurityUtils.hasPermi(permissions, "business:member:list")) visible.add("members");
        if (SecurityUtils.hasPermi(permissions, "business:lottery:list")) visible.add("draws");
        if (SecurityUtils.hasPermi(permissions, "business:redemption:list")) visible.add("redemptions");
        if (SecurityUtils.hasPermi(permissions, "business:settlement:list")
            || SecurityUtils.hasPermi(permissions, "business:report:export"))
        {
            visible.add("salesCent");
            visible.add("commissionCent");
        }
        return visible;
    }
}
