package org.ngengine.network.protocol.messages;

import com.jme3.network.Message;

public interface TextMessage extends Message{
    public String getData();
    public void setData(String data);
    
}
