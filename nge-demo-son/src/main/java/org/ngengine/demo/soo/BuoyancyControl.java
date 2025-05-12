package org.ngengine.demo.soo;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class BuoyancyControl extends AbstractControl implements PhysicsTickListener {
    private transient OceanAppState appState;
    private Vector3f oldGravity = new Vector3f(0, -9.81f, 0);

    // Automatically derived parameters
    // private float waterDensity = 1000f; // kg/m³ (density of water)
    // private float waterDrag;
    // private float angularDrag;
    // private float buoyancyFactor;

    // // Configurable parameters
    // private float densityFactor = 1.0f; // Multiplier for automatic density calculation
    // private float dragFactor = 1.0f; // Multiplier for automatic drag calculation
    // private float floatHeight = 0.0f; // How much of object is submerged (0-1) for non-physics objects

    // // Floating properties
    // private float volume = 1.0f;
    // private float submergedRatio = 0.0f;
    // private float waterHeight = 0.0f;
    // private float objectHeight = 1.0f;

    // // Simple animation for non-physics objects
    // private float bobStrength = 0.1f; // Strength of bobbing motion
    // private float bobSpeed = 1.0f; // Speed of bobbing motion
    // private float lerpSpeed = 2.0f; // Interpolation speed for position
    // private float timeAccumulator = 0; // For bobbing time
    private volatile boolean currentUnderWaterState = false;
    private volatile boolean nextUnderWaterState = false;
    private volatile float buoyancy = 0;

    public void setAppState(OceanAppState appState) {
        this.appState = appState;
    }

    @Override
    public void setSpatial(com.jme3.scene.Spatial spatial) {
        super.setSpatial(spatial);
        // if (spatial != null) {
        // deriveParameters();
        // }
    }

    // /**
    // * Automatically calculates optimal buoyancy parameters based on the object's physical properties and
    // size
    // */
    // private void deriveParameters() {
    // // Get dimensions from world bound
    // BoundingVolume bv = spatial.getWorldBound();
    // volume = 1.0f;
    // float crossSection = 1.0f;

    // if (bv instanceof BoundingBox) {
    // BoundingBox box = (BoundingBox) bv;
    // float width = box.getXExtent() * 2;
    // float height = box.getYExtent() * 2;
    // float length = box.getZExtent() * 2;

    // volume = width * height * length;
    // crossSection = Math.max(width * length, Math.max(width * height, height * length));
    // objectHeight = height;
    // } else if (bv instanceof BoundingSphere) {
    // BoundingSphere sp = (BoundingSphere) bv;
    // float radius = sp.getRadius();
    // volume = (4f / 3f) * FastMath.PI * radius * radius * radius;
    // crossSection = FastMath.PI * radius * radius;
    // objectHeight = radius * 2;
    // }

    // RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);

    // if (rbc != null) {
    // // Get mass from physics control
    // float mass = rbc.getMass();

    // // Calculate density (mass/volume)
    // float objectDensity = mass / volume;

    // // Calculate buoyancy factor based on density difference
    // // If object density < water density, it floats
    // buoyancyFactor = waterDensity / objectDensity * densityFactor * 9.81f; // 9.81 = gravity

    // // Calculate drag based on cross-section and water resistance
    // waterDrag = 0.5f * crossSection * dragFactor;
    // angularDrag = 0.2f * volume * dragFactor;
    // } else {
    // // For non-physics objects, use reasonable defaults
    // buoyancyFactor = 9.81f; // just gravity
    // waterDrag = 0.1f * crossSection;
    // angularDrag = 0.05f * volume;
    // }
    // }

    // /**
    // * Manually set the object's effective density relative to water Values < 1.0 will float, > 1.0 will
    // sink
    // */
    // public void setRelativeDensity(float relativeDensity) {
    // this.densityFactor = 1.0f / relativeDensity;
    // deriveParameters();
    // }

    // /**
    // * Set how high the object floats (0.0 = fully on surface, 1.0 = fully submerged)
    // */
    // public void setFloatHeight(float height) {
    // this.floatHeight = FastMath.clamp(height, 0.0f, 1.0f);
    // }

    // /**
    // * Adjust water drag factor (higher = more drag)
    // */
    // public void setDragFactor(float dragFactor) {
    // this.dragFactor = dragFactor;
    // deriveParameters();
    // }

    // /**
    // * Get current submersion ratio (0-1)
    // */
    // public float getSubmergedRatio() {
    // return submergedRatio;
    // }

    // /**
    // * Get current water height at object position
    // */
    // public float getWaterHeight() {
    // return waterHeight;
    // }

    // /**
    // * Get object volume
    // */
    // public float getVolume() {
    // return volume;
    // }
    boolean appendedToPhysicsSpace = false;

    @Override
    protected void controlUpdate(float tpf) {
        if (appState == null) return;
        if (!appendedToPhysicsSpace) {

            BulletAppState bulletAppState = appState.getStateManager().getState(BulletAppState.class);
            if (bulletAppState != null) {
                bulletAppState.getPhysicsSpace().addTickListener(this);
                appendedToPhysicsSpace = true;
            }

        }

        // Vector3f pos = spatial.getWorldTranslation();
        // waterHeight = appState.getWaterHeightAt(pos.x, pos.z);

        // // Get bottom of object
        // float objectBottom = pos.y - objectHeight / 2;

        // // Calculate how much is submerged
        // float submergedDepth = Math.max(0, waterHeight - objectBottom);
        // submergedRatio = Math.min(submergedDepth / objectHeight, 1.0f);

        // RigidBodyControl rbc = spatial.getControl(RigidBodyControl.class);

        // // Check if this is a boat (for possible special handling)
        // BoatControl boatControl = spatial.getControl(BoatControl.class);
        // boolean isBoat = boatControl != null;

        // // Apply buoyancy to ALL physics objects including boats
        // if (rbc != null) {
        // if (submergedDepth > 0) {
        // // Get volume from bounding box
        // // Archimedes' principle
        // float gravity = 9.81f;

        // // Enhanced buoyancy for boats to ensure they float better
        // float buoyancyMultiplier = isBoat ? 1.5f : 1.0f;

        // float displacedMass = waterDensity * volume * submergedRatio * buoyancyMultiplier;
        // float buoyancyForce = displacedMass * gravity;

        // // Apply upward force
        // Vector3f force = new Vector3f(0, buoyancyForce, 0);
        // rbc.applyForce(force, Vector3f.ZERO);

        // // Apply drag - proper velocity-based drag
        // Vector3f velocity = rbc.getLinearVelocity();
        // float speed = velocity.length();
        // if (speed > 0.01f) {
        // float dragMultiplier;

        // if (isBoat) {
        // // For boats, get direction components for directional drag
        // Vector3f forwardDir = rbc.getPhysicsRotation().mult(Vector3f.UNIT_Z);
        // Vector3f rightDir = rbc.getPhysicsRotation().mult(Vector3f.UNIT_X);
        // Vector3f upDir = rbc.getPhysicsRotation().mult(Vector3f.UNIT_Y);

        // // Calculate velocity components
        // float vForward = forwardDir.dot(velocity);
        // float vRight = rightDir.dot(velocity);
        // float vUp = upDir.dot(velocity);

        // // Apply directional drag - more drag sideways than forward
        // Vector3f dragForce = new Vector3f();
        // float forwardDragCoef = 0.05f;
        // float sidewaysDragCoef = 0.3f;

        // dragForce.addLocal(forwardDir
        // .mult(-forwardDragCoef * vForward * Math.abs(vForward) * submergedRatio));
        // dragForce.addLocal(rightDir
        // .mult(-sidewaysDragCoef * vRight * Math.abs(vRight) * submergedRatio));
        // dragForce.addLocal(
        // upDir.mult(-sidewaysDragCoef * vUp * Math.abs(vUp) * submergedRatio));

        // rbc.applyForce(dragForce, Vector3f.ZERO);
        // } else {
        // // Simple uniform drag for non-boat objects
        // float dragMagnitude = 2.0f * speed * speed * submergedRatio;
        // Vector3f dragForce = velocity.normalize().mult(-dragMagnitude);
        // rbc.applyForce(dragForce, Vector3f.ZERO);
        // }
        // }

        // // Angular drag - less for boats to allow proper turning
        // Vector3f angularVelocity = rbc.getAngularVelocity();
        // float angularSpeed = angularVelocity.length();
        // if (angularSpeed > 0.01f) {
        // float angularDragFactor = isBoat ? 0.5f : 1.0f;
        // angularDragFactor *= submergedRatio;
        // Vector3f angularDragTorque = angularVelocity.normalize()
        // .mult(-angularSpeed * angularDragFactor);
        // rbc.applyTorque(angularDragTorque);
        // }
        // }
        // } else { // Simple position-based floating for non-physics objects
        // // Calculate target height based on float height
        // float targetHeight = waterHeight - objectHeight * floatHeight;

        // // Add bobbing motion
        // timeAccumulator += tpf * bobSpeed;
        // float bobOffset = FastMath.sin(timeAccumulator) * bobStrength;
        // targetHeight += bobOffset;

        // // Calculate new position with smooth interpolation
        // Vector3f newPos = new Vector3f(pos);
        // float currentHeight = pos.y;
        // float interpolatedHeight = currentHeight
        // + (targetHeight - currentHeight) * Math.min(tpf * lerpSpeed, 1.0f);
        // newPos.y = interpolatedHeight;

        // // Update spatial position
        // spatial.setLocalTranslation(newPos);

        // // Add slight rotation based on bobbing
        // Quaternion currentRotation = spatial.getLocalRotation();
        // Quaternion bobRotation = new Quaternion();
        // bobRotation.fromAngles(FastMath.sin(timeAccumulator * 0.6f) * 0.02f, 0,
        // FastMath.sin(timeAccumulator * 0.7f) * 0.02f);
        // spatial.setLocalRotation(currentRotation.mult(bobRotation));
        // }
        Vector3f wpos = spatial.getWorldTranslation();
        float mass = 10f;
        RigidBodyControl rb = spatial.getControl(RigidBodyControl.class);
        if (rb == null) return;

        mass = rb.getMass();

        float buoyancy = 0;
        float waterHeight = getWaterHeight();
        boolean belowWaterLevel = wpos.y < waterHeight;
        if (belowWaterLevel) {
            if (!currentUnderWaterState) {
                // SPLASH1.setLocalTranslation(spatial.getLocalTranslation());
                // SPLASH1.play();
            }
            if (mass < ((BoundingBox) spatial.getWorldBound()).getVolume() * 8.f) {
                buoyancy = -(waterHeight - wpos.y) * tpf * 3.1f;
            }
            nextUnderWaterState = true;

        } else {
            if (currentUnderWaterState) {
                // SPLASH2.setLocalTranslation(spatial.getLocalTranslation());
                // SPLASH2.play();
            }
            nextUnderWaterState = false;
            // ControlEvent ev = new ControlEvent(PRE_STATE_CHANGE, 0, UNDERWATER);
            // fireEvent(ev);
            // ev = new ControlEvent(ON_STATE_CHANGE, 0, UNDERWATER);
            // fireEvent(ev);
        }

        this.buoyancy = buoyancy;

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Nothing to do here
    }

    public float getWaterHeight() {
        if (appState == null) return 0;
        Vector3f pos = spatial.getWorldTranslation();
        return appState.getWaterHeightAt(pos.x, pos.z);
    }

    public float getSubmergedRatio() {
        if (appState == null) return 0;
        Vector3f pos = spatial.getWorldTranslation();
        float waterHeight = appState.getWaterHeightAt(pos.x, pos.z);
        float objectBottom = pos.y - ((BoundingBox) spatial.getWorldBound()).getYExtent();
        float submergedDepth = Math.max(0, waterHeight - objectBottom);
        return Math.min(submergedDepth / ((BoundingBox) spatial.getWorldBound()).getYExtent(), 1.0f);
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {

        if (currentUnderWaterState != nextUnderWaterState) {
            RigidBodyControl rb = spatial.getControl(RigidBodyControl.class);
            if (!nextUnderWaterState) {
                rb.setGravity(oldGravity);
            } else {
                oldGravity.set(rb.getGravity());
                rb.applyImpulse(new Vector3f(0, buoyancy, 0), Vector3f.ZERO);
                rb.setGravity(new Vector3f());
            }
            currentUnderWaterState = nextUnderWaterState;
        }

    }

    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {

    }
}