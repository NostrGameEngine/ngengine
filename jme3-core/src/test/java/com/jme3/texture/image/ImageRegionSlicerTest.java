/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
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

import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageRegionSlicerTest {

    @Test
    public void slicesUncompressedRowsWithoutChangingFormat() {
        ByteBuffer data = BufferUtils.createByteBuffer(4 * 3 * 4);
        for (int pixel = 0; pixel < 12; pixel++) {
            data.put((byte) pixel).put((byte) (pixel + 1)).put((byte) (pixel + 2)).put((byte) 255);
        }
        data.flip();
        Image source = new Image(Image.Format.RGBA8, 4, 3, data, ColorSpace.sRGB);

        Image sliced = ImageRegionSlicer.slice(source, 1, 1, 2, 2);

        assertEquals(Image.Format.RGBA8, sliced.getFormat());
        assertEquals(ColorSpace.sRGB, sliced.getColorSpace());
        assertEquals(2, sliced.getWidth());
        assertEquals(2, sliced.getHeight());
        assertEquals(5, sliced.getData(0).get(0) & 0xff);
        assertEquals(6, sliced.getData(0).get(4) & 0xff);
        assertEquals(9, sliced.getData(0).get(8) & 0xff);
        assertEquals(10, sliced.getData(0).get(12) & 0xff);
    }

    @Test
    public void copiesAlignedDxtBlocksAndRejectsUnalignedRegions() {
        ByteBuffer data = BufferUtils.createByteBuffer(4 * 8);
        for (int i = 0; i < data.capacity(); i++) {
            data.put((byte) i);
        }
        data.flip();
        Image source = new Image(Image.Format.DXT1, 8, 8, data, ColorSpace.Linear);

        assertTrue(ImageRegionSlicer.canSlice(source, 4, 0, 4, 8));
        Image sliced = ImageRegionSlicer.slice(source, 4, 0, 4, 8);
        assertEquals(16, sliced.getData(0).remaining());
        assertEquals(8, sliced.getData(0).get(0) & 0xff);
        assertEquals(24, sliced.getData(0).get(8) & 0xff);

        assertFalse(ImageRegionSlicer.canSlice(source, 2, 0, 4, 4));
        assertThrows(IllegalArgumentException.class,
                () -> ImageRegionSlicer.slice(source, 2, 0, 4, 4));
    }

    @Test
    public void acceptsCompressedEdgeBlocksSmallerThanFourPixels() {
        ByteBuffer data = BufferUtils.createByteBuffer(8);
        data.putLong(0x0102030405060708L).flip();
        Image source = new Image(Image.Format.DXT1, 2, 2, data, ColorSpace.Linear);

        assertTrue(ImageRegionSlicer.canSlice(source, 0, 0, 2, 2));
        Image sliced = ImageRegionSlicer.slice(source, 0, 0, 2, 2);

        assertEquals(8, sliced.getData(0).remaining());
    }

    @Test
    public void supportsDxtAndEtcBlockLayouts() {
        assertCompressedLayout(Image.Format.DXT1, 8);
        assertCompressedLayout(Image.Format.DXT3, 16);
        assertCompressedLayout(Image.Format.DXT5, 16);
        assertCompressedLayout(Image.Format.ETC1, 8);
        assertCompressedLayout(Image.Format.ETC2_ALPHA1, 8);
        assertCompressedLayout(Image.Format.ETC2, 16);
    }

    private static void assertCompressedLayout(Image.Format format, int blockBytes) {
        ByteBuffer data = BufferUtils.createByteBuffer(4 * blockBytes);
        for (int i = 0; i < data.capacity(); i++) {
            data.put((byte) i);
        }
        data.flip();
        Image source = new Image(format, 8, 8, data, ColorSpace.Linear);

        Image sliced = ImageRegionSlicer.slice(source, 4, 0, 4, 8);

        assertEquals(2 * blockBytes, sliced.getData(0).remaining());
        assertEquals(blockBytes, sliced.getData(0).get(0) & 0xff);
        assertEquals(3 * blockBytes, sliced.getData(0).get(blockBytes) & 0xff);
    }
}
