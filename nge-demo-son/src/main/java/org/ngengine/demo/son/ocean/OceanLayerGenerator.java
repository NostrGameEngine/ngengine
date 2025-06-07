/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.ocean;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;

public class OceanLayerGenerator {

    public static IBOcean generateOcean(
        Vector2f tileSize,
        int resolution,
        int layers,
        Vector3f patternScale,
        Vector3f patternWind
    ) {
        ArrayList<IBOceanLayer> oceanLayers = new ArrayList<>(layers);
        for (int i = 0; i < layers; i++) {
            System.out.println("Generating layer " + i + " with time offset " + i);
            float timeOffset = i * 1.0f; // e.g. each layer is 1 unit of time apart
            IBOceanLayer layer = generateLayer(tileSize, resolution, i, timeOffset, patternScale, patternWind);
            oceanLayers.add(layer);
        }
        return new IBOcean(tileSize, oceanLayers, patternScale);
    }

    public static IBOceanLayer generateLayer(
        Vector2f tileSize,
        int resolution,
        int layerIndex,
        float timeOffset,
        Vector3f patternScale,
        Vector3f patternWind
    ) {
        ByteBuffer data = BufferUtils.createByteBuffer(resolution * resolution * 4);
        Image img = new Image(Format.RGBA8, resolution, resolution, data, ColorSpace.Linear);

        AsyncExecutor executor = NGEPlatform.get().newAsyncExecutor();
        try {
            List<AsyncTask<Object>> waitList = new ArrayList<>();

            for (int j = 0; j < resolution; j++) {
                final int jF = j;

                waitList.add(
                    NGEPlatform
                        .get()
                        .promisify(
                            (res, rej) -> {
                                float vNorm = jF / (float) (resolution - 1);
                                for (int i = 0; i < resolution; i++) {
                                    float uNorm = i / (float) (resolution - 1);

                                    // Compute world‐space x,z that correspond to (uNorm, vNorm) in [0, DOMAIN_SIZE)
                                    float worldX = uNorm * tileSize.x;
                                    float worldZ = vNorm * tileSize.y;
                                    Vector3f wpos = new Vector3f(worldX, 0f, worldZ);

                                    // Sample sampleOcean(wpos, wind, scale, timeOffset)
                                    Vector4f result = OceanWaveSim.sampleOcean(
                                        Math.max(tileSize.x, tileSize.y),
                                        wpos,
                                        patternWind,
                                        patternScale,
                                        timeOffset
                                    );

                                    // Encode normal (result.normal) in RGB, height in A
                                    int nx = (int) ((result.x * 0.5f + 0.5f) * 255f);
                                    int ny = (int) ((result.y * 0.5f + 0.5f) * 255f);
                                    int nz = (int) ((result.z * 0.5f + 0.5f) * 255f);
                                    int hByte = (int) (FastMath.clamp(result.w, 0f, 1f) * 255f);

                                    int baseI = (jF * resolution + i) * 4;
                                    data.put(baseI, (byte) (nx & 0xFF));
                                    data.put(baseI + 1, (byte) (ny & 0xFF));
                                    data.put(baseI + 2, (byte) (nz & 0xFF));
                                    data.put(baseI + 3, (byte) (hByte & 0xFF));
                                    res.accept(null);
                                }
                            },
                            executor
                        )
                );
            }

            System.out.println("Waiting for " + waitList.size() + " tasks to finish...");
            NGEPlatform.get().awaitAll(waitList).await();
            System.out.println("All tasks finished, writing data to image...");
            data.position(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.close();
        }
        return new IBOceanLayer(img);
    }
}
