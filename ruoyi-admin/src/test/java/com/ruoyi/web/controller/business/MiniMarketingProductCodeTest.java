package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class MiniMarketingProductCodeTest
{
    @Test
    public void requiresPublishedProductAndActivePromoter()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:product_code_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_product(product_id bigint PRIMARY KEY, product_code varchar(64), status char(1))");
        jdbc.execute("CREATE TABLE biz_promotion_relation(member_id bigint PRIMARY KEY, invite_code varchar(64), status char(1))");
        jdbc.update("INSERT INTO biz_product VALUES(3,'double','0')");
        jdbc.update("INSERT INTO biz_product VALUES(4,'hidden','1')");
        jdbc.update("INSERT INTO biz_promotion_relation VALUES(1,'JS1PROMO','0')");
        jdbc.update("INSERT INTO biz_promotion_relation VALUES(2,'JS2PROMO','1')");
        MiniMarketingService service = new MiniMarketingService(jdbc, null,
                new WechatMiniCodeService(new ObjectMapper()), null);

        assertEquals("JS1PROMO", service.promotionCode(1, "double").get("code"));
        assertEquals("pending", service.promotionCode(1, "double").get("qrCodeStatus"));
        assertEquals(404, assertThrows(MiniBusinessException.class,
                () -> service.promotionCode(1, "hidden")).getCode());
        assertEquals(409, assertThrows(MiniBusinessException.class,
                () -> service.promotionCode(2, "double")).getCode());
    }
}
