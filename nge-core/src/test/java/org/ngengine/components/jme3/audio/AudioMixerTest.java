package org.ngengine.components.jme3.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AudioMixerTest {
    @AfterEach
    void restoreDefaultMixer() {
        AudioMixer.setMusicVolume(1f);
        AudioMixer.setSoundEffectsVolume(1f);
    }

    @Test
    void appliesIndependentMusicAndEffectsGains() {
        Sound sound = new Sound();
        sound.setVolume(0.8f);

        AudioMixer.setMusicVolume(0.25f);
        AudioMixer.setSoundEffectsVolume(0.75f);

        sound.setAudioCategory(AudioCategory.MUSIC);
        assertEquals(0.2f, sound.getVolume(), 0.0001f);
        assertEquals(0.8f, sound.getSourceVolume(), 0.0001f);

        sound.setAudioCategory(AudioCategory.SOUND_EFFECT);
        assertEquals(0.6f, sound.getVolume(), 0.0001f);

        sound.setAudioCategory(AudioCategory.UI);
        assertEquals(0.6f, sound.getVolume(), 0.0001f);
    }

    @Test
    void clampsInvalidMasterGains() {
        AudioMixer.setMusicVolume(-2f);
        AudioMixer.setSoundEffectsVolume(4f);
        assertEquals(0f, AudioMixer.getMusicVolume());
        assertEquals(1f, AudioMixer.getSoundEffectsVolume());

        AudioMixer.setMusicVolume(Float.NaN);
        assertEquals(1f, AudioMixer.getMusicVolume());
    }
}
