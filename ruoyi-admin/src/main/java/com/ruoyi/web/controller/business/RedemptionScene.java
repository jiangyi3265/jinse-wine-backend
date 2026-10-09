package com.ruoyi.web.controller.business;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded WeChat scene for a reward whose full redemption code may exceed 32 characters. */
final class RedemptionScene
{
    private static final Pattern TOKEN = Pattern.compile("^R([0-9A-Z]{1,13})-[0-9a-f]{16}$");

    private RedemptionScene() { }

    static String encode(long rewardId, String exchangeCode)
    {
        if (rewardId <= 0 || exchangeCode == null || exchangeCode.isEmpty())
            throw new IllegalArgumentException("奖品兑换码无效");
        return "R" + Long.toString(rewardId, 36).toUpperCase(Locale.ROOT) + "-"
                + MiniMemberAuthService.sha256(exchangeCode).substring(0, 16);
    }

    static Long rewardId(String scene)
    {
        Matcher matcher = TOKEN.matcher(scene == null ? "" : scene);
        if (!matcher.matches()) return null;
        try
        {
            long id = Long.parseLong(matcher.group(1), 36);
            return id > 0 ? id : null;
        }
        catch (NumberFormatException exception)
        {
            return null;
        }
    }
}
