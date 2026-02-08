/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.ngengine.web.context;

 
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jme3.util.BufferAllocator;

/**
 * This class contains a primitive allocator with no special logic, should work
 * on any jvm
 */
public final class HeapAllocator implements BufferAllocator {



    /**
     * De-allocate a direct buffer.
     *
     * @param toBeDestroyed ignored
     */
    @Override
    public void destroyDirectBuffer(Buffer toBeDestroyed) {
        // no exception by intent, as this way naively written java7/8
        // applications won't crash on 9 assuming they can dispose buffers
        System.err.println("Warning destroyBuffer not supported");
    }

    /**
     * Allocate a direct ByteBuffer of the specified size.
     *
     * @param size in bytes (&ge;0)
     * @return a new direct buffer
     */
    @Override
    public ByteBuffer allocate(int size) {
        ByteBuffer bbf = ByteBuffer.allocateDirect(size);
        bbf=bbf.order(ByteOrder.nativeOrder());
        return bbf;
    }
}
