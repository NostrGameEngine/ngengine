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

import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.contacts.Contact;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.audio.Sound;
import org.ngengine.platform.NGEUtils;
import org.ngengine.world2d.tiled.components.TiledSoundsComponent;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.core.TiledBase;

import com.jme3.math.FastMath;

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
            sounds = Components.mount(mng, new TiledSoundsComponent()).enable().get();
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
        Body body = physics != null ? physics.getBody() : null;
        if (body == null
                || !body.isActive()
                || body.getType() != BodyType.DYNAMIC
                || body.getLinearVelocity().lengthSquared() < minSpeedSquared
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

    private boolean hasTouchingContact(Body body) {
        for (org.jbox2d.dynamics.contacts.ContactEdge edge = body.getContactList();
                edge != null;
                edge = edge.next) {
            Contact contact = edge.contact;
            if (contact != null && contact.isTouching()) {
                return true;
            }
        }
        return false;
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
