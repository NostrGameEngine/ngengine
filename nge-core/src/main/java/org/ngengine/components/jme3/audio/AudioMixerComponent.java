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

package org.ngengine.components.jme3.audio;

import java.util.Map;
import java.util.WeakHashMap;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;

import com.jme3.math.FastMath;

/**
 * Master gains applied to the {@link Sound} instances of one component manager.
 *
 */
public class AudioMixerComponent extends AbstractComponent {

    private volatile float musicVolume = 1f;
    private volatile float soundEffectsVolume = 1f;

    /**
     * Live sounds, held weakly so the registry never keeps one alive. Only
     * touched from the render thread, so no locking is needed.
     */
    private final Map<Sound, Boolean> sounds = new WeakHashMap<>();

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float volume) {
        float normalized = normalized(volume);
        if (musicVolume != normalized) {
            musicVolume = normalized;
            refresh(AudioCategory.MUSIC);
        }
    }

    public float getSoundEffectsVolume() {
        return soundEffectsVolume;
    }

    public void setSoundEffectsVolume(float volume) {
        float normalized = normalized(volume);
        if (soundEffectsVolume != normalized) {
            soundEffectsVolume = normalized;
            refresh(null);
        }
    }

    /**
     * Returns the master gain applied to a category. Every category other than
     * {@link AudioCategory#MUSIC} shares the sound-effects gain.
     *
     * @param category the category to look up
     * @return the master gain of the category
     */
    public float getVolume(String category) {
        return AudioCategory.MUSIC.equals(category) ? musicVolume : soundEffectsVolume;
    }

    /**
     * Scales a source gain by the master gain of its category.
     *
     * @param category the category of the sound
     * @param sourceVolume the per-sound gain
     * @return the effective gain
     */
    float apply(String category, float sourceVolume) {
        return sourceVolume * getVolume(category);
    }

    /**
     * Registers a sound so gain changes reach it while it plays.
     *
     * @param sound the sound to track
     */
    void register(Sound sound) {
        sounds.put(sound, Boolean.TRUE);
    }

    /**
     * Stops tracking a sound.
     *
     * @param sound the sound to drop
     */
    void unregister(Sound sound) {
        sounds.remove(sound);
    }

    /**
     * Propagates a gain change to the sounds it affects.
     *
     * @param category the changed category, or null for every non-music sound
     */
    private void refresh(String category) {
        for (Sound sound : sounds.keySet()) {
            if (sound == null) {
                continue;
            }
            String soundCategory = sound.getAudioCategory();
            boolean affected = category != null
                    ? category.equals(soundCategory)
                    : !AudioCategory.MUSIC.equals(soundCategory);
            if (affected) {
                sound.refreshMixerVolume();
            }
        }
    }

    private static float normalized(float volume) {
        return Float.isFinite(volume) ? FastMath.clamp(volume, 0f, 1f) : 1f;
    }

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
    }

    @Override
    protected void onDisable(ComponentManager mng) {
    }

    @Override
    protected void onDetached() {
        sounds.clear();
    }
}
