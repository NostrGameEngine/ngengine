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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.util.CoordinateSystem;

/**
 * GroupLayer
 *
 * @author yanmaoyuan
 */
public class TiledLayerGroup extends TiledLayer {

    private final List<TiledLayer> layers;

    private final Map<String, TiledLayer> layerMap;

    public TiledLayerGroup() {
        layers = new ArrayList<>();
        layerMap = new HashMap<>();
    }

    public void addLayer(TiledLayer layer) {
        layer.setMap(getMap());
        layer.setParent(this);
        layers.add(layer);
        layerMap.put(layer.getName(), layer);
        layer.setUpdateNeeded();
    }

    @Override
    public void setUpdateNeeded() {
        for (TiledLayer layer : layers) {
            layer.setUpdateNeeded();
        }
    }

    /**
     * @return The number of layers in this group.
     */
    public int getLayerCount() {
        return layers.size();
    }

    /**
     * @return The list of layers in this group.
     */
    public List<TiledLayer> getLayers() {
        return layers;
    }

    /**
     * @param index The index of the layer.
     * @return The layer at the given index.
     */
    public TiledLayer getLayer(int index) {
        return layers.get(index);
    }

    /**
     * @param name The name of the layer.
     * @return The layer with the given name, or null if no layer with that name
     */
    public TiledLayer getLayer(String name) {
        return layerMap.get(name);
    }

    /**
     * Invalid the render offset of all layers.
     */
    @Override
    public void invalidRenderOffset() {
        super.invalidRenderOffset();
        for (TiledLayer layer : layers) {
            layer.invalidRenderOffset();
        }
    }

    /**
     * Invalid the render parallax of all layers.
     */
    @Override
    public void invalidRenderParallax() {
        super.invalidRenderParallax();
        for (TiledLayer layer : layers) {
            layer.invalidRenderParallax();
        }
    }

    @Override
    public void getNearby(
            CoordinateSystem coords,
            Vector2f worldPos,
            float radius,   
            List<TiledBase> out
    ){
        for (TiledLayer layer : layers) {
            layer.getNearby(coords, worldPos, radius, out);
        }
    }
}