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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.controls;

import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.influencers.NewtonianParticleInfluencer;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import org.ngengine.demo.son.ocean.OceanAppState;

public class BuoyancyControl extends AbstractControl implements PhysicsTickListener {

    public static class SamplingPoint {

        volatile float x, y, z;
        volatile float waterHeight;
    }

    private transient OceanAppState appState;
    private boolean appendedToPhysicsSpace = false;
    private float waterDensity = 1000f;

    private Vector3f prevVelocity = new Vector3f();
    private long lastPhysicsTickTime = 0;
    private float accelerationTiltFactor = 0.02f; // Forward/backward tilt during acceleration/braking
    private float bankingFactor = 0.3f;
    // For smooth tilt calculations
    private float smoothedWaterPitch = 0f;
    private float smoothedWaterRoll = 0f;
    private float waterTiltSmoothing = 0.85f; // Higher = smoother (0.0-1.0)

    private SamplingPoint s0 = new SamplingPoint();
    private SamplingPoint sf = new SamplingPoint();
    private SamplingPoint sb = new SamplingPoint();
    private SamplingPoint sl = new SamplingPoint();
    private SamplingPoint sr = new SamplingPoint();

    private float objectLength = 0.5f; // Length of the object in meters
    private float objectWidth = 0.5f; // Width of the object in meters

    private AudioNode splash;
    private ParticleEmitter splashParticles;
    private Material splashParticlesMaterial;

    public void setAppState(OceanAppState appState) {
        this.appState = appState;
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            splash = new AudioNode(appState.getAssetManager(), "Sounds/watersplash.ogg", DataType.Buffer);
            splash.setPositional(true);
            node.attachChild(splash);
            splash.setPositional(true);

            splashParticles = new ParticleEmitter("Splash Particles", Type.Triangle, 30);
            splashParticles.setInWorldSpace(false);
            splashParticles.setShape(new EmitterSphereShape(new Vector3f(0, 0, 0), 1f));
            splashParticles.setParticleInfluencer(new NewtonianParticleInfluencer());
            splashParticles.setParticlesPerSec(0);

            node.attachChild(splashParticles);

            splashParticlesMaterial = new Material(appState.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
            // splashParticlesMaterial.setTexture("Texture",
            //         appState.getApplication().getAssetManager().loadTexture("Effects/Explosion/flame.png"));
            splashParticles.setMaterial(splashParticlesMaterial);
        }
    }

    public float getWaterHeight() {
        if (appState == null) {
            return 0;
        }
        Vector3f pos = spatial.getWorldTranslation();
        return appState.getWaterHeightAt(pos.x, pos.z);
    }

    @Override
    public void setSpatial(com.jme3.scene.Spatial spatial) {
        super.setSpatial(spatial);
        BoundingBox bbox = (BoundingBox) spatial.getWorldBound();
        if (bbox != null) {
            float xExtent = bbox.getXExtent();
            float zExtent = bbox.getZExtent();

            objectLength = xExtent;
            objectWidth = zExtent;
        }
    }

    Vector3f samplingPos = new Vector3f();

    @Override
    protected void controlUpdate(float tpf) {
        if (appState == null) return;

        // Ensure this control is registered as a physics tick listener
        if (!appendedToPhysicsSpace) {
            BulletAppState bulletAppState = appState.getPhysics();
            if (bulletAppState != null) {
                bulletAppState.getPhysicsSpace().addTickListener(this);
                appendedToPhysicsSpace = true;
            }
        }

        Vector3f wpos = spatial.getWorldTranslation();
        Quaternion wrot = spatial.getWorldRotation();

        // sample at wpos
        s0.x = wpos.x;
        s0.y = wpos.y;
        s0.z = wpos.z;
        s0.waterHeight = appState.getWaterHeightAt(s0.x, s0.z);

        // sample in front of wpos

        float zEX = objectLength / 2f;
        float xEX = objectWidth / 2f;
        Vector3f at = wpos.add(wrot.mult(new Vector3f(0, 0, zEX)));
        sf.x = at.x;
        sf.y = at.y;
        sf.z = at.z;
        sf.waterHeight = appState.getWaterHeightAt(sf.x, sf.z);

        // sample behind wpos
        at = wpos.add(wrot.mult(new Vector3f(0, 0, -zEX)));
        sb.x = at.x;
        sb.y = at.y;
        sb.z = at.z;
        sb.waterHeight = appState.getWaterHeightAt(sb.x, sb.z);

        // sample to the left of wpos
        at = wpos.add(wrot.mult(new Vector3f(-xEX, 0, 0)));
        sl.x = at.x;
        sl.y = at.y;
        sl.z = at.z;
        sl.waterHeight = appState.getWaterHeightAt(sl.x, sl.z);

        // sample to the right of wpos
        at = wpos.add(wrot.mult(new Vector3f(xEX, 0, 0)));
        sr.x = at.x;
        sr.y = at.y;
        sr.z = at.z;
        sr.waterHeight = appState.getWaterHeightAt(sr.x, sr.z);

        // System.out.println("s0: " + s0.waterHeight + " sf: " + sf.waterHeight + " sb: " + sb.waterHeight + " sl: " + sl.waterHeight + " sr: " + sr.waterHeight);

        float d = Math.abs(s0.waterHeight - s0.y);
        if (d > 1.4f) {
            if (splash != null && splash.getStatus() != AudioSource.Status.Playing) {
                splash.setPitch(0.9f);
                splash.setVolume(2f); //Math.clamp(d-0.4f, 0.4f, 1f ));
                splash.play();
                splashParticles.setLowLife(1.9f);
                splashParticles.setHighLife(1.9f);
                splashParticles.setEndSize(1.8f);
                splashParticles.setStartSize(.3f);
                splashParticles.setStartColor(new ColorRGBA(35.0f / 255.0f, 0.0f, 110f / 255.0f, 1f).setAlpha(0.8f));
                splashParticles.setEndColor(new ColorRGBA(35.0f / 255.0f, 0.0f, 110f / 255.0f, 1f).setAlpha(0.0f));

                EmitterSphereShape shape = (EmitterSphereShape) splashParticles.getShape();
                shape.setRadius(8f);
                NewtonianParticleInfluencer rp = (NewtonianParticleInfluencer) splashParticles.getParticleInfluencer();
                // rp.setRadialVelocity(20f);
                // rp.setSurfaceTangentFactor(0f);
                // rp.setVelocityVariation(0f);
                rp.setVelocityVariation(0.2f);
                rp.setNormalVelocity(1f);
                // rp.setRadialVelocity(100f);
                rp.setInitialVelocity(new Vector3f(0, 0.6f, 0));
                splashParticles.setGravity(0, 12.81f, 0);
                // rp.setHorizontal(true);

                // splashParticles.emitParticles(22);
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}

    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        RigidBodyControl rb = spatial.getControl(RigidBodyControl.class);
        if (rb == null || appState == null) return;

        float angles[] = new float[3];
        rb.getPhysicsRotation().toAngles(angles);

        // Apply buoyancy force
        {
            float higherWaterHeight = s0.waterHeight;
            float lowestSurfaceY = s0.y;

            float heightDiff = higherWaterHeight - lowestSurfaceY;
            float buoyancyForce = heightDiff * waterDensity * 0.81f;
            if (buoyancyForce > 0) {
                rb.applyCentralForce(new Vector3f(0, buoyancyForce, 0));
            } else {
                rb.applyCentralForce(new Vector3f(0, buoyancyForce, 0));
            }
        }

        // 1. Improved water-based tilt calculations with smooth proportional response
        float tiltSpeed = 0.2f;
        float maxWaterTilt = 0.6f;
        waterTiltSmoothing = 1f;

        // Smoother pitch calculation based on actual height differences
        float frontBackDiff = (sf.waterHeight - sb.waterHeight);
        float normalizedPitchDiff = FastMath.clamp(frontBackDiff / (objectLength * 0.6f), -1f, 1f);
        float targetWaterPitch = -normalizedPitchDiff * maxWaterTilt;

        // Smoother roll calculation based on actual height differences
        float leftRightDiff = (sl.waterHeight - sr.waterHeight);
        float normalizedRollDiff = FastMath.clamp(leftRightDiff / (objectWidth * 0.6f), -1f, 1f);
        float targetWaterRoll = -normalizedRollDiff * maxWaterTilt;

        // Apply exponential smoothing for more natural-looking motion
        smoothedWaterPitch = smoothedWaterPitch * waterTiltSmoothing + targetWaterPitch * (1f - waterTiltSmoothing);
        smoothedWaterRoll = smoothedWaterRoll * waterTiltSmoothing + targetWaterRoll * (1f - waterTiltSmoothing);

        // Use the smoothed values
        targetWaterPitch = smoothedWaterPitch;
        targetWaterRoll = smoothedWaterRoll;

        // 2. Velocity-based tilt calculations
        Vector3f velocity = rb.getLinearVelocity();
        Vector3f forward = rb.getPhysicsRotation().mult(Vector3f.UNIT_Z);
        Vector3f right = rb.getPhysicsRotation().mult(Vector3f.UNIT_X);

        // Calculate forward and lateral components of velocity
        float forwardSpeed = velocity.dot(forward);
        float lateralSpeed = velocity.dot(right);

        // Calculate acceleration (change in speed)
        float acceleration = 0;
        if (lastPhysicsTickTime > 0) {
            float prevForwardSpeed = prevVelocity.dot(forward);
            acceleration = (forwardSpeed - prevForwardSpeed) / tpf;
        }

        // Update previous velocity for next frame
        prevVelocity.set(velocity);
        lastPhysicsTickTime = System.currentTimeMillis();

        // Banking (roll) based on lateral velocity - tilt into turns
        float velocityRoll = -lateralSpeed * bankingFactor;

        // Pitch based on acceleration/deceleration
        float velocityPitch = -acceleration * accelerationTiltFactor;

        // 3. Combine water and velocity-based tilting
        float targetPitch = targetWaterPitch + velocityPitch;
        float targetRoll = targetWaterRoll + velocityRoll;

        // Limit maximum tilt
        float maxTotalTilt = 0.4f;
        targetPitch = FastMath.clamp(targetPitch, -maxTotalTilt, maxTotalTilt);
        targetRoll = FastMath.clamp(targetRoll, -maxTotalTilt, maxTotalTilt);

        // 4. Apply constant-speed changes to reach target angles
        float pitchChange = tiltSpeed * tpf;
        if (angles[0] < targetPitch) {
            angles[0] = Math.min(angles[0] + pitchChange, targetPitch);
        } else if (angles[0] > targetPitch) {
            angles[0] = Math.max(angles[0] - pitchChange, targetPitch);
        }

        float rollChange = tiltSpeed * tpf;
        if (angles[2] < targetRoll) {
            angles[2] = Math.min(angles[2] + rollChange, targetRoll);
        } else if (angles[2] > targetRoll) {
            angles[2] = Math.max(angles[2] - rollChange, targetRoll);
        }

        // Apply the rotation
        Quaternion q = new Quaternion().fromAngles(angles);
        rb.setPhysicsRotation(q);

        // Apply damping
        rb.setLinearDamping(0.9f);
        rb.setAngularDamping(0.9f);
    }

    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {}
}
