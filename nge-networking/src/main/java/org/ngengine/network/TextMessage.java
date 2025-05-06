package org.ngengine.network;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable

public class TextMessage extends AbstractMessage {
    private String text;

    public TextMessage() {
        super();
    }

    public TextMessage(String text) {
        super();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "TextMessage [text=" + text + "]";
    }
}
