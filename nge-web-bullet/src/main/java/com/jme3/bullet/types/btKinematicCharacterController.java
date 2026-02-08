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

 
public interface btKinematicCharacterController  extends btDestructible,JSObject{
    

    public void warp(btVector3 v);

    public void setWalkDirection(btVector3 v);

    public void setFallSpeed(float v);

    public void setJumpSpeed(float v);

    public void setGravity(btVector3 v);

    @Deprecated
    public default void setGravity(float v) {
        btVector3 vv = btUtils.newVector3(this,0, v, 0);
        setGravity(vv);
        btUtils.destroy(this,vv);
    }

    public void setMaxSlope(float v);

    public void setMaxJumpHeight(float v);

    public void setUp(btVector3 v);

    public btVector3 getUp();


    public btVector3 getWalkDirection();

    public float getFallSpeed();

    public float getJumpSpeed();

    public float getGravity();

    public float getMaxSlope();

    public float getMaxJumpHeight();

    public boolean onGround();

    public void jump();
    

    public default void setUpAxis(int axis) {
        btVector3 up = getUp();
        switch (axis) {
            case 0:
                up.setValue(1, 0, 0);
                break;
            case 1:
                up.setValue(0, 1, 0);
                break;
            case 2:
                up.setValue(0, 0, 1);
                break;
            default:
                throw new IllegalArgumentException("Invalid axis " + axis);
        }
        setUp(up);
    }
}
