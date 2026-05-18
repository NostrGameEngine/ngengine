/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.objects.infos;

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.types.btDestructible;
import com.jme3.bullet.types.btDestructibleImpl;
import com.jme3.bullet.types.btMatrix3x3;
import com.jme3.bullet.types.btMotionState;
import com.jme3.bullet.types.btTransform;
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.util.Converter;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * stores transform info of a PhysicsNode in a threadsafe manner to allow
 * multithreaded access from the jme scenegraph and the bullet physics space
 * 
 * @author normenhansen
 */
public class RigidBodyMotionState extends btDestructibleImpl  {
    private final btTransform worldTrans = btUtils.newTransform(this);
    private final btMotionState motionState = btUtils.createDefaultMotionState(this);
    private final Matrix3f worldRotation = new Matrix3f();
    private final Quaternion worldRotationQuat = new Quaternion();
    private final Vector3f worldLocation = new Vector3f();


    private final Vector3f localLocation = new Vector3f();
    private final Quaternion localRotationQuat = new Quaternion();

    private final Quaternion tmp_inverseWorldRotation = new Quaternion();

    private boolean applyPhysicsLocal = false;
    private PhysicsVehicle vehicle;

    public btMotionState getBtMotionState() {
        return motionState;
    }

    public RigidBodyMotionState() {
    }

    
  
    public boolean applyTransform(Spatial spatial) {
        Vector3f worldLocation = getWorldLocation();
        Quaternion worldRotationQuat = getWorldRotationQuat();

        if (!applyPhysicsLocal && spatial.getParent() != null) {
            localLocation.set(worldLocation).subtractLocal(spatial.getParent().getWorldTranslation());
            localLocation.divideLocal(spatial.getParent().getWorldScale());
            tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);

            localRotationQuat.set(worldRotationQuat);
            tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().mult(localRotationQuat, localRotationQuat);

            spatial.setLocalTranslation(localLocation);
            spatial.setLocalRotation(localRotationQuat);
        } else {
            spatial.setLocalTranslation(worldLocation);
            spatial.setLocalRotation(worldRotationQuat);
        }
        if (vehicle != null) {
            vehicle.updateWheels();
        }
        return true;
    }

    public Vector3f getWorldLocation() {
        motionState.getWorldTransform(worldTrans);
        Converter.convert(worldTrans.getOrigin(), worldLocation);
        return worldLocation;
    }

    public Matrix3f getWorldRotation() {
        motionState.getWorldTransform(worldTrans);
        Converter.convert(worldTrans.getBasis(), worldRotation);
        return worldRotation;
    }

    public Quaternion getWorldRotationQuat() {
        worldRotationQuat.fromRotationMatrix(getWorldRotation());
        return worldRotationQuat;
    }

    public void setVehicle(PhysicsVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public boolean isApplyPhysicsLocal() {
        return applyPhysicsLocal;
    }

    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        this.applyPhysicsLocal = applyPhysicsLocal;
    }

    public void setWorldTransform(Transform tr) {
        Converter.convert(tr, worldTrans);
        motionState.setWorldTransform(worldTrans);
    }


}
