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

/**
 * Continuous, bounded local steering over an asynchronous navigator's route.
 * Use one instance per navigator; measured motion remains authoritative.
 */
public final class NavPathFollower {

    private static final NavKinematics DEFAULT_LIMITS = NavKinematics
        .builder()
        .acceleration(8)
        .braking(12)
        .turnRate(6)
        .terminalSpeed(0)
        .build();

    private final float lookAhead;
    private final Vector3f target = new Vector3f();
    private final Vector3f axis = new Vector3f();
    private final Vector3f direction = new Vector3f();
    private final Vector3f endpoint = new Vector3f();
    boolean correcting;
    private float groundSlope;

    public NavPathFollower() {
        this(8);
    }

    /**
     * Maximum distance, in world units, inspected for a visible route point.
     * NavSettings.localBudget also bounds candidate and geometry checks.
     */
    public NavPathFollower(float lookAhead) {
        if (!Float.isFinite(lookAhead) || lookAhead <= 0) throw new IllegalArgumentException("Invalid look ahead");
        this.lookAhead = lookAhead;
    }

    int follow(
        NavPath path,
        int index,
        NavMotion motion,
        float dt,
        NavView view,
        NavSnapshot snapshot,
        NavQuery query,
        Vector3f velocity,
        Vector3f next,
        Vector3f normal
    ) {
        NavSettings settings = view.world.settings;
        correcting = false;
        NavMode mode = motion.mode;
        float spacing = Math.min(settings.cellSize * 0.2f, Math.max(view.radius * 0.25f, 0.001f));
        float reach = Math.min(lookAhead, spacing * settings.localChecks * 0.99f);
        long ref = path.references.get(index);
        if (path.getMode(index) != mode || query.pose(ref, target, axis) == null) return -1;
        // Actions and changes of support are barriers, even when a later point is visible.
        int limit = index;
        NavChunk first = snapshot.resolve(ref);
        int area = first.area(NavSnapshot.node(ref));
        first.normal(NavSnapshot.node(ref), axis);
        for (int i = index + 1; i < path.size() && i - index < settings.localChecks; i++) {
            long previous = path.references.get(i - 1), candidate = path.references.get(i);
            NavChunk chunk = snapshot.resolve(candidate);
            if (
                chunk == null ||
                path.getMode(i) != mode ||
                path.links.get(i) != 0 ||
                chunk.area(NavSnapshot.node(candidate)) != area ||
                mode != NavMode.WALK &&
                query.needsStep(previous, candidate) ||
                !Float.isFinite(query.difficulty(candidate, 0))
            ) break;
            chunk.normal(NavSnapshot.node(candidate), normal);
            path.getPoint(i, endpoint);
            if (
                mode != NavMode.FLY &&
                mode != NavMode.WALK &&
                !view.planeSupport &&
                axis.dot(normal) < 0.99999f ||
                motion.position.distanceSquared(endpoint) > reach * reach
            ) break;
            limit = i;
        }
        int chosen = -1;
        boolean surface = false;
        // Prefer the farthest visible point. Limit failed visibility tests as well as route inspection.
        for (int i = limit, attempts = 0; i >= index && attempts < Math.min(16, settings.localChecks); i--, attempts++) {
            long candidate = path.references.get(i);
            path.getPoint(i, target);
            if (mode == NavMode.WALK && query.surfaceAllowed(ref, candidate, motion.position, target, endpoint)) {
                // Compare the complete surface shortcut with the planned route, rather
                // than rejecting an individual steeper triangle as missing ground.
                double budget = 0;
                try (TempVars vars = TempVars.get()) {
                    vars.vect1.set(motion.position);
                    for (int point = index; point <= i; point++) {
                        path.getPoint(point, vars.vect2);
                        float length = vars.vect1.distance(vars.vect2);
                        float ascent = length == 0 ? 0 : (vars.vect2.dot(view.up) - vars.vect1.dot(view.up)) / length;
                        budget += length * (double) query.difficulty(path.references.get(point), ascent);
                        vars.vect1.set(vars.vect2);
                    }
                }
                // A single committed leg cannot be refined further. Longer shortcuts
                // retain route costs, allowing only a small rasterization tolerance.
                if (i == index || query.surfaceCost <= budget * 1.01 + 1e-6) {
                    chosen = i;
                    surface = true;
                    break;
                }
            }
            if (query.straightAllowed(ref, candidate, motion.position, target, motion.up)) {
                chosen = i;
                break;
            }
        }
        // A curved seam may forbid a long straight shortcut while the next small
        // move remains valid. Keep the same steering limits on that precise leg.
        boolean precise = chosen < 0;
        if (precise) chosen = index;
        correcting = precise && chosen == 0;
        path.getPoint(chosen, next);
        query.pose(path.references.get(chosen), endpoint, normal);
        direction.set(next).subtractLocal(motion.position);
        if (surface) direction.addLocal(view.up.mult(-direction.dot(view.up), endpoint));
        float distance = direction.length();
        if (distance < 1e-7f) {
            velocity.set(0, 0, 0);
            return chosen;
        }
        NavKinematics limits = view.kinematics == null ? DEFAULT_LIMITS : view.kinematics;
        float speed = view.getSpeed(mode);
        if (surface) speed /= FastMath.sqrt(1 + groundSlope * groundSlope);
        float cornerSpeed = 0;
        if (
            chosen + 1 < path.size() &&
            path.getMode(chosen + 1) == mode &&
            path.links.get(chosen + 1) == 0 &&
            !query.needsStep(path.references.get(chosen), path.references.get(chosen + 1))
        ) {
            NavChunk following = snapshot.resolve(path.references.get(chosen + 1));
            NavChunk current = snapshot.resolve(path.references.get(chosen));
            current.normal(NavSnapshot.node(path.references.get(chosen)), axis);
            if (following != null) following.normal(NavSnapshot.node(path.references.get(chosen + 1)), endpoint);
            if (
                following != null &&
                (mode == NavMode.FLY || view.planeSupport || axis.dot(endpoint) >= (mode == NavMode.WALK ? 0.98f : 0.99999f))
            ) {
                path.getPoint(chosen + 1, endpoint).subtractLocal(next);
                float length = endpoint.length();
                float angle = length < 1e-7f
                    ? 0
                    : FastMath.acos(FastMath.clamp(direction.dot(endpoint) / (distance * length), -1, 1));
                cornerSpeed = angle < 0.001f ? speed : Math.min(speed, limits.turnRate * Math.max(view.radius, 0.05f) / angle);
            }
        } else if (chosen == path.size() - 1) cornerSpeed = Math.min(speed, limits.terminalSpeed);
        if (Float.isFinite(limits.braking)) {
            if (cornerSpeed == 0) {
                // Exact stopping distance for the caller's velocity * dt integration.
                // The continuous formula can still leave the last step too fast.
                float stoppingDistance = distance + (chosen == path.size() - 1 ? view.arrivalDistance * 0.5f : 0);
                double frames = Math.max(
                    1,
                    Math.ceil((Math.sqrt(1 + 8 * stoppingDistance / ((double) limits.braking * dt * dt)) - 1) * 0.5)
                );
                float allowed = (float) (stoppingDistance / (frames * dt) + limits.braking * (double) dt * (frames - 1) * 0.5);
                speed = Math.min(speed, allowed);
            } else {
                float allowance = limits.braking * dt * 0.5f;
                speed =
                    Math.min(
                        speed,
                        FastMath.sqrt(cornerSpeed * cornerSpeed + 2 * limits.braking * distance + allowance * allowance) -
                        allowance
                    );
            }
        }
        try (TempVars vars = TempVars.get()) {
            Vector3f previous = vars.vect1.set(motion.velocity);
            // Keep steering tangent to the current support, including after a traversal action.
            if (surface) previous.addLocal(view.up.mult(-previous.dot(view.up), vars.vect3)); else if (
                settings.planar || !precise && mode != NavMode.FLY
            ) {
                NavChunk chunk = snapshot.resolve(path.references.get(chosen));
                chunk.normal(NavSnapshot.node(path.references.get(chosen)), vars.vect2);
                if (settings.planar) vars.vect2.set(settings.axisZ);
                if (mode == NavMode.WALK && !settings.planar) previous.addLocal(
                    view.up.mult(-previous.dot(vars.vect2) / Math.max(0.001f, vars.vect2.dot(view.up)), vars.vect3)
                ); else previous.addLocal(vars.vect2.mult(-previous.dot(vars.vect2), vars.vect3));
            }
            // Pure pursuit at a fixed speed can orbit a close waypoint indefinitely.
            // Slow while turning toward it, especially when reconnecting after a surface action.
            float oldSpeed = previous.length();
            if (oldSpeed > 0.05f) speed *= Math.max(0, previous.dot(direction) / (oldSpeed * distance));
            velocity.set(direction).multLocal(Math.min(speed, distance / dt) / distance);
            float magnitude = velocity.length();
            vars.vect2.set(previous);
            if (vars.vect2.lengthSquared() < 1e-10f && view.kinematics != null) {
                vars.vect2.set(motion.forward);
                if (mode != NavMode.FLY) {
                    snapshot
                        .resolve(path.references.get(chosen))
                        .normal(NavSnapshot.node(path.references.get(chosen)), vars.vect3);
                    if (view.planeSupport) vars.vect3.set(settings.axisZ);
                    float perpendicular = vars.vect2.dot(vars.vect3);
                    vars.vect2.addLocal(
                        -vars.vect3.x * perpendicular,
                        -vars.vect3.y * perpendicular,
                        -vars.vect3.z * perpendicular
                    );
                }
            }
            if (magnitude > 1e-6f && vars.vect2.lengthSquared() > 1e-10f && Float.isFinite(limits.turnRate)) {
                vars.vect2.normalizeLocal();
                vars.vect3.set(velocity).divideLocal(magnitude);
                float angle = FastMath.acos(FastMath.clamp(vars.vect2.dot(vars.vect3), -1, 1));
                if (angle > limits.turnRate * dt) {
                    vars.vect2.cross(vars.vect3, vars.vect4);
                    if (vars.vect4.lengthSquared() < 1e-10f) {
                        vars.vect4.set(settings.planar ? settings.axisZ : motion.up);
                        vars.vect4.addLocal(vars.vect2.mult(-vars.vect4.dot(vars.vect2), vars.vect3));
                        if (vars.vect4.lengthSquared() < 1e-10f) vars.vect2.cross(Vector3f.UNIT_X, vars.vect4);
                        if (vars.vect4.lengthSquared() < 1e-10f) vars.vect2.cross(Vector3f.UNIT_Z, vars.vect4);
                    }
                    vars.vect4.normalizeLocal().cross(vars.vect2, vars.vect3);
                    float turn = limits.turnRate * dt;
                    velocity.set(vars.vect2).multLocal(FastMath.cos(turn));
                    velocity.addLocal(vars.vect3.mult(FastMath.sin(turn), vars.vect4)).multLocal(magnitude);
                }
            }
            vars.vect2.set(velocity).subtractLocal(previous);
            float change = vars.vect2.length();
            float acceleration = velocity.lengthSquared() < previous.lengthSquared() ? limits.braking : limits.acceleration;
            if (Float.isFinite(acceleration) && change > acceleration * dt) velocity
                .set(previous)
                .addLocal(vars.vect2.multLocal(acceleration * dt / change));
            // Late guidance can arrive inside the stopping distance. Limiting the
            // desired speed before acceleration alone still allows an overshoot.
            float travel = velocity.length() * dt;
            float boundary = distance + (chosen == path.size() - 1 ? view.arrivalDistance : 0);
            if (cornerSpeed == 0 && travel > boundary) velocity.multLocal(boundary / travel);
            endpoint.set(velocity).multLocal(dt).addLocal(motion.position);
            boolean allowed = surface
                ? query.surfaceMoveAllowed(ref, path.references.get(chosen), motion.position, endpoint, endpoint)
                : precise
                    ? query.preciseAllowed(ref, motion.position, endpoint, motion.up)
                    : query.straightAllowed(ref, path.references.get(chosen), motion.position, endpoint, motion.up);
            if (!allowed) {
                // Hard geometry takes priority over inertia. Brake on the last safe tangent first.
                velocity.set(previous).multLocal(oldSpeed > 0 ? Math.max(0, oldSpeed - limits.braking * dt) / oldSpeed : 0);
                travel = velocity.length() * dt;
                if (cornerSpeed == 0 && travel > boundary) velocity.multLocal(boundary / travel);
                endpoint.set(velocity).multLocal(dt).addLocal(motion.position);
                allowed =
                    surface
                        ? query.surfaceMoveAllowed(ref, path.references.get(chosen), motion.position, endpoint, endpoint)
                        : precise
                            ? query.preciseAllowed(ref, motion.position, endpoint, motion.up)
                            : query.straightAllowed(ref, path.references.get(chosen), motion.position, endpoint, motion.up);
                if (!allowed) return -1;
            }
            if (surface) {
                float heightSpeed =
                    (
                        (endpoint.x - motion.position.x) *
                        view.up.x +
                        (endpoint.y - motion.position.y) *
                        view.up.y +
                        (endpoint.z - motion.position.z) *
                        view.up.z
                    ) /
                    dt;
                float horizontalSpeed = velocity.length();
                groundSlope = horizontalSpeed > 0.01f ? query.surfaceRise / (dt * horizontalSpeed) : 0;
                // Extra height from a controller skin is a contact correction, not
                // a reason to suddenly reduce horizontal locomotion speed.
                float oldVertical = motion.velocity.dot(view.up);
                float vertical = Math.max(heightSpeed, oldVertical - limits.braking * dt);
                if (vertical > heightSpeed) {
                    vars.vect3
                        .set(velocity)
                        .addLocal(view.up.mult(vertical, vars.vect4))
                        .multLocal(dt)
                        .addLocal(motion.position);
                    if (
                        !NavCollision.sweep(snapshot, view, motion.position, vars.vect3, view.up, view.up, settings.localChecks)
                    ) vertical = heightSpeed;
                }
                velocity.addLocal(view.up.mult(vertical - velocity.dot(view.up), vars.vect3));
            }
        }
        return chosen;
    }
}
