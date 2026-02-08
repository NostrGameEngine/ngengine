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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;

import com.jme3.bullet.objects.infos.RigidBodyMotionState;
import com.jme3.util.BufferUtils;

public class btUtils {

    public static void init(boolean wait) {
        AtomicBoolean monitor = new AtomicBoolean(false);
        _init(() -> {
            monitor.set(true);
        });
        while (!monitor.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
    }

    // init ammo and wait
    @JSBody(params = { "callback" }, script = "Ammo().then((impl)=>{console.info('Ammo loaded');(typeof window !== 'undefined'?window:self).AmmoImpl=impl;callback();});")
    public static native void _init(btCallback callback);

    // new btVector3
    @JSBody(params = { "x", "y", "z" }, script = "return new AmmoImpl.btVector3(x,y,z);")
    private static native btVector3 _newVector3(float x, float y, float z);

    public static btVector3 newVector3(btDestructible p, float x, float y, float z) {
        btVector3 v = _newVector3(x, y, z);
        if(p!=null)p.gc(v);
        return v;
    }

    // new btQuaternion
    @JSBody(params = { "x", "y", "z", "w" }, script = "return new AmmoImpl.btQuaternion(x,y,z,w);")
    private static native btQuaternion _newQuaternion(float x, float y, float z, float w);

    public static btQuaternion newQuaternion(btDestructible p, float x, float y, float z, float w) {
        btQuaternion q = _newQuaternion(x, y, z, w);
        if(p!=null)p.gc(q);
        return q;        
    }

    // new btTransform
    @JSBody(params = { "q", "v" }, script = "return new AmmoImpl.btTransform(q,v);")
    private static native btTransform _newTransform(btQuaternion q, btVector3 v);

    public static btTransform newTransform(btDestructible p, btQuaternion q, btVector3 v) {
        btTransform t = _newTransform(q, v);
        if(p!=null)p.gc(t);
        return t;
    }

    // new btMatrix3x3()
    @JSBody(script = "return new AmmoImpl.btMatrix3x3();")
    private static native btMatrix3x3 _newMatrix3x3();
    
    public static btMatrix3x3 newMatrix3x3(btDestructible p) {
        btMatrix3x3 m = _newMatrix3x3();
        if(p!=null)p.gc(m);
        return m;
    }

    // new Transform(Matrix3x3)
    @JSBody(params = { "m" }, script = "return new AmmoImpl.btTransform(m);")
    private static native btTransform _newTransform(btMatrix3x3 m);

    public static btTransform _newTransform(btDestructible p, btMatrix3x3 m) {
        btTransform t = _newTransform(m);
        if(p!=null)p.gc(t);
        return t;
    }

    @JSBody(params = {}, script =  "var tr=new AmmoImpl.btTransform();tr.setIdentity();return tr;")
    private static native btTransform _newTransform();

    public static btTransform newTransform(btDestructible p) {
        btTransform t = _newTransform();
        if(p!=null)p.gc(t);
        return t;
    }

    // Box shape
    @JSBody(params = { "halfExtents" }, script = "return new AmmoImpl.btBoxShape(halfExtents);")
    private static native btConvexShape _createBoxCollisionShape(btVector3 halfExtents);

    public static btConvexShape createBoxCollisionShape(btDestructible p, btVector3 halfExtents) {
        btConvexShape s = _createBoxCollisionShape(halfExtents);
        if(p!=null)p.gc(s);
        return s;
    }

    // Capsule shape

    @JSBody(params = { "radius", "height" }, script = "return new AmmoImpl.btCapsuleShape(radius,height);")
    private static native btConvexShape _createCapsuleCollisionShape(float radius, float height);

    public static btConvexShape createCapsuleCollisionShape(btDestructible p, float radius, float height) {
        btConvexShape s = _createCapsuleCollisionShape(radius, height);
        if(p!=null)p.gc(s);
        return s;
    }

    @JSBody(params = { "radius", "height" }, script = "return new AmmoImpl.btCapsuleShapeX(radius,height);")
    private static native btConvexShape _createCapsuleCollisionShapeX(float radius, float height);

    public static btConvexShape createCapsuleCollisionShapeX(btDestructible p, float radius, float height) {
        btConvexShape s = _createCapsuleCollisionShapeX(radius, height);
        if(p!=null)p.gc(s);
        return s;
    }

    @JSBody(params = { "radius", "height" }, script = "return new AmmoImpl.btCapsuleShapeZ(radius,height);")
    private static native btConvexShape _createCapsuleCollisionShapeZ(float radius, float height);

    public static btConvexShape createCapsuleCollisionShapeZ(btDestructible p, float radius, float height) {
        btConvexShape s = _createCapsuleCollisionShapeZ(radius, height);
        if(p!=null)p.gc(s);
        return s;
    }

    // Cone shape

    @JSBody(params = { "radius", "height" }, script = "return new AmmoImpl.btConeShape(radius,height);")
    private static native btConvexShape _createConeCollisionShape(float radius, float height);

    public static btConvexShape createConeCollisionShape(btDestructible p, float radius, float height) {
        btConvexShape s = _createConeCollisionShape(radius, height);
        if(p!=null)p.gc(s);
        return s;
    }

    @JSBody(params = { "radius", "height" }, script = "return new AmmoImpl.btConeShapeX(radius,height);")
    private static native btConvexShape _createConeCollisionShapeX(float radius, float height);

    public static btConvexShape createConeCollisionShapeX(btDestructible p, float radius, float height) {
        btConvexShape s = _createConeCollisionShapeX(radius, height);
        if(p!=null)p.gc(s);
        return s;
    }

    @JSBody(params = { "radius", "height" }, script = "return new AmmoImpl.btConeShapeZ(radius,height);")
    private static native btConvexShape _createConeCollisionShapeZ(float radius, float height);

    public static btConvexShape createConeCollisionShapeZ(btDestructible p, float radius, float height) {
        btConvexShape s = _createConeCollisionShapeZ(radius, height);
        if(p!=null)p.gc(s);
        return s;
    }

    // Cylinder shape

    @JSBody(params = { "halfExtents" }, script = "return new AmmoImpl.btCylinderShape(halfExtents);")
    private static native btConvexShape _createCylinderCollisionShape(btVector3 halfExtents);

    public static btConvexShape createCylinderCollisionShape(btDestructible p, btVector3 halfExtents) {
        btConvexShape s = _createCylinderCollisionShape(halfExtents);
        if(p!=null)p.gc(s);
        return s;
    }

    @JSBody(params = { "halfExtents" }, script = "return new AmmoImpl.btCylinderShapeX(halfExtents);")
    private static native btConvexShape _createCylinderCollisionShapeX(btVector3 halfExtents);

    public static btConvexShape createCylinderCollisionShapeX(btDestructible p, btVector3 halfExtents) {
        btConvexShape s = _createCylinderCollisionShapeX(halfExtents);
        if(p!=null)p.gc(s);
        return s;
    }

    @JSBody(params = { "halfExtents" }, script = "return new AmmoImpl.btCylinderShapeZ(halfExtents);")
    private static native btConvexShape _createCylinderCollisionShapeZ(btVector3 halfExtents);

    public static btConvexShape createCylinderCollisionShapeZ(btDestructible p, btVector3 halfExtents) {
        btConvexShape s = _createCylinderCollisionShapeZ(halfExtents);
        if(p!=null)p.gc(s);
        return s;
    }

    // Hull collision shape

    @JSBody(params = { "vertices","nPoints"}, script = "return new AmmoImpl.btConvexHullShape(vertices,nPoints);")
    private static native btConvexShape _createConvexHullCollisionShape(float[] points,int nPoints);

    public static btConvexShape createConvexHullCollisionShape(btDestructible p, float points[]) {
        btConvexShape s = _createConvexHullCollisionShape(points,points.length/3);
        if(p!=null)p.gc(s);
        return s;
    }

    // StaticPlaneShape

    @JSBody(params = { "planeNormal", "planeConstant" }, script = "return new AmmoImpl.btStaticPlaneShape(planeNormal,planeConstant);")
    private static native btConcaveShape _createStaticPlaneCollisionShape(btVector3 planeNormal, float planeConstant);

    public static btConcaveShape createStaticPlaneCollisionShape(btDestructible p, btVector3 planeNormal, float planeConstant) {
        btConcaveShape s = _createStaticPlaneCollisionShape(planeNormal, planeConstant);
        if(p!=null)p.gc(s);
        return s;
    }

    // SphereCollisionShape

    @JSBody(params = { "radius" }, script = "return new AmmoImpl.btSphereShape(radius);")
    private static native btConvexShape _createSphereCollisionShape(float radius);

    public static btConvexShape createSphereCollisionShape(btDestructible p, float radius) {
        btConvexShape s = _createSphereCollisionShape(radius);
        if(p!=null)p.gc(s);
        return s;
    }

    // btBU_Simplex1to4 (const btVector3 &pt0)
    @JSBody(params = { "pt0" }, script = "return new AmmoImpl.btBU_Simplex1to4(pt0);")
    private static native btConvexShape _createSimplex1to4(btVector3 pt0);

    public static btConvexShape createSimplex1to4(btDestructible p, btVector3 pt0) {
        btConvexShape s = _createSimplex1to4(pt0);
        if(p!=null)p.gc(s);
        return s;
    }

    // btBU_Simplex1to4 (const btVector3 &pt0, const btVector3 &pt1)
    @JSBody(params = { "pt0", "pt1" }, script = "return new AmmoImpl.btBU_Simplex1to4(pt0,pt1);")
    private static native btConvexShape _createSimplex1to4(btVector3 pt0, btVector3 pt1);

    public static btConvexShape createSimplex1to4(btDestructible p, btVector3 pt0, btVector3 pt1) {
        btConvexShape s = _createSimplex1to4(pt0, pt1);
        if(p!=null)p.gc(s);
        return s;
    }

    // btBU_Simplex1to4 (const btVector3 &pt0, const btVector3 &pt1, const
    // btVector3 &pt2)
    @JSBody(params = { "pt0", "pt1", "pt2" }, script = "return new AmmoImpl.btBU_Simplex1to4(pt0,pt1,pt2);")
    private static native btConvexShape _createSimplex1to4(btVector3 pt0, btVector3 pt1, btVector3 pt2);

    public static btConvexShape createSimplex1to4(btDestructible p, btVector3 pt0, btVector3 pt1, btVector3 pt2) {
        btConvexShape s = _createSimplex1to4(pt0, pt1, pt2);
        if(p!=null)p.gc(s);
        return s;
    }

    // btBU_Simplex1to4 (const btVector3 &pt0, const btVector3 &pt1, const
    // btVector3 &pt2, const btVector3 &pt3)
    @JSBody(params = { "pt0", "pt1", "pt2", "pt3" }, script = "return new AmmoImpl.btBU_Simplex1to4(pt0,pt1,pt2,pt3);")
    private static native btConvexShape _createSimplex1to4(btVector3 pt0, btVector3 pt1, btVector3 pt2, btVector3 pt3);

    public static btConvexShape createSimplex1to4(btDestructible p, btVector3 pt0, btVector3 pt1, btVector3 pt2, btVector3 pt3) {
        btConvexShape s = _createSimplex1to4(pt0, pt1, pt2, pt3);
        if(p!=null)p.gc(s);
        return s;
    }

    // btHeightfieldTerrainShape (int heightStickWidth, int heightStickLength,
    // const void *heightfieldData, btScalar heightScale, btScalar minHeight,
    // btScalar maxHeight, int upAxis, PHY_ScalarType heightDataType, bool
    // flipQuadEdges)
    @JSBody(params = { "heightStickWidth", "heightStickLength", "heightfieldData", "heightScale", "minHeight", "maxHeight", "upAxis", "heightDataType",
            "flipQuadEdges" }, script = "return new AmmoImpl.btHeightfieldTerrainShape(heightStickWidth,heightStickLength,heightfieldData,heightScale,minHeight,maxHeight,upAxis,heightDataType,flipQuadEdges);")
    private static native btHeightfieldTerrainShape _createHeightfieldTerrainShape(int heightStickWidth, int heightStickLength, int heightfieldData, float heightScale,
            float minHeight, float maxHeight, int upAxis, String heightDataType, boolean flipQuadEdges);

    public static btHeightfieldTerrainShape createHeightfieldTerrainShape(btDestructible p, int heightStickWidth, int heightStickLength, int heightfieldData,
            float heightScale, float minHeight, float maxHeight, int upAxis, String heightDataType, boolean flipQuadEdges) {
        btHeightfieldTerrainShape s = _createHeightfieldTerrainShape(heightStickWidth, heightStickLength, heightfieldData, heightScale, minHeight, maxHeight, upAxis,
                heightDataType, flipQuadEdges);
        if(p!=null)p.gc(s);
        return s;
    }

    // btTriangleIndexVertexArray (int numTriangles, int *triangleIndexBase, int
    // triangleIndexStride, int numVertices, btScalar *vertexBase, int
    // vertexStride)
    @JSBody(params = { "numTriangles", "triangleIndexBase", "triangleIndexStride", "numVertices", "vertexBase",
            "vertexStride" }, script = "return new AmmoImpl.btTriangleIndexVertexArray(numTriangles,triangleIndexBase,triangleIndexStride,numVertices,vertexBase,vertexStride);")
    private static native btTriangleIndexVertexArray _createTriangleIndexVertexArray(int numTriangles, int triangleIndexBase, int triangleIndexStride, int numVertices,
            int vertexBase, int vertexStride);
    
    public static btTriangleIndexVertexArray createTriangleIndexVertexArray(btDestructible p, int numTriangles, int triangleIndexBase, int triangleIndexStride, int numVertices,
            int vertexBase, int vertexStride) {
        btTriangleIndexVertexArray s = _createTriangleIndexVertexArray(numTriangles, triangleIndexBase, triangleIndexStride, numVertices, vertexBase, vertexStride);
        if(p!=null)p.gc(s);
        return s;
    }

    // btBvhTriangleMeshShape::btBvhTriangleMeshShape ( btStridingMeshInterface
    // * meshInterface,bool useQuantizedAabbCompression,bool buildBvh=true)

    @JSBody(params = { "meshInterface", "useQuantizedAabbCompression",
            "buildBvh" }, script = "return new AmmoImpl.btBvhTriangleMeshShape(meshInterface,useQuantizedAabbCompression,buildBvh);")
    private static native btConcaveShape _createBvhTriangleMeshShape(btTriangleMesh meshInterface, boolean useQuantizedAabbCompression, boolean buildBvh);

    public static btConcaveShape createBvhTriangleMeshShape(btDestructible p, btTriangleMesh meshInterface, boolean useQuantizedAabbCompression,
            boolean buildBvh) {
        btConcaveShape s = _createBvhTriangleMeshShape(meshInterface, useQuantizedAabbCompression, buildBvh);
        if(p!=null)p.gc(s);
        return s;
    }

    // btGImpactMeshShape (btStridingMeshInterface *meshInterface)
    @JSBody(params = { "meshInterface" }, script = "return new AmmoImpl.btGImpactMeshShape(meshInterface);")
    private static native btConcaveShape _createGImpactMeshShape(btTriangleMesh meshInterface);

    public static btConcaveShape createGImpactMeshShape(btDestructible p, btTriangleMesh meshInterface) {
        btConcaveShape s = _createGImpactMeshShape(meshInterface);
        if(p!=null)p.gc(s);
        return s;
    }

    // AmmoImpl._malloc(size)
    @JSBody(params = { "size" }, script = "return AmmoImpl._malloc(size);")
    private static native int _malloc(int size);

    public static int malloc(btDestructible p, int size) {
        int s = _malloc(size);
        if(p!=null)p.gc(s);
        return s;
    }

    // AmmoImpl.HEAPF32[x]=y
    @JSBody(params = { "x", "y" }, script = "AmmoImpl.HEAPF32[x]=y;")
    public static native void setHEAPF32(int x, float y);

    // set i32
    @JSBody(params = { "x", "y" }, script = "AmmoImpl.HEAP32[x]=y;")
    public static native void setHEAP32(int x, int y);

    // AmmoImpl.HEAPF32[x]
    @JSBody(params = { "x" }, script = "return AmmoImpl.HEAPF32[x];")
    public static native float getHEAPF32(int x);

    // AmmoImpl.HEAP32[x]
    @JSBody(params = { "x" }, script = "return AmmoImpl.HEAP32[x];")
    public static native int getHEAP32(int x);

    // btCompoundShape ()
    @JSBody(params = {}, script = "return new AmmoImpl.btCompoundShape();")
    private static native btCollisionShape _createCompoundShape();

    public static btCollisionShape createCompoundShape(btDestructible p) {
        btCollisionShape s = _createCompoundShape();
        if(p!=null)p.gc(s);
        return s;
    }

    // btRigidBody (const btRigidBodyConstructionInfo &constructionInfo)
    @JSBody(params = { "constructionInfo" }, script = "return new AmmoImpl.btRigidBody(constructionInfo);")
    private static native btRigidBody _createRigidBody(btRigidBodyConstructionInfo constructionInfo);

    public static btRigidBody createRigidBody(btDestructible p, btRigidBodyConstructionInfo constructionInfo) {
        btRigidBody s = _createRigidBody(constructionInfo);
        if(p!=null)p.gc(s);
        return s;
    }

    // new btRigidBodyConstructionInfo (btScalar mass, btMotionState
    // *motionState,btCollisionShape *collisionShape, const btVector3
    // localInertia) \
    @JSBody(params = { "mass", "motionState", "cShape", "localInertia" }, script = "return new AmmoImpl.btRigidBodyConstructionInfo(mass,motionState,cShape,localInertia);")
    private static native btRigidBodyConstructionInfo _createRigidBodyContructionInfo(float mass, btMotionState motionState, btCollisionShape cShape, btVector3 localInertia);

    public static btRigidBodyConstructionInfo createRigidBodyContructionInfo(btDestructible p, float mass, btMotionState motionState, btCollisionShape cShape, btVector3 localInertia) {
        btRigidBodyConstructionInfo s = _createRigidBodyContructionInfo(mass, motionState, cShape, localInertia);
        if(p!=null)p.gc(s);
        return s;
    }

    // new btDefaultMotionState()
    @JSBody(params = {}, script = "return new AmmoImpl.btDefaultMotionState();")
    private static native btMotionState _createDefaultMotionState();

    public static btMotionState createDefaultMotionState(btDestructible p) {
        btMotionState s = _createDefaultMotionState();
        if(p!=null)p.gc(s);
        return s;
    }

    // btPairCachingGhostObject ()
    @JSBody(params = {}, script = "return new AmmoImpl.btPairCachingGhostObject();")
    private static native btPairCachingGhostObject _createPairCachingGhostObject();

    public static btPairCachingGhostObject createPairCachingGhostObject(btDestructible p) {
        btPairCachingGhostObject s = _createPairCachingGhostObject();
        if(p!=null)p.gc(s);
        return s;
    }

    // btKinematicCharacterController (btPairCachingGhostObject *ghostObject,
    // btConvexShape *convexShape, btScalar stepHeight, const btVector3
    // &up=btVector3(1.0, 0.0, 0.0))
    @JSBody(params = { "ghostObject", "convexShape", "stepHeight", "up" }, script = "return new AmmoImpl.btKinematicCharacterController(ghostObject,convexShape,stepHeight,up);")
    private static native btKinematicCharacterController _createKinematicCharacterController(btPairCachingGhostObject ghostObject, btCollisionShape convexShape, float stepHeight,
            btVector3 up);

    public static btKinematicCharacterController createKinematicCharacterController(btDestructible p, btPairCachingGhostObject ghostObject, btCollisionShape convexShape,
            float stepHeight, btVector3 up) {
        btKinematicCharacterController s = _createKinematicCharacterController(ghostObject, convexShape, stepHeight, up);
        if (p != null) p.gc(s);
        return s;
    }
    

    @JSBody(params = { "ghostObject", "convexShape", "stepHeight", }, script = "return new AmmoImpl.btKinematicCharacterController(ghostObject,convexShape,stepHeight);")
    private static native btKinematicCharacterController _createKinematicCharacterController(btPairCachingGhostObject ghostObject, btCollisionShape convexShape, float stepHeight);

    public static btKinematicCharacterController createKinematicCharacterController(btDestructible p, btPairCachingGhostObject ghostObject, btCollisionShape convexShape,
            float stepHeight) {
        btKinematicCharacterController s = _createKinematicCharacterController(ghostObject, convexShape, stepHeight);
        if (p != null) p.gc(s);
        return s;
    }

    // btVehicleTuning ()\
    @JSBody(params = {}, script = "return new AmmoImpl.btVehicleTuning();")
    private static native btVehicleTuning _createVehicleTuning();

    public static btVehicleTuning createVehicleTuning(btDestructible p) {
        btVehicleTuning s = _createVehicleTuning();
        if (p != null) p.gc(s);
        return s;
    }

    @JSBody(params = { "physicsWorld" }, script = "return new AmmoImpl.btDefaultVehicleRaycaster(physicsWorld);")
    private static native btVehicleRaycaster _createVehicleRaycaster(btDynamicsWorld physicsWorld);

    public static btVehicleRaycaster createVehicleRaycaster(btDestructible p, btDynamicsWorld physicsWorld) {
        btVehicleRaycaster s = _createVehicleRaycaster(physicsWorld);
        if (p != null) p.gc(s);
        return s;
    }

    @JSBody(params = { "tuning", "body", "rayCaster" }, script = "return new AmmoImpl.btRaycastVehicle(tuning,body,rayCaster);")
    private static native btRaycastVehicle _createRaycastVehicle(btVehicleTuning tuning, btRigidBody body, btVehicleRaycaster rayCaster);

    public static btRaycastVehicle createRaycastVehicle(btDestructible p, btVehicleTuning tuning, btRigidBody body, btVehicleRaycaster rayCaster) {
        btRaycastVehicle s = _createRaycastVehicle(tuning, body, rayCaster);
        if (p != null) p.gc(s);
        return s;
    }

    // btPoint2PointConstraint (btRigidBody &rbA, btRigidBody &rbB, const
    // btVector3 &pivotInA, const btVector3 &pivotInB)
    @JSBody(params = { "rbA", "rbB", "pivotInA", "pivotInB" }, script = "return new AmmoImpl.btPoint2PointConstraint(rbA,rbB,pivotInA,pivotInB);")
    private static native btPoint2PointConstraint _createPoint2PointConstraint(btRigidBody rbA, btRigidBody rbB, btVector3 pivotInA, btVector3 pivotInB);

    public static btPoint2PointConstraint createPoint2PointConstraint(btDestructible p, btRigidBody rbA, btRigidBody rbB, btVector3 pivotInA, btVector3 pivotInB) {
        btPoint2PointConstraint s = _createPoint2PointConstraint(rbA, rbB, pivotInA, pivotInB);
        if (p != null) p.gc(s);
        return s;
    }

    // btSliderConstraint (btRigidBody &rbA, btRigidBody &rbB, const btTransform
    // &frameInA, const btTransform &frameInB, bool useLinearReferenceFrameA)
    @JSBody(params = { "rbA", "rbB", "frameInA", "frameInB",
            "useLinearReferenceFrameA" }, script = "return new AmmoImpl.btSliderConstraint(rbA,rbB,frameInA,frameInB,useLinearReferenceFrameA);")
    private static native btSliderConstraint _createSliderConstraint(btRigidBody rbA, btRigidBody rbB, btTransform frameInA, btTransform frameInB,
            boolean useLinearReferenceFrameA);
    
    public static btSliderConstraint createSliderConstraint(btDestructible p, btRigidBody rbA, btRigidBody rbB, btTransform frameInA, btTransform frameInB,
            boolean useLinearReferenceFrameA) {
        btSliderConstraint s = _createSliderConstraint(rbA, rbB, frameInA, frameInB, useLinearReferenceFrameA);
        if (p != null) p.gc(s);
        return s;
    }

    // btConeTwistConstraint (btRigidBody &rbA, btRigidBody &rbB, const
    // btTransform &rbAFrame, const btTransform &rbBFrame)
    @JSBody(params = { "rbA", "rbB", "rbAFrame", "rbBFrame" }, script = "return new AmmoImpl.btConeTwistConstraint(rbA,rbB,rbAFrame,rbBFrame);")
    private static native btConeTwistConstraint _createConeTwistConstraint(btRigidBody rbA, btRigidBody rbB, btTransform rbAFrame, btTransform rbBFrame);

    public static btConeTwistConstraint createConeTwistConstraint(btDestructible p, btRigidBody rbA, btRigidBody rbB, btTransform rbAFrame, btTransform rbBFrame) {
        btConeTwistConstraint s = _createConeTwistConstraint(rbA, rbB, rbAFrame, rbBFrame);
        if (p != null) p.gc(s);
        return s;
    }

    // btGeneric6DofConstraint (btRigidBody &rbA, btRigidBody &rbB, const
    // btTransform &frameInA, const btTransform &frameInB, bool
    // useLinearReferenceFrameA)
    @JSBody(params = { "rbA", "rbB", "frameInA", "frameInB",
            "useLinearReferenceFrameA" }, script = "return new AmmoImpl.btGeneric6DofConstraint(rbA,rbB,frameInA,frameInB,useLinearReferenceFrameA);")
    private static native btGeneric6DofConstraint _createGeneric6DofConstraint(btRigidBody rbA, btRigidBody rbB, btTransform frameInA, btTransform frameInB,
            boolean useLinearReferenceFrameA);
    
    public static btGeneric6DofConstraint createGeneric6DofConstraint(btDestructible p, btRigidBody rbA, btRigidBody rbB, btTransform frameInA, btTransform frameInB,
            boolean useLinearReferenceFrameA) {
        btGeneric6DofConstraint s = _createGeneric6DofConstraint(rbA, rbB, frameInA, frameInB, useLinearReferenceFrameA);
        if (p != null) p.gc(s);
        return s;
    }

    // btHingeConstraint (btRigidBody &rbA, btRigidBody &rbB, const btVector3
    // &pivotInA, const btVector3 &pivotInB, const btVector3 &axisInA, const
    // btVector3 &axisInB, bool useReferenceFrameA=false)
    @JSBody(params = { "rbA", "rbB", "pivotInA", "pivotInB", "axisInA", "axisInB",
            "useReferenceFrameA" }, script = "return new AmmoImpl.btHingeConstraint(rbA,rbB,pivotInA,pivotInB,axisInA,axisInB,useReferenceFrameA);")
    private static native btHingeConstraint _createHingeConstraint(btRigidBody rbA, btRigidBody rbB, btVector3 pivotInA, btVector3 pivotInB, btVector3 axisInA, btVector3 axisInB,
            boolean useReferenceFrameA);

    public static btHingeConstraint createHingeConstraint(btDestructible p, btRigidBody rbA, btRigidBody rbB, btVector3 pivotInA, btVector3 pivotInB, btVector3 axisInA, btVector3 axisInB,
            boolean useReferenceFrameA) {
        btHingeConstraint s = _createHingeConstraint(rbA, rbB, pivotInA, pivotInB, axisInA, axisInB, useReferenceFrameA);
        if (p != null) p.gc(s);
        return s;
    }

    // btDefaultCollisionConfiguration ()
    @JSBody(params = {}, script = "return new AmmoImpl.btDefaultCollisionConfiguration();")
    private static native btDefaultCollisionConfiguration _createDefaultCollisionConfiguration();

    public static btDefaultCollisionConfiguration createDefaultCollisionConfiguration(btDestructible p) {
        btDefaultCollisionConfiguration s = _createDefaultCollisionConfiguration();
        if (p != null) p.gc(s);
        return s;
    }

    // btCollisionDispatcher::btCollisionDispatcher ( btCollisionConfiguration *
    // collisionConfiguration )\
    @JSBody(params = { "collisionConfiguration" }, script = "return new AmmoImpl.btCollisionDispatcher(collisionConfiguration);")
    private static native btCollisionDispatcher _createCollisionDispatcher(btDefaultCollisionConfiguration collisionConfiguration);

    public static btCollisionDispatcher createCollisionDispatcher(btDestructible p, btDefaultCollisionConfiguration collisionConfiguration) {
        btCollisionDispatcher s = _createCollisionDispatcher(collisionConfiguration);
        if (p != null) p.gc(s);
        return s;
    }

    // btAxisSweep3 (const btVector3 &worldAabbMin, const btVector3
    // &worldAabbMax
    @JSBody(params = { "worldAabbMin", "worldAabbMax" }, script = "return new AmmoImpl.btAxisSweep3(worldAabbMin,worldAabbMax);")
    private static native btAxisSweep3 _createAxisSweep3(btVector3 worldAabbMin, btVector3 worldAabbMax);

    public static btAxisSweep3 createAxisSweep3(btDestructible p, btVector3 worldAabbMin, btVector3 worldAabbMax) {
        btAxisSweep3 s = _createAxisSweep3(worldAabbMin, worldAabbMax);
        if (p != null) p.gc(s);
        return s;
    }

    // btDbvtBroadphase()
    @JSBody(params = {}, script = "return new AmmoImpl.btDbvtBroadphase();")
    private static native btDbvtBroadphase _createDbvtBroadphase();

    public static btDbvtBroadphase createDbvtBroadphase(btDestructible p) {
        btDbvtBroadphase s = _createDbvtBroadphase();
        if (p != null) p.gc(s);
        return s;
    }

    // btSequentialImpulseConstraintSolver ()
    @JSBody(params = {}, script = "return new AmmoImpl.btSequentialImpulseConstraintSolver();")
    private static native btConstraintSolver _createSequentialImpulseConstraintSolver();

    public static btConstraintSolver createSequentialImpulseConstraintSolver(btDestructible p) {
        btConstraintSolver s = _createSequentialImpulseConstraintSolver();
        if (p != null) p.gc(s);
        return s;
    }

    // void btDiscreteDynamicsWorld(btDispatcher dispatcher,
    // btBroadphaseInterface
    // pairCache, btConstraintSolver constraintSolver, btCollisionConfiguration
    // collisionConfiguration);
    @JSBody(params = { "dispatcher", "pairCache", "constraintSolver",
            "collisionConfiguration" }, script = "return new AmmoImpl.btDiscreteDynamicsWorld(dispatcher,pairCache,constraintSolver,collisionConfiguration);")
    private static native btDiscreteDynamicsWorld _createDiscreteDynamicsWorld(btCollisionDispatcher dispatcher, btBroadphaseInterface pairCache, btConstraintSolver constraintSolver,
            btDefaultCollisionConfiguration collisionConfiguration);

    public static btDiscreteDynamicsWorld createDiscreteDynamicsWorld(btDestructible p, btCollisionDispatcher dispatcher, btBroadphaseInterface pairCache, btConstraintSolver constraintSolver,
            btDefaultCollisionConfiguration collisionConfiguration) {
        btDiscreteDynamicsWorld s = _createDiscreteDynamicsWorld(dispatcher, pairCache, constraintSolver, collisionConfiguration);
        if (p != null) p.gc(s);
        return s;
    }

    @JSBody(params = { "dispatcher" }, script = "AmmoImpl.btGImpactCollisionAlgorithm.prototype.registerAlgorithm(dispatcher)")
    public static native void registerGImpactCollisionAlgorithm(btCollisionDispatcher dispatcher);

    // btGhostPairCallback ()
    @JSBody(params = {}, script = "return new AmmoImpl.btGhostPairCallback();")
    private static native btGhostPairCallback _createGhostPairCallback();

    public static btGhostPairCallback createGhostPairCallback(btDestructible p) {
        btGhostPairCallback s = _createGhostPairCallback();
        if (p != null) p.gc(s);
        return s;
    }

    // AllHitsRayResultCallback (const btVector3 &rayFromWorld, const btVector3
    // &rayToWorld)
    @JSBody(params = { "rayFromWorld", "rayToWorld" }, script = "return new AmmoImpl.AllHitsRayResultCallback(rayFromWorld,rayToWorld);")
    private static native btAllHitsRayResultCallback _createAllHitsRayResultCallback(btVector3 rayFromWorld, btVector3 rayToWorld);

    public static btAllHitsRayResultCallback createAllHitsRayResultCallback(btDestructible p, btVector3 rayFromWorld, btVector3 rayToWorld) {
        btAllHitsRayResultCallback s = _createAllHitsRayResultCallback(rayFromWorld, rayToWorld);
        if (p != null) p.gc(s);
        return s;
    }

    // void ClosestConvexResultCallback([Const, Ref] btVector3 convexFromWorld,
    // [Const, Ref] btVector3 convexToWorld);
    @JSBody(params = { "convexFromWorld", "convexToWorld" }, script = "return new AmmoImpl.ClosestConvexResultCallback(convexFromWorld,convexToWorld);")
    private static native btClosestConvexResultCallback _createClosestConvexResultCallback(btVector3 convexFromWorld, btVector3 convexToWorld);

    public static btClosestConvexResultCallback createClosestConvexResultCallback(btDestructible p, btVector3 convexFromWorld, btVector3 convexToWorld) {
        btClosestConvexResultCallback s = _createClosestConvexResultCallback(convexFromWorld, convexToWorld);
        if (p != null) p.gc(s);
        return s;
    }

    // btTriangleMesh
    @JSBody(params = {}, script = "return new AmmoImpl.btTriangleMesh(true,false);")
    private static native btTriangleMesh _createTriangleMesh();

    public static btTriangleMesh createTriangleMesh(btDestructible p) {
        btTriangleMesh s = _createTriangleMesh();
        if (p != null) p.gc(s);
        return s;
    }

    // btIndexedMesh ()
    @JSBody(params = {}, script = "return new AmmoImpl.btIndexedMesh();")
    private static native btIndexedMesh _createIndexedMesh();

    public static btIndexedMesh createIndexedMesh(btDestructible p) {
        btIndexedMesh s = _createIndexedMesh();
        if (p != null) p.gc(s);
        return s;
    }

    public static ByteBuffer allocToByteArrayI32(long triangleIndexBase, int lengthInBytes) {
        ByteBuffer bbf = BufferUtils.createByteBuffer(lengthInBytes);
        for (int i = 0; i < lengthInBytes; i++) {
            bbf.putInt(getHEAP32((int) triangleIndexBase + i));
        }
        return bbf;
    }

    public static int createBtBufferFromFloatArray(btDestructible p, float[] array) {
        int size = array.length * 4;
        int buffer = malloc(p,size);
        for (int i = 0; i < array.length; i++) {
            setHEAPF32(buffer + i, array[i]);
        }
        return buffer;
    }

    public static int allocate(btDestructible p,int size) {
        return malloc(p,size);
    }

    public static int setFloat(long pointer, int offset, float f) {
        setHEAPF32((int) pointer + offset, f);
        return (int) pointer;
    }

    public static int setInt(long pointer, int offset, int i) {
        setHEAP32((int) pointer + offset, i);
        return (int) pointer;
    }

    public static int getInt(long pointer, int offset) {
        return getHEAP32((int) pointer + offset);
    }

    public static float getFloat(long pointer, int offset) {
        return getHEAPF32((int) pointer + offset);
    }

    public static int allocFromByteArrayI32(btDestructible p,ByteBuffer triangleIndexBaseBuffer, int lengthInBytes) {
        int alloc = malloc(p,lengthInBytes);
        for (int i = 0; i < lengthInBytes; i++) {
            setHEAP32(alloc + i, triangleIndexBaseBuffer.getInt());
        }
        return alloc;
    }

    public static int allocFromByteArrayF32(btDestructible p,ByteBuffer vertexBaseBuffer, int lengthInBytes) {
        int alloc = malloc(p,lengthInBytes);
        for (int i = 0; i < lengthInBytes; i++) {
            setHEAPF32(alloc + i, vertexBaseBuffer.getFloat());
        }
        return alloc;
    }

    public static ByteBuffer allocToByteArrayF32(long vertexBase, int lengthInBytes) {
        ByteBuffer bbf = BufferUtils.createByteBuffer(lengthInBytes);
        for (int i = 0; i < lengthInBytes; i++) {
            bbf.putFloat(getHEAPF32((int) vertexBase + i));
        }

        return bbf;
    }

    // AmmoImpl.destroy(x)
    @JSBody(params = { "x" }, script = "AmmoImpl.destroy(x);")
    private static native void _destroy(JSObject x);

    // AmmoImpl.destroy(x)
    @JSBody(params = { "x" }, script = "AmmoImpl.destroy(x);")
    private static native void _destroyPointer(int x);

    public static void destroy(btDestructible p, Object x) {
        if (p != null) p.ungc(x);
        destroy(x);
    }

    private static void destroy(Object x) {
        if (x == null) {
            // System.err.println("Null object. Already destroyed?");
            return;
        }
        if (x instanceof Number) {
            int p = ((Number) x).intValue();
            // System.err.println("Destroy pointer " );
            _destroyPointer(p);
        } else if (x instanceof btDestructible) {
            btDestructible dx = (btDestructible) x;
            if (dx.isDestroyed()) return;
            // System.err.println("Destroy btDestructible " );
            dx.destroy();
            dx.markDestroyed();
        } else {
                        // System.err.println("Destroy JSOBJECT " );

            _destroy((JSObject) x);

        }

    }

    static Map<Integer, Object> userPointers = new HashMap<>();
    static AtomicInteger userPointerIndex = new AtomicInteger(0);

    public static int setUserPointer(Object obj) {
        while (true) {
            int index = userPointerIndex.incrementAndGet();
            if (userPointers.putIfAbsent(index, obj) == null) {
                return index;
            }
        }
    }

    public static Object getUserPointer(int index) {
        return userPointers.get(index);
    }

    public static void clearUserPointer(int index) {
        userPointers.remove(index);
    }

    
    public static btTriangleCallback createTriangleCallback(btDestructible p) {
        btTriangleCallback cb = _createTriangleCallback();
        if (p != null) p.gc(cb);
        return cb;
    }

    @JSBody(params = {}, script = "return new AmmoImpl.TriangleCallbackImpl();")
    public static native btTriangleCallback _createTriangleCallback();



    public static btShapeHull createShapeHull(btDestructible p,btConvexShape shape) {
        btShapeHull cb = _createShapeHull(shape);
        if (p != null) p.gc(cb);
        return cb;
    }

    @JSBody(params = {"shape"}, script = "return new AmmoImpl.btShapeHull(shape);")
    public static native btShapeHull _createShapeHull(btConvexShape shape);


    
}
