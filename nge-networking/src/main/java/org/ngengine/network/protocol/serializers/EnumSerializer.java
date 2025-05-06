 
package org.ngengine.network.protocol.serializers;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * Enum serializer.
 *
 * @author Riccardo Balbo
 */
public class EnumSerializer extends DynamicSerializer {
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        try {
            int ordinal = data.getInt();

            if (ordinal == -1) return null;
            T[] enumConstants = c.getEnumConstants();
            if (enumConstants == null) {
                throw new SerializerException("Class has no enum constants:" + c + "  Ordinal:" + ordinal);
            }
            return enumConstants[ordinal];
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        if (object == null) {
            buffer.putInt(-1);
        } else {
            buffer.putInt(((Enum) object).ordinal());
        }
    }
}
