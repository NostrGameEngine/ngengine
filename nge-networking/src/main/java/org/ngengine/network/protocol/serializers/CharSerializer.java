package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * Char serializer.
 *
 * @author Riccardo Balbo
 */
@SuppressWarnings("unchecked")
public class CharSerializer extends DynamicSerializer {

    @Override
    public Character readObject(ByteBuffer data, Class c) throws IOException {
        return data.getChar();
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        buffer.putChar((Character) object);
    }
}
