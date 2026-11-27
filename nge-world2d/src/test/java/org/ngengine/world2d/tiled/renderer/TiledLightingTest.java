package org.ngengine.world2d.tiled.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

class TiledLightingTest {
    @Test
    void inheritancePreservesExplicitUnshadedTiles() {
        TiledMap map = createMap(Orientation.ISOMETRIC);
        TiledObjectLayer layer = new TiledObjectLayer();
        Tile tile = new Tile();
        TiledObjectEntity object = new TiledObjectEntity(1, 0, 0, 0, 0);

        assertEquals(TiledLighting.UNSHADED, TiledLighting.layerMode(layer, map));
        map.putProperty("lit", "dynamic");
        assertEquals(TiledLighting.DYNAMIC, TiledLighting.layerMode(layer, map));
        layer.putProperty("lit", false);
        assertEquals(TiledLighting.UNSHADED, TiledLighting.layerMode(layer, map));
        tile.putProperty("lit", "dynamic");
        assertEquals(TiledLighting.DYNAMIC, TiledLighting.tileMode(object, tile));
        object.putProperty("lit", "unshaded");
        assertEquals(TiledLighting.UNSHADED, TiledLighting.tileMode(object, tile));
    }

    @Test
    void movesAColoredSpotAndBlendsASoftBlobInEitherOrientation() {
        checkSpotAndShadow(Orientation.ISOMETRIC);
        checkSpotAndShadow(Orientation.ORTHOGONAL);
    }

    private void checkSpotAndShadow(Orientation orientation) {
        TiledMap map = createMap(orientation);
        map.putProperty("light.ambient", .4f);
        TiledObjectLayer layer = new TiledObjectLayer();
        map.addLayer(layer);
        TiledObjectEntity object = new TiledObjectEntity(1, 0, 0, 0, 0);
        object.putProperty("light.type", "spot");
        object.putProperty("light.color", ColorRGBA.Red);
        object.putProperty("light.range", 64f);
        object.putProperty("light.direction", 0f);
        object.putProperty("light.strength", .5f);
        object.putProperty("shadow.radius", 16f);
        object.putProperty("shadow.strength", .8f);
        layer.add(object);

        TiledCoordinateSystem coords = TiledCoordinateSystem.create(map, 32);
        Vector2f center = coords.gridToWorldSpace(0f, 0f);
        TiledLighting lighting = new TiledLighting(center.x - 80f, center.y - 80f, 160f, 160f);
        lighting.update(map, coords, ignored -> Vector2f.ZERO);
        ByteBuffer texels = lighting.texture().getImage().getData(0);
        int spot = (80 * 160 + 105) * 4;
        int back = (80 * 160 + 55) * 4;
        int shadow = (80 * 160 + 80) * 4;
        assertTrue(unsigned(texels, spot) > unsigned(texels, spot + 1));
        assertEquals(102, unsigned(texels, back));
        assertTrue(unsigned(texels, shadow + 1) < unsigned(texels, back));

        object.setX(70f);
        lighting.update(map, coords, ignored -> Vector2f.ZERO);
        assertEquals(102, unsigned(texels, shadow + 1));
    }

    private static int unsigned(ByteBuffer data, int position) {
        return Byte.toUnsignedInt(data.get(position));
    }

    private static TiledMap createMap(Orientation orientation) {
        TiledMap map = new TiledMap(10, 10);
        map.setTileWidth(64);
        map.setTileHeight(32);
        map.setOrientation(orientation);
        return map;
    }
}
