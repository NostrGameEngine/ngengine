package org.ngengine.network.protocol.messages;

import java.nio.ByteBuffer;

import com.jme3.network.Message;

public interface ByteDataMessage extends Message{

    public ByteBuffer getData();
    public void setData(ByteBuffer data);
}
