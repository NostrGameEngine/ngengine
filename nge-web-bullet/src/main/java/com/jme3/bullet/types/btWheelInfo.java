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

public interface btWheelInfo extends JSObject {
    // btTransform m_worldTransform


    @JSMethod("get_m_worldTransform")
    public btTransform getWorldTransform();

    @JSMethod("get_m_raycastInfo")
    public btRaycastInfo getRaycastInfo();

    @JSMethod("get_m_skidInfo")
    public float getSkidInfo();

    @JSMethod("get_m_deltaRotation")
    public float getDeltaRotation();



    // setters
    @JSMethod("set_m_worldTransform")
    public void setWorldTransform(btTransform worldTransform);

    @JSMethod("set_m_suspensionStiffness")
    public void setSuspensionStiffness(float suspensionStiffness);

    @JSMethod("set_m_wheelsDampingCompression")
    public void setWheelsDampingCompression(float wheelsDampingCompression);

    @JSMethod("set_m_wheelsDampingRelaxation")
    public void setWheelsDampingRelaxation(float wheelsDampingRelaxation);

    @JSMethod("set_m_frictionSlip")
    public void setFrictionSlip(float frictionSlip);

    @JSMethod("set_m_rollInfluence")
    public void setRollInfluence(float rollInfluence);

    @JSMethod("set_m_maxSuspensionForce")
    public void setMaxSuspensionForce(float maxSuspensionForce);

    @JSMethod("set_m_maxSuspensionTravelCm")
    public void setMaxSuspensionTravelCm(float maxSuspensionTravelCm);

    @JSMethod("set_m_wheelsRadius")
    public void setWheelsRadius(float wheelsRadius);

    @JSMethod("set_m_bIsFrontWheel")
    public void setBIsFrontWheel(boolean isFrontWheel);

    @JSMethod("set_m_suspensionRestLength1")
    public void setSuspensionRestLength1(float suspensionRestLength1);


}
