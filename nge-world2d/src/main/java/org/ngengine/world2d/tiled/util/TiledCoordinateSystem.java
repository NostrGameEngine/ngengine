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

package org.ngengine.world2d.tiled.util;

import org.jbox2d.common.Vec2;

import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.enums.StaggerAxis;
import org.ngengine.world2d.tiled.enums.StaggerIndex;
import org.ngengine.world2d.tiled.math2d.Point;
import org.ngengine.world2d.tiled.renderer.TileObjectAlignment;

/**
 * Coordinate conversion for one tiled map.
 * <p>
 * Rendering code can own any number of {@code MapRenderer}s for different POVs,
 * but the map-to-world and world-to-physics math is a property of the map
 * itself. This object keeps that math independent from scenegraph rendering.
 * </p>
 */
public final class TiledCoordinateSystem implements CoordinateSystem {
    private final TiledMap map;
    private final Orientation orientation;
    private final int ppm;
    private final int width;
    private final int height;
    private final int sourceTileWidth;
    private final int sourceTileHeight;
    private final Point mapSize;

    private int tileWidth;
    private int tileHeight;
    private int sideLengthX;
    private int sideLengthY;
    private int sideOffsetX;
    private int sideOffsetY;
    private int rowHeight;
    private int columnWidth;
    private boolean staggerX;
    private boolean staggerEven;
    private int staggerIndex;
    private double layerDistance = 16f;
    private final Vec2 physicsTmp = new Vec2();

    /**
     * Creates the coordinate system matching a map orientation.
     *
     * @param map the tiled map
     * @param ppm pixels per physics meter
     * @return a coordinate system for {@code map}
     */
    public static TiledCoordinateSystem create(TiledMap map, int ppm) {
        return new TiledCoordinateSystem(map, ppm);
    }

    private TiledCoordinateSystem(TiledMap map, int ppm) {
        this.map = map;
        this.orientation = map.getOrientation();
        this.ppm = ppm;
        this.width = map.getWidth();
        this.height = map.getHeight();
        this.sourceTileWidth = map.getTileWidth();
        this.sourceTileHeight = map.getTileHeight();
        this.tileWidth = sourceTileWidth;
        this.tileHeight = sourceTileHeight;
        this.mapSize = new Point(width * sourceTileWidth, height * sourceTileHeight);
        if (orientation == Orientation.ISOMETRIC) {
            int side = width + height;
            this.mapSize.set(side * sourceTileWidth * 0.5f, side * sourceTileHeight * 0.5f);
        } else if (orientation == Orientation.HEXAGONAL || orientation == Orientation.STAGGERED) {
            configureStaggeredMetrics();
        }
    }

    /**
     * Returns the effective tile width used by coordinate conversion.
     *
     * @return tile width in world pixels
     */
    public int getTileWidth() {
        return tileWidth;
    }

    /**
     * Returns the effective tile height used by coordinate conversion.
     *
     * @return tile height in world pixels
     */
    public int getTileHeight() {
        return tileHeight;
    }

    /**
     * Returns the full map size in world pixels.
     *
     * @return map dimensions
     */
    public Point getMapDimension() {
        return mapSize;
    }

    /**
     * Sets the depth range used to map logical top-down order into renderer Y.
     *
     * @param layerDistance distance between logical layers
     */
    public void setLayerDistance(double layerDistance) {
        this.layerDistance = layerDistance;
    }

    private void configureStaggeredMetrics() {
        staggerX = map.getStaggerAxis() == StaggerAxis.X;
        staggerEven = map.getStaggerIndex() == StaggerIndex.EVEN;
        staggerIndex = staggerEven ? 0 : 1;

        sideLengthX = sideLengthY = 0;
        if (staggerX) {
            sideLengthX = map.getHexSideLength();
        } else {
            sideLengthY = map.getHexSideLength();
        }

        sideOffsetX = (sourceTileWidth - sideLengthX) / 2;
        sideOffsetY = (sourceTileHeight - sideLengthY) / 2;

        columnWidth = sideOffsetX + sideLengthX;
        rowHeight = sideOffsetY + sideLengthY;

        tileWidth = columnWidth + sideOffsetX;
        tileHeight = rowHeight + sideOffsetY;

        int mapWidth;
        int mapHeight;
        if (staggerX) {
            mapWidth = width * columnWidth + sideOffsetX;
            mapHeight = height * (tileHeight + sideLengthY);
            if (width > 1) {
                mapHeight += rowHeight;
            }
        } else {
            mapWidth = width * (tileWidth + sideLengthX);
            mapHeight = height * rowHeight + sideOffsetY;
            if (height > 1) {
                mapWidth += columnWidth;
            }
        }
        mapSize.set(mapWidth, mapHeight);
    }

    @Override
    public void gridToWorldSpace(float x, float y, Vector2f out) {
        if (orientation == Orientation.ISOMETRIC) {
            out.x = (sourceTileWidth / (2f * sourceTileHeight)) * (x - y) + height * sourceTileWidth * 0.5f;
            out.y = 0.5f * (x + y);
            return;
        }
        out.x = x;
        out.y = y;
    }

    @Override
    public void gridToTile(float x, float y, Point out) {
        if (orientation == Orientation.ISOMETRIC) {
            out.set((int) Math.floor(x * (1f / sourceTileHeight)),
                    (int) Math.floor(y * (1f / sourceTileHeight)));
        } else if (orientation == Orientation.HEXAGONAL || orientation == Orientation.STAGGERED) {
            worldToTile(x, y, out);
        } else {
            out.set(x / tileWidth, y / tileHeight);
        }
    }

    @Override
    public void tileToGridSpace(float x, float y, Vector2f out) {
        if (orientation == Orientation.ISOMETRIC) {
            out.set(x * sourceTileHeight, y * sourceTileHeight);
        } else if (orientation == Orientation.HEXAGONAL || orientation == Orientation.STAGGERED) {
            tileToWorldSpace(x, y, out);
        } else {
            out.x = x * tileWidth;
            out.y = y * tileHeight;
        }
    }

    @Override
    public void tileToWorldSpace(float x, float y, Vector2f out) {
        if (orientation == Orientation.ISOMETRIC) {
            out.x = (height + x - y) * sourceTileWidth * 0.5f;
            out.y = (x + y) * sourceTileHeight * 0.5f;
        } else if (orientation == Orientation.HEXAGONAL || orientation == Orientation.STAGGERED) {
            hexTileToWorldSpace(x, y, out);
        } else {
            out.x = x * tileWidth;
            out.y = y * tileHeight;
        }
    }

    @Override
    public void worldToGridSpace(float x, float y, Vector2f out) {
        if (orientation == Orientation.ISOMETRIC) {
            float fx = (x - height * sourceTileWidth * 0.5f) * (1f / sourceTileWidth);
            float fy = y * (1f / sourceTileHeight);
            out.x = (fy + fx) * sourceTileHeight;
            out.y = (fy - fx) * sourceTileHeight;
        } else {
            out.x = x;
            out.y = y;
        }
    }

    @Override
    public void worldToTile(float x, float y, Point out) {
        if (orientation == Orientation.ISOMETRIC) {
            final float fx = (x - height * sourceTileWidth * 0.5f) * (1f / sourceTileWidth);
            final float fy = y * (1f / sourceTileHeight);
            out.set((int) Math.floor(fy + fx), (int) Math.floor(fy - fx));
        } else if (orientation == Orientation.STAGGERED) {
            staggeredWorldToTile(x, y, out);
        } else if (orientation == Orientation.HEXAGONAL) {
            hexWorldToTile(x, y, out);
        } else {
            out.set(x / tileWidth, y / tileHeight);
        }
    }

    @Override
    public void worldToPhysicsSpace(float x, float y, Vector2f out) {
        out.x = x / ppm;
        out.y = y / ppm;
    }

    @Override
    public void physicsToWorldSpace(float x, float y, Vector2f out) {
        physicsTmp.x = x;
        physicsTmp.y = y;
        physicsToWorldSpace(physicsTmp, out);
    }

    @Override
    public void physicsToWorldSpace(Vec2 physicsWorldCoords, Vector2f out) {
        out.x = physicsWorldCoords.x * ppm;
        out.y = physicsWorldCoords.y * ppm;
    }

    @Override
    public float getTopDownYIndex(TiledObjectEntity obj) {
        if (orientation == Orientation.ISOMETRIC) {
            return getTopDownYIndex((float) obj.getX(), (float) obj.getY());
        }
        if (orientation == Orientation.HEXAGONAL) {
            float yNorm = (float) obj.getY() / mapSize.getY();
            float tiebreak = ((float) obj.getX() / mapSize.getX()) * 1e-3f;
            return (float) ((yNorm + tiebreak) * layerDistance);
        }
        return getTopDownYIndex((float) obj.getX(), (float) obj.getY());
    }

    @Override
    public float getTopDownYIndex(float x, float y) {
        if (orientation == Orientation.ISOMETRIC) {
            // Match Tiled's editor renderer: TopDown object layers use the
            // screen-space Y of pixelToScreenCoords(object->position()).
            float screenY = 0.5f * (x + y);
            float mapScreenHeight = Math.max(1f, mapSize.getY());
            return (float) ((screenY / mapScreenHeight) * layerDistance);
        }
        float tileY = y / mapSize.getY();
        return (float) (tileY * layerDistance);
    }

    @Override
    public void getCollisionCenterInGridSpace(TiledObjectEntity parent, TiledObjectEntity coll, Vector2f out) {
        if (orientation == Orientation.ISOMETRIC) {
            getIsometricCollisionCenterInGridSpace(parent, coll, out);
        } else {
            getOrthogonalCollisionCenterInGridSpace(parent, coll, out);
        }
    }

    @Override
    public void getCenterInGridSpace(TiledBase v, Vector2f out) {
        if (v == null) {
            out.set(0, 0);
            return;
        }
        if (v instanceof TiledTileEntity) {
            getTileCenterInGridSpace((TiledTileEntity) v, out);
        } else if (v instanceof TiledObjectEntity) {
            getObjectCenterInGridSpace((TiledObjectEntity) v, out);
        } else {
            getPositionInGridSpace(v, out);
        }
    }

    private void hexTileToWorldSpace(float x, float y, Vector2f out) {
        int tileX = (int) Math.floor(x);
        int tileY = (int) Math.floor(y);
        int pixelX;
        int pixelY;

        if (staggerX) {
            pixelY = tileY * tileHeight;
            if (doStaggerX(tileX)) {
                pixelY += rowHeight;
            }
            pixelX = tileX * columnWidth;
        } else {
            pixelX = tileX * tileWidth;
            if (doStaggerY(tileY)) {
                pixelX += columnWidth;
            }
            pixelY = tileY * rowHeight;
        }
        out.x = pixelX;
        out.y = pixelY;
    }

    private void hexWorldToTile(float x, float y, Point out) {
        if (staggerX) {
            x -= staggerEven ? tileWidth : sideOffsetX;
        } else {
            y -= staggerEven ? tileHeight : sideOffsetY;
        }

        int referenceX = (int) Math.floor(x / (columnWidth * 2));
        int referenceY = (int) Math.floor(y / (rowHeight * 2));
        float relX = x - referenceX * columnWidth * 2;
        float relY = y - referenceY * rowHeight * 2;
        if (staggerX) {
            referenceX = adjust(referenceX);
        } else {
            referenceY = adjust(referenceY);
        }

        float center0X;
        float center0Y;
        float center1X;
        float center1Y;
        float center2X;
        float center2Y;
        float center3X;
        float center3Y;
        if (staggerX) {
            float left = sideLengthX * 0.5f;
            float centerX = left + columnWidth;
            float centerY = tileHeight * 0.5f;
            center0X = left;
            center0Y = centerY;
            center1X = centerX;
            center1Y = centerY - rowHeight;
            center2X = centerX;
            center2Y = centerY + rowHeight;
            center3X = centerX + columnWidth;
            center3Y = centerY;
        } else {
            float top = sideLengthY * 0.5f;
            float centerX = tileWidth * 0.5f;
            float centerY = top + rowHeight;
            center0X = centerX;
            center0Y = top;
            center1X = centerX - columnWidth;
            center1Y = centerY;
            center2X = centerX + columnWidth;
            center2Y = centerY;
            center3X = centerX;
            center3Y = centerY + rowHeight;
        }

        int nearest = 0;
        float minDist = distanceSquared(relX, relY, center0X, center0Y);
        float dist = distanceSquared(relX, relY, center1X, center1Y);
        if (dist < minDist) {
            minDist = dist;
            nearest = 1;
        }
        dist = distanceSquared(relX, relY, center2X, center2Y);
        if (dist < minDist) {
            minDist = dist;
            nearest = 2;
        }
        dist = distanceSquared(relX, relY, center3X, center3Y);
        if (dist < minDist) {
            nearest = 3;
        }

        if (staggerX) {
            switch (nearest) {
                case 1: out.set(referenceX + 1, referenceY - 1); return;
                case 2: out.set(referenceX + 1, referenceY); return;
                case 3: out.set(referenceX + 2, referenceY); return;
                default: out.set(referenceX, referenceY); return;
            }
        }
        switch (nearest) {
            case 1: out.set(referenceX - 1, referenceY + 1); return;
            case 2: out.set(referenceX, referenceY + 1); return;
            case 3: out.set(referenceX, referenceY + 2); return;
            default: out.set(referenceX, referenceY); return;
        }
    }

    private void staggeredWorldToTile(float x, float y, Point out) {
        float alignedX = x;
        float alignedY = y;
        if (staggerX) {
            alignedX -= staggerEven ? sideOffsetX : 0;
        } else {
            alignedY -= staggerEven ? sideOffsetY : 0;
        }

        int referenceX = (int) Math.floor(alignedX / tileWidth);
        int referenceY = (int) Math.floor(alignedY / tileHeight);
        float relX = alignedX - referenceX * tileWidth;
        float relY = alignedY - referenceY * tileHeight;
        if (staggerX) {
            referenceX = adjust(referenceX);
        } else {
            referenceY = adjust(referenceY);
        }

        float yPos = relX * ((float) tileHeight / tileWidth);
        if (sideOffsetY - yPos > relY) {
            topLeft(referenceX, referenceY, out);
        } else if (-sideOffsetY + yPos > relY) {
            topRight(referenceX, referenceY, out);
        } else if (sideOffsetY + yPos < relY) {
            bottomLeft(referenceX, referenceY, out);
        } else if (sideOffsetY * 3 - yPos < relY) {
            bottomRight(referenceX, referenceY, out);
        } else {
            out.set(referenceX, referenceY);
        }
    }

    private float distanceSquared(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private int adjust(int v) {
        v *= 2;
        if (staggerEven) {
            v++;
        }
        return v;
    }

    private boolean doStaggerX(int x) {
        return ((x & 1) ^ staggerIndex) == 0;
    }

    private boolean doStaggerY(int y) {
        return ((y & 1) ^ staggerIndex) == 0;
    }

    private void topLeft(int x, int y, Point out) {
        if (staggerX) {
            out.set(x - 1, doStaggerX(x) ? y : y - 1);
        } else {
            out.set(doStaggerY(y) ? x : x - 1, y - 1);
        }
    }

    private void topRight(int x, int y, Point out) {
        if (staggerX) {
            out.set(x + 1, doStaggerX(x) ? y : y - 1);
        } else {
            out.set(doStaggerY(y) ? x + 1 : x, y - 1);
        }
    }

    private void bottomLeft(int x, int y, Point out) {
        if (staggerX) {
            out.set(x - 1, doStaggerX(x) ? y + 1 : y);
        } else {
            out.set(doStaggerY(y) ? x : x - 1, y + 1);
        }
    }

    private void bottomRight(int x, int y, Point out) {
        if (staggerX) {
            out.set(x + 1, doStaggerX(x) ? y + 1 : y);
        } else {
            out.set(doStaggerY(y) ? x + 1 : x, y + 1);
        }
    }

    private void getOrthogonalCollisionCenterInGridSpace(TiledObjectEntity parent, TiledObjectEntity coll,
            Vector2f out) {
        Tile tile = parent.getTile();
        float dx = (float) ((coll.getX() + coll.getWidth() / 2.) / (double) tile.getWidth());
        float dy = (float) ((coll.getY() + coll.getHeight() / 2.) / (double) tile.getHeight());
        out.x = (float) parent.getX() + dx * (float) parent.getWidth();
        out.y = (float) parent.getHeight() - dy * (float) parent.getHeight();
    }

    private void getIsometricCollisionCenterInGridSpace(TiledObjectEntity parent, TiledObjectEntity coll,
            Vector2f out) {
        Tile tile = parent.getTile();
        if (tile == null) {
            out.set((float) (parent.getX() + coll.getX() + coll.getWidth() * 0.5),
                    (float) (parent.getY() + coll.getY() + coll.getHeight() * 0.5));
            return;
        }

        float wTile = (float) tile.getWidth();
        float hTile = (float) tile.getHeight();
        float wObj = (float) parent.getWidth();
        float hObj = (float) parent.getHeight();
        float wScale = wObj / wTile;
        float hScale = hObj / hTile;

        float bx = (float) coll.getX();
        float by = (float) coll.getY();
        bx += -wTile * 0.5f;
        by += -hTile;
        bx = (bx / wTile) * wObj;
        by = (by / hTile) * hObj;

        float w = (float) coll.getWidth() * wScale;
        float h = (float) coll.getHeight() * hScale;
        if (tile.isFlippedHorizontally()) bx = -(bx + w);
        if (tile.isFlippedVertically()) by = -hObj - (by + h);

        float lx = bx + 0.5f * w;
        float ly = by + 0.5f * h;
        try (TempVars vars = TempVars.get()) {
            Vector2f parentWorld = vars.vect2d;
            getPositionInGridSpace(parent, parentWorld);
            Vector2f parentScreen = vars.vect2d2;
            gridToWorldSpace(parentWorld.x, parentWorld.y, parentScreen);
            worldToGridSpace(parentScreen.x + lx, parentScreen.y + ly, out);
        }
    }

    private void getTileCenterInGridSpace(TiledTileEntity entry, Vector2f out) {
        Tile tile = entry.getTile();
        if (tile == null) {
            getPositionInGridSpace(entry, out);
            return;
        }

        float offsetX = 0f;
        float offsetY = 0f;
        if (tile.getTileset() != null && tile.getTileset().getTileOffset() != null) {
            offsetX = tile.getTileset().getTileOffset().x;
            offsetY = tile.getTileset().getTileOffset().y;
        }

        float originX = orientation == Orientation.ISOMETRIC ? (float) -tile.getWidth() * 0.5f : 0f;
        float originY = orientation == Orientation.ISOMETRIC ? sourceTileHeight : tileHeight;
        float centerLocalX = originX + offsetX + (float) tile.getWidth() * 0.5f;
        float centerLocalY = originY + offsetY - (float) tile.getHeight() * 0.5f;

        try (TempVars vars = TempVars.get()) {
            Vector2f baseWorld = vars.vect2d;
            tileToWorldSpace((float) entry.getX(), (float) entry.getY(), baseWorld);

            Vector2f worldCenter = vars.vect2d2;
            worldCenter.set(baseWorld.x + centerLocalX, baseWorld.y + centerLocalY);
            worldToGridSpace(worldCenter.x, worldCenter.y, out);
        }
    }

    private void getObjectCenterInGridSpace(TiledObjectEntity obj, Vector2f out) {
        float centerX;
        float centerY;

        if (obj.getShape() == ObjectShape.TILE) {
            Tile tile = obj.getTile();
            if (tile != null) {
                float width = (float) obj.getWidth();
                float height = (float) obj.getHeight();
                float offsetX = 0f;
                float offsetY = 0f;
                if (tile.getTileset() != null && tile.getTileset().getTileOffset() != null) {
                    offsetX = tile.getTileset().getTileOffset().x;
                    offsetY = tile.getTileset().getTileOffset().y;
                }

                Vector2f origin = TileObjectAlignment.origin(orientation, tile, width, height);
                try (TempVars vars = TempVars.get()) {
                    Vector2f baseWorld = vars.vect2d;
                    gridToWorldSpace((float) obj.getX(), (float) obj.getY(), baseWorld);

                    Vector2f worldCenter = vars.vect2d2;
                    worldCenter.set(
                        baseWorld.x + origin.x + offsetX + width * 0.5f,
                        baseWorld.y + origin.y + offsetY - height * 0.5f
                    );
                    worldToGridSpace(worldCenter.x, worldCenter.y, out);
                }
                return;
            }
            centerX = (float) (obj.getX() + obj.getWidth() * 0.5);
            centerY = (float) (obj.getY() - obj.getHeight() * 0.5);
        } else {
            centerX = (float) (obj.getX() + obj.getWidth() * 0.5);
            centerY = (float) (obj.getY() + obj.getHeight() * 0.5);
        }

        out.set(centerX, centerY);
    }
}
