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

final class NavCollision {

    private NavCollision() {}

    static boolean clear(NavSnapshot snapshot, NavView view, Vector3f position, Vector3f axis) {
        try (TempVars vars = TempVars.get()) {
            float half = view.planeSupport ? 0 : Math.max(0, view.height * 0.5f - view.radius);
            Vector3f a = axis.mult(half, vars.vect1).addLocal(position);
            Vector3f b = axis.mult(-half, vars.vect2).addLocal(position);
            // Strict by default; physical controllers may admit their solver's
            // small contact slop without shifting the measured navigation pose.
            float radius = Math.max(0, view.radius - Math.max(view.contactSkin, view.world.settings.cellSize * 0.0001f));
            float radiusSquared = radius * radius;
            NavSettings settings = view.world.settings;
            float span = settings.cellSize * settings.chunkSize;
            settings.toLocal(position, vars.vect5);
            float ex = radius + half * Math.abs(axis.dot(settings.axisX));
            float ey = radius + half * Math.abs(axis.dot(settings.axisY));
            float ez = radius + half * Math.abs(axis.dot(settings.axisZ));
            int x0 = (int) Math.floor((vars.vect5.x - ex) / span), x1 = (int) Math.floor((vars.vect5.x + ex) / span);
            int y0 = (int) Math.floor((vars.vect5.y - ey) / span), y1 = (int) Math.floor((vars.vect5.y + ey) / span);
            int z0 = settings.planar ? 0 : (int) Math.floor((vars.vect5.z - ez) / span);
            int z1 = settings.planar ? 0 : (int) Math.floor((vars.vect5.z + ez) / span);
            if ((x1 - (long) x0 + 1) * (y1 - (long) y0 + 1) * (z1 - (long) z0 + 1) > settings.localChecks) return false;
            for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                NavChunk page = snapshot.chunk(x, y, z);
                if (page == null) return false;
                for (int source = 0; source < page.geometry.length + snapshot.obstacles.length; source++) {
                    NavGeometry geometry = source < page.geometry.length ? page.geometry[source] : snapshot.obstacles[source - page.geometry.length];
                    if ((geometry.flags & NavGeometry.SUPPORT_ONLY) != 0) continue;
                    if (source < page.geometry.length && (geometry.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) continue;
                    if (view.ignoredMovableObstacle != 0 && view.ignoredMovableObstacle == geometry.movableObstacleId) continue;
                    if (settings.planar && !NavMath.crossesPlane(settings, geometry)) continue;
                    if (
                        !geometry.overlaps(
                            Math.min(a.x, b.x) - radius,
                            Math.min(a.y, b.y) - radius,
                            Math.min(a.z, b.z) - radius,
                            Math.max(a.x, b.x) + radius,
                            Math.max(a.y, b.y) + radius,
                            Math.max(a.z, b.z) + radius
                        )
                    ) continue;
                    if (
                        geometry.contains(position.x, position.y, position.z) ||
                        geometry.contains(a.x, a.y, a.z) ||
                        geometry.contains(b.x, b.y, b.z)
                    ) return false;
                    if (geometry.type == NavGeometry.SPHERE) {
                        Vector3f center = vars.vect3.set(
                            (geometry.minX + geometry.maxX) * 0.5f,
                            (geometry.minY + geometry.maxY) * 0.5f,
                            (geometry.minZ + geometry.maxZ) * 0.5f
                        );
                        float sum = radius + (geometry.maxX - geometry.minX) * 0.5f;
                        if (NavMath.segmentDistance(a, b, center, center) < sum * sum) return false;
                    } else if (geometry.type == NavGeometry.BOX) {
                        // Squared distance to a convex box along a segment is convex. The interval search
                        // also handles a capsule crossing a box with both endpoints outside it.
                        float low = 0, high = 1;
                        for (int i = 0; i < 32; i++) {
                            float left = low + (high - low) / 3, right = high - (high - low) / 3;
                            if (boxDistance(a, b, left, geometry) <= boxDistance(a, b, right, geometry)) high =
                                right; else low = left;
                        }
                        float distance = boxDistance(a, b, (low + high) * 0.5f, geometry);
                        if (distance < radiusSquared || (radius == 0 && distance == 0)) return false;
                    } else {
                        for (int node = 0; node < geometry.bvhNodes;) {
                            if (
                                !geometry.overlapsNode(
                                    node,
                                    Math.min(a.x, b.x) - radius,
                                    Math.min(a.y, b.y) - radius,
                                    Math.min(a.z, b.z) - radius,
                                    Math.max(a.x, b.x) + radius,
                                    Math.max(a.y, b.y) + radius,
                                    Math.max(a.z, b.z) + radius
                                )
                            ) {
                                node = geometry.skipNode(node);
                                continue;
                            }
                            int first = geometry.bvh.getInt(node * 32 + 24), count = geometry.bvh.getInt(node * 32 + 28);
                            node++;
                            for (int j = 0; j < count; j++) {
                                int t = geometry.triangleOrder.get(first + j);
                                if (
                                    settings.planar &&
                                    Math.abs(
                                        NavMath.triangleNormal(geometry.triangles, t * 9, vars.vect4).dot(settings.axisZ)
                                    ) >
                                    0.999f
                                ) continue;
                                float distance = NavMath.capsuleTriangle(a, b, geometry.triangles, t * 9);
                                if (distance < radiusSquared || (radius == 0 && distance == 0)) return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    private static float boxDistance(Vector3f a, Vector3f b, float t, NavGeometry geometry) {
        float x = a.x + (b.x - a.x) * t, y = a.y + (b.y - a.y) * t, z = a.z + (b.z - a.z) * t;
        float dx = x - FastMath.clamp(x, geometry.minX, geometry.maxX);
        float dy = y - FastMath.clamp(y, geometry.minY, geometry.maxY);
        float dz = z - FastMath.clamp(z, geometry.minZ, geometry.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    static boolean step(NavSnapshot snapshot, NavView view, Vector3f from, Vector3f to, float lift) {
        try (TempVars vars = TempVars.get()) {
            float height = (to.x - from.x) * view.up.x + (to.y - from.y) * view.up.y + (to.z - from.z) * view.up.z;
            Vector3f a = vars.vect1.set(from), b = vars.vect2.set(to);
            if (height > 0) a.addLocal(view.up.x * height, view.up.y * height, view.up.z * height); else b.addLocal(
                -view.up.x * height,
                -view.up.y * height,
                -view.up.z * height
            );
            a.addLocal(view.up.x * lift, view.up.y * lift, view.up.z * lift);
            b.addLocal(view.up.x * lift, view.up.y * lift, view.up.z * lift);
            return (
                sweep(snapshot, view, from, a, view.up, view.up, view.world.settings.localChecks) &&
                sweep(snapshot, view, a, b, view.up, view.up, view.world.settings.localChecks) &&
                sweep(snapshot, view, b, to, view.up, view.up, view.world.settings.localChecks)
            );
        }
    }

    static boolean bend(
        NavSnapshot snapshot,
        NavView view,
        Vector3f from,
        Vector3f to,
        Vector3f fromAxis,
        Vector3f toAxis,
        int area,
        float ascent
    ) {
        float dot = FastMath.clamp(fromAxis.dot(toAxis), -1, 1), denominator = 1 - dot * dot;
        if (denominator < 0.0001f) return false;
        try (TempVars vars = TempVars.get()) {
            float half = view.planeSupport ? view.radius : view.height * 0.5f;
            Vector3f a = fromAxis.mult(-half, vars.vect1).addLocal(from);
            Vector3f b = toAxis.mult(-half, vars.vect2).addLocal(to);
            Vector3f pivot = vars.vect3.interpolateLocal(a, b, 0.5f);
            float ca = fromAxis.dot(a) - fromAxis.dot(pivot), cb = toAxis.dot(b) - toAxis.dot(pivot);
            float wa = (ca - dot * cb) / denominator, wb = (cb - dot * ca) / denominator;
            pivot.addLocal(fromAxis.x * wa + toAxis.x * wb, fromAxis.y * wa + toAxis.y * wb, fromAxis.z * wa + toAxis.z * wb);
            Vector3f point = vars.vect4.set(fromAxis).multLocal(half).addLocal(pivot);
            if (!sweep(snapshot, view, from, point, fromAxis, fromAxis, view.world.settings.localChecks)) return false;
            int samples = Math.max(2, (int) Math.ceil(FastMath.acos(dot) * half * 2 / Math.max(0.01f, view.radius * 0.25f)));
            if (samples > view.world.settings.localChecks) return false;
            for (int i = 0; i <= samples; i++) {
                Vector3f axis = vars.vect5.interpolateLocal(fromAxis, toAxis, (float) i / samples).normalizeLocal();
                float inclination = FastMath.acos(FastMath.clamp(axis.dot(view.up), -1, 1));
                if (!Float.isFinite(view.evaluate(NavMode.CLIMB, area, inclination, ascent))) return false;
                point.set(axis).multLocal(half).addLocal(pivot);
                if (!clear(snapshot, view, point, axis)) return false;
            }
            return sweep(snapshot, view, point, to, toAxis, toAxis, view.world.settings.localChecks);
        }
    }

    static boolean sweep(
        NavSnapshot snapshot,
        NavView view,
        Vector3f from,
        Vector3f to,
        Vector3f fromAxis,
        Vector3f toAxis,
        int budget
    ) {
        float spacing = Math.min(view.world.settings.cellSize * 0.2f, Math.max(view.radius * 0.25f, 0.001f));
        int samples = Math.max(1, (int) Math.ceil(from.distance(to) / spacing));
        float rotation = view.planeSupport ? 0 : FastMath.acos(FastMath.clamp(fromAxis.dot(toAxis), -1, 1));
        samples = Math.max(samples, (int) Math.ceil(rotation * view.height / spacing));
        if (samples > budget) return false;
        try (TempVars vars = TempVars.get()) {
            for (int i = 0; i <= samples; i++) {
                float t = (float) i / samples;
                vars.vect1.interpolateLocal(from, to, t);
                vars.vect2.interpolateLocal(fromAxis, toAxis, t).normalizeLocal();
                if (!clear(snapshot, view, vars.vect1, vars.vect2)) return false;
            }
            return true;
        }
    }
}
