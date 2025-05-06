package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import org.ngengine.network.protocol.GrowableByteBuffer;

public class DateSerializer extends DynamicSerializer {

    @Override
    public Date readObject(ByteBuffer data, Class c) throws IOException {
        return new Date(data.getLong());
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        buffer.putLong(((Date) object).getTime());
    }
}
