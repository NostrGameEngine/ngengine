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

package org.ngengine.world2d.tiled.core.tileset;

import java.util.*;
import java.util.logging.Logger;

import com.jme3.math.Vector2f;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledTerrain;
import org.ngengine.world2d.tiled.core.TiledTransformations;
import org.ngengine.world2d.tiled.core.entity.TiledImageEntity;
import org.ngengine.world2d.tiled.enums.FillMode;
import org.ngengine.world2d.tiled.enums.ObjectAlignment;
import org.ngengine.world2d.tiled.enums.TileRenderSize;

/**
 * If there are multiple &lt;tileset&gt; elements, they are in ascending order of
 * their firstgid attribute. The first tileset always has a firstgid value of 1.
 * Since Tiled 0.15, image collection tilesets do not necessarily number their
 * tiles consecutively since gaps can occur when removing tiles.
 * <p>Can contain at most one:</p>
 *   &lt;image&gt;, &lt;tileoffset&gt;, &lt;grid&gt; (since 1.0),
 *   &lt;properties&gt;, &lt;terraintypes&gt;, &lt;wangsets&gt; (since 1.1),
 *   &lt;transformations&gt; (since 1.5)
 * <p>Can contain any number: &lt;tile&gt;</p>
 *
 * @author yanmaoyuan
 * 
 */
public class Tileset extends TiledBase implements Iterable<Tile> {

    static Logger logger = Logger.getLogger(Tileset.class.getName());

    /**
     * The first global tile ID of this tileset (this global ID maps to the
     * first tile in this tileset).
     */
    private int firstGid;

    /**
     * If this tileset is stored in an external TSX (Tile Set XML) file, this
     * attribute refers to that file. That TSX file has the same structure as
     * the &lt;tileset> element described here. (There is the firstgid attribute
     * missing and this source attribute is also not there. These two attributes
     * are kept in the TMX map, since they are map specific.)
     */
    private String source;

    /**
     * The name of this tileset.
     */
    private String name;

    /**
     * The class of this tileset (since 1.9, defaults to “”).
     */
    private String clazz;

    /**
     * The (maximum) width and height of the tiles in this tileset.
     * Irrelevant for image collection tilesets, but stores the maximum
     * tile width and height.
     */
    private int tileWidth;
    private int tileHeight;

    /**
     * The spacing in pixels between the tiles in this tileset (applies to the
     * tileset image, defaults to 0). Irrelevant for image collection tilesets.
     */
    private int spacing;

    /**
     * The margin around the tiles in this tileset (applies to the tileset
     * image, defaults to 0). Irrelevant for image collection tilesets.
     */
    private int margin;

    /**
     * The number of tiles in this tileset (since 0.13). Note that there can
     * be tiles with a higher ID than the tile count, in case the tileset is
     * an image collection from which tiles have been removed.
     */
    private int tileCount;

    /**
     * The number of tile columns in the tileset. For image collection tilesets
     * it is editable and is used when displaying the tileset. (since 0.15)
     */
    private int columns;

    /**
     * Controls the alignment for tile objects. Valid values are "unspecified",
     * "topleft", "top", "topright", "left", "center", "right", "bottomleft",
     * "bottom" and "bottomright". The default value is "unspecified", for
     * compatibility reasons. When "unspecified", tile objects use "bottomleft"
     * in orthogonal mode and "bottom" in isometric mode. (since 1.4)
     */
    private ObjectAlignment objectAlignment;

    /**
     * The size to use when rendering tiles from this tileset on a tile layer.
     * Valid values are "tile" (the default) and "grid". When set to "grid",
     * the tile is drawn at the tile grid size of the map. (since 1.9)
     */
    private TileRenderSize tileRenderSize;

    /**
     * The fill mode to use when rendering tiles from this tileset. Valid values
     * are "stretch" (the default) and "preserve-aspect-fit". Only relevant when
     * the tiles are not rendered at their native size, so this applies to resized
     * tile objects or in combination with tilerendersize set to "grid". (since 1.9)
     */
    private FillMode fillMode;

    // This element is used to specify an offset in pixels, to be applied when drawing a tile from the related tileset. When not present, no offset is applied.
    private final Vector2f tileOffset = new Vector2f(0, 0);

    private TilesetGrid grid;

    // tileset image, in case image-based tileset
    private TiledImageEntity image;
    private String imageSource;

    private TiledTransformations transformations;

    /**
     * This element defines an array of terrain types, which can be referenced
     * from the terrain attribute of the tile element.
     */
    private List<TiledTerrain> terrains = new ArrayList<>();

    private List<WangSet> wangSets = new ArrayList<>();

    private List<Tile> tiles = new ArrayList<>();
    private final Map<Integer, Tile> idTile = new TreeMap<>();

    /**
     * Default constructor
     */
    public Tileset() {
        this.tileWidth = 32;
        this.tileHeight = 32;
        this.spacing = 0;
        this.margin = 0;
    }

    public Tileset(int width, int height, int space, int margin) {
        this.tileWidth = width;
        this.tileHeight = height;
        this.spacing = space;
        this.margin = margin;
    }

    /**
     * Get the first global tile ID of this tileset.
     * @return The first global tile ID of this tileset.
     */
    public int getFirstGid() {
        return firstGid;
    }

    /**
     * Set the first global tile ID of this tileset.
     * @param firstGid The first global tile ID of this tileset.
     */
    public void setFirstGid(int firstGid) {
        this.firstGid = firstGid;
    }

    /**
     * Update the first global tile ID of this tileset. Also update the global tile ID of all tiles in this tileset.
     * @param firstGid The first global tile ID of this tileset.
     */
    public void updateFirstGid(int firstGid) {
        this.firstGid = firstGid;
        for (Tile t : tiles) {
            t.setGid(firstGid + t.getId());
        }
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getSpacing() {
        return spacing;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public int getTileCount() {
        return tileCount;
    }

    public void setTileCount(int tileCount) {
        this.tileCount = tileCount;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public TiledImageEntity getImage() {
        return image;
    }

    public void setImage(TiledImageEntity image) {
        this.image = image;
    }

    public String getImageSource() {
        return imageSource;
    }

    public void setImageSource(String imageSource) {
        this.imageSource = imageSource;
    }

    public List<TiledTerrain> getTerrains() {
        return terrains;
    }

    public void setTerrains(List<TiledTerrain> terrains) {
        this.terrains = terrains;
    }

    public void addTerrain(TiledTerrain terrain) {
        terrain.setId(terrains.size());
        terrains.add(terrain);
    }

    public TiledTerrain getTerrain(int id) {
        return terrains.get(id);
    }

    public List<WangSet> getWangSets() {
        return wangSets;
    }
    public void setWangSets(List<WangSet> wangSets) {
        this.wangSets = wangSets;
    }
    public void addWangSet(WangSet wangSet) {
        wangSet.setId(wangSets.size());
        wangSet.setTileset(this);
        wangSets.add(wangSet);
    }

    public WangSet getWangSet(int id) {
        return wangSets.get(id);
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public void setTiles(List<Tile> tiles) {
        this.tiles = tiles;
    }

    /**
     * Adds the tile to the set, setting the id of the tile only if the current
     * value of id is -1.
     *
     * @param t
     *            the tile to add
     * @return int The <b>local</b> id of the tile
     */
    public int addTile(Tile t) {
        if (t.getId() < 0) {
            t.setId(tiles.size());
        }

        if (t.getGid() <= 0) {
            t.setGid(firstGid + t.getId());
        }

        if (tileWidth < t.getWidth()) {
            tileWidth = t.getWidth();
        }

        if (tileHeight < t.getHeight()) {
            tileHeight = t.getHeight();
        }

        tiles.add(t);
        t.setTileset(this);

        idTile.put(t.getId(), t);
        return t.getId();
    }

    /**
     * This method takes a new Tile object as argument, and in addition to the
     * functionality of <code>addTile()</code>, sets the id of the tile to -1.
     *
     * @see Tileset#addTile(Tile)
     * @param t
     *            the new tile to add.
     */
    public void addNewTile(Tile t) {
        t.setId(-1);
        addTile(t);
    }

    /**
     * Removes a tile from this tileset. Does not invalidate other tile indices.
     * Removal is simply setting the reference at the specified index to
     * <b>null</b>.
     *
     * @param i the index to remove
     */
    public void removeTile(int i) {
        tiles.set(i, null);
        idTile.remove(i);
    }

    /**
     * Returns the amount of tiles in this tileset.
     *
     * @return the amount of tiles in this tileset
     * @since 0.13
     */
    public int size() {
        return tiles.size();
    }

    /**
     * Returns the maximum tile id.
     *
     * @return the maximum tile id, or -1 when there are no tiles
     */
    public int getMaxTileId() {
        return tiles.size() - 1;
    }

    /**
     * Gets the tile with <b>local</b> id <code>i</code>.
     *
     * @param i
     *            local id of tile
     * @return A tile with local id <code>i</code> or <code>null</code> if no
     *         tile exists with that id
     */
    public Tile getTile(int i) {
        try {
            return idTile.get(i);
        } catch (IndexOutOfBoundsException a) {
            logger.warning("Tileset.getTile(): No tile with id " + i + " exists.");
        }
        return null;
    }

    /**
     * Returns the first non-null tile in the set.
     *
     * @return The first tile in this tileset, or <code>null</code> if none exists.
     */
    public Tile getFirstTile() {
        Tile ret = null;
        int i = 0;
        while (ret == null && i <= getMaxTileId()) {
            ret = getTile(i);
            i++;
        }
        return ret;
    }

    /**
     * Returns whether the tileset is derived from a tileset image.
     *
     * @see <a href="https://doc.mapeditor.org/en/stable/manual/editing-tilesets/#two-types-of-tileset">Two Types of Tileset</a>
     * @return <code>true</code> if the tileset is image-based, <code>false</code> if it is collection of images.
     */
    public boolean isImageBased() {
        return image != null;
    }

    @Override
    public Iterator<Tile> iterator() {
        return tiles.iterator();
    }

    /**
     * Set the tile offset for the tiles in this tileset.
     *
     * @param x Horizontal offset in pixels. (defaults to 0)
     * @param y Vertical offset in pixels (positive is down, defaults to 0)
     */
    public void setTileOffset(int x, int y) {
        this.tileOffset.set(x, y);
    }

    /**
     * @return Horizontal offset in pixels. (defaults to 0)
     */
    public Vector2f getTileOffset() {
        return tileOffset;
    }

    /**
     * @return The class of this tileset (since 1.9, defaults to “”).
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * @param clazz The class of this tileset (since 1.9, defaults to “”).
     */
    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public ObjectAlignment getObjectAlignment() {
        return objectAlignment;
    }

    public void setObjectAlignment(ObjectAlignment objectAlignment) {
        this.objectAlignment = objectAlignment;
    }

    public void setObjectAlignment(String objectAlignment) {
        this.objectAlignment = ObjectAlignment.fromString(objectAlignment);
    }

    public TileRenderSize getTileRenderSize() {
        return tileRenderSize;
    }

    public void setTileRenderSize(TileRenderSize tileRenderSize) {
        this.tileRenderSize = tileRenderSize;
    }

    public void setTileRenderSize(String tileRenderSize) {
        this.tileRenderSize = TileRenderSize.fromString(tileRenderSize);
    }

    public FillMode getFillMode() {
        return fillMode;
    }

    public void setFillMode(FillMode fillMode) {
        this.fillMode = fillMode;
    }

    public void setFillMode(String fillMode) {
        this.fillMode = FillMode.fromString(fillMode);
    }

    public boolean hasGrid() {
        return this.grid != null;
    }

    /**
     * Set the grid for the tiles in this tileset.
     * @param grid The grid for the tiles in this tileset.
     */
    public void setGrid(TilesetGrid grid) {
        this.grid = grid;
    }

    /**
     * Get the grid for the tiles in this tileset.
     * @return The grid for the tiles in this tileset.
     */
    public TilesetGrid getGrid() {
        return this.grid;
    }

    public TiledTransformations getTransformations() {
        return this.transformations;
    }

    public void setTransformations(TiledTransformations transformations) {
        this.transformations = transformations;
    }

    public Tile findByClass(String clazz) {
        for (Tile t : tiles) {
            if (t.getClazz() != null && t.getClazz().equals(clazz)) {
                return t;
            }
        }
        return null;
    }

    public void findAllByClass(String clazz, List<Tile> out) {
        for (Tile t : tiles) {
            if (t.getClazz() != null && t.getClazz().equals(clazz)) {
                out.add(t);
            }
        }
    }

    public Tile findByTag(String tag) {
        for (Tile t : tiles) {
            String tags = (String) t.getProperty("tag");
            if(tags==null) tags = (String) t.getProperty("tags");
            if(tags==null) continue;
            String[] tagArray = tags.split(",");
            for (String ttag : tagArray) {
                if (ttag.trim().equals(tag)) {
                    return t;
                }
            }
        }
        return null;
    }

    public Tile findAllByTag(String tag, List<Tile> out) {
        for (Tile t : tiles) {
            String tags = (String) t.getProperty("tag");
            if(tags==null) tags = (String) t.getProperty("tags");
            if(tags==null) continue;
            String[] tagArray = tags.split(",");
            for (String ttag : tagArray) {
                if (ttag.trim().equals(tag)) {
                    out.add(t);
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Tileset{" +
                "firstGid=" + firstGid +
                ", source='" + source + '\'' +
                ", name='" + name + '\'' +
                ", tileWidth=" + tileWidth +
                ", tileHeight=" + tileHeight +
                ", spacing=" + spacing +
                ", margin=" + margin +
                ", tileCount=" + tileCount +
                ", columns=" + columns +
                ", tiles=" + tiles.size() +
                '}';
    }
}
