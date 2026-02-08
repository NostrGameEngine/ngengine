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

package org.ngengine.web.audio;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.ngengine.platform.NGEPlatform;
import org.ngengine.web.WebBinds;
import org.ngengine.web.WebBindsAsync;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int8Array;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioParam;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.AudioSource;
import com.jme3.audio.AudioStream;
import com.jme3.audio.Environment;
import com.jme3.audio.Filter;
import com.jme3.audio.Listener;
import com.jme3.audio.ListenerParam;
import com.jme3.audio.AudioSource.Status;
import com.jme3.math.Vector3f;
import com.jme3.util.NativeObject;

public class WebAudioRenderer implements AudioRenderer {
    private Logger logger = Logger.getLogger(WebAudioRenderer.class.getName());
    private static final int SAMPLE_RATE = 44100;

    private AtomicInteger idCounter = new AtomicInteger(1);
    private Map<Integer, AudioSource> audioSourceIdMap = new WeakHashMap<>();
    private Map<Integer, Object> otherIdsMap = new WeakHashMap<>();
    private Map<Integer, Runnable> freeMap = new ConcurrentHashMap<>();
    private int ctxId = NativeObject.INVALID_ID;

    @Override
    public void initialize() {
        WebBinds.addAudioEndListener((ctxId,srcId)->{
            if(ctxId!=getContextId())return ;
            AudioSource src = (AudioSource) audioSourceIdMap.get(srcId);
            if(src==null) return;
            if(src.getStatus()==AudioSource.Status.Paused) return;
            src.setStatus(Status.Stopped);
            src.setChannel(-1);
            Runnable free = freeMap.remove(srcId);
            if(free != null) free.run();
        });
    }
    
    private int getNextId(){
        int id;
        do {
            id = idCounter.incrementAndGet();
            if (otherIdsMap.containsKey(id)) id = NativeObject.INVALID_ID;
            else if (audioSourceIdMap.containsKey(id)) id = NativeObject.INVALID_ID;
        }  while (id == NativeObject.INVALID_ID || id == 0);
        return id;
    }
 
    private int getContextId(){
        if(ctxId != NativeObject.INVALID_ID) return ctxId;
        int id = getNextId();
        WebBindsAsync.createAudioContext(SAMPLE_RATE,id);
        freeMap.put(id,NGEPlatform.get().registerFinalizer(this, ()->{
            WebBinds.freeAudioContext(id);
            freeMap.remove(id);
        }));
        ctxId = id;
        return ctxId;
    }

    private int getAudioData(AudioData data){
        boolean updateNeeded = data.isUpdateNeeded();
        int id = data.getId();

        if(id==NativeObject.INVALID_ID){
            id = getNextId();
            otherIdsMap.put(id, data);
            data.setUpdateNeeded();
            updateNeeded = true;
        } else if(updateNeeded){
            Runnable free = freeMap.remove(data.getId());
            if (free != null) free.run();            
        }
        if(updateNeeded){
            JSArray<Float32Array> f32 = WebAudioDataUtils.getF32Data(data,  SAMPLE_RATE);
            WebBindsAsync.createAudioBuffer(getContextId(), id, f32, (int)(data.getDuration()*data.getSampleRate()), data.getSampleRate());
            final int fid = id;
            freeMap.put(id, NGEPlatform.get().registerFinalizer(data, ()->{
                WebBinds.freeAudioBuffer(getContextId(), fid);
                freeMap.remove(fid);
                otherIdsMap.remove(fid);
            }));         
            data.clearUpdateNeeded();   
        }
        return id;
    }

    private int getAudioSourceChannel(AudioSource src,int channel){
        if(channel==-1){
            channel = getNextId();
            WebBindsAsync.createAudioSource(getContextId(), channel);
            audioSourceIdMap.put(channel, src);
            final int fid = channel;
            freeMap.put(channel, NGEPlatform.get().registerFinalizer(src, ()->{
                WebBinds.freeAudioSource(getContextId(), fid);
                freeMap.remove(fid);
                audioSourceIdMap.remove(fid);
            }));
        }
        return channel;
    }

    @Override
    public void updateSourceParam(AudioSource src, AudioParam param) {
        int srcId = src.getChannel();
        updateSourceParam(src, param, srcId);
    }

    public void updateSourceParam(AudioSource src, AudioParam param, int srcId) {
        switch (param) {
            case Position: {
                if (!src.isPositional()) {
                    return;
                }
                Vector3f pos = src.getPosition();
                WebBinds.setAudioPosition(getContextId(),srcId, pos.x, pos.y, pos.z);
                break;
            }
            case Velocity: {
                if (!src.isPositional()) {
                    return;
                }
                Vector3f vel = src.getVelocity();
                WebBinds.setAudioVelocity(getContextId(),srcId, vel.x, vel.y, vel.z);
                break;
            }
            case MaxDistance: {
                if (!src.isPositional()) {
                    return;
                }
                WebBinds.setAudioMaxDistance(getContextId(),srcId, src.getMaxDistance());
                break;
            }
            case RefDistance: {
                if (!src.isPositional()) {
                    return;
                }
                WebBinds.setAudioRefDistance(getContextId(),srcId, src.getRefDistance());
                break;
            }
            case ReverbFilter: {
                break;
            }
            case ReverbEnabled: {
                break;
            }
            case IsPositional: {
                WebBinds.setAudioPositional(getContextId(),srcId, src.isPositional());
                break;
            }
            case Direction: {
                if (!src.isDirectional()) {
                    return;
                }
                Vector3f dir = src.getDirection();
                WebBinds.setAudioDirection(getContextId(),srcId, dir.x, dir.y, dir.z);
                break;
            }
            case InnerAngle: {
                if (!src.isDirectional()) {
                    return;
                }
                WebBinds.setAudioConeInnerAngle(getContextId(),srcId, src.getInnerAngle());
                break;
            }
            case OuterAngle: {
                if (!src.isDirectional()) {
                    return;
                }
                WebBinds.setAudioConeOuterAngle(getContextId(),srcId, src.getOuterAngle());
                break;
            }
            case IsDirectional: {
                if (src.isDirectional()) {
                    updateSourceParam(src, AudioParam.Direction,srcId);
                    updateSourceParam(src, AudioParam.InnerAngle,srcId);
                    updateSourceParam(src, AudioParam.OuterAngle,srcId);
                    WebBinds.setAudioConeOuterGain(getContextId(),srcId, 0);
                } else {
                    WebBinds.setAudioConeInnerAngle(getContextId(),srcId, 360);
                    WebBinds.setAudioConeOuterAngle(getContextId(),srcId, 360);
                    WebBinds.setAudioConeOuterGain(getContextId(),srcId, 1);
                }
                break;
            }
            case DryFilter: {
                break;
            }
            case Looping: {
                WebBinds.setAudioLoop(getContextId(),srcId, src.isLooping());
                break;
            }
            case Volume: {
                WebBinds.setAudioVolume(getContextId(),srcId, src.getVolume());
                break;
            }
            case Pitch: {
                WebBinds.setAudioPitch(getContextId(),srcId, src.getPitch());
                break;
            }
        }
    }

 
    @Override
    public void setListener(Listener listener) {
        listener.setRenderer(this);     
    }

    @Override
    public void deleteAudioData(AudioData ad) {
        Runnable free = freeMap.remove(ad.getId());
        if( free != null ) free.run();
    }

    private void initPlayback(AudioSource src, int srcId){        
        for (AudioParam p : AudioParam.values()) {
            updateSourceParam(src, p, srcId);
        }
    }

    @Override
    public void playSourceInstance(AudioSource src) {
        int bufferId = getAudioData(src.getAudioData());
        int sourceId = getAudioSourceChannel(src, -1);
        initPlayback(src, sourceId);
        WebBindsAsync.setAudioBuffer(getContextId(), sourceId, bufferId);
        WebBindsAsync.playAudioSource(getContextId(), sourceId);
    }

    @Override
    public void playSource(AudioSource src) {
        if (src.getStatus() == AudioSource.Status.Playing) return;        
        int bufferId = getAudioData(src.getAudioData());
        int sourceId = getAudioSourceChannel(src, src.getChannel());
        src.setChannel(sourceId);
        initPlayback(src, sourceId);
        src.setStatus(AudioSource.Status.Playing);
        WebBindsAsync.setAudioBuffer(getContextId(), sourceId, bufferId);
        WebBindsAsync.playAudioSource(getContextId(), sourceId);
    }

    @Override
    public void pauseSource(AudioSource src) {
        if (src.getStatus() == AudioSource.Status.Paused) return;
        if (src.getChannel() == -1) return;
        src.setStatus(AudioSource.Status.Paused);
        WebBindsAsync.pauseAudioSource(getContextId(), src.getChannel());
    }

    @Override
    public void stopSource(AudioSource src) {
        if (src.getStatus() == AudioSource.Status.Stopped) return;
        if (src.getChannel() == -1) return;
        src.setStatus(AudioSource.Status.Stopped);
        WebBindsAsync.stopAudioSource(getContextId(), src.getChannel());
        if (src.getAudioData() instanceof AudioStream) {
            AudioStream stream = (AudioStream) src.getAudioData();
            if (stream.isSeekable()) {
                stream.setTime(0);
            } else {
                stream.close();
            }
        }

    }

    @Override
    public void pauseAll() {
        for (Entry<Integer, AudioSource> e : audioSourceIdMap.entrySet()) {
            AudioSource src = e.getValue();
            if (src.getStatus() == AudioSource.Status.Playing) {
                pauseSource(src);
            }
        }        
    }


   @Override
    public void resumeAll() {
        for (Entry<Integer, AudioSource> e : audioSourceIdMap.entrySet()) {
            AudioSource src = e.getValue();
            if (src.getStatus() == AudioSource.Status.Paused) {
                playSource(src);
            }
        }
    }

    @Override
    public void update(float tpf) {
   
    }

    
    @Override
    public void updateListenerParam(Listener listener, ListenerParam param) {
        Vector3f pos = listener.getLocation();
        Vector3f vel = listener.getVelocity();
        Vector3f dir = listener.getDirection();
        Vector3f up = listener.getUp();
        WebBinds.setAudioContextListener(
            getContextId(), 
            pos.x, pos.y, pos.z, 
            dir.x, dir.y, dir.z, 
            up.x, up.y, up.z
        );
    }

    @Override
    public float getSourcePlaybackTime(AudioSource src) {
        double duration = 0;
        if (src.getAudioData() instanceof AudioStream) {
            AudioStream stream = (AudioStream) src.getAudioData();
            duration = stream.getDuration();
        } else if (src.getAudioData() instanceof com.jme3.audio.AudioBuffer) {
            com.jme3.audio.AudioBuffer buffer = (com.jme3.audio.AudioBuffer) src.getAudioData();
            duration = buffer.getDuration();
        } else {
            throw new UnsupportedOperationException("Unimplemented method 'getSourcePlaybackTime' for "+src.getAudioData().getClass().getName());
        }

        float playBackRate = WebBindsAsync.getAudioPlaybackRate(getContextId(), src.getChannel());
        return (float) (playBackRate * duration);
    }

    @Override
    public void deleteFilter(Filter filter) {
    }

    @Override
    public void setEnvironment(Environment env) {
        Int8Array envData = env!=null?WebAudioDataUtils.getEnvData(env):null;
        WebBinds.setContextAudioEnv(getContextId(), envData);
    }

    @Override
    public void cleanup() {
        for (Entry<Integer, Runnable> e : freeMap.entrySet()) {
            e.getValue().run();
        }
        freeMap.clear();
        audioSourceIdMap.clear();
        otherIdsMap.clear();
        if (ctxId != NativeObject.INVALID_ID) {
            WebBinds.freeAudioContext(ctxId);
            ctxId = NativeObject.INVALID_ID;
        }
    }

 
  
}
