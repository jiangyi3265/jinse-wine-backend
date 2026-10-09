package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class VideoFeedMountConcurrencyTest
{
    @Test
    public void productDeletionWaitsForMountCreation() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:video_mount_lock_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=3000");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_product(product_id BIGINT PRIMARY KEY, product_name VARCHAR(128))");
        jdbc.execute("CREATE TABLE biz_video_feed_mount(mount_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "product_id BIGINT, external_product_id VARCHAR(128), feed_id VARCHAR(128), "
                + "nonce_id VARCHAR(128), mount_title VARCHAR(128), status VARCHAR(24), "
                + "last_error VARCHAR(255), request_id VARCHAR(64), submitted_at TIMESTAMP, "
                + "verified_at TIMESTAMP)");
        jdbc.update("INSERT INTO biz_product VALUES(1,'测试商品')");
        VideoFeedMountService service = new VideoFeedMountService(jdbc);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        CountDownLatch saved = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch checking = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try
        {
            Future<?> create = workers.submit(() -> transaction.execute(status -> {
                Map<String, Object> body = new HashMap<>();
                body.put("productId", 1);
                body.put("externalProductId", "wx-product-1");
                body.put("feedId", "feed-1");
                body.put("nonceId", "nonce-1");
                service.save(body);
                saved.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return null;
            }));
            assertTrue(saved.await(5, TimeUnit.SECONDS));
            Future<Integer> deletionCheck = workers.submit(() -> transaction.execute(status -> {
                checking.countDown();
                jdbc.queryForObject("SELECT product_id FROM biz_product WHERE product_id=1 FOR UPDATE", Long.class);
                return jdbc.queryForObject("SELECT COUNT(*) FROM biz_video_feed_mount WHERE product_id=1", Integer.class);
            }));
            assertTrue(checking.await(5, TimeUnit.SECONDS));
            try { deletionCheck.get(150, TimeUnit.MILLISECONDS); fail("Delete should wait for the product row lock"); }
            catch (TimeoutException expected) { }
            release.countDown();
            create.get(5, TimeUnit.SECONDS);
            assertEquals(Integer.valueOf(1), deletionCheck.get(5, TimeUnit.SECONDS));
        }
        finally
        {
            release.countDown();
            workers.shutdownNow();
        }
    }
}
