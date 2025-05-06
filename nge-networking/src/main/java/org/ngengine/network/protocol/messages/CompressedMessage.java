package org.ngengine.network.protocol.messages;

import java.nio.ByteBuffer;
import java.util.function.BiFunction;

import org.ngengine.network.protocol.NetworkSafe;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;

@NetworkSafe
public class CompressedMessage extends AbstractMessage {
    private Message message;

    public CompressedMessage() {
        super();
    }

    public CompressedMessage(Message message) {
        super();
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CompressedMessage [message=" + message + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        CompressedMessage other = (CompressedMessage) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }
}
