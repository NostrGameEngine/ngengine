/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.animation;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.math.Matrix4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinningWeightTest {
    @Test
    void everyWeightSlotCanAnimateAndUnweightedVerticesStayInBindPose() throws Exception {
        Object[] controls = {
            new SkinningControl(new Armature(new Joint[]{new Joint("root")})),
            new SkeletonControl(new Skeleton(new Bone[]{new Bone("root")}))
        };
        Matrix4f transform = new Matrix4f();
        transform.m03 = 5f;
        for (Object control : controls) {
            for (boolean tangents : new boolean[]{false, true}) {
                Mesh mesh = new Mesh();
                mesh.setMode(Mesh.Mode.Points);
                float[] positions = new float[15];
                float[] normals = new float[15];
                float[] tangentData = new float[20];
                float[] weights = new float[20];
                for (int vertex = 0; vertex < 5; vertex++) {
                    positions[vertex * 3] = 1f;
                    positions[vertex * 3 + 1] = 2f;
                    positions[vertex * 3 + 2] = 3f;
                    normals[vertex * 3 + 2] = 1f;
                    tangentData[vertex * 4] = 1f;
                    tangentData[vertex * 4 + 3] = -1f;
                    if (vertex < 4) weights[vertex * 4 + vertex] = 1f;
                }
                mesh.setBuffer(Type.Position, 3, positions);
                mesh.setBuffer(Type.Normal, 3, normals);
                if (tangents) mesh.setBuffer(Type.Tangent, 4, tangentData);
                mesh.setBuffer(Type.BoneIndex, 4, new byte[20]);
                mesh.setBuffer(Type.BoneWeight, 4, FloatBuffer.wrap(weights));
                mesh.setMaxNumWeights(4);
                Method apply = control.getClass().getDeclaredMethod(control instanceof SkinningControl
                        ? "applySoftwareSkinning" : "softwareSkinUpdate", Mesh.class, Matrix4f[].class);
                apply.setAccessible(true);
                apply.invoke(control, mesh, new Matrix4f[]{transform});
                for (int vertex = 0; vertex < 5; vertex++) {
                    assertEquals(vertex < 4 ? 6f : 1f, mesh.getFloatBuffer(Type.Position).get(vertex * 3),
                            control.getClass().getSimpleName() + ", tangents=" + tangents + ", vertex=" + vertex);
                    assertEquals(2f, mesh.getFloatBuffer(Type.Position).get(vertex * 3 + 1));
                    assertEquals(3f, mesh.getFloatBuffer(Type.Position).get(vertex * 3 + 2));
                    assertEquals(1f, mesh.getFloatBuffer(Type.Normal).get(vertex * 3 + 2));
                    if (tangents) assertEquals(-1f, mesh.getFloatBuffer(Type.Tangent).get(vertex * 4 + 3));
                }
            }
        }
    }
}
