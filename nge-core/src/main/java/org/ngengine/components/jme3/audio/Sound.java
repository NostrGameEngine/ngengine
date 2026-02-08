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

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.AsyncAssetManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStore;
import org.ngengine.store.DataStoreProvider;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioParam;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.AudioSource;
import com.jme3.audio.AudioStream;
import com.jme3.audio.Filter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.PlaceholderAssets;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

public class Sound implements AudioSource, JmeCloneable, Savable {
    private static final Logger logger = Logger.getLogger(Sound.class.getName());

    public static final int SAVABLE_VERSION = 1;
    private boolean loop = false;
    private float volume = 1;
    private float pitch = 1;
    private float timeOffset = 0;
    private Filter dryFilter;
    private AudioKey audioKey;
    private transient AudioData audioData = null;
    private transient volatile AudioSource.Status status = AudioSource.Status.Stopped;
    private transient volatile int channel = -1;
    private Vector3f previousWorldTranslation = Vector3f.NAN.clone();
    private Vector3f velocity = new Vector3f();
    private boolean reverbEnabled = false;
    private float maxDistance = 200; // 200 meters
    private float refDistance = 10; // 10 meters
    private Filter reverbFilter;
    private boolean directional = false;
    private Vector3f direction = new Vector3f(0, 0, 1);
    private float innerAngle = 360;
    private float outerAngle = 360;
    private boolean positional = true;
    private boolean velocityFromTranslation = false;
    private float lastTpf;
    private Supplier<AudioRenderer> rendererProvider;
    private Transform worldTransform = new Transform();
    private transient AsyncTask<AudioData> loadTask;
    private AssetManager assetManager;
    

    Sound() {
    }
   
   
    Sound(AssetManager assetManager, AudioKey audioKey) {
        setAudioData(null, audioKey);
        this.assetManager = assetManager;
    }


    void setAudioRendererProvider(Supplier<AudioRenderer> rendererProvider){
        this.rendererProvider = rendererProvider;
    } 

    public void preload(Runner mainRunner, AsyncAssetManager assetManager){   
        if(audioKey == null) return; // key unset
        if(audioData != null) return; // data already loaded
        if(loadTask!=null)  return; // data loading
        this.loadTask = NGEPlatform.get().wrapPromise((r,j)->{
            ((AsyncAssetManager)assetManager).loadAssetAsync(audioKey, (res,err)->{
                if(err!=null){
                    j.accept(err);
                } else {
                    r.accept((AudioData)res);
                }
              
            });
        }).then(data->{
            mainRunner.run(()->{
                if(audioData==null){
                    setAudioData(audioData, audioKey);
                }                      
                this.loadTask = null;

            });
            return audioData;
        });
        
        
    }  

    private void load(){
        if(audioKey == null) return; // key unset
        if(audioData != null) return; // data already loaded
        AudioData data = assetManager.loadAsset(audioKey);
        setAudioData(data, audioKey);       
    }  
 

    @Override
    public AudioData getAudioData(){
    
        if(this.audioData==null){
            load();
        }
        if(this.audioData!=null){
            return audioData;
        }
        throw new IllegalStateException("Audio data not loaded for sound: "+audioKey);
    }


    protected AudioRenderer getRenderer( ) {
        if(rendererProvider==null)
            throw new IllegalStateException(
                    "No audio renderer available, make sure call is being performed on render thread.");
        return rendererProvider.get();
        // ComponentManager mng = getComponentManager();
        // if(mng==null)return null;
        // AudioRenderer result = mng.getInstanceOf(AudioRenderer.class);
        // if (result == null)
        //     throw new IllegalStateException(
        //             "No audio renderer available, make sure call is being performed on render thread.");
        // return result;
    }

    /**
     * Start playing the audio.
     */
    public final void play() {
        if (positional && getAudioData().getChannels() > 1) {
            throw new IllegalStateException("Only mono audio is supported for positional audio nodes");
        }
        AudioRenderer renderer = getRenderer();
        renderer.playSource(this);
    }

    /**
     * Start playing an instance of this audio. This method can be used
     * to play the same <code>AudioNode</code> multiple times. Note
     * that changes to the parameters of this AudioNode will not affect the
     * instances already playing.
     */
    public final void playInstance() {
        if (positional && getAudioData().getChannels() > 1) {
            throw new IllegalStateException("Only mono audio is supported for positional audio nodes");
        }
        getRenderer().playSourceInstance(this);
    }

    /**
     * Stop playing the audio that was started with {@link AudioNode#play() }.
     */
    public final void stop() {
        getRenderer().stopSource(this);
    }

    /**
     * Pause the audio that was started with {@link AudioNode#play() }.
     */
    public final void pause() {
        getRenderer().pauseSource(this);
    }

    /**
     * Do not use.
     */
    @Override
    public final void setChannel(int channel) {
        if (status != AudioSource.Status.Stopped) {
            throw new IllegalStateException("Can only set source id when stopped");
        }

        this.channel = channel;
    }

    /**
     * Do not use.
     */
    @Override
    public final int getChannel() {
        return channel;
    }

    /**
     * @return The {#link Filter dry filter} that is set.
     * @see AudioNode#setDryFilter(com.jme3.audio.Filter)
     */
    @Override
    public final Filter getDryFilter() {
        return dryFilter;
    }

    /**
     * Set the dry filter to use for this audio node.
     *
     * When {@link AudioNode#setReverbEnabled(boolean) reverb} is used,
     * the dry filter will only influence the "dry" portion of the audio,
     * e.g. not the reverberated parts of the AudioNode playing.
     *
     * See the relevant documentation for the {@link Filter} to determine the
     * effect.
     *
     * @param dryFilter The filter to set, or null to disable dry filter.
     */
    public final void setDryFilter(Filter dryFilter) {
        this.dryFilter = dryFilter;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.DryFilter);
    }

    /**
     * Set the audio data to use for the audio. Note that this method
     * can only be called once, if for example the audio node was initialized
     * without an {@link AudioData}.
     *
     * @param audioData The audio data contains the audio track to play.
     * @param audioKey The audio key that was used to load the AudioData
     */
    private final void setAudioData(AudioData audioData, AudioKey audioKey) {
        boolean playing = false;
        if (this.audioData != null) {
             playing= this.getStatus() == AudioSource.Status.Playing;
            if(playing)
                stop();
        }

        this.audioData = audioData;
        this.audioKey = audioKey;
        if(playing){
            play();
        }
    }
    
    public final AudioKey getAudioKey() {
        return audioKey;
    }

 
    /**
     * @return The {@link Status} of the audio node.
     * The status will be changed when either the {@link AudioNode#play() }
     * or {@link AudioNode#stop() } methods are called.
     */
    @Override
    public final AudioSource.Status getStatus() {
        return status;
    }

    /**
     * Do not use.
     *
     * @param status the desired status
     */
    @Override
    public final void setStatus(AudioSource.Status status) {
        this.status = status;
    }

    /**
     * Get the Type of the underlying AudioData to see if it's streamed or buffered.
     * This is a shortcut to getAudioData().getType()
     * <b>Warning</b>: Can return null!
     * @return The {@link com.jme3.audio.AudioData.DataType} of the audio node.
     */
    public final DataType getType() {
        if (this.audioData == null&&this.loadTask==null)
            return null;
        else
            return getAudioData().getDataType();
    }

    /**
     * @return True if the audio will keep looping after it is done playing,
     * otherwise, false.
     * @see AudioNode#setLooping(boolean)
     */
    @Override
    public final boolean isLooping() {
        return loop;
    }

    /**
     * Set the looping mode for the audio node. The default is false.
     *
     * @param loop True if the audio should keep looping after it is done playing.
     */
    public final void setLooping(boolean loop) {
        this.loop = loop;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Looping);
    }

    /**
     * @return The pitch of the audio, also the speed of playback.
     *
     * @see AudioNode#setPitch(float)
     */
    @Override
    public final float getPitch() {
        return pitch;
    }

    /**
     * Set the pitch of the audio, also the speed of playback.
     * The value must be between 0.5 and 2.0.
     *
     * @param pitch The pitch to set.
     * @throws IllegalArgumentException If pitch is not between 0.5 and 2.0.
     */
    public final void setPitch(float pitch) {
        if (pitch < 0.5f || pitch > 2.0f) {
            throw new IllegalArgumentException("Pitch must be between 0.5 and 2.0");
        }

        this.pitch = pitch;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Pitch);
    }

    /**
     * @return The volume of this audio node.
     *
     * @see AudioNode#setVolume(float)
     */
    @Override
    public final float getVolume() {
        return volume;
    }

    /**
     * Set the volume of this audio node.
     *
     * The volume is specified as gain. 1.0 is the default.
     *
     * @param volume The volume to set.
     * @throws IllegalArgumentException If volume is negative
     */
    public final void setVolume(float volume) {
        if (volume < 0f) {
            throw new IllegalArgumentException("Volume cannot be negative");
        }

        this.volume = volume;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Volume);
    }

    /**
     * @return the time offset in the sound sample when to start playing.
     */
    @Override
    public final float getTimeOffset() {
        return timeOffset;
    }

    /**
     * Set the time offset in the sound sample when to start playing.
     *
     * @param timeOffset The time offset
     * @throws IllegalArgumentException If timeOffset is negative
     */
    public final void setTimeOffset(float timeOffset) {
        if (timeOffset < 0f) {
            throw new IllegalArgumentException("Time offset cannot be negative");
        }

        this.timeOffset = timeOffset;
        if (getAudioData() instanceof AudioStream) {
            ((AudioStream) getAudioData()).setTime(timeOffset);
        } else if (status == AudioSource.Status.Playing) {
            stop();
            play();
        }
    }

    @Override
    public final float getPlaybackTime() {
        if (channel >= 0)
            return getRenderer().getSourcePlaybackTime(this);
        else
            return 0;
    }


    /**
     * @return The velocity of the audio node.
     *
     * @see AudioNode#setVelocity(com.jme3.math.Vector3f)
     */
    @Override
    public final Vector3f getVelocity() {
        return velocity;
    }

    /**
     * Set the velocity of the audio node. The velocity is expected
     * to be in meters. Does nothing if the audio node is not positional.
     *
     * @param velocity The velocity to set.
     * @see AudioNode#setPositional(boolean)
     */
    public final void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Velocity);
    }

    /**
     * @return True if reverb is enabled, otherwise false.
     *
     * @see AudioNode#setReverbEnabled(boolean)
     */
    @Override
    public final boolean isReverbEnabled() {
        return reverbEnabled;
    }

    /**
     * Set to true to enable reverberation effects for this audio node.
     * Does nothing if the audio node is not positional.
     * <br>
     * When enabled, the audio environment set with
     * {@link AudioRenderer#setEnvironment(com.jme3.audio.Environment) }
     * will apply a reverb effect to the audio playing from this audio node.
     *
     * @param reverbEnabled True to enable reverb.
     */
    public final void setReverbEnabled(boolean reverbEnabled) {
        this.reverbEnabled = reverbEnabled;
        if (channel >= 0) {
            getRenderer().updateSourceParam(this, AudioParam.ReverbEnabled);
        }
    }

    /**
     * @return Filter for the reverberations of this audio node.
     *
     * @see AudioNode#setReverbFilter(com.jme3.audio.Filter)
     */
    @Override
    public final Filter getReverbFilter() {
        return reverbFilter;
    }

    /**
     * Set the reverb filter for this audio node.
     * <br>
     * The reverb filter will influence the reverberations
     * of the audio node playing. This only has an effect if
     * reverb is enabled.
     *
     * @param reverbFilter The reverb filter to set.
     * @see AudioNode#setDryFilter(com.jme3.audio.Filter)
     */
    public final void setReverbFilter(Filter reverbFilter) {
        this.reverbFilter = reverbFilter;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.ReverbFilter);
    }

    /**
     * @return Maximum distance for this audio node.
     *
     * @see AudioNode#setMaxDistance(float)
     */
    @Override
    public final float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Set the maximum distance for the attenuation of the audio node.
     * Does nothing if the audio node is not positional.
     * <br>
     * The maximum distance is the distance beyond which the audio
     * node will no longer be attenuated.  Normal attenuation is logarithmic
     * from refDistance (it reduces by half when the distance doubles).
     * Max distance sets where this fall-off stops and the sound will never
     * get any quieter than at that distance.  If you want a sound to fall-off
     * very quickly then set ref distance very short and leave this distance
     * very long.
     *
     * @param maxDistance The maximum playing distance.
     * @throws IllegalArgumentException If maxDistance is negative
     */
    public final void setMaxDistance(float maxDistance) {
        if (maxDistance < 0) {
            throw new IllegalArgumentException("Max distance cannot be negative");
        }

        this.maxDistance = maxDistance;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.MaxDistance);
    }

    /**
     * @return The reference playing distance for the audio node.
     *
     * @see AudioNode#setRefDistance(float)
     */
    @Override
    public final float getRefDistance() {
        return refDistance;
    }

    /**
     * Set the reference playing distance for the audio node.
     * Does nothing if the audio node is not positional.
     * <br>
     * The reference playing distance is the distance at which the
     * audio node will be exactly half of its volume.
     *
     * @param refDistance The reference playing distance.
     * @throws IllegalArgumentException If refDistance is negative
     */
    public final void setRefDistance(float refDistance) {
        if (refDistance < 0) {
            throw new IllegalArgumentException("Reference distance cannot be negative");
        }

        this.refDistance = refDistance;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.RefDistance);
    }

    /**
     * @return True if the audio node is directional
     *
     * @see AudioNode#setDirectional(boolean)
     */
    @Override
    public final boolean isDirectional() {
        return directional;
    }

    /**
     * Set the audio node to be directional.
     * Does nothing if the audio node is not positional.
     * <br>
     * After setting directional, you should call
     * {@link AudioNode#setDirection(com.jme3.math.Vector3f) }
     * to set the audio node's direction.
     *
     * @param directional If the audio node is directional
     */
    public final void setDirectional(boolean directional) {
        this.directional = directional;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.IsDirectional);
    }

    /**
     * @return The direction of this audio node.
     *
     * @see AudioNode#setDirection(com.jme3.math.Vector3f)
     */
    @Override
    public final Vector3f getDirection() {
        return direction;
    }

    /**
     * Set the direction of this audio node.
     * Does nothing if the audio node is not directional.
     *
     * @param direction a direction vector (alias created)
     * @see AudioNode#setDirectional(boolean)
     */
    public final void setDirection(Vector3f direction) {
        this.direction = direction;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.Direction);
    }

    /**
     * @return The directional audio node, cone inner angle.
     *
     * @see AudioNode#setInnerAngle(float)
     */
    @Override
    public final float getInnerAngle() {
        return innerAngle;
    }

    /**
     * Set the directional audio node cone inner angle.
     * Does nothing if the audio node is not directional.
     *
     * @param innerAngle The cone inner angle.
     */
    public final void setInnerAngle(float innerAngle) {
        this.innerAngle = innerAngle;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.InnerAngle);
    }

    /**
     * @return The directional audio node, cone outer angle.
     *
     * @see AudioNode#setOuterAngle(float)
     */
    @Override
    public final float getOuterAngle() {
        return outerAngle;
    }

    /**
     * Set the directional audio node cone outer angle.
     * Does nothing if the audio node is not directional.
     *
     * @param outerAngle The cone outer angle.
     */
    public final void setOuterAngle(float outerAngle) {
        this.outerAngle = outerAngle;
        if (channel >= 0)
            getRenderer().updateSourceParam(this, AudioParam.OuterAngle);
    }

    /**
     * @return True if the audio node is positional.
     *
     * @see AudioNode#setPositional(boolean)
     */
    @Override
    public final boolean isPositional() {
        return positional;
    }

    /**
     * Set the audio node as positional.
     * The position, velocity, and distance parameters affect positional
     * audio nodes. Set to false if the audio node should play in "headspace".
     *
     * @param positional True if the audio node should be positional, otherwise
     * false if it should be headspace.
     */
    public final void setPositional(boolean positional) {
        this.positional = positional;
        if (channel >= 0) {
            getRenderer().updateSourceParam(this, AudioParam.IsPositional);
        }
    }

    public final boolean isVelocityFromTranslation() {
        return velocityFromTranslation;
    }

    public final void setVelocityFromTranslation(boolean velocityFromTranslation) {
        this.velocityFromTranslation = velocityFromTranslation;
    }

  

     public void update(Transform worldTransform,   float tpf) {
        this.worldTransform.set(worldTransform);
        lastTpf = tpf;
        if (channel < 0) return;
        Vector3f currentWorldTranslation = getWorldTransform().getTranslation();
        if (!previousWorldTranslation.equals(currentWorldTranslation)) {
            getRenderer().updateSourceParam(this, AudioParam.Position);
            if (velocityFromTranslation && !Float.isNaN(previousWorldTranslation.x)) {
                velocity.set(currentWorldTranslation).subtractLocal(previousWorldTranslation)
                        .multLocal(1f / lastTpf);
                getRenderer().updateSourceParam(this, AudioParam.Velocity);
            }
            previousWorldTranslation.set(currentWorldTranslation);
        }
    }

    @Override
    public final Vector3f getPosition(){
        return getWorldTransform().getTranslation();
    }

    protected Transform getWorldTransform(){
        return worldTransform;
    }


    


    
 
    /**
     * Called internally by com.jme3.util.clone.Cloner. Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {

        this.direction = cloner.clone(direction);
        this.velocity = velocityFromTranslation ? new Vector3f() : cloner.clone(velocity);
        this.previousWorldTranslation = Vector3f.NAN.clone();

        // Change in behavior: the filters were not cloned before meaning
        // that two cloned audio nodes would share the same filter instance.
        // While settings will only be applied when the filter is actually
        // set, I think it's probably surprising to callers if the values of
        // a filter change from one AudioNode when a different AudioNode's
        // filter attributes are updated.
        // Plus if they disable and re-enable the thing using the filter then
        // the settings get reapplied, and it might be surprising to have them
        // suddenly be strange.
        // ...so I'll clone them.  -pspeed
        this.dryFilter = cloner.clone(dryFilter);
        this.reverbFilter = cloner.clone(reverbFilter);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(audioKey, "audio_key", null);
        oc.write(loop, "looping", false);
        oc.write(volume, "volume", 1);
        oc.write(pitch, "pitch", 1);
        oc.write(timeOffset, "time_offset", 0);
        oc.write(dryFilter, "dry_filter", null);

        oc.write(velocity, "velocity", null);
        oc.write(reverbEnabled, "reverb_enabled", false);
        oc.write(reverbFilter, "reverb_filter", null);
        oc.write(maxDistance, "max_distance", 20);
        oc.write(refDistance, "ref_distance", 10);

        oc.write(directional, "directional", false);
        oc.write(direction, "direction", null);
        oc.write(innerAngle, "inner_angle", 360);
        oc.write(outerAngle, "outer_angle", 360);

        oc.write(positional, "positional", false);
        oc.write(velocityFromTranslation, "velocity_from_translation", false);
        oc.write(worldTransform, "world_transform", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        audioKey = (AudioKey) ic.readSavable("audio_key", null);
        loop = ic.readBoolean("looping", false);
        volume = ic.readFloat("volume", 1);
        pitch = ic.readFloat("pitch", 1);
        timeOffset = ic.readFloat("time_offset", 0);
        dryFilter = (Filter) ic.readSavable("dry_filter", null);

        velocity = (Vector3f) ic.readSavable("velocity", null);
        reverbEnabled = ic.readBoolean("reverb_enabled", false);
        reverbFilter = (Filter) ic.readSavable("reverb_filter", null);
        maxDistance = ic.readFloat("max_distance", 20);
        refDistance = ic.readFloat("ref_distance", 10);

        directional = ic.readBoolean("directional", false);
        direction = (Vector3f) ic.readSavable("direction", null);
        innerAngle = ic.readFloat("inner_angle", 360);
        outerAngle = ic.readFloat("outer_angle", 360);

        positional = ic.readBoolean("positional", false);
        velocityFromTranslation = ic.readBoolean("velocity_from_translation", false);
        worldTransform = (Transform) ic.readSavable("world_transform", null);

        assetManager = im.getAssetManager();

        if (audioKey != null) {
            try {
                audioData = im.getAssetManager().loadAsset(audioKey);
            } catch (AssetNotFoundException ex) {
                Logger.getLogger(AudioNode.class.getName())
                        .log(Level.FINE, "Cannot locate {0}  ", new Object[]{audioKey});
                audioData = PlaceholderAssets.getPlaceholderAudio();
            }
        }
    }

    @Override
    public String toString() {
        String ret = getClass().getSimpleName()
                + "[status=" + status;
        if (volume != 1f) {
            ret += ", vol=" + volume;
        }
        if (pitch != 1f) {
            ret += ", pitch=" + pitch;
        }
        if (loop) {
            ret += ", looping";
        }
        if (positional) {
            ret += ", pos=" + getPosition();
        }
        if(directional){
            ret+=", dir="+direction;
        }
        if(audioKey!=null){
            ret+=", audioKey="+audioKey;
        }
        return ret + "]";
    }

    @Override
    public Object jmeClone() {
       try {
        return super.clone();
       } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
       }
    }



     

 
}
