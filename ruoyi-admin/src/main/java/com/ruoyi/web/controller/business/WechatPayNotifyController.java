package com.ruoyi.web.controller.business;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;

/** 微信支付回调必须返回微信约定的 JSON，不复用小程序错误协议。 */
@Anonymous
@RestController
@RequestMapping("/wechat/pay")
public class WechatPayNotifyController
{
    private final WechatPayService pay;

    public WechatPayNotifyController(WechatPayService pay) { this.pay = pay; }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> notify(HttpServletRequest request, @RequestBody String body)
    {
        pay.handleNotify(request, body);
        Map<String, String> result = new LinkedHashMap<>(); result.put("code", "SUCCESS"); result.put("message", "成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refund-notify")
    public ResponseEntity<Map<String, String>> refundNotify(HttpServletRequest request, @RequestBody String body)
    {
        pay.handleRefundNotify(request, body);
        Map<String, String> result = new LinkedHashMap<>(); result.put("code", "SUCCESS"); result.put("message", "成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transfer-notify")
    public ResponseEntity<Map<String, String>> transferNotify(HttpServletRequest request, @RequestBody String body)
    {
        pay.handleTransferNotify(request, body);
        Map<String, String> result = new LinkedHashMap<>(); result.put("code", "SUCCESS"); result.put("message", "成功");
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> error(Exception ignored)
    {
        Map<String, String> result = new LinkedHashMap<>(); result.put("code", "FAIL"); result.put("message", "回调处理失败");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
