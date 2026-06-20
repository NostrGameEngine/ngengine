package org.ngengine.world2d.tiled.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.Vector2f;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.tileset.Tile;

class TestObjectDecalTransform {

    @Test void horizontalFlipMovesDecalCenterWithoutMirroringArt() {
        Tile tile = new Tile(1, 0, 64, 64);
        tile.setFlippedHorizontally(true);

        Vector2f center = ObjectDecalTransform.toDisplayCenter(0.25f, 0.40f, tile);

        assertEquals(0.75f, center.x, 0.0001f);
        assertEquals(0.40f, center.y, 0.0001f);
    }

    @Test void diagonalAndAxisFlipsUseInverseTiledFlipOrder() {
        Tile tile = new Tile(1, 0, 64, 64);
        tile.setFlippedAntiDiagonally(true);
        tile.setFlippedHorizontally(true);

        Vector2f center = ObjectDecalTransform.toDisplayCenter(0.25f, 0.40f, tile);

        assertEquals(0.60f, center.x, 0.0001f);
        assertEquals(0.25f, center.y, 0.0001f);
    }
}
