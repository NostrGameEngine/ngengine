package org.ngengine.network.protocol.serializers;
 

 
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.BufferInputStream;
import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.GrowableByteBufferOutputStream;

/**
 * Serializes uses Java built-in method. ! potentially dangerous, use with caution !
 *
 * @author Lars Wesselius, Riccardo Balbo
 * @hidden
 */
@SuppressWarnings("unchecked")
public class SerializableSerializer extends DynamicSerializer {

    @Override
    public Serializable readObject(ByteBuffer data, Class c) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new BufferInputStream(data));
        try {
            return (Serializable) ois.readObject();
        } catch (Exception ex) {
            throw new IOException("Error reading Serializable object", ex);
        } finally {
            ois.close();
        }

    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new GrowableByteBufferOutputStream(buffer));
        try {
            oos.writeObject(object);
        } catch (Exception ex) {
            throw new IOException("Error writing Serializable object", ex);
        } finally {
            oos.close();
        }
    }
}
