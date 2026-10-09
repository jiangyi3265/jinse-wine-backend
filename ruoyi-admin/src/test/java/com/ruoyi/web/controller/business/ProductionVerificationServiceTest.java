package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Collections;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;

public class ProductionVerificationServiceTest
{
    private JdbcTemplate jdbc;
    private ProductionVerificationService service;

    @Before
    public void setUp()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:production_verification_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_production_verification(verification_id bigint AUTO_INCREMENT PRIMARY KEY, "
                + "check_key varchar(32), config_fingerprint char(64), evidence varchar(1000), "
                + "verified_by varchar(64), verified_at timestamp DEFAULT CURRENT_TIMESTAMP)");
        service = new ProductionVerificationService(jdbc);
        SysUser user = new SysUser();
        user.setUserName("admin");
        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @After
    public void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    public void configurationChangesInvalidatePreviousEvidence()
    {
        String first = ProductionVerificationService.fingerprint("pay", "merchant-1", "secret-1");
        String changed = ProductionVerificationService.fingerprint("pay", "merchant-1", "secret-2");
        assertNull(service.matching("pay", first));
        assertThrows(MiniBusinessException.class, () -> service.record("pay", first, "short"));
        service.record("pay", first, "payment and refund callback receipts 001");
        assertNotNull(service.matching("pay", first));
        assertNull(service.matching("pay", changed));

        service.record("pay", changed, "payment and refund callback receipts 002");
        assertNull(service.matching("pay", first));
        assertNotNull(service.matching("pay", changed));
        service.record("mini", ProductionVerificationService.fingerprint("mini", "release"),
                "published mini program and legal domain verified");
        assertThrows(MiniBusinessException.class, () -> service.record("unknown", changed, "unsupported platform receipt"));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM biz_production_verification", Integer.class).intValue());
        assertEquals("admin", jdbc.queryForObject("SELECT verified_by FROM biz_production_verification ORDER BY verification_id DESC LIMIT 1", String.class));
    }

    @Test
    public void fingerprintUsesUnambiguousPartsAndModuleKey()
    {
        String value = ProductionVerificationService.fingerprint("pay", "ab", "c");
        assertEquals(64, value.length());
        org.junit.Assert.assertNotEquals(value, ProductionVerificationService.fingerprint("pay", "a", "bc"));
        org.junit.Assert.assertNotEquals(value, ProductionVerificationService.fingerprint("transfer", "ab", "c"));
    }
}
