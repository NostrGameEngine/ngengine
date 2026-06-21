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

package org.ngengine.world2d.box2d;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.ngengine.platform.NGEUtils;
import org.ngengine.world2d.PropertiesKeys;
import org.ngengine.world2d.PropertiesKeys.phy;
import org.ngengine.world2d.TiledWorld2dManagerComponent;

import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

public class Box2dPhysicsFactory {

    private static final Logger logger = Logger.getLogger(Box2dPhysicsFactory.class.getName());

    private static final Vec2 tmpVec2 = new Vec2();
    private static final Vector2f tmpVec2f = new Vector2f();

    /**
     * Build physics definition (a BodyDef + list of FixtureDefs) for a Tiled entity.
     *
     * @param coords coordinate conversion helper
     * @param map    tiled map (used for tile size/orientation)
     * @param entity tile cell or map object
     * @return a PhysicsDef wrapping the body def and its fixtures (may be empty)
     */
    public static PhysicsDef createBody(CoordinateSystem coords, TiledMap map, TiledEntity entity) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyType.valueOf(
                NGEUtils.safeString(entity.getPropertyOrDefault(phy.type, "STATIC")).toUpperCase());
        bodyDef.userData = new Box2dUserData(entity, null);
        bodyDef.bullet = NGEUtils.safeBool(entity.getPropertyOrDefault(phy.bullet, 0.0));
        bodyDef.fixedRotation = NGEUtils.safeBool(entity.getPropertyOrDefault(phy.fixedRotation, 1.0));
        bodyDef.linearDamping = (float) NGEUtils.safeDouble(entity.getPropertyOrDefault(phy.linearDamping, 0.0));
        bodyDef.angularDamping = (float) NGEUtils.safeDouble(entity.getPropertyOrDefault(phy.angularDamping, 0.0));
        bodyDef.gravityScale = (float) NGEUtils.safeDouble(entity.getPropertyOrDefault(phy.gravityScale, 1.0));

        PhysicsDef physicsDef = new PhysicsDef(bodyDef);
        return physicsDef;
    }

      /**
     * Build physics definition (a BodyDef + list of FixtureDefs) for a Tiled entity.
     *
     * @param coords coordinate conversion helper
     * @param map    tiled map (used for tile size/orientation)
     * @param entity tile cell or map object
     * @return a PhysicsDef wrapping the body def and its fixtures (may be empty)
     */
    public static PhysicsDef createFixtures(
        PhysicsDef physicsDef,
        CoordinateSystem coords, TiledMap map, TiledEntity entity
    ) {
         
        physicsDef.fixtureDefs.clear();
        boolean isIso = map.getOrientation() != Orientation.ORTHOGONAL;

        if (entity instanceof TiledTileEntity) {
            // Tile-layer cell with collision objects coming from the tile's collision editor
            TiledTileEntity tileEntity = (TiledTileEntity) entity;
            Tile tile = tileEntity.getTile();
            if (tile == null || tile.getCollisions() == null) {
                return physicsDef;
            }

            // Pad Y to align tile collision origin with map tile cell
            final float hTile = (float) tile.getHeight();
            final float padY = map.getTileHeight() - hTile;
            final boolean wFlip = tile.isFlippedHorizontally();
            final boolean hFlip = tile.isFlippedVertically();

            for (TiledObjectEntity coll : tile.getCollisions().getObjects()) {
                float baseX = (float) coll.getX();
                if (isIso) {
                    // Isometric tiles are centered; shift X half-width
                    baseX -= (float) tile.getWidth() * 0.5f;
                }
                float baseY = (float) coll.getY() + padY;

                float wScale = 1f;
                float hScale = 1f;

                addFixtureLocal(
                        coords, map,
                        baseX, baseY,
                        wScale, hScale,
                        coll, entity, physicsDef,
                        false,
                         wFlip,
                         hFlip
                );
            }

        } else if (entity instanceof TiledObjectEntity) {
            // Object layer item: could be a TILE object or a regular geometric shape
            TiledObjectEntity obj = (TiledObjectEntity) entity;

            if (obj.getShape() == ObjectShape.TILE) {
                // A placed tile as an object; collision shapes are relative to the tile art
                Tile tile = obj.getTile();
                if (tile != null && tile.getCollisions() != null) {
                    float wTile = (float) tile.getWidth();
                    float hTile = (float) tile.getHeight();
                    float wObject = (float) obj.getWidth();
                    float hObject = (float) obj.getHeight();

                    float wScale = wObject / wTile;
                    float hScale = hObject / hTile;

                    boolean wFlip = tile.isFlippedHorizontally();
                    boolean hFlip = tile.isFlippedVertically();

                    for (TiledObjectEntity coll : tile.getCollisions().getObjects()) {
                        Boolean physics = (Boolean) coll.getProperty("physics");
                        if(physics!=null&&!physics)continue;
                        

                        float baseX = (float) coll.getX();
                        float baseY = (float) coll.getY();

                        // Shift from tile-local origin to tile art origin, then scale to the object size
                        baseX += -((float) tile.getWidth() / 2f);
                        baseY += -((float) tile.getHeight());

                        baseX = (baseX / wTile) * wObject;
                        baseY = (baseY / hTile) * hObject;

                        addFixtureLocal(
                                coords, map,
                                baseX, baseY,
                                wScale, hScale,
                                coll, entity, physicsDef,
                                false,
                                wFlip,
                                hFlip
                        );
                    }
                } else {
                    // TILE object without explicit collisions
                    addFixtureLocal(
                            coords, map,
                            -((float) obj.getWidth() * 0.5f),
                            -((float) obj.getHeight()),
                            1f, 1f,
                            obj, entity, physicsDef,
                            false,
                           false,
                            false
                    );
                }
            } else {
                // Regular primitive on object layer (RECTANGLE, POLYGON, ELLIPSE, etc.)
                float baseX = -((float) obj.getWidth() * 0.5f);
                float baseY = -((float) obj.getHeight() * 0.5f);

                addFixtureLocal(
                        coords, map,
                        baseX, baseY,
                        1f, 1f,
                        obj, entity, physicsDef,
                        isIso,
                        false,
                        false
                );
            }
        }

        return physicsDef;
    }


    public static class PhysicsDef {
        private final BodyDef bodyDef;
        private final List<FixtureDef> fixtureDefs = new ArrayList<>();

        public PhysicsDef(BodyDef bodyDef) {
            this.bodyDef = bodyDef;
        }

        public BodyDef getBodyDef() {
            return bodyDef;
        }

        public List<FixtureDef> getFixtureDefs() {
            return fixtureDefs;
        }
    }


    /**
     * Convert an isometric delta in world units to a screen-space delta, writing into {@code out}.
     */
    private static void isoDelta(CoordinateSystem coords, float dx, float dy, Vector2f out) {
        try (TempVars vars = TempVars.get()) {
            Vector2f s0 = vars.vect2d;
            Vector2f s1 = vars.vect2d2;
            coords.gridToWorldSpace(0f, 0f, s0);
            coords.gridToWorldSpace(dx, dy, s1);
            out.x = s1.x - s0.x;
            out.y = s1.y - s0.y;
        }
    }

    /**
     * Build a fixture for the given object, applying scaling, offsets, flips and iso transforms.
     *
     */
    private static void addFixtureLocal(
            CoordinateSystem coords,
            TiledMap map,
            float baseX, float baseY,
            float wScale, float hScale,
            TiledObjectEntity obj,
            TiledEntity entity,
            PhysicsDef def,
            boolean isIso,
            boolean flipX,
            boolean flipY
    ) {
        FixtureDef fixtureDef = new FixtureDef();


        // Density / mass 
        Object d = obj.getProperty(phy.density);
        if (d == null) obj.getProperty(phy.mass);
        if (d == null) entity.getProperty(phy.density);
        if (d == null) entity.getProperty(phy.mass);
        if (d == null) d = 1.0;
        fixtureDef.density = (float) NGEUtils.safeDouble(d);

        // Friction
        d = obj.getProperty(phy.friction);
        if (d == null) entity.getProperty(phy.friction);
        if (d == null) d = 0.5f;
        fixtureDef.friction = (float) NGEUtils.safeDouble(d);

        // Restitution
        d = obj.getProperty(phy.restitution);
        if (d == null) entity.getProperty(phy.restitution);
        if (d == null) d = 0.0f;
        fixtureDef.restitution = (float) NGEUtils.safeDouble(d);

        // Sensor flag
        d = obj.getProperty(phy.sensor);
        if (d == null) entity.getProperty(phy.sensor);
        if (d == null) d = false;
        fixtureDef.isSensor = NGEUtils.safeBool(d);

        // Link back to the source entity/object
        fixtureDef.userData = new Box2dUserData(entity, obj);

        // Effective size of the shape in object-local space
        final float w = ((float) obj.getWidth()) * wScale;
        final float h = ((float) obj.getHeight()) * hScale;

        if (isDegenerateCollisionObject(obj, w, h)) {
            return;
        }

        final float containerW;
        final float containerH;
        if (entity instanceof TiledObjectEntity) {
            containerW = (float) ((TiledObjectEntity) entity).getWidth();
            containerH = (float) ((TiledObjectEntity) entity).getHeight();
        } else if (entity instanceof TiledTileEntity) {
            containerW = map.getTileWidth();
            containerH = map.getTileHeight();
        } else {
            containerW = (float) obj.getWidth();
            containerH = (float) obj.getHeight();
        }

        final boolean parentIsTileObject = (entity instanceof TiledObjectEntity)
                && ((TiledObjectEntity) entity).getShape() == ObjectShape.TILE;

        switch (obj.getShape()) {

            case RECTANGLE: {
                float rotation = localShapeRotation(obj, entity);
                if (isIso) {
                    // Build a quad polygon by converting 4 corners via isoDelta then to physics
                    final float[][] pts = {
                            { baseX, baseY },
                            { baseX + w, baseY },
                            { baseX + w, baseY + h },
                            { baseX, baseY + h }
                    };
                    for (int i = 0; i < pts.length; i++) {
                        rotatePoint(baseX, baseY, rotation, pts[i]);
                    }
                    final float cx = (pts[0][0] + pts[1][0] + pts[2][0] + pts[3][0]) * 0.25f;
                    final float cy = (pts[0][1] + pts[1][1] + pts[2][1] + pts[3][1]) * 0.25f;

                    final Vec2[] verts = new Vec2[4];
                    for (int i = 0; i < 4; i++) {
                        float dx = pts[i][0] - cx;
                        float dy = pts[i][1] - cy;
                        verts[i] = new Vec2();
                        isoDelta(coords, dx, dy, tmpVec2f);
                        coords.worldToPhysicsSpace(tmpVec2f, verts[i]);
                    }

                    PolygonShape poly = new PolygonShape();
                    poly.set(verts, 4);
                    fixtureDef.shape = poly;
                } else {
                    Vec2[] vertices = rectangleVertices(
                            coords,
                            baseX, baseY, w, h,
                            rotation,
                            containerW, containerH,
                            parentIsTileObject,
                            flipX, flipY
                    );
                    PolygonShape poly = new PolygonShape();
                    poly.set(vertices, vertices.length);
                    fixtureDef.shape = poly;
                }
                break;
            }

            case POLYGON: {
                // Up to 8 points
                final List<Vector2f> pts = obj.getPoints();
                final int n = Math.min(pts.size(), 8);
                final List<Vec2> vertices = new ArrayList<>(n);

                if (isIso) {
                    // For isometric, convert to deltas around screen space
                    for (int i = 0; i < n; i++) {
                        Vector2f p = pts.get(i);
                        tmpVec2f.set(p.x * wScale, p.y * hScale);
                        tmpVec2f.x += baseX;
                        tmpVec2f.y += baseY;
                        rotatePoint(baseX, baseY, localShapeRotation(obj, entity), tmpVec2f);
                        isoDelta(coords, tmpVec2f.x, tmpVec2f.y, tmpVec2f);
                        Vec2 v = new Vec2();
                        coords.worldToPhysicsSpace(tmpVec2f, v);
                        vertices.add(v);
                    }
                } else {
                    // Orthogonal: apply flips relative to container, then to physics
                    for (int i = 0; i < n; i++) {
                        Vector2f p = pts.get(i);
                        float px = p.x * wScale + baseX;
                        float py = p.y * hScale + baseY;
                        rotatePoint(baseX, baseY, localShapeRotation(obj, entity), tmpVec2f.set(px, py));
                        px = tmpVec2f.x;
                        py = tmpVec2f.y;

                        if (parentIsTileObject) {
                            if (flipX) px = -px;                 // mirror around local origin
                            if (flipY) py = -containerH - py;    // mirror and shift by container height
                        } else {
                            if (flipX) px = containerW - px;     // mirror inside container bounds
                            if (flipY) py = containerH - py;
                        }

                        Vec2 v2 = new Vec2();
                        coords.worldToPhysicsSpace(tmpVec2f.set(px, py), v2);
                        vertices.add(v2);
                    }

                    // If only one axis is flipped, reverse vertex order to keep winding consistent
                    if (flipX ^ flipY) {
                        java.util.Collections.reverse(vertices);
                    }

                    PolygonShape earlyShape = new PolygonShape();
                    earlyShape.set(vertices.toArray(new Vec2[0]), vertices.size());
                    fixtureDef.shape = earlyShape;
                }

                // Intentional duplicate final assignment preserved 
                PolygonShape shape = new PolygonShape();
                shape.set(vertices.toArray(new Vec2[0]), vertices.size());
                fixtureDef.shape = shape;

                break;
            }

            case ELLIPSE: {
                // Approximate ellipse with N-gon unless it's a circle in orthogonal space
                final int seg = 8;
                final float rx = w * 0.5f;
                final float ry = h * 0.5f;

                if (Math.abs(rx - ry) < 1 && !isIso) {
                    float bx = baseX;
                    float by = baseY;

                    if (parentIsTileObject) {
                        if (flipX) bx = -(bx + w);
                        if (flipY) by = -containerH - (by + h);
                    } else {
                        if (flipX) bx = containerW - (bx + w);
                        if (flipY) by = containerH - (by + h);
                    }

                    float hx = w * 0.5f;

                    // Convert half-extents to physics units
                    coords.worldToPhysicsSpace(tmpVec2f.set(hx, hx), tmpVec2);
                    float whx = Math.abs(tmpVec2.x);

                    // Convert center to physics units
                    coords.worldToPhysicsSpace(tmpVec2f.set(bx + hx, by + hx), tmpVec2);

 

                     CircleShape circle = new CircleShape();
                    circle.m_p.set(tmpVec2.x, tmpVec2.y);
                    circle.m_radius =  whx;
                    fixtureDef.shape = circle;

                } else {
                    // Build an 8-gon approximation
                    Vec2[] vertices = new Vec2[seg];
                    if (isIso) {
                        float cx = baseX + w * 0.5f;
                        float cy = baseY + h * 0.5f;
                        for (int i = 0; i < seg; i++) {
                            float a = (float) (2 * Math.PI * i / seg);
                            float px = cx + rx * (float) Math.cos(a);
                            float py = cy + ry * (float) Math.sin(a);
                            rotatePoint(baseX, baseY, localShapeRotation(obj, entity), tmpVec2f.set(px, py));
                            px = tmpVec2f.x;
                            py = tmpVec2f.y;
                            isoDelta(coords, px - cx, py - cy, tmpVec2f);
                            vertices[i] = new Vec2();
                            coords.worldToPhysicsSpace(tmpVec2f, vertices[i]);
                        }
                    } else {
                        float cx = baseX + rx;
                        float cy = baseY + ry;

                        for (int i = 0; i < seg; i++) {
                            float a = (float) (2 * Math.PI * i / seg);
                            float px = cx + rx * (float) Math.cos(a);
                            float py = cy + ry * (float) Math.sin(a);
                            rotatePoint(baseX, baseY, localShapeRotation(obj, entity), tmpVec2f.set(px, py));
                            px = tmpVec2f.x;
                            py = tmpVec2f.y;

                            if (parentIsTileObject) {
                                if (flipX) px = -px;
                                if (flipY) py = -containerH - py;
                            } else {
                                if (flipX) px = containerW - px;
                                if (flipY) py = containerH - py;
                            }

                            vertices[i] = new Vec2();
                            coords.worldToPhysicsSpace(tmpVec2f.set(px, py), vertices[i]);
                        }

                        // Reverse order if only one axis flipped 
                        if (flipX ^ flipY) {
                            for (int i = 0, j = seg - 1; i < j; i++, j--) {
                                Vec2 t = vertices[i];
                                vertices[i] = vertices[j];
                                vertices[j] = t;
                            }
                        }

                    }

                    PolygonShape poly = new PolygonShape();
                    poly.set(vertices, seg);
                    fixtureDef.shape = poly;
                }
                break;
            }
            default: {

                float rx = w * 0.5f;
                float ry = h * 0.5f;

                coords.worldToPhysicsSpace(tmpVec2f.set(rx, ry), tmpVec2);
                float r = Math.min(Math.abs(tmpVec2.x), Math.abs(tmpVec2.y));

                float cx = baseX + rx;
                float cy = baseY + ry;

                CircleShape circle = new CircleShape();
                coords.worldToPhysicsSpace(tmpVec2f.set(cx, cy), tmpVec2);
                circle.m_p.set(tmpVec2.x, tmpVec2.y);
                circle.m_radius = r;
                fixtureDef.shape = circle;
            }
        }

        def.getFixtureDefs().add(fixtureDef);
    }

    private static Vec2[] rectangleVertices(
            CoordinateSystem coords,
            float baseX, float baseY,
            float w, float h,
            float rotation,
            float containerW, float containerH,
            boolean parentIsTileObject,
            boolean flipX,
            boolean flipY
    ) {
        float[][] pts = {
                { baseX, baseY },
                { baseX + w, baseY },
                { baseX + w, baseY + h },
                { baseX, baseY + h }
        };
        Vec2[] vertices = new Vec2[pts.length];
        for (int i = 0; i < pts.length; i++) {
            rotatePoint(baseX, baseY, rotation, pts[i]);
            float px = pts[i][0];
            float py = pts[i][1];
            if (parentIsTileObject) {
                if (flipX) px = -px;
                if (flipY) py = -containerH - py;
            } else {
                if (flipX) px = containerW - px;
                if (flipY) py = containerH - py;
            }
            Vec2 vertex = new Vec2();
            coords.worldToPhysicsSpace(tmpVec2f.set(px, py), vertex);
            vertices[i] = vertex;
        }
        if (flipX ^ flipY) {
            java.util.Collections.reverse(java.util.Arrays.asList(vertices));
        }
        return vertices;
    }

    private static float localShapeRotation(TiledObjectEntity obj, TiledEntity entity) {
        return obj == entity ? 0f : (float) obj.getRotation();
    }

    private static void rotatePoint(float originX, float originY, float rotationDegrees, Vector2f point) {
        if (Math.abs(rotationDegrees) < 1e-6f) {
            return;
        }
        float[] p = { point.x, point.y };
        rotatePoint(originX, originY, rotationDegrees, p);
        point.set(p[0], p[1]);
    }

    private static void rotatePoint(float originX, float originY, float rotationDegrees, float[] point) {
        if (Math.abs(rotationDegrees) < 1e-6f) {
            return;
        }
        double angle = Math.toRadians(rotationDegrees);
        float dx = point[0] - originX;
        float dy = point[1] - originY;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        point[0] = originX + dx * cos - dy * sin;
        point[1] = originY + dx * sin + dy * cos;
    }

    private static boolean isDegenerateCollisionObject(TiledObjectEntity obj, float w, float h) {
        if (obj.getShape() == ObjectShape.POLYGON) {
            List<Vector2f> points = obj.getPoints();
            return points == null || points.size() < 3;
        }
        return Math.abs(w) < 1e-6f || Math.abs(h) < 1e-6f;
    }

}
