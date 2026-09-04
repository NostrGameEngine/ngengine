/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.world2d.box2d;

import java.util.ArrayList;
import java.util.List;

import org.box2d4j.b2BodyId;
import org.box2d4j.b2ContactData;
import org.box2d4j.b2Vec2;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.audio.Sound;
import org.ngengine.platform.NGEUtils;
import org.ngengine.world2d.tiled.components.TiledSoundsComponent;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.core.TiledBase;

import com.jme3.math.FastMath;

import static org.box2d4j.B2.*;

/**
 * Plays short positional scrape samples while a dynamic Tiled body moves in
 * contact with another fixture.
 *
 * <p>Configuration is read from the owner through these properties:</p>
 * <ul>
 *   <li>{@code physics.sound.drag}: comma/newline-separated asset paths</li>
 *   <li>{@code physics.sound.dragVolume}: gain, default {@code 0.85}</li>
 *   <li>{@code physics.sound.dragMinSpeed}: m/s, default {@code 0.35}</li>
 *   <li>{@code physics.sound.dragInterval}: seconds, default {@code 1.35}</li>
 *   <li>{@code physics.sound.dragPitchVariation}: default {@code 0.08}</li>
 * </ul>
 *
 * <p>Samples must be mono because playback is positional.</p>
 */
public class TiledPhysicsDragSoundComponent extends AbstractComponent implements TiledEntityLogicFragment {
    private static final String DEFAULT_SOUNDS =
        "org/ngengine/world2d/sounds/drag/scrape-3.ogg,"
        + "org/ngengine/world2d/sounds/drag/scrape-4.ogg,"
        + "org/ngengine/world2d/sounds/drag/scrape-5.ogg,"
        + "org/ngengine/world2d/sounds/drag/scrape-6.ogg,"
        + "org/ngengine/world2d/sounds/drag/scrape-7.ogg,"
        + "org/ngengine/world2d/sounds/drag/scrape-8.ogg";

    private final List<String> paths = new ArrayList<>();
    private float cooldown;
    private float volume;
    private float minSpeedSquared;
    private float interval;
    private float pitchVariation;
    private TiledSoundsComponent sounds;
    private b2ContactData[] contactData = new b2ContactData[0];

    @Override
    protected void onAttached() {
        ComponentManager manager = getComponentManager();
        if (manager != null && manager.getComponent(TiledSoundsComponent.class) == null) {
            Components.mount(manager, new TiledSoundsComponent()).enable();
        }
    }

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        TiledBase owner = getInstanceOf(TiledBase.class);
        parsePaths(stringProperty(owner, "physics.sound.drag", DEFAULT_SOUNDS));
        volume = Math.max(0f, floatProperty(owner, "physics.sound.dragVolume", 0.85f));
        float minSpeed = Math.max(0f, floatProperty(owner, "physics.sound.dragMinSpeed", 0.35f));
        minSpeedSquared = minSpeed * minSpeed;
        interval = Math.max(0.1f, floatProperty(owner, "physics.sound.dragInterval", 1.35f));
        pitchVariation = FastMath.clamp(
            floatProperty(owner, "physics.sound.dragPitchVariation", 0.08f),
            0f,
            0.45f
        );
        sounds = Components.get(mng, TiledSoundsComponent.class).get();
        if (sounds == null) {
            Components.mount(mng, new TiledSoundsComponent()).enable();
            return;
        }
        if (sounds.getComponentManager() == null) {
            sounds = null;
            return;
        }
        for (String path : paths) {
            configure(sounds.get(path));
        }
        cooldown = FastMath.nextRandomFloat() * interval;
    }

    @Override
    protected void onDisable(ComponentManager mng) {
        cooldown = 0f;
        sounds = null;
    }

    @Override
    public void onTiledEntityLogicUpdate(ComponentManager mng, float tpf, TiledBase entity) {
        cooldown = Math.max(0f, cooldown - tpf);
        if (cooldown > 0f || sounds == null || paths.isEmpty()) {
            return;
        }
        TiledPhysicsComponent physics = getInstanceOf(TiledPhysicsComponent.class);
        b2BodyId body = physics != null ? physics.getBody() : null;
        if (body == null
                || !b2Body_IsValid(body)
                || !b2Body_IsEnabled(body)
                || b2Body_GetType(body) != b2_dynamicBody
                || lengthSquared(b2Body_GetLinearVelocity(body)) < minSpeedSquared
                || !hasTouchingContact(body)) {
            return;
        }
        String path = paths.get(FastMath.nextRandomInt(0, paths.size() - 1));
        Sound sound = configure(sounds.get(path));
        sound.setPitch(FastMath.clamp(
            1f + (FastMath.nextRandomFloat() * 2f - 1f) * pitchVariation,
            0.5f,
            2f
        ));
        sound.playInstance();
        cooldown = interval * (0.85f + FastMath.nextRandomFloat() * 0.3f);
    }

    private Sound configure(Sound sound) {
        sound.setPositional(true);
        sound.setVolume(volume);
        sounds.setDistanceInTiles(sound, 1.25f, 16f);
        return sound;
    }

    private boolean hasTouchingContact(b2BodyId body) {
        int capacity = b2Body_GetContactCapacity(body);
        ensureContactCapacity(capacity);
        int count = b2Body_GetContactData(body, contactData, capacity);
        for (int i = 0; i < count; i++) {
            if (contactData[i].manifold.pointCount > 0) {
                return true;
            }
        }
        return false;
    }

    private void ensureContactCapacity(int capacity) {
        if (contactData.length >= capacity) {
            return;
        }
        b2ContactData[] expanded = new b2ContactData[capacity];
        System.arraycopy(contactData, 0, expanded, 0, contactData.length);
        for (int i = contactData.length; i < expanded.length; i++) {
            expanded[i] = new b2ContactData();
        }
        contactData = expanded;
    }

    private static float lengthSquared(b2Vec2 value) {
        return value.x * value.x + value.y * value.y;
    }

    private void parsePaths(String value) {
        paths.clear();
        for (String part : value.split("[\\n|,]+")) {
            String path = part.trim();
            if (!path.isEmpty()) {
                paths.add(path);
            }
        }
    }

    private static String stringProperty(TiledBase owner, String key, String fallback) {
        Object value = owner != null ? owner.getProperty(key) : null;
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private static float floatProperty(TiledBase owner, String key, float fallback) {
        Object value = owner != null ? owner.getProperty(key) : null;
        return value == null ? fallback : NGEUtils.safeFloat(value);
    }
}
