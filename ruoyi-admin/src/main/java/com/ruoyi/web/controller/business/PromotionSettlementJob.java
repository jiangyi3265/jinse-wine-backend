package com.ruoyi.web.controller.business;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Advances matured commission records even when no promoter opens the mini app. */
@Component
public class PromotionSettlementJob
{
    private final MiniBusinessExtensionService promotions;

    public PromotionSettlementJob(MiniBusinessExtensionService promotions)
    {
        this.promotions = promotions;
    }

    @Scheduled(fixedDelayString = "${mini.promotion.settlement-interval-ms:60000}",
            initialDelayString = "${mini.promotion.settlement-initial-delay-ms:30000}")
    public void settle()
    {
        try { promotions.settleMaturedPromotions(); }
        catch (RuntimeException ignored)
        {
            // A transient database failure is retried on the next scheduled run.
        }
    }
}
