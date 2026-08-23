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

import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import java.util.Arrays;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

/**
 * Prepared texture state for one tileset. Instanced batches and preferred
 * multi-draw materials share the same load-time conversion cache.
 */
final class InstancedTilesetSource {
    Tileset tileset;
    boolean imageBased;
    boolean arrayBased;
    int imageWidth;
    int imageHeight;
    Texture texture;
    TextureArray textureArray;
    String arrayFailureReason;
    boolean normalizationAttempted;
    private int[] layerByTileId = new int[0];
    private Float[] layerValueByTileId = new Float[0];

    void initializeLayerMap(int maxTileId) {
        layerByTileId = new int[Math.max(maxTileId + 1, 0)];
        Arrays.fill(layerByTileId, -1);
        layerValueByTileId = new Float[layerByTileId.length];
    }

    void setLayer(int tileId, int layer) {
        if (tileId < 0 || tileId >= layerByTileId.length) {
            return;
        }
        layerByTileId[tileId] = layer;
        layerValueByTileId[tileId] = Float.valueOf(layer);
    }

    int getLayer(int tileId) {
        if (tileId < 0 || tileId >= layerByTileId.length) {
            return -1;
        }
        return layerByTileId[tileId];
    }

    Float getLayerValue(int tileId) {
        if (tileId < 0 || tileId >= layerValueByTileId.length) {
            return null;
        }
        return layerValueByTileId[tileId];
    }

    Float[] getLayerValues() {
        return layerValueByTileId;
    }
}
