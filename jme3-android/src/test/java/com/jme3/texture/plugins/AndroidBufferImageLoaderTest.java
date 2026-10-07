package com.jme3.texture.plugins;

import android.graphics.Bitmap;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AndroidBufferImageLoaderTest {
    @Test
    void rgbaChannelsIncludingTheOddMiddleRowHaveAFullUploadBuffer() {
        Bitmap bitmap = mock(Bitmap.class);
        when(bitmap.getConfig()).thenReturn(Bitmap.Config.ARGB_8888);
        when(bitmap.getWidth()).thenReturn(1);
        when(bitmap.getHeight()).thenReturn(3);
        doAnswer(call -> {
            int[] pixels = call.getArgument(0);
            pixels[0] = 0xff112233; pixels[1] = 0x80445566; pixels[2] = 0xff778899;
            return null;
        }).when(bitmap).getPixels(any(int[].class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        for (boolean flip : new boolean[]{false, true}) {
            Image image = AndroidBufferImageLoader.readBitmap(bitmap, new TextureKey("test", flip));
            ByteBuffer data = image.getData(0);
            assertEquals(12, data.remaining());
            assertEquals(flip ? 0x77 : 0x11, Byte.toUnsignedInt(data.get(0)));
            assertEquals(0x44, Byte.toUnsignedInt(data.get(4)));
            assertEquals(0x55, Byte.toUnsignedInt(data.get(5)));
            assertEquals(0x66, Byte.toUnsignedInt(data.get(6)));
            assertEquals(0x80, Byte.toUnsignedInt(data.get(7)));
        }
    }

    @Test
    void halfFloatAndPackedFormatsPreserveBytesAndDiscardRowPadding() {
        for (Bitmap.Config config : new Bitmap.Config[]{Bitmap.Config.RGBA_F16, Bitmap.Config.RGB_565, Bitmap.Config.ALPHA_8}) {
            int bpp = config == Bitmap.Config.RGBA_F16 ? 8 : config == Bitmap.Config.RGB_565 ? 2 : 1;
            Bitmap bitmap = mock(Bitmap.class);
            when(bitmap.getConfig()).thenReturn(config);
            when(bitmap.getWidth()).thenReturn(1); when(bitmap.getHeight()).thenReturn(2);
            when(bitmap.getRowBytes()).thenReturn(bpp + 4);
            doAnswer(call -> {
                ByteBuffer data = call.getArgument(0);
                for (int row = 0; row < 2; row++) {
                    for (int i = 0; i < bpp; i++) data.put((byte) (row * 20 + i));
                    data.putInt(-1);
                }
                return null;
            }).when(bitmap).copyPixelsToBuffer(any());
            Image image = AndroidBufferImageLoader.readBitmap(bitmap, new TextureKey("test", true));
            assertEquals(bpp * 2, image.getData(0).remaining());
            assertEquals(20, image.getData(0).get(0));
            assertEquals(0, image.getData(0).get(bpp));
            if (bpp == 8) assertEquals(Image.Format.RGBA16F, image.getFormat());
        }
    }
}
