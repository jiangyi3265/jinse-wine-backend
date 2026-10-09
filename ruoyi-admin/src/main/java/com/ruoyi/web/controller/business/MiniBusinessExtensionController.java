package com.ruoyi.web.controller.business;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.MiniResult;

/** 小程序扩展业务接口，写操作均由会员会话和服务端事务校验。 */
@Anonymous
@RestController
@RequestMapping("/mini")
public class MiniBusinessExtensionController
{
    private final MiniMemberAuthService auth;
    private final MiniBusinessExtensionService service;
    private final WechatSocialDeliveryService socialDelivery;

    public MiniBusinessExtensionController(MiniMemberAuthService auth, MiniBusinessExtensionService service, WechatSocialDeliveryService socialDelivery)
    {
        this.auth = auth; this.service = service; this.socialDelivery = socialDelivery;
    }

    @ExceptionHandler(MiniBusinessException.class)
    public MiniResult<?> businessError(MiniBusinessException e) { return MiniResult.error(e.getCode(), e.getMessage()); }

    @GetMapping("/cart")
    public MiniResult<?> cart(HttpServletRequest request) { return MiniResult.success(service.cart(auth.requireMember(request))); }

    @PostMapping("/cart")
    public MiniResult<?> addCart(HttpServletRequest request, @RequestBody(required = false) CartInput input) {
        if (input == null) throw new MiniBusinessException(400, "购物车参数无效");
        return MiniResult.success(service.addCart(auth.requireMember(request), input.productId, input.skuCode, input.quantity == null ? 1 : input.quantity));
    }

    @DeleteMapping("/cart/{id}")
    public MiniResult<?> removeCart(HttpServletRequest request, @PathVariable long id) { service.removeCart(auth.requireMember(request), id); return MiniResult.success(true); }

    @PutMapping("/cart/{id}")
    public MiniResult<?> updateCart(HttpServletRequest request, @PathVariable long id, @RequestBody(required = false) CartInput input) {
        if (input == null) throw new MiniBusinessException(400, "购物车参数无效");
        return MiniResult.success(service.updateCart(auth.requireMember(request), id, input.quantity == null ? 1 : input.quantity));
    }

    @PostMapping("/orders/cart")
    public MiniResult<?> checkoutCart(HttpServletRequest request, @RequestBody(required = false) CartOrderInput input)
    {
        if (input == null) throw new MiniBusinessException(400, "购物车结算参数无效");
        return MiniResult.success(service.checkoutCart(auth.requireMember(request), input.addressId, input.couponId, input.requestId, input.fulfillmentMode, input.pickupStoreId));
    }

    @PostMapping("/orders/cart/quote")
    public MiniResult<?> quoteCart(HttpServletRequest request, @RequestBody(required = false) CartOrderInput input)
    {
        if (input == null) throw new MiniBusinessException(400, "购物车结算参数无效");
        return MiniResult.success(service.quoteCart(auth.requireMember(request), input.addressId, input.couponId, input.fulfillmentMode, input.pickupStoreId));
    }

    @PostMapping("/promotion/bind")
    public MiniResult<?> bind(HttpServletRequest request, @RequestBody(required = false) CodeInput input) {
        if (input == null) throw new MiniBusinessException(400, "推广码参数无效");
        return MiniResult.success(service.bind(auth.requireMember(request), input.code));
    }

    @PostMapping("/promotion/withdraw")
    public MiniResult<?> withdraw(HttpServletRequest request, @RequestBody(required = false) WithdrawInput input) {
        if (input == null) throw new MiniBusinessException(400, "提现参数无效");
        return MiniResult.success(service.withdraw(auth.requireMember(request), input.amount == null ? 0 : input.amount, request.getHeader("Idempotency-Key")));
    }

    @GetMapping("/social/campaigns")
    public MiniResult<?> campaigns() { return MiniResult.success(service.socialCampaigns()); }

    @GetMapping("/social/subscribe-config")
    public MiniResult<?> socialSubscribeConfig() { return MiniResult.success(socialDelivery.subscriptionConfig()); }

    @PostMapping("/social/subscribe")
    public MiniResult<?> socialSubscribe(HttpServletRequest request, @RequestBody(required = false) SocialSubscriptionInput input)
    {
        if (input == null || input.templateId == null || input.result == null) throw new MiniBusinessException(400, "订阅授权信息无效");
        String requestId = input.requestId != null && !input.requestId.trim().isEmpty() ? input.requestId : request.getHeader("Idempotency-Key");
        return MiniResult.success(socialDelivery.recordSubscription(auth.requireMember(request), input.templateId, input.result, requestId));
    }

    @GetMapping("/channel")
    public MiniResult<?> channel() { return MiniResult.success(service.channel()); }

    @GetMapping("/video")
    public MiniResult<?> video() { return MiniResult.success(service.video()); }

    @GetMapping("/orders/{id}/delivery")
    public MiniResult<?> delivery(HttpServletRequest request, @PathVariable String id) { return MiniResult.success(service.delivery(auth.requireMember(request), id)); }

    @PostMapping("/orders/{id}/refund")
    public MiniResult<?> refund(HttpServletRequest request, @PathVariable String id, @RequestBody(required = false) RefundInput input) {
        if (input == null) throw new MiniBusinessException(400, "售后参数无效");
        return MiniResult.success(service.refund(auth.requireMember(request), id, input.amount == null ? 0 : input.amount, input.reason, request.getHeader("Idempotency-Key")));
    }

    public static class CartInput { public String productId; public String skuCode; public Integer quantity; }
    public static class CartOrderInput { public String addressId; public String couponId; public String requestId; public String fulfillmentMode; public String pickupStoreId; }
    public static class CodeInput { public String code; }
    public static class WithdrawInput { public Integer amount; }
    public static class RefundInput { public Integer amount; public String reason; }
    public static class SocialSubscriptionInput { public String templateId; public String result; public String requestId; }
}
