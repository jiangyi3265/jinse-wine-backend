package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BusinessReportQueryTest
{
    private BusinessExtensionController controller;
    private JdbcTemplate jdbc;

    @Before public void setup()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:reports_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,member_no VARCHAR(32),nickname VARCHAR(128))");
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,order_no VARCHAR(64),member_id BIGINT,discount_cent INT,shipping_cent INT,total_cent INT,status VARCHAR(24),create_time TIMESTAMP,fulfillment_mode VARCHAR(16),paid_at TIMESTAMP,wechat_transaction_id VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_order_item(order_item_id BIGINT PRIMARY KEY,order_id BIGINT,product_name VARCHAR(128),product_spec VARCHAR(128),quantity INT,total_cent INT)");
        jdbc.execute("CREATE TABLE biz_order_refund(order_id BIGINT,amount_cent INT,status VARCHAR(24))");
        jdbc.execute("CREATE TABLE biz_report_export(report_type VARCHAR(32),filters_json CLOB,file_name VARCHAR(256),row_count INT,create_by VARCHAR(64))");
        jdbc.update("INSERT INTO biz_member VALUES(1,'HY11111111111111111111','会员%_甲'),(2,'HY22222222222222222222','会员乙')");
        jdbc.update("INSERT INTO biz_order VALUES(1,'ORD%_1',1,20,20,400,'shipping','2026-10-08 23:59:59','delivery','2026-10-08 23:59:59','WX1'),(2,'ORD-2',2,0,10,1010,'receiving','2026-10-09 00:00:00','delivery','2026-10-09 00:00:00','WX2')");
        jdbc.update("INSERT INTO biz_order_item VALUES(11,1,'酒%_甲','单瓶',1,100),(12,1,'另一商品','双瓶',1,300),(21,2,'=SUM(1)','礼盒',2,1000)");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,100,'refunded')");
        controller = new BusinessExtensionController(jdbc, new ObjectMapper(), null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null) {
            @Override public String getUsername() { return "report-test"; }
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> query(Map<String, String> params) throws Exception
    {
        return (Map<String, Object>) controller.reportRows("orders", params, new Response().response).get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> rows(Map<String, Object> result)
    {
        return (List<Map<String, String>>) result.get("rows");
    }

    private Map<String, String> params(String... pairs)
    {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i], pairs[i + 1]);
        return result;
    }

    @Test public void paginatesInStableOrderAndDoesNotRepeatOrderMoney() throws Exception
    {
        Map<String, Object> first = query(params("pageSize", "1", "pageNum", "2"));
        Map<String, Object> next = query(params("pageSize", "1", "pageNum", "3"));
        assertEquals(3L, first.get("total"));
        assertEquals("4.00", rows(first).get(0).get("order_payable_yuan"));
        assertEquals("0.20", rows(first).get(0).get("shipping_yuan"));
        assertEquals("3.00", rows(first).get(0).get("net_paid_yuan"));
        assertEquals("0.00", rows(next).get(0).get("order_payable_yuan"));
        assertEquals("3.00", rows(next).get(0).get("product_amount_yuan"));
        assertEquals("待收货", rows(first).get(0).get("status"));
    }

    @Test public void combinesMemberKeywordAndInclusiveDateFiltersWithLiteralWildcards() throws Exception
    {
        Map<String, Object> result = query(params("memberNo", "HY11111111111111111111", "memberName", "%_甲", "keyword", "ORD%_1", "fromDate", "2026-10-08", "toDate", "2026-10-08"));
        assertEquals(2L, result.get("total"));
        assertEquals(0L, query(params("memberName", "%_乙")).get("total"));
        assertEquals(0L, query(params("keyword", "' OR 1=1 --")).get("total"));
        assertEquals(3L, query(params("status", "shipping")).get("total"));
        assertEquals(1L, query(params("fromDate", "2026-10-09")).get("total"));
    }

    @Test public void emptyQueriesKeepHeadersAndHideInternalSortAndMemberIds() throws Exception
    {
        Map<String, Object> result = query(params("keyword", "missing"));
        assertEquals(0L, result.get("total"));
        assertTrue(rows(result).isEmpty());
        @SuppressWarnings("unchecked") List<Map<String, Object>> columns = (List<Map<String, Object>>) result.get("columns");
        assertEquals(17, columns.size());
        assertTrue(columns.stream().anyMatch(column -> "会员编号".equals(column.get("label"))));
        assertFalse(columns.stream().anyMatch(column -> String.valueOf(column.get("key")).startsWith("sort_") || "member_id".equals(column.get("key"))));
    }

    @Test public void exportSharesFiltersIgnoresPaginationAndAuditsMatchedRows() throws Exception
    {
        Map<String, String> filters = params("memberNo", "HY11111111111111111111", "pageSize", "1", "pageNum", "2");
        Response response = new Response();
        controller.export("orders", filters, response.response);
        String csv = response.body.toString(StandardCharsets.UTF_8.name());
        assertEquals(3, csv.split("\n").length);
        assertTrue(csv.contains("会员编号,用户名"));
        assertTrue(csv.contains("\"4.00\""));
        assertFalse(csv.contains("ORD-2"));
        assertEquals(Integer.valueOf(2), jdbc.queryForObject("SELECT row_count FROM biz_report_export", Integer.class));
        Response formula = new Response();
        controller.export("orders", params("keyword", "ORD-2"), formula.response);
        assertTrue(formula.body.toString(StandardCharsets.UTF_8.name()).contains("\"'=SUM(1)\""));
    }

    @Test public void rejectsInvalidStatusesDatesAndUnboundedPagination() throws Exception
    {
        for (Map<String, String> invalid : List.of(params("pageSize", "101"), params("pageNum", "0"), params("status", "' OR 1=1 --")))
        {
            try { query(invalid); fail("Invalid filters accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        for (Map<String, String> invalid : List.of(params("fromDate", "2026-02-30"), params("fromDate", "2026-10-09", "toDate", "2026-10-08")))
        {
            Response response = new Response();
            assertNull(controller.reportRows("orders", invalid, response.response));
            assertEquals(400, response.status);
        }
    }

    @Test public void returnReportIncludesBothReceiptSourcesAndExchangeWithoutRefund() throws Exception
    {
        seedReturnSources();
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) controller.reportRows("returns", params(), new Response().response).get("data");
        assertEquals(3L, result.get("total"));
        assertEquals(3, rows(result).size());
        Map<String, String> exchange = rows(result).stream().filter(row -> "AS1".equals(row.get("receipt_id"))).findFirst().orElseThrow();
        assertEquals("0.00", exchange.get("refund_amount_cent"));
        assertEquals("换货", exchange.get("aftersale_type"));
        assertEquals("退换货售后", exchange.get("return_source"));
        assertEquals("RECEIPT-EX", exchange.get("receipt_reference"));
        assertEquals("HY22222222222222222222", exchange.get("member_no"));
        assertEquals("仓库员", exchange.get("received_by"));
        assertEquals("1", exchange.get("restock_quantity"));
        assertEquals("1", exchange.get("nonrestock_quantity"));
        Map<String, String> legacy = rows(result).stream().filter(row -> "1".equals(row.get("receipt_id"))).findFirst().orElseThrow();
        assertEquals("退款后收货登记", legacy.get("return_source"));
        assertEquals("1.00", legacy.get("refund_amount_cent"));
        assertFalse(rows(result).stream().anyMatch(row -> row.keySet().stream().anyMatch(key -> key.startsWith("sort_"))));
    }

    @Test public void returnReportSharesDateMemberAndAftersaleFiltersAcrossListAndExport() throws Exception
    {
        seedReturnSources();
        Map<String, String> filters = params("keyword", "AS-EX%_1", "memberNo", "HY22222222222222222222", "fromDate", "2026-10-09", "toDate", "2026-10-09", "pageSize", "1");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) controller.reportRows("returns", filters, new Response().response).get("data");
        assertEquals(1L, result.get("total"));
        assertEquals("AS1", rows(result).get(0).get("receipt_id"));
        Response response = new Response();
        controller.export("returns", filters, response.response);
        String csv = response.body.toString(StandardCharsets.UTF_8.name());
        assertEquals(2, csv.split("\n").length);
        assertTrue(csv.contains("入库来源,售后单号,售后类型"));
        assertTrue(csv.contains("\"退换货售后\",\"AS-EX%_1\",\"换货\""));
        assertTrue(csv.contains("\"0.00\""));
        assertFalse(csv.contains("RECEIPT-LEGACY"));
    }

    private void seedReturnSources()
    {
        jdbc.execute("ALTER TABLE biz_order_item ADD sku_code VARCHAR(64)");
        jdbc.execute("ALTER TABLE biz_order_refund ADD refund_id BIGINT");
        jdbc.execute("ALTER TABLE biz_order_refund ADD refund_no VARCHAR(64)");
        jdbc.update("UPDATE biz_order_refund SET refund_id=1,refund_no='RF-1'");
        jdbc.execute("CREATE TABLE sys_user(user_id BIGINT,user_name VARCHAR(64),nick_name VARCHAR(64),phonenumber VARCHAR(32),del_flag CHAR(1))");
        jdbc.update("INSERT INTO sys_user VALUES(1,'warehouse','仓库员','13800000000','0')");
        jdbc.execute("CREATE TABLE biz_order_return_receipt(receipt_id BIGINT,refund_id BIGINT,order_id BIGINT,order_item_id BIGINT,quantity INT,restock_quantity INT,receipt_reference VARCHAR(128),request_id VARCHAR(64),received_by VARCHAR(64),received_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_order_aftersale(aftersale_id BIGINT,aftersale_no VARCHAR(64),refund_id BIGINT,order_id BIGINT,order_item_id BIGINT,quantity INT,restock_quantity INT,receipt_reference VARCHAR(128),request_id VARCHAR(64),received_by VARCHAR(64),received_at TIMESTAMP,aftersale_type VARCHAR(24),amount_cent INT)");
        jdbc.update("INSERT INTO biz_order_return_receipt VALUES(1,1,1,11,1,0,'RECEIPT-LEGACY','legacy-1','warehouse','2026-10-08 23:59:59')");
        jdbc.update("INSERT INTO biz_order_aftersale VALUES(1,'AS-EX%_1',NULL,2,21,2,1,'RECEIPT-EX','exchange-1','warehouse','2026-10-09 00:00:00','exchange',0),(2,'AS-NOT-RECEIVED',NULL,1,11,1,0,'','pending-1',NULL,NULL,'exchange',0),(3,'AS-RETURN',1,1,12,1,1,'RECEIPT-REFUND','return-1','warehouse','2026-10-09 00:00:00','return_refund',100)");
    }

    private static final class Response
    {
        int status = 200;
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final ServletOutputStream output = new ServletOutputStream() {
            @Override public void write(int value) { body.write(value); }
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener listener) { }
        };
        final HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(), new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
            if ("sendError".equals(method.getName())) { status = (Integer) args[0]; return null; }
            if ("isCommitted".equals(method.getName())) return status >= 400;
            if ("getOutputStream".equals(method.getName())) return output;
            return null;
        });
    }
}
