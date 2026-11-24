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

import java.util.Arrays;
import java.util.Comparator;

final class NavSnapshot {

    final long revision, surfaceRevision;
    final NavChunk[] chunks;
    final NavGeometry[] geometry, obstacles;
    final NavLink[] links;
    private final int[] slots;

    NavSnapshot(long revision, long surfaceRevision, NavChunk[] chunks, NavGeometry[] geometry, NavLink[] links) {
        this.revision = revision; this.surfaceRevision = surfaceRevision;
        this.chunks = chunks;
        this.geometry = geometry;
        int movable = 0;
        for (NavGeometry source : geometry) if ((source.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) movable++;
        obstacles = new NavGeometry[movable]; movable = 0;
        for (NavGeometry source : geometry) if ((source.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) obstacles[movable++] = source;
        this.links = links;
        Arrays.sort(chunks, Comparator.comparingLong(chunk -> chunk.serial));
        int capacity = 16;
        while (capacity < chunks.length * 2L) capacity <<= 1;
        slots = new int[capacity];
        for (int i = 0; i < chunks.length; i++) {
            int slot = chunks[i].key.hashCode() & (capacity - 1);
            while (slots[slot] != 0) slot = (slot + 1) & (capacity - 1);
            slots[slot] = i + 1;
        }
    }

    NavChunk chunk(int x, int y, int z) {
        int slot = (x * 73856093 ^ y * 19349663 ^ z * 83492791) & (slots.length - 1);
        while (slots[slot] != 0) {
            NavChunk chunk = chunks[slots[slot] - 1];
            if (chunk.key.x == x && chunk.key.y == y && chunk.key.z == z) return chunk;
            slot = (slot + 1) & (slots.length - 1);
        }
        return null;
    }

    NavChunk resolve(long reference) {
        long serial = reference >>> 24;
        int low = 0, high = chunks.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            NavChunk chunk = chunks[middle];
            if (chunk.serial < serial) low = middle + 1; else if (chunk.serial > serial) high = middle - 1; else return (
                    (reference & 0xffffffL) >>> 3 < chunk.nodeCount
                )
                ? chunk
                : null;
        }
        return null;
    }

    static long reference(NavChunk chunk, int node, NavMode mode) {
        return (chunk.serial << 24) | ((long) node << 3) | mode.ordinal();
    }

    static int node(long reference) {
        return (int) ((reference & 0xffffffL) >>> 3);
    }

    static NavMode mode(long reference) {
        return NavMode.ALL[(int) reference & 7];
    }
}
