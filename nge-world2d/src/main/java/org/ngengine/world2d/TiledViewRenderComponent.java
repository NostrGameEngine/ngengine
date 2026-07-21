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

package org.ngengine.world2d;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;
import org.ngengine.Components;
import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.world2d.box2d.TiledPhysicsComponent;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

/**
 * Default {@link PovRenderer} implementation for a tiled world view.
 * <p>
 * The component registers itself with the current {@link TiledWorld2d}, exposes
 * scene and GUI viewports, and keeps its camera framed around either the current
 * tiled entity or the whole map.
 * </p>
 */
public class TiledViewRenderComponent extends AbstractComponent implements PovRenderer, TiledEntityLogicFragment {
    private static final float DEFAULT_SMOOTHING = 3f;
    private static final float CAMERA_SETTLE_PIXELS = 0.5f;
    private ViewPort viewPort;
    private ViewPort guiViewPort;
    private TiledWorld2d registeredWorld;
    private final Vector3f targetCameraLoc = new Vector3f();
    private final Vector3f smoothCameraLoc = new Vector3f();
    private boolean cameraTargetReady;
    private float maxDistBeforeSnap = -1;
    private float smoothing = DEFAULT_SMOOTHING;
    private boolean pixelSnap;

    /**
     * Creates a component that resolves its scene and GUI viewports from the
     * owning component manager.
     */
    public TiledViewRenderComponent() {
    }

    public TiledViewRenderComponent(float smoothing) {
        this.smoothing = smoothing;
    }

    public TiledViewRenderComponent(ViewPort viewPort, ViewPort guiViewPort) {
        this(viewPort, guiViewPort, DEFAULT_SMOOTHING);
    }

    /**
     * Creates a component bound to explicit scene and GUI viewports.
     *
     * @param viewPort the scene viewport to render the world into
     * @param guiViewPort the GUI viewport to use for world GUI fragments
     * @param smoothing the camera follow smoothing factor, or a negative value to read settings
     */
    public TiledViewRenderComponent(ViewPort viewPort, ViewPort guiViewPort, float smoothing) {
        this.viewPort = viewPort;
        this.guiViewPort = guiViewPort;
        this.smoothing = smoothing;
    }

    /**
     * Enables camera quantization to the visible logical-pixel grid.
     *
     * <p>The smoothed camera position remains continuous internally; only the
     * position submitted for rendering is snapped. This avoids sub-pixel
     * shimmer without feeding quantization error back into camera following.</p>
     *
     * @param enabled true to render on the logical-pixel grid
     * @return this component
     */
    public TiledViewRenderComponent setPixelSnap(boolean enabled) {
        pixelSnap = enabled;
        return this;
    }

    /**
     * Registers this POV with the current tiled world when the component is enabled.
     */
    @Override
    public void onEnable(ComponentManager mng, boolean firstTime) {
        ensureRegistered();
    }

    /**
     * Unregisters this POV and detaches its render target when the component is disabled.
     */
    @Override
    public void onDisable(ComponentManager mng) {
        if (registeredWorld != null) {
            registeredWorld.unregisterPovRenderer(this);
            registeredWorld = null;
        }
    }

    /**
     * Creates a copy preserving the explicitly configured viewports, if any.
     *
     * @return a new render component for the same viewport pair
     */
    @Override
    public Component newInstance() {
        TiledViewRenderComponent copy = new TiledViewRenderComponent(viewPort, guiViewPort, smoothing);
        copy.pixelSnap = pixelSnap;
        return copy;
    }

    /**
     * Returns the scene viewport used for tiled world rendering.
     *
     * @return the explicit scene viewport, or the main scene viewport from
     *         {@link ViewPortManager}
     */
    @Override
    public ViewPort getSceneViewPort() {
        if (viewPort != null) {
            return viewPort;
        }
        ViewPortManager vpm = getComponentManager().getInstanceOf(ViewPortManager.class);
        return vpm != null ? vpm.getMainSceneViewPort() : null;
    }

    /**
     * Returns the GUI viewport used for world GUI fragments.
     *
     * @return the explicit GUI viewport, or the GUI viewport from
     *         {@link ViewPortManager}
     */
    @Override
    public ViewPort getGuiViewPort() {
        if (guiViewPort != null) {
            return guiViewPort;
        }
        NWindowManagerComponent winMng = getComponentManager().getInstanceOf(NWindowManagerComponent.class);
        if (winMng != null) {
            return winMng.getDefaultGuiViewPort();
        }
        ViewPortManager vpm = getComponentManager().getInstanceOf(ViewPortManager.class);
        return vpm != null ? vpm.getGuiViewPort() : null;
    }

    /**
     * Returns or creates a GUI scene node at the requested viewport scene index.
     *
     * @param i the GUI scene index
     * @return the GUI node, or {@code null} when no GUI viewport is available
     */
    @Override
    public Node getGuiNode(int i) {
        ViewPort vp = getGuiViewPort();
        if (vp == null) {
            return null;
        }
        SafeArrayList<Spatial> scenes = vp.getScenes();
        while (scenes.size() <= i) {
            Node guiNode = new Node("GuiNode" + scenes.size());
            guiNode.setQueueBucket(RenderQueue.Bucket.Gui);
            vp.attachScene(guiNode);
        }
        return (Node) scenes.get(i);
    }

    /**
     * Returns or creates a scene node at the requested viewport scene index.
     *
     * @param i the scene index
     * @return the scene node, or {@code null} when no scene viewport is available
     */
    @Override
    public Node getSceneNode(int i) {
        ViewPort vp = getSceneViewPort();
        if (vp == null) {
            return null;
        }
        SafeArrayList<Spatial> scenes = vp.getScenes();
        while (scenes.size() <= i) {
            vp.attachScene(new Node("SceneNode" + scenes.size()));
        }
        return (Node) scenes.get(i);
    }

    /**
     * Returns the currently loaded tiled world for this component manager.
     *
     * @return the current world, or {@code null} when none is available
     */
    public TiledWorld2d getCurrentMap() {
        ComponentManager mng = getComponentManager();
        TiledWorld2d map = mng.getInstanceOf(TiledWorld2d.class);
        if (map == null) {
            TiledWorld2dManagerComponent world = Components.get(mng, TiledWorld2dManagerComponent.class).get();
            if (world != null) {
                map = world.getDefaultWorld();
            }
        }
        return map;
    }

    /**
     * Updates camera framing and keeps this POV registered with the active world.
     *
     * @param mng the component manager running the update
     * @param tpf time per frame
     * @param entity the tiled entity owning this logic update
     */
    @Override
    public void onTiledEntityLogicUpdate(ComponentManager mng, float tpf, TiledBase entity) {
        TiledWorld2d world = getCurrentMap();
        if (world == null) {
            return;
        }
        ensureRegistered(world);

        if (entity instanceof TiledObjectEntity) {
            pointCameraTo((TiledObjectEntity) entity);
        } else {
            pointCameraTo(world.getRenderTarget(this).getMapNode());
        }

        ViewPort guiVp = getGuiViewPort();
        NWindowManagerComponent winMng = mng.getInstanceOf(NWindowManagerComponent.class);
        if (winMng != null && guiVp != null) {
            winMng.getManager(guiVp);
        }

        ViewPort viewPort = getSceneViewPort();
        if (viewPort != null) {
            if (maxDistBeforeSnap == -1) {
                maxDistBeforeSnap = Math.max(world.getMap().getTileWidth(), world.getMap().getTileHeight()) * 5f;               
            }
            try (TempVars vars = TempVars.get()) {
                Camera cam = viewPort.getCamera();
                Vector3f dir = vars.vect1;
                dir.set(targetCameraLoc).subtractLocal(smoothCameraLoc);
                float dist = dir.length();
                float settleDistance = cameraSettleDistance(cam);
                if (shouldSnapCamera(smoothing, cameraTargetReady, dist, maxDistBeforeSnap, settleDistance)) {
                    smoothCameraLoc.set(targetCameraLoc);
                    cameraTargetReady = true;
                } else {
                    moveCameraLocation(smoothCameraLoc, targetCameraLoc, smoothing, tpf);
                }
                Vector3f loc = vars.vect2.set(smoothCameraLoc);
                if (pixelSnap) {
                    snapCameraLocation(loc, cam);
                }
                cam.setLocation(loc);
            }
        }
    }

    private void ensureRegistered() {
        ensureRegistered(getCurrentMap());
    }

    private void ensureRegistered(TiledWorld2d world) {
        if (world == null || registeredWorld == world) {
            return;
        }
        if (registeredWorld != null) {
            registeredWorld.unregisterPovRenderer(this);
        }
        registeredWorld = world;
        registeredWorld.registerPovRenderer(this);
    }

    /**
     * Frames the camera around a spatial's world bound.
     *
     * @param sp the spatial to frame
     */
    public void pointCameraTo(Spatial sp) {
        if (sp == null) {
            return;
        }
        BoundingVolume bv = sp.getWorldBound();
        float d = 1000;
        if (bv instanceof BoundingBox) {
            BoundingBox bbx = (BoundingBox) bv;
            d = Math.max(bbx.getXExtent(), bbx.getZExtent());
        } else if (bv instanceof BoundingSphere) {
            d = ((BoundingSphere) bv).getRadius();
        }
        pointCameraTo(sp, d);
    }

    /**
     * Frames the camera around a spatial using an explicit half-distance.
     *
     * @param sp the spatial whose center should be used
     * @param halfDistance half of the desired vertical world span
     */
    public void pointCameraTo(Spatial sp, float halfDistance) {
        if (sp == null) {
            return;
        }
        BoundingVolume bv = sp.getWorldBound();
        Vector3f center = bv != null ? bv.getCenter() : sp.getWorldTranslation();
        pointCameraTo(center, halfDistance);
    }

    /**
     * Frames the camera around a world-space center.
     *
     * @param center the world-space center to look at
     * @param halfDistance half of the desired vertical world span
     */
    public void pointCameraTo(Vector3f center, float halfDistance) {
        if (center == null) {
            return;
        }
        ViewPort viewPort = getSceneViewPort();
        if (viewPort == null) {
            return;
        }
        Camera cam = viewPort.getCamera();
        float aspect = (float) cam.getWidth() / cam.getHeight();
        float halfHeight = halfDistance;
        float halfWidth = halfHeight * aspect;

        TiledWorld2d map = getCurrentMap();
        float near = map != null ? -map.getRenderTarget(this).getTopLayerHeight() : -100f;
        cam.setParallelProjection(true);
        cam.setFrustum(near, 10f, -halfWidth, halfWidth, halfHeight, -halfHeight);

        Vector3f loc = targetCameraLoc;
        loc.x = center.x;
        loc.y = 0;
        loc.z = center.z;

        try (TempVars vars = TempVars.get()) {
            Vector3f dir = vars.vect1;
            dir.set(0, -10f, 0);
            Vector3f up = vars.vect2;
            up.set(0, 0, -1f);
            cam.lookAtDirection(dir, up);
        }
    }

    /**
     * Frames the camera around a tiled object using the active coordinate system.
     *
     * @param obj the tiled object to frame
     */
    public void pointCameraTo(TiledObjectEntity obj) {
        if (obj == null) {
            return;
        }
        ComponentManager mng = getComponentManager();
        CoordinateSystem coords = mng.getInstanceOf(CoordinateSystem.class);
        if (coords == null) {
            TiledWorld2d world = getCurrentMap();
            coords = world != null ? world.getCoordinateSystem() : null;
        }
        if (coords == null) {
            return;
        }

        try (TempVars vars = TempVars.get()) {
            Vector2f c = vars.vect2d;
            TiledPhysicsComponent physics = mng.getComponent(TiledPhysicsComponent.class);
            float z;
            if (physics != null && physics.getBody() != null) {
                Vector2f objectPositionGrid = vars.vect2d2;
                Vector2f objectPositionWorld = vars.vect2d3;

                coords.getCenterInGridSpace(obj, c);
                coords.getPositionInGridSpace(obj, objectPositionGrid);
                coords.gridToWorldSpace(c.x, c.y, c);
                coords.gridToWorldSpace(objectPositionGrid.x, objectPositionGrid.y, objectPositionWorld);

                coords.physicsToWorldSpace(physics.getPhysicsWorldPosition(), objectPositionGrid);
                applyObjectCenterOffset(c, objectPositionWorld, objectPositionGrid, c);

                coords.worldToGridSpace(c.x, c.y, objectPositionGrid);
                z = coords.getTopDownYIndex(objectPositionGrid.x, objectPositionGrid.y);
            } else {
                coords.getCenterInGridSpace(obj, c);
                coords.gridToWorldSpace(c.x, c.y, c);
                z = coords.getTopDownYIndex(obj);
            }
            float d = (float) Math.max(obj.getWidth(), obj.getHeight()) * 1.2f;

            Vector3f wp = vars.vect1;
            wp.x = c.x;
            wp.y = z;
            wp.z = c.y;
            pointCameraTo(wp, d);
        }
    }

    static boolean shouldSnapCamera(float smoothing, boolean targetReady, float distance,
            float maxDistanceBeforeSnap, float settleDistance) {
        return !targetReady
                || smoothing <= 0f
                || maxDistanceBeforeSnap >= 0f && distance > maxDistanceBeforeSnap
                || distance <= settleDistance;
    }

    static float cameraSettleDistance(Camera camera) {
        if (camera == null || camera.getHeight() <= 0) {
            return 0f;
        }
        float verticalWorldSpan = Math.abs(camera.getFrustumTop() - camera.getFrustumBottom());
        return verticalWorldSpan / camera.getHeight() * CAMERA_SETTLE_PIXELS;
    }

    static void snapCameraLocation(Vector3f location, Camera camera) {
        if (location == null || camera == null || camera.getHeight() <= 0) {
            return;
        }
        float verticalWorldSpan = Math.abs(camera.getFrustumTop() - camera.getFrustumBottom());
        float worldUnitsPerPixel = verticalWorldSpan / camera.getHeight();
        if (worldUnitsPerPixel <= 0f || !Float.isFinite(worldUnitsPerPixel)) {
            return;
        }
        location.x = Math.round(location.x / worldUnitsPerPixel) * worldUnitsPerPixel;
        location.z = Math.round(location.z / worldUnitsPerPixel) * worldUnitsPerPixel;
    }

    static float cameraFollowAlpha(float smoothing, float tpf) {
        if (smoothing <= 0f) {
            return 1f;
        }
        if (tpf <= 0f) {
            return 0f;
        }
        return Math.min(1f, 1f - (float) Math.exp(-smoothing * tpf));
    }

    static void moveCameraLocation(Vector3f location, Vector3f target, float smoothing, float tpf) {
        float alpha = cameraFollowAlpha(smoothing, tpf);
        location.interpolateLocal(target, alpha);
    }

    static void applyObjectCenterOffset(
            Vector2f objectCenterWorld,
            Vector2f objectPositionWorld,
            Vector2f physicsPositionWorld,
            Vector2f out
    ) {
        out.set(objectCenterWorld)
                .subtractLocal(objectPositionWorld)
                .addLocal(physicsPositionWorld);
    }

}
