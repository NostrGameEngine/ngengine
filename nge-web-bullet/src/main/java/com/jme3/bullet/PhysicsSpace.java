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
package com.jme3.bullet;

import com.jme3.bullet.types.*;
import com.jme3.app.AppTask;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionEventFactory;
import com.jme3.bullet.collision.PhysicsCollisionGroupListener;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.PhysicsSweepTestResult;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.util.Converter;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>PhysicsSpace - The central jbullet-jme physics space</p>
 * @author normenhansen
 */
public class PhysicsSpace extends btDestructibleImpl{

    /**
     * message logger for this class
     */
    private static final Logger logger = Logger.getLogger(PhysicsSpace.class.getName());
    /**
     * index of the X axis
     */
    public static final int AXIS_X = 0;
    /**
     * index of the Y axis
     */
    public static final int AXIS_Y = 1;
    /**
     * index of the Z axis
     */
    public static final int AXIS_Z = 2;
    private static ThreadLocal<ConcurrentLinkedQueue<AppTask<?>>> pQueueTL =
            new ThreadLocal<ConcurrentLinkedQueue<AppTask<?>>>() {

                @Override
                protected ConcurrentLinkedQueue<AppTask<?>> initialValue() {
                    return new ConcurrentLinkedQueue<AppTask<?>>();
                }
            };
    private ConcurrentLinkedQueue<AppTask<?>> pQueue = new ConcurrentLinkedQueue<>();
    private static ThreadLocal<PhysicsSpace> physicsSpaceTL = new ThreadLocal<PhysicsSpace>();
    private btDiscreteDynamicsWorld dynamicsWorld = null;
    private btBroadphaseInterface broadphase;
    private BroadphaseType broadphaseType = BroadphaseType.DBVT;
    private btCollisionDispatcher dispatcher;
    private btConstraintSolver solver;
    private btDefaultCollisionConfiguration collisionConfiguration;
    private List<PhysicsGhostObject> physicsGhostObjects = new ArrayList<>();
    private List<PhysicsCharacter> physicsCharacters = new ArrayList<>();
    private List<PhysicsRigidBody> physicsBodies = new ArrayList<>();
    private List<PhysicsJoint> physicsJoints = new ArrayList<>();
    private List<PhysicsVehicle> physicsVehicles = new ArrayList<>();
    /**
     * map from collision groups to registered group listeners
     */
    // private Map<Integer, PhysicsCollisionGroupListener> collisionGroupListeners = new ConcurrentHashMap<>();
    /**
     * queue of registered tick listeners
     */
    private ConcurrentLinkedQueue<PhysicsTickListener> tickListeners = new ConcurrentLinkedQueue<>();
    /**
     * list of registered collision listeners
     */
    final private List<PhysicsCollisionListener> collisionListeners
            = new SafeArrayList<>(PhysicsCollisionListener.class);
    /**
     * queue of collision events not yet distributed to listeners
     */
    private ArrayDeque<PhysicsCollisionEvent> collisionEvents = new ArrayDeque<>();
    private PhysicsCollisionEventFactory eventFactory = new PhysicsCollisionEventFactory();
    /**
     * copy of minimum coordinate values when using AXIS_SWEEP broadphase
     * algorithms
     */
    private Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    /**
     * copy of maximum coordinate values when using AXIS_SWEEP broadphase
     * algorithms
     */
    private Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    /**
     * physics time step (in seconds, &gt;0)
     */
    private float accuracy = 1f / 60f;
    /**
     * maximum number of physics steps per frame (&ge;0, default=4)
     */
    private int maxSubSteps = 4;
 
  

    /**
     * Get the current PhysicsSpace <b>running on this thread</b><br>
     * For parallel physics, this can also be called from the OpenGL thread to receive the PhysicsSpace
     * @return the PhysicsSpace running on this thread
     */
    public static PhysicsSpace getPhysicsSpace() {
        return physicsSpaceTL.get();
    }

    /**
     * Used internally
     *
     * @param space which space to simulate on the current thread
     */
    public static void setLocalThreadPhysicsSpace(PhysicsSpace space) {
        physicsSpaceTL.set(space);
    }

    public PhysicsSpace() {
        this(new Vector3f(-10000f, -10000f, -10000f), new Vector3f(10000f, 10000f, 10000f), BroadphaseType.DBVT);
    }

    public PhysicsSpace(BroadphaseType broadphaseType) {
        this(new Vector3f(-10000f, -10000f, -10000f), new Vector3f(10000f, 10000f, 10000f), broadphaseType);
    }

    public PhysicsSpace(Vector3f worldMin, Vector3f worldMax) {
        this(worldMin, worldMax, BroadphaseType.AXIS_SWEEP_3);
    }

    public PhysicsSpace(Vector3f worldMin, Vector3f worldMax, BroadphaseType broadphaseType) {
        this.worldMin.set(worldMin);
        this.worldMax.set(worldMax);
        this.broadphaseType = broadphaseType;
        create();
    }

    private btGhostPairCallback ghostPairCallback;
    /**
     * Has to be called from the (designated) physics thread
     */
    public void create() {
        pQueueTL.set(pQueue);

        logger.log(Level.FINE, "Initializing Physics Engine");
        btUtils.init(true);
        logger.log(Level.FINE, "Physics Engine initialized");

        logger.log(Level.FINE, "Initializing collision configuration");

        collisionConfiguration = btUtils.createDefaultCollisionConfiguration(this);

        logger.log(Level.FINE, "Initializing collision dispatcher");
        dispatcher = btUtils.createCollisionDispatcher(this,collisionConfiguration);

        logger.log(Level.FINE, "Initializing broadphase");
        switch (broadphaseType) {
            case SIMPLE:
            case AXIS_SWEEP_3_32:
                logger.warning("Broadphase type SIMPLE  or AXIS_SWEEP_3_32 not supported, using AXIS_SWEEP_3 instead");
            case AXIS_SWEEP_3:
                btVector3 v1=Converter.convert(worldMin,btUtils.newVector3(this,0,0,0));
                btVector3 v2=Converter.convert(worldMax,btUtils.newVector3(this,0,0,0));
                broadphase = btUtils.createAxisSweep3(this,v1, v2);
                btUtils.destroy(this,v1);
                btUtils.destroy(this,v2); 
                break;
            default:
            case DBVT:
                broadphase = btUtils.createDbvtBroadphase(this);
                break;
        }

        logger.log(Level.FINE, "Initializing solver");
        solver = btUtils.createSequentialImpulseConstraintSolver(this);

        logger.log(Level.FINE, "Initializing dynamic world");

        dynamicsWorld = btUtils.createDiscreteDynamicsWorld(this,dispatcher, broadphase, solver, collisionConfiguration);

        logger.log(Level.FINE, "Initializing gravity");
        btVector3 gravityV = btUtils.newVector3(this,0, -9.81f, 0);
        dynamicsWorld.setGravity(gravityV);
        btUtils.destroy(this,gravityV);

        logger.log(Level.FINE, "Initializing overlapping pair cache");
        btOverlappingPairCache overlappingPairCache = broadphase.getOverlappingPairCache();
        
        logger.log(Level.FINE, "Initializing ghost pair callback");
        overlappingPairCache.setInternalGhostPairCallback(ghostPairCallback=btUtils.createGhostPairCallback(this));
        btUtils.registerGImpactCollisionAlgorithm(dispatcher);
        
        logger.log(Level.FINE, "Assigning PhysicsSpace to thread");
        physicsSpaceTL.set(this);
        //register filter callback for tick / collision
        // setTickCallback();
        logger.log(Level.FINE, "Initializing contact callback");
        setContactCallbacks();
        //register filter callback for collision groups
        // setOverlapFilterCallback();
    }

    private void setOverlapFilterCallback() {
        // OverlapFilterCallback callback = new OverlapFilterCallback() {

        //     @Override
        //     public boolean needBroadphaseCollision(BroadphaseProxy bp, BroadphaseProxy bp1) {
        //         boolean collides = (bp.collisionFilterGroup & bp1.collisionFilterMask) != 0;
        //         if (collides) {
        //             collides = (bp1.collisionFilterGroup & bp.collisionFilterMask) != 0;
        //         }
        //         if (collides) {
        //             assert (bp.clientObject instanceof com.bulletphysics.collision.dispatch.CollisionObject && bp1.clientObject instanceof com.bulletphysics.collision.dispatch.CollisionObject);
        //             com.bulletphysics.collision.dispatch.CollisionObject colOb = (com.bulletphysics.collision.dispatch.CollisionObject) bp.clientObject;
        //             com.bulletphysics.collision.dispatch.CollisionObject colOb1 = (com.bulletphysics.collision.dispatch.CollisionObject) bp1.clientObject;
        //             assert (colOb.getUserPointer() != null && colOb1.getUserPointer() != null);
        //             PhysicsCollisionObject collisionObject = (PhysicsCollisionObject) colOb.getUserPointer();
        //             PhysicsCollisionObject collisionObject1 = (PhysicsCollisionObject) colOb1.getUserPointer();
        //             if ((collisionObject.getCollideWithGroups() & collisionObject1.getCollisionGroup()) > 0
        //                     || (collisionObject1.getCollideWithGroups() & collisionObject.getCollisionGroup()) > 0) {
        //                 PhysicsCollisionGroupListener listener = collisionGroupListeners.get(collisionObject.getCollisionGroup());
        //                 PhysicsCollisionGroupListener listener1 = collisionGroupListeners.get(collisionObject1.getCollisionGroup());
        //                 if(listener != null){
        //                     collides = listener.collide(collisionObject, collisionObject1);
        //                 }
        //                 if(listener1 != null && collisionObject.getCollisionGroup() != collisionObject1.getCollisionGroup()){
        //                     collides = listener1.collide(collisionObject, collisionObject1) && collides;
        //                 }
        //             } else {
        //                 return false;
        //             }
        //         }
        //         return collides;
        //     }
        // };
        // btOverlappingPairCache pcache=  dynamicsWorld.getPairCache();
        // pcache.setOverlapFilterCallback(callback);
        
    }

 

    private void setContactCallbacks() {
        // BulletGlobals.setContactAddedCallback(new ContactAddedCallback() {

        //     @Override
        //     public boolean contactAdded(ManifoldPoint cp, com.bulletphysics.collision.dispatch.CollisionObject colObj0,
        //             int partId0, int index0, com.bulletphysics.collision.dispatch.CollisionObject colObj1, int partId1,
        //             int index1) {
        //         System.out.println("contact added");
        //         return true;
        //     }
        // });

        dynamicsWorld.setContactProcessedCallback(new btContactProcessedCallback() {

            @Override
            public void callback(btManifoldPoint cp, btCollisionObject rBody0, btCollisionObject rBody1) {
                PhysicsCollisionObject node = null, node1 = null;

                node = (PhysicsCollisionObject) rBody0.getUserPointer();
                node1 = (PhysicsCollisionObject) rBody1.getUserPointer();
                collisionEvents.add(eventFactory.getEvent(PhysicsCollisionEvent.TYPE_PROCESSED, node, node1, cp));

            }
        });

        
    }

    /**
     * updates the physics space
     * @param time the current time value
     */
    public void update(float time) {
        update(time, maxSubSteps);
    }

    /**
     * updates the physics space, uses maxSteps<br>
     * @param time the current time value
     * @param maxSteps the maximum number of steps of size accuracy (&ge;1) or 0
     * for a single step of size timeInterval
     */
    public void update(float time, int maxSteps) {
        _phyiscsTick(time, maxSteps);

    }
    
    private void _phyiscsTick(float time, int maxSteps) {
          if (getDynamicsWorld() == null) {
            return;
        }

        AppTask task = pQueue.poll();
        task = pQueue.poll();
        while (task != null) {
            while (task.isCancelled()) {
                task = pQueue.poll();
            }
            try {
                task.invoke();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            task = pQueue.poll();
        }
        for (Iterator<PhysicsTickListener> it = tickListeners.iterator(); it.hasNext();) {
            PhysicsTickListener physicsTickCallback = it.next();
            physicsTickCallback.prePhysicsTick(this, time);
        }

        // step simulation
        dynamicsWorld.stepSimulation(time, maxSteps, accuracy);

        for (Iterator<PhysicsTickListener> it = tickListeners.iterator(); it.hasNext();) {
            PhysicsTickListener physicsTickCallback = it.next();
            physicsTickCallback.physicsTick(this, time);
        }
    }

    public void distributeEvents() {
        //add collision callbacks
        int cListSize = collisionListeners.size();
        while( collisionEvents.isEmpty() == false ) {
            PhysicsCollisionEvent physicsCollisionEvent = collisionEvents.pop();
            for(int i=0;i<cListSize;i++) {
                collisionListeners.get(i).collision(physicsCollisionEvent);
            }
            //recycle events
            eventFactory.recycle(physicsCollisionEvent);
        }
    }

    public static <V> Future<V> enqueueOnThisThread(Callable<V> callable) {
        AppTask<V> task = new AppTask<>(callable);
        pQueueTL.get().add(task);
        return task;
    }

    /**
     * calls the callable on the next physics tick (ensuring e.g. force applying)
     *
     * @param <V> the return type of the Callable
     * @param callable the Callable to invoke
     * @return a new AppTask
     */
    public <V> Future<V> enqueue(Callable<V> callable) {
        AppTask<V> task = new AppTask<>(callable);
        pQueue.add(task);
        return task;
    }

    /**
     * adds an object to the physics space
     * @param obj the PhysicsControl or Spatial with PhysicsControl to add
     */
    public void add(Object obj) {
        if (obj == null) return;
        if (obj instanceof PhysicsControl) {
            ((PhysicsControl) obj).setPhysicsSpace(this);
        } else if (obj instanceof Spatial) {
            Spatial node = (Spatial) obj;
            for (int i = 0; i < node.getNumControls(); i++) {
                if (node.getControl(i) instanceof PhysicsControl) {
                    add(node.getControl(i));
                }
            }
        } else if (obj instanceof PhysicsCollisionObject) {
            addCollisionObject((PhysicsCollisionObject) obj);
        } else if (obj instanceof PhysicsJoint) {
            addJoint((PhysicsJoint) obj);
        } else {
            throw (new UnsupportedOperationException("Cannot add this kind of object to the physics space."));
        }
    }

    public void addCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsGhostObject) {
            addGhostObject((PhysicsGhostObject) obj);
        } else if (obj instanceof PhysicsRigidBody) {
            addRigidBody((PhysicsRigidBody) obj);
        } else if (obj instanceof PhysicsVehicle) {
            addRigidBody((PhysicsVehicle) obj);
        } else if (obj instanceof PhysicsCharacter) {
            addCharacter((PhysicsCharacter) obj);
        }
    }

    /**
     * removes an object from the physics space
     *
     * @param obj the PhysicsControl or Spatial with PhysicsControl to remove
     */
    public void remove(Object obj) {
        if (obj == null) return;
        if (obj instanceof PhysicsControl) {
            ((PhysicsControl) obj).setPhysicsSpace(null);
        } else if (obj instanceof Spatial) {
            Spatial node = (Spatial) obj;
            for (int i = 0; i < node.getNumControls(); i++) {
                if (node.getControl(i) instanceof PhysicsControl) {
                    remove(node.getControl(i));
                }
            }
        } else if (obj instanceof PhysicsCollisionObject) {
            removeCollisionObject((PhysicsCollisionObject) obj);
        } else if (obj instanceof PhysicsJoint) {
            removeJoint((PhysicsJoint) obj);
        } else {
            throw (new UnsupportedOperationException("Cannot remove this kind of object from the physics space."));
        }
    }

    public void removeCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsGhostObject) {
            removeGhostObject((PhysicsGhostObject) obj);
        } else if (obj instanceof PhysicsRigidBody) {
            removeRigidBody((PhysicsRigidBody) obj);
        } else if (obj instanceof PhysicsCharacter) {
            removeCharacter((PhysicsCharacter) obj);
        }
    }

    /**
     * adds all physics controls and joints in the given spatial node to the physics space
     * (e.g. after loading from disk) - recursive if node
     * @param spatial the rootnode containing the physics objects
     */
    public void addAll(Spatial spatial) {
        add(spatial);

        if (spatial.getControl(RigidBodyControl.class) != null) {
            RigidBodyControl physicsNode = spatial.getControl(RigidBodyControl.class);
            //add joints with physicsNode as BodyA
            List<PhysicsJoint> joints = physicsNode.getJoints();
            for (Iterator<PhysicsJoint> it1 = joints.iterator(); it1.hasNext();) {
                PhysicsJoint physicsJoint = it1.next();
                if (physicsNode.equals(physicsJoint.getBodyA())) {
                    //add(physicsJoint.getBodyB());
                    add(physicsJoint);
                }
            }
        }
        //recursion
        if (spatial instanceof Node) {
            List<Spatial> children = ((Node) spatial).getChildren();
            for (Iterator<Spatial> it = children.iterator(); it.hasNext();) {
                Spatial spat = it.next();
                addAll(spat);
            }
        }
    }

    /**
     * Removes all physics controls and joints in the given spatial from the physics space
     * (e.g. before saving to disk) - recursive if node
     * @param spatial the rootnode containing the physics objects
     */
    public void removeAll(Spatial spatial) {
        if (spatial.getControl(RigidBodyControl.class) != null) {
            RigidBodyControl physicsNode = spatial.getControl(RigidBodyControl.class);
            //remove joints with physicsNode as BodyA
            List<PhysicsJoint> joints = physicsNode.getJoints();
            for (Iterator<PhysicsJoint> it1 = joints.iterator(); it1.hasNext();) {
                PhysicsJoint physicsJoint = it1.next();
                if (physicsNode.equals(physicsJoint.getBodyA())) {
                    removeJoint(physicsJoint);
                    //remove(physicsJoint.getBodyB());
                }
            }
        }
        
        remove(spatial);
        //recursion
        if (spatial instanceof Node) {
            List<Spatial> children = ((Node) spatial).getChildren();
            for (Iterator<Spatial> it = children.iterator(); it.hasNext();) {
                Spatial spat = it.next();
                removeAll(spat);
            }
        }
    }

    private void addGhostObject(PhysicsGhostObject node) {
        if(physicsGhostObjects.contains(node)){
            logger.log(Level.WARNING, "GhostObject {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsGhostObjects.add(node);
        logger.log(Level.FINE, "Adding ghost object {0} to physics space.", node);
        dynamicsWorld.addCollisionObject((btPairCachingGhostObject)node.getBtObject());
    }

    private void removeGhostObject(PhysicsGhostObject node) {
        if(!physicsGhostObjects.contains(node)){
            logger.log(Level.WARNING, "GhostObject {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        physicsGhostObjects.remove(node);
        logger.log(Level.FINE, "Removing ghost object {0} from physics space.", node);
        dynamicsWorld.removeCollisionObject((btPairCachingGhostObject)node.getBtObject());
    }

    private void addCharacter(PhysicsCharacter node) {
        if(physicsCharacters.contains(node)){
            logger.log(Level.WARNING, "Character {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsCharacters.add(node);
        logger.log(Level.FINE, "Adding character {0} to physics space.", node);
        dynamicsWorld.addCollisionObject((btPairCachingGhostObject)node.getBtObject(), btCollisionFilterGroups.CHARACTER_FILTER, (short) (btCollisionFilterGroups.STATIC_FILTER | btCollisionFilterGroups.DEFAULT_FILTER));
        dynamicsWorld.addAction(node.getBtController());
    }

    private void removeCharacter(PhysicsCharacter node) {
        if(!physicsCharacters.contains(node)){
            logger.log(Level.WARNING, "Character {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        physicsCharacters.remove(node);
        logger.log(Level.FINE, "Removing character {0} from physics space.", node);
        dynamicsWorld.removeAction(node.getBtController());
        dynamicsWorld.removeCollisionObject((btPairCachingGhostObject)node.getBtObject());
    }

    private void addRigidBody(PhysicsRigidBody node) {
        if(physicsBodies.contains(node)){
            logger.log(Level.WARNING, "RigidBody {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsBodies.add(node);

        //Workaround
        //It seems that adding a Kinematic RigidBody to the dynamicWorld prevent it from being non-kinematic again afterward.
        //so we add it non-kinematic, then set it kinematic again.
        boolean kinematic = false;
        if (node.isKinematic()) {
            kinematic = true;
            node.setKinematic(false);
        }
        dynamicsWorld.addRigidBody((btRigidBody)node.getBtObject());
        if (kinematic) {
            node.setKinematic(true);
        }

        logger.log(Level.FINE, "Adding RigidBody {0} to physics space.", node);
        if (node instanceof PhysicsVehicle) {
            logger.log(Level.FINE, "Adding vehicle constraint {0} to physics space.", ((PhysicsVehicle) node));
            ((PhysicsVehicle) node).createVehicle(this);
            physicsVehicles.add( (PhysicsVehicle)node);
            dynamicsWorld.addVehicle((btRaycastVehicle)((PhysicsVehicle) node).getBtObject());
        }
    }

    private void removeRigidBody(PhysicsRigidBody node) {
        if(!physicsBodies.contains(node)){
            logger.log(Level.WARNING, "RigidBody {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        if (node instanceof PhysicsVehicle) {
            logger.log(Level.FINE, "Removing vehicle constraint {0} from physics space.", ((PhysicsVehicle) node));
            physicsVehicles.remove(((PhysicsVehicle) node));
            dynamicsWorld.removeVehicle((btRaycastVehicle)((PhysicsVehicle) node).getBtObject());
        }
        logger.log(Level.FINE, "Removing RigidBody {0} from physics space.", node);
        physicsBodies.remove(node);
        dynamicsWorld.removeRigidBody((btRigidBody)node.getBtObject());
    }

    private void addJoint(PhysicsJoint joint) {
        if(physicsJoints.contains(joint)){
            logger.log(Level.WARNING, "Joint {0} already exists in PhysicsSpace, cannot add.", joint);
            return;
        }
        logger.log(Level.FINE, "Adding Joint {0} to physics space.", joint);
        physicsJoints.add( joint);
        dynamicsWorld.addConstraint((btTypedConstraint)joint.getBtObject(), !joint.isCollisionBetweenLinkedBodys());
    }

    private void removeJoint(PhysicsJoint joint) {
        if(!physicsJoints.contains(joint)){
            logger.log(Level.WARNING, "Joint {0} does not exist in PhysicsSpace, cannot remove.", joint);
            return;
        }
        logger.log(Level.FINE, "Removing Joint {0} from physics space.", joint);
        physicsJoints.remove(joint);
        dynamicsWorld.removeConstraint((btTypedConstraint)joint.getBtObject());
    }

    public Collection<PhysicsRigidBody> getRigidBodyList(){
        return physicsBodies;
    }

    public Collection<PhysicsGhostObject> getGhostObjectList(){
        return physicsGhostObjects;
    }

    public Collection<PhysicsCharacter> getCharacterList(){
        return physicsCharacters;
    }

    public Collection<PhysicsJoint> getJointList(){
        return physicsJoints;
    }

    public Collection<PhysicsVehicle> getVehicleList(){
        return physicsVehicles;
    }

    /**
     * Sets the gravity of the PhysicsSpace, set before adding physics objects!
     *
     * @param gravity the desired acceleration vector
     * (in physics-space coordinates, not null, unaffected, default=0,-10,0)
     */
    public void setGravity(Vector3f gravity) {
        btVector3 tempVec = btUtils.newVector3(this, 0, 0, 0);
        dynamicsWorld.setGravity(Converter.convert(gravity, tempVec));
        btUtils.destroy(this, tempVec);

    }

    /**
     * Gets the gravity of the PhysicsSpace
     *
     * @param gravity storage for the result (modified if not null)
     * @return the acceleration vector (in physics-space coordinates, either
     * gravity or a new instance)
     */
    public Vector3f getGravity(Vector3f gravity) {
        btVector3 tempVec=dynamicsWorld.getGravity();
        return Converter.convert(tempVec, gravity);
    }

    /**
     * applies gravity value to all objects
     */
    public void applyGravity() {
        dynamicsWorld.applyGravity();
    }

    /**
     * clears forces of all objects
     */
    public void clearForces() {
        dynamicsWorld.clearForces();
    }

    /**
     * Adds the specified listener to the physics tick listeners.
     * The listeners are called on each physics step, which is not necessarily
     * each frame but is determined by the accuracy of the physics space.
     *
     * @param listener the listener to register (not null, alias created)
     */
    public void addTickListener(PhysicsTickListener listener) {
        tickListeners.add(listener);
    }

    public void removeTickListener(PhysicsTickListener listener) {
        tickListeners.remove(listener);
    }

    /**
     * Adds a CollisionListener that will be informed about collision events
     * @param listener the CollisionListener to add
     */
    public void addCollisionListener(PhysicsCollisionListener listener) {
        collisionListeners.add(listener);
    }

    /**
     * Removes a CollisionListener from the list
     * @param listener the CollisionListener to remove
     */
    public void removeCollisionListener(PhysicsCollisionListener listener) {
        collisionListeners.remove(listener);
    }

    /**
     * Adds a listener for a specific collision group, such a listener can disable collisions when they happen.<br>
     * There can be only one listener per collision group.
     *
     * @param listener the listener to register (not null, alias created)
     * @param collisionGroup which group it should listen for (bitmask with
     * exactly one bit set)
     */
    // public void addCollisionGroupListener(PhysicsCollisionGroupListener listener, int collisionGroup) {
    //     collisionGroupListeners.put(collisionGroup, listener);
    // }

    // public void removeCollisionGroupListener(int collisionGroup) {
    //     collisionGroupListeners.remove(collisionGroup);
    // }

    /**
     * Performs a ray collision test and returns the results as a list of PhysicsRayTestResults
     *
     * @param from the starting location (physics-space coordinates, not null,
     * unaffected)
     * @param to the ending location (in physics-space coordinates, not null,
     * unaffected)
     * @return a new list of results (not null)
     */
    public List<PhysicsRayTestResult> rayTest(Vector3f from, Vector3f to,List<PhysicsRayTestResult> results) {
        btVector3 rayVec1 = btUtils.newVector3(this,0, 0, 0);
        btVector3 rayVec2 = btUtils.newVector3(this,0, 0, 0);

        btAllHitsRayResultCallback callback = btUtils.createAllHitsRayResultCallback(this,rayVec1, rayVec2);
        dynamicsWorld.rayTest(Converter.convert(from, rayVec1), Converter.convert(to, rayVec2), callback);
        
        btScalarArray hitFractions = callback.getHitFractions();
        btVector3Array hitNormalsWorld = callback.getHitNormalWorld();
        // btVector3Array hitPointsWorld = callback.getHitPointWorld();
        btConstCollisionObjectArray collisionObjects = callback.getCollisionObjects();

        for (int i = 0; i < hitFractions.size(); i++) {
            btCollisionObject obj = collisionObjects.at(i);
            // btVector3 hitPointWorld = hitPointsWorld.at(i);
            btVector3 hitNormalWorld = hitNormalsWorld.at(i);
            boolean normalInWorldSpace = true;

            PhysicsCollisionObject phyObj = (PhysicsCollisionObject) obj.getUserPointer();
            results.add(new PhysicsRayTestResult(
                phyObj,
                Converter.convert(hitNormalWorld, new Vector3f()),
                hitFractions.at(i),
                normalInWorldSpace
            ));
        }
        

        btUtils.destroy(this,rayVec2);
        btUtils.destroy(this,rayVec1);
        return results;
    }

    /**
     * Performs a ray collision test and returns the results as a list of PhysicsRayTestResults
     *
     * @param from the starting location (in physics-space coordinates, not
     * null, unaffected)
     * @param to the ending location (in physics-space coordinates, not null,
     * unaffected)
     * @param results the list to hold results (not null, modified)
     * @return results
     */
    public List<PhysicsRayTestResult> rayTest(Vector3f from, Vector3f to) {
        List<PhysicsRayTestResult> results = new LinkedList<>();
        return rayTest(from, to, results);

    }
 

    /**
     * Performs a sweep collision test and returns the results as a list of PhysicsSweepTestResults<br>
     * You have to use different Transforms for start and end (at least distance greater than 0.4f).
     * SweepTest will not see a collision if it starts INSIDE an object and is moving AWAY from its center.
     *
     * @param shape the shape to sweep (not null, convex, unaffected)
     * @param start the starting physics-space transform (not null, unaffected)
     * @param end the ending physics-space transform (not null, unaffected)
     * @return a new list of results
     */
    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape, Transform start, Transform end,List<PhysicsSweepTestResult> results) {
        btTransform sweepTrans1 = btUtils.newTransform(this);
        btTransform sweepTrans2 = btUtils.newTransform(this);
        btVector3 from = Converter.convert(start.getTranslation(),btUtils.newVector3(this,0,0,0));
        btVector3 to = Converter.convert(end.getTranslation(),btUtils.newVector3(this,0,0,0));

        btClosestConvexResultCallback callback = btUtils.createClosestConvexResultCallback(this,from, to);

        dynamicsWorld.convexSweepTest(shape.getCShape(), Converter.convert(start, sweepTrans1), Converter.convert(end, sweepTrans2), callback, 0f);
        
        btCollisionObject obj = callback.getHitCollisionObject();
        btVector3 hitNormalWorld = callback.getHitNormalWorld();
        btVector3 hitPointWorld = callback.getHitPointWorld();

        PhysicsCollisionObject phyObj=(PhysicsCollisionObject) obj.getUserPointer();
        Vector3f hitPos=Converter.convert(hitPointWorld, new Vector3f());
        Vector3f hitNormal=Converter.convert(hitNormalWorld, new Vector3f());

        float hitFraction=start.getTranslation().distance(hitPos)/start.getTranslation().distance(end.getTranslation());

        PhysicsSweepTestResult res=new PhysicsSweepTestResult(phyObj, hitNormal, hitFraction, true);
        results.add(res);

        btUtils.destroy(this,sweepTrans2);
        btUtils.destroy(this,sweepTrans1);
        btUtils.destroy(this,from);
        btUtils.destroy(this,to);
        btUtils.destroy(this,callback);

        return results;

    }

    /**
     * Performs a sweep collision test and returns the results as a list of PhysicsSweepTestResults<br>
     * You have to use different Transforms for start and end (at least distance greater than 0.4f).
     * SweepTest will not see a collision if it starts INSIDE an object and is moving AWAY from its center.
     * 
     * @param shape the shape to sweep (not null, convex, unaffected)
     * @param start the starting physics-space transform (not null, unaffected)
     * @param end the ending physics-space transform (not null, unaffected)
     * @param results the list to hold results (not null, modified)
     * @return results
     */
    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape, Transform start, Transform end) {
                List<PhysicsSweepTestResult> results = new LinkedList<>();

        return sweepTest(shape, start, end, results);
    }

 
    /**
     * destroys the current PhysicsSpace so that a new one can be created
     */
    public void destroy() {
        for (PhysicsRigidBody rb : physicsBodies) {
            rb.destroy();
        }
        physicsBodies.clear();

        for(PhysicsJoint joint : physicsJoints) {
            joint.destroy();
        }
        physicsJoints.clear();

        btUtils.destroy(this,dynamicsWorld);
        dynamicsWorld = null;

        btUtils.destroy(this,ghostPairCallback);
        super.destroy();
    }

    /**
     * used internally
     * @return the dynamicsWorld
     */
    public btDynamicsWorld getDynamicsWorld() {
        return dynamicsWorld;
    }

    public BroadphaseType getBroadphaseType() {
        return broadphaseType;
    }

    public void setBroadphaseType(BroadphaseType broadphaseType) {
        this.broadphaseType = broadphaseType;
    }

    /**
     * Sets the maximum amount of extra steps that will be used to step the physics
     * when the fps is below the physics fps. Doing this maintains determinism in physics.
     * For example a maximum number of 2 can compensate for frame rates as low as 30fps
     * when the physics has the default accuracy of 60 fps. Note that setting this
     * value too high can make the physics drive down its own fps in case it's overloaded.
     * @param steps The maximum number of extra steps, default is 4.
     */
    public void setMaxSubSteps(int steps) {
        maxSubSteps = steps;
    }

    /**
     * get the current accuracy of the physics computation
     * @return the current accuracy
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * sets the accuracy of the physics computation, default=1/60s<br>
     *
     * @param accuracy the desired time step (in seconds, default=1/60)
     */
    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public Vector3f getWorldMin() {
        return worldMin;
    }

    /**
     * only applies for AXIS_SWEEP broadphase
     *
     * @param worldMin the desired minimum coordinates values (not null,
     * unaffected, default=-10k,-10k,-10k)
     */
    public void setWorldMin(Vector3f worldMin) {
        this.worldMin.set(worldMin);
    }

    public Vector3f getWorldMax() {
        return worldMax;
    }

    /**
     * only applies for AXIS_SWEEP broadphase
     *
     * @param worldMax the desired maximum coordinates values (not null,
     * unaffected, default=10k,10k,10k)
     */
    public void setWorldMax(Vector3f worldMax) {
        this.worldMax.set(worldMax);
    }
    
    /**
     * Set the number of iterations used by the contact solver.
     * 
     * The default is 10. Use 4 for low quality, 20 for high quality.
     * 
     * @param numIterations The number of iterations used by the contact and constraint solver.
     */
    public void setSolverNumIterations(int numIterations) {
        btContactSolverInfo info = dynamicsWorld.getSolverInfo();
        info.setNumIterations( numIterations);
    }

    public int getSolverNumIterations() {
        btContactSolverInfo info = dynamicsWorld.getSolverInfo();
        return info.getNumIterations();
    }
    
    /**
     * interface with Broadphase types
     */
    public enum BroadphaseType {

        /**
         * basic Broadphase
         */
        SIMPLE,
        /**
         * better Broadphase, needs worldBounds , max Object number = 16384
         */
        AXIS_SWEEP_3,
        /**
         * better Broadphase, needs worldBounds , max Object number = 65536
         */
        AXIS_SWEEP_3_32,
        /**
         * Broadphase allowing quicker adding/removing of physics objects
         */
        DBVT;
    }
}
