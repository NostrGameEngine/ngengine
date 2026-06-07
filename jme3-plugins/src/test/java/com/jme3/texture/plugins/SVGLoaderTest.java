package com.jme3.texture.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.texture.Image;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SVGLoaderTest {

    @Test
    public void testLoadSvgTexture() throws IOException {
        byte[] svg = ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"20\" viewBox=\"0 0 10 20\">"
                + "<rect width=\"10\" height=\"20\" fill=\"#ff0000\"/></svg>")
                .getBytes(StandardCharsets.UTF_8);
        SVGTextureKey key = new SVGTextureKey("test.svg", 16, 32);
        AssetInfo info = new AssetInfo(null, key) {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(svg);
            }
        };

        Image image = (Image) new SVGLoader().load(info);

        Assertions.assertEquals(Image.Format.RGBA8, image.getFormat());
        Assertions.assertEquals(16, image.getWidth());
        Assertions.assertEquals(32, image.getHeight());

        ByteBuffer data = image.getData(0).duplicate();
        int center = ((16 * 16) + 8) * 4;
        Assertions.assertTrue(Byte.toUnsignedInt(data.get(center)) > 0);
        Assertions.assertTrue(Byte.toUnsignedInt(data.get(center + 3)) > 0);
    }

    @Test
    public void testLoadSvgTextureWithViewBoxClip() throws IOException {
        byte[] svg = ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"10\" viewBox=\"0 0 20 10\">"
                + "<rect x=\"0\" y=\"0\" width=\"10\" height=\"10\" fill=\"#ff0000\"/>"
                + "<rect x=\"10\" y=\"0\" width=\"10\" height=\"10\" fill=\"#0000ff\"/></svg>")
                .getBytes(StandardCharsets.UTF_8);
        SVGTextureKey key = new SVGTextureKey("test.svg", 12, 12, 10, 0, 10, 10);
        AssetInfo info = new AssetInfo(null, key) {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(svg);
            }
        };

        Image image = (Image) new SVGLoader().load(info);

        Assertions.assertEquals(12, image.getWidth());
        Assertions.assertEquals(12, image.getHeight());

        ByteBuffer data = image.getData(0).duplicate();
        int center = ((6 * 12) + 6) * 4;
        Assertions.assertTrue(Byte.toUnsignedInt(data.get(center + 2)) > 0);
        Assertions.assertTrue(Byte.toUnsignedInt(data.get(center + 3)) > 0);
    }
}
