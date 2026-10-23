package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.model.LoginUser;

public class BusinessDashboardRevenueTest
{
    private JdbcTemplate jdbc;
    private BusinessExtensionController controller;

    @Before public void setup()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:dashboard_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY,order_no VARCHAR(64),member_id BIGINT,discount_cent INT DEFAULT 0,shipping_cent INT DEFAULT 0,total_cent INT,status VARCHAR(24),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,fulfillment_mode VARCHAR(16) DEFAULT 'delivery',paid_at TIMESTAMP,wechat_transaction_id VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_order_refund(order_id BIGINT,amount_cent INT,status VARCHAR(24))");
        jdbc.execute("CREATE TABLE biz_promotion_record(amount_cent INT,status VARCHAR(24),create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,member_no VARCHAR(32),nickname VARCHAR(128))");
        jdbc.execute("CREATE TABLE biz_order_item(order_item_id BIGINT PRIMARY KEY,order_id BIGINT,product_name VARCHAR(128),product_spec VARCHAR(128),quantity INT,total_cent INT)");
        jdbc.update("INSERT INTO biz_member VALUES(1,'HY-DASHBOARD-TEST','dashboard-test')");
        controller = new BusinessExtensionController(jdbc, new ObjectMapper(), null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        permissions(Set.of("business:dashboard:query", "business:order:list", "business:settlement:list"));
    }

    @After public void cleanup()
    {
        SecurityContextHolder.clearContext();
    }

    private void permissions(Set<String> permissions)
    {
        LoginUser user = new LoginUser();
        user.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dashboard()
    {
        return (Map<String, Object>) controller.dashboard().get("data");
    }

    @SuppressWarnings("unchecked")
    private long todaySales(Map<String, Object> dashboard)
    {
        return ((List<Map<String, Object>>) dashboard.get("trend")).stream()
                .filter(row -> LocalDate.now().toString().equals(row.get("day")))
                .mapToLong(row -> ((Number) row.get("salesCent")).longValue()).findFirst().orElseThrow();
    }

    @Test public void retainedFreightMatchesFinancialReportAndDailyTrend() throws Exception
    {
        jdbc.update("INSERT INTO biz_order(order_id,order_no,member_id,total_cent,shipping_cent,status,paid_at) VALUES(1,'REFUND-FREIGHT',1,1200,200,'refunded',CURRENT_TIMESTAMP),(2,'PAID',1,500,0,'paid',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,1000,'refunded')");
        jdbc.update("INSERT INTO biz_order_item VALUES(11,1,'returned-product','single',1,1000),(21,2,'paid-product','single',1,500)");
        Map<String, Object> result = dashboard();
        assertEquals(700L, ((Number) result.get("salesCent")).longValue());
        assertEquals(700L, todaySales(result));
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
        @SuppressWarnings("unchecked") Map<String, Object> report = (Map<String, Object>) controller.reportRows("orders", Map.of(), response).get("data");
        @SuppressWarnings("unchecked") List<Map<String, Object>> rows = (List<Map<String, Object>>) report.get("rows");
        BigDecimal net = rows.stream().map(row -> new BigDecimal(String.valueOf(row.get("net_paid_yuan"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(700L, net.movePointRight(2).longValueExact());
    }

    @Test public void onlyConfirmedRefundsReduceSalesAndFullRefundsContributeZero()
    {
        jdbc.update("INSERT INTO biz_order(order_id,order_no,member_id,total_cent,status) VALUES(1,'PARTIAL',1,1000,'receiving'),(2,'FULL',1,500,'refunded'),(3,'CANCELLED',1,999,'cancelled'),(4,'UNPAID',1,999,'pending')");
        jdbc.update("INSERT INTO biz_order_refund VALUES(1,100,'refunded'),(1,200,'refunded'),(1,250,'pending'),(1,100,'rejected'),(2,500,'refunded')");
        Map<String, Object> result = dashboard();
        assertEquals(700L, ((Number) result.get("salesCent")).longValue());
        assertEquals(700L, todaySales(result));
    }

    @Test public void operatorCannotReadFinancialTotalsOrTrend()
    {
        permissions(Set.of("business:dashboard:query", "business:order:list"));
        Map<String, Object> result = dashboard();
        assertFalse(result.containsKey("salesCent"));
        assertFalse(result.containsKey("commissionCent"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> trend = (List<Map<String, Object>>) result.get("trend");
        assertTrue(trend.stream().noneMatch(row -> row.containsKey("salesCent") || row.containsKey("commissionCent")));
    }
}
