/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.texture.plugins;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.nio.ByteBuffer;

/**
 * Loads textures using Android's Bitmap class, but does not have the 
 * RGBA8 alpha bug.
 *
 * See below link for supported image formats:
 * https://developer.android.com/guide/topics/media/media-formats#image-formats
 * 
 * @author Kirill Vainer
 */
public class AndroidBufferImageLoader implements AssetLoader {
    
    private final byte[] tempData = new byte[16 * 1024];
    
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        Bitmap bitmap;
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inTempStorage = tempData;
        options.inScaled = false;
        options.inSampleSize = 1;
        // Do not premultiply alpha channel as it is not intended
        // to be directly drawn by the android view system.
        options.inPremultiplied = false;

        // TODO: It is more GC friendly to reuse the Bitmap class instead of recycling
        //  it on every image load. Android has introduced inBitmap option For this purpose.
        //  However, there are certain restrictions with how inBitmap can be used.
        //  See https://developer.android.com/topic/performance/graphics/manage-memory#inBitmap.

        try (final BufferedInputStream bin = new BufferedInputStream(assetInfo.openStream())) {
            bitmap = BitmapFactory.decodeStream(bin, null, options);
            if (bitmap == null) {
                throw new IOException("Failed to load image: " + assetInfo.getKey().getName());
            }
        }

        try {
            return readBitmap(bitmap, (TextureKey) assetInfo.getKey());
        } finally {
            bitmap.recycle();
        }
    }

    // Keep channel order, row padding and vertical orientation independent of Android's bitmap allocator.
    static Image readBitmap(Bitmap bitmap, TextureKey texKey) {
        Image.Format format;
        int bpp;
        switch (bitmap.getConfig()) {
            case ALPHA_8:
                format = Image.Format.Alpha8;
                bpp = 1;
                break;
            case ARGB_8888:
                format = Image.Format.RGBA8;
                bpp = 4;
                break;
            case RGB_565:
                format = Image.Format.RGB565;
                bpp = 2;
                break;
            case RGBA_F16:
                format = Image.Format.RGBA16F;
                bpp = 8;
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized Android bitmap format: " + bitmap.getConfig());
        }

        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        ByteBuffer data = BufferUtils.createByteBuffer(bitmap.getWidth() * bitmap.getHeight() * bpp);
        
        if (format == Image.Format.RGBA8) {
            int[] pixelData = new int[width * height];
            bitmap.getPixels(pixelData, 0,  width, 0, 0,          width,  height);

            for (int y = 0; y < height; y++) {
                int row = (texKey.isFlipY() ? height - y - 1 : y) * width;
                for (int x = 0; x < width; x++) {
                    int argb = pixelData[row + x];
                    data.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >> 24));
                }
            }
        } else {
            int stride = bitmap.getRowBytes();
            ByteBuffer raw = BufferUtils.createByteBuffer(stride * height);
            bitmap.copyPixelsToBuffer(raw);
            for (int y = 0; y < height; y++) {
                int row = (texKey.isFlipY() ? height - y - 1 : y) * stride;
                raw.limit(raw.capacity()).position(row).limit(row + width * bpp);
                data.put(raw);
            }
        }
        
        data.flip();
        
        ColorSpace space = ColorSpace.sRGB;
        if (format == Image.Format.RGBA16F) {
            android.graphics.ColorSpace bitmapSpace = bitmap.getColorSpace();
            if (bitmapSpace != null && (bitmapSpace.equals(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_SRGB))
                    || bitmapSpace.equals(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB)))) {
                space = ColorSpace.Linear;
            }
        }
        return new Image(format, width, height, data, space);
    }
}
