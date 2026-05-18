package org.ngengine.network.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Test;

public class VarIntTest {

    @Test
    public void signedRoundTripEdgeValues() {
        long[] values = new long[] {
            0L,
            1L,
            -1L,
            63L,
            -64L,
            64L,
            -65L,
            127L,
            -128L,
            128L,
            -129L,
            Long.MAX_VALUE,
            Long.MIN_VALUE
        };

        for (long value : values) {
            byte[] encoded = VarInt.encodeSigned(value);
            long decoded = VarInt.decodeSigned(encoded);
            assertEquals("Round-trip mismatch for value " + value, value, decoded);
        }
    }

    @Test
    public void signedRoundTripRandomValues() {
        Random random = new Random(0x4E4745564152494EL);
        for (int i = 0; i < 100_000; i++) {
            long value = random.nextLong();
            byte[] encoded = VarInt.encodeSigned(value);
            long decoded = VarInt.decodeSigned(encoded);
            assertEquals("Round-trip mismatch at index " + i, value, decoded);
        }
    }

    @Test
    public void signedCanonicalByteLengthsForKnownValues() {
        assertEquals(1, VarInt.encodeSigned(0L).length);
        assertEquals(1, VarInt.encodeSigned(1L).length);
        assertEquals(1, VarInt.encodeSigned(-1L).length);
        assertEquals(1, VarInt.encodeSigned(63L).length);
        assertEquals(1, VarInt.encodeSigned(-64L).length);
        assertEquals(2, VarInt.encodeSigned(64L).length);
        assertEquals(2, VarInt.encodeSigned(-65L).length);
        assertEquals(10, VarInt.encodeSigned(Long.MAX_VALUE).length);
        assertEquals(10, VarInt.encodeSigned(Long.MIN_VALUE).length);
    }

    @Test
    public void signedKnownEncodingSamples() {
        assertArrayEquals(new byte[] { 0x00 }, VarInt.encodeSigned(0L));
        assertArrayEquals(new byte[] { 0x01 }, VarInt.encodeSigned(-1L));
        assertArrayEquals(new byte[] { 0x02 }, VarInt.encodeSigned(1L));
        assertArrayEquals(new byte[] { (byte) 0x80, 0x01 }, VarInt.encodeSigned(64L));
    }

    @Test
    public void rejectIncompleteSignedEncoding() {
        try {
            VarInt.decodeSigned(new byte[] { (byte) 0x80 });
            fail("Expected IllegalArgumentException for incomplete varlong");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Incomplete"));
        }
    }

    @Test
    public void rejectTooLongSignedEncoding() {
        byte[] tooLong = new byte[] {
            (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
            (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80
        };
        try {
            VarInt.decodeSigned(tooLong);
            fail("Expected IllegalArgumentException for too-long varlong");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("too long"));
        }
    }

    @Test
    public void decodeSignedFromByteBufferConsumesPosition() {
        byte[] a = VarInt.encodeSigned(300L);
        byte[] b = VarInt.encodeSigned(-7L);
        ByteBuffer bb = ByteBuffer.allocate(a.length + b.length);
        bb.put(a);
        bb.put(b);
        bb.flip();

        long first = VarInt.decodeSigned(bb);
        assertEquals(300L, first);
        assertEquals(a.length, bb.position());

        long second = VarInt.decodeSigned(bb);
        assertEquals(-7L, second);
        assertEquals(a.length + b.length, bb.position());
    }

    @Test
    public void decodeSignedFromGrowableBufferConsumesPosition() {
        byte[] encoded = VarInt.encodeSigned(-123_456L);
        GrowableByteBuffer gb = new GrowableByteBuffer(ByteBuffer.allocate(encoded.length), 16);
        gb.put(encoded);
        gb.flip();

        long decoded = VarInt.decodeSigned(gb);
        assertEquals(-123_456L, decoded);
        assertEquals(encoded.length, gb.position());
    }

    @Test
    public void encodeSignedWritesToByteBufferWithoutExtraAllocation() {
        ByteBuffer out = ByteBuffer.allocate(16);
        VarInt.encodeSigned(-321L, out);
        int written = out.position();

        out.flip();
        byte[] bytes = new byte[written];
        out.get(bytes);
        assertEquals(-321L, VarInt.decodeSigned(bytes));
    }

    @Test
    public void unsignedRoundTrip() {
        long[] values = new long[] {
            0L,
            1L,
            63L,
            64L,
            127L,
            128L,
            255L,
            16_384L,
            Integer.MAX_VALUE,
            Long.MAX_VALUE
        };

        for (long value : values) {
            byte[] encoded = VarInt.encodeUnsigned(value);
            assertEquals("Unsigned array round-trip mismatch for value " + value, value, VarInt.decodeUnsigned(encoded));

            ByteBuffer bb = ByteBuffer.wrap(encoded);
            assertEquals("Unsigned bytebuffer round-trip mismatch for value " + value, value, VarInt.decodeUnsigned(bb));
            assertEquals(encoded.length, bb.position());

            GrowableByteBuffer out = new GrowableByteBuffer(ByteBuffer.allocate(16), 16);
            VarInt.encodeUnsigned(value, out);
            ByteBuffer data = out.getBuffer();
            data.flip();
            assertEquals("Unsigned growable round-trip mismatch for value " + value, value, VarInt.decodeUnsigned(data));
        }
    }

    @Test
    public void encodeUnsignedRejectsNegativeValues() {
        try {
            VarInt.encodeUnsigned(-1L);
            fail("Expected IllegalArgumentException for negative unsigned varint");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("cannot be negative"));
        }

        GrowableByteBuffer out = new GrowableByteBuffer(ByteBuffer.allocate(16), 16);
        try {
            VarInt.encodeUnsigned(-1L, out);
            fail("Expected IllegalArgumentException for negative unsigned varint");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("cannot be negative"));
        }
    }
}
