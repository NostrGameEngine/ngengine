package org.ngengine.world2d;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.junit.jupiter.api.Test;
import org.ngengine.components.ComponentManager;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.world2d.box2d.TiledPhysicsComponent;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

class TiledPhysicsTileSwapTest {
    @Test
    void tileObjectRebuildsCollisionAfterAVisualOnlyState() {
        TiledMap map = new TiledMap(1, 1);
        map.setTileWidth(64);
        map.setTileHeight(64);
        World physicsWorld = new World(new Vec2());
        TiledWorld2d world = new TiledWorld2d("test", map, physicsWorld, 1, null);

        Tile closed = tileWithCollision(true);
        Tile open = tileWithCollision(false);
        TiledObjectEntity door = new TiledObjectEntity(1, 32, 64, closed);
        TestPhysicsComponent physics = new TestPhysicsComponent(map, world);

        physics.tick(physicsWorld, door);
        assertNotNull(physics.getBody());
        assertNotNull(physics.getBody().getFixtureList());

        door.setTile(open);
        physics.tick(physicsWorld, door);
        assertNull(physics.getBody());

        door.setTile(closed.copy());
        physics.tick(physicsWorld, door);
        assertNotNull(physics.getBody());
        assertNotNull(physics.getBody().getFixtureList());
    }

    private static Tile tileWithCollision(boolean physicsEnabled) {
        Tile tile = new Tile(0, 0, 64, 64);
        tile.putProperty("physics", physicsEnabled);
        TiledObjectLayer collisions = new TiledObjectLayer();
        collisions.add(new TiledObjectEntity(2, 8, 8, 48, 16));
        tile.setCollisions(collisions);
        return tile;
    }

    private static final class TestPhysicsComponent extends TiledPhysicsComponent {
        private final TiledMap map;
        private final TiledWorld2d world;

        private TestPhysicsComponent(TiledMap map, TiledWorld2d world) {
            this.map = map;
            this.world = world;
        }

        private void tick(World physicsWorld, TiledObjectEntity entity) {
            updatePhysics((ComponentManager) null, physicsWorld, entity);
        }

        @Override
        public <T> T getInstanceOf(Class<T> type) {
            if (type == TiledMap.class) return type.cast(map);
            if (type == TiledWorld2d.class) return type.cast(world);
            if (type.isInstance(world.getCoordinateSystem())) return type.cast(world.getCoordinateSystem());
            return null;
        }

        @Override
        public NGEAppSettings getSettings() {
            return null;
        }
    }
}
