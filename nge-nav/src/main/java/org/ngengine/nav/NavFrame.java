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
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;
import java.util.Objects;

/**
 * A rigid reference frame for navigation geometry and actor policies expressed locally.
 * Pose updates and conversions belong to one owner thread; they never rebuild the local world.
 * The caller owns the world's lifecycle and collision checks against other frames.
 */
public final class NavFrame {

    private final NavWorld world;
    private final Vector3f position = new Vector3f();
    private final Quaternion rotation = new Quaternion(), inverse = new Quaternion();
    private final Vector3f linearVelocity = new Vector3f(), angularVelocity = new Vector3f();

    public NavFrame(NavWorld world) {
        this.world = Objects.requireNonNull(world);
    }

    public NavWorld getWorld() {
        return world;
    }

    /**
     * Places or teleports the frame, resetting its measured velocity. Scaling is not supported.
     */
    public void setPose(Vector3f position, Quaternion rotation) {
        requirePose(position, rotation);
        this.position.set(position);
        this.rotation.set(rotation).normalizeLocal();
        inverse.set(this.rotation).inverseLocal();
        linearVelocity.set(0, 0, 0);
        angularVelocity.set(0, 0, 0);
    }

    /**
     * Measures translation and shortest-arc angular velocity between consecutive poses.
     * Consecutive rotations must differ by less than half a turn.
     */
    public void updatePose(Vector3f position, Quaternion rotation, float deltaTime) {
        requirePose(position, rotation);
        if (!Float.isFinite(deltaTime) || deltaTime <= 0) throw new IllegalArgumentException("Invalid frame delta time");
        try (TempVars vars = TempVars.get()) {
            linearVelocity.set(position).subtractLocal(this.position).divideLocal(deltaTime);
            vars.quat1.set(rotation).multLocal(inverse).normalizeLocal();
            if (vars.quat1.getW() < 0) vars.quat1.negateLocal();
            vars.vect1.set(vars.quat1.getX(), vars.quat1.getY(), vars.quat1.getZ());
            float sine = vars.vect1.length();
            if (sine > 1e-8f) angularVelocity
                .set(vars.vect1)
                .multLocal(2 * FastMath.atan2(sine, vars.quat1.getW()) / (deltaTime * sine)); else angularVelocity.set(0, 0, 0);
        }
        this.position.set(position);
        this.rotation.set(rotation).normalizeLocal();
        inverse.set(this.rotation).inverseLocal();
    }

    public Transform getTransform(Transform store) {
        return store.setTranslation(position).setRotation(rotation).setScale(1);
    }

    public Vector3f toWorldPoint(Vector3f local, Vector3f store) {
        return rotation.mult(local, store).addLocal(position);
    }

    public Vector3f toLocalPoint(Vector3f world, Vector3f store) {
        store.set(world).subtractLocal(position);
        return inverse.mult(store, store);
    }

    /**
     * Converts a relative actor pose, including the frame's linear and angular point velocity.
     * Input and output may be the same instance.
     */
    public NavMotion toWorld(NavMotion local, NavMotion store) {
        try (TempVars vars = TempVars.get()) {
            rotation.mult(local.position, vars.vect1);
            angularVelocity.cross(vars.vect1, vars.vect2).addLocal(linearVelocity);
            rotation.mult(local.velocity, store.velocity).addLocal(vars.vect2);
            rotation.mult(local.forward, store.forward);
            rotation.mult(local.up, store.up);
            store.position.set(vars.vect1).addLocal(position);
        }
        store.mode = local.mode;
        store.time = local.time;
        return store;
    }

    /**
     * Removes frame motion from a measured world pose before local navigation queries.
     * Input and output may be the same instance.
     */
    public NavMotion toLocal(NavMotion world, NavMotion store) {
        try (TempVars vars = TempVars.get()) {
            vars.vect1.set(world.position).subtractLocal(position);
            angularVelocity.cross(vars.vect1, vars.vect2).addLocal(linearVelocity);
            vars.vect3.set(world.velocity).subtractLocal(vars.vect2);
            inverse.mult(vars.vect3, store.velocity);
            inverse.mult(world.forward, store.forward);
            inverse.mult(world.up, store.up);
            inverse.mult(vars.vect1, store.position);
        }
        store.mode = world.mode;
        store.time = world.time;
        return store;
    }

    private static void requirePose(Vector3f position, Quaternion rotation) {
        if (
            !Vector3f.isValidVector(position) ||
            rotation == null ||
            !Float.isFinite(rotation.norm()) ||
            Math.abs(rotation.norm() - 1) > 0.001f
        ) throw new IllegalArgumentException("A finite rigid pose is required");
    }
}
