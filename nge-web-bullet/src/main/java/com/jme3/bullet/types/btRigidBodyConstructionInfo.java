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
 * the BSD 3-Clause License. 
 */

package com.jme3.bullet.types;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

public interface btRigidBodyConstructionInfo extends JSObject{
    
    @JSMethod("set_m_mass")
    public void setMass(float mass);

    @JSMethod("set_m_motionState")
    public void setMotionState(btMotionState motionState);
 
    @JSMethod("get_m_mass")
    public float getMass();
    
    @JSMethod("get_m_motionState")
    public btMotionState getMotionState();

    @JSMethod("get_m_collisionShape")
    public btCollisionShape getCollisionShape();

    @JSMethod("set_m_collisionShape")
    public void setCollisionShape(btCollisionShape collisionShape);


    @JSMethod("get_m_friction")
    public float getFriction();

    @JSMethod("set_m_friction")
    public void setFriction(float friction);


    @JSMethod("set_m_linearDamping")
    public void setLinearDamping(float linearDamping);


    @JSMethod("get_m_linearDamping")
    public float getLinearDamping();

    @JSMethod("set_m_angularDamping")
    public void setAngularDamping(float angularDamping);

    @JSMethod("get_m_angularDamping")
    public float getAngularDamping();

    @JSMethod("set_m_restitution")
    public void setRestitution(float restitution);

    @JSMethod("get_m_restitution")
    public float getRestitution();

    @JSMethod("set_m_linearSleepingThreshold")
    public void setLinearSleepingThreshold(float linearSleepingThreshold);

    @JSMethod("get_m_linearSleepingThreshold")
    public float getLinearSleepingThreshold();

    @JSMethod("set_m_angularSleepingThreshold")
    public void setAngularSleepingThreshold(float angularSleepingThreshold);

    @JSMethod("get_m_angularSleepingThreshold")
    public float getAngularSleepingThreshold();
}
