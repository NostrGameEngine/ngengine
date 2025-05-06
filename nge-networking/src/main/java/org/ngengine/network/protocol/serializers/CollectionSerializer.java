 
package org.ngengine.network.protocol.serializers;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerRegistration;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;

import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * Serializes collections.
 *
 * @author Riccardo Balbo
 */
public class CollectionSerializer extends DynamicSerializer {

    private final BiFunction<Object, GrowableByteBuffer, Void> serialize;
    private final BiFunction<ByteBuffer, Class<?>,  Object> deserialize;
    public CollectionSerializer(
        BiFunction<Object, GrowableByteBuffer, Void> serialize,
        BiFunction<ByteBuffer, Class<?>, Object> deserialize
    ){
        this.serialize = serialize;
        this.deserialize = deserialize;

    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        int length = data.getInt();

        Collection collection;
        try {
            collection = (Collection) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.log(Level.FINE, "[Serializer][???] Could not determine collection type. Using ArrayList.");
            collection = new ArrayList(length);
        }

        if (length == 0) return (T) collection;

        for (int i = 0; i < length; i++) {
            Object element = this.deserialize.apply(data,c);
            collection.add(element);
        }

        return (T) collection;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Collection collection = (Collection) object;
        int length = collection.size();

        buffer.putInt(length);
        if (length == 0) return;

        for(Object element : collection){
            this.serialize.apply(element, buffer);
        }
    }
}
