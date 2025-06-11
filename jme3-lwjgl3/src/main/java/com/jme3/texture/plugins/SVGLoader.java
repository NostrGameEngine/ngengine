package com.jme3.texture.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.system.MemoryStack;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

public class SVGLoader implements AssetLoader {

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        int width = 256;
        int height = 256;
        boolean flipY = false;

        if (key instanceof SVGTextureKey) {
            SVGTextureKey svgKey = (SVGTextureKey) key;
            width = svgKey.getWidth();
            height = svgKey.getHeight();
            flipY = svgKey.isFlipY();
        }

        try (InputStream in = assetInfo.openStream()) {
            String svgData = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("currentColor", "#ffffff");

            byte[] data = svgData.getBytes(StandardCharsets.UTF_8);
            ByteBuffer inBuffer = BufferUtils.createByteBuffer(data.length + 1);
            inBuffer.put(data).put((byte) 0).flip(); 
            long rast = NanoSVG.nsvgCreateRasterizer();
            if (rast == 0) {
                throw new IllegalStateException("Failed to create SVG rasterizer.");
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                NSVGImage svg = NanoSVG.nsvgParse(inBuffer, stack.ASCII("px"), 96.0f);
                if (svg == null) {
                    throw new IOException("Failed to parse SVG data.");
                }
                float svgWidth = svg.width();
                float svgHeight = svg.height();
                if (svgWidth <= 0 || svgHeight <= 0) {
                    throw new IOException("Invalid SVG dimensions: " + svgWidth + "x" + svgHeight);
                }

                float scaleX = (float) width / svgWidth;
                float scaleY = (float) height / svgHeight;
                float scale = Math.min(scaleX, scaleY);

                float scaledW = svgWidth * scale;
                float scaledH = svgHeight * scale;
                float tx = (width - scaledW) / 2.0f;
                float ty = (height - scaledH) / 2.0f;

            

                int bufferSize = width * height * 4;
                ByteBuffer dst = BufferUtils.createByteBuffer(bufferSize);
                if (dst.remaining() < bufferSize) {
                    throw new IOException("Destination buffer too small: " + dst.remaining() + " < " + bufferSize);
                }

                NanoSVG.nsvgRasterize(rast, svg, tx, ty, scale, dst, width, height, width * 4);
                dst.rewind();

                if (flipY) {
                    flipImageData(dst, width, height);
                }

                // Copy memory to a tracked buffer
                byte[] imageData = new byte[dst.remaining()];
                dst.get(imageData);
                ByteBuffer outBuf = BufferUtils.createByteBuffer(imageData);

                NanoSVG.nsvgDeleteRasterizer(rast);
                NanoSVG.nsvgDelete(svg);

                Image image = new Image(Format.RGBA8, width, height, outBuf, ColorSpace.sRGB);
                return image;

            } 

        } catch (Exception e) {
            throw new IOException("Error loading SVG: " + e.getMessage(), e);
        }
    }

    private void flipImageData(ByteBuffer buffer, int width, int height) {
        byte[] tempRow = new byte[width * 4];
        for (int y = 0; y < height / 2; y++) {
            int topOffset = y * width * 4;
            int bottomOffset = (height - y - 1) * width * 4;

            buffer.position(topOffset);
            buffer.get(tempRow);

            buffer.position(bottomOffset);
            byte[] bottomRow = new byte[width * 4];
            buffer.get(bottomRow);
            buffer.position(topOffset);
            buffer.put(bottomRow);

            buffer.position(bottomOffset);
            buffer.put(tempRow);
        }
        buffer.rewind();
    }
}
