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
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.enums.RenderOrder;
import org.ngengine.world2d.tiled.renderer.shape.OrthoGrid;
import org.ngengine.world2d.tiled.renderer.shape.Rect;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

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

    public OrthogonalRenderer(TiledMap tiledMap, int PPM, Node rootNode, TiledCoordinateSystem coordinateSystem) {
        super(tiledMap, PPM, rootNode, coordinateSystem);
    }

    public float getTileZAxis(float x, float y) {
        return (float) (getTileZIndex((int) x, (int) y) * step);
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
    protected void visitTiles(TileVisitor visitor, ViewCull cull) {
        if (!cull.active) {
            visitTiles(visitor);
            return;
        }
        if (cull.empty) {
            return;
        }

        int startX = cull.tileMinX;
        int startY = cull.tileMinY;
        int endX = cull.tileMaxX;
        int endY = cull.tileMaxY;
        int incX = 1;
        int incY = 1;
        int tmp;
        switch (tiledMap.getRenderOrder()) {
            case RIGHT_UP:
                tmp = endY;
                endY = startY;
                startY = tmp;
                incY = -1;
                break;
            case LEFT_DOWN:
                tmp = endX;
                endX = startX;
                startX = tmp;
                incX = -1;
                break;
            case LEFT_UP:
                tmp = endX;
                endX = startX;
                startX = tmp;
                incX = -1;
                tmp = endY;
                endY = startY;
                startY = tmp;
                incY = -1;
                break;
            case RIGHT_DOWN:
                break;
        }
        endX += incX;
        endY += incY;

        for (int y = startY; y != endY; y += incY) {
            for (int x = startX; x != endX; x += incX) {
                visitor.visit(x, y, getTileZIndex(x, y));
            }
        }
    }

    private int getTileZIndex(int x, int y) {
        switch (tiledMap.getRenderOrder()) {
            case RIGHT_UP:
                return (height - 1 - y) * width + x;
            case LEFT_DOWN:
                return y * width + (width - 1 - x);
            case LEFT_UP:
                return (height - 1 - y) * width + (width - 1 - x);
            case RIGHT_DOWN:
            default:
                return y * width + x;
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

}
