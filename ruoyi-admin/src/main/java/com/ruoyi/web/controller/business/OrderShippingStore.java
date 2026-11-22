package com.ruoyi.web.controller.business;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

final class OrderShippingStore
{
    private static final String DEFAULT_CODE_KEY = "wine.shipping.defaultStoreCode";

    private OrderShippingStore() {}

    static Map<String, Object> defaultStore(JdbcTemplate jdbc)
    {
        List<String> configs = jdbc.queryForList("SELECT config_value FROM sys_config WHERE config_key=? LIMIT 2",
                String.class, DEFAULT_CODE_KEY);
        if (configs.size() > 1) return null;
        String code = configs.isEmpty() || configs.get(0) == null ? "" : configs.get(0).trim();
        String condition = code.isEmpty() ? "store_name=?" : "store_code=?";
        List<Map<String, Object>> stores = jdbc.queryForList("SELECT store_id,store_name FROM biz_store WHERE status='0' AND "
                + condition + " LIMIT 2", code.isEmpty() ? "国大门市" : code);
        return stores.size() == 1 ? stores.get(0) : null;
    }

    static void assignNewOrder(JdbcTemplate jdbc, long orderId, String mode)
    {
        if (!"delivery".equals(mode)) return;
        Map<String, Object> store = defaultStore(jdbc);
        if (store != null) assign(jdbc, orderId, null, null, store, "系统默认分配");
    }

    static void assign(JdbcTemplate jdbc, long orderId, Object previousId, Object previousName,
            Map<String, Object> store, String operator)
    {
        jdbc.update("UPDATE biz_order SET shipping_store_id=?,shipping_store_name=?,update_time=NOW() WHERE order_id=?",
                store.get("store_id"), store.get("store_name"), orderId);
        jdbc.update("INSERT INTO biz_order_store_assignment(order_id,previous_store_id,previous_store_name,store_id,store_name,operator_name) VALUES(?,?,?,?,?,?)",
                orderId, previousId, previousName, store.get("store_id"), store.get("store_name"), operator);
    }
}
