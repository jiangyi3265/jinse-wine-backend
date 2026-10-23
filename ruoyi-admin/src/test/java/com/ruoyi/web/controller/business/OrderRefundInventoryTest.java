package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class OrderRefundInventoryTest
{
    private JdbcTemplate jdbc;
    private OrderRefundService service;

    @Before
    public void setUp()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:refund_inventory_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,member_id BIGINT,total_cent BIGINT,status VARCHAR(16),coupon_id BIGINT)");
        jdbc.execute("ALTER TABLE biz_order ADD shipping_cent BIGINT DEFAULT 0");
        jdbc.execute("CREATE TABLE biz_order_aftersale(order_id BIGINT,status VARCHAR(24))");
        jdbc.execute("CREATE TABLE biz_order_refund(refund_id BIGINT PRIMARY KEY,order_id BIGINT,amount_cent BIGINT,status VARCHAR(16),channel_status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_product(product_id BIGINT PRIMARY KEY,stock INT)");
        jdbc.execute("CREATE TABLE biz_product_sku(product_id BIGINT,sku_code VARCHAR(64),stock INT,video_sync_status VARCHAR(16),video_last_error VARCHAR(255),video_retry_at TIMESTAMP,video_retry_count INT,PRIMARY KEY(product_id,sku_code))");
        jdbc.execute("CREATE TABLE biz_order_item(order_id BIGINT,product_id BIGINT,sku_code VARCHAR(64),quantity INT)");
        jdbc.execute("CREATE TABLE biz_member_coupon(member_coupon_id BIGINT PRIMARY KEY,status VARCHAR(16),order_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_promotion_config(config_id BIGINT,refund_policy VARCHAR(16),status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_promotion_record(promotion_record_id BIGINT,member_id BIGINT,order_id BIGINT,refund_hold_status VARCHAR(16),status VARCHAR(16),amount_cent BIGINT,original_amount_cent BIGINT,refund_adjustment_cent BIGINT,refund_recovery_required INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE biz_point_account(member_id BIGINT PRIMARY KEY,balance INT)");
        jdbc.execute("CREATE TABLE biz_point_record(point_record_id BIGINT,member_id BIGINT,source_type VARCHAR(32),source_id VARCHAR(64),change_amount INT)");
        service = new OrderRefundService(jdbc, new PointExpiryService(jdbc));
    }

    @Test
    public void fullPaidRefundRestoresPlainAndSkuInventoryOnce()
    {
        order(1, "paid", 100, 1);
        jdbc.update("INSERT INTO biz_product VALUES(10,4),(20,9)");
        jdbc.update("INSERT INTO biz_product_sku VALUES(20,'large',9,'success','old error',CURRENT_TIMESTAMP,2)");
        jdbc.update("INSERT INTO biz_order_item VALUES(1,10,'',2),(1,20,'large',3)");
        jdbc.update("INSERT INTO biz_member_coupon VALUES(1,'locked',1)");
        refund(1, 1, 100);

        service.markRefunded(1, "manual");
        service.markRefunded(1, "manual");

        assertEquals(6, count("SELECT stock FROM biz_product WHERE product_id=10"));
        assertEquals(12, count("SELECT stock FROM biz_product_sku WHERE product_id=20 AND sku_code='large'"));
        assertEquals("pending", jdbc.queryForObject("SELECT video_sync_status FROM biz_product_sku WHERE product_id=20", String.class));
        assertEquals(0, count("SELECT video_retry_count FROM biz_product_sku WHERE product_id=20"));
        assertEquals("refunded", jdbc.queryForObject("SELECT status FROM biz_order WHERE order_id=1", String.class));
        assertEquals("unused", jdbc.queryForObject("SELECT status FROM biz_member_coupon WHERE member_coupon_id=1", String.class));
    }

    @Test
    public void partialAndShippedRefundsDoNotRestock()
    {
        order(2, "paid", 100, null);
        order(3, "shipping", 100, null);
        jdbc.update("INSERT INTO biz_product VALUES(10,4)");
        jdbc.update("INSERT INTO biz_order_item VALUES(2,10,'',2),(3,10,'',1)");
        refund(2, 2, 50);
        refund(3, 3, 100);

        service.markRefunded(2, "manual");
        service.markRefunded(3, "manual");

        assertEquals(4, count("SELECT stock FROM biz_product WHERE product_id=10"));
        assertEquals("paid", jdbc.queryForObject("SELECT status FROM biz_order WHERE order_id=2", String.class));
        assertEquals("refunded", jdbc.queryForObject("SELECT status FROM biz_order WHERE order_id=3", String.class));
    }

    @Test
    public void cumulativeFullRefundRestocksOnlyAfterFinalConfirmation()
    {
        order(5, "paid", 100, null);
        jdbc.update("INSERT INTO biz_product VALUES(10,4)");
        jdbc.update("INSERT INTO biz_order_item VALUES(5,10,'',2)");
        refund(5, 5, 40);
        refund(6, 5, 60);

        service.markRefunded(5, "manual");
        assertEquals(4, count("SELECT stock FROM biz_product WHERE product_id=10"));
        service.markRefunded(6, "manual");
        service.markRefunded(5, "manual");
        assertEquals(6, count("SELECT stock FROM biz_product WHERE product_id=10"));
    }

    @Test
    public void cancelledLatePaymentRefundDoesNotRestockTwice()
    {
        order(4, "cancelled", 100, null);
        jdbc.update("INSERT INTO biz_product VALUES(10,4)");
        jdbc.update("INSERT INTO biz_order_item VALUES(4,10,'',2)");
        refund(4, 4, 100);

        service.markRefunded(4, "wechat");

        assertEquals(4, count("SELECT stock FROM biz_product WHERE product_id=10"));
        assertEquals("cancelled", jdbc.queryForObject("SELECT status FROM biz_order WHERE order_id=4", String.class));
    }

    @Test
    public void shippedMerchandiseRefundClosesOrderWithoutRestoringInventory()
    {
        order(8, "receiving", 150, null);
        jdbc.update("UPDATE biz_order SET shipping_cent=50 WHERE order_id=8");
        jdbc.update("INSERT INTO biz_product VALUES(10,4)");
        jdbc.update("INSERT INTO biz_order_item VALUES(8,10,'',2)");
        refund(8, 8, 100);
        service.markRefunded(8, "manual");
        assertEquals("refunded", jdbc.queryForObject("SELECT status FROM biz_order WHERE order_id=8", String.class));
        assertEquals(4, count("SELECT stock FROM biz_product WHERE product_id=10"));
    }

    private void order(long id, String status, long total, Integer couponId)
    {
        jdbc.update("INSERT INTO biz_order(order_id,member_id,total_cent,status,coupon_id) VALUES(?,1,?,?,?)", id, total, status, couponId);
    }

    private void refund(long id, long orderId, long amount)
    {
        jdbc.update("INSERT INTO biz_order_refund VALUES(?,?,?,'approved',NULL)", id, orderId, amount);
    }

    private int count(String sql)
    {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
