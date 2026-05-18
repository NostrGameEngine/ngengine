package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.VarInt;

/**
 * Serializer for {@link BigInteger}.
 */
@SuppressWarnings("unchecked")
public class BigIntegerSerializer extends DynamicSerializer {

    @Override
    public BigInteger readObject(ByteBuffer data, Class c) throws IOException {
        long len = VarInt.decodeUnsigned(data);
        if (len > Integer.MAX_VALUE) {
            throw new IOException("Invalid BigInteger byte length: " + len);
        }
        int length = (int) len;
        if (length > data.remaining()) {
            throw new IOException("Invalid BigInteger byte length: " + length);
        }
        byte[] bytes = new byte[length];
        data.get(bytes);
        return new BigInteger(bytes);
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        if (!(object instanceof BigInteger)) {
            throw new IOException("Unsupported BigInteger value type: " + object.getClass());
        }
        byte[] bytes = ((BigInteger) object).toByteArray();
        VarInt.encodeUnsigned(bytes.length, buffer);
        buffer.put(bytes);
    }
}
