package org.ngengine.network.protocol.messages;

import org.ngengine.network.protocol.NetworkSafe;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@NetworkSafe
public class ChatMessage extends AbstractMessage implements TextDataMessage {
    private String text;

    public ChatMessage() {
        super();
    }

    public ChatMessage(String text) {
        super();
        this.text = text;
    }

    @Override
    public String getData() {
        return text;
    }

    @Override
    public String toString() {
        return "ChatMessage [text=" + text + "]";
    }

    @Override
    public void setData(String data) {
        this.text = data;
    }
    
}
