/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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


import com.jme3.bullet.types.btDestructible;
import com.jme3.bullet.types.btHeightfieldTerrainShape;
import com.jme3.bullet.types.btIndexedMesh;
import com.jme3.bullet.types.btMatrix3x3;
import com.jme3.bullet.types.btQuaternion;
import com.jme3.bullet.types.btTransform;
import com.jme3.bullet.types.btTriangleMesh;
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.types.btVector3;
import com.jme3.math.FastMath;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Nice convenience methods for conversion between javax.vecmath and com.jme3.math
 * Objects, also some jme to jbullet mesh conversion.
 * @author normenhansen
 */
public class Converter {

    private Converter() {
    }

    // public static com.jme3.math.Vector3f convert(btVector3 oldVec) {
    //     com.jme3.math.Vector3f newVec = new com.jme3.math.Vector3f();
    //     convert(oldVec, newVec);
    //     return newVec;
    // }

    public static com.jme3.math.Vector3f convert(btVector3 oldVec, com.jme3.math.Vector3f newVec) {
        newVec.x = oldVec.getX();
        newVec.y = oldVec.getY();
        newVec.z = oldVec.getZ();
        return newVec;
    }

    // public static btVector3 convert(com.jme3.math.Vector3f oldVec) {
    //     btVector3 newVec = btUtils.newVector3(0,0,0);
    //     convert(oldVec, newVec);
    //     return newVec;
    // }

    public static btVector3 convert(com.jme3.math.Vector3f oldVec, btVector3 newVec) {
        newVec.setValue(oldVec.x, oldVec.y, oldVec.z);
        return newVec;
    }

    public static btQuaternion convert(com.jme3.math.Quaternion oldQuat, btQuaternion newQuat) {
        newQuat.setW(oldQuat.getW());
        newQuat.setX(oldQuat.getX());
        newQuat.setY(oldQuat.getY());
        newQuat.setZ(oldQuat.getZ());
        return newQuat;
    }

    // public static btQuaternion convert(com.jme3.math.Quaternion oldQuat) {
    //     btQuaternion newQuat = btUtils.newQuaternion(0, 0, 0, 1);
    //     convert(oldQuat, newQuat);
    //     return newQuat;
    // }

    public static com.jme3.math.Quaternion convert(btQuaternion oldQuat, com.jme3.math.Quaternion newQuat) {
        newQuat.set(oldQuat.getX(), oldQuat.getY(), oldQuat.getZ(), oldQuat.getW());
        return newQuat;
    }

    // public static com.jme3.math.Quaternion convert(btQuaternion oldQuat) {
    //     com.jme3.math.Quaternion newQuat = new com.jme3.math.Quaternion();
    //     convert(oldQuat, newQuat);
    //     return newQuat;
    // }

    public static com.jme3.math.Quaternion convert(btMatrix3x3 oldMatrix, com.jme3.math.Quaternion newQuaternion) {
        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html

        btVector3 row = oldMatrix.getRow(0);
        float m00 = row.getX();
        float m01 = row.getY();
        float m02 = row.getZ();

        row = oldMatrix.getRow(1);
        float m10 = row.getX();
        float m11 = row.getY();
        float m12 = row.getZ();

        row = oldMatrix.getRow(2);
        float m20 = row.getX();
        float m21 = row.getY();
        float m22 = row.getZ();

        float t = m00 + m11 + m22;
        float w, x, y, z;
        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            float s = FastMath.sqrt(t + 1); // |s|>=1 ...
            w = 0.5f * s;
            s = 0.5f / s; // so this division isn't bad
            x = (m21 - m12) * s;
            y = (m02 - m20) * s;
            z = (m10 - m01) * s;
        } else if ((m00 > m11) && (m00 > m22)) {
            float s = FastMath.sqrt(1.0f + m00 - m11 - m22); // |s|>=1
            x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            y = (m10 + m01) * s;
            z = (m02 + m20) * s;
            w = (m21 - m12) * s;
        } else if (m11 > m22) {
            float s = FastMath.sqrt(1.0f + m11 - m00 - m22); // |s|>=1
            y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            x = (m10 + m01) * s;
            z = (m21 + m12) * s;
            w = (m02 - m20) * s;
        } else {
            float s = FastMath.sqrt(1.0f + m22 - m00 - m11); // |s|>=1
            z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            x = (m02 + m20) * s;
            y = (m21 + m12) * s;
            w = (m10 - m01) * s;
        }
        return newQuaternion.set(x, y, z, w);
    }

    public static btMatrix3x3 convert(com.jme3.math.Quaternion oldQuaternion, btMatrix3x3 newMatrix) {
        float norm = oldQuaternion.getW() * oldQuaternion.getW() + oldQuaternion.getX() * oldQuaternion.getX() + oldQuaternion.getY() * oldQuaternion.getY()
                + oldQuaternion.getZ() * oldQuaternion.getZ();
        float s = (norm == 1f) ? 2f : (norm > 0f) ? 2f / norm : 0;

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        float xs = oldQuaternion.getX() * s;
        float ys = oldQuaternion.getY() * s;
        float zs = oldQuaternion.getZ() * s;
        float xx = oldQuaternion.getX() * xs;
        float xy = oldQuaternion.getX() * ys;
        float xz = oldQuaternion.getX() * zs;
        float xw = oldQuaternion.getW() * xs;
        float yy = oldQuaternion.getY() * ys;
        float yz = oldQuaternion.getY() * zs;
        float yw = oldQuaternion.getW() * ys;
        float zz = oldQuaternion.getZ() * zs;
        float zw = oldQuaternion.getW() * zs;

        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
        float m00 = 1 - (yy + zz);
        float m01 = (xy - zw);
        float m02 = (xz + yw);
        float m10 = (xy + zw);
        float m11 = 1 - (xx + zz);
        float m12 = (yz - xw);
        float m20 = (xz - yw);
        float m21 = (yz + xw);
        float m22 = 1 - (xx + yy);

        newMatrix.setValue(m00, m01, m02, m10, m11, m12, m20, m21, m22);

        return newMatrix;
    }

    public static com.jme3.math.Matrix3f convert(btMatrix3x3 oldMatrix) {
        com.jme3.math.Matrix3f newMatrix = new com.jme3.math.Matrix3f();
        convert(oldMatrix, newMatrix);
        return newMatrix;
    }

    public static com.jme3.math.Matrix3f convert(btMatrix3x3 oldMatrix, com.jme3.math.Matrix3f newMatrix) {
        btVector3 row = oldMatrix.getRow(0);
        float m00 = row.getX();
        float m01 = row.getY();
        float m02 = row.getZ();

        row = oldMatrix.getRow(1);
        float m10 = row.getX();
        float m11 = row.getY();
        float m12 = row.getZ();

        row = oldMatrix.getRow(2);
        float m20 = row.getX();
        float m21 = row.getY();
        float m22 = row.getZ();

        newMatrix.set(0, 0, m00);
        newMatrix.set(0, 1, m01);
        newMatrix.set(0, 2, m02);
        newMatrix.set(1, 0, m10);
        newMatrix.set(1, 1, m11);
        newMatrix.set(1, 2, m12);
        newMatrix.set(2, 0, m20);
        newMatrix.set(2, 1, m21);
        newMatrix.set(2, 2, m22);
        return newMatrix;
    }

    // public static btMatrix3x3 convert(com.jme3.math.Matrix3f oldMatrix) {
    //     btMatrix3x3 newMatrix = btUtils.newMatrix3x3();
    //     convert(oldMatrix, newMatrix);
    //     return newMatrix;
    // }

    public static btMatrix3x3 convert(com.jme3.math.Matrix3f oldMatrix, btMatrix3x3 newMatrix) {
        float m00 = oldMatrix.get(0, 0);
        float m01 = oldMatrix.get(0, 1);
        float m02 = oldMatrix.get(0, 2);
        float m10 = oldMatrix.get(1, 0);
        float m11 = oldMatrix.get(1, 1);
        float m12 = oldMatrix.get(1, 2);
        float m20 = oldMatrix.get(2, 0);
        float m21 = oldMatrix.get(2, 1);
        float m22 = oldMatrix.get(2, 2);
        newMatrix.setValue(m00, m01, m02, m10, m11, m12, m20, m21, m22);
        return newMatrix;
    }

    public static btTransform convert(com.jme3.math.Transform in, btTransform out) {
        convert(in.getTranslation(), out.getOrigin());
        convert(in.getRotation(), out.getBasis());
        return out;
    }

    public static com.jme3.math.Transform convert(btTransform in, com.jme3.math.Transform out) {
        convert(in.getOrigin(), out.getTranslation());
        convert(in.getBasis(), out.getRotation());
        return out;
    }

    // public static synchronized btTriangleMesh convert(Mesh mesh) {
    //     btTriangleMesh btMesh = btUtils.createTriangleMesh();

    //     btVector3 v0 = btUtils.newVector3(0,0,0);
    //     btVector3 v1 = btUtils.newVector3(0,0,0);
    //     btVector3 v2 = btUtils.newVector3(0, 0, 0);
    //     Vector3f tv0 = new Vector3f();
    //     Vector3f tv1 = new Vector3f();
    //     Vector3f tv2 = new Vector3f();
    //     for (int i = 0; i < mesh.getTriangleCount(); i++) {
    //         mesh.getTriangle(i, tv0, tv1, tv2);
    //         convert(tv0, v0);
    //         convert(tv1, v1);
    //         convert(tv2, v2);
    //         btMesh.addTriangle(v0, v1, v2, true);
    //     }

    //     return btMesh;
    // }


    public static class TriangulatedMesh {
        public int numTriangles;
        public int numVertices;
        public Vector3f vertices[];
        public int indices[];
        public int vertexStride = 3 * 4;
        public int triangleIndexStride = 3 * 4;

    }

    public static synchronized btTriangleMesh convert(btDestructible p, TriangulatedMesh tmesh, btTriangleMesh bmesh) {
        // System.out.println("Convert tmesh to bmesh");
        btVector3 v0 = btUtils.newVector3(p,0, 0, 0);
        btVector3 v1 = btUtils.newVector3(p,0, 0, 0);
        btVector3 v2 = btUtils.newVector3(p,0, 0, 0);
        for (int i = 0; i < tmesh.indices.length; ) {
            int i0 = tmesh.indices[i++];
            int i1 = tmesh.indices[i++];
            int i2 = tmesh.indices[i++];
            Converter.convert(tmesh.vertices[i0], v0);
            Converter.convert(tmesh.vertices[i1], v1);
            Converter.convert(tmesh.vertices[i2], v2);
            bmesh.addTriangle(v0, v1, v2, true);
        }
        btUtils.destroy(p, v0);
        btUtils.destroy(p, v1);        
        btUtils.destroy(p, v2);
        return bmesh;
    }


    public static synchronized Mesh convert(TriangulatedMesh tmesh) {
        // System.out.println("Convert tmesh to mesh");
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3,  BufferUtils.createFloatBuffer(tmesh.vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, tmesh.indices);
        mesh.updateBound();
        return mesh;
    }

    public static synchronized TriangulatedMesh convert(btDestructible p, Mesh mesh, TriangulatedMesh tmesh) {
        // System.out.println("Convert mesh to tmesh");
        tmesh.numTriangles = mesh.getTriangleCount();
        tmesh.numVertices = mesh.getVertexCount();
        tmesh.vertices = new Vector3f[tmesh.numVertices];
        tmesh.indices = new int[tmesh.numTriangles * 3];
 
        Triangle t = new Triangle();

        int vI = 0;
        for (int i = 0; i < tmesh.numTriangles; i++) {
            mesh.getTriangle(i, t);
            Vector3f v1 = t.get1();
            Vector3f v2 = t.get2();
            Vector3f v3 = t.get3();

    
            tmesh.vertices[vI + 0] = v1.clone();
            tmesh.vertices[vI + 1] = v2.clone();
            tmesh.vertices[vI + 2] = v3.clone();

            tmesh.indices[vI + 0] = vI + 0 ;
            tmesh.indices[vI + 1] = vI + 1 ;
            tmesh.indices[vI + 2] = vI + 2 ;

            vI += 3;
   
        }

        return tmesh;
  
    }



    public static Mesh convert(btHeightfieldTerrainShape heightfieldShape) {
        return null; //TODO!!
    }
}
