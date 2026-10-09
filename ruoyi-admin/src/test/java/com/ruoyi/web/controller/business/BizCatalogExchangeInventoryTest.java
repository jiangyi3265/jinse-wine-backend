package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;

import com.ruoyi.system.domain.business.BizBrandConfig;
import com.ruoyi.system.domain.business.BizCouponTemplate;
import com.ruoyi.system.domain.business.BizExchangeItem;
import com.ruoyi.system.domain.business.BizProduct;
import com.ruoyi.system.domain.business.BizStore;
import com.ruoyi.system.mapper.BizCatalogMapper;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class BizCatalogExchangeInventoryTest
{
    @Test
    public void exchangeCatalogIncludesUnlimitedStockAndOnlyRedeemableReferences() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:exchange_catalog_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_exchange_item (exchange_item_id BIGINT PRIMARY KEY, "
                + "exchange_code VARCHAR(64), item_name VARCHAR(64), item_type VARCHAR(16), "
                + "coupon_template_id BIGINT, spec VARCHAR(128), image_url VARCHAR(255), "
                + "redeem_store_id BIGINT, points INT, stock INT, per_user_limit INT, sort_num INT, status CHAR(1))");
        jdbc.execute("CREATE TABLE biz_coupon_template (coupon_template_id BIGINT PRIMARY KEY, "
                + "template_code VARCHAR(64), status CHAR(1), stock INT, start_time TIMESTAMP, end_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE biz_store (store_id BIGINT PRIMARY KEY, store_name VARCHAR(64), status CHAR(1))");
        jdbc.update("INSERT INTO biz_store VALUES (1,'营业门店','0'),(2,'停用门店','1')");
        jdbc.update("INSERT INTO biz_coupon_template VALUES (1,'active','0',-1,NULL,NULL)");
        jdbc.update("INSERT INTO biz_coupon_template VALUES (2,'disabled','1',10,NULL,NULL)");
        jdbc.update("INSERT INTO biz_coupon_template VALUES (3,'empty','0',0,NULL,NULL)");
        jdbc.update("INSERT INTO biz_coupon_template VALUES (4,'expired','0',10,NULL,?)",
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
        jdbc.update("INSERT INTO biz_coupon_template VALUES (5,'future','0',10,?,NULL)",
                Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
        insert(jdbc, 1, "goods-unlimited", "goods", null, null, -1);
        insert(jdbc, 2, "coupon-unlimited", "coupon", 1L, null, -1);
        insert(jdbc, 3, "sold-out", "goods", null, null, 0);
        insert(jdbc, 4, "coupon-disabled", "coupon", 2L, null, 10);
        insert(jdbc, 5, "coupon-empty", "coupon", 3L, null, 10);
        insert(jdbc, 6, "coupon-expired", "coupon", 4L, null, 10);
        insert(jdbc, 7, "coupon-future", "coupon", 5L, null, 10);
        insert(jdbc, 8, "store-disabled", "goods", null, 2L, 10);
        insert(jdbc, 9, "store-active", "goods", null, 1L, 10);

        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("BizProduct", BizProduct.class);
        configuration.getTypeAliasRegistry().registerAlias("BizStore", BizStore.class);
        configuration.getTypeAliasRegistry().registerAlias("BizCouponTemplate", BizCouponTemplate.class);
        configuration.getTypeAliasRegistry().registerAlias("BizExchangeItem", BizExchangeItem.class);
        configuration.getTypeAliasRegistry().registerAlias("BizBrandConfig", BizBrandConfig.class);
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), source));
        String resource = "mapper/business/BizCatalogMapper.xml";
        try (InputStream xml = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(xml, configuration, resource, configuration.getSqlFragments()).parse();
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession session = factory.openSession())
        {
            List<BizExchangeItem> items = session.getMapper(BizCatalogMapper.class).selectPublishedExchangeItems();
            assertEquals(Arrays.asList("goods-unlimited", "coupon-unlimited", "store-active"),
                    items.stream().map(BizExchangeItem::getId).collect(Collectors.toList()));
            assertEquals(Integer.valueOf(-1), items.get(0).getStock());
            assertEquals(Integer.valueOf(-1), items.get(1).getStock());
        }
    }

    private static void insert(JdbcTemplate jdbc, long id, String code, String type,
            Long templateId, Long storeId, int stock)
    {
        jdbc.update("INSERT INTO biz_exchange_item VALUES (?,?,?,?,?,NULL,NULL,?,100,?,1,?, '0')",
                id, code, code, type, templateId, storeId, stock, id);
    }
}
