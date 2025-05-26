package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.messages.TextDataMessage;

import com.jme3.network.serializing.Serializer;

public class TextMessageSerializer extends DynamicSerializer{

    @Override
    public <T> T readObject(ByteBuffer buffer, Class<T> c) throws IOException {
        try{
            TextDataMessage message = (TextDataMessage) c.getDeclaredConstructor().newInstance();
            short length = buffer.getShort();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            message.setData(text);
            return (T) message;
        } catch (Exception e) {
            throw new IOException("Error deserializing TextMessage", e);
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        TextDataMessage message = (TextDataMessage) object;
        String data = message.getData();
        if (data == null) throw new IOException("The message data is null");
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) bytes.length);
        buffer.put(bytes);
    }
    
}
