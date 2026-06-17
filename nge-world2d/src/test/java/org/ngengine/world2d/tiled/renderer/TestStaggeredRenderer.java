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

package org.ngengine.world2d.tiled.renderer;

import com.jme3.math.Vector2f;

import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.math2d.Point;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
class TestStaggeredRenderer {
    TiledMap map;
    TiledCoordinateSystem coordinateSystem;

    @BeforeEach
    public void initMap() {
        map = new TiledMap(10, 10);
        map.setOrientation(Orientation.STAGGERED);
        map.setTileWidth(64);
        map.setTileHeight(32);

        TiledTileLayer layer = new TiledTileLayer(10, 10);
        map.addLayer(layer);

        coordinateSystem = TiledCoordinateSystem.create(map, 32);
    }

    @Test void mapSize() {
        Point expect = new Point(10 * 64 + 32, 10 * 16 + 16);
        Point actual = coordinateSystem.getMapDimension();
        assertEquals(expect, actual, "map size");
    }

    @Test void screenToTileCoords() {
        assertEquals(new Point(0, 0), coordinateSystem.worldToTile(10, 16), "10,16");
        assertEquals(new Point(-1, -1), coordinateSystem.worldToTile(5, 5), "5,5");
        assertEquals(new Point(-1, 1), coordinateSystem.worldToTile(1, 20), "1,20");
        assertEquals(new Point(0, 1), coordinateSystem.worldToTile(64, 32), "64,32");
        assertEquals(new Point(0, -2), coordinateSystem.worldToTile(32, -16), "32,-16");
    }

    @Test void tileToScreenCoords() {
        assertEquals(new Vector2f(0f, 0f), coordinateSystem.tileToWorldSpace(0, 0), "0,0");
        assertEquals(new Vector2f(64f, 0f), coordinateSystem.tileToWorldSpace(1, 0), "1,0");
        assertEquals(new Vector2f(32f, 16f), coordinateSystem.tileToWorldSpace(0, 1), "0,1");
        assertEquals(new Vector2f(-32f, -16f), coordinateSystem.tileToWorldSpace(-1, -1), "-1,-1");
    }
}
