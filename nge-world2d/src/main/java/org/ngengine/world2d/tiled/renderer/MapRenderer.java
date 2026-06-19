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

package org.ngengine.world2d.tiled.renderer;

import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.*;
import org.ngengine.world2d.tiled.core.entity.TiledImageEntity;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.enums.RenderingMode;
import org.ngengine.world2d.tiled.renderer.factory.MaterialFactory;
import org.ngengine.world2d.tiled.renderer.factory.SpriteFactory;
import org.ngengine.world2d.tiled.renderer.shape.Rect;
import org.ngengine.world2d.tiled.util.CoordinateSystem;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;
import org.ngengine.world2d.tiled.math2d.Point;
import java.util.logging.Logger;
import org.ngengine.world2d.tiled.animation.AnimatedTileControl;
import org.ngengine.world2d.tiled.components.TiledComponentManager;

import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentManagerProvider;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.world2d.PovRenderer;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

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
    /**
     * Creates the renderer implementation matching the map orientation.
     *
     * @param tiledMap the map to render
     * @param PPM pixels per physics meter
     * @param rootNode root node that will contain rendered map layers
     * @return an orientation-specific renderer
     */
    public static MapRenderer create(TiledMap tiledMap, int PPM, Node rootNode) {
        return create(tiledMap, PPM, rootNode, TiledCoordinateSystem.create(tiledMap, PPM));
    }

    /**
     * Creates the renderer implementation matching the map orientation.
     *
     * @param tiledMap the map to render
     * @param PPM pixels per physics meter
     * @param rootNode root node that will contain rendered map layers
     * @param coordinateSystem map-owned coordinate system used by the renderer
     * @return an orientation-specific renderer
     */
    public static MapRenderer create(TiledMap tiledMap, int PPM, Node rootNode, TiledCoordinateSystem coordinateSystem) {
        switch (tiledMap.getOrientation()) {
            case ORTHOGONAL:
                return new OrthogonalRenderer(tiledMap, PPM, rootNode, coordinateSystem);
            case ISOMETRIC:
                return new IsometricRenderer(tiledMap, PPM, rootNode, coordinateSystem);
            case STAGGERED:
                return new StaggeredRenderer(tiledMap, PPM, rootNode, coordinateSystem);
            case HEXAGONAL:
                return new HexagonalRenderer(tiledMap, PPM, rootNode, coordinateSystem);
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
    final Map<Spatial, TiledBase> entryMap = new WeakHashMap<>();
    private final TiledCoordinateSystem coordinateSystem;
    private final Map<Tileset, InstancedTilesetSource> instancedSourceCache = new IdentityHashMap<>();
    private final ViewCull viewCull = new ViewCull();
    private final Quaternion objectRotation = new Quaternion();
    private final InstancedBatchingPolicy instancedBatchingPolicy = new InstancedBatchingPolicy();
    private final IdentityHashMap<TiledBase, Integer> transientCooldowns = new IdentityHashMap<>();
    private final IdentityHashMap<TiledBase, TransientSignature> transientSignatures = new IdentityHashMap<>();
    private final IdentityHashMap<TiledBase, String> transientReasons = new IdentityHashMap<>();
    private final IdentityHashMap<TiledBase, Spatial> transientSpatials = new IdentityHashMap<>();
    private final ArrayList<TransientDebugRecord> transientDebugRecords = new ArrayList<>();
    private final Node batchDebugNode = new Node("TiledWorld2D-BatchDebug");
    int maxInstancedTilesetSlots = 4;
    private int maxInstancedRebatchesPerFrame = 1;
    int renderFrame;
    private int instancedRebatchesThisFrame;
    boolean batchDebugEnabled;
    private static final int TRANSIENT_COOLDOWN_FRAMES = 30;
    static final int BATCH_COOLDOWN_FRAMES = 45;
    private static final int DECAL_LAYERS = 4;
    private static final String DECAL_TILE_PROPERTY = "decal.tile";
    private static final String DECAL_TILESET_PROPERTY = "decal.tileset";
    private static final String DECAL_SCALE_PROPERTY = "decal.scale";
    private static final String DECAL_SIZE_PROPERTY = "decal.size";
    private static final String DECAL_OFFSET_X_PROPERTY = "decal.offsetX";
    private static final String DECAL_OFFSET_Y_PROPERTY = "decal.offsetY";
    private static final String DEFAULT_DECAL_TILESET = "tilesetDECALS";
    private static final RenderRef EMPTY_RENDER_REF = new RenderRef(0);
  
    public static class RenderRef {
        TiledBase entry;
        int renderPass;
        Spatial sp;
        Map<TiledBase, RenderRef> refs = new HashMap<>();
        long updateId;
        long propertyUpdateId;
        boolean instancedTiles;
        ArrayList<InstancedTileBatch> instancedBatches;
        int visibleTileSignature = Integer.MIN_VALUE;
        RenderingMode renderMode = RenderingMode.AUTO;

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

        public void clearTiles(){
            tiles = null;
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
        return coordinateSystem;
    }

    protected MapRenderer(TiledMap tiledMap, int PPM, Node rootNode) {
        this(tiledMap, PPM, rootNode, TiledCoordinateSystem.create(tiledMap, PPM));
    }

    protected MapRenderer(TiledMap tiledMap, int PPM, Node rootNode, TiledCoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
        this.tiledMap = tiledMap;
        this.width = tiledMap.getWidth();
        this.height = tiledMap.getHeight();
        this.tileWidth = coordinateSystem.getTileWidth();
        this.tileHeight = coordinateSystem.getTileHeight();
        this.step = layerDistance / (height * width);
        this.mapSize = coordinateSystem.getMapDimension();
        this.instancedBatchingPolicy.configureDefaults(tiledMap);

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

    private void updateViewCull(Camera camera) {
        if (camera == null || !camera.isParallelProjection() || camera.getWidth() <= 0 || camera.getHeight() <= 0) {
            viewCull.disable();
            return;
        }

        viewCull.active = true;
        viewCull.empty = false;
        viewCull.minX = Float.POSITIVE_INFINITY;
        viewCull.maxX = Float.NEGATIVE_INFINITY;
        viewCull.minY = Float.POSITIVE_INFINITY;
        viewCull.maxY = Float.NEGATIVE_INFINITY;

        try (TempVars vars = TempVars.get()) {
            includeCameraCorner(camera, 0f, 0f, vars.vect1);
            includeCameraCorner(camera, camera.getWidth(), 0f, vars.vect1);
            includeCameraCorner(camera, 0f, camera.getHeight(), vars.vect1);
            includeCameraCorner(camera, camera.getWidth(), camera.getHeight(), vars.vect1);
        }

        float margin = Math.max(tileWidth, tileHeight) * 2f;
        viewCull.minX -= margin;
        viewCull.maxX += margin;
        viewCull.minY -= margin;
        viewCull.maxY += margin;

        if (viewCull.maxX < screenMinX - margin || viewCull.minX > screenMaxX + margin
                || viewCull.maxY < screenMinY - margin || viewCull.minY > screenMaxY + margin) {
            viewCull.setEmpty();
            return;
        }

        viewCull.tileMinX = Integer.MAX_VALUE;
        viewCull.tileMaxX = Integer.MIN_VALUE;
        viewCull.tileMinY = Integer.MAX_VALUE;
        viewCull.tileMaxY = Integer.MIN_VALUE;
        includeCullTile(viewCull.minX, viewCull.minY);
        includeCullTile(viewCull.maxX, viewCull.minY);
        includeCullTile(viewCull.minX, viewCull.maxY);
        includeCullTile(viewCull.maxX, viewCull.maxY);

        viewCull.tileMinX = clamp(viewCull.tileMinX - 2, 0, width - 1);
        viewCull.tileMaxX = clamp(viewCull.tileMaxX + 2, 0, width - 1);
        viewCull.tileMinY = clamp(viewCull.tileMinY - 2, 0, height - 1);
        viewCull.tileMaxY = clamp(viewCull.tileMaxY + 2, 0, height - 1);
        viewCull.updateSignature();
    }

    private void includeCameraCorner(Camera camera, float x, float y, Vector3f store) {
        viewCull.screenPoint.set(x, y);
        camera.getWorldCoordinates(viewCull.screenPoint, 0f, store);
        viewCull.minX = Math.min(viewCull.minX, store.x);
        viewCull.maxX = Math.max(viewCull.maxX, store.x);
        viewCull.minY = Math.min(viewCull.minY, store.z);
        viewCull.maxY = Math.max(viewCull.maxY, store.z);
    }

    private void includeCullTile(float x, float y) {
        worldToTile(x, y, viewCull.tilePoint);
        viewCull.tileMinX = Math.min(viewCull.tileMinX, viewCull.tilePoint.getX());
        viewCull.tileMaxX = Math.max(viewCull.tileMaxX, viewCull.tilePoint.getX());
        viewCull.tileMinY = Math.min(viewCull.tileMinY, viewCull.tilePoint.getY());
        viewCull.tileMaxY = Math.max(viewCull.tileMaxY, viewCull.tilePoint.getY());
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
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

    /**
     * Returns the tracked spatial for a rendered layer entry.
     * <p>
     * Instanced tile layers do not have one spatial per tile; in that mode this
     * returns the layer node used as the tracked placeholder for the tile entry.
     * </p>
     *
     * @param layer the layer that owns {@code entry}
     * @param entry the layer, object, image, or tile entry to look up
     * @return the tracked spatial, or {@code null} when the entry is not rendered
     */
    public Spatial getSpatial(TiledLayer layer, TiledBase entry){
        RenderRef layerRef = getTrackedLayerRef(layer, false);
        if(layerRef==null){
            return null;
        }
        if(layer instanceof TiledImageLayer){
            return getTrackedSpatialRef(layerRef, layer,  null, null).sp;
        } else if (layer instanceof TiledTileLayer){
            if(entry instanceof TiledTileEntity){
                TiledTileEntity tile = (TiledTileEntity) entry;
                int tx = (int) tile.getX();
                int ty = (int) tile.getY();
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

    /**
     * Returns the maximum number of tileset sources packed into one instanced
     * geometry.
     *
     * @return the current texture slot limit
     */
    public int getMaxInstancedTilesetSlots() {
        return maxInstancedTilesetSlots;
    }

    /**
     * Sets the maximum number of tileset sources packed into one instanced
     * geometry. The current shader exposes four texture slots.
     *
     * @param maxInstancedTilesetSlots the slot limit, from 1 to 4
     */
    public void setMaxInstancedTilesetSlots(int maxInstancedTilesetSlots) {
        if (maxInstancedTilesetSlots < 1 || maxInstancedTilesetSlots > 4) {
            throw new IllegalArgumentException("Instanced tiled renderer supports 1 to 4 texture slots.");
        }
        this.maxInstancedTilesetSlots = maxInstancedTilesetSlots;
    }

    /**
     * Returns the orthogonal tile chunk size used by instanced batch culling.
     *
     * @return the chunk size in tile cells
     */
    public int getInstancedTileChunkSize() {
        return instancedBatchingPolicy.getTileChunkSize();
    }

    /**
     * Sets the orthogonal tile chunk size used by instanced batch culling.
     *
     * @param tileChunkSize the chunk size in tile cells
     */
    public void setInstancedTileChunkSize(int tileChunkSize) {
        instancedBatchingPolicy.setTileChunkSize(tileChunkSize);
    }

    /**
     * Returns how many isometric diagonals are grouped into one instanced tile batch.
     *
     * @return the diagonal span
     */
    public int getInstancedIsometricDiagonalSpan() {
        return instancedBatchingPolicy.getIsometricDiagonalSpan();
    }

    /**
     * Sets how many isometric diagonals are grouped into one instanced tile batch.
     *
     * @param isometricDiagonalSpan the diagonal span; values below 1 become 1
     */
    public void setInstancedIsometricDiagonalSpan(int isometricDiagonalSpan) {
        instancedBatchingPolicy.setIsometricDiagonalSpan(isometricDiagonalSpan);
    }

    /**
     * Returns the top-down object height range used by instanced object batches.
     *
     * @return the object batch height in map pixels
     */
    public int getInstancedObjectBatchHeight() {
        return instancedBatchingPolicy.getObjectBatchHeight();
    }

    /**
     * Sets the top-down object height range used by instanced object batches.
     *
     * @param objectBatchHeight the batch height in map pixels
     */
    public void setInstancedObjectBatchHeight(int objectBatchHeight) {
        instancedBatchingPolicy.setObjectBatchHeight(objectBatchHeight);
    }

    /**
     * Returns the fallback object batch size used for index-ordered object layers.
     *
     * @return the object batch size
     */
    public int getInstancedObjectBatchSize() {
        return instancedBatchingPolicy.getObjectBatchSize();
    }

    /**
     * Sets the fallback object batch size used for index-ordered object layers.
     *
     * @param objectBatchSize the batch size; values below 1 become 1
     */
    public void setInstancedObjectBatchSize(int objectBatchSize) {
        instancedBatchingPolicy.setObjectBatchSize(objectBatchSize);
    }

    /**
     * Returns how many delayed instanced compactions may run in one render frame.
     *
     * @return the per-frame compaction budget
     */
    public int getMaxInstancedRebatchesPerFrame() {
        return maxInstancedRebatchesPerFrame;
    }

    /**
     * Sets how many delayed instanced compactions may run in one render frame.
     *
     * @param maxInstancedRebatchesPerFrame the per-frame compaction budget
     */
    public void setMaxInstancedRebatchesPerFrame(int maxInstancedRebatchesPerFrame) {
        this.maxInstancedRebatchesPerFrame = Math.max(1, maxInstancedRebatchesPerFrame);
    }

    /**
     * Creates a grid cursor spatial matching this map orientation.
     *
     * @param material material to assign to the grid spatial
     * @return a grid spatial for one tile
     */
    public abstract Spatial createTileGrid(Material material);

    /**
     * @deprecated Rendering now requires a point of view. Use
     *             {@link #render(Listener, float, PovRenderer)}.
     *
     * @param listener the render listener
     * @param tpf time per frame
     */
    @Deprecated
    public void render(Listener listener, float tpf) {
        throw new IllegalArgumentException("A PovRenderer is required to render a tiled map.");
    }

    /**
     * Renders the tiled map for a specific point of view.
     *
     * @param listener the render listener
     * @param tpf time per frame
     * @param pov the point of view supplying scene and GUI viewports
     */
    public void render(Listener listener, float tpf, PovRenderer pov) {
        if (tiledMap == null) return;
        renderFrame++;
        instancedRebatchesThisFrame = 0;
        ViewPort viewPort = pov != null ? pov.getSceneViewPort() : null;
        Camera camera = viewPort != null ? viewPort.getCamera() : null;
        updateViewCull(camera);
        transientDebugRecords.clear();
        render(listener, tiledMap.getLayers(),tpf);
        updateBatchDebugOverlay();
    }

    /**
     * Returns whether instanced batch debug overlays are enabled.
     *
     * @return {@code true} when batch debug overlays are shown
     */
    public boolean isBatchDebugEnabled() {
        return batchDebugEnabled;
    }

    /**
     * Enables or disables instanced batch debug overlays.
     *
     * @param batchDebugEnabled {@code true} to show batch debug overlays
     */
    public void setBatchDebugEnabled(boolean batchDebugEnabled) {
        this.batchDebugEnabled = batchDebugEnabled;
        if (!batchDebugEnabled) {
            batchDebugNode.removeFromParent();
            batchDebugNode.detachAllChildren();
        }
    }


    /**
     * Receives callbacks before and after map, layer, and entity rendering.
     */
    public static interface Listener {
        /**
         * Called before the map starts rendering.
         */
        public void beforeMapRender(float tpf, TiledMap map);

        /**
         * Called after the map root visual has been updated.
         */
        public void afterMapRender(float tpf, TiledMap map, Spatial visual);

        /**
         * Called before an entity is rendered or updated in an instanced batch.
         */
        public void beforeEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry);

        /**
         * Called after an entity visual has been rendered or updated.
         */
        public void afterEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry,  Spatial visual);

        /**
         * Called before a layer is rendered.
         */
        public void beforeLayerRender(float tpf, TiledMap map, TiledLayer layer);

        /**
         * Called after a layer visual has been rendered or updated.
         */
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

    protected void visitTiles(TileVisitor visitor, ViewCull cull) {
        if (!cull.active) {
            visitTiles(visitor);
            return;
        }
        if (cull.empty) {
            return;
        }
        visitTiles((x, y, z) -> {
            if (cull.containsTile(x, y)) {
                visitor.visit(x, y, z);
            }
        });
    }


    protected Spatial renderTiles(Listener listener, TiledTileLayer layer, float tpf) {
        RenderRef ref = getTrackedLayerRef(layer, true);
        Node layerNode = (Node) ref.sp;
        RenderingMode renderingMode = instancedBatchingPolicy.resolve(layer, false, tiledMap.getOrientation());

        if (renderingMode != RenderingMode.MULTI_DRAW && canRenderTilesInstanced(layer)) {
            if (!ref.instancedTiles) {
                clearLayerNodeChildren(layerNode, layer);
                ref.clearTiles();
                ref.instancedBatches = null;
            }
            return renderTilesInstanced(listener, layer, tpf, ref, layerNode, renderingMode);
        }

        if (ref.instancedTiles) {
            clearLayerNodeChildren(layerNode, layer);
            ref.clearTiles();
            ref.instancedBatches = null;
            ref.instancedTiles = false;
        }
        ref.renderMode = renderingMode;

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
                    if (oldTile != null) {
                        if (oldTile.sp != null) {
                            oldTile.sp.removeFromParent();
                            entryMap.remove(oldTile.sp);
                        }
                        ref.setTile(index, null, null);
                    }
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

    private void clearLayerNodeChildren(Node layerNode, TiledLayer layer) {
        for (Spatial child : layerNode.getChildren()) {
            entryMap.remove(child);
        }
        layerNode.detachAllChildren();
        clearTransientState(layer);
    }

    private boolean canRenderTilesInstanced(TiledTileLayer layer) {
        final boolean[] supported = { true };
        visitTiles((x, y, z) -> {
            if (!supported[0]) {
                return;
            }
            Tile tile = layer.getTileAt(x, y).getTile();
            if (tile == null) {
                return;
            }
            Tileset tileset = tile.getTileset();
            if (tileset == null || tile.isAnimated() || hasUnsupportedInstancedFlip(tile) || !canRenderInstancedTile(tile)) {
                supported[0] = false;
            }
        });
        return supported[0];
    }

    private boolean hasUnsupportedInstancedFlip(Tile tile) {
        if (tiledMap.getOrientation() == Orientation.HEXAGONAL) {
            return tile.isFlippedAntiDiagonally() || tile.isRotatedHexagonal120();
        }
        return tile.isRotatedHexagonal120();
    }

    private boolean canRenderInstancedTile(Tile tile) {
        Tileset tileset = tile.getTileset();
        if (tileset.isImageBased()) {
            TiledImageEntity image = tileset.getImage();
            return image != null && image.getTexture() != null && image.getTrans() == null;
        }
        TiledImageEntity image = tile.getImage();
        return image != null
                && image.getTexture() != null
                && image.getTexture().getImage() != null
                && image.getTrans() == null
                && ImageRaster.isSupported(image.getTexture().getImage().getFormat());
    }

    private boolean canRenderObjectInstanced(TiledObjectEntity obj) {
        if (obj.getShape() != ObjectShape.TILE || !obj.isVisible()) {
            return false;
        }
        Tile tile = obj.getTile();
        return tile != null
                && tile.getTileset() != null
                && !tile.isAnimated()
                && !hasUnsupportedInstancedFlip(tile)
                && canRenderInstancedTile(tile);
    }

    private void applyObjectDecals(Material material, Tile tile) {
        material.clearParam(MaterialConst.DECAL_MAP);
        material.clearParam(MaterialConst.DECAL_IMAGE_SIZE);
        material.clearParam(MaterialConst.DECAL_TILE_SIZE);
        material.clearParam(MaterialConst.DECAL_0);
        material.clearParam(MaterialConst.DECAL_1);
        material.clearParam(MaterialConst.DECAL_2);
        material.clearParam(MaterialConst.DECAL_3);

        if (tile == null || tile.getCollisions() == null) {
            return;
        }

        Vector4f[] decals = new Vector4f[] {
                new Vector4f(-1f, 0f, 0f, 0f),
                new Vector4f(-1f, 0f, 0f, 0f),
                new Vector4f(-1f, 0f, 0f, 0f),
                new Vector4f(-1f, 0f, 0f, 0f)
        };
        InstancedTilesetSource decalSource = null;
        int layer = 0;
        for (TiledObjectEntity decalObject : tile.getCollisions().getObjects()) {
            if (layer >= DECAL_LAYERS) {
                break;
            }
            Object decalTileValue = decalObject.getProperty(DECAL_TILE_PROPERTY);
            if (decalTileValue == null) {
                continue;
            }

            String tilesetName = String.valueOf(decalObject.getPropertyOrDefault(
                    DECAL_TILESET_PROPERTY, DEFAULT_DECAL_TILESET)).trim();
            Tileset tileset = tiledMap.getTileset(tilesetName);
            if (tileset == null || !tileset.isImageBased()) {
                continue;
            }
            int decalTileId = safeInt(decalTileValue, -1);
            Tile decalTile = tileset.getTile(decalTileId);
            if (decalTile == null) {
                continue;
            }
            InstancedTilesetSource source = getInstancedTilesetSource(tileset);
            if (decalSource == null) {
                decalSource = source;
            } else if (decalSource != source) {
                continue;
            }

            float tileWidth = Math.max((float) tile.getWidth(), 1f);
            float tileHeight = Math.max((float) tile.getHeight(), 1f);
            float centerX = (float) ((decalObject.getX() + decalObject.getWidth() * 0.5) / tileWidth);
            float centerY = (float) ((decalObject.getY() + decalObject.getHeight() * 0.5) / tileHeight);
            centerX += safeFloat(decalObject.getProperty(DECAL_OFFSET_X_PROPERTY), 0f);
            centerY += safeFloat(decalObject.getProperty(DECAL_OFFSET_Y_PROPERTY), 0f);
            float defaultScale = (float) (Math.max(decalObject.getWidth(), decalObject.getHeight()) / tileWidth);
            float scale = safeFloat(decalObject.getProperty(DECAL_SCALE_PROPERTY),
                    safeFloat(decalObject.getProperty(DECAL_SIZE_PROPERTY), defaultScale * tileWidth) / tileWidth);
            decals[layer].set(decalTile.getId(), centerX, centerY, scale);
            layer++;
        }

        if (decalSource == null) {
            return;
        }

        material.setTexture(MaterialConst.DECAL_MAP, decalSource.texture);
        material.setVector2(MaterialConst.DECAL_IMAGE_SIZE,
                new Vector2f(decalSource.imageWidth, decalSource.imageHeight));
        material.setVector4(MaterialConst.DECAL_TILE_SIZE,
                new Vector4f(
                        decalSource.tileset.getTileWidth(),
                        decalSource.tileset.getTileHeight(),
                        decalSource.tileset.getMargin(),
                        decalSource.tileset.getSpacing()));
        material.setVector4(MaterialConst.DECAL_0, decals[0]);
        material.setVector4(MaterialConst.DECAL_1, decals[1]);
        material.setVector4(MaterialConst.DECAL_2, decals[2]);
        material.setVector4(MaterialConst.DECAL_3, decals[3]);
    }

    private int safeInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    private float safeFloat(Object value, float defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    private Spatial renderTilesInstanced(Listener listener, TiledTileLayer layer, float tpf, RenderRef ref, Node layerNode, RenderingMode renderingMode) {
        int layerUpdateId = layer.getUpdateId();
        int layerPropertyUpdateId = layer.getPropertiesUpdateId();
        boolean batchCulled = renderingMode == RenderingMode.INSTANCED_BATCH_CULLED;

        boolean layerUpdateNeeded = ref.isUpdateNeeded(layerUpdateId);
        boolean layerPropertiesUpdateNeeded = ref.isPropertiesUpdateNeeded(layerPropertyUpdateId);
        boolean visibleRangeChanged = !batchCulled && ref.visibleTileSignature != viewCull.signature;
        boolean renderingModeChanged = ref.renderMode != renderingMode;
        if (renderingModeChanged && ref.instancedBatches != null) {
            clearInstancedBatches(ref);
        }
        tickInstancedBatches(ref);
        final boolean[] rebuild = { layerUpdateNeeded || visibleRangeChanged || renderingModeChanged
                || !ref.instancedTiles || (batchCulled && hasLayerTransientCooldown(layer))
                || hasPendingBatchWork(ref) };

        try(TempVars vars = TempVars.get()){
            visitTilesForInstancing((x, y, z) -> {
                TiledTileEntity entry = layer.getTileAt(x, y);
                Tile tile = entry.getTile();
                int index = (int)((y - layer.getY()) * layer.getWidth() + (x - layer.getX()));
                RenderRef oldTile = ref.getTile(index);
                if (tile == null) {
                    if (oldTile != null) {
                        rebuild[0] = true;
                        ref.setTile(index, null, null);
                    }
                    return;
                }

                listener.beforeEntityRender(tpf,tiledMap, layer, entry);
                long tileUpdateId = (((long)layerUpdateId) << 32) | (layer.getUpdateIdAt(x, y) & 0xFFFFFFFFL);
                if (oldTile == null || oldTile.entry != tile || oldTile.isUpdateNeeded(tileUpdateId)) {
                    rebuild[0] = true;
                    ref.setTile(index, tile, layerNode);
                    ref.clearTileUpdateNeeded(index, tileUpdateId);
                }
                listener.afterEntityRender(tpf,tiledMap, layer, entry, layerNode);
            }, batchCulled);
        }

        if (rebuild[0]) {
            ArrayList<InstancedTileBatch> batches = ref.instancedBatches;
            if (batches == null) {
                batches = new ArrayList<>();
                ref.instancedBatches = batches;
            }
            final ArrayList<InstancedTileBatch> instancedBatches = batches;
            for (InstancedTileBatch batch : instancedBatches) {
                batch.beginUpdate();
            }

            final int isometricDiagonalSpan = batchCulled
                    ? instancedBatchingPolicy.resolveTileIsometricDiagonalSpan()
                    : 1;
            try(TempVars vars = TempVars.get()){
                visitTilesForInstancing((x, y, z) -> {
                    TiledTileEntity entry = layer.getTileAt(x, y);
                    Tile tile = entry.getTile();
                    if (tile == null) {
                        removeInstancedEntry(instancedBatches, entry);
                        clearTransientVisual(entry);
                        return;
                    }
                    Vector2f pixelCoord = vars.vect2d;
                    tileToWorldSpace(x, y, pixelCoord);
                    InstancedTilesetSource source = getInstancedTilesetSource(tile.getTileset());
                    int drawGroup = batchCulled
                            ? instancedBatchingPolicy.tileDrawGroup(x, y, z, pixelCoord.y,
                                    tiledMap.getOrientation(), width, isometricDiagonalSpan)
                            : 0;
                    float tileY = getWorldYIndex(layer, getTileYAxis(z));
                    boolean transientChange = false;
                    if (batchCulled) {
                        transientChange = updateTransientSignature(entry, drawGroup, source, pixelCoord.x,
                                tileY, pixelCoord.y, tile.getGid());
                    }
                    if (transientChange) {
                        removeInstancedEntry(instancedBatches, entry);
                    }
                    if (batchCulled && isTransient(entry)) {
                        removeInstancedEntry(instancedBatches, entry);
                        renderTransientTile(layerNode, layer, entry, tile, x, y, z);
                        addTransientDebug(layer, entry, tile, x, y, z);
                        return;
                    }
                    clearTransientVisual(entry);
                    InstancedTileBatch batch = getInstancedBatch(instancedBatches, source, layer.getName(), drawGroup, batchCulled);
                    batch.putTile(entry, tile, source, pixelCoord.x, tileY, pixelCoord.y);
                }, batchCulled);
            }
            if (batchCulled) {
                instancedBatches.sort(Comparator.comparingInt(batch -> batch.drawGroup));
            }

            for (InstancedTileBatch batch : instancedBatches) {
                batch.endUpdate(layerNode, layer);
            }
            if (batchCulled) {
                orderInstancedBatchGeometries(layerNode, instancedBatches);
            }
            ref.instancedTiles = true;
            ref.renderMode = renderingMode;
            ref.visibleTileSignature = viewCull.signature;
        } else if (layerPropertiesUpdateNeeded) {
            MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
            for (Spatial child : layerNode.getChildren()) {
                if (child instanceof Geometry) {
                    Geometry geometry = (Geometry) child;
                    materialFactory.setTintColor(geometry.getMaterial(), layer.getTintColor());
                    materialFactory.setLayerOpacity(geometry.getMaterial(), (float) layer.getOpacity());
                }
            }
        }

        if (layerPropertiesUpdateNeeded) {
            spriteFactory.applyProperties(layer, layerNode);
        }

        ref.clearUpdateNeeded(layerUpdateId);
        ref.clearPropertiesUpdateNeeded(layerPropertyUpdateId);
        return layerNode;
    }

    private void visitTilesForInstancing(TileVisitor visitor, boolean fullLayer) {
        if (fullLayer) {
            visitTiles(visitor);
        } else {
            visitTiles(visitor, viewCull);
        }
    }

    private InstancedTileBatch getInstancedBatch(ArrayList<InstancedTileBatch> batches, InstancedTilesetSource source, String layerName) {
        return getInstancedBatch(batches, source, layerName, 0, false);
    }

    private InstancedTileBatch getInstancedBatch(ArrayList<InstancedTileBatch> batches, InstancedTilesetSource source,
            String layerName, int drawGroup) {
        return getInstancedBatch(batches, source, layerName, drawGroup, false);
    }

    private InstancedTileBatch getInstancedBatch(ArrayList<InstancedTileBatch> batches, InstancedTilesetSource source,
            String layerName, int drawGroup, boolean delayedCompaction) {
        for (InstancedTileBatch batch : batches) {
            if (batch.drawGroup == drawGroup && batch.contains(source)) {
                return batch;
            }
        }
        for (InstancedTileBatch batch : batches) {
            if (batch.drawGroup == drawGroup && batch.canAdd(source)) {
                return batch;
            }
        }
        InstancedTileBatch batch = new InstancedTileBatch(this, layerName, drawGroup, delayedCompaction);
        batches.add(batch);
        return batch;
    }

    private void orderInstancedBatchGeometries(Node layerNode, ArrayList<InstancedTileBatch> batches) {
        int index = 0;
        for (InstancedTileBatch batch : batches) {
            if (batch.geometry == null || batch.geometry.getParent() != layerNode) {
                continue;
            }
            if (layerNode.getChildIndex(batch.geometry) != index) {
                batch.geometry.removeFromParent();
                layerNode.attachChildAt(batch.geometry, index);
            }
            index++;
        }
    }

    private void clearInstancedBatches(RenderRef ref) {
        if (ref.instancedBatches != null) {
            for (InstancedTileBatch batch : ref.instancedBatches) {
                if (batch.geometry != null) {
                    entryMap.remove(batch.geometry);
                    batch.geometry.removeFromParent();
                }
            }
        }
        ref.instancedBatches = null;
        ref.instancedTiles = false;
        ref.visibleTileSignature = Integer.MIN_VALUE;
        if (ref.entry instanceof TiledLayer) {
            clearTransientState((TiledLayer) ref.entry);
        }
    }

    private void clearTransientState(TiledLayer layer) {
        if (layer == null) {
            return;
        }
        Iterator<Entry<TiledBase, Spatial>> visuals = transientSpatials.entrySet().iterator();
        while (visuals.hasNext()) {
            Entry<TiledBase, Spatial> entry = visuals.next();
            if (belongsToLayer(entry.getKey(), layer)) {
                entry.getValue().removeFromParent();
                visuals.remove();
            }
        }
        transientCooldowns.keySet().removeIf(entry -> belongsToLayer(entry, layer));
        transientSignatures.keySet().removeIf(entry -> belongsToLayer(entry, layer));
        transientReasons.keySet().removeIf(entry -> belongsToLayer(entry, layer));
    }

    private boolean belongsToLayer(TiledBase entry, TiledLayer layer) {
        if (entry == layer) {
            return true;
        }
        if (entry instanceof TiledTileEntity) {
            return ((TiledTileEntity) entry).getContainer() == layer;
        }
        if (entry instanceof TiledObjectEntity) {
            return ((TiledObjectEntity) entry).getObjectGroup() == layer;
        }
        return false;
    }

    private boolean updateTransientSignature(TiledBase entry, int drawGroup,
            InstancedTilesetSource source, float x, float y, float z, int tileGid) {
        TransientSignature next = new TransientSignature(drawGroup, System.identityHashCode(source),
                Float.floatToIntBits(x), Float.floatToIntBits(y), Float.floatToIntBits(z), tileGid);
        TransientSignature old = transientSignatures.put(entry, next);
        if (old == null || old.equals(next)) {
            return false;
        }
        String reason = old.reason(next);
        transientReasons.put(entry, reason);
        transientCooldowns.put(entry, TRANSIENT_COOLDOWN_FRAMES);
        return true;
    }

    private boolean isTransient(TiledBase entry) {
        Integer cooldown = transientCooldowns.get(entry);
        if (cooldown == null) {
            return false;
        }
        if (cooldown <= 0) {
            transientCooldowns.remove(entry);
            transientReasons.remove(entry);
            return false;
        }
        transientCooldowns.put(entry, cooldown - 1);
        return true;
    }

    private void clearTransientVisual(TiledBase entry) {
        Spatial old = transientSpatials.remove(entry);
        if (old != null) {
            old.removeFromParent();
        }
    }

    private void renderTransientTile(Node layerNode, TiledTileLayer layer, TiledTileEntity entry, Tile tile, int x, int y, int z) {
        Spatial old = transientSpatials.get(entry);
        if (old != null) {
            old.removeFromParent();
        }

        MaterialFactory materialFactory = spriteFactory.getMaterialFactory();
        Geometry visual = spriteFactory.newTileSprite(tile, materialFactory.newMaterial());
        visual.setName("transientTile#" + x + "#" + y + "#" + tile.getName());

        Vector2f pixelCoord = tileToWorldSpace(x, y);
        visual.setLocalTranslation(pixelCoord.x, getTileYAxis(z), pixelCoord.y);

        Material mat = visual.getMaterial();
        materialFactory.setTile(mat, tile);
        materialFactory.setTintColor(mat, layer.getTintColor());
        materialFactory.setLayerOpacity(mat, (float) layer.getOpacity());
        spriteFactory.setAnimation(visual, tile);
        spriteFactory.applyProperties(tile, visual);

        transientSpatials.put(entry, visual);
        layerNode.attachChild(visual);
    }

    private void addTransientDebug(TiledObjectEntity obj) {
        Integer cooldown = transientCooldowns.get(obj);
        if (cooldown == null || cooldown <= 0 || !batchDebugEnabled) {
            return;
        }
        Vector2f world = gridToWorldSpace((float)obj.getX(), (float)obj.getY());
        TransientDebugRecord record = new TransientDebugRecord();
        record.x = world.x;
        record.z = world.y - Math.max((float)obj.getHeight(), 1f);
        record.y = getWorldYIndex(obj) + 0.3f;
        record.width = Math.max((float)obj.getWidth(), 1f);
        record.height = Math.max((float)obj.getHeight(), 1f);
        record.cooldown = cooldown;
        record.reason = transientReasons.get(obj);
        transientDebugRecords.add(record);
    }

    private void addTransientDebug(TiledTileLayer layer, TiledTileEntity entry, Tile tile, int x, int y, int z) {
        Integer cooldown = transientCooldowns.get(entry);
        if (cooldown == null || cooldown <= 0 || !batchDebugEnabled) {
            return;
        }
        Vector2f world = tileToWorldSpace(x, y);
        TransientDebugRecord record = new TransientDebugRecord();
        record.x = world.x;
        record.z = world.y - tile.getHeight();
        record.y = getWorldYIndex(layer, getTileYAxis(z)) + 0.3f;
        record.width = Math.max((float)tile.getWidth(), 1f);
        record.height = Math.max((float)tile.getHeight(), 1f);
        record.cooldown = cooldown;
        record.reason = transientReasons.get(entry);
        transientDebugRecords.add(record);
    }

    private void removeInstancedEntry(ArrayList<InstancedTileBatch> batches, TiledBase entry) {
        if (batches == null) {
            return;
        }
        for (InstancedTileBatch batch : batches) {
            if (batch.remove(entry)) {
                return;
            }
        }
    }

    private boolean hasPendingBatchWork(RenderRef ref) {
        if (ref.instancedBatches == null) {
            return false;
        }
        for (InstancedTileBatch batch : ref.instancedBatches) {
            if (batch.hasPendingWork()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLayerTransientCooldown(TiledTileLayer layer) {
        if (transientCooldowns.isEmpty()) {
            return false;
        }
        for (TiledBase entry : transientCooldowns.keySet()) {
            if (entry instanceof TiledTileEntity && ((TiledTileEntity) entry).getContainer() == layer) {
                return true;
            }
        }
        return false;
    }

    private void tickInstancedBatches(RenderRef ref) {
        if (ref.instancedBatches == null) {
            return;
        }
        for (InstancedTileBatch batch : ref.instancedBatches) {
            batch.tickFrame();
        }
    }

    boolean tryConsumeInstancedRebatch() {
        if (instancedRebatchesThisFrame >= maxInstancedRebatchesPerFrame) {
            return false;
        }
        instancedRebatchesThisFrame++;
        return true;
    }

    void logInstancedBatchChange(InstancedTileBatch batch, String reason) {
        if (!logger.isLoggable(Level.FINER)) {
            return;
        }
        logger.finer("Tiled instanced batch change"
                + " frame=" + renderFrame
                + " layer=" + batch.layerName
                + " drawGroup=" + batch.drawGroup
                + " reason=" + reason
                + " records=" + batch.records.size()
                + " tombstone=" + batch.tombstoneCount
                + " pending=" + batch.pendingInsertCount + "/" + batch.pendingRemoveCount
                + " frag=" + batch.fragmentation);
    }

    InstancedTilesetSource getInstancedTilesetSource(Tileset tileset) {
        InstancedTilesetSource source = instancedSourceCache.get(tileset);
        if (source != null) {
            return source;
        }

        source = new InstancedTilesetSource();
        source.tileset = tileset;
        source.imageBased = tileset.isImageBased();
        if (source.imageBased) {
            TiledImageEntity image = tileset.getImage();
            source.imageWidth = image.getWidth();
            source.imageHeight = image.getHeight();
            source.texture = image.getTexture();
        } else {
            ArrayList<Image> images = new ArrayList<>();
            int maxWidth = 1;
            int maxHeight = 1;
            ColorSpace colorSpace = ColorSpace.sRGB;
            for (Tile tile : tileset) {
                if (tile == null || tile.getImage() == null || tile.getImage().getTexture() == null) {
                    continue;
                }
                Image image = tile.getImage().getTexture().getImage();
                if (image == null || !ImageRaster.isSupported(image.getFormat())) {
                    continue;
                }
                maxWidth = Math.max(maxWidth, image.getWidth());
                maxHeight = Math.max(maxHeight, image.getHeight());
                colorSpace = image.getColorSpace();
            }
            for (Tile tile : tileset) {
                if (tile == null || tile.getImage() == null || tile.getImage().getTexture() == null) {
                    continue;
                }
                Image srcImage = tile.getImage().getTexture().getImage();
                if (srcImage == null || !ImageRaster.isSupported(srcImage.getFormat())) {
                    continue;
                }
                Image padded = new Image(Image.Format.RGBA8, maxWidth, maxHeight,
                        BufferUtils.createByteBuffer(maxWidth * maxHeight * 4), colorSpace);
                ImageRaster src = ImageRaster.create(srcImage);
                ImageRaster dst = ImageRaster.create(padded);
                ColorRGBA color = new ColorRGBA();
                for (int y = 0; y < srcImage.getHeight(); y++) {
                    for (int x = 0; x < srcImage.getWidth(); x++) {
                        dst.setPixel(x, y, src.getPixel(x, y, color));
                    }
                }
                source.collectionLayerByTileId.put(tile.getId(), images.size());
                images.add(padded);
            }
            source.imageWidth = maxWidth;
            source.imageHeight = maxHeight;
            source.textureArray = new TextureArray(images);
            source.textureArray.setWrap(Texture.WrapMode.EdgeClamp);
            source.textureArray.setMagFilter(Texture.MagFilter.Nearest);
        }
        instancedSourceCache.put(tileset, source);
        return source;
    }

    protected static class ViewCull {
        final Vector2f screenPoint = new Vector2f();
        final Point tilePoint = new Point();
        boolean active;
        boolean empty;
        float minX;
        float maxX;
        float minY;
        float maxY;
        int tileMinX;
        int tileMaxX;
        int tileMinY;
        int tileMaxY;
        int signature;

        void disable() {
            active = false;
            empty = false;
            signature = 0;
        }

        void setEmpty() {
            empty = true;
            tileMinX = 0;
            tileMaxX = -1;
            tileMinY = 0;
            tileMaxY = -1;
            updateSignature();
        }

        boolean containsTile(int x, int y) {
            return !empty && x >= tileMinX && x <= tileMaxX && y >= tileMinY && y <= tileMaxY;
        }

        boolean containsWorld(float x, float y, float padding) {
            return !active || (!empty
                    && x + padding >= minX && x - padding <= maxX
                    && y + padding >= minY && y - padding <= maxY);
        }

        void updateSignature() {
            int h = active ? 1 : 0;
            h = 31 * h + (empty ? 1 : 0);
            h = 31 * h + tileMinX;
            h = 31 * h + tileMaxX;
            h = 31 * h + tileMinY;
            h = 31 * h + tileMaxY;
            signature = h;
        }
    }

    private void updateBatchDebugOverlay() {
        if (!batchDebugEnabled || spriteFactory == null) {
            batchDebugNode.removeFromParent();
            batchDebugNode.detachAllChildren();
            return;
        }
        if (batchDebugNode.getParent() != rootNode) {
            rootNode.attachChild(batchDebugNode);
        }
        batchDebugNode.detachAllChildren();

        for (RenderRef ref : lrefs.values()) {
            if (ref.instancedBatches == null) {
                continue;
            }
            for (InstancedTileBatch batch : ref.instancedBatches) {
                if (batch.records.isEmpty()) {
                    continue;
                }
                if (batch.minX != Float.POSITIVE_INFINITY) {
                    float cooldown = batch.cooldownFrames;
                    float alpha = 0.08f + 0.22f * (1f - cooldown / (float)BATCH_COOLDOWN_FRAMES);
                    ColorRGBA color = debugColor(batch.drawGroup, alpha);
                    String name = "batch#" + batch.drawGroup + "#count" + batch.records.size()
                            + "#tomb" + batch.tombstoneCount
                            + "#pending" + batch.pendingInsertCount + "/" + batch.pendingRemoveCount
                            + "#frag" + Math.round(batch.fragmentation * 100f)
                            + "#dirty" + batch.lastDirtyFrame
                            + (batch.lastDirtyReason != null ? "#" + batch.lastDirtyReason : "");
                    addDebugRect(name, batch.minX, batch.minZ, batch.maxX - batch.minX,
                            Math.max(batch.maxZ - batch.minZ, 1f), batch.maxY + 0.15f, color);
                }
                batch.addTombstoneDebugRects();
            }
        }

        for (TransientDebugRecord record : transientDebugRecords) {
            float alpha = 0.18f + 0.22f * (record.cooldown / (float)TRANSIENT_COOLDOWN_FRAMES);
            addDebugRect("transient#" + (record.reason != null ? record.reason : "unknown"),
                    record.x, record.z, record.width, record.height,
                    record.y, transientDebugColor(record.reason, alpha));
        }
    }

    void addDebugRect(String name, float x, float z, float width, float height, float y, ColorRGBA color) {
        if (width <= 0f || height <= 0f) {
            return;
        }
        Material material = spriteFactory.getMaterialFactory().newMaterial();
        material.setColor(MaterialConst.COLOR, color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.getAdditionalRenderState().setDepthTest(false);
        material.getAdditionalRenderState().setDepthWrite(false);
        Geometry geometry = new Geometry(name, new Rect(width, height, true));
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Translucent);
        geometry.setLocalTranslation(x, y, z);
        batchDebugNode.attachChild(geometry);
    }

    private ColorRGBA debugColor(int index, float alpha) {
        switch (Math.floorMod(index, 8)) {
            case 0: return new ColorRGBA(0.1f, 0.7f, 1f, alpha);
            case 1: return new ColorRGBA(1f, 0.55f, 0.1f, alpha);
            case 2: return new ColorRGBA(0.45f, 1f, 0.25f, alpha);
            case 3: return new ColorRGBA(1f, 0.25f, 0.75f, alpha);
            case 4: return new ColorRGBA(0.7f, 0.45f, 1f, alpha);
            case 5: return new ColorRGBA(1f, 0.95f, 0.2f, alpha);
            case 6: return new ColorRGBA(0.15f, 1f, 0.75f, alpha);
            default: return new ColorRGBA(0.95f, 0.25f, 0.25f, alpha);
        }
    }

    private ColorRGBA transientDebugColor(String reason, float alpha) {
        if ("batch".equals(reason)) return new ColorRGBA(1f, 0.85f, 0.05f, alpha);
        if ("source".equals(reason)) return new ColorRGBA(0.1f, 0.95f, 1f, alpha);
        if ("position".equals(reason)) return new ColorRGBA(1f, 0.1f, 0.05f, alpha);
        return new ColorRGBA(1f, 0.45f, 0.05f, alpha);
    }

    private static final class TransientDebugRecord {
        float x;
        float z;
        float y;
        float width;
        float height;
        int cooldown;
        String reason;
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
        RenderingMode renderingMode = instancedBatchingPolicy.resolve(layer, true, tiledMap.getOrientation());
        boolean useInstancing = renderingMode != RenderingMode.MULTI_DRAW;
        boolean batchCulled = renderingMode == RenderingMode.INSTANCED_BATCH_CULLED;
        boolean renderingModeChanged = layerRef.renderMode != renderingMode;

        List<TiledObjectEntity> objects = layer.getObjects();
        int len = objects.size();
        ArrayList<InstancedTileBatch> objectBatches = null;
        if (useInstancing) {
            if (!layerRef.instancedTiles || renderingModeChanged) {
                clearLayerNodeChildren(layerNode, layer);
                layerRef.refs.clear();
                layerRef.instancedBatches = null;
            }
            objectBatches = layerRef.instancedBatches;
            if (objectBatches == null) {
                objectBatches = new ArrayList<>();
                layerRef.instancedBatches = objectBatches;
            }
            tickInstancedBatches(layerRef);
            for (InstancedTileBatch batch : objectBatches) {
                batch.beginUpdate();
            }
        } else if (layerRef.instancedBatches != null) {
            clearInstancedBatches(layerRef);
        }

        List<TiledObjectEntity> renderObjects = objects;
        if (!useInstancing) {
            tmpSortedObjects.clear();
            tmpSortedObjects.addAll(objects);
            tmpSortedObjects.sort(layer.getDrawOrder());
            renderObjects = tmpSortedObjects;
        }
        int objectBatchHeight = batchCulled ? instancedBatchingPolicy.resolveObjectBatchHeight() : 1;
        try(TempVars vars = TempVars.get()){
            for (int i = 0; i < len; i++) {
                TiledObjectEntity obj = renderObjects.get(i);
                
                listener.beforeEntityRender(tpf,tiledMap, layer, obj);

                int objectUpdateId = obj.getUpdateId();
                int objectPropertyUpdateId = obj.getPropertiesUpdateId();
                MaterialFactory materialFactory = spriteFactory.getMaterialFactory();

                if (useInstancing && obj.getShape() == ObjectShape.TILE && !obj.isVisible()) {
                    removeInstancedEntry(objectBatches, obj);
                    listener.afterEntityRender(tpf,tiledMap, layer, obj, layerNode);
                    continue;
                }

                if (useInstancing && canRenderObjectInstanced(obj)) {
                    Vector2f screenCoord = vars.vect2d;
                    gridToWorldSpace((float) obj.getX(), (float) obj.getY(), screenCoord);
                    Tile tile = obj.getTile();
                    float padding = Math.max((float) Math.max(obj.getWidth(), obj.getHeight()),
                            Math.max((float) tile.getWidth(), (float) tile.getHeight()));
                    if (renderingMode == RenderingMode.INSTANCED_CULLED
                            && !viewCull.containsWorld(screenCoord.x, screenCoord.y, padding)) {
                        removeInstancedEntry(objectBatches, obj);
                        clearTransientVisual(obj);
                        listener.afterEntityRender(tpf,tiledMap, layer, obj, layerNode);
                        continue;
                    }
                    InstancedTilesetSource source = getInstancedTilesetSource(tile.getTileset());
                    int drawGroup = batchCulled
                            ? instancedBatchingPolicy.objectDrawGroup(layer, obj, i, objectBatchHeight)
                            : 0;
                    float objectY = getWorldYIndex(obj);
                    boolean transientChange = false;
                    if (batchCulled) {
                        transientChange = updateTransientSignature(obj, drawGroup, source, screenCoord.x,
                                objectY, screenCoord.y, tile.getGid());
                    }
                    if (transientChange) {
                        removeInstancedEntry(objectBatches, obj);
                    }
                    if (batchCulled && isTransient(obj)) {
                        removeInstancedEntry(objectBatches, obj);
                    } else {
                        clearTransientVisual(obj);
                        InstancedTileBatch batch = getInstancedBatch(objectBatches, source, layer.getName(), drawGroup, batchCulled);
                        batch.putObject(obj, tile, source, screenCoord.x, objectY, screenCoord.y);
                        listener.afterEntityRender(tpf,tiledMap, layer, obj, layerNode);
                        continue;
                    }
                }
                if (useInstancing) {
                    removeInstancedEntry(objectBatches, obj);
                    if (canRenderObjectInstanced(obj)) {
                        addTransientDebug(obj);
                    }
                }

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
                        spatial.setLocalRotation(objectRotation.fromAngles(0, -radian, 0));
                    }

                    spriteFactory.setAnimation(spatial, obj);
                    
                    materialFactory.setMapObject(spatial.getMaterial(), obj);
                    applyObjectDecals(spatial.getMaterial(), obj.getTile());
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

        if (useInstancing) {
            if (batchCulled) {
                objectBatches.sort(Comparator.comparingInt(batch -> batch.drawGroup));
            }
            for (InstancedTileBatch batch : objectBatches) {
                batch.endUpdate(layerNode, layer);
            }
            if (batchCulled) {
                orderInstancedBatchGeometries(layerNode, objectBatches);
            }
            layerRef.instancedTiles = true;
        }

        layerRef.renderMode = renderingMode;
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

    /**
     * Marks every layer dirty so the next render pass refreshes cached spatials.
     */
    public void setUpdateNeeded() {
        for (TiledLayer layer : tiledMap.getLayers()) {
            layer.setUpdateNeeded();
        }
    }

    /**
     * Sets the Y-axis distance reserved for each map layer.
     *
     * @param layerDistance distance between layer depth ranges
     */
    public void setLayerDistance(double layerDistance) {
        this.layerDistance = layerDistance;
        this.coordinateSystem.setLayerDistance(layerDistance);
        this.step = layerDistance / (height * width);
        setUpdateNeeded();
    }

    /**
     * Sets the Y-axis gap inserted between adjacent layers.
     *
     * @param layerGap depth gap between layers
     */
    public void setLayerGap(double layerGap) {
        this.layerGap = layerGap;
        setUpdateNeeded();
    }

    /**
     * Returns the world Y offset for a layer index.
     *
     * @param index layer index in the flattened map layer list
     * @return world Y offset for the layer
     */
    public float getLayerYIndex(int index) {
        return (float) (index * (layerDistance + layerGap));
    }

    /**
     * Returns the world Y offset for a layer.
     *
     * @param layer the layer to locate
     * @return world Y offset, or 0 when the layer is absent
     */
    public float getLayerYIndex(TiledLayer layer) {
        if (layer == null) {
            return 0f;
        }
        int index = tiledMap.getLayersFlat().indexOf(layer);
        return index >= 0 ? getLayerYIndex(index) : 0f;
    }

    /**
     * Converts a layer-local draw-order Y value into world render Y.
     *
     * @param layer layer that owns the local Y value
     * @param layerLocalYIndex draw-order Y inside the layer
     * @return world render Y
     */
    public float getWorldYIndex(TiledLayer layer, float layerLocalYIndex) {
        return getLayerYIndex(layer) + layerLocalYIndex;
    }

    /**
     * Returns the world render Y for an object using this renderer's top-down
     * ordering function.
     *
     * @param object object to sort
     * @return world render Y, or 0 for {@code null}
     */
    public float getWorldYIndex(TiledObjectEntity object) {
        if (object == null) {
            return 0f;
        }
        return getWorldYIndex(object.getObjectGroup(), getTopDownYIndex(object));
    }

    public void gridToWorldSpace(float x, float y, Vector2f out) {
        coordinateSystem.gridToWorldSpace(x, y, out);
    }

    public Vector2f gridToWorldSpace(float x, float y) {
        return coordinateSystem.gridToWorldSpace(x, y);
    }

    public void gridToTile(float x, float y, Point out) {
        coordinateSystem.gridToTile(x, y, out);
    }

    public Point gridToTile(float x, float y) {
        return coordinateSystem.gridToTile(x, y);
    }

    public void tileToGridSpace(float x, float y, Vector2f out) {
        coordinateSystem.tileToGridSpace(x, y, out);
    }

    public Vector2f tileToGridSpace(float x, float y) {
        return coordinateSystem.tileToGridSpace(x, y);
    }

    public void tileToWorldSpace(float x, float y, Vector2f out) {
        coordinateSystem.tileToWorldSpace(x, y, out);
    }

    public Vector2f tileToWorldSpace(float x, float y) {
        return coordinateSystem.tileToWorldSpace(x, y);
    }

    public void worldToGridSpace(float x, float y, Vector2f out) {
        coordinateSystem.worldToGridSpace(x, y, out);
    }

    public Vector2f worldToGridSpace(float x, float y) {
        return coordinateSystem.worldToGridSpace(x, y);
    }

    public void worldToTile(float x, float y, Point out) {
        coordinateSystem.worldToTile(x, y, out);
    }

    public Point worldToTile(float x, float y) {
        return coordinateSystem.worldToTile(x, y);
    }

    public float getTopDownYIndex(TiledObjectEntity object) {
        return coordinateSystem.getTopDownYIndex(object);
    }

    public float getTopDownYIndex(float x, float y) {
        return coordinateSystem.getTopDownYIndex(x, y);
    }

    public void getCollisionCenterInGridSpace(TiledObjectEntity parentTileObject,
            TiledObjectEntity collisionObject, Vector2f out) {
        coordinateSystem.getCollisionCenterInGridSpace(parentTileObject, collisionObject, out);
    }

    public void getCenterInGridSpace(TiledBase entry, Vector2f out) {
        coordinateSystem.getCenterInGridSpace(entry, out);
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

    public void worldToPhysicsSpace(float x, float y, Vector2f out){
        coordinateSystem.worldToPhysicsSpace(x, y, out);
    }

    public Vec2 worldToPhysicsSpace(Vector2f worldCoords) {
        return coordinateSystem.worldToPhysicsSpace(worldCoords);
    }

    public void  physicsToWorldSpace(Vec2 physicsWorldCoords, Vector2f out){
        coordinateSystem.physicsToWorldSpace(physicsWorldCoords, out);
    }

    public Vector2f physicsToWorldSpace(Vec2 physicsWorldCoords) {
        return coordinateSystem.physicsToWorldSpace(physicsWorldCoords);
    }

    public void physicsToWorldSpace(float x, float y, Vector2f out){
        coordinateSystem.physicsToWorldSpace(x, y, out);
    }
}
