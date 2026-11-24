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

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Immutable geometry captured on its scene or physics owner's thread.
 */
public final class NavGeometry {

    public static final int WALKABLE = 1;
    public static final int CLIMBABLE = 2;
    /** Collision-only geometry for an interactable object that must never become support. */
    public static final int MOVABLE_OBSTACLE = 4;
    /** Authored navigation guides provide support without becoming physical barriers. */
    public static final int SUPPORT_ONLY = 8;
    public static final int DEFAULT_FLAGS = WALKABLE | CLIMBABLE;
    static final int MESH = 0, BOX = 1, SPHERE = 2, AREA = 3;
    final int type;
    final boolean solid;
    final int area;
    final int flags;
    final long movableObstacleId;
    final float minX, minY, minZ, maxX, maxY, maxZ;
    final FloatBuffer triangles;
    final int triangleCount;
    final int[] componentEnds;
    final FloatBuffer componentBounds;
    final ByteBuffer bvh;
    final IntBuffer triangleOrder;
    final int bvhNodes;

    private NavGeometry(
        int type,
        boolean solid,
        int area,
        int flags,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        FloatBuffer triangles,
        int count,
        int[] componentEnds
    ) {
        if (area < 0 || area > 255 || (flags & ~(DEFAULT_FLAGS | MOVABLE_OBSTACLE | SUPPORT_ONLY)) != 0
            || (flags & MOVABLE_OBSTACLE) != 0 && (flags & DEFAULT_FLAGS) != 0) throw new IllegalArgumentException(
            "Invalid surface metadata"
        );
        this.type = type;
        this.solid = solid;
        this.area = area;
        this.flags = flags;
        movableObstacleId = 0;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.triangles = triangles;
        triangleCount = count;
        this.componentEnds = componentEnds;
        if (type == MESH) {
            int[] order = new int[count];
            float[] centroids = new float[count * 3];
            for (int i = 0; i < count; i++) {
                order[i] = i;
                for (int axis = 0; axis < 3; axis++) centroids[i * 3 + axis] =
                    triangles.get(i * 9 + axis) / 3 + triangles.get(i * 9 + 3 + axis) / 3 + triangles.get(i * 9 + 6 + axis) / 3;
            }
            bvh = BufferUtils.createByteBuffer(Math.max(1, count / 2 + 2) * 32);
            int[] next = { 0 };
            buildBvh(order, centroids, 0, count, next);
            bvhNodes = next[0];
            triangleOrder = BufferUtils.createIntBuffer(count);
            triangleOrder.put(order).clear();
            if (componentEnds != null) {
                componentBounds = BufferUtils.createFloatBuffer(componentEnds.length * 6);
                int start = 0;
                for (int c = 0; c < componentEnds.length; c++) {
                    float x0 = Float.POSITIVE_INFINITY, y0 = x0, z0 = x0, x1 = Float.NEGATIVE_INFINITY, y1 = x1, z1 = x1;
                    for (int i = start; i < componentEnds[c]; i += 3) {
                        x0 = Math.min(x0, triangles.get(i));
                        y0 = Math.min(y0, triangles.get(i + 1));
                        z0 = Math.min(z0, triangles.get(i + 2));
                        x1 = Math.max(x1, triangles.get(i));
                        y1 = Math.max(y1, triangles.get(i + 1));
                        z1 = Math.max(z1, triangles.get(i + 2));
                    }
                    componentBounds.put(x0).put(y0).put(z0).put(x1).put(y1).put(z1);
                    start = componentEnds[c];
                }
            } else componentBounds = null;
        } else {
            bvh = null;
            triangleOrder = null;
            bvhNodes = 0;
            componentBounds = null;
        }
    }

    private void buildBvh(int[] order, float[] centroids, int first, int limit, int[] next) {
        int node = next[0]++, offset = node * 32;
        float x0 = Float.POSITIVE_INFINITY, y0 = x0, z0 = x0, x1 = Float.NEGATIVE_INFINITY, y1 = x1, z1 = x1;
        float cx0 = x0, cy0 = x0, cz0 = x0, cx1 = x1, cy1 = x1, cz1 = x1;
        for (int i = first; i < limit; i++) {
            int triangle = order[i];
            for (int v = 0; v < 9; v += 3) {
                int p = triangle * 9 + v;
                x0 = Math.min(x0, triangles.get(p));
                y0 = Math.min(y0, triangles.get(p + 1));
                z0 = Math.min(z0, triangles.get(p + 2));
                x1 = Math.max(x1, triangles.get(p));
                y1 = Math.max(y1, triangles.get(p + 1));
                z1 = Math.max(z1, triangles.get(p + 2));
            }
            cx0 = Math.min(cx0, centroids[triangle * 3]);
            cx1 = Math.max(cx1, centroids[triangle * 3]);
            cy0 = Math.min(cy0, centroids[triangle * 3 + 1]);
            cy1 = Math.max(cy1, centroids[triangle * 3 + 1]);
            cz0 = Math.min(cz0, centroids[triangle * 3 + 2]);
            cz1 = Math.max(cz1, centroids[triangle * 3 + 2]);
        }
        bvh.putFloat(offset, x0).putFloat(offset + 4, y0).putFloat(offset + 8, z0);
        bvh.putFloat(offset + 12, x1).putFloat(offset + 16, y1).putFloat(offset + 20, z1);
        if (limit - first <= 8) {
            bvh.putInt(offset + 24, first).putInt(offset + 28, limit - first);
            return;
        }
        int axis = cx1 - cx0 >= cy1 - cy0 && cx1 - cx0 >= cz1 - cz0 ? 0 : cy1 - cy0 >= cz1 - cz0 ? 1 : 2;
        int middle = (first + limit) >>> 1, low = first, high = limit - 1;
        while (low < high) {
            float pivot = centroids[order[(low + high) >>> 1] * 3 + axis];
            int left = low, right = high;
            while (left <= right) {
                while (centroids[order[left] * 3 + axis] < pivot) left++;
                while (centroids[order[right] * 3 + axis] > pivot) right--;
                if (left <= right) {
                    int swap = order[left];
                    order[left++] = order[right];
                    order[right--] = swap;
                }
            }
            if (middle <= right) high = right; else if (middle >= left) low = left; else break;
        }
        buildBvh(order, centroids, first, middle, next);
        buildBvh(order, centroids, middle, limit, next);
        bvh.putInt(offset + 24, next[0]).putInt(offset + 28, 0);
    }

    int skipNode(int node) {
        return bvh.getInt(node * 32 + 28) == 0 ? bvh.getInt(node * 32 + 24) : node + 1;
    }

    boolean overlapsNode(int node, float x0, float y0, float z0, float x1, float y1, float z1) {
        int i = node * 32;
        return (
            bvh.getFloat(i + 12) >= x0 &&
            bvh.getFloat(i) <= x1 &&
            bvh.getFloat(i + 16) >= y0 &&
            bvh.getFloat(i + 4) <= y1 &&
            bvh.getFloat(i + 20) >= z0 &&
            bvh.getFloat(i + 8) <= z1
        );
    }

    float distanceToNode(int node, Vector3f point) {
        int i = node * 32;
        float x = Math.max(0, Math.max(bvh.getFloat(i) - point.x, point.x - bvh.getFloat(i + 12)));
        float y = Math.max(0, Math.max(bvh.getFloat(i + 4) - point.y, point.y - bvh.getFloat(i + 16)));
        float z = Math.max(0, Math.max(bvh.getFloat(i + 8) - point.z, point.z - bvh.getFloat(i + 20)));
        return x * x + y * y + z * z;
    }

    private boolean insideRayNode(int node, float x, float y, float z) {
        int i = node * 32;
        float low = Math.max(
            0,
            Math.max(
                bvh.getFloat(i) - x,
                Math.max((bvh.getFloat(i + 4) - y) / 0.37139067f, (bvh.getFloat(i + 8) - z) / 0.52911389f)
            )
        );
        float high = Math.min(
            bvh.getFloat(i + 12) - x,
            Math.min((bvh.getFloat(i + 16) - y) / 0.37139067f, (bvh.getFloat(i + 20) - z) / 0.52911389f)
        );
        return low <= high;
    }

    /** Runtime obstacle identity; IDs belong to the publishing world and are not saved in static page files. */
    public NavGeometry asMovableObstacle(long identity) {
        if (identity <= 0 || type == AREA) throw new IllegalArgumentException("Invalid movable obstacle identity or geometry");
        return new NavGeometry(this, identity);
    }

    private NavGeometry(NavGeometry source, long identity) {
        type = source.type; solid = source.solid; area = source.area; flags = MOVABLE_OBSTACLE; movableObstacleId = identity;
        minX = source.minX; minY = source.minY; minZ = source.minZ;
        maxX = source.maxX; maxY = source.maxY; maxZ = source.maxZ;
        triangles = source.triangles; triangleCount = source.triangleCount; componentEnds = source.componentEnds;
        componentBounds = source.componentBounds; bvh = source.bvh; triangleOrder = source.triangleOrder; bvhNodes = source.bvhNodes;
    }

    public static NavGeometry box(Vector3f min, Vector3f max) {
        return box(min, max, 0, DEFAULT_FLAGS);
    }

    public static NavGeometry box(Vector3f min, Vector3f max, int area, int flags) {
        if (
            !Vector3f.isValidVector(min) || !Vector3f.isValidVector(max) || min.x >= max.x || min.y >= max.y || min.z >= max.z
        ) throw new IllegalArgumentException("Invalid box");
        return new NavGeometry(BOX, true, area, flags, min.x, min.y, min.z, max.x, max.y, max.z, null, 0, null);
    }

    /**
     * Overrides area cost metadata inside a box without adding an obstacle.
     */
    public static NavGeometry areaBox(Vector3f min, Vector3f max, int area) {
        if (
            !Vector3f.isValidVector(min) || !Vector3f.isValidVector(max) || min.x >= max.x || min.y >= max.y || min.z >= max.z
        ) throw new IllegalArgumentException("Invalid area box");
        return new NavGeometry(AREA, false, area, DEFAULT_FLAGS, min.x, min.y, min.z, max.x, max.y, max.z, null, 0, null);
    }

    public static NavGeometry sphere(Vector3f center, float radius, int area, int flags) {
        if (!Vector3f.isValidVector(center) || !Float.isFinite(radius) || radius <= 0) throw new IllegalArgumentException(
            "Invalid sphere"
        );
        return new NavGeometry(
            SPHERE,
            true,
            area,
            flags,
            center.x - radius,
            center.y - radius,
            center.z - radius,
            center.x + radius,
            center.y + radius,
            center.z + radius,
            null,
            0,
            null
        );
    }

    /**
     * Captures an ordered convex polygon in the world's plane. Coordinates are local plane x/y.
     * Useful for physics polygons; the small extrusion only supplies a closed occupancy solid.
     */
    public static NavGeometry planarPolygon(NavSettings settings, float[] xy, int area, int flags) {
        if (!settings.planar || xy.length < 6 || xy.length % 2 != 0 || xy.length > 131072) {
            throw new IllegalArgumentException("Expected a convex polygon and planar world settings");
        }
        int count = xy.length / 2;
        float orientation = 0;
        for (int i = 0; i < count; i++) {
            int a = i * 2, b = ((i + 1) % count) * 2, c = ((i + 2) % count) * 2;
            if (!Float.isFinite(xy[a]) || !Float.isFinite(xy[a + 1])) throw new IllegalArgumentException(
                "Invalid polygon position"
            );
            float turn = (xy[b] - xy[a]) * (xy[c + 1] - xy[b + 1]) - (xy[b + 1] - xy[a + 1]) * (xy[c] - xy[b]);
            if (Math.abs(turn) < 1e-8f) throw new IllegalArgumentException("Degenerate polygon edge");
            if (orientation == 0) orientation = turn; else if (orientation * turn < 0) throw new IllegalArgumentException(
                "Polygon must be convex and ordered"
            );
        }
        FloatBuffer vertices = BufferUtils.createFloatBuffer(count * 6);
        try (TempVars vars = TempVars.get()) {
            for (int side = 0; side < 2; side++) for (int i = 0; i < count; i++) {
                int source = orientation > 0 ? i : count - 1 - i;
                settings.toWorld(xy[source * 2], xy[source * 2 + 1], (side == 0 ? -1 : 1) * settings.cellSize, vars.vect1);
                vertices.put(vars.vect1.x).put(vars.vect1.y).put(vars.vect1.z);
            }
        }
        int triangles = count * 4 - 4;
        FloatBuffer data = BufferUtils.createFloatBuffer(triangles * 9);
        int[] indices = new int[triangles * 3];
        int cursor = 0;
        for (int i = 1; i < count - 1; i++) {
            indices[cursor++] = 0;
            indices[cursor++] = i + 1;
            indices[cursor++] = i;
            indices[cursor++] = count;
            indices[cursor++] = count + i;
            indices[cursor++] = count + i + 1;
        }
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            indices[cursor++] = i;
            indices[cursor++] = next;
            indices[cursor++] = count + next;
            indices[cursor++] = i;
            indices[cursor++] = count + next;
            indices[cursor++] = count + i;
        }
        for (int index : indices) data
            .put(vertices.get(index * 3))
            .put(vertices.get(index * 3 + 1))
            .put(vertices.get(index * 3 + 2));
        return fromTriangles(data, triangles, true, area, flags);
    }

    public static NavGeometry capture(Geometry geometry, boolean closedSolid, int area, int flags) {
        return capture(geometry.getMesh(), geometry.getWorldTransform(), closedSolid, area, flags);
    }

    public static NavGeometry capture(Mesh mesh, Transform transform, boolean closedSolid, int area, int flags) {
        if (
            mesh.getMode() != Mesh.Mode.Triangles &&
            mesh.getMode() != Mesh.Mode.TriangleStrip &&
            mesh.getMode() != Mesh.Mode.TriangleFan
        ) throw new IllegalArgumentException("Navigation requires triangle topology");
        VertexBuffer positions = mesh.getBuffer(VertexBuffer.Type.Position);
        if (positions == null || positions.getFormat() != VertexBuffer.Format.Float || positions.getNumComponents() != 3) {
            throw new IllegalArgumentException("Navigation requires float3 positions");
        }
        IndexBuffer indices = mesh.getIndicesAsList();
        if (indices.size() % 3 != 0) throw new IllegalArgumentException("Navigation requires triangles");
        FloatBuffer vertices = (FloatBuffer) positions.getData();
        int count = indices.size() / 3;
        if (count > Integer.MAX_VALUE / 36) throw new IllegalArgumentException("Mesh is too large");
        FloatBuffer data = BufferUtils.createFloatBuffer(count * 9);
        try (TempVars vars = TempVars.get()) {
            Vector3f v = vars.vect1;
            for (int i = 0; i < indices.size(); i++) {
                int index = indices.get(i);
                if (index < 0 || index >= mesh.getVertexCount()) throw new IllegalArgumentException("Invalid triangle index");
                v.set(vertices.get(index * 3), vertices.get(index * 3 + 1), vertices.get(index * 3 + 2));
                transform.transformVector(v, v);
                if (!Vector3f.isValidVector(v)) throw new IllegalArgumentException("Non-finite mesh position");
                data.put(v.x).put(v.y).put(v.z);
            }
        }
        return fromTriangles(data, count, closedSolid, area, flags);
    }

    public static NavGeometry triangles(float[] xyz, boolean closedSolid, int area, int flags) {
        if (xyz.length == 0 || xyz.length % 9 != 0) throw new IllegalArgumentException("Expected triangle coordinates");
        FloatBuffer data = BufferUtils.createFloatBuffer(xyz.length);
        for (float value : xyz) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite position");
            data.put(value);
        }
        return fromTriangles(data, xyz.length / 9, closedSolid, area, flags);
    }

    private static NavGeometry fromTriangles(FloatBuffer data, int count, boolean solid, int area, int flags) {
        if (count == 0) throw new IllegalArgumentException("Empty geometry");
        float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
        float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (int i = 0; i < count * 9; i += 3) {
            minX = Math.min(minX, data.get(i));
            maxX = Math.max(maxX, data.get(i));
            minY = Math.min(minY, data.get(i + 1));
            maxY = Math.max(maxY, data.get(i + 1));
            minZ = Math.min(minZ, data.get(i + 2));
            maxZ = Math.max(maxZ, data.get(i + 2));
        }
        int[] components = solid ? validateClosed(data, count) : null;
        data.clear();
        return new NavGeometry(MESH, solid, area, flags, minX, minY, minZ, maxX, maxY, maxZ, data, count, components);
    }

    private static int[] validateClosed(FloatBuffer data, int count) {
        int vertices = count * 3, capacity = 16;
        while (capacity < vertices * 2L) {
            if (capacity >= 1 << 28) throw new IllegalArgumentException("Solid validation capacity exceeded");
            capacity <<= 1;
        }
        int[] slots = new int[capacity];
        int[] canonical = new int[vertices];
        for (int i = 0; i < vertices; i++) {
            int x = Float.floatToIntBits(data.get(i * 3) + 0f);
            int y = Float.floatToIntBits(data.get(i * 3 + 1) + 0f);
            int z = Float.floatToIntBits(data.get(i * 3 + 2) + 0f);
            int slot = (x * 73856093 ^ y * 19349663 ^ z * 83492791) & (capacity - 1);
            while (slots[slot] != 0) {
                int other = slots[slot] - 1;
                if (
                    data.get(i * 3) == data.get(other * 3) &&
                    data.get(i * 3 + 1) == data.get(other * 3 + 1) &&
                    data.get(i * 3 + 2) == data.get(other * 3 + 2)
                ) break;
                slot = (slot + 1) & (capacity - 1);
            }
            if (slots[slot] == 0) slots[slot] = i + 1;
            canonical[i] = slots[slot] - 1;
        }
        long[] edges = new long[vertices];
        for (int i = 0; i < vertices; i += 3) {
            for (int e = 0; e < 3; e++) {
                int a = canonical[i + e], b = canonical[i + (e + 1) % 3];
                if (a == b) throw new IllegalArgumentException("Degenerate closed-solid face");
                edges[i + e] = ((long) Math.min(a, b) << 33) | ((long) Math.max(a, b) << 1) | (a < b ? 1 : 0);
            }
        }
        Arrays.sort(edges);
        for (int i = 0; i < edges.length; i += 2) {
            if (
                i + 1 >= edges.length ||
                (edges[i] ^ edges[i + 1]) != 1L ||
                (i + 2 < edges.length && (edges[i] >>> 1) == (edges[i + 2] >>> 1))
            ) {
                throw new IllegalArgumentException("Closed-solid mesh must be watertight and consistently oriented");
            }
        }
        int[] parents = new int[count], sizes = new int[count];
        Arrays.fill(sizes, 1);
        for (int i = 0; i < count; i++) parents[i] = i;
        for (int i = 0; i < vertices; i++) {
            int first = i / 3, second = canonical[i] / 3;
            while (parents[first] != first) {
                parents[first] = parents[parents[first]];
                first = parents[first];
            }
            while (parents[second] != second) {
                parents[second] = parents[parents[second]];
                second = parents[second];
            }
            if (first != second) {
                if (sizes[first] < sizes[second]) {
                    parents[first] = second;
                    sizes[second] += sizes[first];
                } else {
                    parents[second] = first;
                    sizes[first] += sizes[second];
                }
            }
        }
        int[] ids = new int[count], counts = sizes;
        Arrays.fill(counts, 0);
        int components = 0;
        for (int i = 0; i < count; i++) {
            int root = i;
            while (parents[root] != root) root = parents[root];
            if (ids[root] == 0) ids[root] = ++components;
            canonical[i] = ids[root] - 1;
            counts[canonical[i]]++;
        }
        if (components == 1) return new int[] { count * 9 };
        int[] ends = new int[components], cursor = new int[components];
        int offset = 0;
        for (int i = 0; i < components; i++) {
            cursor[i] = offset;
            offset += counts[i] * 9;
            ends[i] = offset;
        }
        float[] grouped = new float[count * 9];
        for (int i = 0; i < count; i++) {
            int target = cursor[canonical[i]];
            for (int j = 0; j < 9; j++) grouped[target + j] = data.get(i * 9 + j);
            cursor[canonical[i]] += 9;
        }
        data.clear();
        data.put(grouped);
        return ends;
    }

    public BoundingBox getBounds(BoundingBox store) {
        store.setCenter((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f);
        store.setXExtent((maxX - minX) * 0.5f);
        store.setYExtent((maxY - minY) * 0.5f);
        store.setZExtent((maxZ - minZ) * 0.5f);
        return store;
    }

    boolean overlaps(float x0, float y0, float z0, float x1, float y1, float z1) {
        return maxX >= x0 && minX <= x1 && maxY >= y0 && minY <= y1 && maxZ >= z0 && minZ <= z1;
    }

    boolean contains(float x, float y, float z) {
        if (!solid || x <= minX || x >= maxX || y <= minY || y >= maxY || z <= minZ || z >= maxZ) return false;
        if (type == BOX) return true;
        if (type == SPHERE) {
            float dx = x - (minX + maxX) * 0.5f, dy = y - (minY + maxY) * 0.5f, dz = z - (minZ + maxZ) * 0.5f;
            float r = (maxX - minX) * 0.5f;
            return dx * dx + dy * dy + dz * dz < r * r;
        }
        // Generalized winding number avoids parity errors when a ray hits a shared edge.
        int start = 0, component = 0;
        for (int end : componentEnds) {
            int cb = component++ * 6;
            if (
                x <= componentBounds.get(cb) ||
                y <= componentBounds.get(cb + 1) ||
                z <= componentBounds.get(cb + 2) ||
                x >= componentBounds.get(cb + 3) ||
                y >= componentBounds.get(cb + 4) ||
                z >= componentBounds.get(cb + 5)
            ) {
                start = end;
                continue;
            }
            int crossings = 0;
            boolean ambiguous = false;
            for (int node = 0; node < bvhNodes && !ambiguous;) {
                if (!insideRayNode(node, x, y, z)) {
                    node = skipNode(node);
                    continue;
                }
                int offset = bvh.getInt(node * 32 + 24), size = bvh.getInt(node * 32 + 28);
                node++;
                for (int j = 0; j < size; j++) {
                    int triangle = triangleOrder.get(offset + j) * 9;
                    if (triangle < start || triangle >= end) continue;
                    float hit = NavMath.rayTriangle(x, y, z, 1, 0.37139067f, 0.52911389f, triangles, triangle, true);
                    if (hit == -2) {
                        ambiguous = true;
                        break;
                    }
                    if (hit > 0) crossings++;
                }
            }
            if (!ambiguous) {
                if ((crossings & 1) != 0) return true;
                start = end;
                continue;
            }
            double angle = 0;
            for (int i = start; i < end; i += 9) {
                double ax = triangles.get(i) - x, ay = triangles.get(i + 1) - y, az = triangles.get(i + 2) - z;
                double bx = triangles.get(i + 3) - x, by = triangles.get(i + 4) - y, bz = triangles.get(i + 5) - z;
                double cx = triangles.get(i + 6) - x, cy = triangles.get(i + 7) - y, cz = triangles.get(i + 8) - z;
                double a = Math.sqrt(ax * ax + ay * ay + az * az), b = Math.sqrt(bx * bx + by * by + bz * bz);
                double c = Math.sqrt(cx * cx + cy * cy + cz * cz);
                double numerator = ax * (by * cz - bz * cy) + ay * (bz * cx - bx * cz) + az * (bx * cy - by * cx);
                double denominator =
                    a *
                    b *
                    c +
                    (ax * bx + ay * by + az * bz) *
                    c +
                    (bx * cx + by * cy + bz * cz) *
                    a +
                    (cx * ax + cy * ay + cz * az) *
                    b;
                angle += 2 * Math.atan2(numerator, denominator);
            }
            if (Math.abs(angle) > Math.PI * 2) return true;
            start = end;
        }
        return false;
    }
}
