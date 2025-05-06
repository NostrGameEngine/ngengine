package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.Vector3f;

public class Vector3fSerializer extends DynamicSerializer {

    @Override
    public Vector3f readObject(ByteBuffer data, Class c) throws IOException {
        return new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Vector3f vector = (Vector3f) object;
        buffer.putFloat(vector.getX());
        buffer.putFloat(vector.getY());
        buffer.putFloat(vector.getZ());   
    }

    
}
