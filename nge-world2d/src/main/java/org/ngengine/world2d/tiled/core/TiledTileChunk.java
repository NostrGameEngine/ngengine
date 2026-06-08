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

package org.ngengine.world2d.tiled.core;

import java.util.Objects;

import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.math2d.Point;

/**
 * This is currently added only for infinite maps. The contents of a chunk element
 * is same as that of the data element, except it stores the data of the area
 * specified in the attributes.
 *
 * @author yanmaoyuan
 */
public class TiledTileChunk implements TiledTileContainer {
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
    private final TiledTileEntity[][] tiles;


    public TiledTileChunk(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.tiles = new TiledTileEntity[height][width];
        for(int ty=0; ty<height; ty++) {
            for(int tx=0; tx<width; tx++) {
                tiles[ty][tx] = new TiledTileEntity(this,null, x + tx, y + ty);
            }
        }
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
       public TiledTileEntity placeTileAt(int tx, int ty, Tile ti) {
        if (contains(tx, ty)) {
            TiledTileEntity te = getTileAt(tx, ty);
            if(Objects.equals(te.getTile(), ti)) return te;
            te.detached();
            te = new TiledTileEntity(this, ti, tx, ty);
            tiles[ty - y][tx - x] = te;
            te.attached();
            return te;                     
        }
        return TiledTileEntity.OUTSIDE_RANGE;
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
    public TiledTileEntity getTileAt(int tx, int ty) {
        if(!contains(tx, ty)) {
            return TiledTileEntity.OUTSIDE_RANGE;
        }
        return tiles[ty - y][tx - x] ;
    }

   

 
 


}