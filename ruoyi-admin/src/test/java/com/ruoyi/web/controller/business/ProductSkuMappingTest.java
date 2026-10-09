package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class ProductSkuMappingTest
{
    private JdbcTemplate jdbc;
    private BusinessExtensionController controller;

    @Before
    public void setUp()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:sku_mapping_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_product(product_id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE biz_product_sku(sku_id BIGINT PRIMARY KEY,product_id BIGINT,sku_code VARCHAR(64),"
                + "sku_name VARCHAR(128),price_cent INT,stock INT,spec_json VARCHAR(255),status VARCHAR(1),"
                + "external_sku_id VARCHAR(128),video_sync_status VARCHAR(24),video_last_error VARCHAR(255),"
                + "video_retry_at TIMESTAMP,video_retry_count INT)");
        jdbc.execute("CREATE TABLE biz_video_product(product_id BIGINT,channel VARCHAR(24),external_id VARCHAR(128),"
                + "sync_status VARCHAR(24),last_error VARCHAR(255),publish_payload_json CLOB,"
                + "publish_status VARCHAR(24),publish_error VARCHAR(255))");
        jdbc.update("INSERT INTO biz_product VALUES(1)");
        jdbc.update("INSERT INTO biz_video_product VALUES(1,'video','remote-product-1','pending','',NULL,'draft','')");
        jdbc.update("INSERT INTO biz_product_sku VALUES(11,1,'SKU-11','大瓶',39900,8,'{}','0','remote-sku-11','success','',NULL,0)");
        controller = new BusinessExtensionController(jdbc, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void mappingSaveDoesNotReplaceNewSkuOrChangePriceAndStock()
    {
        jdbc.update("INSERT INTO biz_product_sku VALUES(12,1,'SKU-12','小瓶',19900,6,'{}','0','','pending','',NULL,0)");
        assertEquals(200, controller.saveProductSkuMappings(1, body("remote-product-1",
                mapping(11, "remote-sku-11", "remote-sku-new"))).get("code"));
        Map<String, Object> original = jdbc.queryForMap("SELECT price_cent,stock,external_sku_id,video_sync_status FROM biz_product_sku WHERE sku_id=11");
        assertEquals(39900, original.get("price_cent"));
        assertEquals(8, original.get("stock"));
        assertEquals("remote-sku-new", original.get("external_sku_id"));
        assertEquals("pending", original.get("video_sync_status"));
        assertEquals(Integer.valueOf(19900), jdbc.queryForObject("SELECT price_cent FROM biz_product_sku WHERE sku_id=12", Integer.class));
        assertEquals(Integer.valueOf(2), jdbc.queryForObject("SELECT COUNT(*) FROM biz_product_sku WHERE product_id=1", Integer.class));
    }

    @Test
    public void rejectsStaleOrDuplicateMappingsWithoutChangingRows()
    {
        jdbc.update("INSERT INTO biz_product_sku VALUES(12,1,'SKU-12','小瓶',19900,6,'{}','0','remote-sku-12','success','',NULL,0)");
        assertNotEquals(200, controller.saveProductSkuMappings(1, body("remote-product-1",
                mapping(11, "old-value", "remote-sku-new"))).get("code"));
        assertNotEquals(200, controller.saveProductSkuMappings(1, body("remote-product-1",
                mapping(11, "remote-sku-11", "remote-sku-12"))).get("code"));
        assertNotEquals(200, controller.saveProductSkuMappings(1, body("other-product",
                mapping(11, "remote-sku-11", "remote-sku-new"))).get("code"));
        assertEquals("remote-sku-11", jdbc.queryForObject(
                "SELECT external_sku_id FROM biz_product_sku WHERE sku_id=11", String.class));
    }

    @Test
    public void productRemapClearsOldSkuMappingsAndStaleSkuListCannotDeleteNewRows()
    {
        jdbc.update("INSERT INTO biz_product_sku VALUES(12,1,'SKU-12','小瓶',19900,6,'{}','0','remote-sku-12','success','',NULL,0)");
        new WechatShopProductService(jdbc, null, null).saveMapping(1, "remote-product-2", null);
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_product_sku WHERE product_id=1 AND external_sku_id<>''", Integer.class));
        assertNotEquals(200, controller.saveProductSkus(1, skuBody()).get("code"));
        assertEquals(Integer.valueOf(2), jdbc.queryForObject("SELECT COUNT(*) FROM biz_product_sku WHERE product_id=1", Integer.class));
    }

    private static Map<String, Object> body(String externalProductId, Map<String, Object> mapping)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("externalProductId", externalProductId);
        body.put("mappings", Collections.singletonList(mapping));
        return body;
    }

    private static Map<String, Object> mapping(long skuId, String previous, String next)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("skuId", skuId);
        row.put("previousExternalSkuId", previous);
        row.put("externalSkuId", next);
        return row;
    }

    private static Map<String, Object> skuBody()
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("skuId", 11L);
        row.put("skuName", "大瓶");
        row.put("priceCent", 39900);
        row.put("stock", 8);
        row.put("status", "0");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("skus", Collections.singletonList(row));
        body.put("expectedSkuIds", Arrays.asList(11L));
        return body;
    }
}
