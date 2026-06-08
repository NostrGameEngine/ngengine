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

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.enums.RenderOrder;
import org.ngengine.world2d.tiled.math2d.Point;
import org.ngengine.world2d.tiled.renderer.shape.OrthoGrid;
import org.ngengine.world2d.tiled.renderer.shape.Rect;

/**
 * Orthogonal render
 * 
 * @author yanmaoyuan
 *
 */
public class OrthogonalRenderer extends MapRenderer {

    public OrthogonalRenderer(TiledMap tiledMap, int PPM, Node rootNode) {
        super(tiledMap, PPM, rootNode);
    }

    public float getTileZAxis(float x, float y) {
        float z;
        switch (tiledMap.getRenderOrder()) {
            case RIGHT_UP:
                z = (height - 1 - y) * width + x;
                break;
            case LEFT_DOWN:
                z = y * width + (width - 1 - x);
                break;
            case LEFT_UP:
                z = (height - 1 - y) * width + (width - 1 - x);
                break;
            case RIGHT_DOWN:
            default:
                z = y * width + x;
                break;
        }
        return (float) (z * step);
    }

    @Override
    public Spatial createTileGrid(Material material) {
        Mesh mesh = new Rect(tileWidth, tileHeight, true);
        Geometry geom = new Geometry("TileGrid", mesh);
        geom.setMaterial(material);
        return geom;
    }

    @Override
    public void visitTiles(TileVisitor visitor) {
        int startX = 0;
        int startY = 0;
        int endX = width - 1;
        int endY = height - 1;

        int incX = 1;
        int incY = 1;
        int tmp;
        RenderOrder renderOrder = tiledMap.getRenderOrder();
        switch (renderOrder) {
            case RIGHT_UP: {
                // swap y
                tmp = endY;
                endY = startY;
                startY = tmp;
                incY = -1;
                break;
            }
            case LEFT_DOWN: {
                // swap x
                tmp = endX;
                endX = startX;
                startX = tmp;
                incX = -1;
                break;
            }
            case LEFT_UP: {
                // swap x
                tmp = endX;
                endX = startX;
                startX = tmp;
                incX = -1;

                // swap y
                tmp = endY;
                endY = startY;
                startY = tmp;
                incY = -1;
                break;
            }
            case RIGHT_DOWN: {
                break;
            }
        }
        endX += incX;
        endY += incY;

        int tileZIndex = 0;
        for (int y = startY; y != endY; y += incY) {
            for (int x = startX; x != endX; x += incX) {
                visitor.visit(x, y, tileZIndex);
                tileZIndex++;
            }
        }
    }

    @Override
    public void renderGrid(Node gridVisual, Material gridMaterial) {
        // add boundary
        OrthoGrid grid = new OrthoGrid(width, height, tileWidth, tileHeight);
        Geometry geom = new Geometry("Grid#Boundary", grid);
        geom.setMaterial(gridMaterial);
        gridVisual.attachChild(geom);
    }

    // Coordinates System Convert

    // OrthogonalRenderer, StaggeredRenderer, HexagonalRenderer
    @Override
    public void gridToWorldSpace(float x, float y, Vector2f out) {
        out.x = x;
        out.y = y;
    }

    @Override
    public void gridToTile(float x, float y, Point out) {
        out.set(x / tileWidth, y / tileHeight);
    }

    @Override
    public void tileToGridSpace(float x, float y, Vector2f out) {
        // return new Vector2f(x * tileWidth, y * tileHeight);
        out.x = x * tileWidth;
        out.y = y * tileHeight;
    }

    @Override
    public void tileToWorldSpace(float x, float y, Vector2f out) {
        // return new Vector2f(x * tileWidth, y * tileHeight);
        out.x = x * tileWidth;
        out.y = y * tileHeight;
    }

    @Override
    public void worldToGridSpace(float x, float y, Vector2f out) {
        // return new Vector2f(x, y);
        out.x = x;
        out.y = y;
    }

    @Override
    public void worldToTile(float x, float y, Point out) {
        // return new Point(x / tileWidth, y / tileHeight);
        out.set(x / tileWidth, y / tileHeight);
    }

    @Override
    public float getTopDownYIndex(TiledObjectEntity obj) {
        float y = (float) obj.getY();
        float x = (float) obj.getX();
        return getTopDownYIndex(x, y);

    }

    @Override
    public float getTopDownYIndex(float x, float y) {
        float tileY = y / mapSize.getY();
        return (float) (tileY * layerDistance);
    }

    public void getCollisionCenterInGridSpace(TiledObjectEntity parentTileObj, TiledObjectEntity coll,
            Vector2f out) {
        try (TempVars vars = TempVars.get()) {
            Tile tile = parentTileObj.getTile();
            Vector2f pos = vars.vect2d;
            pos.set((float) parentTileObj.getX(), (float) parentTileObj.getY());
            float dx = (float) ((coll.getX() + coll.getWidth() / 2.) / (double) tile.getWidth());
            float dy = (float) ((coll.getY() + coll.getHeight() / 2.) / (double) tile.getHeight());
            pos.x += dx * (float) parentTileObj.getWidth();
            pos.y = (float) parentTileObj.getHeight() - dy * (float) parentTileObj.getHeight();
            out.set(pos.x, pos.y);
        }
    }

    

    @Override
    public void getCenterInGridSpace(TiledBase v, Vector2f out) {
    
        if (v == null) {
            out.set(0, 0);
            return;
        }
        if(v instanceof TiledTileEntity){
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

            float originX = 0f;
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
        } else if(v instanceof TiledObjectEntity){
            TiledObjectEntity obj = (TiledObjectEntity) v;
            float centerX;
            float centerY;

            if (obj.getShape() == ObjectShape.TILE) {

                // For orthogonal tiles, alignment is bottom-left
                centerX = (float) (obj.getX() + obj.getWidth() * 0.5);
                centerY = (float) (obj.getY() - obj.getHeight() * 0.5);
            } else {
                // For other shapes, (x,y) is top-left in Tiled
                centerX = (float) (obj.getX() + obj.getWidth() * 0.5);
                centerY = (float) (obj.getY() + obj.getHeight() * 0.5);
            }

            out.set(centerX, centerY);

        }
    }

}
