package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.VarInt;
import org.ngengine.network.protocol.DynamicSerializerProtocol;

/**
 * Serializer for {@link BigInteger}.
 */
@SuppressWarnings("unchecked")
public class BigIntegerSerializer extends DynamicSerializer {

    @Override
    public BigInteger readObject(ByteBuffer data, Class c) throws IOException {
        long len = VarInt.decodeUnsigned(data);
        int length = DynamicSerializerProtocol.checkedLength(len, DynamicSerializerProtocol.MAX_STRING_BYTES, data.remaining());
        DynamicSerializerProtocol.chargeDecodedBytes(length);
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
        DynamicSerializerProtocol.checkedLength(bytes.length, DynamicSerializerProtocol.MAX_STRING_BYTES, Integer.MAX_VALUE);
        VarInt.encodeUnsigned(bytes.length, buffer);
        buffer.put(bytes);
    }
}
