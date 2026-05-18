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

package io.github.jmecn.tiled.renderer;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

import io.github.jmecn.tiled.core.TiledBase;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.entity.TiledTileEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.enums.ObjectShape;
import io.github.jmecn.tiled.enums.Orientation;
import io.github.jmecn.tiled.math2d.Point;
import io.github.jmecn.tiled.renderer.shape.IsoGrid;
import io.github.jmecn.tiled.renderer.shape.IsoRect;

/**
 * Isometric render
 * 
 * @author yanmaoyuan
 *
 */
public class IsometricRenderer extends MapRenderer {

    final float xCenter;
    final float invTW;
    final float invTH;

    public IsometricRenderer(TiledMap tiledMap, int PPM, Node rootNode) {
        super(tiledMap, PPM, rootNode);

        xCenter = height * tileWidth * 0.5f; // horizontal offset
        invTW = 1f / tileWidth;
        invTH = 1f / tileHeight;

        int side = width + height;
        mapSize.set(side * tileWidth * 0.5f, side * tileHeight * 0.5f);
    }

    @Override
    public Spatial createTileGrid(Material material) {
        // create a grid
        IsoRect mesh = new IsoRect(tileWidth, tileHeight, true);
        Geometry geom = new Geometry("TileGrid", mesh);
        geom.setMaterial(material);
        return geom;
    }

    @Override
    public void visitTiles(TileVisitor visitor) {
        int count = 0;
        for (int p = 0; p < height + width - 1; p++) {
            for (int y = 0; y <= p; y++) {
                int x = p - y;
                if (y < height && x < width) {
                    visitor.visit(x, y, count);
                    count++;
                }
            }
        }
    }

    @Override
    public void renderGrid(Node gridVisual, Material gridMaterial) {
        // add boundary
        IsoGrid grid = new IsoGrid(width, height, tileWidth, tileHeight);
        Geometry geom = new Geometry("Grid#Boundary", grid);
        geom.setMaterial(gridMaterial);
        gridVisual.attachChild(geom);
    }

    // Coordinates System Convert
    @Override
    public void worldToTile(float x, float y, Point out) {
        final float fx = (x - height * tileWidth * 0.5f) * (1f / tileWidth);
        final float fy = y * (1f / tileHeight);

        final int i = (int) Math.floor(fy + fx);
        final int j = (int) Math.floor(fy - fx);
        // return new Point(i, j);
        out.set(i, j);

    }

    @Override
    public void gridToTile(float u, float v, Point outPoint) {
        // isometric "grid" is axial pixels => divide by tileHeight to get indices
        final int i = (int) Math.floor(u * (1f / tileHeight));
        final int j = (int) Math.floor(v * (1f / tileHeight));
        // return new Point(i, j);
        outPoint.set(i, j);
    }

    @Override
    public void tileToGridSpace(float i, float j, Vector2f out) {
        out.set(i * tileHeight, j * tileHeight);
    }

    @Override
    public void gridToWorldSpace(float u, float v, Vector2f out) {
        float sx = (tileWidth / (2f * tileHeight)) * (u - v) + height * tileWidth * 0.5f;
        float sy = 0.5f * (u + v);
        // return new Vector2f(sx, sy);
        out.x = sx;
        out.y = sy;
    }

    @Override
    public void worldToGridSpace(float x, float y, Vector2f out) {
        float fx = (x - height * tileWidth * 0.5f) * (1f / tileWidth);
        float fy = y * (1f / tileHeight);
        // return new Vector2f((fy + fx) * tileHeight, (fy - fx) * tileHeight);
        out.x = (fy + fx) * tileHeight;
        out.y = (fy - fx) * tileHeight;
    }

    @Override
    public void tileToWorldSpace(float i, float j, Vector2f out) {
        // return new Vector2f((height + i - j) * tileWidth * 0.5f, (i + j) * tileHeight * 0.5f);
        out.x = (height + i - j) * tileWidth * 0.5f;
        out.y = (i + j) * tileHeight * 0.5f;
    }

    @Override
    public float getTopDownYIndex(TiledObjectEntity obj) {
        final float footY, footX;
        if (obj.getShape() == ObjectShape.TILE) {
            footY = (float) obj.getY();
            footX = (float) obj.getX();
        } else { // shapes are top-left aligned
            footY = (float) (obj.getY() + obj.getHeight());
            footX = (float) (obj.getX() + obj.getWidth() * 0.5f);
        }

        float tileY = footY / tileHeight;
        float tileX = (footX / tileWidth) - (height * 0.5f);

        return getTopDownYIndex(tileX, tileY);
    }

    @Override
    public float getTopDownYIndex(float tileX, float tileY) {
        // both x and y contribute
        float u = tileY + tileX;
        float v = tileY - tileX;

        float denom = (width + height);
        float uNorm = (u + height * 0.5f) / denom;
        uNorm = Math.max(0f, Math.min(1f, uNorm));

        float tiebreak = (v / denom) * 1e-3f;

        return (float) ((uNorm + tiebreak) * layerDistance);
    }

    @Override
    public void getCollisionCenterInGridSpace(TiledObjectEntity parent, TiledObjectEntity coll,
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

      
        Vector2f parentWorld = new Vector2f();
        getPositionInGridSpace(parent, parentWorld);
        Vector2f parentScreen = new Vector2f();
        gridToWorldSpace(parentWorld.x, parentWorld.y, parentScreen);

         Vector2f targetScreen = new Vector2f(parentScreen.x + lx, parentScreen.y + ly);
        worldToGridSpace(targetScreen.x, targetScreen.y, out);
    }

     
    @Override
    public void getCenterInGridSpace(TiledBase v, Vector2f out) {
        if (v == null) {
            out.set(0, 0);
            return;
        }
        if (v instanceof TiledTileEntity) {
            TiledTileEntity entry = (TiledTileEntity) v;

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

            float originX = (float) -tile.getWidth() * 0.5f;
            float originY = tileHeight;

            float centerLocalX = originX + offsetX + (float) tile.getWidth() * 0.5f;
            float centerLocalY = originY + offsetY - (float) tile.getHeight() * 0.5f;

            try (TempVars vars = TempVars.get()) {
                Vector2f baseWorld = vars.vect2d;
                tileToWorldSpace((float) entry.getX(), (float) entry.getY(), baseWorld);

                Vector2f worldCenter = vars.vect2d2;
                worldCenter.set(baseWorld.x + centerLocalX, baseWorld.y + centerLocalY);

                worldToGridSpace(worldCenter.x, worldCenter.y, out);
            }
        } else if (v instanceof TiledObjectEntity) {
            TiledObjectEntity obj = (TiledObjectEntity) v;
            float centerX;
            float centerY;

            if (obj.getShape() == ObjectShape.TILE) {
                // For isometric tiles, we need to adjust both X and Y to move to visual center
                // while keeping screen X constant.
                //
                // In isometric: screenX depends on (tileX - tileY)
                // where tileX = x/tileWidth, tileY = y/tileHeight
                //
                // To keep screenX constant when moving vertically:
                // We need: Δx/tileWidth = Δy/tileHeight
                // Therefore: Δx = Δy * (tileWidth/tileHeight)
                //
                // To move up by halfHeight in pixel space:
                float halfHeight = (float) (obj.getHeight() * 0.5);
                float ratio = (float) tileWidth / (float) tileHeight;

                centerX = (float) obj.getX() - (halfHeight * ratio);
                centerY = (float) obj.getY() - halfHeight;

            } else {
                // For other shapes, (x,y) is top-left in Tiled
                centerX = (float) (obj.getX() + obj.getWidth() * 0.5);
                centerY = (float) (obj.getY() + obj.getHeight() * 0.5);
            }

            out.set(centerX, centerY);
        } else {
            getPositionInGridSpace(v, out);
        }
    }
}
