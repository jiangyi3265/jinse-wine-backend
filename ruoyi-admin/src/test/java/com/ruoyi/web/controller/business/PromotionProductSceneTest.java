package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PromotionProductSceneTest
{
    @Test
    public void roundTripsLargestIdsWithinWechatSceneLimit()
    {
        String scene = PromotionProductScene.encode(Long.MAX_VALUE, Long.MAX_VALUE);
        assertTrue(scene.length() <= 32);
        assertArrayEquals(new long[] { Long.MAX_VALUE, Long.MAX_VALUE }, PromotionProductScene.decode(scene));
    }

    @Test
    public void rejectsInvalidOrOverflowedScenes()
    {
        assertNull(PromotionProductScene.decode("JS1PROMO"));
        assertNull(PromotionProductScene.decode("P0-1"));
        assertNull(PromotionProductScene.decode("P1-0"));
        assertNull(PromotionProductScene.decode("Pzzzzzzzzzzzzzz-1"));
        assertNull(PromotionProductScene.decode("P1-1_extra"));
    }
}
