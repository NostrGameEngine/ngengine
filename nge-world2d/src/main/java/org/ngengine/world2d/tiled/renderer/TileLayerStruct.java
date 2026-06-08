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

package org.ngengine.world2d.tiled.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.fields.BooleanField;
import com.jme3.util.struct.fields.FloatField;
import com.jme3.util.struct.fields.IntField;
import com.jme3.util.struct.fields.SubStructArrayField;

import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

public final class TileLayerStruct implements Struct {
    public static final class TileStruct implements Struct {
        public final IntField gid = new IntField(0, "gid", 0);
        public final IntField tileset = new IntField(1, "tileset", 0);
        public final IntField width = new IntField(2, "width", 0);
        public final IntField height = new IntField(3, "height", 0);
        public final IntField x = new IntField(4, "x", 0);
        public final IntField y = new IntField(5, "y", 0);

    }

    public static final class TilesetStruct implements Struct {
        public final IntField tileWidth = new IntField(1, "tileWidth", 0);
        public final IntField tileHeight = new IntField(2, "tileHeight", 0);
        public final IntField tileMargin = new IntField(3, "tileMargin", 0);
        public final IntField tileSpacing = new IntField(4, "tileSpacing", 0);

        transient Tileset tileset;
        transient int updateId = 0;
    }

    public final IntField width = new IntField(0, "width", 0);
    public final IntField height = new IntField(1, "width", 0);    
    public final SubStructArrayField<TileStruct> tiles;
    public final SubStructArrayField<TilesetStruct> tilesets;
    
     
    public TileLayerStruct(int width, int height, int tilesetCount) {
        this.width.setValue(width);
        this.height.setValue(height);
        this.tiles = new SubStructArrayField<TileStruct>(2, "tiles", width*height, TileStruct.class);
        this.tilesets = new SubStructArrayField<TilesetStruct>(3, "tilesets", tilesetCount, TilesetStruct.class);
    }

    private transient AtomicInteger updateCounter = new AtomicInteger(0);
    private transient List<TiledTileEntity> enqueued = new ArrayList<>();
    
    private void updateTilesetStruct(Tileset ts, TilesetStruct out){

    }
    
    public void update(TiledTileEntity entity){
        tryUpdate(entity, true);
    }


    private boolean tryUpdate(TiledTileEntity entity, boolean enqueueIfFull){
        Tile tile = entity.getTile();
        Tileset tileset = tile.getTileset();
        int tilesetId = -1;
        if(tilesetId==-1){
            // for(TilesetStruct ts : tilesets.getValue()){
            for(int i=0; i<tilesets.getValue().length; i++){
                TilesetStruct ts = tilesets.getValue()[i];
                if(ts.tileset == tileset){
                    updateTilesetStruct(tileset, ts);
                    ts.updateId = updateCounter.get();
                    tilesetId = i;
                    break;
                } 
            }
        }
        if(tilesetId==-1){
            // for(TilesetStruct ts : tilesets.getValue()){
            for(int i=0; i<tilesets.getValue().length; i++){
                TilesetStruct ts = tilesets.getValue()[i];
                if(ts.tileset == null){
                    updateTilesetStruct(tileset, ts);
                    ts.updateId = updateCounter.get();
                    // updatedTileset = true;
                    tilesetId = i;
                    break;
                }
            }
        }

        if(tilesetId==-1){
            if(enqueueIfFull) enqueued.add(entity);
            return false;
        } 
        
        int tileIndex = (int) (entity.getY() * entity.getContainer().getWidth() + entity.getX());
        TileStruct tileStruct = tiles.getValue()[tileIndex];
        tileStruct.tileset.setValue(tilesetId);
        tileStruct.gid.setValue(tile.getGid());
        tileStruct.width.setValue(tile.getWidth());
        tileStruct.height.setValue(tile.getHeight());
        tileStruct.x.setValue(tile.getX());
        tileStruct.y.setValue(tile.getY());
    
        return true;
    }

    public void commit(){
        try{
            int id = updateCounter.getAndIncrement();
            for(TilesetStruct ts : tilesets.getValue()){
                if (   ts.tileset!=null&&ts.updateId != id){
                    ts.tileset = null;
                }
            }
            for(TiledTileEntity entity : enqueued){
                if(!tryUpdate(entity, false)){
                    throw new RuntimeException("Cannot update TileLayerStruct, tileset slots are full. Max supported tileset pet layer: " + tilesets.getValue().length);
                }
            }
        } finally {
            enqueued.clear();
        }
    }
}
