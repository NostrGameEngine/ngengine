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

package ngetests.tests.benchmark;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

/**
 * Manual benchmark for measuring full vertex-buffer uploads with many moving points.
 */
public class TestVertexBufferParticleBenchmark extends SimpleApplication {

    private static final int DEFAULT_PARTICLES = 250_000;
    private static final int DEFAULT_MOVING_PARTICLES = 25_000;

    private final int particleCount = Math.max(1, Integer.getInteger("particle.count", DEFAULT_PARTICLES));
    private final int movingCount = Math.max(0,
            Math.min(Integer.getInteger("particle.moving", DEFAULT_MOVING_PARTICLES), particleCount));
    private final float[] x = new float[particleCount];
    private final float[] y = new float[particleCount];
    private final float[] vx = new float[particleCount];
    private final float[] vy = new float[particleCount];

    private VertexBuffer positionBuffer;
    private FloatBuffer positions;
    private BitmapText hud;
    private int movingStart;
    private float statsTime;
    private int statsFrames;
    private float fps;

    /**
     * Starts the manual benchmark application.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        TestVertexBufferParticleBenchmark app = new TestVertexBufferParticleBenchmark();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Full VertexBuffer Particle Benchmark");
        settings.setResolution(1280, 720);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    /**
     * Initializes the particle mesh and HUD.
     */
    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(200f);
        cam.setLocation(new Vector3f(0f, 0f, 260f));
        initParticles();
        initMesh();
        initHud();
    }

    /**
     * Advances moving particles and refreshes the benchmark counters.
     *
     * @param tpf time per frame
     */
    @Override
    public void simpleUpdate(float tpf) {
        updateParticles(tpf);
        updateStats(tpf);
    }

    private void initParticles() {
        int columns = (int) Math.ceil(Math.sqrt(particleCount));
        float spacing = 0.45f;
        float half = columns * spacing * 0.5f;

        for (int i = 0; i < particleCount; i++) {
            x[i] = (i % columns) * spacing - half;
            y[i] = (i / columns) * spacing - half;
            vx[i] = 15f + (i % 37) * 0.17f;
            vy[i] = 10f + (i % 53) * 0.13f;
        }
    }

    private void initMesh() {
        positions = BufferUtils.createFloatBuffer(particleCount * 3);
        for (int i = 0; i < particleCount; i++) {
            putPosition(i);
        }

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Points);
        positionBuffer = new VertexBuffer(VertexBuffer.Type.Position);
        positionBuffer.setupData(VertexBuffer.Usage.Stream, 3, VertexBuffer.Format.Float, positions);
        mesh.setBuffer(positionBuffer);
        mesh.setBound(new BoundingBox(Vector3f.ZERO, 140f, 140f, 20f));
        mesh.updateCounts();

        Geometry geometry = new Geometry("particles", mesh);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.Cyan);
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Opaque);
        rootNode.attachChild(geometry);
    }

    private void initHud() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hud = new BitmapText(guiFont);
        hud.setSize(18f);
        hud.setLocalTranslation(12f, settings.getHeight() - 12f, 0f);
        guiNode.attachChild(hud);
    }

    private void updateParticles(float tpf) {
        int start = movingStart;
        for (int n = 0; n < movingCount; n++) {
            int i = (start + n) % particleCount;
            x[i] += vx[i] * tpf;
            y[i] += vy[i] * tpf;

            if (x[i] > 120f || x[i] < -120f) {
                vx[i] = -vx[i];
            }
            if (y[i] > 120f || y[i] < -120f) {
                vy[i] = -vy[i];
            }
            putPosition(i);
        }

        positionBuffer.updateData(positions);
        movingStart = (movingStart + movingCount) % particleCount;
    }

    private void putPosition(int particle) {
        int offset = particle * 3;
        positions.put(offset, x[particle]);
        positions.put(offset + 1, y[particle]);
        positions.put(offset + 2, 0f);
    }

    private void updateStats(float tpf) {
        statsTime += tpf;
        statsFrames++;
        if (statsTime >= 0.5f) {
            fps = statsFrames / statsTime;
            statsTime = 0f;
            statsFrames = 0;
        }

        hud.setText("Particles: " + particleCount
                + "  Moving/frame: " + movingCount
                + "  Upload: full updateData"
                + "  FPS: " + FastMath.floor(fps)
                + "\nSet -Dparticle.count and -Dparticle.moving to match the jMonkeyEngine benchmark.");
    }
}
