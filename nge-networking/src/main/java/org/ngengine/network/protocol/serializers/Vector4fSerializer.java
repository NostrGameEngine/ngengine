package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public class Vector4fSerializer extends DynamicSerializer {

    @Override
    public Vector4f readObject(ByteBuffer data, Class c) throws IOException {
        return new Vector4f(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Vector4f vector = (Vector4f) object;
        buffer.putFloat(vector.getX());
        buffer.putFloat(vector.getY());
        buffer.putFloat(vector.getZ());   
        buffer.putFloat(vector.getW());
    }

    
}
