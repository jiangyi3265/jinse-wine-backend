package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class MemberProfileTest
{
    private JdbcTemplate jdbc;
    private MiniCommerceService commerce;

    @Before public void setup()
    {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:profile_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE biz_member(member_id BIGINT PRIMARY KEY,nickname VARCHAR(128),phone VARCHAR(32),status CHAR(1))");
        jdbc.update("INSERT INTO biz_member VALUES(1,'原昵称','13800000000','0'),(2,'其他会员','13900000000','0'),(3,'停用会员','13700000000','1')");
        commerce = new MiniCommerceService(jdbc, null, null, null, null);
    }

    @Test public void updatesNameAndPhoneOnOnlyCurrentMember()
    {
        commerce.updateProfile(1, " 微信昵称 ", " 13600000000 ");
        assertEquals("微信昵称", jdbc.queryForObject("SELECT nickname FROM biz_member WHERE member_id=1", String.class));
        assertEquals("13600000000", jdbc.queryForObject("SELECT phone FROM biz_member WHERE member_id=1", String.class));
        assertEquals("13900000000", jdbc.queryForObject("SELECT phone FROM biz_member WHERE member_id=2", String.class));
    }

    @Test public void nicknameOnlyOldClientPreservesPhone()
    {
        commerce.updateProfile(1, "新昵称");
        assertEquals("13800000000", jdbc.queryForObject("SELECT phone FROM biz_member WHERE member_id=1", String.class));
    }

    @Test public void optionalPhoneCanBeCleared()
    {
        commerce.updateProfile(1, "新昵称", " ");
        assertEquals("", jdbc.queryForObject("SELECT phone FROM biz_member WHERE member_id=1", String.class));
    }

    @Test public void invalidPhoneDoesNotPartiallySaveName()
    {
        for (String phone : new String[] { "123", "138000000000", "abc", "00000000000" })
        {
            try { commerce.updateProfile(1, "不得保存", phone); fail("Invalid phone accepted"); }
            catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
        }
        assertEquals("原昵称", jdbc.queryForObject("SELECT nickname FROM biz_member WHERE member_id=1", String.class));
    }

    @Test public void missingOrDisabledMemberCannotUpdate()
    {
        for (long member : new long[] { 3, 999 })
        {
            try { commerce.updateProfile(member, "不得保存", "13600000000"); fail("Unavailable member accepted"); }
            catch (MiniBusinessException expected) { assertEquals(404, expected.getCode()); }
        }
        assertEquals("停用会员", jdbc.queryForObject("SELECT nickname FROM biz_member WHERE member_id=3", String.class));
    }
}
