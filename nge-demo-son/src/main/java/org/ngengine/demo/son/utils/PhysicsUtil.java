/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.utils;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;
import java.util.logging.Logger;

public class PhysicsUtil {

    private static Logger log = Logger.getLogger(PhysicsUtil.class.getName());

    public static void accelerateToKmh(float kmh, PhysicsRigidBody body, float timeToReachSec) {
        TempVars vars = TempVars.get();
        try {
            Vector3f currentVel = vars.vect1;
            Vector3f horizontalVel = vars.vect2;
            Vector3f dir = vars.vect3;
            Vector3f force = vars.vect4;
            Quaternion rot = vars.quat1;

            // 1) Convert target speed to m/s
            float targetSpeedMps = kmh / 3.6f;

            // 2) Get current linear velocity & isolate horizontal component
            body.getLinearVelocity(currentVel); // :contentReference[oaicite:0]{index=0}
            horizontalVel.set(currentVel.x, 0f, currentVel.z);
            float currSpeed = horizontalVel.length();

            // 3) Already at or above target? Quit early.
            if (currSpeed >= targetSpeedMps) {
                return;
            }

            // 4) Compute forward direction from body’s orientation
            body.getPhysicsRotation(rot); // :contentReference[oaicite:1]{index=1}
            // In JME, +X is local “right,” +Z is local “forward.”
            // So to get horizontal forward:
            rot.mult(Vector3f.UNIT_Z, dir);
            dir.set(dir.x, 0f, dir.z).normalizeLocal();

            // 5) Compute required accel: Δv / time
            float accel = targetSpeedMps; // - currSpeed;

            // 6) Force = m * a along the forward dir
            force.set(dir).multLocal(accel * body.getMass());

            // 7) Apply it each tick; Bullet integrates over dt internally
            body.applyCentralForce(force); // :contentReference[oaicite:2]{index=2}
        } finally {
            vars.release();
        }
    }

    public static void accelerateToAngularSpeed(float degPerSec, PhysicsRigidBody body, float timeToReachSec) {
        TempVars vars = TempVars.get();
        try {
            Vector3f angVel = vars.vect1;
            Vector3f worldUp = vars.vect2;
            Vector3f torque = vars.vect3;
            Quaternion rot = vars.quat1;

            // Convert target angular speed to radians/sec
            float targetRadPerSec = (float) Math.toRadians(degPerSec);

            // Get current angular velocity and project onto local Y (up) axis in world space
            body.getAngularVelocity(angVel);
            body.getPhysicsRotation(rot);
            rot.mult(Vector3f.UNIT_Y, worldUp).normalizeLocal(); // Local Y to world space

            float currentSpin = angVel.dot(worldUp);
            if (currentSpin >= targetRadPerSec) {
                return; // Already at or above target
            }

            // Compute needed angular acceleration
            float alpha = (targetRadPerSec - currentSpin) / timeToReachSec;

            // Use generic inertia I = 1
            float I = 1f;
            float torqueAmount = I * alpha;

            torque.set(worldUp).multLocal(torqueAmount);
            body.applyTorque(torque);
        } finally {
            vars.release();
        }
    }

    public static void accelerateToAngularSpeed(float degPerSec, PhysicsRigidBody body, float timeToReachSec, float dt) {
        TempVars vars = TempVars.get();
        try {
            // Convert degrees/sec → radians/sec
            float targetRadPerSec = (float) Math.toRadians(degPerSec);

            // Get current angular velocity
            Vector3f angVel = vars.vect1;
            body.getAngularVelocity(angVel);

            // Transform local Y axis to world space (spin axis)
            Quaternion rot = body.getPhysicsRotation(vars.quat1);
            Vector3f spinAxis = rot.mult(Vector3f.UNIT_Y, vars.vect2).normalizeLocal();

            // Project current spin onto spin axis
            float currentSpin = angVel.dot(spinAxis);

            // Compute needed angular acceleration (rad/s²)
            float deltaOmega = targetRadPerSec - currentSpin;
            float accel = deltaOmega / timeToReachSec;

            // Use generic moment of inertia I = 1 → torque = alpha * I
            // But scale by dt to apply per-frame effect
            float torqueAmount = accel * dt;

            // Apply torque in spin axis direction
            Vector3f torque = spinAxis.mult(torqueAmount, vars.vect3);
            body.applyTorque(torque);
        } finally {
            vars.release();
        }
    }
}
