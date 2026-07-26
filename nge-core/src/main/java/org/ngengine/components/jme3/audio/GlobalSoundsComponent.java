/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.components.jme3.audio;

import com.jme3.math.Transform;

/**
 * Audio source collection for non-positional application and interface sounds.
 *
 * <p>The component deliberately uses an identity transform. Callers should set
 * sounds obtained from it to non-positional before playback.</p>
 */
public class GlobalSoundsComponent extends AbstractAudioComponent {
    private final Transform identity = new Transform();

    @Override
    protected Transform getWorldTransform() {
        return identity;
    }
}
