package org.ngengine.world2d.tiled.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.Vector2f;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

class TestObjectDecalPlacement {
    @Test
    void decalUsesDefaultTileWhenParentIsNotFlipped() {
        TiledObjectEntity decal = new TiledObjectEntity(1, 0, 0, 1, 1);
        decal.putProperty(ObjectDecalPlacement.TILE_PROPERTY, 4);
        decal.putProperty(ObjectDecalPlacement.HORIZONTAL_FLIP_TILE_PROPERTY, 7);

        assertEquals(4, ObjectDecalPlacement.tileValueForFlip(decal, new Tile()));
    }

    @Test
    void decalUsesHorizontalVariantWhenParentIsFlipped() {
        TiledObjectEntity decal = new TiledObjectEntity(1, 0, 0, 1, 1);
        decal.putProperty(ObjectDecalPlacement.TILE_PROPERTY, 4);
        decal.putProperty(ObjectDecalPlacement.HORIZONTAL_FLIP_TILE_PROPERTY, 7);
        Tile tile = new Tile();
        tile.setFlippedHorizontally(true);

        assertEquals(7, ObjectDecalPlacement.tileValueForFlip(decal, tile));
    }

    @Test
    void decalUsesAuthoredHorizontalVariantWhenParentIsNotFlipped() {
        TiledObjectEntity decal = new TiledObjectEntity(1, 0, 0, 1, 1);
        decal.putProperty(ObjectDecalPlacement.TILE_PROPERTY, 4);
        decal.putProperty(ObjectDecalPlacement.HORIZONTAL_FLIP_TILE_PROPERTY, 7);
        decal.putProperty(ObjectDecalPlacement.HORIZONTAL_FLIP_PROPERTY, true);

        assertEquals(7, ObjectDecalPlacement.tileValueForFlip(decal, new Tile()));
    }

    @Test
    void parentHorizontalFlipInvertsAuthoredHorizontalVariant() {
        TiledObjectEntity decal = new TiledObjectEntity(1, 0, 0, 1, 1);
        decal.putProperty(ObjectDecalPlacement.TILE_PROPERTY, 4);
        decal.putProperty(ObjectDecalPlacement.HORIZONTAL_FLIP_TILE_PROPERTY, 7);
        decal.putProperty(ObjectDecalPlacement.HORIZONTAL_FLIP_PROPERTY, true);
        Tile tile = new Tile();
        tile.setFlippedHorizontally(true);

        assertEquals(4, ObjectDecalPlacement.tileValueForFlip(decal, tile));
    }

    @Test
    void decalWithoutHorizontalVariantKeepsDefaultTileWhenParentIsFlipped() {
        TiledObjectEntity decal = new TiledObjectEntity(1, 0, 0, 1, 1);
        decal.putProperty(ObjectDecalPlacement.TILE_PROPERTY, 4);
        Tile tile = new Tile();
        tile.setFlippedHorizontally(true);

        assertEquals(4, ObjectDecalPlacement.tileValueForFlip(decal, tile));
    }

    @Test
    void horizontalFlipMovesDecalPositionWithoutMirroringDecalImage() {
        Tile tile = new Tile();
        tile.setFlippedHorizontally(true);

        Vector2f center = ObjectDecalPlacement.transformCenterForTileFlip(0.25f, 0.75f, tile);

        assertEquals(0.75f, center.x, 0.0001f);
        assertEquals(0.75f, center.y, 0.0001f);
    }

    @Test
    void tiledFlipOrderMatchesDiagonalThenAxisFlips() {
        Tile tile = new Tile();
        tile.setFlippedAntiDiagonally(true);
        tile.setFlippedHorizontally(true);

        Vector2f center = ObjectDecalPlacement.transformCenterForTileFlip(0.2f, 0.7f, tile);

        assertEquals(0.7f, center.x, 0.0001f);
        assertEquals(0.8f, center.y, 0.0001f);
    }

    @Test
    void fallbackMeshUvKeepsOriginalDecalCenterBecauseMeshUvIsAlreadyFlipped() {
        Vector2f center = ObjectDecalPlacement.centerForFallbackTileUv(0.25f, 0.75f);

        assertEquals(0.25f, center.x, 0.0001f);
        assertEquals(0.75f, center.y, 0.0001f);
    }
}
