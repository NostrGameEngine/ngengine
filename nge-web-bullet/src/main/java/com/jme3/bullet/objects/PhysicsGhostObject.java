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

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.types.btCollisionFlags;
import com.jme3.bullet.types.btCollisionObject;
import com.jme3.bullet.types.btPairCachingGhostObject;
import com.jme3.bullet.types.btQuaternion;
import com.jme3.bullet.types.btTransform;
import com.jme3.bullet.types.btUtils;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * <i>From Bullet manual:</i><br>
 * GhostObject can keep track of all objects that are overlapping.
 * By default, this overlap is based on the AABB.
 * This is useful for creating a character controller,
 * collision sensors/triggers, explosions etc.<br>
 * @author normenhansen
 */
public class PhysicsGhostObject extends PhysicsCollisionObject {

    protected btPairCachingGhostObject gObject;
    protected boolean locationDirty = false;
    //TEMP VARIABLES
    protected final Quaternion tmp_inverseWorldRotation = new Quaternion();
    private final  Transform physicsLocation = new  Transform();
    protected final  Quaternion tempRot = new Quaternion();
    private final  List<PhysicsCollisionObject> overlappingObjects = new LinkedList<>();

    protected PhysicsGhostObject() {
    }

    public PhysicsGhostObject(CollisionShape shape) {
        collisionShape = shape;
        buildObject();
    }

    public PhysicsGhostObject(Spatial child, CollisionShape shape) {
        collisionShape = shape;
        buildObject();
    }

    private void buildObject() {
        if (gObject == null) {
            gObject = btUtils.createPairCachingGhostObject(this);
            gObject.setCollisionFlags(gObject.getCollisionFlags() | btCollisionFlags.NO_CONTACT_RESPONSE);
        }
        gObject.setCollisionShape(collisionShape.getCShape());
        gObject.setUserPointer(this);
    }

    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if (gObject == null) {
            buildObject();
        }else{
            gObject.setCollisionShape(collisionShape.getCShape());
        }
    }

    /**
     * Sets the physics object location
     * @param location the location of the actual physics object
     */
    public void setPhysicsLocation(Vector3f location) {
        btTransform tr= gObject.getWorldTransform();
        Converter.convert(location, tr.getOrigin());
        gObject.setWorldTransform(tr);
    }

    /**
     * Sets the physics object rotation
     * @param rotation the rotation of the actual physics object
     */
    public void setPhysicsRotation(Matrix3f rotation) {
        btTransform tr=gObject.getWorldTransform();
        Converter.convert(rotation, tr.getBasis());
        gObject.setWorldTransform(tr);
    }

    /**
     * Sets the physics object rotation
     * @param rotation the rotation of the actual physics object
     */
    public void setPhysicsRotation(Quaternion rotation) {
        btTransform tr=gObject.getWorldTransform();
        Converter.convert(rotation, tr.getBasis());
        gObject.setWorldTransform(tr);
    }

    /**
     * @return the physicsLocation
     */
    public com.jme3.math.Transform getPhysicsTransform() {
        return physicsLocation;
    }

    /**
     * @param trans storage for the result (modified if not null)
     * @return the physicsLocation
     */
    public Vector3f getPhysicsLocation(Vector3f trans) {
        if (trans == null) {
            trans = new Vector3f();
        }
        btTransform tr=gObject.getWorldTransform();
        Converter.convert(tr.getOrigin(), physicsLocation.getTranslation());
        return trans.set(physicsLocation.getTranslation());
    }

    /**
     * @param rot storage for the result (modified if not null)
     * @return the physicsLocation
     */
    public Quaternion getPhysicsRotation(Quaternion rot) {
        if (rot == null) {
            rot = new Quaternion();
        }
        btTransform tr = gObject.getWorldTransform();
        btQuaternion quat = tr.getRotation();
        Converter.convert(quat, physicsLocation.getRotation());
        return rot.set(physicsLocation.getRotation());
    }

    /**
     * @param rot storage for the result (modified if not null)
     * @return the physicsLocation
     */
    public Matrix3f getPhysicsRotationMatrix(Matrix3f rot) {
        if (rot == null) {
            rot = new Matrix3f();
        }
        btTransform tr = gObject.getWorldTransform();
        btQuaternion quat = tr.getRotation();
        Converter.convert(quat, physicsLocation.getRotation());
        return rot.set(physicsLocation.getRotation());
    }

    /**
     * @return the physicsLocation
     */
    public Vector3f getPhysicsLocation() {
        btTransform tr = gObject.getWorldTransform();
        Converter.convert(tr.getOrigin(), physicsLocation.getTranslation());
        return physicsLocation.getTranslation();
    }

    /**
     * @return the physicsLocation
     */
    public Quaternion getPhysicsRotation() {
        btTransform tr = gObject.getWorldTransform();
        btQuaternion quat = tr.getRotation();
        Converter.convert(quat, physicsLocation.getRotation());
        return physicsLocation.getRotation();
    }

    public Matrix3f getPhysicsRotationMatrix() {
        btTransform tr = gObject.getWorldTransform();
        btQuaternion quat = tr.getRotation();
        Converter.convert(quat, physicsLocation.getRotation());
        return physicsLocation.getRotation().toRotationMatrix();
    }

    /**
     * used internally
     * 
     * @return the pre-existing instance
     */
    public Object getBtObject() {
        return gObject;
    }

 

    /**
     * Another Object is overlapping with this GhostNode,
     * if and if only there CollisionShapes overlaps.
     * They could be both regular PhysicsRigidBodys or PhysicsGhostObjects.
     * @return All CollisionObjects overlapping with this GhostNode.
     */
    public List<PhysicsCollisionObject> getOverlappingObjects() {
        overlappingObjects.clear();
        for (int i = 0; i < gObject.getNumOverlappingObjects();i++){
            btCollisionObject collObj = gObject.getOverlappingObject(i);
            overlappingObjects.add((PhysicsCollisionObject) collObj.getUserPointer());
        }
        return overlappingObjects;
    }

    /**
     *
     * @return With how many other CollisionObjects this GhostNode is currently overlapping.
     */
    public int getOverlappingCount() {
        return gObject.getNumOverlappingObjects();
    }

    /**
     *
     * @param index The index of the overlapping Node to retrieve.
     * @return The Overlapping CollisionObject at the given index.
     */
    public PhysicsCollisionObject getOverlapping(int index) {
        return overlappingObjects.get(index);
    }

    public void setCcdSweptSphereRadius(float radius) {
        gObject.setCcdSweptSphereRadius(radius);
    }

    public void setCcdMotionThreshold(float threshold) {
        gObject.setCcdMotionThreshold(threshold);
    }

    public float getCcdSweptSphereRadius() {
        return gObject.getCcdSweptSphereRadius();
    }

    public float getCcdMotionThreshold() {
        return gObject.getCcdMotionThreshold();
    }

    public float getCcdSquareMotionThreshold() {
        return gObject.getCcdSquareMotionThreshold();
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation", new Vector3f());
        capsule.write(getPhysicsRotationMatrix(new Matrix3f()), "physicsRotation", new Matrix3f());
        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0);
    }

    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        buildObject();
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation", new Vector3f()));
        setPhysicsRotation(((Matrix3f) capsule.readSavable("physicsRotation", new Matrix3f())));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0));
    }
}
