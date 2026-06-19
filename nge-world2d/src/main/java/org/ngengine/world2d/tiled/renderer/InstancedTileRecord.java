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

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.tileset.Tile;

/**
 * CPU copy of one instance slot, used to decide whether buffer data changed.
 */
final class InstancedTileRecord {
    static final int DECAL_LAYERS = 4;

    TiledBase entry;
    Tile tile;
    InstancedTilesetSource source;
    int textureSlot;
    float tileDataX;
    float tileDataY;
    int flipFlags;
    boolean seen;
    float x;
    float y;
    float z;
    float rotation;
    float tileWidth;
    float tileHeight;
    float offsetX;
    float offsetY;
    float originX;
    float originY;
    float imageWidth;
    float imageHeight;
    float uvWidth;
    float uvHeight;
    final float[] decalTile = new float[] { -1f, -1f, -1f, -1f };
    final float[] decalX = new float[DECAL_LAYERS];
    final float[] decalY = new float[DECAL_LAYERS];
    final float[] decalScale = new float[DECAL_LAYERS];
    boolean writeNeeded;
    boolean tombstone;

    boolean update(Tile tile, InstancedTilesetSource source, int textureSlot,
            float tileDataX, float tileDataY, int flipFlags,
            float x, float y, float z, float rotation,
            float tileWidth, float tileHeight, float offsetX, float offsetY,
            float originX, float originY, float imageWidth, float imageHeight,
            float uvWidth, float uvHeight) {
        boolean changed = writeNeeded || this.tile != tile || this.source != source
                || this.textureSlot != textureSlot
                || this.x != x || this.y != y || this.z != z || this.rotation != rotation
                || this.tileDataX != tileDataX || this.tileDataY != tileDataY
                || this.flipFlags != flipFlags
                || this.tileWidth != tileWidth || this.tileHeight != tileHeight
                || this.offsetX != offsetX || this.offsetY != offsetY
                || this.originX != originX || this.originY != originY
                || this.imageWidth != imageWidth || this.imageHeight != imageHeight
                || this.uvWidth != uvWidth || this.uvHeight != uvHeight;

        seen = true;
        this.tile = tile;
        this.source = source;
        this.textureSlot = textureSlot;
        this.tileDataX = tileDataX;
        this.tileDataY = tileDataY;
        this.flipFlags = flipFlags;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.originX = originX;
        this.originY = originY;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.uvWidth = uvWidth;
        this.uvHeight = uvHeight;
        return changed;
    }

    boolean updateDecals(float[] tile, float[] x, float[] y, float[] scale) {
        boolean changed = false;
        for (int i = 0; i < DECAL_LAYERS; i++) {
            float nextTile = tile != null ? tile[i] : -1f;
            float nextX = x != null ? x[i] : 0f;
            float nextY = y != null ? y[i] : 0f;
            float nextScale = scale != null ? scale[i] : 0f;
            changed |= decalTile[i] != nextTile
                    || decalX[i] != nextX
                    || decalY[i] != nextY
                    || decalScale[i] != nextScale;
            decalTile[i] = nextTile;
            decalX[i] = nextX;
            decalY[i] = nextY;
            decalScale[i] = nextScale;
        }
        return changed;
    }
}
