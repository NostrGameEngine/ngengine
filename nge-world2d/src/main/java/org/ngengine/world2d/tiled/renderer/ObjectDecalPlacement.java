package org.ngengine.world2d.tiled.renderer;

import com.jme3.math.Vector2f;
import org.ngengine.world2d.tiled.core.tileset.Tile;

final class ObjectDecalPlacement {
    private ObjectDecalPlacement() {
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
