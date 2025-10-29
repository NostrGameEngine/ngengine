package io.github.jmecn.tiled.renderer;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.enums.StaggerAxis;
import io.github.jmecn.tiled.enums.StaggerIndex;
import io.github.jmecn.tiled.math2d.Point;
import io.github.jmecn.tiled.renderer.shape.HexGrid;
import io.github.jmecn.tiled.renderer.shape.Hexagon;
import io.github.jmecn.tiled.renderer.shape.Rect;

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

    public HexagonalRenderer(TiledMap tiledMap) {
        super(tiledMap);

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
        Point startTile = screenToTileCoords(0, 0);
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
    public Vector2f tileToPixelCoords(float x, float y) {
        return tileToScreenCoords(x, y);
    }

    @Override
    public Point pixelToTileCoords(float x, float y) {
        return screenToTileCoords(x, y);
    }

    /**
     * Converts tile to screen coordinates. Sub-tile return values are not
     * supported by this renderer.
     */
    @Override
    public Vector2f tileToScreenCoords(float x, float y) {
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

        return new Vector2f(pixelX, pixelY);
    }

    /**
     * Converts screen to tile coordinates. Sub-tile return values are not
     * supported by this renderer.
     */
    @Override
    public Point screenToTileCoords(float x, float y) {

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
        return referencePoint.add(offsets[nearest]);
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

    public Point topLeft(int x, int y) {
        if (staggerX) {
            if (doStaggerX(x)) {
                return new Point(x - 1, y);
            } else {
                return new Point(x - 1, y - 1);
            }
        } else {
            if (doStaggerY(y)) {
                return new Point(x, y - 1);
            } else {
                return new Point(x - 1, y - 1);
            }
        }
    }

    public Point topRight(int x, int y) {
        if (staggerX) {
            if (doStaggerX(x)) {
                return new Point(x + 1, y);
            } else {
                return new Point(x + 1, y - 1);
            }
        } else {
            if (doStaggerY(y)) {
                return new Point(x + 1, y - 1);
            } else {
                return new Point(x, y - 1);
            }
        }
    }

    public Point bottomLeft(int x, int y) {
        if (staggerX) {
            if (doStaggerX(x)) {
                return new Point(x - 1, y + 1);
            } else {
                return new Point(x - 1, y);
            }
        } else {
            if (doStaggerY(y)) {
                return new Point(x, y + 1);
            } else {
                return new Point(x - 1, y + 1);
            }
        }
    }

    public Point bottomRight(int x, int y) {
        if (staggerX) {
            if (doStaggerX(x)) {
                return new Point(x + 1, y + 1);
            } else {
                return new Point(x + 1, y);
            }
        } else {
            if (doStaggerY(y)) {
                return new Point(x + 1, y + 1);
            } else {
                return new Point(x, y + 1);
            }
        }
    }
}
