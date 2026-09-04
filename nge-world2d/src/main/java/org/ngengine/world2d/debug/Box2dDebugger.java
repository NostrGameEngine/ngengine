/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.world2d.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.box2d4j.b2DebugDraw;
import org.box2d4j.b2Transform;
import org.box2d4j.b2Vec2;
import org.box2d4j.b2WorldId;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.runner.Runner;
import org.ngengine.world2d.TiledWorld2d;
import org.ngengine.world2d.tiled.renderer.shape.Polyline;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import static org.box2d4j.B2.b2TransformPoint;
import static org.box2d4j.B2.b2World_Draw;

/** Draws Box2D4J shapes through the engine's regular scene graph. */
public final class Box2dDebugger {
    private static final Map<b2WorldId, Node> nodes = new HashMap<>();

    private Box2dDebugger() {
    }

    public static Node getDebugNode(b2WorldId world) {
        return nodes.get(world);
    }

    public static void update(
            Runner mainRunner,
            AssetManager assetManager,
            Collection<TiledWorld2d> worlds,
            float tpf) {
        update(mainRunner, assetManager, worlds, tpf, null);
    }

    public static void update(
            Runner mainRunner,
            AssetManager assetManager,
            Collection<TiledWorld2d> worlds,
            float tpf,
            NGEAppSettings settings) {
        Map<b2WorldId, Node> updatedNodes = new HashMap<>();
        for (TiledWorld2d tiledWorld : worlds) {
            b2WorldId world = tiledWorld.getPhysics();
            CoordinateSystem coordinates = tiledWorld.getCoordinateSystem();
            Node root = new Node("DebugPhysicsWorld");
            root.setQueueBucket(Bucket.Translucent);
            updatedNodes.put(world, root);

            b2DebugDraw draw = createDebugDraw(root, assetManager, coordinates);
            draw.drawShapes = true;
            draw.drawJoints = true;
            b2World_Draw(world, draw);
        }

        nodes.clear();
        nodes.putAll(updatedNodes);
        for (Node node : nodes.values()) {
            node.updateLogicalState(tpf);
            node.updateGeometricState();
        }
    }

    private static b2DebugDraw createDebugDraw(
            Node root,
            AssetManager assetManager,
            CoordinateSystem coordinates) {
        b2DebugDraw draw = new b2DebugDraw();
        draw.DrawPolygonFcn = (vertices, count, color) -> attachPolyline(
                root, assetManager, coordinates, vertices, count, null, true, color);
        draw.DrawSolidPolygonFcn = (transform, vertices, count, radius, color) -> attachPolyline(
                root, assetManager, coordinates, vertices, count, transform, true, color);
        draw.DrawCircleFcn = (center, radius, color) -> attachCircle(
                root, assetManager, coordinates, center, radius, color);
        draw.DrawSolidCircleFcn = (transform, radius, color) -> attachCircle(
                root, assetManager, coordinates, transform.p, radius, color);
        draw.DrawSolidCapsuleFcn = (p1, p2, radius, color) -> {
            attachSegment(root, assetManager, coordinates, p1, p2, color);
            attachCircle(root, assetManager, coordinates, p1, radius, color);
            attachCircle(root, assetManager, coordinates, p2, radius, color);
        };
        draw.DrawSegmentFcn = (p1, p2, color) -> attachSegment(
                root, assetManager, coordinates, p1, p2, color);
        return draw;
    }

    private static void attachPolyline(
            Node root,
            AssetManager assetManager,
            CoordinateSystem coordinates,
            b2Vec2[] vertices,
            int count,
            b2Transform transform,
            boolean closed,
            int color) {
        List<Vector2f> converted = new ArrayList<>(count);
        Vector2f world = new Vector2f();
        for (int i = 0; i < count; i++) {
            b2Vec2 point = transform == null ? vertices[i] : b2TransformPoint(transform, vertices[i]);
            coordinates.physicsToWorldSpace(point, world);
            converted.add(world.clone());
        }
        attach(root, assetManager, new Polyline(converted, closed), color);
    }

    private static void attachCircle(
            Node root,
            AssetManager assetManager,
            CoordinateSystem coordinates,
            b2Vec2 center,
            float radius,
            int color) {
        int segments = 16;
        b2Vec2 point = new b2Vec2();
        Vector2f world = new Vector2f();
        List<Vector2f> vertices = new ArrayList<>(segments);
        for (int i = 0; i < segments; i++) {
            float angle = ((float) i / segments) * FastMath.TWO_PI;
            point.set(
                    center.x + FastMath.cos(angle) * radius,
                    center.y + FastMath.sin(angle) * radius);
            coordinates.physicsToWorldSpace(point, world);
            vertices.add(world.clone());
        }
        attach(root, assetManager, new Polyline(vertices, true), color);
    }

    private static void attachSegment(
            Node root,
            AssetManager assetManager,
            CoordinateSystem coordinates,
            b2Vec2 p1,
            b2Vec2 p2,
            int color) {
        Vector2f world = new Vector2f();
        List<Vector2f> vertices = new ArrayList<>(2);
        coordinates.physicsToWorldSpace(p1, world);
        vertices.add(world.clone());
        coordinates.physicsToWorldSpace(p2, world);
        vertices.add(world.clone());
        attach(root, assetManager, new Polyline(vertices, false), color);
    }

    private static void attach(Node root, AssetManager assetManager, Polyline mesh, int packedColor) {
        Geometry geometry = new Geometry("DebugPhysicsShape", mesh);
        Material material = new Material(assetManager, com.jme3.material.Materials.UNSHADED);
        material.setColor("Color", unpackColor(packedColor));
        material.getAdditionalRenderState().setDepthTest(false);
        material.getAdditionalRenderState().setDepthWrite(false);
        geometry.setMaterial(material);
        root.attachChild(geometry);
    }

    private static ColorRGBA unpackColor(int color) {
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return new ColorRGBA(r, g, b, 1f);
    }
}
