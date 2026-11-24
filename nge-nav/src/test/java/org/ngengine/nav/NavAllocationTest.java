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
import static org.junit.jupiter.api.Assumptions.*;
import static org.ngengine.nav.NavWorldTest.*;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("allocation")
class NavAllocationTest {

    @Test
    void steadyGuidanceHasNoPerFrameHeapAllocation() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator();
            navigator.target(v(2.25f, 0.5f, -0.25f));
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.5f, -0.25f);
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            for (int i = 0; i < 30000; i++) navigator.update(motion, 0.016f, step);
            long thread = Thread.currentThread().getId();
            int calls = 100000;
            long before = bean.getThreadAllocatedBytes(thread), started = System.nanoTime();
            for (int i = 0; i < calls; i++) navigator.update(motion, 0.016f, step);
            long nanos = System.nanoTime() - started, bytes = bean.getThreadAllocatedBytes(thread) - before;
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertTrue(bytes <= 1024, "Allocated " + bytes + " bytes for " + calls + " stable updates");
            System.out.println(
                "NAV steady guidance: " + (double) bytes / calls + " bytes/update; " + nanos / calls + " ns/update"
            );
            long[] samples = new long[2000];
            for (int i = 0; i < samples.length; i++) {
                long start = System.nanoTime();
                navigator.update(motion, 0.016f, step);
                samples[i] = System.nanoTime() - start;
            }
            Arrays.sort(samples);
            System.out.println(
                "NAV guidance latency ns: p50=" + samples[1000] + ", p95=" + samples[1900] + ", p99=" + samples[1980]
            );
        }
    }

    @Test
    void continuousFollowerHasNoPerFrameHeapAllocation() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.5f, -0.25f);
            motion.velocity.set(2, 0, 0);
            navigator.target(v(2.25f, 0.5f, 0.75f));
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            for (int i = 0; i < 30000; i++) navigator.update(motion, 0.016f, step);
            long thread = Thread.currentThread().getId();
            long before = bean.getThreadAllocatedBytes(thread), started = System.nanoTime();
            for (int i = 0; i < 100000; i++) navigator.update(motion, 0.016f, step);
            long nanos = System.nanoTime() - started, bytes = bean.getThreadAllocatedBytes(thread) - before;
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertTrue(bytes <= 1024, "Follower allocated " + bytes + " bytes");
            System.out.println(
                "NAV continuous follower: " + bytes + " total bytes / 100000 updates; " + nanos / 100000 + " ns/update"
            );
        }
    }

    @Test
    void movingFrameConversionsReuseTheirStorage() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        try (NavWorld world = floor()) {
            NavFrame frame = new NavFrame(world);
            NavMotion local = new NavMotion(), global = new NavMotion(), recovered = new NavMotion();
            local.position.set(1, 0.5f, 0);
            local.velocity.set(0, 0, 1);
            Vector3f position = new Vector3f();
            Quaternion rotation = new Quaternion();
            for (int i = 0; i < 30000; i++) {
                position.x += 0.001f;
                rotation.fromAngleNormalAxis((i & 1023) * 0.0001f, Vector3f.UNIT_Y);
                frame.updatePose(position, rotation, 0.01f);
                frame.toWorld(local, global);
                frame.toLocal(global, recovered);
            }
            long before = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
            for (int i = 0; i < 100000; i++) {
                position.x += 0.001f;
                rotation.fromAngleNormalAxis((i & 1023) * 0.0001f, Vector3f.UNIT_Y);
                frame.updatePose(position, rotation, 0.01f);
                frame.toWorld(local, global);
                frame.toLocal(global, recovered);
            }
            long bytes = bean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
            assertTrue(bytes <= 1024, "Frame conversions allocated " + bytes + " bytes");
            assertEquals(0, recovered.position.distance(local.position), 0.001f);
            assertEquals(0, recovered.velocity.distance(local.velocity), 0.001f);
            System.out.println("NAV moving frame: " + bytes + " total bytes / 100000 updates and round trips");
        }
    }

    @Test
    void waitingForAFullQueueDoesNotAllocateEveryFrame() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        NavSettings settings = NavSettings.builder().jobCapacity(1).chunkSize(4).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            world.layer("floor").add(NavGeometry.box(v(-2, -1, -2), v(2, 0, 2)));
            finish(world);
            NavNavigator first = world.view().build().navigator(), second = world.view().build().navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.9f, -0.25f);
            NavStep step = new NavStep();
            first.target(v(1.25f, 0.9f, -0.25f));
            second.target(v(1.25f, 0.9f, -0.25f));
            first.update(motion, 0.016f, step);
            for (int i = 0; i < 20000; i++) second.update(motion, 0.016f, step);
            long thread = Thread.currentThread().getId(), before = bean.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 100000; i++) second.update(motion, 0.016f, step);
            long bytes = bean.getThreadAllocatedBytes(thread) - before;
            assertEquals(NavReason.QUEUE_FULL, step.getReason());
            assertTrue(bytes <= 1024, "Queue-full allocation: " + bytes);
        }
    }
}
