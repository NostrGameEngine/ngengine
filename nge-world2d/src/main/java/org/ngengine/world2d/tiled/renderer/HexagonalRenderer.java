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
import org.ngengine.world2d.tiled.enums.StaggerAxis;
import org.ngengine.world2d.tiled.enums.StaggerIndex;
import org.ngengine.world2d.tiled.math2d.Point;
import org.ngengine.world2d.tiled.renderer.shape.HexGrid;
import org.ngengine.world2d.tiled.renderer.shape.Hexagon;
import org.ngengine.world2d.tiled.renderer.shape.Rect;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

/**
 * Hexagonal render
 * 
 * @author yanmaoyuan
 * 
 */
public class HexagonalRenderer extends OrthogonalRenderer {

    protected int sideLengthX;
    protected int sideLengthY;
    protected int sideOffsetX;
    protected int sideOffsetY;
    protected int rowHeight;
    protected int columnWidth;
    protected boolean staggerX;
    protected boolean staggerEven;
    protected int staggerIndex;
 
    public HexagonalRenderer(TiledMap tiledMap, int PPM, Node rootNode) {
        super(tiledMap, PPM, rootNode);
        initMetrics(tiledMap);
    }

    public HexagonalRenderer(TiledMap tiledMap, int PPM, Node rootNode, TiledCoordinateSystem coordinateSystem) {
        super(tiledMap, PPM, rootNode, coordinateSystem);
        initMetrics(tiledMap);
    }

    private void initMetrics(TiledMap tiledMap) {
        staggerX = tiledMap.getStaggerAxis() == StaggerAxis.X;
        staggerEven = tiledMap.getStaggerIndex() == StaggerIndex.EVEN;
        staggerIndex = staggerEven ? 0 : 1;

        sideLengthX = sideLengthY = 0;
        if (staggerX) {
            sideLengthX = tiledMap.getHexSideLength();
        } else {
            sideLengthY = tiledMap.getHexSideLength();
        }

        sideOffsetX = (tiledMap.getTileWidth() - sideLengthX) / 2;
        sideOffsetY = (tiledMap.getTileHeight() - sideLengthY) / 2;

        columnWidth = sideOffsetX + sideLengthX;
        rowHeight = sideOffsetY + sideLengthY;

        tileWidth = columnWidth + sideOffsetX;
        tileHeight = rowHeight + sideOffsetY;

        // The map size is the same regardless of which indexes are shifted.
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

    private boolean doStaggerX(int x) {
        return ((x & 1) ^ staggerIndex) == 0;
    }

    @Override
    public Spatial createTileGrid(Material material) {
        Hexagon mesh = new Hexagon(tileWidth, tileHeight, tiledMap.getHexSideLength(), tiledMap.getStaggerAxis(), true);
        Geometry geom = new Geometry("HexGrid", mesh);
        geom.setMaterial(material);
        return geom;
    }

    @Override
    public void visitTiles(TileVisitor visitor) {
        Point startTile = new Point();
        worldToTile(0, 0, startTile);
        if (staggerX) {
            visitStaggerX(startTile, visitor);
        } else {
            visitStaggerY(startTile, visitor);
        }
    }

    public void visitStaggerX(Point startTile, TileVisitor visitor) {
        int tileZIndex = 0;
        int x = startTile.getX();
        int y = startTile.getY();
        boolean staggeredRow = doStaggerX(x);

        while (y < height) {
            for (int rowX = x; rowX < width; rowX += 2) {
                visitor.visit(rowX, y, tileZIndex);
                tileZIndex++;
            }
            if (staggeredRow) {
                x -= 1;
                y += 1;
                staggeredRow = false;
            } else {
                x += 1;
                staggeredRow = true;
            }
        }
    }

    public void visitStaggerY(Point startTile, TileVisitor visitor) {
        int tileZIndex = 0;
        int x = startTile.getX();
        int y = startTile.getY();
        for (int rowY = y; rowY < height; rowY++) {
            for (int rowX = x; rowX < width; rowX++) {
                visitor.visit(rowX, rowY, tileZIndex);
                tileZIndex++;
            }
        }
    }

    @Override
    public void renderGrid(Node gridVisual, Material gridMaterial) {
        Mesh border = new Rect(mapSize.getX(), mapSize.getY(), false);
        Geometry rect = new Geometry("GridBorder", border);
        rect.setMaterial(gridMaterial);
        gridVisual.attachChild(rect);

        HexGrid grid = new HexGrid(width, height, tiledMap.getTileWidth(), tiledMap.getTileHeight(), tiledMap.getHexSideLength(), tiledMap.getStaggerAxis(), tiledMap.getStaggerIndex());
        Geometry geom = new Geometry("HexGrid", grid);
        geom.setMaterial(gridMaterial);
        gridVisual.attachChild(geom);
    }

}
