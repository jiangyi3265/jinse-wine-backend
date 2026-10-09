package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;

@RestController
@RequestMapping("/business/platform-options")
public class PlatformOptionController
{
    private final WechatShopApiService shop;
    private final WechatWorkGroupQrService groups;
    public PlatformOptionController(WechatShopApiService shop, WechatWorkGroupQrService groups) { this.shop = shop; this.groups = groups; }

    @GetMapping("/{kind}")
    @PreAuthorize("@ss.hasAnyPermi('business:channel:query,business:channel:edit')")
    public AjaxResult choices(@PathVariable String kind, @RequestParam(defaultValue = "") String cursor,
            @RequestParam(defaultValue = "") String ids, @RequestParam(defaultValue = "") String productId)
    {
        if (cursor.length() > 2048 || ids.length() > 1024 || productId.length() > 128) return AjaxResult.error(400, "选项参数过长");
        try
        {
            if ("groups".equals(kind)) return AjaxResult.success(groups.groupChoices(cursor, ids));
            List<Map<String, Object>> rows = new ArrayList<>(); String next = ""; boolean more = false;
            if ("shop".equals(kind))
            {
                Map<String, Object> data = shop.checkShop(""); add(rows, String.valueOf(data.get("shopId")), String.valueOf(data.get("shopName")));
            }
            else if ("templates".equals(kind))
            {
                for (JsonNode item : shop.subscriptionTemplates().path("data")) add(rows, item.path("priTmplId").asText(), item.path("title").asText());
            }
            else if ("products".equals(kind))
            {
                if (!ids.isEmpty()) { JsonNode p = product(shop.getProduct(ids)); add(rows, ids, p.path("title").asText()); }
                else
                {
                    JsonNode data = shop.choiceData(kind, cursor, "");
                    for (JsonNode id : data.path("product_ids")) { JsonNode p = product(shop.getProduct(id.asText())); add(rows, id.asText(), p.path("title").asText()); }
                    next = data.path("next_key").asText(""); more = !next.isEmpty() && !next.equals(cursor) && !rows.isEmpty();
                }
            }
            else if ("skus".equals(kind))
            {
                if (productId.isEmpty()) return AjaxResult.error(400, "请先选择微信小店商品");
                JsonNode product = product(shop.getProduct(productId)), skus = product.path("skus");
                if (!skus.isArray()) skus = product.path("sku_infos");
                for (JsonNode sku : skus)
                {
                    List<String> names = new ArrayList<>();
                    for (JsonNode attr : sku.path("sku_attrs")) names.add(attr.path("attr_key").asText() + "：" + attr.path("attr_value").asText());
                    add(rows, sku.path("sku_id").asText(), names.isEmpty() ? product.path("title").asText() + "（默认规格）" : String.join(" / ", names));
                }
            }
            else if ("delivery".equals(kind))
            {
                JsonNode data = shop.choiceData(kind, "", "");
                for (JsonNode item : data.path("company_list")) add(rows, item.path("delivery_id").asText(), item.path("delivery_name").asText());
            }
            else if ("addresses".equals(kind))
            {
                List<String> addresses = new ArrayList<>();
                if (!ids.isEmpty()) addresses.add(ids);
                else
                {
                    if (!cursor.isEmpty() && !cursor.matches("[0-9]{1,7}")) return AjaxResult.error(400, "分页参数无效");
                    JsonNode data = shop.choiceData(kind, cursor, "");
                    for (JsonNode id : data.path("address_id_list")) addresses.add(id.asText());
                    int offset = cursor.isEmpty() ? 0 : Integer.parseInt(cursor);
                    next = String.valueOf(offset + addresses.size()); more = addresses.size() == 5;
                }
                for (String id : addresses)
                {
                    JsonNode data = shop.choiceData("address", "", id), detail = data.path("address_detail");
                    if (detail.isMissingNode()) detail = data.path("address");
                    JsonNode address = detail.path("address_info");
                    add(rows, id, detail.path("name").asText() + " " + address.path("province_name").asText() + address.path("city_name").asText() + address.path("county_name").asText() + address.path("detail_info").asText());
                }
            }
            else return AjaxResult.error(400, "不支持的平台选项");
            Map<String, Object> data = new LinkedHashMap<>(); data.put("rows", rows); data.put("nextCursor", next); data.put("hasMore", more); return AjaxResult.success(data);
        }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    private JsonNode product(JsonNode response) { return response.has("product") ? response.path("product") : response.path("data").path("product"); }
    private void add(List<Map<String, Object>> rows, String id, String label)
    {
        if (id == null || id.isEmpty()) return;
        Map<String, Object> row = new LinkedHashMap<>(); row.put("value", id); row.put("label", label == null || label.trim().isEmpty() ? "平台未提供名称" : label.trim()); rows.add(row);
    }
}
