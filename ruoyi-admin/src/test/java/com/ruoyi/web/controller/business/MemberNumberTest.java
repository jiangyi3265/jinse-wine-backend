package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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

public class MemberNumberTest
{
    private JdbcTemplate jdbc;
    private MiniMemberAuthService auth;
    private TransactionTemplate tx;
    private Method ensureNumber;

    @Before public void setup() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:member_number_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,member_no VARCHAR(32) UNIQUE,update_time TIMESTAMP)");
        jdbc.update("INSERT INTO biz_member VALUES(1,NULL,'2026-01-01 00:00:00'),(2,'','2026-01-01 00:00:00')");
        auth = new MiniMemberAuthService(jdbc, null, null);
        ensureNumber = MiniMemberAuthService.class.getDeclaredMethod("ensureMemberNumber", long.class);
        ensureNumber.setAccessible(true);
    }

    private String number(long id)
    {
        return tx.execute(status -> {
            try { return (String) ensureNumber.invoke(auth, id); }
            catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        });
    }

    @Test public void generatesIndependentStableNumbersWithoutChangingProfileTimestamp()
    {
        String first = number(1), second = number(2);
        assertTrue(first.matches("HY[0-9A-F]{20}"));
        assertTrue(second.matches("HY[0-9A-F]{20}"));
        assertNotEquals(first, second);
        assertEquals(first, number(1));
        assertEquals(second, number(2));
        assertEquals(Timestamp.valueOf("2026-01-01 00:00:00"), jdbc.queryForObject("SELECT update_time FROM biz_member WHERE member_id=1", Timestamp.class));
    }

    @Test public void concurrentFirstLoginsReturnTheSamePersistedNumber() throws Exception
    {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try
        {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) results.add(pool.submit(() -> { start.await(); return number(1); }));
            start.countDown();
            String persisted = results.get(0).get(10, TimeUnit.SECONDS);
            for (Future<String> result : results) assertEquals(persisted, result.get(10, TimeUnit.SECONDS));
            assertEquals(persisted, jdbc.queryForObject("SELECT member_no FROM biz_member WHERE member_id=1", String.class));
        }
        finally { pool.shutdownNow(); }
    }
}
