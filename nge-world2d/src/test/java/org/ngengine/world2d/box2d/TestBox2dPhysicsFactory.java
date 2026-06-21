package org.ngengine.world2d.box2d;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.FixtureDef;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.box2d.Box2dPhysicsFactory.PhysicsDef;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.math2d.Point;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

import com.jme3.math.Vector2f;

public class TestBox2dPhysicsFactory {

    @Test
    public void tileCollisionRectangleRotationAffectsFixtureVertices() {
        PolygonShape unrotated = collisionShapeForTileCollision(0);
        PolygonShape rotated = collisionShapeForTileCollision(90);

        assertEquals(40f, width(unrotated), 0.001f);
        assertEquals(10f, height(unrotated), 0.001f);
        assertEquals(10f, width(rotated), 0.001f);
        assertEquals(40f, height(rotated), 0.001f);
    }

    private PolygonShape collisionShapeForTileCollision(float rotationDegrees) {
        TiledMap map = new TiledMap(1, 1);
        map.setTileWidth(64);
        map.setTileHeight(64);

        Tile tile = new Tile(0, 0, 64, 64);
        TiledObjectLayer collisions = new TiledObjectLayer();
        TiledObjectEntity collision = new TiledObjectEntity(1, 8, 8, 40, 10);
        collision.setRotation(rotationDegrees);
        collisions.add(collision);
        tile.setCollisions(collisions);

        TiledTileEntity tileEntity = new TiledTileEntity(null, tile, 0, 0);
        PhysicsDef def = Box2dPhysicsFactory.createBody(new IdentityCoordinateSystem(), map, tileEntity);
        def = Box2dPhysicsFactory.createFixtures(def, new IdentityCoordinateSystem(), map, tileEntity);
        assertEquals(1, def.getFixtureDefs().size());
        FixtureDef fixture = def.getFixtureDefs().get(0);
        return (PolygonShape) fixture.shape;
    }

    private float width(PolygonShape shape) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < shape.m_count; i++) {
            min = Math.min(min, shape.m_vertices[i].x);
            max = Math.max(max, shape.m_vertices[i].x);
        }
        return max - min;
    }

    private float height(PolygonShape shape) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < shape.m_count; i++) {
            min = Math.min(min, shape.m_vertices[i].y);
            max = Math.max(max, shape.m_vertices[i].y);
        }
        return max - min;
    }

    private static final class IdentityCoordinateSystem implements CoordinateSystem {
        @Override
        public void gridToWorldSpace(float x, float y, Vector2f out) {
            out.set(x, y);
        }

        @Override
        public void gridToTile(float x, float y, Point out) {
            out.set(Math.round(x), Math.round(y));
        }

        @Override
        public void tileToGridSpace(float x, float y, Vector2f out) {
            out.set(x, y);
        }

        @Override
        public void tileToWorldSpace(float x, float y, Vector2f out) {
            out.set(x, y);
        }

        @Override
        public void worldToGridSpace(float x, float y, Vector2f out) {
            out.set(x, y);
        }

        @Override
        public void worldToTile(float x, float y, Point out) {
            out.set(Math.round(x), Math.round(y));
        }

        @Override
        public void worldToPhysicsSpace(float x, float y, Vector2f out) {
            out.set(x, y);
        }

        @Override
        public void physicsToWorldSpace(float x, float y, Vector2f out) {
            out.set(x, y);
        }

        @Override
        public void physicsToWorldSpace(org.jbox2d.common.Vec2 physicsWorldCoords, Vector2f out) {
            out.set(physicsWorldCoords.x, physicsWorldCoords.y);
        }

        @Override
        public float getTopDownYIndex(TiledObjectEntity o) {
            return 0;
        }

        @Override
        public float getTopDownYIndex(float x, float y) {
            return y;
        }

        @Override
        public void getCollisionCenterInGridSpace(TiledObjectEntity parentTileObject,
                TiledObjectEntity collisionObject, Vector2f out) {
            getCenterInGridSpace(collisionObject, out);
        }

        @Override
        public void getCenterInGridSpace(TiledBase obj, Vector2f out) {
            out.set(0, 0);
        }
    }
}
