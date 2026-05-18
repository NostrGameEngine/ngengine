/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.util;


import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape.ShapeType;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.types.btConcaveShape;
import com.jme3.bullet.types.btConvexShape;
import com.jme3.bullet.types.btDestructibleImpl;
import com.jme3.bullet.types.btShapeHull;
import com.jme3.bullet.types.btTriangleCallback;
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.types.btVector3;
import com.jme3.bullet.types.btVector3Array;
import com.jme3.math.Matrix3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author CJ Hare, normenhansen
 */
public class DebugShapeFactory extends btDestructibleImpl {
    private static final Logger logger = Logger.getLogger(DebugShapeFactory.class.getName());
    /** The maximum corner for the aabb used for triangles to include in ConcaveShape processing.*/
    private static final btVector3 aabbMax = btUtils.newVector3(null,1e30f, 1e30f, 1e30f);
    /** The minimum corner for the aabb used for triangles to include in ConcaveShape processing.*/
    private static final btVector3 aabbMin = btUtils.newVector3(null,-1e30f, -1e30f, -1e30f);

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private DebugShapeFactory() {
    }

    /**
     * Creates a debug shape from the given collision shape. This is mostly used internally.<br>
     * To attach a debug shape to a physics object, call <code>attachDebugShape(AssetManager manager);</code> on it.
     *
     * @param collisionShape the CollisionShape to use or null
     * @return a new Spatial or null
     */
    public static Spatial getDebugShape(CollisionShape collisionShape) {
        if (collisionShape == null) {
            return null;
        }
        Spatial debugShape;
        if (collisionShape instanceof CompoundCollisionShape) {
            CompoundCollisionShape shape = (CompoundCollisionShape) collisionShape;
            List<ChildCollisionShape> children = shape.getChildren();
            Node node = new Node("DebugShapeNode");
            for (Iterator<ChildCollisionShape> it = children.iterator(); it.hasNext();) {
                ChildCollisionShape childCollisionShape = it.next();
                CollisionShape ccollisionShape = childCollisionShape.shape;
                Geometry geometry = createDebugShape(ccollisionShape);

                // apply translation
                geometry.setLocalTranslation(childCollisionShape.location);

                // apply rotation
                TempVars vars = TempVars.get();

                Matrix3f tempRot = vars.tempMat3;

                tempRot.set(geometry.getLocalRotation());
                childCollisionShape.rotation.mult(tempRot, tempRot);
                geometry.setLocalRotation(tempRot);

                vars.release();

                node.attachChild(geometry);
            }
            debugShape = node;
        } else {
            debugShape = createDebugShape(collisionShape);
        }
        if (debugShape == null) {
            return null;
        }
        debugShape.updateGeometricState();
        return debugShape;
    }

    private static Geometry createDebugShape(CollisionShape shape) {
        Geometry geom = new Geometry();
        geom.setMesh(DebugShapeFactory.getDebugMesh(shape));
//        geom.setLocalScale(shape.getScale());
        geom.updateModelBound();
        return geom;
    }

    public static Mesh getDebugMesh(CollisionShape shape) {
        Mesh mesh = null;
        ShapeType type = shape.getShapeType();
        if (type == ShapeType.CONVEX) {
            mesh = new Mesh();
            mesh.setBuffer(Type.Position, 3, getVertices((btConvexShape) shape.getCShape()));
            mesh.getFloatBuffer(Type.Position).clear();
        } else if (type == ShapeType.CONCAVE) {
            mesh = new Mesh();
            mesh.setBuffer(Type.Position, 3, getVertices((btConcaveShape) shape.getCShape()));
            mesh.getFloatBuffer(Type.Position).clear();
        }
        mesh = convertToLines(mesh);
        return mesh;
    }

    public static Mesh convertToLines(Mesh triangleMesh) {
        if (triangleMesh.getMode() != Mode.Triangles) {
            logger.warning( "Cannot convert mesh to lines, it is not triangles");
            return triangleMesh;    
        }

        VertexBuffer vb = triangleMesh.getBuffer(Type.Position);
     
        int numIndices = vb.getNumElements();
        int[] linesIndices = new int[numIndices / 3 * 6];
        int linesIndicesIndex = 0;
        for (int i = 0; i < numIndices; i += 3) {
            int a = i;
            int b = i + 1;
            int c = i + 2;

            linesIndices[linesIndicesIndex++] = a;
            linesIndices[linesIndicesIndex++] = b;
            linesIndices[linesIndicesIndex++] = b;
            linesIndices[linesIndicesIndex++] = c;
            linesIndices[linesIndicesIndex++] = c;
            linesIndices[linesIndicesIndex++] = a;

        }

        triangleMesh.setBuffer(Type.Index, 2, BufferUtils.createIntBuffer(linesIndices));
        triangleMesh.setMode(Mode.Lines);
        triangleMesh.updateBound();

        return triangleMesh;
    }

    /**
     *  Constructs the buffer for the vertices of the concave shape.
     *
     * @param concaveShape the shape to get the vertices for / from.
     * @return the shape as stored by the given broadphase rigid body.
     */
    private static FloatBuffer getVertices(btConcaveShape concaveShape) {
        // Create the call back that'll create the vertex buffer
        btTriangleCallback btcb = btUtils.createTriangleCallback(null);
        concaveShape.processAllTriangles(btcb, aabbMin, aabbMax);
        btVector3Array va = btcb.getTriangles();
        int nfloats = va.size() * 3;
        
        FloatBuffer fb = BufferUtils.createFloatBuffer(nfloats);
        for (int i = 0; i < va.size(); i++) {
            btVector3 v = va.at(i);
            fb.put(v.getX());
            fb.put(v.getY());
            fb.put(v.getZ());
        }

        btUtils.destroy(null, btcb);
        
        return fb;
    }

    /**
     *  Processes the given convex shape to retrieve a correctly ordered FloatBuffer to
     *  construct the shape from with a TriMesh.
     *
     * @param convexShape the shape to retrieve the vertices from.
     * @return the vertices as a FloatBuffer, ordered as Triangles.
     */
    private static FloatBuffer getVertices(btConvexShape convexShape) {
        // Check there is a hull shape to render
        // create a hull approximation
        btShapeHull hull =btUtils.createShapeHull(null,convexShape);
        float margin = convexShape.getMargin();
        hull.buildHull(margin);
        

        // Assert we actually have a shape to render
        // assert hull.numTriangles() > 0 : "Expecting the Hull shape to have triangles";
        int numberOfTriangles = hull.numTriangles();

        // The number of bytes needed is: (floats in a vertex) * (vertices in a triangle) * (# of triangles) * (size of float in bytes)
        final int numberOfFloats = 3 * 3 * numberOfTriangles;
        FloatBuffer vertices = BufferUtils.createFloatBuffer(numberOfFloats); 

        // Force the limit, set the cap - the largest number of floats we will use the buffer for
        vertices.limit(numberOfFloats);

     
        btVector3 vertexA = btUtils.newVector3(null, 0, 0, 0);
        btVector3 vertexB = btUtils.newVector3(null, 0, 0, 0);
        btVector3 vertexC = btUtils.newVector3(null, 0, 0, 0);
        int indexA = 0;
        int indexB = 0;
        int indexC = 0;

        int index = 0;

        for (int i = 0; i < numberOfTriangles; i++) {
            // Grab the data for this triangle from the hull
            indexA = hull.indexAt(index++);
            indexB = hull.indexAt(index++);
            indexC = hull.indexAt(index++);

            vertexA = hull.vertexAt(indexA);
            vertexB = hull.vertexAt(indexB);
            vertexC = hull.vertexAt(indexC);

            // Put the vertices into the vertex buffer
            vertices.put(vertexA.getX()).put(vertexA.getY()).put(vertexA.getZ());
            vertices.put(vertexB.getX()).put(vertexB.getY()).put(vertexB.getZ());
            vertices.put(vertexC.getX()).put(vertexC.getY()).put(vertexC.getZ());
        }
        
        btUtils.destroy(null, hull);
        btUtils.destroy(null, vertexA);
        btUtils.destroy(null, vertexB);
        btUtils.destroy(null, vertexC);

        

        vertices.clear();
        return vertices;
    }
}


