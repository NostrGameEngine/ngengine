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

package io.github.jmecn.tiled.core;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ngengine.components.ComponentManagerProvider;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;

import io.github.jmecn.tiled.components.TiledComponentManager;
import io.github.jmecn.tiled.components.TiledComponentReflectionMounting;
import io.github.jmecn.tiled.core.entity.TiledTileEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.core.tileset.Tileset;
import io.github.jmecn.tiled.enums.Orientation;
import io.github.jmecn.tiled.enums.RenderOrder;
import io.github.jmecn.tiled.enums.StaggerAxis;
import io.github.jmecn.tiled.enums.StaggerIndex;
import io.github.jmecn.tiled.util.CoordinateSystem;


/**
 * A map contains three different kinds of layers. Tile layers were once
 * the only type, and are simply called layer, object layers have the
 * objectgroup tag and image layers use the imagelayer tag. The order in
 * which these layers appear is the order in which the layers are
 * rendered by Tiled.
 * @author yanmaoyuan
 */
public class TiledMap extends TiledBase  implements ComponentManagerProvider{
  
    private TiledComponentManager componentManager;

    @Override
    public TiledComponentManager getComponentManager() {
        if(componentManager==null){
            componentManager = new TiledComponentManager();
            TiledComponentReflectionMounting.mountFromProperty(this, componentManager);
        }
        return componentManager;
    }



    private  final static Logger logger = Logger.getLogger(TiledMap.class.getName());

    /**
     * The TMX format version. Was “1.0” so far, and will be incremented to match minor Tiled releases.
     */
    private String version;

    /**
     * The Tiled version used to save the file (since Tiled 1.0.1). Maybe a date (for snapshot builds). (optional)
     */
    private String tiledVersion;

    /**
     * The class of this map (since 1.9, defaults to “”).
     */
    private String clazz;

    /**
     * Map orientation. Tiled supports "orthogonal", "isometric", "staggered"
     * (since 0.9) and "hexagonal" (since 0.11).
     */
    private Orientation orientation = Orientation.ORTHOGONAL;

    /**
     * The order in which tiles on tile layers are rendered. Valid values are
     * right-down (the default), right-up, left-down and left-up. In all cases,
     * the map is drawn row-by-row. (since 0.10, but only supported for
     * orthogonal maps at the moment)
     */
    private RenderOrder renderOrder = RenderOrder.RIGHT_DOWN;

    /**
     * The compression level to use for tile layer data (defaults to -1, which means to use the algorithm default).
     */
    private int compressionLevel;

    /**
     * The map width and height in tiles.
     */
    private int width;
    private int height;

    /**
     * The width and height of a tile.
     */
    private int tileWidth;
    private int tileHeight;

    /**
     * Only for hexagonal maps. Determines the width or height (depending on the
     * staggered axis) of the tile's edge, in pixels.
     */
    private int hexSideLength;

    /**
     * For staggered and hexagonal maps, determines which axis ("x" or "y") is
     * staggered. (since 0.11)
     */
    private StaggerAxis staggerAxis = StaggerAxis.Y;

    /**
     * For staggered and hexagonal maps, determines whether the "even" or "odd"
     * indexes along the staggered axis are shifted. (since 0.11)
     */
    private StaggerIndex staggerIndex = StaggerIndex.ODD;

    /**
     * X coordinate of the parallax origin in pixels (defaults to 0). (since 1.8)
     */
    private int parallaxOriginX;

    /**
     * Y coordinate of the parallax origin in pixels (defaults to 0). (since 1.8)
     */
    private int parallaxOriginY;

    /**
     * The background color of the map. (since 0.9, optional, may include alpha
     * value since 0.15 in the form #AARRGGBB)
     */
    private final ColorRGBA backgroundColor;

    /**
     * Stores the next available ID for new layers. This number is stored to
     * prevent reuse of the same ID after layers have been removed. (since 1.2)
     * (defaults to the highest layer id in the file + 1)
     */
    private int nextLayerId;

    /**
     * Stores the next available ID for new objects. This number is stored to
     * prevent reuse of the same ID after objects have been removed. (since
     * 0.11)
     */
    private int nextObjectId;

    /**
     * Whether this map is infinite. An infinite map has no fixed size and can
     * grow in all directions. Its layer data is stored in chunks.
     * (0 for false, 1 for true, defaults to 0)
     */
    private boolean infinite;

    private final TreeMap<Integer, Tileset> tilesetPerFirstGid;
    private final List<Tileset> tilesets;
    private final Map<String, Tileset> tilesetMap;
    private List<TiledLayer> layers;
    private Map<String, TiledLayer> layerMap;
    private final List<TiledObjectTemplate> templates;
    private final Map<String, TiledObjectTemplate> templateMap;

    /**
     * <p>
     * Constructor for Map.
     * </p>
     *
     * @param width
     *            the map width in tiles.
     * @param height
     *            the map height in tiles.
     */
    public TiledMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.hexSideLength = 0;
        this.nextObjectId = 0;
        this.backgroundColor = new ColorRGBA(0f, 0f, 0f, 0f);
        this.tilesets = new ArrayList<>();
        this.tilesetMap = new HashMap<>();
        this.layers = new SafeArrayList<>(TiledLayer.class);
        this.layerMap = new HashMap<>();
        this.templates = new SafeArrayList<>(TiledObjectTemplate.class);
        this.templateMap = new HashMap<>();

        // Load tilesets first, in case order is munged
        this.tilesetPerFirstGid = new TreeMap<>();
    }

    /**
     * Returns the total number of layers.
     *
     * @return the size of the layer
     */
    public int getLayerCount() {
        return layers.size();
    }

    /**
     * addLayer.
     *
     * @param layer a {@link TiledLayer} object.
     */
    public void addLayer(TiledLayer layer) {
        layer.setMap(this);
        layer.setIndex(layers.size());
        layers.add(layer);
        layerMap.put(layer.getName(), layer);
    }

    /**
     * setLayer
     *
     * @param index an int
     * @param layer a {@link TiledLayer} object
     */
    public void setLayer(int index, TiledLayer layer) {
        layer.setMap(this);
        layers.set(index, layer);
        layerMap.put(layer.getName(), layer);
        layer.setIndex(index);
    }

    /**
     * <p>
     * insertLayer.
     * </p>
     *
     * @param index an int.
     * @param layer a {@link TiledLayer} object.
     */
    public void insertLayer(int index, TiledLayer layer) {
        layer.setMap(this);
        layers.add(index, layer);
        layerMap.put(layer.getName(), layer);
        // reindex
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).setIndex(i);
        }
    }

    /**
     * Removes the layer at the specified index. Layers above this layer will
     * move down to fill the gap.
     *
     * @param index
     *            the index of the layer to be removed
     * @return the layer that was removed from the list
     */
    public TiledLayer removeLayer(int index) {
        TiledLayer layer = layers.remove(index);
        layer.setIndex(0);
        layerMap.remove(layer.getName());
        // reindex
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).setIndex(i);
        }
        return layer;
    }

    /**
     * Removes all layers from the plane.
     */
    public void removeAllLayers() {
        layers.clear();
        layerMap.clear();
    }

    /**
     * Returns the layer list.
     *
     * @return List the layer list
     */
    public List<TiledLayer> getLayers() {
        return layers;
    }

    /**
     * Returns the layer with the given name.
     *
     * @return Layer the layer with the given name
     */
    public Map<String, TiledLayer> getLayerMap() {
        return layerMap;
    }

    /**
     * Sets the layer list to the given java.util.List.
     *
     * @param layers the new set of layers
     */
    public void setLayers(List<TiledLayer> layers) {
        this.layers = layers;
        if (layers == null || layers.isEmpty()) {
            layerMap.clear();
        } else {
            this.layerMap = layers.stream().collect(Collectors.toMap(TiledLayer::getName, layer -> layer));
        }
    }

    /**
     * Returns the layer at the specified vector index.
     *
     * @param i the index of the layer to return
     * @return the layer at the specified index, or null if the index is out of
     *         bounds
     */
    public TiledLayer getLayer(int i) {
        try {
            return layers.get(i);
        } catch (IndexOutOfBoundsException e) {
            logger.log(Level.SEVERE, "can't find the layer with the index: "+ i, e);
        }
        return null;
    }

    /**
     * Returns the layer with the given name.
     *
     * @param name the name of the layer to return
     * @return the layer with the given name, or null if no layer with that name
     *         exists
     */
    public TiledLayer getLayer(String name) {
        return layerMap.get(name);
    }

    /**
     * Adds a Tileset to this Map. If the set is already attached to this map,
     * <code>addTileset</code> simply returns.
     *
     * @param tileset
     *            a tileset to add
     */
    public void addTileset(Tileset tileset) {
        if (tileset == null || tilesets.contains(tileset)) {
            return;
        }

        Tile t = tileset.getTile(0);

        if (t != null) {
            setFirstGidForTileset(tileset, tileset.getFirstGid());

            int tw = t.getWidth();
            int th = t.getHeight();
            if (tw != tileWidth && tileWidth == 0) {
                tileWidth = tw;
                tileHeight = th;
            }
        }

        tilesets.add(tileset);
        tilesetMap.put(tileset.getName(), tileset);
    }

    /**
     * Removes a {@link io.github.jmecn.tiled.core.tileset.Tileset} from the map, and removes any tiles
     * in the set from the map layers.
     *
     * @param tileset TileSet to remove
     */
    public void removeTileset(Tileset tileset) {
        // Sanity check
        final int tilesetIndex = tilesets.indexOf(tileset);
        if (tilesetIndex == -1) {
            return;
        }

        // Go through the map and remove any instances of the tiles in the set
        for (Tile tile : tileset) {
            for (TiledLayer ml : layers) {
                if (ml instanceof TiledTileLayer) {
                    for(int x = 0; x < ml.getWidth(); x++) {
                        for(int y = 0; y < ml.getHeight(); y++) {
                            TiledTileEntity te = ((TiledTileLayer) ml).getTileAt(x, y);
                            if ( Objects.equals(te.getTile(), tile)) {
                                ((TiledTileLayer) ml).placeTileAt(x, y, null);
                            }
                        }
                    }
                }
            }
        }

        tilesets.remove(tileset);
        tilesetMap.remove(tileset.getName());
    }

    /**
     * Returns a list with the currently loaded tileSets.
     *
     * @return List
     */
    public List<Tileset> getTileSets() {
        return tilesets;
    }

    /**
     * Returns the tileset with the given name.
     *
     * @return Tileset the tileset with the given name
     */
    public Map<String, Tileset> getTilesetMap() {
        return tilesetMap;
    }

    /**
     * Sets the tileset list to the given java.util.List.
     *
     * @param name the name of the tileset to return
     * @return the tileset with the given name, or null if no tileset with that
     */
    public Tileset getTileset(String name) {
        return tilesetMap.get(name);
    }

    /**
     * Get the tile set and its corresponding firstgid that matches the given
     * global tile id.
     *
     * @param gid
     *            a global tile id
     * @return the tileset containing the tile with the given global tile id, or
     *         <code>null</code> when no such tileset exists
     */
    private Entry<Integer, Tileset> findTileSetForTileGID(int gid) {
        return tilesetPerFirstGid.floorEntry(gid);
    }

    private void setFirstGidForTileset(Tileset tileset, int firstGid) {
        tilesetPerFirstGid.put(firstGid, tileset);
        tilesetMap.put(tileset.getName(), tileset);
    }

    /**
     * Helper method to set the tile based on its global id.
     *
     * @param tileContainer tile layer or chunk
     * @param x x-coordinate
     * @param y y-coordinate
     * @param tileId global id of the tile as read from the file
     */
    public void setTileAtFromTileId(TiledTileContainer tileContainer, int x, int y, int tileId) {
        // clear the flag
        int gid = tileId & ~Tile.FLIPPED_MASK;

        Tile tile = getTileForTileGID(gid);
        if (tile != null) {
            if (tile.getGid() != tileId) {
                Tile t = tile.copy();
                t.setGid(tileId);
                tileContainer.placeTileAt(x, y, t);
            } else {
                tileContainer.placeTileAt(x, y, tile);
            }
        }
    }

    /**
     * Helper method to get the tile based on its global id
     *
     * @param gid global id of the tile
     * @return <ul>
     *         <li>{@link Tile} object corresponding to the global id, if found</li>
     *         <li><code>null</code>, otherwise</li>
     *         </ul>
     */
    public Tile getTileForTileGID(final int gid) {

        Tile tile = null;
        Entry<Integer, Tileset> ts = findTileSetForTileGID(gid);
        if (ts != null) {
            tile = ts.getValue().getTile(gid - ts.getKey());
        }

        if (gid > 0 && tile == null) {
            logger.warning("Tile not found id: "+ gid);
            throw new IllegalArgumentException("Tile not found id:" + gid);
        }
        return tile;
    }

    public void addObjectTemplate(TiledObjectTemplate objectTemplate) {
        templates.add(objectTemplate);
        templateMap.put(objectTemplate.getSource(), objectTemplate);
    }

    public TiledObjectTemplate getObjectTemplate(String source) {
        return templateMap.get(source);
    }

    /**
     * Returns whether the given tile coordinates fall within the map
     * boundaries.
     *
     * @param x
     *            The tile-space x-coordinate
     * @param y
     *            The tile-space y-coordinate
     * @return <code>true</code> if the point is within the map boundaries,
     *         <code>false</code> otherwise
     */
    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTiledVersion() {
        return tiledVersion;
    }

    public void setTiledVersion(String tiledVersion) {
        this.tiledVersion = tiledVersion;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(String orient) {
        try {
            this.orientation = Orientation.fromString(orient);
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown orientation " + orientation);
        }
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public RenderOrder getRenderOrder() {
        return renderOrder;
    }

    public void setRenderOrder(RenderOrder renderOrder) {
        this.renderOrder = renderOrder;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    public int getHexSideLength() {
        return hexSideLength;
    }

    public void setHexSideLength(int hexSideLength) {
        this.hexSideLength = hexSideLength;
    }

    public StaggerAxis getStaggerAxis() {
        return staggerAxis;
    }

    public void setStaggerAxis(String staggerAxis) {
        try {
            this.staggerAxis = StaggerAxis.valueOf(staggerAxis.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown stagger axis " + staggerAxis);
        }
    }

    public void setStaggerAxis(StaggerAxis staggerAxis) {
        this.staggerAxis = staggerAxis;
    }

    public StaggerIndex getStaggerIndex() {
        return staggerIndex;
    }

    public void setStaggerIndex(String staggerIndex) {
        try {
            this.staggerIndex = StaggerIndex.valueOf(staggerIndex.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown stagger index " + staggerIndex);
        }
    }

    public void setStaggerIndex(StaggerIndex staggerIndex) {
        this.staggerIndex = staggerIndex;
    }

    public int getParallaxOriginX() {
        return parallaxOriginX;
    }

    public void setParallaxOriginX(int parallaxOriginX) {
        this.parallaxOriginX = parallaxOriginX;
    }

    public int getParallaxOriginY() {
        return parallaxOriginY;
    }

    public void setParallaxOriginY(int parallaxOriginY) {
        this.parallaxOriginY = parallaxOriginY;
    }

    public ColorRGBA getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(ColorRGBA backgroundColor) {
        this.backgroundColor.set(backgroundColor);
    }

    public int getNextLayerId() {
        return nextLayerId;
    }

    public void setNextLayerId(int nextLayerId) {
        this.nextLayerId = nextLayerId;
    }

    public int getNextObjectId() {
        return nextObjectId;
    }

    public void setNextObjectId(int nextObjectId) {
        this.nextObjectId = nextObjectId;
    }

    public boolean isInfinite() {
        return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
    }

    
    public void getLayersFlat(List<TiledLayer> layers, List<TiledLayer> out) {
        for(int i=0; i<layers.size(); i++){
            TiledLayer layer = layers.get(i);
            if (layer instanceof TiledLayerGroup) {
                TiledLayerGroup group = (TiledLayerGroup) layer;
                getLayersFlat(group.getLayers(), out);
                continue;
            }
            out.add(layer);
        }
    }

    protected List<TiledLayer> layersFlat = new ArrayList<>();
    protected List<TiledLayer> layersFlatRO = Collections.unmodifiableList(layersFlat);

    public List<TiledLayer> getLayersFlat() {
        layersFlat.clear();
        getLayersFlat(getLayers(), layersFlat);
        return layersFlatRO;
    }


    protected <T extends TiledLayer> T getLayerByName(List<TiledLayer> layers, String name, Class<T> layerType){
        for(TiledLayer l : layers){
            if(l instanceof TiledLayerGroup){
                TiledLayerGroup gl = (TiledLayerGroup) l;
                T layer = getLayerByName(gl.getLayers(), name, layerType);
                if(layer!=null){
                    return layer;
                }
            }
            if(l.getName().equals(name) && layerType.isInstance(l)){
                return layerType.cast(l);
            }
        }
        return null;
    }

    public <T extends TiledLayer> T getLayerByName(String name, Class<T> layerType){
        return getLayerByName(getLayers(), name, layerType);
    }

   
}
