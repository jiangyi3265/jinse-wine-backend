package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class OrderReturnReceiptTest
{
    private JdbcTemplate jdbc;
    private OrderRefundService service;
    private TransactionTemplate tx;

    @Before
    public void setUp()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:return_receipt_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,order_no VARCHAR(64),fulfillment_mode VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_order_refund(refund_id BIGINT PRIMARY KEY,order_id BIGINT,refund_no VARCHAR(64),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_order_item(order_item_id BIGINT PRIMARY KEY,order_id BIGINT,product_id BIGINT,product_name VARCHAR(128),product_spec VARCHAR(128),sku_code VARCHAR(64),quantity INT)");
        jdbc.execute("CREATE TABLE biz_order_delivery(order_id BIGINT,shipped_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_order_aftersale(order_id BIGINT,refund_id BIGINT,order_item_id BIGINT,quantity INT,status VARCHAR(24))");
        jdbc.execute("CREATE TABLE biz_order_pickup_redemption(order_id BIGINT,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_product(product_id BIGINT PRIMARY KEY,stock INT)");
        jdbc.execute("CREATE TABLE biz_product_sku(product_id BIGINT,sku_code VARCHAR(64),stock INT,video_sync_status VARCHAR(16),video_last_error VARCHAR(255),video_retry_at TIMESTAMP,video_retry_count INT,PRIMARY KEY(product_id,sku_code))");
        jdbc.execute("CREATE TABLE biz_order_return_receipt(receipt_id BIGINT AUTO_INCREMENT PRIMARY KEY,refund_id BIGINT,order_id BIGINT,order_item_id BIGINT,quantity INT,restock_quantity INT,receipt_reference VARCHAR(128),request_id VARCHAR(64),received_by VARCHAR(64),received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(refund_id,request_id,order_item_id))");
        jdbc.update("INSERT INTO biz_order VALUES(1,'ORDER-1','delivery')");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1,'REFUND-1','refunded')");
        jdbc.update("INSERT INTO biz_order_item VALUES(11,1,21,'原浆酒','默认规格','',2)");
        jdbc.update("INSERT INTO biz_order_item VALUES(12,1,22,'礼盒酒','双瓶','SKU-22',3)");
        jdbc.update("INSERT INTO biz_order_delivery VALUES(1,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO biz_product VALUES(21,5)");
        jdbc.update("INSERT INTO biz_product_sku VALUES(22,'SKU-22',4,'synced','',NULL,0)");
        service = new OrderRefundService(jdbc, new PointExpiryService(jdbc));
    }

    @Test
    public void receiptRestocksOnlySaleableGoodsAndRequestReplayIsIdempotent()
    {
        Map<String, Object> body = request("receipt_001", line(11, 2, 1), line(12, 2, 0));
        tx.execute(status -> service.recordReturn(1, body, "operator"));
        tx.execute(status -> service.recordReturn(1, body, "operator"));
        assertEquals(2, count("SELECT COUNT(*) FROM biz_order_return_receipt"));
        assertEquals(6, count("SELECT stock FROM biz_product WHERE product_id=21"));
        assertEquals(4, count("SELECT stock FROM biz_product_sku WHERE product_id=22"));
        assertEquals("synced", jdbc.queryForObject("SELECT video_sync_status FROM biz_product_sku WHERE product_id=22", String.class));
        assertTrue((Boolean) service.returnContext(1).get("eligible"));
    }

    @Test
    public void rejectsChangedReplayAndQuantityAboveOrderTotal()
    {
        tx.execute(status -> service.recordReturn(1, request("receipt_002", line(11, 1, 1)), "operator"));
        reject(request("receipt_002", line(11, 1, 0)));
        reject(request("receipt_003", line(11, 2, 2)));
        assertEquals(1, count("SELECT COUNT(*) FROM biz_order_return_receipt"));
        assertEquals(6, count("SELECT stock FROM biz_product WHERE product_id=21"));
    }

    @Test
    public void requiresConfirmedRefundAndFulfillmentEvidence()
    {
        jdbc.update("UPDATE biz_order_refund SET status='approved' WHERE refund_id=1");
        reject(request("receipt_004", line(11, 1, 1)));
        jdbc.update("UPDATE biz_order_refund SET status='refunded' WHERE refund_id=1");
        jdbc.update("DELETE FROM biz_order_delivery WHERE order_id=1");
        reject(request("receipt_005", line(11, 1, 1)));
        assertEquals(0, count("SELECT COUNT(*) FROM biz_order_return_receipt"));
    }

    @Test
    public void concurrentReceiptsCannotRestockMoreThanOrdered() throws Exception
    {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try
        {
            Future<Boolean> first = pool.submit(() -> receiveAfter(start, "concurrent_001"));
            Future<Boolean> second = pool.submit(() -> receiveAfter(start, "concurrent_002"));
            start.countDown();
            int accepted = (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, accepted);
            assertEquals(2, count("SELECT COALESCE(SUM(quantity),0) FROM biz_order_return_receipt"));
            assertEquals(7, count("SELECT stock FROM biz_product WHERE product_id=21"));
        }
        finally { pool.shutdownNow(); }
    }

    private boolean receiveAfter(CountDownLatch start, String requestId)
    {
        try
        {
            start.await();
            tx.execute(status -> service.recordReturn(1, request(requestId, line(11, 2, 2)), "operator"));
            return true;
        }
        catch (MiniBusinessException expected) { return false; }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }

    private void reject(Map<String, Object> body)
    {
        try { tx.execute(status -> service.recordReturn(1, body, "operator")); fail("expected rejection"); }
        catch (MiniBusinessException expected) { }
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }

    private static Map<String, Object> line(long id, int quantity, int restock)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("orderItemId", id);
        result.put("quantity", quantity);
        result.put("restockQuantity", restock);
        return result;
    }

    @SafeVarargs
    private static Map<String, Object> request(String id, Map<String, Object>... items)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", id);
        result.put("receiptReference", "EXPRESS-123456");
        result.put("items", Arrays.asList(items));
        return result;
    }
}
