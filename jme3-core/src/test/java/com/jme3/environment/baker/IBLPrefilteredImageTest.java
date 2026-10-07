package com.jme3.environment.baker;

import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.system.NullRenderer;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IBLPrefilteredImageTest {
    private static final int[] PULLED_512_RGBA16F_SIZES = {2097152, 524288, 131072, 32768, 8192, 2048};

    private static Image[] maps() {
        RenderManager manager = new RenderManager(new NullRenderer());
        return new Image[] {
                new IBLGLEnvBaker(manager, null, Image.Format.RGBA16F, Image.Format.Depth, 512, 512, 32, 32).getSpecularIBL().getImage(),
                new IBLHybridEnvBakerLight(manager, null, Image.Format.RGBA16F, Image.Format.Depth, 512, 512).getSpecularIBL().getImage(),
                new IBLGLEnvBakerLight(manager, null, Image.Format.RGBA16F, Image.Format.Depth, 512, 512).getSpecularIBL().getImage()
        };
    }

    @Test void limitedPrefilteredChainRetainsActualBaseDimensions() {
        for (Image map : maps()) {
            assertEquals(512, map.getWidth());
            assertArrayEquals(PULLED_512_RGBA16F_SIZES, map.getMipMapSizes());
        }
    }

    @Test void pulledMipPixelsCanBeReadAtEveryLevelWithoutOverlappingOffsets() {
        ByteBuffer packed = BufferUtils.createByteBuffer(Arrays.stream(PULLED_512_RGBA16F_SIZES).sum());
        Image pulled = new Image(Image.Format.RGBA16F, 512, 512, packed, ColorSpace.Linear);
        pulled.setMipMapSizes(PULLED_512_RGBA16F_SIZES.clone());
        for (int level = 0; level < 6; level++) {
            float marker = (level + 1) / 8f;
            ImageRaster.create(pulled, 0, level, false).setPixel(0, 0, new ColorRGBA(marker, marker / 2, 1, 1));
        }
        for (Image map : maps()) {
            map.setData(0, packed);
            for (int level = 0; level < 6; level++) {
                ColorRGBA pixel = ImageRaster.create(map, 0, level, false).getPixel(0, 0);
                assertEquals((level + 1) / 8f, pixel.r, 1e-5f, "pulled red mip " + level);
                assertEquals((level + 1) / 16f, pixel.g, 1e-5f, "pulled green mip " + level);
                assertEquals(1, pixel.b, 1e-5f);
            }
        }
    }
}
