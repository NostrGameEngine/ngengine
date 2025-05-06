package org.ngengine.network.protocol.serializers;

public class DurationSerializer extends DynamicSerializer {

    @Override
    public java.time.Duration readObject(java.nio.ByteBuffer data, Class c) throws java.io.IOException {
        return java.time.Duration.ofNanos(data.getLong());
    }

    @Override
    public void writeObject(org.ngengine.network.protocol.GrowableByteBuffer buffer, Object object) throws java.io.IOException {
        buffer.putLong(((java.time.Duration) object).toNanos());
    }
    
}
