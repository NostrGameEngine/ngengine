package org.ngengine.demo.son.packets;

 
import java.time.Instant;

import org.ngengine.network.protocol.NetworkSafe;
import com.jme3.math.Transform;
import com.jme3.network.Message;

 
@NetworkSafe
public class TransformPacket implements Message{
    private Transform transform = new Transform();
    private transient boolean reliable = true;
    private Instant timestamp;

    public TransformPacket() {
        this.timestamp = Instant.now();

    }

    public TransformPacket(Transform transform) {
        this.transform.set(transform);
        this.timestamp = Instant.now();

    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTransform(Transform transform) {
        this.transform.set(transform);
    }

    public Transform getTransform() {
        return transform;
    }

    @Override
    public Message setReliable(boolean f) {
        reliable=f;
        return this;
    }

    @Override
    public boolean isReliable() {
        return reliable;
    }


  
}
