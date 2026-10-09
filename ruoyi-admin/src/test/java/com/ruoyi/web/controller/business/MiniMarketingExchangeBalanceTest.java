package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MiniMarketingExchangeBalanceTest
{
    @Test
    public void firstAndRepeatedExchangeReturnAuthoritativePointBalance()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:exchange_balance_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_point_account (member_id BIGINT PRIMARY KEY, balance INT)");
        jdbc.execute("CREATE TABLE biz_point_record (point_record_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "member_id BIGINT, change_amount INT, balance_after INT, source_type VARCHAR(32), "
                + "source_id VARCHAR(128), title VARCHAR(128), expires_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_mini_idempotency (member_id BIGINT, request_id VARCHAR(64), "
                + "action_type VARCHAR(32), payload_hash VARCHAR(64), reward_id BIGINT, "
                + "member_coupon_id BIGINT, result_type VARCHAR(32), result_value VARCHAR(128), "
                + "PRIMARY KEY (member_id, request_id))");
        jdbc.execute("CREATE TABLE biz_exchange_item (exchange_item_id BIGINT PRIMARY KEY, "
                + "exchange_code VARCHAR(64), item_name VARCHAR(64), item_type VARCHAR(16), "
                + "coupon_template_id BIGINT, spec VARCHAR(128), image_url VARCHAR(255), "
                + "redeem_store_id BIGINT, points INT, stock INT, per_user_limit INT, status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_reward (reward_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "member_id BIGINT, reward_type VARCHAR(16), reward_name VARCHAR(64), spec VARCHAR(128), "
                + "amount_cent INT, points INT, exchange_code VARCHAR(128), image_url VARCHAR(255), "
                + "redeem_store_id BIGINT, status VARCHAR(16) DEFAULT 'pending', expires_at TIMESTAMP, "
                + "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_store (store_id BIGINT PRIMARY KEY, store_name VARCHAR(64))");
        jdbc.update("INSERT INTO biz_point_account VALUES (7,500)");
        jdbc.update("INSERT INTO biz_exchange_item VALUES (1,'cup','品鉴杯','goods',NULL,'礼盒','',NULL,200,2,2,'0')");
        MiniMarketingService service = new MiniMarketingService(jdbc, new PointExpiryService(jdbc), null, null);

        Map<String, Object> first = service.exchange(7, "cup", "request-0001");
        assertEquals(300, ((Number) first.get("pointBalance")).intValue());
        Map<String, Object> replay = service.exchange(7, "cup", "request-0001");
        assertEquals(first.get("id"), replay.get("id"));
        assertEquals(300, ((Number) replay.get("pointBalance")).intValue());
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM biz_reward", Integer.class));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT COUNT(*) FROM biz_point_record", Integer.class));
    }
}
