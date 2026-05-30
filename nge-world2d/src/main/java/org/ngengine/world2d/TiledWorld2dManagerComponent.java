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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.pooling.normal.DefaultWorldPool;
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

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import io.github.jmecn.tiled.components.TiledComponentManager;
import io.github.jmecn.tiled.core.TiledLayer;
import io.github.jmecn.tiled.core.TiledObjectLayer;
import io.github.jmecn.tiled.core.TiledTileLayer;
import io.github.jmecn.tiled.core.TiledEntity;
import io.github.jmecn.tiled.core.TiledImageLayer;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.entity.TiledTileEntity;
import io.github.jmecn.tiled.renderer.MapRenderer;
import io.github.jmecn.tiled.renderer.factory.DefaultMaterialFactory;
import io.github.jmecn.tiled.renderer.factory.DefaultMeshFactory;
import io.github.jmecn.tiled.renderer.factory.DefaultSpriteFactory;
import io.github.jmecn.tiled.renderer.factory.MaterialFactory;
import io.github.jmecn.tiled.renderer.factory.SpriteFactory;
import jakarta.annotation.Nullable;

/**
 * Component that manages one or more Tiled worlds
 */
public class TiledWorld2dManagerComponent extends AbstractComponent
        implements  RenderFragment, LogicFragment, ContactListener, AsyncAssetLoadingFragment {
    private static Logger logger = Logger.getLogger(TiledWorld2dManagerComponent.class.getName());
    private static final float SNAPSHOT_MAX_POSITION_ERROR = 0.02f;
 
    private LinkedHashMap<String, TiledWorld2d> loadedMaps = new LinkedHashMap<>();
    private Map<String, TiledWorld2d> loadedMapsRO = Collections.unmodifiableMap(loadedMaps);
    private final Map<String, TransformQuantizer> transformQuantizers = new LinkedHashMap<>();
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
        Node rootNode = new Node("TiledWorld-Map" + name);
        Node overLayNode = new Node("TiledWorld-Overlay" + name);
        Node worldGuiNode = new Node("TiledWorld-wGUI" + name);
        MapRenderer mapRenderer = MapRenderer.create(map, ppm, rootNode);
        mapRenderer.setSpriteFactory(spriteFactory);

        World phy = new World(new Vec2(0, 0), createWorldPool(map));
        TiledWorld2d l = new TiledWorld2d(name, map, phy, ppm, mapRenderer, rootNode,overLayNode, worldGuiNode);
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
        l.contactListener = new ContactListener(){

            private TiledEntity[] tmp = new TiledEntity[4];
            private TiledEntity[]  getTiledComponentManagers(Contact c){
                Fixture fx = c.getFixtureA();

                Object fxData = fx.getUserData();
                Box2dUserData ud;
                TiledEntity entity;
                TiledEntity collision;


                if(fxData!=null&&fxData instanceof Box2dUserData){                 
                    ud = (Box2dUserData)fxData;
                    entity = ud.getEntity();
                    tmp[0] = entity;
                    collision = ud.getCollision();
                    tmp[2] = collision;
                }

                fx = c.getFixtureB();
                fxData = fx.getUserData();

                if(fxData!=null&&fxData instanceof Box2dUserData){                
                    ud = (Box2dUserData) fx.getUserData();                    
                    entity = ud.getEntity();
                    tmp[1] = entity;
                    collision = ud.getCollision();
                    tmp[3] = collision;
                }

                return tmp;

            }

            private void clear(){
                tmp[0] = null;
                tmp[1] = null;
                tmp[2] = null;
                tmp[3] = null;
            }

            @Override
            public void beginContact(Contact contact) {
                TiledEntity mngs[] = getTiledComponentManagers(contact);
                if(mngs[0] == null || mngs[1] == null)      return;
                for(int i = 0; i< 2; i++){
                    TiledEntity e = mngs[i];
                    if(e!=null){
                        TiledComponentManager mng = e.getComponentManager();
 
                        if(mng!=null){
                            mng.beginContact(
                                mngs[0], 
                                mngs[1],
                                (TiledObjectEntity)mngs[2],
                                (TiledObjectEntity)mngs[3],    
                                contact
                            );
                        }
                    }
                }
                clear();
                
            }

            @Override
            public void endContact(Contact contact) {
   
                TiledEntity mngs[] = getTiledComponentManagers(contact);
                if(mngs[0] == null || mngs[1] == null)      return;
                for(int i = 0; i< 2; i++){
                    TiledEntity e = mngs[i];
                    if(e!=null){
                        TiledComponentManager mng = e.getComponentManager();
                        if(mng!=null){
                            mng.endContact(
                                mngs[0], 
                                mngs[1],
                                (TiledObjectEntity)mngs[2],
                                (TiledObjectEntity)mngs[3],    
                                contact
                            );
                        }
                    }
                }             
                clear();
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
 
                TiledEntity mngs[] = getTiledComponentManagers(contact);
                if(mngs[0] == null || mngs[1] == null)      return;
                for(int i = 0; i< 2; i++){
                    TiledEntity e = mngs[i];
                    if(e!=null){
                        TiledComponentManager mng = e.getComponentManager();
                        if(mng!=null){
                            mng.preSolve(
                                mngs[0], 
                                mngs[1],
                                (TiledObjectEntity)mngs[2],
                                (TiledObjectEntity)mngs[3],    
                                contact, oldManifold
                            );
                        }
                    }
                }

                clear();
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
             
                TiledEntity mngs[] = getTiledComponentManagers(contact);
                if(mngs[0] == null || mngs[1] == null)      return;

                for(int i = 0; i< 2; i++){
                    TiledEntity e = mngs[i];
                    if(e!=null){
                        TiledComponentManager mng = e.getComponentManager();
                        if(mng!=null){
                            mng.postSolve(
                                mngs[0], 
                                mngs[1],
                                (TiledObjectEntity)mngs[2],
                                (TiledObjectEntity)mngs[3],    
                                contact, impulse
                            );
                        }
                    }
                }
                clear();
            }

        };

        phy.setContactListener(l.contactListener);

        loadedMaps.put(name, l);
        transformQuantizers.put(name, buildTransformQuantizer(map));
        for(Consumer<TiledWorld2d> listener : worldLoadListener){
            listener.accept(l);
        }
        return l;
    }

    private DefaultWorldPool createWorldPool(TiledMap map) {
        int poolSize = NGEUtils.safeInt(map.getPropertyOrDefault("physics.poolSize", "512"));
        int poolContainerSize = NGEUtils.safeInt(map.getPropertyOrDefault("physics.poolContainerSize", "64"));
        poolSize = Math.max(poolSize, World.WORLD_POOL_SIZE);
        poolContainerSize = Math.max(poolContainerSize, World.WORLD_POOL_CONTAINER_SIZE);
        return new DefaultWorldPool(poolSize, poolContainerSize);
    }

    public void unloadWorld(String name) {
        String mapNameAndPath[] = getMapNameAndPath(name);
        name = mapNameAndPath[0];

        TiledWorld2d map = loadedMaps.remove(name);
        if (map != null) {
            transformQuantizers.remove(name);
            map.clearPhysicsStepState();
            map.getMapNode().removeFromParent();
        }
    }

    public void unloadWorld(TiledWorld2d l) {
        unloadWorld(l.getName());
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

    public void loadWorldIfNeededAsync(String path) {
        if(loadingMaps.contains(path)){
            // already loading
            return;
        }
        loadingMaps.add(path);
        String mapNameAndPath[] = getMapNameAndPath(path);
        String mapName = mapNameAndPath[0];
        String mapPath = mapNameAndPath[1];

        TiledWorld2d loadedMap = loadedMaps.get(mapName);
        if (loadedMap != null) {
            return;        
        } 
        AsyncAssetManager assetManager = getInstanceOf(AsyncAssetManager.class);
        assetManager.loadAssetAsync(mapPath, (Object mapObj, Throwable exc) -> {
            loadingMaps.remove(mapPath);
            if(exc==null){
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
        ArrayList<TiledWorld2d> mapsToUnload = new ArrayList<>(loadedMaps.values());
        for (TiledWorld2d l : mapsToUnload) {
            unloadWorld(l);
        }
        transformQuantizers.clear();
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
        Collection<TiledWorld2d> worlds = loadedMaps.values();

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
                            }
                        }
                    }

                } else if (layer instanceof TiledObjectLayer) {
                    TiledObjectLayer og = (TiledObjectLayer) layer;
                    for (TiledObjectEntity obj : og.getObjects()) {
                        onEntityUpdate(tpf, world, layer, obj);
                         
                    }
                    
                }
            }
        }

        for (TiledWorld2d map : worlds) {

            World physics = map.getPhysics();
            if (physics != null) {
                map.beginPhysicsStep();
                try {
                    physics.step(tpf, 8, 3);
                } finally {
                    map.endPhysicsStep();
                }
            }

            MapRenderer renderer = map.getRenderer();
            renderer.render(map.getRenderListener(), tpf);

            // map.getMapNode().updateLogicalState(tpf);
            // map.getMapNode().updateGeometricState();
            
            // map.getOverlayNode().updateLogicalState(tpf);
            // map.getOverlayNode().updateGeometricState();
           
        }


        if(isDebugEnabled()){
            MainThreadRunner mainRunner = getInstanceOf(MainThreadRunner.class);
            AssetManager assetManager = getInstanceOf(AssetManager.class);
            Box2dDebugger.update(mainRunner, assetManager, worlds, tpf);
        }
    }
 

    @Override
    public Component newInstance() {
        return new TiledWorld2dManagerComponent(defaultMapName, spriteFactorySupplier);
    }

    @Override
    public void beginContact(Contact contact) {

    }

    @Override
    public void endContact(Contact contact) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

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
        return true;
    }

}
