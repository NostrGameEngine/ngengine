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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

/** Finds spawn markers authored in Tiled with the {@value #TAGS_PROPERTY} property. */
public final class TiledSpawnPoints {
    public static final String TAGS_PROPERTY = "spawn.tags";

    private TiledSpawnPoints() {
    }

    public static List<TiledSpawnPoint> find(TiledMap map, String tag) {
        ArrayList<TiledSpawnPoint> result = new ArrayList<>();
        find(map, tag, result);
        return result;
    }

    /** Appends matching spawn markers to {@code out} without allocating a result list. */
    public static void find(TiledMap map, String tag, List<TiledSpawnPoint> out) {
        if (map == null || out == null) {
            return;
        }
        for (TiledLayer layer : map.getLayersFlat()) {
            if (!(layer instanceof TiledObjectLayer)) {
                continue;
            }
            TiledObjectLayer objectLayer = (TiledObjectLayer) layer;
            for (TiledObjectEntity object : objectLayer.getObjects()) {
                Object rawTags = object.getProperty(TAGS_PROPERTY);
                if (rawTags == null || String.valueOf(rawTags).isBlank()) {
                    continue;
                }
                TiledSpawnPoint point = new TiledSpawnPoint(object, objectLayer, String.valueOf(rawTags));
                if (tag == null || tag.isBlank() || point.hasTag(tag)) {
                    out.add(point);
                }
            }
        }
    }

    public static TiledSpawnPoint first(TiledMap map, String tag) {
        List<TiledSpawnPoint> points = find(map, tag);
        return points.isEmpty() ? null : points.get(0);
    }

    public static TiledSpawnPoint pickDeterministic(TiledMap map, String tag, BigInteger key) {
        List<TiledSpawnPoint> points = find(map, tag);
        if (points.isEmpty()) {
            return null;
        }
        BigInteger safeKey = key != null ? key.abs() : BigInteger.ZERO;
        int index = safeKey.mod(BigInteger.valueOf(points.size())).intValue();
        return points.get(index);
    }

    /** Returns whether an object is a generic spawn marker of any tag. */
    public static boolean isSpawnPoint(TiledObjectEntity object) {
        if (object == null) {
            return false;
        }
        Object tags = object.getProperty(TAGS_PROPERTY);
        return tags != null && !String.valueOf(tags).isBlank();
    }
}
