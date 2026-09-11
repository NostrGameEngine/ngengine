/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.components.jme3.audio;

/**
 * Predefined gain groups assigned to a {@link Sound}.
 *
 * <p>A category is a plain string, so a game can use its own name (for example
 * {@code "voice"}). Only {@link #MUSIC} is special and follows the music master
 * gain of the {@link AudioMixerComponent}; every other category shares the
 * sound-effects gain, which keeps a simple two-slider user interface while each
 * sound keeps its own trim.</p>
 */
public final class AudioCategory {

    /** Follows the music master gain. */
    public static final String MUSIC = "MUSIC";

    /** Follows the sound-effects master gain. */
    public static final String SOUND_EFFECT = "SOUND_EFFECT";

    /** Interface sounds, sharing the sound-effects master gain. */
    public static final String UI = "UI";

    private AudioCategory() {
    }
}
