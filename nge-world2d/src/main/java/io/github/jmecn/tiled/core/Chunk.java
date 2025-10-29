package io.github.jmecn.tiled.core;

import io.github.jmecn.tiled.math2d.Point;

/**
 * This is currently added only for infinite maps. The contents of a chunk element
 * is same as that of the data element, except it stores the data of the area
 * specified in the attributes.
 *
 * @author yanmaoyuan
 */
public class Chunk implements TileContainer {
    /**
     * The x coordinate of the chunk in tiles.
     */
    private final int x;
    /**
     * The y coordinate of the chunk in tiles.
     */
    private final int y;
    /**
     * The width of the chunk in tiles.
     */
    private final int width;
    /**
     * The height of the chunk in tiles.
     */
    private final int height;

    /**
     * The data stored in the chunk. Format is the same as data.
     */
    private final Tile[][] map;
    private final boolean[][] needUpdateSpatial;

    private boolean isNeedUpdate;

    public Chunk(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.map = new Tile[height][width];
        this.needUpdateSpatial = new boolean[height][width];
        this.isNeedUpdate = true;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Removes any occurences of the given tile from this map layer. If layer is
     * locked, an exception is thrown.
     *
     * @param tile
     *            the Tile to be removed
     */
    public void removeTile(Tile tile) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (map[row][col] == tile) {
                    setTileAt(col, row, null);
                }
            }
        }
    }

    /**
     * Returns whether the given tile coordinates fall within the map boundaries.
     *
     * @param x The tile-space x-coordinate
     * @param y The tile-space y-coordinate
     * @return <code>true</code> if the point is within the map boundaries, <code>false</code> otherwise
     */
    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Sets the tile at the specified position. Does nothing if (x, y) falls
     * outside of this layer.
     *
     * @param x x position of tile
     * @param y y position of tile
     * @param tile the tile object to place
     */
    @Override
    public void setTileAt(int x, int y, Tile tile) {
        if (contains(x, y)) {
            map[y][x] = tile;
            needUpdateSpatial[y][x] = true;

            // tell map renderer to update it
            setNeedUpdated(true);
        }
    }

    /**
     * Returns the tile at the specified position.
     *
     * @param x Tile-space x coordinate
     * @param y Tile-space y coordinate
     * @return tile at position (x, y) or <code>null</code> when (x, y) is outside this layer
     */
    @Override
    public Tile getTileAt(int x, int y) {
        return (contains(x, y)) ? map[y][x] : null;
    }

    /**
     * Returns the first occurrence (using top down, left to right search) of
     * the given tile.
     *
     * @param tile the {@link io.github.jmecn.tiled.core.Tile} to look for
     * @return A {@link com.jme3.math.Vector2f} instance of the first tile, or <code>null</code> if it is not found
     */
    public Point locationOf(Tile tile) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (getTileAt(col, row) == tile) {
                    return new Point(col, row);
                }
            }
        }
        return null;
    }

    /**
     * Replaces all occurrences of the Tile <code>find</code> with the Tile
     * <code>replace</code> in the entire layer
     *
     * @param find the tile to replace
     * @param replace the replacement tile
     */
    public void replaceTile(Tile find, Tile replace) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (getTileAt(col, row) == find) {
                    setTileAt(col, row, replace);
                }
            }
        }
    }

    /**
     * Tell if the spatial at position(x, y) should be updated.
     *
     * @param x Tile-space x coordinate
     * @param y Tile-space y coordinate
     *
     * @return true if the spatial should be updated.
     */
    public boolean isNeedUpdateAt(int x, int y) {
        return contains(x, y) && needUpdateSpatial[y][x];
    }

    /**
     * Set the needUpdate flag at position(x, y).
     * @param x Tile-space x coordinate
     * @param y Tile-space y coordinate
     * @param needUpdate true if the spatial should be updated.
     */
    public void setNeedUpdateAt(int x, int y, boolean needUpdate) {
        if (contains(x, y)) {
            needUpdateSpatial[y][x] = needUpdate;
        }
    }

    /**
     * @return true if the spatial should be updated.
     */
    public boolean isNeedUpdated() {
        return isNeedUpdate;
    }

    /**
     * Set the needUpdate flag.
     * @param isSpatialUpdated true if the spatial should be updated.
     */
    public void setNeedUpdated(boolean isSpatialUpdated) {
        this.isNeedUpdate = isSpatialUpdated;
    }

}