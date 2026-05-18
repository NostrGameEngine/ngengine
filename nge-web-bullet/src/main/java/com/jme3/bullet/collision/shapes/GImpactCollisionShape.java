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
import com.jme3.bullet.types.btGImpactMeshShape;

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
public class GImpactCollisionShape extends CollisionShape{
    protected Vector3f worldScale;

 protected btTriangleMesh bmesh;
    protected TriangulatedMesh tmesh;

    // private btVector3 tmpV;
    
    protected GImpactCollisionShape() {
    }

    /**
     * creates a collision shape from the given Mesh
     * @param mesh the Mesh to use
     */
    public GImpactCollisionShape(Mesh mesh) {
        createCollisionMesh(mesh, new Vector3f(1,1,1));
    }


    private void createCollisionMesh(Mesh mesh, Vector3f worldScale) {
        
        this.worldScale = worldScale;
     
        tmesh = new TriangulatedMesh();
        Converter.convert(this, mesh, tmesh);


        createShape(tmesh);
    }

    /**
     * creates a jme mesh from the collision shape, only needed for debugging
     *
     * @return a new Mesh
     */
    public Mesh createJmeMesh(){
        return Converter.convert(tmesh);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(worldScale, "worldScale", new Vector3f(1, 1, 1));
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
        worldScale = (Vector3f) capsule.readSavable("worldScale", new Vector3f(1, 1, 1));
       
        tmesh = new TriangulatedMesh();
        tmesh.numVertices = capsule.readInt("numVertices", 0);
        tmesh.numTriangles = capsule.readInt("numTriangles", 0);
 
        ByteBuffer triangleIndexBaseBuffer = ByteBuffer.wrap(capsule.readByteArray("triangleIndexBase", new byte[0]));
        ByteBuffer vertexBaseBuffer = ByteBuffer.wrap(capsule.readByteArray("vertexBase", new byte[0]));


        tmesh.vertices = new Vector3f[tmesh.numVertices];
        tmesh.indices = new int[tmesh.numTriangles * 3];

        for (int i = 0; i < tmesh.numVertices ; i++) {
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

        bmesh = btUtils.createTriangleMesh(this);
        Converter.convert(this, tmesh, bmesh);
       
        
        btCollisionShape cShape = btUtils.createGImpactMeshShape(this, bmesh);
        // btVector3 s = Converter.convert(worldScale);
        // cShape.setLocalScaling(s);
        // btUtils.destroy(s);

        btVector3 tmpV = btUtils.newVector3(this, 0, 0, 0);
        Converter.convert(getScale(),tmpV);
        cShape.setLocalScaling(tmpV);
        cShape.setMargin(margin);
        setCShape(cShape);

        btUtils.destroy(this, tmpV);
        ((btGImpactMeshShape) cShape).updateBound();
    }

    @Override
    public void setScale(Vector3f scale) {
        super.setScale(scale);
        ((btGImpactMeshShape) getCShape()).updateBound();
    }


    @Override
    public ShapeType getShapeType() {
        return ShapeType.CONCAVE;
    }


}
