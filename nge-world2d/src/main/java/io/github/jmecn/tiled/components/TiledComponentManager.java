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

package io.github.jmecn.tiled.components;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.ngengine.components.AbstractComponentManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentMount;
import org.ngengine.components.runners.ComponentInitializer;
import org.ngengine.components.runners.ComponentUpdater;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.world2d.TiledWorld2d;

import com.jme3.renderer.RenderManager;
import com.jme3.scene.Spatial;

import io.github.jmecn.tiled.core.TiledLayer;
import io.github.jmecn.tiled.core.TiledBase;
import io.github.jmecn.tiled.core.TiledEntity;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.entity.TiledTileEntity;
import io.github.jmecn.tiled.renderer.MapRenderer;
import io.github.jmecn.tiled.util.CoordinateSystem;
import jakarta.annotation.Nullable;

public class TiledComponentManager extends AbstractComponentManager {
    private TiledMap map;
    private TiledLayer layer;
    private TiledEntity entry;
    private boolean initialized = false;
    private Spatial sp;
    private TiledWorld2d loadedMap;

    private final TiledLogicUpdater logicUpdater = new TiledLogicUpdater(() -> {
        if (entry != null) return entry;
        else if (layer != null) return layer;
        else return map;
    }, () -> {
        return sp;
    });

    private final TiledEntityLifecycleManager lifeCycleManager = new TiledEntityLifecycleManager(() -> {
        if (entry != null) return entry;
        else if (layer != null) return layer;
        else return map;
    });


    private final TiledGuiUpdater guiUpdater = new TiledGuiUpdater(() -> {
        if (entry != null) return entry;
        else if (layer != null) return layer;
        else return map;
    });

    private final List<TiledPhysicsUpdater> physicsManagers = new CopyOnWriteArrayList<>();


 

    @Override
    public <T> T getInstanceOf(Class<T> type) {

        if (type == TiledWorld2d.class) {
            return type.cast(loadedMap);
        }
        if (type == MapRenderer.class) {
            if (loadedMap != null) {
                return type.cast(loadedMap.getRenderer());
            } else {
                return null;
            }
        }
        if (type == World.class) {
            if (loadedMap != null) {
                return type.cast(loadedMap.getPhysics());
            } else {
                return null;
            }
        }
        if (type == CoordinateSystem.class) {
            if (loadedMap != null) {
                return type.cast(loadedMap.getCoordinateSystem());
            } else {
                return null;
            }
        }
        if (type == TiledBase.class) {
            if (entry != null) {
                return type.cast(entry);
            } else if (layer != null) {
                return type.cast(layer);
            } else {
                return type.cast(map);
            }
        }
        if (type == TiledMap.class) {

            return type.cast(map);
        }

        if (type == TiledLayer.class) {
            return type.cast(layer);
        }

        if (type == TiledEntity.class) {
            if (entry != null && (entry instanceof TiledEntity)) {
                return type.cast(entry);
            }
        }

        if (type == TiledObjectEntity.class) {
            if (entry != null && (entry instanceof TiledObjectEntity)) {
                return type.cast(entry);
            }
        }

        if (type == TiledTileEntity.class) {
            if (entry != null && (entry instanceof TiledTileEntity)) {
                return type.cast(entry);
            }
        }

        if (type == Spatial.class) {
            return type.cast(sp);
        }
        return super.getInstanceOf(type);
    }

    @Override
    protected void initialize(AbstractComponentManager mng, NGEAppSettings settings) {
        super.initialize(mng, settings);
        List<ComponentUpdater> updaters = mng.getUpdaters();
        if (!updaters.contains(logicUpdater)) {
            updaters.add(logicUpdater);
        }
        if (!updaters.contains(lifeCycleManager)) {
            updaters.add(lifeCycleManager);
        }
        if (!updaters.contains(guiUpdater)) {
            updaters.add(guiUpdater);
        }
        
        List<ComponentInitializer> initializers = mng.getInitializers();
        if (!initializers.contains(lifeCycleManager)) {
            initializers.add(lifeCycleManager);
        }
        if (!initializers.contains(guiUpdater)) {
            initializers.add(guiUpdater);
        }

        physicsManagers.add(new TiledPhysicsUpdater());
    }

    @Override
    protected void onUpdate(float tpf) {
        if (getParent() != null) {
            if (!initialized) {
                initialize();
                initialized = true;
            }
            setEnabled(true);

            super.onUpdate(tpf);
        }
    }

    public void update(TiledWorld2d lmap, TiledMap map, TiledLayer layer, TiledEntity entry, float tpf) {
        this.loadedMap = lmap;
        this.map = map;
        this.layer = layer;
        this.entry = entry;
        this.onUpdate(tpf);
    }

    public void render(TiledWorld2d lmap, RenderManager rm, TiledMap map, TiledLayer layer, TiledEntity entry,
            Spatial sp) {
        this.loadedMap = lmap;
        this.map = map;
        this.layer = layer;
        this.entry = entry;
        this.sp = sp;
        this.onRender(rm);
    }

    /**
     * Forces tiled lifecycle cleanup callbacks for components bound to a detached entity.
     *
     * <p>This is used when an entity is removed from its layer and therefore won't
     * receive regular update cycles that would normally detect the lifecycle transition.
     */
    public void notifyEntityDetached(TiledEntity entity) {
        if (entity == null) {
            return;
        }
        TiledEntity previousEntry = this.entry;
        this.entry = entity;
        try {
            for (ComponentMount mount : this.componentMounts) {
                lifeCycleManager.cleanup(this, mount.component);
            }
        } finally {
            this.entry = previousEntry;
        }
    }

    public void beginContact(TiledBase entityA, TiledBase entityB,
            @Nullable TiledObjectEntity colliderA, @Nullable TiledObjectEntity colliderB,
            Contact contact) {
        TiledBase thisEntity = getInstanceOf(TiledBase.class);
        if (entityA != thisEntity && entityB != thisEntity) return;
        if (thisEntity != entityA) {
            TiledBase temp = entityA;
            entityA = entityB;
            entityB = temp;

            TiledObjectEntity tempC = colliderA;
            colliderA = colliderB;
            colliderB = tempC;
        }
        for (ComponentMount mount : this.componentMounts) {
            if (!mount.enabled) continue;
            Component cmp = mount.component;

            for (TiledPhysicsUpdater physicsMng : physicsManagers) {
                physicsMng.beforeTiledPhysicsContact(this, cmp, entityA, entityB, colliderA, colliderB,
                        contact);
            }
        }
    }

    public void endContact(TiledBase entityA, TiledBase entityB, @Nullable TiledObjectEntity colliderA,
            @Nullable TiledObjectEntity colliderB, Contact contact) {
        TiledBase thisEntity = getInstanceOf(TiledBase.class);
        if (entityA != thisEntity && entityB != thisEntity) return;
        if (thisEntity != entityA) {
            TiledBase temp = entityA;
            entityA = entityB;
            entityB = temp;

            TiledObjectEntity tempC = colliderA;
            colliderA = colliderB;
            colliderB = tempC;
        }
        for (ComponentMount mount : this.componentMounts) {
            if (!mount.enabled) continue;
            Component cmp = mount.component;

            for (TiledPhysicsUpdater physicsMng : physicsManagers) {

                physicsMng.afterTiledPhysicsContact(this, cmp, entityA, entityB, colliderA, colliderB,
                        contact);
            }
        }
    }

    public void preSolve(TiledBase entityA, TiledBase entityB, @Nullable TiledObjectEntity colliderA,
            @Nullable TiledObjectEntity colliderB, Contact contact, Manifold oldManifold) {
        TiledBase thisEntity = getInstanceOf(TiledBase.class);
        if (entityA != thisEntity && entityB != thisEntity) return;
        if (thisEntity != entityA) {
            TiledBase temp = entityA;
            entityA = entityB;
            entityB = temp;

            TiledObjectEntity tempC = colliderA;
            colliderA = colliderB;
            colliderB = tempC;
        }
        for (ComponentMount mount : this.componentMounts) {
            if (!mount.enabled) continue;
            Component cmp = mount.component;

            for (TiledPhysicsUpdater physicsMng : physicsManagers) {

                physicsMng.beforeTiledPhysicsSolve(this, cmp, entityA, entityB, colliderA, colliderB, contact,
                        oldManifold);
            }
        }
    }

    public void postSolve(TiledBase entityA, TiledBase entityB, @Nullable TiledObjectEntity colliderA,
            @Nullable TiledObjectEntity colliderB, Contact contact, ContactImpulse impulse) {
        TiledBase thisEntity = getInstanceOf(TiledBase.class);
        if (entityA != thisEntity && entityB != thisEntity) return;
        if (thisEntity != entityA) {
            TiledBase temp = entityA;
            entityA = entityB;
            entityB = temp;

            TiledObjectEntity tempC = colliderA;
            colliderA = colliderB;
            colliderB = tempC;
        }
        for (ComponentMount mount : this.componentMounts) {
            if (!mount.enabled) continue;
            Component cmp = mount.component;
            for (TiledPhysicsUpdater physicsMng : physicsManagers) {

                physicsMng.afterTiledPhysicsSolve(this, cmp, entityA, entityB, colliderA, colliderB, contact,
                        impulse);
            }
        }
    }

    @Override
    protected void cleanup() {
        initialized = false;
        initializers.clear();
        updaters.clear();
        physicsManagers.clear();
        super.cleanup();
    }

}
