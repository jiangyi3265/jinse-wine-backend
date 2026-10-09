package com.ruoyi.web.controller.business;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Starts eligible transfers and keeps reconciling existing unsettled WeChat transfers. */
@Component
public class CashRewardTransferJob
{
    private static final Logger log = LoggerFactory.getLogger(CashRewardTransferJob.class);
    private final JdbcTemplate jdbc;
    private final MiniMarketingService marketing;
    private final WechatPayService wechatPay;

    public CashRewardTransferJob(JdbcTemplate jdbc, MiniMarketingService marketing, WechatPayService wechatPay)
    {
        this.jdbc = jdbc;
        this.marketing = marketing;
        this.wechatPay = wechatPay;
    }

    @Scheduled(fixedDelayString = "${mini.wechat.cash-transfer-interval-ms:10000}",
            initialDelayString = "${mini.wechat.cash-transfer-initial-delay-ms:20000}")
    public void dispatchPendingCashRewards()
    {
        // Never create a transfer attempt until all official credentials and
        // the public callback are configured. The reward stays pending.
        if (!wechatPay.isTransferConfigured()) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT w.reward_id,w.member_id FROM biz_reward w "
                        + "JOIN biz_member m ON m.member_id=w.member_id "
                        + "WHERE w.reward_type='cash' AND w.status='pending' "
                        + "AND (w.expires_at IS NULL OR w.expires_at>NOW()) "
                        + "AND NOT EXISTS (SELECT 1 FROM biz_reward_transfer pending_t WHERE pending_t.reward_id=w.reward_id AND pending_t.status IN "
                        + "('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN')) "
                        + "AND m.wechat_openid IS NOT NULL AND m.wechat_openid<>'' "
                        + "ORDER BY w.reward_id LIMIT 50");
        for (Map<String, Object> row : rows)
        {
            long memberId = ((Number) row.get("member_id")).longValue();
            String rewardId = String.valueOf(row.get("reward_id"));
            try
            {
                // The stable request ID makes the dispatcher idempotent across
                // retries and process restarts.
                marketing.claimCash(memberId, rewardId, "auto-cash-" + rewardId);
            }
            catch (MiniBusinessException e)
            {
                log.debug("现金红包自动发放暂未完成，rewardId={} reason={}", rewardId, e.getMessage());
            }
            catch (RuntimeException e)
            {
                log.warn("现金红包自动发放失败，rewardId={}", rewardId, e);
            }
        }

        Timestamp due = Timestamp.from(Instant.now().minusSeconds(60));
        List<Map<String, Object>> unsettled = jdbc.queryForList(
                "SELECT w.reward_id,w.member_id,t.status AS transfer_status FROM biz_reward w "
                        + "JOIN biz_member m ON m.member_id=w.member_id "
                        + "JOIN biz_reward_transfer t ON t.reward_id=w.reward_id "
                        + "AND t.transfer_id=(SELECT MAX(latest_t.transfer_id) FROM biz_reward_transfer latest_t WHERE latest_t.reward_id=w.reward_id) "
                        + "WHERE w.reward_type='cash' AND t.status IN ('SUBMITTING','ACCEPTED','PROCESSING','WAIT_USER_CONFIRM','TRANSFERING','CANCELING','UNKNOWN') "
                        + "AND t.update_time<=? "
                        + "AND m.wechat_openid IS NOT NULL AND m.wechat_openid<>'' "
                        + "ORDER BY w.reward_id LIMIT 50", due);
        for (Map<String, Object> row : unsettled)
        {
            try
            {
                long memberId = ((Number) row.get("member_id")).longValue();
                String rewardId = String.valueOf(row.get("reward_id"));
                String state = String.valueOf(row.get("transfer_status"));
                if ("SUBMITTING".equals(state) || "UNKNOWN".equals(state))
                    marketing.claimCash(memberId, rewardId, "auto-cash-" + rewardId);
                else marketing.cashReward(memberId, rewardId);
            }
            catch (RuntimeException e)
            {
                log.warn("现金红包转账补查失败，rewardId={}", row.get("reward_id"), e);
            }
        }
    }
}
