package org.ngengine.world2d.tiled.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.enums.Orientation;

class CoordinateSystemAudioDistanceTest {

    @Test
    void convertsOrthogonalCellsToAveragePhysicsAxisLength() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ORTHOGONAL);
        map.setTileWidth(256);
        map.setTileHeight(128);

        CoordinateSystem coordinates = TiledCoordinateSystem.create(map, 32);

        assertEquals(6f, coordinates.tileDistanceToPhysics(1f), 0.0001f);
        assertEquals(12f, coordinates.tileDistanceToPhysics(2f), 0.0001f);
    }

    @Test
    void convertsIsometricCellsIndependentlyOfPpm() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ISOMETRIC);
        map.setTileWidth(256);
        map.setTileHeight(128);
        map.setWidth(10);
        map.setHeight(10);

        CoordinateSystem coordinates32 = TiledCoordinateSystem.create(map, 32);
        CoordinateSystem coordinates64 = TiledCoordinateSystem.create(map, 64);

        assertEquals(
            coordinates32.tileDistanceToPhysics(1f) * 0.5f,
            coordinates64.tileDistanceToPhysics(1f),
            0.0001f
        );
    }
}
