package com.ruoyi.web.controller.business;

import javax.servlet.http.HttpServletRequest;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.MiniResult;
import org.springframework.web.bind.annotation.*;

@Anonymous
@RestController
@RequestMapping("/mini/promotion/transfers")
public class PromotionTransferController
{
    private final MiniMemberAuthService auth;
    private final PromotionTransferService service;
    public PromotionTransferController(MiniMemberAuthService auth, PromotionTransferService service) { this.auth=auth; this.service=service; }
    @GetMapping public MiniResult<?> list(HttpServletRequest request) { return MiniResult.success(service.list(auth.requireMember(request))); }
    @GetMapping("/{id}") public MiniResult<?> detail(HttpServletRequest request, @PathVariable long id) { return MiniResult.success(service.detail(auth.requireMember(request), id)); }
    @ExceptionHandler(MiniBusinessException.class) public MiniResult<?> error(MiniBusinessException e) { return MiniResult.error(e.getCode(), e.getMessage()); }
}
