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
package org.ngengine.world2d.util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;

/**
 * Converts line primitives on the world2d XZ plane to portable triangle
 * strokes. This avoids native wide-line support, which is not guaranteed by
 * OpenGL ES 3 or ANGLE implementations.
 */
public final class LineMeshTriangulator {
    private static final float EPSILON = 0.000001f;
    private static final float MITER_LIMIT = 4f;

    private LineMeshTriangulator() {
    }

    /**
     * Converts a {@link Mesh.Mode#Lines}, {@link Mesh.Mode#LineStrip}, or
     * {@link Mesh.Mode#LineLoop} mesh into a new triangle mesh.
     *
     * <p>The source mesh is left unchanged. Positions are interpreted on the
     * XZ plane and their original Y coordinate is preserved. Line-strip joins
     * use bounded miters, while independent line segments become quads.</p>
     *
     * @param lineMesh source line mesh
     * @param strokeWidth full stroke width in mesh units
     * @return a new mesh using {@link Mesh.Mode#Triangles}
     */
    public static Mesh triangulate(Mesh lineMesh, float strokeWidth) {
        if (lineMesh == null) {
            throw new IllegalArgumentException("lineMesh is required");
        }
        if (!Float.isFinite(strokeWidth) || strokeWidth <= 0f) {
            throw new IllegalArgumentException("strokeWidth must be finite and greater than 0");
        }
        Mesh.Mode mode = lineMesh.getMode();
        if (mode != Mesh.Mode.Lines && mode != Mesh.Mode.LineStrip && mode != Mesh.Mode.LineLoop) {
            throw new IllegalArgumentException("Expected Lines, LineStrip, or LineLoop mesh, got " + mode);
        }

        FloatBuffer positionBuffer = lineMesh.getFloatBuffer(VertexBuffer.Type.Position);
        VertexBuffer positions = lineMesh.getBuffer(VertexBuffer.Type.Position);
        if (positionBuffer == null || positions.getNumComponents() < 3) {
            throw new IllegalArgumentException("Line mesh requires a 3-component position buffer");
        }

        int[] order = readOrder(lineMesh);
        List<Vector3f> outputPositions = new ArrayList<>();
        List<Vector3f> outputNormals = new ArrayList<>();
        List<Vector2f> outputTexCoords = new ArrayList<>();
        List<Integer> outputIndices = new ArrayList<>();

        if (mode == Mesh.Mode.Lines) {
            if ((order.length & 1) != 0) {
                throw new IllegalArgumentException("Lines mesh requires an even number of indices");
            }
            for (int i = 0; i < order.length; i += 2) {
                appendStroke(
                    List.of(readPosition(positionBuffer, positions.getNumComponents(), order[i]),
                            readPosition(positionBuffer, positions.getNumComponents(), order[i + 1])),
                    false,
                    strokeWidth,
                    outputPositions,
                    outputNormals,
                    outputTexCoords,
                    outputIndices
                );
            }
        } else {
            List<Vector3f> path = new ArrayList<>(order.length);
            for (int index : order) {
                path.add(readPosition(positionBuffer, positions.getNumComponents(), index));
            }
            boolean closePath = mode == Mesh.Mode.LineLoop;
            if (!closePath && path.size() > 2 && samePoint(path.get(0), path.get(path.size() - 1))) {
                closePath = true;
            }
            appendStroke(path, closePath, strokeWidth, outputPositions, outputNormals, outputTexCoords, outputIndices);
        }

        Mesh result = new Mesh();
        result.setMode(Mesh.Mode.Triangles);
        result.setBuffer(VertexBuffer.Type.Position, 3,
            BufferUtils.createFloatBuffer(outputPositions.toArray(Vector3f[]::new)));
        result.setBuffer(VertexBuffer.Type.Normal, 3,
            BufferUtils.createFloatBuffer(outputNormals.toArray(Vector3f[]::new)));
        result.setBuffer(VertexBuffer.Type.TexCoord, 2,
            BufferUtils.createFloatBuffer(outputTexCoords.toArray(Vector2f[]::new)));
        int[] indices = new int[outputIndices.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = outputIndices.get(i);
        }
        result.setBuffer(VertexBuffer.Type.Index, 3, indices);
        result.updateBound();
        result.updateCounts();
        result.setStatic();
        return result;
    }

    private static int[] readOrder(Mesh mesh) {
        IndexBuffer indices = mesh.getIndexBuffer();
        int count = indices != null ? indices.size() : mesh.getVertexCount();
        if (count <= 0) {
            throw new IllegalArgumentException("Line mesh has no vertices");
        }
        int[] order = new int[count];
        for (int i = 0; i < count; i++) {
            order[i] = indices != null ? indices.get(i) : i;
        }
        return order;
    }

    private static Vector3f readPosition(FloatBuffer positions, int components, int vertexIndex) {
        int offset = vertexIndex * components;
        if (vertexIndex < 0 || offset + 2 >= positions.limit()) {
            throw new IllegalArgumentException("Line mesh index is outside the position buffer: " + vertexIndex);
        }
        return new Vector3f(positions.get(offset), positions.get(offset + 1), positions.get(offset + 2));
    }

    private static void appendStroke(
            List<Vector3f> rawPath,
            boolean closePath,
            float strokeWidth,
            List<Vector3f> outputPositions,
            List<Vector3f> outputNormals,
            List<Vector2f> outputTexCoords,
            List<Integer> outputIndices) {
        List<Vector3f> path = sanitize(rawPath, closePath);
        int pointCount = path.size();
        if (pointCount < 2 || closePath && pointCount < 3) {
            throw new IllegalArgumentException("A stroke requires at least 2 distinct points, or 3 when closed");
        }

        int segmentCount = closePath ? pointCount : pointCount - 1;
        Vector2f[] segmentNormals = new Vector2f[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            Vector3f a = path.get(i);
            Vector3f b = path.get((i + 1) % pointCount);
            float dx = b.x - a.x;
            float dz = b.z - a.z;
            float inverseLength = 1f / (float) Math.sqrt(dx * dx + dz * dz);
            segmentNormals[i] = new Vector2f(-dz * inverseLength, dx * inverseLength);
        }

        int baseVertex = outputPositions.size();
        float halfWidth = strokeWidth * 0.5f;
        for (int i = 0; i < pointCount; i++) {
            Vector2f offset = joinOffset(segmentNormals, i, pointCount, closePath, halfWidth);
            Vector3f point = path.get(i);
            outputPositions.add(new Vector3f(point.x + offset.x, point.y, point.z + offset.y));
            outputPositions.add(new Vector3f(point.x - offset.x, point.y, point.z - offset.y));
            outputNormals.add(Vector3f.UNIT_Y.clone());
            outputNormals.add(Vector3f.UNIT_Y.clone());
            float along = pointCount > 1 ? (float) i / (pointCount - 1) : 0f;
            outputTexCoords.add(new Vector2f(along, 0f));
            outputTexCoords.add(new Vector2f(along, 1f));
        }

        for (int i = 0; i < segmentCount; i++) {
            int next = (i + 1) % pointCount;
            int left = baseVertex + i * 2;
            int right = left + 1;
            int nextLeft = baseVertex + next * 2;
            int nextRight = nextLeft + 1;
            outputIndices.add(left);
            outputIndices.add(nextLeft);
            outputIndices.add(right);
            outputIndices.add(nextLeft);
            outputIndices.add(nextRight);
            outputIndices.add(right);
        }
    }

    private static List<Vector3f> sanitize(List<Vector3f> points, boolean closePath) {
        List<Vector3f> sanitized = new ArrayList<>(points.size());
        for (Vector3f point : points) {
            if (point == null) {
                continue;
            }
            if (sanitized.isEmpty() || !samePoint(sanitized.get(sanitized.size() - 1), point)) {
                sanitized.add(point.clone());
            }
        }
        if (closePath && sanitized.size() > 1 && samePoint(sanitized.get(0), sanitized.get(sanitized.size() - 1))) {
            sanitized.remove(sanitized.size() - 1);
        }
        return sanitized;
    }

    private static boolean samePoint(Vector3f a, Vector3f b) {
        float dx = a.x - b.x;
        float dz = a.z - b.z;
        return dx * dx + dz * dz <= EPSILON * EPSILON;
    }

    private static Vector2f joinOffset(Vector2f[] segmentNormals, int pointIndex, int pointCount,
            boolean closePath, float halfWidth) {
        if (!closePath && pointIndex == 0) {
            return segmentNormals[0].mult(halfWidth);
        }
        if (!closePath && pointIndex == pointCount - 1) {
            return segmentNormals[segmentNormals.length - 1].mult(halfWidth);
        }

        int previousSegment = (pointIndex - 1 + segmentNormals.length) % segmentNormals.length;
        int nextSegment = pointIndex % segmentNormals.length;
        Vector2f previousNormal = segmentNormals[previousSegment];
        Vector2f nextNormal = segmentNormals[nextSegment];
        Vector2f miter = previousNormal.add(nextNormal);
        float miterLengthSquared = miter.lengthSquared();
        if (miterLengthSquared <= EPSILON * EPSILON) {
            return nextNormal.mult(halfWidth);
        }
        miter.multLocal(1f / (float) Math.sqrt(miterLengthSquared));
        float denominator = Math.abs(miter.dot(nextNormal));
        float scale = Math.min(halfWidth / Math.max(denominator, EPSILON), halfWidth * MITER_LIMIT);
        return miter.multLocal(scale);
    }
}
