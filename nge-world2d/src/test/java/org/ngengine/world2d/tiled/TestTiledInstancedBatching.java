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

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.bounding.BoundingBox;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.material.MatParamOverride;
import com.jme3.material.RenderState;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.PovRenderer;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledImageLayer;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.entity.TiledImageEntity;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.DrawOrder;
import org.ngengine.world2d.tiled.enums.FillMode;
import org.ngengine.world2d.tiled.enums.LayerBlendMode;
import org.ngengine.world2d.tiled.enums.ObjectAlignment;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.enums.RenderingMode;
import org.ngengine.world2d.tiled.renderer.MapRenderer;
import org.ngengine.world2d.tiled.renderer.MaterialConst;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMaterialFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMeshFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultSpriteFactory;
import org.ngengine.world2d.tiled.renderer.queue.YAxisComparator;
import org.ngengine.world2d.tiled.renderer.shape.TileMesh;

class TestTiledInstancedBatching {
    private AssetManager assetManager;

    @BeforeEach void initAssetManager() {
        assetManager = new DesktopAssetManager();
        assetManager.registerLocator("/", ClasspathLocator.class);
        assetManager.registerLoader(J3MLoader.class, "j3md");
        assetManager.registerLoader(GLSLLoader.class, "vert", "frag", "geom", "tsctrl", "tseval", "glsl", "glsllib");
        assetManager.registerLoader(AWTLoader.class, "jpg", "bmp", "gif", "png", "jpeg");
        assetManager.registerLoader(TmxLoader.class, "tmx", "tsx", "tx");
    }

    @Test void rendererAppliesLayerOffsetAndParallaxToLayerNode() {
        TiledMap map = loadOrthogonalMap();
        map.setParallaxOriginX(10);
        map.setParallaxOriginY(20);
        TiledLayer layer = map.getLayer("Ground");
        layer.setOffset(12, -8);
        layer.setParallaxFactor(0.5, 0.25);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node layerNode = findNode(root, "Ground");
        assertNotNull(layerNode);
        assertEquals(177f, layerNode.getLocalTranslation().x, 0.001f);
        assertEquals(247f, layerNode.getLocalTranslation().z, 0.001f);
    }

    @Test void imageLayerUsesImageSizeAndRepeatTextureCoordinates() {
        TiledMap map = loadOrthogonalMap();
        TiledImageLayer imageLayer = new TiledImageLayer(map.getWidth(), map.getHeight());
        imageLayer.setName("Repeating image");
        imageLayer.setRepeatX(true);
        imageLayer.setRepeatY(false);
        imageLayer.setBlendMode(LayerBlendMode.SCREEN);
        TiledImageEntity image = new TiledImageEntity("generated", null, null, 16, 8);
        Texture2D texture = new Texture2D(new Image(Image.Format.RGBA8, 16, 8,
                BufferUtils.createByteBuffer(16 * 8 * 4), ColorSpace.sRGB));
        image.setTexture(texture);
        imageLayer.setImage(image);
        map.addLayer(imageLayer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Geometry geometry = findGeometry(root, "image#Repeating image");
        assertNotNull(geometry);
        FloatBuffer positions = ((FloatBuffer) geometry.getMesh().getBuffer(VertexBuffer.Type.Position).getData()).duplicate();
        FloatBuffer texCoords = ((FloatBuffer) geometry.getMesh().getBuffer(VertexBuffer.Type.TexCoord).getData()).duplicate();
        float expectedWidth = map.getWidth() * map.getTileWidth();
        assertEquals(expectedWidth, positions.get(3), 0.001f);
        assertEquals(8f, positions.get(2), 0.001f);
        assertEquals(expectedWidth / 16f, texCoords.get(2), 0.001f);
        assertEquals(1f, texCoords.get(5), 0.001f);
        assertEquals(Texture.WrapMode.Repeat, texture.getWrap(Texture.WrapAxis.S));
        assertEquals(Texture.WrapMode.EdgeClamp, texture.getWrap(Texture.WrapAxis.T));
        assertEquals(RenderState.BlendMode.Screen, geometry.getMaterial().getAdditionalRenderState().getBlendMode());
    }

    @Test void rendererInstallsTileAlphaOcclusionOverridesByDefault() {
        TiledMap map = loadOrthogonalMap();
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        assertTrue(renderer.isTileAlphaOcclusionEnabled());
        assertEquals(0.30f, renderer.getTileAlphaOcclusionStrength(), 0.001f);
        assertEquals(1.25f, renderer.getTileAlphaOcclusionRadius(), 0.001f);
        assertEquals(Boolean.TRUE, findOverride(root, MaterialConst.USE_TILE_ALPHA_OCCLUSION).getValue());
        assertEquals(0.30f, (Float) findOverride(root, MaterialConst.TILE_ALPHA_OCCLUSION_STRENGTH).getValue(), 0.001f);
        assertEquals(1.25f, (Float) findOverride(root, MaterialConst.TILE_ALPHA_OCCLUSION_RADIUS).getValue(), 0.001f);
    }

    @Test void tileAlphaOcclusionSettersUpdateRendererOverrides() {
        TiledMap map = loadOrthogonalMap();
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.setTileAlphaOcclusionEnabled(false);
        renderer.setTileAlphaOcclusionStrength(2f);
        renderer.setTileAlphaOcclusionRadius(-4f);

        assertFalse(renderer.isTileAlphaOcclusionEnabled());
        assertEquals(1f, renderer.getTileAlphaOcclusionStrength(), 0.001f);
        assertEquals(0f, renderer.getTileAlphaOcclusionRadius(), 0.001f);
        assertEquals(Boolean.FALSE, findOverride(root, MaterialConst.USE_TILE_ALPHA_OCCLUSION).getValue());
        assertEquals(1f, (Float) findOverride(root, MaterialConst.TILE_ALPHA_OCCLUSION_STRENGTH).getValue(), 0.001f);
        assertEquals(0f, (Float) findOverride(root, MaterialConst.TILE_ALPHA_OCCLUSION_RADIUS).getValue(), 0.001f);
    }

    @Test void batchCulledRemovalLeavesTombstoneThenFillsHoleWithLastInstance() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Batch compact", 4);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Geometry batch = batchGeometry(root, "Batch compact");
        assertEquals(4, batch.getNumInstances());
        float[] beforeX = instanceTranslationsX(batch);

        layer.getObjects().get(1).setVisible(false);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertEquals(4, batch.getNumInstances(), "batch-culling should leave a tombstone until the rebatch cooldown expires");
        assertEquals(1, hiddenInstanceCount(batch));

        renderFrames(renderer, pov, 45);

        assertEquals(3, batch.getNumInstances(), "rebatch should compact tombstones after the cooldown");
        float[] afterX = instanceTranslationsX(batch);
        assertEquals(beforeX[0], afterX[0], 0f);
        assertEquals(beforeX[3], afterX[1], 0f, "last instance should be moved into the removed slot");
        assertEquals(beforeX[2], afterX[2], 0f);
    }

    @Test void batchCulledReviveClearsPendingRemoval() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Batch revive", 1);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Geometry batch = batchGeometry(root, "Batch revive");
        layer.getObjects().get(0).setVisible(false);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        layer.getObjects().get(0).setVisible(true);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertEquals(1, batch.getNumInstances());
        assertEquals(0, hiddenInstanceCount(batch));
    }

    @Test void rebatchBudgetCompactsOnlyOneDirtyBatchPerFrame() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer first = addObjectLayer(map, "Batch compact A", 4);
        TiledObjectLayer second = addObjectLayer(map, "Batch compact B", 4);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Geometry firstBatch = batchGeometry(root, "Batch compact A");
        Geometry secondBatch = batchGeometry(root, "Batch compact B");

        first.getObjects().get(0).setVisible(false);
        second.getObjects().get(0).setVisible(false);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertEquals(4, firstBatch.getNumInstances());
        assertEquals(4, secondBatch.getNumInstances());

        renderFrames(renderer, pov, 45);

        int compacted = 0;
        if (firstBatch.getNumInstances() == 3) compacted++;
        if (secondBatch.getNumInstances() == 3) compacted++;
        assertEquals(1, compacted, "only one delayed batch should be compacted in a frame");

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertEquals(3, firstBatch.getNumInstances());
        assertEquals(3, secondBatch.getNumInstances());
    }

    @Test void refinedTransientIgnoresPropertyOnlyChangeAndTracksPositionChange() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Batch transient", 1);
        TiledObjectEntity object = layer.getObjects().get(0);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setBatchDebugEnabled(true);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        object.putProperty("debugOnly", "value");
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertFalse(hasSpatialNamePrefix(root, "transient#"),
                "property-only updates should not trigger transient batching debug");

        object.setX(object.getX() + 32);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertTrue(hasSpatialNamePrefix(root, "transient#position"),
                "position changes should move the object into transient rendering");
        Geometry debug = findGeometryPrefix(root, "transient#position");
        assertNotNull(debug);
        assertTrue(debug.getLocalTranslation().y < 1000f,
                "transient debug overlay should stay inside the default test-app frustum");
        assertFalse(debug.getMaterial().getAdditionalRenderState().isDepthTest(),
                "debug overlay should not disappear behind map geometry");
    }

    @Test void sourceChangeIsTransientAndRendersOutsideBatchBeforeReentry() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Batch source transient", 1);
        TiledObjectEntity object = layer.getObjects().get(0);
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setBatchDebugEnabled(true);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        object.setTile(findDifferentTile(tileLayer, object.getTile()));
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertTrue(hasSpatialNamePrefix(root, "transient#source"),
                "tile/source changes should be represented as transient debug state");
        assertTrue(hasSpatialNamePrefix(root, object.getName()),
                "transient object should still be rendered through the non-instanced path");
    }

    @Test void drawGroupChangeUsesTransientCooldownBeforeReentry() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Batch group transient");
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        TiledObjectEntity still = new TiledObjectEntity(120000, 64, 32, tile);
        TiledObjectEntity moving = new TiledObjectEntity(120001, 96, 96, tile);
        still.setName("still group");
        moving.setName("moving group");
        layer.add(still);
        layer.add(moving);
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setInstancedObjectBatchHeight(128);
        renderer.setBatchDebugEnabled(true);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Geometry oldBatch = batchGeometry(root, "Batch group transient");
        assertEquals(2, oldBatch.getNumInstances());

        renderer.setInstancedObjectBatchHeight(64);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertEquals(2, oldBatch.getNumInstances(),
                "draw-group changes should leave the old batch slot as a tombstone");
        assertEquals(1, hiddenInstanceCount(oldBatch));
        assertTrue(hasSpatialNamePrefix(root, "transient#batch"),
                "draw-group changes should be visible as batch transient debug state");
        assertTrue(hasSpatialNamePrefix(root, "moving group"),
                "entry should render outside instancing while the batch slot cools down");

        renderFrames(renderer, pov, 31);

        assertFalse(hasSpatialNamePrefix(root, "moving group"),
                "entry should return to instancing after draw-group cooldown");
        assertTrue(countInstancedBatchGeometries(root, "Batch group transient") > 1,
                "stable entry should reenter its new draw-group batch");
    }

    @Test void explicitInstancedCulledTileLayerCullsToCameraAndRestores() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Desert/desert.tmx");
        for (TiledLayer layer : map.getLayersFlat()) {
            layer.setRenderingMode(RenderingMode.INSTANCED_CULLED);
        }
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        Camera fullCamera = wideCamera();
        fullCamera.setLocation(new Vector3f(640f, 0f, 640f));
        TestPovRenderer fullPov = new TestPovRenderer(fullCamera);
        renderer.render(new EmptyMapRenderListener(), 0f, fullPov);
        int fullCount = countInstancedTiles(root);
        assertTrue(fullCount > 0);

        Camera smallCamera = new Camera(320, 240);
        smallCamera.setFrustum(-1000f, 1000f, -64f, 64f, 48f, -48f);
        smallCamera.setParallelProjection(true);
        smallCamera.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        smallCamera.setLocation(new Vector3f(120f, 0f, 120f));
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(smallCamera));
        root.updateGeometricState();
        int culledCount = countInstancedTiles(root);
        assertTrue(culledCount > 0);
        assertTrue(culledCount < fullCount);
        assertVisibleToCamera(root, smallCamera);

        renderer.render(new EmptyMapRenderListener(), 0f, fullPov);
        root.updateGeometricState();
        assertEquals(fullCount, countInstancedTiles(root));
        assertVisibleToCamera(root, fullCamera);
    }

    @Test void tileEditsAfterMultidrawSwitchUpdateInstancedBuffer() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer layer = (TiledTileLayer) map.getLayer("Ground");
        Tile first = layer.getTileAt(0, 0).getTile();
        Tile replacement = findDifferentTile(layer, first);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        layer.setRenderingMode(RenderingMode.MULTI_DRAW);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        map.setTileAtFromTileId(layer, 0, 0, replacement.getGid());
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        layer.setRenderingMode(RenderingMode.INSTANCED_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Geometry batch = batchGeometry(root, "Ground");
        assertFalse(hasSpatialNamePrefix(root, "tile#"),
                "switching a tile layer to instancing must remove stale multidraw tile geometries");
        assertTileDataAt(batch, 0f, 0f, replacement);

        Tile secondReplacement = findDifferentTile(layer, layer.getTileAt(1, 0).getTile());
        map.setTileAtFromTileId(layer, 1, 0, secondReplacement.getGid());
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertTileDataAt(batch, 32f, 0f, secondReplacement);
    }

    @Test void multidrawTileRemovalDetachesStaleGeometry() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer layer = (TiledTileLayer) map.getLayer("Ground");
        layer.setRenderingMode(RenderingMode.MULTI_DRAW);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertNotNull(findGeometry(root, "tile#0#0#" + layer.getTileAt(0, 0).getTile().getName()));

        layer.placeTileAt(0, 0, null);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertNull(findGeometryPrefix(root, "tile#0#0#"),
                "multidraw tile removal must detach the old tile geometry");
    }

    @Test void switchingTransientObjectLayerToMultidrawClearsTransientState() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object transient state switch", 3);
        TiledObjectEntity moving = layer.getObjects().get(1);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        moving.setX(moving.getX() + 64);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertTrue(hasSpatialNamePrefix(root, moving.getName()),
                "moving object should render through transient fallback before reentry");

        layer.setRenderingMode(RenderingMode.MULTI_DRAW);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertEquals(3, countDirectGeometries(findNode(root, "Object transient state switch")),
                "switching to multidraw should render only normal object geometries");

        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Node layerNode = findNode(root, "Object transient state switch");
        assertNotNull(findGeometry(layerNode, "tiles#Object transient state switch"),
                "switching back to instancing should rebuild the batch immediately");
        assertFalse(hasSpatialNamePrefix(layerNode, moving.getName()),
                "switching back to instancing should not revive stale transient cooldown state");
    }

    @Test void switchingObjectLayerFromBatchInstancingToMultidrawClearsInstancedGeometry() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Batch mode switch", 3);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Node layerNode = findNode(root, "Batch mode switch");
        assertNotNull(layerNode);
        assertNotNull(findGeometry(layerNode, "tiles#Batch mode switch"));

        layer.setRenderingMode(RenderingMode.MULTI_DRAW);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertNull(findGeometry(layerNode, "tiles#Batch mode switch"),
                "switching to multidraw must remove the old instanced geometry");
        assertEquals(3, countDirectGeometries(layerNode),
                "multidraw object layer should render one geometry per visible object");
        assertTrue(root.getChildIndex(layerNode) > root.getChildIndex(findNode(root, "Ground")),
                "object layer node should stay above the tile layer node after mode switches");
    }

    @Test void movingTransientObjectDoesNotDuplicateAcrossModeSwitches() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Moving mode switch", 3);
        TiledObjectEntity moving = layer.getObjects().get(1);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        moving.setX(moving.getX() + 64);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        Node layerNode = findNode(root, "Moving mode switch");
        assertNotNull(layerNode);
        assertEquals(1, countDirectGeometryNamePrefix(layerNode, moving.getName()),
                "moving transient object should have one fallback spatial in batch mode");

        layer.setRenderingMode(RenderingMode.MULTI_DRAW);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertNull(findGeometry(layerNode, "tiles#Moving mode switch"));
        assertEquals(3, countDirectGeometries(layerNode),
                "switching transient object layer to multidraw should not duplicate object spatials");

        layer.setRenderingMode(RenderingMode.INSTANCED_CULLED);
        moving.setX(moving.getX() + 32);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertEquals(1, countDirectGeometries(layerNode),
                "instanced-culled object layer should only keep the instanced geometry");
        assertNotNull(findGeometry(layerNode, "tiles#Moving mode switch"));
    }

    @Test void instancedCulledObjectLayerKeepsVisibleObjectsInCamera() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object instance culled", 3);
        layer.setRenderingMode(RenderingMode.INSTANCED_CULLED);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        Camera camera = wideCamera();
        camera.setLocation(new Vector3f(128f, 0f, 128f));

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(camera));
        root.updateGeometricState();

        Geometry batch = batchGeometry(root, "Object instance culled");
        assertEquals(3, batch.getNumInstances());
        assertNotEquals(FrustumIntersect.Outside, camera.contains(batch.getWorldBound()),
                "instanced object batch should not be frustum-culled while objects are visible");
    }

    @Test void instancedObjectLayerStoresWorldYForLayerOrdering() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object world y", 1);
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        TiledObjectEntity object = layer.getObjects().get(0);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        Geometry batch = batchGeometry(root, "Object world y");
        int instance = findInstanceAt(batch, (float)object.getX(), (float)object.getY());
        assertEquals(renderer.getWorldYIndex(object), instanceTranslationY(batch, instance), 0.001f,
                "single-object instanced data must include the world Y because instancing bypasses the layer node transform");
        assertEquals(instanceTranslationY(batch, instance), YAxisComparator.sortY(batch), 0.001f,
                "instanced batch sort Y must not be inflated by the fallback bounding-box extent");
    }

    @Test void yAxisComparatorUsesBoundingBoxTopEdgeInsteadOfTranslation() {
        Geometry lower = new Geometry("lower", new Quad(1f, 1f));
        Geometry higher = new Geometry("higher", new Quad(1f, 1f));
        lower.setModelBound(new BoundingBox(new Vector3f(0f, 10f, 0f), 1f, 1f, 1f));
        higher.setModelBound(new BoundingBox(new Vector3f(0f, 5f, 0f), 1f, 11f, 1f));
        lower.updateGeometricState();
        higher.updateGeometricState();

        assertTrue(new YAxisComparator().compare(lower, higher) < 0,
                "batch sorting should use the front Y edge of the world bound, not the geometry translation");
    }

    @Test void yAxisComparatorUsesExplicitSortKeyBeforeBoundingBoxFallback() {
        Geometry lower = new Geometry("lower", new Quad(1f, 1f));
        Geometry higher = new Geometry("higher", new Quad(1f, 1f));
        lower.setModelBound(new BoundingBox(new Vector3f(0f, 10f, 0f), 1f, 1f, 1f));
        higher.setModelBound(new BoundingBox(new Vector3f(0f, 5f, 0f), 1f, 11f, 1f));
        lower.setUserData(YAxisComparator.SORT_Y_USER_DATA, 20f);
        higher.setUserData(YAxisComparator.SORT_Y_USER_DATA, 10f);
        lower.updateGeometricState();
        higher.updateGeometricState();

        assertTrue(new YAxisComparator().compare(lower, higher) > 0,
                "explicit Tiled sort keys should override bounds when the renderer provides them");
    }

    @Test void yAxisComparatorUsesExplicitOrderAsTieBreaker() {
        Geometry earlier = new Geometry("earlier", new Quad(1f, 1f));
        Geometry later = new Geometry("later", new Quad(1f, 1f));
        earlier.setUserData(YAxisComparator.SORT_Y_USER_DATA, 10f);
        later.setUserData(YAxisComparator.SORT_Y_USER_DATA, 10f);
        earlier.setUserData(YAxisComparator.SORT_ORDER_USER_DATA, 1f);
        later.setUserData(YAxisComparator.SORT_ORDER_USER_DATA, 2f);

        assertTrue(new YAxisComparator().compare(earlier, later) < 0,
                "same-depth geometry should preserve the renderer's explicit Tiled order");
    }

    @Test void tileMeshAppliesDiagonalFlipBeforeHorizontalAndVerticalFlips() {
        int gid = Tile.FLIPPED_DIAGONALLY_FLAG | Tile.FLIPPED_HORIZONTALLY_FLAG | 1;
        TileMesh mesh = new TileMesh(new Vector2f(), new Vector2f(1f, 1f), new Vector2f(), new Vector2f(),
                gid, Orientation.ISOMETRIC);

        FloatBuffer texCoords = ((FloatBuffer) mesh.getBuffer(VertexBuffer.Type.TexCoord).getData()).duplicate();
        float[] actual = new float[8];
        texCoords.get(actual);

        assertArrayEquals(new float[] {1f, 0f, 1f, 1f, 0f, 1f, 0f, 0f}, actual, 0.0001f,
                "Tiled applies diagonal flip before horizontal and vertical flips");
    }

    @Test void tileObjectMeshUsesTilesetObjectAlignment() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        tile.getTileset().setObjectAlignment(ObjectAlignment.CENTER);
        TiledObjectEntity object = new TiledObjectEntity(900000, 0, 0, tile);

        TileMesh mesh = new DefaultMeshFactory(map).tile(object);

        FloatBuffer positions = ((FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData()).duplicate();
        assertEquals(-tile.getWidth() * 0.5f, positions.get(0), 0.001f);
        assertEquals(-tile.getHeight() * 0.5f, positions.get(2), 0.001f);
    }

    @Test void instancedTileObjectUsesTilesetObjectAlignment() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        tile.getTileset().setObjectAlignment(ObjectAlignment.CENTER);
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object alignment");
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        layer.add(new TiledObjectEntity(900001, 64, 96, tile));
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Geometry batch = batchGeometry(root, "Object alignment");
        assertOriginData(batch, 0, -tile.getWidth() * 0.5f, tile.getHeight() * 0.5f);
    }

    @Test void instancedShaderAppliesDiagonalFlipBeforeHorizontalAndVerticalFlips() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("Shader/Tiled.vert")) {
            assertNotNull(in, "Tiled vertex shader must be available as a test resource");
            String shader = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int diagonal = shader.indexOf("vec2(1.0 - v_TexCoord.y, 1.0 - v_TexCoord.x)");
            int horizontal = shader.indexOf("v_TexCoord.x = 1.0 - v_TexCoord.x");
            int vertical = shader.indexOf("v_TexCoord.y = 1.0 - v_TexCoord.y");
            assertTrue(diagonal >= 0, "instanced shader should handle Tiled diagonal flip");
            assertTrue(horizontal >= 0, "instanced shader should handle Tiled horizontal flip");
            assertTrue(vertical >= 0, "instanced shader should handle Tiled vertical flip");
            assertTrue(diagonal < horizontal && diagonal < vertical,
                    "instanced shader must apply Tiled diagonal flip before axis flips so decals sample correctly");
        }
    }

    @Test void instancedCulledObjectLayerUpdatesMovedObjectAfterModeSwitch() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object move culled", 3);
        TiledObjectEntity object = layer.getObjects().get(1);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        layer.setRenderingMode(RenderingMode.MULTI_DRAW);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        layer.setRenderingMode(RenderingMode.INSTANCED_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        object.setX(object.getX() + 96);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);

        assertTrue(contains(instanceTranslationsX(batchGeometry(root, "Object move culled")), (float)object.getX()),
                "instanced-culled object movement should update the instance buffer after a runtime mode switch");
    }

    @Test void instancedObjectLayerUpdatesStretchedObjectSizeWithoutPositionChange() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object size update", 1);
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        TiledObjectEntity object = layer.getObjects().get(0);
        Tile tile = object.getTile();
        float uvWidth = tile.getWidth();
        float uvHeight = tile.getHeight();
        FillMode oldFillMode = tile.getTileset().getFillMode();
        tile.getTileset().setFillMode(FillMode.STRETCH);
        object.setWidth(48);
        object.setHeight(40);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        try {
            renderer.render(new EmptyMapRenderListener(), 0f, pov);
            object.setWidth(96);
            object.setHeight(24);
            renderer.render(new EmptyMapRenderListener(), 0f, pov);

            Geometry batch = batchGeometry(root, "Object size update");
            int instance = findInstanceAt(batch, (float)object.getX(), (float)object.getY());
            assertSizeData(batch, instance, 96f, 24f);
            assertUvSizeData(batch, instance, uvWidth, uvHeight);
        } finally {
            tile.getTileset().setFillMode(oldFillMode);
        }
    }

    @Test void instancedObjectLayerUpdatesTileDataWhenTileMutatesInPlace() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object tile data update", 1);
        layer.setRenderingMode(RenderingMode.INSTANCED_CULLED);
        TiledObjectEntity object = layer.getObjects().get(0);
        Tile tile = object.getTile();
        int oldX = tile.getX();
        int oldY = tile.getY();
        boolean oldFlip = tile.isFlippedHorizontally();
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        try {
            renderer.render(new EmptyMapRenderListener(), 0f, pov);
            tile.setX(oldX + 7);
            tile.setY(oldY + 11);
            tile.setFlippedHorizontally(!oldFlip);
            renderer.render(new EmptyMapRenderListener(), 0f, pov);

            Geometry batch = batchGeometry(root, "Object tile data update");
            int instance = findInstanceAt(batch, (float)object.getX(), (float)object.getY());
            assertTileData(batch, instance, tile);
            assertFlipData(batch, instance, tile);
        } finally {
            tile.setX(oldX);
            tile.setY(oldY);
            tile.setFlippedHorizontally(oldFlip);
        }
    }

    @Test void instancedCulledObjectLayerRemovesObjectsOutsideCameraAndRestoresThem() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object cull restore", 3);
        layer.setRenderingMode(RenderingMode.INSTANCED_CULLED);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertEquals(3, batchGeometry(root, "Object cull restore").getNumInstances());

        Camera farCamera = new Camera(320, 240);
        farCamera.setFrustum(-1000f, 1000f, -64f, 64f, 48f, -48f);
        farCamera.setParallelProjection(true);
        farCamera.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        farCamera.setLocation(new Vector3f(2000f, 0f, 2000f));
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(farCamera));
        Geometry culled = findGeometry(root, "tiles#Object cull restore");
        assertTrue(culled == null || culled.getNumInstances() == 0,
                "instanced-culled object layer should not keep stale instances outside the camera");

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertEquals(3, batchGeometry(root, "Object cull restore").getNumInstances());
    }

    @Test void configuredBatchCulledTileLayerCreatesMultipleChunkBatches() {
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Desert/desert.tmx");
        TiledTileLayer layer = (TiledTileLayer) map.getLayer("Ground");
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setInstancedTileChunkSize(8);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertTrue(countInstancedBatchGeometries(root, "Ground") > 1,
                "small tile chunks should produce multiple batch geometries for batch culling");
    }

    @Test void configuredBatchCulledObjectLayerCreatesMultipleBatchGroups() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object multi batch");
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        for (int i = 0; i < 6; i++) {
            layer.add(new TiledObjectEntity(200000 + i, 64, 64 + i * 48, tile));
        }
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setInstancedObjectBatchHeight(64);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertTrue(countInstancedBatchGeometries(root, "Object multi batch") > 1,
                "small object batch height should produce multiple object batch geometries");
    }

    @Test void instancedBatchDefaultsUseMapTileMetrics() {
        TiledMap map = new TiledMap(8, 8);
        map.setTileWidth(32);
        map.setTileHeight(32);
        map.setOrientation(Orientation.ISOMETRIC);
        Tileset tileset = new Tileset(32, 64, 0, 0);
        tileset.setName("Tall tiles");
        tileset.setFirstGid(1);
        tileset.addTile(new Tile(0, 0, 32, 64));
        map.addTileset(tileset);
        TiledObjectLayer objectLayer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        objectLayer.setName("Tall object metrics");
        objectLayer.add(new TiledObjectEntity(620000, 64, 64, tileset.getTile(0)));
        map.addLayer(objectLayer);

        MapRenderer renderer = createRenderer(map, new Node("map"));

        assertEquals(1, renderer.getInstancedIsometricDiagonalSpan(),
                "isometric batching must keep separate draw-order units in separate batches");
        assertEquals(64, renderer.getInstancedObjectBatchHeight(),
                "top-down object batch height should use the largest visual tile height by default");
        assertEquals(1, renderer.getInstancedObjectBatchSize(),
                "INDEX draw order has no tile-size-derived safe range");
    }

    @Test void autoOrthogonalTopDownObjectLayerUsesMultidraw() {
        TiledMap map = loadOrthogonalMap();
        TiledObjectLayer layer = addObjectLayer(map, "Object orthogonal auto", 3);
        layer.setRenderingMode(RenderingMode.AUTO);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node layerNode = findNode(root, "Object orthogonal auto");
        assertNotNull(layerNode);
        assertEquals(0, countInstancedBatchGeometries(root, "Object orthogonal auto"),
                "object layers should preserve per-object ordering by default");
        assertEquals(3, countDirectGeometries(layerNode));
    }

    @Test void autoIsometricTopDownObjectLayerUsesMultidraw() {
        TiledMap map = loadIsometricMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object isometric auto");
        layer.add(new TiledObjectEntity(520000, 0, 64, tile));
        layer.add(new TiledObjectEntity(520001, 64, 64, tile));
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node layerNode = findNode(root, "Object isometric auto");
        assertNotNull(layerNode);
        assertEquals(0, countInstancedBatchGeometries(root, "Object isometric auto"),
                "isometric TOPDOWN object layers should preserve per-object ordering by default");
        assertEquals(2, countDirectGeometries(layerNode));
    }

    @Test void isometricTopDownObjectLayerUsesTiledScreenYForDepth() {
        TiledMap map = loadIsometricMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object isometric screen y");
        TiledObjectEntity frontByScreenY = new TiledObjectEntity(520010, 342, -32, tile);
        TiledObjectEntity backByScreenY = new TiledObjectEntity(520011, 306, -10, tile);
        frontByScreenY.setName("front by screen y");
        backByScreenY.setName("back by screen y");
        layer.add(frontByScreenY);
        layer.add(backByScreenY);
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Geometry front = findGeometry(root, "front by screen y");
        Geometry back = findGeometry(root, "back by screen y");
        assertNotNull(front);
        assertNotNull(back);
        assertTrue(YAxisComparator.sortY(front) > YAxisComparator.sortY(back),
                "isometric TOPDOWN object layers should use Tiled pixelToScreenCoords(object.position).y");
        assertTrue(front.getLocalTranslation().y > back.getLocalTranslation().y,
                "object local depth should use the same Tiled screen-space Y as the render queue");
    }

    @Test void autoIsometricTileLayerUsesBatchInstancing() {
        TiledMap map = loadIsometricMap();
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertTrue(countInstancedBatchGeometries(root, "Ground") > 0,
                "isometric tile layers can batch by draw-order diagonal in AUTO mode");
    }

    @Test void instancingDebugMapsLoadAndRenderInstancedGround() {
        String[] maps = {
                "tmx/InstancingDebug/orthogonal.tmx",
                "tmx/InstancingDebug/isometric.tmx",
                "tmx/InstancingDebug/staggered.tmx",
                "tmx/InstancingDebug/hexagonal-y.tmx",
                "tmx/InstancingDebug/hexagonal-x.tmx"
        };
        for (String path : maps) {
            TiledMap map = (TiledMap) assetManager.loadAsset(path);
            Node root = new Node("map");
            MapRenderer renderer = createRenderer(map, root);

            renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

            assertTrue(countInstancedBatchGeometries(root, "Ground") > 0,
                    path + " should render the visual debug ground layer with instancing");
        }
    }

    @Test void autoHexTileLayersUseLineBatchingAndObjectLayersUseMultidraw() {
        TiledMap map = loadHexagonalDebugMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer objectLayer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        objectLayer.setName("Hex object auto");
        objectLayer.add(new TiledObjectEntity(530000, 0, 0, tile));
        objectLayer.add(new TiledObjectEntity(530001, 32, 0, tile));
        map.addLayer(objectLayer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node tileNode = findNode(root, "Ground");
        Node objectNode = findNode(root, "Hex object auto");
        assertNotNull(tileNode);
        assertNotNull(objectNode);
        assertEquals(0, countDirectGeometryNamePrefix(tileNode, "tile#"),
                "hex tile layers should not keep direct tile geometries in AUTO mode");
        assertTrue(countInstancedBatchGeometries(root, "Ground") > 1,
                "hex tile batching should group by renderer draw line, not collapse the whole layer");
        assertEquals(0, countInstancedBatchGeometries(root, "Hex object auto"),
                "hex object layers should preserve per-object ordering by default");
        assertEquals(2, countDirectGeometries(objectNode));

        tileLayer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        objectLayer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertTrue(countInstancedBatchGeometries(root, "Ground") > 1,
                "forced hex tile batch instancing should use the same line batching policy");
        assertEquals(0, countInstancedBatchGeometries(root, "Hex object auto"),
                "forced hex object instancing should fall back to multidraw");
    }

    @Test void autoHexStaggerXTileLayerUsesLineBatching() {
        TiledMap map = loadHexagonalXDebugMap();
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node tileNode = findNode(root, "Ground");
        assertNotNull(tileNode);
        assertEquals(0, countDirectGeometryNamePrefix(tileNode, "tile#"),
                "hex stagger-x tile layers should not keep direct tile geometries in AUTO mode");
        assertTrue(countInstancedBatchGeometries(root, "Ground") > 1,
                "hex stagger-x tile batching should group by renderer draw line");
    }

    @Test void autoStaggeredTileLayersUseLineBatchingAndObjectLayersUseMultidraw() {
        TiledMap map = loadStaggeredDebugMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer objectLayer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        objectLayer.setName("Staggered object auto");
        objectLayer.add(new TiledObjectEntity(535000, 0, 0, tile));
        objectLayer.add(new TiledObjectEntity(535001, 32, 0, tile));
        map.addLayer(objectLayer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node tileNode = findNode(root, "Ground");
        Node objectNode = findNode(root, "Staggered object auto");
        assertNotNull(tileNode);
        assertNotNull(objectNode);
        assertEquals(0, countDirectGeometryNamePrefix(tileNode, "tile#"),
                "staggered tile layers should not keep direct tile geometries in AUTO mode");
        assertTrue(countInstancedBatchGeometries(root, "Ground") > 1,
                "staggered tile batching should group by renderer draw line, not collapse the whole layer");
        assertEquals(0, countInstancedBatchGeometries(root, "Staggered object auto"),
                "staggered object layers should preserve per-object ordering by default");
        assertEquals(2, countDirectGeometries(objectNode));

        tileLayer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        objectLayer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertTrue(countInstancedBatchGeometries(root, "Ground") > 1,
                "forced staggered tile batch instancing should use the same line batching policy");
        assertEquals(0, countInstancedBatchGeometries(root, "Staggered object auto"),
                "forced staggered object instancing should fall back to multidraw");
    }

    @Test void staggeredTileLineBatchingFallsBackToExactDrawOrderWhenSameLineTilesCanOverlap() {
        TiledMap map = loadStaggeredDebugMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        tileLayer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        map.getTileSets().get(0).getTileOffset().x = map.getTileWidth();
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertEquals(countNonEmptyTiles(tileLayer), countInstancedBatchGeometries(root, "Ground"),
                "wide tile offsets can overlap same-line neighbors, so each tile needs its own draw group");
    }

    @Test void autoIndexObjectLayerUsesMultidraw() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object index auto");
        layer.setDrawOrder(DrawOrder.INDEX);
        layer.add(new TiledObjectEntity(540000, 64, 96, tile));
        layer.add(new TiledObjectEntity(540001, 96, 96, tile));
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node layerNode = findNode(root, "Object index auto");
        assertNotNull(layerNode);
        assertEquals(0, countInstancedBatchGeometries(root, "Object index auto"),
                "INDEX object layers should preserve explicit object order by default");
        assertEquals(2, countDirectGeometries(layerNode));
    }

    @Test void indexObjectLayerUsesLayerAppearanceOrderForDepth() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object index depth");
        layer.setDrawOrder(DrawOrder.INDEX);
        TiledObjectEntity firstInLayer = new TiledObjectEntity(540011, 96, 96, tile);
        TiledObjectEntity secondInLayer = new TiledObjectEntity(540010, 64, 96, tile);
        firstInLayer.setName("first in layer");
        secondInLayer.setName("second in layer");
        layer.add(firstInLayer);
        layer.add(secondInLayer);
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);

        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Geometry first = findGeometry(root, "first in layer");
        Geometry second = findGeometry(root, "second in layer");
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(YAxisComparator.sortY(first) < YAxisComparator.sortY(second),
                "INDEX draw order means layer appearance order, not object id order");
    }

    @Test void instancedIndexObjectLayerDoesNotGroupObjectsWithConfiguredBatchSize() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object index configured batch groups");
        layer.setDrawOrder(DrawOrder.INDEX);
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        layer.add(new TiledObjectEntity(600001, 224, 96, tile));
        layer.add(new TiledObjectEntity(600000, 64, 96, tile));
        map.addLayer(layer);

        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setInstancedObjectBatchSize(128);
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertEquals(2, countInstancedBatchGeometries(root, "Object index configured batch groups"),
                "INDEX draw order must keep each object index in its own batch even if a larger batch size is configured");
    }

    @Test void instancedIndexObjectLayerSplitsDrawGroupsByDefault() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object index batch groups");
        layer.setDrawOrder(DrawOrder.INDEX);
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        layer.add(new TiledObjectEntity(610000, 64, 96, tile));
        layer.add(new TiledObjectEntity(610001, 96, 96, tile));
        map.addLayer(layer);

        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        assertEquals(2, countInstancedBatchGeometries(root, "Object index batch groups"),
                "INDEX draw order should not group multiple object indices into the same batch by default");
    }

    @Test void forcedIsometricObjectInstancingFallsBackToMultidraw() {
        TiledMap map = loadIsometricMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object isometric batch group");
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        layer.add(new TiledObjectEntity(500000, 0, 66, tile));
        layer.add(new TiledObjectEntity(500001, 780, 66, tile));
        map.addLayer(layer);

        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setInstancedObjectBatchHeight(64);
        renderer.render(new EmptyMapRenderListener(), 0f, new TestPovRenderer(wideCamera()));

        Node layerNode = findNode(root, "Object isometric batch group");
        assertNotNull(layerNode);
        assertEquals(0, countInstancedBatchGeometries(root, "Object isometric batch group"),
                "isometric object layers should preserve per-object ordering even when instancing is requested");
        assertEquals(2, countDirectGeometries(layerNode));
    }

    @Test void batchCulledObjectReentryKeepsDistinctTileData() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile stillTile = tileLayer.getTileAt(0, 0).getTile();
        Tile movingTile = findDifferentTile(tileLayer, stillTile);
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object reentry data");
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        TiledObjectEntity still = new TiledObjectEntity(300000, 160, 160, stillTile);
        TiledObjectEntity moving = new TiledObjectEntity(300001, 64, 96, movingTile);
        still.setName("still");
        moving.setName("moving");
        layer.add(still);
        layer.add(moving);
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setInstancedObjectBatchHeight(64);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        moving.setX(192);
        moving.setY(160);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        renderFrames(renderer, pov, 31);
        renderFrames(renderer, pov, 46);

        Geometry batch = batchGeometry(root, "Object reentry data");
        assertTileDataAt(batch, 160f, 160f, stillTile);
        assertTileDataAt(batch, 192f, 160f, movingTile);
        assertFalse(hasSpatialNamePrefix(root, "moving"),
                "object should return to instancing after transient cooldown");
    }

    @Test void batchCulledSameBatchObjectReentryKeepsDistinctTileData() {
        TiledMap map = loadOrthogonalMap();
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile stillTile = tileLayer.getTileAt(0, 0).getTile();
        Tile movingTile = findDifferentTile(tileLayer, stillTile);
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName("Object same batch reentry");
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        TiledObjectEntity still = new TiledObjectEntity(310000, 160, 160, stillTile);
        TiledObjectEntity moving = new TiledObjectEntity(310001, 64, 160, movingTile);
        TiledObjectEntity neighbor = new TiledObjectEntity(310002, 256, 160, stillTile);
        still.setName("still same batch");
        moving.setName("moving same batch");
        neighbor.setName("neighbor same batch");
        layer.add(still);
        layer.add(moving);
        layer.add(neighbor);
        map.addLayer(layer);
        Node root = new Node("map");
        MapRenderer renderer = createRenderer(map, root);
        renderer.setInstancedObjectBatchHeight(512);
        TestPovRenderer pov = new TestPovRenderer(wideCamera());

        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        moving.setX(192);
        renderer.render(new EmptyMapRenderListener(), 0f, pov);
        assertTrue(hasSpatialNamePrefix(root, "moving same batch"),
                "moving object should be rendered as transient while the batch slot cools down");
        renderFrames(renderer, pov, 31);

        Geometry batch = batchGeometry(root, "Object same batch reentry");
        assertTileDataAt(batch, 160f, 160f, stillTile);
        assertTileDataAt(batch, 192f, 160f, movingTile);
        assertTileDataAt(batch, 256f, 160f, stillTile);
        assertFalse(hasSpatialNamePrefix(root, "moving same batch"),
                "object should return to the same instanced batch after transient cooldown");
    }

    private TiledMap loadOrthogonalMap() {
        return (TiledMap) assetManager.loadAsset("tmx/Orthogonal/01.tmx");
    }

    private TiledMap loadIsometricMap() {
        return (TiledMap) assetManager.loadAsset("tmx/Isometric/01.tmx");
    }

    private TiledMap loadHexagonalMap() {
        return (TiledMap) assetManager.loadAsset("tmx/Hexagonal/01.tmx");
    }

    private TiledMap loadStaggeredMap() {
        return (TiledMap) assetManager.loadAsset("tmx/Staggered/01.tmx");
    }

    private TiledMap loadStaggeredDebugMap() {
        return (TiledMap) assetManager.loadAsset("tmx/InstancingDebug/staggered.tmx");
    }

    private TiledMap loadHexagonalDebugMap() {
        return (TiledMap) assetManager.loadAsset("tmx/InstancingDebug/hexagonal-y.tmx");
    }

    private TiledMap loadHexagonalXDebugMap() {
        return (TiledMap) assetManager.loadAsset("tmx/InstancingDebug/hexagonal-x.tmx");
    }

    private TiledObjectLayer addObjectLayer(TiledMap map, String name, int count) {
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();
        TiledObjectLayer layer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        layer.setName(name);
        layer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        for (int i = 0; i < count; i++) {
            TiledObjectEntity object = new TiledObjectEntity(100000 + i, 64 + i * 32, 96, tile);
            object.setName(name + " object " + i);
            layer.add(object);
        }
        map.addLayer(layer);
        return layer;
    }

    private Tile findDifferentTile(TiledTileLayer tileLayer, Tile current) {
        for (int y = 0; y < tileLayer.getHeight(); y++) {
            for (int x = 0; x < tileLayer.getWidth(); x++) {
                Tile tile = tileLayer.getTileAt(x, y).getTile();
                if (tile != null && tile != current && tile.getGid() != current.getGid()) {
                    return tile;
                }
            }
        }
        throw new AssertionError("Could not find a different tile in test map");
    }

    private MapRenderer createRenderer(TiledMap map, Node root) {
        MapRenderer renderer = MapRenderer.create(map, 32, root);
        DefaultSpriteFactory spriteFactory = new DefaultSpriteFactory();
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));
        spriteFactory.setMaterialFactory(new DefaultMaterialFactory(assetManager));
        renderer.setSpriteFactory(spriteFactory);
        return renderer;
    }

    private Camera wideCamera() {
        Camera cam = new Camera(1280, 720);
        cam.setFrustum(-1000f, 1000f, -640f, 640f, 360f, -360f);
        cam.setParallelProjection(true);
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        cam.setLocation(new Vector3f(320f, 0f, 320f));
        return cam;
    }

    private void renderFrames(MapRenderer renderer, TestPovRenderer pov, int frames) {
        for (int i = 0; i < frames; i++) {
            renderer.render(new EmptyMapRenderListener(), 0f, pov);
        }
    }

    private Geometry batchGeometry(Node root, String layerName) {
        Geometry geometry = findGeometry(root, "tiles#" + layerName);
        assertNotNull(geometry, "Missing instanced geometry for layer " + layerName);
        return geometry;
    }

    private MatParamOverride findOverride(Node node, String name) {
        for (MatParamOverride override : node.getLocalMatParamOverrides()) {
            if (name.equals(override.getName())) {
                return override;
            }
        }
        fail("Missing material override " + name);
        return null;
    }

    private Geometry findGeometry(Node node, String name) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry && name.equals(child.getName())) {
                return (Geometry) child;
            }
            if (child instanceof Node) {
                Geometry found = findGeometry((Node) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Geometry findGeometryPrefix(Node node, String prefix) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry && child.getName() != null && child.getName().startsWith(prefix)) {
                return (Geometry) child;
            }
            if (child instanceof Node) {
                Geometry found = findGeometryPrefix((Node) child, prefix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Node findNode(Node node, String name) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Node && name.equals(child.getName())) {
                return (Node) child;
            }
            if (child instanceof Node) {
                Node found = findNode((Node) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private int countDirectGeometries(Node node) {
        int count = 0;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                count++;
            }
        }
        return count;
    }

    private int countDirectGeometryNamePrefix(Node node, String prefix) {
        int count = 0;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry && child.getName() != null && child.getName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private void assertVisibleToCamera(Node root, Camera camera) {
        for (Spatial child : root.getChildren()) {
            if (child instanceof Geometry && child.getName().startsWith("tiles#")) {
                FrustumIntersect result = camera.contains(child.getWorldBound());
                assertNotEquals(FrustumIntersect.Outside, result, child.getName() + " should not be frustum-culled");
            } else if (child instanceof Node) {
                assertVisibleToCamera((Node) child, camera);
            }
        }
    }

    private int countInstancedTiles(Node node) {
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

    private int countNonEmptyTiles(TiledTileLayer layer) {
        int count = 0;
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                if (layer.getTileAt(x, y).getTile() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countInstancedBatchGeometries(Node node, String layerName) {
        int count = 0;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry && ("tiles#" + layerName).equals(child.getName())) {
                count++;
            } else if (child instanceof Node) {
                count += countInstancedBatchGeometries((Node) child, layerName);
            }
        }
        return count;
    }

    private void assertTileData(Geometry geometry, int instance, Tile expected) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.TexCoord2);
        assertEquals((float)expected.getX(), data.get(instance * 4), 0f);
        assertEquals((float)expected.getY(), data.get(instance * 4 + 1), 0f);
        assertTrue(data.get(instance * 4 + 2) >= 0f, "edited tile should be visible in instanced data");
    }

    private void assertTileDataAt(Geometry geometry, float x, float z, Tile expected) {
        int instance = findInstanceAt(geometry, x, z);
        assertTileData(geometry, instance, expected);
    }

    private void assertFlipData(Geometry geometry, int instance, Tile expected) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.TexCoord2);
        int flags = 0;
        if (expected.isFlippedHorizontally()) flags |= 1;
        if (expected.isFlippedVertically()) flags |= 2;
        if (expected.isFlippedAntiDiagonally()) flags |= 4;
        assertEquals((float) flags, data.get(instance * 4 + 3), 0f);
    }

    private void assertSizeData(Geometry geometry, int instance, float width, float height) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.TexCoord3);
        assertEquals(width, data.get(instance * 4), 0.001f);
        assertEquals(height, data.get(instance * 4 + 1), 0.001f);
    }

    private void assertUvSizeData(Geometry geometry, int instance, float width, float height) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.TexCoord5);
        assertEquals(width, data.get(instance * 2), 0.001f);
        assertEquals(height, data.get(instance * 2 + 1), 0.001f);
    }

    private void assertOriginData(Geometry geometry, int instance, float x, float y) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.TexCoord4);
        assertEquals(x, data.get(instance * 4), 0.001f);
        assertEquals(y, data.get(instance * 4 + 1), 0.001f);
    }

    private int findInstanceAt(Geometry geometry, float x, float z) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.InstanceData);
        for (int i = 0; i < geometry.getNumInstances(); i++) {
            float ix = data.get(i * 16 + 12);
            float iz = data.get(i * 16 + 14);
            if (Math.abs(ix - x) < 0.001f && Math.abs(iz - z) < 0.001f) {
                return i;
            }
        }
        throw new AssertionError("Missing instance at " + x + ", " + z);
    }

    private float[] instanceTranslationsX(Geometry geometry) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.InstanceData);
        float[] out = new float[geometry.getNumInstances()];
        for (int i = 0; i < out.length; i++) {
            out[i] = data.get(i * 16 + 12);
        }
        return out;
    }

    private float instanceTranslationY(Geometry geometry, int instance) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.InstanceData);
        return data.get(instance * 16 + 13);
    }

    private int hiddenInstanceCount(Geometry geometry) {
        FloatBuffer data = instanceBuffer(geometry, VertexBuffer.Type.TexCoord2);
        int count = 0;
        for (int i = 0; i < geometry.getNumInstances(); i++) {
            if (data.get(i * 4 + 2) < 0f) {
                count++;
            }
        }
        return count;
    }

    private boolean contains(float[] values, float expected) {
        for (float value : values) {
            if (Math.abs(value - expected) < 0.001f) {
                return true;
            }
        }
        return false;
    }

    private FloatBuffer instanceBuffer(Geometry geometry, VertexBuffer.Type type) {
        VertexBuffer buffer = geometry.getMesh().getBuffer(type);
        assertNotNull(buffer, "Missing " + type + " buffer");
        return ((FloatBuffer) buffer.getData()).duplicate();
    }

    private boolean hasSpatialNamePrefix(Node node, String prefix) {
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
