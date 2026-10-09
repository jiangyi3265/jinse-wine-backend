package com.ruoyi.web.controller.business;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compact referral and product identity for WeChat's 32-character scene limit. */
final class PromotionProductScene
{
    private static final Pattern PATTERN = Pattern.compile("P([0-9a-z]+)-([0-9a-z]+)");

    private PromotionProductScene() { }

    static String encode(long memberId, long productId)
    {
        if (memberId <= 0 || productId <= 0) throw new IllegalArgumentException("IDs must be positive");
        return "P" + Long.toString(memberId, 36) + "-" + Long.toString(productId, 36);
    }

    static long[] decode(String scene)
    {
        if (scene == null || scene.length() > 32) return null;
        Matcher match = PATTERN.matcher(scene);
        if (!match.matches()) return null;
        try
        {
            long memberId = Long.parseLong(match.group(1), 36);
            long productId = Long.parseLong(match.group(2), 36);
            return memberId > 0 && productId > 0 ? new long[] { memberId, productId } : null;
        }
        catch (NumberFormatException e) { return null; }
    }
}
