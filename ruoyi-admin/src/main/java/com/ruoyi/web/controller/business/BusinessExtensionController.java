package com.ruoyi.web.controller.business;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;

/** 抽奖、积分、分销、社群、微信渠道和财务导出管理接口。 */
@RestController
@RequestMapping("/business/extension")
public class BusinessExtensionController extends BaseController
{
    private static final Set<String> REPORTS = new HashSet<>(Arrays.asList("orders", "refunds", "returns", "commissions", "settlements", "lottery", "redemptions", "rankingRewards", "videoRefunds"));
    private static final Set<String> CHANNEL_KEYS = new HashSet<>(Arrays.asList(
        "wechat.mini.socialTemplateId", "wechat.mini.socialTitleField", "wechat.mini.socialContentField",
        "wechat.customer.serviceQr",
        "wechat.video.finderUserName", "wechat.video.shopId", "wechat.video.syncEnabled",
        "wechat.video.liveFeedId", "wechat.video.liveNonceId", "wechat.video.feedId", "wechat.video.nonceId",
        "wechat.video.returnAddressId",
        "wechat.work.groupChatIds", "wechat.work.groupQr", "wechat.work.groupQrConfigId",
        "wechat.work.groupQrCreatedAt", "wechat.work.groupQrExpiresAt", "wechat.work.groupQrPreviousConfigId",
        "wechat.work.groupQrRevocationStatus"));
    private static final Set<String> CHANNEL_MANAGED_KEYS = new HashSet<>(Arrays.asList(
        "wechat.work.groupChatIds", "wechat.work.groupQr", "wechat.work.groupQrConfigId",
        "wechat.work.groupQrCreatedAt", "wechat.work.groupQrExpiresAt", "wechat.work.groupQrPreviousConfigId",
        "wechat.work.groupQrRevocationStatus"));
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter CHANNEL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final LotteryPreallocationService lotteryPreallocation;
    private final ObjectMapper json;
    private final PointExpiryService pointExpiry;
    private final WechatMiniCodeService miniCode;
    private final WechatPayService wechatPay;
    private final PendingPaymentReconciliationJob paymentReconciliation;
    private final OrderRefundService orderRefunds;
    private final WechatSocialDeliveryService socialDelivery;
    private final WechatWorkGroupMessageService workGroupMessage;
    private final WechatWorkGroupQrService workGroupQr;
    private final WechatShopApiService wechatShopApi;
    private final WechatShopStockSyncService wechatShopStockSync;
    private final WechatShopOrderSyncService wechatShopOrderSync;
    private final WechatShopAftersaleSyncService wechatShopAftersaleSync;
    private final WechatShopProductService wechatShopProduct;
    private final VideoFeedMountService videoFeedMount;
    private final ProductionVerificationService productionVerification;

    @Value("${mini.wechat.app-id:}") private String miniAppId;
    @Value("${mini.wechat.app-secret:}") private String miniAppSecret;
    @Value("${mini.wechat.code-env-version:release}") private String miniCodeEnvVersion;
    @Value("${mini.wechat.code-check-path:true}") private boolean miniCodeCheckPath;
    /** Production default keeps QR generation usable when the external env file was not reloaded. */
    @Value("${mini.wechat.lottery-link-prefix:https://jiuyekl.oksja.cn/qr/lottery/}") private String lotteryLinkPrefix;
    @Value("${mini.wechat.local-login-enabled:false}") private boolean localLoginEnabled;
    @Value("${mini.wechat.pay-enabled:false}") private boolean wechatPayEnabled;
    @Value("${mini.wechat.merchant-id:}") private String wechatMerchantId;
    @Value("${mini.wechat.merchant-serial:}") private String wechatMerchantSerial;
    @Value("${mini.wechat.private-key:}") private String wechatPrivateKey;
    @Value("${mini.wechat.api-v3-key:}") private String wechatApiV3Key;
    @Value("${mini.wechat.pay-notify-url:}") private String wechatPayNotifyUrl;
    @Value("${mini.wechat.refund-notify-url:}") private String wechatRefundNotifyUrl;
    @Value("${mini.wechat.platform-cert:}") private String wechatPlatformCert;
    @Value("${mini.wechat.transfer-enabled:false}") private boolean transferEnabled;
    @Value("${mini.wechat.transfer-notify-url:}") private String transferNotifyUrl;
    @Value("${mini.wechat.transfer-scene-id:}") private String transferSceneId;
    @Value("${mini.wechat.work.corp-id:}") private String workCorpId;
    @Value("${mini.wechat.work.secret:}") private String workSecret;
    @Value("${token.secret:}") private String jwtSecret;
    @Value("${app.environment:production}") private String appEnvironment;

    public BusinessExtensionController(JdbcTemplate jdbc, ObjectMapper json, PointExpiryService pointExpiry, WechatMiniCodeService miniCode, WechatPayService wechatPay, PendingPaymentReconciliationJob paymentReconciliation, OrderRefundService orderRefunds, WechatSocialDeliveryService socialDelivery, WechatWorkGroupMessageService workGroupMessage, WechatWorkGroupQrService workGroupQr, WechatShopApiService wechatShopApi, WechatShopStockSyncService wechatShopStockSync, WechatShopOrderSyncService wechatShopOrderSync, WechatShopAftersaleSyncService wechatShopAftersaleSync, WechatShopProductService wechatShopProduct, VideoFeedMountService videoFeedMount, ProductionVerificationService productionVerification, LotteryPreallocationService lotteryPreallocation)
    {
        this.jdbc = jdbc;
        this.lotteryPreallocation = lotteryPreallocation;
        this.json = json;
        this.pointExpiry = pointExpiry;
        this.miniCode = miniCode;
        this.wechatPay = wechatPay;
        this.paymentReconciliation = paymentReconciliation;
        this.orderRefunds = orderRefunds;
        this.socialDelivery = socialDelivery;
        this.workGroupMessage = workGroupMessage;
        this.workGroupQr = workGroupQr;
        this.wechatShopApi = wechatShopApi;
        this.wechatShopStockSync = wechatShopStockSync;
        this.wechatShopOrderSync = wechatShopOrderSync;
        this.wechatShopAftersaleSync = wechatShopAftersaleSync;
        this.wechatShopProduct = wechatShopProduct;
        this.videoFeedMount = videoFeedMount;
        this.productionVerification = productionVerification;
    }

    @PostMapping("/lottery/codes")
    @Log(title = "抽奖码生成", businessType = BusinessType.INSERT)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:lottery:generate')")
    public AjaxResult generateCodes(@RequestBody Map<String, Object> body)
    {
        String activityCode = text(body.get("activityCode"));
        int count = integer(body.get("count"), 1, 100);
        String prefix = text(body.get("prefix"));
        if (prefix.isEmpty()) prefix = "JS";
        if (!prefix.matches("[A-Za-z0-9_-]{1,2}")) return error("活动码前缀仅支持1至2位英文字母、数字或-_符号");
        String linkPrefix = checkedLotteryLinkPrefix(lotteryLinkPrefix);
        String rawCount = text(body.get("count"));
        if (!rawCount.matches("[0-9]{1,3}") || Integer.parseInt(rawCount) < 1 || Integer.parseInt(rawCount) > 100)
            return error("生成数量必须为1至100的整数");
        return success(lotteryPreallocation.generate(activityCode, count, prefix, text(body.get("requestId")),
                linkPrefix, SecurityUtils.getUsername()));
    }

    @GetMapping("/lottery/code-batches")
    @PreAuthorize("@ss.hasPermi('business:lottery:generate')")
    public AjaxResult lotteryCodeBatches(@RequestParam String activityCode,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        return success(lotteryPreallocation.batches(activityCode, pageNum, pageSize));
    }

    @GetMapping("/lottery/code-batches/{batchId}")
    @PreAuthorize("@ss.hasPermi('business:lottery:generate')")
    public AjaxResult lotteryCodeBatch(@PathVariable String batchId) {
        return success(lotteryPreallocation.batch(batchId, checkedLotteryLinkPrefix(lotteryLinkPrefix)));
    }

    static String checkedLotteryLinkPrefix(String value)
    {
        String prefix = value == null ? "" : value.trim();
        if (prefix.isEmpty()) return "";
        try
        {
            URI uri = URI.create(prefix);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getPort() == -1
                    && uri.getUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && "/qr/lottery/".equals(uri.getRawPath())) return prefix;
        }
        catch (IllegalArgumentException ignored) { }
        throw new MiniBusinessException(409, "普通链接二维码前缀必须是 HTTPS 域名下的 /qr/lottery/ 路径");
    }

    @GetMapping("/lottery/records")
    @PreAuthorize("@ss.hasPermi('business:lottery:list')")
    public TableDataInfo lotteryRecords(@RequestParam Map<String, String> params)
    {
        return page("SELECT r.record_id AS recordId, a.activity_name AS activityName, r.member_id AS memberId, c.draw_code AS drawCode, "
                + "CASE WHEN r.result_type IN ('cash','goods') AND w.reward_name IS NOT NULL THEN w.reward_name ELSE COALESCE(JSON_UNQUOTE(JSON_EXTRACT(o.result_snapshot,'$.prize_name')),p.prize_name) END AS prizeName, r.result_type AS resultType, r.result_value AS resultValue, r.create_time AS createTime "
                + "FROM biz_lottery_record r JOIN biz_lottery_activity a ON a.activity_id=r.activity_id JOIN biz_lottery_code c ON c.code_id=r.code_id "
                + "LEFT JOIN biz_lottery_prize p ON p.prize_id=r.prize_id "
                + "LEFT JOIN biz_lottery_code_outcome o ON o.code_id=r.code_id "
                + "LEFT JOIN biz_mini_idempotency i ON i.member_id=r.member_id AND i.request_id=r.request_id AND i.action_type='draw' "
                + "LEFT JOIN biz_reward w ON w.reward_id=i.reward_id AND w.member_id=r.member_id ORDER BY r.record_id DESC", params);
    }

    @GetMapping("/lottery/activities")
    @PreAuthorize("@ss.hasPermi('business:lottery:list')")
    public AjaxResult lotteryActivities()
    {
        return success(jdbc.queryForList("SELECT activity_id AS activityId, activity_code AS activityCode, activity_name AS activityName, start_time AS startTime, end_time AS endTime, daily_limit AS dailyLimit, total_limit AS totalLimit, used_count AS usedCount, status, rule_json AS ruleJson FROM biz_lottery_activity ORDER BY activity_id DESC"));
    }

    @PutMapping("/lottery/activity")
    @Transactional
    @Log(title = "抽奖活动配置", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:lottery:edit')")
    public AjaxResult updateLotteryActivity(@RequestBody Map<String, Object> body)
    {
        String code = text(body.get("activityCode"));
        if (code.isEmpty()) return error("活动编码不能为空");
        String activityName = text(body.get("activityName"));
        if (activityName.isEmpty() || activityName.length() > 128) return error("活动名称不能为空且最多128字");
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = parseOptionalDateTime(body.get("startTime"), "活动开始时间");
            end = parseOptionalDateTime(body.get("endTime"), "活动结束时间");
        } catch (IllegalArgumentException e) { return AjaxResult.error(400, e.getMessage()); }
        if (start != null && end != null && !start.isBefore(end)) return error("活动结束时间必须晚于开始时间");
        String ruleJson = jsonText(body.get("ruleJson"));
        try {
            com.fasterxml.jackson.databind.JsonNode rules = json.readTree(ruleJson);
            if (rules == null || !rules.isObject()) return error("活动规则必须是 JSON 对象");
            if (rules.has("codeReusable") && !rules.path("codeReusable").isBoolean()) return error("codeReusable 必须是布尔值");
            // 0 is the documented value for no expiry. Positive values set the
            // validity window for newly issued cash and physical rewards.
            if (rules.has("rewardValidityDays") && (!rules.path("rewardValidityDays").canConvertToInt()
                    || rules.path("rewardValidityDays").asInt() < 0 || rules.path("rewardValidityDays").asInt() > 3650))
                return error("rewardValidityDays 必须是0至3650天");
        } catch (Exception e) { return error("活动规则 JSON 格式无效"); }
        Map<String, Object> activity = first("SELECT activity_id,used_count FROM biz_lottery_activity WHERE activity_code=? FOR UPDATE", code);
        if (activity == null) return error("活动不存在");
        long outstanding = longValue(scalar("SELECT COUNT(*) FROM biz_lottery_code_outcome o JOIN biz_lottery_code c ON c.code_id=o.code_id "
                + "WHERE c.activity_id=? AND o.status='reserved' AND (o.expires_at IS NULL OR o.expires_at>NOW()) FOR UPDATE", activity.get("activity_id")));
        int totalLimit = integer(body.get("totalLimit"), -1, 100000000);
        if (totalLimit >= 0 && totalLimit < longValue(activity.get("used_count")) + outstanding)
            return error("总次数不能少于已抽次数与已生成未领取的码之和");
        int changed = jdbc.update("UPDATE biz_lottery_activity SET activity_name=?, start_time=?, end_time=?, daily_limit=?, total_limit=?, status=?, rule_json=? WHERE activity_code=?",
                activityName, body.get("startTime"), body.get("endTime"), integer(body.get("dailyLimit"), 0, 1000000), integer(body.get("totalLimit"), -1, 100000000), status(body.get("status")), ruleJson, code);
        return changed == 0 ? error("活动不存在") : success();
    }

    @GetMapping("/lottery/prizes")
    @PreAuthorize("@ss.hasPermi('business:lottery:list')")
    public AjaxResult lotteryPrizes(@RequestParam(required = false, defaultValue = "") String activityCode)
    {
        if (activityCode == null || activityCode.trim().isEmpty()) return success(Collections.emptyList());
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT p.prize_id AS prizeId, a.activity_code AS activityCode, p.prize_type AS prizeType, p.prize_name AS prizeName, p.probability_bp AS probabilityBp, p.amount_min_cent AS amountMinCent, p.amount_max_cent AS amountMaxCent, p.amount_rules_json AS amountRulesJson, p.points, p.coupon_template_id AS couponTemplateId, p.stock, p.issued_count AS issuedCount, (SELECT COUNT(*) FROM biz_lottery_code_outcome o WHERE o.prize_id=p.prize_id AND o.status='reserved') AS reservedCount, p.image_url AS imageUrl, p.redeem_store_id AS redeemStoreId, s.store_name AS redeemStoreName, p.status FROM biz_lottery_prize p JOIN biz_lottery_activity a ON a.activity_id=p.activity_id LEFT JOIN biz_store s ON s.store_id=p.redeem_store_id WHERE a.activity_code=? ORDER BY p.prize_id", activityCode);
        rows.forEach(row -> row.put("cashAmountRules", parseCashAmountRules(row.remove("amountRulesJson"))));
        rows.forEach(row -> {
            if ("ticket".equals(row.get("prizeType"))) row.put("ticketAvailable", new ExternalTicketService(jdbc).available(longValue(row.get("prizeId"))));
        });
        return success(rows);
    }

    @PostMapping("/lottery/prizes")
    @Log(title = "抽奖奖品", businessType = BusinessType.INSERT)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:lottery:edit')")
    public AjaxResult addLotteryPrize(@RequestBody Map<String, Object> body)
    {
        Map<String, Object> activity = first("SELECT activity_id FROM biz_lottery_activity WHERE activity_code=? FOR UPDATE", text(body.get("activityCode")));
        if (activity == null) return error("活动不存在");
        String type = text(body.get("prizeType")); int probability = integer(body.get("probabilityBp"), 0, 10000); int min = integer(body.get("amountMinCent"), 0, Integer.MAX_VALUE); int max = integer(body.get("amountMaxCent"), 0, Integer.MAX_VALUE); String amountRules = cashAmountRules(body.get("cashAmountRules"));
        if (!Arrays.asList("cash", "goods", "points", "coupon", "ticket", "none").contains(type) || text(body.get("prizeName")).isEmpty()) return error("奖项类型或名称无效");
        if ("ticket".equals(type)) body.put("stock", -1);
        if ("cash".equals(type) && (min < 1 || max < min) && amountRules.isEmpty()) return error("红包金额区间无效，最低为0.01元");
        if ("points".equals(type) && integer(body.get("points"), 0, Integer.MAX_VALUE) <= 0) return error("积分奖项必须大于0");
        if ("coupon".equals(type) && (body.get("couponTemplateId") == null || !activeCouponTemplate(longValue(body.get("couponTemplateId"))))) return error("优惠券奖项必须绑定有效模板");
        Long redeemStoreId;
        try { redeemStoreId = nullableLong(body.get("redeemStoreId")); }
        catch (IllegalArgumentException e) { return AjaxResult.error(400, e.getMessage()); }
        if (redeemStoreId != null && !"goods".equals(type)) return error("只有实物奖项可以指定兑换门店");
        if (redeemStoreId != null && !activeStore(redeemStoreId)) return error("指定兑换门店不存在或已停用");
        if (probabilityTotal(0, longValue(activity.get("activity_id"))) + probability > 10000) return error("奖项概率总和不能超过10000");
        jdbc.update("INSERT INTO biz_lottery_prize(activity_id,prize_type,prize_name,probability_bp,amount_min_cent,amount_max_cent,amount_rules_json,points,coupon_template_id,stock,image_url,redeem_store_id,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", activity.get("activity_id"), type, text(body.get("prizeName")), probability, min, max, amountRules.isEmpty() ? null : amountRules, integer(body.get("points"), 0, Integer.MAX_VALUE), body.get("couponTemplateId") == null ? null : longValue(body.get("couponTemplateId")), integer(body.get("stock"), -1, Integer.MAX_VALUE), text(body.get("imageUrl")), redeemStoreId, status(body.get("status")));
        return success();
    }

    @PutMapping("/lottery/prizes")
    @Log(title = "抽奖奖品", businessType = BusinessType.UPDATE)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:lottery:edit')")
    public AjaxResult editLotteryPrize(@RequestBody Map<String, Object> body)
    {
        long id = longValue(body.get("prizeId")); Map<String, Object> prize = first("SELECT activity_id FROM biz_lottery_prize WHERE prize_id=?", id);
        if (prize == null) return error("奖项不存在");
        jdbc.queryForObject("SELECT activity_id FROM biz_lottery_activity WHERE activity_id=? FOR UPDATE", Long.class, prize.get("activity_id"));
        jdbc.queryForObject("SELECT prize_id FROM biz_lottery_prize WHERE prize_id=? FOR UPDATE", Long.class, id);
        String type = text(body.get("prizeType"));
        int probability = integer(body.get("probabilityBp"), 0, 10000); int min = integer(body.get("amountMinCent"), 0, Integer.MAX_VALUE); int max = integer(body.get("amountMaxCent"), 0, Integer.MAX_VALUE); String amountRules = cashAmountRules(body.get("cashAmountRules"));
        if (!Arrays.asList("cash", "goods", "points", "coupon", "ticket", "none").contains(type) || text(body.get("prizeName")).isEmpty()) return error("奖项类型或名称无效");
        String oldType = jdbc.queryForObject("SELECT prize_type FROM biz_lottery_prize WHERE prize_id=?", String.class, id);
        if ("ticket".equals(oldType) && !"ticket".equals(type)) return error("门票奖项不能改为其他类型，请新增奖项");
        if (!"ticket".equals(oldType) && "ticket".equals(type)) return error("请新增独立门票奖项，已有奖项不能改为门票");
        if ("ticket".equals(type)) body.put("stock", -1);
        if ("cash".equals(type) && (min < 1 || max < min) && amountRules.isEmpty()) return error("红包金额区间无效，最低为0.01元");
        if ("points".equals(type) && integer(body.get("points"), 0, Integer.MAX_VALUE) <= 0) return error("积分奖项必须大于0");
        if ("coupon".equals(type) && (body.get("couponTemplateId") == null || !activeCouponTemplate(longValue(body.get("couponTemplateId"))))) return error("优惠券奖项必须绑定有效模板");
        Long redeemStoreId;
        try { redeemStoreId = nullableLong(body.get("redeemStoreId")); }
        catch (IllegalArgumentException e) { return AjaxResult.error(400, e.getMessage()); }
        if (redeemStoreId != null && !"goods".equals(type)) return error("只有实物奖项可以指定兑换门店");
        if (redeemStoreId != null && !activeStore(redeemStoreId)) return error("指定兑换门店不存在或已停用");
        if (probabilityTotal(id, longValue(prize.get("activity_id"))) + probability > 10000) return error("奖项概率总和不能超过10000");
        jdbc.update("UPDATE biz_lottery_prize SET prize_type=?,prize_name=?,probability_bp=?,amount_min_cent=?,amount_max_cent=?,amount_rules_json=?,points=?,coupon_template_id=?,stock=?,image_url=?,redeem_store_id=?,status=? WHERE prize_id=?", type, text(body.get("prizeName")), probability, min, max, amountRules.isEmpty() ? null : amountRules, integer(body.get("points"), 0, Integer.MAX_VALUE), body.get("couponTemplateId") == null ? null : longValue(body.get("couponTemplateId")), integer(body.get("stock"), -1, Integer.MAX_VALUE), text(body.get("imageUrl")), redeemStoreId, status(body.get("status")), id);
        return success();
    }

    @PostMapping("/lottery/prizes/{id}/disable")
    @Log(title = "抽奖奖品", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:lottery:edit')")
    public AjaxResult disableLotteryPrize(@PathVariable long id)
    {
        return jdbc.update("UPDATE biz_lottery_prize SET status='1' WHERE prize_id=?", id) == 0 ? error("奖项不存在") : success();
    }

    @GetMapping("/products/{productId}/skus")
    @PreAuthorize("@ss.hasPermi('business:product:list')")
    public AjaxResult productSkus(@PathVariable long productId)
    {
        if (longValue(scalar("SELECT COUNT(*) FROM biz_product WHERE product_id=?", productId)) == 0) return error("商品不存在");
        return success(jdbc.queryForList("SELECT sku_id AS skuId,product_id AS productId,sku_code AS skuCode,sku_name AS skuName,price_cent AS priceCent,stock,spec_json AS specJson,status,external_sku_id AS externalSkuId,video_sync_status AS videoSyncStatus,video_last_error AS videoLastError,video_synced_at AS videoSyncedAt FROM biz_product_sku WHERE product_id=? ORDER BY sku_id", productId));
    }

    @PutMapping("/products/{productId}/skus/mappings")
    @Log(title = "微信小店规格映射", businessType = BusinessType.UPDATE)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:product:edit')")
    public AjaxResult saveProductSkuMappings(@PathVariable long productId, @RequestBody Map<String, Object> body)
    {
        if (first("SELECT product_id FROM biz_product WHERE product_id=? FOR UPDATE", productId) == null) return error("商品不存在");
        Object input = body.get("mappings");
        if (!(input instanceof List)) return error("规格映射数据无效");
        Map<String, Object> productMapping = first("SELECT external_id FROM biz_video_product WHERE product_id=? AND channel='video'", productId);
        String mappedProductId = productMapping == null ? "" : text(productMapping.get("external_id"));
        if (mappedProductId.isEmpty()) return error("请先保存微信小店商品映射");
        if (!mappedProductId.equals(text(body.get("externalProductId")))) return error("微信小店商品映射已变化，请刷新后重试");
        Map<Long, String> current = new LinkedHashMap<>();
        for (Map<String, Object> sku : jdbc.queryForList("SELECT sku_id,external_sku_id FROM biz_product_sku WHERE product_id=? FOR UPDATE", productId))
            current.put(longValue(sku.get("sku_id")), text(sku.get("external_sku_id")));
        Map<Long, String> changes = new LinkedHashMap<>();
        for (Object value : (List<?>) input) {
            if (!(value instanceof Map)) return error("规格映射数据无效");
            Map<?, ?> row = (Map<?, ?>) value;
            long skuId;
            try { skuId = longValue(row.get("skuId")); }
            catch (IllegalArgumentException e) { return error("规格 ID 无效"); }
            if (!current.containsKey(skuId)) return error("规格不存在或不属于当前商品，请刷新后重试");
            String externalSkuId = text(row.get("externalSkuId"));
            if (externalSkuId.length() > 128) return error("微信小店 SKU ID 不能超过128个字符");
            if (changes.containsKey(skuId)) return error("规格映射不能重复提交");
            if (!row.containsKey("previousExternalSkuId") || !current.get(skuId).equals(text(row.get("previousExternalSkuId"))))
                return error("规格映射已被其他人修改，请刷新后重试");
            changes.put(skuId, externalSkuId);
        }
        Set<String> externalIds = new HashSet<>();
        for (Map.Entry<Long, String> sku : current.entrySet()) {
            String externalSkuId = changes.getOrDefault(sku.getKey(), sku.getValue());
            if (!externalSkuId.isEmpty() && !externalIds.add(externalSkuId)) return error("同一微信小店规格不能关联多个本地规格");
        }
        for (Map.Entry<Long, String> change : changes.entrySet()) {
            if (change.getValue().equals(current.get(change.getKey()))) continue;
            jdbc.update("UPDATE biz_product_sku SET external_sku_id=?,video_sync_status='pending',video_last_error='',video_retry_at=NULL,video_retry_count=0 WHERE product_id=? AND sku_id=?",
                    change.getValue(), productId, change.getKey());
        }
        return success();
    }

    @PutMapping("/products/{productId}/skus")
    @Log(title = "商品规格库存", businessType = BusinessType.UPDATE)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:product:edit')")
    public AjaxResult saveProductSkus(@PathVariable long productId, @RequestBody Map<String, Object> body)
    {
        if (first("SELECT product_id FROM biz_product WHERE product_id=? FOR UPDATE", productId) == null) return error("商品不存在");
        Object input = body.get("skus");
        if (!(input instanceof List)) return error("规格数据无效");
        Object expectedInput = body.get("expectedSkuIds");
        if (!(expectedInput instanceof List)) return error("请刷新商品规格后重试");
        Set<Long> expectedIds = new HashSet<>();
        try {
            for (Object id : (List<?>) expectedInput) {
                if (!expectedIds.add(longValue(id))) return error("规格版本数据无效");
            }
        } catch (IllegalArgumentException e) { return error("规格版本数据无效"); }
        List<Map<String, Object>> existingRows = jdbc.queryForList(
                "SELECT sku_id,sku_code FROM biz_product_sku WHERE product_id=? FOR UPDATE", productId);
        Set<Long> currentIds = new HashSet<>();
        for (Map<String, Object> existing : existingRows) currentIds.add(longValue(existing.get("sku_id")));
        if (!expectedIds.equals(currentIds)) return error("商品规格已发生变化，请刷新后重试");
        List<Map<String, Object>> rows = new ArrayList<>(); Set<String> codes = new HashSet<>();
        for (Object value : (List<?>) input) {
            if (!(value instanceof Map)) return error("规格数据无效");
            @SuppressWarnings("unchecked") Map<String, Object> row = (Map<String, Object>) value;
            String code, name = text(row.get("skuName"));
            if (row.get("skuId") != null)
            {
                Map<String, Object> saved = first("SELECT sku_code FROM biz_product_sku WHERE sku_id=? AND product_id=?", row.get("skuId"), productId);
                if (saved == null) return error("规格不存在或不属于当前商品，请刷新后重试");
                code = text(saved.get("sku_code"));
            }
            else code = com.ruoyi.common.utils.BusinessCode.next("sku");
            row.put("skuCode", code);
            if (!code.matches("[A-Za-z0-9_-]{1,64}") || name.isEmpty() || name.length() > 128 || !codes.add(code)) return error("规格编码重复或名称无效");
            if (!nonNegativeInt(row.get("priceCent")) || !nonNegativeInt(row.get("stock"))) return error("规格价格和库存必须是非负整数");
            if (row.get("status") != null && !Arrays.asList("0", "1").contains(text(row.get("status")))) return error("规格状态无效");
            Map<String, Object> owner = first("SELECT product_id FROM biz_product_sku WHERE sku_code=?", code);
            if (owner != null && longValue(owner.get("product_id")) != productId) return error("规格编码已被其他商品使用");
            rows.add(row);
        }
        // 同步删除后台已移除的 SKU。已有购物车或订单引用的规格不能物理删除，
        // 置为停用并清零库存，以保留历史快照并阻止继续下单。
        for (Map<String, Object> existing : existingRows) {
            String existingCode = text(existing.get("sku_code"));
            if (codes.contains(existingCode)) continue;
            long references = longValue(scalar(
                    "SELECT (SELECT COUNT(*) FROM biz_cart_item WHERE product_id=? AND sku_code=?) "
                            + "+ (SELECT COUNT(*) FROM biz_order_item WHERE product_id=? AND sku_code=?)",
                    productId, existingCode, productId, existingCode));
            if (references == 0) {
                jdbc.update("DELETE FROM biz_product_sku WHERE sku_id=?", existing.get("sku_id"));
            } else {
                jdbc.update("UPDATE biz_product_sku SET status='1',stock=0,video_sync_status='pending',"
                        + "video_last_error='',video_retry_at=NULL,video_retry_count=0 WHERE sku_id=?",
                        existing.get("sku_id"));
            }
        }
        for (Map<String, Object> row : rows) {
            String code = text(row.get("skuCode"));
            int price = integer(row.get("priceCent"), 0, Integer.MAX_VALUE);
            int stock = integer(row.get("stock"), 0, Integer.MAX_VALUE);
            String status = status(row.get("status"));
            String spec = jsonText(row.get("specJson"));
            Map<String, Object> existing = first("SELECT sku_id FROM biz_product_sku WHERE product_id=? AND sku_code=?", productId, code);
            if (existing == null) jdbc.update("INSERT INTO biz_product_sku(product_id,sku_code,sku_name,price_cent,stock,spec_json,external_sku_id,status) VALUES(?,?,?,?,?,?,?,?)", productId, code, text(row.get("skuName")), price, stock, spec, "", status);
            else jdbc.update("UPDATE biz_product_sku SET sku_name=?,price_cent=?,stock=?,spec_json=?,status=?,video_sync_status=IF(stock<>?,'pending',video_sync_status),video_last_error=IF(stock<>?,'',video_last_error),video_retry_at=IF(stock<>?,NULL,video_retry_at),video_retry_count=IF(stock<>?,0,video_retry_count) WHERE sku_id=?", text(row.get("skuName")), price, stock, spec, status, stock, stock, stock, stock, existing.get("sku_id"));
        }
        return success(jdbc.queryForList("SELECT sku_id AS skuId,product_id AS productId,sku_code AS skuCode,sku_name AS skuName,price_cent AS priceCent,stock,spec_json AS specJson,status,external_sku_id AS externalSkuId,video_sync_status AS videoSyncStatus,video_last_error AS videoLastError,video_synced_at AS videoSyncedAt FROM biz_product_sku WHERE product_id=? ORDER BY sku_id", productId));
    }

    @GetMapping("/point-rules")
    @PreAuthorize("@ss.hasPermi('business:pointRule:list')")
    public AjaxResult pointRules()
    {
        return success(jdbc.queryForList("SELECT rule_id AS ruleId, rule_code AS ruleCode, rule_name AS ruleName, source_type AS sourceType, points, daily_limit AS dailyLimit, validity_days AS validityDays, status, rule_json AS ruleJson FROM biz_point_rule ORDER BY rule_id"));
    }

    @PutMapping("/point-rules")
    @Log(title = "积分规则", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:pointRule:edit')")
    public AjaxResult updatePointRule(@RequestBody Map<String, Object> body)
    {
        long id = longValue(body.get("ruleId"));
        String ruleName = text(body.get("ruleName"));
        if (ruleName.isEmpty() || ruleName.length() > 128) return error("积分规则名称不能为空且最多128字");
        Map<String, Object> existing = first("SELECT rule_code FROM biz_point_rule WHERE rule_id=?", id);
        if (existing == null) return error("积分规则不存在");
        String ruleJson = jsonText(body.get("ruleJson"));
        try {
            com.fasterxml.jackson.databind.JsonNode rules = json.readTree(ruleJson);
            if ("order_paid".equals(text(existing.get("rule_code"))))
                OrderPointRule.pointsPerYuan(rules);
        } catch (IllegalArgumentException e) { return error(e.getMessage()); }
        catch (Exception e) { return error("积分规则扩展参数 JSON 格式无效"); }
        int changed = jdbc.update("UPDATE biz_point_rule SET rule_name=?, points=?, daily_limit=?, validity_days=?, status=?, rule_json=? WHERE rule_id=?",
                ruleName, integer(body.get("points"), 0, 100000000), integer(body.get("dailyLimit"), -1, 1000000), integer(body.get("validityDays"), 0, 3650), status(body.get("status")), ruleJson, id);
        return changed == 0 ? error("积分规则不存在") : success();
    }

    @GetMapping("/promotion/config")
    @PreAuthorize("@ss.hasPermi('business:promotion:query')")
    public AjaxResult promotionConfig()
    {
        return success(first("SELECT config_id AS configId, max_level AS maxLevel, customer_rate_bp AS customerRateBp, staff_rate_bp AS staffRateBp, level_rates_json AS levelRatesJson, settlement_days AS settlementDays, COALESCE(NULLIF(min_withdraw_cent,0),1000) AS minWithdrawCent, refund_policy AS refundPolicy, status FROM biz_promotion_config ORDER BY config_id LIMIT 1"));
    }

    @PutMapping("/promotion/config")
    @Log(title = "分销规则", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:promotion:edit')")
    public AjaxResult updatePromotionConfig(@RequestBody Map<String, Object> body)
    {
        long id = longValue(body.get("configId"));
        String refundPolicy = text(body.get("refundPolicy"));
        if (!("reverse".equals(refundPolicy) || "close".equals(refundPolicy))) return error("退款佣金策略只能是 reverse 或 close");
        int maxLevel = integer(body.get("maxLevel"), 1, 10);
        int customerRate = integer(body.get("customerRateBp"), 0, 10000);
        int staffRate = integer(body.get("staffRateBp"), 0, 10000);
        String ratesJson = promotionRates(body.get("levelRatesJson"), maxLevel, customerRate, staffRate);
        if (ratesJson == null) return error("员工和客户的每层佣金比例必须在 0 至 10000 万分比内，且各自累计不得超过 10000");
        int minWithdrawCent = integer(body.get("minWithdrawCent"), 0, 100000000);
        if (minWithdrawCent <= 0) return error("最低提现金额必须大于0分");
        int changed = jdbc.update("UPDATE biz_promotion_config SET max_level=?, customer_rate_bp=?, staff_rate_bp=?, level_rates_json=?, settlement_days=?, min_withdraw_cent=?, refund_policy=?, status=? WHERE config_id=?",
                maxLevel, customerRate, staffRate, ratesJson, integer(body.get("settlementDays"), 0, 90), minWithdrawCent, refundPolicy, status(body.get("status")), id);
        return changed == 0 ? error("分销配置不存在") : success();
    }

    private String promotionRates(Object value, int maxLevel, int customerFirst, int staffFirst)
    {
        try {
            com.fasterxml.jackson.databind.JsonNode input = value instanceof String
                    ? json.readTree((String) value) : json.valueToTree(value);
            com.fasterxml.jackson.databind.node.ObjectNode result = json.createObjectNode();
            for (String role : Arrays.asList("customer", "staff")) {
                int first = "staff".equals(role) ? staffFirst : customerFirst;
                com.fasterxml.jackson.databind.JsonNode source = input != null && input.isObject() ? input.path(role) : input;
                com.fasterxml.jackson.databind.node.ArrayNode schedule = result.putArray(role);
                long total = 0;
                for (int tier = 0; tier < maxLevel; tier++) {
                    int rate = tier == 0 ? first : source != null && source.isArray() && source.size() > tier
                            ? source.get(tier).asInt(-1) : 0;
                    if (rate < 0 || rate > 10000) return null;
                    total += rate;
                    schedule.add(rate);
                }
                if (total > 10000) return null;
            }
            return json.writeValueAsString(result);
        } catch (Exception e) { return null; }
    }

    @GetMapping("/promotion/settlements")
    @PreAuthorize("@ss.hasPermi('business:settlement:list')")
    public TableDataInfo promotionSettlements(@RequestParam Map<String, String> params)
    {
        return page("SELECT settlement_id AS settlementId, member_id AS memberId, amount_cent AS amountCent, status, request_id AS requestId, payment_reference AS paymentReference, failure_reason AS failureReason, paid_at AS paidAt, create_time AS createTime FROM biz_promotion_settlement ORDER BY settlement_id DESC", params);
    }

    @PutMapping("/promotion/settlements/{id}/status")
    @Log(title = "佣金提现审核", businessType = BusinessType.UPDATE)
    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("@ss.hasPermi('business:settlement:edit')")
    public AjaxResult updatePromotionSettlement(@PathVariable long id, @RequestBody Map<String, Object> body)
    {
        String next = text(body.get("status"));
        String paymentReference = text(body.get("paymentReference")).trim();
        String failureReason = text(body.get("failureReason")).trim();
        Map<String, Object> settlement = first("SELECT settlement_id, status FROM biz_promotion_settlement WHERE settlement_id=? FOR UPDATE", id);
        if (settlement == null) return error("提现申请不存在");
        String previous = text(settlement.get("status"));
        if (!("pending".equals(previous) && ("processing".equals(next) || "failed".equals(next))
                || "processing".equals(previous) && ("paid".equals(next) || "failed".equals(next)))) return error("提现状态不能变更");
        if ("paid".equals(next) && (paymentReference.isEmpty() || paymentReference.length() > 128)) return error("确认付款必须填写有效的转账凭证号（最多128字）");
        if ("failed".equals(next) && (failureReason.isEmpty() || failureReason.length() > 500)) return error("退回提现必须填写原因（最多500字）");
        jdbc.update("UPDATE biz_promotion_settlement SET status=?" + ("paid".equals(next) ? ", payment_reference=?, failure_reason=NULL, paid_at=NOW()" : "failed".equals(next) ? ", failure_reason=?, payment_reference=NULL" : "") + " WHERE settlement_id=?",
                "paid".equals(next) ? new Object[] { next, paymentReference, id } : "failed".equals(next) ? new Object[] { next, failureReason, id } : new Object[] { next, id });
        if ("failed".equals(next)) jdbc.update("UPDATE biz_promotion_record p JOIN biz_promotion_settlement_item i ON i.promotion_record_id=p.promotion_record_id SET p.status='settled' WHERE i.settlement_id=? AND p.status='processing'", id);
        if ("paid".equals(next)) {
            jdbc.update("UPDATE biz_promotion_record p JOIN biz_promotion_settlement_item i ON i.promotion_record_id=p.promotion_record_id SET p.status='paid' WHERE i.settlement_id=? AND p.status='processing'", id);
            jdbc.update("UPDATE biz_promotion_record SET refund_recovery_required=0 WHERE status='paid' AND amount_cent<0 AND refund_recovery_required=1 AND promotion_record_id IN (SELECT promotion_record_id FROM biz_promotion_settlement_item WHERE settlement_id=?)", id);
            jdbc.update("UPDATE biz_promotion_record original JOIN biz_promotion_record recovery ON recovery.member_id=original.member_id AND recovery.order_id=original.order_id AND recovery.amount_cent<0 AND recovery.status='paid' AND recovery.refund_recovery_required=0 "
                            + "LEFT JOIN biz_promotion_record outstanding ON outstanding.member_id=original.member_id AND outstanding.order_id=original.order_id AND outstanding.amount_cent<0 AND outstanding.refund_recovery_required=1 AND outstanding.status IN ('pending','settled','processing') "
                            + "SET original.refund_recovery_required=0 WHERE original.status='paid' AND original.refund_recovery_required=1 AND outstanding.promotion_record_id IS NULL");
        }
        return success();
    }

    @GetMapping("/ranking/config")
    @PreAuthorize("@ss.hasPermi('business:ranking:query')")
    public AjaxResult rankingConfig()
    {
        return success(first("SELECT config_id AS configId, period_type AS periodType, reward_json AS rewardJson, start_time AS startTime, end_time AS endTime, status FROM biz_ranking_config ORDER BY config_id LIMIT 1"));
    }

    @PutMapping("/ranking/config")
    @Log(title = "销冠排行规则", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:ranking:edit')")
    public AjaxResult updateRankingConfig(@RequestBody Map<String, Object> body)
    {
        long id = longValue(body.get("configId"));
        String periodType = text(body.get("periodType"));
        if (!("week".equals(periodType) || "month".equals(periodType) || "quarter".equals(periodType))) return error("排行榜周期无效");
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = parseOptionalDateTime(body.get("startTime"), "排行榜开始时间");
            end = parseOptionalDateTime(body.get("endTime"), "排行榜结束时间");
        } catch (IllegalArgumentException e) { return AjaxResult.error(400, e.getMessage()); }
        if ((start == null) != (end == null)) return error("自定义排行榜周期必须同时填写开始与结束时间");
        if (start != null && end != null && !start.isBefore(end)) return error("排行榜结束时间必须晚于开始时间");
        String rewardJson = jsonText(body.get("rewardJson"));
        try {
            com.fasterxml.jackson.databind.JsonNode rewards = json.readTree(rewardJson);
            String invalid = rankingRewardsError(rewards);
            if (invalid != null) return error(invalid);
        } catch (Exception e) { return error("排行榜奖励规则 JSON 格式无效"); }
        int changed = jdbc.update("UPDATE biz_ranking_config SET period_type=?, reward_json=?, start_time=?, end_time=?, status=? WHERE config_id=?",
                periodType, rewardJson, body.get("startTime"), body.get("endTime"), status(body.get("status")), id);
        return changed == 0 ? error("排行榜配置不存在") : success();
    }

    @GetMapping("/ranking/rewards")
    @PreAuthorize("@ss.hasPermi('business:ranking:query')")
    public AjaxResult rankingRewards(@RequestParam(required = false, defaultValue = "0") long configId)
    {
        if (configId <= 0) return success(Collections.emptyList());
        return success(jdbc.queryForList("SELECT r.reward_id AS rewardId,r.member_id AS memberId,m.nickname AS nickname,r.rank_no AS rankNo,r.points,r.subsidy_cent AS subsidyCent,r.issue_reference AS issueReference,CASE WHEN r.period_start='1000-01-01 00:00:00' THEN '历史未分期' ELSE DATE_FORMAT(r.period_start,'%Y-%m-%d') END AS periodStart,r.status,r.issued_at AS issuedAt FROM biz_ranking_reward r LEFT JOIN biz_member m ON m.member_id=r.member_id WHERE r.config_id=? ORDER BY r.period_start DESC,r.rank_no", configId));
    }

    @GetMapping("/ranking/periods")
    @PreAuthorize("@ss.hasPermi('business:ranking:query')")
    public AjaxResult rankingPeriods(@RequestParam long configId)
    {
        return success(jdbc.queryForList("SELECT period_id AS periodId,period_start AS periodStart,period_end AS periodEnd,period_type AS periodType,reward_count AS rewardCount,create_by AS createBy,create_time AS createTime FROM biz_ranking_period WHERE config_id=? ORDER BY period_start DESC", configId));
    }

    @PostMapping("/ranking/issue")
    @Log(title = "销冠奖励计算", businessType = BusinessType.INSERT)
    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("@ss.hasPermi('business:ranking:issue')")
    public AjaxResult issueRanking(@RequestBody Map<String, Object> body)
    {
        long configId = longValue(body.get("configId"));
        // Serialize every finalization under this configuration, including
        // empty periods and concurrent requests selecting overlapping windows.
        Map<String, Object> config = first("SELECT period_type AS periodType, start_time AS startTime, end_time AS endTime, reward_json AS rewardJson FROM biz_ranking_config WHERE config_id=? AND status='0' FOR UPDATE", configId);
        if (config == null) return error("排行榜配置不存在或已停用");
        LocalDateTime[] period;
        try { period = rankingIssuePeriod(config, text(body.get("periodDate"))); }
        catch (IllegalArgumentException e) { return AjaxResult.error(400, e.getMessage()); }
        if (period[1].isAfter(LocalDateTime.now())) return AjaxResult.error(409, "统计周期尚未结束，不能生成最终奖励");
        java.sql.Timestamp periodStart = java.sql.Timestamp.valueOf(period[0]);
        java.sql.Timestamp periodEnd = java.sql.Timestamp.valueOf(period[1]);
        Map<String, Object> existing = first("SELECT period_start AS periodStart,period_end AS periodEnd,reward_count AS count FROM biz_ranking_period WHERE config_id=? AND period_start=?", configId, periodStart);
        if (existing != null) { existing.put("alreadyIssued", true); return success(existing); }
        if (longValue(scalar("SELECT COUNT(*) FROM biz_ranking_period WHERE config_id=? AND period_start<? AND period_end>?", configId, periodEnd, periodStart)) > 0
                || longValue(scalar("SELECT COUNT(*) FROM biz_ranking_reward WHERE config_id=? AND period_start<? AND period_end>?", configId, periodEnd, periodStart)) > 0)
            return AjaxResult.error(409, "所选时间与已发奖周期重叠，请核对历史记录，不能重复结算");
        com.fasterxml.jackson.databind.JsonNode rewards;
        try { rewards = json.readTree(String.valueOf(config.get("rewardJson"))); } catch (Exception e) { return error("排行榜奖励规则无效"); }
        String invalid = rankingRewardsError(rewards);
        if (invalid != null) return error(invalid);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT member_id AS memberId, SUM(amount_cent) AS amount FROM biz_promotion_record WHERE status IN ('pending','settled','processing','paid') AND create_time>=? AND create_time<? GROUP BY member_id HAVING SUM(amount_cent)>0 ORDER BY amount DESC, member_id ASC LIMIT 100", java.sql.Timestamp.valueOf(period[0]), java.sql.Timestamp.valueOf(period[1]));
        int rank = 0, issuedCount = 0;
        for (Map<String, Object> row : rows) {
            rank++;
            row.put("rank", rank);
            com.fasterxml.jackson.databind.JsonNode reward = null;
            for (com.fasterxml.jackson.databind.JsonNode item : rewards) if (item.path("rank").asInt(-1) == rank) { reward = item; break; }
            if (reward == null) continue;
            int points = Math.max(0, reward.path("points").asInt(0));
            int subsidy = Math.max(0, reward.path("subsidyCent").asInt(0));
            jdbc.update("INSERT INTO biz_ranking_reward(config_id,period_start,period_end,member_id,rank_no,points,subsidy_cent,status) VALUES(?,?,?,?,?,?,?,'pending')", configId, periodStart, periodEnd, row.get("memberId"), rank, points, subsidy);
            issuedCount++;
            Map<String, Object> created = first("SELECT reward_id FROM biz_ranking_reward WHERE config_id=? AND period_start=? AND member_id=?", configId, periodStart, row.get("memberId"));
            long rewardId = longValue(created.get("reward_id"));
            if (points > 0) {
                pointExpiry.expireMember(longValue(row.get("memberId")));
                jdbc.update("INSERT IGNORE INTO biz_point_account(member_id,balance) VALUES(?,0)", row.get("memberId"));
                // Serialize the ledger write with sign-in, exchange, order
                // points, and expiry jobs so balance_after belongs to this
                // reward transaction rather than a concurrent update.
                jdbc.queryForObject("SELECT balance FROM biz_point_account WHERE member_id=? FOR UPDATE",
                        Integer.class, row.get("memberId"));
                jdbc.update("UPDATE biz_point_account SET balance=balance+? WHERE member_id=?", points, row.get("memberId"));
                jdbc.update("INSERT INTO biz_point_record(member_id,change_amount,balance_after,source_type,source_id,title,expires_at) SELECT member_id, ?, balance, 'ranking', ?, '销冠排行榜奖励', ? FROM biz_point_account WHERE member_id=?", points, String.valueOf(rewardId), pointExpiry.expirationFor("ranking"), row.get("memberId"));
            }
            if (subsidy == 0) {
                jdbc.update("UPDATE biz_ranking_reward SET status='issued', issued_at=NOW() WHERE reward_id=?", rewardId);
            } else {
                jdbc.update("UPDATE biz_ranking_reward SET status='processing' WHERE reward_id=?", rewardId);
            }
        }
        String resultJson;
        try { resultJson = json.writeValueAsString(rows); }
        catch (JsonProcessingException e) { throw new IllegalStateException("无法保存排行榜结算快照", e); }
        jdbc.update("INSERT INTO biz_ranking_period(config_id,period_start,period_end,period_type,reward_json,result_json,reward_count,create_by) VALUES(?,?,?,?,?,?,?,?)",
                configId, periodStart, periodEnd, config.get("periodType"), String.valueOf(config.get("rewardJson")), resultJson, issuedCount, getUsername());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", issuedCount); result.put("alreadyIssued", false); result.put("periodStart", periodStart); result.put("periodEnd", periodEnd);
        return success(result);
    }

    @PutMapping("/ranking/rewards/{id}/status")
    @Log(title = "销冠奖励发放", businessType = BusinessType.UPDATE)
    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("@ss.hasPermi('business:ranking:issue')")
    public AjaxResult updateRankingReward(@PathVariable long id, @RequestBody Map<String, Object> body)
    {
        String next = text(body.get("status"));
        Map<String, Object> reward = first("SELECT reward_id,status FROM biz_ranking_reward WHERE reward_id=? FOR UPDATE", id);
        if (reward == null) return error("排行榜獎励不存在");
        if (!"processing".equals(text(reward.get("status"))) || !"issued".equals(next)) return error("该奖励当前不可确认");
        String issueReference = text(body.get("issueReference"));
        if (issueReference.isEmpty() || issueReference.length() > 128) return error("请填写有效的补贴发放凭证号（最多128字）");
        jdbc.update("UPDATE biz_ranking_reward SET status='issued', issue_reference=?, issued_at=NOW() WHERE reward_id=?", issueReference, id);
        return success();
    }

    @PostMapping("/social/campaigns")
    @Log(title = "VIP社群活动", businessType = BusinessType.INSERT)
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("@ss.hasPermi('business:social:add')")
    public AjaxResult createSocialCampaign(@RequestBody Map<String, Object> body)
    {
        if (body == null) return error("活动内容不能为空");
        String campaignType = text(body.get("campaignType"));
        String title = text(body.get("title"));
        String content = text(body.get("content"));
        if (!("promotion".equals(campaignType) || "coupon".equals(campaignType) || "points".equals(campaignType))) return error("社群活动类型无效");
        if (title.isEmpty() || title.length() > 128 || content.isEmpty() || content.length() > 4000) return error("请填写标题和内容，标题最多128字、内容最多4000字");
        String audience = text(body.get("audience"));
        if (audience.isEmpty()) audience = "vip";
        if (!("vip".equals(audience) || "all".equals(audience))) return error("社群受众范围无效");
        Long couponTemplateId = null;
        if ("coupon".equals(campaignType))
        {
            if (body.get("couponTemplateId") == null) return error("优惠券提醒必须选择券模板");
            couponTemplateId = longValue(body.get("couponTemplateId"));
            if (first("SELECT coupon_template_id FROM biz_coupon_template WHERE coupon_template_id=? AND status='0' FOR UPDATE", couponTemplateId) == null)
                return error("券模板不存在或已停用");
        }
        String channels = text(body.get("channels"));
        if (channels.isEmpty()) channels = "subscribe";
        if (!("subscribe".equals(channels) || "group".equals(channels) || "both".equals(channels))) return error("社群发送通道无效");
        LocalDateTime scheduledAt;
        try {
            scheduledAt = parseOptionalDateTime(body.get("scheduledAt"), "计划发送时间");
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(400, e.getMessage());
        }
        if (scheduledAt != null && scheduledAt.isBefore(LocalDateTime.now().minusMinutes(1)))
            return error("计划发送时间不能早于当前时间");
        Map<String, Object> target = new LinkedHashMap<>(); target.put("audience", audience); target.put("channels", channels);
        final Long finalCouponTemplateId = couponTemplateId;
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement("INSERT INTO biz_social_campaign(campaign_type,title,content,coupon_template_id,target_json,scheduled_at,status,create_by) VALUES(?,?,?,?,?,?,?,?)", new String[] {"campaign_id"});
            ps.setString(1, campaignType); ps.setString(2, title); ps.setString(3, content); if (finalCouponTemplateId == null) ps.setNull(4, java.sql.Types.BIGINT); else ps.setLong(4, finalCouponTemplateId); ps.setString(5, jsonText(target)); if (scheduledAt == null) ps.setNull(6, java.sql.Types.TIMESTAMP); else ps.setTimestamp(6, java.sql.Timestamp.valueOf(scheduledAt)); ps.setString(7, "draft"); ps.setString(8, getUsername()); return ps;
        }, key);
        return success(Collections.singletonMap("campaignId", key.getKey()));
    }

    @GetMapping("/social/campaigns")
    @PreAuthorize("@ss.hasPermi('business:social:list')")
    public AjaxResult socialCampaigns()
    {
        return success(socialDelivery.campaigns());
    }

    @GetMapping("/social/campaigns/{id}/deliveries")
    @PreAuthorize("@ss.hasPermi('business:social:list')")
    public AjaxResult socialCampaignDeliveries(@PathVariable long id)
    {
        try { return success(socialDelivery.deliveries(id)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/social/campaigns/{id}/send")
    @Log(title = "VIP社群消息投递", businessType = BusinessType.OTHER)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:social:send')")
    public AjaxResult sendSocialCampaign(@PathVariable long id)
    {
        Map<String, Object> campaign = first("SELECT campaign_id, target_json, content, status FROM biz_social_campaign WHERE campaign_id=? FOR UPDATE", id);
        if (campaign == null) return error("活动不存在");
        if (!"draft".equals(text(campaign.get("status")))) return error("只有草稿活动可以加入发送队列");
        Set<String> channels = socialChannels(campaign.get("target_json"));
        String templateId = channelValue("wechat.mini.socialTemplateId");
        if (channels.contains("subscribe") && templateId.isEmpty()) return AjaxResult.error(503, "该活动需要订阅通知，请先配置微信小程序订阅消息模板 ID");
        if (channels.contains("group") && !workGroupMessage.configured()) return AjaxResult.error(503, "该活动需要企业微信群，请先配置 CorpID、Secret 和目标群 ID");
        String audience = socialAudience(campaign.get("target_json"));
        int subscribeCount = 0;
        if (channels.contains("subscribe"))
        {
            String sql = "INSERT IGNORE INTO biz_social_delivery(campaign_id,member_id,channel,status,subscription_id) SELECT ?,s.member_id,'subscribe','pending',MIN(s.subscription_id) FROM biz_social_subscription s JOIN biz_member m ON m.member_id=s.member_id AND m.status='0' AND m.wechat_openid IS NOT NULL AND m.wechat_openid<>'' WHERE s.status='available' AND s.template_id=?";
            if ("vip".equals(audience)) sql += " AND m.vip_level>0";
            sql += " GROUP BY s.member_id";
            jdbc.update(sql, id, templateId);
            subscribeCount = jdbc.update("UPDATE biz_social_subscription s JOIN biz_social_delivery d ON d.subscription_id=s.subscription_id SET s.status='reserved',s.delivery_id=d.delivery_id,s.reserved_until=NULL WHERE d.campaign_id=? AND d.status='pending' AND s.status='available'", id);
        }
        int groupCount = channels.contains("group") ? workGroupMessage.enqueue(id, text(campaign.get("content"))) : 0;
        int total = subscribeCount + groupCount;
        jdbc.update("UPDATE biz_social_campaign SET status=?, sent_count=? WHERE campaign_id=?", total > 0 ? "queued" : "failed", total, id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queued", total);
        result.put("subscribeQueued", subscribeCount);
        result.put("groupQueued", groupCount);
        String message = total > 0 ? "已创建社群发送任务，系统将分别记录订阅通知和企业微信群回执" : "没有找到符合受众范围的订阅会员或可用企业微信群";
        result.put("message", message);
        if (total == 0)
        {
            AjaxResult failure = AjaxResult.error(409, message);
            failure.put("data", result);
            return failure;
        }
        return success(result);
    }

    private String channelValue(String key)
    {
        Map<String, Object> row = first("SELECT config_value FROM biz_channel_config WHERE config_key=?", key);
        return row == null ? "" : text(row.get("config_value"));
    }

    @GetMapping("/readiness")
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult readiness()
    {
        List<Map<String, Object>> checks = new ArrayList<>();

        checks.add(platformCheck("mini", "小程序身份与发布版本", "请配置 AppID/Secret，并使用 release 版本和路径校验",
                "参数齐全，需用正式包真机验证页面及合法域名", "正式小程序版本及域名已验收", "微信公众平台 / 开发者工具"));

        checks.add(readinessItem("localLogin", "本地联调登录", localLoginEnabled ? "pending" : "ready",
                localLoginEnabled ? "本地联调登录已启用，生产环境必须关闭并清空本地登录凭据" : "本地联调登录已关闭",
                localLoginEnabled ? "配置文件 / 环境变量 MINI_LOCAL_LOGIN_ENABLED=false" : "无需处理"));
        boolean adminPasswordReady = adminPasswordReady();
        checks.add(readinessItem("adminPassword", "管理员密码", adminPasswordReady ? "ready" : "pending",
                adminPasswordReady ? "超级管理员密码已修改" : "超级管理员仍使用初始密码或账号未启用，正式部署前必须修改",
                adminPasswordReady ? "无需处理" : "个人中心 / 修改密码"));

        boolean jwtReady = present(jwtSecret) && jwtSecret.length() >= 32
                && !"development".equalsIgnoreCase(appEnvironment)
                && !jwtSecret.contains("change-before-production")
                && !jwtSecret.contains("replace-with");
        checks.add(readinessItem("jwt", "登录令牌密钥", jwtReady ? "ready" : "pending",
                jwtReady ? "已配置独立的登录令牌密钥" : "请为正式环境配置独立的 JWT_SECRET，不能沿用本地联调值",
                jwtReady ? "无需处理" : "部署环境 / JWT_SECRET"));

        checks.add(platformCheck("pay", "微信支付与退款", "尚未完成商户号、证书、API v3 密钥或公网回调配置",
                "参数齐全，需用真实订单完成支付与退款回调验收", "真实支付和退款回执已登记", "微信商户平台 + 部署环境"));
        checks.add(platformCheck("transfer", "现金红包/商户转账", "商户转账未启用或缺少凭据、场景、回调配置",
                "参数齐全，需真机中奖并等待微信异步结果", "真实转账到账回执已登记", "微信商户平台 + 部署环境"));
        checks.add(platformCheck("social", "VIP 订阅通知", "请配置小程序凭据和已审核的订阅模板 ID",
                "模板已配置，需会员授权后真机验证送达", "真实订阅消息送达已登记", "运营中心 / 微信公众平台"));
        checks.add(platformCheck("work", "企业微信群消息与二维码", "请配置 CorpID、Secret、目标群并生成有效二维码",
                "群二维码已生成，需真机扫码并验证群消息送达", "真实加群及群消息回执已登记", "部署环境 + 运营中心"));
        checks.add(platformCheck("video", "视频号/微信小店", "请配置小店 ID、Finder 用户名并启用同步",
                "参数齐全，需验证商品、库存、订单与售后回流", "真实小店及视频号回流已登记", "运营中心 / 微信小店"));

        int ready = (int) checks.stream().filter(item -> "ready".equals(item.get("status"))).count();
        int pending = (int) checks.stream().filter(item -> "pending".equals(item.get("status"))).count();
        int verify = (int) checks.stream().filter(item -> "verify".equals(item.get("status"))).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checks", checks);
        result.put("readyCount", ready);
        result.put("verificationCount", verify);
        result.put("pendingCount", pending);
        result.put("productionReady", pending == 0 && verify == 0);
        result.put("checkedAt", LocalDateTime.now().toString());
        return success(result);
    }

    @PostMapping("/readiness/{key}/verify")
    @Log(title = "正式平台上线验收", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult verifyReadiness(@PathVariable String key, @RequestBody Map<String, Object> body)
    {
        String fingerprint = verificationFingerprint(key);
        if (fingerprint == null) return AjaxResult.error(409, "该渠道配置尚不完整，不能登记平台验收");
        try { return success(productionVerification.record(key, fingerprint, body == null ? "" : text(body.get("evidence")))); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    private Map<String, Object> platformCheck(String key, String label, String pendingDetail, String verifyDetail, String readyDetail, String action)
    {
        String fingerprint = verificationFingerprint(key);
        Map<String, Object> verified = fingerprint == null ? null : productionVerification.matching(key, fingerprint);
        String status = fingerprint == null ? "pending" : verified == null ? "verify" : "ready";
        Map<String, Object> item = readinessItem(key, label, status,
                "pending".equals(status) ? pendingDetail : "verify".equals(status) ? verifyDetail : readyDetail, action);
        if (verified != null) item.putAll(verified);
        return item;
    }

    private String verificationFingerprint(String key)
    {
        switch (key)
        {
            case "mini":
                if (!present(miniAppId) || !present(miniAppSecret) || !"release".equals(miniCodeEnvVersion)
                        || !miniCodeCheckPath) return null;
                return ProductionVerificationService.fingerprint(key, miniAppId, miniAppSecret,
                        miniCodeEnvVersion, String.valueOf(miniCodeCheckPath));
            case "pay":
                String payMaterial = wechatPay.verificationMaterialFingerprint();
                if (!wechatPayEnabled || !present(miniAppId) || !present(wechatMerchantId) || !present(wechatMerchantSerial)
                        || !present(wechatPrivateKey) || !present(wechatApiV3Key) || !https(wechatPayNotifyUrl)
                        || !https(wechatRefundNotifyUrl) || !present(wechatPlatformCert) || payMaterial == null) return null;
                return ProductionVerificationService.fingerprint(key, miniAppId, wechatMerchantId, wechatMerchantSerial,
                        wechatPrivateKey, wechatApiV3Key, wechatPayNotifyUrl, wechatRefundNotifyUrl, wechatPlatformCert,
                        payMaterial);
            case "transfer":
                String transferMaterial = wechatPay.verificationMaterialFingerprint();
                if (!transferEnabled || !present(miniAppId) || !present(wechatMerchantId) || !present(wechatMerchantSerial)
                        || !present(wechatPrivateKey) || !present(wechatApiV3Key) || !present(wechatPlatformCert)
                        || !https(transferNotifyUrl) || !present(transferSceneId) || transferMaterial == null) return null;
                return ProductionVerificationService.fingerprint(key, miniAppId, wechatMerchantId, wechatMerchantSerial,
                        wechatPrivateKey, wechatApiV3Key, wechatPlatformCert, transferNotifyUrl, transferSceneId,
                        transferMaterial);
            case "social":
                String templateId = channelValue("wechat.mini.socialTemplateId");
                if (!present(miniAppId) || !present(miniAppSecret) || !present(templateId)) return null;
                return ProductionVerificationService.fingerprint(key, miniAppId, miniAppSecret, templateId,
                        channelValue("wechat.mini.socialTitleField"), channelValue("wechat.mini.socialContentField"));
            case "work":
                String groupIds = channelValue("wechat.work.groupChatIds");
                String qr = channelValue("wechat.work.groupQr");
                String configId = channelValue("wechat.work.groupQrConfigId");
                String expiry = channelValue("wechat.work.groupQrExpiresAt");
                if (!present(workCorpId) || !present(workSecret) || !present(groupIds) || !present(qr)
                        || !present(configId) || present(channelValue("wechat.work.groupQrPreviousConfigId"))) return null;
                try { if (!LocalDateTime.parse(expiry, CHANNEL_TIME).isAfter(LocalDateTime.now())) return null; }
                catch (Exception e) { return null; }
                return ProductionVerificationService.fingerprint(key, workCorpId, workSecret, groupIds, qr, configId, expiry);
            case "video":
                String shopId = channelValue("wechat.video.shopId");
                String finder = channelValue("wechat.video.finderUserName");
                String syncEnabled = channelValue("wechat.video.syncEnabled");
                if (!present(miniAppId) || !present(miniAppSecret) || !present(shopId) || !present(finder)
                        || !"true".equalsIgnoreCase(syncEnabled)) return null;
                return ProductionVerificationService.fingerprint(key, miniAppId, miniAppSecret, shopId, finder, syncEnabled,
                        channelValue("wechat.video.liveFeedId"), channelValue("wechat.video.liveNonceId"),
                        channelValue("wechat.video.feedId"), channelValue("wechat.video.nonceId"),
                        channelValue("wechat.video.returnAddressId"));
            default: return null;
        }
    }

    private boolean adminPasswordReady()
    {
        List<String> hashes = jdbc.query("SELECT password FROM sys_user WHERE user_name='admin' AND status='0' AND del_flag='0'",
                (rs, row) -> rs.getString(1));
        if (hashes.size() != 1 || hashes.get(0) == null || !hashes.get(0).startsWith("$2")) return false;
        return !SecurityUtils.matchesPassword("admin123", hashes.get(0));
    }

    private Map<String, Object> readinessItem(String key, String label, String status, String detail, String action)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key); item.put("label", label); item.put("status", status); item.put("detail", detail); item.put("action", action);
        return item;
    }

    private boolean present(String value) { return value != null && !value.trim().isEmpty(); }

    private boolean https(String value) { return present(value) && value.trim().toLowerCase().startsWith("https://"); }

    private String socialAudience(Object targetValue)
    {
        if (targetValue == null || text(targetValue).isEmpty()) return "vip";
        try {
            Map<?, ?> target = json.readValue(String.valueOf(targetValue), Map.class);
            String audience = text(target.get("audience"));
            if ("vip".equals(audience) || "all".equals(audience)) return audience;
        }
        catch (Exception ignored) { }
        throw new IllegalArgumentException("社群受众配置无效，请重新编辑活动");
    }

    private Set<String> socialChannels(Object targetValue)
    {
        String value = "subscribe";
        try
        {
            if (targetValue != null && !text(targetValue).isEmpty())
            {
                Map<?, ?> target = json.readValue(String.valueOf(targetValue), Map.class);
                String configured = text(target.get("channels"));
                if (!configured.isEmpty()) value = configured;
            }
        }
        catch (Exception ignored) { }
        Set<String> result = new HashSet<>();
        if ("subscribe".equals(value) || "both".equals(value)) result.add("subscribe");
        if ("group".equals(value) || "both".equals(value)) result.add("group");
        if (result.isEmpty()) throw new IllegalArgumentException("社群发送通道配置无效，请重新编辑活动");
        return result;
    }

    @GetMapping("/payments/reconcile")
    @PreAuthorize("@ss.hasPermi('business:refund:list')")
    public TableDataInfo paymentReconciliations(@RequestParam Map<String, String> params)
    {
        int pageNum = integer(params.get("pageNum"), 1, 100000);
        int pageSize = integer(params.get("pageSize"), 10, 200);
        String where = " FROM biz_order_pay_reconcile r JOIN biz_order o ON o.order_no=r.order_no "
                + "WHERE r.last_trade_state IN ('ERROR','REVIEW','REFUND')";
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT r.order_no AS orderNo,r.member_id AS memberId,r.last_trade_state AS tradeState,"
                        + "r.attempt_count AS attemptCount,r.last_query_at AS lastQueryAt,r.next_query_at AS nextQueryAt,"
                        + "r.last_error AS lastError,o.status AS orderStatus,o.wechat_transaction_id AS transactionId,"
                        + "r.update_time AS updateTime" + where + " ORDER BY r.update_time DESC LIMIT ? OFFSET ?",
                pageSize, (pageNum - 1) * pageSize);
        TableDataInfo result = new TableDataInfo(rows, total == null ? 0 : total);
        result.setCode(200);
        result.setMsg("查询成功");
        return result;
    }

    @PostMapping("/payments/reconcile/{orderNo}")
    @Log(title = "微信支付人工查单", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    public AjaxResult reconcilePayment(@PathVariable String orderNo)
    {
        return success(paymentReconciliation.reconcileOne(orderNo));
    }

    @GetMapping("/refunds")
    @PreAuthorize("@ss.hasPermi('business:refund:list')")
    public TableDataInfo refunds(@RequestParam Map<String, String> params)
    {
        return page("SELECT r.refund_id AS refundId, r.refund_no AS refundNo, r.order_id AS orderId, o.order_no AS orderNo, o.status AS orderStatus, o.fulfillment_mode AS fulfillmentMode, o.total_cent AS orderTotalCent, r.member_id AS memberId, r.amount_cent AS amountCent, r.reason, r.status, r.channel_status AS channelStatus, r.manual_payment_reference AS manualPaymentReference, r.manual_paid_at AS manualPaidAt, r.manual_verified_by AS manualVerifiedBy, r.create_time AS createTime FROM biz_order_refund r LEFT JOIN biz_order o ON o.order_id=r.order_id ORDER BY r.refund_id DESC", params);
    }

    @GetMapping("/refunds/{id}/returns")
    @PreAuthorize("@ss.hasPermi('business:refund:list')")
    public AjaxResult returnReceipts(@PathVariable long id)
    {
        try { return success(orderRefunds.returnContext(id)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/refunds/{id}/returns")
    @Log(title = "退货实收与库存入库", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    public AjaxResult recordReturnReceipt(@PathVariable long id, @RequestBody Map<String, Object> body)
    {
        try { return success(orderRefunds.recordReturn(id, body, getUsername())); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/refunds/{id}/wechat")
    @Log(title = "微信退款提交", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    public AjaxResult wechatRefund(@PathVariable long id)
    {
        return success(wechatPay.initiateRefund(id));
    }

    @PostMapping("/refunds/{id}/wechat-sync")
    @Log(title = "微信退款查单", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    public AjaxResult syncWechatRefund(@PathVariable long id)
    {
        return success(wechatPay.queryRefundStatus(id));
    }

    @PutMapping("/refunds/{id}/status")
    @Log(title = "退款售后审核", businessType = BusinessType.UPDATE)
    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    public AjaxResult refundStatus(@PathVariable long id, @RequestBody Map<String, Object> body)
    {
        String next = text(body.get("status"));
        Map<String, Object> refund = first("SELECT refund_id, order_id, amount_cent, status, channel_status FROM biz_order_refund WHERE refund_id=? FOR UPDATE", id);
        if (refund == null) return error("退款申请不存在");
        if (first("SELECT order_id FROM biz_order WHERE order_id=? FOR UPDATE", refund.get("order_id")) == null) return error("退款所属订单不存在");
        String previous = text(refund.get("status"));
        if (!("pending".equals(previous) && ("approved".equals(next) || "rejected".equals(next))
                || "approved".equals(previous) && "refunded".equals(next))) return error("退款状态不能变更");
        if ("refunded".equals(next)) {
            String channelStatus = text(refund.get("channel_status"));
            if (!channelStatus.isEmpty() && !"CLOSED".equals(channelStatus))
                return AjaxResult.error(409, "微信退款状态尚未明确关闭，请先查单核对，不能线下确认");
            String paymentReference = text(body.get("paymentReference"));
            if (paymentReference.isEmpty() || paymentReference.length() > 128)
                return AjaxResult.error(400, "请填写不超过128字的实际付款凭证号");
            orderRefunds.markRefunded(id, "manual");
            jdbc.update("UPDATE biz_order_refund SET channel_status='MANUAL_CONFIRMED', manual_payment_reference=?, manual_paid_at=NOW(), manual_verified_by=? WHERE refund_id=? AND status='refunded'",
                    paymentReference, getUsername(), id);
            return success();
        }
        jdbc.update("UPDATE biz_order_refund SET status=? WHERE refund_id=?", next, id);
        orderRefunds.syncPromotionRefundHold(((Number) refund.get("order_id")).longValue());
        return success();
    }

    @GetMapping("/channel")
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult channel()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        String placeholders = String.join(",", Collections.nCopies(CHANNEL_KEYS.size(), "?"));
        for (Map<String, Object> row : jdbc.queryForList("SELECT config_key, config_value FROM biz_channel_config WHERE config_key IN (" + placeholders + ")", CHANNEL_KEYS.toArray())) result.put(String.valueOf(row.get("config_key")), row.get("config_value"));
        return success(result);
    }

    @PutMapping("/channel")
    @Log(title = "微信渠道配置", businessType = BusinessType.UPDATE)
    @Transactional
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult updateChannel(@RequestBody Map<String, Object> body)
    {
        if (body == null || body.isEmpty()) return AjaxResult.error(400, "请填写渠道配置");
        for (Map.Entry<String, Object> entry : body.entrySet())
        {
            if (!CHANNEL_KEYS.contains(entry.getKey())) return error("包含不支持的渠道配置项");
            if (CHANNEL_MANAGED_KEYS.contains(entry.getKey())) return AjaxResult.error(400, "企业微信群二维码及群 ID 只能通过生成/轮换接口更新");
            String value = text(entry.getValue());
            if (value.length() > 2048) return error("渠道配置值不能超过2048个字符");
        }
        for (Map.Entry<String, Object> entry : body.entrySet()) jdbc.update("INSERT INTO biz_channel_config(config_key,config_value) VALUES(?,?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)", entry.getKey(), text(entry.getValue()));
        return success();
    }

    @PostMapping("/video/sync")
    @Log(title = "微信小店同步", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    @Transactional
    public AjaxResult syncVideoProducts(@RequestBody(required = false) Map<String, Object> body)
    {
        if (body == null || body.get("productId") == null) return error("商品 ID 无效");
        long productId = longValue(body.get("productId"));
        try { return success(wechatShopStockSync.syncProduct(productId, true)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @GetMapping("/video/products/{productId}")
    @PreAuthorize("@ss.hasPermi('business:product:list')")
    public AjaxResult videoProductMapping(@PathVariable long productId)
    {
        return success(wechatShopProduct.mapping(productId));
    }

    @GetMapping("/video/mounts")
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult videoFeedMounts(@RequestParam(required = false) Long productId, @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "50") int pageSize)
    {
        return success(videoFeedMount.list(productId, pageNum, pageSize));
    }

    @PostMapping("/video/mounts")
    @Log(title = "短视频商品挂载配置", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult saveVideoFeedMount(@RequestBody Map<String, Object> body)
    {
        try { return success(videoFeedMount.save(body)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/video/mounts/{id}/submit")
    @Log(title = "短视频商品挂载提交", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult submitVideoFeedMount(@PathVariable long id)
    {
        try { return success(videoFeedMount.submit(id)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PutMapping("/video/mounts/{id}/verify")
    @Log(title = "短视频商品挂载结果确认", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult verifyVideoFeedMount(@PathVariable long id, @RequestBody Map<String, Object> body)
    {
        try { return success(videoFeedMount.verify(id, body)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @GetMapping("/video/products/{productId}/template")
    @PreAuthorize("@ss.hasPermi('business:product:list')")
    public AjaxResult videoProductTemplate(@PathVariable long productId)
    {
        try { return success(wechatShopProduct.template(productId)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PutMapping("/video/products/{productId}")
    @Log(title = "微信小店商品映射", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:product:edit')")
    public AjaxResult saveVideoProductMapping(@PathVariable long productId, @RequestBody Map<String, Object> body)
    {
        String externalId = text(body.get("externalId"));
        try { wechatShopProduct.saveMapping(productId, externalId, body.containsKey("payload") ? text(body.get("payload")) : null); return success(wechatShopProduct.mapping(productId)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/video/products/{productId}/submit")
    @Log(title = "微信小店商品发布", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult submitVideoProduct(@PathVariable long productId)
    {
        try { return success(wechatShopProduct.submit(productId)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/video/products/{productId}/listing")
    @Log(title = "微信小店商品申请上架", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult requestVideoProductListing(@PathVariable long productId)
    {
        try { return success(wechatShopProduct.requestListing(productId)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/video/products/{productId}/refresh")
    @Log(title = "微信小店商品状态刷新", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult refreshVideoProduct(@PathVariable long productId)
    {
        try { return success(wechatShopProduct.refresh(productId)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @GetMapping("/video/orders")
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult videoOrders(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "50") int pageSize)
    {
        return success(wechatShopOrderSync.listOrders(pageNum, pageSize));
    }

    @PostMapping("/video/orders/sync")
    @Log(title = "微信小店订单同步", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult syncVideoOrders()
    {
        try { return success(wechatShopOrderSync.syncOnePage()); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/video/orders/{orderId}/ship")
    @Log(title = "微信小店订单发货", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult shipVideoOrder(@PathVariable String orderId, @RequestBody Map<String, Object> body)
    {
        try { return success(wechatShopOrderSync.shipOrder(orderId, text(body.get("deliveryId")), text(body.get("waybillId")))); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PutMapping("/video/orders/{orderId}/price")
    @Log(title = "微信小店订单改价", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult changeVideoOrderPrice(@PathVariable String orderId, @RequestBody Map<String, Object> body)
    {
        try { return success(wechatShopOrderSync.changeOrderPrice(orderId, body)); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @GetMapping("/video/aftersales")
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult videoAftersales(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "50") int pageSize)
    {
        return success(wechatShopAftersaleSync.list(pageNum, pageSize));
    }

    @GetMapping("/video/refund-ledger")
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult videoRefundLedger(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "50") int pageSize)
    {
        int page = integer(String.valueOf(pageNum), 1, 100000);
        int size = integer(String.valueOf(pageSize), 1, 200);
        long offset = (long) (page - 1) * size;
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT ledger_id AS ledgerId,external_aftersale_id AS externalAftersaleId,external_order_id AS externalOrderId,amount_cent AS amountCent,external_status AS externalStatus,ledger_status AS ledgerStatus,confirmed_at AS confirmedAt,last_error AS lastError,updated_at AS updatedAt FROM biz_video_refund_ledger ORDER BY updated_at DESC,ledger_id DESC LIMIT ? OFFSET ?", size, offset);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM biz_video_refund_ledger", Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows); result.put("total", total == null ? 0 : total); result.put("pageNum", page); result.put("pageSize", size);
        return success(result);
    }

    @PostMapping("/video/aftersales/sync")
    @Log(title = "微信小店售后同步", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult syncVideoAftersales()
    {
        try { return success(wechatShopAftersaleSync.syncOnePage()); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @GetMapping("/video/aftersales/reject-reasons")
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult videoAftersaleRejectReasons()
    {
        AjaxResult unavailable = videoShopUnavailable();
        if (unavailable != null) return unavailable;
        try { return success(wechatShopApi.getAftersaleRejectReasons().path("reason_list")); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    /**
     * External shop APIs must only be called after the operator has enabled the
     * channel and supplied the shop identity. This keeps local deployments
     * usable while making the missing production configuration explicit.
     */
    private AjaxResult videoShopUnavailable()
    {
        Map<String, Object> enabled = first("SELECT config_value FROM biz_channel_config WHERE config_key=?", "wechat.video.syncEnabled");
        if (enabled == null || !"true".equalsIgnoreCase(text(enabled.get("config_value"))))
        {
            return AjaxResult.error(503, "微信小店数据同步未开启，请先在渠道配置中开启同步");
        }
        Map<String, Object> shop = first("SELECT config_value FROM biz_channel_config WHERE config_key=?", "wechat.video.shopId");
        if (shop == null || text(shop.get("config_value")).isEmpty())
        {
            return AjaxResult.error(503, "未配置微信小店店铺 ID，请先完成渠道授权配置");
        }
        return null;
    }

    @PostMapping("/video/aftersales/{aftersaleId}/accept")
    @Log(title = "微信小店售后同意", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult acceptVideoAftersale(@PathVariable String aftersaleId, @RequestBody Map<String, Object> body)
    {
        try { wechatShopAftersaleSync.accept(aftersaleId, text(body.get("returnAddressId"))); return success(); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/video/aftersales/{aftersaleId}/reject")
    @Log(title = "微信小店售后拒绝", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult rejectVideoAftersale(@PathVariable String aftersaleId, @RequestBody Map<String, Object> body)
    {
        try { wechatShopAftersaleSync.reject(aftersaleId, integer(body.get("reasonType"), 0, Integer.MAX_VALUE), text(body.get("reason"))); return success(); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/video/check")
    @Log(title = "微信小店授权校验", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('business:channel:query')")
    public AjaxResult checkVideoShop()
    {
        Map<String, Object> configured = first("SELECT config_value FROM biz_channel_config WHERE config_key='wechat.video.shopId'");
        try
        {
            return success(wechatShopApi.checkShop(configured == null ? "" : text(configured.get("config_value"))));
        }
        catch (MiniBusinessException e)
        {
            return AjaxResult.error(e.getCode(), e.getMessage());
        }
    }

    @PostMapping("/group-qr")
    @Log(title = "企业微信群二维码", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult groupQr(@RequestBody Map<String, Object> body)
    {
        try { return success(workGroupQr.create(body == null ? null : body.get("chatIds"))); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @PostMapping("/group-qr/reconcile")
    @Log(title = "企业微信群旧码人工核验", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('business:channel:edit')")
    public AjaxResult reconcileGroupQr(@RequestBody Map<String, Object> body)
    {
        if (body == null) return AjaxResult.error(400, "请填写旧码配置 ID 和核验证据");
        try { return success(workGroupQr.confirmRevoked(text(body.get("configId")), text(body.get("evidence")))); }
        catch (MiniBusinessException e) { return AjaxResult.error(e.getCode(), e.getMessage()); }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@ss.hasPermi('business:dashboard:query')")
    public AjaxResult dashboard()
    {
        Set<String> visibleMetrics = DashboardMetricAccess.visibleMetrics(SecurityUtils.getLoginUser().getPermissions());
        boolean canSeeOrders = visibleMetrics.contains("orders");
        boolean canSeeFinance = visibleMetrics.contains("salesCent");
        boolean canSeeMembers = visibleMetrics.contains("members");
        boolean canSeeDraws = visibleMetrics.contains("draws");
        boolean canSeeRedemptions = visibleMetrics.contains("redemptions");
        Map<String, Object> result = new LinkedHashMap<>();
        if (canSeeOrders) result.put("orders", scalar("SELECT COUNT(*) FROM biz_order"));
        if (canSeeFinance) result.put("salesCent", scalar("SELECT COALESCE(SUM(GREATEST(o.total_cent-COALESCE(r.refunded_cent,0),0)),0) FROM biz_order o LEFT JOIN (SELECT order_id,SUM(amount_cent) AS refunded_cent FROM biz_order_refund WHERE status='refunded' GROUP BY order_id) r ON r.order_id=o.order_id WHERE o.status IN ('paid','shipping','receiving','complete','refunded')"));
        if (canSeeMembers) result.put("members", scalar("SELECT COUNT(*) FROM biz_member WHERE status='0'"));
        if (canSeeDraws) result.put("draws", scalar("SELECT COUNT(*) FROM biz_lottery_record"));
        if (canSeeRedemptions) result.put("redemptions", ((Number) scalar("SELECT COUNT(*) FROM biz_redemption WHERE status='confirmed'")).longValue() + ((Number) scalar("SELECT COUNT(*) FROM biz_order_pickup_redemption WHERE status='confirmed'")).longValue());
        if (canSeeFinance) result.put("commissionCent", scalar("SELECT COALESCE(SUM(amount_cent),0) FROM biz_promotion_record WHERE status IN ('pending','settled','processing','paid')"));
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int offset = 6; (canSeeOrders || canSeeFinance || canSeeDraws || canSeeRedemptions) && offset >= 0; offset--)
        {
            LocalDate day = today.minusDays(offset);
            java.sql.Timestamp from = java.sql.Timestamp.valueOf(day.atStartOfDay());
            java.sql.Timestamp to = java.sql.Timestamp.valueOf(day.plusDays(1).atStartOfDay());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day.toString());
            if (canSeeOrders) item.put("orders", scalar("SELECT COUNT(*) FROM biz_order WHERE create_time>=? AND create_time<?", from, to));
            if (canSeeFinance) item.put("salesCent", scalar("SELECT COALESCE(SUM(GREATEST(o.total_cent-COALESCE(r.refunded_cent,0),0)),0) FROM biz_order o LEFT JOIN (SELECT order_id,SUM(amount_cent) AS refunded_cent FROM biz_order_refund WHERE status='refunded' GROUP BY order_id) r ON r.order_id=o.order_id WHERE o.create_time>=? AND o.create_time<? AND o.status IN ('paid','shipping','receiving','complete','refunded')", from, to));
            if (canSeeDraws) item.put("draws", scalar("SELECT COUNT(*) FROM biz_lottery_record WHERE create_time>=? AND create_time<?", from, to));
            if (canSeeRedemptions) item.put("redemptions", ((Number) scalar("SELECT COUNT(*) FROM biz_redemption WHERE create_time>=? AND create_time<? AND status='confirmed'", from, to)).longValue() + ((Number) scalar("SELECT COUNT(*) FROM biz_order_pickup_redemption WHERE create_time>=? AND create_time<? AND status='confirmed'", from, to)).longValue());
            if (canSeeFinance) item.put("commissionCent", scalar("SELECT COALESCE(SUM(amount_cent),0) FROM biz_promotion_record WHERE create_time>=? AND create_time<? AND status IN ('pending','settled','processing','paid')", from, to));
            trend.add(item);
        }
        result.put("trend", trend);
        return success(result);
    }

    @GetMapping("/reports/{type}/export")
    @Log(title = "业务财务报表", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('business:report:export')")
    public void export(@PathVariable String type, @RequestParam Map<String, String> params,
            HttpServletResponse response) throws IOException
    {
        ReportDefinition report = reportDefinition(type, params, response);
        if (report == null) return;
        List<Map<String, Object>> rows = jdbc.queryForList(report.sql + report.order, report.args.toArray());
        String range = text(params.get("fromDate")) + "_" + text(params.get("toDate"));
        String fileName = BusinessReportLabels.reportName(type) + "-" + range + ".csv";
        jdbc.update("INSERT INTO biz_report_export(report_type, filters_json, file_name, row_count, create_by) VALUES(?,?,?,?,?)",
                type, json.writeValueAsString(params), fileName, rows.size(), getUsername());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"report.csv\"; filename*=UTF-8''"
                + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20"));
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(String.join(",", report.headers)).append('\n');
        for (Map<String, Object> row : rows)
        {
            boolean first = true;
            for (Map.Entry<String, Object> entry : row.entrySet())
            {
                if (!first) csv.append(',');
                first = false;
                csv.append(csv(BusinessReportLabels.display(type, entry.getKey(), entry.getValue())));
            }
            csv.append('\n');
        }
        response.getOutputStream().write(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/reports/{type}")
    @PreAuthorize("@ss.hasPermi('business:report:export')")
    public AjaxResult reportRows(@PathVariable String type, @RequestParam Map<String, String> params,
            HttpServletResponse response) throws IOException
    {
        ReportDefinition report = reportDefinition(type, params, response);
        if (report == null) return null;
        int pageNum = reportPage(params.get("pageNum"), 1, 1000000);
        int pageSize = reportPage(params.get("pageSize"), 20, 100);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + report.sql + ") report_count", Long.class, report.args.toArray());
        // Metadata keeps empty reports queryable with the same columns as their exports.
        List<Map<String, Object>> columns = jdbc.query(report.sql + " LIMIT 0", result -> {
            List<Map<String, Object>> fields = new ArrayList<>();
            java.sql.ResultSetMetaData metadata = result.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++)
            {
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("key", metadata.getColumnLabel(i));
                field.put("label", report.headers[i - 1]);
                fields.add(field);
            }
            return fields;
        }, report.args.toArray());
        List<Object> args = new ArrayList<>(report.args);
        args.add(pageSize); args.add((pageNum - 1) * pageSize);
        List<Map<String, Object>> rows = jdbc.queryForList(report.sql + report.order + " LIMIT ? OFFSET ?", args.toArray());
        for (Map<String, Object> row : rows)
            row.replaceAll((column, value) -> {
                Object displayed = BusinessReportLabels.display(type, column, value);
                return displayed == null ? "" : String.valueOf(displayed);
            });
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns); result.put("rows", rows); result.put("total", total);
        result.put("statusOptions", BusinessReportLabels.options(type, report.statusColumn));
        result.put("memberSearch", !"videoRefunds".equals(type));
        return success(result);
    }

    private ReportDefinition reportDefinition(String type, Map<String, String> params,
            HttpServletResponse response) throws IOException
    {
        if (!REPORTS.contains(type)) { response.sendError(400, "报表类型无效"); return null; }
        LocalDate from = parseReportDate(params.get("fromDate"), "开始日期", response);
        if (response.isCommitted()) return null;
        LocalDate to = parseReportDate(params.get("toDate"), "结束日期", response);
        if (response.isCommitted()) return null;
        if (from != null && to != null && from.isAfter(to)) { response.sendError(400, "开始日期不能晚于结束日期"); return null; }

        String sql; String dateColumn; String orderColumn; String[] headers;
        if ("orders".equals(type)) {
            sql = "SELECT o.order_id AS sort_order_id, i.order_item_id AS sort_item_id, o.order_no, "
                    + "COALESCE(m.member_no,'') AS member_no, COALESCE(m.nickname,'') AS member_name, "
                    + "COALESCE(i.product_name,'') AS product_name, COALESCE(i.product_spec,'') AS product_spec, COALESCE(i.quantity,0) AS sale_quantity, "
                    + "CAST(COALESCE(i.total_cent,0) / 100.0 AS DECIMAL(12,2)) AS product_amount_yuan, "
                    + reportOrderAmount("o.discount_cent") + " AS discount_yuan, "
                    + reportOrderAmount("o.shipping_cent") + " AS shipping_yuan, "
                    + reportOrderAmount("o.total_cent") + " AS order_payable_yuan, "
                    + reportOrderAmount("COALESCE(r.refunded_cent,0)") + " AS refunded_yuan, "
                    + reportOrderAmount("CASE WHEN o.status IN ('paid','shipping','receiving','complete','refunded') THEN GREATEST(o.total_cent-COALESCE(r.refunded_cent,0),0) ELSE 0 END") + " AS net_paid_yuan, "
                    + "o.status, o.create_time, o.fulfillment_mode, o.paid_at, o.wechat_transaction_id FROM biz_order o "
                    + "LEFT JOIN biz_member m ON m.member_id=o.member_id LEFT JOIN biz_order_item i ON i.order_id=o.order_id "
                    + "LEFT JOIN (SELECT order_id,SUM(amount_cent) AS refunded_cent FROM biz_order_refund WHERE status='refunded' GROUP BY order_id) r ON r.order_id=o.order_id WHERE 1=1";
            dateColumn = "o.create_time"; orderColumn = "o.order_id";
            headers = new String[] {"订单号","会员编号","用户名","商品名称","商品规格","销售数量","商品金额(元)","优惠金额(元)","运费(元)","订单应付(元)","已确认退款(元)","净已支付(元)","订单状态","下单时间","履约方式","支付时间","微信支付交易号"};
        }
        else if ("refunds".equals(type)) {
            sql = "SELECT r.refund_id, r.refund_no, r.channel_refund_id, o.order_no, o.wechat_transaction_id, COALESCE(m.member_no,'') AS member_no, COALESCE(m.nickname,'') AS member_name, r.amount_cent, r.status, r.channel_status, r.manual_payment_reference, COALESCE(NULLIF(u.nick_name,''),r.manual_verified_by,'') AS manual_verified_by, COALESCE(u.phonenumber,'') AS manual_verifier_phone, r.manual_paid_at, r.reason, r.request_id, r.create_time, r.update_time FROM biz_order_refund r JOIN biz_order o ON o.order_id=r.order_id LEFT JOIN biz_member m ON m.member_id=r.member_id LEFT JOIN sys_user u ON u.user_id=(SELECT su.user_id FROM sys_user su WHERE su.user_name=r.manual_verified_by ORDER BY su.del_flag ASC,su.user_id DESC LIMIT 1) WHERE 1=1";
            dateColumn = "r.create_time"; orderColumn = "r.refund_id";
            headers = new String[] {"退款记录ID","商户退款单号","微信退款单号","订单号","微信支付交易号","会员编号","用户名","申请退款(分)","业务状态","资金渠道状态","线下付款凭证号","线下确认人姓名","线下确认人手机号","线下付款时间","退款原因","请求号","申请时间","最后更新时间"};
        }
        else if ("returns".equals(type)) {
            sql = returnReportSql();
            dateColumn = "COALESCE(rr.received_at,a.received_at)"; orderColumn = "return_id";
            headers = new String[] {"收货流水ID","退款单号","订单号","会员编号","用户名","商品名称","商品规格","规格编码","原订单数量","退款金额(分)","实际收到数量","可售入库数量","不入库数量","收货凭证","请求号","操作人姓名","操作人手机号","收货时间","入库来源","售后单号","售后类型"};
        }
        else if ("commissions".equals(type)) { sql = "SELECT p.promotion_record_id, COALESCE(m.member_no,'') AS promoter_no, COALESCE(m.nickname,'') AS promoter_name, p.order_id, p.title, p.amount_cent, p.original_amount_cent, p.refund_adjustment_cent, p.refund_recovery_required, p.status, p.create_time FROM biz_promotion_record p LEFT JOIN biz_member m ON m.member_id=p.member_id WHERE 1=1"; dateColumn = "p.create_time"; orderColumn = "p.promotion_record_id"; headers = new String[] {"记录ID","推广会员编号","推广会员用户名","订单ID","标题","当前佣金(分)","原始佣金(分)","退款冲回(分)","已提现待追偿","状态","创建时间"}; }
        else if ("settlements".equals(type)) { sql = "SELECT s.settlement_id, COALESCE(m.member_no,'') AS promoter_no, COALESCE(m.nickname,'') AS promoter_name, s.amount_cent, s.status, s.request_id, s.payment_reference, s.failure_reason, s.paid_at, s.create_time FROM biz_promotion_settlement s LEFT JOIN biz_member m ON m.member_id=s.member_id WHERE 1=1"; dateColumn = "s.create_time"; orderColumn = "s.settlement_id"; headers = new String[] {"提现单号","推广会员编号","推广会员用户名","金额(分)","状态","请求号","付款凭证号","失败原因","付款时间","申请时间"}; }
        else if ("lottery".equals(type)) { sql = "SELECT l.record_id, l.activity_id, COALESCE(m.member_no,'') AS member_no, COALESCE(m.nickname,'') AS member_name, l.result_type, l.result_value, l.request_id, l.create_time FROM biz_lottery_record l LEFT JOIN biz_member m ON m.member_id=l.member_id WHERE 1=1"; dateColumn = "l.create_time"; orderColumn = "l.record_id"; headers = new String[] {"记录ID","活动ID","会员编号","用户名","结果类型","结果","请求号","创建时间"}; }
        else if ("rankingRewards".equals(type)) { sql = "SELECT r.reward_id, r.period_start, r.period_end, r.rank_no, COALESCE(m.member_no,'') AS member_no, COALESCE(m.nickname,'') AS member_name, r.points, r.subsidy_cent, r.status, r.issue_reference, r.issued_at FROM biz_ranking_reward r LEFT JOIN biz_member m ON m.member_id=r.member_id WHERE 1=1"; dateColumn = "r.period_start"; orderColumn = "r.reward_id"; headers = new String[] {"奖励ID","周期开始","周期结束","排名","会员编号","用户名","奖励积分","补贴(分)","状态","发放凭证","发放时间"}; }
        else if ("videoRefunds".equals(type)) { sql = "SELECT ledger_id, external_aftersale_id, external_order_id, amount_cent, external_status, ledger_status, confirmed_at, updated_at FROM biz_video_refund_ledger WHERE 1=1"; dateColumn = "updated_at"; orderColumn = "ledger_id"; headers = new String[] {"台账ID","微信售后单号","微信订单号","退款金额(分)","平台状态","对账状态","确认时间","更新时间"}; }
        else { sql = "SELECT redemption.redemption_type, redemption.id, redemption.business_ref, COALESCE(m.member_no,'') AS member_no, COALESCE(m.nickname,'') AS member_name, redemption.store_id, COALESCE(v.member_no,'') AS verifier_no, COALESCE(v.nickname,'') AS verifier_name, redemption.status, redemption.request_id, redemption.created_at FROM (SELECT CONVERT('抽奖奖品' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS redemption_type, r.redemption_id AS id, CONVERT(CAST(r.reward_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci AS business_ref, r.member_id, r.store_id, r.verifier_member_id, CONVERT(r.status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS status, CONVERT(r.request_id USING utf8mb4) COLLATE utf8mb4_unicode_ci AS request_id, r.create_time AS created_at FROM biz_redemption r UNION ALL SELECT CONVERT('商城自提' USING utf8mb4) COLLATE utf8mb4_unicode_ci AS redemption_type, p.pickup_redemption_id AS id, CONVERT(p.order_no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS business_ref, o.member_id, p.store_id, p.verifier_member_id, CONVERT(p.status USING utf8mb4) COLLATE utf8mb4_unicode_ci AS status, CONVERT(p.request_id USING utf8mb4) COLLATE utf8mb4_unicode_ci AS request_id, p.create_time AS created_at FROM biz_order_pickup_redemption p JOIN biz_order o ON o.order_id=p.order_id) redemption LEFT JOIN biz_member m ON m.member_id=redemption.member_id LEFT JOIN biz_member v ON v.member_id=redemption.verifier_member_id WHERE 1=1"; dateColumn = "redemption.created_at"; orderColumn = "redemption.id"; headers = new String[] {"核销类型","核销ID","订单号/奖品ID","会员编号","用户名","门店ID","核销员会员编号","核销员用户名","状态","请求号","核销时间"}; }

        List<Object> args = new ArrayList<>();
        if (from != null) { sql += " AND " + dateColumn + " >= ?"; args.add(java.sql.Timestamp.valueOf(from.atStartOfDay())); }
        if (to != null) { sql += " AND " + dateColumn + " < ?"; args.add(java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay())); }
        for (int i = 0; i < headers.length; i++) headers[i] = headers[i].replace("(分)", "(元)");
        String order;
        if ("orders".equals(type))
        {
            order = " ORDER BY sort_order_id DESC, sort_item_id ASC";
        }
        else if ("redemptions".equals(type)) order = " ORDER BY created_at DESC, redemption_type ASC, id DESC";
        else if ("returns".equals(type)) order = " ORDER BY received_at DESC, sort_return_source ASC, sort_return_id DESC";
        else order = " ORDER BY " + orderColumn.substring(orderColumn.lastIndexOf('.') + 1) + " DESC";

        String statusColumn = "lottery".equals(type) ? "result_type" : "videoRefunds".equals(type) ? "ledger_status" : "returns".equals(type) ? "" : "status";
        String memberColumn = Arrays.asList("commissions", "settlements").contains(type) ? "promoter_no" : "member_no";
        String nameColumn = "promoter_no".equals(memberColumn) ? "promoter_name" : "member_name";
        String[] searchColumns = reportSearchColumns(type);
        String projection = "orders".equals(type)
                ? "order_no,member_no,member_name,product_name,product_spec,sale_quantity,product_amount_yuan,discount_yuan,shipping_yuan,order_payable_yuan,refunded_yuan,net_paid_yuan,status,create_time,fulfillment_mode,paid_at,wechat_transaction_id"
                : "returns".equals(type)
                ? "receipt_id,refund_no,order_no,member_no,member_name,product_name,product_spec,sku_code,order_quantity,refund_amount_cent,received_quantity,restock_quantity,nonrestock_quantity,receipt_reference,request_id,received_by,receiver_phone,received_at,return_source,aftersale_no,aftersale_type"
                : "report_data.*";
        sql = "SELECT " + projection + " FROM (" + sql + ") report_data WHERE 1=1";
        String memberNo = reportFilter(params, "memberNo");
        String memberName = reportFilter(params, "memberName");
        if (!"videoRefunds".equals(type))
        {
            if (!memberNo.isEmpty()) { sql += " AND " + memberColumn + " = ?"; args.add(memberNo); }
            if (!memberName.isEmpty()) { sql += " AND " + nameColumn + " LIKE ? ESCAPE '!'"; args.add(reportLike(memberName)); }
        }
        String status = reportFilter(params, "status");
        if (!status.isEmpty())
        {
            if (!BusinessReportLabels.options(type, statusColumn).containsKey(status))
                throw new IllegalArgumentException("请选择有效的报表状态");
            if ("orders".equals(type) && "shipping".equals(status)) sql += " AND status IN ('shipping','receiving')";
            else if ("rankingRewards".equals(type) && "issued".equals(status)) sql += " AND status IN ('issued','paid')";
            else { sql += " AND " + statusColumn + " = ?"; args.add(status); }
        }
        String keyword = reportFilter(params, "keyword");
        if (!keyword.isEmpty())
        {
            List<String> conditions = new ArrayList<>();
            for (String column : searchColumns) { conditions.add(column + " LIKE ? ESCAPE '!'"); args.add(reportLike(keyword)); }
            sql += " AND (" + String.join(" OR ", conditions) + ")";
        }
        return new ReportDefinition(sql, order, headers, args, statusColumn);
    }

    private static String reportOrderAmount(String expression)
    {
        // Order totals occur once, independent of page boundaries.
        return "CAST((CASE WHEN i.order_item_id IS NULL OR i.order_item_id=(SELECT MIN(fi.order_item_id) FROM biz_order_item fi WHERE fi.order_id=o.order_id) THEN "
                + expression + " ELSE 0 END) / 100.0 AS DECIMAL(12,2))";
    }

    private static String returnReportSql()
    {
        // Join each receipt source after the numeric union so legacy table collations do not conflict.
        return "SELECT returns_data.return_id AS sort_return_id,returns_data.return_source AS sort_return_source,"
                + "CASE WHEN returns_data.return_source=0 THEN CAST(returns_data.return_id AS CHAR) ELSE CONCAT('AS',returns_data.return_id) END AS receipt_id,"
                + "COALESCE(r.refund_no,'') AS refund_no,o.order_no,COALESCE(m.member_no,'') AS member_no,COALESCE(m.nickname,'') AS member_name,"
                + "i.product_name,i.product_spec,i.sku_code,i.quantity AS order_quantity,"
                + "CASE WHEN returns_data.return_source=0 THEN r.amount_cent ELSE a.amount_cent END AS refund_amount_cent,"
                + "COALESCE(rr.quantity,a.quantity) AS received_quantity,COALESCE(rr.restock_quantity,a.restock_quantity) AS restock_quantity,"
                + "COALESCE(rr.quantity,a.quantity)-COALESCE(rr.restock_quantity,a.restock_quantity) AS nonrestock_quantity,"
                + "CAST(COALESCE(rr.receipt_reference,a.receipt_reference,'') AS CHAR(128)) AS receipt_reference,CAST(COALESCE(rr.request_id,a.request_id) AS CHAR(64)) AS request_id,"
                + "CAST(COALESCE(NULLIF(u.nick_name,''),rr.received_by,a.received_by,'') AS CHAR(128)) AS received_by,COALESCE(u.phonenumber,'') AS receiver_phone,"
                + "COALESCE(rr.received_at,a.received_at) AS received_at,returns_data.return_source,COALESCE(a.aftersale_no,'') AS aftersale_no,"
                + "COALESCE(a.aftersale_type,'legacy') AS aftersale_type "
                + "FROM (SELECT receipt_id AS return_id,0 AS return_source,order_id,order_item_id,refund_id FROM biz_order_return_receipt "
                + "UNION ALL SELECT aftersale_id AS return_id,1 AS return_source,order_id,order_item_id,refund_id FROM biz_order_aftersale WHERE received_at IS NOT NULL) returns_data "
                + "LEFT JOIN biz_order_return_receipt rr ON returns_data.return_source=0 AND rr.receipt_id=returns_data.return_id "
                + "LEFT JOIN biz_order_aftersale a ON returns_data.return_source=1 AND a.aftersale_id=returns_data.return_id "
                + "LEFT JOIN biz_order_refund r ON r.refund_id=returns_data.refund_id JOIN biz_order o ON o.order_id=returns_data.order_id "
                + "JOIN biz_order_item i ON i.order_item_id=returns_data.order_item_id LEFT JOIN biz_member m ON m.member_id=o.member_id "
                + "LEFT JOIN sys_user u ON u.user_id=(SELECT su.user_id FROM sys_user su WHERE CAST(su.user_name AS CHAR(256))=CAST(COALESCE(rr.received_by,a.received_by) AS CHAR(256)) ORDER BY su.del_flag ASC,su.user_id DESC LIMIT 1) WHERE 1=1";
    }

    private static String[] reportSearchColumns(String type)
    {
        switch (type)
        {
            case "orders": return new String[] {"order_no", "product_name", "product_spec", "wechat_transaction_id"};
            case "refunds": return new String[] {"order_no", "refund_no", "channel_refund_id", "reason", "manual_payment_reference"};
            case "returns": return new String[] {"order_no", "refund_no", "product_name", "product_spec", "receipt_reference", "aftersale_no"};
            case "commissions": return new String[] {"title", "order_id"};
            case "settlements": return new String[] {"settlement_id", "payment_reference", "failure_reason"};
            case "lottery": return new String[] {"record_id", "activity_id", "result_value"};
            case "rankingRewards": return new String[] {"issue_reference", "period_start", "period_end"};
            case "videoRefunds": return new String[] {"external_aftersale_id", "external_order_id"};
            default: return new String[] {"business_ref", "redemption_type", "verifier_no", "verifier_name"};
        }
    }

    private static String reportFilter(Map<String, String> params, String name)
    {
        String value = text(params.get(name));
        if (value.length() > 128) throw new IllegalArgumentException("查询内容不能超过128个字符");
        return value;
    }

    private static String reportLike(String value)
    {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static int reportPage(String value, int fallback, int maximum)
    {
        if (text(value).isEmpty()) return fallback;
        try { int page = Integer.parseInt(value); if (page >= 1 && page <= maximum) return page; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("分页参数无效");
    }

    private static final class ReportDefinition
    {
        final String sql, order, statusColumn;
        final String[] headers;
        final List<Object> args;
        ReportDefinition(String sql, String order, String[] headers, List<Object> args, String statusColumn)
        {
            this.sql = sql; this.order = order; this.headers = headers; this.args = args; this.statusColumn = statusColumn;
        }
    }

    private static LocalDate parseReportDate(String value, String label, HttpServletResponse response) throws IOException
    {
        if (value == null || value.trim().isEmpty()) return null;
        try { return LocalDate.parse(value.trim()); }
        catch (java.time.format.DateTimeParseException e) { response.sendError(400, label + "格式应为 YYYY-MM-DD"); return null; }
    }

    private TableDataInfo page(String sql, Map<String, String> params)
    {
        int page = integer(params.get("pageNum"), 1, 100000), size = integer(params.get("pageSize"), 10, 200);
        List<Map<String, Object>> all = jdbc.queryForList(sql);
        int from = Math.min((page - 1) * size, all.size()), to = Math.min(from + size, all.size());
        TableDataInfo result = new TableDataInfo(all.subList(from, to), all.size()); result.setCode(200); result.setMsg("查询成功"); return result;
    }

    private Map<String, Object> first(String sql, Object... args) { List<Map<String, Object>> rows = jdbc.queryForList(sql, args); return rows.isEmpty() ? null : rows.get(0); }
    private static String rankingRewardsError(com.fasterxml.jackson.databind.JsonNode rewards)
    {
        if (rewards == null || !rewards.isArray() || rewards.size() == 0) return "请配置至少一个名次奖励";
        Set<Integer> ranks = new HashSet<>();
        for (com.fasterxml.jackson.databind.JsonNode reward : rewards) {
            for (String field : Arrays.asList("rank", "points", "subsidyCent")) {
                com.fasterxml.jackson.databind.JsonNode value = reward.path(field);
                if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0) return "名次、积分和补贴分值必须为有效整数";
            }
            int rank = reward.path("rank").intValue();
            if (rank < 1 || rank > 100 || !ranks.add(rank)) return "奖励名次必须在 1 至 100 之间且不能重复";
        }
        return null;
    }

    private static LocalDateTime[] rankingIssuePeriod(Map<String, Object> config, String periodDate)
    {
        String type = text(config.get("periodType"));
        LocalDateTime start = localDateTime(config.get("startTime"));
        LocalDateTime end = localDateTime(config.get("endTime"));
        if ((start == null) != (end == null)) throw new IllegalArgumentException("请先补全排行榜自定义开始与结束时间");
        if (start == null) {
            LocalDate today;
            try { today = LocalDate.parse(periodDate); }
            catch (Exception e) { throw new IllegalArgumentException("请选择待结算周期内的日期，格式为 YYYY-MM-DD"); }
            if ("week".equals(type)) start = today.with(DayOfWeek.MONDAY).atStartOfDay();
            else if ("quarter".equals(type)) start = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1).atStartOfDay();
            else start = today.withDayOfMonth(1).atStartOfDay();
        }
        if (end == null) end = "week".equals(type) ? start.plusWeeks(1) : "quarter".equals(type) ? start.plusMonths(3) : start.plusMonths(1);
        if (!start.isBefore(end)) throw new IllegalArgumentException("排行榜结束时间必须晚于开始时间");
        return new LocalDateTime[] { start, end };
    }
    private static LocalDateTime localDateTime(Object value)
    {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime();
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof java.util.Date) return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), java.time.ZoneId.systemDefault());
        try { return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T')); } catch (Exception ignored) { return null; }
    }
    private static LocalDateTime parseOptionalDateTime(Object value, String label)
    {
        if (value == null || text(value).isEmpty()) return null;
        LocalDateTime parsed = localDateTime(value);
        if (parsed == null) throw new IllegalArgumentException(label + "格式无效，应为 YYYY-MM-DD HH:mm:ss");
        return parsed;
    }
    private int probabilityTotal(long excludeId, long activityId) { Integer value = jdbc.queryForObject("SELECT COALESCE(SUM(probability_bp),0) FROM biz_lottery_prize WHERE activity_id=? AND prize_id<>? AND status='0'", Integer.class, activityId, excludeId); return value == null ? 0 : value; }
    private boolean activeStore(long storeId) { return first("SELECT store_id FROM biz_store WHERE store_id=? AND status='0' FOR UPDATE", storeId) != null; }
    private boolean activeCouponTemplate(long templateId) { return first("SELECT coupon_template_id FROM biz_coupon_template WHERE coupon_template_id=? AND status='0' AND (start_time IS NULL OR start_time<=NOW()) AND (end_time IS NULL OR end_time>NOW()) FOR UPDATE", templateId) != null; }
    private static Long nullableLong(Object value) { if (value == null || text(value).isEmpty()) return null; try { long id = Long.parseLong(text(value)); return id > 0 ? id : null; } catch (Exception e) { throw new IllegalArgumentException("门店 ID 无效"); } }
    private Object scalar(String sql, Object... args) { return jdbc.queryForObject(sql, Object.class, args); }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String cashAmountRules(Object value)
    {
        if (value == null || text(value).isEmpty()) return "";
        try
        {
            List<Map<String, Object>> rules = json.readValue(value instanceof String ? text(value) : json.writeValueAsString(value), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            if (rules == null || rules.isEmpty() || rules.size() > 50) throw new IllegalArgumentException();
            int total = 0;
            for (Map<String, Object> rule : rules)
            {
                int amount = integer(rule.get("amountCent"), 0, Integer.MAX_VALUE);
                int count = integer(rule.get("count"), 0, 1000000);
                if (amount < 1 || count < 1 || (total += count) > 1000000) throw new IllegalArgumentException();
                rule.clear(); rule.put("amountCent", amount); rule.put("count", count);
            }
            return json.writeValueAsString(rules);
        }
        catch (Exception e) { throw new IllegalArgumentException("固定红包金额和数量配置无效，金额最低为0.01元"); }
    }
    private List<Map<String, Object>> parseCashAmountRules(Object value)
    {
        if (value == null || text(value).isEmpty()) return Collections.emptyList();
        try { return json.readValue(text(value), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}); }
        catch (Exception e) { return Collections.emptyList(); }
    }
    private static String status(Object value) { return "1".equals(text(value)) ? "1" : "0"; }
    private static long longValue(Object value) { try { return Long.parseLong(text(value)); } catch (Exception e) { throw new IllegalArgumentException("ID无效"); } }
    private static int integer(Object value, int fallback, int max) { try { int n = Integer.parseInt(text(value)); return n < fallback ? fallback : Math.min(n, max); } catch (Exception e) { return fallback; } }
    private static boolean nonNegativeInt(Object value) { try { long number = Long.parseLong(text(value)); return number >= 0 && number <= Integer.MAX_VALUE; } catch (Exception e) { return false; } }
    private String jsonText(Object value) { if (value == null || text(value).isEmpty()) return "{}"; if (value instanceof String) return text(value); try { return json.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalArgumentException("JSON格式无效"); } }
    private static String csv(Object value)
    {
        String raw = value == null ? "" : String.valueOf(value);
        if (value instanceof String && !raw.isEmpty())
        {
            int first = 0;
            while (first < raw.length() && Character.isWhitespace(raw.charAt(first))) first++;
            if ("\t\r\n".indexOf(raw.charAt(0)) >= 0
                    || first < raw.length() && "=+-@".indexOf(raw.charAt(first)) >= 0) raw = "'" + raw;
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }
}
