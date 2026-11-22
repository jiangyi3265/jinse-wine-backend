package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.domain.AjaxResult;

public class OrderShippingStoreTest
{
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private BusinessAdminController admin;
    private boolean restricted;

    @Before public void setup() throws Exception
    {
        JdbcDataSource db = new JdbcDataSource();
        db.setURL("jdbc:h2:mem:shipping_store_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(db);
        tx = new TransactionTemplate(new DataSourceTransactionManager(db));
        jdbc.execute("CREATE TABLE sys_config(config_key VARCHAR,config_value VARCHAR)");
        jdbc.execute("CREATE TABLE biz_store(store_id BIGINT PRIMARY KEY,store_code VARCHAR,store_name VARCHAR,status VARCHAR)");
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,order_no VARCHAR,status VARCHAR,fulfillment_mode VARCHAR,shipping_store_id BIGINT,shipping_store_name VARCHAR,update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_order_store_assignment(order_id BIGINT,previous_store_id BIGINT,previous_store_name VARCHAR,store_id BIGINT,store_name VARCHAR,operator_name VARCHAR)");
        jdbc.execute("CREATE TABLE biz_order_refund(order_id BIGINT,status VARCHAR)");
        jdbc.execute("CREATE TABLE biz_order_aftersale(order_id BIGINT,status VARCHAR)");
        jdbc.execute("CREATE TABLE biz_order_delivery(order_id BIGINT PRIMARY KEY,company VARCHAR,company_code VARCHAR,tracking_no VARCHAR,status VARCHAR,last_trace VARCHAR,shipped_at TIMESTAMP,received_at TIMESTAMP,last_synced_at TIMESTAMP,next_query_at TIMESTAMP,sync_error VARCHAR,waybill_token VARCHAR,signed_at TIMESTAMP)");
        jdbc.update("INSERT INTO biz_store VALUES(1,'G','国大门市','0'),(2,'S','门店二','0'),(3,'D','停用门店','1')");
        jdbc.update("INSERT INTO biz_order(order_id,order_no,status,fulfillment_mode) VALUES(1,'TEST','paid','delivery'),(2,'PICK','paid','pickup')");
        BusinessDataScopeService scope = new BusinessDataScopeService(jdbc) {
            @Override public void append(String resource, StringBuilder where, List<Object> args)
            {
                if (restricted && "store".equals(resource)) where.append(" AND store_id=1");
            }
        };
        admin = new BusinessAdminController(jdbc, scope) {
            @Override public String getUsername() { return "operator"; }
        };
        Field field = BusinessAdminController.class.getDeclaredField("logistics");
        field.setAccessible(true);
        field.set(admin, new OrderLogisticsService(jdbc, null, tx, null) {
            @Override public Map<String,Object> delivery(long id, boolean refresh) { return new HashMap<>(); }
        });
    }

    @Test public void newDeliveryGetsDefaultAndPickupHasNoShippingStore()
    {
        OrderShippingStore.assignNewOrder(jdbc, 1, "delivery");
        OrderShippingStore.assignNewOrder(jdbc, 2, "pickup");
        assertEquals(Long.valueOf(1), assigned(1));
        assertNull(assigned(2));
        assertEquals(1, count("biz_order_store_assignment"));
    }

    @Test public void configuredCodeSurvivesRenameAndAmbiguousOrDisabledStoresAreNeverChosen()
    {
        jdbc.update("UPDATE biz_store SET store_name='国大门市' WHERE store_id=2");
        assertNull(OrderShippingStore.defaultStore(jdbc));
        jdbc.update("INSERT INTO sys_config VALUES('wine.shipping.defaultStoreCode','S')");
        jdbc.update("UPDATE biz_store SET store_name='新名称' WHERE store_id=2");
        assertEquals("新名称", OrderShippingStore.defaultStore(jdbc).get("store_name"));
        jdbc.update("UPDATE biz_store SET status='1' WHERE store_id=2");
        OrderShippingStore.assignNewOrder(jdbc, 1, "delivery");
        assertNull(assigned(1));
    }

    @Test public void oldUnassignedOrderShipsWithDefaultInOneRequest()
    {
        assertTrue(ship(1, null, null).isSuccess());
        assertEquals(Long.valueOf(1), assigned(1));
        assertEquals(1, count("biz_order_delivery"));
    }

    @Test public void overrideAndShipIsAtomicAndHonorsScopeAndExpectedStore()
    {
        OrderShippingStore.assignNewOrder(jdbc, 1, "delivery");
        assertEquals(409, ship(1, null, 2).get("code"));
        restricted = true;
        assertEquals(403, ship(1, 1, 2).get("code"));
        restricted = false;
        assertFalse(ship(1, 1, 3).isSuccess());
        assertFalse(ship(2, null, 2).isSuccess());
        assertEquals(Long.valueOf(1), assigned(1));
        assertEquals(0, count("biz_order_delivery"));
        assertTrue(ship(1, 1, 2).isSuccess());
        assertEquals(Long.valueOf(2), assigned(1));
        assertEquals(2, count("biz_order_store_assignment"));
        assertFalse(ship(1, 2, 1).isSuccess());
    }

    @Test public void staleShippingFormCannotReplaceAWaybill()
    {
        assertTrue(ship(1, null, null).isSuccess());
        Map<String,Object> body = new HashMap<>();
        body.put("expectedShippingStoreId", 1);
        body.put("company", "顺丰");
        body.put("trackingNo", "TEST-456");
        body.put("expectedOrderStatus", "paid");
        assertEquals(409, admin.updateOrderDelivery("1", body).get("code"));
        body.put("expectedOrderStatus", "receiving");
        body.put("expectedTrackingNo", "");
        assertEquals(409, admin.updateOrderDelivery("1", body).get("code"));
        assertEquals("TEST-123", jdbc.queryForObject("SELECT tracking_no FROM biz_order_delivery WHERE order_id=1", String.class));
    }

    private AjaxResult ship(int id, Integer expected, Integer selected)
    {
        Map<String,Object> body = new HashMap<>();
        body.put("expectedShippingStoreId", expected);
        body.put("shippingStoreId", selected);
        body.put("company","顺丰");
        body.put("trackingNo","TEST-123");
        return tx.execute(s -> admin.updateOrderDelivery(String.valueOf(id),body));
    }

    private Long assigned(int id)
    {
        return jdbc.queryForObject("SELECT shipping_store_id FROM biz_order WHERE order_id=?",Long.class,id);
    }

    private int count(String table)
    {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table,Integer.class);
    }
}
