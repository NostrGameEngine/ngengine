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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.StbImageLoader;

import org.ngengine.world2d.tiled.core.entity.TiledImageEntity;
import org.ngengine.world2d.tiled.xml.XmlNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.ngengine.world2d.tiled.TiledConst.*;
import static org.ngengine.world2d.tiled.loader.Utils.*;

/**
 * Tiled Image Loader.
 *
 * @author yanmaoyuan
 */
public final class ImageLoader {

    private static final Logger logger = Logger.getLogger(ImageLoader.class.getName());

    private final AssetManager assetManager;
    private final AssetKey<?> assetKey;

    public ImageLoader(AssetManager assetManager, AssetKey<?> assetKey) {
        this.assetManager = assetManager;
        this.assetKey = assetKey;
    }

    /**
     * <p>Load an image from file or decode from the data elements.</p>
     * <p>
     * Note that it is not currently possible to use Tiled to create maps with
     * embedded image data, even though the TMX format supports this. It is
     * possible to create such maps using libtiled (Qt/C++) or tmxlib (Python).
     * </p>
     *
     * @param node the node representing the "image" element
     * @return the loaded image
     */
    public TiledImageEntity load(XmlNode node) {
        String source = getAttributeValue(node, SOURCE);
        String trans = getAttributeValue(node, TRANS);
        String format = getAttributeValue(node, FORMAT);
        int width = getAttribute(node, WIDTH, 0);
        int height = getAttribute(node, HEIGHT, 0);

        Texture2D texture = null;
        // load an image from file or decode from the CDATA.
        if (source != null) {
            String assetPath = toJmeAssetPath(assetManager, assetKey, assetKey.getFolder() + source);
            source = assetPath;
            texture = loadTexture2D(assetPath);
        } else {
            // embedded image data, decode from the <data> node text
            XmlNode item = getChildByTag(node, DATA);
            if (item != null) {
                String encodedData = item.getTextContent();
                if ((encodedData == null || encodedData.trim().isEmpty()) && item.getFirstChild() != null) {
                    encodedData = item.getFirstChild().getTextContent();
                }
                if (encodedData != null && !encodedData.trim().isEmpty()) {
                    byte[] imageData = Base64.getDecoder().decode(encodedData.trim());
                    texture = loadTexture2D(imageData);
                }
            }
        }

        if (texture == null) {
            logger.log(Level.SEVERE, "Image source not found: " + source);
            throw new IllegalArgumentException("Image source not found: " + source);
        }

        if (width == 0 || height == 0) {
            logger.info("Image size is not specified, using the texture size.");
            width = texture.getImage().getWidth();
            height = texture.getImage().getHeight();
        }

        TiledImageEntity image = new TiledImageEntity(source, trans, format, width, height);
        image.setTexture(texture);

        return image;
    }


    /**
     * Load a Texture from source
     *
     * @param source the source path
     * @return the loaded texture
     */
    private Texture2D loadTexture2D(final String source) {
        Texture2D tex = null;
        try {
            TextureKey texKey = new TextureKey(source, true);
            texKey.setGenerateMips(false);
            tex = (Texture2D) assetManager.loadTexture(texKey);
            tex.setWrap(Texture.WrapMode.EdgeClamp);
            tex.setMagFilter(Texture.MagFilter.Nearest);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can't load texture " + source, e);
        }

        return tex;
    }

    private Texture2D loadTexture2D(final byte[] data) {
        TextureKey texKey = new TextureKey();
        AssetInfo info = new AssetInfo(assetManager, texKey) {
            public InputStream openStream() {
                return new ByteArrayInputStream(data);
            }
        };

        Texture2D tex = null;
        try {
            Image img = (Image) new StbImageLoader().load(info);

            tex = new Texture2D();
            tex.setWrap(Texture.WrapMode.EdgeClamp);
            tex.setMagFilter(Texture.MagFilter.Nearest);
            tex.setAnisotropicFilter(texKey.getAnisotropy());
            tex.setName(texKey.getName());
            tex.setImage(img);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can't load texture from byte array", e);
        }
        return tex;
    }
}
