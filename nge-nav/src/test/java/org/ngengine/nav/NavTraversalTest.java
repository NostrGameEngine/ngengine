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

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

class NavTraversalTest {

    @Test void paidTraversalLinksDoNotTurnAnOrdinaryRouteIntoAnExhaustiveSearch() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(.1f).chunkSize(8).searchBudget(96, 128).build())) {
            world.layer("floor").add(NavGeometry.box(v(-2, -.1f, -2), v(2, 0, 2)));
            world.addLink(new NavLink(v(-1.5f, .1f, 1), v(-.5f, .1f, 1), NavMode.JUMP,
                    .1f, 0, 2, .5f, .2f, true));
            finish(world);
            NavView view = world.view().radius(.04f).height(.2f).allow(NavMode.WALK, NavMode.JUMP).build();
            NavPath path = view.findPath(v(-1.5f, .1f, 0), v(1.5f, .1f, 0));
            assertTrue(path.isComplete(), "A distant paid link must preserve directed search: " + path.getReason());
        }
    }

    @Test
    void externalMovementSweepRejectsThinObstaclesBodyPenetrationAndMissingPages() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("jump clearance");
            layer.add(NavGeometry.box(v(-4, -1, -4), v(4, 0, 4)));
            layer.add(NavGeometry.box(v(-0.01f, 0, -1), v(0.01f, 2, 1)));
            finish(world);
            NavView view = world.view().radius(0.16f).height(1).build();
            assertTrue(view.isSegmentClear(v(-2, 0.5f, 2), v(2, 0.5f, 2)));
            assertFalse(view.isSegmentClear(v(-2, 0.5f, 0), v(2, 0.5f, 0)));
            assertFalse(view.isSegmentClear(v(-2, 0.4f, 2), v(-2, 0.5f, 2)));
            assertFalse(view.isSegmentClear(v(100, 3, 0), v(101, 3, 0)));
            assertThrows(IllegalArgumentException.class, () -> view.isSegmentClear(v(Float.NaN, 0, 0), v(0, 0, 0)));
        }
    }

    private static NavWorld ridgeTerrain() {
        NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build());
        NavLayer layer = world.layer("ridge and level detour");
        layer.add(NavGeometry.box(v(-4, -1, -4), v(-2, 0, 2)));
        layer.add(NavGeometry.box(v(2, -1, -4), v(4, 0, 2)));
        layer.add(NavGeometry.box(v(-2, -1, -4), v(2, 0, -2)));
        for (int side = -1; side <= 1; side += 2) {
            Box wedge = new Box(1, 0.5f, 1.25f);
            FloatBuffer points = wedge.getFloatBuffer(VertexBuffer.Type.Position);
            for (int i = 0; i < points.capacity(); i += 3) {
                float x = points.get(i) + side;
                points.put(i + 1, points.get(i + 1) > 0 ? 0.75f * (1 - Math.abs(x) * 0.5f) : -1);
            }
            wedge.updateBound();
            layer.add(NavGeometry.capture(wedge, new Transform(v(side, 0, 0)), true, 0, 3));
        }
        finish(world);
        return world;
    }

    @Test
    void shortModerateRidgeBeatsDetourUntilDifficultyMakesItMoreExpensive() {
        try (NavWorld world = ridgeTerrain()) {
            NavView balanced = world.view().radius(0.18f).height(1).slopeCost(NavMode.WALK, 0.75f).build();
            Vector3f from = v(-3.25f, 0.5f, 0.25f), to = v(3.25f, 0.5f, 0.25f), point = new Vector3f();
            NavPath direct = balanced.findPath(from, to);
            assertTrue(direct.isComplete(), direct.getReason().toString());
            assertTrue(direct.getCost() < 9, "Ridge should cost less than the long level detour");
            for (int i = 0; i < direct.size(); i++) assertTrue(Math.abs(direct.getPoint(i, point).z) < 1);
            for (NavView view : new NavView[] {
                balanced.copy().slopeCost(NavMode.WALK, 20).build(),
                balanced.copy().slopeCost(NavMode.WALK, 20).costCutoff(4).build(),
            }) {
                NavPath detour = view.findPath(from, to);
                assertTrue(detour.isComplete());
                boolean levelRoute = false;
                for (int i = 0; i < detour.size(); i++) levelRoute |= Math.abs(detour.getPoint(i, point).z) > 2;
                assertTrue(levelRoute, "High difficulty should favor the level detour");
            }
        }
    }

    @Test
    void ridgeControllerFollowsTheShortRouteWithoutBodyPenetration() {
        try (NavWorld world = ridgeTerrain()) {
            NavView view = world.view().radius(0.18f).height(1).speed(NavMode.WALK, 2.8f).build();
            NavNavigator navigator = view.navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-3.25f, 0.5f, 0.25f);
            navigator.target(v(3.25f, 0.5f, 0.25f));
            NavStep step = new NavStep();
            Vector3f from = new Vector3f(), to = new Vector3f(), movement = new Vector3f();
            long token = 0;
            float time = 0;
            for (int tick = 0; tick < 1600; tick++) {
                navigator.update(motion, 1f / 120, step);
                world.pump(1);
                if (step.getStatus() == NavStatus.MOVE) {
                    step.getDesiredVelocity(movement).multLocal(step.getHorizon());
                    motion.position.addLocal(movement);
                } else if (step.getStatus() == NavStatus.TRANSITION) {
                    if (token != step.getActionId()) {
                        token = step.getActionId();
                        from.set(motion.position);
                        step.getNextPosition(to);
                        time = 0;
                    }
                    time = Math.min(1, time + 1f / 36);
                    float apex = Math.max(from.y, to.y) + step.getStepLift();

                    if (time < 0.2f) motion.position.set(from.x, from.y + (apex - from.y) * time * 5, from.z); else if (
                        time < 0.8f
                    ) {
                        motion.position.interpolateLocal(from, to, (time - 0.2f) / 0.6f);
                        motion.position.y = apex;
                    } else motion.position.set(to.x, apex + (to.y - apex) * (time - 0.8f) * 5, to.z);
                    if (time >= 1) {
                        motion.mode = step.getMode();
                        step.getSupportNormal(motion.up);
                        navigator.acknowledge(token, true);
                    }
                } else if (step.getStatus() == NavStatus.ARRIVED) break;
                assertTrue(
                    NavCollision.clear(world.snapshot, view, motion.position, motion.up),
                    "Body penetrated crest at " + motion.position
                );
            }
            assertTrue(navigator.getPath().getCost() < 9);
            assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
        }
    }

    @Test
    void waypointArrivalToleranceDoesNotCutAnUnsupportedCorner() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(8).build())) {
            NavLayer layer = world.layer("banks and narrow crossing");
            layer.add(NavGeometry.capture(new Box(4, 0.5f, 6), new Transform(v(-6, -0.5f, 0)), true, 0, 3));
            layer.add(NavGeometry.capture(new Box(4, 0.5f, 6), new Transform(v(6, -0.5f, 0)), true, 0, 3));
            layer.add(NavGeometry.capture(new Box(2, 0.5f, 1), new Transform(v(0, -0.5f, -4.5f)), true, 0, 3));
            finish(world);
            for (int direction = -1; direction <= 1; direction += 2) {
                NavNavigator navigator = world
                    .view()
                    .radius(0.18f)
                    .height(1)
                    .speed(NavMode.WALK, 2.8f)
                    .arrivalDistance(0.12f)
                    .build()
                    .navigator();
                NavMotion motion = new NavMotion();
                motion.position.set(direction * 8.25f, 0.5f, 0.25f);
                navigator.target(v(-motion.position.x, 0.5f, 0.25f));
                NavStep step = new NavStep();
                Vector3f velocity = new Vector3f();
                for (int tick = 0; tick < 1600; tick++) {
                    navigator.update(motion, 1f / 120, step);
                    world.pump(1);
                    if (step.getStatus() == NavStatus.MOVE) {
                        step.getDesiredVelocity(velocity).multLocal(step.getHorizon());
                        motion.position.addLocal(velocity);
                        assertTrue(
                            Math.abs(motion.position.x) >= 2 || motion.position.z <= -3.5f,
                            "Cut across missing support at " + motion.position
                        );
                    } else if (step.getStatus() == NavStatus.ARRIVED) break;
                }
                assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
            }
        }
    }

    @Test
    void walkingCrossesAConvexRampCrestInBothDirections() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(4).build())) {
            NavLayer layer = world.layer("slope and landings");
            layer.add(NavGeometry.box(v(-4, -1, -1), v(-2, 0, 1)));
            layer.add(
                NavGeometry.triangles(new float[] { -2, 0, -1, 2, 1, 1, 2, 1, -1, -2, 0, -1, -2, 0, 1, 2, 1, 1 }, false, 0, 3)
            );
            layer.add(NavGeometry.box(v(2, -1, -1), v(4, 1, 1)));
            finish(world);
            NavView view = world.view().radius(0.18f).height(1).stepHeight(0.5f).nearestDistance(0.4f).build();
            Vector3f from = v(-3.25f, 0.5f, 0.25f), to = v(3.25f, 1.5f, 0.25f);
            assertTrue(view.findPath(from, to).isComplete());
            assertTrue(view.findPath(to, from).isComplete());
            NavNavigator navigator = view.navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(from);
            NavStep step = new NavStep();
            Vector3f movement = new Vector3f();
            navigator.target(to);
            for (int tick = 0; tick < 1200; tick++) {
                navigator.update(motion, 1f / 60, step);
                world.pump(1);
                if (step.getStatus() == NavStatus.MOVE) {
                    step.getDesiredVelocity(movement).multLocal(step.getHorizon());
                    motion.position.addLocal(movement);
                } else if (step.getStatus() == NavStatus.TRANSITION) {
                    step.getNextPosition(motion.position);
                    step.getSupportNormal(motion.up);
                    motion.mode = step.getMode();
                    assertTrue(navigator.acknowledge(step.getActionId(), true));
                } else if (step.getStatus() == NavStatus.ARRIVED) break;
            }
            assertEquals(NavStatus.ARRIVED, step.getStatus(), step.getReason() + " at " + motion.position);
        }
    }

    @Test
    void captureCopiesMeshAndClassifiesClosedAndOverlappingSolids() {
        Box box = new Box(1, 1, 1);
        NavGeometry captured = NavGeometry.capture(box, Transform.IDENTITY, true, 0, NavGeometry.DEFAULT_FLAGS);
        assertTrue(captured.contains(0, 0, 0));
        assertTrue(captured.contains(0.999f, 0, 0));
        assertFalse(captured.contains(1.001f, 0, 0));
        NavGeometry shell = NavGeometry.capture(box, Transform.IDENTITY, false, 0, NavGeometry.DEFAULT_FLAGS);
        assertFalse(shell.contains(0, 0, 0));
        assertThrows(
            IllegalArgumentException.class,
            () -> NavGeometry.triangles(new float[] { 0, 0, 0, 1, 0, 0, 0, 1, 0 }, true, 0, 3)
        );
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.25f).chunkSize(4).build())) {
            world.layer("solids").add(captured);
            world.layer("solids").add(NavGeometry.box(v(0, -1, -1), v(2, 1, 1)));
            finish(world);
            NavView flyer = world.view().radius(0.05f).height(0.1f).allow(NavMode.FLY).nearestDistance(0.15f).build();
            assertFalse(flyer.nearestPoint(v(0.5f, 0.125f, 0.125f), new Vector3f()));
            assertFalse(flyer.nearestPoint(v(-0.5f, 0.125f, 0.125f), new Vector3f()));
        }
    }

    @Test
    void zeroCostTeleportChoosesShorterCostAndUsesSingleAcknowledgement() {
        try (NavWorld world = floor()) {
            NavView view = world.view().radius(0.15f).height(1).allow(NavMode.WALK, NavMode.TELEPORT).build();
            Vector3f from = v(-2.25f, 0.5f, -0.25f), to = v(2.25f, 0.5f, -0.25f);
            NavLink link = NavLink.teleport(from, to, 0.2f, 0, true);
            world.addLink(link);
            NavPath route = view.findPath(from, to);
            assertTrue(route.isComplete(), route.getReason().toString());
            assertEquals(0, route.getCost());
            assertEquals(2, route.size());
            NavNavigator navigator = view.navigator();
            navigator.target(to);
            NavMotion motion = new NavMotion();
            motion.position.set(from);
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            assertEquals(NavStatus.TRANSITION, navigator.update(motion, 0.016f, step).getStatus());
            assertEquals(NavAction.TELEPORT, step.getAction());
            long token = step.getActionId();
            assertEquals(token, navigator.update(motion, 0.016f, step).getActionId());
            assertTrue(navigator.acknowledge(token, true));
            assertFalse(navigator.acknowledge(token, true));
            assertEquals(NavReason.INVALID_MOTION, navigator.update(motion, 0.016f, step).getReason());
            motion.position.set(to);
            assertEquals(NavStatus.ARRIVED, navigator.update(motion, 0.016f, step).getStatus());
            assertFalse(navigator.acknowledge(token, true));
        }
    }

    @Test
    void serializedPageStitchesOnAttachAndRejectsCorruption() throws IOException {
        try (NavWorld world = floor(); NavWorld loaded = NavWorld.manual(world.settings)) {
            for (NavChunk page : world.snapshot.chunks) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                NavChunkIO.write(page, new DataOutputStream(bytes));
                byte[] data = bytes.toByteArray();
                NavChunk copy = NavChunkIO.read(new DataInputStream(new ByteArrayInputStream(data)), world.settings);
                assertEquals(page.nodeCount, copy.nodeCount);
                loaded.attach(copy);
                data[0] ^= 1;
                assertThrows(
                    IOException.class,
                    () -> NavChunkIO.read(new DataInputStream(new ByteArrayInputStream(data)), world.settings)
                );
            }
            NavView view = loaded.view().radius(0.15f).height(1).build();
            assertTrue(view.findPath(v(-2.25f, 0.5f, -0.25f), v(2.25f, 0.5f, -0.25f)).isComplete());
            NavChunk page = loaded.getChunk(new NavChunkKey(-1, 0, -1));
            loaded.unload(page.key);
            loaded.attach(page);
            assertNotEquals(page.serial, loaded.getChunk(page.key).serial);
        }
    }

    @Test
    void ceilingsAndWallBendsUseSharedSurfaces() {
        try (NavWorld world = floor()) {
            world.layer("ceiling").add(NavGeometry.box(v(-3, 2, -3), v(3, 2.5f, 3)));
            finish(world);
            NavView climber = world.view().radius(0.1f).height(0.4f).nearestDistance(0.3f).allow(NavMode.CLIMB).build();
            NavPath path = climber.findPath(v(-1.25f, 1.8f, -0.25f), v(1.25f, 1.8f, -0.25f));
            assertTrue(path.isComplete(), path.getReason().toString());
            assertFalse(
                climber.copy().costCutoff(1).slopeCost(NavMode.CLIMB, 1).build().nearestPoint(v(0, 1.8f, 0), new Vector3f())
            );
        }
    }

    @Test
    void arcadeGuidanceIgnoresCurrentSpeedUntilDynamicsAreEnabled() {
        try (NavWorld world = floor()) {
            NavView view = world.view().radius(0.15f).height(1).speed(NavMode.WALK, 2).build();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            motion.velocity.set(-10, 0, 0);
            NavNavigator arcade = view.navigator(), physical = view
                .copy()
                .kinematics(NavKinematics.builder().acceleration(1).braking(1).build())
                .build()
                .navigator();
            arcade.target(v(1.25f, 0.5f, -0.25f));
            physical.target(v(1.25f, 0.5f, -0.25f));
            NavStep a = new NavStep(), b = new NavStep();
            arcade.update(motion, 0.016f, a);
            physical.update(motion, 0.016f, b);
            world.pump(2);
            assertEquals(NavStatus.MOVE, arcade.update(motion, 0.016f, a).getStatus());
            assertEquals(NavStatus.MOVE, physical.update(motion, 0.016f, b).getStatus());
            assertTrue(a.getDesiredVelocity(new Vector3f()).x > 0);
            assertTrue(b.getDesiredVelocity(new Vector3f()).x < 0);
        }
    }

    @Test
    void automaticFlightTransitionsAreReturnedToTheController() {
        try (NavWorld world = floor()) {
            NavView view = world
                .view()
                .radius(0.1f)
                .height(0.5f)
                .allow(NavMode.WALK, NavMode.FLY)
                .cost(NavMode.WALK, 10)
                .cost(NavMode.FLY, 0)
                .build();
            NavPath path = view.findPath(v(-1.25f, 0.25f, -0.25f), v(1.25f, 0.25f, -0.25f));
            assertTrue(path.isComplete());
            boolean flight = false;
            for (int i = 0; i < path.size(); i++) flight |= path.getMode(i) == NavMode.FLY;
            assertTrue(flight);
            NavNavigator navigator = view.navigator();
            navigator.target(v(1.25f, 0.25f, -0.25f));
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.25f, -0.25f);
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            assertEquals(NavStatus.TRANSITION, navigator.update(motion, 0.016f, step).getStatus());
            assertEquals(NavAction.TAKEOFF, step.getAction());
        }
    }

    @Test
    void jumpingCrossesAGapButWalkingCannotBridgeMissingSupport() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(4).build())) {
            NavLayer layer = world.layer("islands");
            layer.add(NavGeometry.box(v(-3, -1, -1), v(-0.5f, 0, 1)));
            layer.add(NavGeometry.box(v(0.5f, -1, -1), v(3, 0, 1)));
            finish(world);
            Vector3f from = v(-0.75f, 0.5f, 0.25f), to = v(0.75f, 0.5f, 0.25f);
            NavView walker = world.view().radius(0.15f).height(1).nearestDistance(0.3f).build();
            assertFalse(walker.findPath(from, to).isComplete());
            world.addLink(NavLink.jump(from, to, 0.2f, 0.8f, 0.6f));
            assertTrue(walker.copy().allow(NavMode.WALK, NavMode.JUMP).build().findPath(from, to).isComplete());
            layer.add(NavGeometry.box(v(-0.25f, 1, -1), v(0.25f, 2, 1)));
            finish(world);
            assertFalse(walker.copy().allow(NavMode.WALK, NavMode.JUMP).build().findPath(from, to).isComplete());
        }
    }

    @Test
    void climbingAroundAConvexEdgeRequiresReorientation() {
        try (NavWorld world = floor()) {
            world.layer("wall").add(NavGeometry.box(v(-1, 0, -1), v(0, 2, 1)));
            finish(world);
            NavView view = world.view().radius(0.1f).height(0.4f).nearestDistance(0.3f).allow(NavMode.CLIMB).build();
            NavPath path = view.findPath(v(0.2f, 1.75f, 0.25f), v(-0.25f, 2.2f, 0.25f));
            assertTrue(path.isComplete(), path.getReason().toString());
            NavNavigator navigator = view.navigator();
            navigator.target(v(-0.25f, 2.2f, 0.25f));
            NavMotion motion = new NavMotion();
            motion.position.set(0.2f, 1.75f, 0.25f);
            motion.mode = NavMode.CLIMB;
            motion.up.set(1, 0, 0);
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            assertEquals(NavStatus.TRANSITION, navigator.update(motion, 0.016f, step).getStatus());
            assertEquals(NavAction.REORIENT, step.getAction());
        }
    }

    @Test
    void actorStepHeightEnablesStairsAndEmitsAStepAction() {
        try (NavWorld world = floor()) {
            world.layer("stairs").add(NavGeometry.box(v(0, 0, -1), v(2, 0.25f, 1)));
            finish(world);
            Vector3f from = v(-0.25f, 0.5f, 0.25f), to = v(0.25f, 0.75f, 0.25f);
            NavView view = world.view().radius(0.1f).height(1).nearestDistance(0.3f).stepHeight(0.3f).build();
            assertTrue(view.findPath(from, to).isComplete());
            assertFalse(view.copy().stepHeight(0.1f).build().findPath(from, to).isComplete());
            NavNavigator navigator = view.navigator();
            navigator.target(to);
            NavMotion motion = new NavMotion();
            motion.position.set(from);
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            assertEquals(NavStatus.TRANSITION, navigator.update(motion, 0.016f, step).getStatus());
            assertEquals(NavAction.STEP, step.getAction());
        }
    }

    @Test
    void configuredTurnRateUsesHeadingAtRestAndKeepsReversalInTheWalkPlane() {
        try (NavWorld world = floor()) {
            NavView view = world
                .view()
                .radius(0.1f)
                .height(1)
                .kinematics(NavKinematics.builder().turnRate(0.5f).build())
                .build();
            NavNavigator navigator = view.navigator();
            navigator.target(v(1.25f, 0.5f, 0.25f));
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, 0.25f);
            motion.forward.set(0, 0, 1);
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            navigator.update(motion, 0.016f, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            Vector3f velocity = step.getDesiredVelocity(v(0, 0, 0)).normalizeLocal();
            assertTrue(velocity.dot(motion.forward) > 0.999f);
            motion.velocity.set(-0.1f, 0, 0);
            navigator.replan();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            navigator.update(motion, 0.016f, step);
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertEquals(0, step.getDesiredVelocity(velocity).y, 0.0001f);
        }
    }

    @Test
    void uprightCapsulesWalkSlopesWithoutPenetratingTheSupportPlane() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.25f).chunkSize(8).build())) {
            float[] triangles = { -2, -1, -2, 2, 1, -2, 2, 1, 2, -2, -1, -2, 2, 1, 2, -2, -1, 2 };
            world.layer("ramp").add(NavGeometry.triangles(triangles, false, 0, 3));
            finish(world);
            NavView view = world.view().radius(0.1f).height(0.6f).nearestDistance(0.5f).maxSlope(35).build();
            NavPath path = view.findPath(v(-1.25f, -0.31f, 0.25f), v(1.25f, 0.94f, 0.25f));
            assertTrue(path.isComplete(), path.getReason().toString());
            NavView steep = view.copy().maxSlope(20).build();
            assertFalse(steep.findPath(v(-1.25f, -0.31f, 0.25f), v(1.25f, 0.94f, 0.25f)).isComplete());
        }
    }

    @Test
    void nestedOppositeWindingShellsDoNotCreateNavigationInsideAFilledObject() {
        NavGeometry box = NavGeometry.capture(new Box(1, 1, 1), Transform.IDENTITY, true, 0, 3);
        float[] triangles = new float[box.triangleCount * 18];
        for (int i = 0; i < box.triangleCount * 9; i++) triangles[i] = box.triangles.get(i);
        int base = box.triangleCount * 9;
        for (int t = 0; t < box.triangleCount; t++) for (int vertex = 0; vertex < 3; vertex++) for (
            int axis = 0;
            axis < 3;
            axis++
        ) {
            int reversed = vertex == 0 ? 0 : 3 - vertex;
            triangles[base + t * 9 + vertex * 3 + axis] = box.triangles.get(t * 9 + reversed * 3 + axis) * 0.5f;
        }
        NavGeometry solid = NavGeometry.triangles(triangles, true, 0, 3);
        assertTrue(solid.contains(0, 0, 0));
        assertTrue(solid.contains(0.75f, 0, 0));
    }

    @Test
    void packedTriangleIndexAgreesWithBruteForceClosestSurface() {
        int n = 20;
        float[] triangles = new float[n * n * 18];
        int cursor = 0;
        for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
            float[] face = {
                x,
                x * 0.25f,
                z,
                x + 1,
                (x + 1) * 0.25f,
                z,
                x + 1,
                (x + 1) * 0.25f,
                z + 1,
                x,
                x * 0.25f,
                z,
                x + 1,
                (x + 1) * 0.25f,
                z + 1,
                x,
                x * 0.25f,
                z + 1,
            };
            System.arraycopy(face, 0, triangles, cursor, 18);
            cursor += 18;
        }
        NavGeometry mesh = NavGeometry.triangles(triangles, false, 0, 3);
        assertTrue(mesh.bvhNodes > 1);
        java.util.Random random = new java.util.Random(7);
        Vector3f point = v(0, 0, 0), expected = v(0, 0, 0), actual = v(0, 0, 0), normal = v(0, 0, 0), candidate = v(0, 0, 0);
        for (int i = 0; i < 100; i++) {
            point.set(random.nextFloat() * n, random.nextFloat() * 8 + 6, random.nextFloat() * n);
            float best = Float.POSITIVE_INFINITY;
            for (int t = 0; t < mesh.triangleCount; t++) {
                float distance = NavMath.closestTriangle(point, mesh.triangles, t * 9, candidate, normal);
                if (distance < best) {
                    best = distance;
                    expected.set(candidate);
                }
            }
            float indexed = NavMath.closestSurface(mesh, point, Vector3f.UNIT_Y, actual, normal);
            assertEquals(best, indexed, 0.0001f);
            assertEquals(0, actual.distance(expected), 0.001f);
        }
    }

    @Test
    void distanceLowerBoundAgreesWithTheDijkstraOracle() {
        try (NavWorld world = floor()) {
            world.layer("obstacle").add(NavGeometry.box(v(-0.5f, 0, -0.5f), v(0.5f, 1, 0.5f)));
            finish(world);
            NavView optimized = world.view().radius(0.1f).height(0.8f).build();
            NavView oracle = optimized.copy().policy((mode, area, inclination, ascent, base) -> base).build();
            for (int i = 0; i < 5; i++) {
                NavPath a = optimized.findPath(v(-2.25f, 0.4f, -1.25f + i * 0.5f), v(2.25f, 0.4f, 1.25f - i * 0.5f));
                NavPath b = oracle.findPath(v(-2.25f, 0.4f, -1.25f + i * 0.5f), v(2.25f, 0.4f, 1.25f - i * 0.5f));
                assertTrue(a.isComplete());
                assertTrue(b.isComplete());
                assertEquals(b.getCost(), a.getCost(), 0.00001);
            }
        }
    }

    @Test
    void authoredTraversalDifficultyAddsToTheActorCostBeforeCutoff() {
        try (NavWorld world = floor()) {
            Vector3f from = v(-1.25f, 0.5f, 0.25f), to = v(1.25f, 0.5f, 0.25f);
            world.addLink(new NavLink(from, to, NavMode.TELEPORT, 0.2f, 5, 0, 0, 0, true));
            NavView view = world.view().radius(0.1f).height(1).allow(NavMode.WALK, NavMode.TELEPORT).costCutoff(5).build();
            assertTrue(view.findPath(from, to).getCost() > 0);
            assertEquals(0, view.copy().costCutoff(6).build().findPath(from, to).getCost());
        }
    }
}
