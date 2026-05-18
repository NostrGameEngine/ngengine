package org.ngengine.network.protocol.serializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.ngengine.network.protocol.GrowableByteBuffer;

public class NumberSerializerTest {

    private final NumberSerializer serializer = new NumberSerializer();

    @Test
    public void roundTripIntegralTypesWithVarInt() throws Exception {
        GrowableByteBuffer out = new GrowableByteBuffer(ByteBuffer.allocate(64), 64);
        serializer.writeObject(out, (short) -65);
        serializer.writeObject(out, 127);
        serializer.writeObject(out, 9_000_000_000L);

        ByteBuffer in = out.getBuffer();
        in.flip();

        assertEquals((short) -65, serializer.readObject(in, short.class).shortValue());
        assertEquals(127, serializer.readObject(in, int.class).intValue());
        assertEquals(9_000_000_000L, serializer.readObject(in, long.class).longValue());
    }

    @Test
    public void roundTripFloatingTypesFixedWidth() throws Exception {
        GrowableByteBuffer out = new GrowableByteBuffer(ByteBuffer.allocate(32), 32);
        serializer.writeObject(out, 1.25f);
        serializer.writeObject(out, -9.5d);

        ByteBuffer in = out.getBuffer();
        in.flip();

        assertEquals(1.25f, serializer.readObject(in, float.class).floatValue(), 0.00001f);
        assertEquals(-9.5d, serializer.readObject(in, double.class).doubleValue(), 0.0000001d);
    }

    @Test
    public void intEncodingIsSmallerForSmallValues() throws Exception {
        GrowableByteBuffer out = new GrowableByteBuffer(ByteBuffer.allocate(16), 16);
        serializer.writeObject(out, 1);
        int written = out.position();

        assertTrue("Expected varint-compressed int to use fewer than 4 bytes", written < 4);
    }

    @Test
    public void rejectOutOfRangeForShort() throws Exception {
        GrowableByteBuffer out = new GrowableByteBuffer(ByteBuffer.allocate(16), 16);
        serializer.writeObject(out, 40000);
        ByteBuffer in = out.getBuffer();
        in.flip();

        try {
            serializer.readObject(in, short.class);
            fail("Expected IOException for short overflow");
        } catch (java.io.IOException ex) {
            assertTrue(ex.getMessage().contains("short out of range"));
        }
    }
}
