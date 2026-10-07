/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.util.mikktspace;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.MorphTarget;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 *
 * @author Nehon
 */
public class MikkTSpaceImpl implements MikkTSpaceContext {

    Mesh mesh;
    final private IndexBuffer index;
    private final float[] cornerTangents;
    private final float signScale;

    public MikkTSpaceImpl(Mesh mesh) {
        this(mesh, MikktspaceTangentGenerator.TangentConvention.GLTF);
    }

    public MikkTSpaceImpl(Mesh mesh, MikktspaceTangentGenerator.TangentConvention convention) {
        java.util.Objects.requireNonNull(convention, "convention");
        signScale = convention == MikktspaceTangentGenerator.TangentConvention.GLTF ? -1f : 1f;
        this.mesh = mesh;

        // If the mesh lacks indices, generate a virtual index buffer.
        this.index = mesh.getIndicesAsList();
        for (VertexBuffer buffer : mesh.getBufferList()) {
            if (buffer.getStride() != 0 || buffer.getOffset() != 0) {
                throw new IllegalArgumentException("MikkTSpace requires deinterleaved vertex buffers");
            }
        }
        cornerTangents = new float[index.size() * 4];

        //replacing any existing tangent buffer, if you came here you want them new.
        mesh.clearBuffer(VertexBuffer.Type.Tangent);
        FloatBuffer fb = BufferUtils.createFloatBuffer(mesh.getVertexCount() * 4);
        mesh.setBuffer(VertexBuffer.Type.Tangent, 4, fb);
    }

    @Override
    public int getNumFaces() {
        return mesh.getTriangleCount();        
    }

    @Override
    public int getNumVerticesOfFace(int face) {
        return 3;
    }

    @Override
    public void getPosition(float[] posOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer position = mesh.getBuffer(VertexBuffer.Type.Position);
        FloatBuffer pos = (FloatBuffer) position.getData();
        posOut[0] = pos.get(vertIndex * 3);
        posOut[1] = pos.get(vertIndex * 3 + 1);
        posOut[2] = pos.get(vertIndex * 3 + 2);
    }

    @Override
    public void getNormal(float[] normOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer normal = mesh.getBuffer(VertexBuffer.Type.Normal);
        FloatBuffer norm = (FloatBuffer) normal.getData();
        normOut[0] = norm.get(vertIndex * 3);
        normOut[1] = norm.get(vertIndex * 3 + 1);
        normOut[2] = norm.get(vertIndex * 3 + 2);
    }

    @Override
    public void getTexCoord(float[] texOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer texCoord = mesh.getBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer tex = (FloatBuffer) texCoord.getData();
        texOut[0] = tex.get(vertIndex * texCoord.getNumComponents());
        texOut[1] = tex.get(vertIndex * texCoord.getNumComponents() + 1);
    }

    @Override
    public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
        sign *= signScale;
        int vertIndex = getIndex(face, vert);
        int corner = (face * 3 + vert) * 4;
        System.arraycopy(tangent, 0, cornerTangents, corner, 3);
        cornerTangents[corner + 3] = sign;
        VertexBuffer tangentBuffer = mesh.getBuffer(VertexBuffer.Type.Tangent);
        FloatBuffer tan = (FloatBuffer) tangentBuffer.getData();
        
        tan.position(vertIndex * 4);
        tan.put(tangent);
        tan.put(sign);
        
        tan.rewind();
        tangentBuffer.setUpdateNeeded();
    }

    @Override
    public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
        //Do nothing
    }

    private int getIndex(int face, int vert) {
        int vertIndex = index.get(face * 3 + vert);
        return vertIndex;
    }

    void splitTangentSeams() {
        // Mikk returns per-corner data. A mirrored UV seam cannot share one tangent.
        int originalCount = mesh.getVertexCount();
        int capacity = originalCount + index.size();
        int[] sourceVertices = new int[capacity];
        int[] next = new int[capacity];
        Arrays.fill(next, -1);
        float[] tangents = new float[capacity * 4];
        boolean[] assigned = new boolean[originalCount];
        int[] indices = new int[index.size()];
        int count = originalCount;
        for (int i = 0; i < originalCount; i++) {
            sourceVertices[i] = i;
        }
        for (int corner = 0; corner < indices.length; corner++) {
            int source = index.get(corner);
            int vertex = source;
            if (assigned[source]) {
                while (true) {
                    int a = vertex * 4;
                    int b = corner * 4;
                    if (tangents[a] == cornerTangents[b] && tangents[a + 1] == cornerTangents[b + 1]
                            && tangents[a + 2] == cornerTangents[b + 2]
                            && tangents[a + 3] == cornerTangents[b + 3]) {
                        break;
                    }
                    if (next[vertex] == -1) {
                        next[vertex] = count;
                        sourceVertices[count] = source;
                        vertex = count++;
                        break;
                    }
                    vertex = next[vertex];
                }
            }
            assigned[source] = true;
            System.arraycopy(cornerTangents, corner * 4, tangents, vertex * 4, 4);
            indices[corner] = vertex;
        }
        if (count > originalCount) {
            for (VertexBuffer old : mesh.getBufferList().getArray()) {
                if (old.getBufferType() == VertexBuffer.Type.Index
                        || old.getBufferType() == VertexBuffer.Type.Tangent
                        || old.isInstanced() || old.getData() == null) {
                    continue;
                }
                VertexBuffer replacement = old.clone();
                replacement.updateData(VertexBuffer.createBuffer(old.getFormat(), old.getNumComponents(), count));
                for (int i = 0; i < count; i++) {
                    old.copyElement(sourceVertices[i], replacement, i);
                }
                if (old.getBufferType() == VertexBuffer.Type.Custom) {
                    mesh.clearBuffer(old.getShaderAttributeName());
                } else {
                    mesh.clearBuffer(old.getBufferType());
                }
                mesh.setBuffer(replacement);
            }
            for (MorphTarget morph : mesh.getMorphTargets()) {
                for (VertexBuffer.Type type : morph.getBuffers().keySet()) {
                    FloatBuffer old = morph.getBuffer(type);
                    int components = old.limit() / originalCount;
                    FloatBuffer replacement = BufferUtils.createFloatBuffer(count * components);
                    for (int i = 0; i < count; i++) {
                        for (int c = 0; c < components; c++) {
                            replacement.put(old.get(sourceVertices[i] * components + c));
                        }
                    }
                    replacement.flip();
                    morph.setBuffer(type, replacement);
                }
            }
            mesh.setMode(Mesh.Mode.Triangles);
            mesh.clearBuffer(VertexBuffer.Type.Index);
            IndexBuffer remapped = IndexBuffer.createIndexBuffer(count, indices.length);
            for (int i = 0; i < indices.length; i++) {
                remapped.put(i, indices[i]);
            }
            mesh.setBuffer(VertexBuffer.Type.Index, 3, remapped.getFormat(), remapped.getBuffer());
        }
        mesh.clearBuffer(VertexBuffer.Type.Tangent);
        mesh.setBuffer(VertexBuffer.Type.Tangent, 4,
                BufferUtils.createFloatBuffer(Arrays.copyOf(tangents, count * 4)));
        mesh.updateCounts();
    }

}
