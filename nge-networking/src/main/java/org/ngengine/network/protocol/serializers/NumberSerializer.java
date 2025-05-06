 
package org.ngengine.network.protocol.serializers;

import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * Boolean serializer.
 *
 * @author Riccardo Balbo
 */
@SuppressWarnings("unchecked")
public class NumberSerializer extends DynamicSerializer {

    @Override
    public Number readObject(ByteBuffer data, Class c) throws IOException {
        if(c == Byte.class) {
            return data.get();
        } else if(c == Short.class) {
            return data.getShort();
        } else if(c == Integer.class) {
            return data.getInt();
        } else if(c == Long.class) {
            return data.getLong();
        } else if(c == Float.class) {
            return data.getFloat();
        } else if(c == Double.class) {
            return data.getDouble();
        } else {
            throw new IOException("Unsupported number type: " + c);
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        if(object instanceof Byte) {
            buffer.put((Byte) object);
        } else if(object instanceof Short) {
            buffer.putShort((Short) object);
        } else if(object instanceof Integer) {
            buffer.putInt((Integer) object);
        } else if(object instanceof Long) {
            buffer.putLong((Long) object);
        } else if(object instanceof Float) {
            buffer.putFloat((Float) object);
        } else if(object instanceof Double) {
            buffer.putDouble((Double) object);
        } else {
            throw new IOException("Unsupported number type: " + object.getClass());
        }
    }
}
