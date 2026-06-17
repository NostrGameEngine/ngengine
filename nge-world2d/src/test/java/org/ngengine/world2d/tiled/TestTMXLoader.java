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

package org.ngengine.world2d.tiled;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import org.ngengine.world2d.tiled.core.*;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.enums.RenderingMode;
import org.ngengine.world2d.tiled.enums.StaggerAxis;
import org.ngengine.world2d.tiled.enums.StaggerIndex;
import org.ngengine.world2d.tiled.renderer.MapRenderer;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMaterialFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMeshFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultSpriteFactory;
import org.ngengine.world2d.PovRenderer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
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

    @Test void testDesertInstancedMapIsNotCulledWhenFirstTileLeavesView() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Desert/desert.tmx");
        Node sceneRoot = new Node("scene");
        Node mapRoot = new Node("map");
        MapRenderer renderer = MapRenderer.create(map, 32, mapRoot);
        DefaultSpriteFactory spriteFactory = new DefaultSpriteFactory();
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));
        spriteFactory.setMaterialFactory(new DefaultMaterialFactory(assetManager));
        renderer.setSpriteFactory(spriteFactory);
        sceneRoot.attachChild(mapRoot);

        Camera cam = new Camera(1280, 720);
        cam.setFrustum(-1000f, 1000f, -640f, 640f, 360f, -360f);
        cam.setParallelProjection(true);
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        cam.setLocation(new Vector3f(0f, 0f, 450f));
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(cam));
        sceneRoot.updateGeometricState();

        assertTrue(sceneRoot.checkCulling(cam), "scene root should still intersect the camera");
        assertTrue(mapRoot.checkCulling(cam), "map root should still intersect the camera");
        assertMapChildrenNotCulled(mapRoot, cam);
        assertTrue(countInstancedTiles(mapRoot) > 1, "instanced map should draw more than the first tile");
    }

    @Test void testInstancedTilesCullToCameraAndRestoreWithoutCamera() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Desert/desert.tmx");
        Node mapRoot = new Node("map");
        MapRenderer renderer = MapRenderer.create(map, 32, mapRoot);
        DefaultSpriteFactory spriteFactory = new DefaultSpriteFactory();
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));
        spriteFactory.setMaterialFactory(new DefaultMaterialFactory(assetManager));
        renderer.setSpriteFactory(spriteFactory);

        assertThrows(IllegalArgumentException.class, () -> renderer.render(new EmptyMapRenderListener(), 0f));

        Camera fullCam = new Camera(1280, 720);
        fullCam.setFrustum(-1000f, 1000f, -800f, 800f, 800f, -800f);
        fullCam.setParallelProjection(true);
        fullCam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        fullCam.setLocation(new Vector3f(640f, 0f, 640f));
        TestPovRenderer fullPov = new TestPovRenderer(fullCam);
        renderer.render(new EmptyMapRenderListener(), 0f, fullPov);
        int fullCount = countInstancedTiles(mapRoot);
        assertTrue(fullCount > 0, "unculled render should create instanced tiles");

        Camera cam = new Camera(320, 240);
        cam.setFrustum(-1000f, 1000f, -64f, 64f, 48f, -48f);
        cam.setParallelProjection(true);
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        cam.setLocation(new Vector3f(120f, 0f, 120f));
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(cam));
        int culledCount = countInstancedTiles(mapRoot);
        assertTrue(culledCount > 0, "camera over the map should keep visible instanced tiles");
        assertTrue(culledCount < fullCount,
                "camera culling should draw fewer instances than the full map: " + culledCount + " / " + fullCount);

        cam.setLocation(new Vector3f(-10000f, 0f, -10000f));
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(cam));
        assertEquals(0, countInstancedTiles(mapRoot), "camera outside the map should cull all instanced tiles");

        renderer.render(new EmptyMapRenderListener(), 0f, fullPov);
        assertEquals(fullCount, countInstancedTiles(mapRoot), "rendering with a wide POV should restore the full batch");
    }

    @Test void testBatchDebugShowsMovingObjectTransientAndCleansUpWhenDisabled() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Orthogonal/01.tmx");
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer movingLayer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        movingLayer.setName("Moving debug object");
        movingLayer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        TiledObjectEntity movingObject = new TiledObjectEntity(100000, 96, 96, tile);
        movingObject.setName("moving transient debug tile");
        movingLayer.add(movingObject);
        map.addLayer(movingLayer);

        Node mapRoot = new Node("map");
        MapRenderer renderer = MapRenderer.create(map, 32, mapRoot);
        DefaultSpriteFactory spriteFactory = new DefaultSpriteFactory();
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));
        spriteFactory.setMaterialFactory(new DefaultMaterialFactory(assetManager));
        renderer.setSpriteFactory(spriteFactory);
        renderer.setBatchDebugEnabled(true);

        Camera cam = new Camera(1280, 720);
        cam.setFrustum(-1000f, 1000f, -640f, 640f, 360f, -360f);
        cam.setParallelProjection(true);
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        cam.setLocation(new Vector3f(320f, 0f, 320f));
        TestPovRenderer pov = new TestPovRenderer(cam);

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        movingObject.setX(160);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertTrue(hasSpatialNamePrefix(mapRoot, "transient#position"),
                "moving tile object should show a position transient debug overlay");

        renderer.setBatchDebugEnabled(false);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertFalse(hasSpatialNamePrefix(mapRoot, "TiledWorld2D-BatchDebug"),
                "disabling batch debug should remove the overlay node from the map root");
    }

    @Test void testGetSpatialFindsMultidrawTileEntity() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Orthogonal/01.tmx");
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        tileLayer.setRenderingMode(RenderingMode.MULTI_DRAW);
        TiledTileEntity tileEntry = tileLayer.getTileAt(0, 0);
        Node mapRoot = new Node("map");
        MapRenderer renderer = MapRenderer.create(map, 32, mapRoot);
        DefaultSpriteFactory spriteFactory = new DefaultSpriteFactory();
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));
        spriteFactory.setMaterialFactory(new DefaultMaterialFactory(assetManager));
        renderer.setSpriteFactory(spriteFactory);

        Camera cam = new Camera(1280, 720);
        cam.setFrustum(-1000f, 1000f, -640f, 640f, 360f, -360f);
        cam.setParallelProjection(true);
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        cam.setLocation(new Vector3f(320f, 0f, 320f));
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(cam));

        Spatial spatial = renderer.getSpatial(tileLayer, tileEntry);
        assertNotNull(spatial);
        assertTrue(spatial.getName().startsWith("tile#0#0#"));
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

    private static class EmptyMapRenderListener implements MapRenderer.Listener {

        @Override
        public void beforeMapRender(float tpf, TiledMap map) {
        }

        @Override
        public void afterMapRender(float tpf, TiledMap map, Spatial visual) {
        }

        @Override
        public void beforeEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry) {
        }

        @Override
        public void afterEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry, Spatial visual) {
        }

        @Override
        public void beforeLayerRender(float tpf, TiledMap map, TiledLayer layer) {
        }

        @Override
        public void afterLayerRender(float tpf, TiledMap map, TiledLayer layer, Spatial visual) {
        }
    }

    private static void assertMapChildrenNotCulled(Node node, Camera cam) {
        for (Spatial child : node.getChildren()) {
            if ("tiled-map-bounds".equals(child.getName())) {
                continue;
            }
            assertTrue(child.checkCulling(cam), child.getName() + " should still intersect the camera");
            if (child instanceof Node) {
                assertMapChildrenNotCulled((Node) child, cam);
            }
        }
    }

    private static int countInstancedTiles(Node node) {
        int count = 0;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry && child.getName().startsWith("tiles#")) {
                count += ((Geometry) child).getNumInstances();
            } else if (child instanceof Node) {
                count += countInstancedTiles((Node) child);
            }
        }
        return count;
    }

    private static boolean hasSpatialNamePrefix(Node node, String prefix) {
        for (Spatial child : node.getChildren()) {
            if (child.getName() != null && child.getName().startsWith(prefix)) {
                return true;
            }
            if (child instanceof Node && hasSpatialNamePrefix((Node) child, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static class TestPovRenderer implements PovRenderer {
        private final ViewPort sceneViewPort;

        TestPovRenderer(Camera camera) {
            sceneViewPort = new ViewPort("test", camera);
        }

        @Override
        public ViewPort getSceneViewPort() {
            return sceneViewPort;
        }

        @Override
        public ViewPort getGuiViewPort() {
            return null;
        }

        @Override
        public Node getGuiNode(int i) {
            return null;
        }

        @Override
        public Node getSceneNode(int i) {
            return null;
        }
    }
}
