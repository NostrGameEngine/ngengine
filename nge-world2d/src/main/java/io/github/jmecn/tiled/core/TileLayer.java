package io.github.jmecn.tiled.core;

import java.util.*;

import io.github.jmecn.tiled.math2d.Point;

/**
 * A TileLayer is a specialized MapLayer, used for tracking two-dimensional tile
 * data.
 * 
 * @author yanmaoyuan
 */
public class TileLayer extends Layer implements TileContainer {

    private Tile[][] tiles;
    

    protected HashMap<Object, Properties> tileInstanceProperties = new HashMap<>();

    private List<Chunk> chunks;

    /**
     * Construct a TileLayer from the given width and height.
     * 
     * @param w
     *            width in tiles
     * @param h
     *            height in tiles
     */
    public TileLayer(int w, int h) {
        super(w, h);

        tiles = new Tile[height][width];
        chunks = new ArrayList<>();
    }

    /**
     * @param m
     *            the map this layer is part of
     */
    public TileLayer(TiledMap m) {
        setMap(m);
    }

    /**
     * <p>
     * Constructor for TileLayer.
     * </p>
     * 
     * @param m
     *            the map this layer is part of
     * @param w
     *            width in tiles
     * @param h
     *            height in tiles
     */
    public TileLayer(TiledMap m, int w, int h) {
        super(w, h);
        setMap(m);
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

    /**
     * Removes any occurences of the given tile from this map layer. If layer is
     * locked, an exception is thrown.
     * 
     * @param tile
     *            the Tile to be removed
     */
    public void removeTile(Tile tile) {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (tiles[y][x] == tile) {
                    setTileAt(x + this.x, y + this.y, null);
                }
            }
        }
    }

    /**
     * Sets the tile at the specified position. Does nothing if (tx, ty) falls
     * outside of this layer.
     * 
     * @param tx
     *            x position of tile
     * @param ty
     *            y position of tile
     * @param ti
     *            the tile object to place
     */
    @Override
    public void setTileAt(int tx, int ty, Tile ti) {
        if (contains(tx, ty)) {
            tiles[ty - y][tx - x] = ti;           
            // tell map renderer to update it
            ti.setX(tx);
            ti.setY(ty);
        }
    }

    /**
     * Returns the tile at the specified position.
     * 
     * @param tx
     *            Tile-space x coordinate
     * @param ty
     *            Tile-space y coordinate
     * @return tile at position (tx, ty) or <code>null</code> when (tx, ty) is
     *         outside this layer
     */
    @Override
    public Tile getTileAt(int tx, int ty) {
        return (contains(tx, ty)) ? tiles[ty - y][tx - x] : null;
    }

    /**
     * Returns the first occurrence (using top down, left to right search) of
     * the given tile.
     * 
     * @param t the {@link io.github.jmecn.tiled.core.Tile} to look for
     * @return A {@link com.jme3.math.Vector2f} instance of the first instance of t, or <code>null</code> if it is not found
     */
    public Point locationOf(Tile t) {
        for (int y = this.y; y < this.height + this.y; y++) {
            for (int x = this.x; x < this.width + this.x; x++) {
                if (getTileAt(x, y) == t) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    /**
     * Replaces all occurrences of the Tile <code>find</code> with the Tile
     * <code>replace</code> in the entire layer
     * 
     * @param find
     *            the tile to replace
     * @param replace
     *            the replacement tile
     */
    public void replaceTile(Tile find, Tile replace) {
        for (int y = this.y; y < this.y + this.height; y++) {
            for (int x = this.x; x < this.x + this.width; x++) {
                if (getTileAt(x, y) == find) {
                    setTileAt(x, y, replace);
                }
            }
        }
    }

    /**
     * <p>
     * getTileInstancePropertiesAt.
     * </p>
     * 
     * @param x
     *            a int.
     * @param y
     *            a int.
     * @return a {@link java.util.Properties} object.
     */
    public Properties getTileInstancePropertiesAt(int x, int y) {
        if (!contains(x, y)) {
            return new Properties();
        }
        Object key = new Point(x, y);
        return tileInstanceProperties.get(key);
    }

    /**
     * <p>
     * setTileInstancePropertiesAt.
     * </p>
     * 
     * @param x
     *            a int.
     * @param y
     *            a int.
     * @param tip
     *            a {@link java.util.Properties} object.
     */
    public void setTileInstancePropertiesAt(int x, int y, Properties tip) {
        if (contains(x, y)) {
            Object key = new Point(x, y);
            tileInstanceProperties.put(key, tip);
        }
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public void addChunk(Chunk chunk) {
        chunks.add(chunk);
    }
}
