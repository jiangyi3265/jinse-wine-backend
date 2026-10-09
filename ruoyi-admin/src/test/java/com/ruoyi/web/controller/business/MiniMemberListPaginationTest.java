package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MiniMemberListPaginationTest
{
    private JdbcTemplate jdbc;
    private MiniMarketingService marketing;
    private MiniCommerceService commerce;

    @Before
    public void schema()
    {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:member_lists_" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY, staff_store_id BIGINT, status VARCHAR(1))");
        jdbc.execute("CREATE TABLE biz_store(store_id BIGINT PRIMARY KEY, store_code VARCHAR(64), store_name VARCHAR(64), province VARCHAR(32), city VARCHAR(32), district VARCHAR(32), address VARCHAR(128), phone VARCHAR(32), latitude DECIMAL(12,8), longitude DECIMAL(12,8), status VARCHAR(1))");
        jdbc.execute("CREATE TABLE biz_point_account(member_id BIGINT PRIMARY KEY, balance INT)");
        jdbc.execute("CREATE TABLE biz_point_record(point_record_id BIGINT PRIMARY KEY, member_id BIGINT, title VARCHAR(64), change_amount INT, create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_reward(reward_id BIGINT PRIMARY KEY, member_id BIGINT, reward_type VARCHAR(16), reward_name VARCHAR(64), amount_cent INT, points INT, exchange_code VARCHAR(128), image_url VARCHAR(128), spec VARCHAR(64), status VARCHAR(16), expires_at TIMESTAMP, create_time TIMESTAMP, redeem_store_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_reward_transfer(transfer_id BIGINT, reward_id BIGINT, member_id BIGINT, request_id VARCHAR(64), out_bill_no VARCHAR(64), transfer_bill_no VARCHAR(64), amount_cent INT, openid VARCHAR(64), status VARCHAR(32), package_info VARCHAR(64), fail_reason VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_exchange_item(exchange_code VARCHAR(64), spec VARCHAR(64), image_url VARCHAR(128))");
        jdbc.execute("CREATE TABLE biz_lottery_record(record_id BIGINT PRIMARY KEY, member_id BIGINT, activity_id BIGINT, result_type VARCHAR(16), result_value VARCHAR(64), request_id VARCHAR(64), create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_lottery_activity(activity_id BIGINT PRIMARY KEY, activity_name VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_mini_idempotency(member_id BIGINT, request_id VARCHAR(64), action_type VARCHAR(16), reward_id BIGINT, member_coupon_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_redemption(redemption_id BIGINT PRIMARY KEY, reward_id BIGINT, member_id BIGINT, store_id BIGINT, verifier_member_id BIGINT, status VARCHAR(16), request_id VARCHAR(64), create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_order(order_id BIGINT PRIMARY KEY, order_no VARCHAR(64), member_id BIGINT, address_snapshot VARCHAR(100), fulfillment_mode VARCHAR(16), pickup_store_id BIGINT, subtotal_cent INT, discount_cent INT, shipping_cent INT, total_cent INT, coupon_id BIGINT, request_id VARCHAR(64), status VARCHAR(16), create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_order_delivery(order_id BIGINT, status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_product(product_id BIGINT, product_code VARCHAR(64))");
        jdbc.execute("CREATE TABLE biz_order_item(order_item_id BIGINT, order_id BIGINT, sku_code VARCHAR(64), product_id BIGINT, product_name VARCHAR(64), product_spec VARCHAR(64), image_url VARCHAR(128), unit_price_cent INT, quantity INT, total_cent INT)");
        jdbc.execute("CREATE TABLE biz_order_refund(refund_id BIGINT, order_id BIGINT, member_id BIGINT, refund_no VARCHAR(64), amount_cent INT, reason VARCHAR(64), status VARCHAR(16), channel_status VARCHAR(16), create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_order_aftersale(order_id BIGINT, status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_order_pickup_redemption(pickup_redemption_id BIGINT PRIMARY KEY, order_id BIGINT, order_no VARCHAR(64), verifier_member_id BIGINT, store_id BIGINT, status VARCHAR(16), create_time TIMESTAMP)");
        jdbc.update("INSERT INTO biz_member VALUES(7,1,'0'),(8,NULL,'0')");
        jdbc.update("INSERT INTO biz_store(store_id,store_name,address,status) VALUES(1,'Store','Address','0'),(2,'Other','Other address','0')");
        marketing = new MiniMarketingService(jdbc, new PointExpiryService(jdbc), null, null);
        commerce = new MiniCommerceService(jdbc, new ObjectMapper(), new PointExpiryService(jdbc), null, null);
    }

    @Test
    public void paginationBoundsRejectMalformedOrExcessiveRequests()
    {
        assertEquals("", MiniListPage.all().suffix());
        assertEquals(" LIMIT 20 OFFSET 220", new MiniListPage(Map.of("pageNum", "12")).suffix());
        for (String value : new String[] { "0", "-1", "1.5", "", "101", "2147483648", "1 UNION SELECT 1" })
        {
            try { new MiniListPage(Map.of("pageSize", value)); fail(value); }
            catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
        }
        try { MiniListPage.status(Map.of("status", "paid' OR 1=1"), "paid"); fail(); }
        catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
    }

    @Test
    public void pointHistoryIncludesRecordsBeyondTwoHundredAndExcludesOtherMembers()
    {
        for (int id = 1; id <= 244; id++) jdbc.update("INSERT INTO biz_point_record VALUES(?,?,?,1,CURRENT_TIMESTAMP)", id, id <= 237 ? 7 : 8, "Point " + id);
        Map<String, Object> page = asPage(marketing.pointRecords(7, params(12)));
        assertPage(page, 237, 17, "17", "1");
        assertEquals(237, marketing.pointRecords(7).size());
        assertTrue(marketing.pointRecords(7, Collections.emptyMap()) instanceof List);
    }

    @Test
    public void drawHistoryUsesTheEntireMemberHistoryInStableDescendingOrder()
    {
        for (int id = 1; id <= 244; id++) jdbc.update("INSERT INTO biz_lottery_record VALUES(?,?,NULL,'none','',?,CURRENT_TIMESTAMP)", id, id <= 237 ? 7 : 8, "draw-" + id);
        assertPage(asPage(marketing.drawRecords(7, params(12))), 237, 17, "17", "1");
        assertEquals(237, marketing.drawRecords(7).size());
        assertEquals(7, rows(asPage(marketing.drawRecords(8, params(1)))).size());
    }

    @Test
    public void rewardStatusFiltersApplyBeforePagingAndExpiredAwardsHaveTheirOwnCount()
    {
        for (int id = 1; id <= 237; id++) reward(id, 7, "goods", "pending", 0);
        reward(238, 7, "goods", "redeemed", 0);
        reward(239, 8, "goods", "pending", 0);
        reward(240, 7, "cash", "pending", 1);
        reward(241, 7, "goods", "pending", 0);
        jdbc.update("UPDATE biz_reward SET expires_at=? WHERE reward_id=241", new Timestamp(System.currentTimeMillis() - 10000));
        assertPage(asPage(marketing.rewards(7, Map.of("pageNum", "12", "status", "pending"))), 237, 17, "17", "1");
        assertPage(asPage(marketing.rewards(7, Map.of("pageNum", "1", "status", "redeemed"))), 1, 1, "238", "238");
        assertPage(asPage(marketing.rewards(7, Map.of("pageNum", "1", "status", "closed"))), 1, 1, "241", "241");
        assertEquals(239, marketing.rewards(7).size());
    }

    @Test
    public void cashHistoryPreservesCentAmountsAndDoesNotIncludeOtherPrizeTypesOrMembers()
    {
        for (int id = 1; id <= 237; id++) reward(id, 7, "cash", "pending", id % 2 == 0 ? 100 : 1);
        reward(238, 7, "goods", "pending", 0);
        reward(239, 8, "cash", "pending", 100);
        Map<String, Object> page = asPage(marketing.cashRewards(7, params(12)));
        assertPage(page, 237, 17, "17", "1");
        assertEquals(1, ((Number) rows(page).get(0).get("amount")).intValue());
        assertEquals(237, marketing.cashRewards(7).size());
    }

    @Test
    public void verifierHistoryIncludesOlderRecordsAndStillEnforcesActiveStaff()
    {
        reward(1, 8, "goods", "redeemed", 0);
        for (int id = 1; id <= 237; id++) jdbc.update("INSERT INTO biz_redemption VALUES(?,1,8,1,7,'redeemed',?,CURRENT_TIMESTAMP)", id, "redemption-" + id);
        jdbc.update("INSERT INTO biz_redemption VALUES(238,1,8,1,8,'redeemed','other',CURRENT_TIMESTAMP)");
        assertPage(asPage(marketing.redemptions(7, params(12))), 237, 17, "17", "1");
        assertEquals(237, marketing.redemptions(7).size());
        jdbc.update("UPDATE biz_store SET status='1' WHERE store_id=1");
        try { marketing.redemptions(7, params(1)); fail(); }
        catch (MiniBusinessException expected) { assertEquals(403, expected.getCode()); }
    }

    @Test
    public void orderStatusSelectionCountsAllMatchingOrdersInsteadOfFilteringOnePage()
    {
        for (int id = 1; id <= 237; id++) order(id, 7, id > 200 ? "pending" : "paid");
        order(238, 8, "paid");
        Map<String, Object> page = asPage(commerce.orders(7, Map.of("pageNum", "10", "status", "paid")));
        assertPage(page, 200, 20, "ORDER-20", "ORDER-1");
        assertEquals(237, commerce.orders(7).size());
        jdbc.update("UPDATE biz_order SET status='shipping' WHERE order_id=1");
        jdbc.update("UPDATE biz_order SET status='receiving' WHERE order_id=2");
        assertEquals(2L, asPage(commerce.orders(7, Map.of("pageNum", "1", "status", "receiving"))).get("total"));
    }

    @Test
    public void pickupHistoryChecksBothVerifierAndAssignedStore()
    {
        order(1, 8, "complete");
        for (int id = 1; id <= 237; id++) jdbc.update("INSERT INTO biz_order_pickup_redemption VALUES(?,1,'ORDER-1',7,1,'complete',CURRENT_TIMESTAMP)", id);
        jdbc.update("INSERT INTO biz_order_pickup_redemption VALUES(238,1,'ORDER-1',7,2,'complete',CURRENT_TIMESTAMP),(239,1,'ORDER-1',8,1,'complete',CURRENT_TIMESTAMP)");
        assertPage(asPage(commerce.pickupRedemptions(7, params(12))), 237, 17, 17L, 1L);
        assertEquals(237, commerce.pickupRedemptions(7).size());
        try { commerce.pickupRedemptions(8, params(1)); fail(); }
        catch (MiniBusinessException expected) { assertEquals(403, expected.getCode()); }
    }

    private void reward(int id, int memberId, String kind, String status, int cents)
    {
        jdbc.update("INSERT INTO biz_reward(reward_id,member_id,reward_type,reward_name,amount_cent,points,exchange_code,image_url,status,create_time) VALUES(?,?,?,'Prize',?,0,?,'',?,CURRENT_TIMESTAMP)", id, memberId, kind, cents, "REDEEM-" + id, status);
    }

    private void order(int id, int memberId, String status)
    {
        jdbc.update("INSERT INTO biz_order VALUES(?,?,?,'{}','delivery',NULL,100,0,0,100,NULL,?,?,CURRENT_TIMESTAMP)", id, "ORDER-" + id, memberId, "request-" + id, status);
    }

    private Map<String, String> params(int page) { return Map.of("pageNum", String.valueOf(page), "pageSize", "20"); }
    @SuppressWarnings("unchecked") private Map<String, Object> asPage(Object result) { return (Map<String, Object>) result; }
    @SuppressWarnings("unchecked") private List<Map<String, Object>> rows(Map<String, Object> page) { return (List<Map<String, Object>>) page.get("rows"); }
    private void assertPage(Map<String, Object> page, long total, int size, Object first, Object last)
    {
        assertEquals(total, page.get("total"));
        assertEquals(size, rows(page).size());
        assertEquals(first, rows(page).get(0).get("id"));
        assertEquals(last, rows(page).get(size - 1).get("id"));
    }
}
