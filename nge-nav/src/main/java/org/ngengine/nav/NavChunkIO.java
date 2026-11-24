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

package org.ngengine.nav;

import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Versioned portable page format. Reading happens on the loading worker, before attach.
 */
public final class NavChunkIO {

    private static final int MAGIC = 0x4e474e56;
    private static final int VERSION = 1;

    private NavChunkIO() {}

    public static void write(NavChunk chunk, DataOutput output) throws IOException {
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        NavSettings settings = chunk.settings;
        output.writeFloat(settings.cellSize);
        output.writeInt(settings.chunkSize);
        output.writeBoolean(settings.planar);
        Vector3f[] frame = { settings.origin, settings.axisX, settings.axisY, settings.axisZ };
        for (Vector3f vector : frame) {
            output.writeFloat(vector.x);
            output.writeFloat(vector.y);
            output.writeFloat(vector.z);
        }
        output.writeInt(chunk.key.x);
        output.writeInt(chunk.key.y);
        output.writeInt(chunk.key.z);
        output.writeInt(chunk.nodeCount);
        for (int i = 0; i < chunk.nodeCount; i++) {
            int offset = i * NavChunk.STRIDE;
            for (int j = 0; j < 6; j++) output.writeFloat(chunk.nodes.getFloat(offset + j * 4));
            for (int j = 6; j < 10; j++) output.writeInt(chunk.nodes.getInt(offset + j * 4));
        }
        for (int i = 0; i < chunk.cells.capacity(); i++) output.writeInt(chunk.cells.get(i));
        output.writeInt(chunk.geometry.length);
        for (NavGeometry geometry : chunk.geometry) {
            output.writeInt(geometry.type);
            output.writeBoolean(geometry.solid);
            output.writeInt(geometry.area);
            output.writeInt(geometry.flags);
            output.writeFloat(geometry.minX);
            output.writeFloat(geometry.minY);
            output.writeFloat(geometry.minZ);
            output.writeFloat(geometry.maxX);
            output.writeFloat(geometry.maxY);
            output.writeFloat(geometry.maxZ);
            output.writeInt(geometry.triangleCount);
            for (int i = 0; i < geometry.triangleCount * 9; i++) output.writeFloat(geometry.triangles.get(i));
        }
    }

    public static NavChunk read(DataInput input, NavSettings expected) throws IOException {
        return read(input, expected, 1000000);
    }

    public static NavChunk read(DataInput input, NavSettings expected, int maxTriangles) throws IOException {
        if (maxTriangles < 0) throw new IllegalArgumentException("Negative geometry budget");
        if (input.readInt() != MAGIC || input.readInt() != VERSION) throw new IOException("Unsupported navigation page");
        if (
            input.readFloat() != expected.cellSize ||
            input.readInt() != expected.chunkSize ||
            input.readBoolean() != expected.planar
        ) throw new IOException("Incompatible page resolution");
        Vector3f[] frame = { expected.origin, expected.axisX, expected.axisY, expected.axisZ };
        for (Vector3f vector : frame) {
            if (input.readFloat() != vector.x || input.readFloat() != vector.y || input.readFloat() != vector.z) {
                throw new IOException("Incompatible page coordinate frame");
            }
        }
        NavChunkKey key = new NavChunkKey(input.readInt(), input.readInt(), input.readInt());
        if (
            Math.abs((long) key.x) > 1000000 ||
            Math.abs((long) key.y) > 1000000 ||
            Math.abs((long) key.z) > 1000000 ||
            expected.planar &&
            key.z != 0
        ) throw new IOException("Invalid page coordinate");
        int n = expected.chunkSize, cellCount = n * n * (expected.planar ? 1 : n);
        int count = input.readInt();
        if (count < 0 || count > cellCount * 7) throw new IOException("Invalid page node count");
        ByteBuffer nodes = BufferUtils.createByteBuffer(count * NavChunk.STRIDE);
        for (int i = 0; i < count; i++) {
            int offset = i * NavChunk.STRIDE;
            for (int j = 0; j < 6; j++) {
                float value = input.readFloat();
                if (!Float.isFinite(value)) throw new IOException("Non-finite page coordinate");
                nodes.putFloat(offset + j * 4, value);
            }
            for (int j = 6; j < 10; j++) nodes.putInt(offset + j * 4, input.readInt());
            int area = nodes.getInt(offset + 24), flags = nodes.getInt(offset + 28);
            int cell = nodes.getInt(offset + 32), face = nodes.getInt(offset + 36);
            if (
                area < 0 ||
                area > 255 ||
                (flags & ~(NavGeometry.DEFAULT_FLAGS | NavGeometry.SUPPORT_ONLY)) != 0 ||
                cell < 0 ||
                cell >= cellCount ||
                face < 0 ||
                face > 6
            ) throw new IOException("Invalid page node metadata");
            float x = nodes.getFloat(offset + 12), y = nodes.getFloat(offset + 16), z = nodes.getFloat(offset + 20);
            float length = x * x + y * y + z * z;
            if (face == 6 ? length != 0 : Math.abs(length - 1) > 0.001f) throw new IOException("Invalid support normal");
        }
        IntBuffer cells = BufferUtils.createIntBuffer(cellCount * 7);
        int mapped = 0;
        for (int i = 0; i < cells.capacity(); i++) {
            int value = input.readInt();
            if (value < 0 || value > count) throw new IOException("Invalid page cell reference");
            if (value > 0) {
                int offset = (value - 1) * NavChunk.STRIDE;
                if (nodes.getInt(offset + 32) != i / 7 || nodes.getInt(offset + 36) != i % 7) throw new IOException(
                    "Inconsistent page topology"
                );
                mapped++;
            }
            cells.put(i, value);
        }
        if (mapped != count) throw new IOException("Unmapped page nodes");
        int sources = input.readInt();
        if (sources < 0 || sources > 65536) throw new IOException("Invalid source count");
        NavGeometry[] geometry = new NavGeometry[sources];
        int remaining = maxTriangles;
        try {
            for (int s = 0; s < sources; s++) {
                int type = input.readInt();
                boolean solid = input.readBoolean();
                int area = input.readInt(), flags = input.readInt();
                Vector3f min = new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
                Vector3f max = new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
                int triangles = input.readInt();
                if (triangles < 0 || triangles > remaining || triangles > Integer.MAX_VALUE / 9) throw new IOException(
                    "Geometry budget exceeded"
                );
                remaining -= triangles;
                if (type == NavGeometry.BOX && triangles == 0 && solid) geometry[s] =
                    NavGeometry.box(min, max, area, flags); else if (type == NavGeometry.SPHERE && triangles == 0 && solid) {
                    float radius = (max.x - min.x) * 0.5f;
                    if (
                        radius <= 0 ||
                        Math.abs(max.y - min.y - radius * 2) > 0.0001f ||
                        Math.abs(max.z - min.z - radius * 2) > 0.0001f
                    ) throw new IOException("Invalid sphere bounds");
                    geometry[s] = NavGeometry.sphere(min.addLocal(max).multLocal(0.5f), radius, area, flags);
                } else if (type == NavGeometry.AREA && triangles == 0 && !solid) geometry[s] =
                    NavGeometry.areaBox(min, max, area); else if (type == NavGeometry.MESH && triangles > 0) {
                    float[] vertices = new float[triangles * 9];
                    for (int i = 0; i < vertices.length; i++) vertices[i] = input.readFloat();
                    geometry[s] = NavGeometry.triangles(vertices, solid, area, flags);
                } else throw new IOException("Invalid geometry type");
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid page geometry", exception);
        }
        return new NavChunk(key, 0, expected, nodes, cells, count, geometry);
    }
}
