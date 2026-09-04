package org.ngengine.world2d;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.box2d4j.b2BodyId;
import org.box2d4j.b2Circle;
import org.box2d4j.b2ShapeDef;
import org.box2d4j.b2ShapeId;
import org.box2d4j.b2Vec2;
import org.box2d4j.b2WorldId;
import org.junit.jupiter.api.Test;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.world2d.box2d.TiledPhysicsComponent;
import org.ngengine.world2d.box2d.Box2dHelper;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

import static org.box2d4j.B2.*;

class TiledPhysicsTileSwapTest {
    @Test
    void destroyedContactShapeIsIgnored() {
        b2WorldId world = b2CreateWorld(b2DefaultWorldDef());
        b2BodyId body = b2CreateBody(world, b2DefaultBodyDef());
        b2ShapeDef shapeDef = b2DefaultShapeDef();
        shapeDef.userData = new Object();
        b2ShapeId shape = b2CreateCircleShape(body, shapeDef, new b2Circle(new b2Vec2(), 1f));
        b2DestroyShape(shape, false);

        assertNull(TiledWorld2dManagerComponent.getShapeUserDataIfValid(shape));
        b2DestroyWorld(world);
    }

    @Test
    void refreshPhysicsIsSafeBeforeTheComponentIsAttached() {
        TiledPhysicsComponent physics = new TiledPhysicsComponent();

        assertDoesNotThrow(physics::refreshPhysics);
    }

    @Test
    void tileObjectRebuildsCollisionAfterAVisualOnlyState() {
        TiledMap map = new TiledMap(1, 1);
        map.setTileWidth(64);
        map.setTileHeight(64);
        b2WorldId physicsWorld = b2CreateWorld(b2DefaultWorldDef());
        TiledWorld2d world = new TiledWorld2d("test", map, physicsWorld, 1, null);

        Tile closed = tileWithCollision(true);
        Tile open = tileWithCollision(false);
        TiledObjectEntity door = new TiledObjectEntity(1, 32, 64, closed);
        TestPhysicsComponent physics = new TestPhysicsComponent(map, world);
        physics.attach(door);

        physics.refreshPhysics();
        assertNotNull(physics.getBody());
        assertTrue(b2Body_GetShapeCount(physics.getBody()) > 0);

        door.setTile(open);
        physics.refreshPhysics();
        assertNull(physics.getBody());

        door.setTile(closed.copy());
        physics.refreshPhysics();
        assertNotNull(physics.getBody());
        assertTrue(b2Body_GetShapeCount(physics.getBody()) > 0);
    }

    @Test
    void collisionOverrideReactivatesTheExistingDoorBody() {
        TiledMap map = new TiledMap(1, 1);
        map.setTileWidth(64);
        map.setTileHeight(64);
        b2WorldId physicsWorld = b2CreateWorld(b2DefaultWorldDef());
        TiledWorld2d world = new TiledWorld2d("test", map, physicsWorld, 1, null);
        TiledObjectEntity door = new TiledObjectEntity(1, 32, 64, tileWithCollision(true));
        TestPhysicsComponent physics = new TestPhysicsComponent(map, world);
        physics.attach(door);
        physics.refreshPhysics();

        b2BodyId originalBody = physics.getBody();
        assertNotNull(originalBody);
        assertTrue(b2Body_GetShapeCount(originalBody) > 0);

        physics.setCollisionEnabled(false);
        assertFalse(b2Body_IsEnabled(originalBody));

        physics.setCollisionEnabled(true);
        assertSame(originalBody, physics.getBody());
        assertTrue(b2Body_IsEnabled(originalBody));
        assertTrue(b2Body_GetShapeCount(originalBody) > 0);
    }

    @Test
    void externallyDestroyedBodyIsRecreatedInsteadOfDereferenced() {
        TiledMap map = new TiledMap(1, 1);
        map.setTileWidth(64);
        map.setTileHeight(64);
        b2WorldId physicsWorld = b2CreateWorld(b2DefaultWorldDef());
        TiledWorld2d world = new TiledWorld2d("test", map, physicsWorld, 1, null);
        TiledObjectEntity entity = new TiledObjectEntity(1, 32, 64, tileWithCollision(true));
        TestPhysicsComponent physics = new TestPhysicsComponent(map, world);
        physics.attach(entity);
        physics.refreshPhysics();

        b2BodyId destroyedBody = physics.getBody();
        b2DestroyBody(destroyedBody);
        assertFalse(physics.hasBody());

        assertDoesNotThrow(physics::refreshPhysics);
        assertTrue(physics.hasBody());
        assertFalse(B2_ID_EQUALS(destroyedBody, physics.getBody()));
    }

    @Test
    void bodyDestructionRequestedDuringStepIsDeferredAndIdempotent() {
        TiledMap map = new TiledMap(1, 1);
        b2WorldId physicsWorld = b2CreateWorld(b2DefaultWorldDef());
        TiledWorld2d world = new TiledWorld2d("test", map, physicsWorld, 1, null);
        b2BodyId body = b2CreateBody(physicsWorld, b2DefaultBodyDef());

        world.beginPhysicsStep();
        world.destroyPhysics(body);
        world.destroyPhysics(body);
        assertTrue(b2Body_IsValid(body));

        assertDoesNotThrow(world::endPhysicsStep);
        assertFalse(b2Body_IsValid(body));
        b2DestroyWorld(physicsWorld);
    }

    @Test
    void worldLifecycleMutationIsDeferredUntilTheFrameUpdateEnds() {
        TiledWorld2dManagerComponent manager = new TiledWorld2dManagerComponent((String) null);
        AtomicBoolean executed = new AtomicBoolean();

        manager.beginWorldUpdate();
        manager.runAfterWorldUpdate(() -> executed.set(true));
        assertFalse(executed.get());

        manager.endWorldUpdate();
        assertTrue(executed.get());
    }

    @Test
    void stringPhysicsPropertiesUseTheirDeclaredBooleanValue() {
        TiledObjectEntity enabled = new TiledObjectEntity(1, 0, 0, 1, 1);
        enabled.putProperty("physics", "true");
        TiledObjectEntity disabled = new TiledObjectEntity(2, 0, 0, 1, 1);
        disabled.putProperty("physics", "false");

        assertTrue(Box2dHelper.isPhysicsEnabled(enabled));
        assertFalse(Box2dHelper.isPhysicsEnabled(disabled));
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
        private TiledObjectEntity entity;

        private TestPhysicsComponent(TiledMap map, TiledWorld2d world) {
            this.map = map;
            this.world = world;
        }

        private void attach(TiledObjectEntity entity) {
            this.entity = entity;
            onAttached(entity.getComponentManager(), null, null);
        }

        @Override
        public <T> T getInstanceOf(Class<T> type) {
            if (type == TiledMap.class) return type.cast(map);
            if (type == TiledWorld2d.class) return type.cast(world);
            if (entity != null && type.isInstance(entity)) return type.cast(entity);
            if (type.isInstance(world.getCoordinateSystem())) return type.cast(world.getCoordinateSystem());
            return null;
        }

        @Override
        public NGEAppSettings getSettings() {
            return null;
        }
    }
}
