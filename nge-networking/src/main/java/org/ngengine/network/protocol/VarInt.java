package org.ngengine.network.protocol;

import java.nio.ByteBuffer;

public final class VarInt {

    private VarInt() {
    }

    @FunctionalInterface
    private interface ByteSink {
        void put(byte value);
    }

    private static long zigZagEncode(long value) {
        return (value << 1) ^ (value >> 63);
    }

    private static long zigZagDecode(long value) {
        return (value >>> 1) ^ -(value & 1);
    }

    public static byte[] encodeSigned(long value) {
        return encodeUnsignedInternalToArray(zigZagEncode(value));
    }

    public static void encodeSigned(long value, ByteBuffer output) {
        writeUnsignedInternal(zigZagEncode(value), output::put);
    }

    public static void encodeSigned(long value, GrowableByteBuffer output) {
        writeUnsignedInternal(zigZagEncode(value), output::put);
    }

    public static long decodeSigned(byte[] bytes) {
        return zigZagDecode(decodeUnsignedInternal(ByteBuffer.wrap(bytes)));
    }

    public static long decodeSigned(ByteBuffer buffer) {
        return zigZagDecode(decodeUnsignedInternal(buffer));
    }

    public static long decodeSigned(GrowableByteBuffer buffer) {
        return decodeSigned(buffer.getBuffer());
    }

    public static byte[] encodeUnsigned(long value) {
        return encodeUnsignedInternalToArray(requireUnsigned(value));
    }

    public static void encodeUnsigned(long value, ByteBuffer output) {
        writeUnsignedInternal(requireUnsigned(value), output::put);
    }

    public static void encodeUnsigned(long value, GrowableByteBuffer output) {
        writeUnsignedInternal(requireUnsigned(value), output::put);
    }

    public static long decodeUnsigned(byte[] bytes) {
        return decodeUnsignedInternal(ByteBuffer.wrap(bytes));
    }

    public static long decodeUnsigned(ByteBuffer buffer) {
        return decodeUnsignedInternal(buffer);
    }

    public static long decodeUnsigned(GrowableByteBuffer buffer) {
        return decodeUnsigned(buffer.getBuffer());
    }

    private static long requireUnsigned(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Unsigned VarLong cannot be negative: " + value);
        }
        return value;
    }

    private static int unsignedLength(long value) {
        int len = 1;
        long v = value;
        while ((v & ~0x7FL) != 0) {
            len++;
            v >>>= 7;
        }
        return len;
    }

    private static byte[] encodeUnsignedInternalToArray(long value) {
        int length = unsignedLength(value);
        byte[] out = new byte[length];
        ByteBuffer output = ByteBuffer.wrap(out);
        writeUnsignedInternal(value, output::put);
        return out;
    }

    private static void writeUnsignedInternal(long value, ByteSink sink) {
        long v = value;
        while ((v & ~0x7FL) != 0) {
            sink.put((byte) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        sink.put((byte) v);
    }

    private static long decodeUnsignedInternal(ByteBuffer buffer) {
        long result = 0;
        int shift = 0;

        while (true) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("Incomplete VarLong");
            }

            int b = buffer.get() & 0xFF;
            result |= (long) (b & 0x7F) << shift;

            if ((b & 0x80) == 0) {
                return result;
            }

            shift += 7;
            if (shift > 63) {
                throw new IllegalArgumentException("VarLong too long");
            }
        }
    }
}
