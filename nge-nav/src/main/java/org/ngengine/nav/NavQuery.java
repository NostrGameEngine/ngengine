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
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

/**
 * A query workspace belongs to one worker or navigator. Arrays survive subsequent searches.
 */
final class NavQuery {
    private final Vector3f marginStart = new Vector3f(), marginEnd = new Vector3f();
    private final Vector3f marginFrom = new Vector3f(), marginTo = new Vector3f(), marginAxis = new Vector3f();

    private final NavSettings settings;
    private final long[] refs;
    private final long[] via;
    private final double[] costs;
    private final double[] distances;
    private final double[] priorities;
    private final double[] distancePriorities;
    private final Vector3f goalPoint = new Vector3f();
    private final Vector3f boundsFrom = new Vector3f();
    private final Vector3f boundsTo = new Vector3f();
    private final NavGeometry[] localSources;
    private final long[] supportIntervals;
    private final float[] surfacePlanes;
    private final int[] surfaceRoute;
    private final float[] surfacePrices;
    private int sourceCount;
    private float minimumCost;
    private double minimumEntryCost;
    private boolean usableLinks;
    private boolean wideSteps, exhaustedWideSteps;
    private int expandedNodes;
    private int stepX, stepY, stepZ;
    private long[] linkDestinations = new long[0];
    private byte[] linkArcs = new byte[0];
    private final long[] edgeFrom, edgeTo;
    private final byte[] edgeClear;
    private long edgeRevision = Long.MIN_VALUE;
    private long edgeSurfaceRevision = Long.MIN_VALUE;
    private java.lang.ref.WeakReference<NavView> edgeView;
    private java.lang.ref.WeakReference<NavGeometry[]> edgeGeometry;
    private long exhaustedBegin;
    private final Vector3f initialDirection = new Vector3f();
    private final Vector3f supportCenter = new Vector3f();
    private float initialSpeed;
    private final int[] parents;
    private final int[] heap;
    private final int[] heapPositions;
    private final int[] slots;
    private final int[] route;
    private int count;
    private int heapSize;
    private int current;
    private long goal;
    private boolean exhausted;
    private boolean missing;
    private NavSnapshot snapshot;
    private NavView view;
    private final Vector3f p0 = new Vector3f();
    private final Vector3f p1 = new Vector3f();
    private final Vector3f n0 = new Vector3f();
    private final Vector3f n1 = new Vector3f();
    private final Vector3f direction = new Vector3f();
    private final Vector3f nearPoint = new Vector3f();
    private final Vector3f nearNormal = new Vector3f();
    private final Vector3f arcPoint = new Vector3f();
    private final Vector3f arcPrevious = new Vector3f();
    NavReason reason = NavReason.NONE;
    float surfaceRise;
    double surfaceCost;

    NavQuery(NavSettings settings) {
        this(settings, true);
    }

    NavQuery(NavSettings settings, boolean search) {
        this.settings = settings;
        localSources = new NavGeometry[settings.localChecks];
        supportIntervals = new long[settings.localChecks];
        surfacePlanes = new float[settings.localChecks * 4];
        surfaceRoute = new int[settings.localChecks];
        surfacePrices = new float[settings.localChecks];
        refs = new long[search ? settings.maxSearchNodes : 0];
        via = new long[refs.length];
        costs = new double[refs.length];
        distances = new double[refs.length];
        priorities = new double[refs.length];
        distancePriorities = new double[refs.length];
        parents = new int[refs.length];
        heap = new int[refs.length];
        heapPositions = new int[refs.length];
        int capacity = 16;
        while (capacity < refs.length * 2L) capacity <<= 1;
        slots = new int[capacity];
        route = new int[search ? settings.maxPathPoints : 0];
        int edges = search ? Math.min(65536, capacity * 4) : 0;
        edgeFrom = new long[edges]; edgeTo = new long[edges]; edgeClear = new byte[edges];
    }

    void bind(NavSnapshot snapshot, NavView view) {
        this.snapshot = snapshot;
        this.view = view;
        if (snapshot != null && (edgeRevision != snapshot.revision || edgeView == null || edgeView.get() != view)) {
            NavGeometry[] previous = edgeGeometry == null ? null : edgeGeometry.get();
            // New traversal links do not change ordinary edge clearance. Moving
            // obstacles do, even when they preserve the rasterized floor revision.
            if (edgeSurfaceRevision != snapshot.surfaceRevision || edgeView == null || edgeView.get() != view
                    || !Arrays.equals(previous, snapshot.geometry)) Arrays.fill(edgeFrom, 0);
            exhaustedBegin = 0;
            edgeRevision = snapshot.revision; edgeView = new java.lang.ref.WeakReference<>(view);
            edgeSurfaceRevision = snapshot.surfaceRevision; edgeGeometry = new java.lang.ref.WeakReference<>(snapshot.geometry);
        }
        if (snapshot == null) {
            Arrays.fill(localSources, 0, sourceCount, null);
            sourceCount = 0;
        }
        reason = NavReason.NONE;
    }

    Vector3f pose(long ref, Vector3f point, Vector3f normal) {
        NavChunk chunk = snapshot.resolve(ref);
        if (chunk == null) return null;
        int node = NavSnapshot.node(ref);
        chunk.point(node, point);
        chunk.normal(node, normal);
        NavMode mode = NavSnapshot.mode(ref);
        boolean virtualPlane = settings.planar && chunk.face(node) == 5;
        float offset = view.planeSupport ? view.radius : view.height * 0.5f;
        if (virtualPlane) {
            normal.set(view.up);
            offset = 0;
        } else if (mode == NavMode.WALK) {
            float cosine = normal.dot(view.up);
            if (view.height <= view.radius * 2 && cosine > 0 && cosine < .99999f) {
                supportCenter.set(view.up).multLocal(view.radius / cosine).addLocal(point);
                if (!NavCollision.clear(snapshot, view, supportCenter, view.up)) {
                    // Keep ordinary standing samples stable; only a concave contact
                    // needs the sphere centred along the measured surface normal.
                    point.addLocal(normal.x * view.radius, normal.y * view.radius, normal.z * view.radius);
                    normal.set(view.up);
                    return point;
                }
            }
            if (cosine > 0) offset += view.radius * (1 / Math.max(cosine, 0.001f) - 1);
            normal.set(view.up);
        } else if (mode == NavMode.FLY) normal.set(view.up);
        if (mode != NavMode.FLY) point.addLocal(normal.x * offset, normal.y * offset, normal.z * offset);
        return point;
    }

    float difficulty(long ref, float ascent) {
        NavChunk chunk = snapshot.resolve(ref);
        if (chunk == null) return Float.POSITIVE_INFINITY;
        int node = NavSnapshot.node(ref);
        NavMode mode = NavSnapshot.mode(ref);
        boolean virtualPlane = settings.planar && chunk.face(node) == 5;
        if (virtualPlane && !view.planeSupport) return Float.POSITIVE_INFINITY;
        if (view.getSpeed(mode) <= 0) return Float.POSITIVE_INFINITY;
        if (
            mode == NavMode.WALK &&
            (chunk.flags(node) & NavGeometry.WALKABLE) == 0 ||
            mode == NavMode.CLIMB &&
            (chunk.flags(node) & NavGeometry.CLIMBABLE) == 0
        ) return Float.POSITIVE_INFINITY;
        float inclination = 0;
        if (mode != NavMode.FLY && !virtualPlane) {
            int offset = node * NavChunk.STRIDE + 12;
            float dot =
                chunk.nodes.getFloat(offset) *
                view.up.x +
                chunk.nodes.getFloat(offset + 4) *
                view.up.y +
                chunk.nodes.getFloat(offset + 8) *
                view.up.z;
            inclination = FastMath.acos(FastMath.clamp(dot, -1, 1));
        }
        return view.evaluate(mode, chunk.area(node), inclination, ascent);
    }

    boolean admitted(long ref) {
        return (
            Float.isFinite(difficulty(ref, 0)) &&
            pose(ref, nearPoint, nearNormal) != null &&
            NavCollision.clear(snapshot, view, nearPoint, nearNormal) &&
            (NavSnapshot.mode(ref) != NavMode.WALK || view.supportMargin == 0 || supported(nearPoint, nearPoint, 0, false))
        );
    }

    long nearest(Vector3f position, NavMode preferred, float distance) {
        long result = 0;
        float best = distance * distance;
        int visited = 0;
        reason = NavReason.UNSUPPORTED_POSITION;
        settings.toLocal(position, boundsFrom);
        float px = boundsFrom.x, py = boundsFrom.y, pz = boundsFrom.z;
        float span = settings.cellSize * settings.chunkSize;
        float poseReach = Math.max(view.radius, view.height * .5f);
        if (view.allows(NavMode.WALK)) poseReach += view.radius * (1 / Math.max(FastMath.cos(view.maxSlope), .001f) - 1);
        for (NavChunk chunk : snapshot.chunks) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("Navigation query interrupted");
            if (!chunk.hasSurfaces && !view.allows(NavMode.FLY)) continue;
            // Query-space pages are orthonormal. Reject distant pages before
            // inspecting normals, costs and capsule support for every sample.
            float dx = Math.max(0, Math.max(chunk.key.x * span - settings.cellSize - px, px - (chunk.key.x + 1) * span - settings.cellSize));
            float dy = Math.max(0, Math.max(chunk.key.y * span - settings.cellSize - py, py - (chunk.key.y + 1) * span - settings.cellSize));
            float dz = settings.planar ? Math.abs(pz) : Math.max(0, Math.max(chunk.key.z * span - settings.cellSize - pz, pz - (chunk.key.z + 1) * span - settings.cellSize));
            float reach = (float) Math.sqrt(best) + poseReach;
            if (dx * dx + dy * dy + dz * dz > reach * reach) continue;
            for (int node = 0; node < chunk.nodeCount; node++) {
                NavMode first = chunk.face(node) == 6 ? NavMode.FLY : NavMode.WALK;
                int choices = first == NavMode.FLY ? 1 : 2;
                for (int choice = 0; choice < choices; choice++) {
                    NavMode mode = choice == 0 ? first : NavMode.CLIMB;
                    if (preferred != null && mode != preferred || !view.allows(mode)) continue;
                    long ref = NavSnapshot.reference(chunk, node, mode);
                    // Reject distant raw samples before pose() performs concave support
                    // collision checks. Include the full possible standing offset.
                    chunk.point(node, nearPoint); chunk.normal(node, nearNormal);
                    float offset = view.planeSupport ? view.radius : view.height * .5f;
                    float cosine = nearNormal.dot(view.up);
                    if (mode == NavMode.WALK && cosine > 0) offset += view.radius * (1 / Math.max(cosine, .001f) - 1);
                    if (mode == NavMode.FLY || settings.planar && chunk.face(node) == 5) offset = 0;
                    float sampleReach = (float) Math.sqrt(best) + Math.max(offset, view.radius);
                    if (nearPoint.distanceSquared(position) > sampleReach * sampleReach) continue;
                    if (!Float.isFinite(difficulty(ref, 0))) continue;
                    pose(ref, nearPoint, nearNormal);
                    float delta = nearPoint.distanceSquared(position);
                    if (delta > best) continue;
                    if (++visited > settings.maxNearestNodes) {
                        reason = NavReason.BUDGET_EXCEEDED;
                        return 0;
                    }
                    if (
                        !NavCollision.clear(snapshot, view, nearPoint, nearNormal)
                        || mode == NavMode.WALK && view.supportMargin > 0 && !supported(nearPoint, nearPoint, 0, false)
                    ) continue;
                    if (result == 0 || delta < best || delta == best && ref < result) {
                        best = delta;
                        result = ref;
                    }
                }
            }
        }
        if (result != 0) reason = NavReason.NONE;
        return result;
    }

    private boolean hasData(Vector3f point) {
        settings.toLocal(point, nearPoint);
        float span = settings.cellSize * settings.chunkSize;
        return (
            snapshot.chunk(
                (int) Math.floor(nearPoint.x / span),
                (int) Math.floor(nearPoint.y / span),
                settings.planar ? 0 : (int) Math.floor(nearPoint.z / span)
            ) !=
            null
        );
    }

    NavPath search(
        NavSnapshot snapshot,
        NavView view,
        Vector3f start,
        Vector3f target,
        NavMode startMode,
        Vector3f velocity,
        Vector3f forward,
        boolean closest
    ) {
        expandedNodes = 0;
        NavPath ordinary = search(snapshot, view, start, target, startMode, velocity, forward, closest, false);
        // Clearance gaps need wider step edges, but ordinary routes already have continuous
        // support. Preserve those routes instead of introducing shortcuts across their lips.
        if (view.extendedSteps && view.allows(NavMode.WALK) && view.stepHeight > 0 && !view.planeSupport
                && (ordinary.getReason() == NavReason.NO_PATH || ordinary.getReason() == NavReason.WAITING_FOR_DATA))
            return search(snapshot, view, start, target, startMode, velocity, forward, closest, true);
        return ordinary;
    }

    private NavPath search(NavSnapshot snapshot, NavView view, Vector3f start, Vector3f target,
            NavMode startMode, Vector3f velocity, Vector3f forward, boolean closest, boolean extendedSteps) {
        wideSteps = extendedSteps;
        bind(snapshot, view);
        if (!hasData(start) || !closest && !hasData(target)) return failure(NavReason.WAITING_FOR_DATA);
        initialSpeed = velocity.length();
        initialDirection.set(initialSpeed > 0.000001f ? velocity : forward);
        long begin = nearest(start, startMode, view.nearestDistance);
        if (begin == 0) return failure(reason);
        goal = nearest(target, null, view.nearestDistance);
        if (goal == 0 && !closest) return failure(reason);
        // A fully explored component cannot reach another destination outside it.
        // Keep the existing search table until a new search or snapshot replaces it.
        if (exhaustedBegin != 0 && (!wideSteps || exhaustedWideSteps)) {
            int slot = (int) ((goal ^ (goal >>> 33)) * 0xff51afd7ed558ccdl) & (slots.length - 1);
            while (slots[slot] != 0 && refs[slots[slot] - 1] != goal) slot = (slot + 1) & (slots.length - 1);
            if (slots[slot] == 0) {
                int startSlot = (int) ((begin ^ (begin >>> 33)) * 0xff51afd7ed558ccdl) & (slots.length - 1);
                while (slots[startSlot] != 0 && refs[slots[startSlot] - 1] != begin) startSlot = (startSlot + 1) & (slots.length - 1);
                // Every node reachable from this start was also reachable in the exhausted
                // search, even with directed links. Moving within it cannot open a route out.
                if (!closest && slots[startSlot] != 0) return failure(NavReason.NO_PATH);
                if (closest && exhaustedBegin == begin) {
                    float distance = Float.POSITIVE_INFINITY;
                    for (int i = 0; i < count; i++) {
                        pose(refs[i], p0, n0);
                        float next = p0.distanceSquared(target);
                        if (next < distance) { current = i; distance = next; }
                    }
                    if (current >= 0) return result(NavReason.NO_PATH);
                }
            }
        }
        exhaustedBegin = 0;
        // Cost estimates must remain lower bounds, including for zero-cost modes and authored links.
        minimumCost = Float.POSITIVE_INFINITY;
        for (NavMode mode : NavMode.ALL) if (view.allows(mode) && mode.ordinal() <= NavMode.FLY.ordinal()) {
            minimumCost = Math.min(minimumCost, view.costs[mode.ordinal()]);
        }
        usableLinks = false;
        int endpoints = snapshot.links.length * 2;
        if (linkDestinations.length < endpoints) {
            linkDestinations = new long[endpoints]; linkArcs = new byte[endpoints];
        }
        Arrays.fill(linkDestinations, 0, endpoints, 0);
        Arrays.fill(linkArcs, 0, endpoints, (byte) 0);
        for (NavLink link : snapshot.links) {
            float difficulty = view.evaluate(link.mode, 0, 0, 0) + link.difficulty;
            if (view.allows(link.mode) && Float.isFinite(difficulty) && difficulty <= view.cutoff) {
                usableLinks = true;
                float length = link.from.distance(link.to);
                // Endpoint snapping can cover up to two radii for free. Include
                // that reach when bounding the cost per world-space metre.
                float bound = length == 0 ? 0 : length * link.cost * difficulty / (length + 2 * link.radius);
                minimumCost = Math.min(minimumCost, bound);
            }
        }
        if (view.policy != null || !Float.isFinite(minimumCost)) minimumCost = 0;
        if (goal != 0) pose(goal, goalPoint, nearNormal); else goalPoint.set(target);
        float stepSpan = 2 * view.radius + view.supportMargin + settings.cellSize * .5f;
        float stepRise = view.stepHeight + settings.cellSize * .5f;
        float maximumStepLength = FastMath.sqrt(stepSpan * stepSpan + stepRise * stepRise);
        float upX = Math.abs(view.up.dot(settings.axisX)), upY = Math.abs(view.up.dot(settings.axisY)), upZ = Math.abs(view.up.dot(settings.axisZ));
        stepX = Math.max(1, (int) Math.ceil((stepSpan * FastMath.sqrt(Math.max(0, 1 - upX * upX)) + stepRise * upX) / settings.cellSize));
        stepY = Math.max(1, (int) Math.ceil((stepSpan * FastMath.sqrt(Math.max(0, 1 - upY * upY)) + stepRise * upY) / settings.cellSize));
        stepZ = settings.planar ? 0 : Math.max(1, (int) Math.ceil((stepSpan * FastMath.sqrt(Math.max(0, 1 - upZ * upZ)) + stepRise * upZ) / settings.cellSize));
        minimumEntryCost = goal == 0 ? 0 : entryLowerBound();
        Arrays.fill(slots, 0);
        count = heapSize = 0;
        exhausted = missing = false;
        current = -1;
        add(begin, 0, 0, -1, 0);
        int nearestReachable = -1;
        float nearestDistance = Float.POSITIVE_INFINITY;
        while (heapSize > 0) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("Navigation query interrupted");
            if (exhausted || expandedNodes++ >= settings.maxSearchNodes) return failure(NavReason.BUDGET_EXCEEDED);
            current = pop();
            if (refs[current] == goal) return result(NavReason.NONE);
            long ref = refs[current];
            NavChunk chunk = snapshot.resolve(ref);
            int node = NavSnapshot.node(ref), cell = chunk.cell(node), n = settings.chunkSize;
            int x = chunk.key.x * n + cell % n, y = chunk.key.y * n + cell / n % n;
            int z = settings.planar ? 0 : chunk.key.z * n + cell / (n * n);
            pose(ref, p0, n0);
            if (closest) {
                float distance = p0.distanceSquared(target);
                if (distance < nearestDistance) { nearestDistance = distance; nearestReachable = current; }
            }
            NavMode mode = NavSnapshot.mode(ref);
            boolean blockedWalk = false;
            // At a step, capsule clearance can separate valid stance samples by several cells.
            // Search wider only at an obstructed walking boundary, retaining full support/sweep checks.
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1 && (!wideSteps || !blockedWalk || mode != NavMode.WALK || view.stepHeight == 0 || view.planeSupport)) break;
                int reachX = pass == 0 ? 1 : stepX, reachY = pass == 0 ? 1 : stepY;
                int reachZ = settings.planar ? 0 : pass == 0 ? 1 : stepZ, stepChecks = 0;
                for (int dz = -reachZ; dz <= reachZ; dz++) {
                    for (int dy = -reachY; dy <= reachY; dy++) for (int dx = -reachX; dx <= reachX; dx++) {
                        if (pass == 1 && Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1) continue;
                        if (pass == 1 && ++stepChecks > settings.maxNearestNodes) return failure(NavReason.BUDGET_EXCEEDED);
                        int gx = x + dx, gy = y + dy, gz = z + dz;
                        NavChunk neighbor = snapshot.chunk(
                            Math.floorDiv(gx, n),
                            Math.floorDiv(gy, n),
                            settings.planar ? 0 : Math.floorDiv(gz, n)
                        );
                        if (neighbor == null) {
                            if (pass == 0) missing = true;
                            continue;
                        }
                        for (int face = 0; face < (pass == 0 ? 7 : 6); face++) {
                            int next = neighbor.nodeAt(
                                Math.floorMod(gx, n),
                                Math.floorMod(gy, n),
                                settings.planar ? 0 : Math.floorMod(gz, n),
                                face
                            );
                            if (next < 0) continue;
                            for (int choice = 0; choice < (pass == 1 || face == 6 ? 1 : 2); choice++) {
                                NavMode nextMode = face == 6 ? NavMode.FLY : choice == 0 ? NavMode.WALK : NavMode.CLIMB;
                                if (!view.allows(nextMode)) continue;
                                long candidate = NavSnapshot.reference(neighbor, next, nextMode);
                                if (candidate == ref) continue;
                                boolean stepping = pass == 1 && needsStep(ref, candidate);
                                if (pass == 1 && !stepping) continue;
                                int edge = edgeSlot(ref, candidate);
                                byte cached = view.policy == null && edgeFrom[edge] == ref && edgeTo[edge] == candidate ? edgeClear[edge] : 0;
                                if (cached < 0) {
                                    if (mode == NavMode.WALK && nextMode == NavMode.WALK) blockedWalk = true;
                                    continue;
                                }
                                // Every edge has non-negative cost. An already cheaper arrival
                                // cannot improve through this node, even before collision tests.
                                int slot = (int) ((candidate ^ (candidate >>> 33)) * 0xff51afd7ed558ccdl) & (slots.length - 1);
                                while (slots[slot] != 0 && refs[slots[slot] - 1] != candidate) slot = (slot + 1) & (slots.length - 1);
                                if (slots[slot] != 0) {
                                    int known = slots[slot] - 1;
                                    if (costs[known] < costs[current] || costs[known] == costs[current]
                                            && distances[known] <= distances[current]) continue;
                                }
                                if (mode != NavMode.FLY && nextMode != NavMode.FLY) {
                                    chunk.normal(node, n0);
                                    neighbor.normal(next, n1);
                                    if (n0.dot(n1) < FastMath.cos(view.maxBend)) continue;
                                    // Opposing sheets must never connect through a thin solid.
                                    if (chunk.face(node) == (face ^ 1) && n0.dot(n1) < 0) continue;
                                }
                                pose(ref, p0, n0);
                                pose(candidate, p1, n1);
                                direction.set(p1).subtractLocal(p0);
                                float length = direction.length();
                                if (length > (pass == 0 ? settings.cellSize * 2.6f + view.height
                                        : maximumStepLength)) continue;
                                if (pass == 0) stepping = needsStep(ref, candidate);
                                if (
                                    stepping && Math.abs(direction.dot(view.up)) > view.stepHeight + settings.cellSize * 0.01f
                                ) continue;
                                float ascent = length == 0 ? 0 : direction.dot(view.up) / length;
                                float difficulty = difficulty(candidate, ascent);
                                if (!Float.isFinite(difficulty) || !Float.isFinite(difficulty(ref, ascent))) continue;
                                float lift = stepping ? stepLift(ref, candidate) : 0;
                                if (cached == 0) {
                                    // A blocked stance cannot be reached by any incoming edge.
                                    // Cache it in the same table instead of repeating a whole step sweep.
                                    int stance = edgeSlot(candidate, candidate);
                                    byte standing = edgeFrom[stance] == candidate && edgeTo[stance] == candidate ? edgeClear[stance] : 0;
                                    boolean clear = standing > 0 || standing == 0 && NavCollision.clear(snapshot, view, p1, n1);
                                    if (standing == 0) {
                                        edgeFrom[stance] = edgeTo[stance] = candidate;
                                        edgeClear[stance] = (byte) (clear ? 1 : -1);
                                    }
                                    if (clear && mode == NavMode.CLIMB && nextMode == NavMode.CLIMB && n0.dot(n1) < .995f)
                                        clear = NavCollision.bend(snapshot, view, p0, p1, n0, n1, neighbor.area(next), ascent);
                                    else if (clear) clear = stepping ? NavCollision.step(snapshot, view, p0, p1, lift)
                                            : NavCollision.sweep(snapshot, view, p0, p1, n0, n1, settings.localChecks);
                                    if (clear) clear = segmentAllowed(ref, candidate, p0, p1, n0, n1, ascent);
                                    if (clear && mode == NavMode.WALK && nextMode == NavMode.WALK)
                                        clear = supported(p0, p1, ascent, stepping);
                                    // Geometry and actor settings are immutable. Obstacle publication
                                    // changes the revision; caller-defined cost policies remain uncached.
                                    if (view.policy == null) { edgeFrom[edge] = ref; edgeTo[edge] = candidate; edgeClear[edge] = (byte) (clear ? 1 : -1); }
                                    if (!clear) {
                                        if (mode == NavMode.WALK && nextMode == NavMode.WALK) blockedWalk = true;
                                        continue;
                                    }
                                }
                                float travel = length;
                                if (stepping) {
                                    float rise = direction.dot(view.up);
                                    travel = FastMath.sqrt(Math.max(0, length * length - rise * rise)) + Math.abs(rise) + 2 * lift;
                                }
                                double cost = travel * (double) difficulty;
                                cost += timeCost(mode, nextMode, travel, velocity, direction);
                                add(candidate, costs[current] + cost, distances[current] + travel, current, 0);
                            }
                        }
                    }
                }
            }
            // Links are rare and immutable; ordinary adjacency needs no per-node link objects.
            for (int linkIndex = 0; linkIndex < snapshot.links.length; linkIndex++) {
                NavLink link = snapshot.links[linkIndex];
                float traversalDifficulty = view.evaluate(link.mode, 0, 0, 0) + link.difficulty;
                if (
                    !view.allows(link.mode) || !Float.isFinite(traversalDifficulty) || traversalDifficulty > view.cutoff
                ) continue;
                for (int reverse = 0; reverse < (link.bidirectional ? 2 : 1); reverse++) {
                    Vector3f entrance = reverse == 0 ? link.from : link.to;
                    Vector3f exit = reverse == 0 ? link.to : link.from;
                    if (p0.distanceSquared(entrance) > link.radius * link.radius) continue;
                    int endpoint = linkIndex * 2 + reverse;
                    long destination = linkDestinations[endpoint];
                    if (destination == 0) {
                        destination = nearest(exit, null, link.radius);
                        linkDestinations[endpoint] = destination == 0 ? -1 : destination;
                    }
                    if (destination == -1) continue;
                    if (destination == 0) continue;
                    pose(destination, p1, n1);
                    if (
                        !NavCollision.sweep(snapshot, view, p0, entrance, n0, n0, settings.localChecks) ||
                        !NavCollision.sweep(snapshot, view, exit, p1, n1, n1, settings.localChecks)
                    ) continue;
                    if (link.mode == NavMode.JUMP || link.mode == NavMode.DROP) {
                        if (linkArcs[endpoint] == 0) linkArcs[endpoint] = (byte) (arcClear(link, entrance, exit) ? 1 : -1);
                        if (linkArcs[endpoint] < 0) continue;
                    }
                    double distance = entrance.distance(exit);
                    double cost = distance * link.cost * traversalDifficulty;
                    if (view.kinematics != null) cost += view.kinematics.timeWeight * link.duration;
                    add(destination, costs[current] + cost, distances[current] + distance, current, link.id);
                }
            }
        }
        NavReason failed = exhausted ? NavReason.BUDGET_EXCEEDED : missing ? NavReason.WAITING_FOR_DATA : NavReason.NO_PATH;
        if (failed == NavReason.NO_PATH && view.policy == null && view.kinematics == null) {
            exhaustedBegin = begin; exhaustedWideSteps = wideSteps;
        }
        if (closest && nearestReachable >= 0 && !exhausted) { current = nearestReachable; return result(failed); }
        return failure(failed);
    }

    private int edgeSlot(long from, long to) {
        int edge = (int) ((from * 0x9e3779b97f4a7c15L ^ to * 0xc2b2ae3d27d4eb4fL) >>> 32) & (edgeFrom.length - 1);
        // Adjacent slots avoid repeatedly sweeping edges that share a hash bucket.
        for (int probe = 0; probe < 3 && edgeFrom[edge] != 0
                && (edgeFrom[edge] != from || edgeTo[edge] != to); probe++)
            edge = (edge + 1) & (edgeFrom.length - 1);
        return edge;
    }

    private double entryLowerBound() {
        if (usableLinks) return 0;
        boolean steps = wideSteps && view.allows(NavMode.WALK) && view.stepHeight > 0 && !view.planeSupport;
        int reachX = steps ? stepX : 1, reachY = steps ? stepY : 1;
        int reachZ = settings.planar ? 0 : steps ? stepZ : 1;
        NavChunk target = snapshot.resolve(goal);
        int n = settings.chunkSize, cell = target.cell(NavSnapshot.node(goal));
        int x = target.key.x * n + cell % n, y = target.key.y * n + cell / n % n;
        int z = settings.planar ? 0 : target.key.z * n + cell / (n * n);
        double best = Double.POSITIVE_INFINITY;
        int inspected = 0;
        // Every route into the goal must pay one of these incoming edge costs. This remains
        // a lower bound when clearance, bend and kinematic checks later remove some edges.
        for (int dz = -reachZ; dz <= reachZ; dz++) {
            for (int dy = -reachY; dy <= reachY; dy++) for (int dx = -reachX; dx <= reachX; dx++) {
                if (++inspected > settings.maxNearestNodes) return 0;
                boolean extended = Math.abs(dx) > 1 || Math.abs(dy) > 1 || Math.abs(dz) > 1;
                int gx = x + dx, gy = y + dy, gz = z + dz;
                NavChunk chunk = snapshot.chunk(
                    Math.floorDiv(gx, n),
                    Math.floorDiv(gy, n),
                    settings.planar ? 0 : Math.floorDiv(gz, n)
                );
                if (chunk == null) continue;
                for (int face = 0; face < 7; face++) {
                    int node = chunk.nodeAt(
                        Math.floorMod(gx, n),
                        Math.floorMod(gy, n),
                        settings.planar ? 0 : Math.floorMod(gz, n),
                        face
                    );
                    if (node < 0) continue;
                    for (int choice = 0; choice < (face == 6 ? 1 : 2); choice++) {
                        NavMode mode = face == 6 ? NavMode.FLY : choice == 0 ? NavMode.WALK : NavMode.CLIMB;
                        if (!view.allows(mode) || view.getSpeed(mode) <= 0 || extended && mode != NavMode.WALK) continue;
                        long ref = NavSnapshot.reference(chunk, node, mode);
                        if (ref == goal) continue;
                        pose(ref, nearPoint, nearNormal);
                        float distance = nearPoint.distance(goalPoint);
                        float ascent = distance == 0
                            ? 0
                            : (
                                (goalPoint.x - nearPoint.x) *
                                view.up.x +
                                (goalPoint.y - nearPoint.y) *
                                view.up.y +
                                (goalPoint.z - nearPoint.z) *
                                view.up.z
                            ) /
                            distance;
                        float difficulty = difficulty(goal, ascent);
                        if (Float.isFinite(difficulty)) best = Math.min(best, (double) distance * difficulty);
                    }
                }
            }
        }
        return Double.isFinite(best) ? best : 0;
    }

    boolean needsStep(long from, long to) {
        if (view.planeSupport || NavSnapshot.mode(from) != NavMode.WALK || NavSnapshot.mode(to) != NavMode.WALK) return false;
        NavChunk a = snapshot.resolve(from), b = snapshot.resolve(to);
        if (a == null || b == null) return false;
        if (stepLift(from, to) > settings.cellSize * 0.0001f) return true;
        a.normal(NavSnapshot.node(from), nearNormal);
        b.normal(NavSnapshot.node(to), arcPoint);
        // An upright capsule also needs a raised crossing at a convex slope/landing seam.
        // Interpolating its endpoint centers can cut through the crest even on continuous terrain.
        if (nearNormal.dot(view.up) < 0.995f || arcPoint.dot(view.up) < 0.995f) {
            if (nearNormal.dot(arcPoint) >= 0.995f) return false;
        }
        a.point(NavSnapshot.node(from), nearPoint);
        b.point(NavSnapshot.node(to), arcPoint);
        return (
            Math.abs(
                (arcPoint.x - nearPoint.x) *
                view.up.x +
                (arcPoint.y - nearPoint.y) *
                view.up.y +
                (arcPoint.z - nearPoint.z) *
                view.up.z
            ) >
            settings.cellSize *
            0.01f
        );
    }

    float stepLift(long from, long to) {
        if (view.planeSupport || NavSnapshot.mode(from) != NavMode.WALK || NavSnapshot.mode(to) != NavMode.WALK) return 0;
        NavChunk a = snapshot.resolve(from), b = snapshot.resolve(to);
        if (a == null || b == null) return 0;
        try (TempVars vars = TempVars.get()) {
            Vector3f pa = a.point(NavSnapshot.node(from), vars.vect1), pb = b.point(NavSnapshot.node(to), vars.vect2);
            Vector3f na = a.normal(NavSnapshot.node(from), vars.vect3), nb = b.normal(NavSnapshot.node(to), vars.vect4);
            float ca = na.dot(view.up), cb = nb.dot(view.up);
            if (ca <= 0.001f || cb <= 0.001f || na.dot(nb) > 0.995f) return 0;
            float ha = pa.dot(view.up), hb = pb.dot(view.up);
            Vector3f horizontal = vars.vect5.set(pb).subtractLocal(pa);
            horizontal.addLocal(-view.up.x * (hb - ha), -view.up.y * (hb - ha), -view.up.z * (hb - ha));
            float riseA = -na.dot(horizontal) / ca, riseB = -nb.dot(horizontal) / cb;
            float denominator = riseA - riseB;
            if (denominator <= 0.000001f) return 0; // A valley needs no extra crest clearance.
            float t = (hb - ha - riseB) / denominator;
            if (t <= 0 || t >= 1) return 0;
            float apex = ha + riseA * t + view.height * 0.5f;
            float bodyA = ha + view.height * 0.5f + view.radius * (1 / ca - 1);
            float bodyB = hb + view.height * 0.5f + view.radius * (1 / cb - 1);
            return Math.max(0, apex - Math.max(bodyA, bodyB));
        }
    }

    private boolean collectGeometry(Vector3f from, Vector3f to, float padding) {
        Arrays.fill(localSources, 0, sourceCount, null);
        sourceCount = 0;
        settings.toLocal(from, boundsFrom);
        settings.toLocal(to, boundsTo);
        float span = settings.cellSize * settings.chunkSize;
        int x0 = (int) Math.floor((Math.min(boundsFrom.x, boundsTo.x) - padding) / span), x1 = (int) Math.floor(
            (Math.max(boundsFrom.x, boundsTo.x) + padding) / span
        );
        int y0 = (int) Math.floor((Math.min(boundsFrom.y, boundsTo.y) - padding) / span), y1 = (int) Math.floor(
            (Math.max(boundsFrom.y, boundsTo.y) + padding) / span
        );
        int z0 = settings.planar ? 0 : (int) Math.floor((Math.min(boundsFrom.z, boundsTo.z) - padding) / span);
        int z1 = settings.planar ? 0 : (int) Math.floor((Math.max(boundsFrom.z, boundsTo.z) + padding) / span);
        if ((x1 - (long) x0 + 1) * (y1 - (long) y0 + 1) * (z1 - (long) z0 + 1) > settings.localChecks) return false;
        for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            NavChunk chunk = snapshot.chunk(x, y, z);
            if (chunk == null) continue;
            for (int source = 0; source < chunk.geometry.length + snapshot.obstacles.length; source++) {
                NavGeometry geometry = source < chunk.geometry.length ? chunk.geometry[source] : snapshot.obstacles[source - chunk.geometry.length];
                if (source < chunk.geometry.length && (geometry.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) continue;
                if (view.ignoredMovableObstacle != 0 && view.ignoredMovableObstacle == geometry.movableObstacleId) continue;
                if (
                    !geometry.overlaps(
                        Math.min(from.x, to.x) - padding,
                        Math.min(from.y, to.y) - padding,
                        Math.min(from.z, to.z) - padding,
                        Math.max(from.x, to.x) + padding,
                        Math.max(from.y, to.y) + padding,
                        Math.max(from.z, to.z) + padding
                    )
                ) continue;
                boolean found = false;
                for (int i = 0; i < sourceCount; i++) if (localSources[i] == geometry) {
                    found = true;
                    break;
                }
                if (found) continue;
                if (sourceCount == localSources.length) return false;
                localSources[sourceCount++] = geometry;
            }
        }
        return true;
    }

    boolean regionAllowed(NavMode mode, Vector3f position, Vector3f axis, int area, float ascent, float difficultyOffset) {
        if (!collectGeometry(position, position, 0)) return false;
        for (int s = 0; s < sourceCount; s++) {
            NavGeometry geometry = localSources[s];
            if (
                geometry.type == NavGeometry.AREA &&
                geometry.overlaps(position.x, position.y, position.z, position.x, position.y, position.z)
            ) area = geometry.area;
        }
        float inclination = mode == NavMode.FLY ? 0 : FastMath.acos(FastMath.clamp(axis.dot(view.up), -1, 1));
        float difficulty = view.evaluate(mode, area, inclination, ascent) + difficultyOffset;
        return Float.isFinite(difficulty) && difficulty <= view.cutoff;
    }

    boolean straightAllowed(long fromRef, long toRef, Vector3f from, Vector3f to, Vector3f up) {
        return followingAllowed(fromRef, toRef, from, to, up, false);
    }

    boolean preciseAllowed(long ref, Vector3f from, Vector3f to, Vector3f up) {
        return followingAllowed(ref, ref, from, to, up, true);
    }

    private boolean followingAllowed(long fromRef, long toRef, Vector3f from, Vector3f to, Vector3f up, boolean precise) {
        NavChunk a = snapshot.resolve(fromRef), b = snapshot.resolve(toRef);
        if (a == null || b == null || NavSnapshot.mode(fromRef) != NavSnapshot.mode(toRef)) return false;
        NavMode mode = NavSnapshot.mode(fromRef);
        try (TempVars vars = TempVars.get()) {
            Vector3f support = a.normal(NavSnapshot.node(fromRef), vars.vect1);
            b.normal(NavSnapshot.node(toRef), vars.vect2);
            boolean plane = view.planeSupport;
            if (!precise && mode != NavMode.FLY && !plane && support.dot(vars.vect2) < 0.99999f) return false;
            Vector3f axis = vars.vect2.set(mode == NavMode.CLIMB ? up : view.up);
            if (!precise && mode == NavMode.CLIMB && axis.dot(support) < 0.99999f) return false;
            float length = from.distance(to);
            float ascent = length == 0
                ? 0
                : ((to.x - from.x) * view.up.x + (to.y - from.y) * view.up.y + (to.z - from.z) * view.up.z) / length;
            float maximum = difficulty(fromRef, ascent);
            if (!Float.isFinite(maximum) || !Float.isFinite(difficulty(toRef, ascent))) return false;
            if (
                !segmentAllowed(fromRef, toRef, from, to, axis, axis, ascent) ||
                !NavCollision.sweep(snapshot, view, from, to, axis, axis, settings.localChecks)
            ) return false;
            if (!collectGeometry(from, to, view.height * 0.5f + view.radius)) return false;
            float inclination = mode == NavMode.FLY || plane ? 0 : FastMath.acos(FastMath.clamp(support.dot(view.up), -1, 1));
            float offset = view.height * 0.5f;
            if (mode == NavMode.WALK) offset += view.radius * (1 / Math.max(support.dot(view.up), 0.001f) - 1);
            Vector3f footFrom = vars.vect3.set(from), footTo = vars.vect4.set(to);
            float supportPlane = 0, supportCosine = support.dot(axis);
            if (mode != NavMode.FLY && !plane) {
                footFrom.addLocal(axis.mult(-offset, vars.vect5));
                footTo.addLocal(vars.vect5);
                supportPlane = support.dot(a.point(NavSnapshot.node(fromRef), vars.vect5));
            }
            // A visible shortcut must preserve a cheap detour, even when expensive areas are admitted.
            for (int i = 0; i < sourceCount; i++) {
                NavGeometry geometry = localSources[i];
                if (geometry.type != NavGeometry.AREA) continue;
                vars.vect5.set(footFrom);
                vars.vect6.set(footTo);
                if (mode != NavMode.FLY && !plane) {
                    // Test annotations at the support surface, including with a controller skin.
                    float first = (support.dot(footFrom) - supportPlane) / supportCosine;
                    float last = (support.dot(footTo) - supportPlane) / supportCosine;
                    vars.vect5.addLocal(-axis.x * first, -axis.y * first, -axis.z * first);
                    vars.vect6.addLocal(-axis.x * last, -axis.y * last, -axis.z * last);
                }
                if (
                    NavMath.segmentBox(vars.vect5, vars.vect6, geometry) >= 0 &&
                    !(view.evaluate(mode, geometry.area, inclination, ascent) <= maximum + 1e-6f)
                ) return false;
            }
            if (precise) return mode != NavMode.WALK || supported(from, to, ascent, false);
            if (mode == NavMode.FLY || plane) return true;
            // Controllers commonly keep a small skin above support. Coverage still clips
            // horizontal boundaries exactly, so this does not bridge a gap between faces.
            float tolerance = settings.cellSize * 0.02f;
            int intervals = 0, inspected = 0;
            for (int s = 0; s < sourceCount; s++) {
                NavGeometry geometry = localSources[s];
                int flag = mode == NavMode.CLIMB ? NavGeometry.CLIMBABLE : NavGeometry.WALKABLE;
                if (settings.planar && !NavMath.crossesPlane(settings, geometry)) continue;
                if (
                    (geometry.flags & flag) == 0 || geometry.type == NavGeometry.AREA || geometry.type == NavGeometry.SPHERE
                ) continue;
                if (!(view.evaluate(mode, geometry.area, inclination, ascent) <= maximum + 1e-6f)) continue;
                boolean box = geometry.type == NavGeometry.BOX;
                for (int node = 0; node < (box ? 1 : geometry.bvhNodes);) {
                    if (
                        !box &&
                        !geometry.overlapsNode(
                            node,
                            Math.min(footFrom.x, footTo.x) - tolerance,
                            Math.min(footFrom.y, footTo.y) - tolerance,
                            Math.min(footFrom.z, footTo.z) - tolerance,
                            Math.max(footFrom.x, footTo.x) + tolerance,
                            Math.max(footFrom.y, footTo.y) + tolerance,
                            Math.max(footFrom.z, footTo.z) + tolerance
                        )
                    ) {
                        node = geometry.skipNode(node);
                        continue;
                    }
                    int first = box ? 0 : geometry.bvh.getInt(node * 32 + 24);
                    int count = box ? 6 : geometry.bvh.getInt(node * 32 + 28);
                    node++;
                    for (int j = 0; j < count; j++) {
                        if (++inspected > settings.localChecks) return false;
                        int face = box ? j : geometry.triangleOrder.get(first + j);
                        if (
                            !NavMath.supportInterval(geometry, face, footFrom, footTo, support, tolerance, vars.vect6)
                        ) continue;
                        supportIntervals[intervals++] =
                            ((long) Float.floatToIntBits(vars.vect6.x) << 32) |
                            (Float.floatToIntBits(vars.vect6.y) & 0xffffffffL);
                    }
                }
            }
            Arrays.sort(supportIntervals, 0, intervals);
            float covered = 0;
            for (int i = 0; i < intervals; i++) {
                float start = Float.intBitsToFloat((int) (supportIntervals[i] >>> 32));
                if (start > covered + 1e-6f) return false;
                covered = Math.max(covered, Float.intBitsToFloat((int) supportIntervals[i]));
                if (covered >= 1 - 1e-6f) return true;
            }
            return false;
        }
    }

    // Walk a continuous height field instead of treating every changed triangle normal as a step.
    // Exact projected face intervals reject even gaps narrower than the collision sample spacing.
    boolean surfaceAllowed(long fromRef, long toRef, Vector3f from, Vector3f to, Vector3f store) {
        return followSurface(fromRef, toRef, from, to, store, true)
                && (view.supportMargin == 0 || supported(from, to, from.distance(to) == 0 ? 0 :
                        ((to.x - from.x) * view.up.x + (to.y - from.y) * view.up.y + (to.z - from.z) * view.up.z) / from.distance(to), false));
    }

    boolean surfaceMoveAllowed(long fromRef, long toRef, Vector3f from, Vector3f to, Vector3f store) {
        return followSurface(fromRef, toRef, from, to, store, false)
                && (view.supportMargin == 0 || supported(from, to, from.distance(to) == 0 ? 0 :
                        ((to.x - from.x) * view.up.x + (to.y - from.y) * view.up.y + (to.z - from.z) * view.up.z) / from.distance(to), false));
    }

    private boolean followSurface(long fromRef, long toRef, Vector3f from, Vector3f to, Vector3f store, boolean matchTarget) {
        if (
            view.planeSupport ||
            NavSnapshot.mode(fromRef) != NavMode.WALK ||
            NavSnapshot.mode(toRef) != NavMode.WALK ||
            snapshot.resolve(fromRef) == null ||
            snapshot.resolve(toRef) == null
        ) return false;
        float length = from.distance(to);
        float spacing = Math.min(settings.cellSize * 0.1f, Math.max(view.radius * 0.25f, 0.001f));
        int samples = Math.max(1, (int) Math.ceil(length / spacing));
        if (
            samples > settings.localChecks || !collectGeometry(from, to, view.height * 0.5f + view.stepHeight + view.radius)
        ) return false;
        float ascent = length == 0
            ? 0
            : ((to.x - from.x) * view.up.x + (to.y - from.y) * view.up.y + (to.z - from.z) * view.up.z) / length;
        float maximum = Math.max(difficulty(fromRef, ascent), difficulty(toRef, ascent));
        if (!Float.isFinite(maximum)) return false;
        int areaFrom = snapshot.resolve(fromRef).area(NavSnapshot.node(fromRef));
        int areaTo = snapshot.resolve(toRef).area(NavSnapshot.node(toRef));
        int intervals = collectSurface(from, to, areaFrom, areaTo);
        if (intervals <= 0) return false;
        try (TempVars vars = TempVars.get()) {
            Arrays.sort(supportIntervals, 0, intervals);
            float covered = 0, height = from.dot(view.up) - view.height * 0.5f, tolerance = settings.cellSize * 0.01f;
            int routeCount = 0;
            float vertical = to.dot(view.up) - from.dot(view.up);
            float horizontalSquared = Math.max(0, from.distanceSquared(to) - vertical * vertical);
            double cost = 0;
            while (covered < 1 - 1e-6f) {
                int chosen = -1;
                float end = covered, difference = Float.POSITIVE_INFINITY;
                for (int i = 0; i < intervals; i++) {
                    float start = Float.intBitsToFloat((int) (supportIntervals[i] >>> 32));
                    if (start > covered + 1e-6f) break;
                    int face = (int) supportIntervals[i], p = face * 4;
                    float h = surfacePlanes[p] + (surfacePlanes[p + 1] - surfacePlanes[p]) * covered;
                    float delta = Math.abs(h - height);
                    float slack = routeCount == 0
                        ? view.stepHeight + view.radius * (1 / Math.max(surfacePlanes[p + 2], 0.001f) - 1) + tolerance
                        : tolerance;
                    if (
                        surfacePlanes[p + 3] > covered + 1e-6f &&
                        delta <= slack &&
                        (
                            chosen < 0 ||
                            routeCount == 0 &&
                            delta < difference - 1e-6f ||
                            delta <= difference + 1e-6f &&
                            surfacePlanes[p + 3] > end
                        )
                    ) {
                        chosen = face;
                        end = surfacePlanes[p + 3];
                        difference = delta;
                    }
                }
                if (chosen < 0 || routeCount == surfaceRoute.length) return false;
                surfaceRoute[routeCount++] = chosen;
                float rise = surfacePlanes[chosen * 4 + 1] - surfacePlanes[chosen * 4];
                cost += Math.sqrt(horizontalSquared + rise * rise) * (end - covered) * surfacePrices[chosen];
                covered = end;
                height = surfacePlanes[chosen * 4] + (surfacePlanes[chosen * 4 + 1] - surfacePlanes[chosen * 4]) * covered;
            }
            if (matchTarget) {
                // Overlapping storeys share horizontal coverage, but a shortcut must
                // finish on its destination's support sheet, not a floor below it.
                NavChunk target = snapshot.resolve(toRef);
                target.point(NavSnapshot.node(toRef), vars.vect1);
                target.normal(NavSnapshot.node(toRef), vars.vect2);
                float cosine = vars.vect2.dot(view.up);
                if (cosine <= 0) return false;
                float expected = to.dot(view.up) + (vars.vect2.dot(vars.vect1) - vars.vect2.dot(to)) / cosine;
                if (Math.abs(height - expected) > tolerance) return false;
            }
            Vector3f previous = vars.vect3.set(from), point = vars.vect4;
            int first = surfaceRoute[0] * 4;
            vars.vect6.set(from);
            float startLift = surfacePlanes[first] + view.height * 0.5f - from.dot(view.up);
            vars.vect6.addLocal(view.up.x * startLift, view.up.y * startLift, view.up.z * startLift);
            if (!surfaceContact(vars.vect6, surfacePlanes[first + 2])) return false;
            float supportStart = vars.vect6.dot(view.up);
            int routeIndex = 0;
            for (int i = 1; i <= samples; i++) {
                float t = (float) i / samples;
                while (routeIndex + 1 < routeCount && surfacePlanes[surfaceRoute[routeIndex] * 4 + 3] < t) routeIndex++;
                int p = surfaceRoute[routeIndex] * 4;
                point.interpolateLocal(from, to, t);
                float center = surfacePlanes[p] + (surfacePlanes[p + 1] - surfacePlanes[p]) * t + view.height * 0.5f;
                float lift = center - point.dot(view.up);
                point.addLocal(view.up.x * lift, view.up.y * lift, view.up.z * lift);
                if (!surfaceContact(point, surfacePlanes[p + 2])) return false;
                boolean clear = false;
                for (int attempt = 0; attempt < 8; attempt++) {
                    if (NavCollision.sweep(snapshot, view, previous, point, view.up, view.up, settings.localChecks)) {
                        clear = true;
                        break;
                    }
                    point.addLocal(
                        view.up.x * settings.cellSize * 0.01f,
                        view.up.y * settings.cellSize * 0.01f,
                        view.up.z * settings.cellSize * 0.01f
                    );
                }
                if (!clear) return false;
                previous.set(point);
            }
            surfaceRise = point.dot(view.up) - supportStart;
            surfaceCost = cost;
            if (store != null) store.set(point);
            return true;
        }
    }

    private int collectSurface(Vector3f from, Vector3f to, int areaFrom, int areaTo) {
        try (TempVars vars = TempVars.get()) {
            Vector3f footFrom = vars.vect1
                .set(from)
                .addLocal(-view.up.x * view.height * 0.5f, -view.up.y * view.height * 0.5f, -view.up.z * view.height * 0.5f);
            Vector3f footTo = vars.vect2
                .set(to)
                .addLocal(-view.up.x * view.height * 0.5f, -view.up.y * view.height * 0.5f, -view.up.z * view.height * 0.5f);
            float padX = view.radius + view.stepHeight * Math.abs(view.up.x);
            float padY = view.radius + view.stepHeight * Math.abs(view.up.y);
            float padZ = view.radius + view.stepHeight * Math.abs(view.up.z);
            int intervals = 0, inspected = 0;
            for (int g = 0; g < sourceCount; g++) {
                NavGeometry geometry = localSources[g];
                if (
                    (geometry.flags & NavGeometry.WALKABLE) == 0 ||
                    geometry.type == NavGeometry.AREA ||
                    geometry.type == NavGeometry.SPHERE
                ) continue;
                boolean box = geometry.type == NavGeometry.BOX;
                for (int node = 0; node < (box ? 1 : geometry.bvhNodes);) {
                    if (
                        !box &&
                        !geometry.overlapsNode(
                            node,
                            Math.min(footFrom.x, footTo.x) - padX,
                            Math.min(footFrom.y, footTo.y) - padY,
                            Math.min(footFrom.z, footTo.z) - padZ,
                            Math.max(footFrom.x, footTo.x) + padX,
                            Math.max(footFrom.y, footTo.y) + padY,
                            Math.max(footFrom.z, footTo.z) + padZ
                        )
                    ) {
                        node = geometry.skipNode(node);
                        continue;
                    }
                    int first = box ? 0 : geometry.bvh.getInt(node * 32 + 24), count = box
                        ? 6
                        : geometry.bvh.getInt(node * 32 + 28);
                    node++;
                    for (int j = 0; j < count; j++) {
                        if (++inspected > settings.localChecks) return -1;
                        int face = box ? j : geometry.triangleOrder.get(first + j);
                        Vector3f normal = vars.vect3;
                        float plane;
                        if (box) {
                            normal.set(NavMath.DX[face], NavMath.DY[face], NavMath.DZ[face]);
                            plane =
                                face == 0
                                    ? -geometry.minX
                                    : face == 1
                                        ? geometry.maxX
                                        : face == 2
                                            ? -geometry.minY
                                            : face == 3 ? geometry.maxY : face == 4 ? -geometry.minZ : geometry.maxZ;
                        } else {
                            NavMath.triangleNormal(geometry.triangles, face * 9, normal);
                            if (normal.dot(view.up) < 0) normal.negateLocal();
                            plane =
                                normal.x *
                                geometry.triangles.get(face * 9) +
                                normal.y *
                                geometry.triangles.get(face * 9 + 1) +
                                normal.z *
                                geometry.triangles.get(face * 9 + 2);
                        }
                        float cosine = normal.dot(view.up);
                        if (cosine <= 0) continue;
                        Vector3f a = vars.vect4.set(footFrom), b = vars.vect5.set(footTo);
                        float da = (plane - normal.dot(a)) / cosine, db = (plane - normal.dot(b)) / cosine;
                        a.addLocal(view.up.x * da, view.up.y * da, view.up.z * da);
                        b.addLocal(view.up.x * db, view.up.y * db, view.up.z * db);
                        float distance = a.distance(b);
                        float surfaceAscent = distance == 0 ? 0 : (b.dot(view.up) - a.dot(view.up)) / distance;
                        float inclination = FastMath.acos(FastMath.clamp(cosine, -1, 1));
                        float price = view.evaluate(NavMode.WALK, geometry.area, inclination, surfaceAscent);
                        float maximum = Math.max(
                            view.evaluate(NavMode.WALK, areaFrom, inclination, surfaceAscent),
                            view.evaluate(NavMode.WALK, areaTo, inclination, surfaceAscent)
                        );
                        // Slope difficulty belongs in the route's integrated cost. It must
                        // not erase an admitted triangle from continuous support coverage.
                        if (!Float.isFinite(price) || !Float.isFinite(maximum) || price > maximum + 1e-6f) continue;
                        if (
                            !NavMath.supportInterval(geometry, face, a, b, normal, settings.cellSize * 0.01f, vars.vect6)
                        ) continue;
                        // Preserve forbidden annotations and the route's cheap-area detours at surface height.
                        boolean admitted = true;
                        for (int r = 0; r < sourceCount; r++) {
                            NavGeometry area = localSources[r];
                            if (
                                area.type == NavGeometry.AREA &&
                                NavMath.segmentBox(a, b, area) >= 0 &&
                                !(
                                    view.evaluate(
                                        NavMode.WALK,
                                        area.area,
                                        FastMath.acos(FastMath.clamp(cosine, -1, 1)),
                                        surfaceAscent
                                    ) <=
                                    maximum +
                                    1e-6f
                                )
                            ) {
                                admitted = false;
                                break;
                            }
                        }
                        if (!admitted) continue;
                        int p = intervals * 4;
                        surfacePlanes[p] = a.dot(view.up);
                        surfacePlanes[p + 1] = b.dot(view.up);
                        surfacePlanes[p + 2] = cosine;
                        surfacePlanes[p + 3] = vars.vect6.y;
                        surfacePrices[intervals] = price;
                        supportIntervals[intervals] = ((long) Float.floatToIntBits(vars.vect6.x) << 32) | intervals;
                        intervals++;
                    }
                }
            }
            return intervals;
        }
    }

    // Capsule contact stays continuous where finite triangles meet; their infinite planes do not.
    private boolean surfaceContact(Vector3f point, float cosine) {
        try (TempVars vars = TempVars.get()) {
            Vector3f scratch = vars.vect1;
            if (!NavCollision.clear(snapshot, view, point, view.up)) {
                float low = 0, high = Math.max(settings.cellSize * 0.01f, view.radius * (1 / cosine - 1));
                boolean found = false;
                for (int attempt = 0; attempt < 8; attempt++) {
                    scratch.set(point).addLocal(view.up.x * high, view.up.y * high, view.up.z * high);
                    if (NavCollision.clear(snapshot, view, scratch, view.up)) {
                        found = true;
                        break;
                    }
                    high += view.radius * 0.125f;
                }
                if (!found) return false;
                for (int iteration = 0; iteration < 8; iteration++) {
                    float middle = (low + high) * 0.5f;
                    scratch.set(point).addLocal(view.up.x * middle, view.up.y * middle, view.up.z * middle);
                    if (NavCollision.clear(snapshot, view, scratch, view.up)) high = middle; else low = middle;
                }
                high += settings.cellSize * 0.002f;
                point.addLocal(view.up.x * high, view.up.y * high, view.up.z * high);
            }
            return true;
        }
    }

    boolean segmentAllowed(
        long fromRef,
        long toRef,
        Vector3f from,
        Vector3f to,
        Vector3f fromAxis,
        Vector3f toAxis,
        float ascent
    ) {
        NavChunk a = snapshot.resolve(fromRef), b = snapshot.resolve(toRef);
        if (a == null || b == null) return false;
        if (!collectGeometry(from, to, view.planeSupport ? view.radius : view.height * 0.5f)) return false;
        for (int s = 0; s < sourceCount; s++) {
            NavGeometry geometry = localSources[s];
            if (geometry.type == NavGeometry.AREA) {
                for (int side = 0; side < 2; side++) {
                    NavMode mode = NavSnapshot.mode(side == 0 ? fromRef : toRef);
                    nearPoint.set(from);
                    arcPrevious.set(to);
                    NavChunk sideChunk = side == 0 ? a : b;
                    long sideRef = side == 0 ? fromRef : toRef;
                    if (mode != NavMode.FLY && !(settings.planar && sideChunk.face(NavSnapshot.node(sideRef)) == 5)) {
                        float footOffset = view.planeSupport ? view.radius : view.height * 0.5f;
                        nearPoint.addLocal(-fromAxis.x * footOffset, -fromAxis.y * footOffset, -fromAxis.z * footOffset);
                        arcPrevious.addLocal(-toAxis.x * footOffset, -toAxis.y * footOffset, -toAxis.z * footOffset);
                    }
                    float hit = NavMath.segmentBox(nearPoint, arcPrevious, geometry);
                    if (hit < 0) continue;
                    nearNormal.interpolateLocal(fromAxis, toAxis, hit).normalizeLocal();
                    float inclination = mode == NavMode.FLY ? 0 : FastMath.acos(FastMath.clamp(nearNormal.dot(view.up), -1, 1));
                    if (!Float.isFinite(view.evaluate(mode, geometry.area, inclination, ascent))) return false;
                }
            }
        }
        int samples = Math.max(1, (int) Math.ceil(from.distance(to) / (settings.cellSize * 0.2f)));
        if (samples > settings.localChecks) return false;
        for (int i = 0; i <= samples; i++) {
            float t = (float) i / samples;
            arcPoint.interpolateLocal(from, to, t);
            nearNormal.interpolateLocal(fromAxis, toAxis, t).normalizeLocal();
            int area = t < 0.5f ? a.area(NavSnapshot.node(fromRef)) : b.area(NavSnapshot.node(toRef));
            if (
                !regionAllowed(NavSnapshot.mode(fromRef), arcPoint, nearNormal, area, ascent, 0) ||
                !regionAllowed(NavSnapshot.mode(toRef), arcPoint, nearNormal, area, ascent, 0)
            ) return false;
        }
        return true;
    }

    boolean supported(Vector3f from, Vector3f to, float ascent, boolean allowHigherSupport) {
        if (view.supportMargin == 0 || view.planeSupport) return supportedCenter(from, to, ascent, allowHigherSupport);
        marginStart.set(from); marginEnd.set(to);
        if (!supportedCenter(marginStart, marginEnd, ascent, allowHigherSupport)) return false;
        // Eight offset lanes reject precarious edge routes without changing the shape's clearance radius.
        try (TempVars vars = TempVars.get()) {
            Vector3f a = vars.vect7.set(Math.abs(view.up.y) > .9f ? Vector3f.UNIT_X : Vector3f.UNIT_Y);
            a.cross(view.up, a).normalizeLocal();
            Vector3f b = view.up.cross(a, vars.vect8);
            for (int i = 0; i < 8; i++) {
                float angle = i * FastMath.QUARTER_PI;
                marginAxis.set(a).multLocal(FastMath.cos(angle)).addLocal(b.x * FastMath.sin(angle),
                        b.y * FastMath.sin(angle), b.z * FastMath.sin(angle)).multLocal(view.supportMargin);
                marginFrom.set(marginStart).addLocal(marginAxis); marginTo.set(marginEnd).addLocal(marginAxis);
                if (!supportedCenter(marginFrom, marginTo, ascent, allowHigherSupport)) return false;
            }
        }
        return true;
    }

    private boolean supportedCenter(Vector3f from, Vector3f to, float ascent, boolean allowHigherSupport) {
        if (view.planeSupport) return true;
        float footOffset = view.height * 0.5f;
        float cosine = FastMath.cos(Math.min(view.maxSlope, 89 * FastMath.DEG_TO_RAD));
        float reach =
            (allowHigherSupport ? Math.max(view.stepHeight, settings.cellSize) : view.stepHeight) +
            view.radius *
            (1 / cosine - 1) +
            settings.cellSize *
            0.01f;
        boundsFrom.set(from).addLocal(-view.up.x * footOffset, -view.up.y * footOffset, -view.up.z * footOffset);
        boundsTo.set(to).addLocal(-view.up.x * footOffset, -view.up.y * footOffset, -view.up.z * footOffset);
        // Separate stores prevent the collection's plane conversion from changing its input bounds.
        nearPoint.set(boundsFrom);
        arcPrevious.set(boundsTo);
        if (!collectGeometry(nearPoint, arcPrevious, reach)) return false;
        int samples = Math.max(1, (int) Math.ceil(from.distance(to) / (settings.cellSize * 0.2f)));
        if (samples > settings.localChecks) return false;
        for (int i = 0; i <= samples; i++) {
            nearPoint.interpolateLocal(from, to, (float) i / samples);
            nearPoint.addLocal(-view.up.x * footOffset, -view.up.y * footOffset, -view.up.z * footOffset);
            boolean found = false;
            for (int s = 0; s < sourceCount; s++) {
                NavGeometry geometry = localSources[s];
                if ((geometry.flags & NavGeometry.WALKABLE) == 0) continue;
                if (settings.planar && !NavMath.crossesPlane(settings, geometry)) continue;
                if (!NavMath.support(geometry, nearPoint, view.up, reach, arcPoint, nearNormal)) continue;
                if (
                    !allowHigherSupport &&
                    (arcPoint.x - nearPoint.x) *
                    view.up.x +
                    (arcPoint.y - nearPoint.y) *
                    view.up.y +
                    (arcPoint.z - nearPoint.z) *
                    view.up.z >
                    Math.max(view.contactSkin, settings.cellSize * 0.01f)
                ) continue;
                float slope = FastMath.acos(FastMath.clamp(nearNormal.dot(view.up), -1, 1));
                int area = geometry.area;
                for (int j = 0; j < sourceCount; j++) {
                    NavGeometry region = localSources[j];
                    if (
                        region.type == NavGeometry.AREA &&
                        region.overlaps(arcPoint.x, arcPoint.y, arcPoint.z, arcPoint.x, arcPoint.y, arcPoint.z)
                    ) area = region.area;
                }
                if (!Float.isFinite(view.evaluate(NavMode.WALK, area, slope, ascent))) return false;
                found = true;
                break;
            }
            if (!found) return false;
        }
        return true;
    }

    private double timeCost(NavMode from, NavMode to, float length, Vector3f velocity, Vector3f direction) {
        NavKinematics dynamics = view.kinematics;
        if (dynamics == null) return 0;
        float speed = view.getSpeed(to);
        if (speed <= 0) return Double.POSITIVE_INFINITY;
        double time = length / speed;
        if (parents[current] >= 0) {
            long parent = refs[parents[current]];
            pose(parent, nearPoint, nearNormal);
            nearPoint.set(p0.x - nearPoint.x, p0.y - nearPoint.y, p0.z - nearPoint.z);
        } else nearPoint.set(initialDirection);
        float incoming = nearPoint.length();
        float motionSpeed = parents[current] < 0 ? initialSpeed : view.getSpeed(from);
        if (incoming > 1e-6f && length > 1e-6f) {
            float angle = FastMath.acos(FastMath.clamp(nearPoint.dot(direction) / (incoming * length), -1, 1));
            if (Float.isFinite(dynamics.turnRate)) {
                float turnTime = angle / dynamics.turnRate;
                // A finite turn rate changes feasibility even when travel-time weighting is disabled.
                if (
                    angle > FastMath.HALF_PI && length < motionSpeed / dynamics.turnRate * angle
                ) return Double.POSITIVE_INFINITY;
                time += turnTime;
            }
            if (parents[current] < 0 && Float.isFinite(dynamics.braking) && velocity.dot(direction) < 0) {
                time += motionSpeed / dynamics.braking;
            }
        }
        if (Float.isFinite(dynamics.acceleration) && parents[current] < 0 && motionSpeed < speed) time +=
            (speed - motionSpeed) / dynamics.acceleration;
        return dynamics.timeWeight * time;
    }

    private boolean arcClear(NavLink link, Vector3f from, Vector3f to) {
        float length = from.distance(to) + link.arcHeight * 2;
        int segments = Math.max(2, (int) Math.ceil(length / (settings.cellSize * 0.25f)));
        if (segments > settings.localChecks) return false;
        arcPrevious.set(from);
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            arcPoint.interpolateLocal(from, to, t);
            float rise = 4 * link.arcHeight * t * (1 - t);
            arcPoint.addLocal(view.up.x * rise, view.up.y * rise, view.up.z * rise);
            if (!regionAllowed(link.mode, arcPoint, view.up, 0, 0, link.difficulty)) return false;
            if (
                !NavCollision.sweep(snapshot, view, arcPrevious, arcPoint, view.up, view.up, settings.localChecks)
            ) return false;
            arcPrevious.set(arcPoint);
        }
        return true;
    }

    private void add(long ref, double cost, double distance, int parent, long link) {
        if (!Double.isFinite(cost)) return;
        int slot = (int) ((ref ^ (ref >>> 33)) * 0xff51afd7ed558ccdl) & (slots.length - 1);
        while (slots[slot] != 0 && refs[slots[slot] - 1] != ref) slot = (slot + 1) & (slots.length - 1);
        int index;
        if (slots[slot] == 0) {
            if (count == refs.length) {
                exhausted = true;
                return;
            }
            index = count++;
            slots[slot] = index + 1;
            refs[index] = ref;
            heapPositions[index] = -1;
        } else {
            index = slots[slot] - 1;
            if (cost > costs[index] || cost == costs[index] && distance >= distances[index]) return;
        }
        costs[index] = cost;
        priorities[index] = cost;
        distancePriorities[index] = distance;
        if (ref != goal) {
            pose(ref, nearPoint, nearNormal);
            // Only the estimate is rounded down; zero and positive traversal costs stay exact.
            double estimate = (double) nearPoint.distance(goalPoint) * 0.999999;
            priorities[index] += Math.max(minimumEntryCost, minimumCost * estimate);
            if (!usableLinks) distancePriorities[index] += estimate;
        }
        distances[index] = distance;
        parents[index] = parent;
        via[index] = link;
        int position = heapPositions[index];
        if (position == -2) position = -1; // Reopen an improved state when floating-point estimates differ.
        if (position < 0) {
            position = heapSize++;
            heap[position] = index;
        }
        while (position > 0) {
            int above = (position - 1) >>> 1;
            if (!before(index, heap[above])) break;
            heap[position] = heap[above];
            heapPositions[heap[position]] = position;
            position = above;
        }
        heap[position] = index;
        heapPositions[index] = position;
    }

    private boolean before(int a, int b) {
        return (
            priorities[a] < priorities[b] ||
            priorities[a] == priorities[b] &&
            (
                distancePriorities[a] < distancePriorities[b] ||
                distancePriorities[a] == distancePriorities[b] &&
                refs[a] < refs[b]
            )
        );
    }

    private int pop() {
        int result = heap[0], last = heap[--heapSize], position = 0;
        while (position * 2 + 1 < heapSize) {
            int next = position * 2 + 1;
            if (next + 1 < heapSize && before(heap[next + 1], heap[next])) next++;
            if (!before(heap[next], last)) break;
            heap[position] = heap[next];
            heapPositions[heap[position]] = position;
            position = next;
        }
        if (heapSize > 0) {
            heap[position] = last;
            heapPositions[last] = position;
        }
        heapPositions[result] = -2;
        return result;
    }

    private NavPath result(NavReason resultReason) {
        int length = 0;
        for (int i = current; i >= 0; i = parents[i]) {
            if (length == route.length) return failure(NavReason.BUDGET_EXCEEDED);
            route[length++] = i;
        }
        LongBuffer references = BufferUtils.createByteBuffer(length * Long.BYTES).asLongBuffer();
        LongBuffer links = BufferUtils.createByteBuffer(length * Long.BYTES).asLongBuffer();
        FloatBuffer points = BufferUtils.createFloatBuffer(length * 3);
        for (int i = length - 1; i >= 0; i--) {
            int index = route[i];
            pose(refs[index], p1, n1);
            references.put(refs[index]);
            links.put(via[index]);
            points.put(p1.x).put(p1.y).put(p1.z);
        }
        reason = resultReason;
        return new NavPath(view.world, view.revision, snapshot.revision, snapshot.surfaceRevision, references, links, points, reason, costs[current]);
    }

    private NavPath failure(NavReason reason) {
        this.reason = reason;
        return new NavPath(view.world, view.revision, snapshot.revision, snapshot.surfaceRevision, null, null, null, reason, Double.POSITIVE_INFINITY);
    }
}
