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
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Immutable compact navigation page. Actor policy is never baked into a page.
 */
public final class NavChunk {

    static final int STRIDE = 40;
    final NavChunkKey key;
    final long serial;
    final NavSettings settings;
    final ByteBuffer nodes;
    final IntBuffer cells;
    final int nodeCount;
    final boolean hasSurfaces;
    final NavGeometry[] geometry;

    NavChunk(
        NavChunkKey key,
        long serial,
        NavSettings settings,
        ByteBuffer nodes,
        IntBuffer cells,
        int nodeCount,
        NavGeometry[] geometry
    ) {
        this.key = key;
        this.serial = serial;
        this.settings = settings;
        this.nodes = nodes;
        this.cells = cells;
        this.nodeCount = nodeCount;
        this.geometry = geometry;
        boolean surface = false;
        for (int i = 0; i < nodeCount && !surface; i++) surface = nodes.getInt(i * STRIDE + 36) != 6;
        hasSurfaces = surface;
    }

    public NavChunkKey getKey() {
        return key;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public long getGeneration() {
        return serial;
    }

    int nodeAt(int x, int y, int z, int face) {
        int n = settings.chunkSize;
        if (x < 0 || y < 0 || z < 0 || x >= n || y >= n || z >= (settings.planar ? 1 : n)) return -1;
        return cells.get(((z * n + y) * n + x) * 7 + face) - 1;
    }

    Vector3f point(int node, Vector3f store) {
        int i = node * STRIDE;
        return store.set(nodes.getFloat(i), nodes.getFloat(i + 4), nodes.getFloat(i + 8));
    }

    Vector3f normal(int node, Vector3f store) {
        int i = node * STRIDE;
        return store.set(nodes.getFloat(i + 12), nodes.getFloat(i + 16), nodes.getFloat(i + 20));
    }

    int face(int node) {
        return nodes.getInt(node * STRIDE + 36);
    }

    int cell(int node) {
        return nodes.getInt(node * STRIDE + 32);
    }

    int area(int node) {
        return nodes.getInt(node * STRIDE + 24);
    }

    int flags(int node) {
        return nodes.getInt(node * STRIDE + 28);
    }

    static NavChunk build(NavChunkKey key, long serial, NavSettings settings, NavGeometry[] geometry, byte[] occupancy) {
        int n = settings.chunkSize, depth = settings.planar ? 1 : n, h = n + 2;
        float size = settings.cellSize, half = size * 0.5f;
        Arrays.fill(occupancy, (byte) 0);
        int nodeCount = 0;
        try (TempVars vars = TempVars.get()) {
            Vector3f world = vars.vect1;
            float ex = half * (Math.abs(settings.axisX.x) + Math.abs(settings.axisY.x) + Math.abs(settings.axisZ.x));
            float ey = half * (Math.abs(settings.axisX.y) + Math.abs(settings.axisY.y) + Math.abs(settings.axisZ.y));
            float ez = half * (Math.abs(settings.axisX.z) + Math.abs(settings.axisY.z) + Math.abs(settings.axisZ.z));
            for (int z = -1; z <= depth; z++) {
                if (settings.planar && z != 0) continue;
                for (int y = -1; y <= n; y++) for (int x = -1; x <= n; x++) {
                    float lx = (key.x * n + x + 0.5f) * size;
                    float ly = (key.y * n + y + 0.5f) * size;
                    float lz = settings.planar ? 0 : (key.z * n + z + 0.5f) * size;
                    settings.toWorld(lx, ly, lz, world);
                    boolean occupied = false;
                    for (NavGeometry source : geometry) {
                        if (source.type == NavGeometry.AREA || (source.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) continue;
                        if (
                            !source.overlaps(world.x - ex, world.y - ey, world.z - ez, world.x + ex, world.y + ey, world.z + ez)
                        ) continue;
                        if (settings.planar && !NavMath.crossesPlane(settings, source)) continue;
                        if (source.contains(world.x, world.y, world.z)) {
                            occupied = true;
                            break;
                        }
                        if (source.type == NavGeometry.BOX) {
                            occupied =
                                source.maxX > world.x - ex &&
                                source.minX < world.x + ex &&
                                source.maxY > world.y - ey &&
                                source.minY < world.y + ey &&
                                source.maxZ > world.z - ez &&
                                source.minZ < world.z + ez;
                            if (occupied) break;
                            continue;
                        }
                        if (source.type == NavGeometry.SPHERE) {
                            float cx = (source.minX + source.maxX) * 0.5f;
                            float cy = (source.minY + source.maxY) * 0.5f;
                            float cz = (source.minZ + source.maxZ) * 0.5f;
                            float dx = Math.max(0, Math.abs(cx - world.x) - ex);
                            float dy = Math.max(0, Math.abs(cy - world.y) - ey);
                            float dz = Math.max(0, Math.abs(cz - world.z) - ez);
                            float r = (source.maxX - source.minX) * 0.5f;
                            occupied = dx * dx + dy * dy + dz * dz < r * r;
                        } else {
                            for (int node = 0; node < source.bvhNodes && !occupied;) {
                                if (
                                    !source.overlapsNode(
                                        node,
                                        world.x - ex,
                                        world.y - ey,
                                        world.z - ez,
                                        world.x + ex,
                                        world.y + ey,
                                        world.z + ez
                                    )
                                ) {
                                    node = source.skipNode(node);
                                    continue;
                                }
                                int first = source.bvh.getInt(node * 32 + 24), count = source.bvh.getInt(node * 32 + 28);
                                node++;
                                for (int j = 0; j < count && !occupied; j++) {
                                    int t = source.triangleOrder.get(first + j);
                                    if (settings.planar) {
                                        NavMath.triangleNormal(source.triangles, t * 9, vars.vect3);
                                        if (Math.abs(vars.vect3.dot(settings.axisZ)) > 0.999f) continue;
                                    }
                                    occupied =
                                        NavMath.triangleBox(
                                            settings,
                                            source.triangles,
                                            t * 9,
                                            lx,
                                            ly,
                                            lz,
                                            source.solid ? half * 0.9999f : half
                                        );
                                }
                            }
                        }
                        if (occupied) break;
                    }
                    if (occupied) occupancy[((z + 1) * h + y + 1) * h + x + 1] = 1;
                }
            }
            for (int z = 0; z < depth; z++) for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
                if (occupancy[((z + 1) * h + y + 1) * h + x + 1] != 0) continue;
                nodeCount++;
                if (settings.planar) nodeCount++;
                for (int d = 0; d < (settings.planar ? 4 : 6); d++) {
                    if (
                        occupancy[((z + 1 + NavMath.DZ[d]) * h + y + 1 + NavMath.DY[d]) * h + x + 1 + NavMath.DX[d]] != 0
                    ) nodeCount++;
                }
            }
            ByteBuffer data = BufferUtils.createByteBuffer(nodeCount * STRIDE);
            IntBuffer cells = BufferUtils.createIntBuffer(n * n * depth * 7);
            for (int i = 0; i < cells.capacity(); i++) cells.put(i, 0);
            int node = 0;
            for (int z = 0; z < depth; z++) for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
                if (occupancy[((z + 1) * h + y + 1) * h + x + 1] != 0) continue;
                int cell = (z * n + y) * n + x;
                for (int face = 0; face < 7; face++) {
                    if (
                        face < 6 &&
                        !(settings.planar && face == 5) &&
                        (
                            settings.planar &&
                            face >= 4 ||
                            occupancy[((z + 1 + NavMath.DZ[face]) * h + y + 1 + NavMath.DY[face]) *
                                h +
                                x +
                                1 +
                                NavMath.DX[face]] ==
                            0
                        )
                    ) continue;
                    float lx = (key.x * n + x + 0.5f) * size;
                    float ly = (key.y * n + y + 0.5f) * size;
                    float lz = settings.planar ? 0 : (key.z * n + z + 0.5f) * size;
                    settings.toWorld(lx, ly, lz, world);
                    Vector3f point = vars.vect2.set(world), normal = vars.vect3.set(0, 0, 0);
                    int area = 0, flags = NavGeometry.DEFAULT_FLAGS;
                    if (face < 6 && !(settings.planar && face == 5)) {
                        settings.normalToWorld(-NavMath.DX[face], -NavMath.DY[face], -NavMath.DZ[face], vars.vect4);
                        float best = Float.POSITIVE_INFINITY;
                        for (NavGeometry source : geometry) {
                            if ((source.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) continue;
                            float distance = NavMath.closestSurface(source, world, vars.vect4, vars.vect5, vars.vect6);
                            if (distance < best) {
                                best = distance;
                                point.set(vars.vect5);
                                normal.set(vars.vect6);
                                area = source.area;
                                flags = source.flags;
                            }
                        }
                        if (!Float.isFinite(best)) {
                            point.set(world);
                            normal.set(vars.vect4);
                        }
                    } else if (face == 5) normal.set(settings.axisZ);
                    if (settings.planar) {
                        float displacement =
                            (point.x - settings.origin.x) *
                            settings.axisZ.x +
                            (point.y - settings.origin.y) *
                            settings.axisZ.y +
                            (point.z - settings.origin.z) *
                            settings.axisZ.z;
                        point.addLocal(
                            -settings.axisZ.x * displacement,
                            -settings.axisZ.y * displacement,
                            -settings.axisZ.z * displacement
                        );
                        if (face < 4) {
                            float perpendicular = normal.dot(settings.axisZ);
                            normal
                                .addLocal(
                                    -settings.axisZ.x * perpendicular,
                                    -settings.axisZ.y * perpendicular,
                                    -settings.axisZ.z * perpendicular
                                )
                                .normalizeLocal();
                        }
                    }
                    for (NavGeometry source : geometry) if (
                        source.type == NavGeometry.AREA && source.overlaps(point.x, point.y, point.z, point.x, point.y, point.z)
                    ) area = source.area;
                    int i = node * STRIDE;
                    data.putFloat(i, point.x).putFloat(i + 4, point.y).putFloat(i + 8, point.z);
                    data.putFloat(i + 12, normal.x).putFloat(i + 16, normal.y).putFloat(i + 20, normal.z);
                    data.putInt(i + 24, area).putInt(i + 28, flags).putInt(i + 32, cell).putInt(i + 36, face);
                    cells.put(cell * 7 + face, ++node);
                }
            }
            return new NavChunk(key, serial, settings, data, cells, node, geometry);
        }
    }

    public Mesh createDebugMesh() {
        int surfaces = 0;
        for (int i = 0; i < nodeCount; i++) if (face(i) != 6) surfaces++;
        FloatBuffer vertices = BufferUtils.createFloatBuffer(surfaces * 12);
        IntBuffer indices = BufferUtils.createIntBuffer(surfaces * 6);
        try (TempVars vars = TempVars.get()) {
            for (int i = 0; i < nodeCount; i++) {
                if (face(i) == 6) continue;
                Vector3f center = point(i, vars.vect1), normal = normal(i, vars.vect2);
                Vector3f u = vars.vect3.set(Math.abs(normal.y) < 0.8f ? Vector3f.UNIT_Y : Vector3f.UNIT_X);
                normal.cross(u, u).normalizeLocal().multLocal(settings.cellSize * 0.5f);
                normal.cross(u, vars.vect4);
                int base = vertices.position() / 3;
                for (int corner = 0; corner < 4; corner++) {
                    float a = corner == 0 || corner == 3 ? -1 : 1;
                    float b = corner < 2 ? -1 : 1;
                    vertices.put(center.x + u.x * a + vars.vect4.x * b);
                    vertices.put(center.y + u.y * a + vars.vect4.y * b);
                    vertices.put(center.z + u.z * a + vars.vect4.z * b);
                }
                indices.put(base).put(base + 1).put(base + 2).put(base).put(base + 2).put(base + 3);
            }
        }
        vertices.flip();
        indices.flip();
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, vertices);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        mesh.updateBound();
        return mesh;
    }
}
