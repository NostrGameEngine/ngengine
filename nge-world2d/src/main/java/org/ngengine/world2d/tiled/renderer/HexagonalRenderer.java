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

import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.enums.StaggerAxis;
import org.ngengine.world2d.tiled.enums.StaggerIndex;
import org.ngengine.world2d.tiled.math2d.Point;
import org.ngengine.world2d.tiled.renderer.shape.HexGrid;
import org.ngengine.world2d.tiled.renderer.shape.Hexagon;
import org.ngengine.world2d.tiled.renderer.shape.Rect;

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

    private boolean doStaggerY(int y) {
        return ((y & 1) ^ staggerIndex) == 0;
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

    @Override
    public void tileToGridSpace(float x, float y, Vector2f out) {
        tileToWorldSpace(x, y, out);
    }

    @Override
    public void gridToTile(float x, float y, Point out) {
        // return screenToTileCoords(x, y);
        worldToTile(x, y, out);
    }

    /**
     * Converts tile to screen coordinates. Sub-tile return values are not
     * supported by this renderer.
     */
    @Override
    public void tileToWorldSpace(float x, float y, Vector2f out) {
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

        // return new Vector2f(pixelX, pixelY);
        out.x = pixelX;
        out.y = pixelY;
    }

    /**
     * Converts screen to tile coordinates. Sub-tile return values are not
     * supported by this renderer.
     */
    @Override
    public void worldToTile(float x, float y, Point out) {

        if (staggerX) {
            x -= staggerEven ? tileWidth : sideOffsetX;
        } else {
            y -= staggerEven ? tileHeight : sideOffsetY;
        }

        // Start with the coordinates of a grid-aligned tile
        Point referencePoint = new Point(x / (columnWidth * 2), y / (rowHeight * 2));

        // Relative x and y position on the base square of the grid-aligned tile
        Point rel = new Point(x - referencePoint.getX() * columnWidth * 2, y - referencePoint.getY() * rowHeight * 2);

        // Adjust the reference point to the correct tile coordinates
        adjustReferencePoint(referencePoint);

        // Determine the nearest hexagon tile by the distance to the center
        Point[] centers = new Point[4];

        if (staggerX) {
            float left = sideLengthX * 0.5f;
            float centerX = left + columnWidth;
            float centerY = tileHeight * 0.5f;

            centers[0] = new Point(left, centerY);
            centers[1] = new Point(centerX, centerY - rowHeight);
            centers[2] = new Point(centerX, centerY + rowHeight);
            centers[3] = new Point(centerX + columnWidth, centerY);
        } else {
            float top = sideLengthY * 0.5f;
            float centerX = tileWidth * 0.5f;
            float centerY = top + rowHeight;

            centers[0] = new Point(centerX, top);
            centers[1] = new Point(centerX - columnWidth, centerY);
            centers[2] = new Point(centerX + columnWidth, centerY);
            centers[3] = new Point(centerX, centerY + rowHeight);
        }

        int nearest = 0;
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            float dc = centers[i].distanceSquared(rel);
            if (dc < minDist) {
                minDist = dc;
                nearest = i;
            }
        }

        Point[] offsetsStaggerX = { new Point(0, 0), new Point(1, -1),
                new Point(1, 0), new Point(2, 0) };

        Point[] offsetsStaggerY = { new Point(0, 0), new Point(-1, 1),
                new Point(0, 1), new Point(0, 2) };

        final Point[] offsets = staggerX ? offsetsStaggerX : offsetsStaggerY;
        // return referencePoint.add(offsets[nearest]);
        out.set(referencePoint.getX() + offsets[nearest].getX(),
                referencePoint.getY() + offsets[nearest].getY());
    }

    /**
     * Adjust the reference point to the correct tile coordinates
     *
     * @param referencePoint the reference point
     */
    public void adjustReferencePoint(Point referencePoint) {
        if (staggerX) {
            referencePoint.setX(adjust(referencePoint.getX()));
        } else {
            referencePoint.setY(adjust(referencePoint.getY()));
        }
    }

    private int adjust(int v) {
        v *= 2;
        if (staggerEven) {
            v++;
        }
        return v;
    }

    public void topLeft(int x, int y, Point out) {
        if (staggerX) {
            if (doStaggerX(x)) {
                // return new Point(x - 1, y);
                out.set(x - 1, y);
            } else {
                // return new Point(x - 1, y - 1);
                out.set(x - 1, y - 1);
            }
        } else {
            if (doStaggerY(y)) {
                // return new Point(x, y - 1);
                out.set(x, y - 1);
            } else {
                // return new Point(x - 1, y - 1);
                out.set(x - 1, y - 1);
            }
        }
    }

    public Point topLeft(int x, int y) {
        Point out = new Point();
        topLeft(x, y, out);
        return out;
    }


    public void topRight(int x, int y, Point out) {
        if (staggerX) {
            if (doStaggerX(x)) {
                // return new Point(x + 1, y);
                out.set(x + 1, y);
            } else {
                // return new Point(x + 1, y - 1);
                out.set(x + 1, y - 1);
            }
        } else {
            if (doStaggerY(y)) {
                // return new Point(x + 1, y - 1);
                out.set(x + 1, y - 1);
            } else {
                // return new Point(x, y - 1);
                out.set(x, y - 1);
            }
        }
    }

    public Point topRight(int x, int y) {
        Point out = new Point();
        topRight(x, y, out);
        return out;
    }

    public void bottomLeft(int x, int y, Point out) {
        if (staggerX) {
            if (doStaggerX(x)) {
                // return new Point(x - 1, y + 1);
                out.set(x - 1, y + 1);
            } else {
                // return new Point(x - 1, y);
                out.set(x - 1, y);
            }
        } else {
            if (doStaggerY(y)) {
                // return new Point(x, y + 1);
                out.set(x, y + 1);
            } else {
                // return new Point(x - 1, y + 1);
                out.set(x - 1, y + 1);
            }
        }
    }

    public Point bottomLeft(int x, int y) {
        Point out = new Point();
        bottomLeft(x, y, out);
        return out;
    }

    public void bottomRight(int x, int y, Point out) {
        if (staggerX) {
            if (doStaggerX(x)) {
                // return new Point(x + 1, y + 1);
                out.set(x + 1, y + 1);
            } else {
                // return new Point(x + 1, y);
                out.set(x + 1, y);
            }
        } else {
            if (doStaggerY(y)) {
                // return new Point(x + 1, y + 1);
                out.set(x + 1, y + 1);
            } else {
                // return new Point(x, y + 1);
                out.set(x, y + 1);
            }
        }
    }

    public Point bottomRight(int x, int y) {
        Point out = new Point();
        bottomRight(x, y, out);
        return out;
    }

    @Override
    public float getTopDownYIndex(TiledObjectEntity obj) {
        float y = (float) obj.getY();
        float x = (float) obj.getX();
        float yNorm = y / mapSize.getY();
        float tiebreak = (x / mapSize.getX()) * 1e-3f;
        return (float) ((yNorm + tiebreak) * layerDistance);
    }

}