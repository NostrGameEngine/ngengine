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

import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import java.util.function.Supplier;

/**
 * When read a &lt;image&gt; element there 5 attribute there. This class is just a data struct to return the whole image node.
 *
 * @author yanmaoyuan
 */
public class TiledImageEntity {
    private final String source;
    private final String trans;
    private final String format;
    private final int width;
    private final int height;

    private Texture2D texture;
    private Supplier<Texture2D> textureSupplier;
    private TextureArray textureArray;

    public TiledImageEntity(String source, String trans, String format, int width, int height) {
        this.source = source;
        this.trans = trans;
        this.format = format;
        this.width = width;
        this.height = height;
    }

    public String getSource() {
        return source;
    }

    public String getTrans() {
        return trans;
    }

    public String getFormat() {
        return format;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Texture2D getTexture() {
        if (texture == null && textureSupplier != null) {
            Supplier<Texture2D> supplier = textureSupplier;
            textureSupplier = null;
            texture = supplier.get();
        }
        return texture;
    }

    public void setTexture(Texture2D texture) {
        this.texture = texture;
        this.textureSupplier = null;
    }

    /**
     * Defers loading the atlas fallback until a renderer actually needs the
     * original 2D image. A pre-sliced texture array can therefore be rendered
     * without decoding both representations.
     *
     * @param textureSupplier one-shot texture loader, or {@code null}
     */
    public void setTextureSupplier(Supplier<Texture2D> textureSupplier) {
        this.texture = null;
        this.textureSupplier = textureSupplier;
    }

    /**
     * Returns the optional build-time sliced texture array for a Tiled atlas.
     *
     * @return prebuilt array, or {@code null} when the atlas must be sliced at runtime
     */
    public TextureArray getTextureArray() {
        return textureArray;
    }

    /**
     * Stores the optional build-time sliced texture array for a Tiled atlas.
     *
     * @param textureArray prebuilt array, or {@code null}
     */
    public void setTextureArray(TextureArray textureArray) {
        this.textureArray = textureArray;
    }

    @Override
    public String toString() {
        return "TiledImage{" +
                "source='" + source + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", trans='" + trans + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}
