package org.ngengine.network;

import org.ngengine.network.protocol.NetworkSafe;

import com.jme3.network.AbstractMessage;
import java.util.Objects;

/**
 * Internal control message used to negotiate logical custom channels between peers.
 */
@NetworkSafe
public class OpenChannelMessage extends AbstractMessage {
    private int channel;

    public OpenChannelMessage() {}

    public OpenChannelMessage(int channel) {
        this.channel = channel;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(channel));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OpenChannelMessage)) return false;
        OpenChannelMessage other = (OpenChannelMessage) obj;
        return channel == other.channel;
    }
}
