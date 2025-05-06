package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

public class ByteBufferSerializer extends DynamicSerializer {

    @Override
    public <T> T readObject(ByteBuffer buffer, Class<T> c) throws IOException {
        int length = buffer.getInt();
        ByteBuffer bbf = ByteBuffer.allocate(length);
        bbf.put(buffer);
        bbf.flip();
        return (T)bbf;
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        ByteBuffer bbf = (ByteBuffer) object;
        bbf = bbf.slice();
        byte[] bytes = new byte[bbf.remaining()];
        bbf.get(bytes);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

}
