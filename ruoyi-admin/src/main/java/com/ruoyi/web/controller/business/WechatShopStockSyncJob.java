package com.ruoyi.web.controller.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically retries local SKU inventory changes that have not reached WeChat Shop. */
@Component
public class WechatShopStockSyncJob
{
    private static final Logger log = LoggerFactory.getLogger(WechatShopStockSyncJob.class);
    private final WechatShopStockSyncService stockSync;

    public WechatShopStockSyncJob(WechatShopStockSyncService stockSync)
    {
        this.stockSync = stockSync;
    }

    @Scheduled(fixedDelayString = "${mini.wechat.shop-stock-sync-interval-ms:60000}")
    public void syncPendingProducts()
    {
        for (Long productId : stockSync.pendingProducts(20))
        {
            try
            {
                stockSync.syncProduct(productId, false);
            }
            catch (RuntimeException e)
            {
                log.warn("微信小店库存自动对账失败，商品ID={}，原因={}", productId, e.getMessage());
            }
        }
    }
}
