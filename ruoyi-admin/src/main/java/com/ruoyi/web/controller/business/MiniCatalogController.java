package com.ruoyi.web.controller.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
