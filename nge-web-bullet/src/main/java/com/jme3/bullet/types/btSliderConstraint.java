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

public interface btSliderConstraint extends btTypedConstraint {

    float getLowerLinLimit();

    void setLowerLinLimit(float lowerLinLimit);

    float getUpperLinLimit();

    void setUpperLinLimit(float upperLinLimit);

    float getLowerAngLimit();

    void setLowerAngLimit(float lowerAngLimit);

    float getUpperAngLimit();

    void setUpperAngLimit(float upperAngLimit);

    float getSoftnessDirLin();

    void setSoftnessDirLin(float softnessDirLin);

    float getRestitutionDirLin();

    void setRestitutionDirLin(float restitutionDirLin);

    float getDampingDirLin();

    void setDampingDirLin(float dampingDirLin);

    float getSoftnessDirAng();

    void setSoftnessDirAng(float softnessDirAng);

    float getRestitutionDirAng();

    void setRestitutionDirAng(float restitutionDirAng);

    float getDampingDirAng();

    void setDampingDirAng(float dampingDirAng);

    float getSoftnessLimLin();

    void setSoftnessLimLin(float softnessLimLin);

    float getRestitutionLimLin();

    void setRestitutionLimLin(float restitutionLimLin);

    float getDampingLimLin();

    void setDampingLimLin(float dampingLimLin);

    float getSoftnessLimAng();

    void setSoftnessLimAng(float softnessLimAng);

    float getRestitutionLimAng();

    void setRestitutionLimAng(float restitutionLimAng);

    float getDampingLimAng();

    void setDampingLimAng(float dampingLimAng);

    float getSoftnessOrthoLin();

    void setSoftnessOrthoLin(float softnessOrthoLin);

    float getRestitutionOrthoLin();

    void setRestitutionOrthoLin(float restitutionOrthoLin);

    float getDampingOrthoLin();

    void setDampingOrthoLin(float dampingOrthoLin);

    float getSoftnessOrthoAng();

    void setSoftnessOrthoAng(float softnessOrthoAng);

    float getRestitutionOrthoAng();

    void setRestitutionOrthoAng(float restitutionOrthoAng);

    float getDampingOrthoAng();

    void setDampingOrthoAng(float dampingOrthoAng);

    boolean getPoweredLinMotor();

    void setPoweredLinMotor(boolean poweredLinMotor);

    float getTargetLinMotorVelocity();

    void setTargetLinMotorVelocity(float targetLinMotorVelocity);

    float getMaxLinMotorForce();

    void setMaxLinMotorForce(float maxLinMotorForce);

    boolean getPoweredAngMotor();

    void setPoweredAngMotor(boolean poweredAngMotor);

    float getTargetAngMotorVelocity();

    void setTargetAngMotorVelocity(float targetAngMotorVelocity);

    float getMaxAngMotorForce();

    void setMaxAngMotorForce(float maxAngMotorForce);
    
}
