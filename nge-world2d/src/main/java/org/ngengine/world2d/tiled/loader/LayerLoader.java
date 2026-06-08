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

package org.ngengine.world2d.tiled.loader;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.util.ColorUtil;
import org.ngengine.world2d.tiled.xml.XmlNode;

import java.io.IOException;
import java.util.Map;

import static org.ngengine.world2d.tiled.TiledConst.*;
import static org.ngengine.world2d.tiled.loader.Utils.*;
import static org.ngengine.world2d.tiled.loader.Utils.getDoubleAttribute;

/**
 * The base class for all layer loaders.
 *
 * @author yanmaoyuan
 */
public abstract class LayerLoader {

    protected AssetManager assetManager;
    protected AssetKey<?> assetKey;
    protected PropertyLoader propertiesLoader;
    protected ImageLoader imageLoader;

    protected LayerLoader(AssetManager assetManager, AssetKey<?> key) {
        this.assetManager = assetManager;
        this.assetKey = key;

        this.propertiesLoader = new PropertyLoader();
        this.imageLoader = new ImageLoader(assetManager, key);
    }

    /**
     * Loads a map layer from a layer node.
     * @param node the node representing the "layer" element
     * @return the loaded map layer
     * @throws IOException if an I/O error occurs
     */
    public abstract TiledLayer load(XmlNode node) throws IOException;

    /**
     * read the common part of a Layer
     *
     * @param node the Layer node
     * @param layer the Layer
     */
    protected void readLayerBase(XmlNode node, TiledLayer layer) {
        String id = getAttributeValue(node, ID);
        if (id != null) {
            layer.setId(Integer.parseInt(id));
        }

        final String name = getAttributeValue(node, NAME);
        String clazz = getAttribute(node, CLASS, EMPTY);
        double opacity = getDoubleAttribute(node, OPACITY, 1.0);
        boolean visible = getAttribute(node, VISIBLE, 1) == 1;
        boolean locked = getAttribute(node, LOCKED, 0) == 1;
        String tintColor = getAttributeValue(node, TINT_COLOR);
        int offsetX = getAttribute(node, OFFSET_X, 0);
        int offsetY = getAttribute(node, OFFSET_Y, 0);
        float parallaxX = (float) getDoubleAttribute(node, PARALLAX_X, 1.0);
        float parallaxY = (float) getDoubleAttribute(node, PARALLAX_Y, 1.0);

        layer.setName(name);
        layer.setClazz(clazz);
        layer.setOpacity(opacity);

        if (tintColor != null) {
            layer.setTintColor(ColorUtil.toColorRGBA(tintColor));
        }

        // This is done at the end, otherwise the offset is applied during
        // the loading of the tiles.
        layer.setOffset(offsetX, offsetY);

        // The parallax scrolling factor determines the amount by which the layer
        // moves in relation to the camera.
        layer.setParallaxFactor(parallaxX, parallaxY);

        // Invisible layers are automatically locked, so it is important to
        // set the layer to potentially invisible _after_ the layer data is
        // loaded.
        layer.setVisible(visible);

        layer.setLocked(locked);

        // read properties
        Map<String, Object> props = propertiesLoader.readProperties(node);
        layer.setProperties(props);
    }

}
