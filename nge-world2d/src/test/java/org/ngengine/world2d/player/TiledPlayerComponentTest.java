package org.ngengine.world2d.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.box2d4j.b2BodyDef;
import org.box2d4j.b2BodyId;
import org.box2d4j.b2ShapeDef;
import org.box2d4j.b2Vec2;
import org.box2d4j.b2WorldId;
import org.box2d4j.b2WorldDef;
import org.junit.jupiter.api.Test;

import com.jme3.math.Vector2f;

import static org.box2d4j.B2.*;

public class TiledPlayerComponentTest {
    @Test
    public void movementForceAcceleratesWithoutOverwritingVelocity() {
        b2WorldId world = createWorld();
        b2BodyId body = createBody(world);
        b2Body_SetLinearVelocity(body, new b2Vec2(-6f, 0f));

        TiledPlayerComponent.applyMovementForce(
            body,
            new b2Vec2(12f, 0f),
            TiledPlayerComponent.DEFAULT_ACCELERATION,
            1f / 60f,
            new b2Vec2()
        );
        b2World_Step(world, 1f / 60f, 8);

        assertTrue(b2Body_GetLinearVelocity(body).x > -6f);
        assertTrue(b2Body_GetLinearVelocity(body).x < 0f);
        assertEquals(0f, b2Body_GetLinearVelocity(body).y, 0.001f);
    }

    @Test
    public void invalidFrameDoesNotApplyForce() {
        b2WorldId world = createWorld();
        b2BodyId body = createBody(world);

        TiledPlayerComponent.applyMovementForce(
            body,
            new b2Vec2(12f, 0f),
            TiledPlayerComponent.DEFAULT_ACCELERATION,
            0f,
            new b2Vec2()
        );
        b2World_Step(world, 1f / 60f, 8);

        assertEquals(0f, lengthSquared(b2Body_GetLinearVelocity(body)), 0.001f);
    }

    @Test
    public void vectorMovementNormalizesDiagonalInput() {
        b2WorldId world = createWorld();
        b2BodyId body = createBody(world);
        TiledPlayerComponent player = new TiledPlayerComponent();

        player.move(body, new Vector2f(1f, 1f), 1f / 60f, false);
        b2World_Step(world, 1f / 60f, 8);

        b2Vec2 velocity = b2Body_GetLinearVelocity(body);
        assertEquals(velocity.x, velocity.y, 0.001f);
        assertTrue(Math.sqrt(lengthSquared(velocity)) < player.getMaxSpeed());
        assertEquals(
            TiledPlayerComponent.DEFAULT_MOVING_LINEAR_DAMPING,
            b2Body_GetLinearDamping(body),
            0.001f
        );
    }

    private static b2BodyId createBody(b2WorldId world) {
        b2BodyDef bodyDefinition = b2DefaultBodyDef();
        bodyDefinition.type = b2_dynamicBody;
        b2BodyId body = b2CreateBody(world, bodyDefinition);
        b2ShapeDef fixtureDefinition = b2DefaultShapeDef();
        fixtureDefinition.density = 1f;
        b2CreatePolygonShape(body, fixtureDefinition, b2MakeBox(0.5f, 0.5f));
        return body;
    }

    private static b2WorldId createWorld() {
        b2WorldDef definition = b2DefaultWorldDef();
        definition.gravity.set(0f, 0f);
        return b2CreateWorld(definition);
    }

    private static float lengthSquared(b2Vec2 value) {
        return value.x * value.x + value.y * value.y;
    }
}
