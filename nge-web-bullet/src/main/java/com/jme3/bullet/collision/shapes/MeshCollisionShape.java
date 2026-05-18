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
package com.jme3.bullet.collision.shapes;


import com.jme3.bullet.types.btCollisionShape;

import com.jme3.bullet.types.btTriangleMesh;
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.types.btVector3;
import com.jme3.bullet.util.Converter;
import com.jme3.bullet.util.Converter.TriangulatedMesh;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.util.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Basic mesh collision shape
 * @author normenhansen
 */
public class MeshCollisionShape extends CollisionShape {

    
    protected btTriangleMesh bmesh;
    protected TriangulatedMesh tmesh;

    // protected btIndexedMesh bulletMesh;

    protected MeshCollisionShape() {
    }

    /** 
     * Creates a collision shape from the given TriMesh
     *
     * @param mesh
     *            the TriMesh to use
     */
    public MeshCollisionShape(Mesh mesh) {
        this(mesh, false);
    }

    /**
     * API compatibility with native bullet.
     *
     * @param mesh the TriMesh to use
     * @param dummy Unused
     */
    public MeshCollisionShape(Mesh mesh, boolean dummy) {
        createCollisionMesh(mesh, new Vector3f(1, 1, 1));
    }

    private void createCollisionMesh(Mesh mesh, Vector3f worldScale) {
 
        
        this.scale = worldScale;

        tmesh = new TriangulatedMesh();
        Converter.convert(this,mesh, tmesh);


        createShape(tmesh);
    }

    /**
     * creates a jme mesh from the collision shape, only needed for debugging
     *
     * @return a new Mesh
     */
    public Mesh createJmeMesh() {
        return Converter.convert(tmesh);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(tmesh.numVertices, "numVertices", 0);
        capsule.write(tmesh.numTriangles, "numTriangles", 0);
        capsule.write(tmesh.vertexStride, "vertexStride", 0);
        capsule.write(tmesh.triangleIndexStride, "triangleIndexStride", 0);

        ByteBuffer triangleIndexBaseBuffer = BufferUtils.createByteBuffer(tmesh.numTriangles * 3 * 4);
        ByteBuffer vertexBaseBuffer = BufferUtils.createByteBuffer(tmesh.numVertices * 3 * 4);
        
        for (int i = 0; i < tmesh.numVertices ; i++) {
            triangleIndexBaseBuffer.putFloat(tmesh.vertices[i].x);
            triangleIndexBaseBuffer.putFloat(tmesh.vertices[i].y);
            triangleIndexBaseBuffer.putFloat(tmesh.vertices[i].z);
        }

        for (int i = 0; i < tmesh.numTriangles * 3; i++) {
            vertexBaseBuffer.putInt(tmesh.indices[i]);
        }


        capsule.write(triangleIndexBaseBuffer.array(), "triangleIndexBase", new byte[0]);
        capsule.write(vertexBaseBuffer.array(), "vertexBase", new byte[0]);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        tmesh = new TriangulatedMesh();
        tmesh.numVertices = capsule.readInt("numVertices", 0);
        tmesh.numTriangles = capsule.readInt("numTriangles", 0);
        // vertexStride = capsule.readInt("vertexStride", 0);
        // triangleIndexStride = capsule.readInt("triangleIndexStride", 0);

        ByteBuffer triangleIndexBaseBuffer = ByteBuffer.wrap(capsule.readByteArray("triangleIndexBase", new byte[0]));
        ByteBuffer vertexBaseBuffer = ByteBuffer.wrap(capsule.readByteArray("vertexBase", new byte[0]));

        // triangleIndexBase = btUtils.allocFromByteArrayI32(this,triangleIndexBaseBuffer, numTriangles * 3 * 4);
        // vertexBase = btUtils.allocFromByteArrayF32(this,vertexBaseBuffer, numVertices * 3 * 4);

        tmesh.vertices = new Vector3f[tmesh.numVertices ];
        tmesh.indices = new int[tmesh.numTriangles * 3];

        for (int i = 0; i < tmesh.numVertices * 3; i++) {
            tmesh.vertices[i] = new Vector3f(
                vertexBaseBuffer.getFloat(),
                vertexBaseBuffer.getFloat(),
                vertexBaseBuffer.getFloat()
            );
        }

        for (int i = 0; i < tmesh.numTriangles * 3; i++) {
            tmesh.indices[i] = triangleIndexBaseBuffer.getInt();
        }

        createShape(tmesh);
    }

 
    private void createShape(TriangulatedMesh tmesh) {
        if (bmesh != null) {
            btUtils.destroy(this, bmesh);          
            bmesh = null;
        }

        // if (m != null) {
        //     bulletMesh = m;
        // } else {
        //     bulletMesh = btUtils.createIndexedMesh(this);
        //     bulletMesh.setNumVertices(numVertices);
        //     bulletMesh.setNumTriangles(numTriangles);
        //     bulletMesh.setVertexStride(vertexStride);
        //     bulletMesh.setTriangleIndexStride(triangleIndexStride);
        //     bulletMesh.setTriangleIndexBase(triangleIndexBase);
        //     bulletMesh.setVertexBase(vertexBase);
        // }

        // if (tmpTIVA != null) {
        //     btUtils.destroy(this, tmpTIVA);
        //     tmpTIVA = null;
        // }

        // btTriangleIndexVertexArray tmpTIVA = btUtils.createTriangleIndexVertexArray(this, numTriangles, triangleIndexBase, triangleIndexStride, numVertices, vertexBase, vertexStride);
        bmesh = btUtils.createTriangleMesh(this);
        Converter.convert(this,tmesh, bmesh);
        
        btCollisionShape cShape = btUtils.createBvhTriangleMeshShape(this, bmesh, true, true);

        btVector3 s = Converter.convert(getScale(),btUtils.newVector3(this,0,0,0));
        cShape.setLocalScaling(s);
        cShape.setMargin(margin);

        setCShape(cShape);

        
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.CONCAVE;
    }

}

