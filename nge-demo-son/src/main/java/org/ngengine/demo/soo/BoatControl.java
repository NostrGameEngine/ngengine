package org.ngengine.demo.soo;

import java.util.logging.Logger;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class BoatControl extends RigidBodyControl implements ActionListener, PhysicsTickListener {
    private static final Logger logger = Logger.getLogger(BoatControl.class.getName());

    // Control inputs
    private volatile boolean forward = false;
    private volatile boolean backward = false;
    private volatile float steerLeft = 0;
    private volatile float steerRight = 0;

    // Wind properties
    private volatile float windX = 10.0f;
    private volatile float windY = 0.0f;
    private volatile float windZ = 0.0f;
    private final Vector3f physicsWind = new Vector3f(10.0f, 0, 0);

    // Boat physics state
    private float physicsRudderRotation = 0;
    private float physicsSailLength = 0.5f;
    private float currentRoll = 0;
    private float targetRoll = 0;

    // Configuration parameters
    private float rudderSensitivity = 0.5f;
    private float sailSensitivity = 0.5f;
    private float rudderStrength = 150f;
    private float sailStrength = 30f;
    private float rollStrength = 2.0f;
    private float stabilityStrength = 10.0f;

    // Network replication flag
    private boolean isRemote = false;

    // Water interaction - from BuoyancyControl
    private BuoyancyControl buoyancyControl;
    private float submergedRatio = 0.0f;
    private float waterHeight = 0.0f;

    public BoatControl(boolean isRemote, float mass) {
        super(mass);
        this.isRemote = isRemote;
    }

    @Override
    public void setSpatial(com.jme3.scene.Spatial spatial) {
        setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(spatial));
        if (isRemote) setKinematic(true);
        super.setSpatial(spatial);

        // Check for existing BuoyancyControl
        buoyancyControl = spatial.getControl(BuoyancyControl.class);
        if (buoyancyControl == null) {
            // Add buoyancy control if not present
            buoyancyControl = new BuoyancyControl();
            spatial.addControl(buoyancyControl);
            logger.info("Added BuoyancyControl to boat");
        }
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (isEnabled()) {
            // Get buoyancy info
            if (buoyancyControl != null) {
                submergedRatio = buoyancyControl.getSubmergedRatio();
                waterHeight = buoyancyControl.getWaterHeight();
            }

            // Check for wind updates
            WindControl windControl = getSpatial().getControl(WindControl.class);
            if (windControl != null) {
                float newX = windControl.getWind().x;
                float newY = windControl.getWind().y;
                float newZ = windControl.getWind().z;
                if (newX != windX || newY != windY || newZ != windZ) {
                    windX = newX;
                    windY = newY;
                    windZ = newZ;
                }
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
        if (!isRemote) {
            // Process input for controls
            physicsWind.set(windX, windY, windZ);

            // Update sail length based on input
            if (forward) {
                physicsSailLength += tpf * sailSensitivity;
                logger.info("Set sail length " + physicsSailLength);
            } else if (backward) {
                physicsSailLength -= tpf * sailSensitivity;
                logger.info("Set sail length " + physicsSailLength);
            }
            physicsSailLength = FastMath.clamp(physicsSailLength, 0.0f, 1.0f);

            // Update rudder based on input
            if (steerLeft > 0) {
                physicsRudderRotation += tpf * rudderSensitivity;
                logger.info("Set rudder rotation " + physicsRudderRotation);
            } else if (steerRight < 0) {
                physicsRudderRotation -= tpf * rudderSensitivity;
                logger.info("Set rudder rotation " + physicsRudderRotation);

            }
            physicsRudderRotation = FastMath.clamp(physicsRudderRotation, -0.785f, 0.785f);

            // Natural rudder centering
            if (steerLeft == 0 && steerRight == 0) {
                physicsRudderRotation *= 0.95f;
            }
        }
    }

    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {
        if (!isRemote) {
            // Skip physics if we're not submerged at all
            if (submergedRatio <= 0) {
                logger.info("Skip");
                return;
            }

            // Get orientation vectors
            Quaternion physicsRotation = getPhysicsRotation();
            Vector3f forwardDir = physicsRotation.mult(Vector3f.UNIT_Z);
            Vector3f rightDir = physicsRotation.mult(Vector3f.UNIT_X);
            Vector3f upDir = physicsRotation.mult(Vector3f.UNIT_Y);

            // Current velocity
            Vector3f boatVelocity = getLinearVelocity();
            float speed = boatVelocity.length();

            // NOTE: Buoyancy and water drag are now handled by BuoyancyControl

            // 1. SAILING PHYSICS
            // Calculate apparent wind
            Vector3f apparentWind = physicsWind.subtract(boatVelocity);
            float windStrength = apparentWind.length();

            if (windStrength > 0.001f) {
                // Calculate wind angle
                float cosAngle = forwardDir.dot(apparentWind) / (forwardDir.length() * windStrength);
                float windAngle = FastMath.acos(FastMath.clamp(cosAngle, -1.0f, 1.0f));
                float windSide = Math.signum(rightDir.dot(apparentWind));

                // Calculate sailing efficiency
                float sailEfficiency = 0;

                // No sailing directly into the wind (within ~45 degrees)
                if (windAngle > 0.8f) {
                    if (windAngle < 1.6f) { // Close hauled
                        sailEfficiency = 0.5f;
                    } else if (windAngle < 2.0f) { // Close reach
                        sailEfficiency = 0.8f;
                    } else if (windAngle < 2.4f) { // Beam reach
                        sailEfficiency = 1.0f;
                    } else if (windAngle < 2.8f) { // Broad reach
                        sailEfficiency = 0.7f;
                    } else { // Running
                        sailEfficiency = 0.5f;
                    }
                }

                // Apply sail force
                float sailPower = windStrength * sailEfficiency * physicsSailLength * sailStrength;
                applyCentralForce(forwardDir.mult(sailPower));

                // 2. RUDDER PHYSICS
                // Apply turning torque based on rudder and speed
                if (Math.abs(physicsRudderRotation) > 0.01f && speed > 0.5f) {
                    float rudderTorque = physicsRudderRotation * Math.min(speed * 0.2f, 1.0f)
                            * rudderStrength;
                    applyTorque(new Vector3f(0, rudderTorque, 0));
                }

                // 3. ROLL PHYSICS
                // Calculate roll based on wind and sail
                if (physicsSailLength > 0.1f && windStrength > 1.0f) {
                    // Calculate target roll
                    targetRoll = windSide * physicsSailLength * windStrength * 0.01f * rollStrength;
                    targetRoll = FastMath.clamp(targetRoll, -0.4f, 0.4f);

                    // Gradually approach target roll
                    currentRoll = FastMath.interpolateLinear(tpf * 2.0f, currentRoll, targetRoll);

                    // Extract current roll angle
                    float[] angles = new float[3];
                    physicsRotation.toAngles(angles);
                    float currentPhysicsRoll = angles[2];

                    // Apply restoring torque for stability (prevents flipping)
                    float stabilityTorque = -currentPhysicsRoll * stabilityStrength;
                    applyTorque(new Vector3f(0, 0, stabilityTorque));
                }
            }
        }
    }

    public float getSailLength() {
        return physicsSailLength;
    }

    public float getRudderRotation() {
        return physicsRudderRotation;
    }

    public Vector3f getWindDirection() {
        return physicsWind.normalize();
    }

    public float getWindStrength() {
        return physicsWind.length();
    }
}