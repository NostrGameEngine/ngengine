package io.github.jmecn.tiled.core;

import io.github.jmecn.tiled.enums.Orientation;

/**
 * This element is only used in case of isometric orientation, and determines how tile overlays for terrain and collision information are rendered.
 *
 * @author yanmaoyuan
 */
public class TilesetGrid {
    /**
     * Orientation of the grid for the tiles in this tileset (orthogonal or isometric, defaults to orthogonal)
     */
    private Orientation orientation;
    // Width of a grid cell
    private int width;
    // Height of a grid cell
    private int height;

    public TilesetGrid(Orientation orientation, int width, int height) {
        this.orientation = orientation;
        this.width = width;
        this.height = height;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
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
}
