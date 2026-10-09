package com.ruoyi.web.controller.business;

import com.fasterxml.jackson.databind.JsonNode;

/** Shared earning rate for delivery confirmation, store pickup, and admin fulfillment. */
final class OrderPointRule
{
    static final int MAX_POINTS_PER_YUAN = 1000;

    private OrderPointRule() { }

    static int pointsPerYuan(JsonNode rule)
    {
        if (rule == null || !rule.isObject()) throw new IllegalArgumentException("订单积分扩展参数必须是 JSON 对象");
        JsonNode rate = rule.get("per_cent");
        if (rate == null) return 1;
        if (!rate.isIntegralNumber() || !rate.canConvertToInt()
                || rate.intValue() < 0 || rate.intValue() > MAX_POINTS_PER_YUAN)
            throw new IllegalArgumentException("每消费1元积分必须是0至1000的整数");
        return rate.intValue();
    }

    static int points(long totalCent, int pointsPerYuan)
    {
        long points = totalCent / 100 * pointsPerYuan;
        if (points > Integer.MAX_VALUE) throw new MiniBusinessException(409, "订单积分超过账户上限");
        return (int) points;
    }

}
