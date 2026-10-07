package com.jme3.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class JmeDesktopSystemImageFileTest {
    @Test
    void pngPreservesRgbaChannelsAndFlipsFramebufferRows() throws Exception {
        ByteBuffer pixels = ByteBuffer.wrap(new byte[] {
            (byte) 255, 0, 0, (byte) 255, 0, (byte) 255, 0, (byte) 128,
            0, 0, (byte) 255, (byte) 255, (byte) 255, 0, (byte) 255, 64
        });
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new JmeDesktopSystem().writeImageFile(output, "png", pixels, 2, 2);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));

        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(0xff0000ff, image.getRGB(0, 0));
        assertEquals(0x40ff00ff, image.getRGB(1, 0));
        assertEquals(0xffff0000, image.getRGB(0, 1));
        assertEquals(0x8000ff00, image.getRGB(1, 1));
        assertEquals(0, pixels.position());
    }
}
