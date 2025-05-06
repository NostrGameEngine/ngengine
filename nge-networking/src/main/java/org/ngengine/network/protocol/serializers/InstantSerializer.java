package org.ngengine.network.protocol.serializers;

import java.time.Instant;

public class InstantSerializer extends DynamicSerializer {

    @Override
    public  Instant readObject(java.nio.ByteBuffer data, Class c) throws java.io.IOException {
        return Instant.ofEpochMilli(data.getLong());
    }

    @Override
    public void writeObject(org.ngengine.network.protocol.GrowableByteBuffer buffer, Object object) throws java.io.IOException {
        buffer.putLong(((Instant) object).toEpochMilli());
    }
    
}
