/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system.lwjgl;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SdlWindowIconTest {

    @Test
    public void shouldGenerateMissingStandardSizesWithoutChangingSource() {
        Image source = solidImage(512, 512, ColorRGBA.Red);

        List<SdlWindowIcon.IconImage> images = SdlWindowIcon.prepareImages(new Object[]{source}, null);

        Set<String> sizes = images.stream()
                .map(icon -> icon.image.getWidth() + "x" + icon.image.getHeight())
                .collect(Collectors.toSet());
        assertEquals(Set.of("512x512", "256x256", "128x128", "64x64", "32x32", "16x16"), sizes);
        assertNull(source.getMipMapSizes());
    }

    @Test
    public void shouldLoadStringWithAssetManagerWithoutVerticalFlip() {
        Image source = solidImage(32, 32, ColorRGBA.Green);
        Texture texture = new Texture2D(source);
        AssetManager assetManager = mock(AssetManager.class);
        when(assetManager.loadTexture(org.mockito.ArgumentMatchers.any(TextureKey.class))).thenReturn(texture);

        List<SdlWindowIcon.IconImage> images =
                SdlWindowIcon.prepareImages(new Object[]{"Icons/application.png"}, assetManager);

        ArgumentCaptor<TextureKey> key = ArgumentCaptor.forClass(TextureKey.class);
        verify(assetManager).loadTexture(key.capture());
        assertEquals("Icons/application.png", key.getValue().getName());
        assertFalse(key.getValue().isFlipY());
        assertFalse(images.isEmpty());
        assertFalse(images.get(0).flipY);
    }

    @Test
    public void shouldPreserveTextureFlipMetadata() {
        Texture2D texture = new Texture2D(solidImage(32, 32, ColorRGBA.Blue));
        texture.setKey(new TextureKey("Icons/flipped.png", true));

        List<SdlWindowIcon.IconImage> images = SdlWindowIcon.prepareImages(new Object[]{texture}, null);

        assertTrue(images.get(0).flipY);
    }

    @Test
    public void shouldScaleFromClosestSuppliedResolution() {
        Image small = solidImage(24, 24, ColorRGBA.Red);
        Image large = solidImage(512, 512, ColorRGBA.Blue);

        List<SdlWindowIcon.IconImage> images =
                SdlWindowIcon.prepareImages(new Object[]{small, large}, null);

        Image generated = images.stream()
                .map(icon -> icon.image)
                .filter(image -> image.getWidth() == 32 && image.getHeight() == 32)
                .findFirst()
                .orElseThrow();
        ColorRGBA color = ImageRaster.create(generated).getPixel(0, 0, null);
        assertEquals(ColorRGBA.Red, color);
    }

    @Test
    public void shouldReadBufferedImageReflectively() {
        BufferedImage bufferedImage = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, 0x80402010);

        List<SdlWindowIcon.IconImage> images =
                SdlWindowIcon.prepareImages(new Object[]{bufferedImage}, null);

        SdlWindowIcon.IconImage converted = images.stream()
                .filter(icon -> icon.image.getWidth() == 48 && icon.image.getHeight() == 48)
                .findFirst()
                .orElseThrow();
        ColorRGBA color = ImageRaster.create(converted.image).getPixel(0, 0, null);
        assertEquals(0x40 / 255f, color.r, 0.001f);
        assertEquals(0x20 / 255f, color.g, 0.001f);
        assertEquals(0x10 / 255f, color.b, 0.001f);
        assertEquals(0x80 / 255f, color.a, 0.001f);
    }

    private static Image solidImage(int width, int height, ColorRGBA color) {
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
        for (int i = 0; i < width * height; i++) {
            data.put((byte) Math.round(color.r * 255f));
            data.put((byte) Math.round(color.g * 255f));
            data.put((byte) Math.round(color.b * 255f));
            data.put((byte) Math.round(color.a * 255f));
        }
        data.flip();
        return new Image(Image.Format.RGBA8, width, height, data, ColorSpace.sRGB);
    }
}
