package org.ngengine.network.protocol.serializers;
 

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerRegistration;

public class MapSerializer extends DynamicSerializer {
    private final BiFunction<Object, GrowableByteBuffer, Void> serialize;
    private final BiFunction<ByteBuffer, Class<?>,  Object> deserialize;
    
   public MapSerializer(
          BiFunction<Object, GrowableByteBuffer, Void> serialize,
        BiFunction<ByteBuffer, Class<?>, Object> deserialize
    ){
        this.serialize = serialize;
        this.deserialize = deserialize;

    }
    /*
     * 
     * Structure:
     * 
     * struct Map { INT length BYTE flags = { 0x01 = all keys have the same type, 0x02 = all values have the
     * same type } if (flags has 0x01 set) SHORT keyType if (flags has 0x02 set) SHORT valType
     * 
     * struct MapEntry[length] entries { if (flags does not have 0x01 set) SHORT keyType OBJECT key
     * 
     * if (flags does not have 0x02 set) SHORT valType OBJECT value } }
     * 
     */

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        int length = data.getInt();

        Map map;
        try {
            map = (Map) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.log(Level.WARNING, "[Serializer][???] Could not determine map type. Using HashMap.");
            map = new HashMap();
        }

        for(int i = 0; i < length; i++){
            Object key = this.deserialize.apply(data,c);
            Object value = this.deserialize.apply(data,c);
            map.put(key, value);
        }

        return (T) map;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Map map = (Map) object;
        int length = map.size();

        buffer.putInt(length);
 
        for(Entry entry : (Set<Entry>) map.entrySet()){
            Object key = entry.getKey();
            Object value = entry.getValue();
            this.serialize.apply(key, buffer);
            this.serialize.apply(value, buffer);          
        }
    }
}
