/*
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MathPatchTest {

    @Test
    public void scalbMatchesTheJdkForHdrAndBoundaryValues() {
        float[] values = {
                0f, -0f, 1f, -1f, Float.MIN_VALUE, Float.MIN_NORMAL,
                Float.MAX_VALUE, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN,
                0.00390625f, 0.5f, 127.75f
        };
        int[] scales = {-400, -278, -149, -127, -126, -1, 0, 1, 126, 127, 278, 400};

        for (float value : values) {
            for (int scale : scales) {
                assertEquals(
                        Float.floatToRawIntBits(Math.scalb(value, scale)),
                        Float.floatToRawIntBits(MathPatch.scalb(value, scale)),
                        "value=" + value + ", scale=" + scale);
            }
        }
    }
}
