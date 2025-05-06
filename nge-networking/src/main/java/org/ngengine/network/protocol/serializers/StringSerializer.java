 
package org.ngengine.network.protocol.serializers;

import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * Boolean serializer.
 *
 * @author Riccardo Balbo
 */
@SuppressWarnings("unchecked")
public class StringSerializer extends DynamicSerializer {

    @Override
    public String readObject(ByteBuffer data, Class c) throws IOException {
       int length = data.getInt();
         byte[] bytes = new byte[length];
            data.get(bytes);
            String str = new String(bytes, StandardCharsets.UTF_8);
            return str;

    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        String str = (String) object;
        if (str == null) throw new IOException("The string is null");
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
   
    }
}
