package org.ngengine.world2d.tiled.core.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.tileset.Tile;

public class TiledObjectEntityTest {

    @Test
    public void tileConstructorPreservesGlobalTileIdForNetworkSnapshots() {
        Tile tile = new Tile();
        tile.setGid(321);
        tile.setWidth(64);
        tile.setHeight(96);

        TiledObjectEntity entity = new TiledObjectEntity(
            BigInteger.valueOf(77),
            12,
            34,
            tile
        );

        assertSame(tile, entity.getTile());
        assertEquals(321, entity.getGid());
        assertEquals(64d, entity.getWidth(), 0d);
        assertEquals(96d, entity.getHeight(), 0d);
    }
}
