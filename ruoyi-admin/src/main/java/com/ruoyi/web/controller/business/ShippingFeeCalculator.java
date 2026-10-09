package com.ruoyi.web.controller.business;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Shared integer-cent freight calculation for product previews and both checkout paths. */
final class ShippingFeeCalculator
{
    private ShippingFeeCalculator() { }

    static Map<String, Object> rule(JdbcTemplate jdbc, Object templateId)
    {
        String select = "SELECT template_id,template_name,free_threshold_cent,base_fee_cent,extra_fee_cent,extra_quantity FROM biz_shipping_template WHERE status='0'";
        if (templateId != null)
        {
            List<Map<String, Object>> rows = jdbc.queryForList(select + " AND template_id=?", templateId);
            if (!rows.isEmpty()) return rows.get(0);
        }
        List<Map<String, Object>> rows = jdbc.queryForList(select + " ORDER BY template_id LIMIT 1");
        return rows.isEmpty() ? null : rows.get(0);
    }

    static int amount(Map<String, Object> rule, long subtotal, long discount, long quantity)
    {
        if (rule == null || subtotal - discount >= number(rule.get("free_threshold_cent"))) return 0;
        long step = Math.max(1, number(rule.get("extra_quantity")));
        long extraGroups = (Math.max(0, quantity - 1) + step - 1) / step;
        long fee = number(rule.get("base_fee_cent")) + extraGroups * number(rule.get("extra_fee_cent"));
        if (fee < 0 || fee > Integer.MAX_VALUE) throw new MiniBusinessException(400, "运费金额超过上限");
        return (int) fee;
    }

    static int cart(JdbcTemplate jdbc, List<Map<String, Object>> items, int subtotal, int discount)
    {
        Map<Object, Map<String, Object>> rules = new LinkedHashMap<>();
        Map<Object, long[]> groups = new LinkedHashMap<>();
        Map<Object, Map<String, Object>> resolved = new LinkedHashMap<>();
        for (Map<String, Object> item : items)
        {
            Object selected = item.get("shipping_template_id");
            if (!resolved.containsKey(selected)) resolved.put(selected, rule(jdbc, selected));
            Map<String, Object> rule = resolved.get(selected);
            Object key = rule == null ? null : rule.get("template_id");
            rules.put(key, rule);
            long[] group = groups.computeIfAbsent(key, ignored -> new long[2]);
            group[0] += number(item.get("price_cent")) * number(item.get("quantity"));
            group[1] += number(item.get("quantity"));
        }
        // Allocate the coupon proportionally, using cumulative division to preserve every cent.
        long accumulated = 0, allocated = 0, shipping = 0;
        for (Map.Entry<Object, long[]> entry : groups.entrySet())
        {
            long[] group = entry.getValue();
            accumulated += group[0];
            long nextAllocated = subtotal == 0 ? 0 : (long) discount * accumulated / subtotal;
            shipping += amount(rules.get(entry.getKey()), group[0], nextAllocated - allocated, group[1]);
            allocated = nextAllocated;
        }
        if (shipping > Integer.MAX_VALUE) throw new MiniBusinessException(400, "运费金额超过上限");
        return (int) shipping;
    }

    private static long number(Object value)
    {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }
}
