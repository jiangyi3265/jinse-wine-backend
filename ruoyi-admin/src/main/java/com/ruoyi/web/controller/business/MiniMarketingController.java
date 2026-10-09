package com.ruoyi.web.controller.business;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.MiniResult;

/** 小程序积分、抽奖、奖品核销和推广接口；业务结果均由服务端数据库决定。 */
@Anonymous
@RestController
@RequestMapping("/mini")
public class MiniMarketingController
{
    private final MiniMemberAuthService auth;
    private final MiniMarketingService marketing;

    public MiniMarketingController(MiniMemberAuthService auth, MiniMarketingService marketing)
    {
        this.auth = auth;
        this.marketing = marketing;
    }

    /** 营销业务校验失败也按小程序统一协议返回，避免被 Spring 转成 HTML 错误页。 */
    @ExceptionHandler(MiniBusinessException.class)
    public MiniResult<?> businessError(MiniBusinessException exception)
    {
        return MiniResult.error(exception.getCode(), exception.getMessage());
    }

    /** 请求体或数据库出现未预期异常时仍返回小程序协议，避免泄露 Spring 默认错误页。 */
    @ExceptionHandler(Exception.class)
    public MiniResult<?> systemError(Exception exception)
    {
        return MiniResult.error(500, "服务暂不可用");
    }

    @GetMapping("/points")
    public MiniResult<?> points(HttpServletRequest request)
    {
        return MiniResult.success(marketing.points(auth.requireMember(request)));
    }

    @GetMapping("/points/records")
    public MiniResult<?> pointRecords(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(marketing.pointRecords(auth.requireMember(request), params));
    }

    @PostMapping("/points/signin")
    public MiniResult<?> signIn(HttpServletRequest request, @RequestBody(required = false) RequestKey input)
    {
        if (input == null) throw new MiniBusinessException(400, "签到请求参数无效");
        return MiniResult.success(marketing.signIn(auth.requireMember(request), input.requestId));
    }

    @PostMapping("/exchanges/{code}/redeem")
    public MiniResult<?> exchange(HttpServletRequest request, @PathVariable String code)
    {
        return MiniResult.success(marketing.exchange(auth.requireMember(request), code,
                request.getHeader("Idempotency-Key")));
    }

    @GetMapping("/rewards")
    public MiniResult<?> rewards(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(marketing.rewards(auth.requireMember(request), params));
    }

    @GetMapping("/rewards/{id}/redeem-code")
    public MiniResult<?> rewardRedeemCode(HttpServletRequest request, @PathVariable String id)
    {
        return MiniResult.success(marketing.rewardRedeemCode(auth.requireMember(request), id));
    }

    @GetMapping("/rewards/cash")
    public MiniResult<?> cashRewards(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(marketing.cashRewards(auth.requireMember(request), params));
    }

    @GetMapping("/rewards/cash/{id}")
    public MiniResult<?> cashReward(HttpServletRequest request, @PathVariable String id)
    {
        return MiniResult.success(marketing.cashReward(auth.requireMember(request), id));
    }

    @PostMapping("/rewards/cash/{id}/claim")
    public MiniResult<?> claimCash(HttpServletRequest request, @PathVariable String id)
    {
        // 最终到账状态只能由微信转账查单或验签回调确认。
        return MiniResult.success(marketing.claimCash(auth.requireMember(request), id,
                request.getHeader("Idempotency-Key")));
    }

    @PostMapping("/draws")
    public MiniResult<?> draw(HttpServletRequest request, @RequestBody(required = false) DrawInput input)
    {
        return MiniResult.success(marketing.draw(auth.requireMember(request), input == null ? null : input.code,
                request.getHeader("Idempotency-Key")));
    }

    @GetMapping("/draws/prizes")
    public MiniResult<?> drawPrizes(HttpServletRequest request)
    {
        auth.requireMember(request);
        return MiniResult.success(marketing.drawPrizes());
    }

    @GetMapping("/draws")
    public MiniResult<?> drawRecords(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(marketing.drawRecords(auth.requireMember(request), params));
    }

    @GetMapping("/staff/redemptions")
    public MiniResult<?> redemptions(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(marketing.redemptions(auth.requireMember(request), params));
    }

    @PostMapping("/staff/redemptions/lookup")
    public MiniResult<?> lookup(HttpServletRequest request, @RequestBody(required = false) CodeInput input)
    {
        if (input == null || input.code == null || input.code.trim().isEmpty())
            throw new MiniBusinessException(400, "核销码参数无效");
        return MiniResult.success(marketing.lookup(auth.requireMember(request), input == null ? null : input.code));
    }

    @PostMapping("/staff/redemptions/confirm")
    public MiniResult<?> confirm(HttpServletRequest request, @RequestBody(required = false) CodeInput input)
    {
        if (input == null || input.code == null || input.code.trim().isEmpty())
            throw new MiniBusinessException(400, "核销码参数无效");
        return MiniResult.success(marketing.confirm(auth.requireMember(request), input == null ? null : input.code,
                request.getHeader("Idempotency-Key")));
    }

    @GetMapping("/promotion")
    public MiniResult<?> promotion(HttpServletRequest request)
    {
        return MiniResult.success(marketing.promotion(auth.requireMember(request)));
    }

    @GetMapping("/promotion/team")
    public MiniResult<?> team(HttpServletRequest request)
    {
        return MiniResult.success(marketing.team(auth.requireMember(request)));
    }

    @GetMapping("/promotion/records")
    public MiniResult<?> promotionRecords(HttpServletRequest request, @RequestParam Map<String, String> params)
    {
        return MiniResult.success(marketing.promotionRecords(auth.requireMember(request), params));
    }

    @GetMapping("/promotion/ranking")
    public MiniResult<?> ranking(HttpServletRequest request)
    {
        return MiniResult.success(marketing.ranking(auth.requireMember(request)));
    }

    @PostMapping("/promotion/code")
    public MiniResult<?> promotionCode(HttpServletRequest request, @RequestBody(required = false) PromotionCodeInput input)
    {
        return MiniResult.success(marketing.promotionCode(auth.requireMember(request), input == null ? null : input.productCode));
    }

    public static class PromotionCodeInput
    {
        public String productCode;
    }

    public static class DrawInput
    {
        public String code;
    }

    public static class CodeInput
    {
        public String code;
    }

    public static class RequestKey { public String requestId; }
}
