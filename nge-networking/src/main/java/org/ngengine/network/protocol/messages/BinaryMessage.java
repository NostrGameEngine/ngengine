package org.ngengine.network.protocol.messages;

import java.nio.ByteBuffer;

import org.ngengine.network.protocol.NetworkSafe;

import com.jme3.network.AbstractMessage;

@NetworkSafe
public class BinaryMessage extends AbstractMessage implements ByteMessage {

    private ByteBuffer data;

    public BinaryMessage() {
        this.data = null;
    }

    public BinaryMessage(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public ByteBuffer getData() {
        return data;
    }

    @Override
    public void setData(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "BinaryMessage{" +
                "data=" + (data != null ? data.remaining() + " bytes" : "null") +
                '}';
    }
    
}
