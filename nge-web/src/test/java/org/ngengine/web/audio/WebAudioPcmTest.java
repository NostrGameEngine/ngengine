package org.ngengine.web.audio;

import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebAudioPcmTest {
    @Test
    void convertsSignedEndpointsForEverySupportedWidthAndByteOrder() {
        for (int bits : new int[] {8, 16, 24}) {
            for (ByteOrder order : new ByteOrder[] {ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
                int bytes = bits / 8;
                byte[] pcm = new byte[bytes * 3];
                int[] samples = {-(1 << (bits - 1)), 0, (1 << (bits - 1)) - 1};
                for (int frame = 0; frame < samples.length; frame++) {
                    for (int b = 0; b < bytes; b++) {
                        int shift = (order == ByteOrder.LITTLE_ENDIAN ? b : bytes - b - 1) * 8;
                        pcm[frame * bytes + b] = (byte) (samples[frame] >> shift);
                    }
                }
                assertArrayEquals(new float[] {-1f, 0f, 1f},
                        WebAudioDataUtils.decodePcm(pcm, 1, bits, order, 48000, 48000)[0]);
            }
        }
    }

    @Test
    void deinterleavesAndResamplesWithoutCrossingChannelBoundaries() {
        byte[] pcm = {0, 127, 64, -128};
        float[][] up = WebAudioDataUtils.decodePcm(pcm, 2, 8, ByteOrder.LITTLE_ENDIAN, 24000, 48000);
        assertArrayEquals(new float[] {0f, 0f, 64f / 127f, 64f / 127f}, up[0]);
        assertArrayEquals(new float[] {1f, 1f, -1f, -1f}, up[1]);
        float[][] down = WebAudioDataUtils.decodePcm(pcm, 2, 8, ByteOrder.LITTLE_ENDIAN, 48000, 24000);
        assertArrayEquals(new float[] {0f}, down[0]);
        assertArrayEquals(new float[] {1f}, down[1]);
    }

    @Test
    void ignoresPartialFramesAndHandlesFractionalRates() {
        byte[] pcm = {0, 0, -1, 127, 42};
        float[][] data = WebAudioDataUtils.decodePcm(pcm, 1, 16, ByteOrder.LITTLE_ENDIAN, 44100, 48000);
        assertArrayEquals(new float[] {0f, 0f, 1f}, data[0]);
        assertEquals(0, WebAudioDataUtils.decodePcm(new byte[0], 2, 16,
                ByteOrder.LITTLE_ENDIAN, 44100, 48000)[0].length);
    }

    @Test
    void rejectsUnsupportedFormats() {
        assertThrows(UnsupportedOperationException.class, () -> WebAudioDataUtils.decodePcm(
                new byte[4], 1, 32, ByteOrder.LITTLE_ENDIAN, 48000, 48000));
        assertThrows(IllegalArgumentException.class, () -> WebAudioDataUtils.decodePcm(
                new byte[4], 0, 16, ByteOrder.LITTLE_ENDIAN, 48000, 48000));
    }
}
