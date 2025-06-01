package org.ngengine.demo.son.controls;

import java.util.logging.Logger;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;


public class BoatControl extends RigidBodyControl implements ActionListener, PhysicsTickListener {
    private static final Logger logger = Logger.getLogger(BoatControl.class.getName());

    private volatile boolean forward = false;
    private volatile boolean backward = false;
    private volatile float steerLeft = 0;
    private volatile float steerRight = 0;
    private float physicsRudderRotation = 0;
    private float physicsSailLength = 0.5f;
    private float sailStrength = 1.0f;
    private boolean isRemote = false;
    private volatile float windFactor = 1f;
    private volatile float windStrength = 1f;
    private volatile float sailSensitivity = 0.5f;

    public BoatControl(boolean isRemote, float mass) {
        super(mass);
        this.isRemote = isRemote;
    }

    @Override
    public void setSpatial(com.jme3.scene.Spatial spatial) {
        setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(spatial));
        if (isRemote) setKinematic(true);
        super.setSpatial(spatial);

    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (isEnabled()) {
            if(forward){
                sailSensitivity += tpf * 0.4f;
                if (sailSensitivity > 1f) {
                    sailSensitivity = 1f;
                }
            } 
            if(backward){
                sailSensitivity -= tpf * 0.4f;
                if (sailSensitivity < 0.1f) {
                    sailSensitivity = 0.1f;
                }
            }

            // Check for wind updates
            float d = 0f;
            WindControl windControl = getSpatial().getControl(WindControl.class);
            if (windControl != null) {
                Vector3f wind = windControl.getWind();
    
                d = (wind.normalize().dot(getSpatial().getWorldRotation().mult(Vector3f.UNIT_Z)) + 1f) / 2f;
                if (windFactor < 0.6f) {
                    windFactor = 0.6f;
                } else{
                    windFactor = d;

                }
               
                windStrength = wind.length();
            }

            BoatAnimationControl animControl = getSpatial().getControl(BoatAnimationControl.class);
            if (animControl != null) {
                animControl.setSailAnim(1f-sailSensitivity, d);

                float flagRot = 1f-d;
                animControl.setFlagAnim(flagRot);
            }
        }

    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ("Forward".equals(name)) {
            forward = isPressed;
        } else if ("Backward".equals(name)) {
            backward = isPressed;
        } else if ("SteerLeft".equals(name)) {
            steerLeft = isPressed ? 1 : 0;
        } else if ("SteerRight".equals(name)) {
            steerRight = isPressed ? -1 : 0;
        }
    }

    @Override
    public void setPhysicsSpace(PhysicsSpace newSpace) {
        if (newSpace != null) {
            newSpace.addTickListener(this);
        } else if (getPhysicsSpace() != null) {
            getPhysicsSpace().removeTickListener(this);
        }
        super.setPhysicsSpace(newSpace);
    }

    
    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        

        float windContribution = windStrength*windFactor;

        float forwardSpeed =  windContribution*450f / 3.6f;
        if (forwardSpeed < 4024f) {
            forwardSpeed =4024f;
        }
        forwardSpeed *=sailStrength;
        forwardSpeed*=sailSensitivity;
        float rotationSpeed = forwardSpeed * 2f;
        if(rotationSpeed<60){
            rotationSpeed = 60;
        }
 


        if (steerLeft > 0) {
            applyTorque(new Vector3f(0, rotationSpeed, 0));          
        }
        if (steerRight < 0) {
            applyTorque(new Vector3f(0, -rotationSpeed, 0));
        }

        Vector3f dir = getPhysicsRotation().mult(Vector3f.UNIT_Z);
        dir.y = 0;
        dir.normalizeLocal();
        applyForce(dir.mult(forwardSpeed), Vector3f.ZERO);
    }

    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {
        

    }


    public void fireCannonBalls(){
        

    }

    public float getSailLength() {
        return physicsSailLength;
    }

    public float getRudderRotation() {
        return physicsRudderRotation;
    }

 

}