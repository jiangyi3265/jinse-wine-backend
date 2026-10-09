package com.ruoyi.web.controller.business;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderLogisticsJob
{
    private static final Logger log = LoggerFactory.getLogger(OrderLogisticsJob.class);
    private final OrderLogisticsService logistics;
    private final MiniCommerceService commerce;
    private final OrderAftersaleService aftersales;
    private final JdbcTemplate jdbc;
    public OrderLogisticsJob(OrderLogisticsService logistics, MiniCommerceService commerce, OrderAftersaleService aftersales, JdbcTemplate jdbc)
    { this.logistics = logistics; this.commerce = commerce; this.aftersales = aftersales; this.jdbc = jdbc; }

    @Scheduled(fixedDelayString="${logistics.sync.interval-ms:60000}", initialDelayString="${logistics.sync.initial-delay-ms:60000}")
    public void synchronize()
    {
        try { logistics.synchronizeDue(); } catch (Exception e) { log.warn("物流批次同步失败，将在下轮重试"); }
        try { aftersales.reconcileRefunds(); } catch (Exception e) { log.warn("售后退款状态更新失败，将在下轮重试"); }
        for (Map<String, Object> row : jdbc.queryForList("SELECT o.order_id FROM biz_order o JOIN biz_order_delivery d ON d.order_id=o.order_id "
                + "WHERE o.status IN ('shipping','receiving') AND o.fulfillment_mode='delivery' AND d.status='signed' "
                + "AND d.signed_at<=DATE_SUB(NOW(),INTERVAL 7 DAY) "
                + "AND NOT EXISTS (SELECT 1 FROM biz_order_refund r WHERE r.order_id=o.order_id AND r.status IN ('pending','approved')) "
                + "AND NOT EXISTS (SELECT 1 FROM biz_order_aftersale a WHERE a.order_id=o.order_id AND a.status IN ('pending','approved','returning','received','refund_pending','exchange_shipping')) "
                + "ORDER BY d.signed_at,o.order_id LIMIT 100"))
            try { commerce.autoReceiveSignedOrder(((Number) row.get("order_id")).longValue()); }
            catch (Exception e) { log.warn("订单 {} 自动收货未完成，将在下轮重试", row.get("order_id")); }
    }
}
