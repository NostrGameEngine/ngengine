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
package com.jme3.bullet.objects;

   import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.bullet.types.btCollisionFlags;
import com.jme3.bullet.types.btCollisionObject;
import com.jme3.bullet.types.btMatrix3x3;
import com.jme3.bullet.types.btRigidBody;
import com.jme3.bullet.types.btRigidBodyConstructionInfo;
import com.jme3.bullet.types.btTransform;
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.types.btVector3;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>PhysicsRigidBody - Basic physics object</p>
 * @author normenhansen
 */
public class PhysicsRigidBody extends PhysicsCollisionObject {

    protected btRigidBodyConstructionInfo constructionInfo;
    protected btRigidBody rBody;
    protected RigidBodyMotionState motionState = new RigidBodyMotionState();
    protected float mass = 1.0f;
    protected boolean kinematic = false;
    protected final btVector3 tempVec = btUtils.newVector3(this,0, 0, 0);
    protected final  btVector3 tempVec2 =  btUtils.newVector3(this,0, 0, 0);
    protected final  btMatrix3x3 tempMatrix = btUtils.newMatrix3x3(this);
    //TEMP VARIABLES
    protected final  btVector3 localInertia = btUtils.newVector3(this,0, 0, 0);
    protected ArrayList<PhysicsJoint> joints = new ArrayList<>();
    protected Transform tmpJmeTransform=new Transform();
    protected PhysicsRigidBody() {
    }

    /**
     * Creates a new PhysicsRigidBody with the supplied collision shape
     *
     * @param shape the desired shape (not null, alias created)
     */
    public PhysicsRigidBody(CollisionShape shape) {
        collisionShape = shape;
        rebuildRigidBody();
    }

    public PhysicsRigidBody(CollisionShape shape, float mass) {
        collisionShape = shape;
        this.mass = mass;
        rebuildRigidBody();
    }

    /**
     * Builds/rebuilds the physics body when parameters have changed
     */
    protected void rebuildRigidBody() {
        boolean removed = false;
        if(collisionShape instanceof MeshCollisionShape && mass != 0){
            throw new IllegalStateException("Dynamic rigid body cannot have mesh collision shape!");
        }
        if (rBody != null) {
            if (rBody.isInWorld()) {
                PhysicsSpace.getPhysicsSpace().remove(this);
                removed = true;
            }
            btUtils.destroy(this,rBody);
            
        }
        preRebuild();
        rBody = btUtils.createRigidBody(this,constructionInfo);
        postRebuild();
        if (removed) {
            PhysicsSpace.getPhysicsSpace().add(this);
        }
    }

    protected void preRebuild() {
        collisionShape.calculateLocalInertia(mass, localInertia);
        if (constructionInfo == null) {
            constructionInfo = btUtils.createRigidBodyContructionInfo(this,mass, motionState.getBtMotionState(), collisionShape.getCShape(), localInertia);
        } else {
            constructionInfo.setMass(mass);
            constructionInfo.setCollisionShape(collisionShape.getCShape());
            constructionInfo.setMotionState(motionState.getBtMotionState());
        }
    }

    protected void postRebuild() {
        rBody.setUserPointer(this);
        if (mass == 0.0f) {
            rBody.setCollisionFlags(rBody.getCollisionFlags() | btCollisionFlags.STATIC_OBJECT);
        } else {
            rBody.setCollisionFlags(rBody.getCollisionFlags() & ~btCollisionFlags.STATIC_OBJECT);
        }
    }

    /**
     * @return the motionState
     */
    public RigidBodyMotionState getMotionState() {
        return motionState;
    }

    /**
     * Sets the physics object location
     * @param location the location of the actual physics object
     */
    public void setPhysicsLocation(Vector3f location) {
        btTransform tr=rBody.getCenterOfMassTransform();
        Converter.convert(location, tr.getOrigin());
        rBody.setCenterOfMassTransform(tr);
        Converter.convert(tr, tmpJmeTransform);
        motionState.setWorldTransform(tmpJmeTransform);
    }

    /**
     * Sets the physics object rotation
     * @param rotation the rotation of the actual physics object
     */
    public void setPhysicsRotation(Matrix3f rotation) {
        btTransform tr=rBody.getCenterOfMassTransform();
        Converter.convert(rotation, tr.getBasis());
        rBody.setCenterOfMassTransform(tr);
        Converter.convert(tr, tmpJmeTransform);
        motionState.setWorldTransform(tmpJmeTransform);
    }

    /**
     * Sets the physics object rotation
     * @param rotation the rotation of the actual physics object
     */
    public void setPhysicsRotation(Quaternion rotation) {
        btTransform tr = rBody.getCenterOfMassTransform();
        Converter.convert(rotation, tr.getBasis());
        rBody.setCenterOfMassTransform(tr);
        Converter.convert(tr, tmpJmeTransform);
        motionState.setWorldTransform(tmpJmeTransform);
    }

    /**
     * Gets the physics object location, instantiates a new Vector3f object
     * 
     * @return a new location vector (in physics-space coordinates, not null)
     */
    public Vector3f getPhysicsLocation() {
        return getPhysicsLocation(null);
    }

    /**
     * Gets the physics object rotation
     *
     * @return a new rotation matrix (in physics-space coordinates, not null)
     */
    public Matrix3f getPhysicsRotationMatrix() {
        return getPhysicsRotationMatrix(null);
    }

    /**
     * Gets the physics object location, no object instantiation
     * @param location the location of the actual physics object is stored in this Vector3f
     * @return a location vector (in physics-space coordinates, either
     * location or a new vector)
     */
    public Vector3f getPhysicsLocation(Vector3f location) {
        if (location == null) {
            location = new Vector3f();
        }
        btTransform tr=rBody.getCenterOfMassTransform();
        return Converter.convert(tr.getOrigin(), location);
    }

    /**
     * Gets the physics object rotation as a matrix, no conversions and no object instantiation
     * @param rotation the rotation of the actual physics object is stored in this Matrix3f
     * @return a rotation matrix (in physics-space coordinates, either
     * rotation or a new matrix)
     */
    public Matrix3f getPhysicsRotationMatrix(Matrix3f rotation) {
        if (rotation == null) {
            rotation = new Matrix3f();
        }
        btTransform tr=rBody.getCenterOfMassTransform();
         return Converter.convert(tr.getBasis(), rotation);
    }

    /**
     * Gets the physics object rotation as a quaternion, converts the bullet Matrix3f value,
     * instantiates new object
     *
     * @return a new rotation Quaternion (in physics-space coordinates)
     */
    public Quaternion getPhysicsRotation(){
        return getPhysicsRotation(null);
    }

    /**
     * Gets the physics object rotation as a quaternion, converts the bullet Matrix3f value
     * @param rotation the rotation of the actual physics object is stored in this Quaternion
     * @return a rotation Quaternion (in physics-space coordinates, either
     * rotation or a new instance)
     */
    public Quaternion getPhysicsRotation(Quaternion rotation){
        if (rotation == null) {
            rotation = new Quaternion();
        }
        btTransform tr=rBody.getCenterOfMassTransform();
        return Converter.convert(tr.getBasis(), rotation);
    }

    /**
     * Gets the physics object location
     * @param location the location of the actual physics object is stored in this Vector3f
     * @return a location vector (in physics-space coordinates, either
     * location or a new vector)
     */
    public Vector3f getInterpolatedPhysicsLocation(Vector3f location) {
        if (location == null) {
            location = new Vector3f();
        }
        btTransform tr=rBody.getInterpolationWorldTransform();
        return Converter.convert(tr.getOrigin(), location);
    }

    /**
     * Gets the physics object rotation
     * @param rotation the rotation of the actual physics object is stored in this Matrix3f
     * @return a rotation matrix (in physics-space coordinates, either
     * rotation or a new matrix)
     */
    public Matrix3f getInterpolatedPhysicsRotation(Matrix3f rotation) {
        if (rotation == null) {
            rotation = new Matrix3f();
        }
        btTransform tr=rBody.getInterpolationWorldTransform();
         return Converter.convert(tr.getBasis(), rotation);
    }

    /**
     * Sets the node to kinematic mode. in this mode the node is not affected by physics
     * but affects other physics objects. Its kinetic force is calculated by the amount
     * of movement it is exposed to and its weight.
     *
     * @param kinematic true&rarr;set kinematic mode, false&rarr;set dynamic
     * (default=false)
     */
    public void setKinematic(boolean kinematic) {
        this.kinematic = kinematic;
        if (kinematic) {
            rBody.setCollisionFlags(rBody.getCollisionFlags() | btCollisionFlags.KINEMATIC_OBJECT);
            rBody.setActivationState(btCollisionObject.DISABLE_DEACTIVATION);
        } else {
            rBody.setCollisionFlags(rBody.getCollisionFlags() & ~btCollisionFlags.KINEMATIC_OBJECT);
            rBody.setActivationState(btCollisionObject.ACTIVE_TAG);
        }
    }

    public boolean isKinematic() {
        return kinematic;
    }

    /**
     * Enable/disable this body's contact response.
     *
     * @param responsive true to respond to contacts, false to ignore them
     * (default=true)
     */
    public void setContactResponse(boolean responsive) {
        int flags = rBody.getCollisionFlags();
        if (responsive) {
            flags &= ~btCollisionFlags.NO_CONTACT_RESPONSE;
        } else {
            flags |= btCollisionFlags.NO_CONTACT_RESPONSE;
        }
        rBody.setCollisionFlags(flags);
    }

    /**
     * Test whether this body responds to contacts.
     *
     * @return true if responsive, otherwise false
     */
    public boolean isContactResponse() {
        int flags = rBody.getCollisionFlags();
        boolean result = (flags & btCollisionFlags.NO_CONTACT_RESPONSE) == 0x0;
        return result;
    }

    public void setCcdSweptSphereRadius(float radius) {
        rBody.setCcdSweptSphereRadius(radius);
    }

    /**
     * Sets the amount of motion that has to happen in one physics tick to trigger the continuous motion detection<br>
     * This avoids the problem of fast objects moving through other objects, set to zero to disable (default)
     *
     * @param threshold the desired minimum distance per timestep to trigger CCD
     * (in physics-space units, &gt;0) or zero to disable CCD (default=0)
     */
    public void setCcdMotionThreshold(float threshold) {
        rBody.setCcdMotionThreshold(threshold);
    }

    public float getCcdSweptSphereRadius() {
        return rBody.getCcdSweptSphereRadius();
    }

    public float getCcdMotionThreshold() {
        return rBody.getCcdMotionThreshold();
    }

    public float getCcdSquareMotionThreshold() {
        return rBody.getCcdSquareMotionThreshold();
    }

    public float getMass() {
        return mass;
    }

    /**
     * Sets the mass of this PhysicsRigidBody, objects with mass=0 are static.
     *
     * @param mass the desired mass (&gt;0) or 0 for a static body (default=1)
     */
    public void setMass(float mass) {
        this.mass = mass;
        if(collisionShape instanceof MeshCollisionShape && mass != 0){
            throw new IllegalStateException("Dynamic rigid body cannot have mesh collision shape!");
        }
        if (collisionShape != null) {
            collisionShape.calculateLocalInertia(mass, localInertia);
        }
        if (rBody != null) {
            rBody.setMassProps(mass, localInertia);
            if (mass == 0.0f) {
                rBody.setCollisionFlags(rBody.getCollisionFlags() | btCollisionFlags.STATIC_OBJECT);
            } else {
                rBody.setCollisionFlags(rBody.getCollisionFlags() & ~btCollisionFlags.STATIC_OBJECT);
            }
        }
    }

    public Vector3f getGravity() {
        return getGravity(null);
    }

    public Vector3f getGravity(Vector3f gravity) {
        if (gravity == null) {
            gravity = new Vector3f();
        }
        btVector3 g=  rBody.getGravity();
        return Converter.convert(g, gravity);
    }

    /**
     * Set the local gravity of this PhysicsRigidBody<br>
     * Set this after adding the node to the PhysicsSpace,
     * the PhysicsSpace assigns its current gravity to the physics node when it's added.
     * @param gravity the gravity vector to set
     */
    public void setGravity(Vector3f gravity) {
        rBody.setGravity(Converter.convert(gravity, tempVec));
    }

    public float getFriction() {
        return rBody.getFriction();
    }

    /**
     * Sets the friction of this physics object
     * @param friction the friction of this physics object
     */
    public void setFriction(float friction) {
        constructionInfo.setFriction(friction);
        rBody.setFriction(friction);
    }

    public void setDamping(float linearDamping, float angularDamping) {
        constructionInfo.setLinearDamping(linearDamping);
        constructionInfo.setAngularDamping(angularDamping);
        rBody.setDamping(linearDamping, angularDamping);
    }

    public void setLinearDamping(float linearDamping) {
        constructionInfo.setLinearDamping(linearDamping);
        rBody.setDamping(linearDamping, constructionInfo.getAngularDamping());
    }

    public void setAngularDamping(float angularDamping) {
        constructionInfo.setAngularDamping( angularDamping);
        rBody.setDamping(constructionInfo.getLinearDamping(), angularDamping);
    }

    public float getLinearDamping() {
        return constructionInfo.getLinearDamping();
    }

    public float getAngularDamping() {
        return constructionInfo.getAngularDamping();
    }

    public float getRestitution() {
        return rBody.getRestitution();
    }

    /**
     * The "bounciness" of the PhysicsRigidBody. Best performance with restitution=0.
     *
     * @param restitution the desired value (default=0)
     */
    public void setRestitution(float restitution) {
        constructionInfo.setRestitution( restitution);
        rBody.setRestitution(restitution);
    }

    /**
     * Get the current angular velocity of this PhysicsRigidBody
     * @return the current linear velocity
     */
    public Vector3f getAngularVelocity() {
        return Converter.convert(rBody.getAngularVelocity(),new Vector3f());
    }

    /**
     * Get the current angular velocity of this PhysicsRigidBody
     * @param vec the vector to store the velocity in
     */
    public void getAngularVelocity(Vector3f vec) {
        Converter.convert(rBody.getAngularVelocity(), vec);
    }

    /**
     * Sets the angular velocity of this PhysicsRigidBody
     * @param vec the angular velocity of this PhysicsRigidBody
     */
    public void setAngularVelocity(Vector3f vec) {
        rBody.setAngularVelocity(Converter.convert(vec, tempVec));
        rBody.activate();
    }

    /**
     * Get the current linear velocity of this PhysicsRigidBody
     * @return the current linear velocity
     */
    public Vector3f getLinearVelocity() {
        return Converter.convert(rBody.getLinearVelocity(),new Vector3f());
    }

    /**
     * Get the current linear velocity of this PhysicsRigidBody
     * @param vec the vector to store the velocity in
     */
    public void getLinearVelocity(Vector3f vec) {
        Converter.convert(rBody.getLinearVelocity(), vec);
    }

    /**
     * Sets the linear velocity of this PhysicsRigidBody
     * @param vec the linear velocity of this PhysicsRigidBody
     */
    public void setLinearVelocity(Vector3f vec) {
        rBody.setLinearVelocity(Converter.convert(vec, tempVec));
        rBody.activate();
    }

    /**
     * Apply a force to the PhysicsRigidBody, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse, use applyContinuousForce to apply continuous force.
     * @param force the force
     * @param location the location of the force
     */
    public void applyForce(final Vector3f force, final Vector3f location) {
        rBody.applyForce(Converter.convert(force, tempVec), Converter.convert(location, tempVec2));
        rBody.activate();
    }

    /**
     * Apply a force to the PhysicsRigidBody, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse.
     * 
     * @param force the force
     */
    public void applyCentralForce(final Vector3f force) {
        rBody.applyCentralForce(Converter.convert(force, tempVec));
        rBody.activate();
    }

    /**
     * Apply a force to the PhysicsRigidBody, only applies force if the next physics update call
     * updates the physics space.<br>
     * To apply an impulse, use applyImpulse.
     * 
     * @param torque the torque
     */
    public void applyTorque(final Vector3f torque) {
        rBody.applyTorque(Converter.convert(torque, tempVec));
        rBody.activate();
    }

    /**
     * Apply an impulse to the PhysicsRigidBody in the next physics update.
     * @param impulse applied impulse
     * @param relativePosition location relative to object
     */
    public void applyImpulse(final Vector3f impulse, final Vector3f relativePosition) {
        rBody.applyImpulse(Converter.convert(impulse, tempVec), Converter.convert(relativePosition, tempVec2));
        rBody.activate();
    }

    /**
     * Apply a torque impulse to the PhysicsRigidBody in the next physics update.
     *
     * @param vec the torque impulse vector (in physics-space coordinates,
     * not null, unaffected)
     */
    public void applyTorqueImpulse(final Vector3f vec) {
        rBody.applyTorqueImpulse(Converter.convert(vec, tempVec));
        rBody.activate();
    }

    /**
     * Clear all forces from the PhysicsRigidBody
     * 
     */
    public void clearForces() {
        rBody.clearForces();
    }

    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if(collisionShape instanceof MeshCollisionShape && mass!=0){
            throw new IllegalStateException("Dynamic rigid body cannot have mesh collision shape!");
        }
        if (rBody == null) {
            rebuildRigidBody();
        } else {
            collisionShape.calculateLocalInertia(mass, localInertia);
            constructionInfo.setCollisionShape( collisionShape.getCShape());
            rBody.setCollisionShape(collisionShape.getCShape());
        }
    }

    /**
     * reactivates this PhysicsRigidBody when it has been deactivated because it was not moving
     */
    public void activate() {
        rBody.activate();
    }

    public boolean isActive() {
        return rBody.isActive();
    }

    /**
     * sets the sleeping thresholds, these define when the object gets deactivated
     * to save resources. Low values keep the object active when it barely moves
     * @param linear the linear sleeping threshold
     * @param angular the angular sleeping threshold
     */
    public void setSleepingThresholds(float linear, float angular) {
        constructionInfo.setLinearSleepingThreshold(linear); 
        constructionInfo.setAngularSleepingThreshold(angular);  
        rBody.setSleepingThresholds(linear, angular);
    }

    public void setLinearSleepingThreshold(float linearSleepingThreshold) {
        constructionInfo.setLinearSleepingThreshold(linearSleepingThreshold); 
        rBody.setSleepingThresholds(linearSleepingThreshold, constructionInfo.getAngularSleepingThreshold());
    }

    public void setAngularSleepingThreshold(float angularSleepingThreshold) {
        constructionInfo.setAngularSleepingThreshold( angularSleepingThreshold);
        rBody.setSleepingThresholds(constructionInfo.getLinearSleepingThreshold(), angularSleepingThreshold);
    }

    public float getLinearSleepingThreshold() {
        return constructionInfo.getLinearSleepingThreshold();
    }

    public float getAngularSleepingThreshold() {
        return constructionInfo.getAngularSleepingThreshold();
    }

    @Deprecated
    public float getAngularFactor() {
        btVector3 f = rBody.getAngularFactor();
        return f.getX();
    }

    public Vector3f getAngularFactor(Vector3f out) {
        btVector3 f = rBody.getAngularFactor();
        out.x = f.getX();
        out.y = f.getY();
        out.z = f.getZ();
        return out;
    }

    @Deprecated
    public void setAngularFactor(float factor) {
        tempVec.setValue(factor, 0,0);
        rBody.setAngularFactor(tempVec);
    }

    public void setAngularFactor(Vector3f factor) {
        Converter.convert(factor, tempVec);
        rBody.setAngularFactor(tempVec);
    }

    
    /**
     * do not use manually, joints are added automatically
     *
     * @param joint the joint to add (not null, alias created)
     */
    public void addJoint(PhysicsJoint joint) {
        if (!joints.contains(joint)) {
            joints.add(joint);
        }
    }

    /**
     * @param joint the joint to remove (not null, unaffected)
     */
    public void removeJoint(PhysicsJoint joint) {
        joints.remove(joint);
    }

    /**
     * Returns a list of connected joints. This list is only filled when
     * the PhysicsRigidBody is actually added to the physics space or loaded from disk.
     * @return list of active joints connected to this PhysicsRigidBody
     */
    public List<PhysicsJoint> getJoints() {
        return joints;
    }

   
 

    /**
     * Serialize this body, for example when saving to a J3O file.
     *
     * @param e exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);

        capsule.write(getMass(), "mass", 1.0f);

        capsule.write(getGravity(), "gravity", Vector3f.ZERO);
        capsule.write(getFriction(), "friction", 0.5f);
        capsule.write(getRestitution(), "restitution", 0);
        capsule.write(getAngularFactor(), "angularFactor", 1);
        capsule.write(kinematic, "kinematic", false);

        capsule.write(constructionInfo.getLinearDamping(), "linearDamping", 0);
        capsule.write(constructionInfo.getAngularDamping(), "angularDamping", 0);
        capsule.write(constructionInfo.getLinearSleepingThreshold(), "linearSleepingThreshold", 0.8f);
        capsule.write(constructionInfo.getAngularSleepingThreshold(), "angularSleepingThreshold", 1.0f);

        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0);

        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation", new Vector3f());
        capsule.write(getPhysicsRotationMatrix(new Matrix3f()), "physicsRotation", new Matrix3f());
        capsule.write(getLinearVelocity(), "linearVelocity", null);
        capsule.write(getAngularVelocity(), "angularVelocity", null);

        capsule.writeSavableArrayList(joints, "joints", null);
    }

    /**
     * De-serialize this body, for example when loading from a J3O file.
     *
     * @param importer importer (not null)
     * @throws IOException from importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);

        InputCapsule capsule = importer.getCapsule(this);
        float mass = capsule.readFloat("mass", 1.0f);
        this.mass = mass;
        rebuildRigidBody();
        setGravity((Vector3f) capsule.readSavable("gravity", Vector3f.ZERO.clone()));
        setContactResponse(capsule.readBoolean("contactResponse", true));
        setFriction(capsule.readFloat("friction", 0.5f));
        setKinematic(capsule.readBoolean("kinematic", false));

        setRestitution(capsule.readFloat("restitution", 0));
        setAngularFactor(capsule.readFloat("angularFactor", 1));
        setDamping(capsule.readFloat("linearDamping", 0), capsule.readFloat("angularDamping", 0));
        setSleepingThresholds(capsule.readFloat("linearSleepingThreshold", 0.8f), capsule.readFloat("angularSleepingThreshold", 1.0f));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0));

        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation", new Vector3f()));
        setPhysicsRotation((Matrix3f) capsule.readSavable("physicsRotation", new Matrix3f()));
        setLinearVelocity((Vector3f) capsule.readSavable("linearVelocity", new Vector3f()));
        setAngularVelocity((Vector3f) capsule.readSavable("angularVelocity", new Vector3f()));

        joints = capsule.readSavableArrayList("joints", null);
    }

    public Object getBtObject() {
        return rBody;
    }
}
