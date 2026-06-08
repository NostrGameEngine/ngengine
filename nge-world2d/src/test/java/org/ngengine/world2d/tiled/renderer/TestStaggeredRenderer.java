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
import com.jme3.scene.Node;

import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.math2d.Point;
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
    StaggeredRenderer renderer;

    @BeforeEach
    public void initMap() {
        map = new TiledMap(10, 10);
        map.setOrientation(Orientation.STAGGERED);
        map.setTileWidth(64);
        map.setTileHeight(32);

        TiledTileLayer layer = new TiledTileLayer(10, 10);
        map.addLayer(layer);

        renderer = new StaggeredRenderer(map,32,new Node());
    }

    @Test void mapSize() {
        Point expect = new Point(10 * 64 + 32, 10 * 16 + 16);
        Point actual = renderer.getMapDimension();
        assertEquals(expect, actual, "map size");
    }

    @Test void screenToTileCoords() {
        assertEquals(new Point(0, 0), renderer.worldToTile(10, 16), "10,16");
        assertEquals(new Point(-1, -1), renderer.worldToTile(5, 5), "5,5");
        assertEquals(new Point(-1, 1), renderer.worldToTile(1, 20), "1,20");
        assertEquals(new Point(0, 1), renderer.worldToTile(64, 32), "64,32");
        assertEquals(new Point(0, -2), renderer.worldToTile(32, -16), "32,-16");
    }

    @Test void tileToScreenCoords() {
        assertEquals(new Vector2f(0f, 0f), renderer.tileToWorldSpace(0, 0), "0,0");
        assertEquals(new Vector2f(64f, 0f), renderer.tileToWorldSpace(1, 0), "1,0");
        assertEquals(new Vector2f(32f, 16f), renderer.tileToWorldSpace(0, 1), "0,1");
        assertEquals(new Vector2f(-32f, -16f), renderer.tileToWorldSpace(-1, -1), "-1,-1");
    }

    @Test void relativeCoordinates() {
        assertEquals(new Point(-1, -1), renderer.topLeft(0, 0));
        assertEquals(new Point(0, -1), renderer.topRight(0, 0));
        assertEquals(new Point(-1, 1), renderer.bottomLeft(0, 0));
        assertEquals(new Point(0, 1), renderer.bottomRight(0, 0));

        assertEquals(new Point(1, 0), renderer.topLeft(1, 1));
        assertEquals(new Point(2, 0), renderer.topRight(1, 1));
        assertEquals(new Point(1, 2), renderer.bottomLeft(1, 1));
        assertEquals(new Point(2, 2), renderer.bottomRight(1, 1));
    }
}
