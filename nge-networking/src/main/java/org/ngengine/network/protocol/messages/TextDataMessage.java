package org.ngengine.network.protocol.messages;

import com.jme3.network.Message;

public interface TextDataMessage extends Message{
    public String getData();
    public void setData(String data);
    
}
