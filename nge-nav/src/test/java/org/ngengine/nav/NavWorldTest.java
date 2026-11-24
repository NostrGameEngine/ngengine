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

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

class NavWorldTest {

    static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    static NavWorld floor() {
        NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(4).build());
        world.layer("terrain").add(NavGeometry.box(v(-3, -1, -3), v(3, 0, 3)));
        finish(world);
        return world;
    }

    static void finish(NavWorld world) {
        for (int i = 0; !world.isReady() && i < 10000; i++) {
            if (world.pump(32) == 0 && world.getFailure() != null) fail(world.getFailure());
        }
        assertTrue(world.isReady(), "Unfinished navigation builds: " + world.getPendingBuildCount());
    }

    @Test
    void nearestAreaTracksPublishedSurfaceMetadata() {
        try (NavWorld world = floor()) {
            NavView view = world.view().radius(.15f).height(1).nearestDistance(.6f).build();
            Vector3f point = v(-1.25f, .5f, -.25f);
            assertEquals(0, view.nearestArea(point));
            var overlay = world.layer("materials").add(NavGeometry.areaBox(v(-2, -.1f, -1), v(-.5f, .1f, .5f), 7));
            finish(world);
            assertEquals(7, view.nearestArea(point));
            overlay.close(); finish(world);
            assertEquals(0, view.nearestArea(point));
            assertEquals(-1, view.nearestArea(v(20, 20, 20)));
        }
    }

    @Test
    void loadedChunkSnapshotCannotMutateNavigation() {
        try (NavWorld world = floor()) {
            NavChunk[] chunks = world.getLoadedChunks();
            assertEquals(world.getLoadedChunkCount(), chunks.length);
            assertTrue(chunks.length > 0);
            chunks[0] = null;
            assertNotNull(world.getLoadedChunks()[0]);
            assertTrue(world.view().radius(.15f).height(1).build()
                    .findPath(v(-2.25f, .5f, -.25f), v(2.25f, .5f, -.25f)).isComplete());
        }
    }

    @Test
    void buildsWalkableFloorAcrossPages() {
        try (NavWorld world = floor()) {
            NavView view = world.view().radius(0.15f).height(1).build();
            NavPath path = view.findPath(v(-2.25f, 0.5f, -0.25f), v(2.25f, 0.5f, -0.25f));
            assertTrue(path.isComplete(), path.getReason().toString());
            assertTrue(path.size() > 2);
            Vector3f point = new Vector3f();
            for (int i = 0; i < path.size(); i++) {
                assertEquals(0.5f, path.getPoint(i, point).y, 0.001f);
                assertEquals(NavMode.WALK, path.getMode(i));
            }
        }
    }

    @Test
    void actorViewsHaveIndependentCutoffsAndZeroCosts() {
        try (NavWorld world = floor()) {
            NavView allowed = world.view().radius(0.15f).height(1).cost(NavMode.WALK, 2).costCutoff(2).build();
            NavView forbidden = allowed.copy().costCutoff(1.999f).build();
            Vector3f from = v(-1.25f, 0.5f, -0.25f), to = v(1.25f, 0.5f, -0.25f), store = new Vector3f();
            assertTrue(allowed.findPath(from, to).isComplete());
            assertFalse(forbidden.findPath(from, to).isComplete());
            assertFalse(forbidden.nearestPoint(from, store));
            assertFalse(forbidden.randomPoint(7, store));
            NavPath free = allowed.copy().cost(NavMode.WALK, 0).build().findPath(from, to);
            assertTrue(free.isComplete());
            assertEquals(0, free.getCost());
            assertTrue(allowed.randomPoint(7, store));
        }
    }

    @Test
    void closedSolidHasNoInteriorNavigationButRoofedRoomsRemainUsable() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().cellSize(0.5f).chunkSize(4).build())) {
            NavLayer layer = world.layer("room");
            layer.add(NavGeometry.box(v(-2, -1, -2), v(2, 0, 2)));
            layer.add(NavGeometry.box(v(-2, 2, -2), v(2, 2.5f, 2)));
            layer.add(NavGeometry.box(v(-0.5f, 0, -0.5f), v(0.5f, 1.5f, 0.5f)));
            finish(world);
            NavView flyer = world.view().radius(0.1f).height(0.2f).nearestDistance(0.3f).allow(NavMode.FLY).build();
            assertFalse(flyer.nearestPoint(v(0, 0.75f, 0), new Vector3f()));
            assertTrue(flyer.nearestPoint(v(1.25f, 1.25f, 1.25f), new Vector3f()));
        }
    }

    @Test
    void navigationYieldsWhilePlanningAndNeverBuildsInUpdate() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            NavStep step = new NavStep();
            navigator.target(v(1.25f, 0.5f, -0.25f));
            assertEquals(NavStatus.YIELD, navigator.update(motion, 0.016f, step).getStatus());
            assertEquals(NavReason.PATH_PENDING, step.getReason());
            world.pump(1);
            assertEquals(NavStatus.MOVE, navigator.update(motion, 0.016f, step).getStatus());
            assertTrue(step.getDesiredVelocity(new Vector3f()).x > 0);
        }
    }

    @Test
    void movingWhilePlanningJoinsTheRouteAheadOfTheCurrentPosition() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            motion.velocity.set(2, 0, 0);
            NavStep step = new NavStep();
            navigator.target(v(1.25f, 0.5f, -0.25f));
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            motion.position.x += 0.7f;
            assertEquals(NavStatus.MOVE, navigator.update(motion, 0.016f, step).getStatus());
            assertFalse(step.isPositionCorrection());
            assertTrue(
                step.getDesiredVelocity(new Vector3f()).x > 0,
                "A completed plan must not send a coasting actor backward"
            );
            assertTrue(step.getNextPosition(new Vector3f()).x > motion.position.x);
        }
    }

    @Test
    void routeProjectionIsDistinguishedFromForwardTravel() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.15f).height(1).build().navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            NavStep step = new NavStep();
            navigator.target(v(1.25f, 0.5f, -0.25f));
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            motion.position.addLocal(0.1f, 0, 0.2f);
            assertEquals(NavStatus.MOVE, navigator.update(motion, 0.016f, step).getStatus());
            assertTrue(step.isPositionCorrection());
            Vector3f velocity = new Vector3f();
            assertTrue(step.getDesiredVelocity(velocity).x < 0);
            for (int i = 0; i < 20 && step.isPositionCorrection(); i++) {
                step.getDesiredVelocity(velocity).multLocal(step.getHorizon());
                motion.position.addLocal(velocity);
                navigator.update(motion, 0.016f, step);
            }
            assertEquals(NavStatus.MOVE, step.getStatus());
            assertFalse(step.isPositionCorrection());
            assertTrue(step.getDesiredVelocity(velocity).x > 0);
            navigator.clearTarget();
            assertEquals(NavStatus.YIELD, navigator.update(motion, 0.016f, step).getStatus());
            assertFalse(step.isPositionCorrection());
        }
    }

    @Test
    void rejoiningAfterCoastingKeepsAnAuthoredTraversalAction() {
        try (NavWorld world = floor()) {
            Vector3f from = v(-1.25f, 0.5f, -0.25f), to = v(1.25f, 0.5f, -0.25f);
            world.addLink(NavLink.teleport(from, to, 0.2f, 0, false));
            NavNavigator navigator = world
                .view()
                .radius(0.15f)
                .height(1)
                .allow(NavMode.WALK, NavMode.TELEPORT)
                .build()
                .navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(from);
            NavStep step = new NavStep();
            navigator.target(to);
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            motion.position.x += 0.1f;
            Vector3f displacement = new Vector3f();
            for (int i = 0; i < 20; i++) {
                navigator.update(motion, 0.016f, step);
                if (step.getStatus() != NavStatus.MOVE) break;
                step.getDesiredVelocity(displacement).multLocal(step.getHorizon());
                motion.position.addLocal(displacement);
            }
            assertEquals(NavStatus.TRANSITION, step.getStatus());
            assertEquals(NavAction.TELEPORT, step.getAction());
            assertTrue(step.getActionId() > 0);
            assertEquals(from, motion.position);
        }
    }

    @Test
    void destructionInvalidatesAnExistingRouteBeforeRebuilding() {
        try (NavWorld world = floor()) {
            NavView view = world.view().radius(0.15f).height(1).build();
            NavNavigator navigator = view.navigator();
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, -0.25f);
            NavStep step = new NavStep();
            navigator.target(v(1.25f, 0.5f, -0.25f));
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            assertEquals(NavStatus.MOVE, navigator.update(motion, 0.016f, step).getStatus());
            world.layer("terrain").close();
            assertEquals(NavStatus.YIELD, navigator.update(motion, 0.016f, step).getStatus());
            assertEquals(Vector3f.ZERO, step.getDesiredVelocity(new Vector3f()));
            finish(world);
            assertFalse(view.nearestPoint(motion.position, new Vector3f()));
        }
    }

    @Test
    void staleBuildCannotPublishAfterASecondEditOrUnload() {
        try (NavWorld world = NavWorld.manual(NavSettings.builder().chunkSize(4).build())) {
            NavSource source = world.layer("terrain").add(NavGeometry.box(v(-1, -1, -1), v(1, 0, 1)));
            source.replace(NavGeometry.box(v(-1, 3, -1), v(1, 4, 1)));
            NavChunkKey absent = new NavChunkKey(0, 0, 0);
            world.unload(absent);
            finish(world);
            assertNull(world.getChunk(absent));
            NavView view = world.view().radius(0.1f).height(0.5f).nearestDistance(0.5f).build();
            assertFalse(view.nearestPoint(v(0.75f, 0.25f, 0.75f), new Vector3f()));
            assertTrue(view.nearestPoint(v(0.75f, 4.25f, 0.75f), new Vector3f()));
        }
    }

    @Test
    void explicitPagingDoesNotReloadEvictedPagesOnGeometryUpdates() {
        NavSettings settings = NavSettings.builder().cellSize(0.5f).chunkSize(4).autoLoadPages(false).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            NavSource source = world.layer("terrain").add(NavGeometry.box(v(-3, -1, -3), v(3, 0, 3)));
            assertEquals(0, world.getPendingBuildCount());
            assertEquals(0, world.getLoadedChunkCount());
            NavChunkKey key = new NavChunkKey(0, 0, 0);
            world.load(key);
            finish(world);
            NavView view = world.view().radius(0.1f).height(0.5f).nearestDistance(1).build();
            Vector3f from = v(0.75f, 0.25f, 0.75f), target = v(2.25f, 0.25f, 0.75f);
            assertEquals(NavReason.WAITING_FOR_DATA, view.findPath(from, target).getReason());
            world.load(new NavChunkKey(1, 0, 0));
            finish(world);
            assertTrue(view.findPath(from, target).isComplete());
            world.unload(key);
            source.replace(NavGeometry.box(v(-3, -2, -3), v(3, 0, 3)));
            finish(world);
            assertNull(world.getChunk(key));
            assertEquals(1, world.getLoadedChunkCount());
            assertEquals(NavReason.WAITING_FOR_DATA, view.findPath(from, target).getReason());
        }
    }

    @Test
    void planarWorldUsesTheSameSearchAndSupportsArbitraryPlanes() {
        NavSettings settings = NavSettings.builder().cellSize(0.5f).chunkSize(4).plane(v(0, 0, 0), v(0, 0, 1)).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) world.load(new NavChunkKey(x, y, 0));
            world.layer("obstacles").add(NavGeometry.box(v(-0.3f, -0.3f, -1), v(0.3f, 0.3f, 1)));
            finish(world);
            NavView view = world.view().radius(0.1f).height(0.2f).build();
            NavPath path = view.findPath(v(-1.25f, 0.25f, 0), v(1.25f, 0.25f, 0));
            assertTrue(path.isComplete(), path.getReason().toString());
            Vector3f point = new Vector3f();
            for (int i = 0; i < path.size(); i++) assertEquals(0, path.getPoint(i, point).z, 0.0001f);
        }
    }

    @Test
    void flyingAndClimbingAreActorPoliciesOnOneWorld() {
        try (NavWorld world = floor()) {
            world.layer("wall").add(NavGeometry.box(v(-0.5f, 0, -2), v(0, 3, 2)));
            finish(world);
            NavView climber = world.view().radius(0.1f).height(0.4f).allow(NavMode.CLIMB).nearestDistance(0.4f).build();
            NavPath climb = climber.findPath(v(0.2f, 0.75f, 0.75f), v(0.2f, 2.25f, 0.75f));
            assertTrue(climb.isComplete(), climb.getReason().toString());
            NavView walker = climber.copy().allow(NavMode.WALK).build();
            assertFalse(walker.nearestPoint(v(0.2f, 2.25f, 0.75f), new Vector3f()));
            world.load(new NavChunkKey(1, 1, 0));
            finish(world);
            NavView flyer = climber.copy().radius(0.1f).height(0.2f).allow(NavMode.FLY).cost(NavMode.FLY, 0).build();
            NavPath flight = flyer.findPath(v(0.75f, 0.75f, 0.75f), v(2.75f, 2.75f, 0.75f));
            assertTrue(flight.isComplete(), flight.getReason().toString());
            assertEquals(0, flight.getCost());
        }
    }

    @Test
    void planarPhysicsPolygonAndAreaCostsShareTheGenerationPipeline() {
        NavSettings settings = NavSettings.builder().chunkSize(4).plane(v(0, 0, 0), v(0, 0, 1)).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            NavLayer layer = world.layer("physics");
            layer.add(
                NavGeometry.planarPolygon(settings, new float[] { -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f }, 0, 3)
            );
            layer.add(NavGeometry.areaBox(v(-2, -2, -0.25f), v(-0.5f, 2, 0.25f), 9));
            finish(world);
            NavView view = world.view().radius(0.1f).height(0.2f).nearestDistance(0.3f).areaCost(9, 10).costCutoff(5).build();
            assertFalse(view.nearestPoint(v(-0.75f, 0.75f, 0), v(0, 0, 0)));
            assertTrue(view.nearestPoint(v(0.75f, 0.75f, 0), v(0, 0, 0)));
            assertFalse(view.nearestPoint(v(0.25f, 0.25f, 0), v(0, 0, 0)));
        }
    }

    @Test
    void unloadingReadyPagesDoesNotDecrementThePendingBuildCounter() {
        try (NavWorld world = floor(); NavWorld loaded = NavWorld.manual(world.settings)) {
            NavChunkKey key = new NavChunkKey(0, 0, 0);
            NavChunk page = world.getChunk(key);
            world.unload(key);
            assertEquals(0, world.getPendingBuildCount());
            assertTrue(world.isReady());
            world.load(key);
            finish(world);
            assertEquals(0, world.getPendingBuildCount());
            NavLayer layer = loaded.layer("cache");
            layer.attach(page);
            layer.close();
            assertEquals(0, loaded.getLoadedChunkCount());
            assertEquals(0, loaded.getPendingBuildCount());
            assertTrue(loaded.isReady());
        }
    }

    @Test
    void anActorCanRecoverAfterAnUnsupportedInitialPositionWithoutChangingTheGoal() {
        try (NavWorld world = floor()) {
            NavNavigator navigator = world.view().radius(0.1f).height(1).nearestDistance(0.3f).build().navigator();
            navigator.target(v(1.25f, 0.5f, 0.25f));
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 1.5f, 0.25f);
            NavStep step = new NavStep();
            navigator.update(motion, 0.016f, step);
            world.pump(1);
            assertEquals(NavReason.UNSUPPORTED_POSITION, navigator.update(motion, 0.016f, step).getReason());
            motion.position.set(-1.25f, 0.5f, 0.25f);
            assertEquals(NavReason.PATH_PENDING, navigator.update(motion, 0.016f, step).getReason());
            world.pump(1);
            assertEquals(NavStatus.MOVE, navigator.update(motion, 0.016f, step).getStatus());
        }
    }

    @Test
    void planarGravityCanLieInsideTheMotionPlaneForPlatformGames() {
        NavSettings settings = NavSettings.builder().chunkSize(4).plane(v(0, 0, 0), v(0, 0, 1)).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            world.layer("floor").add(NavGeometry.box(v(-3, -1, -1), v(3, 0, 1)));
            finish(world);
            NavView view = world.view().radius(0.1f).height(1).up(Vector3f.UNIT_Y).nearestDistance(0.3f).build();
            NavPath path = view.findPath(v(-1.25f, 0.5f, 0), v(1.25f, 0.5f, 0));
            assertTrue(path.isComplete(), path.getReason().toString());
            for (int i = 0; i < path.size(); i++) {
                Vector3f point = path.getPoint(i, v(0, 0, 0));
                assertEquals(0, point.z, 0.0001f);
                assertEquals(0.5f, point.y, 0.0001f);
            }
        }
    }

    @Test
    void planarPlatformCharactersJumpAcrossGapsWithoutAnArtificialAirborneSupportSurface() {
        NavSettings settings = NavSettings.builder().chunkSize(4).plane(v(0, 0, 0), v(0, 0, 1)).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            world.layer("platforms").add(NavGeometry.box(v(-3, -1, -1), v(-0.5f, 0, 1)));
            world.layer("platforms").add(NavGeometry.box(v(0.5f, -1, -1), v(3, 0, 1)));
            finish(world);
            NavView view = world.view().radius(0.1f).height(1).up(Vector3f.UNIT_Y).nearestDistance(0.3f).build();
            assertFalse(view.copy().allow(NavMode.WALK, NavMode.CLIMB).build().nearestPoint(v(0, 1.25f, 0), v(0, 0, 0)));
            assertFalse(view.findPath(v(-1.25f, 0.5f, 0), v(1.25f, 0.5f, 0)).isComplete());
            world.addLink(NavLink.jump(v(-0.75f, 0.5f, 0), v(0.75f, 0.5f, 0), 0.2f, 0.8f, 0.6f));
            assertTrue(
                view
                    .copy()
                    .allow(NavMode.WALK, NavMode.JUMP)
                    .build()
                    .findPath(v(-1.25f, 0.5f, 0), v(1.25f, 0.5f, 0))
                    .isComplete()
            );
        }
    }

    @Test
    void coplanarRenderSheetsAndSolidsBelowThePlaneDoNotBlockTopDownActors() {
        NavSettings settings = NavSettings.builder().chunkSize(4).plane(v(0, 0, 0), v(0, 0, 1)).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            world
                .layer("floor")
                .add(
                    NavGeometry.triangles(
                        new float[] { -2, -2, 0, 2, -2, 0, 2, 2, 0, -2, -2, 0, 2, 2, 0, -2, 2, 0 },
                        false,
                        0,
                        3
                    )
                );
            world.layer("floor").add(NavGeometry.box(v(-2, -2, -1), v(2, 2, 0)));
            finish(world);
            NavView view = world.view().radius(0.1f).height(1).nearestDistance(0.3f).build();
            assertTrue(view.findPath(v(-1.25f, 0.25f, 0), v(1.25f, 0.25f, 0)).isComplete());
        }
    }

    @Test
    void freeFlightUsesDistanceToBreakCostTiesWithinABoundedWorkspace() {
        NavSettings settings = NavSettings.builder().cellSize(0.5f).chunkSize(4).searchBudget(256, 128).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            for (int z = 0; z < 3; z++) for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) world.load(
                new NavChunkKey(x, y, z)
            );
            finish(world);
            NavPath path = world
                .view()
                .radius(0.1f)
                .height(0.2f)
                .allow(NavMode.FLY)
                .cost(NavMode.FLY, 0)
                .build()
                .findPath(v(0.75f, 0.75f, 0.75f), v(4.75f, 4.75f, 4.75f));
            assertTrue(path.isComplete(), path.getReason().toString());
            assertEquals(0, path.getCost());
        }
    }

    @Test
    void freeFlightCanReachAPaidLandingWithoutFloodingAllResidentAirspace() {
        NavSettings settings = NavSettings.builder().cellSize(0.5f).chunkSize(4).searchBudget(256, 128).build();
        try (NavWorld world = NavWorld.manual(settings)) {
            world.layer("floor").add(NavGeometry.box(v(-1, -1, -1), v(7, 0, 7)));
            finish(world);
            NavPath path = world
                .view()
                .radius(0.15f)
                .height(1)
                .allow(NavMode.WALK, NavMode.FLY)
                .cost(NavMode.WALK, 10)
                .cost(NavMode.FLY, 0)
                .build()
                .findPath(v(0.25f, 0.5f, 0.25f), v(5.25f, 0.5f, 0.25f));
            assertTrue(path.isComplete(), path.getReason().toString());
            assertEquals(2.5, path.getCost(), 0.0001);
        }
    }
}
