package io.github.jmecn.tiled.renderer;

import com.jme3.math.Vector2f;
import io.github.jmecn.tiled.core.TileLayer;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.enums.Orientation;
import io.github.jmecn.tiled.math2d.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
class TestStaggeredRenderer {
    TiledMap map;
    StaggeredRenderer renderer;

    @BeforeEach
    public void initMap() {
        map = new TiledMap(10, 10);
        map.setOrientation(Orientation.STAGGERED);
        map.setTileWidth(64);
        map.setTileHeight(32);

        TileLayer layer = new TileLayer(10, 10);
        map.addLayer(layer);

        renderer = new StaggeredRenderer(map);
    }

    @Test void mapSize() {
        Point expect = new Point(10 * 64 + 32, 10 * 16 + 16);
        Point actual = renderer.getMapDimension();
        assertEquals(expect, actual, "map size");
    }

    @Test void screenToTileCoords() {
        assertEquals(new Point(0, 0), renderer.screenToTileCoords(10, 16), "10,16");
        assertEquals(new Point(-1, -1), renderer.screenToTileCoords(5, 5), "5,5");
        assertEquals(new Point(-1, 1), renderer.screenToTileCoords(1, 20), "1,20");
        assertEquals(new Point(0, 1), renderer.screenToTileCoords(64, 32), "64,32");
        assertEquals(new Point(0, -2), renderer.screenToTileCoords(32, -16), "32,-16");
    }

    @Test void tileToScreenCoords() {
        assertEquals(new Vector2f(0f, 0f), renderer.tileToScreenCoords(0, 0), "0,0");
        assertEquals(new Vector2f(64f, 0f), renderer.tileToScreenCoords(1, 0), "1,0");
        assertEquals(new Vector2f(32f, 16f), renderer.tileToScreenCoords(0, 1), "0,1");
        assertEquals(new Vector2f(-32f, -16f), renderer.tileToScreenCoords(-1, -1), "-1,-1");
    }

    @Test void relativeCoordinates() {
        assertEquals(new Point(-1, -1), renderer.topLeft(0, 0));
        assertEquals(new Point(0, -1), renderer.topRight(0, 0));
        assertEquals(new Point(-1, 1), renderer.bottomLeft(0, 0));
        assertEquals(new Point(0, 1), renderer.bottomRight(0, 0));

        assertEquals(new Point(1, 0), renderer.topLeft(1, 1));
        assertEquals(new Point(2, 0), renderer.topRight(1, 1));
        assertEquals(new Point(1, 2), renderer.bottomLeft(1, 1));
        assertEquals(new Point(2, 2), renderer.bottomRight(1, 1));
    }
}
