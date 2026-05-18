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
package com.jme3.bullet.joints;


import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.io.IOException;

import com.jme3.bullet.types.btMatrix3x3;
import com.jme3.bullet.types.btRigidBody;
import com.jme3.bullet.types.btSliderConstraint;
import com.jme3.bullet.types.btTransform;
import com.jme3.bullet.types.btUtils;

/**
 * <i>From bullet manual:</i><br>
 * The slider constraint allows the body to rotate around one axis and translate along this axis.
 * @author normenhansen
 */
public class SliderJoint extends PhysicsJoint {
    protected Matrix3f rotA, rotB;
    protected boolean useLinearReferenceFrameA;

    protected SliderJoint() {
    }

    /**
     * @param nodeA the body for the A end (not null, alias created)
     * @param nodeB the body for the B end (not null, alias created)
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     * @param rotA the joint orientation in A's local coordinates (not null,
     * alias unaffected)
     * @param rotB the joint orientation in B's local coordinates (not null,
     * alias unaffected)
     * @param useLinearReferenceFrameA true&rarr;use body A, false&rarr;use body
     * B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA=rotA;
        this.rotB=rotB;
        this.useLinearReferenceFrameA=useLinearReferenceFrameA;
        createJoint();
    }

    /**
     * @param nodeA the body for the A end (not null, alias created)
     * @param nodeB the body for the B end (not null, alias created)
     * @param pivotA local translation of the joint connection point in node A
     * @param pivotB local translation of the joint connection point in node B
     * @param useLinearReferenceFrameA true&rarr;use body A, false&rarr;use body
     * B
     */
    public SliderJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB, boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB);
        this.rotA=new Matrix3f();
        this.rotB=new Matrix3f();
        this.useLinearReferenceFrameA=useLinearReferenceFrameA;
        createJoint();
    }

    public float getLowerLinLimit() {
        return ((btSliderConstraint) getConstraint()).getLowerLinLimit();
    }

    public void setLowerLinLimit(float lowerLinLimit) {
        ((btSliderConstraint) getConstraint()).setLowerLinLimit(lowerLinLimit);
    }

    public float getUpperLinLimit() {
        return ((btSliderConstraint) getConstraint()).getUpperLinLimit();
    }

    public void setUpperLinLimit(float upperLinLimit) {
        ((btSliderConstraint) getConstraint()).setUpperLinLimit(upperLinLimit);
    }

    public float getLowerAngLimit() {
        return ((btSliderConstraint) getConstraint()).getLowerAngLimit();
    }

    public void setLowerAngLimit(float lowerAngLimit) {
        ((btSliderConstraint) getConstraint()).setLowerAngLimit(lowerAngLimit);
    }

    public float getUpperAngLimit() {
        return ((btSliderConstraint) getConstraint()).getUpperAngLimit();
    }

    public void setUpperAngLimit(float upperAngLimit) {
        ((btSliderConstraint) getConstraint()).setUpperAngLimit(upperAngLimit);
    }

    public float getSoftnessDirLin() {
        return ((btSliderConstraint) getConstraint()).getSoftnessDirLin();
    }

    public void setSoftnessDirLin(float softnessDirLin) {
        ((btSliderConstraint) getConstraint()).setSoftnessDirLin(softnessDirLin);
    }

    public float getRestitutionDirLin() {
        return ((btSliderConstraint) getConstraint()).getRestitutionDirLin();
    }

    public void setRestitutionDirLin(float restitutionDirLin) {
        ((btSliderConstraint) getConstraint()).setRestitutionDirLin(restitutionDirLin);
    }

    public float getDampingDirLin() {
        return ((btSliderConstraint) getConstraint()).getDampingDirLin();
    }

    public void setDampingDirLin(float dampingDirLin) {
        ((btSliderConstraint) getConstraint()).setDampingDirLin(dampingDirLin);
    }

    public float getSoftnessDirAng() {
        return ((btSliderConstraint) getConstraint()).getSoftnessDirAng();
    }

    public void setSoftnessDirAng(float softnessDirAng) {
        ((btSliderConstraint) getConstraint()).setSoftnessDirAng(softnessDirAng);
    }

    public float getRestitutionDirAng() {
        return ((btSliderConstraint) getConstraint()).getRestitutionDirAng();
    }

    public void setRestitutionDirAng(float restitutionDirAng) {
        ((btSliderConstraint) getConstraint()).setRestitutionDirAng(restitutionDirAng);
    }

    public float getDampingDirAng() {
        return ((btSliderConstraint) getConstraint()).getDampingDirAng();
    }

    public void setDampingDirAng(float dampingDirAng) {
        ((btSliderConstraint) getConstraint()).setDampingDirAng(dampingDirAng);
    }

    public float getSoftnessLimLin() {
        return ((btSliderConstraint) getConstraint()).getSoftnessLimLin();
    }

    public void setSoftnessLimLin(float softnessLimLin) {
        ((btSliderConstraint) getConstraint()).setSoftnessLimLin(softnessLimLin);
    }

    public float getRestitutionLimLin() {
        return ((btSliderConstraint) getConstraint()).getRestitutionLimLin();
    }

    public void setRestitutionLimLin(float restitutionLimLin) {
        ((btSliderConstraint) getConstraint()).setRestitutionLimLin(restitutionLimLin);
    }

    public float getDampingLimLin() {
        return ((btSliderConstraint) getConstraint()).getDampingLimLin();
    }

    public void setDampingLimLin(float dampingLimLin) {
        ((btSliderConstraint) getConstraint()).setDampingLimLin(dampingLimLin);
    }

    public float getSoftnessLimAng() {
        return ((btSliderConstraint) getConstraint()).getSoftnessLimAng();
    }

    public void setSoftnessLimAng(float softnessLimAng) {
        ((btSliderConstraint) getConstraint()).setSoftnessLimAng(softnessLimAng);
    }

    public float getRestitutionLimAng() {
        return ((btSliderConstraint) getConstraint()).getRestitutionLimAng();
    }

    public void setRestitutionLimAng(float restitutionLimAng) {
        ((btSliderConstraint) getConstraint()).setRestitutionLimAng(restitutionLimAng);
    }

    public float getDampingLimAng() {
        return ((btSliderConstraint) getConstraint()).getDampingLimAng();
    }

    public void setDampingLimAng(float dampingLimAng) {
        ((btSliderConstraint) getConstraint()).setDampingLimAng(dampingLimAng);
    }

    public float getSoftnessOrthoLin() {
        return ((btSliderConstraint) getConstraint()).getSoftnessOrthoLin();
    }

    public void setSoftnessOrthoLin(float softnessOrthoLin) {
        ((btSliderConstraint) getConstraint()).setSoftnessOrthoLin(softnessOrthoLin);
    }

    public float getRestitutionOrthoLin() {
        return ((btSliderConstraint) getConstraint()).getRestitutionOrthoLin();
    }

    public void setRestitutionOrthoLin(float restitutionOrthoLin) {
        ((btSliderConstraint) getConstraint()).setRestitutionOrthoLin(restitutionOrthoLin);
    }

    public float getDampingOrthoLin() {
        return ((btSliderConstraint) getConstraint()).getDampingOrthoLin();
    }

    public void setDampingOrthoLin(float dampingOrthoLin) {
        ((btSliderConstraint) getConstraint()).setDampingOrthoLin(dampingOrthoLin);
    }

    public float getSoftnessOrthoAng() {
        return ((btSliderConstraint) getConstraint()).getSoftnessOrthoAng();
    }

    public void setSoftnessOrthoAng(float softnessOrthoAng) {
        ((btSliderConstraint) getConstraint()).setSoftnessOrthoAng(softnessOrthoAng);
    }

    public float getRestitutionOrthoAng() {
        return ((btSliderConstraint) getConstraint()).getRestitutionOrthoAng();
    }

    public void setRestitutionOrthoAng(float restitutionOrthoAng) {
        ((btSliderConstraint) getConstraint()).setRestitutionOrthoAng(restitutionOrthoAng);
    }

    public float getDampingOrthoAng() {
        return ((btSliderConstraint) getConstraint()).getDampingOrthoAng();
    }

    public void setDampingOrthoAng(float dampingOrthoAng) {
        ((btSliderConstraint) getConstraint()).setDampingOrthoAng(dampingOrthoAng);
    }

    public boolean isPoweredLinMotor() {
        return ((btSliderConstraint) getConstraint()).getPoweredLinMotor();
    }

    public void setPoweredLinMotor(boolean poweredLinMotor) {
        ((btSliderConstraint) getConstraint()).setPoweredLinMotor(poweredLinMotor);
    }

    public float getTargetLinMotorVelocity() {
        return ((btSliderConstraint) getConstraint()).getTargetLinMotorVelocity();
    }

    public void setTargetLinMotorVelocity(float targetLinMotorVelocity) {
        ((btSliderConstraint) getConstraint()).setTargetLinMotorVelocity(targetLinMotorVelocity);
    }

    public float getMaxLinMotorForce() {
        return ((btSliderConstraint) getConstraint()).getMaxLinMotorForce();
    }

    public void setMaxLinMotorForce(float maxLinMotorForce) {
        ((btSliderConstraint) getConstraint()).setMaxLinMotorForce(maxLinMotorForce);
    }

    public boolean isPoweredAngMotor() {
        return ((btSliderConstraint) getConstraint()).getPoweredAngMotor();
    }

    public void setPoweredAngMotor(boolean poweredAngMotor) {
        ((btSliderConstraint) getConstraint()).setPoweredAngMotor(poweredAngMotor);
    }

    public float getTargetAngMotorVelocity() {
        return ((btSliderConstraint) getConstraint()).getTargetAngMotorVelocity();
    }

    public void setTargetAngMotorVelocity(float targetAngMotorVelocity) {
        ((btSliderConstraint) getConstraint()).setTargetAngMotorVelocity(targetAngMotorVelocity);
    }

    public float getMaxAngMotorForce() {
        return ((btSliderConstraint) getConstraint()).getMaxAngMotorForce();
    }

    public void setMaxAngMotorForce(float maxAngMotorForce) {
        ((btSliderConstraint) getConstraint()).setMaxAngMotorForce(maxAngMotorForce);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        //TODO: standard values..
        btSliderConstraint constraint = (btSliderConstraint) getConstraint();
        capsule.write(((btSliderConstraint) constraint).getDampingDirAng(), "dampingDirAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getDampingDirLin(), "dampingDirLin", 0f);
        capsule.write(((btSliderConstraint) constraint).getDampingLimAng(), "dampingLimAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getDampingLimLin(), "dampingLimLin", 0f);
        capsule.write(((btSliderConstraint) constraint).getDampingOrthoAng(), "dampingOrthoAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getDampingOrthoLin(), "dampingOrthoLin", 0f);
        capsule.write(((btSliderConstraint) constraint).getLowerAngLimit(), "lowerAngLimit", 0f);
        capsule.write(((btSliderConstraint) constraint).getLowerLinLimit(), "lowerLinLimit", 0f);
        capsule.write(((btSliderConstraint) constraint).getMaxAngMotorForce(), "maxAngMotorForce", 0f);
        capsule.write(((btSliderConstraint) constraint).getMaxLinMotorForce(), "maxLinMotorForce", 0f);
        capsule.write(((btSliderConstraint) constraint).getPoweredAngMotor(), "poweredAngMotor", false);
        capsule.write(((btSliderConstraint) constraint).getPoweredLinMotor(), "poweredLinMotor", false);
        capsule.write(((btSliderConstraint) constraint).getRestitutionDirAng(), "restitutionDirAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getRestitutionDirLin(), "restitutionDirLin", 0f);
        capsule.write(((btSliderConstraint) constraint).getRestitutionLimAng(), "restitutionLimAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getRestitutionLimLin(), "restitutionLimLin", 0f);
        capsule.write(((btSliderConstraint) constraint).getRestitutionOrthoAng(), "restitutionOrthoAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getRestitutionOrthoLin(), "restitutionOrthoLin", 0f);

        capsule.write(((btSliderConstraint) constraint).getSoftnessDirAng(), "softnessDirAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getSoftnessDirLin(), "softnessDirLin", 0f);
        capsule.write(((btSliderConstraint) constraint).getSoftnessLimAng(), "softnessLimAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getSoftnessLimLin(), "softnessLimLin", 0f);
        capsule.write(((btSliderConstraint) constraint).getSoftnessOrthoAng(), "softnessOrthoAng", 0f);
        capsule.write(((btSliderConstraint) constraint).getSoftnessOrthoLin(), "softnessOrthoLin", 0f);

        capsule.write(((btSliderConstraint) constraint).getTargetAngMotorVelocity(), "targetAngMotorVelicoty", 0f);
        capsule.write(((btSliderConstraint) constraint).getTargetLinMotorVelocity(), "targetLinMotorVelicoty", 0f);

        capsule.write(((btSliderConstraint) constraint).getUpperAngLimit(), "upperAngLimit", 0f);
        capsule.write(((btSliderConstraint) constraint).getUpperLinLimit(), "upperLinLimit", 0f);

        capsule.write(useLinearReferenceFrameA, "useLinearReferenceFrameA", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        float dampingDirAng = capsule.readFloat("dampingDirAng", 0f);
        float dampingDirLin = capsule.readFloat("dampingDirLin", 0f);
        float dampingLimAng = capsule.readFloat("dampingLimAng", 0f);
        float dampingLimLin = capsule.readFloat("dampingLimLin", 0f);
        float dampingOrthoAng = capsule.readFloat("dampingOrthoAng", 0f);
        float dampingOrthoLin = capsule.readFloat("dampingOrthoLin", 0f);
        float lowerAngLimit = capsule.readFloat("lowerAngLimit", 0f);
        float lowerLinLimit = capsule.readFloat("lowerLinLimit", 0f);
        float maxAngMotorForce = capsule.readFloat("maxAngMotorForce", 0f);
        float maxLinMotorForce = capsule.readFloat("maxLinMotorForce", 0f);
        boolean poweredAngMotor = capsule.readBoolean("poweredAngMotor", false);
        boolean poweredLinMotor = capsule.readBoolean("poweredLinMotor", false);
        float restitutionDirAng = capsule.readFloat("restitutionDirAng", 0f);
        float restitutionDirLin = capsule.readFloat("restitutionDirLin", 0f);
        float restitutionLimAng = capsule.readFloat("restitutionLimAng", 0f);
        float restitutionLimLin = capsule.readFloat("restitutionLimLin", 0f);
        float restitutionOrthoAng = capsule.readFloat("restitutionOrthoAng", 0f);
        float restitutionOrthoLin = capsule.readFloat("restitutionOrthoLin", 0f);

        float softnessDirAng = capsule.readFloat("softnessDirAng", 0f);
        float softnessDirLin = capsule.readFloat("softnessDirLin", 0f);
        float softnessLimAng = capsule.readFloat("softnessLimAng", 0f);
        float softnessLimLin = capsule.readFloat("softnessLimLin", 0f);
        float softnessOrthoAng = capsule.readFloat("softnessOrthoAng", 0f);
        float softnessOrthoLin = capsule.readFloat("softnessOrthoLin", 0f);

        float targetAngMotorVelicoty = capsule.readFloat("targetAngMotorVelicoty", 0f);
        float targetLinMotorVelicoty = capsule.readFloat("targetLinMotorVelicoty", 0f);

        float upperAngLimit = capsule.readFloat("upperAngLimit", 0f);
        float upperLinLimit = capsule.readFloat("upperLinLimit", 0f);

        useLinearReferenceFrameA = capsule.readBoolean("useLinearReferenceFrameA", false);

        createJoint();

        btSliderConstraint constraint = (btSliderConstraint) getConstraint();

        ((btSliderConstraint)constraint).setDampingDirAng(dampingDirAng);
        ((btSliderConstraint)constraint).setDampingDirLin(dampingDirLin);
        ((btSliderConstraint)constraint).setDampingLimAng(dampingLimAng);
        ((btSliderConstraint)constraint).setDampingLimLin(dampingLimLin);
        ((btSliderConstraint)constraint).setDampingOrthoAng(dampingOrthoAng);
        ((btSliderConstraint)constraint).setDampingOrthoLin(dampingOrthoLin);
        ((btSliderConstraint)constraint).setLowerAngLimit(lowerAngLimit);
        ((btSliderConstraint)constraint).setLowerLinLimit(lowerLinLimit);
        ((btSliderConstraint)constraint).setMaxAngMotorForce(maxAngMotorForce);
        ((btSliderConstraint)constraint).setMaxLinMotorForce(maxLinMotorForce);
        ((btSliderConstraint)constraint).setPoweredAngMotor(poweredAngMotor);
        ((btSliderConstraint)constraint).setPoweredLinMotor(poweredLinMotor);
        ((btSliderConstraint)constraint).setRestitutionDirAng(restitutionDirAng);
        ((btSliderConstraint)constraint).setRestitutionDirLin(restitutionDirLin);
        ((btSliderConstraint)constraint).setRestitutionLimAng(restitutionLimAng);
        ((btSliderConstraint)constraint).setRestitutionLimLin(restitutionLimLin);
        ((btSliderConstraint)constraint).setRestitutionOrthoAng(restitutionOrthoAng);
        ((btSliderConstraint)constraint).setRestitutionOrthoLin(restitutionOrthoLin);

        ((btSliderConstraint)constraint).setSoftnessDirAng(softnessDirAng);
        ((btSliderConstraint)constraint).setSoftnessDirLin(softnessDirLin);
        ((btSliderConstraint)constraint).setSoftnessLimAng(softnessLimAng);
        ((btSliderConstraint)constraint).setSoftnessLimLin(softnessLimLin);
        ((btSliderConstraint)constraint).setSoftnessOrthoAng(softnessOrthoAng);
        ((btSliderConstraint)constraint).setSoftnessOrthoLin(softnessOrthoLin);

        ((btSliderConstraint)constraint).setTargetAngMotorVelocity(targetAngMotorVelicoty);
        ((btSliderConstraint)constraint).setTargetLinMotorVelocity(targetLinMotorVelicoty);

        ((btSliderConstraint)constraint).setUpperAngLimit(upperAngLimit);
        ((btSliderConstraint)constraint).setUpperLinLimit(upperLinLimit);
    }

    private void createJoint() {
        btTransform transA = btUtils.newTransform(this);
        Converter.convert(pivotA, transA.getOrigin());
        Converter.convert(rotA, transA.getBasis());
         
        btTransform transB = btUtils.newTransform(this);
        Converter.convert(pivotB, transB.getOrigin());
        Converter.convert(rotB, transB.getBasis());

        btSliderConstraint constraint = btUtils.createSliderConstraint(this, (btRigidBody) nodeA.getBtObject(), (btRigidBody) nodeB.getBtObject(), transA, transB,
                useLinearReferenceFrameA);
        setConstraint(constraint);

        btUtils.destroy(this,transA);
        btUtils.destroy(this,transB);
    }
}
