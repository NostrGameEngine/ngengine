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

package com.jme3.bullet.types;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

public interface btRigidBody extends btCollisionObject {

    boolean isInWorld();

    btTransform getCenterOfMassTransform();

    void setCenterOfMassTransform(btTransform v);

    void setMassProps(float mass, btVector3 inertia);

    btVector3 getGravity();

    void setGravity(btVector3 acceleration);

    void setDamping(float lin_damping, float ang_damping);

    btVector3 getLinearVelocity();

    void setLinearVelocity(btVector3 lin_vel);

    btVector3 getAngularVelocity();

    void setAngularVelocity(btVector3 ang_vel);

    void setSleepingThresholds(float linear, float angular);

    void applyCentralForce(btVector3 force);

    void applyTorque(btVector3 torque);

    void applyForce(btVector3 force, btVector3 rel_pos);

    void applyCentralImpulse(btVector3 impulse);

    void applyTorqueImpulse(btVector3 torque);

    void applyImpulse(btVector3 impulse, btVector3 rel_pos);

    void clearForces();

    btVector3 getAngularFactor();

    void setAngularFactor(btVector3 angFac);

    void setLinearFactor(btVector3 v);
}


