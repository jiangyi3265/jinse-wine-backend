package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Publishes explicitly prepared product data to the official WeChat Shop API. */
@Service
public class WechatShopProductService
{
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final WechatShopApiService api;

    public WechatShopProductService(JdbcTemplate jdbc, ObjectMapper json, WechatShopApiService api)
    {
        this.jdbc = jdbc;
        this.json = json;
        this.api = api;
    }

    public Map<String, Object> mapping(long productId)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT external_id AS externalId,sync_status AS syncStatus,last_error AS lastError,synced_at AS syncedAt,publish_payload_json AS publishPayloadJson,publish_status AS publishStatus,publish_error AS publishError,external_status AS externalStatus,published_at AS publishedAt FROM biz_video_product WHERE product_id=? AND channel='video'", productId);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
    }

    public String template(long productId)
    {
        Map<String, Object> product = first("SELECT product_name,description FROM biz_product WHERE product_id=?", productId);
        if (product == null) throw new MiniBusinessException(404, "商品不存在");
        List<Map<String, Object>> skus = jdbc.queryForList("SELECT sku_code,sku_name,external_sku_id,price_cent,stock FROM biz_product_sku WHERE product_id=? AND status='0' ORDER BY sku_id", productId);
        if (skus.isEmpty()) throw new MiniBusinessException(409, "商品至少需要一个启用规格，才能生成微信发布模板");
        Map<String, Object> body = new LinkedHashMap<>();
        String title = text(product.get("product_name"));
        body.put("title", title);
        body.put("short_title", title.length() > 20 ? title.substring(0, 20) : title);
        body.put("head_imgs", new ArrayList<>());
        Map<String, Object> description = new LinkedHashMap<>();
        description.put("imgs", new ArrayList<>());
        description.put("desc", text(product.get("description")));
        body.put("desc_info", description);
        body.put("cats_v2", new ArrayList<>());
        body.put("attrs", new ArrayList<>());
        body.put("express_info", mapOf("template_id", 0));
        List<Map<String, Object>> externalSkus = new ArrayList<>();
        for (Map<String, Object> sku : skus)
        {
            Map<String, Object> external = new LinkedHashMap<>();
            if (!text(sku.get("external_sku_id")).isEmpty()) external.put("sku_id", text(sku.get("external_sku_id")));
            external.put("thumb_img", "");
            external.put("sale_price", number(sku.get("price_cent")));
            external.put("stock_num", number(sku.get("stock")));
            external.put("sku_code", text(sku.get("sku_code")));
            external.put("sku_attrs", new ArrayList<>());
            external.put("sku_deliver_info", mapOf("stock_type", 0));
            externalSkus.add(external);
        }
        body.put("skus", externalSkus);
        try { return json.writerWithDefaultPrettyPrinter().writeValueAsString(body); }
        catch (JsonProcessingException e) { throw new MiniBusinessException(500, "微信小店发布模板生成失败"); }
    }

    @Transactional
    public void saveMapping(long productId, String externalId, String payload)
    {
        if (first("SELECT product_id FROM biz_product WHERE product_id=? FOR UPDATE", productId) == null) throw new MiniBusinessException(404, "商品不存在");
        if (externalId.length() > 128) throw new MiniBusinessException(400, "微信小店商品 ID 不能超过128个字符");
        if (payload != null && payload.length() > 500000) throw new MiniBusinessException(400, "微信小店发布资料不能超过500KB");
        if (payload != null && !payload.trim().isEmpty()) parsePayload(payload);
        Map<String, Object> current = first("SELECT external_id,publish_status,publish_payload_json FROM biz_video_product WHERE product_id=? AND channel='video' FOR UPDATE", productId);
        if (!externalId.isEmpty())
        {
            Long duplicate = jdbc.queryForObject("SELECT COUNT(*) FROM biz_video_product WHERE channel='video' AND external_id=? AND product_id<>?", Long.class, externalId, productId);
            if (duplicate != null && duplicate > 0) throw new MiniBusinessException(409, "该微信小店商品 ID 已映射到其他本地商品");
        }
        String nextPayload = payload == null || payload.trim().isEmpty() ? null : payload;
        boolean unresolved = current != null && ("unknown".equals(text(current.get("publish_status"))) || "submitting".equals(text(current.get("publish_status")))
                || "listing_unknown".equals(text(current.get("publish_status"))) || "listing_submitting".equals(text(current.get("publish_status"))));
        boolean manualRecovery = unresolved && text(current.get("external_id")).isEmpty() && !externalId.isEmpty()
                && (payload == null || java.util.Objects.equals(nextPayload, current.get("publish_payload_json")));
        if (unresolved && !manualRecovery)
            throw new MiniBusinessException(409, "微信请求结果仍待核对；未知状态只允许补录平台商品 ID，不能修改发布资料或覆盖已有商品映射");
        if (current == null)
            jdbc.update("INSERT INTO biz_video_product(product_id,channel,external_id,sync_status,last_error,publish_payload_json,publish_status,publish_error) VALUES(?,'video',?,'pending','',?,'draft','')",
                    productId, externalId, nextPayload);
        else
        {
            boolean mappingChanged = !externalId.equals(text(current.get("external_id")))
                    || (payload != null && !java.util.Objects.equals(nextPayload, current.get("publish_payload_json")));
            boolean resetPublishState = mappingChanged && !manualRecovery;
            if (payload == null)
                jdbc.update("UPDATE biz_video_product SET external_id=?,sync_status='pending',last_error='',publish_status=CASE WHEN ? THEN 'draft' ELSE publish_status END,publish_error=CASE WHEN ? THEN '' ELSE publish_error END WHERE product_id=? AND channel='video'",
                        externalId, resetPublishState, resetPublishState, productId);
            else
                jdbc.update("UPDATE biz_video_product SET external_id=?,sync_status='pending',last_error='',publish_payload_json=?,publish_status=CASE WHEN ? THEN 'draft' ELSE publish_status END,publish_error=CASE WHEN ? THEN '' ELSE publish_error END WHERE product_id=? AND channel='video'",
                        externalId, nextPayload, resetPublishState, resetPublishState, productId);
        }
        if (!externalId.equals(current == null ? "" : text(current.get("external_id"))))
            jdbc.update("UPDATE biz_product_sku SET external_sku_id='',video_sync_status='pending',video_last_error='',video_retry_at=NULL,video_retry_count=0 WHERE product_id=?", productId);
        else
            jdbc.update("UPDATE biz_product_sku SET video_sync_status='pending',video_last_error='',video_retry_at=NULL,video_retry_count=0 WHERE product_id=? AND status='0'", productId);
    }

    public Map<String, Object> submit(long productId)
    {
        Map<String, Object> mapping = first("SELECT external_id,publish_payload_json,publish_status FROM biz_video_product WHERE product_id=? AND channel='video'", productId);
        if (mapping == null) throw new MiniBusinessException(409, "请先保存商品映射和微信发布资料");
        Map<String, Object> product = first("SELECT product_name FROM biz_product WHERE product_id=?", productId);
        if (product == null) throw new MiniBusinessException(404, "本地商品不存在");
        String externalId = text(mapping.get("external_id"));
        String payloadText = text(mapping.get("publish_payload_json"));
        String priorStatus = text(mapping.get("publish_status"));
        requireAuthorizedShop();
        if (("submitting".equals(priorStatus) || "unknown".equals(priorStatus)) && externalId.isEmpty())
            throw new MiniBusinessException(409, "上次创建商品的结果未知。请先到微信小店后台按本地商品编码核对；取得商品 ID 后填入映射，再刷新状态，不能盲目重复创建。");
        if ("submitting".equals(priorStatus) || "unknown".equals(priorStatus))
        {
            String status = refreshExternal(productId, externalId);
            Map<String, Object> recovered = new LinkedHashMap<>();
            recovered.put("externalId", externalId);
            recovered.put("publishStatus", "submitted");
            recovered.put("externalStatus", status);
            recovered.put("message", "已从微信回读现有商品状态，本次未重复提交；检查状态后可继续操作");
            return recovered;
        }
        Map<String, Object> body = parsePayload(payloadText);
        if (!externalId.isEmpty())
        {
            body.put("product_id", externalId);
            fillMappedSkuIds(productId, body);
        }
        validatePayload(body, !externalId.isEmpty());
        int claimed = jdbc.update("UPDATE biz_video_product SET publish_status='submitting',publish_error='' WHERE product_id=? AND channel='video' AND publish_status IN ('draft','submitted')", productId);
        if (claimed != 1) throw new MiniBusinessException(409, "商品发布状态已变化，请刷新后重试");
        Map<String, Object> result = new LinkedHashMap<>();
        try
        {
            if (externalId.isEmpty())
            {
                JsonNode response = api.addProduct(body);
                externalId = response.path("product_id").asText(response.path("data").path("product_id").asText(""));
                if (externalId.isEmpty()) throw new MiniBusinessException(502, "微信已返回成功，但没有商品 ID；请刷新微信商品列表核对，勿重复创建");
            }
            else
            {
                api.updateProduct(body);
            }
            jdbc.update("UPDATE biz_video_product SET external_id=?,publish_status='submitted',publish_error='',published_at=NOW() WHERE product_id=? AND channel='video'", externalId, productId);
            String status = refreshExternal(productId, externalId);
            result.put("externalId", externalId);
            result.put("publishStatus", "submitted");
            result.put("externalStatus", status);
            result.put("message", "微信已接收商品资料，当前平台状态：" + (status.isEmpty() ? "待审核状态回读" : status));
            return result;
        }
        catch (MiniBusinessException e)
        {
            if (e.getCode() != 502)
            {
                jdbc.update("UPDATE biz_video_product SET publish_status=?,publish_error=? WHERE product_id=? AND channel='video'", externalId.isEmpty() ? "draft" : "submitted", limit(e.getMessage(), 255), productId);
                throw e;
            }
            jdbc.update("UPDATE biz_video_product SET external_id=?,publish_status='unknown',publish_error=? WHERE product_id=? AND channel='video'", externalId, limit(e.getMessage(), 255), productId);
            result.put("externalId", externalId);
            result.put("publishStatus", "unknown");
            result.put("message", externalId.isEmpty() ? "商品创建结果未知；请先在微信小店核对，勿重复创建" : "微信接口结果未知；已保留商品 ID，先刷新平台状态核对，不要重复提交");
            return result;
        }
    }

    public Map<String, Object> requestListing(long productId)
    {
        Map<String, Object> mapping = first("SELECT external_id,publish_status FROM biz_video_product WHERE product_id=? AND channel='video'", productId);
        String externalId = mapping == null ? "" : text(mapping.get("external_id"));
        if (externalId.isEmpty()) throw new MiniBusinessException(409, "请先向微信提交商品资料并取得商品 ID");
        requireAuthorizedShop();
        String priorStatus = text(mapping.get("publish_status"));
        if ("listing_submitted".equals(priorStatus)) throw new MiniBusinessException(409, "上架申请已提交，请刷新微信商品状态后再操作");
        if ("listing_submitting".equals(priorStatus) || "listing_unknown".equals(priorStatus))
        {
            String current = refreshExternal(productId, externalId);
            Map<String, Object> recovered = new LinkedHashMap<>();
            recovered.put("externalId", externalId);
            recovered.put("publishStatus", "listing_submitted");
            recovered.put("externalStatus", current);
            recovered.put("message", "已从微信回读商品状态，本次未重复提交上架申请");
            return recovered;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("externalId", externalId);
        int claimed = jdbc.update("UPDATE biz_video_product SET publish_status='listing_submitting',publish_error='' WHERE product_id=? AND channel='video' AND publish_status IN ('submitted','listing_submitted')", productId);
        if (claimed != 1) throw new MiniBusinessException(409, "商品资料尚未提交或状态已变化，请刷新平台状态");
        try
        {
            api.requestProductListing(externalId);
            jdbc.update("UPDATE biz_video_product SET publish_status='listing_submitted',publish_error='' WHERE product_id=? AND channel='video'", productId);
            result.put("publishStatus", "listing_submitted");
            try { result.put("externalStatus", refreshExternal(productId, externalId)); }
            catch (MiniBusinessException e) { result.put("message", "微信已接受上架申请，但状态回读暂不可用，请稍后刷新"); return result; }
            result.put("message", "微信已接受上架申请；是否在售以平台状态回读为准");
        }
        catch (MiniBusinessException e)
        {
            if (e.getCode() != 502)
            {
                jdbc.update("UPDATE biz_video_product SET publish_status='submitted',publish_error=? WHERE product_id=? AND channel='video'", limit(e.getMessage(), 255), productId);
                throw e;
            }
            jdbc.update("UPDATE biz_video_product SET publish_status='listing_unknown',publish_error=? WHERE product_id=? AND channel='video'", limit(e.getMessage(), 255), productId);
            result.put("publishStatus", "listing_unknown");
            result.put("message", "上架申请结果未知；请先刷新微信商品状态，系统不会自动重复提交");
        }
        return result;
    }

    @Transactional
    public Map<String, Object> refresh(long productId)
    {
        Map<String, Object> mapping = first("SELECT external_id FROM biz_video_product WHERE product_id=? AND channel='video'", productId);
        String externalId = mapping == null ? "" : text(mapping.get("external_id"));
        if (externalId.isEmpty()) throw new MiniBusinessException(409, "该商品尚未取得微信小店商品 ID");
        requireAuthorizedShop();
        String status = refreshExternal(productId, externalId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("externalId", externalId);
        result.put("externalStatus", status);
        result.put("message", status.isEmpty() ? "平台未返回商品状态" : "已刷新微信商品状态");
        return result;
    }

    private String refreshExternal(long productId, String externalId)
    {
        JsonNode response = api.getProduct(externalId);
        JsonNode product = response.path("product");
        if (product.isMissingNode() || product.isNull()) product = response.path("data").path("product");
        if (product.isMissingNode() || product.isNull()) throw new MiniBusinessException(502, "微信商品详情为空");
        String status = product.path("status").asText("");
        String audit = product.path("audit_status").asText("");
        String externalStatus = audit.isEmpty() ? status : "status=" + status + ",audit=" + audit;
        jdbc.update("UPDATE biz_video_product SET external_status=?,publish_error='',publish_status=CASE WHEN publish_status IN ('submitting','unknown') THEN 'submitted' WHEN publish_status IN ('listing_submitting','listing_unknown') THEN 'listing_submitted' ELSE publish_status END WHERE product_id=? AND channel='video'", limit(externalStatus, 32), productId);
        JsonNode skus = product.path("skus");
        if (!skus.isArray()) skus = product.path("sku_infos");
        if (skus.isArray())
        {
            for (JsonNode sku : skus)
            {
                String code = sku.path("sku_code").asText("");
                String skuId = sku.path("sku_id").asText("");
                if (!code.isEmpty() && !skuId.isEmpty())
                    jdbc.update("UPDATE biz_product_sku SET external_sku_id=? WHERE product_id=? AND sku_code=?", skuId, productId, code);
            }
        }
        return externalStatus;
    }

    private void fillMappedSkuIds(long productId, Map<String, Object> body)
    {
        Object value = body.get("skus");
        if (!(value instanceof List<?>)) return;
        @SuppressWarnings("unchecked")
        List<Object> skus = (List<Object>) value;
        for (Object item : skus)
        {
            if (!(item instanceof Map<?, ?>)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> sku = (Map<String, Object>) item;
            String code = text(sku.get("sku_code"));
            if (code.isEmpty()) continue;
            Map<String, Object> local = first("SELECT external_sku_id FROM biz_product_sku WHERE product_id=? AND sku_code=?", productId, code);
            if (local != null && !text(local.get("external_sku_id")).isEmpty()) sku.put("sku_id", text(local.get("external_sku_id")));
        }
    }

    private String requireAuthorizedShop()
    {
        if (!"true".equalsIgnoreCase(config("wechat.video.syncEnabled"))) throw new MiniBusinessException(409, "请先开启微信小店数据同步");
        String shopId = config("wechat.video.shopId");
        if (shopId.isEmpty()) throw new MiniBusinessException(409, "请先配置微信小店 ID 并完成授权校验");
        api.checkShop(shopId);
        return shopId;
    }

    private Map<String, Object> parsePayload(String payload)
    {
        if (payload == null || payload.trim().isEmpty()) throw new MiniBusinessException(400, "请先填写微信小店商品发布资料");
        try
        {
            JsonNode node = json.readTree(payload);
            if (node == null || !node.isObject()) throw new MiniBusinessException(400, "微信小店发布资料必须是 JSON 对象");
            return json.convertValue(node, new TypeReference<Map<String, Object>>() {});
        }
        catch (MiniBusinessException e) { throw e; }
        catch (Exception e) { throw new MiniBusinessException(400, "微信小店发布资料 JSON 格式无效"); }
    }

    private void validatePayload(Map<String, Object> body, boolean update)
    {
        JsonNode node = json.valueToTree(body);
        if (blank(node.path("title").asText("")) || blank(node.path("short_title").asText(""))) throw new MiniBusinessException(400, "商品名称和短标题必填");
        if (!nonEmptyArray(node.path("head_imgs"))) throw new MiniBusinessException(400, "请填写至少一张微信小店商品主图素材");
        if (!node.path("desc_info").isObject() || !nonEmptyArray(node.path("desc_info").path("imgs"))) throw new MiniBusinessException(400, "请填写商品详情素材图片");
        if (!nonEmptyArray(node.path("cats_v2"))) throw new MiniBusinessException(400, "请填写微信小店商品类目 ID");
        if (node.path("express_info").path("template_id").asLong(0) <= 0) throw new MiniBusinessException(400, "请填写有效的微信小店运费模板 ID");
        JsonNode skus = node.path("skus");
        if (!nonEmptyArray(skus)) throw new MiniBusinessException(400, "至少需要一个商品规格");
        for (JsonNode sku : skus)
        {
            if (blank(sku.path("sku_code").asText("")) || blank(sku.path("thumb_img").asText("")) || sku.path("sale_price").asLong(-1) < 0 || sku.path("stock_num").asLong(-1) < 0 || !sku.path("sku_attrs").isArray())
                throw new MiniBusinessException(400, "每个 SKU 都必须填写规格编码、微信素材图、价格、库存和属性数组");
            if (update && blank(sku.path("sku_id").asText(""))) throw new MiniBusinessException(400, "更新微信商品时，每个 SKU 必须带有平台 sku_id；请先刷新平台状态");
        }
        if (update && blank(node.path("product_id").asText(""))) throw new MiniBusinessException(400, "更新商品资料缺少 product_id");
    }

    private boolean nonEmptyArray(JsonNode value) { return value != null && value.isArray() && !value.isEmpty(); }

    private Map<String, Object> first(String sql, Object... args)
    {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String config(String key)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT config_value FROM biz_channel_config WHERE config_key=?", key);
        return rows.isEmpty() ? "" : text(rows.get(0).get("config_value"));
    }

    private static Map<String, Object> mapOf(String key, Object value)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(key, value);
        return result;
    }

    private static long number(Object value)
    {
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception e) { return 0; }
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static String limit(String value, int max) { String safe = value == null ? "" : value; return safe.length() <= max ? safe : safe.substring(0, max); }
}
