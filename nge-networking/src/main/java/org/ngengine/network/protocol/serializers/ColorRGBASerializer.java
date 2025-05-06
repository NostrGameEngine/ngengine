package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.math.ColorRGBA;


public class ColorRGBASerializer extends DynamicSerializer {

    @Override
    public ColorRGBA readObject(ByteBuffer data, Class c) throws IOException {
        return new ColorRGBA(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        ColorRGBA vector = (ColorRGBA) object;
        buffer.putFloat(vector.getRed());
        buffer.putFloat(vector.getGreen());
        buffer.putFloat(vector.getBlue());
        buffer.putFloat(vector.getAlpha());

    }

    
}
