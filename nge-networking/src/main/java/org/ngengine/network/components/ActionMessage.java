package org.ngengine.network.components;

import java.math.BigInteger;
import java.util.Objects;

import org.ngengine.components.actions.ComponentActionEvent;
import org.ngengine.network.RemotePeer;

import com.jme3.network.AbstractMessage;

public abstract class ActionMessage extends AbstractMessage implements ComponentActionEvent {
    private int channel = 1;
    private String componentId = "";
    private RemotePeer source;
    public static final RemotePeer LOCAL_PEER = null;
    private BigInteger networkId;

    void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    void setSource(RemotePeer source) {
        this.source = source;
    }

    public RemotePeer getSource() {
        return source;
    }

    public String getComponentId() {
        return componentId;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }
    
    public BigInteger getNetworkId() {
        return networkId;
    }

    public void setNetworkId(BigInteger networkId) {
        this.networkId = networkId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, componentId, networkId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ActionMessage other = (ActionMessage) obj;
        return channel == other.channel
            && Objects.equals(componentId, other.componentId)
            && Objects.equals(networkId, other.networkId);
    }

}
