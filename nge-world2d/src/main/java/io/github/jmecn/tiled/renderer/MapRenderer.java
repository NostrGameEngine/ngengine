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
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.renderer.factory.MaterialFactory;
import io.github.jmecn.tiled.renderer.factory.SpriteFactory;
import io.github.jmecn.tiled.math2d.Point;
import java.util.logging.Logger;
import io.github.jmecn.tiled.animation.AnimatedTileControl;
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
public abstract class MapRenderer {
    public static MapRenderer create(TiledMap tiledMap) {
        switch (tiledMap.getOrientation()) {
            case ORTHOGONAL:
                return new OrthogonalRenderer(tiledMap);
            case ISOMETRIC:
                return new IsometricRenderer(tiledMap);
            case STAGGERED:
                return new StaggeredRenderer(tiledMap);
            case HEXAGONAL:
                return new HexagonalRenderer(tiledMap);
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
    
    private final Map<Layer, RenderRef> lrefs = new HashMap<>();
    private final Map<Spatial, Base> entryMap = new WeakHashMap<>();
 
    public static class RenderRef {
        Base entry;
        int renderPass;
        Spatial sp;
        Map<Base, RenderRef> refs = new HashMap<>();

        final int maxTiles ;
        RenderRef(int maxTiles){
            this.maxTiles = maxTiles;
        }
        private Spatial tiles[];

        Spatial getSpatialTile(int index){
            if(tiles == null || index<0 || index>=tiles.length){
                return null;
            }
            return tiles[index];
        }

        public void setSpatialTile(int index, Spatial sp){
            if(tiles == null){
                tiles = new Spatial[maxTiles];
            }
            if(index<0 || index>=tiles.length){
                return;
            }
            tiles[index] = sp;            
        }
    }


    private int lastRenderPass = 0;
 


    protected SpriteFactory spriteFactory;

    /**
     * The whole map size in pixel
     */
    protected Point mapSize;

    protected MapRenderer(TiledMap tiledMap) {
        this.tiledMap = tiledMap;
        this.width = tiledMap.getWidth();
        this.height = tiledMap.getHeight();
        this.tileWidth = tiledMap.getTileWidth();
        this.tileHeight = tiledMap.getTileHeight();
        this.step = layerDistance / (height * width);
        this.mapSize = new Point(width * tileWidth, height * tileHeight);

        this.rootNode = new Node("TileMap");
        this.rootNode.setQueueBucket(RenderQueue.Bucket.Opaque);
    }

    
 
   protected RenderRef getTrackedLayerRef(Layer layer, boolean createIfAbsent){ 
        RenderRef r;
        if(createIfAbsent){
            r = lrefs.computeIfAbsent(layer, key -> {
                Node node = new Node(layer.getName());
                rootNode.attachChild(node);
                entryMap.put(node, layer);

                RenderRef ref = new RenderRef( layer.getWidth() * layer.getHeight());
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
  
    protected Spatial getTrackedSpatial(RenderRef layerRef, Base entry, Function<Base, Spatial> creator, BiFunction<Base,Spatial, Boolean> filter){
        if(creator==null){
            RenderRef r = layerRef.refs.get(entry);
            if(r!=null && (filter==null || filter.apply(entry, r.sp))){
                r.renderPass = lastRenderPass;
                return r.sp;
            }
            return null;
        }
        RenderRef r = layerRef.refs.computeIfAbsent(entry, key -> {
            Spatial sp = creator.apply(entry);
            RenderRef ref = new RenderRef(0);
            ref.entry = entry;
            ref.sp = sp;
            entryMap.put(sp, entry);
            return ref;
        });
        if(filter!=null && !filter.apply(entry, r.sp)){
            return null;
        }
        r.renderPass = lastRenderPass;
        return r.sp;
    }

    public Spatial getSpatial(Layer layer, Base entry){
        RenderRef layerRef = getTrackedLayerRef(layer, false);
        if(layerRef==null){
            return null;
        }
        if(layer instanceof ImageLayer){
            return getTrackedSpatial(layerRef, layer,  null, null);             
        } else if (layer instanceof TileLayer){
            if(entry instanceof Tile){
                Tile tile = (Tile) entry;
                int tx = tile.getX();
                int ty = tile.getY();
                int index = (ty - layer.getY()) * layer.getWidth() + (tx - layer.getX());
                Spatial sp = layerRef.getSpatialTile(index);
                return sp;
            }
        } else if (layer instanceof ObjectGroup){
            return getTrackedSpatial(layerRef, entry, null, MapRenderer::filterByShape);             
        }
        logger.warning("Cannot get spatial for entry: " + entry.getName()+" in layer: " + layer.getName());
        return null;
    }

    public Base getEntry(Spatial sp){
        return entryMap.get(sp);
    }

    public Node getLayerNode(Layer layer){
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
    public void render() {
        if (tiledMap == null) return;
        render(tiledMap.getLayers());       
    }

    private void render(List<Layer> layers) {

        for(int i=0; i<layers.size(); i++){
            Layer layer = layers.get(i);
            if (layer instanceof GroupLayer) {
                GroupLayer group = (GroupLayer) layer;
                render(group.getLayers());
                continue;
            }
            
            Spatial visual = null;
            if (layer instanceof ImageLayer) {
                visual = renderImageLayer((ImageLayer) layer);
            } else if (layer instanceof TileLayer) {
                visual = renderTiles((TileLayer) layer);
            } else if (layer instanceof ObjectGroup) {
                visual = renderObjectGroup((ObjectGroup) layer);
            }

            if (visual != null) {
                Vector3f loc = visual.getLocalTranslation();
                visual.setLocalTranslation(loc.x, getLayerYIndex(i), loc.z);
                layer.clearUpdateNeeded();
                
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

        }

        // run garbage collection
        {
            int n = 0 ;
            Iterator<Entry<Layer, RenderRef>> iter = lrefs.entrySet().iterator();
            while(iter.hasNext()){
                Entry<Layer, RenderRef> entry = iter.next();
                RenderRef ref = entry.getValue();
                boolean removeLayer = ref.renderPass != lastRenderPass;

                if(removeLayer){
                    ref.sp.removeFromParent();
                    n++;
                    if(ref.tiles!=null){
                        for(Spatial sp : ref.tiles){
                            if(sp!=null){
                                n++;
                                sp.removeFromParent();
                            }
                        }
                    }
                    iter.remove();
                }           

                
                Iterator<Entry<Base, RenderRef>> iter2 = ref.refs.entrySet().iterator();
                while(iter2.hasNext()){
                    Entry<Base, RenderRef> e2 = iter2.next();
                    RenderRef r2 = e2.getValue();
                    if(removeLayer || r2.renderPass != lastRenderPass){
                        r2.sp.removeFromParent();
                        n++;
                        if(ref.tiles!=null){
                            for(Spatial sp : ref.tiles){
                                if(sp!=null){
                                    sp.removeFromParent();
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
        
        lastRenderPass++;
    }

    private void visitLayers(Consumer<Layer> visitor, List<Layer> layers){
        for(int i=0; i<layers.size(); i++){
            Layer layer = layers.get(i);
            if (layer instanceof GroupLayer) {
                GroupLayer group = (GroupLayer) layer;
                visitLayers(visitor, group.getLayers());
                continue;
            }
            visitor.accept(layer);
        }
    }

    public void visitLayers(Consumer<Layer> visitor){
        visitLayers(visitor, tiledMap.getLayers());
    }
 

    public abstract void visitTiles(TileVisitor visitor);

    public void getSortedLayers(List<Layer> layers, List<Layer> out) {
        for(int i=0; i<layers.size(); i++){
            Layer layer = layers.get(i);
            if (layer instanceof GroupLayer) {
                GroupLayer group = (GroupLayer) layer;
                getSortedLayers(group.getLayers(), out);
                continue;
            }
            out.add(layer);
        }
    }

    private List<Layer> sortedLayers = new ArrayList<>();
    private List<Layer> sortedLayersRO = Collections.unmodifiableList(sortedLayers);

    public List<Layer> getSortedLayers() {
        sortedLayers.clear();
        getSortedLayers(tiledMap.getLayers(), sortedLayers);
        return sortedLayersRO;
    }

    protected Spatial renderTiles(TileLayer layer) {
        RenderRef ref = getTrackedLayerRef(layer, true);
        Node layerNode = (Node) ref.sp;
        boolean layerUpdateNeeded = layer.isUpdateNeeded();

        visitTiles((x, y, z) -> {
            Tile tile = layer.getTileAt(x, y);
            if(tile.isUpdateNeeded()||layerUpdateNeeded){
                int index = (y - layer.getY()) * layer.getWidth() + (x - layer.getX());
                Geometry visual = (Geometry)ref.getSpatialTile(index);
                MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
                if(visual == null){
                    visual = spriteFactory.newTileSprite(tile, materialFactory.newMaterial());
                    spriteFactory.applyProperties(tile, visual);
                    tile.clearPropertiesUpdateNeeded();                    
                    ref.setSpatialTile(index, visual);      
                    entryMap.put(visual, tile);            
                }

                visual.setName("tile#" + x + "#" + y+"#" + tile.getName());

                 // sort top-down
                Vector2f pixelCoord = tileToScreenCoords(x, y);
                float zTile =  getTileYAxis(z);
                visual.move(pixelCoord.x, zTile, pixelCoord.y);

                Material mat = visual.getMaterial();
                materialFactory.setTile(mat, tile);
                materialFactory.setTintColor(mat, layer.getTintColor());
                materialFactory.setLayerOpacity(mat, (float) layer.getOpacity());
            
                if(tile.isPropertiesUpdateNeeded()){
                    spriteFactory.applyProperties(tile, visual);
                    tile.clearPropertiesUpdateNeeded();
                }

                spriteFactory.setAnimation(visual, tile);

                tile.clearUpdateNeeded();

                if(visual.getParent()!=layerNode){
                    layerNode.attachChild(visual);
                }
            }
        });   

        layer.clearUpdateNeeded();
        layer.clearPropertiesUpdateNeeded();
        return layerNode;
    }

    
    protected Spatial renderImageLayer(ImageLayer layer) {
        RenderRef layerRef = getTrackedLayerRef(layer, true);
        Node layerNode = (Node)layerRef.sp;
        if (layer.isUpdateNeeded()) {
            MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
            Geometry spatial;
            if (layer.isVisible()) {
                 spatial = (Geometry)getTrackedSpatial(layerRef, layer, (base)->{
                    Mesh mesh = spriteFactory.getMeshFactory().rectangle(mapSize.getX(), mapSize.getY(), true);
                    Geometry geo = new Geometry(layer.getName(), mesh);
                    geo.setMaterial( materialFactory.newMaterial());
                    spriteFactory.applyProperties(layer, geo);
                    spriteFactory.applyProperties(layer, layerNode);
                    layer.clearPropertiesUpdateNeeded();
                    return geo;
                }, null);                       
             

                layerNode.attachChild(spatial);
            } else {
                spatial = (Geometry)getTrackedSpatial(layerRef, layer, null, null);
                if (spatial != null) {
                    layerNode.detachChild(spatial);
                }
            }

            if (spatial != null) {
                spatial.setName("image#" + layer.getName());
                materialFactory.setTiledImage(spatial.getMaterial(), layer.getImage());
                materialFactory.setTintColor(spatial.getMaterial(), layer.getTintColor());
                materialFactory.setLayerOpacity(spatial.getMaterial(), (float) layer.getOpacity());
                if(layer.isPropertiesUpdateNeeded()){
                    spriteFactory.applyProperties(layer, spatial);
                    spriteFactory.applyProperties(layer, layerNode);
                }      
            }

            layer.clearUpdateNeeded();
            layer.clearPropertiesUpdateNeeded();
        } else {
            // just refresh for custom gc
            getTrackedSpatial(layerRef, layer, null, null);                       
        }
        return layerNode;
    }


    private static final boolean filterByShape( Base base, Spatial sp) {
        if (base instanceof MapObject) {
            MapObject obj = (MapObject) base;
            Number shape = sp.getUserData("ngengine.world2d.shape");
            return shape!=null && shape.intValue() == obj.getShape().ordinal();         
        } 
        return true;   
    }
    /**
     * Create the visual part for every ObjectNode in a ObjectLayer.
     * 
     * @param layer A ObjectLayer object
     * 
     * @return a Spatial for this layer
     */
    protected Spatial renderObjectGroup(ObjectGroup layer) {
        RenderRef layerRef = getTrackedLayerRef(layer, true);
        Node layerNode = (Node)layerRef.sp;
        boolean layerUpdateNeeded = layer.isUpdateNeeded();
     
        List<MapObject> objects = layer.getObjects();
        int len = objects.size();
        objects.sort(layer.getDrawOrder());
        for (int i = 0; i < len; i++) {
            MapObject obj = objects.get(i);
            MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
           
            Geometry spatial;
            if (obj.isUpdateNeeded()||layerUpdateNeeded) {
                if (obj.isVisible()) {
                    spatial = (Geometry)getTrackedSpatial(layerRef,obj, (base)->{
                        Geometry v = spriteFactory.newObjectSprite(obj);
                        v.setMaterial(materialFactory.newMaterial());
                        spriteFactory.applyProperties(obj, v);
                        obj.clearPropertiesUpdateNeeded();
                        return v;
                    }, MapRenderer::filterByShape);
                    
                    spatial.setUserData("ngengine.world2d.shape", obj.getShape().ordinal());                    
                    layerNode.attachChild(spatial);
                } else {
                    spatial = (Geometry) getTrackedSpatial(layerRef, obj, null, MapRenderer::filterByShape);
                    if (spatial != null) {
                        layerNode.detachChild(spatial);
                    }
                }               
                
                spatial.setName(obj.getName());
               
                if (spatial != null) {
                    float x = (float) obj.getX();
                    float y = (float) obj.getY();

                    // sort top-down
                    // don't support sorting by index
                    float z = getObjectTopDownYIndex(y);

                    Vector2f screenCoord = pixelToScreenCoords(x, y);
                    spatial.move(screenCoord.x, z, screenCoord.y);

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
                    if(layer.isPropertiesUpdateNeeded()){
                        spriteFactory.applyProperties(layer, spatial);
                        spriteFactory.applyProperties(layer, layerNode);
                    }      
                }

                layer.clearUpdateNeeded();
                layer.clearPropertiesUpdateNeeded();
            } else {
                // just refresh for custom gc
                getTrackedSpatial(layerRef, obj, null, MapRenderer::filterByShape);
            }

            
        }

        layer.clearPropertiesUpdateNeeded();
        layer.clearUpdateNeeded();

        return layerNode;
    }


    public abstract void renderGrid(Node gridVisual, Material gridMaterial);

    ///////////////////////////////////////////////
    ///////// Coordinates System Convert //////////
    ///////////////////////////////////////////////

    /**
     * Convert the pixel coordinates to screen coordinates.
     * @param x the x coordinate in pixel
     * @param y the y coordinate in pixel
     * @return the screen coordinates
     */
    public abstract Vector2f pixelToScreenCoords(float x, float y);

    /**
     * Convert the screen coordinates to pixel coordinates.
     * @param x the x coordinate in screen
     * @param y the y coordinate in screen
     * @return the pixel coordinates
     */
    public abstract Point pixelToTileCoords(float x, float y);

    /**
     * Convert the tile coordinates to pixel coordinates.
     * @param x the x coordinate in tile
     * @param y the y coordinate in tile
     * @return the pixel coordinates
     */
    public abstract Vector2f tileToPixelCoords(float x, float y);

    /**
     * Convert the tile coordinates to screen coordinates.
     * @param x the x coordinate in tile
     * @param y the y coordinate in tile
     * @return the screen coordinates
     */
    public abstract Vector2f tileToScreenCoords(float x, float y);

    /**
     * Convert the screen coordinates to pixel coordinates.
     * @param x the x coordinate in screen
     * @param y the y coordinate in screen
     * @return the pixel coordinates
     */
    public abstract Vector2f screenToPixelCoords(float x, float y);

    /**
     * Convert the screen coordinates to tile coordinates.
     * @param x the x coordinate in screen
     * @param y the y coordinate in screen
     * @return the tile coordinates
     */
    public abstract Point screenToTileCoords(float x, float y);

     
  
    /**
     * Get the map node
     * @return the map node
     */
    public Node getRootNode() {
        return rootNode;
    }

  
    public void setUpdateNeeded() {
        for (Layer layer : getSortedLayers()) {
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
        return  (float) (index *(layerDistance + layerGap));
    }

    /**
     * this is the z-index in the layer
     * @param tileZIndex the z-index in the layer, range from [0 , width * height)
     * @return the y-axis in the layer
     */
    protected float getTileYAxis(int tileZIndex) {
        return (float) (tileZIndex * step);
    }

    public float getObjectTopDownYIndex(float y) {
        float tileY = y / mapSize.getY();
        return (float) (tileY * layerDistance);
    }

    public Point getMapDimension() {
        return mapSize;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }
}
