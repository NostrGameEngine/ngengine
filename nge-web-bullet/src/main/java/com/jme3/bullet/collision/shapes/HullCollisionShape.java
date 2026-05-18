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
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.io.IOException;
import java.nio.FloatBuffer;
 
public class HullCollisionShape extends CollisionShape {

    private float[] points;
    protected HullCollisionShape() {
    }

    public HullCollisionShape(Mesh mesh) {
        this.points = getPoints(mesh);
        createShape(this.points);
    }

    public HullCollisionShape(float[] points) {
        this.points = points;
        createShape(this.points);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);

        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(points, "points", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);

        // for backwards compatability
        Mesh mesh = (Mesh) capsule.readSavable("hullMesh", null);
        if (mesh != null) {
            this.points = getPoints(mesh);
        } else {
            this.points = capsule.readFloatArray("points", null);

        }
        createShape(this.points);
    }

    private void createShape(float[] points) 
    {
        // ArrayList<btVector3> pointList = new ArrayList<btVector3>();
        // for (int i = 0; i < points.length; i += 3) {
        //     float x = points[i];
        //     float y = points[i + 1];
        //     float z = points[i + 2];
        //     btVector3 v = btUtils.newVector3(this, x, y, z);
        //     pointList.add(v);           
        // }
        btCollisionShape cShape = btUtils.createConvexHullCollisionShape(this,points);
        cShape.setLocalScaling(Converter.convert(getScale(),btUtils.newVector3(this, 0,0,0)));
        cShape.setMargin(margin);
        setCShape(cShape);
    }

    protected float[] getPoints(Mesh mesh) {
        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        vertices.rewind();
        int components = mesh.getVertexCount() * 3;
        float[] pointsArray = new float[components];
        for (int i = 0; i < components;) {
            pointsArray[i++] = vertices.get();
            pointsArray[i++] = vertices.get();
            pointsArray[i++] = vertices.get();
        }
        return pointsArray;
    }

    @Override
    public ShapeType getShapeType() {
        return ShapeType.CONVEX;
    }
    
}
