/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.components.jme3.audio;

/**
 * Logical mixer category assigned to a {@link Sound}.
 *
 * <p>Interface sounds intentionally share the sound-effects master gain. The
 * separate value lets controls keep their own per-sound trim while exposing a
 * simple two-slider user interface: music and sound effects.</p>
 */
public enum AudioCategory {
    MUSIC,
    SOUND_EFFECT,
    UI
}
