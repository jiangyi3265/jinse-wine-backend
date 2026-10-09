package com.ruoyi.web.controller.business;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the short-video product placement workflow auditable.
 * WeChat does not expose a public API that can prove a product was attached to
 * an arbitrary video feed, so the service records the submission manifest and
 * requires an explicit platform confirmation before marking it verified.
 */
@Service
public class VideoFeedMountService
{
    private final JdbcTemplate jdbc;

    public VideoFeedMountService(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    public Map<String, Object> list(Long productId, int pageNum, int pageSize)
    {
        int page = Math.max(1, Math.min(pageNum, 100000));
        int size = Math.max(1, Math.min(pageSize, 200));
        long offset = (long) (page - 1) * size;
        String where = productId == null ? "" : " WHERE m.product_id=" + productId;
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT m.mount_id AS mountId,m.product_id AS productId,p.product_name AS productName,m.external_product_id AS externalProductId,m.feed_id AS feedId,m.nonce_id AS nonceId,m.mount_title AS mountTitle,m.status,m.last_error AS lastError,m.request_id AS requestId,m.submitted_at AS submittedAt,m.verified_at AS verifiedAt,m.create_time AS createTime,m.update_time AS updateTime FROM biz_video_feed_mount m LEFT JOIN biz_product p ON p.product_id=m.product_id" + where + " ORDER BY m.update_time DESC,m.mount_id DESC LIMIT ? OFFSET ?", size, offset);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM biz_video_feed_mount m" + where, Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows); result.put("total", total == null ? 0 : total); result.put("pageNum", page); result.put("pageSize", size);
        return result;
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body)
    {
        long productId = number(body == null ? null : body.get("productId"));
        Map<String, Object> product = productId <= 0 ? null : first("SELECT product_name FROM biz_product WHERE product_id=? FOR UPDATE", productId);
        if (product == null) throw new MiniBusinessException(404, "本地商品不存在");
        String externalProductId = required(body.get("externalProductId"), "微信小店商品 ID", 128);
        String feedId = required(body.get("feedId"), "短视频 Feed ID", 128);
        String nonceId = required(body.get("nonceId"), "短视频 Nonce ID", 128);
        String title = limit(text(body.get("mountTitle")), 128);
        if (title.isEmpty()) title = text(product.get("product_name"));
        Map<String, Object> current = first("SELECT mount_id,status FROM biz_video_feed_mount WHERE product_id=? AND feed_id=? AND nonce_id=? FOR UPDATE", productId, feedId, nonceId);
        if (current == null)
        {
            jdbc.update("INSERT INTO biz_video_feed_mount(product_id,external_product_id,feed_id,nonce_id,mount_title,status,last_error,request_id) VALUES(?,?,?,?,?,'draft','','')", productId, externalProductId, feedId, nonceId, title);
        }
        else
        {
            if ("submitting".equals(text(current.get("status")))) throw new MiniBusinessException(409, "该挂载记录正在提交，请先核对状态");
            jdbc.update("UPDATE biz_video_feed_mount SET external_product_id=?,mount_title=?,status='draft',last_error='',request_id='' WHERE mount_id=?", externalProductId, title, current.get("mount_id"));
        }
        return first("SELECT mount_id AS mountId,product_id AS productId,external_product_id AS externalProductId,feed_id AS feedId,nonce_id AS nonceId,mount_title AS mountTitle,status,last_error AS lastError,request_id AS requestId,submitted_at AS submittedAt,verified_at AS verifiedAt FROM biz_video_feed_mount WHERE product_id=? AND feed_id=? AND nonce_id=?", productId, feedId, nonceId);
    }

    @Transactional
    public Map<String, Object> submit(long mountId)
    {
        Map<String, Object> row = first("SELECT mount_id,product_id,external_product_id,feed_id,nonce_id,status FROM biz_video_feed_mount WHERE mount_id=? FOR UPDATE", mountId);
        if (row == null) throw new MiniBusinessException(404, "短视频挂载记录不存在");
        if (text(row.get("external_product_id")).isEmpty()) throw new MiniBusinessException(409, "请先填写微信小店商品 ID");
        String status = text(row.get("status"));
        if ("submitting".equals(status)) throw new MiniBusinessException(409, "该挂载记录正在提交");
        if ("verified".equals(status)) throw new MiniBusinessException(409, "该挂载已确认，无需重复提交");
        String requestId = UUID.randomUUID().toString().replace("-", "");
        jdbc.update("UPDATE biz_video_feed_mount SET status='submitting',request_id=?,submitted_at=NOW(),last_error='' WHERE mount_id=?", requestId, mountId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mountId", mountId); result.put("requestId", requestId); result.put("status", "submitting");
        result.put("feedId", row.get("feed_id")); result.put("nonceId", row.get("nonce_id")); result.put("externalProductId", row.get("external_product_id"));
        result.put("message", "已生成短视频挂载清单，请在视频号助手提交商品并回到此处确认平台结果");
        result.put("requiresPlatformConfirmation", true);
        return result;
    }

    @Transactional
    public Map<String, Object> verify(long mountId, Map<String, Object> body)
    {
        Map<String, Object> row = first("SELECT mount_id,status FROM biz_video_feed_mount WHERE mount_id=? FOR UPDATE", mountId);
        if (row == null) throw new MiniBusinessException(404, "短视频挂载记录不存在");
        boolean confirmed = Boolean.TRUE.equals(body == null ? null : body.get("platformConfirmed"));
        String next = text(body == null ? null : body.get("status"));
        if ("verified".equals(next) && !confirmed) throw new MiniBusinessException(400, "请确认已在视频号助手看到商品挂载成功");
        if (!("verified".equals(next) || "failed".equals(next))) throw new MiniBusinessException(400, "挂载结果只能确认成功或失败");
        String error = limit(text(body == null ? null : body.get("lastError")), 255);
        jdbc.update("UPDATE biz_video_feed_mount SET status=?,last_error=?,verified_at=IF(?='verified',NOW(),verified_at) WHERE mount_id=?", next, error, next, mountId);
        return first("SELECT mount_id AS mountId,product_id AS productId,external_product_id AS externalProductId,feed_id AS feedId,nonce_id AS nonceId,mount_title AS mountTitle,status,last_error AS lastError,request_id AS requestId,submitted_at AS submittedAt,verified_at AS verifiedAt FROM biz_video_feed_mount WHERE mount_id=?", mountId);
    }

    private Map<String, Object> first(String sql, Object... args) { List<Map<String, Object>> rows = jdbc.queryForList(sql, args); return rows.isEmpty() ? null : rows.get(0); }
    private static String required(Object value, String label, int max) { String result = text(value); if (result.isEmpty()) throw new MiniBusinessException(400, label + "不能为空"); if (result.length() > max) throw new MiniBusinessException(400, label + "不能超过" + max + "个字符"); return result; }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private static long number(Object value) { try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return 0; } }
}
