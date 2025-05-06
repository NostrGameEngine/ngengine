package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public class Vector2fSerializer extends DynamicSerializer {

    @Override
    public Vector2f readObject(ByteBuffer data, Class c) throws IOException {
        return new Vector2f(data.getFloat(), data.getFloat());
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Vector2f vector = (Vector2f) object;
        buffer.putFloat(vector.getX());
        buffer.putFloat(vector.getY());
    }

    
}
