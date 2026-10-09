package com.ruoyi.common.utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/** Server-owned identifiers with 96 bits of entropy, within the shop's 20-character SKU limit. */
public final class BusinessCode
{
    private BusinessCode() {}
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String next(String prefix)
    {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return prefix.substring(0, Math.min(3, prefix.length())).toUpperCase(Locale.ROOT) + "_"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
