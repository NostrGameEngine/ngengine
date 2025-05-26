package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public class TransformSerializer extends DynamicSerializer {

    @Override
    public Transform readObject(ByteBuffer data, Class c) throws IOException {
        return new Transform(
            new Vector3f(data.getFloat(), data.getFloat(), data.getFloat()),
            new Quaternion(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat()),
            new Vector3f(data.getFloat(), data.getFloat(), data.getFloat())            
        );
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Transform transform = (Transform) object;
        Vector3f translation = transform.getTranslation();
        Quaternion rotation = transform.getRotation();
        Vector3f scale = transform.getScale();
        buffer.putFloat(translation.getX());
        buffer.putFloat(translation.getY());
        buffer.putFloat(translation.getZ());
        buffer.putFloat(rotation.getX());
        buffer.putFloat(rotation.getY());
        buffer.putFloat(rotation.getZ());
        buffer.putFloat(rotation.getW());
        buffer.putFloat(scale.getX());
        buffer.putFloat(scale.getY());
        buffer.putFloat(scale.getZ());

    }

    
}
