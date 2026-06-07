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

package io.github.jmecn.tiled.renderer;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.entity.TiledTileEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.enums.ObjectShape;
import io.github.jmecn.tiled.enums.Orientation;
import io.github.jmecn.tiled.renderer.factory.MaterialFactory;
import io.github.jmecn.tiled.renderer.factory.SpriteFactory;
import io.github.jmecn.tiled.util.CoordinateSystem;
import io.github.jmecn.tiled.math2d.Point;
import java.util.logging.Logger;
import io.github.jmecn.tiled.animation.AnimatedTileControl;
import io.github.jmecn.tiled.components.TiledComponentManager;

import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentManagerProvider;
import org.ngengine.platform.NGEPlatform;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * we don't really draw 2d image in a 3d game engine, instead I create spatials
 * and apply Material to tiles and objects.
 * </p>
 * 
 * In Tiled Qt they use XOY axis, X positive to right and Y positive to down
 * 
 * <pre>
 * O------- X
 * |
 * |
 * |
 * Y
 * </pre>
 * 
 * Once in jme3 I choose XOY plane, which means I have to modify the Y for every
 * tile and every object. Now I choose XOZ plane, it's much easier to do the
 * math.
 * 
 * <p>The Point(x,y) in Tiled now converted to Vector3f(x, 0, y).</p>
 * 
 * <pre>
 * O------- X
 * |
 * |
 * |
 * Z
 * </pre>
 * 
 * @author yanmaoyuan
 */
public abstract class MapRenderer implements CoordinateSystem {   
    
 

    public static MapRenderer create(TiledMap tiledMap, int PPM, Node rootNode) {
        switch (tiledMap.getOrientation()) {
            case ORTHOGONAL:
                return new OrthogonalRenderer(tiledMap, PPM, rootNode);
            case ISOMETRIC:
                return new IsometricRenderer(tiledMap, PPM, rootNode);
            case STAGGERED:
                return new StaggeredRenderer(tiledMap, PPM, rootNode);
            case HEXAGONAL:
                return new HexagonalRenderer(tiledMap, PPM, rootNode);
            default:
                throw new IllegalArgumentException("Unsupported orientation: " + tiledMap.getOrientation());
        }
    }

    private final static Logger logger = Logger.getLogger(MapRenderer.class.getName());

    protected double layerDistance = 16f;// the distance between layers
    protected double layerGap = 1f;// the gap between layers
    protected double step;

    protected TiledMap tiledMap;
    protected int width;
    protected int height;
    protected int tileWidth;
    protected int tileHeight;

    protected Node rootNode;
    
    private final Map<TiledLayer, RenderRef> lrefs = new HashMap<>();
    private final Map<Spatial, TiledBase> entryMap = new WeakHashMap<>();
    private final int PPM;
    private static final RenderRef EMPTY_RENDER_REF = new RenderRef(0);
  
    public static class RenderRef {
        TiledBase entry;
        int renderPass;
        Spatial sp;
        Map<TiledBase, RenderRef> refs = new HashMap<>();
        long updateId;
        long propertyUpdateId;

        final int maxTiles ;
        RenderRef(int maxTiles){
            this.maxTiles = maxTiles;
        }
        private RenderRef tiles[];

        public boolean isUpdateNeeded(long updateId){
            return this.updateId != updateId;
        }

        public void clearUpdateNeeded(long updateId){
            this.updateId = updateId;
        }

        public boolean isPropertiesUpdateNeeded(long propertyUpdateId){
            return this.propertyUpdateId != propertyUpdateId;
        }

        public void clearPropertiesUpdateNeeded(long propertyUpdateId){
            this.propertyUpdateId = propertyUpdateId;
        }

        RenderRef getTile(int index){
            if(tiles == null || index<0 || index>=tiles.length){
                return null;
            }
            return tiles[index];
        }

        public void setTile(int index, TiledBase tileEntry, Spatial sp){
            if(tiles == null){
                tiles = new RenderRef[maxTiles];
            }
            if(index<0 || index>=tiles.length){
                return;
            }
            RenderRef ref = new RenderRef(0);
            ref.entry = tileEntry;
            ref.sp = sp;
            tiles[index] = ref ;        
        }

        public boolean isTileUpdateNeeded(int index, long updateId){
            if(tiles == null || index<0 || index>=tiles.length){
                return false;
            }
            RenderRef ref = tiles[index];
            if(ref==null){
                return true;
            }
            return ref.isUpdateNeeded(updateId);
        }

        public void clearTileUpdateNeeded(int index, long updateId){
            if(tiles == null || index<0 || index>=tiles.length){
                return;
            }
            RenderRef ref = tiles[index];
            if(ref==null){
                return;
            }
            ref.clearUpdateNeeded(updateId);
        }

        public boolean isTilePropertiesUpdateNeeded(int index, long propertyUpdateId){
            if(tiles == null || index<0 || index>=tiles.length){
                return false;
            }
            RenderRef ref = tiles[index];
            if(ref==null){
                return false;
            }
            return ref.isPropertiesUpdateNeeded(propertyUpdateId);
        }

        public void clearTilePropertiesUpdateNeeded(int index, long propertyUpdateId){
            if(tiles == null || index<0 || index>=tiles.length){
                return;
            }
            RenderRef ref = tiles[index];
            if(ref==null){
                return;
            }
            ref.clearPropertiesUpdateNeeded(propertyUpdateId);
        }
    }


    private int lastRenderPass = 0;
 


    protected SpriteFactory spriteFactory;

    /**
     * The whole map size in pixel
     */
    protected Point mapSize;
    protected float screenMinY=0, screenMaxY=1;
    protected float screenMinX=0, screenMaxX=1;
    

    private BiFunction<TiledLayer, TiledBase,Spatial> imageSpriteGenerator =  (layer, base)->{
        MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
        Mesh mesh = spriteFactory.getMeshFactory().rectangle(mapSize.getX(), mapSize.getY(), true);
        Geometry geo = new Geometry(layer.getName(), mesh);
        geo.setMaterial( materialFactory.newMaterial());                   
        return geo;
    };

    private BiFunction<TiledLayer, TiledBase, Spatial> objectSpatialGenerator = (layer, base)->{
        TiledObjectEntity obj = (TiledObjectEntity) base;
        MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
        Geometry v = spriteFactory.newObjectSprite(obj);
        v.setMaterial(materialFactory.newMaterial());
        v.setUserData("ngengine.world2d.shape", obj.getShape().ordinal());     

        v.setUserData("ngengine.world2d.gid", obj.getTile()!=null? obj.getTile().getGid() : -1);
        return v;
    };

    public CoordinateSystem getCoordinateSystem(){
        return this;
    }

    protected MapRenderer(TiledMap tiledMap, int PPM, Node rootNode) {
        this.PPM = PPM;
        this.tiledMap = tiledMap;
        this.width = tiledMap.getWidth();
        this.height = tiledMap.getHeight();
        this.tileWidth = tiledMap.getTileWidth();
        this.tileHeight = tiledMap.getTileHeight();
        this.step = layerDistance / (height * width);
        this.mapSize = new Point(width * tileWidth, height * tileHeight);

        this.rootNode = rootNode;
        this.rootNode.setQueueBucket(RenderQueue.Bucket.Opaque);

        recalcScreenExtents();
    }

    
    protected void recalcScreenExtents() {
        Vector2f s00 = new Vector2f();
        tileToWorldSpace(0, 0, s00);
        Vector2f sW0 = new Vector2f();
        tileToWorldSpace(width, 0, sW0);
        Vector2f s0H = new Vector2f();
        tileToWorldSpace(0, height, s0H);
        Vector2f sWH = new Vector2f();
        tileToWorldSpace(width, height, sWH);

        screenMinY = Math.min(Math.min(s00.y, sW0.y), Math.min(s0H.y, sWH.y));
        screenMaxY = Math.max(Math.max(s00.y, sW0.y), Math.max(s0H.y, sWH.y));
        if (screenMaxY == screenMinY) screenMaxY = screenMinY + 1f;

        screenMinX = Math.min(Math.min(s00.x, sW0.x), Math.min(s0H.x, sWH.x));
        screenMaxX = Math.max(Math.max(s00.x, sW0.x), Math.max(s0H.x, sWH.x));
        if (screenMaxX == screenMinX) screenMaxX = screenMinX + 1f;
    }
 
   protected RenderRef getTrackedLayerRef(TiledLayer layer, boolean createIfAbsent){ 
        RenderRef r;
        if(createIfAbsent){
            r = lrefs.computeIfAbsent(layer, key -> {
                Node node = new Node(layer.getName());
                rootNode.attachChild(node);
                entryMap.put(node, layer);

                RenderRef ref = new RenderRef((int)( layer.getWidth() * layer.getHeight()));
                ref.entry = layer;
                ref.sp = node;
                return ref;
            });
        } else {
            r = lrefs.get(layer);
        }
        if(r!=null){
            r.renderPass = lastRenderPass;
        }
        return r;
    }
  
    protected RenderRef getTrackedSpatialRef(RenderRef layerRef, TiledBase entry, BiFunction<TiledLayer, TiledBase, Spatial> creator, BiFunction<TiledBase,Spatial, Boolean> filter){
     
        RenderRef r = layerRef.refs.get(entry);

        if(r!=null && (filter!=null && !filter.apply(entry, r.sp))){
            r.sp.removeFromParent();
            r = null;
        }

        if(r==null && creator!=null){
            Spatial sp = creator.apply((TiledLayer)layerRef.entry, entry);
            r = new RenderRef(0);
            r.entry = entry;
            r.sp = sp;
            entryMap.put(sp, entry);
            layerRef.refs.put(entry, r);
        }

        if(r!=null){
            r.renderPass = lastRenderPass;
            return r;
        }
        
        if(r==null) r= EMPTY_RENDER_REF;
        return r;
    }

    public Spatial getSpatial(TiledLayer layer, TiledBase entry){
        RenderRef layerRef = getTrackedLayerRef(layer, false);
        if(layerRef==null){
            return null;
        }
        if(layer instanceof TiledImageLayer){
            return getTrackedSpatialRef(layerRef, layer,  null, null).sp;       
        } else if (layer instanceof TiledTileLayer){
            if(entry instanceof Tile){
                Tile tile = (Tile) entry;
                int tx = tile.getX();
                int ty = tile.getY();
                int index = (int)((ty - layer.getY()) * layer.getWidth() + (tx - layer.getX()));
                RenderRef ref = layerRef.getTile(index);
                
                return ref != null? ref.sp : null;
            }
        } else if (layer instanceof TiledObjectLayer){
            return getTrackedSpatialRef(layerRef, entry, null, MapRenderer::filterByShape).sp;             
        }
        logger.warning("Cannot get spatial for entry: " + entry.getName()+" in layer: " + layer.getName());
        return null;
    }

    public TiledBase getEntry(Spatial sp){
        return entryMap.get(sp);
    }

    public Node getLayerNode(TiledLayer layer){
        RenderRef layerRef = getTrackedLayerRef(layer, true);
        if(layerRef==null){
            return null;
        }
        return (Node) layerRef.sp;
    }
    

    /**
     * Set the sprite factory
     * @param spriteFactory the sprite factory
     */
    public void setSpriteFactory(SpriteFactory spriteFactory) {
        this.spriteFactory = spriteFactory;
    }

    

    public abstract Spatial createTileGrid(Material material);


    /**
     * Render the tiled map
     * 
     * @return return a Spatial for the whole map.
     */
    public void render(Listener listener, float tpf) {
        if (tiledMap == null) return;
        render(listener, tiledMap.getLayers(),tpf);       
    }


    public static interface Listener {
        public void beforeMapRender(float tpf, TiledMap map);
        public void afterMapRender(float tpf, TiledMap map, Spatial visual);

        public void beforeEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry);
        public void afterEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry,  Spatial visual);

        public void beforeLayerRender(float tpf, TiledMap map, TiledLayer layer);
        public void afterLayerRender(float tpf, TiledMap map, TiledLayer layer, Spatial visual);

        // public void onEntityCleanup(float tpf,  TiledEntity tile);

    }

    

    

    private void render(Listener listener, List<TiledLayer> layers, float tpf) {
        listener.beforeMapRender(tpf,tiledMap);
 
 
        for(int i=0; i<layers.size(); i++){
            TiledLayer layer = layers.get(i);
            if (layer instanceof TiledLayerGroup) {
                TiledLayerGroup group = (TiledLayerGroup) layer;
                render(listener, group.getLayers(),tpf);
                continue;
            }

            listener.beforeLayerRender(tpf,tiledMap, layer);
            
            Spatial visual = null;
            if (layer instanceof TiledImageLayer) {
                visual = renderImageLayer((TiledImageLayer) layer);
            } else if (layer instanceof TiledTileLayer) {
                TiledTileLayer tileLayer = (TiledTileLayer) layer;
                int h = tileLayer.getHeight();
          
                visual = renderTiles(listener, tileLayer,tpf);
            } else if (layer instanceof TiledObjectLayer) {
                TiledObjectLayer objectLayer =(TiledObjectLayer) layer;
                int h = objectLayer.getHeight();
              
                visual = renderObjectGroup(listener, objectLayer,tpf);
                
            }

            if (visual != null) {
                Vector3f loc = visual.getLocalTranslation();
                float y = getLayerYIndex(layer);
                visual.setLocalTranslation(loc.x, y, loc.z);
                if (layer.isVisible()) {
                    if(visual.getParent()!=rootNode){
                        rootNode.attachChild(visual);
                    } 
                } else {
                    if(visual.getParent()==rootNode){
                        rootNode.detachChild(visual);
                    }
                }
            }
            listener.afterLayerRender(tpf,tiledMap, layer, visual);

        }

 
        // run garbage collection
        {
            int n = 0 ;
            Iterator<Entry<TiledLayer, RenderRef>> iter = lrefs.entrySet().iterator();
            while(iter.hasNext()){
                Entry<TiledLayer, RenderRef> entry = iter.next();
                RenderRef ref = entry.getValue();
                boolean removeLayer = ref.renderPass != lastRenderPass;

                if(removeLayer){
                    ref.sp.removeFromParent();
                    n++;
                    if(ref.tiles!=null){
                        for(RenderRef r : ref.tiles){
                            if(r.sp!=null){
                                n++;
                                r.sp.removeFromParent();
                            }
                            // listener.onEntityCleanup(tpf,r.entry);
                            
                        }
                    }
                    // listener.onEntityCleanup(tpf, ref.entry);
                    iter.remove();
                }           

                
                Iterator<Entry<TiledBase, RenderRef>> iter2 = ref.refs.entrySet().iterator();
                while(iter2.hasNext()){
                    Entry<TiledBase, RenderRef> e2 = iter2.next();
                    RenderRef r2 = e2.getValue();
                    if(removeLayer || r2.renderPass != lastRenderPass){
                        // listener.onEntityCleanup(tpf, r2.entry);
                        
                        r2.sp.removeFromParent();
                        n++;
                        if(ref.tiles!=null){
                            for(RenderRef r : ref.tiles){
                                if(r.sp!=null){
                                    r.sp.removeFromParent();
                                    n++;
                                }
                            }
                        }
                        iter2.remove();
                    }
                }           
            }
            if(n>0){
                logger.fine("World2d-GC: cleared "+ n + " unused map objects.");
            }
        }
        
        listener.afterMapRender(tpf,tiledMap, rootNode);
        lastRenderPass++;
    }

    private void visitLayers(Consumer<TiledLayer> visitor, List<TiledLayer> layers){
        for(int i=0; i<layers.size(); i++){
            TiledLayer layer = layers.get(i);
            if (layer instanceof TiledLayerGroup) {
                TiledLayerGroup group = (TiledLayerGroup) layer;
                visitLayers(visitor, group.getLayers());
                continue;
            }
            visitor.accept(layer);
        }
    }

    public void visitLayers(Consumer<TiledLayer> visitor){
        visitLayers(visitor, tiledMap.getLayers());
    }
 

    public abstract void visitTiles(TileVisitor visitor);


    protected Spatial renderTiles(Listener listener, TiledTileLayer layer, float tpf) {
        RenderRef ref = getTrackedLayerRef(layer, true);
        Node layerNode = (Node) ref.sp;

        int layerUpdateId = layer.getUpdateId();
        int layerPropertyUpdateId = layer.getPropertiesUpdateId();
        
        boolean layerUpdateNeeded = ref.isUpdateNeeded(layerUpdateId);
        boolean layerPropertiesUpdateNeeded = ref.isPropertiesUpdateNeeded(layerPropertyUpdateId);

        try(TempVars vars = TempVars.get()){         
            visitTiles((x, y, z) -> {
                TiledTileEntity entry = layer.getTileAt(x, y);
                int index = (int)((y - layer.getY()) * layer.getWidth() + (x - layer.getX()));

                RenderRef oldTile = ref.getTile(index);
                if(oldTile!=null&&oldTile.entry!=entry){
                    // listener.onEntityCleanup(tpf, oldTile.entry);
                    oldTile.entry = entry;
                    
                }

                Tile tile = entry.getTile();
                if(tile==null){
                    return;
                }   
                listener.beforeEntityRender(tpf,tiledMap, layer, entry);
                long tileUpdateId = (((long)layerUpdateId) << 32) | (layer.getUpdateIdAt(x, y) & 0xFFFFFFFFL);
                long propertyUpdateId = tile.getPropertiesUpdateId();
                boolean tileUpdateNeeded = ref.isTileUpdateNeeded(index, tileUpdateId);
                boolean tilePropertiesUpdateNeeded = ref.isTilePropertiesUpdateNeeded(index, propertyUpdateId);
                
                Geometry visual = oldTile!=null?(Geometry)oldTile.sp:null;

                if(layerUpdateNeeded || tileUpdateNeeded ){
                    MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
                    if(visual != null){
                        visual.removeFromParent();
                        entryMap.remove(visual);
                    }
                    visual = spriteFactory.newTileSprite(tile, materialFactory.newMaterial());
                    tilePropertiesUpdateNeeded = true;
                    ref.setTile(index, tile,  visual);      
                    entryMap.put(visual, tile);            
                

                    visual.setName("tile#" + x + "#" + y+"#" + tile.getName());

                    // sort top-down
                    Vector2f pixelCoord = vars.vect2d;
                    tileToWorldSpace(x, y, pixelCoord);
                    float zTile =  getTileYAxis(z);
                    visual.move(pixelCoord.x, zTile, pixelCoord.y);

                    Material mat = visual.getMaterial();
                    materialFactory.setTile(mat, tile);
                    materialFactory.setTintColor(mat, layer.getTintColor());
                    materialFactory.setLayerOpacity(mat, (float) layer.getOpacity());
                
                    spriteFactory.setAnimation(visual, tile);


                    if(visual.getParent()!=layerNode){
                        layerNode.attachChild(visual);
                    }
                    ref.clearTileUpdateNeeded(index, tileUpdateId);
                }  

                if(tilePropertiesUpdateNeeded||layerPropertiesUpdateNeeded){
                    spriteFactory.applyProperties(tile, visual);
                    ref.clearTilePropertiesUpdateNeeded(index, propertyUpdateId);
                }
                listener.afterEntityRender(tpf,tiledMap, layer, entry, visual);
            });   
        }

        ref.clearUpdateNeeded(layerUpdateId);
        ref.clearPropertiesUpdateNeeded(layerPropertyUpdateId);
        return layerNode;
    }



    
    protected Spatial renderImageLayer(TiledImageLayer layer) {
        int updateId = layer.getUpdateId();
        int propertyUpdateId = layer.getPropertiesUpdateId();
        RenderRef layerRef = getTrackedLayerRef(layer, true);


        Node layerNode = (Node)layerRef.sp;
 
        boolean layerUpdateNeeded = layerRef.isUpdateNeeded(updateId);

        if (layerUpdateNeeded) {
            MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
            
            RenderRef ref = getTrackedSpatialRef(layerRef, layer, imageSpriteGenerator, null);        

            if (layer.isVisible()) {        
                layerNode.attachChild(ref.sp);
            } else {
                layerNode.detachChild(ref.sp);
            }

            Geometry geo = (Geometry) ref.sp;
            geo.setName("image#" + layer.getName());
            materialFactory.setTiledImage(geo.getMaterial(), layer.getImage());
            materialFactory.setTintColor(geo.getMaterial(), layer.getTintColor());
            materialFactory.setLayerOpacity(geo.getMaterial(), (float) layer.getOpacity());

            if(layerRef.isPropertiesUpdateNeeded(propertyUpdateId) || ref.isPropertiesUpdateNeeded(propertyUpdateId)){
                spriteFactory.applyProperties(layer, ref.sp);
                spriteFactory.applyProperties(layer, layerNode);
            }      

            ref.clearUpdateNeeded(updateId);
            ref.clearPropertiesUpdateNeeded(propertyUpdateId);
            layerRef.clearUpdateNeeded(updateId);
            layerRef.clearPropertiesUpdateNeeded(propertyUpdateId);
        } else {
            // just refresh for custom gc
            getTrackedSpatialRef(layerRef, layer, null, null);                       
        }
        return layerNode;
    }


    private static final boolean filterByShape( TiledBase base, Spatial sp) {
        if (base instanceof TiledObjectEntity) {
            TiledObjectEntity obj = (TiledObjectEntity) base;
            Number shape = sp.getUserData("ngengine.world2d.shape");
            if(shape!=null && shape.intValue() != obj.getShape().ordinal()) {
                return false;
            }
            Number gid = sp.getUserData("ngengine.world2d.gid");
            int tileGid = obj.getTile()!=null? obj.getTile().getGid() : -1;
            if(gid!=null && gid.intValue() != tileGid){
                return false;
            }

        } 
        return true;   
    }

    private final ArrayList<TiledObjectEntity> tmpSortedObjects = new ArrayList<>();
 
    /**
     * Create the visual part for every ObjectNode in a ObjectLayer.
     * 
     * @param layer A ObjectLayer object
     * 
     * @return a Spatial for this layer
     */
    protected Spatial renderObjectGroup(Listener listener, TiledObjectLayer layer, float tpf) {
        RenderRef layerRef = getTrackedLayerRef(layer, true);
        Node layerNode = (Node)layerRef.sp;
        
        int layerUpdateId = layer.getUpdateId();
        int layerPropertyUpdateId = layer.getPropertiesUpdateId();

        boolean layerUpdateNeeded = layerRef.isUpdateNeeded(layerUpdateId);
        boolean layerPropertiesUpdateNeeded = layerRef.isPropertiesUpdateNeeded(layerPropertyUpdateId);
     
        List<TiledObjectEntity> objects = layer.getObjects();
        int len = objects.size();

        tmpSortedObjects.clear();
        tmpSortedObjects.addAll(objects);
        tmpSortedObjects.sort(layer.getDrawOrder());
        try(TempVars vars = TempVars.get()){         
            for (int i = 0; i < len; i++) {
                TiledObjectEntity obj = tmpSortedObjects.get(i);
                
                listener.beforeEntityRender(tpf,tiledMap, layer, obj);

                int objectUpdateId = obj.getUpdateId();
                int objectPropertyUpdateId = obj.getPropertiesUpdateId();
                MaterialFactory materialFactory = spriteFactory.getMaterialFactory();

                RenderRef ref = getTrackedSpatialRef(layerRef, obj, objectSpatialGenerator, MapRenderer::filterByShape);
            
                Geometry spatial = (Geometry) ref.sp;
                if (layerUpdateNeeded||ref.isUpdateNeeded(objectUpdateId)) {
                    if (obj.isVisible()) {
                        layerNode.attachChild(spatial);
                    } else {
                        layerNode.detachChild(spatial);
                    }               
                    
                    spatial.setName(obj.getName());
                
                            
                    float x = (float) obj.getX();
                    float y = (float) obj.getY();

                    // sort top-down
                    // don't support sorting by index
                    float z = getTopDownYIndex(obj);
                    Vector2f screenCoord = vars.vect2d;
                    gridToWorldSpace(x, y, screenCoord);
                    spatial.setLocalTranslation(screenCoord.x, z, screenCoord.y);
                

                    double deg = obj.getRotation();
                    if (deg != 0) {
                        float radian = (float) (FastMath.DEG_TO_RAD * deg);
                        // rotate the spatial clockwise
                        spatial.setLocalRotation(new Quaternion().fromAngles(0, -radian, 0));
                    }

                    spriteFactory.setAnimation(spatial, obj);
                    
                    materialFactory.setMapObject(spatial.getMaterial(), obj);
                    materialFactory.setTintColor(spatial.getMaterial(), layer.getTintColor());
                    materialFactory.setLayerOpacity(spatial.getMaterial(), (float) layer.getOpacity());
                
                    ref.clearUpdateNeeded(objectUpdateId);
                } 
                
                if(layerPropertiesUpdateNeeded||ref.isPropertiesUpdateNeeded(objectPropertyUpdateId)){
                    spriteFactory.applyProperties(layer, spatial);
                    spriteFactory.applyProperties(layer, layerNode);
                    ref.clearPropertiesUpdateNeeded(objectPropertyUpdateId);
                }      
                listener.afterEntityRender(tpf,tiledMap, layer, obj, spatial);
                
            }
        }

        layerRef.clearUpdateNeeded(layerUpdateId);
        layerRef.clearPropertiesUpdateNeeded(layerPropertyUpdateId);

        return layerNode;
    }


    public abstract void renderGrid(Node gridVisual, Material gridMaterial);

 
    
  
    /**
     * Get the map node
     * @return the map node
     */
    public Node getRootNode() {
        return rootNode;
    }

  
    public void setUpdateNeeded() {
        for (TiledLayer layer : tiledMap.getLayers()) {
            layer.setUpdateNeeded();
        }
    }

    public void setLayerDistance(double layerDistance) {
        this.layerDistance = layerDistance;
        this.step = layerDistance / (height * width);
        setUpdateNeeded();
    }

    public void setLayerGap(double layerGap) {
        this.layerGap = layerGap;
        setUpdateNeeded();
    }

    public float getLayerYIndex(int index) {
        return (float) (index * (layerDistance + layerGap));
    }

    public float getLayerYIndex(TiledLayer layer) {
        if (layer == null) {
            return 0f;
        }
        int index = tiledMap.getLayersFlat().indexOf(layer);
        return index >= 0 ? getLayerYIndex(index) : 0f;
    }

    public float getWorldYIndex(TiledLayer layer, float layerLocalYIndex) {
        return getLayerYIndex(layer) + layerLocalYIndex;
    }

    public float getWorldYIndex(TiledObjectEntity object) {
        if (object == null) {
            return 0f;
        }
        return getWorldYIndex(object.getObjectGroup(), getTopDownYIndex(object));
    }

    /**
     * this is the z-index in the layer
     * @param tileZIndex the z-index in the layer, range from [0 , width * height)
     * @return the y-axis in the layer
     */
    protected float getTileYAxis(int tileZIndex) {
        return (float) (tileZIndex * step);
    }

   
    
    // /**
    //  * Get the center point of a map object in screen coordinates
    //  * 
    //  * @param obj the map object
    //  * @return the center point in screen space (Vector2f with x and y coordinates)
    //  */
    // @Override
    // public void getObjectCenterScreenCoords(TiledMapObjectEntity obj, Vector2f out) {
    //     float centerX;
    //     float centerY;

    //     if (obj.getShape() == ObjectType.TILE) {
    //         if (tiledMap.getOrientation() == Orientation.ISOMETRIC) {
    //             // For isometric tiles, we need to adjust both X and Y to move to visual center
    //             // while keeping screen X constant.
    //             // 
    //             // In isometric: screenX depends on (tileX - tileY)
    //             // where tileX = x/tileWidth, tileY = y/tileHeight
    //             //
    //             // To keep screenX constant when moving vertically:
    //             // We need: Δx/tileWidth = Δy/tileHeight
    //             // Therefore: Δx = Δy * (tileWidth/tileHeight)
    //             //
    //             // To move up by halfHeight in pixel space:
    //             float halfHeight = (float) (obj.getHeight() * 0.5);
    //             float ratio = (float) tileWidth / (float) tileHeight;
                
    //             centerX = (float) obj.getX() - (halfHeight * ratio);
    //             centerY = (float) obj.getY() - halfHeight;
    //         } else {
    //             // For orthogonal tiles, alignment is bottom-left
    //             centerX = (float) (obj.getX() + obj.getWidth() * 0.5);
    //             centerY = (float) (obj.getY() - obj.getHeight() * 0.5);
    //         }
    //     } else {
    //         // For other shapes, (x,y) is top-left in Tiled
    //         centerX = (float) (obj.getX() + obj.getWidth() * 0.5);
    //         centerY = (float) (obj.getY() + obj.getHeight() * 0.5);
    //     }

    //     worldToScreenCoords(centerX, centerY, out);
    // }


    public Point getMapDimension() {
        return mapSize;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    @Override
    public void worldToPhysicsSpace(float x, float y, Vector2f out){
        out.x = x / PPM;
        out.y = y / PPM;
    }

    @Override
    public void  physicsToWorldSpace(Vec2 physicsWorldCoords, Vector2f out){
        out.x = physicsWorldCoords.x * PPM;
        out.y = physicsWorldCoords.y * PPM;
    }


    private final Vec2 phy = new Vec2();
    @Override
    public void physicsToWorldSpace(float x, float y, Vector2f out){
        phy.x = x;
        phy.y = y;
        physicsToWorldSpace(phy, out);
    }
}
