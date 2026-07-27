/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.components.jme3.audio;

import java.util.Map;
import java.util.WeakHashMap;

import com.jme3.math.FastMath;

/**
 * Process-wide master gains used by engine {@link Sound} instances.
 *
 * <p>Games should configure the gains before mounting components that create
 * or start sounds. Per-sound volume remains independent and is multiplied by
 * the appropriate master gain when the renderer reads it.</p>
 */
public final class AudioMixer {
    private static volatile float musicVolume = 1f;
    private static volatile float soundEffectsVolume = 1f;
    private static final Map<Sound, Boolean> sounds = new WeakHashMap<>();

    private AudioMixer() {
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    public static void setMusicVolume(float volume) {
        float normalized = normalized(volume);
        if (musicVolume != normalized) {
            musicVolume = normalized;
            refresh(AudioCategory.MUSIC);
        }
    }

    public static float getSoundEffectsVolume() {
        return soundEffectsVolume;
    }

    public static void setSoundEffectsVolume(float volume) {
        float normalized = normalized(volume);
        if (soundEffectsVolume != normalized) {
            soundEffectsVolume = normalized;
            refresh(null);
        }
    }

    public static float getVolume(AudioCategory category) {
        return category == AudioCategory.MUSIC ? musicVolume : soundEffectsVolume;
    }

    static float apply(AudioCategory category, float sourceVolume) {
        return sourceVolume * getVolume(category);
    }

    static void register(Sound sound) {
        synchronized (sounds) {
            sounds.put(sound, Boolean.TRUE);
        }
    }

    private static void refresh(AudioCategory category) {
        synchronized (sounds) {
            for (Sound sound : sounds.keySet()) {
                if (sound != null
                        && (category == null || sound.getAudioCategory() == category)
                        && (category != null || sound.getAudioCategory() != AudioCategory.MUSIC)) {
                    sound.refreshMixerVolume();
                }
            }
        }
    }

    private static float normalized(float volume) {
        return Float.isFinite(volume) ? FastMath.clamp(volume, 0f, 1f) : 1f;
    }
}
