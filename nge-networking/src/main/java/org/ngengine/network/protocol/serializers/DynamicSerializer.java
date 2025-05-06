package org.ngengine.network.protocol.serializers;


import java.io.IOException;

import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

import com.jme3.network.serializing.Serializer;

public abstract class DynamicSerializer extends Serializer {

 

    @Override
    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        writeObject(new GrowableByteBuffer(buffer,0), object);
    }
   
     public  abstract void writeObject(GrowableByteBuffer buffer, Object object) throws IOException;
}
