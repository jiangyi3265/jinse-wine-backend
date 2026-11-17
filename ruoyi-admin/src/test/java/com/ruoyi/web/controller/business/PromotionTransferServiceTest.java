package com.ruoyi.web.controller.business;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.Assert.*;

public class PromotionTransferServiceTest
{
    JdbcTemplate jdbc; TransactionTemplate tx; PromotionTransferService service; FakePay pay;
    @Before public void setup()
    {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds); DataSourceTransactionManager manager = new DataSourceTransactionManager(ds); tx = new TransactionTemplate(manager);
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,wechat_openid VARCHAR(128),status CHAR(1))");
        jdbc.execute("INSERT INTO biz_member VALUES(1,'member-openid','0')");
        jdbc.execute("CREATE TABLE biz_promotion_config(config_id BIGINT PRIMARY KEY,min_withdraw_cent INT,status CHAR(1))");
        jdbc.execute("INSERT INTO biz_promotion_config VALUES(1,100,'0')");
        jdbc.execute("CREATE TABLE biz_promotion_record(promotion_record_id BIGINT PRIMARY KEY,member_id BIGINT,order_id BIGINT,amount_cent INT,status VARCHAR(20),refund_recovery_required INT DEFAULT 0)");
        jdbc.execute("ALTER TABLE biz_promotion_record ADD refund_hold_status VARCHAR(16)");
        jdbc.execute("CREATE TABLE biz_order_refund(order_id BIGINT,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_order_aftersale(order_id BIGINT,status VARCHAR(24))");
        jdbc.execute("CREATE TABLE biz_promotion_settlement(settlement_id BIGINT AUTO_INCREMENT PRIMARY KEY,member_id BIGINT,amount_cent INT,status VARCHAR(20),request_id VARCHAR(64),payment_reference VARCHAR(128),paid_at TIMESTAMP,failure_reason VARCHAR(500))");
        jdbc.execute("CREATE TABLE biz_promotion_settlement_item(settlement_id BIGINT,promotion_record_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_promotion_transfer(settlement_id BIGINT PRIMARY KEY,member_id BIGINT,out_bill_no VARCHAR(32) UNIQUE,transfer_bill_no VARCHAR(64),amount_cent INT,openid VARCHAR(128),status VARCHAR(32),package_info VARCHAR(2048),failure_reason VARCHAR(500),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        pay = new FakePay(); service = new PromotionTransferService(jdbc, pay, null, manager);
    }
    void credit(long id, int amount, String state) { jdbc.update("INSERT INTO biz_promotion_record(promotion_record_id,member_id,amount_cent,status) VALUES(?,1,?,?)",id,amount,state); }
    Map<String,Object> reserve() { return tx.execute(s -> service.reserve(1)); }

    @Test public void offsetsRefundDebtAndNeverReservesPendingOrDuplicateMoney()
    {
        credit(1, 490, "settled"); credit(2, -90, "settled"); credit(3, 100, "pending");
        Map<String,Object> row = reserve();
        assertEquals(400, ((Number)row.get("amount_cent")).intValue()); assertNull(reserve());
        assertEquals("pending", jdbc.queryForObject("SELECT status FROM biz_promotion_record WHERE promotion_record_id=3",String.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM biz_promotion_settlement_item",Integer.class).intValue());
    }
    @Test public void waitingForUserConfirmationDoesNotMarkCommissionPaid()
    {
        credit(1,490,"settled"); reserve(); pay.state="WAIT_USER_CONFIRM"; service.reconcile(1);
        assertEquals("processing",jdbc.queryForObject("SELECT status FROM biz_promotion_record WHERE promotion_record_id=1",String.class));
        assertEquals("WAIT_USER_CONFIRM",service.detail(1,1).get("state"));
        pay.state="SUCCESS"; service.reconcile(1); service.reconcile(1);
        assertEquals("paid",jdbc.queryForObject("SELECT status FROM biz_promotion_record WHERE promotion_record_id=1",String.class));
        assertEquals(1,pay.creates);
    }
    @Test public void ambiguousResponseQueriesTheOriginalBillAndNeverCreatesAnotherSettlement()
    {
        credit(1,490,"settled"); reserve(); pay.unknown=true; service.reconcile(1);
        assertEquals("UNKNOWN",jdbc.queryForObject("SELECT status FROM biz_promotion_transfer",String.class));
        assertNull(reserve()); pay.unknown=false; pay.state="SUCCESS"; service.reconcile(1);
        assertEquals(1,pay.creates); assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM biz_promotion_settlement",Integer.class).intValue());
    }
    @Test public void mismatchAndTerminalFailureCannotProducePaidStatus()
    {
        credit(1,490,"settled"); Map<String,Object> row=reserve();
        Map<String,Object> wrong=pay.result("XSCP1"); wrong.put("amountCent",491);
        assertThrows(MiniBusinessException.class,()->tx.execute(s->{PromotionTransferLedger.apply(jdbc,row,wrong);return null;}));
        pay.state="FAIL"; service.reconcile(1); service.reconcile(1); assertNull(reserve());
        assertEquals(1,pay.creates); assertEquals("failed",jdbc.queryForObject("SELECT status FROM biz_promotion_settlement",String.class));
        assertThrows(MiniBusinessException.class,()->service.detail(2,1));
    }
    @Test public void nonpositiveBalanceAndRealNameThresholdDoNotCreateBills()
    {
        credit(1,-500,"settled"); credit(2,490,"settled"); credit(3,200000,"settled"); assertNull(reserve());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM biz_promotion_transfer",Integer.class).intValue());
    }
    @Test public void unpayableMembersDoNotStarveEligibleMembers()
    {
        for (int id=2; id<=60; id++) {
            jdbc.update("INSERT INTO biz_member VALUES(?,'valid-openid','0')",id);
            jdbc.update("INSERT INTO biz_promotion_record(promotion_record_id,member_id,amount_cent,status) VALUES(?,?,200000,'settled')",id,id);
        }
        jdbc.update("INSERT INTO biz_member VALUES(61,'valid-openid','0')");
        jdbc.update("INSERT INTO biz_promotion_record(promotion_record_id,member_id,amount_cent,status) VALUES(61,61,490,'settled')");
        credit(1,490,"settled"); reserve();
        List<Map<String,Object>> eligible=service.eligibleMembers();
        assertEquals(1,eligible.size()); assertEquals(61,((Number)eligible.get(0).get("member_id")).intValue());
    }
    @Test public void successfulOffsetClearsOnlyFullyRecoveredOriginalOrders()
    {
        credit(1,670,"settled"); credit(2,-90,"settled"); credit(3,-90,"settled");
        credit(4,90,"paid"); credit(5,180,"paid"); credit(6,-90,"pending");
        jdbc.update("UPDATE biz_promotion_record SET order_id=10,refund_recovery_required=1 WHERE promotion_record_id IN (2,4)");
        jdbc.update("UPDATE biz_promotion_record SET order_id=20,refund_recovery_required=1 WHERE promotion_record_id IN (3,5,6)");
        reserve(); service.reconcile(1);
        assertEquals(0,jdbc.queryForObject("SELECT refund_recovery_required FROM biz_promotion_record WHERE promotion_record_id=4",Integer.class).intValue());
        assertEquals(1,jdbc.queryForObject("SELECT refund_recovery_required FROM biz_promotion_record WHERE promotion_record_id=5",Integer.class).intValue());
        assertEquals(1,jdbc.queryForObject("SELECT refund_recovery_required FROM biz_promotion_record WHERE promotion_record_id=6",Integer.class).intValue());
    }

    @Test public void automaticPayoutUsesConfiguredMinimumAndActiveRule()
    {
        jdbc.update("UPDATE biz_promotion_config SET min_withdraw_cent=1000");
        credit(1,999,"settled");
        assertNull(reserve()); assertTrue(service.eligibleMembers().isEmpty());
        credit(2,1,"settled");
        jdbc.update("UPDATE biz_promotion_config SET status='1'");
        assertNull(reserve()); assertTrue(service.eligibleMembers().isEmpty());
        jdbc.update("UPDATE biz_promotion_config SET status='0'");
        assertEquals(1,service.eligibleMembers().size());
        assertEquals(1000,((Number)reserve().get("amount_cent")).intValue());
    }

    @Test public void onlyReviewedTerminalFailureCanReleaseAndCreateANewBill()
    {
        credit(1,490,"settled"); reserve(); pay.unknown=true; service.reconcile(1);
        OrderRefundService refunds=new OrderRefundService(jdbc,null);
        assertThrows(MiniBusinessException.class,()->tx.execute(s->{PromotionTransferLedger.releaseFailed(jdbc,refunds,1,"review");return null;}));
        pay.unknown=false; pay.state="FAIL"; service.reconcile(1);
        assertNull(reserve());
        tx.execute(s->{PromotionTransferLedger.releaseFailed(jdbc,refunds,1,"Verified failure");return null;});
        assertEquals("settled",jdbc.queryForObject("SELECT status FROM biz_promotion_record",String.class));
        Map<String,Object> retry=reserve();
        assertEquals("XSCP2",retry.get("out_bill_no"));
        // Replaying recovery of the old bill must not release the new reservation.
        tx.execute(s->{PromotionTransferLedger.releaseFailed(jdbc,refunds,1,"Verified failure");return null;});
        assertEquals("processing",jdbc.queryForObject("SELECT status FROM biz_promotion_record",String.class));
        assertEquals("released",jdbc.queryForObject("SELECT status FROM biz_promotion_settlement WHERE settlement_id=1",String.class));
    }

    @Test public void reviewedFailureStillHoldsCommissionForAnOpenRefund()
    {
        credit(1,490,"settled"); jdbc.update("UPDATE biz_promotion_record SET order_id=10");
        reserve(); pay.state="CANCELLED"; service.reconcile(1);
        jdbc.update("INSERT INTO biz_order_refund VALUES(10,'pending')");
        tx.execute(s->{PromotionTransferLedger.releaseFailed(jdbc,new OrderRefundService(jdbc,null),1,"Verified cancellation");return null;});
        assertEquals("closed",jdbc.queryForObject("SELECT status FROM biz_promotion_record",String.class));
        assertNull(reserve());
    }
    static class FakePay extends WechatPayService
    {
        int creates; boolean submitted,unknown; String state="SUCCESS";
        FakePay(){super(null,new ObjectMapper(),null);}
        @Override public boolean isCommissionTransferConfigured(){return true;}
        @Override public Map<String,Object> cashTransferClientInfo(){return Collections.singletonMap("appId","app");}
        @Override public Map<String,Object> queryCashTransfer(String bill){return submitted ? result(bill) : null;}
        @Override public Map<String,Object> createCommissionTransfer(String bill,int amount,String openid){creates++;submitted=true;if(unknown)throw new MiniBusinessException(502,"timeout");return result(bill);}
        Map<String,Object> result(String bill){Map<String,Object> r=new HashMap<>();r.put("outBillNo",bill);r.put("openid","member-openid");r.put("amountCent",490);r.put("state",state);r.put("transferBillNo","wx-bill");r.put("packageInfo","package");r.put("failReason","");return r;}
    }
}
