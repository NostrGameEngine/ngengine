package com.jme3.util;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEAllocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SaferAllocMemoryGuardTest {

    @Test
    public void jmeBuffersUseThePlatformAllocator() {
        NGEAllocator platformAllocator = NGEPlatform.get().getNativeAllocator();
        assertSame(platformAllocator, SaferBufferAllocator.allocator());
        SaferBufferAllocator jmeAllocator = new SaferBufferAllocator();
        ByteBuffer buffer = jmeAllocator.allocate(32);
        assertNotNull(buffer);
        assertEquals(32, buffer.capacity());
        assertNotEquals(0L, platformAllocator.address(buffer));
        jmeAllocator.destroyDirectBuffer(buffer);
        // The platform and jME entry points share ownership bookkeeping.
        platformAllocator.free(buffer);
    }

    @Test
    public void jmePointerApiDelegatesToThePlatform() {
        NGEAllocator platformAllocator = NGEPlatform.get().getNativeAllocator();
        assertEquals(platformAllocator.mallocFunctionPointer(), SaferBufferAllocator.getMallocFunctionPointer());
        assertEquals(platformAllocator.alignedFreeFunctionPointer(), SaferBufferAllocator.getAlignedFreeFunctionPointer());
        long address = SaferBufferAllocator.malloc(32L);
        assertNotEquals(0L, address);
        try {
            address = SaferBufferAllocator.realloc(address, 64L);
            assertNotEquals(0L, address);
        } finally {
            SaferBufferAllocator.free(address);
        }
        assertThrows(OutOfMemoryError.class, () -> SaferBufferAllocator.calloc(Long.MAX_VALUE, 2L));
        SaferAllocMemoryGuard.beforeAlloc(0L);
        SaferAllocMemoryGuard.notifyGC();
    }
}
