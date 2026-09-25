package com.jme3.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import org.ngengine.platform.NGEAllocator;
import org.ngengine.platform.NGEPlatform;

/** Retains the jME allocator API while NGE Platform owns native allocations. */
public final class SaferBufferAllocator implements BufferAllocator {
    private static final Logger logger = Logger.getLogger(SaferBufferAllocator.class.getName());

    public SaferBufferAllocator() {
        logger.info(getClass().getSimpleName() + " enabled!");
    }

    static NGEAllocator allocator() {
        return NGEPlatform.get().getNativeAllocator();
    }

    public static long getMallocFunctionPointer() {
        return allocator().mallocFunctionPointer();
    }

    public static long getCallocFunctionPointer() {
        return allocator().callocFunctionPointer();
    }

    public static long getReallocFunctionPointer() {
        return allocator().reallocFunctionPointer();
    }

    public static long getFreeFunctionPointer() {
        return allocator().freeFunctionPointer();
    }

    public static long getAlignedAllocFunctionPointer() {
        return allocator().alignedAllocFunctionPointer();
    }

    public static long getAlignedFreeFunctionPointer() {
        return allocator().alignedFreeFunctionPointer();
    }

    public static long malloc(long size) {
        return allocator().mallocRaw(size);
    }

    public static long calloc(long num, long size) {
        return allocator().callocRaw(num, size);
    }

    public static long realloc(long ptr, long size) {
        return allocator().reallocRaw(ptr, size);
    }

    public static void free(long ptr) {
        allocator().freeRaw(ptr);
    }

    public static long alignedAlloc(long alignment, long size) {
        return allocator().mallocAlignedRaw(alignment, size);
    }

    public static void alignedFree(long ptr) {
        allocator().freeAlignedRaw(ptr);
    }

    @Override
    public ByteBuffer allocate(int size) {
        ByteBuffer buffer = allocator().calloc(1, size);
        if (buffer == null) {
            throw new OutOfMemoryError("Could not allocate " + size + " bytes through NGE Platform");
        }
        return buffer;
    }

    @Override
    public void destroyDirectBuffer(Buffer buffer) {
        allocator().freeBuffer(buffer);
    }
}
