package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class OrderPromotionSnapshotTest
{
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private final ObjectMapper json = new ObjectMapper();

    @Before public void setup()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:promotion_snapshot_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,member_id BIGINT,total_cent INT,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_order_promotion_snapshot(order_id BIGINT PRIMARY KEY,snapshot_version INT,config_id BIGINT,recipients_json VARCHAR(8192))");
        jdbc.execute("CREATE TABLE biz_promotion_config(config_id BIGINT PRIMARY KEY,max_level INT,customer_rate_bp INT,staff_rate_bp INT,level_rates_json VARCHAR(512),status CHAR(1),refund_policy VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_promotion_relation(member_id BIGINT PRIMARY KEY,inviter_member_id BIGINT,status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,staff_store_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_promotion_record(promotion_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,member_id BIGINT,order_id BIGINT,title VARCHAR(128),amount_cent BIGINT,original_amount_cent BIGINT,status VARCHAR(16),refund_adjustment_cent BIGINT DEFAULT 0,refund_hold_status VARCHAR(16),refund_recovery_required INT DEFAULT 0)");
        jdbc.update("INSERT INTO biz_member VALUES(1,NULL),(2,NULL),(3,8),(4,NULL)");
        jdbc.update("INSERT INTO biz_promotion_config VALUES(1,1,500,800,'[500]','0','reverse')");
        jdbc.update("INSERT INTO biz_order VALUES(10,1,21000,'complete'),(11,1,21000,'complete')");
    }

    @Test public void recipientAndRateStayFrozenAcrossAdminChanges() throws Exception
    {
        bind(2); capture(10);
        jdbc.update("UPDATE biz_promotion_relation SET inviter_member_id=3 WHERE member_id=1");
        jdbc.update("UPDATE biz_promotion_config SET customer_rate_bp=1500,staff_rate_bp=2000");
        complete(10, false);
        assertEquals(1050, amount(10,2)); assertEquals(0, amount(10,3));
        capture(11); complete(11, true);
        assertEquals(4200, amount(11,3));
    }

    @Test public void laterBindingCannotClaimAnUninvitedNewOrder() throws Exception
    {
        capture(10); bind(2); complete(10,false); complete(10,true);
        assertEquals(0, count("SELECT COUNT(*) FROM biz_promotion_record"));
        capture(11); complete(11,false); assertEquals(1050, amount(11,2));
    }

    @Test public void disabledAtCreationRemainsEmptyAfterRulesAreEnabled() throws Exception
    {
        bind(2); jdbc.update("UPDATE biz_promotion_config SET status='1'"); capture(10);
        jdbc.update("UPDATE biz_promotion_config SET status='0'"); complete(10,true);
        assertEquals(0, count("SELECT COUNT(*) FROM biz_promotion_record"));
    }

    @Test public void laterDisablingRulesDoesNotCancelAnExistingSnapshot() throws Exception
    {
        bind(2); capture(10); jdbc.update("UPDATE biz_promotion_config SET status='1'");
        complete(10,true); assertEquals(1050, amount(10,2));
    }

    @Test public void staffRoleAndEachLevelAreCaptured() throws Exception
    {
        bind(3); jdbc.update("INSERT INTO biz_promotion_relation VALUES(3,2,'0')");
        jdbc.update("UPDATE biz_promotion_config SET max_level=2,level_rates_json=?",
                "{\"staff\":[800,300],\"customer\":[500,200]}");
        capture(10);
        jdbc.update("UPDATE biz_member SET staff_store_id=NULL WHERE member_id=3");
        jdbc.update("UPDATE biz_promotion_config SET max_level=1,level_rates_json='[100]' ");
        complete(10,false);
        assertEquals(1680, amount(10,3)); assertEquals(420, amount(10,2));
    }

    @Test public void zeroRateCannotBecomePositiveAfterCreation() throws Exception
    {
        bind(2); jdbc.update("UPDATE biz_promotion_config SET customer_rate_bp=0"); capture(10);
        jdbc.update("UPDATE biz_promotion_config SET customer_rate_bp=1000"); complete(10,true);
        assertEquals(0, count("SELECT COUNT(*) FROM biz_promotion_record"));
    }

    @Test public void legacyOrdersUseCompletionTimeRulesInBothPaths() throws Exception
    {
        bind(2); jdbc.update("UPDATE biz_promotion_config SET customer_rate_bp=900");
        complete(10,false); complete(11,true);
        assertEquals(1890, amount(10,2)); assertEquals(1890, amount(11,2));
        assertEquals(0,count("SELECT COUNT(*) FROM biz_order_promotion_snapshot"));
    }

    @Test public void repeatedCompletionDoesNotDuplicateOrRestoreRefundedCommission() throws Exception
    {
        bind(2); capture(10); complete(10,false); complete(10,true);
        assertEquals(1,count("SELECT COUNT(*) FROM biz_promotion_record"));
        OrderRefundService refunds = new OrderRefundService(jdbc,new PointExpiryService(jdbc));
        Method refund = OrderRefundService.class.getDeclaredMethod("applyCommissionRefund",long.class,long.class,long.class);
        refund.setAccessible(true);
        tx.execute(status -> { try { refund.invoke(refunds,10L,4200L,21000L); } catch(Exception e) { throw new RuntimeException(e); } return null; });
        complete(10,false); complete(10,true);
        assertEquals(840,amount(10,2));
        assertEquals(210,count("SELECT refund_adjustment_cent FROM biz_promotion_record"));
    }

    @Test public void failedCreationRollsBackItsSnapshot()
    {
        bind(2);
        tx.execute(status -> { OrderPromotionSnapshot.capture(jdbc,json,10,1); status.setRollbackOnly(); return null; });
        assertEquals(0,count("SELECT COUNT(*) FROM biz_order_promotion_snapshot"));
    }

    @Test public void corruptSnapshotFailsInsteadOfFallingBackToCurrentRules()
    {
        bind(2); capture(10);
        jdbc.update("UPDATE biz_order_promotion_snapshot SET recipients_json='null' WHERE order_id=10");
        try { tx.execute(status -> OrderPromotionSnapshot.settleIfCaptured(jdbc,json,10)); fail("Must not fallback"); }
        catch(IllegalStateException expected) { assertTrue(expected.getMessage().contains("快照")); }
        assertEquals(0,count("SELECT COUNT(*) FROM biz_promotion_record"));
    }

    private void bind(long inviter) { jdbc.update("INSERT INTO biz_promotion_relation VALUES(1,?,'0')",inviter); }
    private void capture(long id) { tx.execute(status -> { OrderPromotionSnapshot.capture(jdbc,json,id,1); return null; }); }
    private long count(String sql) { return jdbc.queryForObject(sql,Long.class); }
    private long amount(long id,long member) { return jdbc.queryForObject("SELECT COALESCE(SUM(amount_cent),0) FROM biz_promotion_record WHERE order_id=? AND member_id=?",Long.class,id,member); }
    private void complete(long id,boolean admin) throws Exception
    {
        Object target;
        if(admin)
        {
            target = new BusinessAdminController(jdbc,new BusinessDataScopeService(jdbc));
            Field mapper = BusinessAdminController.class.getDeclaredField("objectMapper"); mapper.setAccessible(true); mapper.set(target,json);
        }
        else target = new MiniCommerceService(jdbc,json,new PointExpiryService(jdbc),null,null);
        Method method = target.getClass().getDeclaredMethod("settlePromotion",long.class); method.setAccessible(true);
        tx.execute(status -> { try { method.invoke(target,id); } catch(Exception e) { throw new RuntimeException(e); } return null; });
    }
}
