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

package org.ngengine.world2d.tiled.loader.layer;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import org.ngengine.world2d.tiled.core.*;
import org.ngengine.world2d.tiled.loader.LayerLoader;
import org.ngengine.world2d.tiled.xml.XmlNode;

import java.io.IOException;

import static org.ngengine.world2d.tiled.TiledConst.*;
import static org.ngengine.world2d.tiled.TiledConst.TEXT_EMPTY;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class GroupLayerLoader extends LayerLoader {

    private final TiledMap map;

    public GroupLayerLoader(AssetManager assetManager, AssetKey<?> key, TiledMap map) {
        super(assetManager, key);
        this.map = map;
    }

    @Override
    public TiledLayerGroup load(XmlNode node) throws IOException {
        TiledLayerGroup groupLayer = new TiledLayerGroup();
        readLayerBase(node, groupLayer);
        groupLayer.setMap(map);

        LayerLoaders layerLoaders = new LayerLoaders(assetManager, assetKey, map);

        XmlNode child = node.getFirstChild();
        while (child != null) {
            // ignore properties
            if (!PROPERTIES.equals(child.getNodeName()) && !TEXT_EMPTY.equals(child.getNodeName())) {
                LayerLoader layerLoader = layerLoaders.create(child.getNodeName());
                if (layerLoader != null) {
                    TiledLayer layer = layerLoader.load(child);
                    groupLayer.addLayer(layer);
                }
            }
            child = child.getNextSibling();
        }
        return groupLayer;
    }
}
