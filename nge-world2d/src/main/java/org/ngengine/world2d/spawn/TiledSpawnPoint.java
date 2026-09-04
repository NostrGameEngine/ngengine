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
package org.ngengine.world2d.spawn;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

/** A tagged point or area from which runtime objects can be spawned. */
public final class TiledSpawnPoint {
    private final TiledObjectEntity object;
    private final TiledObjectLayer layer;
    private final Set<String> tags;

    TiledSpawnPoint(TiledObjectEntity object, TiledObjectLayer layer, String rawTags) {
        this.object = object;
        this.layer = layer;
        this.tags = parseTags(rawTags);
    }

    public TiledObjectEntity getObject() {
        return object;
    }

    public TiledObjectLayer getLayer() {
        return layer;
    }

    public Set<String> getTags() {
        return tags;
    }

    public boolean hasTag(String tag) {
        return tag != null && tags.contains(normalizeTag(tag));
    }

    public Vector2f getPosition(CoordinateSystem coordinates) {
        return getPosition(coordinates, new Vector2f());
    }

    /** Writes the center of this spawn marker in grid space. */
    public Vector2f getPosition(CoordinateSystem coordinates, Vector2f out) {
        if (coordinates != null) {
            coordinates.getCenterInGridSpace(object, out);
        } else {
            out.set(
                (float) (object.getX() + object.getWidth() * 0.5),
                (float) (object.getY() + object.getHeight() * 0.5)
            );
        }
        return out;
    }

    /**
     * Samples this marker's rectangle, ellipse, or polygon in grid space.
     * Point, tile, and polyline markers fall back to their stable center.
     */
    public Vector2f samplePosition(Random random, CoordinateSystem coordinates, Vector2f out) {
        ObjectShape shape = object.getShape();
        if (random == null || shape == ObjectShape.POINT || shape == ObjectShape.TILE) {
            return getPosition(coordinates, out);
        }
        if ((shape == ObjectShape.RECTANGLE || shape == ObjectShape.ELLIPSE)
                && (object.getWidth() <= 0d || object.getHeight() <= 0d)) {
            return getPosition(coordinates, out);
        }
        float localX;
        float localY;
        if (shape == ObjectShape.ELLIPSE) {
            float radius = FastMath.sqrt(random.nextFloat());
            float angle = random.nextFloat() * FastMath.TWO_PI;
            localX = (0.5f + FastMath.cos(angle) * radius * 0.5f) * (float) object.getWidth();
            localY = (0.5f + FastMath.sin(angle) * radius * 0.5f) * (float) object.getHeight();
        } else if (shape == ObjectShape.RECTANGLE) {
            localX = random.nextFloat() * (float) object.getWidth();
            localY = random.nextFloat() * (float) object.getHeight();
        } else if (shape == ObjectShape.POLYGON) {
            return samplePolygon(random, coordinates, out);
        } else {
            return getPosition(coordinates, out);
        }
        rotateAndTranslate(localX, localY, out);
        return out;
    }

    private Vector2f samplePolygon(Random random, CoordinateSystem coordinates, Vector2f out) {
        List<Vector2f> points = object.getPoints();
        if (points == null || points.size() < 3) {
            return getPosition(coordinates, out);
        }
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Vector2f point : points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        for (int attempt = 0; attempt < 32; attempt++) {
            float x = minX + random.nextFloat() * (maxX - minX);
            float y = minY + random.nextFloat() * (maxY - minY);
            if (contains(points, x, y)) {
                rotateAndTranslate(x, y, out);
                return out;
            }
        }
        return getPosition(coordinates, out);
    }

    private static boolean contains(List<Vector2f> polygon, float x, float y) {
        boolean inside = false;
        for (int i = 0, previous = polygon.size() - 1; i < polygon.size(); previous = i++) {
            Vector2f a = polygon.get(i);
            Vector2f b = polygon.get(previous);
            boolean crosses = (a.y > y) != (b.y > y)
                && x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x;
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    private void rotateAndTranslate(float localX, float localY, Vector2f out) {
        float rotation = (float) object.getRotation();
        if (rotation == 0f) {
            out.set((float) object.getX() + localX, (float) object.getY() + localY);
            return;
        }
        float radians = rotation * FastMath.DEG_TO_RAD;
        float cosine = FastMath.cos(radians);
        float sine = FastMath.sin(radians);
        out.set(
            (float) object.getX() + localX * cosine - localY * sine,
            (float) object.getY() + localX * sine + localY * cosine
        );
    }

    private static Set<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        for (String rawTag : rawTags.split("[,\\s]+")) {
            String tag = normalizeTag(rawTag);
            if (!tag.isEmpty()) {
                parsed.add(tag);
            }
        }
        return Collections.unmodifiableSet(parsed);
    }

    private static String normalizeTag(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }
}
