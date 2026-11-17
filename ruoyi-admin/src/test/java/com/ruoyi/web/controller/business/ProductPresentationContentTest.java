package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class ProductPresentationContentTest
{
    private static final String ORIGINAL = "{\"productCode\":\"WINE-1\",\"photos\":[\"/media/front.jpg\"],\"detail\":{\"spec\":\"500mL\"}}";
    private JdbcTemplate jdbc;
    private BusinessContentAdminController controller;

    @Before
    public void setUp()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:product_presentation_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_product(product_code VARCHAR(64) PRIMARY KEY,spec VARCHAR(128))");
        jdbc.execute("CREATE TABLE biz_mini_content(content_id BIGINT PRIMARY KEY,content_key VARCHAR(128),"
                + "content_type VARCHAR(32),title VARCHAR(128),subtitle VARCHAR(255),content_json CLOB,"
                + "image_url VARCHAR(512),link_route VARCHAR(128),sort_num INT,status VARCHAR(1),remark VARCHAR(500),"
                + "create_time TIMESTAMP,update_time TIMESTAMP)");
        jdbc.update("INSERT INTO biz_product VALUES('WINE-1','500mL')");
        jdbc.update("INSERT INTO biz_mini_content(content_id,content_key,content_type,content_json,sort_num,status) "
                + "VALUES(1,'product.gallery.WINE-1','page',?,0,'0')", ORIGINAL);
        controller = new BusinessContentAdminController(jdbc, new ObjectMapper());
    }

    @Test
    public void savesCurrentPresentationAndRejectsStaleCopy()
    {
        Map<String, Object> body = body();
        String updated = ORIGINAL.replace("front.jpg", "updated.jpg");
        body.put("contentJson", updated);
        assertEquals(200, controller.edit(body).get("code"));
        assertEquals(409, controller.edit(body()).get("code"));
        assertEquals(updated, stored());
    }

    @Test
    public void protectsRenamedOrUnpublishedContent()
    {
        jdbc.update("UPDATE biz_mini_content SET status='1' WHERE content_id=1");
        assertEquals(409, controller.edit(body()).get("code"));
        jdbc.update("UPDATE biz_mini_content SET status='0',content_key='product.gallery.NEW' WHERE content_id=1");
        assertEquals(409, controller.edit(body()).get("code"));
        assertEquals(ORIGINAL, stored());
    }

    @Test
    public void rejectsChangedProductSpecAndInvalidPhotoWithoutWriting()
    {
        jdbc.update("UPDATE biz_product SET spec='1500mL'");
        assertNotEquals(200, controller.edit(body()).get("code"));
        jdbc.update("UPDATE biz_product SET spec='500mL'");
        Map<String, Object> body = body();
        body.put("contentJson", ORIGINAL.replace("/media/front.jpg", "javascript:alert(1)"));
        assertNotEquals(200, controller.edit(body).get("code"));
        body.put("contentJson", ORIGINAL.replace("[\"/media/front.jpg\"]", "[]"));
        assertNotEquals(200, controller.edit(body).get("code"));
        assertEquals(ORIGINAL, stored());
    }

    @Test
    public void exactKeyQueryDoesNotIncludeSimilarProducts()
    {
        jdbc.update("INSERT INTO biz_mini_content(content_id,content_key,content_type,content_json,sort_num,status) "
                + "VALUES(2,'product.gallery.WINE-10','page','{}',0,'0')");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("exactContentKey", "product.gallery.WINE-1");
        assertEquals(1L, controller.list(params).getTotal());
        params.clear();
        params.put("contentKey", "product.gallery.WINE-1");
        assertEquals(2L, controller.list(params).getTotal());
    }

    @Test
    public void legacyContentEditorStillAcceptsUpdatesWithoutVersionFields()
    {
        Map<String, Object> body = body();
        body.remove("expectedContentJson");
        body.remove("expectedProductSpec");
        body.remove("expectedContentKey");
        body.remove("expectedStatus");
        body.put("contentJson", "{\"sections\":[]}");
        assertEquals(200, controller.edit(body).get("code"));
    }

    private Map<String, Object> body()
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contentId", 1);
        body.put("contentKey", "product.gallery.WINE-1");
        body.put("contentType", "page");
        body.put("contentJson", ORIGINAL);
        body.put("status", "0");
        body.put("expectedContentJson", ORIGINAL);
        body.put("expectedContentKey", "product.gallery.WINE-1");
        body.put("expectedStatus", "0");
        body.put("expectedProductSpec", "500mL");
        return body;
    }

    private String stored()
    {
        return jdbc.queryForObject("SELECT content_json FROM biz_mini_content WHERE content_id=1", String.class);
    }
}
