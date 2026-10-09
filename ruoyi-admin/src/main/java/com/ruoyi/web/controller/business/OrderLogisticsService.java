package com.ruoyi.web.controller.business;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Courier progress never directly completes or settles an order. */
@Service
public class OrderLogisticsService
{
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final WechatLogisticsClient client;
    @Value("${logistics.wechat.enabled:${WECHAT_LOGISTICS_ENABLED:false}}") private boolean enabled;
    @Value("${mini.public-base-url:https://jiuyekl.oksja.cn}") private String publicBase;

    public OrderLogisticsService(JdbcTemplate jdbc, ObjectMapper json, TransactionTemplate transactions, WechatLogisticsClient client)
    {
        this.jdbc = jdbc; this.json = json; this.transactions = transactions; this.client = client;
    }

    public List<Map<String, String>> companyOptions()
    {
        if (enabled && client.configured()) return client.companies();
        return List.of(Map.of("code", "SF", "name", "顺丰速运"), Map.of("code", "ZTO", "name", "中通快递"),
                Map.of("code", "YTO", "name", "圆通速递"), Map.of("code", "YD", "name", "韵达快递"),
                Map.of("code", "STO", "name", "申通快递"), Map.of("code", "EMS", "name", "EMS"));
    }

    public static String companyCode(String code, String name)
    {
        if (code == null || code.isBlank()) return "";
        if (!code.matches("[A-Za-z0-9_().-]{1,40}")) throw new IllegalArgumentException("快递公司编码无效");
        return code;
    }

    public Map<String, Object> delivery(long orderId, boolean refresh)
    {
        Map<String, Object> row = first("SELECT d.company,d.company_code AS companyCode,d.tracking_no AS trackingNo,d.status,"
                + "d.last_trace AS lastTrace,d.shipped_at AS shippedAt,d.last_synced_at AS lastSyncedAt,"
                + "d.sync_error AS syncError,d.signed_at AS signedAt FROM biz_order_delivery d WHERE d.order_id=?", orderId);
        if (row == null) return null;
        boolean ready = enabled && client.configured();
        if (refresh && ready && !blank(row.get("trackingNo")) && !"signed".equals(row.get("status")))
        {
            // Database lease throttles scheduled sync and concurrent views across instances.
            int claimed = jdbc.update("UPDATE biz_order_delivery d JOIN biz_order o ON o.order_id=d.order_id "
                    + "SET d.next_query_at=DATE_ADD(NOW(),INTERVAL 30 MINUTE) WHERE d.order_id=? AND d.tracking_no=? "
                    + "AND o.fulfillment_mode='delivery' AND o.status IN ('shipping','receiving') "
                    + "AND (d.next_query_at IS NULL OR d.next_query_at<=NOW())", orderId, row.get("trackingNo"));
            if (claimed > 0) { synchronize(orderId, row); return delivery(orderId, false); }
        }
        row.put("trackingEnabled", ready);
        row.put("trackingAvailable", ready);
        return row;
    }

    public void synchronizeDue()
    {
        if (!enabled || !client.configured()) return;
        for (Map<String, Object> row : jdbc.queryForList("SELECT d.order_id FROM biz_order_delivery d JOIN biz_order o ON o.order_id=d.order_id "
                + "WHERE o.fulfillment_mode='delivery' AND o.status IN ('shipping','receiving') AND d.tracking_no<>'' "
                + "AND d.status<>'signed' AND (d.next_query_at IS NULL OR d.next_query_at<=NOW()) "
                + "ORDER BY COALESCE(d.next_query_at,d.shipped_at),d.order_id LIMIT 10"))
            delivery(((Number) row.get("order_id")).longValue(), true);
    }

    private void synchronize(long orderId, Map<String, Object> delivery)
    {
        try
        {
            Map<String, Object> order = first("SELECT o.order_no,o.address_snapshot,o.wechat_transaction_id,m.wechat_openid AS openid "
                    + "FROM biz_order o JOIN biz_member m ON m.member_id=o.member_id WHERE o.order_id=?", orderId);
            if (order == null || blank(order.get("wechat_transaction_id")))
                throw new MiniBusinessException(409, "订单缺少微信支付流水号，暂不能绑定微信物流查询");
            JsonNode address = json.readTree(String.valueOf(order.get("address_snapshot")));
            Map<String, Object> stored = first("SELECT waybill_token FROM biz_order_delivery WHERE order_id=? AND company_code=? AND tracking_no=?",
                    orderId, delivery.get("companyCode"), delivery.get("trackingNo"));
            if (stored == null) return;
            String waybillToken = stored == null || blank(stored.get("waybill_token")) ? "" : String.valueOf(stored.get("waybill_token"));
            if (waybillToken.isBlank())
            {
                List<Map<String, String>> goods = new ArrayList<>();
                for (Map<String, Object> item : jdbc.queryForList("SELECT product_name,image_url FROM biz_order_item WHERE order_id=? ORDER BY order_item_id LIMIT 20", orderId))
                {
                    String image = blank(item.get("image_url")) ? "/mini-assets/design/product-placeholder.jpg" : String.valueOf(item.get("image_url"));
                    if (image.startsWith("/")) image = publicBase.replaceAll("/$", "") + image;
                    if (!image.startsWith("https://")) throw new MiniBusinessException(409, "商品图片需为HTTPS地址才能绑定微信物流");
                    goods.add(Map.of("goods_name", String.valueOf(item.get("product_name")), "goods_img_url", image));
                }
                Map<String, Object> request = new LinkedHashMap<>();
                request.put("openid", order.get("openid"));
                request.put("receiver_phone", address.path("phone").asText());
                request.put("waybill_id", delivery.get("trackingNo"));
                if (!blank(delivery.get("companyCode"))) request.put("delivery_id", delivery.get("companyCode"));
                request.put("goods_info", Map.of("detail_list", goods));
                request.put("trans_id", order.get("wechat_transaction_id"));
                request.put("order_detail_path", "pages/order-detail/index?id=" + order.get("order_no"));
                waybillToken = client.post("trace_waybill", request).path("waybill_token").asText();
                if (waybillToken.isBlank()) throw new MiniBusinessException(502, "微信未返回物流查询凭据");
                if (jdbc.update("UPDATE biz_order_delivery SET waybill_token=? WHERE order_id=? AND company_code=? AND tracking_no=?",
                        waybillToken, orderId, delivery.get("companyCode"), delivery.get("trackingNo")) == 0) return;
            }
            JsonNode response = client.post("query_trace", Map.of("waybill_token", waybillToken, "openid", order.get("openid")));
            JsonNode info = response.path("waybill_info");
            if (!String.valueOf(delivery.get("trackingNo")).equals(info.path("waybill_id").asText()))
                throw new MiniBusinessException(502, "微信返回的运单不匹配，已保留原物流信息");
            String normalized = normalizeStatus(info.path("status").asInt(-1));
            if (normalized == null) throw new MiniBusinessException(502, "微信返回未知物流状态，已保留原物流信息");
            // WeChat supplies a status snapshot without a signing timestamp.
            // First confirmed observation starts the timer, never an inferred courier date.
            String snapshot = json.writeValueAsString(List.of(Map.of("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "description", "微信物流查询：" + statusLabel(normalized))));
            transactions.executeWithoutResult(tx -> {
                first("SELECT order_id FROM biz_order WHERE order_id=? FOR UPDATE", orderId);
                jdbc.update("UPDATE biz_order_delivery SET status=?,last_trace=?,last_synced_at=NOW(),sync_error=NULL,"
                        + "signed_at=CASE WHEN ?='signed' THEN COALESCE(signed_at,NOW()) ELSE NULL END "
                        + "WHERE order_id=? AND company_code=? AND tracking_no=?", normalized, snapshot, normalized,
                        orderId, delivery.get("companyCode"), delivery.get("trackingNo"));
            });
        }
        catch (Exception error)
        {
            String message = error instanceof MiniBusinessException ? error.getMessage() : "微信物流查询暂时不可用，已保留上次结果";
            jdbc.update("UPDATE biz_order_delivery SET sync_error=? WHERE order_id=? AND company_code=? AND tracking_no=?",
                    message, orderId, delivery.get("companyCode"), delivery.get("trackingNo"));
        }
    }

    static String normalizeStatus(int status)
    {
        switch (status)
        {
            case 0: return "awaiting_collect";
            case 1: return "collected";
            case 2: return "in_transit";
            case 3: return "out_for_delivery";
            case 4: return "signed";
            case 5: return "exception";
            case 6: return "agent_signed";
            default: return null;
        }
    }
    static String statusLabel(String status)
    {
        switch (status)
        {
            case "awaiting_collect": return "已发货，待揽收";
            case "collected": return "已揽收";
            case "in_transit": return "运输中";
            case "out_for_delivery": return "派送中";
            case "signed": return "已签收";
            case "agent_signed": return "已代签收，请核对取件";
            default: return "物流异常";
        }
    }
    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }
    private static boolean blank(Object value) { return value == null || String.valueOf(value).isBlank(); }
}
