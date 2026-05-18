/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.bullet.joints.motors;

import com.jme3.bullet.types.btRotationalLimitMotor;
public class RotationalLimitMotor {

    private btRotationalLimitMotor motor;

    public RotationalLimitMotor(btRotationalLimitMotor motor) {
        this.motor = motor;
    }

    public btRotationalLimitMotor getMotor() {
        return motor;
    }

    public float getLoLimit() {
        return motor.getLoLimit();
    }

    public void setLoLimit(float loLimit) {
        motor.setLoLimit(loLimit);
    }

    public float getHiLimit() {
        return motor.getHiLimit();
    }

    public void setHiLimit(float hiLimit) {
        motor.setHiLimit(hiLimit);
    }

    public float getTargetVelocity() {
        return motor.getTargetVelocity();
    }

    public void setTargetVelocity(float targetVelocity) {
        motor.setTargetVelocity(targetVelocity);
    }

    public float getMaxMotorForce() {
        return motor.getMaxMotorForce();
    }

    public void setMaxMotorForce(float maxMotorForce) {
        motor.setMaxMotorForce(maxMotorForce);
    }

    public float getMaxLimitForce() {
        return motor.getMaxLimitForce();
    }

    public void setMaxLimitForce(float maxLimitForce) {
        motor.setMaxLimitForce(maxLimitForce);
    }

    public float getDamping() {
        return motor.getDamping();
    }

    public void setDamping(float damping) {
        motor.setDamping(damping);
    }

    public float getLimitSoftness() {
        return motor.getLimitSoftness();
    }

    public void setLimitSoftness(float limitSoftness) {
        motor.setLimitSoftness(limitSoftness);
    }

    public float getERP() {
        return motor.getERP();
    }

    public void setERP(float ERP) {
        motor.setERP(ERP);
    }

    public float getBounce() {
        return motor.getBounce();
    }

    public void setBounce(float bounce) {
        motor.setBounce(bounce);
    }

    public boolean isEnableMotor() {
        return motor.getEnableMotor();
    }

    public void setEnableMotor(boolean enableMotor) {
        motor.setEnableMotor(enableMotor);
    }
}