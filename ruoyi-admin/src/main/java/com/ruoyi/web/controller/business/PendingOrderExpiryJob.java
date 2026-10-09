package com.ruoyi.web.controller.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Releases inventory and coupons held by unpaid orders after the payment window. */
@Component
public class PendingOrderExpiryJob
{
    private static final Logger log = LoggerFactory.getLogger(PendingOrderExpiryJob.class);

    private final MiniCommerceService commerce;

    @Value("${mini.order.pending-timeout-minutes:30}")
    private int timeoutMinutes;

    public PendingOrderExpiryJob(MiniCommerceService commerce)
    {
        this.commerce = commerce;
    }

    @Scheduled(
            fixedDelayString = "${mini.order.pending-expiry-interval-ms:60000}",
            initialDelayString = "${mini.order.pending-expiry-initial-delay-ms:60000}")
    public void expire()
    {
        int expired = commerce.expirePendingOrders(timeoutMinutes);
        if (expired > 0)
        {
            log.info("已关闭 {} 个超时未支付订单，并释放库存和优惠券", expired);
        }
    }
}
