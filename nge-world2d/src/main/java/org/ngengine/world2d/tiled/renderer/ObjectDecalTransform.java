package org.ngengine.world2d.tiled.renderer;

import com.jme3.math.Vector2f;
import org.ngengine.world2d.tiled.core.tileset.Tile;

final class ObjectDecalTransform {

    private ObjectDecalTransform() {
    }

    static Vector2f toDisplayCenter(float x, float y, Tile tile) {
        Vector2f center = new Vector2f(x, y);
        if (tile == null) {
            return center;
        }

        // The tile texture flip is applied diagonal first, then horizontal/vertical.
        // Decal art is sampled in unflipped screen UVs, so decal centers need the inverse.
        if (tile.isFlippedVertically()) {
            center.y = 1f - center.y;
        }
        if (tile.isFlippedHorizontally()) {
            center.x = 1f - center.x;
        }
        if (tile.isFlippedAntiDiagonally()) {
            float oldX = center.x;
            center.x = 1f - center.y;
            center.y = 1f - oldX;
        }
        return center;
    }
}
