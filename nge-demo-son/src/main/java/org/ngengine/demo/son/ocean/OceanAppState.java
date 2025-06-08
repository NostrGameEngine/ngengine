/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.ocean;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.util.TempVars;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.MainViewPortFragment;
import org.ngengine.components.fragments.RenderFragment;
import org.ngengine.demo.son.PhysicsManager;
import org.ngengine.demo.son.controls.BuoyancyControl;
import org.ngengine.demo.son.controls.WindControl;
import org.ngengine.demo.son.utils.GridMesh;
import org.ngengine.demo.son.utils.ReflectionBaker;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStore;
import org.ngengine.store.DataStoreProvider;

public class OceanAppState implements Component<Object>, MainViewPortFragment, RenderFragment, AssetLoadingFragment {

    private static final Logger log = Logger.getLogger(OceanAppState.class.getName());
    private Geometry oceanGeometry;

    private int VERTEX_DENSITY = 256;
    private final int IBOCEAN_RESOLUTION = 1024;
    private final int IBOCEAN_LAYERS = 3;

    private final float GRID_SIZE = 1024;
    private final float HORIZON_EXTENT = 3000f;
    private final Format reflectionsFormat = Image.Format.RGBA16F;
    private final int reflectionSize = 1024;

    private final List<BuoyancyControl> controls = new ArrayList<>();
    private final Vector3f WAVE_SCALE = new Vector3f(1f, 60f, 1f);
    private final Vector3f WIND = new Vector3f(0, 0, 64f);

    private final Vector3f samplePos = new Vector3f();

    private Texture2D reflectionMap;
    private Camera envCam;
    private ReflectionBaker reflectionProcessor;
    private Plane plane;
    private ViewPort reflectionViewPort;
    private AudioNode oceanWavesSound;
    private IBOcean ibocean;
    private RenderManager renderManager;
    private AssetManager assetManager;
    private ViewPort viewPort;
    private ComponentManager componentManager;

    @Override
    public Object getSlot() {
        return "environment";
    }

    public OceanAppState(int vertexDensity) {
        super();
        this.VERTEX_DENSITY = vertexDensity;
    }

    public OceanAppState() {
        this(256);
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    @Override
    public void receiveRenderManager(RenderManager renderManager) {
        this.renderManager = renderManager;
    }

    @Override
    public void loadAssets(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public void receiveMainViewPort(ViewPort viewPort) {
        this.viewPort = viewPort;
    }

    public BulletAppState getPhysics() {
        return componentManager.getComponent(PhysicsManager.class).getPhysics();
    }

    private void initializeReflectionView(ViewPort vp, Node scene) {
        FrameBuffer refbuf = new FrameBuffer(reflectionSize, reflectionSize, 1);
        refbuf.setMultiTarget(true);
        {
            reflectionMap = new Texture2D(reflectionSize, reflectionSize, reflectionsFormat);
            reflectionMap.setName("ReflectionMap");
            refbuf.addColorTarget(FrameBufferTarget.newTarget(reflectionMap));
            refbuf.setDepthTarget(FrameBufferTarget.newTarget(Format.Depth));
        }

        envCam = new Camera(reflectionSize, reflectionSize);
        envCam.setName("ReflectionCamera");

        plane = new Plane(Vector3f.UNIT_Y, 0);

        {
            reflectionProcessor = new ReflectionBaker(oceanGeometry, envCam, refbuf, plane);
            reflectionProcessor.setReflectionClipPlane(plane);
        }

        {
            reflectionViewPort = renderManager.createPreView("ReflectionViewport", envCam);
            reflectionViewPort.setClearFlags(true, true, true);
            reflectionViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);
            reflectionViewPort.attachScene(scene);
            reflectionViewPort.addProcessor(reflectionProcessor);
        }
    }

    @Override
    public void onEnable(
        ComponentManager fragmentManager,
        Runner runner,
        DataStoreProvider dataStoreProvider,
        boolean firstTime,
        Object arg
    ) {
        this.componentManager = fragmentManager;
        Node rootNode = getRootNode(viewPort);

        oceanWavesSound = new AudioNode(assetManager, "Sounds/Beach_Ocean_Waves_Fienup_001_mono.ogg", DataType.Buffer);
        oceanWavesSound.setLooping(true);
        oceanWavesSound.setPositional(false);
        oceanWavesSound.setVolume(0.8f);
        oceanWavesSound.setPitch(FastMath.clamp(this.WIND.length() / 120f, 0f, 0.5f) + 0.5f);

        oceanWavesSound.play();
        rootNode.attachChild(oceanWavesSound);
        WAVE_SCALE.set(1f, 10f, 1f);

        DataStore oceanData = dataStoreProvider.getDataStore("ocean");
        try {
            ibocean = oceanData.read("iboceandata");
        } catch (Exception e) {
            log.warning("Failed to load IBOcean data, generating new ocean data.");
            ibocean = null;
        }
        if (ibocean == null) {
            ibocean =
                OceanLayerGenerator.generateOcean(
                    new Vector2f(GRID_SIZE, GRID_SIZE),
                    IBOCEAN_RESOLUTION,
                    IBOCEAN_LAYERS,
                    WAVE_SCALE,
                    WIND
                );
            try {
                oceanData.write("iboceandata", ibocean);
            } catch (Exception e) {
                log.warning("Failed to save IBOcean data: " + e.getMessage());
            }
        }

        oceanGeometry = new Geometry("OceanSurface", new GridMesh(VERTEX_DENSITY, GRID_SIZE, HORIZON_EXTENT));
        oceanGeometry.setMaterial(ibocean.getMaterial(assetManager));
        oceanGeometry.setQueueBucket(Bucket.Opaque);
        oceanGeometry.setCullHint(Spatial.CullHint.Never);

        oceanGeometry.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(oceanGeometry);
        MikktspaceTangentGenerator.generate(oceanGeometry);

        initializeReflectionView(viewPort, rootNode);
        ibocean.update(Instant.now(), assetManager, reflectionMap, envCam.getViewProjectionMatrix(), WIND);
    }

    @Override
    public void updateMainViewPort(ViewPort vp, float tpf) {
        float windStrength = 60f;
        this.WIND.set(0, 0, windStrength);

        Camera cam = vp.getCamera();
        updateReflectionCam(cam, envCam, plane);

        Vector3f t = oceanGeometry.getLocalTranslation();
        t.x = cam.getLocation().getX();
        t.z = cam.getLocation().getZ();
        t.y = 0;

        oceanGeometry.setLocalTranslation(t);
        ibocean.update(Instant.now(), assetManager, reflectionMap, envCam.getViewProjectionMatrix(), WIND);
    }

    @Override
    public void onDisable(ComponentManager fragmentManager, Runner runner, DataStoreProvider dataStoreProvider) {
        if (oceanGeometry != null) oceanGeometry.removeFromParent();
        if (reflectionViewPort != null) renderManager.removePreView(reflectionViewPort);
        if (oceanWavesSound != null) {
            oceanWavesSound.stop();
            oceanWavesSound.removeFromParent();
        }
    }

    public float getWaterHeightAt(float x, float z) {
        return ibocean.getWaterHeight(samplePos.set(x, 0, z));
    }

    public void add(Spatial spat) {
        BuoyancyControl bc = spat.getControl(BuoyancyControl.class);
        if (bc == null) {
            bc = new BuoyancyControl();
            spat.addControl(bc);
        }
        WindControl windControl = spat.getControl(WindControl.class);
        if (windControl == null) {
            windControl = new WindControl();
            spat.addControl(windControl);
        }
        windControl.setWind(WIND);
        bc.setAppState(this);

        controls.add(bc);

        spat.depthFirstTraversal(sxx -> {
            Object filterWater = sxx.getUserData("nowater");

            if (filterWater != null) {
                sxx.depthFirstTraversal(sx -> {
                    if (sx instanceof Geometry) {
                        Geometry geom = (Geometry) sx;
                        log.info("Enable stencil mask for  " + geom.getName());
                        Material mat = geom.getMaterial().clone();
                        mat
                            .getAdditionalRenderState()
                            .setStencil(
                                true,
                                RenderState.StencilOperation.Keep, // front triangle  fails  stencil test
                                RenderState.StencilOperation.Keep, // front triangle fails depth test
                                RenderState.StencilOperation.Replace, // front triangle passes depth test
                                RenderState.StencilOperation.Keep, // back triangle fails stencil test
                                RenderState.StencilOperation.Keep, // back triangle fails depth test
                                RenderState.StencilOperation.Replace, // back triangle passes depth test
                                RenderState.TestFunction.Always, // front triangle stencil test function
                                RenderState.TestFunction.Always
                            ); // back triangle stencil test function
                        mat.getAdditionalRenderState().setFrontStencilReference(1);
                        mat.getAdditionalRenderState().setBackStencilReference(1);
                        mat.getAdditionalRenderState().setFrontStencilMask(0xFF);
                        mat.getAdditionalRenderState().setBackStencilMask(0xFF);
                        geom.setMaterial(mat);
                    }
                });
            }
        });
        spat.depthFirstTraversal(sxx -> {
            if (sxx instanceof Geometry) {
                Geometry geom = (Geometry) sxx;
                Material mat = geom.getMaterial();
                mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
            }
        });
    }

    public void remove(Spatial spat) {
        BuoyancyControl bc = spat.getControl(BuoyancyControl.class);
        if (bc != null) {
            controls.remove(bc);
            bc.setAppState(null);
        }
    }

    private void updateReflectionCam(Camera sceneCam, Camera reflectionCam, Plane plane) {
        TempVars vars = TempVars.get();
        try {
            // Get distance to water for near plane adjustment
            Float waterHeight = null;
            for (BuoyancyControl control : controls) {
                float g = control.getWaterHeight();
                if (waterHeight == null || g < waterHeight) {
                    waterHeight = g;
                }
            }
            if (waterHeight == null) {
                waterHeight = 0f;
            }
            waterHeight -= 1f;
            plane.setConstant(waterHeight);

            // Calculate view angle relative to water for FOV adjustment
            float dotProduct = sceneCam.getDirection().dot(Vector3f.UNIT_Y);
            float angleToWater = FastMath.acos(FastMath.abs(dotProduct));
            boolean isShallowAngle = angleToWater > FastMath.HALF_PI * 0.7f; // ~63 degrees from horizontal

            Vector3f sceneTarget = vars.vect1;
            Vector3f reflectDirection = vars.vect2;
            Vector3f reflectUp = vars.vect3;
            Vector3f reflectLeft = vars.vect4;
            Vector3f camLoc = vars.vect5;
            camLoc = plane.reflect(sceneCam.getLocation(), camLoc);
            reflectionCam.setLocation(camLoc);

            float fixedFOV = 60f; // Fixed FOV in degrees

            // Increase FOV at shallow angles to capture more reflections
            if (isShallowAngle) {
                // Add up to 40 degrees more FOV based on view angle
                float shallowAngleBoost = (angleToWater - (FastMath.HALF_PI * 0.7f)) / (FastMath.HALF_PI * 0.3f);
                shallowAngleBoost = FastMath.clamp(shallowAngleBoost, 0f, 1f) * 40f;
                fixedFOV += shallowAngleBoost;
            }

            // Apply the FOV with perspective projection
            float aspect = sceneCam.getWidth() / (float) sceneCam.getHeight();
            reflectionCam.setFrustumPerspective(
                fixedFOV,
                aspect,
                0.1f, // Smaller near plane helps with close
                // reflections
                sceneCam.getFrustumFar()
            );

            reflectionCam.setParallelProjection(sceneCam.isParallelProjection());

            sceneTarget.set(sceneCam.getLocation()).addLocal(sceneCam.getDirection(vars.vect6));
            reflectDirection = plane.reflect(sceneTarget, reflectDirection);
            reflectDirection.subtractLocal(camLoc);

            sceneTarget.set(sceneCam.getLocation()).subtractLocal(sceneCam.getUp(vars.vect6));
            reflectUp = plane.reflect(sceneTarget, reflectUp);
            reflectUp.subtractLocal(camLoc);

            sceneTarget.set(sceneCam.getLocation()).addLocal(sceneCam.getLeft(vars.vect6));
            reflectLeft = plane.reflect(sceneTarget, reflectLeft);
            reflectLeft.subtractLocal(camLoc);

            reflectionCam.setAxes(reflectLeft, reflectUp, reflectDirection);
            // Add clip plane with small offset to prevent z-fighting
            // float clipOffset = isShallowAngle ? 0.02f : 0.05f;
            // Plane clipPlane = new Plane(plane.getNormal(), plane.getConstant() + clipOffset);
            // reflectionCam.setClipPlane(clipPlane, Plane.Side.Negative);
        } finally {
            vars.release();
        }
    }

    @Override
    public void updateRender(RenderManager renderer) {}

    @Override
    public void loadMainViewPortFilterPostprocessor(AssetManager assetManager, FilterPostProcessor fpp) {}

    @Override
    public void receiveMainViewPortFilterPostProcessor(FilterPostProcessor fpp) {}
}
