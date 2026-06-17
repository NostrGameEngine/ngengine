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

import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.DrawOrder;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.enums.RenderingMode;
import org.ngengine.world2d.tiled.enums.StaggerAxis;

/**
 * Chooses conservative instancing groups for map orientations where instances
 * inside one batch can be rendered in any order.
 */
class InstancedBatchingPolicy {
    private int tileChunkSize = 32;
    private int isometricDiagonalSpan = 1;
    private int objectBatchHeight = 1;
    private int objectBatchSize = 1;
    private float hexStaggerLineHeight = 1f;
    private float hexStaggerSameLineSpacing = 1f;
    private boolean hexStaggerLineBatchingSafe = true;

    void configureDefaults(TiledMap map) {
        int maxTileVisualHeight = maxTileVisualHeight(map);
        int maxTileVisualWidth = maxTileVisualWidth(map);
        isometricDiagonalSpan = 1;
        objectBatchHeight = Math.max(1, maxTileVisualHeight);
        objectBatchSize = 1;
        configureHexStaggerDefaults(map, maxTileVisualWidth);
    }

    int getTileChunkSize() {
        return tileChunkSize;
    }

    void setTileChunkSize(int tileChunkSize) {
        this.tileChunkSize = Math.max(1, tileChunkSize);
    }

    int getIsometricDiagonalSpan() {
        return isometricDiagonalSpan;
    }

    void setIsometricDiagonalSpan(int isometricDiagonalSpan) {
        this.isometricDiagonalSpan = Math.max(1, isometricDiagonalSpan);
    }

    int getObjectBatchHeight() {
        return objectBatchHeight;
    }

    void setObjectBatchHeight(int objectBatchHeight) {
        this.objectBatchHeight = Math.max(1, objectBatchHeight);
    }

    int getObjectBatchSize() {
        return objectBatchSize;
    }

    void setObjectBatchSize(int objectBatchSize) {
        this.objectBatchSize = Math.max(1, objectBatchSize);
    }

    RenderingMode resolve(TiledLayer layer, boolean objectLayer, Orientation orientation) {
        RenderingMode mode = layer.getRenderingMode();
        if (mode != RenderingMode.AUTO) {
            if (objectLayer && mode != RenderingMode.MULTI_DRAW
                    && orientation != Orientation.ORTHOGONAL) {
                return RenderingMode.MULTI_DRAW;
            }
            return mode;
        }
        if (objectLayer) {
            if (!(layer instanceof TiledObjectLayer)) {
                return RenderingMode.MULTI_DRAW;
            }
            TiledObjectLayer objectLayerRef = (TiledObjectLayer) layer;
            if (objectLayerRef.getDrawOrder() != DrawOrder.TOPDOWN) {
                return RenderingMode.MULTI_DRAW;
            }
            // Object tile sprites can be stretched or arbitrarily sized. AUTO keeps
            // their normal per-object ordering unless the map author opts in.
            return RenderingMode.MULTI_DRAW;
        }
        if (orientation == Orientation.ISOMETRIC) {
            return RenderingMode.INSTANCED_BATCH_CULLED;
        }
        if (orientation == Orientation.HEXAGONAL || orientation == Orientation.STAGGERED) {
            return RenderingMode.INSTANCED_BATCH_CULLED;
        }
        return RenderingMode.INSTANCED_CULLED;
    }

    int resolveTileIsometricDiagonalSpan() {
        return isometricDiagonalSpan;
    }

    int resolveObjectBatchHeight() {
        return objectBatchHeight;
    }

    int tileDrawGroup(int x, int y, int tileZIndex, float pixelY,
            Orientation orientation, int mapWidth, int isometricDiagonalSpan) {
        if (orientation == Orientation.ISOMETRIC) {
            return Math.floorDiv(x + y, isometricDiagonalSpan);
        }
        if (orientation == Orientation.HEXAGONAL || orientation == Orientation.STAGGERED) {
            if (!hexStaggerLineBatchingSafe) {
                return tileZIndex;
            }
            return Math.round(pixelY / hexStaggerLineHeight);
        }
        int chunksX = Math.max(1, Math.floorDiv(mapWidth + tileChunkSize - 1, tileChunkSize));
        return Math.floorDiv(y, tileChunkSize) * chunksX + Math.floorDiv(x, tileChunkSize);
    }

    int objectDrawGroup(TiledObjectLayer layer, TiledObjectEntity object, int sortedIndex,
            int objectBatchHeight) {
        if (layer.getDrawOrder() == DrawOrder.TOPDOWN) {
            return (int) Math.floor(object.getY() / objectBatchHeight);
        }
        return sortedIndex;
    }

    private int maxTileVisualHeight(TiledMap map) {
        int max = Math.max(1, map.getTileHeight());
        for (Tileset tileset : map.getTileSets()) {
            int offsetY = Math.round(Math.abs(tileset.getTileOffset().y));
            max = Math.max(max, tileset.getTileHeight() + offsetY);
            for (Tile tile : tileset) {
                max = Math.max(max, tile.getHeight() + offsetY);
            }
        }
        return max;
    }

    private int maxTileVisualWidth(TiledMap map) {
        int max = Math.max(1, map.getTileWidth());
        for (Tileset tileset : map.getTileSets()) {
            int offsetX = Math.round(Math.abs(tileset.getTileOffset().x));
            max = Math.max(max, tileset.getTileWidth() + offsetX * 2);
            for (Tile tile : tileset) {
                max = Math.max(max, tile.getWidth() + offsetX * 2);
            }
        }
        return max;
    }

    private void configureHexStaggerDefaults(TiledMap map, int maxTileVisualWidth) {
        int hexSideLength = map.getOrientation() == Orientation.HEXAGONAL ? map.getHexSideLength() : 0;
        if (map.getStaggerAxis() == StaggerAxis.X) {
            int sideOffsetX = (map.getTileWidth() - hexSideLength) / 2;
            int columnWidth = sideOffsetX + hexSideLength;
            hexStaggerLineHeight = Math.max(1f, map.getTileHeight() * 0.5f);
            hexStaggerSameLineSpacing = Math.max(1f, columnWidth * 2f);
        } else {
            hexStaggerLineHeight = Math.max(1f, (map.getTileHeight() + hexSideLength) * 0.5f);
            hexStaggerSameLineSpacing = Math.max(1f, map.getTileWidth());
        }
        hexStaggerLineBatchingSafe = maxTileVisualWidth <= hexStaggerSameLineSpacing;
    }

}
