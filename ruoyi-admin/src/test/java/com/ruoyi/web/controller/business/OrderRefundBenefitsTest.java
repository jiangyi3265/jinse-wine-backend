package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real ledger SQL in isolated databases; no payment client or network access. */
public class OrderRefundBenefitsTest
{
    private JdbcTemplate jdbc;
    private OrderRefundService service;
    private TransactionTemplate tx;

    @Before
    public void setUp()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:refund_benefits_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,member_id BIGINT,total_cent BIGINT,status VARCHAR(16),coupon_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_order_refund(refund_id BIGINT PRIMARY KEY,order_id BIGINT,amount_cent BIGINT,status VARCHAR(16),channel_status VARCHAR(32))");
        jdbc.execute("CREATE TABLE biz_member_coupon(member_coupon_id BIGINT PRIMARY KEY,status VARCHAR(16),order_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_promotion_config(config_id BIGINT,refund_policy VARCHAR(16),status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_promotion_record(promotion_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,member_id BIGINT,order_id BIGINT,title VARCHAR(128),amount_cent BIGINT,original_amount_cent BIGINT,refund_adjustment_cent BIGINT DEFAULT 0,refund_hold_status VARCHAR(16),status VARCHAR(16),refund_recovery_required INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE biz_point_account(member_id BIGINT PRIMARY KEY,balance INT)");
        jdbc.execute("CREATE TABLE biz_point_record(point_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,member_id BIGINT,source_type VARCHAR(32),source_id VARCHAR(64),change_amount INT,balance_after INT,title VARCHAR(128),expires_at TIMESTAMP)");
        jdbc.update("INSERT INTO biz_order VALUES(1,10,10000,'complete',NULL)");
        jdbc.execute("ALTER TABLE biz_order ADD shipping_cent BIGINT DEFAULT 0");
        jdbc.execute("CREATE TABLE biz_order_aftersale(order_id BIGINT,status VARCHAR(24))");
        jdbc.update("INSERT INTO biz_promotion_config VALUES(1,'reverse','0')");
        jdbc.update("INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,status) VALUES(20,1,'original',1000,1000,'pending')");
        jdbc.update("INSERT INTO biz_point_account VALUES(10,100)");
        jdbc.update("INSERT INTO biz_point_record(member_id,source_type,source_id,change_amount,balance_after,title) VALUES(10,'order','1',100,100,'original')");
        service = new OrderRefundService(jdbc, new PointExpiryService(jdbc));
    }

    @Test
    public void preCompletionRefundIsDeductedOnceAndLaterRefundUsesCumulativeTotal()
    {
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'refunded',NULL)");
        reconcile();
        reconcile();
        assertNet(80, 800);
        assertEquals(1, count("SELECT COUNT(*) FROM biz_point_record WHERE source_type='order_refund'"));
        jdbc.update("INSERT INTO biz_order_refund VALUES(2,1,3000,'approved',NULL)");
        tx.execute(status -> { service.markRefunded(2, "manual"); return null; });
        reconcile();
        assertNet(50, 500);
        assertEquals(2, count("SELECT COUNT(*) FROM biz_point_record WHERE source_type='order_refund'"));
    }

    @Test
    public void simultaneousReconciliationCannotDebitPointsTwice() throws Exception
    {
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'refunded',NULL)");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try
        {
            Future<?> a = pool.submit(() -> { await(start); reconcile(); });
            Future<?> b = pool.submit(() -> { await(start); reconcile(); });
            start.countDown();
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
            assertNet(80, 800);
            assertEquals(1, count("SELECT COUNT(*) FROM biz_point_record WHERE source_type='order_refund'"));
        }
        finally { pool.shutdownNow(); }
    }

    @Test
    public void pendingRefundHoldsCommissionAndRejectionRestoresOriginalState()
    {
        jdbc.update("UPDATE biz_promotion_record SET status='settled'");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'pending',NULL)");
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        assertEquals("closed", text("SELECT status FROM biz_promotion_record"));
        assertEquals("settled", text("SELECT refund_hold_status FROM biz_promotion_record"));
        jdbc.update("UPDATE biz_order_refund SET status='rejected'");
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        assertEquals("settled", text("SELECT status FROM biz_promotion_record"));
        assertNet(100, 1000);
    }

    @Test
    public void processingWithdrawalKeepsFixedAmountAndBooksOneRecovery()
    {
        jdbc.update("UPDATE biz_promotion_record SET status='processing'");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'approved',NULL)");
        tx.execute(status -> { service.markRefunded(1, "wechat"); return null; });
        tx.execute(status -> { service.markRefunded(1, "wechat"); return null; });
        reconcile();
        assertNet(80, 800);
        assertEquals(1000, count("SELECT amount_cent FROM biz_promotion_record WHERE status='processing'"));
        assertEquals(-200, count("SELECT amount_cent FROM biz_promotion_record WHERE original_amount_cent=0"));
        assertEquals(2, count("SELECT COUNT(*) FROM biz_promotion_record"));
    }

    @Test
    public void secondRefundCannotHideExistingRecoveryAndRejectionKeepsDebt()
    {
        jdbc.update("UPDATE biz_promotion_record SET status='paid'");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'approved',NULL)");
        tx.execute(status -> { service.markRefunded(1, "wechat"); return null; });
        jdbc.update("INSERT INTO biz_order_refund VALUES(2,1,3000,'pending',NULL)");
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        assertEquals("settled", text("SELECT status FROM biz_promotion_record WHERE amount_cent<0"));
        assertEquals(-200, count("SELECT SUM(amount_cent) FROM biz_promotion_record WHERE status='settled'"));
        jdbc.update("UPDATE biz_order_refund SET status='rejected' WHERE refund_id=2");
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        assertEquals(-200, count("SELECT SUM(amount_cent) FROM biz_promotion_record WHERE status='settled'"));
        assertEquals(1, count("SELECT COUNT(*) FROM biz_promotion_record WHERE amount_cent<0"));
    }

    @Test
    public void successiveConfirmedRefundsKeepBothRecoveriesWithoutDuplicateDebt()
    {
        jdbc.update("UPDATE biz_promotion_record SET status='paid'");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'approved',NULL)");
        tx.execute(status -> { service.markRefunded(1, "wechat"); return null; });
        jdbc.update("INSERT INTO biz_order_refund VALUES(2,1,3000,'approved',NULL)");
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        tx.execute(status -> { service.markRefunded(2, "wechat"); return null; });
        tx.execute(status -> { service.markRefunded(2, "wechat"); return null; });
        reconcile();
        assertEquals(-500, count("SELECT SUM(amount_cent) FROM biz_promotion_record WHERE status='settled'"));
        assertEquals(2, count("SELECT COUNT(*) FROM biz_promotion_record WHERE amount_cent<0 AND refund_hold_status IS NULL"));
        assertEquals(1000, count("SELECT amount_cent FROM biz_promotion_record WHERE status='paid'"));
        assertNet(50, 500);
    }

    @Test
    public void previouslyHeldRecoveryReopensWhilePositiveCommissionRemainsHeld()
    {
        jdbc.update("INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,"
                + "refund_adjustment_cent,status,refund_hold_status,refund_recovery_required) "
                + "VALUES(20,1,'recovery',-200,0,200,'closed','settled',1)");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'pending',NULL)");
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        assertEquals("settled", text("SELECT status FROM biz_promotion_record WHERE amount_cent<0"));
        assertEquals(1, count("SELECT COUNT(*) FROM biz_promotion_record WHERE amount_cent<0 AND refund_hold_status IS NULL"));
        assertEquals("closed", text("SELECT status FROM biz_promotion_record WHERE amount_cent>0"));
        assertEquals("pending", text("SELECT refund_hold_status FROM biz_promotion_record WHERE amount_cent>0"));
    }

    @Test
    public void withdrawalOffsetsExistingDebtAndRepeatedRequestDoesNotSelectItTwice()
    {
        jdbc.update("UPDATE biz_promotion_record SET status='paid'");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'approved',NULL)");
        tx.execute(status -> { service.markRefunded(1, "wechat"); return null; });
        jdbc.update("INSERT INTO biz_order_refund VALUES(2,1,3000,'pending',NULL)");
        tx.execute(status -> { service.syncPromotionRefundHold(1); return null; });
        jdbc.update("INSERT INTO biz_promotion_record(member_id,order_id,title,amount_cent,original_amount_cent,status) "
                + "VALUES(20,2,'new earnings',1000,1000,'settled')");
        jdbc.execute("ALTER TABLE biz_promotion_config ADD min_withdraw_cent INT DEFAULT 1");
        jdbc.execute("ALTER TABLE biz_promotion_config ADD settlement_days INT DEFAULT 0");
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,status CHAR(1))");
        jdbc.update("INSERT INTO biz_member VALUES(20,'0')");
        jdbc.execute("CREATE TABLE biz_promotion_settlement(settlement_id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "member_id BIGINT,amount_cent BIGINT,status VARCHAR(16),request_id VARCHAR(64),"
                + "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(member_id,request_id))");
        jdbc.execute("CREATE TABLE biz_promotion_settlement_item(settlement_id BIGINT,promotion_record_id BIGINT,"
                + "PRIMARY KEY(settlement_id,promotion_record_id))");
        MiniBusinessExtensionService promotions = new MiniBusinessExtensionService(jdbc, new ObjectMapper(),
                new PointExpiryService(jdbc), service) {
            // H2 cannot execute the MySQL maturity UPDATE JOIN; records here are already matured.
            @Override public int settleMaturedPromotions() { return 0; }
        };
        try
        {
            tx.execute(status -> promotions.withdraw(20, 1000, "withdraw-gross-rejected"));
            fail("Withdrawal ignored existing refund debt");
        }
        catch (MiniBusinessException expected) { assertEquals(409, expected.getCode()); }
        assertEquals(0, count("SELECT COUNT(*) FROM biz_promotion_settlement"));
        tx.execute(status -> promotions.withdraw(20, 800, "withdraw-net-accepted"));
        tx.execute(status -> promotions.withdraw(20, 800, "withdraw-net-accepted"));
        assertEquals(1, count("SELECT COUNT(*) FROM biz_promotion_settlement"));
        assertEquals(800, count("SELECT amount_cent FROM biz_promotion_settlement"));
        assertEquals(2, count("SELECT COUNT(*) FROM biz_promotion_settlement_item"));
        assertEquals(-200, count("SELECT amount_cent FROM biz_promotion_record WHERE amount_cent<0 AND status='processing'"));
        assertEquals(1, count("SELECT COUNT(*) FROM biz_promotion_record WHERE status='paid'"));
    }

    @Test
    public void spentPointsBecomeDebtButDoNotBlockRefund()
    {
        jdbc.update("UPDATE biz_point_account SET balance=10");
        jdbc.update("INSERT INTO biz_point_record(member_id,source_type,source_id,change_amount,balance_after,title) VALUES(10,'exchange','x',-90,10,'spent')");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'refunded',NULL)");
        reconcile();
        reconcile();
        assertNet(-10, 800);
    }

    @Test
    public void unfinishedOrderIsRejectedWithoutChangingLedger()
    {
        jdbc.update("UPDATE biz_order SET status='receiving'");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,2000,'refunded',NULL)");
        try { reconcile(); fail("Unfinished order accepted"); }
        catch (MiniBusinessException expected) { }
        assertNet(100, 1000);
        assertEquals(1, count("SELECT COUNT(*) FROM biz_point_record"));
    }

    private void reconcile() { tx.execute(status -> { service.reconcileCompletedOrderBenefits(1); return null; }); }
    private void assertNet(int points, int commission)
    {
        assertEquals(points, count("SELECT balance FROM biz_point_account WHERE member_id=10"));
        assertEquals(points, count("SELECT SUM(change_amount) FROM biz_point_record WHERE member_id=10"));
        assertEquals(commission, count("SELECT SUM(amount_cent) FROM biz_promotion_record WHERE member_id=20"));
    }
    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private String text(String sql) { return jdbc.queryForObject(sql, String.class); }
    private static void await(CountDownLatch latch)
    {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start timeout"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
}
