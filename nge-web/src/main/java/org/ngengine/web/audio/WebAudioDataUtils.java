/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.ngengine.web.audio;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.teavm.jso.core.JSArray;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int8Array;

import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioStream;
import com.jme3.audio.Environment;
import com.jme3.util.res.Resources;

public class WebAudioDataUtils {
    static float[][] decodePcm(byte[] pcm, int channels, int bits, ByteOrder order,
            int sourceRate, int destinationRate) {
        if (bits != 8 && bits != 16 && bits != 24) {
            throw new UnsupportedOperationException("Unsupported bits per sample: " + bits);
        }
        if (channels <= 0) throw new IllegalArgumentException("Channel count must be positive");
        int bytesPerSample = bits / 8;
        int bytesPerFrame = channels * bytesPerSample;
        int sourceFrames = pcm.length / bytesPerFrame;
        int frames = resampledFrameCount(sourceFrames, sourceRate, destinationRate);
        float[][] result = new float[channels][frames];
        double sampleIncrement = (double) sourceRate / destinationRate;
        double negativeScale = 1 << (bits - 1);
        double positiveScale = negativeScale - 1;
        boolean littleEndian = order == ByteOrder.LITTLE_ENDIAN;
        for (int channel = 0; channel < channels; channel++) {
            float[] output = result[channel];
            for (int frame = 0; frame < frames; frame++) {
                int sourceFrame = Math.min(sourceFrames - 1, (int) (frame * sampleIncrement));
                int offset = sourceFrame * bytesPerFrame + channel * bytesPerSample;
                int sample = 0;
                for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
                    int shift = (littleEndian ? byteIndex : bytesPerSample - byteIndex - 1) * 8;
                    sample |= (pcm[offset + byteIndex] & 0xff) << shift;
                }
                sample = sample << (32 - bits) >> (32 - bits);
                output[frame] = (float) (sample / (sample < 0 ? negativeScale : positiveScale));
            }
        }
        return result;
    }

    private static JSArray<Float32Array> audioDataToF32(AudioData audio, byte[] pcm, ByteOrder order,
            int sourceRate, int destinationRate) {
        float[][] channels = decodePcm(pcm, audio.getChannels(), audio.getBitsPerSample(), order,
                sourceRate, destinationRate);
        JSArray<Float32Array> result = new JSArray<>(channels.length);
        // Cross the Wasm/JS boundary once per channel, not several times per sample.
        for (int channel = 0; channel < channels.length; channel++) {
            result.set(channel, Float32Array.copyFromJavaArray(channels[channel]));
        }
        return result;
    }

    private static JSArray<Float32Array> getF32Data(
            AudioStream ab, int srcSampleRate, int destSampleRate) {
        int bytesPerFrame = ab.getChannels() * (ab.getBitsPerSample() / 8);
        int expectedSize = Math.max(0, (int) (ab.getDuration() * srcSampleRate) * bytesPerFrame);
        ByteArrayOutputStream decodedPcm = new ByteArrayOutputStream(expectedSize);
        byte chunk[] = new byte[1024];
        int read;
        while ((read = ab.readSamples(chunk)) > 0) {
            decodedPcm.write(chunk, 0, read);
        }
        byte[] pcm = decodedPcm.toByteArray();
        return audioDataToF32(ab, pcm, ByteOrder.nativeOrder(), srcSampleRate, destSampleRate);

    }

    private static JSArray<Float32Array> getF32Data(
            AudioBuffer ab, int srcSampleRate, int destSampleRate) {
        ByteBuffer source = ab.getData();
        ByteBuffer inputData = source.duplicate().order(source.order());
        inputData.rewind();
        byte[] pcm = new byte[inputData.remaining()];
        inputData.get(pcm);
        return audioDataToF32(ab, pcm, inputData.order(), srcSampleRate, destSampleRate);
    }

    private static int resampledFrameCount(int sourceFrames, int sourceRate, int destinationRate) {
        if (sourceFrames == 0 || sourceRate <= 0 || destinationRate <= 0) return 0;
        return Math.max(1, (int) Math.ceil(
                (double) sourceFrames * destinationRate / sourceRate));
    }

    public static JSArray<Float32Array> getF32Data(AudioData ab, int destSampleRate) {
        int srcSampleRate = ab.getSampleRate();
        if (ab instanceof AudioStream) {
            return getF32Data((AudioStream) ab, srcSampleRate, destSampleRate);
        } else if (ab instanceof AudioBuffer) {
            return getF32Data((AudioBuffer) ab, srcSampleRate, destSampleRate);
        } else {
            throw new UnsupportedOperationException("Unsupported AudioData type: " + ab.getClass().getName());
        }
    }
  public static Int8Array getEnvData(Environment currentEnv) {
            String impulseName = "";
            if (currentEnv == null) throw new RuntimeException("No environment set");
            if (currentEnv == Environment.AcousticLab) {
                impulseName = "Basement.m4a";
            } else if (currentEnv == Environment.Cavern) {
                impulseName = "EmptyApartmentBedroom.m4a";
            } else if (currentEnv == Environment.Closet) {
                impulseName = "Basement.m4a";
            } else if (currentEnv == Environment.Dungeon) {
                impulseName = "PurnodesRailroadTunnel.m4a";
            } else if (currentEnv == Environment.Garage) {
                impulseName = "Basement.m4a";
            }
            InputStream is = Resources.getResourceAsStream("org/ngengine/web/impulse/"+impulseName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte buff[] = new byte[1024];
            int read = 0;
            try {
                while ((read = is.read(buff)) != -1) {
                    baos.write(buff, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            buff = baos.toByteArray();
            Int8Array i8 = new Int8Array(buff.length);
            for (int i = 0; i < buff.length; i++) {
                i8.set(i, buff[i]);
            }          
            return i8;
        }
}
