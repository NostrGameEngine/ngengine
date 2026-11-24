/**
 * Copyright (c) 2025-2026, Nostr Game Engine
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

package org.ngengine.nav;

import com.jme3.math.Vector3f;
import com.jme3.util.BufferAllocator;
import com.jme3.util.PrimitiveAllocator;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public final class NavAllocatorProbe {

    public static final class CountingAllocator implements BufferAllocator {

        static final AtomicInteger allocations = new AtomicInteger();
        static final AtomicInteger destroyed = new AtomicInteger();
        private final PrimitiveAllocator delegate = new PrimitiveAllocator();

        @Override
        public ByteBuffer allocate(int size) {
            allocations.incrementAndGet();
            return delegate.allocate(size);
        }

        @Override
        public void destroyDirectBuffer(Buffer buffer) {
            destroyed.incrementAndGet();
        }
    }

    public static void main(String[] args) {
        NavWorld world = NavWorld.manual(NavSettings.builder().chunkSize(4).build());
        world.layer("floor").add(NavGeometry.box(new Vector3f(-2, -1, -2), new Vector3f(2, 0, 2)));
        while (!world.isReady()) {
            world.pump(32);
            if (world.getFailure() != null) throw new AssertionError(world.getFailure());
        }
        NavPath path = world
            .view()
            .radius(0.1f)
            .height(1)
            .build()
            .findPath(new Vector3f(-1.25f, 0.5f, 0.25f), new Vector3f(1.25f, 0.5f, 0.25f));
        if (!path.isComplete() || !path.points.isDirect() || CountingAllocator.allocations.get() == 0) throw new AssertionError(
            "Allocator was bypassed"
        );
        NavChunk retained = world.getChunk(new NavChunkKey(0, 0, 0));
        int nodes = retained.nodeCount;
        world.close();
        retained.point(0, new Vector3f());
        path.getPoint(0, new Vector3f());
        if (CountingAllocator.destroyed.get() != 0 || retained.nodeCount != nodes) throw new AssertionError(
            "Freed a live snapshot"
        );
        System.out.println(
            "Engine allocator allocations=" +
            CountingAllocator.allocations.get() +
            "; eager frees=" +
            CountingAllocator.destroyed.get()
        );
    }
}
