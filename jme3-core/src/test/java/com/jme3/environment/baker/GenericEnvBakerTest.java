/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.environment.baker;

import com.jme3.texture.Image.Format;
import com.jme3.texture.TextureCubeMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericEnvBakerTest {

    @Test
    void truncatedSpecularMipMetadataMatchesTheActualFaceDimensions() {
        IBLEnvBakerLight[] bakers = {
            new IBLHybridEnvBakerLight(null, null, Format.RGBA16F, Format.Depth, 256, 256),
            new IBLGLEnvBakerLight(null, null, Format.RGBA16F, Format.Depth, 256, 256)
        };
        int[] expected = {524288, 131072, 32768, 8192, 2048, 512};
        for (IBLEnvBakerLight baker : bakers) {
            assertArrayEquals(expected, baker.getSpecularIBL().getImage().getMipMapSizes());
        }
        IBLGLEnvBaker full = new IBLGLEnvBaker(null, null, Format.RGBA16F, Format.Depth,
                256, 256, 8, 8);
        assertArrayEquals(expected, full.getSpecularIBL().getImage().getMipMapSizes());
    }

    @Test
    void pulledTextureDataUsesNativeBuffers() throws IOException {
        TestBaker baker = new TestBaker();
        TextureCubeMap texture = new TextureCubeMap(1, 1, Format.RGBA8);
        byte[] expected = {1, 2, 3, 4};

        baker.finishPulling(texture, expected);

        ByteBuffer data = texture.getImage().getData(0);
        byte[] actual = new byte[data.remaining()];
        data.get(actual);
        assertTrue(data.isDirect());
        assertArrayEquals(expected, actual);
    }

    private static final class TestBaker extends GenericEnvBaker {

        private TestBaker() {
            super(null, null, Format.RGBA8, Format.Depth, 1);
        }

        private void finishPulling(TextureCubeMap texture, byte[] data) throws IOException {
            bos.add(new java.io.ByteArrayOutputStream());
            bos.get(0).write(data);
            endPulling(texture);
        }
    }
}
