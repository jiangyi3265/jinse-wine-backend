package com.ruoyi.system.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.business.BizBrandConfig;
import com.ruoyi.system.domain.business.BizCouponTemplate;
import com.ruoyi.system.domain.business.BizExchangeItem;
import com.ruoyi.system.domain.business.BizProduct;
import com.ruoyi.system.domain.business.BizStore;
import com.ruoyi.system.mapper.BizCatalogMapper;
import com.ruoyi.system.service.IBizCatalogService;

@Service
public class BizCatalogServiceImpl implements IBizCatalogService
{
    @Autowired
    private BizCatalogMapper catalogMapper;

    @Override
    public List<BizProduct> selectPublishedProducts(String category)
    {
        return catalogMapper.selectPublishedProducts(category);
    }

    @Override
    public BizProduct selectPublishedProductByCode(String code)
    {
        return catalogMapper.selectPublishedProductByCode(code);
    }

    @Override
    public List<BizStore> selectActiveStores()
    {
        return catalogMapper.selectActiveStores();
    }

    @Override
    public BizStore selectActiveStoreByCode(String code)
    {
        return catalogMapper.selectActiveStoreByCode(code);
    }

    @Override
    public List<BizCouponTemplate> selectActiveCouponTemplates()
    {
        return catalogMapper.selectActiveCouponTemplates();
    }

    @Override
    public List<BizExchangeItem> selectPublishedExchangeItems()
    {
        return catalogMapper.selectPublishedExchangeItems();
    }

    @Override
    public Map<String, String> selectBrandConfig()
    {
        Map<String, String> config = new LinkedHashMap<>();
        for (BizBrandConfig item : catalogMapper.selectBrandConfigs())
        {
            config.put(item.getConfigKey(), item.getConfigValue());
        }
        return config;
    }
}
