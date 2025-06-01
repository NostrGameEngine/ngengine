package org.ngengine.demo.son.packets;

import java.time.Instant;

import org.ngengine.network.protocol.NetworkSafe;

import com.jme3.network.Message;

@NetworkSafe
public class AnimPacket implements Message {
    private boolean reliable = true;

    private float flagFactor;
    private float sailFactor;
    private float windFactor;
    private Instant timestamp;


    public AnimPacket() {
        this.timestamp = Instant.now();
    }
    
    public AnimPacket(float flagFactor, float sailFactor, float windFactor) {
        this.flagFactor = flagFactor;
        this.sailFactor = sailFactor;
        this.windFactor = windFactor;
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public float getFlagFactor() {
        return flagFactor;
    }

    public void setFlagFactor(float flagFactor) {
        this.flagFactor = flagFactor;
    }

    public float getSailFactor() {
        return sailFactor;
    }

    public void setSailFactor(float sailFactor) {
        this.sailFactor = sailFactor;
    }

    public float getWindFactor() {
        return windFactor;
    }

    public void setWindFactor(float windFactor) {
        this.windFactor = windFactor;
    }


    @Override
    public Message setReliable(boolean f) {
        reliable = f;
        return this;
    }

    @Override
    public boolean isReliable() {
       return reliable;
    }
    
}
