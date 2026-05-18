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
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentManagerProvider;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.scene.Node;
import com.jme3.texture.Texture2D;
import com.jme3.util.WeakCollection;

import io.github.jmecn.tiled.components.TiledGuiUpdater.GuiFragmentContext;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.renderer.MapRenderer;
import io.github.jmecn.tiled.util.CoordinateSystem;
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
    private final MapRenderer renderer;
    private final Node mapNode;
    private final Node overlayNode;
    private final Node worldGuiNode;
    protected MapRenderer.Listener listener;
    protected ContactListener contactListener;
    protected Collection<PovRenderer> povRenderers = new WeakCollection<>();
    protected Collection<PovRenderer> povRenderersRO = Collections.unmodifiableCollection(povRenderers);
    private final Deque<Runnable> postPhysicsStepQueue = new ArrayDeque<>();
    private boolean physicsStepInProgress;
    
    TiledWorld2d(String name, TiledMap map, World physics, int ppm, MapRenderer renderer, Node rootNode, Node overlayNode, Node worldGuiNode) {
        this.name = name;
        this.map = map;
        this.physics = physics;
        this.ppm = ppm;
        this.renderer = renderer;
        this.mapNode = rootNode;
        this.overlayNode = overlayNode;
        this.worldGuiNode = worldGuiNode;
    }

    public MapRenderer.Listener getRenderListener(){
        return listener;
    }

    public ContactListener getContactListener(){
        return contactListener;
    }

    public void registerPovRenderer(PovRenderer pov){
        if(povRenderers.contains(pov)) return;
        povRenderers.add(pov);
    }

    public void unregisterPovRenderer(PovRenderer pov){
        povRenderers.remove(pov);
    }

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

    public float getTopLayerHeight() {
        BoundingVolume bv = mapNode.getWorldBound();
        if (bv != null) {
            if (bv instanceof BoundingSphere) {
                BoundingSphere bs = (BoundingSphere) bv;
                return bs.getRadius();
            } else if (bv instanceof BoundingBox) {
                BoundingBox bb = (BoundingBox) bv;
                return bb.getZExtent(); // FIXME: seems too far away
            }
        }
        return 32f;

    }

    public String getName() {
        return name;
    }

    public TiledMap getMap() {
        return map;
    }

    public World getPhysics() {
        return physics;
    }

    public int getPpm() {
        return ppm;
    }

    public MapRenderer getRenderer() {
        return renderer;
    }

    public Node getMapNode() {
        return mapNode;
    }

    public Node getOverlayNode() {
        return overlayNode;
    }

    public Node getWorldGuiNode(){
        return worldGuiNode;
    }

    // public boolean isRenderToDefaultViewport() {
    //     return renderToDefaultViewport;
    // }

    // public void setRenderToDefaultViewport(boolean renderToDefaultViewport) {
    //     this.renderToDefaultViewport = renderToDefaultViewport;
    // }

    public CoordinateSystem getCoordinateSystem(){
        return getRenderer().getCoordinateSystem();
    }

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

    public boolean isDuringPhysicsStep() {
        return physicsStepInProgress;
    }

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
