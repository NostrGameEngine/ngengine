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
package org.ngengine.demo.son.utils;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.BiFunction;

public class FlipBookTexture extends TextureArray {

    private final Vector2f flipData = new Vector2f(0, 0);
    private final int frameWidth;
    private final int frameHeight;

    public static FlipBookTexture load(
        AssetManager assetManager,
        String basePath,
        int frameCount,
        boolean flipY,
        BiFunction<String, Integer, String> framePathSupplier
    ) {
        ArrayList<Image> images = new ArrayList<>(frameCount);
        int frameWidth = 0;
        int frameHeight = 0;
        for (int i = 0; i < frameCount; i++) {
            String path = framePathSupplier.apply(basePath, i);
            TextureKey key = new TextureKey(path, flipY);
            Texture tx = assetManager.loadTexture(key);
            Image image = tx.getImage();
            if (frameWidth == 0 && frameHeight == 0) {
                frameWidth = image.getWidth();
                frameHeight = image.getHeight();
            } else if (frameWidth != image.getWidth() || frameHeight != image.getHeight()) {
                throw new IllegalArgumentException("All images must have the same size");
            }
            images.add(image);
        }

        FlipBookTexture texture = new FlipBookTexture(images, frameWidth, frameHeight);
        texture.flipData.setY(frameCount);
        return texture;
    }

    public static FlipBookTexture load(
        AssetManager assetManager,
        String basePath,
        String fileExt,
        int frameCount,
        boolean flipY
    ) {
        return load(
            assetManager,
            basePath,
            frameCount,
            flipY,
            (path, index) -> {
                StringBuilder sb = new StringBuilder(path);
                if (!path.endsWith("/")) {
                    sb.append("/");
                }
                sb.append(String.format("%04d", index + 1));
                sb.append(fileExt);
                return sb.toString();
            }
        );
    }

    public FlipBookTexture(ArrayList<Image> images, int frameWidth, int frameHeight) {
        super(images);
        setWrap(WrapMode.Repeat);
        setMagFilter(MagFilter.Bilinear);
        setMinFilter(MinFilter.BilinearNoMipMaps);
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    public Vector2f getFlipData() {
        return flipData;
    }

    public void setFrame(int frame) {
        flipData.setX(frame);
    }

    public int getFrameCount() {
        return (int) flipData.y;
    }

    public void apply(Material mat, String paramName) {
        mat.setTexture(paramName, this);
        mat.setVector2(paramName + "Data", flipData);
    }

    private int getUByte(ByteBuffer data, int index) {
        return data.get(index) & 0xFF;
    }

    public ColorRGBA getColor(int x, int y, ColorRGBA store) {
        if (store == null) {
            store = new ColorRGBA();
        }
        Image image = getImage();
        Format format = image.getFormat();
        ByteBuffer data = image.getData((int) flipData.x);

        int index = (y * frameWidth + x) * format.getBitsPerPixel() / 8;

        if (format == Format.RGBA8) {
            store.set(
                getUByte(data, index) / 255f,
                getUByte(data, index + 1) / 255f,
                getUByte(data, index + 2) / 255f,
                getUByte(data, index + 3) / 255f
            );
        } else if (format == Format.RGB8) {
            store.set(getUByte(data, index) / 255f, getUByte(data, index + 1) / 255f, getUByte(data, index + 2) / 255f, 1);
        } else if (format == Format.RGB16F) {
            store.set(data.getFloat(index), data.getFloat(index + 4), data.getFloat(index + 8), 1);
        } else if (format == Format.RGBA8) {
            store.set(data.getFloat(index), data.getFloat(index + 4), data.getFloat(index + 8), data.getFloat(index + 12));
        } else if (format == Format.RGBA16F) {
            store.set(data.getFloat(index), data.getFloat(index + 4), data.getFloat(index + 8), data.getFloat(index + 12));
        } else if (format == Format.RGBA32F) {
            store.set(data.getFloat(index), data.getFloat(index + 4), data.getFloat(index + 8), data.getFloat(index + 12));
        } else if (format == Format.RGB32F) {
            store.set(data.getFloat(index), data.getFloat(index + 4), data.getFloat(index + 8), 1);
        } else if (format == Format.ABGR8) {
            store.set(
                getUByte(data, index + 2) / 255f,
                getUByte(data, index + 1) / 255f,
                getUByte(data, index) / 255f,
                getUByte(data, index + 3) / 255f
            );
        } else if (format == Format.BGRA8) {
            store.set(
                getUByte(data, index + 2) / 255f,
                getUByte(data, index + 1) / 255f,
                getUByte(data, index) / 255f,
                getUByte(data, index + 3) / 255f
            );
        } else if (format == Format.ARGB8) {
            store.set(
                getUByte(data, index) / 255f,
                getUByte(data, index + 1) / 255f,
                getUByte(data, index + 2) / 255f,
                getUByte(data, index + 3) / 255f
            );
        } else if (format == Format.BGR8) {
            store.set(getUByte(data, index + 2) / 255f, getUByte(data, index + 1) / 255f, getUByte(data, index) / 255f, 1);
        } else {
            throw new IllegalArgumentException("Unsupported image format: " + format);
        }
        return store;
    }

    public ColorRGBA getColor(int x, int y) {
        return getColor(x, y, null);
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }
}
