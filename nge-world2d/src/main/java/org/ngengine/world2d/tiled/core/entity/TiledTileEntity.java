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

package org.ngengine.world2d.tiled.core.entity;

import java.math.BigInteger;

import com.jme3.util.SafeArrayList;

import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledTileContainer;
import org.ngengine.world2d.tiled.core.tileset.Tile;


/**
 * A tile that has been placed on a tile map layer
 * @author Riccardo Balbo
 */
public class TiledTileEntity extends TiledEntity {
    public static final TiledTileEntity OUTSIDE_RANGE = new TiledTileEntity(null, null, null, null);
    static {
        OUTSIDE_RANGE.outOfRange = true;
    }
    private  Tile tile;
    private final double x;
    private final double y;
    private boolean outOfRange;
    private final TiledTileContainer container;

    public TiledTileEntity(TiledTileContainer container, Tile tile, Number x, Number  y){
        this.tile = tile;
        this.container = container;
        this.x = x == null ? 0 : x.doubleValue();
        this.y = y == null ? 0 : y.doubleValue();
    }

    public void removeFromLayer(){
        if(container!=null){
            container.placeTileAt((int)x,(int)y, null);
        }
    }

    public TiledTileContainer getContainer() {
        return container;
    }

    public boolean isOutOfRange() {
        return outOfRange;
    }

 
    @Override
    public double getHeight() {
        return tile.getHeight();
    }

    @Override
    public double getWidth() {
        return tile.getWidth();
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public String getClazz() {
        return tile.getClazz();
    }

    @Override
    public BigInteger getId() {
        return BigInteger.valueOf(tile.getId());
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        if (this.tile == tile) {
            return;
        }
        this.tile = tile;
        logicalTileChanged();
        this.setUpdateNeeded();
    }
  
 


}
