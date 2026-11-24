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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;
import java.nio.FloatBuffer;

final class NavMath {

    static final int[] DX = { -1, 1, 0, 0, 0, 0 };
    static final int[] DY = { 0, 0, -1, 1, 0, 0 };
    static final int[] DZ = { 0, 0, 0, 0, -1, 1 };

    private NavMath() {}

    static boolean crossesPlane(NavSettings settings, NavGeometry geometry) {
        float cx = (geometry.minX + geometry.maxX) * 0.5f - settings.origin.x;
        float cy = (geometry.minY + geometry.maxY) * 0.5f - settings.origin.y;
        float cz = (geometry.minZ + geometry.maxZ) * 0.5f - settings.origin.z;
        float distance = cx * settings.axisZ.x + cy * settings.axisZ.y + cz * settings.axisZ.z;
        float extent = geometry.type == NavGeometry.SPHERE
            ? (geometry.maxX - geometry.minX) * 0.5f
            : Math.abs(settings.axisZ.x) *
            (geometry.maxX - geometry.minX) *
            0.5f +
            Math.abs(settings.axisZ.y) *
            (geometry.maxY - geometry.minY) *
            0.5f +
            Math.abs(settings.axisZ.z) *
            (geometry.maxZ - geometry.minZ) *
            0.5f;
        return Math.abs(distance) < extent - settings.cellSize * 0.00001f;
    }

    static Vector3f triangleNormal(FloatBuffer triangle, int offset, Vector3f store) {
        float ax = triangle.get(offset), ay = triangle.get(offset + 1), az = triangle.get(offset + 2);
        float bx = triangle.get(offset + 3) - ax, by = triangle.get(offset + 4) - ay, bz = triangle.get(offset + 5) - az;
        float cx = triangle.get(offset + 6) - ax, cy = triangle.get(offset + 7) - ay, cz = triangle.get(offset + 8) - az;
        return store.set(by * cz - bz * cy, bz * cx - bx * cz, bx * cy - by * cx).normalizeLocal();
    }

    /**
     * Clips a coplanar support segment to one box face or triangle; x/y receive its parameter interval.
     */
    static boolean supportInterval(
        NavGeometry geometry,
        int face,
        Vector3f from,
        Vector3f to,
        Vector3f normal,
        float tolerance,
        Vector3f interval
    ) {
        double low = 0, high = 1;
        if (geometry.type == NavGeometry.BOX) {
            float facing = DX[face] * normal.x + DY[face] * normal.y + DZ[face] * normal.z;
            if (facing < 0.99999f) return false;
            float plane = face == 0
                ? -geometry.minX
                : face == 1
                    ? geometry.maxX
                    : face == 2 ? -geometry.minY : face == 3 ? geometry.maxY : face == 4 ? -geometry.minZ : geometry.maxZ;
            if (
                Math.abs(DX[face] * from.x + DY[face] * from.y + DZ[face] * from.z - plane) > tolerance ||
                Math.abs(DX[face] * to.x + DY[face] * to.y + DZ[face] * to.z - plane) > tolerance
            ) return false;
            for (int axis = 0; axis < 3; axis++) {
                if (axis == face / 2) continue;
                double start = axis == 0 ? from.x : axis == 1 ? from.y : from.z;
                double delta = (axis == 0 ? to.x : axis == 1 ? to.y : to.z) - start;
                double min = axis == 0 ? geometry.minX : axis == 1 ? geometry.minY : geometry.minZ;
                double max = axis == 0 ? geometry.maxX : axis == 1 ? geometry.maxY : geometry.maxZ;
                if (Math.abs(delta) < 1e-12) {
                    if (start < min || start > max) return false;
                } else {
                    double a = (min - start) / delta, b = (max - start) / delta;
                    low = Math.max(low, Math.min(a, b));
                    high = Math.min(high, Math.max(a, b));
                }
                if (low > high) return false;
            }
        } else {
            FloatBuffer points = geometry.triangles;
            int p = face * 9;
            double ax = points.get(p), ay = points.get(p + 1), az = points.get(p + 2);
            double bx = points.get(p + 3) - ax, by = points.get(p + 4) - ay, bz = points.get(p + 5) - az;
            double cx = points.get(p + 6) - ax, cy = points.get(p + 7) - ay, cz = points.get(p + 8) - az;
            double nx = by * cz - bz * cy, ny = bz * cx - bx * cz, nz = bx * cy - by * cx;
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < 1e-12 || Math.abs((nx * normal.x + ny * normal.y + nz * normal.z) / length) < 0.99999) return false;
            if (
                Math.abs(nx * (from.x - ax) + ny * (from.y - ay) + nz * (from.z - az)) > tolerance * length ||
                Math.abs(nx * (to.x - ax) + ny * (to.y - ay) + nz * (to.z - az)) > tolerance * length
            ) return false;
            double bb = bx * bx + by * by + bz * bz, cc = cx * cx + cy * cy + cz * cz;
            double bc = bx * cx + by * cy + bz * cz, determinant = bb * cc - bc * bc;
            if (determinant <= 1e-20) return false;
            double fb = (from.x - ax) * bx + (from.y - ay) * by + (from.z - az) * bz;
            double fc = (from.x - ax) * cx + (from.y - ay) * cy + (from.z - az) * cz;
            double tb = (to.x - ax) * bx + (to.y - ay) * by + (to.z - az) * bz;
            double tc = (to.x - ax) * cx + (to.y - ay) * cy + (to.z - az) * cz;
            double u0 = (cc * fb - bc * fc) / determinant, v0 = (bb * fc - bc * fb) / determinant;
            double u1 = (cc * tb - bc * tc) / determinant, v1 = (bb * tc - bc * tb) / determinant;
            for (int edge = 0; edge < 3; edge++) {
                double start = edge == 0 ? u0 : edge == 1 ? v0 : 1 - u0 - v0;
                double end = edge == 0 ? u1 : edge == 1 ? v1 : 1 - u1 - v1;
                double delta = end - start;
                if (Math.abs(delta) < 1e-12) {
                    if (start < -1e-7) return false;
                } else if (delta > 0) low = Math.max(low, -start / delta); else high = Math.min(high, -start / delta);
                if (low > high + 1e-7) return false;
            }
        }
        interval.set((float) Math.max(0, low), (float) Math.min(1, high), 0);
        return interval.x <= interval.y + 1e-7f;
    }

    static float segmentBox(Vector3f from, Vector3f to, NavGeometry box) {
        float low = 0, high = 1;
        for (int axis = 0; axis < 3; axis++) {
            float a = axis == 0 ? from.x : axis == 1 ? from.y : from.z;
            float b = axis == 0 ? to.x : axis == 1 ? to.y : to.z;
            float min = axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ;
            float max = axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ;
            float delta = b - a;
            if (Math.abs(delta) < 1e-12f) {
                if (a < min || a > max) return -1;
            } else {
                float first = (min - a) / delta, last = (max - a) / delta;
                if (first > last) {
                    float swap = first;
                    first = last;
                    last = swap;
                }
                low = Math.max(low, first);
                high = Math.min(high, last);
                if (low > high) return -1;
            }
        }
        return low;
    }

    static float rayTriangle(float px, float py, float pz, float dx, float dy, float dz, FloatBuffer t, int i) {
        return rayTriangle(px, py, pz, dx, dy, dz, t, i, false);
    }

    static float rayTriangle(
        float px,
        float py,
        float pz,
        float dx,
        float dy,
        float dz,
        FloatBuffer t,
        int i,
        boolean detectEdges
    ) {
        float ax = t.get(i), ay = t.get(i + 1), az = t.get(i + 2);
        float ex = t.get(i + 3) - ax, ey = t.get(i + 4) - ay, ez = t.get(i + 5) - az;
        float fx = t.get(i + 6) - ax, fy = t.get(i + 7) - ay, fz = t.get(i + 8) - az;
        float hx = dy * fz - dz * fy, hy = dz * fx - dx * fz, hz = dx * fy - dy * fx;
        double det = (double) ex * hx + (double) ey * hy + (double) ez * hz;
        if (Math.abs(det) < 1e-12) return -1;
        double sx = px - ax, sy = py - ay, sz = pz - az;
        double u = (sx * hx + sy * hy + sz * hz) / det;
        if (u < 0 || u > 1) return -1;
        double qx = sy * ez - sz * ey, qy = sz * ex - sx * ez, qz = sx * ey - sy * ex;
        double v = (dx * qx + dy * qy + dz * qz) / det;
        if (v < 0 || u + v > 1) return -1;
        float distance = (float) ((fx * qx + fy * qy + fz * qz) / det);
        if (detectEdges && distance > 0 && (u < 1e-7 || v < 1e-7 || 1 - u - v < 1e-7)) return -2;
        return distance;
    }

    static float closestTriangle(Vector3f p, FloatBuffer t, int i, Vector3f store, Vector3f normal) {
        float ax = t.get(i), ay = t.get(i + 1), az = t.get(i + 2);
        float bx = t.get(i + 3), by = t.get(i + 4), bz = t.get(i + 5);
        float cx = t.get(i + 6), cy = t.get(i + 7), cz = t.get(i + 8);
        float abx = bx - ax, aby = by - ay, abz = bz - az;
        float acx = cx - ax, acy = cy - ay, acz = cz - az;
        normal.set(aby * acz - abz * acy, abz * acx - abx * acz, abx * acy - aby * acx);
        if (normal.lengthSquared() < 1e-16f) return Float.POSITIVE_INFINITY;
        normal.normalizeLocal();
        float apx = p.x - ax, apy = p.y - ay, apz = p.z - az;
        float d1 = abx * apx + aby * apy + abz * apz;
        float d2 = acx * apx + acy * apy + acz * apz;
        if (d1 <= 0 && d2 <= 0) store.set(ax, ay, az); else {
            float bpx = p.x - bx, bpy = p.y - by, bpz = p.z - bz;
            float d3 = abx * bpx + aby * bpy + abz * bpz;
            float d4 = acx * bpx + acy * bpy + acz * bpz;
            float vc = d1 * d4 - d3 * d2;
            if (d3 >= 0 && d4 <= d3) store.set(bx, by, bz); else if (vc <= 0 && d1 >= 0 && d3 <= 0) {
                float v = d1 / (d1 - d3);
                store.set(ax + abx * v, ay + aby * v, az + abz * v);
            } else {
                float cpx = p.x - cx, cpy = p.y - cy, cpz = p.z - cz;
                float d5 = abx * cpx + aby * cpy + abz * cpz;
                float d6 = acx * cpx + acy * cpy + acz * cpz;
                float vb = d5 * d2 - d1 * d6;
                float va = d3 * d6 - d5 * d4;
                if (d6 >= 0 && d5 <= d6) store.set(cx, cy, cz); else if (vb <= 0 && d2 >= 0 && d6 <= 0) {
                    float w = d2 / (d2 - d6);
                    store.set(ax + acx * w, ay + acy * w, az + acz * w);
                } else if (va <= 0 && d4 - d3 >= 0 && d5 - d6 >= 0) {
                    float w = (d4 - d3) / (d4 - d3 + d5 - d6);
                    store.set(bx + (cx - bx) * w, by + (cy - by) * w, bz + (cz - bz) * w);
                } else {
                    float inverse = 1f / (va + vb + vc), v = vb * inverse, w = vc * inverse;
                    store.set(ax + abx * v + acx * w, ay + aby * v + acy * w, az + abz * v + acz * w);
                }
            }
        }
        return p.distanceSquared(store);
    }

    static float segmentDistance(Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        float ux = b.x - a.x, uy = b.y - a.y, uz = b.z - a.z;
        float vx = d.x - c.x, vy = d.y - c.y, vz = d.z - c.z;
        float wx = a.x - c.x, wy = a.y - c.y, wz = a.z - c.z;
        float aa = ux * ux + uy * uy + uz * uz, bb = ux * vx + uy * vy + uz * vz;
        float cc = vx * vx + vy * vy + vz * vz, dd = ux * wx + uy * wy + uz * wz;
        float ee = vx * wx + vy * wy + vz * wz;
        float s = 0, t = 0;
        if (aa < 1e-12f) t = cc < 1e-12f ? 0 : FastMath.clamp(ee / cc, 0, 1); else if (cc < 1e-12f) s =
            FastMath.clamp(-dd / aa, 0, 1); else {
            float denominator = aa * cc - bb * bb;
            if (denominator > 1e-12f) s = FastMath.clamp((bb * ee - cc * dd) / denominator, 0, 1);
            t = (bb * s + ee) / cc;
            if (t < 0) {
                t = 0;
                s = FastMath.clamp(-dd / aa, 0, 1);
            } else if (t > 1) {
                t = 1;
                s = FastMath.clamp((bb - dd) / aa, 0, 1);
            }
        }
        float x = wx + ux * s - vx * t, y = wy + uy * s - vy * t, z = wz + uz * s - vz * t;
        return x * x + y * y + z * z;
    }

    static float capsuleTriangle(Vector3f a, Vector3f b, FloatBuffer triangles, int offset) {
        float hit = rayTriangle(a.x, a.y, a.z, b.x - a.x, b.y - a.y, b.z - a.z, triangles, offset);
        if (hit >= 0 && hit <= 1) return 0;
        try (TempVars vars = TempVars.get()) {
            float best = Math.min(
                closestTriangle(a, triangles, offset, vars.vect1, vars.vect2),
                closestTriangle(b, triangles, offset, vars.vect1, vars.vect2)
            );
            for (int e = 0; e < 3; e++) {
                int i = offset + e * 3, j = offset + ((e + 1) % 3) * 3;
                vars.vect3.set(triangles.get(i), triangles.get(i + 1), triangles.get(i + 2));
                vars.vect4.set(triangles.get(j), triangles.get(j + 1), triangles.get(j + 2));
                best = Math.min(best, segmentDistance(a, b, vars.vect3, vars.vect4));
            }
            return best;
        }
    }

    static boolean support(NavGeometry geometry, Vector3f foot, Vector3f up, float reach, Vector3f point, Vector3f normal) {
        if (geometry.type == NavGeometry.AREA) return false;
        try (TempVars vars = TempVars.get()) {
            Vector3f start = up.mult(reach, vars.vect1).addLocal(foot);
            Vector3f end = up.mult(-reach, vars.vect2).addLocal(foot);
            if (geometry.type == NavGeometry.BOX) {
                float hit = segmentBox(start, end, geometry);
                if (hit < 0) return false;
                point.interpolateLocal(start, end, hit);
                float best = Float.POSITIVE_INFINITY;
                for (int face = 0; face < 6; face++) {
                    float coordinate = face < 2 ? point.x : face < 4 ? point.y : point.z;
                    float bound = face == 0
                        ? geometry.minX
                        : face == 1
                            ? geometry.maxX
                            : face == 2 ? geometry.minY : face == 3 ? geometry.maxY : face == 4 ? geometry.minZ : geometry.maxZ;
                    float facing = NavMath.DX[face] * up.x + NavMath.DY[face] * up.y + NavMath.DZ[face] * up.z;
                    if (facing <= 0 || Math.abs(coordinate - bound) >= best) continue;
                    best = Math.abs(coordinate - bound);
                    normal.set(NavMath.DX[face], NavMath.DY[face], NavMath.DZ[face]);
                }
                float tolerance = Math.max(Math.ulp(point.x), Math.max(Math.ulp(point.y), Math.ulp(point.z))) * 8 + 0.000001f;
                return Float.isFinite(best) && best <= tolerance;
            }
            if (geometry.type == NavGeometry.SPHERE) {
                float x = foot.x - (geometry.minX + geometry.maxX) * 0.5f, y = foot.y - (geometry.minY + geometry.maxY) * 0.5f;
                float z = foot.z - (geometry.minZ + geometry.maxZ) * 0.5f, radius = (geometry.maxX - geometry.minX) * 0.5f;
                float projection = x * up.x + y * up.y + z * up.z;
                float discriminant = projection * projection - (x * x + y * y + z * z - radius * radius);
                if (discriminant < 0) return false;
                float height = -projection + FastMath.sqrt(discriminant);
                if (Math.abs(height) > reach) return false;
                point.set(foot).addLocal(up.x * height, up.y * height, up.z * height);
                normal.set(x + up.x * height, y + up.y * height, z + up.z * height).normalizeLocal();
                return true;
            }
            float best = Float.POSITIVE_INFINITY;
            for (int node = 0; node < geometry.bvhNodes;) {
                if (
                    !geometry.overlapsNode(
                        node,
                        Math.min(start.x, end.x),
                        Math.min(start.y, end.y),
                        Math.min(start.z, end.z),
                        Math.max(start.x, end.x),
                        Math.max(start.y, end.y),
                        Math.max(start.z, end.z)
                    )
                ) {
                    node = geometry.skipNode(node);
                    continue;
                }
                int first = geometry.bvh.getInt(node * 32 + 24), count = geometry.bvh.getInt(node * 32 + 28);
                node++;
                for (int j = 0; j < count; j++) {
                    int t = geometry.triangleOrder.get(first + j);
                    float hit = rayTriangle(
                        start.x,
                        start.y,
                        start.z,
                        end.x - start.x,
                        end.y - start.y,
                        end.z - start.z,
                        geometry.triangles,
                        t * 9
                    );
                    if (hit < 0 || hit > 1 || Math.abs(hit - 0.5f) >= best) continue;
                    vars.vect3.interpolateLocal(start, end, hit);
                    closestTriangle(vars.vect3, geometry.triangles, t * 9, vars.vect4, vars.vect5);
                    if (vars.vect5.dot(up) < 0) vars.vect5.negateLocal();
                    if (vars.vect5.dot(up) <= 0.0001f) continue;
                    best = Math.abs(hit - 0.5f);
                    point.set(vars.vect3);
                    normal.set(vars.vect5);
                }
            }
            return Float.isFinite(best);
        }
    }

    static float closestSurface(NavGeometry geometry, Vector3f p, Vector3f facing, Vector3f store, Vector3f normal) {
        if (geometry.type == NavGeometry.AREA) return Float.POSITIVE_INFINITY;
        if (geometry.type == NavGeometry.BOX) {
            store.set(
                FastMath.clamp(p.x, geometry.minX, geometry.maxX),
                FastMath.clamp(p.y, geometry.minY, geometry.maxY),
                FastMath.clamp(p.z, geometry.minZ, geometry.maxZ)
            );
            normal.set(p).subtractLocal(store);
            if (normal.lengthSquared() < 1e-12f) {
                normal.set(facing);
                int axis = Math.abs(facing.x) > 0.5f ? 0 : Math.abs(facing.y) > 0.5f ? 1 : 2;
                if (axis == 0) store.x = facing.x > 0 ? geometry.maxX : geometry.minX; else if (axis == 1) store.y =
                    facing.y > 0 ? geometry.maxY : geometry.minY; else store.z = facing.z > 0 ? geometry.maxZ : geometry.minZ;
            }
            normal.normalizeLocal();
            return normal.dot(facing) > 0.25f ? p.distanceSquared(store) : Float.POSITIVE_INFINITY;
        }
        if (geometry.type == NavGeometry.SPHERE) {
            float cx = (geometry.minX + geometry.maxX) * 0.5f;
            float cy = (geometry.minY + geometry.maxY) * 0.5f;
            float cz = (geometry.minZ + geometry.maxZ) * 0.5f;
            normal.set(p.x - cx, p.y - cy, p.z - cz).normalizeLocal();
            float radius = (geometry.maxX - geometry.minX) * 0.5f;
            store.set(cx + normal.x * radius, cy + normal.y * radius, cz + normal.z * radius);
            return normal.dot(facing) > 0.25f ? p.distanceSquared(store) : Float.POSITIVE_INFINITY;
        }
        float best = Float.POSITIVE_INFINITY;
        try (TempVars vars = TempVars.get()) {
            for (int node = 0; node < geometry.bvhNodes;) {
                if (geometry.distanceToNode(node, p) > best) {
                    node = geometry.skipNode(node);
                    continue;
                }
                int first = geometry.bvh.getInt(node * 32 + 24), count = geometry.bvh.getInt(node * 32 + 28);
                node++;
                for (int j = 0; j < count; j++) {
                    int i = geometry.triangleOrder.get(first + j);
                    float distance = closestTriangle(p, geometry.triangles, i * 9, vars.vect1, vars.vect2);
                    if (vars.vect2.dot(facing) < 0) vars.vect2.negateLocal();
                    if (distance < best && vars.vect2.dot(facing) > 0.25f) {
                        best = distance;
                        store.set(vars.vect1);
                        normal.set(vars.vect2);
                    }
                }
            }
        }
        return best;
    }

    static boolean triangleBox(NavSettings settings, FloatBuffer triangle, int offset, float x, float y, float z, float half) {
        try (TempVars vars = TempVars.get()) {
            Vector3f a = vars.vect1, b = vars.vect2, c = vars.vect3;
            a.set(triangle.get(offset), triangle.get(offset + 1), triangle.get(offset + 2));
            b.set(triangle.get(offset + 3), triangle.get(offset + 4), triangle.get(offset + 5));
            c.set(triangle.get(offset + 6), triangle.get(offset + 7), triangle.get(offset + 8));
            settings.toLocal(a, a).subtractLocal(x, y, z);
            settings.toLocal(b, b).subtractLocal(x, y, z);
            settings.toLocal(c, c).subtractLocal(x, y, z);
            if (
                Math.min(a.x, Math.min(b.x, c.x)) > half ||
                Math.max(a.x, Math.max(b.x, c.x)) < -half ||
                Math.min(a.y, Math.min(b.y, c.y)) > half ||
                Math.max(a.y, Math.max(b.y, c.y)) < -half ||
                Math.min(a.z, Math.min(b.z, c.z)) > half ||
                Math.max(a.z, Math.max(b.z, c.z)) < -half
            ) return false;
            Vector3f edge = vars.vect4, axis = vars.vect5;
            for (int e = 0; e < 3; e++) {
                Vector3f from = e == 0 ? a : e == 1 ? b : c;
                Vector3f to = e == 0 ? b : e == 1 ? c : a;
                to.subtract(from, edge);
                if (
                    separated(a, b, c, 0, edge.z, -edge.y, half) ||
                    separated(a, b, c, -edge.z, 0, edge.x, half) ||
                    separated(a, b, c, edge.y, -edge.x, 0, half)
                ) return false;
            }
            b.subtract(a, edge);
            c.subtract(a, axis);
            edge.cross(axis, axis);
            return !separated(a, b, c, axis.x, axis.y, axis.z, half);
        }
    }

    private static boolean separated(Vector3f a, Vector3f b, Vector3f c, float x, float y, float z, float half) {
        float p = a.x * x + a.y * y + a.z * z;
        float q = b.x * x + b.y * y + b.z * z;
        float r = c.x * x + c.y * y + c.z * z;
        float extent = half * (Math.abs(x) + Math.abs(y) + Math.abs(z));
        return Math.min(p, Math.min(q, r)) > extent || Math.max(p, Math.max(q, r)) < -extent;
    }
}
