package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public class Matrix3fSerializer extends DynamicSerializer {

    @Override
    public Matrix3f readObject(ByteBuffer data, Class c) throws IOException {
        Matrix3f matrix = new Matrix3f();
        int rows = 3;
        int cols = 3;        
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
        Matrix3f m = (Matrix3f) object;
        int rows = 3;
        int cols = 3;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                buffer.putFloat(m.get(i, j));
            }
        }
    }

    
}
