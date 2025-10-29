package io.github.jmecn.tiled.renderer.shape;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import io.github.jmecn.tiled.enums.StaggerAxis;
import io.github.jmecn.tiled.enums.StaggerIndex;
import io.github.jmecn.tiled.math2d.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class HexGrid extends Mesh {

    private final int tileWidth;
    private final int tileHeight;
    private final int sideLengthX;
    private final int sideLengthY;
    private final int sideOffsetX;
    private final int sideOffsetY;
    private final int rowHeight;
    private final int columnWidth;
    private final boolean staggerX;
    private final boolean staggerEven;
    private final int staggerIndex;

    public HexGrid(int width, int height, int mapTileWidth, int mapTileHeight, int hexSideLength, StaggerAxis staggerAxis, StaggerIndex staggerIndex) {
        staggerX = staggerAxis == StaggerAxis.X;
        staggerEven = staggerIndex == StaggerIndex.EVEN;
        this.staggerIndex = staggerEven ? 0 : 1;

        if (staggerX) {
            sideLengthX = hexSideLength;
            sideLengthY = 0;
        } else {
            sideLengthX = 0;
            sideLengthY = hexSideLength;
        }

        sideOffsetX = (mapTileWidth - sideLengthX) / 2;
        sideOffsetY = (mapTileHeight - sideLengthY) / 2;

        columnWidth = sideOffsetX + sideLengthX;
        rowHeight = sideOffsetY + sideLengthY;

        tileWidth = columnWidth + sideOffsetX;
        tileHeight = rowHeight + sideOffsetY;

        Point startTile = screenToTileCoords(0, 0);
        if (staggerX) {
            staggerX(startTile.getX(), startTile.getY(), width, height);
        } else {
            staggerY(startTile.getX(), startTile.getY(), width, height);
        }
    }

    private void staggerX(int startX, int startY, int width, int height) {
        List<Point> allPoints = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        Map<Long, Integer> existsPointIndex = new HashMap<>();

        boolean staggeredRow = doStaggerX(startX);
        while (startY < height) {
            int rowY = startY;
            for (int rowX = startX; rowX < width; rowX += 2) {
                if (rowX < 0 || rowY < 0) {
                    continue;
                }
                addPoints(allPoints, indexes, existsPointIndex, tileToScreenPolygon(rowX, rowY));
            }

            if (staggeredRow) {
                startX -= 1;
                startY += 1;
                staggeredRow = false;
            } else {
                startX += 1;
                staggeredRow = true;
            }
        }

        setMesh(allPoints, indexes);
    }

    private void staggerY(int startX, int startY, int width, int height) {
        List<Point> allPoints = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        Map<Long, Integer> existsPointIndex = new HashMap<>();

        for (int rowY = startY; rowY < height; rowY++) {
            for (int rowX = startX; rowX < width; rowX++) {
                if (rowX < 0 || rowY < 0) {
                    continue;
                }
                addPoints(allPoints, indexes, existsPointIndex, tileToScreenPolygon(rowX, rowY));
            }
        }

        setMesh(allPoints, indexes);
    }

    private void setMesh(List<Point> allPoints, List<Integer> indexes) {
        float[] vertices = new float[allPoints.size() * 3];
        float[] normals = new float[allPoints.size() * 3];
        float[] texCoord = new float[allPoints.size() * 2];
        for (int i = 0; i < allPoints.size(); i++) {
            Point p = allPoints.get(i);
            vertices[i * 3] = p.getX();
            vertices[i * 3 + 1] = 0.f;
            vertices[i * 3 + 2] = p.getY();

            normals[i * 3] = 0.f;
            normals[i * 3 + 1] = 1.f;
            normals[i * 3 + 2] = 0.f;

            texCoord[i * 2] = 0.f;
            texCoord[i * 2 + 1] = 0.f;
        }

        short[] indices = new short[indexes.size()];
        for (int i = 0; i < indexes.size(); i++) {
            indices[i] = indexes.get(i).shortValue();
        }

        setMode(Mode.Lines);
        setBuffer(VertexBuffer.Type.Position, 3, vertices);
        setBuffer(VertexBuffer.Type.Normal, 3, normals);
        setBuffer(VertexBuffer.Type.TexCoord, 2, texCoord);
        setBuffer(VertexBuffer.Type.Index, 3, indices);
        updateBound();
        updateCounts();
    }

    private void addPoints(List<Point> allPoints, List<Integer> indexes, Map<Long, Integer> existsPointIndex, List<Point> points) {
        // add points, skip if already exists
        for (Point p : points) {
            existsPointIndex.computeIfAbsent(key(p), k -> {
                allPoints.add(p);
                return allPoints.size() - 1;
            });
        }

        // calculate indexes
        int len = points.size();
        for (int i = 0; i < len; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % len);
            indexes.add(existsPointIndex.get(key(p1)));
            indexes.add(existsPointIndex.get(key(p2)));
        }
    }

    private long key(Point p) {
        return p.getX() * 1000000L + p.getY();
    }

    private boolean doStaggerX(int x) {
        return ((x & 1) ^ staggerIndex) == 0;
    }

    private boolean doStaggerY(int y) {
        return ((y & 1) ^ staggerIndex) == 0;
    }

    public List<Point> tileToScreenPolygon(int x, int y) {
        ArrayList<Point> polygon = new ArrayList<>(6);

        polygon.add(new Point(0, rowHeight));
        if (!staggerX) {
            polygon.add(new Point(0, sideOffsetY));
        }
        polygon.add(new Point(sideOffsetX, 0));
        if (staggerX) {
            polygon.add(new Point(columnWidth, 0));
        }
        polygon.add(new Point(tileWidth, sideOffsetY));
        if (!staggerX) {
            polygon.add(new Point(tileWidth, rowHeight));
        }
        polygon.add(new Point(columnWidth, tileHeight));
        if (staggerX) {
            polygon.add(new Point(sideOffsetX, tileHeight));
        }

        Point position = tileToScreenCoords(x, y);
        for(Point p : polygon) {
            p.addLocal(position);
        }
        return polygon;
    }

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
        Point[] centers = getNearestTile();

        int nearest = 0;
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            float dc = centers[i].distanceSquared(rel);
            if (dc < minDist) {
                minDist = dc;
                nearest = i;
            }
        }

        Point[] offsetsStaggerX = { new Point(0, 0), new Point(1, -1), new Point(1, 0), new Point(2, 0) };

        Point[] offsetsStaggerY = { new Point(0, 0), new Point(-1, 1), new Point(0, 1), new Point(0, 2) };

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

    private Point[] getNearestTile() {
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
        return centers;
    }

    /**
     * Converts tile to screen coordinates. Sub-tile return values are not
     * supported by this renderer.
     */
    private Point tileToScreenCoords(int tileX, int tileY) {
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

        return new Point(pixelX, pixelY);
    }
}