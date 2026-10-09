package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WechatWorkGroupQrServiceTest
{
    private final ObjectMapper json = new ObjectMapper();
    private FailingJdbcTemplate jdbc;
    private FakeWechat http;
    private WechatWorkGroupQrService service;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:group_qr_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new FailingJdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_channel_config(config_key varchar(64) PRIMARY KEY, config_value text NOT NULL)");
        http = new FakeWechat();
        service = new WechatWorkGroupQrService(jdbc, new DataSourceTransactionManager(source), http);
        Field corpId = WechatWorkGroupQrService.class.getDeclaredField("corpId");
        corpId.setAccessible(true);
        corpId.set(service, "test-corp");
        Field secret = WechatWorkGroupQrService.class.getDeclaredField("secret");
        secret.setAccessible(true);
        secret.set(service, "test-secret");
    }

    @Test
    public void keepsAllFailedOldCodesAcrossSeveralRotations() throws Exception
    {
        service.create("chat1");
        http.failDeletes.add("config-1");
        Map<String, Object> second = service.create("chat1");
        assertEquals(1, second.get("pendingRevocations"));
        http.failDeletes.add("config-2");
        Map<String, Object> third = service.create("chat1");
        assertEquals(2, third.get("pendingRevocations"));
        assertFalse((Boolean) third.get("oldCodeRevoked"));
        JsonNode pending = json.readTree(config("wechat.work.groupQrPreviousConfigId"));
        assertEquals(2, pending.size());
        assertEquals("config-1", pending.get(0).asText());
        assertEquals("config-2", pending.get(1).asText());

        http.failDeletes.remove("config-1");
        service.refreshExpiringCode();
        assertEquals("[\"config-2\"]", config("wechat.work.groupQrPreviousConfigId"));
        assertEquals("1 个旧二维码撤销待重试", config("wechat.work.groupQrRevocationStatus"));

        http.failDeletes.clear();
        service.refreshExpiringCode();
        assertEquals("", config("wechat.work.groupQrPreviousConfigId"));
        assertEquals("已撤销旧二维码", config("wechat.work.groupQrRevocationStatus"));
        assertEquals("config-3", config("wechat.work.groupQrConfigId"));
    }

    @Test
    public void retainsPendingRecordWhenDeleteSucceededButDatabaseConfirmationFailed() throws Exception
    {
        service.create("chat1");
        http.afterDelete = () -> jdbc.failPendingClear = true;
        Map<String, Object> response = service.create("chat1");
        assertEquals(1, response.get("pendingRevocations"));
        assertFalse((Boolean) response.get("oldCodeRevoked"));
        assertEquals("[\"config-1\"]", config("wechat.work.groupQrPreviousConfigId"));
        assertEquals("config-2", config("wechat.work.groupQrConfigId"));
        assertThrows(MiniBusinessException.class, () -> service.confirmRevoked("config-2", "verified in WeCom console"));
        assertThrows(MiniBusinessException.class, () -> service.confirmRevoked("config-1", "short"));
        Map<String, Object> reconciled = service.confirmRevoked("config-1", "verified in WeCom console");
        assertEquals(0, reconciled.get("pendingRevocations"));
        assertEquals("", config("wechat.work.groupQrPreviousConfigId"));
        assertEquals("旧二维码已人工核验撤销", config("wechat.work.groupQrRevocationStatus"));
    }

    @Test
    public void readsLegacySinglePendingConfigId()
    {
        service.create("chat1");
        jdbc.update("UPDATE biz_channel_config SET config_value=? WHERE config_key=?", "legacy-config", "wechat.work.groupQrPreviousConfigId");
        service.refreshExpiringCode();
        assertEquals("", config("wechat.work.groupQrPreviousConfigId"));
        assertEquals("config-1", config("wechat.work.groupQrConfigId"));
    }

    private String config(String key)
    {
        List<String> values = jdbc.queryForList("SELECT config_value FROM biz_channel_config WHERE config_key=?", String.class, key);
        return values.isEmpty() ? "" : values.get(0);
    }

    private static final class FailingJdbcTemplate extends JdbcTemplate
    {
        private boolean failPendingClear;

        private FailingJdbcTemplate(JdbcDataSource source) { super(source); }

        @Override
        public int update(String sql, Object... args)
        {
            if (failPendingClear && args.length == 2 && "wechat.work.groupQrPreviousConfigId".equals(args[0]) && "".equals(args[1]))
            {
                failPendingClear = false;
                throw new IllegalStateException("simulated database write failure");
            }
            return super.update(sql, args);
        }
    }

    private final class FakeWechat extends RestTemplate
    {
        private final Set<String> failDeletes = new HashSet<>();
        private int nextConfigId = 1;
        private Runnable afterDelete;

        @Override
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables)
        {
            return responseType.cast(node("{\"errcode\":0,\"access_token\":\"test-token\",\"expires_in\":7200}"));
        }

        @Override
        public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables)
        {
            if (url.contains("add_join_way"))
            {
                String id = "config-" + nextConfigId++;
                return responseType.cast(node("{\"errcode\":0,\"config_id\":\"" + id + "\",\"qr_code\":\"https://example.test/" + id + "\"}"));
            }
            String id = String.valueOf(((Map<?, ?>) request).get("config_id"));
            if (failDeletes.contains(id)) return responseType.cast(node("{\"errcode\":1,\"errmsg\":\"temporary failure\"}"));
            if (afterDelete != null)
            {
                Runnable action = afterDelete;
                afterDelete = null;
                action.run();
            }
            return responseType.cast(node("{\"errcode\":0}"));
        }

        private JsonNode node(String value)
        {
            try { return json.readTree(value); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }
    }
}
