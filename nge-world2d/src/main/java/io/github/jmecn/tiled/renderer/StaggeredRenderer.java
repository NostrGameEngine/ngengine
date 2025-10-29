package io.github.jmecn.tiled.renderer;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.math2d.Point;
import io.github.jmecn.tiled.renderer.shape.Diamond;

/**
 * Staggered render
 * 
 * @author yanmaoyuan
 * 
 */
public class StaggeredRenderer extends HexagonalRenderer {

    public StaggeredRenderer(TiledMap tiledMap) {
        super(tiledMap);
    }

    @Override
    public Spatial createTileGrid(Material material) {
        // create a grid
        Diamond mesh = new Diamond(tiledMap.getTileWidth(), tiledMap.getTileHeight(), true);
        Geometry geom = new Geometry("TileGrid", mesh);
        geom.setMaterial(material);
        return geom;
    }

    /**
     * Converts screen to tile coordinates. Sub-tile return values are not
     * supported by this renderer.
     * This override exists because the method used by the HexagonalRenderer
     * does not produce nice results for isometric shapes in the tile corners.
     */
    @Override
    public Point screenToTileCoords(float x, float y) {
        float alignedX = x;
        float alignedY = y;
        if (staggerX) {
            alignedX -= staggerEven ? sideOffsetX : 0;
        } else {
            alignedY -= staggerEven ? sideOffsetY : 0;
        }

        // Start with the coordinates of a grid-aligned tile
        Point referencePoint = new Point(alignedX / tileWidth, alignedY / tileHeight);

        // Relative x and y position on the base square of the grid-aligned tile
        Point rel = new Point(
                alignedX - referencePoint.getX() * tileWidth,
                alignedY - referencePoint.getY() * tileHeight);

        // Adjust the reference point to the correct tile coordinates
        adjustReferencePoint(referencePoint);

        float yPos = rel.getX() * ((float) tileHeight / tileWidth);

        // Check whether the cursor is in any of the corners (neighboring tiles)
        if (sideOffsetY - yPos > rel.getY()) {
            return topLeft(referencePoint.getX(), referencePoint.getY());
        } else if (-sideOffsetY + yPos > rel.getY()) {
            return topRight(referencePoint.getX(), referencePoint.getY());
        } else if (sideOffsetY + yPos < rel.getY()) {
            return bottomLeft(referencePoint.getX(), referencePoint.getY());
        } else if (sideOffsetY * 3 - yPos < rel.getY()) {
            return bottomRight(referencePoint.getX(), referencePoint.getY());
        }

        return referencePoint;
    }
}
