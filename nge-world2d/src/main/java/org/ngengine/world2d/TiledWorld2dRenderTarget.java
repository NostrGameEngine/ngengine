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

import java.lang.ref.WeakReference;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.scene.Node;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.renderer.MapRenderer;
import org.ngengine.world2d.tiled.renderer.factory.SpriteFactory;
import org.ngengine.world2d.tiled.renderer.queue.YAxisComparator;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

/**
 * Per-POV render state for a {@link TiledWorld2d}.
 * <p>
 * Each target owns its own map renderer and scene nodes, which lets one tiled
 * world render into multiple viewports without sharing cull state, overlays, or
 * GUI nodes between cameras.
 * </p>
 */
public class TiledWorld2dRenderTarget {
    private final TiledWorld2d world;
    private final WeakReference<PovRenderer> pov;
    private final MapRenderer renderer;
    private final Node mapNode;
    private final Node overlayNode;
    private final Node worldGuiNode;
    private ViewPort attachedSceneViewPort;
    private ViewPort attachedGuiViewPort;

    TiledWorld2dRenderTarget(TiledWorld2d world, PovRenderer pov, TiledMap map, int ppm, SpriteFactory spriteFactory,
            TiledCoordinateSystem coordinateSystem) {
        if (pov == null) {
            throw new IllegalArgumentException("A PovRenderer is required to create a tiled world render target.");
        }
        this.world = world;
        this.pov = new WeakReference<>(pov);
        String suffix = world.getName() + "-" + Integer.toHexString(System.identityHashCode(pov));
        this.mapNode = new Node("TiledWorld-Map" + suffix);
        this.overlayNode = new Node("TiledWorld-Overlay" + suffix);
        this.worldGuiNode = new Node("TiledWorld-wGUI" + suffix);
        this.worldGuiNode.setQueueBucket(RenderQueue.Bucket.Gui);
        this.renderer = MapRenderer.create(map, ppm, mapNode, coordinateSystem);
        this.renderer.setSpriteFactory(spriteFactory);
    }

    /**
     * Returns the logical world rendered by this target.
     *
     * @return the tiled world
     */
    public TiledWorld2d getWorld() {
        return world;
    }

    /**
     * Returns the point of view that owns this render target.
     *
     * @return the POV renderer
     */
    public PovRenderer getPovRenderer() {
        return pov.get();
    }

    /**
     * Returns the renderer used for the target's map node.
     *
     * @return the POV-specific map renderer
     */
    public MapRenderer getRenderer() {
        return renderer;
    }

    /**
     * Returns the scene node containing rendered tiled map layers.
     *
     * @return the map node
     */
    public Node getMapNode() {
        return mapNode;
    }

    /**
     * Returns the scene node intended for world-space overlays.
     *
     * @return the overlay node
     */
    public Node getOverlayNode() {
        return overlayNode;
    }

    /**
     * Returns the GUI node associated with this world target.
     *
     * @return the world GUI node
     */
    public Node getWorldGuiNode() {
        return worldGuiNode;
    }

    /**
     * Returns an approximate half-height for rendered world content along the
     * renderer's depth axis.
     *
     * @return the target's current top layer height estimate
     */
    public float getTopLayerHeight() {
        BoundingVolume bv = mapNode.getWorldBound();
        if (bv instanceof BoundingSphere) {
            return ((BoundingSphere) bv).getRadius();
        }
        if (bv instanceof BoundingBox) {
            return ((BoundingBox) bv).getZExtent();
        }
        return 32f;
    }

    void render(MapRenderer.Listener listener, float tpf) {
        PovRenderer livePov = pov.get();
        if (livePov == null) {
            return;
        }
        renderer.render(listener, tpf, livePov);
    }

    void syncViewPorts() {
        PovRenderer livePov = pov.get();
        if (livePov == null) {
            return;
        }

        ViewPort sceneViewPort = livePov.getSceneViewPort();
        if (attachedSceneViewPort != sceneViewPort) {
            detachSceneNodes();
            attachedSceneViewPort = sceneViewPort;
        }
        if (sceneViewPort != null) {
            if (!(sceneViewPort.getQueue().getGeometryComparator(RenderQueue.Bucket.Opaque) instanceof YAxisComparator)) {
                sceneViewPort.getQueue().setGeometryComparator(RenderQueue.Bucket.Opaque, new YAxisComparator());
            }
            attachSceneNode(sceneViewPort, mapNode);
            attachSceneNode(sceneViewPort, overlayNode);
        }

        ViewPort guiViewPort = livePov.getGuiViewPort();
        if (attachedGuiViewPort != guiViewPort) {
            detachGuiNode();
            attachedGuiViewPort = guiViewPort;
        }
        if (guiViewPort != null) {
            attachSceneNode(guiViewPort, worldGuiNode);
        }
    }

    boolean isPovAlive() {
        return pov.get() != null;
    }

    /**
     * Detaches all target-owned nodes from their current viewports and parents.
     */
    public void detach() {
        detachSceneNodes();
        detachGuiNode();
        mapNode.removeFromParent();
        overlayNode.removeFromParent();
        worldGuiNode.removeFromParent();
    }

    private void attachSceneNode(ViewPort viewPort, Node node) {
        if (!viewPort.getScenes().contains(node)) {
            viewPort.attachScene(node);
        }
    }

    private void detachSceneNodes() {
        if (attachedSceneViewPort != null) {
            attachedSceneViewPort.detachScene(mapNode);
            attachedSceneViewPort.detachScene(overlayNode);
            attachedSceneViewPort = null;
        }
    }

    private void detachGuiNode() {
        if (attachedGuiViewPort != null) {
            attachedGuiViewPort.detachScene(worldGuiNode);
            attachedGuiViewPort = null;
        }
    }
}
