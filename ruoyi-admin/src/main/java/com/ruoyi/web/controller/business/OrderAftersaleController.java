package com.ruoyi.web.controller.business;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.MiniResult;

@RestController
public class OrderAftersaleController extends BaseController
{
    private final OrderAftersaleService service;
    private final MiniMemberAuthService auth;
    public OrderAftersaleController(OrderAftersaleService service, MiniMemberAuthService auth) { this.service = service; this.auth = auth; }

    @Anonymous @GetMapping("/mini/orders/{orderNo}/aftersales")
    public MiniResult<?> memberList(HttpServletRequest request, @PathVariable String orderNo, @RequestParam Map<String, String> params)
    {
        long memberId = auth.requireMember(request);
        return MiniResult.success(params.containsKey("pageNum") || params.containsKey("pageSize")
                ? service.memberPage(memberId, orderNo, params) : service.memberList(memberId, orderNo));
    }
    @Anonymous @PostMapping("/mini/orders/{orderNo}/aftersales")
    public MiniResult<?> apply(HttpServletRequest request, @PathVariable String orderNo, @RequestBody Map<String, Object> input) { return MiniResult.success(service.apply(auth.requireMember(request), orderNo, input, request.getHeader("Idempotency-Key"))); }
    @Anonymous @GetMapping("/mini/aftersales/{id}")
    public MiniResult<?> memberDetail(HttpServletRequest request, @PathVariable long id) { return MiniResult.success(service.memberDetail(auth.requireMember(request), id)); }
    @Anonymous @PostMapping("/mini/aftersales/{id}/return-shipment")
    public MiniResult<?> returnShipment(HttpServletRequest request, @PathVariable long id, @RequestBody Map<String, Object> input) { return MiniResult.success(service.returnShipment(auth.requireMember(request), id, input)); }
    @Anonymous @PostMapping("/mini/aftersales/{id}/cancel")
    public MiniResult<?> cancel(HttpServletRequest request, @PathVariable long id) { return MiniResult.success(service.memberAction(auth.requireMember(request), id, false)); }
    @Anonymous @PostMapping("/mini/aftersales/{id}/receive")
    public MiniResult<?> receive(HttpServletRequest request, @PathVariable long id) { return MiniResult.success(service.memberAction(auth.requireMember(request), id, true)); }

    @GetMapping("/business/extension/aftersales") @PreAuthorize("@ss.hasPermi('business:refund:list')")
    public AjaxResult adminList(@RequestParam Map<String, String> params) { return success(service.adminList(params)); }
    @GetMapping("/business/extension/aftersales/{id}") @PreAuthorize("@ss.hasPermi('business:refund:list')")
    public AjaxResult adminDetail(@PathVariable long id) { return success(service.adminDetail(id)); }
    @PostMapping("/business/extension/aftersales/{id}/review") @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    @Log(title="退换货审核", businessType=BusinessType.UPDATE)
    public AjaxResult review(@PathVariable long id, @RequestBody Map<String, Object> input) { return success(service.review(id, input, getUsername())); }
    @PostMapping("/business/extension/aftersales/{id}/inspect") @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    @Log(title="售后退货收货", businessType=BusinessType.UPDATE)
    public AjaxResult inspect(@PathVariable long id, @RequestBody Map<String, Object> input) { return success(service.inspect(id, input, getUsername())); }
    @PostMapping("/business/extension/aftersales/{id}/replacement") @PreAuthorize("@ss.hasPermi('business:refund:edit')")
    @Log(title="换货商品发货", businessType=BusinessType.UPDATE)
    public AjaxResult replacement(@PathVariable long id, @RequestBody Map<String, Object> input) { return success(service.replacement(id, input, getUsername())); }

    @ExceptionHandler(MiniBusinessException.class)
    public Object businessError(HttpServletRequest request, MiniBusinessException error)
    {
        return request.getRequestURI().startsWith("/mini/") ? MiniResult.error(error.getCode(), error.getMessage()) : AjaxResult.error(error.getCode(), error.getMessage());
    }
}
