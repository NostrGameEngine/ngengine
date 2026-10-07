/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.shadow;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightFilter;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.NullRenderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.clone.Cloner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowRendererLifecycleTest {
    private static final DesktopAssetManager ASSETS = new DesktopAssetManager(true);

    @Test
    void mapDrawFailureRestoresPreviouslyForcedStateAndAllowsNextFrame() {
        Fixture fixture = new Fixture(pbrMaterial());
        fixture.manager.throwOnTechnique = "PreShadow";

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> fixture.shadows.postQueue(fixture.viewPort.getQueue()));

        assertSame(fixture.manager.failure, thrown);
        assertTrue(fixture.manager.preShadowDraws > 0);
        assertTrue(fixture.manager.sawNativeMapTarget);
        fixture.assertSavedState();
        assertTrue(fixture.shadows.isInitialized());
        assertSame(fixture.viewPort, fixture.shadows.viewPort);

        int drawsAfterFailure = fixture.manager.preShadowDraws;
        fixture.manager.throwOnTechnique = null;
        fixture.shadows.postQueue(fixture.viewPort.getQueue());
        assertTrue(fixture.manager.preShadowDraws > drawsAfterFailure);
        fixture.assertSavedState();
    }

    @Test
    void receiverDrawFailureRestoresForcedStateAndClearsTransientMaterialParameters() {
        Fixture fixture = new Fixture(pbrMaterial());
        fixture.shadows.postQueue(fixture.viewPort.getQueue());
        fixture.manager.throwOnTechnique = "PostShadow";

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> fixture.shadows.postFrame(fixture.savedTarget));

        assertSame(fixture.manager.failure, thrown);
        assertTrue(fixture.manager.sawReceiverMaps);
        fixture.assertSavedState();
        assertNull(fixture.material.getParam("ShadowMap1"));
        assertNull(fixture.material.getParam("ShadowMap2"));
        assertNull(fixture.material.getParam("LightViewProjectionMatrix1"));
        assertNull(fixture.material.getParam("LightViewProjectionMatrix2"));
        assertNull(fixture.material.getParam("Splits"));
        assertNull(fixture.material.getParam("FadeInfo"));
        assertNull(fixture.material.getParam("LightDir"));
        // The native post-shadow contract keeps map0 and common scalar parameters.
        assertSame(fixture.shadows.shadowMaps[0], fixture.material.getParam("ShadowMap0").getValue());
        assertEquals(.45f, ((Float) fixture.material.getParam("ShadowIntensity").getValue()).floatValue());

        fixture.manager.throwOnTechnique = null;
        fixture.shadows.postFrame(fixture.savedTarget);
        assertEquals(2, fixture.manager.postShadowDraws);
        fixture.assertSavedState();
    }

    @Test
    void fallbackDrawFailureRestoresPreviouslyForcedMaterial() {
        Material fallback = new Material(new MaterialDef(ASSETS, "No post-shadow technique"));
        Fixture fixture = new Fixture(fallback);
        fixture.shadows.postQueue(fixture.viewPort.getQueue());
        fixture.manager.throwOnTechnique = "PostShadow";

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> fixture.shadows.postFrame(fixture.savedTarget));

        assertSame(fixture.manager.failure, thrown);
        assertSame(fixture.shadows.postshadowMat, fixture.manager.forcedMaterialDuringPost);
        fixture.assertSavedState();
        assertTrue(fixture.shadows.isInitialized());
    }

    @Test
    void successfulShadowPassesPreservePreviouslyForcedState() {
        Fixture fixture = new Fixture(pbrMaterial());

        fixture.shadows.postQueue(fixture.viewPort.getQueue());
        assertTrue(fixture.manager.preShadowDraws > 0);
        fixture.assertSavedState();
        fixture.shadows.postFrame(fixture.savedTarget);

        assertEquals(1, fixture.manager.postShadowDraws);
        fixture.assertSavedState();
        assertNull(fixture.material.getParam("ShadowMap1"));
        assertNull(fixture.material.getParam("FadeInfo"));
    }

    @Test
    void activeRendererCloneStartsDetachedWithIndependentTransientState() {
        Fixture fixture = new Fixture(pbrMaterial());
        fixture.shadows.postQueue(fixture.viewPort.getQueue());
        fixture.shadows.shadowMapOccluders.add(fixture.geometry);
        fixture.shadows.matCache.add(fixture.material);
        fixture.shadows.frustumCam = fixture.viewPort.getCamera().clone();
        fixture.shadows.skipPostPass = true;
        fixture.shadows.needsfallBackMaterial = true;
        Vector3f[] originalPoints = fixture.shadows.points;
        Vector3f[] pointValues = new Vector3f[originalPoints.length];
        for (int index = 0; index < originalPoints.length; index++) pointValues[index] = originalPoints[index].clone();
        Vector2f originalFade = fixture.shadows.fadeInfo.clone();
        Camera originalFrustum = fixture.shadows.frustumCam;

        DirectionalLightShadowRenderer clone = new Cloner().clone(fixture.shadows);

        assertTrue(fixture.shadows.isInitialized());
        assertFalse(clone.isInitialized());
        assertSame(fixture.manager, fixture.shadows.renderManager);
        assertSame(fixture.viewPort, fixture.shadows.viewPort);
        assertNull(clone.renderManager);
        assertNull(clone.viewPort);
        assertSame(originalFrustum, fixture.shadows.frustumCam);
        assertNull(clone.frustumCam);
        assertTrue(fixture.shadows.skipPostPass);
        assertTrue(fixture.shadows.needsfallBackMaterial);
        assertFalse(clone.skipPostPass);
        assertFalse(clone.needsfallBackMaterial);
        assertNotSame(fixture.shadows.lightReceivers, clone.lightReceivers);
        assertNotSame(fixture.shadows.shadowMapOccluders, clone.shadowMapOccluders);
        assertNotSame(fixture.shadows.matCache, clone.matCache);
        assertEquals(0, clone.lightReceivers.size());
        assertEquals(0, clone.shadowMapOccluders.size());
        assertTrue(clone.matCache.isEmpty());
        assertSame(fixture.geometry, fixture.shadows.lightReceivers.get(0));
        assertSame(fixture.geometry, fixture.shadows.shadowMapOccluders.get(0));
        assertSame(fixture.material, fixture.shadows.matCache.get(0));
        assertNotSame(fixture.shadows.forcedRenderState, clone.forcedRenderState);
        assertNotSame(fixture.shadows.tempVec, clone.tempVec);
        assertNotSame(originalPoints, clone.points);
        assertSame(originalPoints, fixture.shadows.points);
        for (int index = 0; index < originalPoints.length; index++) {
            assertNotSame(originalPoints[index], clone.points[index]);
            assertEquals(pointValues[index], originalPoints[index]);
        }
        assertNotSame(fixture.shadows.fadeInfo, clone.fadeInfo);
        assertEquals(originalFade, clone.fadeInfo);
        clone.setShadowZExtend(130);
        assertEquals(originalFade, fixture.shadows.fadeInfo);
        assertEquals(new Vector2f(105, .04f), clone.fadeInfo);
        assertNotSame(fixture.shadows.light, clone.light);
        assertEquals(fixture.shadows.light.getDirection(), clone.light.getDirection());
        assertEquals(fixture.shadows.light.getColor(), clone.light.getColor());
        assertEquals(fixture.shadows.getLambda(), clone.getLambda());
        assertEquals(fixture.shadows.getShadowIntensity(), clone.getShadowIntensity());
        assertEquals(fixture.shadows.getEdgeFilteringMode(), clone.getEdgeFilteringMode());
        assertEquals(fixture.shadows.getShadowCompareMode(), clone.getShadowCompareMode());
        assertEquals(3, clone.nbShadowMaps);
        for (int index = 0; index < clone.nbShadowMaps; index++) {
            assertNotSame(fixture.shadows.shadowMaps[index], clone.shadowMaps[index]);
            assertNotSame(fixture.shadows.shadowMaps[index].getImage(), clone.shadowMaps[index].getImage());
            assertNotSame(fixture.shadows.shadowFB[index], clone.shadowFB[index]);
            assertNotSame(fixture.shadows.lightViewProjectionsMatrices[index], clone.lightViewProjectionsMatrices[index]);
            assertEquals(64, clone.shadowMaps[index].getImage().getWidth());
            assertEquals(fixture.shadows.shadowMaps[index].getShadowCompareMode(), clone.shadowMaps[index].getShadowCompareMode());
        }

        Camera camera = fixture.viewPort.getCamera().clone();
        ViewPort cloneView = new ViewPort("clone", camera);
        Node emptyScene = new Node("empty scene");
        emptyScene.updateGeometricState();
        cloneView.attachScene(emptyScene);
        RecordingRenderer cloneRenderer = new RecordingRenderer();
        RecordingManager cloneManager = new RecordingManager(cloneRenderer);
        clone.initialize(cloneManager, cloneView);
        clone.postQueue(cloneView.getQueue());
        clone.postFrame(null);

        assertTrue(clone.isInitialized());
        assertSame(cloneView, clone.viewPort);
        assertSame(cloneManager, clone.renderManager);
        assertEquals(0, cloneManager.preShadowDraws);
        assertEquals(0, cloneManager.postShadowDraws);
        assertSame(fixture.geometry, fixture.shadows.lightReceivers.get(0));
        assertSame(fixture.geometry, fixture.shadows.shadowMapOccluders.get(0));
        assertSame(fixture.material, fixture.shadows.matCache.get(0));
        assertSame(originalFrustum, fixture.shadows.frustumCam);
        for (int index = 0; index < originalPoints.length; index++) assertEquals(pointValues[index], originalPoints[index]);
        fixture.assertSavedState();
    }

    @Test
    void uninitializedRendererCloneRemainsUninitializedAndKeepsConfiguration() {
        DirectionalLightShadowRenderer source = new DirectionalLightShadowRenderer(ASSETS, 64, 3);
        source.setLight(new DirectionalLight(new Vector3f(0, -1, 0)));
        source.setShadowZExtend(110);
        source.setShadowZFadeLength(25);

        DirectionalLightShadowRenderer clone = new Cloner().clone(source);

        assertFalse(source.isInitialized());
        assertFalse(clone.isInitialized());
        assertEquals(110, clone.getShadowZExtend());
        assertEquals(25, clone.getShadowZFadeLength());
        assertNotSame(source.fadeInfo, clone.fadeInfo);
        clone.setShadowZFadeLength(0);
        assertEquals(25, source.getShadowZFadeLength());
        assertNull(clone.fadeInfo);
    }

    private static Material pbrMaterial() {
        return new Material(ASSETS, "Common/MatDefs/Light/PBRLighting.j3md");
    }

    private static final class Fixture {
        private final Material material;
        private final Geometry geometry;
        private final RecordingRenderer renderer = new RecordingRenderer();
        private final RecordingManager manager = new RecordingManager(renderer);
        private final ViewPort viewPort;
        private final DirectionalLightShadowRenderer shadows = new DirectionalLightShadowRenderer(ASSETS, 64, 3);
        private final Material savedMaterial = new Material(ASSETS, "Common/MatDefs/Misc/Unshaded.j3md");
        private final RenderState savedState = new RenderState();
        private final FrameBuffer savedTarget = new FrameBuffer(128, 96, 1);
        private final LightFilter savedFilter = new LightFilter() {
            @Override public void setCamera(Camera camera) { }
            @Override public void filterLights(Geometry geometry, LightList filteredLightList) { }
        };

        private Fixture(Material material) {
            this.material = material;
            Camera camera = new Camera(320, 180);
            camera.setFrustumPerspective(78, 320f / 180, .1f, 160);
            camera.lookAtDirection(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
            camera.update();
            viewPort = new ViewPort("scene", camera);
            viewPort.setOutputFrameBuffer(new FrameBuffer(64, 64, 1));
            geometry = new Geometry("receiver and caster", new Box(.2f, .2f, .2f));
            geometry.setLocalTranslation(0, 0, -5);
            geometry.setMaterial(material);
            geometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            Node scene = new Node("scene");
            scene.attachChild(geometry);
            scene.updateGeometricState();
            viewPort.attachScene(scene);
            viewPort.addProcessor(shadows);
            shadows.setLight(new DirectionalLight(new Vector3f(-.3f, -1, -.2f).normalizeLocal(), new ColorRGBA(.4f, .3f, .2f, 1)));
            shadows.setLambda(.7f);
            shadows.setShadowZExtend(110);
            shadows.setShadowZFadeLength(25);
            shadows.setShadowIntensity(.45f);
            shadows.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
            shadows.initialize(manager, viewPort);
            savedState.setDepthWrite(false);
            manager.setForcedMaterial(savedMaterial);
            manager.setForcedTechnique("previous technique");
            manager.setForcedRenderState(savedState);
            manager.setLightFilter(savedFilter);
            manager.setCamera(camera, false);
            renderer.setFrameBuffer(savedTarget);
        }

        private void assertSavedState() {
            assertSame(savedMaterial, manager.getForcedMaterial());
            assertEquals("previous technique", manager.getForcedTechnique());
            assertSame(savedState, manager.getForcedRenderState());
            assertSame(savedFilter, manager.getLightFilter());
            assertSame(savedTarget, renderer.getCurrentFrameBuffer());
            assertSame(viewPort.getCamera(), manager.getCurrentCamera());
        }
    }

    private static final class RecordingRenderer extends NullRenderer {
        private FrameBuffer target;
        @Override public void setFrameBuffer(FrameBuffer target) { this.target = target; }
        @Override public FrameBuffer getCurrentFrameBuffer() { return target; }
    }

    private static final class RecordingManager extends RenderManager {
        private final RuntimeException failure = new IllegalStateException("synthetic draw failure");
        private String throwOnTechnique;
        private int preShadowDraws;
        private int postShadowDraws;
        private boolean sawNativeMapTarget;
        private boolean sawReceiverMaps;
        private Material forcedMaterialDuringPost;

        private RecordingManager(RecordingRenderer renderer) { super(renderer); }

        @Override public void renderGeometry(Geometry geometry) {
            String technique = getForcedTechnique();
            if ("PreShadow".equals(technique)) {
                preShadowDraws++;
                sawNativeMapTarget = getRenderer().getCurrentFrameBuffer().getWidth() == 64;
            }
            if ("PostShadow".equals(technique)) {
                postShadowDraws++;
                forcedMaterialDuringPost = getForcedMaterial();
                sawReceiverMaps = geometry.getMaterial().getParam("ShadowMap1") != null
                        && geometry.getMaterial().getParam("FadeInfo") != null;
            }
            if (technique != null && technique.equals(throwOnTechnique)) throw failure;
        }
    }
}
