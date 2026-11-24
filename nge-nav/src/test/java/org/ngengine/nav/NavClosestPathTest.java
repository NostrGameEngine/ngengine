package org.ngengine.nav;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavClosestPathTest {
    @Test void exhaustedSearchCanAnswerClosestAndMovedStartsWithoutLosingDirectedReachability() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("separate floors");
            for (int i = 0; i < 3; i++) layer.add(NavGeometry.box(new Vector3f(i * 2, -.1f, -.3f), new Vector3f(i * 2 + 1, 0, .3f)));
            while (!world.isReady()) world.pump(64);
            var from = new Vector3f(.3f, .1f, 0); var goal = new Vector3f(4.5f, .1f, 0);
            var view = world.view().radius(.05f).height(.2f).allow(NavMode.WALK, NavMode.TELEPORT).build();
            world.addLink(NavLink.teleport(from, new Vector3f(2.3f, .1f, 0), .1f, 0, false));
            assertFalse(view.findPath(from, goal).isComplete());
            var closest = view.findClosestPath(from, goal);
            assertTrue(closest.size() > 1);
            assertTrue(closest.getPoint(closest.size() - 1, new Vector3f()).x > 2.5f);
            assertFalse(view.findPath(new Vector3f(2.6f, .1f, 0), goal).isComplete());
            assertTrue(view.findPath(new Vector3f(4.2f, .1f, 0), goal).isComplete(), "A different component must get a new search");
            assertFalse(view.findPath(new Vector3f(2.6f, .1f, 0), from).isComplete(), "The one-way link must not be reversed");
        }
    }

    @Test void movingObstaclesInvalidateWarmEdgesEvenWhenTheFloorAndLinksStayTheSame() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("floor");
            layer.add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            while (!world.isReady()) world.pump(64);
            var view = world.view().radius(.05f).height(.2f).build();
            var from = new Vector3f(-.7f, .1f, 0); var to = new Vector3f(.7f, .1f, 0);
            assertTrue(view.findPath(from, to).isComplete());
            long floor = world.getSurfaceRevision();
            world.addLink(NavLink.teleport(new Vector3f(2, .1f, 0), new Vector3f(3, .1f, 0), .1f, 0, true));
            assertEquals(floor, world.getSurfaceRevision()); assertTrue(view.findPath(from, to).isComplete());
            var obstacle = layer.add(NavGeometry.box(new Vector3f(-.1f, 0, -.3f), new Vector3f(.1f, .4f, .3f),
                    0, NavGeometry.MOVABLE_OBSTACLE).asMovableObstacle(7));
            assertEquals(floor, world.getSurfaceRevision()); assertFalse(view.findPath(from, to).isComplete());
            obstacle.close(); assertEquals(floor, world.getSurfaceRevision()); assertTrue(view.findPath(from, to).isComplete());
        }
    }

    @Test void nearestDoesNotEvaluateCostsForDistantPages() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("separate floors");
            layer.add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            layer.add(NavGeometry.box(new Vector3f(10, -.1f, -.3f), new Vector3f(12, 0, .3f)));
            while (!world.isReady()) world.pump(64);
            var evaluations = new java.util.concurrent.atomic.AtomicInteger();
            var view = world.view().radius(.05f).height(.2f).nearestDistance(.1f).policy((mode, area, slope, ascent, cost) -> {
                evaluations.incrementAndGet(); return cost;
            }).build();
            var result = new Vector3f(); assertTrue(view.nearestPoint(new Vector3f(0, .1f, 0), result));
            assertEquals(.1f, result.y, .001f); assertTrue(result.lengthSquared() < .02f);
            assertTrue(evaluations.get() < 200, "A local nearest query must not evaluate the distant island: " + evaluations.get());
        }
    }

    @Test void removingATraversalInvalidatesRoutesWithoutPretendingTheFloorChanged() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("floor");
            layer.add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(-.2f, 0, .3f)));
            layer.add(NavGeometry.box(new Vector3f(.2f, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            while (!world.isReady()) world.pump(64);
            var view = world.view().radius(.05f).height(.2f).allow(NavMode.WALK, NavMode.TELEPORT).build();
            var from = new Vector3f(-.7f, .1f, 0); var to = new Vector3f(.7f, .1f, 0);
            long surfaces = world.getSurfaceRevision();
            assertFalse(view.findPath(from, to).isComplete());
            var link = new NavLink(from, to, NavMode.TELEPORT, .1f, 0, 0, 0, 0, true);
            world.addLink(link);
            assertTrue(view.findPath(from, to).isComplete());
            long revision = world.getRevision(); world.removeLink(link);
            assertTrue(world.getRevision() > revision); assertEquals(surfaces, world.getSurfaceRevision());
            assertNull(world.getLink(link.getId())); assertFalse(view.findPath(from, to).isComplete());
        }
    }

    @Test void exhaustedComponentsAreInvalidatedByNewSupportAndDoNotRejectReachableTargets() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("floor");
            layer.add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(-.2f, 0, .3f)));
            layer.add(NavGeometry.box(new Vector3f(.2f, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            while (!world.isReady()) world.pump(64);
            var view = world.view().radius(.05f).height(.2f).build();
            var from = new Vector3f(-.8f, .1f, 0);
            for (float x : new float[]{.8f, .6f, .4f})
                assertFalse(view.findPath(from, new Vector3f(x, .1f, 0)).isComplete());
            assertTrue(view.findPath(from, new Vector3f(-.4f, .1f, 0)).isComplete());
            assertFalse(view.findPath(from, new Vector3f(.8f, .1f, 0)).isComplete());
            assertTrue(view.findPath(new Vector3f(.4f, .1f, 0), new Vector3f(.8f, .1f, 0)).isComplete());
            assertFalse(view.findPath(from, new Vector3f(.8f, .1f, 0)).isComplete());
            layer.add(NavGeometry.box(new Vector3f(-.3f, -.1f, -.3f), new Vector3f(.3f, 0, .3f)));
            while (!world.isReady()) world.pump(64);
            assertTrue(view.findPath(from, new Vector3f(.8f, .1f, 0)).isComplete());
        }
    }

    @Test void anExplicitClosestQueryRetainsOnlyTheReachableSideOfAGap() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("floor");
            layer.add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(-.2f, 0, .3f)));
            layer.add(NavGeometry.box(new Vector3f(.2f, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            while (!world.isReady()) world.pump(64);
            var view = world.view().radius(.05f).height(.2f).build();
            Vector3f from = new Vector3f(-.8f, .1f, 0), target = new Vector3f(.8f, .1f, 0);
            assertFalse(view.findPath(from, target).isComplete());
            var closest = view.findClosestPath(from, target);
            assertFalse(closest.isComplete()); assertTrue(closest.size() > 1);
            Vector3f endpoint = closest.getPoint(closest.size() - 1, new Vector3f());
            assertTrue(endpoint.x > -.5f && endpoint.x < -.2f, "The safe corridor must finish on the original bank: " + endpoint);
            assertTrue(view.findClosestPath(from, new Vector3f(-.4f, .1f, 0)).isComplete());
        }
    }
}
