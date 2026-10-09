package com.ruoyi.web.controller.business;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;

/** Imports only non-identifying fields from WeChat Shop after-sale orders. */
@Service
public class WechatShopAftersaleSyncService
{
    private static final long MAX_RANGE_SECONDS = 24L * 60 * 60;
    private static final long OVERLAP_SECONDS = 120L;

    private final JdbcTemplate jdbc;
    private final WechatShopApiService api;

    public WechatShopAftersaleSyncService(JdbcTemplate jdbc, WechatShopApiService api)
    {
        this.jdbc = jdbc;
        this.api = api;
    }

    @Transactional
    public Map<String, Object> syncOnePage()
    {
        if (!enabled()) throw new MiniBusinessException(409, "请先开启微信小店数据同步");
        String shopId = config("wechat.video.shopId");
        if (shopId.isEmpty()) throw new MiniBusinessException(409, "请先配置微信小店 ID 并完成授权校验");
        api.checkShop(shopId);

        Map<String, Object> state = jdbc.queryForMap("SELECT completed_through,window_start,window_end,next_key FROM biz_video_aftersale_sync_state WHERE sync_key='aftersales' FOR UPDATE");
        long now = Instant.now().getEpochSecond();
        long completedThrough = number(state.get("completed_through"));
        long start = number(state.get("window_start"));
        long end = number(state.get("window_end"));
        String nextKey = text(state.get("next_key"));
        if (end <= 0)
        {
            start = completedThrough > 0 ? Math.max(0, completedThrough - OVERLAP_SECONDS) : Math.max(0, now - MAX_RANGE_SECONDS);
            end = Math.min(now, start + MAX_RANGE_SECONDS);
            nextKey = "";
            jdbc.update("UPDATE biz_video_aftersale_sync_state SET window_start=?,window_end=?,next_key='' WHERE sync_key='aftersales'", start, end);
        }

        JsonNode list = api.listAftersales(start, end, nextKey);
        JsonNode ids = list.path("after_sale_order_id_list");
        if (!ids.isArray()) throw new MiniBusinessException(502, "微信小店售后列表没有返回售后单号列表");
        int imported = 0;
        for (JsonNode id : ids)
        {
            String aftersaleId = id.asText("");
            if (aftersaleId.isEmpty()) continue;
            JsonNode detail = api.getAftersale(aftersaleId).path("after_sale_order");
            if (detail.isMissingNode() || detail.isNull()) throw new MiniBusinessException(502, "微信小店售后详情为空，售后单号=" + aftersaleId);
            save(aftersaleId, detail);
            imported++;
        }

        boolean hasMore = list.path("has_more").asBoolean(false);
        String responseNextKey = text(list.path("next_key").asText(""));
        if (hasMore && responseNextKey.isEmpty()) throw new MiniBusinessException(502, "微信小店售后分页未返回 next_key");
        if (hasMore)
        {
            jdbc.update("UPDATE biz_video_aftersale_sync_state SET next_key=? WHERE sync_key='aftersales'", responseNextKey);
        }
        else
        {
            jdbc.update("UPDATE biz_video_aftersale_sync_state SET completed_through=?,window_start=0,window_end=0,next_key='' WHERE sync_key='aftersales'", end);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("hasMore", hasMore);
        result.put("message", hasMore ? "本页售后已同步，剩余记录将继续分页处理" : "微信小店售后增量同步完成");
        return result;
    }

    public Map<String, Object> list(int pageNum, int pageSize)
    {
        int size = Math.max(1, Math.min(pageSize, 200));
        int page = Math.max(1, Math.min(pageNum, 100000));
        long offset = (long) (page - 1) * size;
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT a.external_aftersale_id AS externalAftersaleId,a.external_order_id AS externalOrderId,a.external_status AS externalStatus,a.aftersale_type AS aftersaleType,a.refund_amount_cent AS refundAmountCent,a.created_at AS createdAt,a.updated_at AS updatedAt,a.last_synced_at AS lastSyncedAt,COALESCE(l.ledger_status,'pending') AS refundLedgerStatus,l.confirmed_at AS refundConfirmedAt,l.last_error AS refundLedgerError FROM biz_video_aftersale a LEFT JOIN biz_video_refund_ledger l ON l.external_aftersale_id=a.external_aftersale_id ORDER BY a.updated_at DESC,a.aftersale_id DESC LIMIT ? OFFSET ?", size, offset);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM biz_video_aftersale", Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", page);
        result.put("pageSize", size);
        return result;
    }

    public void accept(String aftersaleId, String returnAddressId)
    {
        requireKnown(aftersaleId);
        api.acceptAftersale(aftersaleId, returnAddressId);
        saveRemote(aftersaleId);
    }

    public void reject(String aftersaleId, int reasonType, String reason)
    {
        requireKnown(aftersaleId);
        if (reasonType <= 0) throw new MiniBusinessException(400, "请选择微信小店提供的拒绝原因");
        api.rejectAftersale(aftersaleId, reasonType, reason);
        saveRemote(aftersaleId);
    }

    private void saveRemote(String aftersaleId)
    {
        JsonNode detail = api.getAftersale(aftersaleId).path("after_sale_order");
        if (detail.isMissingNode() || detail.isNull()) throw new MiniBusinessException(502, "操作已提交，但读取售后状态失败；请稍后同步核对");
        save(aftersaleId, detail);
    }

    private void requireKnown(String aftersaleId)
    {
        if (aftersaleId == null || aftersaleId.trim().isEmpty()) throw new MiniBusinessException(400, "售后单号无效");
        Long exists = jdbc.queryForObject("SELECT COUNT(*) FROM biz_video_aftersale WHERE external_aftersale_id=?", Long.class, aftersaleId);
        if (exists == null || exists == 0) throw new MiniBusinessException(404, "售后单不存在，请先同步微信小店售后记录");
    }

    private void save(String aftersaleId, JsonNode detail)
    {
        JsonNode refund = detail.path("refund_info");
        jdbc.update("INSERT INTO biz_video_aftersale(external_aftersale_id,external_order_id,external_status,aftersale_type,refund_amount_cent,created_at,updated_at,last_synced_at) VALUES(?,?,?,?,?,?,?,NOW()) "
                        + "ON DUPLICATE KEY UPDATE external_order_id=VALUES(external_order_id),external_status=VALUES(external_status),aftersale_type=VALUES(aftersale_type),refund_amount_cent=VALUES(refund_amount_cent),created_at=VALUES(created_at),updated_at=VALUES(updated_at),last_synced_at=NOW()",
                aftersaleId, detail.path("order_id").asText(""), detail.path("status").asText(""), detail.path("type").asText(""),
                refund.path("amount").asLong(0), timestamp(detail.path("create_time").asLong(0)), timestamp(detail.path("update_time").asLong(0)));
        String externalOrderId = detail.path("order_id").asText("");
        String externalStatus = detail.path("status").asText("");
        long amountCent = refund.path("amount").asLong(0);
        String ledgerStatus = ledgerStatus(externalStatus);
        jdbc.update("INSERT INTO biz_video_refund_ledger(external_aftersale_id,external_order_id,amount_cent,external_status,ledger_status,confirmed_at,last_error) VALUES(?,?,?,?,?,IF(?='confirmed',NOW(),NULL),'') "
                        + "ON DUPLICATE KEY UPDATE external_order_id=VALUES(external_order_id),amount_cent=VALUES(amount_cent),external_status=VALUES(external_status),ledger_status=IF(ledger_status='confirmed' AND VALUES(ledger_status)='pending','confirmed',VALUES(ledger_status)),confirmed_at=IF(VALUES(ledger_status)='confirmed' AND confirmed_at IS NULL,NOW(),confirmed_at),last_error=IF(VALUES(ledger_status)='failed',VALUES(last_error),last_error)",
                aftersaleId, externalOrderId, amountCent, externalStatus, ledgerStatus, ledgerStatus);
    }

    private static String ledgerStatus(String externalStatus)
    {
        String status = externalStatus == null ? "" : externalStatus.toUpperCase(java.util.Locale.ROOT);
        if (status.contains("REFUND_SUCCESS") || status.contains("RETURN_SUCCESS") || status.endsWith("_SUCCESS")) return "confirmed";
        if (status.contains("FAIL") || status.contains("REJECT") || status.contains("CLOSE")) return "failed";
        return "pending";
    }

    private boolean enabled() { return "true".equalsIgnoreCase(config("wechat.video.syncEnabled")); }

    private String config(String key)
    {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT config_value FROM biz_channel_config WHERE config_key=?", key);
        return rows.isEmpty() ? "" : text(rows.get(0).get("config_value"));
    }

    private static java.sql.Timestamp timestamp(long epochSeconds)
    {
        return epochSeconds <= 0 ? null : new java.sql.Timestamp(epochSeconds * 1000L);
    }

    private static long number(Object value)
    {
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception e) { return 0; }
    }

    private static String text(Object value)
    {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
