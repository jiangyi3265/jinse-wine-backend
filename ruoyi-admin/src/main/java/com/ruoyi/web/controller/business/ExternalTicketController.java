package com.ruoyi.web.controller.business;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.MiniResult;

@RestController
public class ExternalTicketController {
    private final ExternalTicketService tickets;
    private final MiniMemberAuthService auth;
    public ExternalTicketController(ExternalTicketService tickets, MiniMemberAuthService auth) {
        this.tickets = tickets; this.auth = auth;
    }

    @GetMapping("/business/lottery/tickets")
    @PreAuthorize("@ss.hasPermi('business:lottery:list')")
    public AjaxResult list(@RequestParam long prizeId, @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        return AjaxResult.success(tickets.list(prizeId, status, pageNum, pageSize));
    }

    @PostMapping("/business/lottery/tickets")
    @PreAuthorize("@ss.hasPermi('business:lottery:edit')")
    public AjaxResult upload(@RequestParam long prizeId, @RequestParam String providerName,
            @RequestParam String externalNo, @RequestParam String validUntil, @RequestParam MultipartFile file) throws Exception {
        tickets.upload(prizeId, providerName, externalNo, validUntil, file, com.ruoyi.common.utils.SecurityUtils.getUsername());
        return AjaxResult.success();
    }

    @PostMapping("/business/lottery/tickets/{id}/disable")
    @PreAuthorize("@ss.hasPermi('business:lottery:edit')")
    public AjaxResult disable(@PathVariable long id) { tickets.disable(id); return AjaxResult.success(); }

    @GetMapping("/business/lottery/tickets/{id}/image")
    @PreAuthorize("@ss.hasPermi('business:lottery:edit')")
    public ResponseEntity<byte[]> image(@PathVariable long id) {
        Map<String, Object> row = tickets.adminImage(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(String.valueOf(row.get("qr_mime"))))
                .body((byte[]) row.get("qr_image"));
    }

    @Anonymous
    @GetMapping("/mini/rewards/{id}/ticket-code")
    public MiniResult<?> memberQr(@PathVariable long id, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return MiniResult.success(tickets.memberQr(auth.requireMember(request), id));
    }

    @ExceptionHandler(MiniBusinessException.class)
    public Object error(MiniBusinessException e, HttpServletRequest request) {
        return request.getRequestURI().startsWith("/mini/") ? MiniResult.error(e.getCode(), e.getMessage())
                : AjaxResult.error(e.getCode(), e.getMessage());
    }
}
