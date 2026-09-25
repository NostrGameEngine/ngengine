package com.jme3.util;

/** Compatibility entry points for the NGE Platform memory guard. */
public class SaferAllocMemoryGuard {
    public static void beforeAlloc(long size) {
        SaferBufferAllocator.allocator().beforeAlloc(size);
    }

    public static void notifyGC() {
        SaferBufferAllocator.allocator().notifyGC();
    }
}
