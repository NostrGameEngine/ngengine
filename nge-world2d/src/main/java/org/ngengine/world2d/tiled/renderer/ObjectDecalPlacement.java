package org.ngengine.world2d.tiled.renderer;

import com.jme3.math.Vector2f;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

final class ObjectDecalPlacement {
    static final String TILE_PROPERTY = "decal.tile";
    static final String HORIZONTAL_FLIP_TILE_PROPERTY = "decal.tile.flipH";
    static final String HORIZONTAL_FLIP_PROPERTY = "decal.flipH";

    private ObjectDecalPlacement() {
    }

    static Object tileValueForFlip(TiledObjectEntity decalObject, Tile tile) {
        Object tileValue = decalObject.getProperty(TILE_PROPERTY);
        Object authoredFlip = decalObject.getProperty(HORIZONTAL_FLIP_PROPERTY);
        boolean useHorizontalVariant = Boolean.TRUE.equals(authoredFlip)
                || authoredFlip instanceof String && Boolean.parseBoolean((String) authoredFlip);
        if (tile != null && tile.isFlippedHorizontally()) {
            useHorizontalVariant = !useHorizontalVariant;
        }
        if (useHorizontalVariant) {
            Object flippedTileValue = decalObject.getProperty(HORIZONTAL_FLIP_TILE_PROPERTY);
            if (flippedTileValue != null) {
                return flippedTileValue;
            }
        }
        return tileValue;
    }

    static Vector2f transformCenterForTileFlip(float x, float y, Tile tile) {
        float tx = x;
        float ty = y;
        if (tile != null && tile.isFlippedAntiDiagonally()) {
            float oldX = tx;
            tx = 1f - ty;
            ty = 1f - oldX;
        }
        if (tile != null && tile.isFlippedHorizontally()) {
            tx = 1f - tx;
        }
        if (tile != null && tile.isFlippedVertically()) {
            ty = 1f - ty;
        }
        return new Vector2f(tx, ty);
    }

    static Vector2f centerForFallbackTileUv(float x, float y) {
        return new Vector2f(x, y);
    }
}
