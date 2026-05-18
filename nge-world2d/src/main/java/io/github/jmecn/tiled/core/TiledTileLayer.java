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

import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import io.github.jmecn.tiled.core.entity.TiledTileEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.math2d.Point;
import io.github.jmecn.tiled.util.CoordinateSystem;

/**
 * A TileLayer is a specialized MapLayer, used for tracking two-dimensional tile
 * data.
 * 
 * @author yanmaoyuan
 */
public class TiledTileLayer extends TiledLayer implements TiledTileContainer {
    private TiledTileEntity[][] tiles;
    // protected HashMap<Object, Properties> tileInstanceProperties = new HashMap<>();
    private List<TiledTileChunk> chunks;

    /**
     * Construct a TileLayer from the given width and height.
     * 
     * @param w
     *            width in tiles
     * @param h
     *            height in tiles
     */
    public TiledTileLayer(int w, int h) {
        super(w, h);
        tiles = new TiledTileEntity[height][width];
        chunks = new ArrayList<>();
        for(int ty=0; ty<height; ty++) {
            for(int tx=0; tx<width; tx++) {
                tiles[ty][tx] = new TiledTileEntity(this, null, x + tx, y + ty);
            }
        }
    }

    /**
     * @param m
     *            the map this layer is part of
     */
    public TiledTileLayer(TiledMap m) {
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
    public TiledTileLayer(TiledMap m, int w, int h) {
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

  
  


    /**
     * Tell if the spatial at position(tx, ty) should be updated.
     * 
     * @param tx
     *            Tile-space x coordinate
     * @param ty
     *            Tile-space y coordinate
     * 
     * @return true if the spatial should be updated.
     */
    public int getUpdateIdAt(int tx, int ty) {
        if(!contains(tx, ty)) {
            return -1;
        }
        TiledTileEntity te = tiles[ty - y][tx - x];
        if(te == null)return -1;
        return  te.getUpdateId();
    }



 
    

    public List<TiledTileChunk> getChunks() {
        return chunks;
    }

    public void addChunk(TiledTileChunk chunk) {
        chunks.add(chunk);
    }

    @Override
     public void getNearby(
            CoordinateSystem coords,
            Vector2f worldPos,
            float radius,   
            List<TiledBase> out
    ){
        try(TempVars temp = TempVars.get()){          
           
                for(int x = 0; x < getWidth(); x++) {
                    for(int y = 0; y < getHeight(); y++) {
                        TiledTileEntity te = getTileAt(x, y);
                        if(te!=null){
                            Vector2f wpos = temp.vect2d;
                            coords.getPositionInGridSpace(this, wpos);
                            float dx = wpos.x - worldPos.x;
                            float dy = wpos.y - worldPos.y;
                            float dist2 = dx*dx + dy*dy;
                            if(dist2 <= radius*radius){
                                out.add(te);
                            }
                        }
                    }
                }
        }
    }
  
}
