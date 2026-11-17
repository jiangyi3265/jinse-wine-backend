package com.ruoyi.web.controller.business;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.jdbc.core.JdbcTemplate;

/** Written in the order creation transaction. Missing rows identify legacy orders only. */
final class OrderPromotionSnapshot
{
    private OrderPromotionSnapshot() { }

    static void capture(JdbcTemplate jdbc, ObjectMapper json, long orderId, long buyerId)
    {
        ArrayNode recipients = json.createArrayNode();
        Map<String, Object> config = first(jdbc, "SELECT config_id,max_level,customer_rate_bp,staff_rate_bp,level_rates_json "
                + "FROM biz_promotion_config WHERE status='0' ORDER BY config_id LIMIT 1");
        if (config != null)
        {
            JsonNode rates;
            try { rates = json.readTree(String.valueOf(config.get("level_rates_json"))); }
            catch (Exception e) { rates = json.createArrayNode(); }
            if (rates == null) rates = json.createArrayNode();
            Set<Long> visited = new HashSet<>();
            visited.add(buyerId);
            long current = buyerId;
            int maxLevel = ((Number) config.get("max_level")).intValue();
            if (maxLevel < 1 || maxLevel > 10) throw invalid();
            for (int level = 1; level <= maxLevel; level++)
            {
                Map<String, Object> relation = first(jdbc, "SELECT inviter_member_id FROM biz_promotion_relation WHERE member_id=? AND status='0'", current);
                if (relation == null || relation.get("inviter_member_id") == null) break;
                long inviter = ((Number) relation.get("inviter_member_id")).longValue();
                if (inviter <= 0 || !visited.add(inviter)) throw invalid();
                Map<String, Object> promoter = first(jdbc, "SELECT staff_store_id FROM biz_member WHERE member_id=?", inviter);
                boolean staff = promoter != null && promoter.get("staff_store_id") != null;
                int rate = ((Number) config.get(staff ? "staff_rate_bp" : "customer_rate_bp")).intValue();
                if (rates.isObject())
                {
                    JsonNode schedule = rates.path(staff ? "staff" : "customer");
                    if (schedule.isArray() && schedule.size() >= level) rate = schedule.get(level - 1).asInt(rate);
                }
                else if (rates.isArray() && level > 1 && rates.size() >= level) rate = rates.get(level - 1).asInt(rate);
                if (rate < 0 || rate > 10000) throw invalid();
                recipients.addObject().put("memberId", inviter).put("level", level).put("rateBp", rate)
                        .put("role", staff ? "staff" : "customer");
                current = inviter;
            }
        }
        // An empty array is intentional: later binding/enabling must not earn commission on this order.
        jdbc.update("INSERT INTO biz_order_promotion_snapshot(order_id,snapshot_version,config_id,recipients_json) VALUES(?,1,?,?)",
                orderId, config == null ? null : config.get("config_id"), recipients.toString());
    }

    /** Returns false only for orders created before snapshots were introduced. */
    static boolean settleIfCaptured(JdbcTemplate jdbc, ObjectMapper json, long orderId)
    {
        Map<String, Object> snapshot = first(jdbc, "SELECT snapshot_version,recipients_json FROM biz_order_promotion_snapshot WHERE order_id=?", orderId);
        if (snapshot == null) return false;
        if (((Number) snapshot.get("snapshot_version")).intValue() != 1) throw invalid();
        JsonNode recipients;
        try { recipients = json.readTree(String.valueOf(snapshot.get("recipients_json"))); }
        catch (Exception e) { throw invalid(); }
        if (recipients == null || !recipients.isArray() || recipients.size() > 10) throw invalid();
        Map<String, Object> order = first(jdbc, "SELECT member_id,total_cent FROM biz_order WHERE order_id=? FOR UPDATE", orderId);
        if (order == null) throw invalid();
        Set<Long> visited = new HashSet<>();
        visited.add(((Number) order.get("member_id")).longValue());
        int expectedLevel = 1;
        for (JsonNode recipient : recipients)
        {
            if (!recipient.path("memberId").isIntegralNumber() || !recipient.path("memberId").canConvertToLong()
                    || recipient.path("memberId").asLong() <= 0 || !visited.add(recipient.path("memberId").asLong())
                    || !recipient.path("level").isIntegralNumber() || recipient.path("level").asLong() != expectedLevel++
                    || !recipient.path("rateBp").isIntegralNumber() || recipient.path("rateBp").asLong() < 0
                    || recipient.path("rateBp").asLong() > 10000) throw invalid();
        }
        long total = ((Number) order.get("total_cent")).longValue();
        for (JsonNode recipient : recipients)
        {
            long memberId = recipient.path("memberId").asLong();
            int amount = Math.toIntExact(Math.round(total * recipient.path("rateBp").asInt() / 10000d));
            if (amount > 0 && jdbc.queryForObject("SELECT COUNT(*) FROM biz_promotion_record WHERE order_id=? AND member_id=?",
                    Integer.class, orderId, memberId) == 0)
                jdbc.update("INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,status) VALUES(?,?,?,?,?,'pending')",
                        memberId, orderId, "订单确认收货" + recipient.path("level").asInt() + "级佣金", amount, amount);
        }
        return true;
    }

    private static Map<String, Object> first(JdbcTemplate jdbc, String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static IllegalStateException invalid()
    {
        return new IllegalStateException("订单返佣快照无效，需核对后处理");
    }
}
