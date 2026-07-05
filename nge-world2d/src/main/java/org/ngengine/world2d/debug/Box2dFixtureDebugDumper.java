/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */

package org.ngengine.world2d.debug;

import java.util.Locale;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.world2d.box2d.Box2dUserData;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

import com.jme3.math.Vector2f;

public final class Box2dFixtureDebugDumper {
    public static final String SETTING = "PhysicsFixtureDebug";
    public static final String PROPERTY = "nge.box2d.debug.dumpFixtures";
    public static final String LEGACY_PROPERTY = "ngengine.box2d.debug.dumpFixtures";

    private Box2dFixtureDebugDumper() {
    }

    public static boolean isEnabled() {
        return Boolean.getBoolean(PROPERTY) || Boolean.getBoolean(LEGACY_PROPERTY);
    }

    public static boolean isEnabled(NGEAppSettings settings) {
        return settings != null && settings.getBoolean(SETTING, false);
    }

    public static void dumpFixture(CoordinateSystem coords, Body body, Fixture fixture) {
        Shape shape = fixture.getShape();
        Bounds bounds = worldBounds(coords, body, shape);
        System.out.println(String.format(
                Locale.ROOT,
                "[Box2DPhysicsFixture] body=%s fixture=%s sensor=%s type=%s bounds=[%.2f,%.2f]-[%.2f,%.2f] %s",
                Integer.toHexString(System.identityHashCode(body)),
                Integer.toHexString(System.identityHashCode(fixture)),
                fixture.isSensor(),
                shape.getType(),
                bounds.minX,
                bounds.minY,
                bounds.maxX,
                bounds.maxY,
                sourceSummary(fixture)));
    }

    private static Bounds worldBounds(CoordinateSystem coords, Body body, Shape shape) {
        Bounds bounds = new Bounds();
        Vec2 physics = new Vec2();
        Vector2f world = new Vector2f();

        if (shape instanceof PolygonShape) {
            PolygonShape poly = (PolygonShape) shape;
            for (int i = 0; i < poly.getVertexCount(); i++) {
                body.getWorldPointToOut(poly.getVertex(i), physics);
                coords.physicsToWorldSpace(physics, world);
                bounds.include(world.x, world.y);
            }
        } else if (shape instanceof CircleShape) {
            CircleShape cir = (CircleShape) shape;
            body.getWorldPointToOut(cir.m_p, physics);
            coords.physicsToWorldSpace(physics, world);

            Vec2 radiusPhysics = new Vec2(cir.m_radius, cir.m_radius);
            Vector2f radiusWorld = new Vector2f();
            coords.physicsToWorldSpace(radiusPhysics, radiusWorld);
            float radius = Math.max(Math.abs(radiusWorld.x), Math.abs(radiusWorld.y));
            bounds.include(world.x - radius, world.y - radius);
            bounds.include(world.x + radius, world.y + radius);
        }
        return bounds;
    }

    private static String sourceSummary(Fixture fixture) {
        Object userData = fixture.getUserData();
        if (!(userData instanceof Box2dUserData)) {
            return "source=" + userData;
        }

        Box2dUserData data = (Box2dUserData) userData;
        return "entity={" + entitySummary(data.getEntity()) + "} collision={" + collisionSummary(data.getCollision()) + "}";
    }

    private static String entitySummary(TiledEntity entity) {
        if (entity == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        out.append("id=").append(entity.getId())
                .append(" name=").append(entity.getName())
                .append(" class=").append(entity.getClazz());
        if (entity instanceof TiledObjectEntity) {
            TiledObjectEntity obj = (TiledObjectEntity) entity;
            out.append(" shape=").append(obj.getShape())
                    .append(" x=").append(fmt(obj.getX()))
                    .append(" y=").append(fmt(obj.getY()))
                    .append(" w=").append(fmt(obj.getWidth()))
                    .append(" h=").append(fmt(obj.getHeight()))
                    .append(" rot=").append(fmt(obj.getRotation()))
                    .append(" gid=").append(obj.getGid());
            Tile tile = obj.getTile();
            if (tile != null) {
                out.append(" tile=").append(tileSummary(tile));
            }
        }
        return out.toString();
    }

    private static String collisionSummary(TiledObjectEntity obj) {
        if (obj == null) {
            return "null";
        }
        return "id=" + obj.getId()
                + " name=" + obj.getName()
                + " class=" + obj.getClazz()
                + " shape=" + obj.getShape()
                + " x=" + fmt(obj.getX())
                + " y=" + fmt(obj.getY())
                + " w=" + fmt(obj.getWidth())
                + " h=" + fmt(obj.getHeight())
                + " rot=" + fmt(obj.getRotation());
    }

    private static String tileSummary(Tile tile) {
        String tileset = tile.getTileset() == null ? "null" : tile.getTileset().getName();
        return tileset + "#" + tile.getId()
                + "(" + tile.getClazz() + ")"
                + " gid=" + tile.getGid()
                + " gidNoMask=" + tile.getGidNoMask()
                + " flipH=" + tile.isFlippedHorizontally()
                + " flipV=" + tile.isFlippedVertically()
                + " flipD=" + tile.isFlippedAntiDiagonally();
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        private void include(float x, float y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
    }
}
