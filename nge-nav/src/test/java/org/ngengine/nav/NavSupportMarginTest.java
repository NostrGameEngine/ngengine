package org.ngengine.nav;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Box;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.ngengine.nav.NavWorldTest.finish;

class NavSupportMarginTest {
    @Test void fineVoxelsKeepStepsConnectedAcrossCapsuleClearance() {
        for (float offset : new float[]{-.08f, 0, .08f}) {
            try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
                var layer = world.layer("floor and resting plank");
                layer.add(NavGeometry.box(new Vector3f(-2, .5f, -2), new Vector3f(0, .8f, -.575f)));
                layer.add(NavGeometry.box(new Vector3f(-1.6f, .8f, -.575f), new Vector3f(-.6f, 1.08f, .075f)));
                var pose = new Transform(new Vector3f(-1.099f + offset, 1.005f, -.954f),
                        new Quaternion().fromAngleAxis(-16.4f * FastMath.DEG_TO_RAD, Vector3f.UNIT_X));
                layer.add(NavGeometry.capture(new Box(.18f, .035f, .55f), pose, true, 0, NavGeometry.DEFAULT_FLAGS));
                finish(world);
                var view = world.view().radius(.085f).height(.23f).stepHeight(.13f).supportMargin(.025f)
                        .contactSkin(.004f).nearestDistance(.2f).extendedSteps(true).build();
                var from = new Vector3f(-1.1f + offset, .919f, -1.7f);
                var to = new Vector3f(-1.1f + offset, 1.199f, -.2f);
                var path = view.findPath(from, to);
                assertTrue(path.isComplete(), "A supported low ramp must remain reachable: " + path.getReason());
                assertFalse(view.copy().extendedSteps(false).build().findPath(from, to).isComplete(),
                        "A rolling or constrained controller must not inherit clearance-wide stepping");
                assertFalse(view.copy().stepHeight(.07f).build().findPath(from, to).isComplete(),
                        "Wider adjacency must still respect the agent's step limit");
                layer.add(NavGeometry.box(new Vector3f(-1.8f, 1.05f, -1.6f), new Vector3f(-.4f, 1.3f, -1.3f), 0, 0));
                finish(world);
                assertFalse(view.findPath(from, to).isComplete(), "A wider step cannot pass through a low ceiling");
            }
        }
    }

    @Test void aSphericalAgentCanRestInsideAConcaveMesh() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.025f).chunkSize(12).build())) {
            world.layer("recess").add(NavGeometry.triangles(new float[]{
                    -.3f, 0, -.3f, 0, -.2f, -.3f, 0, -.2f, .3f,
                    -.3f, 0, -.3f, 0, -.2f, .3f, -.3f, 0, .3f,
                    0, -.2f, -.3f, .3f, 0, -.3f, .3f, 0, .3f,
                    0, -.2f, -.3f, .3f, 0, .3f, 0, -.2f, .3f
            }, false, 0, NavGeometry.WALKABLE));
            finish(world);
            var view = world.view().radius(.1f).height(.2f).contactSkin(.002f).nearestDistance(.045f).build();
            Vector3f fitted = new Vector3f();
            assertTrue(view.nearestPoint(new Vector3f(0, -.08f, 0), fitted),
                    "Offset a sphere along the support normal rather than through the opposite wall");
            assertTrue(Math.abs(fitted.x) < .05f && fitted.y < -.04f, "Stay inside the recess: " + fitted);
        }
    }

    @Test void navigationGuidesSupplySupportWithoutAddingInvisibleCollision() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("authored guide");
            layer.add(NavGeometry.box(new Vector3f(-.5f, -.02f, -.5f), new Vector3f(.5f, 0, .5f),
                    0, NavGeometry.WALKABLE | NavGeometry.SUPPORT_ONLY));
            finish(world);
            var walking = world.view().radius(.05f).height(.2f).nearestDistance(.05f).build();
            assertTrue(walking.findPath(new Vector3f(-.3f, .1f, 0), new Vector3f(.3f, .1f, 0)).isComplete());
            assertTrue(walking.isSegmentClear(new Vector3f(0, .15f, 0), new Vector3f(0, -.15f, 0)),
                    "A guide is not a physical floor or a lid across a recessed collider");
            layer.add(NavGeometry.box(new Vector3f(-.1f, -.2f, -.1f), new Vector3f(.1f, -.1f, .1f), 0, 0));
            finish(world);
            assertFalse(walking.isSegmentClear(new Vector3f(0, .15f, 0), new Vector3f(0, -.15f, 0)),
                    "Physical geometry still blocks traversal through a guide");
        }
    }

    @Test void walkingShortcutsRequireContinuousSupportAndCurrentObstacleClearance() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("floor");
            layer.add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(-.01f, 0, .3f)));
            layer.add(NavGeometry.box(new Vector3f(.01f, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            finish(world);
            var view = world.view().radius(.05f).height(.2f).nearestDistance(.05f).build();
            Vector3f from = new Vector3f(-.2f, .1f, 0), to = new Vector3f(.2f, .1f, 0);
            assertTrue(view.isSegmentClear(from, to));
            assertFalse(view.isWalkableSegment(from, to), "A capsule sweep cannot certify the floor beneath it");
            layer.add(NavGeometry.box(new Vector3f(-.02f, -.1f, -.3f), new Vector3f(.02f, 0, .3f)));
            finish(world); assertTrue(view.isWalkableSegment(from, to));
            var obstacle = layer.add(NavGeometry.box(new Vector3f(-.03f, 0, -.3f), new Vector3f(.03f, .3f, .3f)).asMovableObstacle(7));
            assertFalse(view.isWalkableSegment(from, to));
            obstacle.close(); assertTrue(view.isWalkableSegment(from, to));
        }
    }

    @Test void edgeClearanceAndSupportMarginAreIndependentPolicies() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            world.layer("arbitrary-support").add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            for (int x = -2; x <= 1; x++) for (int y = -1; y <= 0; y++)
                for (int z = -1; z <= 0; z++) world.load(new NavChunkKey(x, y, z));
            finish(world);
            var ordinary = world.view().radius(.05f).height(.2f).nearestDistance(.04f).build();
            var cautious = ordinary.copy().supportMargin(.08f).build();
            Vector3f edge = new Vector3f(0, .1f, .275f), center = new Vector3f(0, .1f, 0), out = new Vector3f();
            assertTrue(ordinary.nearestPoint(edge, out));
            assertFalse(cautious.nearestPoint(edge, out), "A free capsule can still have insufficient support near an edge");
            assertTrue(cautious.nearestPoint(center, out));
            assertTrue(cautious.copy().build().findPath(new Vector3f(-.7f, .1f, 0), new Vector3f(.7f, .1f, 0)).isComplete());
            assertFalse(cautious.copy().build().nearestPoint(edge, out), "Copy must retain the margin");
            assertTrue(cautious.copy().supportMargin(0).build().nearestPoint(edge, out));
            assertThrows(IllegalArgumentException.class, () -> ordinary.copy().supportMargin(-1));
        }
    }
}
