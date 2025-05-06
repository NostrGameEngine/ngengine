package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.Quaternion;

public class QuaternionSerializer extends DynamicSerializer {

    @Override
    public Quaternion readObject(ByteBuffer data, Class c) throws IOException {
        Quaternion q = new Quaternion(
                data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
        return q;
       
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Quaternion q = (Quaternion) object;
        buffer.putFloat(q.getX());
        buffer.putFloat(q.getY());
        buffer.putFloat(q.getZ());
        buffer.putFloat(q.getW());
        
    }

}
