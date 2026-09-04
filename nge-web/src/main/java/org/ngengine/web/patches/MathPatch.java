/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

/** Adds JDK math methods that are missing from the supported TeaVM class library. */
public final class MathPatch {

    private MathPatch() {
    }

    /**
     * Returns {@code value} multiplied by two raised to {@code scaleFactor}.
     * Multiplication is split into representable powers of two so overflow,
     * underflow, infinities, NaN, and signed zero follow the JVM operation.
     */
    public static float scalb(float value, int scaleFactor) {
        if (value == 0 || !Float.isFinite(value)) {
            return value;
        }

        float result = value;
        while (scaleFactor > 127) {
            result *= Float.intBitsToFloat(254 << 23);
            if (Float.isInfinite(result)) {
                return result;
            }
            scaleFactor -= 127;
        }
        while (scaleFactor < -126) {
            result *= Float.intBitsToFloat(1 << 23);
            if (result == 0) {
                return result;
            }
            scaleFactor += 126;
        }
        return result * Float.intBitsToFloat((scaleFactor + 127) << 23);
    }
}
