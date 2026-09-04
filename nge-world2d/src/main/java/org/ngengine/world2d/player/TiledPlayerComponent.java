/**
 * Copyright (c) 2025-2026, Nostr Game Engine
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
package org.ngengine.world2d.player;

import org.box2d4j.b2BodyId;
import org.box2d4j.b2ShapeId;
import org.box2d4j.b2Vec2;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.world2d.box2d.TiledPhysicsComponent;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

import static org.box2d4j.B2.*;

/**
 * Base component for a physics-driven player in a Tiled world.
 *
 * <p>The component deliberately does not read input. Games translate keyboard,
 * controller, AI, or network input into a direction vector and pass it to
 * {@link #move(Vector2f, float)}. The helper accelerates the Box2D body toward
 * the requested velocity instead of overwriting it, so joints and collisions
 * can still oppose player movement.</p>
 */
public class TiledPlayerComponent extends AbstractComponent {
    public static final float DEFAULT_MAX_SPEED = 8.5f;
    public static final float DEFAULT_ACCELERATION = 45f;
    public static final float DEFAULT_MOVING_LINEAR_DAMPING = 1.2f;
    public static final float DEFAULT_IDLE_LINEAR_DAMPING = 8f;
    public static final float DEFAULT_SLEEP_SPEED = 0.04f;

    private final b2Vec2 desiredVelocity = new b2Vec2();
    private final b2Vec2 appliedForce = new b2Vec2();
    private b2ShapeId[] shapeBuffer = new b2ShapeId[0];
    private float maxSpeed = DEFAULT_MAX_SPEED;
    private float acceleration = DEFAULT_ACCELERATION;
    private float movingLinearDamping = DEFAULT_MOVING_LINEAR_DAMPING;
    private float idleLinearDamping = DEFAULT_IDLE_LINEAR_DAMPING;
    private float sleepSpeed = DEFAULT_SLEEP_SPEED;

    @Override
    protected void onEnable(ComponentManager manager, boolean firstTime) {
    }

    @Override
    protected void onDisable(ComponentManager manager) {
    }

    /**
     * Moves the player using the mounted {@link TiledPhysicsComponent}.
     * Direction vectors longer than one are normalized, preserving equal speed
     * for diagonal movement.
     *
     * @param direction requested movement direction, or {@code null} to stop
     * @param tpf elapsed frame time in seconds
     * @return true when a live physics body handled the request
     */
    public boolean move(Vector2f direction, float tpf) {
        return move(direction, tpf, true);
    }

    /**
     * Moves the player and optionally lets an idle body sleep.
     *
     * @param direction requested movement direction, or {@code null} to stop
     * @param tpf elapsed frame time in seconds
     * @param allowSleep whether an almost stationary idle body may sleep
     * @return true when a live physics body handled the request
     */
    public boolean move(Vector2f direction, float tpf, boolean allowSleep) {
        TiledPhysicsComponent physics = getInstanceOf(TiledPhysicsComponent.class);
        b2BodyId body = physics != null ? physics.getBody() : null;
        return move(body, direction, tpf, allowSleep);
    }

    /**
     * Applies the configured motion to an explicit body. This overload is
     * useful to input components that temporarily replace normal movement with
     * another physical action.
     */
    public boolean move(b2BodyId body, Vector2f direction, float tpf, boolean allowSleep) {
        if (body == null || !b2Body_IsValid(body) || tpf <= 0f) {
            return false;
        }
        if (b2Body_GetType(body) != b2_dynamicBody) {
            b2Body_SetType(body, b2_dynamicBody);
        }

        float x = direction != null ? direction.x : 0f;
        float y = direction != null ? direction.y : 0f;
        float lengthSquared = x * x + y * y;
        if (lengthSquared > 1f) {
            float inverseLength = 1f / FastMath.sqrt(lengthSquared);
            x *= inverseLength;
            y *= inverseLength;
            lengthSquared = 1f;
        }
        boolean moving = lengthSquared > 1e-4f;
        desiredVelocity.set(x * maxSpeed, y * maxSpeed);
        b2Body_SetLinearDamping(body, moving ? movingLinearDamping : idleLinearDamping);
        applyMovementForce(body, desiredVelocity, acceleration, tpf, appliedForce);

        float sleepSpeedSquared = sleepSpeed * sleepSpeed;
        if (!moving && lengthSquared(b2Body_GetLinearVelocity(body)) < sleepSpeedSquared) {
            desiredVelocity.set(0f, 0f);
            b2Body_SetLinearVelocity(body, desiredVelocity);
            if (allowSleep) {
                b2Body_SetAwake(body, false);
            }
        }
        int shapeCount = b2Body_GetShapeCount(body);
        ensureShapeCapacity(shapeCount);
        b2Body_GetShapes(body, shapeBuffer, shapeCount);
        for (int i = 0; i < shapeCount; i++) {
            b2Shape_SetFriction(shapeBuffer[i], 0f);
        }
        return true;
    }

    /**
     * Accelerates a body toward a target velocity while respecting physics
     * constraints. The supplied scratch vector is reused for the applied force.
     */
    public static void applyMovementForce(
            b2BodyId body,
            b2Vec2 desiredVelocity,
            float acceleration,
            float tpf,
            b2Vec2 forceScratch) {
        if (body == null || desiredVelocity == null || forceScratch == null
                || !b2Body_IsValid(body)
                || !Float.isFinite(acceleration) || acceleration < 0f || tpf <= 0f) {
            return;
        }
        b2Vec2 currentVelocity = b2Body_GetLinearVelocity(body);
        float dvx = desiredVelocity.x - currentVelocity.x;
        float dvy = desiredVelocity.y - currentVelocity.y;
        float maxDelta = acceleration * tpf;
        float deltaSquared = dvx * dvx + dvy * dvy;
        if (deltaSquared > maxDelta * maxDelta && deltaSquared > 1e-8f) {
            float scale = maxDelta / FastMath.sqrt(deltaSquared);
            dvx *= scale;
            dvy *= scale;
        }
        float inverseTpf = 1f / Math.max(tpf, 1e-4f);
        float mass = Math.max(0.001f, b2Body_GetMass(body));
        forceScratch.set(dvx * mass * inverseTpf, dvy * mass * inverseTpf);
        b2Body_ApplyForceToCenter(body, forceScratch, true);
    }

    private void ensureShapeCapacity(int capacity) {
        if (shapeBuffer.length >= capacity) {
            return;
        }
        shapeBuffer = new b2ShapeId[capacity];
    }

    private static float lengthSquared(b2Vec2 value) {
        return value.x * value.x + value.y * value.y;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = requireNonNegative(maxSpeed, "maxSpeed");
    }

    public float getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(float acceleration) {
        this.acceleration = requireNonNegative(acceleration, "acceleration");
    }

    public float getMovingLinearDamping() {
        return movingLinearDamping;
    }

    public void setMovingLinearDamping(float movingLinearDamping) {
        this.movingLinearDamping = requireNonNegative(movingLinearDamping, "movingLinearDamping");
    }

    public float getIdleLinearDamping() {
        return idleLinearDamping;
    }

    public void setIdleLinearDamping(float idleLinearDamping) {
        this.idleLinearDamping = requireNonNegative(idleLinearDamping, "idleLinearDamping");
    }

    public float getSleepSpeed() {
        return sleepSpeed;
    }

    public void setSleepSpeed(float sleepSpeed) {
        this.sleepSpeed = requireNonNegative(sleepSpeed, "sleepSpeed");
    }

    private static float requireNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
        return value;
    }
}
