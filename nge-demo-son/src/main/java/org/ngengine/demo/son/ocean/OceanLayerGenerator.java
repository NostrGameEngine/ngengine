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

            Vector2f tileSize, int resolution, int layers, Vector3f patternScale, Vector3f patternWind) {
        ArrayList<IBOceanLayer> oceanLayers = new ArrayList<>(layers);
        for (int i = 0; i < layers; i++) {
            System.out.println("Generating layer " + i + " with time offset " + i);
            float timeOffset = i * 1.0f; // e.g. each layer is 1 unit of time apart
            IBOceanLayer layer = generateLayer(tileSize, resolution, i, timeOffset, patternScale,
                    patternWind);
            oceanLayers.add(layer);
        }
        return new IBOcean(tileSize, oceanLayers, patternScale);
    }

    public static IBOceanLayer generateLayer(Vector2f tileSize, int resolution, int layerIndex,
            float timeOffset, Vector3f patternScale, Vector3f patternWind) {

        ByteBuffer data = BufferUtils.createByteBuffer(resolution * resolution * 4);
        Image img = new Image(Format.RGBA8, resolution, resolution, data, ColorSpace.Linear);

        AsyncExecutor executor = NGEPlatform.get().newAsyncExecutor();
        try {

            List<AsyncTask<Object>> waitList = new ArrayList<>();

            for (int j = 0; j < resolution; j++) {
                final int jF = j;

                waitList.add(NGEPlatform.get().promisify((res, rej) -> {

                    float vNorm = jF / (float) (resolution - 1);
                    for (int i = 0; i < resolution; i++) {
                        float uNorm = i / (float) (resolution - 1);

                        // Compute world‐space x,z that correspond to (uNorm, vNorm) in [0, DOMAIN_SIZE)
                        float worldX = uNorm * tileSize.x;
                        float worldZ = vNorm * tileSize.y;
                        Vector3f wpos = new Vector3f(worldX, 0f, worldZ);

                        // Sample sampleOcean(wpos, wind, scale, timeOffset)
                        Vector4f result = OceanWaveSim.sampleOcean(Math.max(tileSize.x, tileSize.y), wpos,
                                patternWind, patternScale, timeOffset);

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
                }, executor));

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
