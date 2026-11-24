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

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

class NavPathFollowerTest {
    @Test
    void explicitContactSkinAdmitsRestingPhysicsSlopWithoutHidingDeepPenetration() {
        try (NavWorld world = floor()) {
            NavView strict = world.view().radius(.18f).height(1).build();
            NavView physical = strict.copy().contactSkin(.004f).build();
            assertFalse(NavCollision.clear(world.snapshot, strict, v(0, .498f, 0), Vector3f.UNIT_Y));
            assertTrue(NavCollision.clear(world.snapshot, physical, v(0, .498f, 0), Vector3f.UNIT_Y));
            assertFalse(NavCollision.clear(world.snapshot, physical, v(0, .49f, 0), Vector3f.UNIT_Y));
            assertEquals(.004f, physical.copy().build().getContactSkin());
            assertEquals(0, strict.getContactSkin());
            assertThrows(IllegalArgumentException.class, () -> strict.copy().contactSkin(-.001f));
            assertThrows(IllegalArgumentException.class, () -> strict.copy().contactSkin(.2f).build());
            NavNavigator navigator = physical.navigator(new NavPathFollower(.5f));
            navigator.target(v(2, .5f, 0));
            NavMotion motion = new NavMotion(); motion.position.set(-2, .498f, 0);
            NavStep step = new NavStep(); Vector3f delta = new Vector3f();
            for (int tick = 0; tick < 1200; tick++) {
                navigator.update(motion, 1f / 90, step); world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                if (step.getStatus() == NavStatus.MOVE) {
                    step.getDesiredVelocity(motion.velocity); motion.velocity.y = 0;
                    motion.position.addLocal(motion.velocity.mult(1f / 90, delta));
                }
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus());
            assertEquals(.498f, motion.position.y, 1e-6f, "No artificial upward shift of the measured pose");
        }
    }

    @Test
    void openFloorHasConstantCruiseSpeedAndNoGridTurnsAtDifferentFrameRates() {
        for (int rate : new int[] { 30, 60, 120 }) {
            try (NavWorld world = floor()) {
                NavView view = world.view().radius(0.18f).height(1).speed(NavMode.WALK, 2.8f).build();
                NavNavigator navigator = view.navigator(new NavPathFollower());
                NavMotion motion = new NavMotion();
                Vector3f start = v(-2.25f, 0.5f, -1.25f), goal = v(2.25f, 0.5f, 1.25f);
                Vector3f line = goal.subtract(start).normalizeLocal(), movement = new Vector3f(), previous = new Vector3f();
                motion.position.set(start);
                navigator.target(goal);
                NavStep step = new NavStep();
                int cruise = 0;
                for (int tick = 0; tick < rate * 12; tick++) {
                    navigator.update(motion, 1f / rate, step);
                    world.pump(32);
                    if (step.getStatus() == NavStatus.ARRIVED) break;
                    if (step.getStatus() != NavStatus.MOVE) continue;
                    step.getDesiredVelocity(motion.velocity);
                    float distance = motion.position.distance(start), remaining = motion.position.distance(goal);
                    if (distance > 1 && remaining > 1) {
                        cruise++;
                        assertEquals(2.8f, motion.velocity.length(), 0.001f, "Grid waypoint changed cruise speed");
                        assertEquals(
                            1,
                            motion.velocity.dot(line) / motion.velocity.length(),
                            0.0001f,
                            "Grid waypoint changed heading"
                        );
                    }
                    float acceleration = motion.velocity.distance(previous) * rate;
                    assertTrue(
                        acceleration <= 12.01f,
                        "Unbounded acceleration: " +
                        acceleration +
                        " rate=" +
                        rate +
                        " pos=" +
                        motion.position +
                        " old=" +
                        previous +
                        " new=" +
                        motion.velocity
                    );
                    previous.set(motion.velocity);
                    motion.position.addLocal(motion.velocity.mult(step.getHorizon(), movement));
                    assertTrue(NavCollision.clear(world.snapshot, view, motion.position, motion.up));
                }
                assertTrue(cruise > rate / 2);
                assertEquals(NavStatus.ARRIVED, step.getStatus());
                assertTrue(motion.position.distance(goal) <= 0.15f);
                assertTrue(motion.velocity.length() < 0.1f, "Arrived before braking");
            }
        }
    }

    @Test
    void shortcutsCannotCrossAnUnsupportedSliverBetweenSamples() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("narrow slit");
            layer.add(NavGeometry.box(v(-4, -1, -3), v(-0.013f, 0, 3)));
            layer.add(NavGeometry.box(v(0.013f, -1, -3), v(4, 0, 3)));
            finish(world);
            NavView view = world.view().radius(0.15f).height(1).build();
            NavQuery query = new NavQuery(world.getSettings(), false);
            query.bind(world.snapshot, view);
            Vector3f from = v(-2.25f, 0.5f, 0.25f), to = v(2.25f, 0.5f, 0.25f);
            long a = query.nearest(from, NavMode.WALK, 0.1f), b = query.nearest(to, NavMode.WALK, 0.1f);
            assertNotEquals(0, a);
            assertNotEquals(0, b);
            assertFalse(query.straightAllowed(a, b, from, to, Vector3f.UNIT_Y));
            assertFalse(query.surfaceAllowed(a, b, from, to, null));
        }
    }

    @Test
    void followsTheBridgeRatherThanRoundingAcrossMissingGround() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("banks");
            layer.add(NavGeometry.box(v(-6, -1, -4), v(-1, 0, 4)));
            layer.add(NavGeometry.box(v(1, -1, -4), v(6, 0, 4)));
            layer.add(NavGeometry.box(v(-1, -1, -3.5f), v(1, 0, -2)));
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).speed(NavMode.WALK, 2.8f).build();
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-4.25f, 0.5f, 0.25f);
            navigator.target(v(4.25f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            for (int tick = 0; tick < 2400; tick++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                assertTrue(
                    Math.abs(motion.position.x) >= 1 || motion.position.z <= -2,
                    "Rounded into missing support at " + motion.position
                );
                assertTrue(NavCollision.clear(world.snapshot, view, motion.position, motion.up));
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
        }
    }

    @Test
    void aCheapDetourIsPreservedEvenWhenTheDirectAreaIsAllowed() {
        try (NavWorld world = floor()) {
            world.layer("costs").add(NavGeometry.areaBox(v(-0.5f, -1, -0.5f), v(0.5f, 2, 0.5f), 7));
            finish(world);
            NavView view = world.view().radius(0.15f).height(1).areaCost(7, 20).build();
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.5f, 0.25f);
            navigator.target(v(2.25f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            for (int tick = 0; tick < 2000; tick++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                assertFalse(
                    Math.abs(motion.position.x) < 0.5f && Math.abs(motion.position.z) < 0.5f,
                    "Shortcut discarded the view's area costs"
                );
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
        }
    }

    @Test
    void replanningFromAnOffGridMovingPoseDoesNotReverseTheActor() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.5f, -0.25f);
            navigator.target(v(2.25f, 0.5f, -0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            for (int tick = 0; tick < 120; tick++) {
                if (tick == 55) navigator.replan();
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                assertTrue(motion.velocity.x >= 0, "Replan chased its captured start pose backwards");
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
            }
        }
    }

    @Test
    void explicitReplanningFollowsTheValidatedRouteWhileTheWorkerIsDelayed() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.5f, -0.25f);
            navigator.target(v(2.25f, 0.5f, -0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            navigator.update(motion, 1f / 120, step);
            world.pump(1);
            for (int tick = 0; tick < 80; tick++) {
                if (tick == 40) navigator.replan();
                navigator.update(motion, 1f / 120, step);
                assertEquals(NavStatus.MOVE, step.getStatus(), "Valid route was discarded during a background replan");
                step.getDesiredVelocity(motion.velocity);
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
            }
            world.pump(1);
            navigator.update(motion, 1f / 120, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertTrue(step.getDesiredVelocity(delta).x > 0);
        }
    }

    @Test
    void followsClimbingFlightAndTheSamePipelineInTwoDimensions() {
        for (int kind = 0; kind < 3; kind++) {
            NavSettings.Builder settings = NavSettings.builder().cellSize(0.5f).chunkSize(8);
            if (kind == 2) settings.plane(v(0, 0, 0), Vector3f.UNIT_Z);
            try (NavWorld world = NavWorld.manual(settings.build())) {
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                    if (kind != 2 || z == 0) world.load(new NavChunkKey(x, y, z));
                }
                if (kind == 0) world.layer("wall").add(NavGeometry.box(v(-3, -3, -0.5f), v(3, 3, 0)));
                finish(world);
                NavMode mode = kind == 0 ? NavMode.CLIMB : kind == 1 ? NavMode.FLY : NavMode.WALK;
                NavView view = world.view().radius(0.1f).height(0.4f).allow(mode).nearestDistance(0.4f).build();
                NavNavigator navigator = view.navigator(new NavPathFollower());
                NavMotion motion = new NavMotion();
                motion.mode = mode;
                if (kind == 0) motion.up.set(Vector3f.UNIT_Z);
                if (kind == 2) motion.up.set(Vector3f.UNIT_Z);
                motion.position.set(-2.25f, -1.25f, kind == 0 ? 0.2f : kind == 1 ? 0.75f : 0);
                Vector3f goal = v(2.25f, 1.25f, motion.position.z), delta = new Vector3f();
                navigator.target(goal);
                NavStep step = new NavStep();
                for (int tick = 0; tick < 1200; tick++) {
                    navigator.update(motion, 1f / 120, step);
                    world.pump(32);
                    if (step.getStatus() == NavStatus.ARRIVED) break;
                    assertNotEquals(NavStatus.TRANSITION, step.getStatus(), "Unexpected surface/mode switch");
                    if (step.getStatus() != NavStatus.MOVE) continue;
                    step.getDesiredVelocity(motion.velocity);
                    motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                    assertEquals(goal.z, motion.position.z, 0.0001f);
                    assertTrue(NavCollision.clear(world.snapshot, view, motion.position, motion.up));
                }
                assertEquals(NavStatus.ARRIVED, step.getStatus(), "kind=" + kind + " " + step.getReason());
            }
        }
    }

    @Test
    void stepTrajectoryKeepsForwardMotionAcrossAGentleSlopeAndChecksLiveGeometry() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            world
                .layer("ramp")
                .add(
                    NavGeometry.triangles(
                        new float[] { -3, -0.75f, -2, 3, 0.75f, 2, 3, 0.75f, -2, -3, -0.75f, -2, -3, -0.75f, 2, 3, 0.75f, 2 },
                        false,
                        0,
                        3
                    )
                );
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).build();
            float offset = 0.5f + 0.18f * ((float) Math.sqrt(1.0625) - 1);
            Vector3f from = v(-0.25f, offset - 0.0625f, 0.25f), to = v(0.25f, offset + 0.0625f, 0.25f);
            NavStep step = new NavStep();
            step.transition(NavMode.WALK, NavAction.STEP, 1, 0, to, Vector3f.UNIT_Y, 0);
            NavStepTrajectory trajectory = new NavStepTrajectory();
            assertTrue(trajectory.begin(view, from, step));
            assertEquals(from.distance(to), trajectory.getLength(), 0.001f);
            Vector3f actual = from.clone(), next = new Vector3f();
            for (int i = 1; i <= 20; i++) {
                assertTrue(trajectory.sample(trajectory.getLength() * i / 20, actual, next));
                assertTrue(next.x > actual.x, "Step stopped horizontal travel on a continuous slope");
                actual.set(next);
                assertTrue(NavCollision.clear(world.snapshot, view, actual, Vector3f.UNIT_Y));
            }
            assertEquals(0, actual.distance(to), 0.0001f);
            actual.set(from);
            world.layer("new blocker").add(NavGeometry.box(v(-0.01f, 0, 0), v(0.01f, 3, 0.5f)));
            finish(world);
            next.set(10, 20, 30);
            assertFalse(trajectory.sample(trajectory.getLength(), actual, next));
            assertEquals(v(10, 20, 30), next, "Failed sample changed the output");
        }
    }

    @Test
    void aControllerSkinAboveSupportDoesNotRestoreGridPointChasing() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.18f).height(1).build().navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.12f, 0.505f, 0.19f);
            motion.velocity.set(2, 0, 0);
            navigator.target(v(2.25f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            navigator.update(motion, 1f / 120, step);
            world.pump(1);
            navigator.update(motion, 1f / 120, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertFalse(step.isPositionCorrection());
            assertTrue(step.getDesiredVelocity(new Vector3f()).x > 0);
        }
    }

    @Test
    void aControllerSkinDoesNotHideThinForbiddenSurfaceAnnotations() {
        try (NavWorld world = floor()) {
            world.layer("thin annotation").add(NavGeometry.areaBox(v(-0.5f, -0.01f, -0.5f), v(0.5f, 0, 0.5f), 7));
            finish(world);
            NavView view = world.view().radius(0.15f).height(1).areaCost(7, 20).costCutoff(5).build();
            NavQuery query = new NavQuery(world.getSettings(), false);
            query.bind(world.snapshot, view);
            Vector3f from = v(-2.25f, 0.505f, 0.25f), to = v(2.25f, 0.505f, 0.25f);
            long a = query.nearest(from, NavMode.WALK, 0.1f), b = query.nearest(to, NavMode.WALK, 0.1f);
            assertNotEquals(0, a);
            assertNotEquals(0, b);
            assertFalse(query.straightAllowed(a, b, from, to, Vector3f.UNIT_Y));
            assertFalse(query.surfaceAllowed(a, b, from, to, null));
        }
    }

    @Test
    void obstacleCornersRespectAccelerationAndTurnLimits() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("obstacle course");
            layer.add(NavGeometry.box(v(-6, -1, -6), v(6, 0, 6)));
            layer.add(NavGeometry.box(v(-1, 0, -1), v(1, 2, 1)));
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).speed(NavMode.WALK, 2.8f).build();
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-4.25f, 0.5f, 0.25f);
            navigator.target(v(4.25f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            Vector3f previous = new Vector3f(), delta = new Vector3f();
            for (int tick = 0; tick < 1600; tick++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                assertTrue(previous.distance(motion.velocity) * 120 <= 12.01f, "Discontinuous velocity at a corner");
                if (previous.lengthSquared() > 0.01f && motion.velocity.lengthSquared() > 0.01f) {
                    float cosine = previous.dot(motion.velocity) / (previous.length() * motion.velocity.length());
                    assertTrue(cosine >= Math.cos(6f / 120) - 0.00001, "Unbounded corner steering");
                }
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                assertTrue(NavCollision.clear(world.snapshot, view, motion.position, motion.up));
                previous.set(motion.velocity);
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus());
        }
    }

    @Test
    void visibleDestinationsDoNotSkipAuthoredActionTokens() {
        try (NavWorld world = floor()) {
            Vector3f from = v(-1.25f, 0.5f, 0.25f), to = v(1.25f, 0.5f, 0.25f);
            NavLink link = new NavLink(from, to, NavMode.TELEPORT, 0.2f, 0, 0, 0, 0, true);
            world.addLink(link);
            NavNavigator navigator = world
                .view()
                .radius(0.15f)
                .height(1)
                .allow(NavMode.WALK, NavMode.TELEPORT)
                .build()
                .navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(from);
            navigator.target(to);
            NavStep step = new NavStep();
            navigator.update(motion, 1f / 120, step);
            world.pump(1);
            navigator.update(motion, 1f / 120, step);
            assertEquals(NavStatus.TRANSITION, step.getStatus());
            assertEquals(NavAction.TELEPORT, step.getAction());
            assertEquals(link.id, step.getTraversalId());
            long token = step.getActionId();
            navigator.update(motion, 1f / 120, step);
            assertEquals(token, step.getActionId());
            assertEquals(from, motion.position);
        }
    }

    @Test
    void aStepArcAdvancesAcrossTheLedgeAndRemainsClear() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("ledge");
            layer.add(NavGeometry.box(v(-3, -1, -2), v(0, 0, 2)));
            layer.add(NavGeometry.box(v(0, -1, -2), v(3, 0.25f, 2)));
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).build();
            Vector3f from = v(-0.25f, 0.5f, 0.25f), to = v(0.25f, 0.75f, 0.25f);
            NavStep step = new NavStep();
            step.transition(NavMode.WALK, NavAction.STEP, 1, 0, to, Vector3f.UNIT_Y, 0);
            NavStepTrajectory trajectory = new NavStepTrajectory();
            assertTrue(trajectory.begin(view, from, step));
            Vector3f actual = from.clone(), next = new Vector3f();
            for (int i = 1; i <= 80; i++) {
                assertTrue(trajectory.sample(trajectory.getLength() * i / 80, actual, next));
                assertTrue(next.x > actual.x, "Step fell back to vertical point chasing");
                actual.set(next);
                assertTrue(NavCollision.clear(world.snapshot, view, actual, Vector3f.UNIT_Y));
            }
            assertEquals(0, actual.distance(to), 0.0001f);
        }
    }

    @Test
    void aCloseGoalWithTransverseVelocityDoesNotBecomeAnOrbit() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world
                .view()
                .radius(0.1f)
                .height(1)
                .speed(NavMode.WALK, 1.9f)
                .build()
                .navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            motion.velocity.set(0, 0, 1.9f);
            navigator.target(v(-0.75f, 0.5f, -0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            for (int tick = 0; tick < 120 * 5; tick++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus(), "Circling the destination at " + motion.position);
        }
    }

    @Test
    void configuredHeadingAtRestAndTerminalSpeedAreRespected() {
        try (NavWorld world = floor()) {
            NavView view = world
                .view()
                .radius(0.1f)
                .height(1)
                .kinematics(NavKinematics.builder().turnRate(0.5f).terminalSpeed(2).build())
                .build();
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, 0.25f);
            motion.forward.set(0, 0, 1);
            navigator.target(v(1.25f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            navigator.update(motion, 0.016f, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertTrue(step.getDesiredVelocity(new Vector3f()).normalizeLocal().dot(motion.forward) > 0.999f);
            motion.position.set(1.24f, 0.5f, 0.25f);
            motion.velocity.set(1.5f, 0, 0);
            navigator.update(motion, 0.016f, step);
            assertEquals(NavStatus.ARRIVED, step.getStatus());
        }
    }

    @Test
    void fineCellsAndSmallActorsKeepCruiseSpeedWithinTheLocalSweepBudget() {
        NavSettings settings = NavSettings.builder().cellSize(0.1f).chunkSize(8).localBudget(64, 4096).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            world.layer("fine floor").add(NavGeometry.box(v(-3, -0.1f, -0.5f), v(3, 0, 0.5f)));
            finish(world);
            NavView view = world.view().radius(0.04f).height(0.2f).speed(NavMode.WALK, 1.3f).build();
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.85f, 0.1f, 0.05f);
            navigator.target(v(2.85f, 0.1f, 0.05f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            int cruise = 0;
            for (int tick = 0; tick < 120 * 10; tick++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                if (Math.abs(motion.position.x) < 1.8f) {
                    cruise++;
                    assertEquals(1.3f, motion.velocity.length(), 0.001f, "Budget pressure restored grid speed dips");
                }
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
            }
            assertTrue(cruise > 120);
            assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
        }
    }

    @Test
    void continuousTriangulatedSlopesKeepCruisingAcrossChunkSeamsAndDelayedReplans() {
        for (Vector3f up : new Vector3f[] { Vector3f.UNIT_Y, Vector3f.UNIT_X }) for (int rate : new int[] { 30, 60, 120 }) {
            try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
                float[] triangles = new float[12 * 18];
                int offset = 0;
                for (int i = 0; i < 12; i++) {
                    float a = -3 + i * 0.5f, b = a + 0.5f;
                    float ha = 0.03f * (a + 3) * (a + 3), hb = 0.03f * (b + 3) * (b + 3);
                    for (float coordinate : new float[] {
                        a,
                        ha,
                        -1,
                        b,
                        hb,
                        1,
                        b,
                        hb,
                        -1,
                        a,
                        ha,
                        -1,
                        a,
                        ha,
                        1,
                        b,
                        hb,
                        1,
                    }) triangles[offset++] = coordinate;
                }
                Vector3f advance = up == Vector3f.UNIT_Y ? Vector3f.UNIT_X : Vector3f.UNIT_Y;
                if (up == Vector3f.UNIT_X) for (int i = 0; i < triangles.length; i += 3) {
                    float x = triangles[i];
                    triangles[i] = triangles[i + 1];
                    triangles[i + 1] = x;
                }
                world.layer("curved terrain").add(NavGeometry.triangles(triangles, false, 0, 3));
                finish(world);
                NavView view = world
                    .view()
                    .up(up)
                    .radius(0.18f)
                    .height(1)
                    .speed(NavMode.WALK, 2.8f)
                    .slopeCost(NavMode.WALK, 0.75f)
                    .build();
                NavNavigator navigator = view.navigator(new NavPathFollower());
                NavMotion motion = new NavMotion();
                motion.up.set(up);
                assertTrue(
                    view.nearestPoint(
                        up == Vector3f.UNIT_Y ? v(-2.25f, 0.52f, 0.25f) : v(0.52f, -2.25f, 0.25f),
                        motion.position
                    )
                );
                navigator.target(up == Vector3f.UNIT_Y ? v(2.25f, 1.35f, 0.25f) : v(1.35f, 2.25f, 0.25f));
                NavStep step = new NavStep();
                Vector3f delta = new Vector3f(), previous = new Vector3f();
                int cruise = 0;
                for (int tick = 0; tick < rate * 5; tick++) {
                    if (tick == rate) navigator.replan();
                    navigator.update(motion, 1f / rate, step);
                    // Let the captured plan age while its actor keeps following the current route.
                    if (tick < rate || tick > rate + rate / 5) world.pump(32);
                    if (step.getStatus() == NavStatus.ARRIVED) break;
                    assertNotEquals(NavStatus.TRANSITION, step.getStatus(), "Continuous slope became a stair action");
                    if (step.getStatus() != NavStatus.MOVE) continue;
                    step.getDesiredVelocity(motion.velocity);
                    assertTrue(motion.velocity.dot(advance) >= 0, "Returned to a terrain sample behind the moving actor");
                    assertTrue(
                        Math.abs(motion.velocity.dot(advance) - previous.dot(advance)) * rate <= 12.05f,
                        "Terrain sample caused a speed discontinuity: rate=" +
                        rate +
                        " tick=" +
                        tick +
                        " pos=" +
                        motion.position +
                        " old=" +
                        previous +
                        " new=" +
                        motion.velocity
                    );
                    if (motion.position.dot(advance) > -1.5f && motion.position.dot(advance) < 1.5f) {
                        cruise++;
                        assertTrue(
                            motion.velocity.dot(advance) > 2.5f,
                            "Braked on a triangle or chunk seam: rate=" +
                            rate +
                            " tick=" +
                            tick +
                            " position=" +
                            motion.position +
                            " velocity=" +
                            motion.velocity +
                            " next=" +
                            step.getNextPosition(delta)
                        );
                    }
                    motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                    assertTrue(NavCollision.clear(world.snapshot, view, motion.position, motion.up));
                    previous.set(motion.velocity);
                }
                assertTrue(cruise > rate / 2);
                assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
            }
        }
    }

    @Test
    void aDistantLookAheadOnARampDoesNotBecomeAPrematureStepAction() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("ramp and landings");
            layer.add(NavGeometry.box(v(-4, -1, -2), v(0, 0, 2)));
            layer.add(
                NavGeometry.triangles(new float[] { 0, 0, -2, 2, 1, 2, 2, 1, -2, 0, 0, -2, 0, 0, 2, 2, 1, 2 }, false, 0, 3)
            );
            layer.add(NavGeometry.box(v(2, -1, -2), v(6, 1, 2)));
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).speed(NavMode.WALK, 2.8f).build();
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.5f, 0.25f);
            navigator.target(v(4.25f, 1.5f, 0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            for (int frame = 0; frame < 120 * 8; frame++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                assertNotEquals(
                    NavStatus.TRANSITION,
                    step.getStatus(),
                    "A continuous ramp emitted a step toward its look-ahead target"
                );
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                assertTrue(motion.velocity.x >= 0);
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                assertTrue(NavCollision.clear(world.snapshot, view, motion.position, motion.up));
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
        }
    }

    @Test
    void aSurfaceShortcutCannotArriveOnTheFloorBelowItsDestination() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("two storeys");
            layer.add(NavGeometry.box(v(-4, -1, -2), v(4, 0, 2)));
            layer.add(NavGeometry.box(v(0, 2.9f, -2), v(4, 3, 2)));
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).build();
            NavQuery query = new NavQuery(world.getSettings(), false);
            query.bind(world.snapshot, view);
            Vector3f from = v(-2.25f, 0.5f, 0.25f), to = v(2.25f, 3.5f, 0.25f);
            long a = query.nearest(from, NavMode.WALK, 0.1f), b = query.nearest(to, NavMode.WALK, 0.1f);
            assertNotEquals(0, a);
            assertNotEquals(0, b);
            assertFalse(query.surfaceAllowed(a, b, from, to, null));
        }
    }

    @Test
    void surfaceProjectionAppliesDirectionalCutoffsToTheActualSlope() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            world
                .layer("ramp")
                .add(
                    NavGeometry.triangles(
                        new float[] { -3, -0.75f, -2, 3, 0.75f, 2, 3, 0.75f, -2, -3, -0.75f, -2, -3, -0.75f, 2, 3, 0.75f, 2 },
                        false,
                        0,
                        3
                    )
                );
            finish(world);
            NavView view = world
                .view()
                .radius(0.18f)
                .height(1)
                .policy((mode, area, inclination, ascent, base) -> ascent > 0.2f ? Float.POSITIVE_INFINITY : base)
                .build();
            NavQuery query = new NavQuery(world.getSettings(), false);
            query.bind(world.snapshot, view);
            float offset = 0.5f + 0.18f * ((float) Math.sqrt(1.0625) - 1) + 0.001f;
            Vector3f from = v(-0.25f, offset - 0.0625f, 0.25f);
            long ref = query.nearest(from, NavMode.WALK, 0.5f);
            assertNotEquals(0, ref);
            assertFalse(
                query.surfaceAllowed(ref, ref, from, from.add(0.05f, 0, 0), null),
                "A horizontal intent hid its actual uphill cost"
            );
            assertTrue(query.surfaceAllowed(ref, ref, from, from.add(-0.05f, 0, 0), null));
        }
    }

    @Test
    void surfaceFollowingDoesNotTurnARealLedgeIntoAContinuousSlope() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("ledge");
            layer.add(NavGeometry.box(v(-3, -1, -2), v(0, 0, 2)));
            layer.add(NavGeometry.box(v(0, -1, -2), v(3, 0.25f, 2)));
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).build();
            NavQuery query = new NavQuery(world.getSettings(), false);
            query.bind(world.snapshot, view);
            Vector3f from = v(-0.25f, 0.5f, 0.25f), to = v(0.25f, 0.75f, 0.25f);
            long a = query.nearest(from, NavMode.WALK, 0.1f), b = query.nearest(to, NavMode.WALK, 0.1f);
            assertNotEquals(0, a);
            assertNotEquals(0, b);
            assertFalse(query.surfaceAllowed(a, b, from, to, null));
            assertTrue(query.needsStep(a, b));
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.5f, 0.25f);
            navigator.target(v(2.25f, 0.75f, 0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            for (int frame = 0; frame < 120 * 6; frame++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.TRANSITION) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                assertTrue(motion.velocity.x >= 0, "Overshot the takeoff and turned backwards");
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                assertTrue(motion.position.x < 0, "Crossed a real ledge without its action");
            }
            assertEquals(NavStatus.TRANSITION, step.getStatus());
            assertEquals(NavAction.STEP, step.getAction());
        }
    }

    @Test
    void lateGuidanceAtAChunkBorderCannotOvershootAndReverseBeforeAJump() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("chunk boundary");
            layer.add(NavGeometry.box(v(-4, -1, -1), v(0, 0, 1)));
            layer.add(NavGeometry.box(v(1, -1, -1), v(4, 0, 1)));
            finish(world);
            Vector3f takeoff = v(-0.25f, 0.5f, 0.25f);
            world.addLink(NavLink.jump(takeoff, v(1.25f, 0.5f, 0.25f), 0.2f, 0.8f, 0.6f));
            NavNavigator navigator = world
                .view()
                .radius(0.18f)
                .height(1)
                .allow(NavMode.WALK, NavMode.JUMP)
                .speed(NavMode.WALK, 2.8f)
                .build()
                .navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-0.3f, 0.5f, 0.25f);
            motion.velocity.set(2.8f, 0, 0);
            navigator.target(v(2.25f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            Vector3f delta = new Vector3f();
            for (int tick = 0; tick < 120; tick++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.TRANSITION) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                assertTrue(motion.velocity.x >= 0, "Reversed while chasing the takeoff point");
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                assertTrue(motion.position.x <= takeoff.x + 0.00005f, "Overshot the validated action boundary");
            }
            assertEquals(NavStatus.TRANSITION, step.getStatus(), "Never reached the jump at the chunk border");
            assertEquals(NavAction.JUMP, step.getAction());
            assertEquals(0, motion.position.distance(takeoff), 0.0001f);
        }
    }

    @Test
    void preciseReconnectionUsesTheSameAccelerationLimitsAsShortcuts() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world
                .view()
                .radius(0.18f)
                .height(1)
                .speed(NavMode.WALK, 2.8f)
                .stepHeight(0.01f)
                .build()
                .navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.25f, 0.58f, -0.25f);
            navigator.target(v(2.25f, 0.5f, -0.25f));
            NavStep step = new NavStep();
            navigator.update(motion, 1f / 120, step);
            world.pump(1);
            navigator.update(motion, 1f / 120, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertTrue(step.isPositionCorrection());
            assertTrue(
                step.getDesiredVelocity(new Vector3f()).length() <= 8f / 120 + 0.0001f,
                "Precise fallback bypassed acceleration limits"
            );
        }
    }

    @Test
    void lateGuidanceUsesTheArrivalRegionToBrakeWithoutSnappingAtTheGoalCenter() {
        try (NavWorld world = floor()) {
            NavView view = world.view().radius(0.18f).height(1).speed(NavMode.WALK, 2.8f).arrivalDistance(0.15f).build();
            NavNavigator navigator = view.navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-0.29f, 0.5f, 0.25f);
            motion.velocity.set(0.9f, 0, 0);
            Vector3f goal = v(-0.25f, 0.5f, 0.25f), previous = motion.velocity.clone(), delta = new Vector3f();
            navigator.target(goal);
            NavStep step = new NavStep();
            for (int frame = 0; frame < 120; frame++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(32);
                if (step.getStatus() == NavStatus.ARRIVED) break;
                if (step.getStatus() != NavStatus.MOVE) continue;
                step.getDesiredVelocity(motion.velocity);
                assertTrue(motion.velocity.x >= 0, "Reversed inside the destination's arrival region");
                assertTrue(motion.velocity.distance(previous) * 120 <= 12.01f, "Snapped to the goal instead of braking");
                motion.position.addLocal(motion.velocity.mult(step.getHorizon(), delta));
                previous.set(motion.velocity);
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus());
            assertTrue(motion.position.distance(goal) <= 0.15f);
        }
    }

    @Test
    void aBlockedFollowerWaitsForChangedMotionWithoutBusyReplanning() {
        try (NavWorld world = floor()) {
            world.layer("wall").add(NavGeometry.box(v(-1.9f, 0, -0.5f), v(-1.8f, 2, 0.5f)));
            finish(world);
            NavNavigator navigator = world
                .view()
                .radius(0.18f)
                .height(1)
                .speed(NavMode.WALK, 2.8f)
                .build()
                .navigator(new NavPathFollower());
            NavMotion motion = new NavMotion();
            motion.position.set(-2.08f, 0.5f, 0.25f);
            motion.velocity.set(2.8f, 0, 0);
            navigator.target(v(-2.75f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            navigator.update(motion, 1f / 120, step);
            world.pump(1);
            navigator.update(motion, 1f / 120, step);
            assertEquals(NavStatus.YIELD, step.getStatus());
            for (int tick = 0; tick < 20; tick++) {
                navigator.update(motion, 1f / 120, step);
                assertEquals(NavStatus.YIELD, step.getStatus());
                assertEquals(0, world.pump(1), "Replanned unchanged blocked motion every frame");
            }
            // The controller reports a collision stop at the same position.
            motion.velocity.set(0, 0, 0);
            navigator.update(motion, 1f / 120, step);
            assertEquals(NavReason.PATH_PENDING, step.getReason());
            world.pump(1);
            navigator.update(motion, 1f / 120, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertTrue(step.getDesiredVelocity(new Vector3f()).x < 0);
        }
    }
}
