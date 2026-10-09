package com.ruoyi.web.controller.business;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.MiniResult;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.web.core.storage.MinioStorageService;

/** 小程序交易接口；每个非登录接口均自行校验会员会话。 */
@Anonymous
@RestController
@RequestMapping("/mini")
public class MiniCommerceController
{
    @Autowired
    private MiniMemberAuthService auth;

    @Autowired
    private MiniCommerceService commerce;

    @Autowired
    private WechatPayService wechatPay;

    @Autowired
    private MinioStorageService storage;

    private static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    @ExceptionHandler(MiniBusinessException.class)
    public MiniResult<?> businessError(MiniBusinessException exception)
    {
        return MiniResult.error(exception.getCode(), exception.getMessage());
    }

    /** 未预期异常也保持小程序响应格式，避免客户端混用后台错误结构。 */
    @ExceptionHandler(Exception.class)
    public MiniResult<?> systemError(Exception exception)
    {
        return MiniResult.error(500, "服务暂不可用");
    }

    @PostMapping("/auth/wechat")
    public MiniResult<?> login(@RequestBody(required = false) WechatLogin request)
    {
        if (request == null) throw new MiniBusinessException(400, "微信登录参数无效");
        return MiniResult.success(auth.login(request.code, request.inviteCode));
    }

    @GetMapping("/me")
    public MiniResult<?> me(HttpServletRequest request)
    {
        return MiniResult.success(auth.member(auth.requireMember(request)));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/me")
    public MiniResult<?> updateMe(HttpServletRequest request, @RequestBody(required = false) ProfileInput input)
    {
        long memberId = auth.requireMember(request);
        commerce.updateProfile(memberId, input == null ? null : input.name, input == null ? null : input.phone);
        return MiniResult.success(auth.member(memberId));
    }

    @PostMapping("/me/avatar")
    public MiniResult<?> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file)
            throws Exception
    {
        long memberId = auth.requireMember(request);
        if (file == null || file.isEmpty() || file.getSize() > MAX_AVATAR_BYTES)
        {
            throw new MiniBusinessException(400, "请选择不超过 2MB 的头像图片");
        }
        String path = storage.isEnabled()
                ? storage.upload(file, "upload", MimeTypeUtils.IMAGE_EXTENSION)
                : FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file, MimeTypeUtils.IMAGE_EXTENSION, true);
        commerce.updateAvatar(memberId, path);
        return MiniResult.success(auth.member(memberId));
    }

    @GetMapping("/addresses")
    public MiniResult<?> addresses(HttpServletRequest request)
    {
        return MiniResult.success(commerce.addresses(auth.requireMember(request)));
    }

    @PostMapping("/addresses")
    public MiniResult<?> createAddress(HttpServletRequest request, @RequestBody(required = false) AddressInput input)
    {
        return MiniResult.success(commerce.saveAddress(auth.requireMember(request), null, input));
    }

    @PatchMapping("/addresses/{id}")
    public MiniResult<?> updateAddress(HttpServletRequest request, @PathVariable long id, @RequestBody(required = false) AddressInput input)
    {
        return MiniResult.success(commerce.saveAddress(auth.requireMember(request), id, input));
    }

    @DeleteMapping("/addresses/{id}")
    public MiniResult<?> deleteAddress(HttpServletRequest request, @PathVariable long id)
    {
        commerce.deleteAddress(auth.requireMember(request), id);
        return MiniResult.success(true);
    }

    @GetMapping("/coupons")
    public MiniResult<?> coupons(HttpServletRequest request)
    {
        return MiniResult.success(commerce.coupons(auth.requireMember(request)));
    }

    @PostMapping("/coupons/{templateCode}/claim")
    public MiniResult<?> claim(HttpServletRequest request, @PathVariable String templateCode,
            @RequestBody(required = false) RequestKey input)
    {
        if (input == null) throw new MiniBusinessException(400, "领券请求参数无效");
        return MiniResult.success(commerce.claim(auth.requireMember(request), templateCode, input.requestId));
    }

    @PostMapping("/orders/quote")
    public MiniResult<?> quote(HttpServletRequest request, @RequestBody(required = false) OrderInput input)
    {
        if (input == null)
        {
            throw new MiniBusinessException(400, "报价参数不能为空");
        }
        return MiniResult.success(commerce.quote(auth.requireMember(request), input));
    }

    @GetMapping("/orders")
    public MiniResult<?> orders(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(commerce.orders(auth.requireMember(request), params));
    }

    @PostMapping("/orders")
    public MiniResult<?> createOrder(HttpServletRequest request, @RequestBody(required = false) OrderInput input)
    {
        return MiniResult.success(commerce.createOrder(auth.requireMember(request), input));
    }

    @GetMapping("/orders/{id}")
    public MiniResult<?> order(HttpServletRequest request, @PathVariable String id)
    {
        return MiniResult.success(commerce.order(auth.requireMember(request), id));
    }

    @GetMapping("/orders/{id}/pickup-code")
    public MiniResult<?> pickupCode(HttpServletRequest request, @PathVariable String id)
    {
        return MiniResult.success(commerce.pickupCode(auth.requireMember(request), id));
    }

    @PostMapping("/staff/orders/lookup")
    public MiniResult<?> lookupPickupOrder(HttpServletRequest request, @RequestBody(required = false) OrderCodeInput input)
    {
        if (input == null || input.orderNo == null || input.orderNo.trim().isEmpty())
            throw new MiniBusinessException(400, "订单核销码参数无效");
        return MiniResult.success(commerce.lookupPickupOrder(auth.requireMember(request), input == null ? null : input.orderNo));
    }

    @GetMapping("/staff/orders")
    public MiniResult<?> pickupRedemptions(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(commerce.pickupRedemptions(auth.requireMember(request), params));
    }

    @PostMapping("/staff/orders/confirm")
    public MiniResult<?> confirmPickupOrder(HttpServletRequest request, @RequestBody(required = false) OrderCodeInput input)
    {
        if (input == null || input.orderNo == null || input.orderNo.trim().isEmpty())
            throw new MiniBusinessException(400, "订单核销码参数无效");
        return MiniResult.success(commerce.confirmPickupOrder(auth.requireMember(request), input == null ? null : input.orderNo, request.getHeader("Idempotency-Key")));
    }

    @PostMapping("/orders/{id}/cancel")
    public MiniResult<?> cancel(HttpServletRequest request, @PathVariable String id, @RequestBody(required = false) RequestKey input)
    {
        return MiniResult.success(commerce.cancel(auth.requireMember(request), id, requestId(input, request)));
    }

    @PostMapping("/orders/{id}/receive")
    public MiniResult<?> receive(HttpServletRequest request, @PathVariable String id, @RequestBody(required = false) RequestKey input)
    {
        return MiniResult.success(commerce.receive(auth.requireMember(request), id, requestId(input, request)));
    }

    private static String requestId(RequestKey input, HttpServletRequest request)
    {
        return input != null && input.requestId != null ? input.requestId : request.getHeader("Idempotency-Key");
    }

    @PostMapping("/orders/{id}/pay")
    public MiniResult<?> pay(HttpServletRequest request, @PathVariable String id)
    {
        long memberId = auth.requireMember(request);
        commerce.order(memberId, id);
        return MiniResult.success(wechatPay.createJsapiPay(memberId, id, request.getHeader("Idempotency-Key")));
    }

    @PostMapping("/orders/{id}/pay-sync")
    public MiniResult<?> syncPayment(HttpServletRequest request, @PathVariable String id)
    {
        long memberId = auth.requireMember(request);
        String tradeState = wechatPay.syncPaymentStatus(memberId, id);
        Map<String, Object> order = commerce.order(memberId, id);
        order.put("wechatTradeState", tradeState);
        return MiniResult.success(order);
    }

    public static class WechatLogin
    {
        /** 微信 wx.login 返回的一次性 code。 */
        public String code;
        /** 可选推广码；服务端只在登录后绑定有效上级关系。 */
        public String inviteCode;
    }

    public static class RequestKey
    {
        /** 客户端生成、服务端幂等使用的请求号。 */
        public String requestId;
    }

    public static class OrderCodeInput { public String orderNo; }

    public static class AddressInput
    {
        /** 收货人姓名。 */
        public String name;
        /** 收货人手机号。 */
        public String phone;
        /** 小程序页面使用的地区文本。 */
        public String region;
        public String province;
        public String city;
        public String district;
        /** 详细地址。 */
        public String detail;
        /** 是否设为默认地址。 */
        public Boolean isDefault;
    }

    public static class ProfileInput
    {
        public String name;
        public String phone;
    }

    public static class OrderInput
    {
        /** 商品编码，而非数据库自增 ID。 */
        public String productId;
        /** 可选商品规格编码；空值使用商品基础库存与价格。 */
        public String skuCode;
        /** 购买数量。 */
        public Integer quantity;
        /** 会员券 ID，可为空。 */
        public String couponId;
        /** 会员地址 ID。 */
        public String addressId;
        /** 创建订单必须提供的幂等请求号。 */
        public String requestId;
        /** delivery 保持平台配送；pickup 为指定合作门店自提。 */
        public String fulfillmentMode;
        /** 自提门店 ID，仅 pickup 使用。 */
        public String pickupStoreId;
    }
}
