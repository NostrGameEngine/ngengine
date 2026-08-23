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
import com.jme3.util.ByteBufferUtils;
import java.nio.ByteBuffer;

/**
 * Extracts rectangular base-level regions without decoding supported packed
 * images. Block-compressed regions are accepted only when every edge follows
 * the compression block grid.
 */
public final class ImageRegionSlicer {

    private ImageRegionSlicer() {
    }

    /**
     * Returns whether the requested region can be copied without decoding.
     */
    public static boolean canSlice(Image image, int x, int y, int width, int height) {
        if (!validRegion(image, x, y, width, height) || image.getData() == null
                || image.getData().size() != 1 || image.getData(0) == null) {
            return false;
        }
        Image.Format format = image.getFormat();
        if (!format.isCompressed()) {
            int bitsPerPixel = format.getBitsPerPixel();
            long baseSize = (long) image.getWidth() * image.getHeight() * (bitsPerPixel / 8);
            return bitsPerPixel > 0 && (bitsPerPixel & 7) == 0
                    && image.getData(0).capacity() >= baseSize;
        }
        int blockBytes = compressedBlockBytes(format);
        long baseSize = (long) ((image.getWidth() + 3) / 4)
                * ((image.getHeight() + 3) / 4) * blockBytes;
        return blockBytes > 0 && image.getData(0).capacity() >= baseSize
                && blockAlignedRegion(x, width, image.getWidth())
                && blockAlignedRegion(y, height, image.getHeight());
    }

    /**
     * Extracts the base mip level of a region.
     *
     * @throws IllegalArgumentException if the region or image layout cannot be
     *         sliced losslessly
     */
    public static Image slice(Image image, int x, int y, int width, int height) {
        if (!canSlice(image, x, y, width, height)) {
            throw new IllegalArgumentException("Unsupported image region: " + x + "," + y
                    + " " + width + "x" + height + " in " + image);
        }

        int rowBytes;
        int sourceRowBytes;
        int sourceOffset;
        int rows;
        if (image.getFormat().isCompressed()) {
            int blockBytes = compressedBlockBytes(image.getFormat());
            int sourceBlocksWide = (image.getWidth() + 3) / 4;
            int regionBlocksWide = (width + 3) / 4;
            rowBytes = regionBlocksWide * blockBytes;
            sourceRowBytes = sourceBlocksWide * blockBytes;
            sourceOffset = ((y / 4) * sourceBlocksWide + x / 4) * blockBytes;
            rows = (height + 3) / 4;
        } else {
            int bytesPerPixel = image.getFormat().getBitsPerPixel() / 8;
            rowBytes = width * bytesPerPixel;
            sourceRowBytes = image.getWidth() * bytesPerPixel;
            sourceOffset = (y * image.getWidth() + x) * bytesPerPixel;
            rows = height;
        }

        ByteBuffer source = ByteBufferUtils.duplicate(image.getData(0));
        ByteBuffer target = BufferUtils.createByteBuffer(rowBytes * rows).order(source.order());
        for (int row = 0; row < rows; row++) {
            int start = sourceOffset + row * sourceRowBytes;
            source.position(start);
            source.limit(start + rowBytes);
            target.put(source);
            source.limit(source.capacity());
        }
        target.flip();
        return new Image(image.getFormat(), width, height, target, image.getColorSpace());
    }

    private static boolean validRegion(Image image, int x, int y, int width, int height) {
        return image != null && x >= 0 && y >= 0 && width > 0 && height > 0
                && x + width <= image.getWidth() && y + height <= image.getHeight();
    }

    private static boolean blockAlignedRegion(int start, int length, int imageLength) {
        return (start & 3) == 0
                && ((length & 3) == 0 || start + length == imageLength);
    }

    private static int compressedBlockBytes(Image.Format format) {
        switch (format) {
            case DXT1:
            case DXT1A:
            case ETC1:
            case ETC2_ALPHA1:
                return 8;
            case DXT3:
            case DXT5:
            case ETC2:
                return 16;
            default:
                return 0;
        }
    }
}
