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
package com.jme3.bullet.collision;

 import com.jme3.bullet.types.btManifoldPoint;
import com.jme3.bullet.util.Converter;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.EventObject;

import org.teavm.jso.JSObject;

/**
 * A CollisionEvent stores all information about a collision in the PhysicsWorld.
 * Do not store this Object, as it will be reused after the collision() method has been called.
 * Get/reference all data you need in the collide method.
 * @author normenhansen
 */
public class PhysicsCollisionEvent extends EventObject {

    public static final int TYPE_ADDED = 0;
    public static final int TYPE_PROCESSED = 1;
    public static final int TYPE_DESTROYED = 2;
    private int type;
    private PhysicsCollisionObject nodeA;
    private PhysicsCollisionObject nodeB;
    private btManifoldPoint cp;

    public PhysicsCollisionEvent(int type, PhysicsCollisionObject source, PhysicsCollisionObject nodeB, btManifoldPoint cp) {
        super(source);
        this.type = type;
        this.nodeA = source;
        this.nodeB = nodeB;
        this.cp = cp;
    }

    /**
     * used by event factory, called when event is destroyed
     */
    public void clean() {
        source = null;
        type = 0;
        nodeA = null;
        nodeB = null;
        cp = null;
    }

    /**
     * used by event factory, called when event reused
     *
     * @param type the desired type
     * @param source the desired first object (alias created)
     * @param nodeB the desired 2nd object (alias created)
     * @param cp the desired manifold (alias created)
     */
    public void refactor(int type, PhysicsCollisionObject source, PhysicsCollisionObject nodeB, btManifoldPoint cp) {
        this.source = source;
        this.type = type;
        this.nodeA = source;
        this.nodeB = nodeB;
        this.cp = cp;
    }

    public int getType() {
        return type;
    }

    /**
     * @return A Spatial if the UserObject of the PhysicsCollisionObject is a Spatial
     */
    public Spatial getNodeA() {
        if (nodeA.getUserObject() instanceof Spatial) {
            return (Spatial) nodeA.getUserObject();
        }
        return null;
    }

    /**
     * @return A Spatial if the UserObject of the PhysicsCollisionObject is a Spatial
     */
    public Spatial getNodeB() {
        if (nodeB.getUserObject() instanceof Spatial) {
            return (Spatial) nodeB.getUserObject();
        }
        return null;
    }

    public PhysicsCollisionObject getObjectA() {
        return nodeA;
    }

    public PhysicsCollisionObject getObjectB() {
        return nodeB;
    }

    public float getAppliedImpulse() {
        return cp.getAppliedImpulse();
    }

    public float getAppliedImpulseLateral1() {
        return cp.getAppliedImpulseLateral1();
    }

    public float getAppliedImpulseLateral2() {
        return cp.getAppliedImpulseLateral2();
    }

    public float getCombinedFriction() {
        return cp.getCombinedFriction();
    }

    public float getCombinedRestitution() {
        return cp.getCombinedRestitution();
    }

    public float getDistance1() {
        return cp.getDistance1();
    }

    public int getIndex0() {
        return cp.getIndex0();
    }

    public int getIndex1() {
        return cp.getIndex1();
    }

    public Vector3f getLateralFrictionDir1() {
        return Converter.convert(cp.getLateralFrictionDir1(),new Vector3f());
    }

    public Vector3f getLateralFrictionDir2() {
        return Converter.convert(cp.getLateralFrictionDir2(),new Vector3f());
    }

    public boolean isLateralFrictionInitialized() {
        return cp.isLateralFrictionInitialized();
    }

    public int getLifeTime() {
        return cp.getLifeTime();
    }

    public Vector3f getLocalPointA() {
        return Converter.convert(cp.getLocalPointA(),new Vector3f());
    }

    public Vector3f getLocalPointB() {
        return Converter.convert(cp.getLocalPointB(),new Vector3f());
    }

    public Vector3f getNormalWorldOnB() {
        return Converter.convert(cp.getNormalWorldOnB(),new Vector3f());
    }

    public int getPartId0() {
        return cp.getPartId0();
    }

    public int getPartId1() {
        return cp.getPartId1();
    }

    public Vector3f getPositionWorldOnA() {
        return Converter.convert(cp.getPositionWorldOnA(),new Vector3f());
    }

    public Vector3f getPositionWorldOnB() {
        return Converter.convert(cp.getPositionWorldOnB(),new Vector3f());
    }

    // public JSObject getUserPersistentData() {
    //     return cp.getUserPersistentData();
    // }
}
