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

package io.github.jmecn.tiled.renderer.factory;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;

import io.github.jmecn.tiled.core.entity.TiledImageEntity;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.core.tileset.Tileset;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public interface MaterialFactory {
    Material newMaterial();

    void setTile(Material mat, Tile tile);

    void setTileset(Material mat, Tileset tileset);

    void setTiledImage(Material mat, TiledImageEntity image);

    void setColor(Material mat, ColorRGBA color);

    void setTintColor(Material mat, ColorRGBA tintColor);

    void setTintColor(Spatial spatial, ColorRGBA tintColor);

    void setLayerOpacity(Material material, float opacity);

    void setLayerOpacity(Spatial spatial, float opacity);

    void setOpacity(Material material, float opacity);

    void setOpacity(Spatial spatial, float opacity);

    void setMapObject(Material mat, TiledObjectEntity obj);
}