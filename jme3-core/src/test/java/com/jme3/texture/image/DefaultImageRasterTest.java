/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.texture.image;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DefaultImageRasterTest {

    @Test
    public void luminance8ReadsEveryByteAsOpaqueGray() {
        byte[] samples = new byte[256];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (byte) i;
        }
        byte[] original = samples.clone();
        ImageRaster raster = raster(Image.Format.Luminance8, samples.length,
                ByteBuffer.wrap(samples), ColorSpace.Linear, false);
        ColorRGBA store = new ColorRGBA(9f, 8f, 7f, 0.25f);

        for (int i = 0; i < samples.length; i++) {
            assertSame(store, raster.getPixel(i, 0, store));
            float gray = i / 255f;
            assertColor(gray, gray, gray, 1f, store);
        }
        assertArrayEquals(original, samples);
    }

    @Test
    public void luminance16FReadsOpaqueGrayWithoutClampingHdr() {
        // Raw IEEE 754 half samples: zero, 1/4, 2, -2, largest finite, smallest normal.
        ByteBuffer samples = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        samples.putShort((short) 0x0000).putShort((short) 0x3400)
                .putShort((short) 0x4000).putShort((short) 0xc000)
                .putShort((short) 0x7bff).putShort((short) 0x0400);
        byte[] original = samples.array().clone();
        float[] expected = {0f, 0.25f, 2f, -2f, 65504f, 0x1.0p-14f};
        ImageRaster raster = raster(Image.Format.Luminance16F, expected.length,
                samples, ColorSpace.Linear, true);

        for (int i = 0; i < expected.length; i++) {
            assertColor(expected[i], expected[i], expected[i], 1f, raster.getPixel(i, 0));
        }
        assertArrayEquals(original, samples.array());
    }

    @Test
    public void luminance32FReadsOpaqueGrayWithoutChangingStoredValues() {
        float[] expected = {0f, 0.25f, 2f, -2f, 65536f, Float.MAX_VALUE,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN};
        ByteBuffer samples = ByteBuffer.allocate(expected.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : expected) {
            samples.putFloat(value);
        }
        byte[] original = samples.array().clone();
        ImageRaster raster = raster(Image.Format.Luminance32F, expected.length,
                samples, ColorSpace.Linear, false);

        for (int i = 0; i < expected.length; i++) {
            assertColor(expected[i], expected[i], expected[i], 1f, raster.getPixel(i, 0));
        }
        assertArrayEquals(original, samples.array());
    }

    @Test
    public void writingLuminanceDoesNotCreateAnAlphaChannel() {
        Image.Format[] formats = {Image.Format.Luminance8, Image.Format.Luminance16F,
                Image.Format.Luminance32F};
        int[] sizes = {1, 2, 4};
        for (int i = 0; i < formats.length; i++) {
            ImageRaster raster = raster(formats[i], 1, ByteBuffer.allocate(sizes[i]),
                    ColorSpace.Linear, false);
            float gray = i == 0 ? 1f : 2f;
            raster.setPixel(0, 0, new ColorRGBA(gray, gray, gray, 0f));
            assertColor(gray, gray, gray, 1f, raster.getPixel(0, 0));
        }
    }

    @Test
    public void luminanceColorSpaceConversionKeepsOpaqueAlpha() {
        ByteBuffer samples = ByteBuffer.wrap(new byte[]{(byte) 128});
        ImageRaster nativeSrgb = raster(Image.Format.Luminance8, 1, samples,
                ColorSpace.sRGB, false);
        ImageRaster linearized = raster(Image.Format.Luminance8, 1, samples,
                ColorSpace.sRGB, true);
        ImageRaster linearData = raster(Image.Format.Luminance8, 1, samples,
                ColorSpace.Linear, true);

        assertColor(128f / 255f, 128f / 255f, 128f / 255f, 1f, nativeSrgb.getPixel(0, 0));
        assertColor(128f / 255f, 128f / 255f, 128f / 255f, 1f, linearData.getPixel(0, 0));
        ColorRGBA converted = linearized.getPixel(0, 0);
        // Preserve ColorRGBA's existing gamma-2.2 conversion.
        assertEquals(0.21951973f, converted.r, 0.0000001f);
        assertEquals(converted.r, converted.g);
        assertEquals(converted.r, converted.b);
        assertEquals(1f, converted.a);
        assertEquals((byte) 128, samples.get(0));
    }

    @Test
    public void luminance8Alpha8PreservesStoredTransparency() {
        byte[] samples = {(byte) 150, 0, (byte) 150, 64, (byte) 150, (byte) 128,
                (byte) 150, (byte) 255};
        byte[] original = samples.clone();
        float[] alpha = {0f, 64f / 255f, 128f / 255f, 1f};
        ImageRaster raster = raster(Image.Format.Luminance8Alpha8, alpha.length,
                ByteBuffer.wrap(samples), ColorSpace.Linear, false);

        for (int i = 0; i < alpha.length; i++) {
            assertColor(150f / 255f, 150f / 255f, 150f / 255f, alpha[i], raster.getPixel(i, 0));
        }
        assertArrayEquals(original, samples);
    }

    @Test
    public void luminance16FAlpha16FPreservesHdrAndStoredAlpha() {
        ByteBuffer samples = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        samples.putShort((short) 0xc000).putShort((short) 0x0000)
                .putShort((short) 0x4000).putShort((short) 0x3800)
                .putShort((short) 0x7bff).putShort((short) 0x3c00);
        byte[] original = samples.array().clone();
        ImageRaster raster = raster(Image.Format.Luminance16FAlpha16F, 3,
                samples, ColorSpace.Linear, false);

        assertColor(-2f, -2f, -2f, 0f, raster.getPixel(0, 0));
        assertColor(2f, 2f, 2f, 0.5f, raster.getPixel(1, 0));
        assertColor(65504f, 65504f, 65504f, 1f, raster.getPixel(2, 0));
        assertArrayEquals(original, samples.array());
    }

    @Test
    public void rgba8PreservesStoredTransparency() {
        byte[] samples = {10, 20, 30, 0, 10, 20, 30, (byte) 128, 10, 20, 30, (byte) 255};
        byte[] original = samples.clone();
        ImageRaster raster = raster(Image.Format.RGBA8, 3, ByteBuffer.wrap(samples),
                ColorSpace.Linear, false);

        assertColor(10f / 255f, 20f / 255f, 30f / 255f, 0f, raster.getPixel(0, 0));
        assertColor(10f / 255f, 20f / 255f, 30f / 255f, 128f / 255f, raster.getPixel(1, 0));
        assertColor(10f / 255f, 20f / 255f, 30f / 255f, 1f, raster.getPixel(2, 0));
        assertArrayEquals(original, samples);
    }

    @Test
    public void rgba16FPreservesHdrAndStoredAlpha() {
        ByteBuffer samples = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        samples.putShort((short) 0xc000).putShort((short) 0x3400)
                .putShort((short) 0x4400).putShort((short) 0x3800);
        byte[] original = samples.array().clone();
        ImageRaster raster = raster(Image.Format.RGBA16F, 1, samples, ColorSpace.Linear, false);

        assertColor(-2f, 0.25f, 4f, 0.5f, raster.getPixel(0, 0));
        assertArrayEquals(original, samples.array());
    }

    @Test
    public void rgba32FPreservesHdrAndStoredNonfiniteAlpha() {
        ByteBuffer samples = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        samples.putFloat(-2f).putFloat(0.25f).putFloat(65536f).putFloat(2f)
                .putFloat(-2f).putFloat(0.25f).putFloat(65536f).putFloat(Float.NaN);
        byte[] original = samples.array().clone();
        ImageRaster raster = raster(Image.Format.RGBA32F, 2, samples, ColorSpace.Linear, true);

        assertColor(-2f, 0.25f, 65536f, 2f, raster.getPixel(0, 0));
        assertColor(-2f, 0.25f, 65536f, Float.NaN, raster.getPixel(1, 0));
        assertArrayEquals(original, samples.array());
    }

    @Test
    public void nonGrayMissingComponentsKeepTheirDefaults() {
        ImageRaster rgb = raster(Image.Format.RGB8, 1,
                ByteBuffer.wrap(new byte[]{10, 20, 30}), ColorSpace.Linear, false);
        ImageRaster alpha = raster(Image.Format.Alpha8, 1,
                ByteBuffer.wrap(new byte[]{64}), ColorSpace.Linear, false);
        ByteBuffer samples = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        samples.putShort((short) 0x4000);
        ImageRaster red = raster(Image.Format.R16F, 1, samples, ColorSpace.Linear, false);

        assertColor(10f / 255f, 20f / 255f, 30f / 255f, 1f, rgb.getPixel(0, 0));
        assertColor(1f, 1f, 1f, 64f / 255f, alpha.getPixel(0, 0));
        assertColor(2f, 1f, 1f, 1f, red.getPixel(0, 0));
    }

    private static ImageRaster raster(Image.Format format, int width, ByteBuffer samples,
            ColorSpace colorSpace, boolean convertToLinear) {
        Image image = new Image(format, width, 1, samples, colorSpace);
        return ImageRaster.create(image, 0, 0, convertToLinear);
    }

    private static void assertColor(float red, float green, float blue, float alpha, ColorRGBA actual) {
        assertEquals(red, actual.r, "red");
        assertEquals(green, actual.g, "green");
        assertEquals(blue, actual.b, "blue");
        assertEquals(alpha, actual.a, "alpha");
    }
}
