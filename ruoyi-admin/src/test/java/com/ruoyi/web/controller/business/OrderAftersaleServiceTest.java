package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class OrderAftersaleServiceTest
{
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private OrderAftersaleService service;

    @Before
    public void setup() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:aftersale_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source); tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        Path schema = Path.of("../sql/business-aftersale-upgrade.sql");
        if (!Files.exists(schema)) schema = Path.of("sql/business-aftersale-upgrade.sql");
        for (String statement : Files.readString(schema).split("SET @aftersale_receipt_sql")[0].replaceAll("ENGINE=InnoDB COMMENT='[^']*'", "").split(";"))
            if (!statement.isBlank()) jdbc.execute(statement);
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,status CHAR(1),member_no VARCHAR(32),nickname VARCHAR(128))");
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,order_no VARCHAR(64),member_id BIGINT,status VARCHAR(24),total_cent BIGINT,subtotal_cent BIGINT,discount_cent BIGINT,shipping_cent BIGINT,fulfillment_mode VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_order_item(order_item_id BIGINT PRIMARY KEY,order_id BIGINT,product_id BIGINT,sku_code VARCHAR(64),product_name VARCHAR(128),product_spec VARCHAR(128),quantity INT,unit_price_cent BIGINT,total_cent BIGINT)");
        jdbc.execute("CREATE TABLE biz_order_refund(refund_id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id BIGINT,member_id BIGINT,refund_no VARCHAR(64),request_id VARCHAR(64),amount_cent BIGINT,reason VARCHAR(255),status VARCHAR(24))");
        jdbc.execute("CREATE TABLE biz_order_return_receipt(order_item_id BIGINT,quantity INT)");
        jdbc.execute("CREATE TABLE biz_product(product_id BIGINT PRIMARY KEY,stock INT,status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_promotion_record(order_id BIGINT,status VARCHAR(24),refund_hold_status VARCHAR(24),amount_cent BIGINT)");
        jdbc.update("INSERT INTO biz_member VALUES(1,'0','HY11111111111111111111','会员%_甲'),(2,'0','HY22222222222222222222','会员乙')");
        jdbc.update("INSERT INTO biz_order VALUES(10,'ORDER-10',1,'receiving',103,4,1,100,'delivery')");
        jdbc.update("INSERT INTO biz_order_item VALUES(11,10,21,'','测试酒A','单瓶',2,1,2),(12,10,22,'','测试酒B','单瓶',2,1,2)");
        jdbc.update("INSERT INTO biz_product VALUES(21,10,'0'),(22,10,'0')");
        jdbc.update("INSERT INTO biz_promotion_record VALUES(10,'pending',NULL,100)");
        BusinessDataScopeService scope = new BusinessDataScopeService(jdbc) {
            @Override public void append(String resource, StringBuilder where, List<Object> args) { }
        };
        service = new OrderAftersaleService(jdbc, new OrderRefundService(jdbc, new PointExpiryService(jdbc)), scope);
    }

    private Map<String, Object> apply(long item, int qty, String type, String key)
    { return tx.execute(status -> service.apply(1, "ORDER-10", Map.of("orderItemId", item, "quantity", qty, "type", type, "reason", "测试售后"), key)); }
    private long id(Map<String, Object> row) { return ((Number) row.get("id")).longValue(); }
    private void approve(long id)
    { tx.execute(status -> service.review(id, Map.of("status", "approved", "note", "同意", "returnAddress", "测试收件人 13800000000 测试地址"), "admin")); }
    private void returned(long id)
    { tx.execute(status -> service.returnShipment(1, id, Map.of("company", "顺丰", "trackingNo", "RETURN-1"))); }
    private void expectConflict(Runnable action)
    { try { action.run(); fail("Expected business rejection"); } catch (MiniBusinessException expected) { } }

    @Test public void discountedAllocationPreservesEveryCent()
    {
        long sum = 0;
        for (long offset = 0; offset < 4; offset++) sum += OrderAftersaleService.allocatedAmount(offset, 1, 3, 4);
        assertEquals(3, sum);
        assertEquals(1, OrderAftersaleService.allocatedAmount(0, 2, 3, 4));
        assertEquals(2, OrderAftersaleService.allocatedAmount(2, 2, 3, 4));
    }

    @Test public void returnRefundRequiresReviewAndReturnAndDoesNotPayOrRestockTwice()
    {
        long id = id(apply(11, 2, "return_refund", "return_request_1"));
        assertEquals("closed", jdbc.queryForObject("SELECT status FROM biz_promotion_record", String.class));
        expectConflict(() -> tx.execute(status -> service.inspect(id, Map.of("restockQuantity", 2, "receiptReference", "RECEIPT-1"), "admin")));
        approve(id); returned(id);
        expectConflict(() -> tx.execute(status -> service.inspect(id, Map.of("restockQuantity", 1), "admin")));
        Map<String, Object> result = tx.execute(status -> service.inspect(id, Map.of("restockQuantity", 1, "receiptReference", "RECEIPT-1"), "admin"));
        assertEquals("refund_pending", result.get("status"));
        assertEquals(11, jdbc.queryForObject("SELECT stock FROM biz_product WHERE product_id=21", Integer.class).intValue());
        assertEquals("approved", jdbc.queryForObject("SELECT status FROM biz_order_refund", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT amount_cent FROM biz_order_refund", Integer.class).intValue());
        tx.execute(status -> service.inspect(id, Map.of("restockQuantity", 1, "receiptReference", "RECEIPT-1"), "admin"));
        expectConflict(() -> tx.execute(status -> service.inspect(id, Map.of("restockQuantity", 1, "receiptReference", "CHANGED"), "admin")));
        assertEquals("RECEIPT-1", jdbc.queryForObject("SELECT receipt_reference FROM biz_order_aftersale", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM biz_order_refund", Integer.class).intValue());
        jdbc.update("UPDATE biz_order_refund SET status='refunded'");
        tx.execute(status -> { service.reconcileRefunds(); return null; });
        assertEquals("completed", service.memberDetail(1, id).get("status"));
        assertEquals("pending", jdbc.queryForObject("SELECT status FROM biz_promotion_record", String.class));
    }

    @Test public void exchangeConsumesStockOnceAndRequiresReceipt()
    {
        long id = id(apply(11, 2, "exchange", "exchange_request_1"));
        approve(id); returned(id);
        tx.execute(status -> service.inspect(id, Map.of("restockQuantity", 1, "receiptReference", "RECEIPT-EX"), "admin"));
        Map<String, Object> shipment = Map.of("company", "中通", "trackingNo", "REPLACEMENT-1");
        tx.execute(status -> service.replacement(id, shipment, "admin"));
        tx.execute(status -> service.replacement(id, shipment, "admin"));
        assertEquals(9, jdbc.queryForObject("SELECT stock FROM biz_product WHERE product_id=21", Integer.class).intValue());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM biz_order_refund", Integer.class).intValue());
        assertEquals("closed", jdbc.queryForObject("SELECT status FROM biz_promotion_record", String.class));
        tx.execute(status -> service.memberAction(1, id, true));
        tx.execute(status -> service.memberAction(1, id, true));
        assertEquals("completed", service.memberDetail(1, id).get("status"));
        assertEquals("pending", jdbc.queryForObject("SELECT status FROM biz_promotion_record", String.class));
    }

    @Test public void retriesOwnershipAndQuantityAreEnforced()
    {
        long id = id(apply(11, 2, "exchange", "same_request_1"));
        assertEquals(id, id(apply(11, 2, "exchange", "same_request_1")));
        expectConflict(() -> apply(12, 2, "exchange", "same_request_1"));
        expectConflict(() -> apply(11, 1, "exchange", "other_request_1"));
        expectConflict(() -> service.memberDetail(2, id));
        expectConflict(() -> tx.execute(status -> service.memberAction(2, id, false)));
        tx.execute(status -> service.memberAction(1, id, false));
        assertEquals("pending", jdbc.queryForObject("SELECT status FROM biz_promotion_record", String.class));
        assertEquals("pending", apply(11, 2, "exchange", "after_cancel_1").get("status"));
    }

    @Test public void paidOrRefundingOrdersCannotApply()
    {
        jdbc.update("UPDATE biz_order SET status='paid'");
        expectConflict(() -> apply(11, 2, "exchange", "paid_request_1"));
        jdbc.update("UPDATE biz_order SET status='receiving'");
        jdbc.update("INSERT INTO biz_order_refund(order_id,status,amount_cent) VALUES(10,'approved',1)");
        expectConflict(() -> apply(11, 2, "exchange", "refund_request_1"));
    }

    @Test public void logisticsStatesNeverInferSigningFromStationOrException()
    {
        assertEquals("agent_signed", OrderLogisticsService.normalizeStatus(6));
        assertEquals("signed", OrderLogisticsService.normalizeStatus(4));
        assertEquals("awaiting_collect", OrderLogisticsService.normalizeStatus(0));
        assertEquals("exception", OrderLogisticsService.normalizeStatus(5));
        assertNull(OrderLogisticsService.normalizeStatus(99));
        assertEquals("SF(AU)", OrderLogisticsService.companyCode("SF(AU)", "测试快递"));
    }

    @Test public void automaticReceiptRequiresSevenDaysAndHasOneCompletion()
    {
        AtomicInteger benefits = new AtomicInteger();
        MiniCommerceService commerce = autoReceiptFixture(benefits);
        jdbc.update("INSERT INTO biz_order_delivery VALUES(10,'agent_signed',?)", Timestamp.valueOf(LocalDateTime.now().minusDays(8)));
        assertFalse(tx.execute(status -> commerce.autoReceiveSignedOrder(10)));
        jdbc.update("UPDATE biz_order_delivery SET status='signed',signed_at=?", Timestamp.valueOf(LocalDateTime.now().minusDays(6)));
        assertFalse(tx.execute(status -> commerce.autoReceiveSignedOrder(10)));
        jdbc.update("UPDATE biz_order_delivery SET signed_at=?", Timestamp.valueOf(LocalDateTime.now().minusDays(8)));
        assertTrue(tx.execute(status -> commerce.autoReceiveSignedOrder(10)));
        assertFalse(tx.execute(status -> commerce.autoReceiveSignedOrder(10)));
        assertEquals(1, benefits.get());
        assertEquals("complete", jdbc.queryForObject("SELECT status FROM biz_order", String.class));
    }

    @Test public void automaticReceiptPausesDuringRefundOrAftersale()
    {
        AtomicInteger benefits = new AtomicInteger();
        MiniCommerceService commerce = autoReceiptFixture(benefits);
        jdbc.update("INSERT INTO biz_order_delivery VALUES(10,'signed',?)", Timestamp.valueOf(LocalDateTime.now().minusDays(8)));
        long id = id(apply(11, 2, "exchange", "held_request_1"));
        assertFalse(tx.execute(status -> commerce.autoReceiveSignedOrder(10)));
        tx.execute(status -> service.memberAction(1, id, false));
        jdbc.update("INSERT INTO biz_order_refund(order_id,status,amount_cent) VALUES(10,'approved',1)");
        assertFalse(tx.execute(status -> commerce.autoReceiveSignedOrder(10)));
        assertEquals(0, benefits.get());
    }

    private MiniCommerceService autoReceiptFixture(AtomicInteger benefits)
    {
        jdbc.execute("ALTER TABLE biz_order ADD coupon_id BIGINT");
        jdbc.execute("ALTER TABLE biz_order ADD receive_request_id VARCHAR(64)");
        jdbc.execute("CREATE TABLE biz_order_delivery(order_id BIGINT PRIMARY KEY,status VARCHAR(24),signed_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_point_rule(points INT,daily_limit INT,rule_json VARCHAR(255),rule_code VARCHAR(32),status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_promotion_config(config_id BIGINT,max_level INT,customer_rate_bp INT,staff_rate_bp INT,level_rates_json VARCHAR(255),status CHAR(1))");
        OrderRefundService refunds = new OrderRefundService(jdbc, new PointExpiryService(jdbc)) {
            @Override public void reconcileCompletedOrderBenefits(long orderId) { benefits.incrementAndGet(); }
        };
        return new MiniCommerceService(jdbc, new com.fasterxml.jackson.databind.ObjectMapper(), new PointExpiryService(jdbc), null, refunds);
    }

    @Test public void adminPaginationCanReachRecordsBeyondTwoHundred()
    {
        seedAdminRows(205);
        Map<String, Object> page = service.adminList(Map.of("pageNum", "3", "pageSize", "100"));
        assertEquals(205L, page.get("total"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> rows = (List<Map<String, Object>>) page.get("rows");
        assertEquals(5, rows.size());
        assertEquals(5L, number(rows.get(0).get("id")));
        assertEquals("HY11111111111111111111", rows.get(0).get("memberNo"));
        assertEquals("会员%_甲", rows.get(0).get("memberName"));
    }

    @Test public void adminFiltersUseDisplayedStatusAndLiteralSearch()
    {
        seedAdminRows(2);
        jdbc.update("UPDATE biz_order_aftersale SET status='refund_pending',refund_id=1 WHERE aftersale_id=1");
        jdbc.update("INSERT INTO biz_order_refund(refund_id,order_id,status,amount_cent) VALUES(1,10,'refunded',1)");
        assertEquals(1L, service.adminList(Map.of("status", "completed")).get("total"));
        assertEquals(0L, service.adminList(Map.of("status", "refund_pending")).get("total"));
        assertEquals(2L, service.adminList(Map.of("keyword", "%_甲", "memberNo", "HY11111111111111111111", "type", "exchange", "fromDate", "2026-10-09", "toDate", "2026-10-09")).get("total"));
        assertEquals(0L, service.adminList(Map.of("keyword", "' OR 1=1 --")).get("total"));
        assertEquals(0L, service.adminList(Map.of("fromDate", "2026-10-10")).get("total"));
        assertEquals(0L, service.adminList(Map.of("memberNo", "missing")).get("total"));
    }

    @Test public void adminListRejectsInvalidFiltersAndEnforcesDataScope()
    {
        seedAdminRows(1);
        for (Map<String, String> filter : List.of(Map.of("status", "invalid"), Map.of("pageNum", "0"), Map.of("pageSize", "101"), Map.of("fromDate", "2026-02-30"), Map.of("fromDate", "2026-10-10", "toDate", "2026-10-09"), Map.of("keyword", "x".repeat(129))))
        {
            try { service.adminList(filter); fail("Invalid filters accepted"); }
            catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
        }
        OrderAftersaleService restricted = new OrderAftersaleService(jdbc, new OrderRefundService(jdbc, new PointExpiryService(jdbc)), new BusinessDataScopeService(jdbc) {
            @Override public void append(String resource, StringBuilder where, List<Object> args) { where.append(" AND 1=0"); }
        });
        Map<String, Object> result = restricted.adminList(Map.of());
        assertEquals(0L, result.get("total"));
        assertTrue(((List<?>) result.get("rows")).isEmpty());
    }

    @Test public void memberPaginationRetainsCompleteQuantitiesAndLegacyCompatibility()
    {
        seedAdminRows(205);
        jdbc.update("UPDATE biz_order_aftersale SET status='rejected' WHERE aftersale_id=205");
        jdbc.update("UPDATE biz_order_aftersale SET status='cancelled' WHERE aftersale_id=204");
        jdbc.update("INSERT INTO biz_order_return_receipt VALUES(11,2),(12,1)");
        Map<String, Object> page = service.memberPage(1, "ORDER-10", Map.of("pageNum", "3", "pageSize", "100"));
        assertEquals(205L, page.get("total"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> rows = (List<Map<String, Object>>) page.get("rows");
        assertEquals(5, rows.size());
        assertEquals(5L, number(rows.get(0).get("id")));
        assertEquals(Map.of("11", 205L, "12", 1L), page.get("reservedQuantities"));
        assertEquals(page.get("reservedQuantities"), service.memberPage(1, "ORDER-10", Map.of("pageSize", "1")).get("reservedQuantities"));
        assertEquals(205, service.memberList(1, "ORDER-10").size());
    }

    @Test public void memberPaginationValidatesOwnershipAndParameters()
    {
        seedAdminRows(1);
        for (Map<String, String> params : List.of(Map.of("pageNum", "0"), Map.of("pageNum", "1000001"), Map.of("pageSize", "101"), Map.of("pageSize", "abc")))
        {
            try { service.memberPage(1, "ORDER-10", params); fail("Invalid pagination accepted"); }
            catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
        }
        try { service.memberPage(2, "ORDER-10", Map.of()); fail("Another member's order was accessible"); }
        catch (MiniBusinessException expected) { assertEquals(404, expected.getCode()); }
        try { service.memberList(2, "ORDER-10"); fail("Legacy list leaked another member's order"); }
        catch (MiniBusinessException expected) { assertEquals(404, expected.getCode()); }
        jdbc.update("INSERT INTO biz_order VALUES(20,'ORDER-20',2,'receiving',103,4,1,100,'delivery')");
        jdbc.update("INSERT INTO biz_order_item VALUES(21,20,21,'','其他会员商品','单瓶',2,1,2)");
        Map<String, Object> empty = service.memberPage(2, "ORDER-20", Map.of());
        assertEquals(0L, empty.get("total"));
        assertTrue(((List<?>) empty.get("rows")).isEmpty());
        assertEquals(Map.of("21", 0L), empty.get("reservedQuantities"));
    }

    private void seedAdminRows(int count)
    {
        for (int i = 1; i <= count; i++)
            jdbc.update("INSERT INTO biz_order_aftersale(aftersale_no,order_id,member_id,order_item_id,request_id,aftersale_type,quantity,reason,create_time) VALUES(?,10,1,11,?,'exchange',1,'测试','2026-10-09 23:59:59')", "AS-" + i, "REQUEST-" + i);
    }
    private static long number(Object value) { return ((Number) value).longValue(); }
}
