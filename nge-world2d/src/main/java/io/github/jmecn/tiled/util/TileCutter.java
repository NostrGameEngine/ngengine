package io.github.jmecn.tiled.util;

import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.math2d.Point;

public class TileCutter {
    
    private final int tileWidth;
    private final int tileHeight;
    private final int margin;
    private final int space;
    
    private final int imageWidth;
    private final int imageHeight;
    
    private int nextX;
    private int nextY;
    
    public TileCutter(int imageWidth, int imageHeight, int width, int height, int margin, int space) {
        this.tileWidth = width;
        this.tileHeight = height;
        this.margin = margin;
        this.space = space;
        
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        
        this.nextX = this.margin;
        this.nextY = this.margin;
    }

    public Tile getNextTile() {

        if (nextY + tileHeight + margin <= imageHeight) {

            Tile tile = new Tile(nextX, nextY, tileWidth, tileHeight);

            nextX += tileWidth + space;
            if (nextX + tileWidth + margin > imageWidth) {
                nextX = margin;
                nextY += tileHeight + space;
            }

            return tile;
        }

        return null;
    }

    /**
     * Get the number of columns in the image.
     * @return the number of columns
     */
    public int getColumns() {
        return (imageWidth - 2 * margin + space) / (tileWidth + space);
    }

    /**
     * Get the number of rows in the image.
     * @return the number of rows
     */
    public int getRows() {
        return (imageHeight - 2 * margin + space) / (tileHeight + space);
    }

    /**
     * Get the total number of tiles in the image.
     * @return the total number of tiles
     */
    public int getTileCount() {
        return getColumns() * getRows();
    }

    /**
     * Get the pixel coordinate of the tile in the image. index starts from 0, from left to right, top to bottom.
     * @param index the index of the tile
     * @return the pixel position of the tile
     */
    public Point getPixelCoord(int index) {
        int x = index % getColumns();
        int y = index / getColumns();
        return new Point(margin + x * (tileWidth + space), margin + y * (tileHeight + space));
    }
}