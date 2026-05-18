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

package io.github.jmecn.tiled;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.core.tileset.Tileset;
import io.github.jmecn.tiled.enums.Orientation;
import io.github.jmecn.tiled.enums.StaggerAxis;
import io.github.jmecn.tiled.enums.StaggerIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.texture.plugins.AWTLoader;

/**
 * JUnit test case
 * 
 * @author yanmaoyuan
 *
 */
class TestTMXLoader {

    AssetManager assetManager;

    @BeforeEach void initAssetManager() {
        assetManager = new DesktopAssetManager();
        assetManager.registerLocator("/", ClasspathLocator.class);
        assetManager.registerLoader(J3MLoader.class, "j3md");
        assetManager.registerLoader(GLSLLoader.class, "vert", "frag", "geom", "tsctrl", "tseval", "glsl", "glsllib");
        assetManager.registerLoader(AWTLoader.class, "jpg", "bmp", "gif", "png", "jpeg");
        assetManager.registerLoader(TmxLoader.class, "tmx", "tsx", "tx");
    }

    @Test void testReadingExampleMap() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/sewers.tmx");

        // Assert
        assertEquals(Orientation.ORTHOGONAL, map.getOrientation());
        assertEquals(50, map.getHeight());
        assertEquals(50, map.getHeight());
        assertEquals(24, map.getTileWidth());
        assertEquals(24, map.getTileHeight());
        assertEquals(3, map.getLayerCount());
        assertNotNull(((TiledTileLayer)map.getLayer(0)).getTileAt(0, 0).getTile());
    }

    @Test void testReadingExampleCsvMap() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/csvmap.tmx");

        // Assert
        assertEquals(Orientation.ORTHOGONAL, map.getOrientation());
        assertEquals(100, map.getHeight());
        assertEquals(100, map.getHeight());
        assertEquals(32, map.getTileWidth());
        assertEquals(32, map.getTileHeight());
        assertEquals(1, map.getLayerCount());
        assertNotNull(((TiledTileLayer)map.getLayer(0)).getTileAt(0, 0).getTile());
    }

    @Test void testReadingExampleHexagonalMap() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/hexagonal.tmx");

        // Assert
        assertEquals(Orientation.HEXAGONAL, map.getOrientation());
        assertEquals(9, map.getHeight());
        assertEquals(9, map.getHeight());
        assertEquals(32, map.getTileWidth());
        assertEquals(32, map.getTileHeight());
        assertEquals(16, map.getHexSideLength());
        assertEquals(StaggerAxis.Y, map.getStaggerAxis());
        assertEquals(StaggerIndex.ODD, map.getStaggerIndex());
        assertEquals(1, map.getLayerCount());
    }

    @Test void testReadingExampleStaggeredMap() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/staggered.tmx");

        // Assert
        assertEquals(Orientation.STAGGERED, map.getOrientation());
        assertEquals(9, map.getHeight());
        assertEquals(9, map.getHeight());
        assertEquals(32, map.getTileWidth());
        assertEquals(32, map.getTileHeight());
        assertEquals(StaggerAxis.Y, map.getStaggerAxis());
        assertEquals(StaggerIndex.ODD, map.getStaggerIndex());
        assertEquals(1, map.getLayerCount());
    }

    @Test void testReadingExampleIsometricMap() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Isometric/isometric_grass_and_water.tmx");

        // Assert
        assertEquals(Orientation.ISOMETRIC, map.getOrientation());
        assertEquals(25, map.getHeight());
        assertEquals(25, map.getHeight());
        assertEquals(64, map.getTileWidth());
        assertEquals(32, map.getTileHeight());
        assertEquals(1, map.getLayerCount());
    }

    @Test void testTemplateLoader() {
        TiledObjectTemplate block = (TiledObjectTemplate) assetManager.loadAsset("tmx/sticker-knight/map/templates/block.tx");
        assertNotNull(block);
        assertEquals("tmx/sticker-knight/map/templates/block.tx", block.getSource());

        TiledObjectEntity object = block.getObject();
        assertNotNull(object);
        assertEquals("block", object.getName());
        assertEquals(44, object.getGid());
        assertEquals(96, object.getWidth());
        assertEquals(96, object.getHeight());

        Tileset tileset = block.getTileset();
        assertNotNull(tileset);
        assertFalse(tileset.isImageBased());
        assertEquals("../objs.tsx", tileset.getSource());

        Tile tile = object.getTile();
        assertNotNull(tile);
        assertEquals(44, tile.getGid());
        assertEquals(96, tile.getWidth());
        assertEquals(96, tile.getHeight());
        assertNotNull(tile.getImage());
    }

    @Test void testTiledMapWithTemplate() {
        TiledMap tiledMap = (TiledMap) assetManager.loadAsset("tmx/sticker-knight/map/sandbox.tmx");
        TiledObjectTemplate block = tiledMap.getObjectTemplate("tmx/templates/block.tx");
        assertNotNull(block);

        TiledObjectEntity object = block.getObject();
        assertNotNull(object);
        assertEquals("block", object.getName());
        assertEquals(44, object.getGid());
        assertEquals(96, object.getWidth());
        assertEquals(96, object.getHeight());

        Tileset tileset = block.getTileset();
        assertNotNull(tileset);
        assertFalse(tileset.isImageBased());
        assertEquals("objs.tsx", tileset.getSource());

        Tile tile = object.getTile();
        assertNotNull(tile);
        assertEquals(44, tile.getGid());
        assertEquals(96, tile.getWidth());
        assertEquals(96, tile.getHeight());
        assertNotNull(tile.getImage());
    }
}