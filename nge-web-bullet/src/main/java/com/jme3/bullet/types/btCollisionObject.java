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

import org.teavm.jso.JSObject;

public interface btCollisionObject extends JSObject {
    public static int ACTIVE_TAG = 1;

    public static int ISLAND_SLEEPING = 2;

    public static int WANTS_DEACTIVATION = 3;

    public static int DISABLE_DEACTIVATION = 4;

    public static int DISABLE_SIMULATION = 5;

    public int getUserIndex();

    public void setUserIndex(int index);

    int getCollisionFlags();

    void setCollisionFlags(int f);

    btTransform getInterpolationWorldTransform();

    void setActivationState(int state);

    void setCcdSweptSphereRadius(float radius);

    void setCcdMotionThreshold(float threshold);

    float getCcdSweptSphereRadius();

    float getCcdMotionThreshold();

    float getCcdSquareMotionThreshold();

    float getFriction();

    void setFriction(float frict);

    float getRestitution();

    void setRestitution(float rest);

    void activate();

    void activate(boolean forceActivation);

    boolean isActive();

    void setCollisionShape(btCollisionShape collisionShape);


    btTransform getWorldTransform();

    void setWorldTransform(btTransform worldTrans);

    public default void setUserPointer(Object obj) {
        int index = btUtils.setUserPointer(obj);
        setUserIndex(index);

    }
    
    public default Object getUserPointer() {
        int index = getUserIndex();
        return btUtils.getUserPointer(index);
    }

    public default void clearUserPointer() {
        int index = getUserIndex();
        btUtils.clearUserPointer(index);

    }

}

