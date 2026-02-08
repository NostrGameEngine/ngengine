/**
 * Copyright (c) 2025, Nostr Game Engine
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.ngengine.AsyncAssetManager;
import org.ngengine.components.ComponentManager;
import com.jme3.audio.AudioKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Transform;

public class RandomBackgroundMusicComponent extends AbstractAudioComponent {
    private static final java.util.logging.Logger logger = java.util.logging.Logger
            .getLogger(RandomBackgroundMusicComponent.class.getName());
    private final Transform worldTransform = new Transform();
    private final List<AudioKey> soundKeys = new ArrayList<>();

    
    private int offset = 0;
    private int firstMusicId = 1;
    private int lastMusicId = -1;
    private String musicPath = "bgmusic/";
    private int lastSeed = 0;
    private transient Sound selectedSound = null;

    @Override
    protected Transform getWorldTransform() {
        return worldTransform;
    }


    public RandomBackgroundMusicComponent() {
        super();
    }

    public RandomBackgroundMusicComponent(String musicPath, int firstMusicId, int lastMusicId) {
        this();
        this.musicPath = musicPath.endsWith("/") ? musicPath : musicPath + "/";
        this.firstMusicId = firstMusicId;
        this.lastMusicId = lastMusicId;

    }

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        super.onEnable(mng, firstTime);
        if(selectedSound==null){
            selectMusic(lastSeed);
        }   
    }

  

    public void setOffset(int offset) {
        this.offset = offset;
    }

    protected void onAttached() {
        reloadMusic();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(offset, "offset", 0);
        oc.write(firstMusicId, "first_music_id", 1);
        oc.write(lastMusicId, "last_music_id", -1);
        oc.write(musicPath, "music_path", "bgmusic/");
        oc.write(lastSeed, "last_seed", 0);
        oc.write(soundKeys.toArray(new AudioKey[0]), "sound_keys", null);
        
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        offset = ic.readInt("offset", 0);
        firstMusicId = ic.readInt("first_music_id", 1);
        lastMusicId = ic.readInt("last_music_id", -1);
        musicPath = ic.readString("music_path", "bgmusic/");
        lastSeed = ic.readInt("last_seed", 0);
        AudioKey[] keys = (AudioKey[]) ic.readSavableArray("sound_keys", null);
        soundKeys.clear();
        if (keys != null) {
            for (AudioKey k : keys) {
                soundKeys.add(k);
            }
        }
        selectMusic(lastSeed);
    }

    protected void reloadMusic() {
        AsyncAssetManager assetManager = getInstanceOf(AsyncAssetManager.class);

        int s = firstMusicId;
        if (lastMusicId == -1) {
            int i = s;
            while (true) {
                AudioKey k = new AudioKey(musicPath + "" + (i) + ".ogg");
                logger.fine("Locating background music: " + k);
                if (assetManager.locateAsset(k) == null) {
                    break;
                }
                soundKeys.add(k);
                get(k); // preload
                i++;
            }
            logger.info("Located " + (i - 1) + " background music tracks in path: " + musicPath);
            lastMusicId = i - 1;
        }

    }

    protected void selectMusic(int seed) {
        if (lastMusicId == 0) return;
        lastSeed = seed;

        int s = firstMusicId;
        int e = lastMusicId;

        int id = seed;
        id = Math.abs(id) % (e - s + 1) + s;
        id += offset;
        id = id % (e - s + 1) + s;

        AudioKey k = soundKeys.get(id - s);
        logger.info("Selecting random music: " + k);
        Sound newSound = get(k);
        if(selectedSound != newSound){
            if (selectedSound != null) {
                selectedSound.stop();
            }

            selectedSound = newSound;
            selectedSound.setLooping(true);
            selectedSound.setVolume(0.1f);
            selectedSound.setPositional(false);
            selectedSound.play();
        }
       

    }

    public Sound getCurrentMusic() {
        return selectedSound;
    }
}
