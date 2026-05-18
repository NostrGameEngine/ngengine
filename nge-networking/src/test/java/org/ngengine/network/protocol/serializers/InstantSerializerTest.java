package org.ngengine.network.protocol.serializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;

import org.junit.Test;
import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.VarInt;

public class InstantSerializerTest {

    private static final long BITCOIN_GENESIS_EPOCH_MILLIS = 1231006505000L;
    private static final long CHUNK_MILLIS = 90L * 24L * 60L * 60L * 1000L;

    private final InstantSerializer serializer = new InstantSerializer();

    @Test
    public void testRoundTripSpecialAbsoluteAndCompactValues() throws IOException {
        assertRoundTrip(0L);
        assertRoundTrip(-1L);
        assertRoundTrip(-1234567890123L);
        assertRoundTrip(1L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS - 1L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + 1L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + 1234567L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + CHUNK_MILLIS - 1L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + CHUNK_MILLIS);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + CHUNK_MILLIS + 42L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + 17L * CHUNK_MILLIS + 123456789L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + 255L * CHUNK_MILLIS + 999L);
        assertRoundTrip(BITCOIN_GENESIS_EPOCH_MILLIS + 500L * CHUNK_MILLIS + 987654321L);
    }

    @Test
    public void testZeroUsesDedicatedMode() throws IOException {
        byte[] encoded = encode(Instant.ofEpochMilli(0L));
        assertEquals(1, encoded.length);
        assertEquals(0x00, encoded[0] & 0xFF);
    }

    @Test
    public void testPreGenesisUsesAbsoluteMode() throws IOException {
        byte[] encoded = encode(Instant.ofEpochMilli(BITCOIN_GENESIS_EPOCH_MILLIS - 1L));
        assertTrue(encoded.length >= 2);
        assertEquals(0x01, encoded[0] & 0xFF);
    }

    @Test
    public void testNegativeUsesAbsoluteMode() throws IOException {
        byte[] encoded = encode(Instant.ofEpochMilli(-123L));
        assertTrue(encoded.length >= 2);
        assertEquals(0x01, encoded[0] & 0xFF);
    }

    @Test
    public void testGenesisUsesCompactMode() throws IOException {
        byte[] encoded = encode(Instant.ofEpochMilli(BITCOIN_GENESIS_EPOCH_MILLIS));
        assertTrue(encoded.length >= 3);
        assertEquals(0x02, encoded[0] & 0xFF);
    }

    @Test
    public void testUnknownModeThrows() {
        ByteBuffer data = ByteBuffer.wrap(new byte[] { (byte) 0x7F });

        try {
            serializer.readObject(data, Instant.class);
            fail("Expected IOException for unknown mode");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unknown Instant encoding mode"));
        }
    }

    @Test
    public void testInvalidCompactRemainderThrows() {
        GrowableByteBuffer gb = new GrowableByteBuffer( ByteBuffer.allocate(16), 16);
        gb.put((byte) 0x02);

        VarInt.encodeUnsigned(0L, gb);
        VarInt.encodeUnsigned(CHUNK_MILLIS, gb);

        ByteBuffer data = ByteBuffer.wrap(toByteArray(gb));

        try {
            serializer.readObject(data, Instant.class);
            fail("Expected IOException for invalid compact remainder");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Invalid compact Instant remainder"));
        }
    }

    @Test
    public void testRepresentativeValuesAreLossless() throws IOException {
        long[] millis = new long[] {
                0L,
                -1L,
                -1000L,
                -86400000L,
                1L,
                999L,
                1000L,
                BITCOIN_GENESIS_EPOCH_MILLIS - 1000L,
                BITCOIN_GENESIS_EPOCH_MILLIS - 1L,
                BITCOIN_GENESIS_EPOCH_MILLIS,
                BITCOIN_GENESIS_EPOCH_MILLIS + 1L,
                BITCOIN_GENESIS_EPOCH_MILLIS + CHUNK_MILLIS / 2L,
                BITCOIN_GENESIS_EPOCH_MILLIS + CHUNK_MILLIS - 1L,
                BITCOIN_GENESIS_EPOCH_MILLIS + CHUNK_MILLIS,
                BITCOIN_GENESIS_EPOCH_MILLIS + 2L * CHUNK_MILLIS + 12345L,
                BITCOIN_GENESIS_EPOCH_MILLIS + 63L * 365L * 24L * 60L * 60L * 1000L,
                Long.MAX_VALUE / 4L,
                Long.MIN_VALUE / 4L
        };

        for (long epochMilli : millis) {
            Instant original = Instant.ofEpochMilli(epochMilli);
            Instant decoded = roundTrip(original);
            assertEquals("Mismatch for epochMilli=" + epochMilli,
                    original.toEpochMilli(), decoded.toEpochMilli());
        }
    }

    private void assertRoundTrip(long epochMilli) throws IOException {
        Instant original = Instant.ofEpochMilli(epochMilli);
        Instant decoded = roundTrip(original);
        assertEquals("Roundtrip mismatch for epochMilli=" + epochMilli,
                original.toEpochMilli(), decoded.toEpochMilli());
    }

    private Instant roundTrip(Instant instant) throws IOException {
        byte[] encoded = encode(instant);
        return serializer.readObject(ByteBuffer.wrap(encoded), Instant.class);
    }

    private byte[] encode(Instant instant) throws IOException {
        GrowableByteBuffer gb = new GrowableByteBuffer(ByteBuffer.allocate(16), 16);
        serializer.writeObject(gb, instant);
        return toByteArray(gb);
    }

    private byte[] toByteArray(GrowableByteBuffer gb) {
        ByteBuffer bb = gb.getBuffer();
        ByteBuffer copy = bb.duplicate();
        copy.flip();

        byte[] out = new byte[copy.remaining()];
        copy.get(out);
        return out;
    }
}