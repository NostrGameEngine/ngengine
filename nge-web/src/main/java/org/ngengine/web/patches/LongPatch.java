/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

/** Adds JDK long methods that are missing from the supported TeaVM class library. */
public final class LongPatch {

    private LongPatch() {
    }

    public static long parseUnsignedLong(String value) {
        return parseUnsignedLong(value, 10);
    }

    public static long parseUnsignedLong(String value, int radix) {
        if (value == null) {
            throw new NumberFormatException("null");
        }
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " out of range");
        }
        int length = value.length();
        if (length == 0) {
            throw invalidNumber(value);
        }

        int index = 0;
        char first = value.charAt(0);
        if (first == '+') {
            index++;
        } else if (first == '-') {
            throw invalidNumber(value);
        }
        if (index == length) {
            throw invalidNumber(value);
        }

        long maxQuotient = Long.divideUnsigned(-1L, radix);
        int maxRemainder = (int) Long.remainderUnsigned(-1L, radix);
        long result = 0L;
        while (index < length) {
            int digit = Character.digit(value.charAt(index++), radix);
            if (digit < 0
                    || Long.compareUnsigned(result, maxQuotient) > 0
                    || (result == maxQuotient && digit > maxRemainder)) {
                throw invalidNumber(value);
            }
            result = result * radix + digit;
        }
        return result;
    }

    private static NumberFormatException invalidNumber(String value) {
        return new NumberFormatException("For input string: \"" + value + "\"");
    }
}
