package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MiniMarketingExchangeRewardTest
{
    @Test
    public void exchangeCodeMatchesExactlyAndIssuedStoreScopeDoesNotChange()
    {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:exchange_reward_join;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE biz_store (store_id BIGINT PRIMARY KEY, store_name VARCHAR(64), status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_member (member_id BIGINT PRIMARY KEY, staff_store_id BIGINT, "
                + "status CHAR(1), nickname VARCHAR(64), phone VARCHAR(32))");
        jdbc.execute("CREATE TABLE biz_exchange_item (exchange_code VARCHAR(64) PRIMARY KEY, "
                + "redeem_store_id BIGINT, spec VARCHAR(64), image_url VARCHAR(256))");
        jdbc.execute("CREATE TABLE biz_reward (reward_id BIGINT PRIMARY KEY, member_id BIGINT, "
                + "reward_type VARCHAR(16), reward_name VARCHAR(64), amount_cent INT, points INT, "
                + "redeem_store_id BIGINT, exchange_code VARCHAR(128), image_url VARCHAR(256), "
                + "spec VARCHAR(64), status VARCHAR(16), expires_at TIMESTAMP, create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_redemption (redemption_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "reward_id BIGINT, member_id BIGINT, store_id BIGINT, verifier_member_id BIGINT, "
                + "status VARCHAR(16), request_id VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO biz_store VALUES(1,'一号门店','0'),(2,'二号门店','0')");
        jdbc.update("INSERT INTO biz_member VALUES(1,1,'0','核销员',NULL),(2,NULL,'0','会员',NULL)");
        jdbc.update("INSERT INTO biz_exchange_item VALUES('CUP_1',2,'错误匹配','wrong.jpg')");
        jdbc.update("INSERT INTO biz_exchange_item VALUES('CUPA1',2,'正确奖品','right.jpg')");
        String code = "EX-CUPA1-123456789abc";
        jdbc.update("INSERT INTO biz_reward VALUES(10,2,'goods','品鉴杯',0,0,NULL,?, '', '', "
                + "'pending',NULL,CURRENT_TIMESTAMP)", code);

        MiniMarketingService service = new MiniMarketingService(jdbc, null, null, null);
        Map<String, Object> preview = service.lookup(1, code);
        assertEquals("正确奖品", preview.get("spec"));
        assertEquals("right.jpg", preview.get("image"));
        assertEquals("all", preview.get("redeemStoreScope"));
        assertFalse(preview.containsKey("redeemStoreId"));

        Map<String, Object> confirmed = service.confirm(1, code, "confirm-0001");
        assertEquals("redeemed", confirmed.get("status"));
        assertEquals("all", confirmed.get("redeemStoreScope"));
        assertEquals(Long.valueOf(1), jdbc.queryForObject(
                "SELECT store_id FROM biz_redemption WHERE reward_id=10", Long.class));

        String longItemCode = "L" + new String(new char[63]).replace('\0', 'A');
        String longRewardCode = "EX-" + longItemCode + "-123456789abc";
        assertEquals(80, longRewardCode.length());
        jdbc.update("INSERT INTO biz_exchange_item VALUES(?,NULL,'长编码奖品','long.jpg')", longItemCode);
        jdbc.update("INSERT INTO biz_reward VALUES(11,2,'goods','长编码奖品',0,0,NULL,?, '', '', "
                + "'pending',NULL,CURRENT_TIMESTAMP)", longRewardCode);
        String scene = RedemptionScene.encode(11, longRewardCode);
        assertTrue(scene.length() <= 32);
        assertEquals(Long.valueOf(11), RedemptionScene.rewardId(scene));
        assertEquals("长编码奖品", service.lookup(1, scene).get("spec"));
        assertEquals("redeemed", service.confirm(1, scene, "confirm-0002").get("status"));
        String wrongScene = scene.substring(0, scene.length() - 1) + (scene.endsWith("0") ? "1" : "0");
        assertThrows(MiniBusinessException.class, () -> service.lookup(1, wrongScene));

        jdbc.update("INSERT INTO biz_reward VALUES(12,2,'goods','指定门店奖品',0,0,2,'STORE-ONLY', '', '', "
                + "'pending',NULL,CURRENT_TIMESTAMP)");
        assertEquals(403, assertThrows(MiniBusinessException.class,
                () -> service.lookup(1, "STORE-ONLY")).getCode());
        assertEquals(403, assertThrows(MiniBusinessException.class,
                () -> service.lookup(1, RedemptionScene.encode(12, "STORE-ONLY"))).getCode());
        jdbc.update("UPDATE biz_member SET staff_store_id=2 WHERE member_id=1");
        assertEquals("指定门店奖品", service.lookup(1, "STORE-ONLY").get("name"));
        jdbc.update("UPDATE biz_store SET status='1' WHERE store_id=2");
        assertEquals(403, assertThrows(MiniBusinessException.class,
                () -> service.lookup(1, "STORE-ONLY")).getCode());
        assertEquals("pending", jdbc.queryForObject("SELECT status FROM biz_reward WHERE reward_id=12", String.class));
    }
}
