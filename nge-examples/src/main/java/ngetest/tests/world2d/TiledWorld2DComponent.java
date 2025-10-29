package ngetest.tests.world2d;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.ngengine.AsyncAssetManager;
import org.ngengine.Components;
import org.ngengine.ViewPortManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.platform.NGEUtils;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import io.github.jmecn.tiled.TmxLoader;
import io.github.jmecn.tiled.core.Base;
import io.github.jmecn.tiled.core.ImageLayer;
import io.github.jmecn.tiled.core.Layer;
import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.ObjectGroup;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.core.TileLayer;
import io.github.jmecn.tiled.core.TiledImage;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.renderer.MapRenderer;
import io.github.jmecn.tiled.renderer.factory.DefaultMaterialFactory;
import io.github.jmecn.tiled.renderer.factory.DefaultMeshFactory;
import io.github.jmecn.tiled.renderer.factory.DefaultSpriteFactory;
import io.github.jmecn.tiled.renderer.factory.MaterialFactory;
import io.github.jmecn.tiled.renderer.factory.SpriteFactory;
import io.github.jmecn.tiled.renderer.queue.YAxisComparator;
import jakarta.annotation.Nullable;

public class TiledWorld2DComponent implements Component, LogicFragment, ContactListener {
    private static Logger logger = Logger.getLogger(TiledWorld2DComponent.class.getName());
    private ComponentManager mng;
    private MapRenderer mapRenderer;
    private Node mapNode;
    private String mapPath;
    private volatile boolean ready = false;
    private World world;

    private BiFunction<ComponentManager, TiledMap, SpriteFactory> spriteFactorySupplier = (mng, map) -> {
        AssetManager assetManager = mng.getGlobalInstance(AssetManager.class);
        MaterialFactory materialFactory = new DefaultMaterialFactory(assetManager);

        SpriteFactory spriteFactory = new TiledWorldSpriteFactory();
        spriteFactory.setMaterialFactory(materialFactory);
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));

        return spriteFactory;
    };

    public TiledWorld2DComponent(
        @Nullable String mapPath,
        @Nullable BiFunction<ComponentManager, TiledMap, SpriteFactory> spriteFactorySupplier
    ) {
        if (spriteFactorySupplier != null) this.spriteFactorySupplier = spriteFactorySupplier;
        if (mapPath != null) this.mapPath = mapPath;
        world = new World(new Vec2(0, 0));
        world.setContactListener(this);
    }

    public TiledWorld2DComponent(@Nullable String mapPath) {
        this(mapPath, null);
    }

    public void setGravity(Vector2f gravity) {
        world.setGravity(new Vec2(gravity.x, gravity.y));
    }

    public World getPhysicsWorld() {
        return world;
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime) {
        this.mng = mng;
        ViewPortManager viewPortManager = mng.getGlobalInstance(ViewPortManager.class);

        ViewPort viewPort = viewPortManager.getMainSceneViewPort();
        viewPort.getQueue().setGeometryComparator(RenderQueue.Bucket.Opaque, new YAxisComparator());

        Camera cam = viewPort.getCamera();
        float near = -1000f;
        float far = 10f;

        float ratio = (float) cam.getWidth() / cam.getHeight();
        float viewHeight = 128;// tileSize = 16, see 8 tiles in height
        float viewWidth = ratio * viewHeight;
        float halfWidth = viewWidth * 0.5f;
        float halfHeight = viewHeight * 0.5f;
        
        cam.setParallelProjection(true);
        cam.setFrustum(near, far, -halfWidth, halfWidth, halfHeight, -halfHeight);
        cam.setLocation(new Vector3f(halfWidth, 0, halfHeight));
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));

        logger.log(Level.FINE, "cam: " + cam);

    }

    public void setMap(String mapPath) {
        ready = false;
        ViewPortManager viewPortManager = mng.getGlobalInstance(ViewPortManager.class);
        AsyncAssetManager assetManager = mng.getGlobalInstance(AsyncAssetManager.class);
        MainThreadRunner runner = mng.getGlobalInstance(MainThreadRunner.class);
        ViewPort viewPort = viewPortManager.getMainSceneViewPort();
        RenderManager renderManager = mng.getGlobalInstance(RenderManager.class);

        assetManager.loadAssetAsync(mapPath, (Object mapObj, Throwable exc) -> {
            if (exc != null) {
                logger.log(Level.SEVERE, "Failed to load map: " + mapPath, exc);
                return;
            }
            runner.run(() -> {
                if (!mng.isComponentEnabled(this)) { // component was disabled while loading
                    return;
                }
                TiledMap map = (TiledMap) mapObj;
                viewPort.setBackgroundColor(map.getBackgroundColor());
                
                SpriteFactory spriteFactory = spriteFactorySupplier.apply(mng, map);
                mapRenderer = MapRenderer.create(map);
                mapRenderer.setSpriteFactory(spriteFactory);

                Node rootNode = viewPortManager.getRootNode(viewPort);
                mapNode = mapRenderer.getRootNode();

                renderManager.preload(mapNode);
                rootNode.attachChild(mapNode);

                ready = true;
            });
        });
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        if (mapNode != null)  mapNode.removeFromParent();
        mapNode = null;
        mapRenderer = null;
        ready = false;
    }

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        if (mapPath != null) {
            setMap(mapPath);
            mapPath = null;
        }
        if (mapRenderer != null) {
            mapRenderer.render();
        }
        if (world!=null){
            world.step(tpf, 8, 3);
        }
    }

    public Base findWorldEntity(Spatial sp){
        mapRenderer.getImageLayerSpatial(null)

    }


    public MapObject findObject(String layerName, Predicate<MapObject> filter) {
        for (Layer l : mapRenderer.getSortedLayers()) {
            if (l instanceof ObjectGroup) {
                ObjectGroup og = (ObjectGroup) l;
                if (og.getName().equals(layerName)) {
                    for (MapObject obj : og.getObjects()) {
                        if (filter.test(obj)) {
                            return obj;
                        }
                    }
                }
            }
        }
        return null;
    }

    public Spatial addObject(String layerName, MapObject object){
        for(Layer l : mapRenderer.getSortedLayers()){
            if(l instanceof ObjectGroup){
                ObjectGroup og = (ObjectGroup)l;
                if(og.getName().equals(layerName)){
                    og.add(object);
                    Spatial sp = mapRenderer.getOrCreateMapObjectSpatial(og, object);
                    return sp;
                }
            }
        }
        throw new IllegalArgumentException("Layer not found or not an ObjectGroup: "+ layerName);
    }

    public static interface RemoveObjectPredicate extends Function<MapObject, Boolean>{
        public static Boolean REMOVE_AND_CONTINUE = null;
        public static Boolean REMOVE_AND_STOP = Boolean.TRUE;
        public static Boolean KEEP_AND_CONTINUE = Boolean.FALSE;
        public Boolean apply(MapObject t);
    }

    public void removeObject(String layerName, RemoveObjectPredicate filter){
        for(Layer l : mapRenderer.getSortedLayers()){
            if(l instanceof ObjectGroup){
                ObjectGroup og = (ObjectGroup)l;
                if(og.getName().equals(layerName)){
                    for(MapObject obj : og.getObjects()){
                        Boolean res = filter.apply(obj);
                        if(res == RemoveObjectPredicate.REMOVE_AND_CONTINUE){
                            og.remove(obj);
                            mapRenderer.removeSpatial(og, obj);
                            continue;
                        } else if(res == RemoveObjectPredicate.REMOVE_AND_STOP){
                            og.remove(obj);
                            mapRenderer.removeSpatial(og, obj);
                            return;
                        } else {
                            continue;
                        }
                    }                    
                }
            }
        }        
    }

    public void removeObject(String layerName, MapObject obj){
        for(Layer l : mapRenderer.getSortedLayers()){
            if(l instanceof ObjectGroup){
                ObjectGroup og = (ObjectGroup)l;
                if(og.getName().equals(layerName)){
                    og.remove(obj);
                    mapRenderer.removeSpatial(og, obj);
                }
            }
        }        
    }
     
    public void setTile(String layerName, int x, int y, Tile t){
        for(Layer l : mapRenderer.getSortedLayers()){
            if(l instanceof TileLayer){
                TileLayer og = (TileLayer)l;
                if(og.getName().equals(layerName)){
                    og.setTileAt(x, y, t);
                    return;
                }
            }
        }        
    }

    public Tile findTile(String layerName, Predicate<Tile> filter) {
        for (Layer l : mapRenderer.getSortedLayers()) {
            if (l instanceof TileLayer) {
                TileLayer tl = (TileLayer) l;
                if (tl.getName().equals(layerName)) {
                    for (int ty = tl.getY(); ty < tl.getY() + tl.getHeight(); ty++) {
                        for (int tx = tl.getX(); tx < tl.getX() + tl.getWidth(); tx++) {
                            Tile tile = tl.getTileAt(tx, ty);
                            if (filter.test(tile)) {
                                return tile;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void setImage(String layer, TiledImage image){
        for(Layer l : mapRenderer.getSortedLayers()){
            if(l instanceof ImageLayer){
                ImageLayer il = (ImageLayer)l;
                if(il.getName().equals(layer)){
                    il.setImage(image);
                    return;
                }
            }
        }
    }

    public TiledImage findImage(String layerName) {
        for (Layer l : mapRenderer.getSortedLayers()) {
            if (l instanceof ImageLayer) {
                ImageLayer il = (ImageLayer) l;
                if (il.getName().equals(layerName)) {
                    return il.getImage();
                }
            }
        }
        return null;
    }

    public List<Layer> getLayers(){
        return mapRenderer.getSortedLayers();
    }

    public MapRenderer getMap(){
        return mapRenderer;
    }

    public Spatial getObjectSpatial(String layerName, MapObject object){
        for(Layer l : mapRenderer.getSortedLayers()){
            if(l instanceof ObjectGroup){
                ObjectGroup og = (ObjectGroup)l;
                if(og.getName().equals(layerName)){
                    return mapRenderer.getOrCreateMapObjectSpatial(og, object);
                }
            }
        }
        throw new IllegalArgumentException("Object "+object.getName()+" not found in layer: "+ layerName+" or layer not an ObjectGroup");
    }

    @Override
    public Component newInstance() {
        return new TiledWorld2DComponent(mapPath, spriteFactorySupplier);
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

    public void tileToPixelCoords(int x, int y, Vector2f out) {
        out.set(this.mapRenderer.tileToPixelCoords(x, y));
    }


}
