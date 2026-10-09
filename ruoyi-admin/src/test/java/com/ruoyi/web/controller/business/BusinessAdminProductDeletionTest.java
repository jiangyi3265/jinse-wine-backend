package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class BusinessAdminProductDeletionTest
{
    @Test
    public void cartAndVideoMountReferencesPreventDeletingProduct() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:product_delete_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_product_sku(product_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_order_item(product_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_cart_item(product_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_video_product(product_id BIGINT)");
        jdbc.execute("CREATE TABLE biz_video_feed_mount(product_id BIGINT)");
        BusinessAdminController controller = new BusinessAdminController(jdbc, new BusinessDataScopeService(jdbc));
        Method references = BusinessAdminController.class.getDeclaredMethod("businessReferenceMessage", String.class, long.class);
        references.setAccessible(true);

        jdbc.update("INSERT INTO biz_cart_item VALUES(3)");
        assertEquals("商品已被业务数据引用，请先停用后再删除", references.invoke(controller, "product", 3L));
        assertNull(references.invoke(controller, "product", 4L));

        jdbc.update("DELETE FROM biz_cart_item WHERE product_id=3");
        assertNull(references.invoke(controller, "product", 3L));

        jdbc.update("INSERT INTO biz_video_feed_mount VALUES(3)");
        assertEquals("商品已被业务数据引用，请先停用后再删除", references.invoke(controller, "product", 3L));
        jdbc.update("DELETE FROM biz_video_feed_mount WHERE product_id=3");
        assertNull(references.invoke(controller, "product", 3L));
    }
}
