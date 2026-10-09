package com.ruoyi.web.controller.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the official WeChat Shop order update feed with a durable pagination cursor. */
@Component
public class WechatShopOrderSyncJob
{
    private static final Logger log = LoggerFactory.getLogger(WechatShopOrderSyncJob.class);
    private final WechatShopOrderSyncService orderSync;

    public WechatShopOrderSyncJob(WechatShopOrderSyncService orderSync)
    {
        this.orderSync = orderSync;
    }

    @Scheduled(fixedDelayString = "${mini.wechat.shop-order-sync-interval-ms:300000}")
    public void syncOrders()
    {
        try
        {
            orderSync.syncOnePage();
        }
        catch (MiniBusinessException e)
        {
            if (e.getCode() == 409)
            {
                log.debug("微信小店订单同步未启用，跳过本轮任务");
            }
            else
            {
                log.warn("微信小店订单同步失败，原因={}", e.getMessage());
            }
        }
        catch (RuntimeException e)
        {
            log.warn("微信小店订单同步失败，原因={}", e.getMessage());
        }
    }
}
