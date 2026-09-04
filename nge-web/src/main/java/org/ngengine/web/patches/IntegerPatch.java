/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

/** Adds JDK integer methods that are missing from the supported TeaVM class library. */
public final class IntegerPatch {

    private IntegerPatch() {
    }

    public static long toUnsignedLong(int value) {
        return value & 0xffffffffL;
    }
}
