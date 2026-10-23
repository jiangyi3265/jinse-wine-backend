package com.ruoyi.web.controller.business;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExternalTicketService {
    private final JdbcTemplate jdbc;

    public ExternalTicketService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void upload(long prizeId, String provider, String externalNo, String validUntil,
            MultipartFile file, String operator) throws Exception {
        provider = required(provider, "请输入售票方名称");
        externalNo = required(externalNo, "请输入售票方票号");
        Timestamp expiry;
        try { expiry = Timestamp.valueOf(LocalDateTime.parse(validUntil.replace(' ', 'T'))); }
        catch (Exception e) { throw new MiniBusinessException(400, "请选择门票有效期"); }
        if (expiry.getTime() <= System.currentTimeMillis()) throw new MiniBusinessException(400, "门票有效期必须晚于当前时间");
        if (file == null || file.isEmpty()) throw new MiniBusinessException(400, "请上传门票原始二维码图片");
        if (file.getSize() > 2 * 1024 * 1024) throw new MiniBusinessException(400, "门票二维码图片不能超过2MB");
        byte[] image = file.getBytes();
        String mime = validateImage(image);
        String digest = hex(MessageDigest.getInstance("SHA-256").digest(image));
        Map<String, Object> prize = first("SELECT activity_id FROM biz_lottery_prize WHERE prize_id=?", prizeId);
        if (prize == null) throw new MiniBusinessException(404, "奖项不存在");
        // Use the same lock order as generation and drawing.
        first("SELECT activity_id FROM biz_lottery_activity WHERE activity_id=? FOR UPDATE", prize.get("activity_id"));
        prize = first("SELECT prize_type FROM biz_lottery_prize WHERE prize_id=? FOR UPDATE", prizeId);
        if (prize == null || !"ticket".equals(prize.get("prize_type")))
            throw new MiniBusinessException(409, "只能向门票奖项录入门票");
        try {
            jdbc.update("INSERT INTO biz_external_ticket(prize_id,provider_name,external_no,qr_image,qr_mime,qr_sha256,valid_until,create_by) VALUES(?,?,?,?,?,?,?,?)",
                    prizeId, provider, externalNo, image, mime, digest, expiry, operator);
        } catch (DuplicateKeyException e) {
            throw new MiniBusinessException(409, "该票号或二维码图片已录入，请勿重复添加");
        }
    }

    public Map<String, Object> list(long prizeId, String status, int pageNum, int pageSize) {
        if (!java.util.Arrays.asList("", "available", "reserved", "issued", "disabled", "expired").contains(status))
            throw new MiniBusinessException(400, "门票状态无效");
        int size = Math.max(1, Math.min(100, pageSize)), page = Math.max(1, pageNum);
        String derived = "CASE WHEN t.status IN ('available','disabled','reserved') AND t.valid_until<=NOW() THEN 'expired' ELSE t.status END";
        String filter = status.isEmpty() ? "" : " AND " + derived + "=?";
        Object[] args = status.isEmpty() ? new Object[] {prizeId} : new Object[] {prizeId, status};
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM biz_external_ticket t WHERE t.prize_id=?" + filter, Long.class, args);
        Object[] paging = java.util.Arrays.copyOf(args, args.length + 2);
        paging[args.length] = size; paging[args.length + 1] = (long) (page - 1) * size;
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT t.ticket_id AS ticketId,t.provider_name AS providerName,t.external_no AS externalNo,"
                + "t.valid_until AS validUntil," + derived + " AS status,t.create_time AS createTime,t.issued_at AS issuedAt,"
                + "m.member_no AS memberNo,m.nickname AS memberName FROM biz_external_ticket t LEFT JOIN biz_reward w ON w.reward_id=t.reward_id "
                + "LEFT JOIN biz_member m ON m.member_id=w.member_id WHERE t.prize_id=?" + filter + " ORDER BY t.ticket_id DESC LIMIT ? OFFSET ?", paging);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows); result.put("total", total); return result;
    }

    @Transactional
    public void disable(long ticketId) {
        Map<String, Object> identity = first("SELECT p.activity_id,p.prize_id FROM biz_external_ticket t JOIN biz_lottery_prize p ON p.prize_id=t.prize_id WHERE t.ticket_id=?", ticketId);
        if (identity == null) throw new MiniBusinessException(404, "门票不存在");
        first("SELECT activity_id FROM biz_lottery_activity WHERE activity_id=? FOR UPDATE", identity.get("activity_id"));
        first("SELECT prize_id FROM biz_lottery_prize WHERE prize_id=? FOR UPDATE", identity.get("prize_id"));
        if (jdbc.update("UPDATE biz_external_ticket SET status='disabled' WHERE ticket_id=? AND status='available'", ticketId) != 1)
            throw new MiniBusinessException(409, "仅未分配门票可以停用，已预留或发放的门票不能撤回");
    }

    public long available(long prizeId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM biz_external_ticket WHERE prize_id=? AND status='available' AND valid_until>NOW()", Long.class, prizeId);
    }

    public Map<String, Object> pick(Object prizeId) {
        return first("SELECT ticket_id,valid_until FROM biz_external_ticket WHERE prize_id=? AND status='available' AND valid_until>NOW() ORDER BY valid_until,ticket_id LIMIT 1 FOR UPDATE", prizeId);
    }

    public void reserve(Object ticketId, Object codeId) {
        if (jdbc.update("UPDATE biz_external_ticket SET status='reserved',code_id=? WHERE ticket_id=? AND status='available' AND valid_until>NOW()", codeId, ticketId) != 1)
            throw new MiniBusinessException(409, "门票已分配或过期，请重试生成");
    }

    public void issue(Object ticketId, Object codeId, long rewardId, boolean preallocated) {
        String condition = preallocated ? "status='reserved' AND code_id=?" : "status='available' AND code_id IS NULL";
        Object[] args = preallocated ? new Object[] {rewardId, codeId, ticketId, codeId} : new Object[] {rewardId, codeId, ticketId};
        if (jdbc.update("UPDATE biz_external_ticket SET status='issued',reward_id=?,code_id=?,issued_at=NOW() WHERE ticket_id=? AND valid_until>NOW() AND " + condition, args) != 1)
            throw new MiniBusinessException(409, "对应门票已过期或已分配，请联系运营人员");
        jdbc.update("UPDATE biz_reward w JOIN biz_external_ticket t ON t.reward_id=w.reward_id "
                + "SET w.expires_at=CASE WHEN w.expires_at IS NULL OR w.expires_at>t.valid_until THEN t.valid_until ELSE w.expires_at END WHERE w.reward_id=?", rewardId);
    }

    public void release(Object codeId) {
        jdbc.update("UPDATE biz_external_ticket SET status=CASE WHEN valid_until>NOW() THEN 'available' ELSE 'expired' END,code_id=NULL "
                + "WHERE code_id=? AND status='reserved' AND reward_id IS NULL", codeId);
    }

    public Map<String, Object> metadata(Object rewardId) {
        return first("SELECT provider_name AS providerName,external_no AS ticketNo,valid_until AS ticketValidUntil FROM biz_external_ticket WHERE reward_id=? AND status='issued'", rewardId);
    }

    public Map<String, Object> memberQr(long memberId, long rewardId) {
        Map<String, Object> ticket = first("SELECT t.qr_image,t.qr_mime,t.external_no,t.provider_name,t.valid_until FROM biz_external_ticket t "
                + "JOIN biz_reward w ON w.reward_id=t.reward_id WHERE w.member_id=? AND w.reward_id=? AND w.reward_type='ticket' "
                + "AND w.status='pending' AND t.status='issued' AND t.valid_until>NOW() AND (w.expires_at IS NULL OR w.expires_at>NOW())", memberId, rewardId);
        if (ticket == null) throw new MiniBusinessException(404, "门票不存在、已过期或无权查看");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qrCode", "data:" + ticket.get("qr_mime") + ";base64," + Base64.getEncoder().encodeToString((byte[]) ticket.get("qr_image")));
        result.put("ticketNo", ticket.get("external_no")); result.put("providerName", ticket.get("provider_name"));
        result.put("validUntil", ticket.get("valid_until")); return result;
    }

    public Map<String, Object> adminImage(long ticketId) {
        Map<String, Object> row = first("SELECT qr_image,qr_mime FROM biz_external_ticket WHERE ticket_id=?", ticketId);
        if (row == null) throw new MiniBusinessException(404, "门票不存在");
        return row;
    }

    private static String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new MiniBusinessException(400, message);
        if (value.trim().length() > 128) throw new MiniBusinessException(400, "售票方名称和票号不能超过128个字符");
        return value.trim();
    }

    static String validateImage(byte[] image) throws Exception {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new MiniBusinessException(400, "请选择有效的PNG或JPG二维码图片");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!"png".equals(format) && !"jpeg".equals(format)) throw new MiniBusinessException(400, "门票二维码仅支持PNG或JPG");
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 64 || height < 64 || width > 4096 || height > 4096)
                    throw new MiniBusinessException(400, "二维码图片尺寸须为64至4096像素");
                if (reader.read(0) == null) throw new MiniBusinessException(400, "二维码图片无法读取");
                return "png".equals(format) ? "image/png" : "image/jpeg";
            } finally { reader.dispose(); }
        } catch (java.io.IOException e) {
            throw new MiniBusinessException(400, "二维码图片已损坏，请上传有效的PNG或JPG图片");
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte b : bytes) text.append(String.format("%02x", b & 255));
        return text.toString();
    }

    private Map<String, Object> first(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
