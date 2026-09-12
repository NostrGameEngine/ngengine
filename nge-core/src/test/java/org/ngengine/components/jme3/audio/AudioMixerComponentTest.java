package org.ngengine.components.jme3.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AudioMixerComponentTest {

    @Test
    void appliesIndependentMusicAndEffectsGains() {
        AudioMixerComponent mixer = new AudioMixerComponent();
        Sound sound = new Sound();
        sound.setAudioMixer(mixer);
        sound.setVolume(0.8f);

        mixer.setMusicVolume(0.25f);
        mixer.setSoundEffectsVolume(0.75f);

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
        AudioMixerComponent mixer = new AudioMixerComponent();

        mixer.setMusicVolume(-2f);
        mixer.setSoundEffectsVolume(4f);
        assertEquals(0f, mixer.getMusicVolume());
        assertEquals(1f, mixer.getSoundEffectsVolume());

        mixer.setMusicVolume(Float.NaN);
        assertEquals(1f, mixer.getMusicVolume());
    }

    @Test
    void mixersOfDifferentManagersStayIndependent() {
        AudioMixerComponent first = new AudioMixerComponent();
        AudioMixerComponent second = new AudioMixerComponent();
        Sound firstSound = new Sound();
        Sound secondSound = new Sound();
        firstSound.setAudioMixer(first);
        secondSound.setAudioMixer(second);
        firstSound.setVolume(1f);
        secondSound.setVolume(1f);

        first.setMusicVolume(0.25f);

        // The gain of one application must not leak into another.
        firstSound.setAudioCategory(AudioCategory.MUSIC);
        secondSound.setAudioCategory(AudioCategory.MUSIC);
        assertEquals(0.25f, firstSound.getVolume(), 0.0001f);
        assertEquals(1f, secondSound.getVolume(), 0.0001f);
    }

    @Test
    void aCustomCategoryFollowsTheSoundEffectsGain() {
        AudioMixerComponent mixer = new AudioMixerComponent();
        Sound sound = new Sound();
        sound.setAudioMixer(mixer);
        sound.setVolume(1f);

        // Any name is a valid category, not just the predefined ones.
        sound.setAudioCategory("voice");
        assertEquals("voice", sound.getAudioCategory());

        mixer.setMusicVolume(0.5f);
        mixer.setSoundEffectsVolume(0.25f);
        assertEquals(0.25f, sound.getVolume(), 0.0001f);

        mixer.setMusicVolume(0.75f);
        assertEquals(0.25f, sound.getVolume(), 0.0001f);
    }

    @Test
    void aMissingCategoryFallsBackToSoundEffects() {
        Sound sound = new Sound();

        sound.setAudioCategory(null);
        assertEquals(AudioCategory.SOUND_EFFECT, sound.getAudioCategory());

        sound.setAudioCategory("");
        assertEquals(AudioCategory.SOUND_EFFECT, sound.getAudioCategory());
    }

    @Test
    void aSoundWithoutAMixerUsesItsSourceGain() {
        AudioMixerComponent mixer = new AudioMixerComponent();
        mixer.setMusicVolume(0.1f);

        Sound sound = new Sound();
        sound.setVolume(0.5f);
        sound.setAudioCategory(AudioCategory.MUSIC);

        // No mixer attached, so the source gain is used as is.
        assertEquals(0.5f, sound.getVolume(), 0.0001f);

        sound.setAudioMixer(mixer);
        assertEquals(0.05f, sound.getVolume(), 0.0001f);

        sound.setAudioMixer(null);
        assertEquals(0.5f, sound.getVolume(), 0.0001f);
    }
}
