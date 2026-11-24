/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system;

import com.jme3.math.FastMath;
import com.jme3.math.Line;
import com.jme3.math.Vector3f;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClockAndFastMathSeedTest {

    @AfterEach
    public void tearDown() {
        Clock.resetProvider();
    }

    @Test
    public void testClockCustomProviderAndNanoTimer() {
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        AtomicLong millis = new AtomicLong(123456789L);

        Clock.setProvider(new Clock.Provider() {
            @Override
            public long nanoTime() {
                return nanos.get();
            }

            @Override
            public long currentTimeMillis() {
                return millis.get();
            }

            @Override
            public Instant now() {
                return Instant.ofEpochMilli(millis.get());
            }
        });

        assertEquals(1_000_000_000L, Clock.nanoTime());
        assertEquals(123456789L, Clock.currentTimeMillis());
        assertEquals(Instant.ofEpochMilli(123456789L), Clock.now());

        NanoTimer timer = new NanoTimer();
        timer.reset();
        nanos.addAndGet(500_000_000L);
        timer.update();
        assertEquals(0.5f, timer.getTimePerFrame(), 1e-6f);
        assertEquals(0.5f, timer.getTimeInSeconds(), 1e-6f);
    }

    @Test
    public void testFastMathSeedDeterminismAndReseed() {
        FastMath.setSeed(987654321L);
        assertEquals(987654321L, FastMath.getSeed());
        float f1 = FastMath.nextRandomFloat();
        int i1 = FastMath.nextRandomInt();
        long l1 = FastMath.nextRandomLong();
        Vector3f p1 = new Line(Vector3f.ZERO, Vector3f.UNIT_X).random(null);

        // Re-seeding with the same seed must replay the exact sequence on the same thread.
        FastMath.setSeed(987654321L);
        assertEquals(f1, FastMath.nextRandomFloat());
        assertEquals(i1, FastMath.nextRandomInt());
        assertEquals(l1, FastMath.nextRandomLong());
        assertEquals(p1, new Line(Vector3f.ZERO, Vector3f.UNIT_X).random(null));

        // Legacy FastMath.rand.setSeed must also re-seed deterministically.
        FastMath.rand.setSeed(987654321L);
        assertEquals(f1, FastMath.rand.nextFloat());
    }

    @Test
    public void testFastMathThreadLocalIsolationByThreadName() throws Exception {
        FastMath.setSeed(42L);
        long[] workerA = new long[2];
        long[] workerB = new long[1];

        Thread t1 = new Thread(() -> workerA[0] = FastMath.nextRandomLong(), "sim-worker-A");
        t1.start();
        t1.join();

        Thread t2 = new Thread(() -> workerB[0] = FastMath.nextRandomLong(), "sim-worker-B");
        t2.start();
        t2.join();

        // Advance main thread RNG; another thread with the same name after reseed must get the same value.
        FastMath.nextRandomLong();
        FastMath.setSeed(42L);
        Thread t3 = new Thread(() -> workerA[1] = FastMath.nextRandomLong(), "sim-worker-A");
        t3.start();
        t3.join();

        assertEquals(workerA[0], workerA[1]);
        assertNotEquals(workerA[0], workerB[0]);
    }

    @Test
    public void testAppSettingsRandomSeedRoundTrip() throws Exception {
        AppSettings settings = new AppSettings(true);
        assertNull(settings.getRandomSeed());

        settings.setRandomSeed(1234567890123L);
        assertEquals(1234567890123L, settings.getRandomSeed());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        settings.save(out);

        AppSettings loaded = new AppSettings(false);
        loaded.load(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1234567890123L, loaded.getRandomSeed());

        loaded.setRandomSeed(null);
        assertNull(loaded.getRandomSeed());
    }
}
