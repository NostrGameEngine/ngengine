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

package io.github.jmecn.tiled.core.tileset;

import java.util.ArrayList;
import java.util.List;

import io.github.jmecn.tiled.core.TiledBase;

/**
 * Defines a list of colors and any number of Wang tiles using these colors. (since 1.1)
 *
 * @author yanmaoyuan
 */
public class WangSet extends TiledBase {

    /**
     * the tileset this wangset belongs to.
     */
    private Tileset tileset;

    /**
     * ID of this wangset.
     */
    private int id;

    /**
     * The name of the Wang set.
     */
    private String name;
    /**
     * The class of the Wang set (since 1.9, defaults to “”).
     */
    private String clazz;
    /**
     * The tile ID of the tile representing this Wang set.
     */
    private int tile;

    /**
     * Can contain up to 254.
     * (255 since Tiled 1.5, 254 since Tiled 1.10.2)
     */
    private List<WangColor> wangColors;

    /**
     * Can contain any number of Wang tiles.
     */
    private List<WangTile> wangTiles;

    public WangSet(String name) {
        this.name = name;
        wangColors = new ArrayList<>(255);
        wangTiles = new ArrayList<>();
    }

    public Tileset getTileset() {
        return tileset;
    }

    public void setTileset(Tileset tileset) {
        this.tileset = tileset;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public int getTile() {
        return tile;
    }

    public void setTile(int tile) {
        this.tile = tile;
    }

    public List<WangColor> getWangColors() {
        return wangColors;
    }

    public void addWangColor(WangColor wangColor) {
        wangColors.add(wangColor);
    }

    public List<WangTile> getWangTiles() {
        return wangTiles;
    }

    public void addWangTile(WangTile wangTile) {
        wangTiles.add(wangTile);
    }
}