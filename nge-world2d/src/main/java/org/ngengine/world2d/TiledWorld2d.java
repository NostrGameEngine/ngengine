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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentManagerProvider;

import com.jme3.scene.Node;
import com.jme3.util.WeakCollection;

import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.renderer.MapRenderer;
import org.ngengine.world2d.tiled.renderer.factory.SpriteFactory;
import org.ngengine.world2d.tiled.util.CoordinateSystem;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;
import jakarta.annotation.Nullable;

/**
 * A tiled world instance, contains everything needed to render the world, its 
 * scenegraph, physics and tiled map data.
 */
public class TiledWorld2d implements ComponentManagerProvider {
    private static final Logger logger = Logger.getLogger(TiledWorld2d.class.getName());
    private final String name;
    private final TiledMap map;
    private final World physics;
    private final int ppm;
    private final SpriteFactory spriteFactory;
    private final TiledCoordinateSystem coordinateSystem;
    private final Map<PovRenderer, TiledWorld2dRenderTarget> renderTargets = new WeakHashMap<>();
    protected MapRenderer.Listener listener;
    protected ContactListener contactListener;
    protected Collection<PovRenderer> povRenderers = new WeakCollection<>();
    protected Collection<PovRenderer> povRenderersRO = Collections.unmodifiableCollection(povRenderers);
    private final Deque<Runnable> postPhysicsStepQueue = new ArrayDeque<>();
    private boolean physicsStepInProgress;
    
    TiledWorld2d(String name, TiledMap map, World physics, int ppm, SpriteFactory spriteFactory) {
        this.name = name;
        this.map = map;
        this.physics = physics;
        this.ppm = ppm;
        this.spriteFactory = spriteFactory;
        this.coordinateSystem = TiledCoordinateSystem.create(map, ppm);
    }

    /**
     * Returns the listener used to dispatch render callbacks to tiled components.
     *
     * @return the map render listener, or {@code null} when none has been installed
     */
    public MapRenderer.Listener getRenderListener(){
        return listener;
    }

    /**
     * Returns the Box2D contact listener installed for this world.
     *
     * @return the contact listener, or {@code null} when none has been installed
     */
    public ContactListener getContactListener(){
        return contactListener;
    }

    /**
     * Registers a point of view that should receive an independent tiled render target.
     *
     * @param pov the point of view to render this world from
     */
    public void registerPovRenderer(PovRenderer pov){
        if (pov == null) return;
        if(!povRenderers.contains(pov)) {
            povRenderers.add(pov);
        }
        getRenderTarget(pov);
    }

    /**
     * Unregisters a point of view and detaches its map, overlay, and world GUI nodes.
     *
     * @param pov the point of view to remove
     */
    public void unregisterPovRenderer(PovRenderer pov){
        povRenderers.remove(pov);
        TiledWorld2dRenderTarget target = renderTargets.remove(pov);
        if (target != null) {
            target.detach();
        }
    }

    /**
     * Returns the currently registered points of view.
     *
     * @return an unmodifiable weak collection of registered POV renderers
     */
    public Collection<PovRenderer> getPovRenderers(){
        return povRenderersRO;
    }

    

    // public void addGuiFragment(Component c, FragmentData tex){
    //     guiFragments.put(c, tex);
    // }

    // public FragmentData removeGuiFragment(Component c){
    //     return guiFragments.remove(c);
    // }

    // public Map<Component, FragmentData> getGuiFragments(){
    //     return guiFragmentsRO;
    // }

    /**
     * @deprecated Rendered bounds are now POV-specific. Use
     *             {@link #getRenderTarget(PovRenderer)} and
     *             {@link TiledWorld2dRenderTarget#getTopLayerHeight()} instead.
     *
     * @return never returns normally
     * @throws IllegalStateException always, because a {@link PovRenderer} is required
     */
    @Deprecated
    public float getTopLayerHeight() {
        throw new IllegalStateException("A PovRenderer is required to query rendered tiled world bounds.");

    }

    /**
     * Returns the logical name this world was loaded with.
     *
     * @return the world name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Tiled map data backing this world.
     *
     * @return the tiled map
     */
    public TiledMap getMap() {
        return map;
    }

    /**
     * Returns the Box2D physics world associated with this map.
     *
     * @return the physics world
     */
    public World getPhysics() {
        return physics;
    }

    /**
     * Returns the pixels-per-meter scale used by physics conversion.
     *
     * @return the pixels-per-meter scale
     */
    public int getPpm() {
        return ppm;
    }

    /**
     * @deprecated Map renderers are now POV-specific. Use
     *             {@link #getRenderer(PovRenderer)} when rendered scene state is needed,
     *             or {@link #getCoordinateSystem()} for coordinate conversion.
     *
     * @return never returns normally
     * @throws IllegalStateException always, because a {@link PovRenderer} is required
     */
    @Deprecated
    public MapRenderer getRenderer() {
        throw new IllegalStateException("A PovRenderer is required to access a tiled world renderer.");
    }

    /**
     * Returns the map renderer for a specific point of view, creating its render target
     * if needed.
     *
     * @param pov the point of view whose renderer should be returned
     * @return the POV-specific map renderer
     */
    public MapRenderer getRenderer(PovRenderer pov) {
        return getRenderTarget(pov).getRenderer();
    }

    /**
     * Returns the render target for a point of view, creating it on first access.
     *
     * @param pov the point of view to render from
     * @return the render target associated with {@code pov}
     * @throws IllegalArgumentException when {@code pov} is {@code null}
     */
    public TiledWorld2dRenderTarget getRenderTarget(PovRenderer pov) {
        if (pov == null) {
            throw new IllegalArgumentException("A PovRenderer is required to create a tiled world render target.");
        }
        return renderTargets.computeIfAbsent(pov, key -> new TiledWorld2dRenderTarget(this, key, map, ppm, spriteFactory, coordinateSystem));
    }

    /**
     * Returns render targets for all currently registered points of view.
     *
     * @return a snapshot of active render targets
     */
    public Collection<TiledWorld2dRenderTarget> getActiveRenderTargets() {
        ArrayList<TiledWorld2dRenderTarget> targets = new ArrayList<>(renderTargets.size());
        collectActiveRenderTargets(targets);
        return targets;
    }

    void collectActiveRenderTargets(ArrayList<TiledWorld2dRenderTarget> targets) {
        Iterator<Entry<PovRenderer, TiledWorld2dRenderTarget>> it = renderTargets.entrySet().iterator();
        while (it.hasNext()) {
            TiledWorld2dRenderTarget target = it.next().getValue();
            if (target == null || !target.isPovAlive()) {
                if (target != null) {
                    target.detach();
                }
                it.remove();
            } else {
                targets.add(target);
            }
        }
    }

    /**
     * @deprecated Map nodes are now POV-specific. Use
     *             {@link TiledWorld2dRenderTarget#getMapNode()} on a render target instead.
     *
     * @return never returns normally
     * @throws IllegalStateException always, because a {@link PovRenderer} is required
     */
    @Deprecated
    public Node getMapNode() {
        throw new IllegalStateException("A PovRenderer is required to access the tiled world map node.");
    }

    /**
     * @deprecated Overlay nodes are now POV-specific. Use
     *             {@link TiledWorld2dRenderTarget#getOverlayNode()} on a render target instead.
     *
     * @return never returns normally
     * @throws IllegalStateException always, because a {@link PovRenderer} is required
     */
    @Deprecated
    public Node getOverlayNode() {
        throw new IllegalStateException("A PovRenderer is required to access the tiled world overlay node.");
    }

    /**
     * @deprecated World GUI nodes are now POV-specific. Use
     *             {@link TiledWorld2dRenderTarget#getWorldGuiNode()} on a render target instead.
     *
     * @return never returns normally
     * @throws IllegalStateException always, because a {@link PovRenderer} is required
     */
    @Deprecated
    public Node getWorldGuiNode(){
        throw new IllegalStateException("A PovRenderer is required to access the tiled world GUI node.");
    }

    /**
     * Detaches every active render target from its viewports and clears the target cache.
     */
    public void detachRenderTargets() {
        for (TiledWorld2dRenderTarget target : renderTargets.values()) {
            target.detach();
        }
        renderTargets.clear();
    }

    // public boolean isRenderToDefaultViewport() {
    //     return renderToDefaultViewport;
    // }

    // public void setRenderToDefaultViewport(boolean renderToDefaultViewport) {
    //     this.renderToDefaultViewport = renderToDefaultViewport;
    // }

    /**
     * Returns the coordinate system for this world map.
     *
     * @return the coordinate system for this tiled map
     */
    public CoordinateSystem getCoordinateSystem(){
        return coordinateSystem;
    }

    /**
     * Runs an operation immediately, or defers it until the current physics step ends.
     *
     * @param operation the operation to run after the physics world is safe to mutate
     */
    public void runAfterPhysicsStep(@Nullable Runnable operation) {
        if (operation == null) {
            return;
        }
        if (!physicsStepInProgress) {
            operation.run();
            return;
        }
        postPhysicsStepQueue.addLast(operation);
    }

    /**
     * Returns whether the Box2D world is currently inside its step callback.
     *
     * @return {@code true} while physics stepping is in progress
     */
    public boolean isDuringPhysicsStep() {
        return physicsStepInProgress;
    }

    /**
     * Safely destroys a body immediately or after the current physics step.
     *
     * @param body the body to destroy, ignored when {@code null}
     */
    public void destroyPhysics(@Nullable Body body) {
        if (body == null) {
            return;
        }
        runAfterPhysicsStep(() -> {
            World bodyWorld = body.getWorld();
            if (bodyWorld != null) {
                bodyWorld.destroyBody(body);
            }
        });
    }

    /**
     * Safely destroys a joint immediately or after the current physics step.
     *
     * @param joint the joint to destroy, ignored when {@code null}
     */
    public void destroyPhysics(@Nullable Joint joint) {
        if (joint == null) {
            return;
        }
        runAfterPhysicsStep(() -> {
            World jointWorld = joint.getBodyA() != null ? joint.getBodyA().getWorld() : null;
            if (jointWorld == null && joint.getBodyB() != null) {
                jointWorld = joint.getBodyB().getWorld();
            }
            if (jointWorld != null) {
                jointWorld.destroyJoint(joint);
            }
        });
    }

    void beginPhysicsStep() {
        physicsStepInProgress = true;
    }

    void endPhysicsStep() {
        physicsStepInProgress = false;
        flushPostPhysicsStepQueue();
    }

    void clearPhysicsStepState() {
        physicsStepInProgress = false;
        postPhysicsStepQueue.clear();
    }

    private void flushPostPhysicsStepQueue() {
        while (!postPhysicsStepQueue.isEmpty()) {
            Runnable operation = postPhysicsStepQueue.pollFirst();
            try {
                operation.run();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Post-physics operation failed", e);
            }
        }
    }

    @Override
    public ComponentManager getComponentManager() {
        return map.getComponentManager();
    }

    


}
