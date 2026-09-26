package com.ruoyi.web.controller.business;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.MiniResult;

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

    @Value("${mini.wechat.pay-enabled:false}")
    private boolean payEnabled;

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
    public MiniResult<?> login(@RequestBody WechatLogin request)
    {
        return MiniResult.success(auth.login(request.code));
    }

    @GetMapping("/me")
    public MiniResult<?> me(HttpServletRequest request)
    {
        return MiniResult.success(auth.member(auth.requireMember(request)));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/me")
    public MiniResult<?> updateMe(HttpServletRequest request, @RequestBody ProfileInput input)
    {
        long memberId = auth.requireMember(request);
        commerce.updateProfile(memberId, input == null ? null : input.name);
        return MiniResult.success(auth.member(memberId));
    }

    @GetMapping("/addresses")
    public MiniResult<?> addresses(HttpServletRequest request)
    {
        return MiniResult.success(commerce.addresses(auth.requireMember(request)));
    }

    @PostMapping("/addresses")
    public MiniResult<?> createAddress(HttpServletRequest request, @RequestBody AddressInput input)
    {
        return MiniResult.success(commerce.saveAddress(auth.requireMember(request), null, input));
    }

    @PatchMapping("/addresses/{id}")
    public MiniResult<?> updateAddress(HttpServletRequest request, @PathVariable long id, @RequestBody AddressInput input)
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
            @RequestBody RequestKey input)
    {
        return MiniResult.success(commerce.claim(auth.requireMember(request), templateCode, input.requestId));
    }

    @PostMapping("/orders/quote")
    public MiniResult<?> quote(HttpServletRequest request, @RequestBody OrderInput input)
    {
        return MiniResult.success(commerce.quote(auth.requireMember(request), input));
    }

    @GetMapping("/orders")
    public MiniResult<?> orders(HttpServletRequest request)
    {
        return MiniResult.success(commerce.orders(auth.requireMember(request)));
    }

    @PostMapping("/orders")
    public MiniResult<?> createOrder(HttpServletRequest request, @RequestBody OrderInput input)
    {
        return MiniResult.success(commerce.createOrder(auth.requireMember(request), input));
    }

    @GetMapping("/orders/{id}")
    public MiniResult<?> order(HttpServletRequest request, @PathVariable String id)
    {
        return MiniResult.success(commerce.order(auth.requireMember(request), id));
    }

    @PostMapping("/orders/{id}/cancel")
    public MiniResult<?> cancel(HttpServletRequest request, @PathVariable String id)
    {
        return MiniResult.success(commerce.cancel(auth.requireMember(request), id));
    }

    @PostMapping("/orders/{id}/receive")
    public MiniResult<?> receive(HttpServletRequest request, @PathVariable String id)
    {
        return MiniResult.success(commerce.receive(auth.requireMember(request), id));
    }

    @PostMapping("/orders/{id}/pay")
    public MiniResult<?> pay(HttpServletRequest request, @PathVariable String id)
    {
        commerce.order(auth.requireMember(request), id);
        if (!payEnabled)
        {
            throw new MiniBusinessException(503, "微信支付商户凭据尚未配置，无法发起真实支付");
        }
        // 支付签名、商户证书和回调验签需在正式商户参数就绪后开启；未实现前绝不返回伪造参数。
        throw new MiniBusinessException(503, "微信支付签名服务尚未接入，请联系运营配置");
    }

    public static class WechatLogin
    {
        /** 微信 wx.login 返回的一次性 code。 */
        public String code;
    }

    public static class RequestKey
    {
        /** 客户端生成、服务端幂等使用的请求号。 */
        public String requestId;
    }

    public static class AddressInput
    {
        /** 收货人姓名。 */
        public String name;
        /** 收货人手机号。 */
        public String phone;
        /** 小程序页面使用的地区文本。 */
        public String region;
        /** 详细地址。 */
        public String detail;
        /** 是否设为默认地址。 */
        public Boolean isDefault;
    }

    public static class ProfileInput
    {
        public String name;
    }

    public static class OrderInput
    {
        /** 商品编码，而非数据库自增 ID。 */
        public String productId;
        /** 购买数量。 */
        public Integer quantity;
        /** 会员券 ID，可为空。 */
        public String couponId;
        /** 会员地址 ID。 */
        public String addressId;
        /** 创建订单必须提供的幂等请求号。 */
        public String requestId;
    }
}
