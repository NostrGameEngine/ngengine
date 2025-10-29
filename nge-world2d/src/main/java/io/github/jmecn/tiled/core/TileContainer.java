package io.github.jmecn.tiled.core;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public interface TileContainer {

    int getWidth();

    int getHeight();

    /**
     * Sets the tile at the specified position. Does nothing if (tx, ty) falls
     * outside of this layer.
     *
     * @param x
     *            x position of tile
     * @param y
     *            y position of tile
     * @param tile
     *            the tile object to place
     */
    void setTileAt(int x, int y, Tile tile);
    /**
     * Returns the tile at the specified position.
     *
     * @param x
     *            Tile-space x coordinate
     * @param y
     *            Tile-space y coordinate
     * @return tile at position (tx, ty) or <code>null</code> when (tx, ty) is
     *         outside this layer
     */
    Tile getTileAt(int x, int y);
}
