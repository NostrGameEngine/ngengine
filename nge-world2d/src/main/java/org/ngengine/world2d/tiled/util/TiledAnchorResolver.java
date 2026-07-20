/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */
package org.ngengine.world2d.tiled.util;

import java.util.List;

import org.ngengine.world2d.box2d.Box2dHelper;
import org.ngengine.world2d.tiled.animation.Animation;
import org.ngengine.world2d.tiled.animation.Frame;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

/** Resolves a stable anchor inside a placed Tiled object. */
public final class TiledAnchorResolver {
    private static final float MIN_SHAPE_SIZE = 1e-5f;

    private TiledAnchorResolver() {
    }

    /**
     * Resolves marker, physical-bounds center, then geometric object center.
     * The preferred tile is useful for animated frames. Physical bounds come
     * from the source tile, the preferred frame, or an animation frame, in that
     * order, so changing the displayed tile does not move the anchor.
     */
    public static boolean resolve(
            TiledObjectEntity source,
            Tile preferredMarkerTile,
            String markerProperty,
            Object markerValue,
            CoordinateSystem coordinates,
            Vector2f out) {
        if (source == null || coordinates == null || out == null) {
            return false;
        }
        Tile sourceTile = source.getTile();
        Tile markerTile = preferredMarkerTile != null ? preferredMarkerTile : sourceTile;
        TiledObjectEntity marker = findMarker(markerTile, markerProperty, markerValue);
        if (marker == null && markerTile != sourceTile) {
            marker = findMarker(sourceTile, markerProperty, markerValue);
            markerTile = sourceTile;
        }
        if (marker != null && markerTile != null) {
            coordinates.getTileObjectCenterInGridSpace(source, markerTile, marker, out);
            return true;
        }
        if (physicalCenter(source, preferredMarkerTile, coordinates, out)) {
            return true;
        }
        coordinates.getCenterInGridSpace(source, out);
        return true;
    }

    public static TiledObjectEntity findMarker(Tile tile, String markerProperty, Object markerValue) {
        TiledObjectLayer objects = tile != null ? tile.getCollisions() : null;
        if (objects == null || markerProperty == null || markerProperty.isBlank()) {
            return null;
        }
        for (TiledObjectEntity object : objects.getObjects()) {
            Object value = object.getProperty(markerProperty);
            if (matches(value, markerValue)) {
                return object;
            }
        }
        return null;
    }

    public static boolean resolveWorld(
            TiledObjectEntity source,
            Tile preferredMarkerTile,
            String markerProperty,
            Object markerValue,
            CoordinateSystem coordinates,
            Vector2f out) {
        if (!resolve(source, preferredMarkerTile, markerProperty, markerValue, coordinates, out)) {
            return false;
        }
        coordinates.gridToWorldSpace(out.x, out.y, out);
        return true;
    }

    public static boolean resolvePhysics(
            TiledObjectEntity source,
            Tile preferredMarkerTile,
            String markerProperty,
            Object markerValue,
            CoordinateSystem coordinates,
            Vector2f out) {
        if (!resolveWorld(source, preferredMarkerTile, markerProperty, markerValue, coordinates, out)) {
            return false;
        }
        coordinates.worldToPhysicsSpace(out.x, out.y, out);
        return true;
    }

    public static boolean physicalCenter(
            TiledObjectEntity source,
            CoordinateSystem coordinates,
            Vector2f out) {
        return physicalCenter(source, null, coordinates, out);
    }

    private static boolean physicalCenter(
            TiledObjectEntity source,
            Tile preferredTile,
            CoordinateSystem coordinates,
            Vector2f out) {
        Tile tile = findPhysicalTile(source != null ? source.getTile() : null, preferredTile);
        TiledObjectLayer collisions = tile != null ? tile.getCollisions() : null;
        if (source == null || coordinates == null || out == null || collisions == null) {
            return false;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean found = false;
        Vector2f rotated = new Vector2f();
        for (TiledObjectEntity collision : collisions.getObjects()) {
            if (!Box2dHelper.isPhysicsEnabled(collision)) {
                continue;
            }
            ObjectShape shape = collision.getShape();
            if (shape == ObjectShape.POLYGON) {
                List<Vector2f> points = collision.getPoints();
                if (points == null || points.size() < 3) {
                    continue;
                }
                int count = Math.min(points.size(), 8);
                for (int i = 0; i < count; i++) {
                    Vector2f point = points.get(i);
                    rotate(point.x, point.y, (float) collision.getRotation(), rotated);
                    float x = (float) collision.getX() + rotated.x;
                    float y = (float) collision.getY() + rotated.y;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    found = true;
                }
                continue;
            }
            if (shape != ObjectShape.RECTANGLE && shape != ObjectShape.ELLIPSE) {
                continue;
            }
            float width = (float) collision.getWidth();
            float height = (float) collision.getHeight();
            if (Math.abs(width) < MIN_SHAPE_SIZE || Math.abs(height) < MIN_SHAPE_SIZE) {
                continue;
            }
            float[] xs = { 0f, width, width, 0f };
            float[] ys = { 0f, 0f, height, height };
            for (int i = 0; i < xs.length; i++) {
                rotate(xs[i], ys[i], (float) collision.getRotation(), rotated);
                float x = (float) collision.getX() + rotated.x;
                float y = (float) collision.getY() + rotated.y;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                found = true;
            }
        }
        if (!found) {
            return false;
        }

        TiledObjectEntity bounds = new TiledObjectEntity(
            -1,
            minX,
            minY,
            Math.max(0f, maxX - minX),
            Math.max(0f, maxY - minY)
        );
        coordinates.getTileObjectCenterInGridSpace(source, tile, bounds, out);
        return true;
    }

    private static Tile findPhysicalTile(Tile sourceTile, Tile preferredTile) {
        if (Box2dHelper.hasPhysicalCollisions(sourceTile)) {
            return sourceTile;
        }
        if (preferredTile != sourceTile && Box2dHelper.hasPhysicalCollisions(preferredTile)) {
            return preferredTile;
        }
        Tile animationFrame = findPhysicalAnimationFrame(sourceTile);
        if (animationFrame != null) {
            return animationFrame;
        }
        return preferredTile != sourceTile ? findPhysicalAnimationFrame(preferredTile) : null;
    }

    private static Tile findPhysicalAnimationFrame(Tile tile) {
        if (tile == null || tile.getTileset() == null) {
            return null;
        }
        for (Animation animation : tile.getAnimations()) {
            for (int i = 0; i < animation.getTotalFrames(); i++) {
                Frame frame = animation.getFrame(i);
                Tile frameTile = frame != null ? tile.getTileset().getTile(frame.getTileId()) : null;
                if (Box2dHelper.hasPhysicalCollisions(frameTile)) {
                    return frameTile;
                }
            }
        }
        return null;
    }

    private static boolean matches(Object actual, Object expected) {
        if (actual == null) {
            return false;
        }
        if (expected == null) {
            if (actual instanceof Boolean) {
                return (Boolean) actual;
            }
            return !String.valueOf(actual).isBlank();
        }
        if (expected instanceof Boolean) {
            return ((Boolean) expected).booleanValue() == (actual instanceof Boolean
                ? ((Boolean) actual).booleanValue()
                : Boolean.parseBoolean(String.valueOf(actual)));
        }
        return String.valueOf(expected).trim().equals(String.valueOf(actual).trim());
    }

    private static void rotate(float x, float y, float degrees, Vector2f out) {
        if (Math.abs(degrees) < MIN_SHAPE_SIZE) {
            out.set(x, y);
            return;
        }
        float angle = degrees * FastMath.DEG_TO_RAD;
        float cos = FastMath.cos(angle);
        float sin = FastMath.sin(angle);
        out.set(x * cos - y * sin, x * sin + y * cos);
    }
}
