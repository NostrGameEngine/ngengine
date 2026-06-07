/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package com.jme3.texture.plugins;

import com.jme3.asset.TextureKey;

public class SVGTextureKey extends TextureKey {

    private final int width;
    private final int height;
    private final float viewBoxX;
    private final float viewBoxY;
    private final float viewBoxWidth;
    private final float viewBoxHeight;

    public SVGTextureKey(String name, int width, int height) {
        this(name, false, width, height);
    }

    public SVGTextureKey(String name, boolean flipY, int width, int height) {
        super(name, flipY);
        this.width = width;
        this.height = height;
        this.viewBoxX = Float.NaN;
        this.viewBoxY = Float.NaN;
        this.viewBoxWidth = Float.NaN;
        this.viewBoxHeight = Float.NaN;
    }

    public SVGTextureKey(String name, int width, int height,
                         float viewBoxX, float viewBoxY, float viewBoxWidth, float viewBoxHeight) {
        super(name, false);
        this.width = width;
        this.height = height;
        this.viewBoxX = viewBoxX;
        this.viewBoxY = viewBoxY;
        this.viewBoxWidth = viewBoxWidth;
        this.viewBoxHeight = viewBoxHeight;
    }

    @Override
    public String getExtension() {
        return "svg";
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasViewBoxClip() {
        return !Float.isNaN(viewBoxX)
                && !Float.isNaN(viewBoxY)
                && viewBoxWidth > 0f
                && viewBoxHeight > 0f;
    }

    public float getViewBoxX() {
        return viewBoxX;
    }

    public float getViewBoxY() {
        return viewBoxY;
    }

    public float getViewBoxWidth() {
        return viewBoxWidth;
    }

    public float getViewBoxHeight() {
        return viewBoxHeight;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        SVGTextureKey other = (SVGTextureKey) obj;
        return width == other.width
                && height == other.height
                && Float.compare(viewBoxX, other.viewBoxX) == 0
                && Float.compare(viewBoxY, other.viewBoxY) == 0
                && Float.compare(viewBoxWidth, other.viewBoxWidth) == 0
                && Float.compare(viewBoxHeight, other.viewBoxHeight) == 0;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + width;
        hash = 31 * hash + height;
        hash = 31 * hash + Float.floatToIntBits(viewBoxX);
        hash = 31 * hash + Float.floatToIntBits(viewBoxY);
        hash = 31 * hash + Float.floatToIntBits(viewBoxWidth);
        hash = 31 * hash + Float.floatToIntBits(viewBoxHeight);
        return hash;
    }
}
