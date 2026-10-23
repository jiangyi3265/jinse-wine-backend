package com.ruoyi.web.controller.business;

import static org.junit.Assert.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.multipart.MultipartFile;

public class ExternalTicketServiceTest {
    private static MultipartFile file(byte[] bytes) {
        return new MultipartFile() {
            public String getName() { return "file"; }
            public String getOriginalFilename() { return "ticket.png"; }
            public String getContentType() { return "image/png"; }
            public boolean isEmpty() { return bytes.length == 0; }
            public long getSize() { return bytes.length; }
            public byte[] getBytes() { return bytes; }
            public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
            public void transferTo(java.io.File destination) throws java.io.IOException { java.nio.file.Files.write(destination.toPath(), bytes); }
        };
    }
    private JdbcTemplate jdbc;
    private ExternalTicketService service;
    private byte[] image;

    @Before
    public void setup() throws Exception {
        DriverManagerDataSource data = new DriverManagerDataSource("jdbc:h2:mem:ticket_" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(data); service = new ExternalTicketService(jdbc);
        jdbc.execute("CREATE TABLE biz_lottery_activity(activity_id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE biz_lottery_prize(prize_id BIGINT PRIMARY KEY,activity_id BIGINT,prize_type VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_external_ticket(ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,prize_id BIGINT,provider_name VARCHAR(128),external_no VARCHAR(128),qr_image BLOB,qr_mime VARCHAR(32),qr_sha256 VARCHAR(64) UNIQUE,valid_until TIMESTAMP,status VARCHAR(16) DEFAULT 'available',code_id BIGINT UNIQUE,reward_id BIGINT UNIQUE,create_by VARCHAR(64),issued_at TIMESTAMP,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(provider_name,external_no))");
        jdbc.execute("CREATE TABLE biz_reward(reward_id BIGINT PRIMARY KEY,member_id BIGINT,reward_type VARCHAR(16),status VARCHAR(16),expires_at TIMESTAMP)");
        jdbc.update("INSERT INTO biz_lottery_activity VALUES(1)");
        jdbc.update("INSERT INTO biz_lottery_prize VALUES(2,1,'ticket'),(3,1,'goods')");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB), "png", output); image = output.toByteArray();
    }

    private void upload(long prize, String no, byte[] data) throws Exception {
        service.upload(prize, "Ticket vendor", no, "2099-01-01 00:00:00", file(data), "admin");
    }

    @Test public void reservesUniqueTicketAndReleasesWithoutDuplicating() throws Exception {
        upload(2, "ticket-1", image);
        assertEquals(1, service.available(2));
        Map<String, Object> ticket = service.pick(2);
        service.reserve(ticket.get("ticket_id"), 10);
        assertEquals(0, service.available(2)); assertNull(service.pick(2));
        try { service.reserve(ticket.get("ticket_id"), 11); fail(); } catch (MiniBusinessException expected) { assertEquals(409, expected.getCode()); }
        service.release(10); service.release(10);
        assertEquals(1, service.available(2));
    }

    @Test public void duplicateImageAndTicketIdentityAreRejected() throws Exception {
        upload(2, "ticket-1", image);
        try { upload(2, "ticket-2", image); fail(); } catch (MiniBusinessException expected) { assertEquals(409, expected.getCode()); }
        assertEquals(1, service.available(2));
    }

    @Test public void privateQrOnlyAvailableToWinningMember() throws Exception {
        upload(2, "ticket-1", image);
        jdbc.update("INSERT INTO biz_reward VALUES(7,20,'ticket','pending',NULL)");
        jdbc.update("UPDATE biz_external_ticket SET reward_id=7,status='issued'");
        String qr = String.valueOf(service.memberQr(20, 7).get("qrCode"));
        assertArrayEquals(image, java.util.Base64.getDecoder().decode(qr.substring(qr.indexOf(',') + 1)));
        try { service.memberQr(21, 7); fail(); } catch (MiniBusinessException expected) { assertEquals(404, expected.getCode()); }
        jdbc.update("UPDATE biz_reward SET expires_at=?", new Timestamp(1));
        try { service.memberQr(20, 7); fail(); } catch (MiniBusinessException expected) { assertEquals(404, expected.getCode()); }
    }

    @Test public void issuedAndReservedTicketsCannotBeDisabledOrRecycled() throws Exception {
        upload(2, "ticket-1", image); Object id = service.pick(2).get("ticket_id");
        service.reserve(id, 10);
        try { service.disable(((Number) id).longValue()); fail(); } catch (MiniBusinessException expected) { assertEquals(409, expected.getCode()); }
        jdbc.update("UPDATE biz_external_ticket SET status='issued',reward_id=7");
        service.release(10); assertEquals(0, service.available(2));
    }

    @Test public void badImagesExpiredTicketsAndWrongPrizeTypesAreRejected() throws Exception {
        try { upload(2, "bad", "not an image".getBytes()); fail(); } catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
        try { upload(3, "wrong", image); fail(); } catch (MiniBusinessException expected) { assertEquals(409, expected.getCode()); }
        try { service.upload(2, "vendor", "old", "2000-01-01 00:00:00", file(image), "admin"); fail(); }
        catch (MiniBusinessException expected) { assertEquals(400, expected.getCode()); }
    }

    @Test public void expiredCodeReturnsOnlyUnissuedStillValidTickets() throws Exception {
        upload(2, "ticket-1", image);
        Object id = service.pick(2).get("ticket_id"); service.reserve(id, 10);
        jdbc.execute("CREATE TABLE biz_lottery_code(code_id BIGINT PRIMARY KEY,activity_id BIGINT,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE biz_lottery_code_outcome(code_id BIGINT PRIMARY KEY,status VARCHAR(16),expires_at TIMESTAMP,result_type VARCHAR(16),prize_reserved INT,coupon_reserved INT)");
        jdbc.update("INSERT INTO biz_lottery_code VALUES(10,1,'unused')");
        jdbc.update("INSERT INTO biz_lottery_code_outcome VALUES(10,'reserved',?,'ticket',0,0)", new Timestamp(1));
        LotteryPreallocationService lottery = new LotteryPreallocationService(jdbc, new com.fasterxml.jackson.databind.ObjectMapper());
        lottery.expire(10); lottery.expire(10);
        assertEquals(1, service.available(2));
        assertEquals("expired", jdbc.queryForObject("SELECT status FROM biz_lottery_code_outcome WHERE code_id=10", String.class));
        assertEquals("disabled", jdbc.queryForObject("SELECT status FROM biz_lottery_code WHERE code_id=10", String.class));
        jdbc.update("UPDATE biz_external_ticket SET status='reserved',code_id=11,valid_until=?", new Timestamp(1));
        service.release(11); assertEquals(0, service.available(2));
        assertEquals("expired", jdbc.queryForObject("SELECT status FROM biz_external_ticket", String.class));
    }
}
