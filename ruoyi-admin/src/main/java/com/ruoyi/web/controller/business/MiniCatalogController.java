package com.ruoyi.web.controller.business;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.MiniResult;
import com.ruoyi.system.domain.business.BizProduct;
import com.ruoyi.system.domain.business.BizStore;
import com.ruoyi.system.service.IBizCatalogService;

/**
 * 用户端商品与线下服务目录。
 * 目录接口保持匿名只读；会员、订单、库存和支付接口由 MiniCommerceController 提供。
 */
@Anonymous
@RestController
@RequestMapping("/mini")
public class MiniCatalogController
{
    @Autowired
    private IBizCatalogService catalogService;

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/products")
    public MiniResult<?> products(@RequestParam(required = false) String category)
    {
        return MiniResult.success(catalogService.selectPublishedProducts(category));
    }

    @GetMapping("/products/{code}")
    public MiniResult<?> product(@PathVariable String code)
    {
        BizProduct product = catalogService.selectPublishedProductByCode(code);
        return product == null ? MiniResult.error(404, "商品不存在或已下架") : MiniResult.success(product);
    }

    @GetMapping("/products/{code}/skus")
    public MiniResult<?> productSkus(@PathVariable String code)
    {
        BizProduct product = catalogService.selectPublishedProductByCode(code);
        if (product == null) return MiniResult.error(404, "商品不存在或已下架");
        return MiniResult.success(jdbc.queryForList("SELECT sku_code AS skuCode,sku_name AS skuName,price_cent AS priceCent,stock,spec_json AS specJson FROM biz_product_sku WHERE product_id=? AND status='0' ORDER BY sku_id", product.getProductId()));
    }

    @GetMapping("/products/{code}/shipping-quote")
    public MiniResult<?> productShipping(@PathVariable String code,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(defaultValue = "") String skuCode)
    {
        if (quantity < 1 || quantity > 99) return MiniResult.error(400, "购买数量必须为1至99件");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT product_id,price_cent,shipping_template_id FROM biz_product WHERE product_code=? AND status='0'", code);
        if (rows.isEmpty()) return MiniResult.error(404, "商品不存在或已下架");
        Map<String, Object> product = rows.get(0);
        long price = ((Number) product.get("price_cent")).longValue();
        if (!skuCode.trim().isEmpty())
        {
            List<Map<String, Object>> skus = jdbc.queryForList("SELECT price_cent FROM biz_product_sku WHERE product_id=? AND sku_code=? AND status='0'", product.get("product_id"), skuCode.trim());
            if (skus.isEmpty()) return MiniResult.error(404, "商品规格不存在或已停用");
            price = ((Number) skus.get(0).get("price_cent")).longValue();
        }
        Map<String, Object> rule = ShippingFeeCalculator.rule(jdbc, product.get("shipping_template_id"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateName", rule == null ? "免运费" : rule.get("template_name"));
        result.put("freeThreshold", rule == null ? 0 : rule.get("free_threshold_cent"));
        try { result.put("shipping", ShippingFeeCalculator.amount(rule, price * quantity, 0, quantity)); }
        catch (MiniBusinessException e) { return MiniResult.error(e.getCode(), e.getMessage()); }
        return MiniResult.success(result);
    }

    @GetMapping("/stores")
    public MiniResult<?> stores()
    {
        return MiniResult.success(catalogService.selectActiveStores());
    }

    @GetMapping("/stores/{code}")
    public MiniResult<?> store(@PathVariable String code)
    {
        BizStore store = catalogService.selectActiveStoreByCode(code);
        return store == null ? MiniResult.error(404, "门店不存在或已停用") : MiniResult.success(store);
    }

    @GetMapping("/coupons/templates")
    public MiniResult<?> couponTemplates()
    {
        return MiniResult.success(catalogService.selectActiveCouponTemplates());
    }

    @GetMapping("/exchanges")
    public MiniResult<?> exchanges()
    {
        return MiniResult.success(catalogService.selectPublishedExchangeItems());
    }

    @GetMapping("/brand")
    public MiniResult<?> brand()
    {
        return MiniResult.success(catalogService.selectBrandConfig());
    }
}
