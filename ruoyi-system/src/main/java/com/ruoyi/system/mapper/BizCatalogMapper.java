package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.business.BizBrandConfig;
import com.ruoyi.system.domain.business.BizCouponTemplate;
import com.ruoyi.system.domain.business.BizExchangeItem;
import com.ruoyi.system.domain.business.BizProduct;
import com.ruoyi.system.domain.business.BizStore;

/** 小程序公共目录数据访问层。 */
public interface BizCatalogMapper
{
    List<BizProduct> selectPublishedProducts(@Param("category") String category);

    BizProduct selectPublishedProductByCode(@Param("code") String code);

    List<BizStore> selectActiveStores();

    BizStore selectActiveStoreByCode(@Param("code") String code);

    List<BizCouponTemplate> selectActiveCouponTemplates();

    List<BizExchangeItem> selectPublishedExchangeItems();

    List<BizBrandConfig> selectBrandConfigs();
}
