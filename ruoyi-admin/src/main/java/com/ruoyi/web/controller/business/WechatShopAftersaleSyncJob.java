package com.ruoyi.web.controller.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls WeChat Shop after-sale updates and stores privacy-minimized summaries. */
@Component
public class WechatShopAftersaleSyncJob
{
    private static final Logger log = LoggerFactory.getLogger(WechatShopAftersaleSyncJob.class);
    private final WechatShopAftersaleSyncService sync;

    public WechatShopAftersaleSyncJob(WechatShopAftersaleSyncService sync)
    {
        this.sync = sync;
    }

    @Scheduled(fixedDelayString = "${mini.wechat.shop-aftersale-sync-interval-ms:300000}")
    public void sync()
    {
        try { sync.syncOnePage(); }
        catch (MiniBusinessException e)
        {
            if (e.getCode() == 409)
            {
                log.debug("微信小店售后同步未启用，跳过本轮任务");
            }
            else
            {
                log.warn("微信小店售后同步失败，原因={}", e.getMessage());
            }
        }
        catch (RuntimeException e) { log.warn("微信小店售后同步失败，原因={}", e.getMessage()); }
    }
}
