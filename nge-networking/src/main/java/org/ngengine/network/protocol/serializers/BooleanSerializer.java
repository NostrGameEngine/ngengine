 
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
public class BooleanSerializer extends DynamicSerializer {

    @Override
    public Boolean readObject(ByteBuffer data, Class c) throws IOException {
        return data.get() == 1;
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        buffer.put(((Boolean) object) ? (byte) 1 : (byte) 0);
    }
}
