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
import com.jme3.util.TempVars;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared geometry and published pages. Mutation belongs to the scene/physics owner;
 * immutable snapshots permit concurrent queries without holding the mutation lock.
 */
public final class NavWorld implements AutoCloseable {

    final NavSettings settings;
    volatile NavSnapshot snapshot;
    volatile boolean closed;
    private volatile int pendingBuilds;
    private volatile Throwable failure;
    private final Executor generationExecutor;
    private final Executor planningExecutor;
    private final ExecutorService ownedGeneration;
    private final ExecutorService ownedPlanning;
    private final ConcurrentLinkedQueue<Runnable> generationQueue;
    private final ConcurrentLinkedQueue<Runnable> planningQueue;
    private final AtomicInteger planningCount = new AtomicInteger();
    private final Map<String, NavLayer> layers = new LinkedHashMap<>();
    private final Map<NavChunkKey, NavChunk> pages = new HashMap<>();
    private final Map<NavChunkKey, NavChunk> imported = new HashMap<>();
    private final Map<NavChunkKey, NavLayer> importedOwners = new HashMap<>();
    private final Set<NavChunkKey> requested = new HashSet<>();
    private final Set<NavChunkKey> dirty = new LinkedHashSet<>();
    private final Map<NavChunkKey, Long> versions = new HashMap<>();
    private final ArrayList<NavLink> links = new ArrayList<>();
    private final ThreadLocal<NavQuery> queries;
    private final ThreadLocal<byte[]> buildScratch;
    private long revision;
    private long serial;
    private long epoch;

    public NavWorld(NavSettings settings) {
        this(settings, false, null, null);
    }

    public NavWorld(NavSettings settings, Executor generationExecutor, Executor planningExecutor) {
        this(settings, false, requireExecutor(generationExecutor), requireExecutor(planningExecutor));
    }

    private static Executor requireExecutor(Executor executor) {
        if (executor == null) throw new IllegalArgumentException("Both executors are required");
        return executor;
    }

    public static NavWorld manual(NavSettings settings) {
        return new NavWorld(settings, true, null, null);
    }

    private NavWorld(NavSettings settings, boolean manual, Executor generation, Executor planning) {
        if (settings == null) throw new NullPointerException("settings");
        this.settings = settings;
        queries = ThreadLocal.withInitial(() -> new NavQuery(settings));
        int width = settings.chunkSize + 2;
        int depth = settings.planar ? 3 : width;
        buildScratch = ThreadLocal.withInitial(() -> new byte[width * width * depth]);
        generationQueue = new ConcurrentLinkedQueue<>();
        planningQueue = new ConcurrentLinkedQueue<>();
        if (!manual && generation == null && planning == null) {
            AtomicInteger number = new AtomicInteger();
            ThreadFactory factory = task -> {
                Thread thread = new Thread(task, "nge-nav-" + number.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            int workers = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
            ownedGeneration = Executors.newFixedThreadPool(workers, factory);
            ownedPlanning = Executors.newFixedThreadPool(workers, factory);
            generationExecutor = ownedGeneration;
            planningExecutor = ownedPlanning;
            for (int i = 0; i < workers; i++) ownedPlanning.execute(() -> {
                while (!closed) {
                    Runnable task = planningQueue.poll();
                    if (task == null) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else task.run();
                }
            });
        } else {
            ownedGeneration = ownedPlanning = null;
            generationExecutor = generation;
            planningExecutor = planning;
        }
        snapshot = new NavSnapshot(0, 0, new NavChunk[0], new NavGeometry[0], new NavLink[0]);
    }

    public NavSettings getSettings() {
        return settings;
    }

    /** Changes to walkable pages and links; movable collision updates have a separate world revision. */
    public long getSurfaceRevision() { return snapshot.surfaceRevision; }

    public long getRevision() {
        return snapshot.revision;
    }

    public int getLoadedChunkCount() {
        return snapshot.chunks.length;
    }

    /** Returns a snapshot of the immutable loaded chunks, for diagnostics and tooling. */
    public NavChunk[] getLoadedChunks() {
        return snapshot.chunks.clone();
    }

    public int getPendingBuildCount() {
        return pendingBuilds;
    }

    public boolean isReady() {
        return pendingBuilds == 0 && failure == null && !closed;
    }

    public Throwable getFailure() {
        return failure;
    }

    public NavView.Builder view() {
        return new NavView.Builder(this);
    }

    public synchronized NavLayer layer(String name) {
        requireOpen();
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Layer name is required");
        NavLayer layer = layers.get(name);
        if (layer == null) {
            layer = new NavLayer(this, name);
            layers.put(name, layer);
        }
        return layer;
    }

    public synchronized void load(NavChunkKey key) {
        requireOpen();
        validateKey(key);
        if (requested.add(key)) {
            dirty.add(key);
            versions.put(key, ++epoch);
            pendingBuilds++;
            scheduleBuilds();
        }
    }

    public synchronized void unload(NavChunkKey key) {
        requireOpen();
        if (requested.remove(key)) {
            dirty.remove(key);
            versions.remove(key);
            boolean pending = pages.remove(key) == null;
            imported.remove(key);
            NavLayer owner = importedOwners.remove(key);
            if (owner != null) owner.importedPages.remove(key);
            publish(false);
            if (pending) pendingBuilds--;
        }
    }

    public NavChunk getChunk(NavChunkKey key) {
        return snapshot.chunk(key.x, key.y, key.z);
    }

    public synchronized void attach(NavChunk page) {
        attach(null, page);
    }

    synchronized void attach(NavLayer owner, NavChunk page) {
        requireOpen();
        if (owner != null) owner.requireAttached();
        if (!compatible(page.settings)) throw new IllegalArgumentException("Incompatible navigation page settings");
        NavChunkKey key = page.key;
        if (requested.contains(key) && !pages.containsKey(key)) pendingBuilds--;
        requested.add(key);
        dirty.remove(key);
        versions.remove(key);
        // A new identity prevents an old route from binding to a reattached page.
        NavLayer previousOwner = importedOwners.remove(key);
        if (previousOwner != null) previousOwner.importedPages.remove(key);
        if (owner != null) {
            importedOwners.put(key, owner);
            owner.importedPages.add(key);
        }
        imported.put(key, page);
        pages.put(key, new NavChunk(key, nextSerial(), settings, page.nodes, page.cells, page.nodeCount, page.geometry));
        publish(false);
    }

    public NavLink getLink(long id) {
        for (NavLink link : snapshot.links) if (link.id == id) return link;
        return null;
    }

    public synchronized void addLink(NavLink link) {
        requireOpen();
        if (!links.contains(link)) {
            links.add(link);
            // Adding a traversal leaves existing floors and routes intact. A
            // planner discovering a jump must not cancel its own pending query.
            publish(true);
        }
    }

    public synchronized void removeLink(NavLink link) {
        requireOpen();
        if (links.remove(link)) publish(true);
    }

    /**
     * Runs at most this many queued tasks on the caller. Useful with a platform task pump.
     * A task is one tile build or one bounded search, so call outside the render loop.
     */
    public synchronized void retryBuilds() {
        requireOpen();
        failure = null;
        for (NavChunkKey key : requested) if (!pages.containsKey(key) && !versions.containsKey(key)) {
            dirty.add(key);
            versions.put(key, ++epoch);
        }
        scheduleBuilds();
    }

    public int pump(int maxJobs) {
        if (maxJobs < 0) throw new IllegalArgumentException("Negative job budget");
        int completed = 0;
        while (completed < maxJobs) {
            Runnable task = planningQueue.poll();
            if (task == null) task = generationQueue.poll();
            if (task == null) break;
            task.run();
            completed++;
        }
        return completed;
    }

    /**
     * Explicit synchronous query for tools, loading screens and worker code.
     */
    public NavPath findPath(NavView view, Vector3f start, Vector3f target) {
        return findPath(view, start, target, false);
    }

    public NavPath findClosestPath(NavView view, Vector3f start, Vector3f target) {
        return findPath(view, start, target, true);
    }

    private NavPath findPath(NavView view, Vector3f start, Vector3f target, boolean closest) {
        checkView(view);
        validatePosition(start);
        validatePosition(target);
        NavSnapshot current = snapshot;
        NavQuery query = queries.get();
        try {
            return query.search(current, view, start, target, null, Vector3f.ZERO, Vector3f.ZERO, closest);
        } finally {
            query.bind(null, null);
        }
    }

    public boolean nearestPoint(NavView view, Vector3f position, Vector3f store) {
        checkView(view);
        validatePosition(position);
        NavQuery query = queries.get();
        query.bind(snapshot, view);
        try {
            long ref = query.nearest(position, null, view.nearestDistance);
            if (ref == 0) return false;
            try (TempVars vars = TempVars.get()) {
                query.pose(ref, store, vars.vect1);
            }
            return true;
        } finally {
            query.bind(null, null);
        }
    }

    /** Returns the nearest walkable surface area, or -1 when no surface is available. */
    public int nearestArea(NavView view, Vector3f position) {
        checkView(view);
        validatePosition(position);
        NavSnapshot current = snapshot;
        NavQuery query = queries.get();
        query.bind(current, view);
        try {
            long ref = query.nearest(position, NavMode.WALK, view.nearestDistance);
            if (ref == 0) return -1;
            NavChunk chunk = current.resolve(ref);
            return chunk == null ? -1 : chunk.area(NavSnapshot.node(ref));
        } finally {
            query.bind(null, null);
        }
    }

    boolean isSegmentClear(NavView view, Vector3f from, Vector3f to) {
        checkView(view);
        validatePosition(from);
        validatePosition(to);
        return NavCollision.sweep(snapshot, view, from, to, view.up, view.up, settings.localChecks);
    }

    boolean isWalkableSegment(NavView view, Vector3f from, Vector3f to) {
        checkView(view); validatePosition(from); validatePosition(to);
        NavQuery query = queries.get();
        query.bind(snapshot, view);
        try (TempVars vars = TempVars.get()) {
            long start = query.nearest(from, NavMode.WALK, view.nearestDistance);
            long end = query.nearest(to, NavMode.WALK, view.nearestDistance);
            return start != 0 && end != 0 && query.surfaceAllowed(start, end, from, to, vars.vect1);
        } finally { query.bind(null, null); }
    }

    public boolean randomPoint(NavView view, long seed, Vector3f store) {
        checkView(view);
        NavSnapshot current = snapshot;
        NavQuery query = queries.get();
        query.bind(current, view);
        try {
            long chosen = 0, seen = 0;
            for (NavChunk chunk : current.chunks) for (int node = 0; node < chunk.nodeCount; node++) {
                for (int choice = 0; choice < (chunk.face(node) == 6 ? 1 : 2); choice++) {
                    NavMode mode = chunk.face(node) == 6 ? NavMode.FLY : choice == 0 ? NavMode.WALK : NavMode.CLIMB;
                    long ref = NavSnapshot.reference(chunk, node, mode);
                    if (!view.allows(mode) || !query.admitted(ref)) continue;
                    seed += 0x9e3779b97f4a7c15L;
                    long random = seed;
                    random = (random ^ random >>> 30) * 0xbf58476d1ce4e5b9L;
                    random = (random ^ random >>> 27) * 0x94d049bb133111ebL;
                    random ^= random >>> 31;
                    if (Long.remainderUnsigned(random, ++seen) == 0) chosen = ref;
                }
            }
            if (chosen == 0) return false;
            try (TempVars vars = TempVars.get()) {
                query.pose(chosen, store, vars.vect1);
            }
            return true;
        } finally {
            query.bind(null, null);
        }
    }

    boolean canPlan() {
        return !closed && planningCount.get() < settings.maxPendingJobs;
    }

    boolean plan(Runnable task) {
        if (closed || planningCount.incrementAndGet() > settings.maxPendingJobs) {
            if (!closed) planningCount.decrementAndGet();
            return false;
        }
        Runnable wrapped = () -> {
            try {
                if (!closed) task.run();
            } finally {
                planningCount.decrementAndGet();
            }
        };
        planningQueue.offer(wrapped);
        return true;
    }

    /**
     * Dispatches queued planning to supplied platform executors. Call from the task scheduler.
     * The guidance call only enqueues, even if the supplied executor executes tasks inline.
     */
    public int dispatch(int maxJobs) {
        if (ownedPlanning != null || planningExecutor == null) return 0;
        int submitted = 0;
        while (submitted < maxJobs) {
            Runnable task = planningQueue.poll();
            if (task == null) break;
            try {
                planningExecutor.execute(task);
                submitted++;
            } catch (RejectedExecutionException exception) {
                planningQueue.offer(task);
                break;
            }
        }
        return submitted;
    }

    NavPath search(
        NavSnapshot snapshot,
        NavView view,
        Vector3f start,
        Vector3f target,
        NavMode mode,
        Vector3f velocity,
        Vector3f forward
    ) {
        NavQuery query = queries.get();
        try {
            return query.search(snapshot, view, start, target, mode, velocity, forward, false);
        } finally {
            query.bind(null, null);
        }
    }

    synchronized NavSource add(NavLayer layer, NavGeometry geometry) {
        requireOpen();
        layer.requireAttached();
        Objects.requireNonNull(geometry, "geometry");
        include(geometry);
        NavSource source = new NavSource(layer, geometry);
        layer.sources.add(source);
        if ((geometry.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) { publish(true); scheduleBuilds(); } else invalidate(geometry, null);
        return source;
    }

    synchronized void replace(NavSource source, NavGeometry geometry) {
        requireOpen();
        source.layer.requireAttached();
        if (source.removed) throw new IllegalStateException("Source has been removed");
        Objects.requireNonNull(geometry, "geometry");
        include(geometry);
        NavGeometry previous = source.geometry;
        source.geometry = geometry;
        if ((previous.flags & geometry.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) { publish(true); scheduleBuilds(); } else invalidate(previous, geometry);
    }

    synchronized void remove(NavSource source) {
        if (closed || source.removed) return;
        if (!source.removed) {
            source.removed = true;
            source.layer.sources.remove(source);
            if ((source.geometry.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) publish(true); else invalidate(source.geometry, null);
        }
    }

    synchronized void detach(NavLayer layer) {
        if (closed) return;
        if (!layer.attached) return;
        layer.attached = false;
        layers.remove(layer.name);
        // Sources disappear together; invalidate before any rebuild can publish old data.
        for (NavSource source : layer.sources) {
            source.removed = true;
            if ((source.geometry.flags & NavGeometry.MOVABLE_OBSTACLE) != 0) publish(true); else invalidate(source.geometry, null);
        }
        layer.sources.clear();
        for (NavChunkKey key : layer.importedPages.toArray(new NavChunkKey[0])) unload(key);
    }

    private void include(NavGeometry geometry) {
        if (!settings.autoLoadPages) return;
        try (TempVars vars = TempVars.get()) {
            Vector3f min = vars.vect1.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            Vector3f max = vars.vect2.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
            for (int corner = 0; corner < 8; corner++) {
                vars.vect3.set(
                    (corner & 1) == 0 ? geometry.minX : geometry.maxX,
                    (corner & 2) == 0 ? geometry.minY : geometry.maxY,
                    (corner & 4) == 0 ? geometry.minZ : geometry.maxZ
                );
                settings.toLocal(vars.vect3, vars.vect3);
                min.minLocal(vars.vect3);
                max.maxLocal(vars.vect3);
            }
            float span = settings.cellSize * settings.chunkSize;
            int x0 = (int) Math.floor((min.x - settings.cellSize) / span), x1 = (int) Math.floor(
                (max.x + settings.cellSize) / span
            );
            int y0 = (int) Math.floor((min.y - settings.cellSize) / span), y1 = (int) Math.floor(
                (max.y + settings.cellSize) / span
            );
            int z0 = settings.planar ? 0 : (int) Math.floor((min.z - settings.cellSize) / span);
            int z1 = settings.planar ? 0 : (int) Math.floor((max.z + settings.cellSize) / span);
            if (
                Math.abs((long) x0) > 1000000 ||
                Math.abs((long) x1) > 1000000 ||
                Math.abs((long) y0) > 1000000 ||
                Math.abs((long) y1) > 1000000 ||
                Math.abs((long) z0) > 1000000 ||
                Math.abs((long) z1) > 1000000 ||
                !Float.isFinite(span)
            ) throw new IllegalArgumentException("Geometry exceeds the navigation coordinate range");
            long total = (x1 - (long) x0 + 1) * (y1 - (long) y0 + 1) * (z1 - (long) z0 + 1);
            if (total > 65536 || total < 0) throw new IllegalArgumentException("Load large worlds in geometry pages");
            for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                NavChunkKey key = new NavChunkKey(x, y, z);
                if (requested.add(key)) {
                    dirty.add(key);
                    versions.put(key, ++epoch);
                    pendingBuilds++;
                }
            }
        }
    }

    private void invalidate(NavGeometry previous, NavGeometry next) {
        failure = null;
        for (NavChunkKey key : requested) {
            if (!overlaps(key, previous) && (next == null || !overlaps(key, next))) continue;
            if (pages.remove(key) != null) pendingBuilds++;
            dirty.add(key);
            versions.put(key, ++epoch);
        }
        publish(false);
        scheduleBuilds();
    }

    private boolean overlaps(NavChunkKey key, NavGeometry geometry) {
        if (geometry == null) return false;
        float span = settings.cellSize * settings.chunkSize;
        try (TempVars vars = TempVars.get()) {
            float cx = (key.x + 0.5f) * span, cy = (key.y + 0.5f) * span;
            float cz = settings.planar ? 0 : (key.z + 0.5f) * span;
            settings.toWorld(cx, cy, cz, vars.vect1);
            float half = span * 0.5f + settings.cellSize;
            float ex = half * (Math.abs(settings.axisX.x) + Math.abs(settings.axisY.x) + Math.abs(settings.axisZ.x));
            float ey = half * (Math.abs(settings.axisX.y) + Math.abs(settings.axisY.y) + Math.abs(settings.axisZ.y));
            float ez = half * (Math.abs(settings.axisX.z) + Math.abs(settings.axisY.z) + Math.abs(settings.axisZ.z));
            return geometry.overlaps(
                vars.vect1.x - ex,
                vars.vect1.y - ey,
                vars.vect1.z - ez,
                vars.vect1.x + ex,
                vars.vect1.y + ey,
                vars.vect1.z + ez
            );
        }
    }

    private int activeBuilds;
    private boolean scheduling;

    private void scheduleBuilds() {
        if (scheduling) return;
        scheduling = true;
        try {
            while (!closed && activeBuilds < settings.maxPendingJobs && !dirty.isEmpty()) {
                NavChunkKey key = dirty.iterator().next();
                dirty.remove(key);
                long version = versions.get(key), identity = nextSerial();
                ArrayList<NavGeometry> geometry = new ArrayList<>();
                for (NavLayer layer : layers.values()) if (layer.attached) {
                    for (NavSource source : layer.sources) if (overlaps(key, source.geometry)) geometry.add(source.geometry);
                }
                for (NavChunk page : imported.values()) for (NavGeometry source : page.geometry) {
                    if (overlaps(key, source) && !geometry.contains(source)) geometry.add(source);
                }
                NavGeometry[] captured = geometry.toArray(new NavGeometry[0]);
                activeBuilds++;
                Runnable build = () -> {
                    NavChunk result = null;
                    Throwable error = null;
                    boolean useful;
                    synchronized (NavWorld.this) {
                        Long current = versions.get(key);
                        useful = !closed && current != null && current == version;
                    }
                    try {
                        if (useful) result = NavChunk.build(key, identity, settings, captured, buildScratch.get());
                    } catch (RuntimeException | LinkageError exception) {
                        error = exception;
                    }
                    synchronized (NavWorld.this) {
                        activeBuilds--;
                        Long expected = versions.get(key);
                        if (!closed && expected != null && expected == version) {
                            boolean finished = error == null && result != null;
                            if (finished) {
                                pages.put(key, result);
                            } else {
                                failure = error;
                            }
                            versions.remove(key);
                            publish(false);
                            if (finished) pendingBuilds--;
                        }
                        scheduleBuilds();
                    }
                };
                if (generationExecutor == null) {
                    if (!generationQueue.offer(build)) {
                        activeBuilds--;
                        dirty.add(key);
                        break;
                    }
                } else {
                    try {
                        generationExecutor.execute(build);
                    } catch (RejectedExecutionException exception) {
                        activeBuilds--;
                        dirty.add(key);
                        failure = exception;
                        break;
                    }
                }
            }
        } finally {
            scheduling = false;
        }
    }

    private void publish(boolean preserveSurfaces) {
        LinkedHashSet<NavGeometry> geometry = new LinkedHashSet<>();
        for (NavLayer layer : layers.values()) if (layer.attached) for (NavSource source : layer.sources) geometry.add(
            source.geometry
        );
        for (NavChunk chunk : imported.values()) for (NavGeometry source : chunk.geometry) geometry.add(source);
        snapshot =
            new NavSnapshot(
                ++revision,
                preserveSurfaces ? snapshot.surfaceRevision : revision,
                pages.values().toArray(new NavChunk[0]),
                geometry.toArray(new NavGeometry[0]),
                links.toArray(new NavLink[0])
            );
    }

    private long nextSerial() {
        if (serial == (Long.MAX_VALUE >>> 24)) throw new IllegalStateException("Navigation page identity exhausted");
        return ++serial;
    }

    private boolean compatible(NavSettings other) {
        return (
            settings.cellSize == other.cellSize &&
            settings.chunkSize == other.chunkSize &&
            settings.planar == other.planar &&
            settings.origin.equals(other.origin) &&
            settings.axisX.equals(other.axisX) &&
            settings.axisY.equals(other.axisY) &&
            settings.axisZ.equals(other.axisZ)
        );
    }

    private void validateKey(NavChunkKey key) {
        if (
            key == null ||
            settings.planar &&
            key.z != 0 ||
            Math.abs((long) key.x) > 1000000 ||
            Math.abs((long) key.y) > 1000000 ||
            Math.abs((long) key.z) > 1000000
        ) throw new IllegalArgumentException("Invalid page coordinate");
    }

    private void checkView(NavView view) {
        requireOpen();
        if (view.world != this) throw new IllegalArgumentException("View belongs to another world");
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Navigation world is closed");
    }

    private static void validatePosition(Vector3f value) {
        if (!Vector3f.isValidVector(value)) throw new IllegalArgumentException("Non-finite position");
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (ownedGeneration != null) ownedGeneration.shutdownNow();
        if (ownedPlanning != null) ownedPlanning.shutdownNow();
        generationQueue.clear();
        planningQueue.clear();
        for (NavLayer layer : layers.values()) {
            layer.attached = false;
            for (NavSource source : layer.sources) source.removed = true;
            layer.sources.clear();
            layer.importedPages.clear();
        }
        pages.clear();
        imported.clear();
        importedOwners.clear();
        requested.clear();
        dirty.clear();
        versions.clear();
        layers.clear();
        links.clear();
        pendingBuilds = 0;
        snapshot = new NavSnapshot(++revision, revision, new NavChunk[0], new NavGeometry[0], new NavLink[0]);
        queries.remove();
        buildScratch.remove();
        // Managed engine buffers are reclaimed after the last reader releases its snapshot.
    }
}
