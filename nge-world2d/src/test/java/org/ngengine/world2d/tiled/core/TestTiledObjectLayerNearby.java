package org.ngengine.world2d.tiled.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.util.CoordinateSystem;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

import com.jme3.math.Vector2f;

class TestTiledObjectLayerNearby {
    @Test
    void nearbyObjectQueriesUseObjectCenter() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ORTHOGONAL);
        map.setTileWidth(32);
        map.setTileHeight(32);
        CoordinateSystem coords = TiledCoordinateSystem.create(map, 32);

        TiledObjectLayer layer = new TiledObjectLayer();
        layer.setName("objects");
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 100, 100, 40, 40);
        layer.add(object);

        ArrayList<TiledBase> out = new ArrayList<>();
        layer.getNearby(coords, new Vector2f(120, 120), 1f, out);

        assertEquals(1, out.size());
        assertEquals(object, out.get(0));
    }
}
