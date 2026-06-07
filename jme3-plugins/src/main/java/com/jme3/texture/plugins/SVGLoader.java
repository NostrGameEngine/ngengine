package com.jme3.texture.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import org.ngengine.nanosvg.NanoSvgFitMode;
import org.ngengine.nanosvg.NanoSvgRenderResult;
import org.ngengine.nanosvg.NanoSvgRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SVGLoader implements AssetLoader {

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        int width = 256;
        int height = 256;
        boolean flipY = false;
        SVGTextureKey svgKey = null;

        if (key instanceof SVGTextureKey) {
            svgKey = (SVGTextureKey) key;
            width = svgKey.getWidth();
            height = svgKey.getHeight();
            flipY = svgKey.isFlipY();
        }

        try (InputStream in = assetInfo.openStream()) {
            String svgData = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("currentColor", "#ffffff");
            ByteBuffer input = ByteBuffer.wrap(svgData.getBytes(StandardCharsets.UTF_8));

            NanoSvgRenderer renderer = new NanoSvgRenderer(BufferUtils::createByteBuffer);
            NanoSvgRenderResult result;
            if (svgKey != null && svgKey.hasViewBoxClip()) {
                result = renderer.renderViewBox(
                        input,
                        width,
                        height,
                        Math.round(svgKey.getViewBoxX()),
                        Math.round(svgKey.getViewBoxY()),
                        Math.round(svgKey.getViewBoxWidth()),
                        Math.round(svgKey.getViewBoxHeight()),
                        NanoSvgFitMode.CONTAIN);
            } else {
                result = renderer.render(input, width, height);
            }
            ByteBuffer pixels = imageData(result, flipY);

            return new Image(Format.RGBA8, result.width(), result.height(), pixels, ColorSpace.sRGB);
        } catch (RuntimeException e) {
            throw new IOException("Error loading SVG: " + e.getMessage(), e);
        }
    }

    private ByteBuffer imageData(NanoSvgRenderResult result, boolean flipY) throws IOException {
        int rowSize = result.width() * 4;
        ByteBuffer pixels = result.pixels().duplicate();
        if (result.stride() == rowSize && !flipY) {
            pixels.rewind();
            return pixels;
        }

        if (result.stride() < rowSize) {
            throw new IOException("Invalid SVG output stride: " + result.stride() + " < " + rowSize);
        }

        ByteBuffer compact = BufferUtils.createByteBuffer(rowSize * result.height());
        byte[] row = new byte[rowSize];
        for (int y = 0; y < result.height(); y++) {
            int srcY = flipY ? result.height() - 1 - y : y;
            pixels.position(srcY * result.stride());
            pixels.get(row);
            compact.put(row);
        }
        compact.flip();
        return compact;
    }
}
