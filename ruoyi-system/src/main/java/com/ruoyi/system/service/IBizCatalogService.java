package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.business.BizCouponTemplate;
import com.ruoyi.system.domain.business.BizExchangeItem;
import com.ruoyi.system.domain.business.BizProduct;
import com.ruoyi.system.domain.business.BizStore;

/** 面向用户端的只读业务目录服务。 */
public interface IBizCatalogService
{
    List<BizProduct> selectPublishedProducts(String category);

    BizProduct selectPublishedProductByCode(String code);

    List<BizStore> selectActiveStores();

    BizStore selectActiveStoreByCode(String code);

    List<BizCouponTemplate> selectActiveCouponTemplates();

    List<BizExchangeItem> selectPublishedExchangeItems();

    Map<String, String> selectBrandConfig();
}
