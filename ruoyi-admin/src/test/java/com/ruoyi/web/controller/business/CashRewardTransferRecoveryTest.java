package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CashRewardTransferRecoveryTest
{
    private JdbcTemplate jdbc;
    private FakePay pay;
    private MiniMarketingService marketing;

    @Before
    public void setUp()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:cash_recovery_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,wechat_openid VARCHAR(128))");
        jdbc.execute("CREATE TABLE biz_reward(reward_id BIGINT PRIMARY KEY,member_id BIGINT,reward_type VARCHAR(16),amount_cent INT,status VARCHAR(16),expires_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_reward_transfer(transfer_id BIGINT PRIMARY KEY,reward_id BIGINT,member_id BIGINT,request_id VARCHAR(64),out_bill_no VARCHAR(32),transfer_bill_no VARCHAR(64),amount_cent INT,openid VARCHAR(128),status VARCHAR(32),package_info VARCHAR(255),fail_reason VARCHAR(255),update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO biz_member(member_id,wechat_openid) VALUES(7,'openid-7')");
        jdbc.update("INSERT INTO biz_reward(reward_id,member_id,reward_type,amount_cent,status,expires_at) VALUES(1,7,'cash',500,'processing',?)",
                Timestamp.from(Instant.now().minusSeconds(3600)));
        jdbc.update("INSERT INTO biz_reward_transfer(transfer_id,reward_id,member_id,request_id,out_bill_no,amount_cent,openid,status,update_time) VALUES(1,1,7,'auto-cash-1','JW-1-1',500,'openid-7','UNKNOWN',?)",
                Timestamp.from(Instant.now().minusSeconds(300)));
        pay = new FakePay(jdbc);
        marketing = new MiniMarketingService(jdbc, null, null, pay);
    }

    @Test
    public void unknownTransferIsReissuedWithOriginalBillEvenAfterRewardExpiry()
    {
        CashRewardTransferJob job = new CashRewardTransferJob(jdbc, marketing, pay);
        job.dispatchPendingCashRewards();
        assertEquals(MiniMarketingService.cashBillNo(1, 1), pay.createdBillNo);
        assertEquals(pay.createdBillNo, jdbc.queryForObject("SELECT out_bill_no FROM biz_reward_transfer WHERE transfer_id=1", String.class));
        assertEquals(1, pay.queryCalls);
        assertEquals(1, pay.createCalls);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM biz_reward_transfer WHERE reward_id=1", Integer.class).intValue());
        assertEquals("SUCCESS", jdbc.queryForObject("SELECT status FROM biz_reward_transfer WHERE transfer_id=1", String.class));
        assertEquals("received", jdbc.queryForObject("SELECT status FROM biz_reward WHERE reward_id=1", String.class));

        job.dispatchPendingCashRewards();
        assertEquals(1, pay.createCalls);
    }

    @Test
    public void submittingTransferUsesOriginalBillWhenMerchantHasNoRecord()
    {
        jdbc.update("UPDATE biz_reward_transfer SET status='SUBMITTING' WHERE transfer_id=1");
        new CashRewardTransferJob(jdbc, marketing, pay).dispatchPendingCashRewards();
        assertEquals(MiniMarketingService.cashBillNo(1, 1), pay.createdBillNo);
        assertEquals(1, pay.createCalls);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM biz_reward_transfer WHERE reward_id=1", Integer.class).intValue());
    }

    @Test
    public void confirmedMerchantBillIsNeverSubmittedAgain()
    {
        pay.queryResponse = pay.successResult("JW-1-1", 500, "openid-7");
        new CashRewardTransferJob(jdbc, marketing, pay).dispatchPendingCashRewards();
        assertEquals(1, pay.queryCalls);
        assertEquals(0, pay.createCalls);
        assertEquals("SUCCESS", jdbc.queryForObject("SELECT status FROM biz_reward_transfer WHERE transfer_id=1", String.class));
        assertEquals("received", jdbc.queryForObject("SELECT status FROM biz_reward WHERE reward_id=1", String.class));
    }

    @Test
    public void acceptedTransfersRemainQueryOnly()
    {
        RoutingMarketing routing = new RoutingMarketing(jdbc, pay);
        CashRewardTransferJob job = new CashRewardTransferJob(jdbc, routing, pay);
        jdbc.update("UPDATE biz_reward_transfer SET status='PROCESSING' WHERE transfer_id=1");
        job.dispatchPendingCashRewards();
        assertEquals(0, routing.claimCalls);
        assertEquals(1, routing.queryCalls);

        jdbc.update("UPDATE biz_reward_transfer SET status='WAIT_USER_CONFIRM' WHERE transfer_id=1");
        job.dispatchPendingCashRewards();
        assertEquals(0, routing.claimCalls);
        assertEquals(2, routing.queryCalls);
    }

    @Test
    public void expiredRewardWithoutExistingBillCannotStartTransfer()
    {
        jdbc.update("DELETE FROM biz_reward_transfer WHERE transfer_id=1");
        assertEquals(409, assertThrows(MiniBusinessException.class,
                () -> marketing.claimCash(7, "1", "auto-cash-1")).getCode());
        assertEquals(0, pay.createCalls);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM biz_reward_transfer", Integer.class).intValue());
    }

    private static final class RoutingMarketing extends MiniMarketingService
    {
        private int claimCalls;
        private int queryCalls;

        private RoutingMarketing(JdbcTemplate jdbc, WechatPayService pay)
        {
            super(jdbc, null, null, pay);
        }

        @Override public Map<String, Object> claimCash(long memberId, String id, String requestId)
        {
            claimCalls++;
            return new LinkedHashMap<>();
        }

        @Override public Map<String, Object> cashReward(long memberId, String id)
        {
            queryCalls++;
            return new LinkedHashMap<>();
        }
    }

    private static final class FakePay extends WechatPayService
    {
        private String createdBillNo;
        private Map<String, Object> queryResponse;
        private int createCalls;
        private int queryCalls;

        private FakePay(JdbcTemplate jdbc)
        {
            super(jdbc, new ObjectMapper(), null);
        }

        @Override public boolean isTransferConfigured() { return true; }

        @Override public Map<String, Object> queryCashTransfer(String outBillNo)
        {
            queryCalls++;
            return queryResponse;
        }

        @Override public Map<String, Object> createCashTransfer(String outBillNo, int amountCent, String openid)
        {
            createCalls++;
            createdBillNo = outBillNo;
            return successResult(outBillNo, amountCent, openid);
        }

        private Map<String, Object> successResult(String outBillNo, int amountCent, String openid)
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("outBillNo", outBillNo);
            result.put("transferBillNo", "WX-BILL-1");
            result.put("amountCent", amountCent);
            result.put("openid", openid);
            result.put("state", "SUCCESS");
            result.put("packageInfo", "");
            result.put("failReason", "");
            return result;
        }
    }
}
