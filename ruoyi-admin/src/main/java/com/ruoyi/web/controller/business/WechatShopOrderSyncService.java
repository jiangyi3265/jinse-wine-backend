package com.ruoyi.web.controller.business;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Imports a privacy-minimized, read-only mirror of WeChat Shop orders. */
@Service
public class WechatShopOrderSyncService
{
    private static final long MAX_RANGE_SECONDS = 7L * 24 * 60 * 60;
    private static final long OVERLAP_SECONDS = 120L;
    private static final int PAGE_SIZE = 30;

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final WechatShopApiService wechatShopApi;

    public WechatShopOrderSyncService(JdbcTemplate jdbc, ObjectMapper json, WechatShopApiService wechatShopApi)
    {
        this.jdbc = jdbc;
        this.json = json;
        this.wechatShopApi = wechatShopApi;
    }

    @Transactional
    public Map<String, Object> syncOnePage()
    {
        if (!enabled()) throw new MiniBusinessException(409, "请先开启微信小店数据同步");
        String shopId = config("wechat.video.shopId");
        if (shopId.isEmpty()) throw new MiniBusinessException(409, "请先配置微信小店 ID 并完成授权校验");
        wechatShopApi.checkShop(shopId);

        Map<String, Object> state = jdbc.queryForMap("SELECT completed_through,window_start,window_end,next_key FROM biz_video_order_sync_state WHERE sync_key='orders' FOR UPDATE");
        long now = Instant.now().getEpochSecond();
        long completedThrough = number(state.get("completed_through"));
        long start = number(state.get("window_start"));
        long end = number(state.get("window_end"));
        String nextKey = text(state.get("next_key"));
        if (end <= 0)
        {
            start = completedThrough > 0 ? Math.max(0, completedThrough - OVERLAP_SECONDS) : Math.max(0, now - MAX_RANGE_SECONDS);
            end = Math.min(now, start + MAX_RANGE_SECONDS);
            nextKey = "";
            jdbc.update("UPDATE biz_video_order_sync_state SET window_start=?,window_end=?,next_key='' WHERE sync_key='orders'", start, end);
        }

        JsonNode list = wechatShopApi.listOrders(start, end, nextKey, PAGE_SIZE);
        JsonNode ids = list.path("order_id_list");
        if (!ids.isArray()) throw new MiniBusinessException(502, "微信小店订单列表没有返回订单 ID 列表");
        int imported = 0;
        for (JsonNode id : ids)
        {
            String orderId = id.asText("");
            if (orderId.isEmpty()) continue;
            JsonNode response = wechatShopApi.getOrder(orderId);
            JsonNode order = response.path("order");
            if (order.isMissingNode() || order.isNull()) throw new MiniBusinessException(502, "微信小店订单详情为空，订单号=" + orderId);
            saveOrder(orderId, order);
            imported++;
        }

        boolean hasMore = list.path("has_more").asBoolean(false);
        String responseNextKey = text(list.path("next_key").asText(""));
        boolean complete = !hasMore;
        if (hasMore && responseNextKey.isEmpty()) throw new MiniBusinessException(502, "微信小店订单分页未返回 next_key");
        if (complete)
        {
            jdbc.update("UPDATE biz_video_order_sync_state SET completed_through=?,window_start=0,window_end=0,next_key='' WHERE sync_key='orders'", end);
        }
        else
        {
            jdbc.update("UPDATE biz_video_order_sync_state SET next_key=? WHERE sync_key='orders'", responseNextKey);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("hasMore", hasMore);
        result.put("windowStart", start);
        result.put("windowEnd", end);
        result.put("message", hasMore ? "本页订单已同步，剩余订单将继续分页处理" : "微信小店订单增量同步完成");
        return result;
    }

    public Map<String, Object> listOrders(int pageNum, int pageSize)
    {
        int size = Math.max(1, Math.min(pageSize, 200));
        int page = Math.max(1, Math.min(pageNum, 100000));
        long offset = (long) (page - 1) * size;
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT o.external_order_id AS externalOrderId,o.external_status AS externalStatus,o.created_at AS createdAt,o.updated_at AS updatedAt,o.order_price_cent AS orderPriceCent,o.freight_cent AS freightCent,o.merchant_receive_cent AS merchantReceiveCent,o.aftersale_count AS aftersaleCount,o.items_json AS itemsJson,o.last_synced_at AS lastSyncedAt,f.status AS fulfillmentStatus,f.last_error AS fulfillmentError FROM biz_video_order o LEFT JOIN biz_video_order_fulfillment f ON f.external_order_id=o.external_order_id ORDER BY o.updated_at DESC,o.order_id DESC LIMIT ? OFFSET ?", size, offset);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM biz_video_order", Long.class);
        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("rows", rows);
        pageResult.put("total", total);
        pageResult.put("pageNum", page);
        pageResult.put("pageSize", size);
        return pageResult;
    }

    @Transactional
    public Map<String, Object> shipOrder(String externalOrderId, String deliveryId, String waybillId)
    {
        if (externalOrderId == null || externalOrderId.trim().isEmpty()) throw new MiniBusinessException(400, "微信小店订单号无效");
        if (deliveryId == null || !deliveryId.matches("[A-Za-z0-9_-]{1,64}")) throw new MiniBusinessException(400, "请输入微信支持的快递公司 ID");
        if (waybillId == null || !waybillId.trim().matches("[A-Za-z0-9_-]{3,128}")) throw new MiniBusinessException(400, "运单号格式无效");
        if (!enabled()) throw new MiniBusinessException(409, "请先开启微信小店数据同步");
        String shopId = config("wechat.video.shopId");
        if (shopId.isEmpty()) throw new MiniBusinessException(409, "请先配置微信小店 ID 并完成授权校验");
        wechatShopApi.checkShop(shopId);

        List<Map<String, Object>> orderRows = jdbc.queryForList("SELECT external_status,items_json FROM biz_video_order WHERE external_order_id=? FOR UPDATE", externalOrderId.trim());
        if (orderRows.isEmpty()) throw new MiniBusinessException(404, "微信小店订单不存在，请先同步订单");
        Map<String, Object> order = orderRows.get(0);
        long externalStatus = number(order.get("external_status"));
        if (externalStatus != 20) throw new MiniBusinessException(409, "仅微信小店待发货订单可以发货，请先同步最新平台状态");

        Map<String, Object> prior = jdbc.queryForList("SELECT status,update_time,waybill_id FROM biz_video_order_fulfillment WHERE external_order_id=? FOR UPDATE", externalOrderId.trim())
                .stream().findFirst().orElse(null);
        if (prior != null)
        {
            String priorStatus = text(prior.get("status"));
            if ("submitted".equals(priorStatus) || "verified".equals(priorStatus) || "submitting".equals(priorStatus))
                throw new MiniBusinessException(409, "该订单已有发货请求，请同步平台状态后再处理");
            if ("unknown".equals(priorStatus))
            {
                java.sql.Timestamp updated = (java.sql.Timestamp) prior.get("update_time");
                if (updated == null || updated.toInstant().getEpochSecond() > Instant.now().getEpochSecond() - 300)
                    throw new MiniBusinessException(409, "上次发货结果未知，请等待平台状态同步后再重试");
                JsonNode latest = wechatShopApi.getOrder(externalOrderId.trim()).path("order");
                if (latest.path("status").asInt(-1) != 20)
                {
                    jdbc.update("UPDATE biz_video_order_fulfillment SET status='verified',last_error='' WHERE external_order_id=?", externalOrderId.trim());
                    saveOrder(externalOrderId.trim(), latest);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("status", "verified");
                    result.put("message", "微信小店订单状态已变化，请勿重复发货");
                    return result;
                }
            }
        }

        JsonNode itemNodes = readItems(order.get("items_json"));
        if (!itemNodes.isArray() || itemNodes.isEmpty()) throw new MiniBusinessException(409, "订单缺少可发货商品明细");
        List<Map<String, Object>> productInfos = new ArrayList<>();
        for (JsonNode item : itemNodes)
        {
            String productId = item.path("productId").asText("");
            String skuId = item.path("skuId").asText("");
            int quantity = item.path("quantity").asInt(0);
            if (productId.isEmpty() || skuId.isEmpty() || quantity <= 0)
                throw new MiniBusinessException(409, "订单商品缺少微信小店商品/SKU 映射或数量无效，不能发货");
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("product_id", productId);
            product.put("sku_id", skuId);
            product.put("product_cnt", quantity);
            productInfos.add(product);
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        if (prior == null)
            jdbc.update("INSERT INTO biz_video_order_fulfillment(external_order_id,delivery_id,waybill_id,request_id,status) VALUES(?,?,?,?,'submitting')", externalOrderId.trim(), deliveryId, waybillId.trim(), requestId);
        else
            jdbc.update("UPDATE biz_video_order_fulfillment SET delivery_id=?,waybill_id=?,request_id=?,status='submitting',last_error='' WHERE external_order_id=?", deliveryId, waybillId.trim(), requestId, externalOrderId.trim());

        Map<String, Object> delivery = new LinkedHashMap<>();
        delivery.put("deliver_type", 1);
        delivery.put("delivery_id", deliveryId);
        delivery.put("waybill_id", waybillId.trim());
        delivery.put("product_infos", productInfos);
        delivery.put("is_all_product", true);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("order_id", externalOrderId.trim());
        request.put("delivery_list", java.util.Collections.singletonList(delivery));
        try
        {
            wechatShopApi.sendOrderDelivery(request);
            jdbc.update("UPDATE biz_video_order_fulfillment SET status='submitted',last_error='' WHERE external_order_id=? AND request_id=?", externalOrderId.trim(), requestId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "submitted");
            result.put("message", "微信已接受发货请求，订单平台状态稍后同步更新");
            return result;
        }
        catch (MiniBusinessException e)
        {
            jdbc.update("UPDATE biz_video_order_fulfillment SET status='unknown',last_error=? WHERE external_order_id=? AND request_id=?", limit(e.getMessage(), 255), externalOrderId.trim(), requestId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "unknown");
            result.put("message", "发货请求结果未知，请等待同步平台状态后再决定是否重试");
            return result;
        }
    }

    @Transactional
    public Map<String, Object> changeOrderPrice(String externalOrderId, Map<String, Object> input)
    {
        if (externalOrderId == null || externalOrderId.trim().isEmpty()) throw new MiniBusinessException(400, "微信小店订单号无效");
        if (!enabled()) throw new MiniBusinessException(409, "请先开启微信小店数据同步");
        String shopId = config("wechat.video.shopId");
        if (shopId.isEmpty()) throw new MiniBusinessException(409, "请先配置微信小店 ID 并完成授权校验");
        wechatShopApi.checkShop(shopId);

        List<Map<String, Object>> orderRows = jdbc.queryForList("SELECT external_status,items_json FROM biz_video_order WHERE external_order_id=? FOR UPDATE", externalOrderId.trim());
        if (orderRows.isEmpty()) throw new MiniBusinessException(404, "微信小店订单不存在，请先同步订单");
        Map<String, Object> order = orderRows.get(0);
        if (number(order.get("external_status")) != 10) throw new MiniBusinessException(409, "仅待付款微信小店订单可改价，请先同步最新平台状态");

        JsonNode originalItems = readItems(order.get("items_json"));
        Object changesValue = input.get("items");
        JsonNode changes = changesValue instanceof String ? readItems(changesValue) : json.valueToTree(changesValue);
        if (!originalItems.isArray() || !changes.isArray() || originalItems.size() != changes.size())
            throw new MiniBusinessException(400, "改价明细必须覆盖订单中的全部商品行");

        List<Map<String, Object>> priceInfos = new ArrayList<>();
        java.util.Set<String> changedSkus = new java.util.HashSet<>();
        for (JsonNode original : originalItems)
        {
            String productId = original.path("productId").asText("");
            String skuId = original.path("skuId").asText("");
            String key = productId + ":" + skuId;
            JsonNode change = null;
            for (JsonNode candidate : changes)
            {
                if (productId.equals(candidate.path("productId").asText("")) && skuId.equals(candidate.path("skuId").asText("")))
                {
                    change = candidate;
                    break;
                }
            }
            if (change == null || !changedSkus.add(key)) throw new MiniBusinessException(400, "改价商品与订单商品行不匹配");
            long amount = change.path("changePriceCent").asLong(-1);
            if (productId.isEmpty() || skuId.isEmpty() || amount < 0 || amount > Integer.MAX_VALUE)
                throw new MiniBusinessException(400, "商品改价金额无效或缺少商品映射");
            Map<String, Object> price = new LinkedHashMap<>();
            price.put("product_id", productId);
            price.put("sku_id", skuId);
            price.put("change_price", amount);
            priceInfos.add(price);
        }

        boolean changeExpress = Boolean.TRUE.equals(input.get("changeExpress"));
        long expressFee = number(input.get("expressFeeCent"));
        if (changeExpress && (expressFee < 0 || expressFee > Integer.MAX_VALUE)) throw new MiniBusinessException(400, "修改后的运费金额无效");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("order_id", externalOrderId.trim());
        request.put("change_express", changeExpress);
        if (changeExpress) request.put("express_fee", expressFee);
        request.put("change_order_infos", priceInfos);
        wechatShopApi.changeOrderPrice(request);

        JsonNode latest = null;
        try
        {
            latest = wechatShopApi.getOrder(externalOrderId.trim()).path("order");
            if (!latest.isMissingNode() && !latest.isNull()) saveOrder(externalOrderId.trim(), latest);
        }
        catch (MiniBusinessException ignored)
        {
            // The price update was accepted; a later scheduled sync will refresh the mirror.
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "submitted");
        result.put("message", latest == null || latest.isMissingNode() || latest.isNull() ? "微信已接受改价，但回读暂不可用，请稍后同步订单核对" : "微信已接受改价，订单摘要已回读更新");
        return result;
    }

    private void saveOrder(String orderId, JsonNode order)
    {
        JsonNode detail = order.path("order_detail");
        JsonNode prices = detail.path("price_info");
        JsonNode items = detail.path("product_infos");
        List<Map<String, Object>> safeItems = new ArrayList<>();
        if (items.isArray())
        {
            for (JsonNode item : items)
            {
                Map<String, Object> safe = new LinkedHashMap<>();
                safe.put("productId", item.path("product_id").asText(""));
                safe.put("skuId", item.path("sku_id").asText(""));
                safe.put("skuCode", item.path("sku_code").asText(""));
                safe.put("title", item.path("title").asText(""));
                safe.put("quantity", item.path("sku_cnt").asInt(0));
                safe.put("unitPriceCent", item.path("sale_price").asLong(0));
                safe.put("realPriceCent", item.path("real_price").asLong(0));
                safeItems.add(safe);
            }
        }
        int aftersaleCount = order.path("aftersale_detail").path("aftersale_order_list").isArray()
                ? order.path("aftersale_detail").path("aftersale_order_list").size() : 0;
        long created = order.path("create_time").asLong(0);
        long updated = order.path("update_time").asLong(0);
        jdbc.update("INSERT INTO biz_video_order(external_order_id,external_status,created_at,updated_at,order_price_cent,freight_cent,merchant_receive_cent,aftersale_count,items_json,last_synced_at) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE external_status=VALUES(external_status),created_at=VALUES(created_at),updated_at=VALUES(updated_at),order_price_cent=VALUES(order_price_cent),freight_cent=VALUES(freight_cent),merchant_receive_cent=VALUES(merchant_receive_cent),aftersale_count=VALUES(aftersale_count),items_json=VALUES(items_json),last_synced_at=NOW()",
                orderId, order.path("status").asInt(0), timestamp(created), timestamp(updated), prices.path("order_price").asLong(0),
                prices.path("freight").asLong(0), prices.path("merchant_receieve_price").asLong(0), aftersaleCount, toJson(safeItems));
        if (order.path("status").asInt(0) != 20)
            jdbc.update("UPDATE biz_video_order_fulfillment SET status='verified',last_error='' WHERE external_order_id=? AND status IN ('submitted','unknown')", orderId);
    }

    private String toJson(Object value)
    {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new MiniBusinessException(500, "微信小店订单摘要序列化失败"); }
    }

    private JsonNode readItems(Object value)
    {
        try { return value instanceof String ? json.readTree((String) value) : json.valueToTree(value); }
        catch (Exception e) { throw new MiniBusinessException(500, "微信小店订单商品摘要无效"); }
    }

    private boolean enabled()
    {
        return "true".equalsIgnoreCase(config("wechat.video.syncEnabled"));
    }

    private String config(String key)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT config_value FROM biz_channel_config WHERE config_key=?", key);
        return rows.isEmpty() ? "" : text(rows.get(0).get("config_value"));
    }

    private static java.sql.Timestamp timestamp(long epochSeconds)
    {
        return epochSeconds <= 0 ? null : new java.sql.Timestamp(epochSeconds * 1000L);
    }

    private static long number(Object value)
    {
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception e) { return 0; }
    }

    private static String text(Object value)
    {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String limit(String value, int max)
    {
        String safe = value == null ? "" : value;
        return safe.length() <= max ? safe : safe.substring(0, max);
    }
}
