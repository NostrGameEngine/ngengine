package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.messages.ByteDataMessage;

import com.jme3.network.serializing.Serializer;

public class ByteMessageSerializer extends DynamicSerializer{
    

    @Override
    public <T> T readObject(ByteBuffer buffer, Class<T> c) throws IOException {
        try{
            ByteDataMessage message = (ByteDataMessage) c.getDeclaredConstructor().newInstance();
            int length = buffer.getInt();
            ByteBuffer bbf = buffer.slice(buffer.position(), length);
            message.setData(bbf);
            return (T) message;
        } catch (Exception e) {
            throw new IOException("Error deserializing ByteMessage", e);
        }
    }

 
    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        ByteDataMessage message = (ByteDataMessage) object;
        ByteBuffer data = message.getData().slice();
        if (data == null) throw new IOException("The message data is null");
        buffer.putInt(data.remaining());
        buffer.put(data);
    }
    
}
