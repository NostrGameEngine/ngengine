package org.ngengine.demo.son.packets;

import java.time.Instant;

import org.ngengine.network.protocol.NetworkSafe;
import com.jme3.math.Vector3f;
import com.jme3.network.Message;

@NetworkSafe
public class ShotPacket implements Message {
    private Vector3f from = new Vector3f();
    private Vector3f to = new Vector3f();
    private transient boolean reliable = true;
    private Instant timestamp;

    public ShotPacket() {
        this.timestamp = Instant.now();
    }
    public ShotPacket(Vector3f from, Vector3f to) {
        this.from.set(from);
        this.to.set(to);
        this.timestamp = Instant.now();
    }

    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setFrom(Vector3f from) {
        this.from.set(from);
    }
    public void setTo(Vector3f to) {
        this.to.set(to);
    }
    public Vector3f getFrom() {
        return from;
    }
    public Vector3f getTo() {
        return to;
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