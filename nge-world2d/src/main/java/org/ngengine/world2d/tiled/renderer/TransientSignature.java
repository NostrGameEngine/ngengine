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

package org.ngengine.world2d.tiled.renderer;

/**
 * Compact signature used to detect when an entry should leave its batch briefly
 * instead of mutating texture/source data in place.
 */
final class TransientSignature {
    final int drawGroup;
    final int sourceId;
    final int x;
    final int y;
    final int z;
    final int tileGid;

    TransientSignature(int drawGroup, int sourceId, int x, int y, int z, int tileGid) {
        this.drawGroup = drawGroup;
        this.sourceId = sourceId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tileGid = tileGid;
    }

    String reason(TransientSignature next) {
        if (sourceId != next.sourceId || tileGid != next.tileGid) return "source";
        if (x != next.x || y != next.y || z != next.z) return "position";
        if (drawGroup != next.drawGroup) return "batch";
        return "state";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TransientSignature)) {
            return false;
        }
        TransientSignature o = (TransientSignature) other;
        return drawGroup == o.drawGroup && sourceId == o.sourceId
                && x == o.x && y == o.y && z == o.z && tileGid == o.tileGid;
    }

    @Override
    public int hashCode() {
        int h = drawGroup;
        h = 31 * h + sourceId;
        h = 31 * h + x;
        h = 31 * h + y;
        h = 31 * h + z;
        h = 31 * h + tileGid;
        return h;
    }
}
