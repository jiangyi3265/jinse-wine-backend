package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reconciles local SKU inventory to the mapped WeChat Shop SKU. */
@Service
public class WechatShopStockSyncService
{
    private final JdbcTemplate jdbc;
    private final WechatShopApiService wechatShopApi;

    public WechatShopStockSyncService(JdbcTemplate jdbc, WechatShopApiService wechatShopApi)
    {
        this.jdbc = jdbc;
        this.wechatShopApi = wechatShopApi;
    }

    @Transactional
    public Map<String, Object> syncProduct(long productId, boolean force)
    {
        if (!enabled()) throw new MiniBusinessException(409, "请先开启视频号库存同步");
        String shopId = config("wechat.video.shopId");
        if (shopId.isEmpty()) throw new MiniBusinessException(409, "请先配置微信小店 ID 并完成授权校验");
        wechatShopApi.checkShop(shopId);

        Map<String, Object> mapping = first("SELECT external_id FROM biz_video_product WHERE product_id=? AND channel='video' FOR UPDATE", productId);
        if (mapping == null || text(mapping.get("external_id")).isEmpty()) throw new MiniBusinessException(409, "请先为该商品配置微信小店商品 ID");
        List<Map<String, Object>> skus = jdbc.queryForList(
                "SELECT sku_id,sku_code,sku_name,stock,external_sku_id FROM biz_product_sku WHERE product_id=? "
                        + "AND (status='0' OR (status='1' AND external_sku_id<>'')) "
                        + (force ? "" : "AND video_sync_status IN ('pending','failed') AND (video_retry_at IS NULL OR video_retry_at<=NOW()) ")
                        + "ORDER BY sku_id FOR UPDATE", productId);
        if (skus.isEmpty())
        {
            if (force) throw new MiniBusinessException(409, "该商品没有可同步的 SKU，无法同步库存");
            return result(productId, text(mapping.get("external_id")), 0, 0, new ArrayList<Map<String, Object>>());
        }

        int synced = 0;
        int failed = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> sku : skus)
        {
            long skuId = number(sku.get("sku_id"));
            String externalSkuId = text(sku.get("external_sku_id"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skuCode", sku.get("sku_code"));
            item.put("skuName", sku.get("sku_name"));
            try
            {
                if (externalSkuId.isEmpty()) throw new MiniBusinessException(400, "未配置微信小店 SKU ID");
                int localStock = Math.max(0, Math.min(Integer.MAX_VALUE, (int) number(sku.get("stock"))));
                int remoteStock = wechatShopApi.getStock(text(mapping.get("external_id")), externalSkuId);
                if (remoteStock != localStock) wechatShopApi.adjustStock(text(mapping.get("external_id")), externalSkuId, localStock - remoteStock);
                int verifiedStock = wechatShopApi.getStock(text(mapping.get("external_id")), externalSkuId);
                if (verifiedStock != localStock) throw new MiniBusinessException(502, "微信小店回读库存与本地库存不一致");
                jdbc.update("UPDATE biz_product_sku SET video_sync_status='success',video_last_error='',video_synced_at=NOW(),video_retry_at=NULL,video_retry_count=0 WHERE sku_id=?", skuId);
                item.put("status", "success");
                item.put("remoteStock", verifiedStock);
                synced++;
            }
            catch (RuntimeException e)
            {
                String message = e.getMessage() == null ? "微信库存同步失败" : e.getMessage();
                if (message.length() > 255) message = message.substring(0, 255);
                jdbc.update("UPDATE biz_product_sku SET video_sync_status='failed',video_last_error=?,video_retry_at=DATE_ADD(NOW(),INTERVAL 5 MINUTE),video_retry_count=video_retry_count+1 WHERE sku_id=?", message, skuId);
                item.put("status", "failed");
                item.put("error", message);
                failed++;
            }
            results.add(item);
        }

        String productError = "";
        if (failed > 0)
        {
            productError = results.stream().filter(item -> "failed".equals(item.get("status")))
                    .map(item -> text(item.get("skuCode")) + "：" + text(item.get("error")))
                    .reduce((left, right) -> left + "；" + right).orElse("存在失败 SKU");
            if (productError.length() > 255) productError = productError.substring(0, 255);
        }
        int unsettled = jdbc.queryForObject("SELECT COUNT(*) FROM biz_product_sku WHERE product_id=? "
                + "AND (status='0' OR (status='1' AND external_sku_id<>'')) AND video_sync_status<>'success'", Integer.class, productId);
        String productStatus = failed > 0 ? "failed" : unsettled > 0 ? "pending" : "success";
        jdbc.update("UPDATE biz_video_product SET sync_status=?,last_error=?,synced_at=IF(?='success',NOW(),synced_at) WHERE product_id=? AND channel='video'",
                productStatus, productError, productStatus, productId);
        return result(productId, text(mapping.get("external_id")), synced, failed, results);
    }

    public List<Long> pendingProducts(int limit)
    {
        if (!enabled()) return new ArrayList<>();
        return jdbc.queryForList("SELECT DISTINCT v.product_id FROM biz_video_product v JOIN biz_product_sku s ON s.product_id=v.product_id "
                + "WHERE v.channel='video' AND v.external_id<>'' AND (s.status='0' OR s.status='1') AND s.external_sku_id<>'' "
                + "AND s.video_sync_status IN ('pending','failed') AND (s.video_retry_at IS NULL OR s.video_retry_at<=NOW()) "
                + "ORDER BY v.product_id LIMIT ?", Long.class, Math.max(1, Math.min(limit, 100)));
    }

    private Map<String, Object> result(long productId, String externalId, int synced, int failed, List<Map<String, Object>> rows)
    {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("productId", productId);
        value.put("externalProductId", externalId);
        value.put("synced", synced);
        value.put("failed", failed);
        value.put("results", rows);
        value.put("message", failed > 0 ? "部分 SKU 同步失败，将在 5 分钟后自动重试" : "微信小店库存已逐 SKU 回读校验");
        return value;
    }

    private boolean enabled()
    {
        return "true".equalsIgnoreCase(config("wechat.video.syncEnabled"));
    }

    private String config(String key)
    {
        Map<String, Object> row = first("SELECT config_value FROM biz_channel_config WHERE config_key=?", key);
        return row == null ? "" : text(row.get("config_value"));
    }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static long number(Object value)
    {
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception e) { throw new MiniBusinessException(400, "库存或 ID 格式无效"); }
    }

    private static String text(Object value)
    {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
