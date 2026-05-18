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

import com.jme3.bullet.types.btDestructibleImpl;
import com.jme3.bullet.types.btTranslationalLimitMotor;
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.types.btVector3;
import com.jme3.bullet.util.Converter;
import com.jme3.math.Vector3f;

/**
 *
 * @author normenhansen
 */
public class TranslationalLimitMotor extends btDestructibleImpl {

    private btTranslationalLimitMotor motor;

    public TranslationalLimitMotor(btTranslationalLimitMotor motor) {
        this.motor = motor;
    }

    public btTranslationalLimitMotor getMotor() {
        return motor;
    }

    public Vector3f getLowerLimit() {
        return Converter.convert(motor.getLowerLimit(),new Vector3f());
    }

    public void setLowerLimit(Vector3f lowerLimit) {
        btVector3 v = Converter.convert(lowerLimit, btUtils.newVector3(this, 0, 0, 0));
        motor.setLowerLimit(v);
        btUtils.destroy(this, v);
    }

    public Vector3f getUpperLimit() {
        return Converter.convert(motor.getUpperLimit(),new Vector3f());
    }

    public void setUpperLimit(Vector3f upperLimit) {
        btVector3 v = Converter.convert(upperLimit, btUtils.newVector3(this, 0, 0, 0));
        motor.setUpperLimit(v);
        btUtils.destroy(this, v);
    }

    public Vector3f getAccumulatedImpulse() {
        return Converter.convert(motor.getAccumulatedImpulse(),new Vector3f());
    }

    public void setAccumulatedImpulse(Vector3f accumulatedImpulse) {
        btVector3 v = Converter.convert(accumulatedImpulse, btUtils.newVector3(this, 0, 0, 0));
        motor.setAccumulatedImpulse(v);
        btUtils.destroy(this, v);
    }

    public float getLimitSoftness() {
        return motor.getLimitSoftness();
    }

    public void setLimitSoftness(float limitSoftness) {
        motor.setLimitSoftness(limitSoftness);
    }

    public float getDamping() {
        return motor.getDamping();
    }

    public void setDamping(float damping) {
        motor.setDamping(damping);
    }

    public float getRestitution() {
        return motor.getRestitution();
    }

    public void setRestitution(float restitution) {
        motor.setRestitution(restitution);
    }
}
