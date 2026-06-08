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

package org.ngengine.world2d.tiled;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import org.ngengine.world2d.tiled.loader.MapLoader;
import org.ngengine.world2d.tiled.loader.TilesetLoader;
import org.ngengine.world2d.tiled.loader.layer.ObjectLayerLoader;

import java.io.IOException;

/**
 * Tiled map loader.
 *
 * @author yanmaoyuan
 */
public class TmxLoader implements AssetLoader {

    public static void registerLoader(AssetManager assetManager) {
        assetManager.registerLoader(TmxLoader.class, TiledConst.TMX_EXTENSION, TiledConst.TSX_EXTENSION, TiledConst.TX_EXTENSION);
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        AssetManager assetManager = assetInfo.getManager();

        String extension = key.getExtension();

        switch (extension) {
            case TiledConst.TMX_EXTENSION:
                MapLoader mapLoader = new MapLoader(assetManager, key);
                return mapLoader.load(assetInfo.openStream());
            case TiledConst.TSX_EXTENSION:
                TilesetLoader tilesetLoader = new TilesetLoader(assetManager, key);
                return tilesetLoader.load(assetInfo.openStream());
            case TiledConst.TX_EXTENSION:
                ObjectLayerLoader objectLayerLoader = new ObjectLayerLoader(assetManager, key, null);
                Object template = objectLayerLoader.loadObjectTemplate(assetInfo.openStream());
                if (template instanceof org.ngengine.world2d.tiled.core.TiledObjectTemplate) {
                    ((org.ngengine.world2d.tiled.core.TiledObjectTemplate) template).setSource(key.getName());
                }
                return template;
            default:
                return null;
        }
    }

}
