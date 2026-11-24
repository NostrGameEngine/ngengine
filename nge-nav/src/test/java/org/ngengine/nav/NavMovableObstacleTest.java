package org.ngengine.nav;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.ngengine.nav.NavWorldTest.finish;

class NavMovableObstacleTest {
    @Test void movingCollisionDoesNotBecomeSupportOrRebuildTheFloorAndOnlyItsOwnerCanIgnoreIt() {
        try (var world = NavWorld.manual(NavSettings.builder().cellSize(.05f).chunkSize(12).build())) {
            var layer = world.layer("world");
            layer.add(NavGeometry.box(new Vector3f(-1, -.1f, -.3f), new Vector3f(1, 0, .3f)));
            finish(world);
            long surfaces = world.getSurfaceRevision();
            var obstacle = layer.add(NavGeometry.box(new Vector3f(-.1f, 0, -.4f), new Vector3f(.1f, .3f, .4f)).asMovableObstacle(7));
            assertTrue(world.isReady()); assertEquals(surfaces, world.getSurfaceRevision());
            var ordinary = world.view().radius(.05f).height(.2f).nearestDistance(.05f).build();
            var carrying = ordinary.copy().ignoreMovableObstacle(7).build();
            Vector3f from = new Vector3f(-.7f, .1f, 0), to = new Vector3f(.7f, .1f, 0);
            assertFalse(ordinary.findPath(from, to).isComplete());
            assertTrue(carrying.findPath(from, to).isComplete(), "The obstacle's route must retain the floor underneath it");
            assertFalse(ordinary.nearestPoint(new Vector3f(0, .4f, 0), new Vector3f()), "A movable object is not a walking surface");
            var other = layer.add(NavGeometry.box(new Vector3f(.35f, 0, -.4f), new Vector3f(.45f, .3f, .4f)).asMovableObstacle(8));
            assertFalse(carrying.copy().build().findPath(from, to).isComplete(), "Ignoring one target must retain other obstacles");
            other.close();
            obstacle.replace(NavGeometry.box(new Vector3f(-.1f, 0, .5f), new Vector3f(.1f, .3f, .7f)).asMovableObstacle(7));
            finish(world);
            assertTrue(ordinary.findPath(from, to).isComplete(), "Clearance must use the obstacle's new pose");
            assertThrows(IllegalArgumentException.class, () -> carrying.copy().ignoreMovableObstacle(-1));
        }
    }
}
