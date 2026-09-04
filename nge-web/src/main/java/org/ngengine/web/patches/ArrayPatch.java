/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

import java.lang.reflect.Array;

/** Adds multidimensional array allocation missing from the supported TeaVM class library. */
public final class ArrayPatch {

    private ArrayPatch() {
    }

    public static Object newInstance(Class<?> componentType, int... dimensions) {
        if (componentType == null) {
            throw new NullPointerException();
        }
        if (dimensions == null) {
            throw new NullPointerException();
        }
        if (componentType == Void.TYPE || dimensions.length == 0 || dimensions.length > 255) {
            throw new IllegalArgumentException();
        }

        Class<?>[] componentTypes = new Class<?>[dimensions.length];
        componentTypes[dimensions.length - 1] = componentType;
        for (int i = dimensions.length - 2; i >= 0; i--) {
            componentTypes[i] = Array.newInstance(componentTypes[i + 1], 0).getClass();
        }
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i] < 0) {
                throw new NegativeArraySizeException();
            }
        }

        Object result = Array.newInstance(componentTypes[0], dimensions[0]);
        populate(result, componentTypes, dimensions, 1);
        return result;
    }

    private static void populate(Object array, Class<?>[] componentTypes, int[] dimensions, int depth) {
        if (depth >= dimensions.length) {
            return;
        }
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object child = Array.newInstance(componentTypes[depth], dimensions[depth]);
            if (depth < dimensions.length - 1) {
                populate(child, componentTypes, dimensions, depth + 1);
            }
            Array.set(array, i, child);
        }
    }
}
