package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class BusinessAdminCouponDeletionTest
{
    @Test
    public void socialCampaignReferencePreventsDeletingCouponTemplate() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:coupon_delete_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_member_coupon(coupon_template_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_lottery_prize(coupon_template_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_lottery_code_outcome(coupon_template_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_exchange_item(coupon_template_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_social_campaign(coupon_template_id BIGINT)");
        BusinessAdminController controller = new BusinessAdminController(jdbc, new BusinessDataScopeService(jdbc));
        Method references = BusinessAdminController.class.getDeclaredMethod("businessReferenceMessage", String.class, long.class);
        references.setAccessible(true);

        jdbc.update("INSERT INTO biz_social_campaign VALUES(7)");
        assertEquals("优惠券模板已被业务数据引用，请先停用后再删除", references.invoke(controller, "couponTemplate", 7L));
        assertNull(references.invoke(controller, "couponTemplate", 8L));
    }
}
