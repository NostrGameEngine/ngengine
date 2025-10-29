package io.github.jmecn.tiled.renderer;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import io.github.jmecn.tiled.core.TiledMap;
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

    public IsometricRenderer(TiledMap tiledMap) {
        super(tiledMap);
        
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
        for(int p = 0; p < height + width - 1; p++) {
            for(int y = 0; y <= p; y++) {
                int x = p - y;
                if(y < height && x < width) {
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
    public Point pixelToTileCoords(float x, float y) {
        return new Point(x / tileHeight, y / tileHeight);
    }

    @Override
    public Vector2f tileToPixelCoords(float x, float y) {
        return new Vector2f(x * tileHeight, y * tileHeight);
    }

    @Override
    public Point screenToTileCoords(float x, float y) {
        x -= height * tileWidth * 0.5f;
        float tileY = y / tileHeight;
        float tileX = x / tileWidth;
        
        return new Point(tileY + tileX, tileY - tileX);
    }
    
    @Override
    public Vector2f tileToScreenCoords(float x, float y) {
        return new Vector2f((height + x - y) * tileWidth * 0.5f, (x + y) * tileHeight * 0.5f);
    }
    
    @Override
    public Vector2f screenToPixelCoords(float x, float y) {
        
        x -= height * tileWidth * 0.5f;
        float tileY = y / tileHeight;
        float tileX = x / tileWidth;

        return new Vector2f((tileY + tileX) * tileHeight,
                       (tileY - tileX) * tileHeight);
    }

    @Override
    public Vector2f pixelToScreenCoords(float x, float y) {
        
        final float tileY = y / tileHeight;
        final float tileX = x / tileHeight;
        
        return new Vector2f((height + tileX - tileY) * tileWidth * 0.5f, (tileX + tileY) * tileHeight * 0.5f);
    }

}
