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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class NavConcurrencyTest {

    @Test
    void realWorkersPublishWithoutBlockingGuidance() throws Exception {
        try (NavWorld world = new NavWorld(NavSettings.builder().chunkSize(4).build())) {
            world.layer("floor").add(NavGeometry.box(v(-2, -1, -2), v(2, 0, 2)));
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            NavStep step = new NavStep();
            navigator.target(v(1.25f, 0.5f, -0.25f));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (
                System.nanoTime() < deadline && navigator.update(motion, 0.016f, step).getStatus() != NavStatus.MOVE
            ) Thread.sleep(1);
            assertEquals(NavStatus.MOVE, step.getStatus(), step.getReason().toString());
        }
    }

    @Test
    void inlinePlatformExecutorDoesNotRunPlanningInsideUpdate() {
        Executor inline = Runnable::run;
        try (NavWorld world = new NavWorld(NavSettings.builder().chunkSize(4).build(), inline, inline)) {
            world.layer("floor").add(NavGeometry.box(v(-2, -1, -2), v(2, 0, 2)));
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            NavStep step = new NavStep();
            navigator.target(v(1.25f, 0.5f, -0.25f));
            assertEquals(NavReason.PATH_PENDING, navigator.update(motion, 0.016f, step).getReason());
            assertEquals(NavReason.PATH_PENDING, navigator.update(motion, 0.016f, step).getReason());
            assertEquals(1, world.dispatch(1));
            assertEquals(NavStatus.MOVE, navigator.update(motion, 0.016f, step).getStatus());
        }
    }

    @Test
    void anOldGoalCannotReplaceANewerRequest() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            NavStep step = new NavStep();
            navigator.target(v(1.25f, 0.5f, -0.25f));
            navigator.update(motion, 0.016f, step);
            navigator.target(v(-2.25f, 0.5f, -0.25f));
            navigator.update(motion, 0.016f, step);
            world.pump(2);
            navigator.update(motion, 0.016f, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertTrue(step.getDesiredVelocity(v(0, 0, 0)).x < 0);
        }
    }

    @Test
    void changingGeometryDuringABlockedBuildRejectsItsOldResult() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        Executor blocked = task -> {
            Thread worker = new Thread(() -> {
                entered.countDown();
                try {
                    release.await();
                    task.run();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            worker.setDaemon(true);
            worker.start();
        };
        try (NavWorld world = new NavWorld(NavSettings.builder().chunkSize(4).jobCapacity(1).build(), blocked, Runnable::run)) {
            NavSource source = world.layer("floor").add(NavGeometry.box(v(-1, -1, -1), v(1, 0, 1)));
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            source.replace(NavGeometry.box(v(-1, 3, -1), v(1, 4, 1)));
            assertEquals(0, world.getLoadedChunkCount());
            release.countDown();
            assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> {
                    while (!world.isReady()) Thread.sleep(1);
                }
            );
            assertFalse(
                world
                    .view()
                    .radius(0.1f)
                    .height(0.5f)
                    .nearestDistance(0.2f)
                    .build()
                    .nearestPoint(v(0.75f, 0.25f, 0.75f), v(0, 0, 0))
            );
        } finally {
            release.countDown();
        }
    }
}
