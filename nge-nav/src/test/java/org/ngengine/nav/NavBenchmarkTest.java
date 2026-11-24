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

package org.ngengine.nav;

import static org.junit.jupiter.api.Assertions.*;
import static org.ngengine.nav.NavWorldTest.*;

import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("benchmark")
class NavBenchmarkTest {

    @Test
    void largeMeshBuildAndRouteBenchmark() {
        int n = 64;
        float size = 0.25f;
        float[] triangles = new float[n * n * 18];
        int cursor = 0;
        for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
            float x0 = x * size, x1 = (x + 1) * size, z0 = z * size, z1 = (z + 1) * size;
            triangles[cursor++] = x0;
            triangles[cursor++] = 0;
            triangles[cursor++] = z0;
            triangles[cursor++] = x1;
            triangles[cursor++] = 0;
            triangles[cursor++] = z0;
            triangles[cursor++] = x1;
            triangles[cursor++] = 0;
            triangles[cursor++] = z1;
            triangles[cursor++] = x0;
            triangles[cursor++] = 0;
            triangles[cursor++] = z0;
            triangles[cursor++] = x1;
            triangles[cursor++] = 0;
            triangles[cursor++] = z1;
            triangles[cursor++] = x0;
            triangles[cursor++] = 0;
            triangles[cursor++] = z1;
        }
        long start = System.nanoTime();
        NavGeometry geometry = NavGeometry.triangles(triangles, false, 0, 3);
        long capture = System.nanoTime() - start;
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(size).build())) {
            start = System.nanoTime();
            world.layer("terrain").add(geometry);
            finish(world);
            long build = System.nanoTime() - start;
            NavView view = world.view().radius(0.1f).height(0.6f).build();
            for (int i = 0; i < 3; i++) assertTrue(
                view.findPath(v(1.125f, 0.3f, 1.125f), v(14.125f, 0.3f, 14.125f)).isComplete()
            );
            long[] queries = new long[20];
            for (int i = 0; i < queries.length; i++) {
                start = System.nanoTime();
                NavPath path = view.findPath(v(1.125f, 0.3f, 1.125f), v(14.125f, 0.3f, 14.125f));
                queries[i] = System.nanoTime() - start;
                assertTrue(path.isComplete(), path.getReason().toString());
            }
            Arrays.sort(queries);
            System.out.println(
                "NAV mesh benchmark: triangles=" +
                geometry.triangleCount +
                ", pages=" +
                world.getLoadedChunkCount() +
                ", capture_ms=" +
                capture /
                1e6 +
                ", build_ms=" +
                build /
                1e6 +
                ", query_p50_ms=" +
                queries[10] /
                1e6 +
                ", query_p95_ms=" +
                queries[19] /
                1e6
            );
        }
    }
}
