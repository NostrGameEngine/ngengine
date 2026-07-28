package com.jme3.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakCollectionTest {

    @Test
    void removesLiveReferencesWithoutUsingTheIterator() {
        WeakCollection<Object> collection = new WeakCollection<>();
        Object value = new Object();
        collection.add(value);

        assertTrue(collection.remove(value));
        assertFalse(collection.contains(value));
        assertFalse(collection.remove(value));
    }

    @Test
    void clearRemovesEveryLiveReference() {
        WeakCollection<Object> collection = new WeakCollection<>();
        Object first = new Object();
        Object second = new Object();
        collection.add(first);
        collection.add(second);

        collection.clear();

        assertFalse(collection.contains(first));
        assertFalse(collection.contains(second));
    }
}
