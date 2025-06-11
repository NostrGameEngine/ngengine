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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.stb.STBImage;


public class StbImageLoader implements AssetLoader {
   

    @Override
    public Object load(AssetInfo info) throws IOException {

        try (InputStream in = info.openStream(); BufferedInputStream bin = new BufferedInputStream(in)) {
            boolean flipY = true;
            if (info.getKey() instanceof TextureKey) {
                flipY = ((TextureKey) info.getKey()).isFlipY();
            }

            IntBuffer wB    = BufferUtils.createIntBuffer(1);
            IntBuffer hB    =  BufferUtils.createIntBuffer(1);
            IntBuffer compB = BufferUtils.createIntBuffer(1);
            
            ByteBuffer imageBuffer = BufferUtils.createByteBuffer(bin.readAllBytes());
            
            if (!STBImage.stbi_info_from_memory(imageBuffer, wB, hB, compB)) {
                throw new RuntimeException("Failed to read image information: " + STBImage.stbi_failure_reason());
            }  

            int width = wB.get(0);
            int height = hB.get(0);
            int comp = compB.get(0);

            if (width <= 0 || height <= 0) {
                throw new AssetLoadException("Invalid image dimensions: " + width + "x" + height);
            }

            if(width > 16384 || height > 16384) {
                throw new AssetLoadException("Image dimensions too large: " + width + "x" + height);
            }

            if(comp < 1 || comp > 4) {
                throw new AssetLoadException("Unsupported number of components: " + comp);
            }
           
            STBImage.stbi_set_flip_vertically_on_load(flipY);

            ByteBuffer jmeImageBuffer;
            Format format;
            boolean is16Bit = STBImage.stbi_is_16_bit_from_memory(imageBuffer);
            if(is16Bit){
                switch (compB.get(0)) {
                    case 1:
                        format = Format.Luminance16F;
                        break;
                    case 2:
                        format = Format.RG16F;
                        break;
                    case 3:
                        format = Format.RGB16F;
                        break;
                    case 4:
                        format = Format.RGBA16F;
                        break;
                    default:
                        throw new AssetLoadException("Unsupported number of components: " + compB.get(0));
                }

                FloatBuffer bbf = STBImage.stbi_loadf_from_memory(imageBuffer, wB, hB, compB, 0);
                if (bbf == null) {
                    throw new RuntimeException("Failed to load 16-bit image: " + STBImage.stbi_failure_reason());
                }

                jmeImageBuffer = BufferUtils.createByteBuffer(width * height * comp * 2);
                for (int i = 0; i < bbf.capacity(); i++) {
                    jmeImageBuffer.putShort(FastMath.convertFloatToHalf(bbf.get(i)));
                }
                jmeImageBuffer.flip();
                STBImage.stbi_image_free(bbf);
            }else{
                switch (compB.get(0)) {
                    case 1:
                        format = Format.Luminance8;
                        break;
                    case 2:
                        format = Format.Luminance8Alpha8;
                        break;
                    case 3:
                        format = Format.RGB8;
                        break;
                    case 4:
                        format = Format.RGBA8;
                        break;
                    default:
                        throw new AssetLoadException("Unsupported number of components: " + compB.get(0));
                }

                ByteBuffer bbf = STBImage.stbi_load_from_memory(imageBuffer, wB, hB, compB, 0);
                if (bbf == null) {
                    throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
                }

                jmeImageBuffer = BufferUtils.createByteBuffer(width * height * comp);
                for (int i = 0; i < bbf.capacity(); i++) {
                    jmeImageBuffer.put(bbf.get(i));
                }
                jmeImageBuffer.flip();
                STBImage.stbi_image_free(bbf);           

            }
            Image image = new Image(format, wB.get(0), hB.get(0), jmeImageBuffer,format.isFloatingPont()? ColorSpace.Linear : ColorSpace.sRGB);
            return image;
        }

 
    }
}
