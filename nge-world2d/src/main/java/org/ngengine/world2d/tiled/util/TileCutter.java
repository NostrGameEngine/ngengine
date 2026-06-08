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

package org.ngengine.world2d.tiled.util;

import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.math2d.Point;

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