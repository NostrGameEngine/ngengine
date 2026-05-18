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

package org.ngengine.world2d.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.ngengine.runner.Runner;
import org.ngengine.world2d.TiledWorld2d;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.util.TempVars;

import io.github.jmecn.tiled.renderer.shape.Polyline;
import io.github.jmecn.tiled.renderer.shape.Rect;
import io.github.jmecn.tiled.util.CoordinateSystem;

public class Box2dDebugger {

    
    private static Map<World,Node> nodes = new HashMap<>();

    public static Node getDebugNode(World world) {
        return nodes.get(world);
    }


    public static void update(Runner mainRunner, AssetManager assetManager,  
            Collection<TiledWorld2d> tworlds, float tpf) {
        Map<World,Node> newNodes = new HashMap<>(); // TODO: in multithreading use ConcurrentHashMap
        for (TiledWorld2d tworld : tworlds) {
            CoordinateSystem coords = tworld.getCoordinateSystem();
            World world = tworld.getPhysics();             
            Node rootNode = new Node("DebugPhysicsWorld_" + System.currentTimeMillis());
            newNodes.put(world, rootNode);
            Body body = world.getBodyList();
            while (body != null) {
                try (TempVars vars = TempVars.get()) {
                    Vector2f pos = vars.vect2d;
                    Rect mesh = new Rect(10f, 10f, false);
                    Geometry geom = new Geometry("DebugPhysicsBody", mesh);
                    Material mat = new Material(assetManager, com.jme3.material.Materials.UNSHADED);
                    mat.setColor("Color", ColorRGBA.Red);
                    mat.getAdditionalRenderState().setDepthTest(false);
                    mat.getAdditionalRenderState().setDepthWrite(false);
                    geom.setMaterial(mat);

                    Node debugNode = new Node("BodyNode"+body.hashCode());
                    debugNode.attachChild(geom);
                    debugNode.setQueueBucket(Bucket.Translucent);
                    rootNode.attachChild(debugNode);

                    Fixture fixture = body.getFixtureList();
                    while (fixture != null) {
                        Shape shape = fixture.getShape();
                        Geometry geometry = null;
                        if (shape instanceof PolygonShape) {
                            PolygonShape poly = (PolygonShape) shape;
                            int count = poly.getVertexCount();
                            List<Vector2f> vertices = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                Vec2 vec = poly.getVertex(i);
                                coords.physicsToWorldSpace(vec, pos);
                                Vector2f v = new Vector2f(pos.x, pos.y);
                                vertices.add(v);
                            }
                            Polyline m = new Polyline(vertices, true);
                            geometry = new Geometry("Polygon", m);

                        } else if (shape instanceof CircleShape) {
                            CircleShape cir = (CircleShape) shape;
                            Vec2 vec = new Vec2(cir.m_radius, cir.m_radius);
                            coords.physicsToWorldSpace(vec, pos);

                            float radius = Math.max(pos.x, pos.y);
                            vec.set(cir.m_p.x, cir.m_p.y);
                            coords.physicsToWorldSpace(vec, pos);

                            int segments = 16;
                            List<Vector2f> vertices = new ArrayList<>(segments);
                            for (int i = 0; i < segments; i++) {
                                float angle = ((float) i / (float) segments) * FastMath.TWO_PI;
                                float x = pos.x + FastMath.cos(angle) * radius;
                                float y = pos.y + FastMath.sin(angle) * radius;
                                Vector2f v = new Vector2f(x, y);
                                vertices.add(v);
                            }
                            Polyline m = new Polyline(vertices, true);
                            geometry = new Geometry("Circle", m);
                        }

                        if (geometry != null) {
                            Material material = new Material(assetManager,
                                    com.jme3.material.Materials.UNSHADED);
                            if (fixture.isSensor()) {
                                material.setColor("Color", ColorRGBA.Magenta);
                            } else {
                                if (body.getType() == BodyType.STATIC) {
                                    material.setColor("Color",
                                            body.isAwake() ? ColorRGBA.White : ColorRGBA.Gray);
                                } else {
                                    material.setColor("Color",
                                            body.isAwake() ? ColorRGBA.Yellow : ColorRGBA.Orange);
                                }
                            }
                            material.getAdditionalRenderState().setDepthTest(false);
                            material.getAdditionalRenderState().setDepthWrite(false);
                            geometry.setMaterial(material);
                            debugNode.attachChild(geometry);
                        }
                        fixture = fixture.getNext();
                    }
          
 
                    coords.physicsToWorldSpace(body.getPosition(), pos);
                    debugNode.setLocalTranslation(pos.x, 0, pos.y);

                    Quaternion rotation = debugNode.getLocalRotation();
                    float angle = body.getAngle();
                    rotation.fromAngleNormalAxis(-angle, Vector3f.UNIT_Y);
                    debugNode.setLocalRotation(rotation);
                }

                body = body.getNext();
            }


            

        }

        // TODO: if multithreading is implemented, this should run in runner
        nodes.clear();
        nodes.putAll(newNodes);

        for(Node node: nodes.values()) {
          node.updateLogicalState(tpf);
            node.updateGeometricState();
        }

    }

}
