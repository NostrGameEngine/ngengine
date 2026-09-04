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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.box2d4j.b2ContactBeginTouchEvent;
import org.box2d4j.b2ContactEndTouchEvent;
import org.box2d4j.b2ContactEvents;
import org.box2d4j.b2ShapeId;
import org.box2d4j.b2Vec2;
import org.box2d4j.b2WorldDef;
import org.box2d4j.b2WorldId;
import org.ngengine.AsyncAssetManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.RenderFragment;
import org.ngengine.platform.NGEUtils;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.store.DataStore;
import org.ngengine.network.quantization.TransformQuantizer;
import org.ngengine.world2d.box2d.Box2dUserData;
import org.ngengine.world2d.debug.Box2dDebugger;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.ngengine.world2d.tiled.components.TiledComponentManager;
import org.ngengine.world2d.tiled.components.TiledComponentReflectionMounting;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledImageLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.renderer.MapRenderer;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMaterialFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMeshFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultSpriteFactory;
import org.ngengine.world2d.tiled.renderer.factory.MaterialFactory;
import org.ngengine.world2d.tiled.renderer.factory.SpriteFactory;
import jakarta.annotation.Nullable;

import static org.box2d4j.B2.b2CreateWorld;
import static org.box2d4j.B2.b2DefaultWorldDef;
import static org.box2d4j.B2.b2DestroyWorld;
import static org.box2d4j.B2.b2Shape_GetUserData;
import static org.box2d4j.B2.b2Shape_IsValid;
import static org.box2d4j.B2.b2World_IsValid;
import static org.box2d4j.B2.b2World_GetContactEvents;
import static org.box2d4j.B2.b2World_Step;

/**
 * Component that manages one or more Tiled worlds
 */
public class TiledWorld2dManagerComponent extends AbstractComponent
        implements RenderFragment, LogicFragment, AsyncAssetLoadingFragment {
    private static Logger logger = Logger.getLogger(TiledWorld2dManagerComponent.class.getName());
    public static final String PHYSICS_DEBUG_SETTING = "PhysicsDebug";
    private static final float SNAPSHOT_MAX_POSITION_ERROR = 0.02f;
    private static final MapRenderer.Listener EMPTY_RENDER_LISTENER = new MapRenderer.Listener() {
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
    };
 
    private LinkedHashMap<String, TiledWorld2d> loadedMaps = new LinkedHashMap<>();
    private Map<String, TiledWorld2d> loadedMapsRO = Collections.unmodifiableMap(loadedMaps);
    private final Map<String, TransformQuantizer> transformQuantizers = new LinkedHashMap<>();
    private final ArrayList<TiledWorld2dRenderTarget> activeRenderTargets = new ArrayList<>();
    private final ArrayDeque<Runnable> postWorldUpdateQueue = new ArrayDeque<>();
    private final Set<String> pendingWorldUnloads = new HashSet<>();
    private final Set<TiledObjectEntity> updatedObjects = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean worldUpdateInProgress;
    private List<Consumer<TiledWorld2d>> worldLoadListener = new ArrayList<>();

    private String defaultMapName;

    public Map<String, TiledWorld2d> getLoadedMaps() {
        return loadedMapsRO;
    }

    public void addWorldLoadListener(Consumer<TiledWorld2d> listener) {
        worldLoadListener.add(listener);
    }
    public void removeWorldLoadListener(Consumer<TiledWorld2d> listener) {
        worldLoadListener.remove(listener);
    }

    public TiledWorld2dManagerComponent onWorldLoad(Consumer<TiledWorld2d> listener) {
        addWorldLoadListener(listener);
        return this;
    }

    public TiledWorld2d getDefaultWorld() {
        TiledWorld2d map = loadedMaps.get(defaultMapName);
        if (map == null && loadedMaps.size() > 0) {
            map = loadedMaps.values().iterator().next();
        }
        return map;
    }

    public TiledWorld2d getWorld(String name) {
        return loadedMaps.get(name);
    }

    public @Nullable TransformQuantizer getTransformQuantizer(@Nullable TiledMap map) {
        if (map == null) {
            return null;
        }
        for (Map.Entry<String, TiledWorld2d> entry : loadedMaps.entrySet()) {
            if (entry.getValue().getMap() == map) {
                String worldName = entry.getKey();
                return transformQuantizers.computeIfAbsent(worldName, ignored -> buildTransformQuantizer(map));
            }
        }
        String key = map.getName() != null && !map.getName().isEmpty()
            ? map.getName()
            : "map@" + Integer.toHexString(System.identityHashCode(map));
        return transformQuantizers.computeIfAbsent(key, ignored -> buildTransformQuantizer(map));
    }

    private BiFunction<ComponentManager, TiledMap, SpriteFactory> spriteFactorySupplier = (mng, map) -> {
        AssetManager assetManager = mng.getInstanceOf(AssetManager.class);
        MaterialFactory materialFactory = new DefaultMaterialFactory(assetManager);

        SpriteFactory spriteFactory = new DefaultSpriteFactory();
        spriteFactory.setMaterialFactory(materialFactory);
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));

        return spriteFactory;
    };

    public TiledWorld2dManagerComponent(@Nullable String defaultMap,
            @Nullable BiFunction<ComponentManager, TiledMap, SpriteFactory> spriteFactorySupplier) {
        if (spriteFactorySupplier != null) this.spriteFactorySupplier = spriteFactorySupplier;
        if (defaultMap != null) setDefaultWorld(defaultMap);
        
    }

    public void setDefaultWorld(String mapPath) {
        String mapNameAndPath[] = getMapNameAndPath(mapPath);
        this.defaultMapName = mapNameAndPath[0];
    }

    private String mapNameAndPath[] = new String[2];

    private String[] getMapNameAndPath(String mapPath){
        String mapName = mapPath;
        if (mapName.endsWith(".tmx")) {
            mapName = mapName.substring(0, mapName.length() - 4);
        } else {
            mapPath = mapPath + ".tmx";
        }
        mapNameAndPath[0] = mapName;
        mapNameAndPath[1] = mapPath;
        return mapNameAndPath;

    }

    @Override
    public void loadAssetsAsync(ComponentManager mng, AsyncAssetManager assetManager, DataStore assetCache,
            Consumer<Object> preload) {
        if (defaultMapName != null) loadWorld(defaultMapName);
    }

    public TiledWorld2dManagerComponent(@Nullable String mapPath) {
        this(mapPath, null);
    }

    public TiledWorld2d loadWorld(String name, TiledMap map) {

        String mapNameAndPath[] = getMapNameAndPath(name);
        name = mapNameAndPath[0];

        int ppm = NGEUtils.safeInt(map.getPropertyOrDefault("ppm", "32"));
        if (ppm < 0) ppm = 32;

        SpriteFactory spriteFactory = spriteFactorySupplier.apply(getComponentManager(), map);
        b2WorldDef worldDef = b2DefaultWorldDef();
        worldDef.gravity = new b2Vec2(0f, 0f);
        b2WorldId phy = b2CreateWorld(worldDef);
        TiledWorld2d l = new TiledWorld2d(name, map, phy, ppm, spriteFactory);
        TiledWorld2dManagerComponent world = this;
        l.listener = new MapRenderer.Listener() {

            @Override
            public void beforeMapRender(float tpf, TiledMap map) {
            }

            @Override
            public void afterMapRender(float tpf, TiledMap map, Spatial visual) {
                world.afterMapRender(tpf, l, visual);
            }

            @Override
            public void beforeEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry) {
            }

            @Override
            public void afterEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry,
                    Spatial visual) {
                world.afterEntityRender(tpf, l, layer, entry, visual);
            }

            @Override
            public void beforeLayerRender(float tpf, TiledMap map, TiledLayer layer) {
            }

            @Override
            public void afterLayerRender(float tpf, TiledMap map, TiledLayer layer, Spatial visual) {
                world.afterLayerRender(tpf, l, layer, visual);
            }

            // @Override
            // public void onEntityCleanup(float tpf, TiledEntity tile) {
            //     world.onEntityCleanup(tpf, l, tile);
            // }

        };
        loadedMaps.put(name, l);
        transformQuantizers.put(name, buildTransformQuantizer(map));
        for(Consumer<TiledWorld2d> listener : worldLoadListener){
            listener.accept(l);
        }
        return l;
    }

    public void unloadWorld(String name) {
        String mapNameAndPath[] = getMapNameAndPath(name);
        name = mapNameAndPath[0];
        String mapPath = mapNameAndPath[1];

        if (worldUpdateInProgress) {
            String queuedName = name;
            if (pendingWorldUnloads.add(queuedName)) {
                runAfterWorldUpdate(() -> {
                    pendingWorldUnloads.remove(queuedName);
                    unloadWorld(queuedName);
                });
            }
            return;
        }

        TiledWorld2d map = loadedMaps.remove(name);
        if (map != null) {
            transformQuantizers.remove(name);
            map.clearPhysicsStepState();
            disableWorldComponentManagers(map.getMap());
            map.detachRenderTargets();
            if (map.getPhysics() != null && b2World_IsValid(map.getPhysics())) {
                b2DestroyWorld(map.getPhysics());
            }
        }
        AsyncAssetManager assetManager = getInstanceOf(AsyncAssetManager.class);
        if (assetManager != null) {
            assetManager.deleteFromCache(new AssetKey<TiledMap>(mapPath));
        }
    }

    public void unloadWorld(TiledWorld2d l) {
        unloadWorld(l.getName());
    }

    /**
     * Fully tears down every loaded world and cancels any pending asynchronous
     * world load. A later load therefore creates a fresh map graph instead of
     * resuming components and mutable entities from the previous session.
     */
    public void unloadAllWorlds() {
        worldLoadGeneration++;
        loadingMaps.clear();
        ArrayList<TiledWorld2d> mapsToUnload = new ArrayList<>(loadedMaps.values());
        for (TiledWorld2d world : mapsToUnload) {
            unloadWorld(world);
        }
        transformQuantizers.clear();
    }

    static void disableWorldComponentManagers(TiledMap map) {
        if (map == null) {
            return;
        }
        for (TiledLayer layer : map.getLayersFlat()) {
            if (layer instanceof TiledObjectLayer objectLayer) {
                for (TiledObjectEntity entity : objectLayer.getObjects()) {
                    entity.getComponentManager().setEnabled(false);
                }
            } else if (layer instanceof TiledTileLayer tileLayer) {
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    for (int y = 0; y < tileLayer.getHeight(); y++) {
                        TiledTileEntity entity = tileLayer.getTileAt(x, y);
                        if (entity != null) {
                            entity.getComponentManager().setEnabled(false);
                        }
                    }
                }
            }
            layer.getComponentManager().setEnabled(false);
        }
        map.getComponentManager().setEnabled(false);
    }

    public TiledWorld2d loadWorld( String mapPath) {
        String mapNameAndPath[] = getMapNameAndPath(mapPath);
        String mapName = mapNameAndPath[0];
        mapPath = mapNameAndPath[1];

        TiledWorld2d loadedMap = loadedMaps.get(mapName);
        if (loadedMap != null) {
            unloadWorld(loadedMap);
        }

        AsyncAssetManager assetManager = getInstanceOf(AsyncAssetManager.class);
        TiledMap map = (TiledMap) assetManager.loadAsset(mapPath);

        return loadWorld(mapName, map);
    }

    // public void loadWorldAsync(  String mapPath, Consumer<TiledWorld2d> onLoaded) {
    //     String mapNameAndPath[] = getMapNameAndPath(mapPath);
    //     String mapName = mapNameAndPath[0];
    //     mapPath = mapNameAndPath[1];

    //     TiledWorld2d loadedMap = loadedMaps.get(mapName);
    //     if (loadedMap != null) {
    //         unloadWorld(loadedMap);
    //     }
    //     AsyncAssetManager assetManager = mng.getInstanceOf(AsyncAssetManager.class);
    //     assetManager.loadAssetAsync(mapPath, (Object mapObj, Throwable exc) -> {
    //         TiledMap map = (TiledMap) mapObj;
    //         onLoaded.accept(loadWorld(mapName, map));
    //     });
    // }

    // public TiledWorld2d loadWorldIfNeeded(  String mapPath) {
        
    //     String mapNameAndPath[] = getMapNameAndPath(mapPath);
    //     String mapName = mapNameAndPath[0];
    //     mapPath = mapNameAndPath[1];
    //     TiledWorld2d loadedMap = loadedMaps.get(mapName);
    //     if (loadedMap != null) {
    //         return loadedMap;
    //     }

    //     AsyncAssetManager assetManager = mng.getInstanceOf(AsyncAssetManager.class);
    //     TiledMap map = (TiledMap) assetManager.loadAsset(mapPath);

    //     return loadWorld(mapName, map);
    // }

    private List<String> loadingMaps = new ArrayList<>();
    private long worldLoadGeneration;

    public void loadWorldIfNeededAsync(String path) {
        String mapNameAndPath[] = getMapNameAndPath(path);
        String mapName = mapNameAndPath[0];
        String mapPath = mapNameAndPath[1];

        TiledWorld2d loadedMap = loadedMaps.get(mapName);
        if (loadedMap != null) {
            return;        
        }
        if(loadingMaps.contains(mapPath)){
            // already loading
            return;
        }
        loadingMaps.add(mapPath);
        long loadGeneration = worldLoadGeneration;
        AsyncAssetManager assetManager = getInstanceOf(AsyncAssetManager.class);
        assetManager.loadAssetAsync(mapPath, (Object mapObj, Throwable exc) -> {
            loadingMaps.remove(mapPath);
            if(exc==null && loadGeneration == worldLoadGeneration){
                loadWorld(mapName, (TiledMap) mapObj);
            }
        });
    }

    @Override
    public void onEnable(ComponentManager mng,
            boolean firstTime) {
    
    }

    @Override
    public void onDisable(ComponentManager mng) {
        unloadAllWorlds();
    }

    private TransformQuantizer buildTransformQuantizer(TiledMap map) {
        int tileWidth = Math.max(1, map.getTileWidth());
        int tileHeight = Math.max(1, map.getTileHeight());
        int mapWidthTiles = Math.max(1, map.getWidth());
        int mapHeightTiles = Math.max(1, map.getHeight());
        float marginX = tileWidth * 2f;
        float marginY = tileHeight * 2f;
        float mapWidth = mapWidthTiles * tileWidth;
        float mapHeight = mapHeightTiles * tileHeight;
        return new TransformQuantizer(
            new com.jme3.math.Vector3f(-marginX, -8f, -marginY),
            new com.jme3.math.Vector3f(Math.max(1f, mapWidth + marginX * 2f), 16f, Math.max(1f, mapHeight + marginY * 2f)),
            SNAPSHOT_MAX_POSITION_ERROR
        );
    }

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        beginWorldUpdate();
        updatedObjects.clear();
        try {
        Collection<TiledWorld2d> worlds = loadedMaps.values();
        RenderManager renderManager = getInstanceOf(RenderManager.class);

        // run logic update
        for (TiledWorld2d world : worlds) {
            TiledMap tiledMap = world.getMap();
            onMapUpdate(tpf, world);
            for (TiledLayer layer: tiledMap.getLayers()) {
                onLayerUpdate(tpf, world, layer);
                if (layer instanceof TiledImageLayer) {
                    TiledImageLayer il = (TiledImageLayer) layer;
                    // TODO
                } else if (layer instanceof TiledTileLayer) {
                    TiledTileLayer tl = (TiledTileLayer) layer;
                    int w = tl.getWidth();
                    int h = tl.getHeight();
                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            TiledTileEntity tile = tl.getTileAt(x, y);
                            if (tile != null) {
                                onEntityUpdate(tpf, world, layer, tile);
                                if (tl.getTileAt(x, y) == tile) {
                                    tile.updateTileAnimation(tpf);
                                }
                            }
                        }
                    }

                } else if (layer instanceof TiledObjectLayer) {
                    TiledObjectLayer og = (TiledObjectLayer) layer;
                    updateObjectLayer(tpf, world, og);
                    
                }
            }
        }

        for (TiledWorld2d map : worlds) {

            b2WorldId physics = map.getPhysics();
            if (physics != null && !b2World_IsValid(physics)) {
                logger.warning("Unloading Tiled world with an invalid Box2D world handle: " + map.getName());
                unloadWorld(map);
                continue;
            }
            if (physics != null) {
                map.beginPhysicsStep();
                try {
                    b2World_Step(physics, tpf, 8);
                    dispatchContactEvents(map);
                } finally {
                    map.endPhysicsStep();
                }
            }

            boolean notifyComponents = true;
            activeRenderTargets.clear();
            map.collectActiveRenderTargets(activeRenderTargets);
            for (TiledWorld2dRenderTarget target : activeRenderTargets) {
                target.getRenderer().setRendererCapabilities(renderManager.getRenderer().getCaps());
                target.syncViewPorts();
                // Component render callbacks are world-level side effects. Run
                // them once; TiledGuiUpdater fans GUI fragments out to every
                // registered POV during that first callback pass.
                target.render(notifyComponents ? map.getRenderListener() : EMPTY_RENDER_LISTENER, tpf);
                notifyComponents = false;
            }
            activeRenderTargets.clear();

            // map.getMapNode().updateLogicalState(tpf);
            // map.getMapNode().updateGeometricState();
            
            // map.getOverlayNode().updateLogicalState(tpf);
            // map.getOverlayNode().updateGeometricState();
           
        }


        if(isDebugEnabled()){
            MainThreadRunner mainRunner = getInstanceOf(MainThreadRunner.class);
            AssetManager assetManager = getInstanceOf(AssetManager.class);
            Box2dDebugger.update(mainRunner, assetManager, worlds, tpf, getSettings());
        }
        } finally {
            endWorldUpdate();
        }
    }

    void runAfterWorldUpdate(Runnable operation) {
        if (operation == null) {
            return;
        }
        if (!worldUpdateInProgress) {
            operation.run();
            return;
        }
        postWorldUpdateQueue.addLast(operation);
    }

    void beginWorldUpdate() {
        worldUpdateInProgress = true;
    }

    void endWorldUpdate() {
        worldUpdateInProgress = false;
        flushPostWorldUpdateQueue();
    }

    private void flushPostWorldUpdateQueue() {
        while (!postWorldUpdateQueue.isEmpty()) {
            Runnable operation = postWorldUpdateQueue.pollFirst();
            try {
                operation.run();
            } catch (RuntimeException exception) {
                logger.log(Level.SEVERE, "Post-world-update operation failed", exception);
            }
        }
    }
 

    @Override
    public Component newInstance() {
        return new TiledWorld2dManagerComponent(defaultMapName, spriteFactorySupplier);
    }

    private final TiledEntity[] contactEntities = new TiledEntity[4];

    private void dispatchContactEvents(TiledWorld2d world) {
        b2ContactEvents events = b2World_GetContactEvents(world.getPhysics());
        for (int i = 0; i < events.beginCount; i++) {
            b2ContactBeginTouchEvent event = events.beginEvents[i];
            if (resolveContactEntities(event.shapeIdA, event.shapeIdB)) {
                dispatchBeginContact(event);
            }
            clearContactEntities();
        }
        for (int i = 0; i < events.endCount; i++) {
            b2ContactEndTouchEvent event = events.endEvents[i];
            if (resolveContactEntities(event.shapeIdA, event.shapeIdB)) {
                dispatchEndContact(event);
            }
            clearContactEntities();
        }
    }

    private boolean resolveContactEntities(b2ShapeId shapeA, b2ShapeId shapeB) {
        resolveContactEntity(shapeA, 0, 2);
        resolveContactEntity(shapeB, 1, 3);
        return contactEntities[0] != null && contactEntities[1] != null;
    }

    private void resolveContactEntity(b2ShapeId shapeId, int entityIndex, int collisionIndex) {
        Object userData = getShapeUserDataIfValid(shapeId);
        if (userData instanceof Box2dUserData) {
            Box2dUserData data = (Box2dUserData) userData;
            contactEntities[entityIndex] = data.getEntity();
            contactEntities[collisionIndex] = data.getCollision();
        }
    }

    static Object getShapeUserDataIfValid(b2ShapeId shapeId) {
        // Box2D can report an end-contact event for a shape that was destroyed
        // during the same step. Its ID is useful as an event token but must not
        // be dereferenced after destruction.
        if (shapeId == null || !b2Shape_IsValid(shapeId)) {
            return null;
        }
        return b2Shape_GetUserData(shapeId);
    }

    private void dispatchBeginContact(b2ContactBeginTouchEvent event) {
        for (int i = 0; i < 2; i++) {
            TiledComponentManager manager = contactEntities[i].getComponentManager();
            if (manager != null) {
                manager.beginContact(contactEntities[0], contactEntities[1],
                        (TiledObjectEntity) contactEntities[2],
                        (TiledObjectEntity) contactEntities[3], event);
            }
        }
    }

    private void dispatchEndContact(b2ContactEndTouchEvent event) {
        for (int i = 0; i < 2; i++) {
            TiledComponentManager manager = contactEntities[i].getComponentManager();
            if (manager != null) {
                manager.endContact(contactEntities[0], contactEntities[1],
                        (TiledObjectEntity) contactEntities[2],
                        (TiledObjectEntity) contactEntities[3], event);
            }
        }
    }

    private void clearContactEntities() {
        for (int i = 0; i < contactEntities.length; i++) {
            contactEntities[i] = null;
        }
    }

    private void updateParent(TiledMap map, TiledLayer layer, TiledEntity entity) {
        TiledComponentManager mapCm = map!=null ? map.getComponentManager() : null;
        TiledComponentManager layerCm = layer!=null ? layer.getComponentManager() : null;
        TiledComponentManager entryCm = entity!=null ? entity.getComponentManager() : null;
        
        if(mapCm!=null){
            mapCm.setParent(this.getComponentManager());
        }

        if(layerCm!=null){
            if(mapCm!=null){
                layerCm.setParent(mapCm);
            } else {
                layerCm.setParent(this.getComponentManager());
            }
        }

        if (entryCm!=null){
            if(layerCm!=null){
                entryCm.setParent(layerCm);
            } else if(mapCm!=null){
                entryCm.setParent(mapCm);
            } else {
                entryCm.setParent(this.getComponentManager());
            }
        }
        
    }

    protected void onMapUpdate(float tpf, TiledWorld2d lmap) {
        TiledMap map = lmap.getMap();

        TiledComponentManager cm = map.getComponentManager();
        if (cm != null) {
            // cm.setParent(getParentManager(null, null));
            updateParent(map, null, null);
            cm.update(lmap, map, null, null, tpf);

        }
    }

    protected void onEntityUpdate(float tpf, TiledWorld2d lmap, TiledLayer layer, TiledEntity entity) {
        TiledComponentManager cm = entity.getComponentManager();
        if (cm != null) {
            TiledMap map = lmap.getMap();
            // cm.setParent( getParentManager(map, layer));
            updateParent(map, layer, entity);
            cm.update(lmap, map, layer, entity, tpf);
        }
    }

    void updateObjectLayer(float tpf, TiledWorld2d world, TiledObjectLayer layer) {
        for (TiledObjectEntity object : layer.getObjects()) {
            // SafeArrayList iterators intentionally retain a stable snapshot. An
            // entity removed by an earlier component update can therefore still
            // occur later in this traversal. Do not update (and consequently
            // re-enable) a component manager after its entity was detached or
            // moved to another layer during the same frame.
            if (object.getObjectGroup() != layer || !updatedObjects.add(object)) {
                continue;
            }
            onEntityUpdate(tpf, world, layer, object);
            if (object.getObjectGroup() == layer) {
                object.updateTileAnimation(tpf);
            }
        }
    }

    protected void onLayerUpdate(float tpf, TiledWorld2d lmap, TiledLayer layer) {
        TiledComponentManager cm = layer.getComponentManager();
        if (cm != null) {
            TiledMap map = lmap.getMap();
            // cm.setParent( getParentManager(map, null));
            // updateParent(map, layer);
            updateParent(map, layer, null);
            cm.update(lmap, map, layer, null, tpf);
        }
    }

    protected void afterMapRender(float tpf, TiledWorld2d lmap, Spatial visual) {
        TiledMap map = lmap.getMap();
        TiledComponentManager cm = map.getComponentManager();
        RenderManager rm = getInstanceOf(RenderManager.class);
        if (cm != null && rm != null) {
            // cm.setParent(getParentManager(null,  null));
            updateParent(map, null, null);
            cm.render(lmap, rm, map, null, null, visual);
        }

    }

    protected void afterEntityRender(float tpf, TiledWorld2d lmap, TiledLayer layer, TiledEntity entity,
            Spatial visual) {
        TiledComponentManager cm = entity.getComponentManager();
        RenderManager rm = getInstanceOf(RenderManager.class);
        if (cm != null && rm != null) {
            TiledMap map = lmap.getMap();
            // cm.setParent( getParentManager(map, layer));
            updateParent(map, layer, entity);
            cm.render(lmap, rm, map, layer, entity, visual);
        }
    }

    protected void afterLayerRender(float tpf, TiledWorld2d lmap, TiledLayer layer, Spatial visual) {
        TiledComponentManager cm = layer.getComponentManager();
        RenderManager rm = getInstanceOf(RenderManager.class);
        if (cm != null && rm != null) {
            TiledMap map = lmap.getMap();
            // cm.setParent( getParentManager(map, null));
            updateParent(map, layer, null);
            cm.render(lmap, rm, map, layer, null, visual);
        }
    }

    // protected void onEntityCleanup(float tpf, TiledWorld2d lmap, TiledEntity entity) {
    //     if (entity instanceof ComponentManagerProvider) {
    //         ComponentManagerProvider cmp = (ComponentManagerProvider) entity;
    //         ComponentManager cm = cmp.getComponentManager();

    //         if (cm instanceof TiledComponentManager) {
    //             TiledComponentManager tcm = (TiledComponentManager) cm;
    //             tcm.setParent(this.mng);
    //             tcm.cleanup();
    //         }
    //     }

    // }

    @Override
    public void updateRender(ComponentManager mng, RenderManager renderer) {

    }

    public boolean isDebugEnabled(){
        return getSettings().getBoolean(PHYSICS_DEBUG_SETTING, false);
    }

}
