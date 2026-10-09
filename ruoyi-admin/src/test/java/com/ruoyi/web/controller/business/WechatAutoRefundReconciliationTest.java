package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WechatAutoRefundReconciliationTest
{
    private JdbcTemplate jdbc;
    private FakePay pay;
    private FakeRefunds refunds;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:auto_refund_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,order_no VARCHAR(64),total_cent INT,wechat_transaction_id VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_order_refund(refund_id BIGINT PRIMARY KEY,order_id BIGINT,member_id BIGINT,refund_no VARCHAR(64),amount_cent INT,reason VARCHAR(255),status VARCHAR(16),channel_status VARCHAR(32),channel_refund_id VARCHAR(64),update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO biz_order(order_id,order_no,total_cent,wechat_transaction_id) VALUES(1,'JS100',1000,'WX100')");
        jdbc.update("INSERT INTO biz_order_refund(refund_id,order_id,member_id,refund_no,amount_cent,reason,status,channel_status) VALUES(1,1,7,'RF100',1000,?,'approved','PENDING_AUTO')",
                "系统自动退款：订单取消后收到支付成功通知");
        refunds = new FakeRefunds(jdbc);
        pay = new FakePay(jdbc, refunds);
        field("enabled", true);
        field("appId", "app");
        field("merchantId", "merchant");
        field("merchantSerial", "serial");
        field("privateKeyPem", "key");
        field("apiV3Key", "12345678901234567890123456789012");
        field("notifyUrl", "https://example.com/pay");
        field("refundNotifyUrl", "https://example.com/refund");
        field("platformCertPem", "cert");
    }

    @Test
    public void unknownSubmissionIsQueriedUntilWechatConfirmsRefund()
    {
        pay.postFailure = new IOException("timeout");
        pay.refundLatePayments();
        assertEquals("UNKNOWN", channelStatus());
        assertEquals(1, pay.postCalls);

        makeDue();
        pay.getResponse = refundResponse("PROCESSING", "WX100");
        pay.refundLatePayments();
        assertEquals("PROCESSING", channelStatus());
        assertEquals("approved", businessStatus());

        makeDue();
        pay.getResponse = refundResponse("SUCCESS", "WX100");
        pay.refundLatePayments();
        assertEquals("SUCCESS", channelStatus());
        assertEquals("refunded", businessStatus());
        assertEquals(1, refunds.calls);
    }

    @Test
    public void confirmedNotFoundResubmitsUsingSameRefundNumber()
    {
        pay.postFailure = new IOException("timeout");
        pay.refundLatePayments();
        makeDue();
        pay.getFailure = new WechatPayService.WechatApiException(404);
        pay.refundLatePayments();
        assertEquals("PENDING_AUTO", channelStatus());

        pay.postFailure = null;
        pay.postResponse = refundResponse("PROCESSING", "WX100");
        pay.refundLatePayments();
        assertEquals("PROCESSING", channelStatus());
        assertEquals(2, pay.postCalls);
        assertTrue(pay.lastPostBody.contains("\"out_refund_no\":\"RF100\""));
        assertTrue(pay.lastPostBody.contains("系统自动退款"));
    }

    @Test
    public void mismatchedRefundResponseRequiresReview()
    {
        pay.postResponse = refundResponse("PROCESSING", "OTHER-TRANSACTION");
        pay.refundLatePayments();
        assertEquals("REVIEW", channelStatus());
        pay.refundLatePayments();
        assertEquals(1, pay.postCalls);
        assertEquals("approved", businessStatus());
    }

    @Test
    public void merchantRejectionAndMismatchedQueryDoNotConfirmRefund()
    {
        pay.postFailure = new WechatPayService.WechatApiException(400);
        pay.refundLatePayments();
        assertEquals("REVIEW", channelStatus());
        assertEquals("approved", businessStatus());

        jdbc.update("UPDATE biz_order_refund SET channel_status='UNKNOWN' WHERE refund_id=1");
        makeDue();
        pay.getResponse = refundResponse("SUCCESS", "OTHER-TRANSACTION");
        pay.refundLatePayments();
        assertEquals("REVIEW", channelStatus());
        assertEquals("approved", businessStatus());
        assertEquals(0, refunds.calls);
    }

    @Test
    public void previouslyAcceptedRefundMissingFromMerchantRequiresReview()
    {
        jdbc.update("UPDATE biz_order_refund SET channel_status='PROCESSING' WHERE refund_id=1");
        makeDue();
        pay.getFailure = new WechatPayService.WechatApiException(404);
        pay.refundLatePayments();
        assertEquals("REVIEW", channelStatus());
        assertEquals("approved", businessStatus());
    }

    @Test
    public void confirmedRefundRetriesLocalLedgerIfItInitiallyFails()
    {
        jdbc.update("UPDATE biz_order_refund SET channel_status='SUCCESS' WHERE refund_id=1");
        makeDue();
        refunds.failOnce = true;
        pay.refundLatePayments();
        assertEquals("SUCCESS", channelStatus());
        assertEquals("approved", businessStatus());
        makeDue();
        pay.refundLatePayments();
        assertEquals("refunded", businessStatus());
        assertEquals(2, refunds.calls);
    }

    private String refundResponse(String status, String transactionId)
    {
        return "{\"refund_id\":\"WR100\",\"out_refund_no\":\"RF100\",\"out_trade_no\":\"JS100\","
                + "\"transaction_id\":\"" + transactionId + "\",\"status\":\"" + status + "\","
                + "\"amount\":{\"refund\":1000,\"total\":1000,\"currency\":\"CNY\"}}";
    }

    private void makeDue()
    {
        jdbc.update("UPDATE biz_order_refund SET update_time=? WHERE refund_id=1", Timestamp.from(Instant.now().minusSeconds(300)));
    }

    private String channelStatus() { return jdbc.queryForObject("SELECT channel_status FROM biz_order_refund WHERE refund_id=1", String.class); }
    private String businessStatus() { return jdbc.queryForObject("SELECT status FROM biz_order_refund WHERE refund_id=1", String.class); }

    private void field(String name, Object value) throws Exception
    {
        Field field = WechatPayService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(pay, value);
    }

    private static final class FakeRefunds extends OrderRefundService
    {
        private final JdbcTemplate jdbc;
        private boolean failOnce;
        private int calls;

        private FakeRefunds(JdbcTemplate jdbc)
        {
            super(jdbc, null);
            this.jdbc = jdbc;
        }

        @Override
        public void markRefunded(long refundId, String channel)
        {
            calls++;
            if (failOnce)
            {
                failOnce = false;
                throw new MiniBusinessException(503, "temporary database failure");
            }
            jdbc.update("UPDATE biz_order_refund SET status='refunded',channel_status='SUCCESS' WHERE refund_id=?", refundId);
        }
    }

    private static final class FakePay extends WechatPayService
    {
        private Exception postFailure;
        private Exception getFailure;
        private String postResponse;
        private String getResponse;
        private String lastPostBody;
        private int postCalls;

        private FakePay(JdbcTemplate jdbc, OrderRefundService refunds)
        {
            super(jdbc, new ObjectMapper(), refunds);
        }

        @Override
        protected String request(String method, String path, String body) throws Exception
        {
            if ("POST".equals(method))
            {
                postCalls++;
                lastPostBody = body;
                if (postFailure != null) throw postFailure;
                return postResponse;
            }
            if (getFailure != null) throw getFailure;
            return getResponse;
        }
    }
}
