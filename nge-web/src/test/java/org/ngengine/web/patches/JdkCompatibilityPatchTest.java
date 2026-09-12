/*
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class JdkCompatibilityPatchTest {

    @Test
    public void webClassPathEnumerationIsExplicitlyEmpty() throws Exception {
        ClassLoaderPatch patch = new ClassLoaderPatch();
        for (String name : new String[] {"", "com/jme3/network", "META-INF/services/example.Service"}) {
            var resources = patch.getResources(name);
            assertFalse(resources.hasMoreElements());
            assertThrows(NoSuchElementException.class, resources::nextElement);
        }
        assertThrows(NullPointerException.class, () -> patch.getResources(null));
    }

    @Test
    public void unsignedIntegerConversionMatchesTheJdk() {
        int[] values = {0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE, -1};
        for (int value : values) {
            assertEquals(Integer.toUnsignedLong(value), IntegerPatch.toUnsignedLong(value));
        }
    }

    @Test
    public void unsignedLongParsingMatchesTheJdk() {
        String[] values = {
                "0", "1", "+1", "7fffffffffffffff", "8000000000000000", "ffffffffffffffff"
        };
        for (String value : values) {
            assertEquals(Long.parseUnsignedLong(value, 16), LongPatch.parseUnsignedLong(value, 16));
        }
        assertThrows(NumberFormatException.class,
                () -> LongPatch.parseUnsignedLong("10000000000000000", 16));
        assertThrows(NumberFormatException.class, () -> LongPatch.parseUnsignedLong("-1", 16));
        assertThrows(NumberFormatException.class, () -> LongPatch.parseUnsignedLong("+", 16));
    }

    @Test
    public void multidimensionalArrayAllocationMatchesTheJdkShape() {
        Object expected = java.lang.reflect.Array.newInstance(String.class, 2, 3, 4);
        Object actual = ArrayPatch.newInstance(String.class, 2, 3, 4);
        assertEquals(expected.getClass(), actual.getClass());
        assertArrayEquals(
                new int[] {2, 3, 4},
                new int[] {
                        java.lang.reflect.Array.getLength(actual),
                        java.lang.reflect.Array.getLength(java.lang.reflect.Array.get(actual, 0)),
                        java.lang.reflect.Array.getLength(
                                java.lang.reflect.Array.get(java.lang.reflect.Array.get(actual, 0), 0))
                });
        assertThrows(NegativeArraySizeException.class,
                () -> ArrayPatch.newInstance(Integer.TYPE, 2, -1));
        assertThrows(IllegalArgumentException.class,
                () -> ArrayPatch.newInstance(Integer.TYPE, new int[0]));
        assertEquals("[[[Ljava.lang.String;", actual.getClass().getName());
        assertEquals(24, Arrays.stream((Object[][][]) actual).mapToInt(level -> level.length * level[0].length).sum());
    }
}
