package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WechatMiniCodeCacheTest
{
    @Test
    public void separatesCodesByAppAndGenerationSettings() throws Exception
    {
        String release = WechatMiniCodeService.codeFileName("wx-app-a", "release", true, "pages/scan/index", "DRAW-1");
        assertEquals(release, WechatMiniCodeService.codeFileName("wx-app-a", "release", true, "pages/scan/index", "DRAW-1"));
        assertNotEquals(release, WechatMiniCodeService.codeFileName("wx-app-b", "release", true, "pages/scan/index", "DRAW-1"));
        assertNotEquals(release, WechatMiniCodeService.codeFileName("wx-app-a", "trial", true, "pages/scan/index", "DRAW-1"));
        assertNotEquals(release, WechatMiniCodeService.codeFileName("wx-app-a", "release", false, "pages/scan/index", "DRAW-1"));
        assertNotEquals(release, WechatMiniCodeService.codeFileName("wx-app-a", "release", true, "pages/index/index", "DRAW-1"));
        assertNotEquals(release, WechatMiniCodeService.codeFileName("wx-app-a", "release", true, "pages/scan/index", "DRAW-2"));
        assertTrue(release.matches("[0-9a-f]{64}\\.png"));
    }
}
