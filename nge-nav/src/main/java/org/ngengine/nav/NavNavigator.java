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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Persistent, single-owner guidance state. update never builds a page or searches a route.
 * Physics remains responsible for applying desired velocity and reporting actual motion.
 */
public final class NavNavigator {

    private NavView view;
    private final NavWorld world;
    private final NavQuery query;
    private final NavPathFollower follower;
    private final Vector3f target = new Vector3f();
    private final Vector3f next = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private final Vector3f fromNormal = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private final Vector3f probe = new Vector3f();
    private final Vector3f attemptPosition = new Vector3f();
    private final Vector3f attemptVelocity = new Vector3f();
    private NavMode attemptMode;
    private boolean hasTarget;
    private boolean replanRequested;
    private volatile long requestId;
    private long pendingRequest;
    private final AtomicReference<Result> completed = new AtomicReference<>();
    private NavPath path;
    private int index;
    private long lastAttemptRevision = -1;
    private NavReason lastFailure = NavReason.NONE;
    private long actionSequence;
    private long waitingAction;
    private long acknowledged;
    private boolean actionSucceeded;
    private int acknowledgedIndex = -1;

    NavNavigator(NavView view) {
        this(view, null);
    }

    NavNavigator(NavView view, NavPathFollower follower) {
        this.view = view;
        this.follower = follower;
        world = view.world;
        query = new NavQuery(world.settings, false);
    }

    public void target(Vector3f position) {
        if (!Vector3f.isValidVector(position)) throw new IllegalArgumentException("Invalid target");
        if (hasTarget && target.equals(position)) return;
        target.set(position);
        hasTarget = true;
        invalidate();
    }

    public void clearTarget() {
        hasTarget = false;
        invalidate();
    }

    /**
     * Returns the immutable active path for inspection, or null while no route is available.
     */
    public NavPath getPath() {
        return path;
    }

    public NavView getView() {
        return view;
    }

    public void setView(NavView view) {
        if (view.world != world) throw new IllegalArgumentException("View belongs to another world");
        if (this.view != view) {
            this.view = view;
            invalidate();
        }
    }

    public void replan() {
        if (path == null) invalidate(); else {
            requestId++;
            pendingRequest = 0;
            completed.set(null);
            replanRequested = true;
        }
    }

    private void invalidate() {
        query.bind(null, null);
        requestId++;
        pendingRequest = 0;
        completed.set(null);
        path = null;
        index = 0;
        waitingAction = acknowledged = 0;
        lastAttemptRevision = -1;
        lastFailure = NavReason.NONE;
        acknowledgedIndex = -1;
        replanRequested = false;
    }

    /**
     * Each action token accepts one acknowledgement. Success is checked against actual motion
     * on the next update; a controller cannot acknowledge teleportation without moving the body.
     */
    public boolean acknowledge(long token, boolean succeeded) {
        if (token == 0 || token != waitingAction || acknowledged == token) return false;
        acknowledged = token;
        actionSucceeded = succeeded;
        return true;
    }

    public NavStep update(NavMotion motion, float deltaTime, NavStep store) {
        NavSnapshot snapshot = world.snapshot;
        store.reset(NavStatus.YIELD, NavReason.NONE, snapshot.revision, view.revision);
        if (world.closed) {
            query.bind(null, null);
            path = null;
            completed.set(null);
            return store.reset(NavStatus.YIELD, NavReason.CLOSED, snapshot.revision, view.revision);
        }
        if (!hasTarget) return store.reset(NavStatus.YIELD, NavReason.NO_TARGET, snapshot.revision, view.revision);
        if (!motion.isValid() || !Float.isFinite(deltaTime) || deltaTime <= 0) {
            return store.reset(NavStatus.YIELD, NavReason.INVALID_MOTION, snapshot.revision, view.revision);
        }
        query.bind(snapshot, view);
        Result result = waitingAction == 0 ? completed.getAndSet(null) : null;
        if (result != null) {
            if (result.id == pendingRequest && result.id == requestId) {
                pendingRequest = 0;
                if (result.path.isComplete()) {
                    path = result.path;
                    index = 0;
                    acknowledgedIndex = -1;
                    lastFailure = NavReason.NONE;
                    rejoin(motion, snapshot);
                } else {
                    lastFailure = result.path.getReason();
                    lastAttemptRevision = result.path.revision;
                }
            }
        }
        if (path == null) {
            if (world.getFailure() != null) return store.reset(
                NavStatus.YIELD,
                NavReason.FAILED,
                snapshot.revision,
                view.revision
            );
            if (pendingRequest == 0 && (lastAttemptRevision != snapshot.revision || motionChanged(motion))) request(
                motion,
                snapshot
            );
            NavReason reason = pendingRequest != 0 ? NavReason.PATH_PENDING : lastFailure;
            if (snapshot.chunks.length == 0 && world.getPendingBuildCount() > 0) reason = NavReason.WAITING_FOR_NAVMESH;
            if (reason == NavReason.NONE) reason = NavReason.QUEUE_FULL;
            return store.reset(
                reason == NavReason.NO_PATH ? NavStatus.NO_PATH : NavStatus.YIELD,
                reason,
                snapshot.revision,
                view.revision
            );
        }
        if (replanRequested && pendingRequest == 0 && waitingAction == 0) {
            request(motion, snapshot);
            if (pendingRequest != 0) replanRequested = false;
        }
        if (waitingAction != 0) {
            if (snapshot.resolve(path.references.get(index)) == null) {
                invalidate();
                return store.reset(NavStatus.YIELD, NavReason.STALE_PATH, snapshot.revision, view.revision);
            }
            if (acknowledged != waitingAction) return transition(store);
            if (!actionSucceeded) {
                invalidate();
                return store.reset(NavStatus.YIELD, NavReason.STALE_PATH, snapshot.revision, view.revision);
            }
            path.getPoint(index, next);
            if (
                motion.position.distanceSquared(next) > Math.max(view.arrivalDistance * view.arrivalDistance, 0.01f) ||
                motion.mode != path.getMode(index) ||
                motion.mode == NavMode.CLIMB &&
                query.pose(path.references.get(index), probe, normal) != null &&
                motion.up.dot(normal) < 0.995f
            ) {
                return store.reset(NavStatus.YIELD, NavReason.INVALID_MOTION, snapshot.revision, view.revision);
            }
            waitingAction = acknowledged = 0;
            acknowledgedIndex = index;
            if (index < path.size() - 1) index++;
        }
        for (int checks = 0; checks < world.settings.localChecks; checks++) {
            long ref = path.references.get(index);
            if (snapshot.resolve(ref) == null || !Float.isFinite(query.difficulty(ref, 0))) {
                invalidate();
                request(motion, snapshot);
                return store.reset(NavStatus.YIELD, NavReason.STALE_PATH, snapshot.revision, view.revision);
            }
            path.getPoint(index, next);
            query.pose(ref, probe, normal);
            boolean stepUp = index > 0 && query.needsStep(path.references.get(index - 1), ref);
            if (stepUp && follower != null && path.links.get(index) == 0) {
                // The follower's index can be a distant look-ahead target. Classify its
                // approach from the actual contact pose, not an unvisited raw sample.
                if (query.surfaceAllowed(path.references.get(index - 1), ref, motion.position, next, null)) stepUp =
                    false; else {
                    float reach = world.settings.cellSize * 2.6f + view.height;
                    if (motion.position.distanceSquared(next) > reach * reach) return rejectMotion(motion, snapshot, store);
                }
            }
            boolean reorient = path.getMode(index) == NavMode.CLIMB && motion.up.dot(normal) < 0.995f;
            if (
                index != acknowledgedIndex &&
                (reorient || stepUp || index > 0 && (path.links.get(index) != 0 || motion.mode != path.getMode(index)))
            ) {
                if (path.links.get(index) != 0 && findLink(snapshot, path.links.get(index)) == null) {
                    invalidate();
                    return store.reset(NavStatus.YIELD, NavReason.STALE_PATH, snapshot.revision, view.revision);
                }
                waitingAction = ++actionSequence;
                return transition(store);
            }
            // Arrival slack belongs to the final destination. Intermediate samples must be
            // reached closely: a sampled shortcut check can miss a thin unsupported corner.
            float tolerance = index == path.size() - 1
                ? view.arrivalDistance
                : Math.min(view.arrivalDistance, world.settings.cellSize * 0.0001f);
            float remaining = motion.position.distanceSquared(next);
            if (follower != null && motion.mode == NavMode.WALK && !view.planeSupport) {
                // Ground controllers own capsule contact. A tiny difference in support
                // height must not force a stop on every triangulated terrain sample.
                float height =
                    (motion.position.x - next.x) *
                    view.up.x +
                    (motion.position.y - next.y) *
                    view.up.y +
                    (motion.position.z - next.z) *
                    view.up.z;
                if (
                    Math.abs(height) <= world.settings.cellSize * 0.02f + view.radius * 0.25f &&
                    query.supported(motion.position, motion.position, 0, false)
                ) {
                    remaining = Math.max(0, remaining - height * height);
                    if (index + 1 < path.size() && path.getMode(index + 1) == NavMode.WALK && path.links.get(index + 1) == 0) {
                        float slack = Math.min(view.radius * 0.25f, world.settings.cellSize * 0.1f);
                        if (remaining <= slack * slack) {
                            path.getPoint(index + 1, probe);
                            if (
                                query.straightAllowed(ref, path.references.get(index + 1), motion.position, probe, motion.up) ||
                                query.surfaceAllowed(ref, path.references.get(index + 1), motion.position, probe, null)
                            ) tolerance = Math.max(tolerance, slack);
                        }
                    }
                }
            }
            if (
                remaining <= tolerance * tolerance &&
                (
                    follower == null ||
                    index < path.size() - 1 ||
                    motion.velocity.lengthSquared() <=
                    Math.max(0.01f, view.kinematics == null ? 0 : view.kinematics.terminalSpeed * view.kinematics.terminalSpeed)
                )
            ) {
                if (index < path.size() - 1) {
                    index++;
                    continue;
                }
                // The returned endpoint is projected onto the admitted shared graph.
                return store.reset(NavStatus.ARRIVED, NavReason.NONE, snapshot.revision, view.revision);
            }
            if (follower != null) {
                int advanced = follower.follow(
                    path,
                    index,
                    motion,
                    Math.min(deltaTime, 0.25f),
                    view,
                    snapshot,
                    query,
                    velocity,
                    next,
                    normal
                );
                if (advanced >= 0) {
                    index = advanced;
                    if (world.snapshot != snapshot) return store.reset(
                        NavStatus.YIELD,
                        NavReason.STALE_PATH,
                        snapshot.revision,
                        view.revision
                    );
                    store.move(velocity, next, normal, path.getMode(index), Math.min(deltaTime, 0.25f), follower.correcting);
                    return store;
                }
                return rejectMotion(motion, snapshot, store);
            }
            velocity.set(next).subtractLocal(motion.position);
            float distance = velocity.length();
            float ascent = distance == 0 ? 0 : velocity.dot(view.up) / distance;
            if (!Float.isFinite(query.difficulty(ref, ascent))) {
                invalidate();
                return store.reset(NavStatus.YIELD, NavReason.COST_CUTOFF, snapshot.revision, view.revision);
            }
            float speed = view.getSpeed(path.getMode(index));
            float horizon = Math.min(deltaTime, 0.25f);
            velocity.multLocal(Math.min(speed, distance / horizon) / distance);
            if (view.kinematics != null) constrain(motion, horizon, distance);
            query.pose(ref, next, normal);
            float commandedSpeed = velocity.length();
            ascent = commandedSpeed == 0 ? 0 : velocity.dot(view.up) / commandedSpeed;
            if (!Float.isFinite(query.difficulty(ref, ascent))) {
                return store.reset(NavStatus.YIELD, NavReason.COST_CUTOFF, snapshot.revision, view.revision);
            }
            probe.set(velocity).multLocal(horizon).addLocal(motion.position);
            fromNormal.set(motion.mode == NavMode.CLIMB ? motion.up : view.up);
            if (
                !query.segmentAllowed(ref, ref, motion.position, probe, fromNormal, normal, ascent) ||
                path.getMode(index) == NavMode.WALK &&
                !query.supported(motion.position, probe, ascent, false) ||
                !NavCollision.sweep(snapshot, view, motion.position, probe, fromNormal, normal, world.settings.localChecks)
            ) {
                return rejectMotion(motion, snapshot, store);
            }
            if (world.snapshot != snapshot) return store.reset(
                NavStatus.YIELD,
                NavReason.STALE_PATH,
                snapshot.revision,
                view.revision
            );
            store.move(velocity, next, normal, path.getMode(index), horizon, index == 0);
            return store;
        }
        return store.reset(NavStatus.YIELD, NavReason.BUDGET_EXCEEDED, snapshot.revision, view.revision);
    }

    private void rejoin(NavMotion motion, NavSnapshot snapshot) {
        // Planning captured an earlier pose. Join its first unpassed segment without
        // walking back to that pose, preserving every action and surface boundary.
        try (TempVars vars = TempVars.get()) {
            float reach = Math.max(view.radius, world.settings.cellSize * 0.1f);
            Vector3f up = motion.mode == NavMode.CLIMB ? motion.up : view.up;
            for (int i = 1; i < path.size() && i <= world.settings.localChecks; i++) {
                long previous = path.references.get(i - 1), ref = path.references.get(i);
                if (
                    path.getMode(i - 1) != motion.mode ||
                    path.getMode(i) != motion.mode ||
                    path.links.get(i) != 0 ||
                    snapshot.resolve(previous) == null ||
                    snapshot.resolve(ref) == null
                ) break;
                path.getPoint(i - 1, vars.vect1);
                query.pose(ref, vars.vect2, vars.vect3);
                if (up.dot(vars.vect3) < 0.995f) break;
                vars.vect4.set(vars.vect2).subtractLocal(vars.vect1);
                float length = vars.vect4.lengthSquared();
                if (length < 1e-12f) continue;
                vars.vect5.set(motion.position).subtractLocal(vars.vect1);
                float projected = vars.vect5.dot(vars.vect4) / length;
                if (projected <= 0) break;
                // A controller can already have crossed a slope seam while planning.
                // Keep a genuine uncompleted step, but do not return to its old takeoff.
                if (
                    query.needsStep(previous, ref) &&
                    projected < 1 &&
                    !query.preciseAllowed(ref, motion.position, vars.vect2, up)
                ) break;
                vars.vect1.addLocal(vars.vect4.multLocal(projected));
                if (motion.position.distanceSquared(vars.vect1) > reach * reach) break;
                vars.vect4.set(vars.vect2).subtractLocal(motion.position);
                float distance = vars.vect4.length();
                float ascent = distance == 0 ? 0 : vars.vect4.dot(view.up) / distance;
                if (
                    !query.surfaceAllowed(previous, ref, motion.position, vars.vect2, null) &&
                    (
                        !query.segmentAllowed(previous, ref, motion.position, vars.vect2, up, vars.vect3, ascent) ||
                        motion.mode == NavMode.WALK &&
                        !query.supported(motion.position, vars.vect2, ascent, false) ||
                        !NavCollision.sweep(
                            snapshot,
                            view,
                            motion.position,
                            vars.vect2,
                            up,
                            vars.vect3,
                            world.settings.localChecks
                        )
                    )
                ) break;
                index = i;
                if (projected < 1) break;
            }
        }
    }

    private boolean motionChanged(NavMotion motion) {
        float distance = world.settings.cellSize * 0.5f;
        float speed = Math.max(0.1f, view.getSpeed(motion.mode) * 0.25f);
        return (
            motion.mode != attemptMode ||
            motion.position.distanceSquared(attemptPosition) > distance * distance ||
            (view.kinematics != null || follower != null) &&
            motion.velocity.distanceSquared(attemptVelocity) > speed * speed
        );
    }

    private NavStep rejectMotion(NavMotion motion, NavSnapshot snapshot, NavStep store) {
        boolean retry = lastAttemptRevision != snapshot.revision || motionChanged(motion);
        invalidate();
        if (retry) request(motion, snapshot); else {
            lastAttemptRevision = snapshot.revision;
            lastFailure = NavReason.UNSUPPORTED_POSITION;
        }
        return store.reset(NavStatus.YIELD, NavReason.STALE_PATH, snapshot.revision, view.revision);
    }

    private void request(NavMotion motion, NavSnapshot snapshot) {
        if (!world.canPlan()) {
            lastFailure = NavReason.QUEUE_FULL;
            return;
        }
        long id = ++requestId;
        NavView policy = view;
        Vector3f start = motion.position.clone(), goal = target.clone(), speed = motion.velocity.clone(), forward =
            motion.forward.clone();
        NavMode mode = motion.mode;
        if (
            world.plan(() -> {
                NavPath route;
                try {
                    route = world.search(snapshot, policy, start, goal, mode, speed, forward);
                } catch (RuntimeException exception) {
                    route =
                        new NavPath(
                            world,
                            policy.revision,
                            snapshot.revision,
                            snapshot.surfaceRevision,
                            null,
                            null,
                            null,
                            NavReason.FAILED,
                            Double.POSITIVE_INFINITY
                        );
                }
                if (!world.closed && requestId == id) {
                    Result ready = new Result(id, route), previous;
                    do {
                        previous = completed.get();
                        if (previous != null && previous.id > id) return;
                    } while (!completed.compareAndSet(previous, ready));
                }
            })
        ) {
            pendingRequest = id;
            lastAttemptRevision = snapshot.revision;
            attemptPosition.set(motion.position);
            attemptVelocity.set(motion.velocity);
            attemptMode = motion.mode;
        } else lastFailure = NavReason.QUEUE_FULL;
    }

    private NavStep transition(NavStep store) {
        long linkId = path.links.get(index);
        NavMode mode = path.getMode(index);
        NavAction action;
        if (linkId != 0) {
            NavLink link = findLink(world.snapshot, linkId);
            if (link == null) {
                invalidate();
                return store.reset(NavStatus.YIELD, NavReason.STALE_PATH, world.getRevision(), view.revision);
            }
            action =
                link.mode == NavMode.JUMP
                    ? NavAction.JUMP
                    : link.mode == NavMode.DROP
                        ? NavAction.DROP
                        : link.mode == NavMode.TELEPORT ? NavAction.TELEPORT : NavAction.CUSTOM;
        } else if (index > 0 && query.needsStep(path.references.get(index - 1), path.references.get(index))) action =
            NavAction.STEP; else action =
            mode == NavMode.FLY
                ? NavAction.TAKEOFF
                : mode == NavMode.CLIMB
                    ? (index > 0 && path.getMode(index - 1) == NavMode.CLIMB ? NavAction.REORIENT : NavAction.ATTACH)
                    : NavAction.LAND;
        path.getPoint(index, next);
        query.pose(path.references.get(index), probe, normal);
        store.transition(
            mode,
            action,
            waitingAction,
            linkId,
            next,
            normal,
            action == NavAction.STEP ? query.stepLift(path.references.get(index - 1), path.references.get(index)) : 0
        );
        return store;
    }

    private static NavLink findLink(NavSnapshot snapshot, long id) {
        for (NavLink link : snapshot.links) if (link.id == id) return link;
        return null;
    }

    private void constrain(NavMotion motion, float dt, float distance) {
        NavKinematics dynamics = view.kinematics;
        float speed = velocity.length();
        if (index == path.size() - 1 && Float.isFinite(dynamics.braking)) {
            float allowed = FastMath.sqrt(dynamics.terminalSpeed * dynamics.terminalSpeed + 2 * dynamics.braking * distance);
            if (speed > allowed) {
                velocity.multLocal(allowed / speed);
                speed = allowed;
            }
        }
        if (
            Float.isFinite(dynamics.turnRate) &&
            speed > 1e-6f &&
            (motion.velocity.lengthSquared() > 1e-12f || motion.forward.lengthSquared() > 1e-12f)
        ) {
            probe.set(motion.velocity.lengthSquared() > 1e-12f ? motion.velocity : motion.forward).normalizeLocal();
            fromNormal.set(velocity).divideLocal(speed);
            float angle = FastMath.acos(FastMath.clamp(probe.dot(fromNormal), -1, 1));
            if (angle > dynamics.turnRate * dt) {
                float fraction = dynamics.turnRate * dt / angle;
                // A reversal needs a stable axis even when the two directions are collinear.
                normal.set(probe).crossLocal(fromNormal);
                if (normal.lengthSquared() < 1e-12f) {
                    normal.set(
                        world.settings.planar ? world.settings.axisZ : motion.mode == NavMode.CLIMB ? motion.up : view.up
                    );
                    normal.subtractLocal(probe.mult(normal.dot(probe), fromNormal));
                }
                if (normal.lengthSquared() < 1e-12f) normal.set(probe).crossLocal(Vector3f.UNIT_X);
                normal.normalizeLocal();
                normal.cross(probe, fromNormal);
                float turn = angle * fraction;
                velocity.set(probe).multLocal(FastMath.cos(turn));
                velocity.addLocal(fromNormal.mult(FastMath.sin(turn), probe)).normalizeLocal().multLocal(speed);
            }
        }
        probe.set(velocity).subtractLocal(motion.velocity);
        float change = probe.length();
        float limit = velocity.lengthSquared() < motion.velocity.lengthSquared() ? dynamics.braking : dynamics.acceleration;
        if (Float.isFinite(limit) && change > limit * dt) velocity
            .set(motion.velocity)
            .addLocal(probe.multLocal(limit * dt / change));
    }

    private static final class Result {

        final long id;
        final NavPath path;

        Result(long id, NavPath path) {
            this.id = id;
            this.path = path;
        }
    }
}
