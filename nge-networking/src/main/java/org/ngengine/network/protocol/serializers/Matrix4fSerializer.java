package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public class Matrix4fSerializer extends DynamicSerializer {

    @Override
    public Matrix4f readObject(ByteBuffer data, Class c) throws IOException {
        Matrix4f matrix = new Matrix4f();
        int rows = 4;
        int cols = 4;        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float value = data.getFloat();
                matrix.set(i, j, value);
            }
        }
        return matrix;        
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Matrix4f m = (Matrix4f) object;
        int rows = 4;
        int cols = 4;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                buffer.putFloat(m.get(i, j));
            }
        }
    }

    
}
