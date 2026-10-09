package com.ruoyi.web.controller.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LotteryPreallocationExpiryJob {
    private static final Logger LOG = LoggerFactory.getLogger(LotteryPreallocationExpiryJob.class);
    private final LotteryPreallocationService lottery;
    public LotteryPreallocationExpiryJob(LotteryPreallocationService lottery) { this.lottery = lottery; }

    @Scheduled(fixedDelayString = "${mini.lottery.expiry-interval-ms:60000}", initialDelay = 60000)
    public void expire() {
        for (Long id : lottery.expiredCodes()) {
            try { lottery.expire(id); }
            catch (Exception e) { LOG.warn("Lottery code reservation expiry failed for {}", id, e); }
        }
    }
}
