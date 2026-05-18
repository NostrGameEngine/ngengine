package org.ngengine.network.protocol;


import java.nio.ByteBuffer;

public final class VarFloat {

    private VarFloat() {
    }

    /**
     * Header type bits stored in the two least significant bits
     * of the first unsigned VarInt.
     */
    private static final int TYPE_INTEGER = 0;
    private static final int TYPE_DECIMAL = 1;
    private static final int TYPE_RAW_FLOAT = 2;
    private static final int TYPE_RAW_DOUBLE = 3;

    /**
     * Maximum decimal scale attempted for compact decimal encoding.
     * Higher values improve compression opportunities but increase encoding cost.
     */
    private static final int MAX_DECIMAL_SCALE = 9;

    private static final double[] POW10_DOUBLE = {
            1.0d,
            10.0d,
            100.0d,
            1000.0d,
            10000.0d,
            100000.0d,
            1000000.0d,
            10000000.0d,
            100000000.0d,
            1000000000.0d
    };

    private static final float[] POW10_FLOAT = {
            1.0f,
            10.0f,
            100.0f,
            1000.0f,
            10000.0f,
            100000.0f,
            1000000.0f,
            10000000.0f,
            100000000.0f,
            1000000000.0f
    };

 
    public static void encodeDouble(double value, ByteBuffer output) {
        writeDoubleInternal(normalizeZero(value), output);
    }

    public static void encodeDouble(double value, GrowableByteBuffer output) {
        writeDoubleInternal(normalizeZero(value), output);
    }

    public static double decodeDouble(byte[] bytes) {
        return decodeDouble(ByteBuffer.wrap(bytes));
    }

    public static double decodeDouble(ByteBuffer buffer) {
        long header = VarInt.decodeUnsigned(buffer);
        int type = headerType(header);
        long payload = headerPayload(header);

        switch (type) {
            case TYPE_INTEGER:
                return normalizeZero((double) zigZagDecode(payload));

            case TYPE_DECIMAL: {
                int scale = (int) payload;
                if (scale < 0 || scale >= POW10_DOUBLE.length) {
                    throw new IllegalArgumentException("Invalid decimal scale: " + scale);
                }
                long mantissa = VarInt.decodeSigned(buffer);
                return normalizeZero(mantissa / POW10_DOUBLE[scale]);
            }

            case TYPE_RAW_DOUBLE:
                return normalizeZero(buffer.getDouble());

            default:
                throw new IllegalArgumentException("Unknown VarFloat header type for double: " + type);
        }
    }

    public static double decodeDouble(GrowableByteBuffer buffer) {
        return decodeDouble(buffer.getBuffer());
    }

 
    public static void encodeFloat(float value, ByteBuffer output) {
        writeFloatInternal(normalizeZero(value), output);
    }

    public static void encodeFloat(float value, GrowableByteBuffer output) {
        writeFloatInternal(normalizeZero(value), output);
    }

    public static float decodeFloat(byte[] bytes) {
        return decodeFloat(ByteBuffer.wrap(bytes));
    }

    public static float decodeFloat(ByteBuffer buffer) {
        long header = VarInt.decodeUnsigned(buffer);
        int type = headerType(header);
        long payload = headerPayload(header);

        switch (type) {
            case TYPE_INTEGER:
                return normalizeZero((float) zigZagDecode(payload));

            case TYPE_DECIMAL: {
                int scale = (int) payload;
                if (scale < 0 || scale >= POW10_FLOAT.length) {
                    throw new IllegalArgumentException("Invalid decimal scale: " + scale);
                }
                long mantissa = VarInt.decodeSigned(buffer);
                return normalizeZero(mantissa / POW10_FLOAT[scale]);
            }

            case TYPE_RAW_FLOAT:
                return normalizeZero(buffer.getFloat());

            default:
                throw new IllegalArgumentException("Unknown VarFloat header type for float: " + type);
        }
    }

    public static float decodeFloat(GrowableByteBuffer buffer) {
        return decodeFloat(buffer.getBuffer());
    }

    private static void writeDoubleInternal(double value, ByteBuffer output) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            VarInt.encodeUnsigned(makeHeader(TYPE_RAW_DOUBLE, 0), output);
            output.putDouble(value);
            return;
        }

        if (isExactLong(value)) {
            long encoded = zigZagEncode((long) value);
            VarInt.encodeUnsigned(makeHeader(TYPE_INTEGER, encoded), output);
            return;
        }

        for (int scale = 1; scale <= MAX_DECIMAL_SCALE; scale++) {
            double factor = POW10_DOUBLE[scale];
            double scaled = value * factor;

            if (!isExactLong(scaled)) {
                continue;
            }

            long mantissa = (long) scaled;
            double reconstructed = mantissa / factor;

            if (sameNormalizedDouble(value, reconstructed)) {
                VarInt.encodeUnsigned(makeHeader(TYPE_DECIMAL, scale), output);
                VarInt.encodeSigned(mantissa, output);
                return;
            }
        }

        VarInt.encodeUnsigned(makeHeader(TYPE_RAW_DOUBLE, 0), output);
        output.putDouble(value);
    }

    private static void writeDoubleInternal(double value, GrowableByteBuffer output) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            VarInt.encodeUnsigned(makeHeader(TYPE_RAW_DOUBLE, 0), output);
            output.putDouble(value);
            return;
        }

        if (isExactLong(value)) {
            long encoded = zigZagEncode((long) value);
            VarInt.encodeUnsigned(makeHeader(TYPE_INTEGER, encoded), output);
            return;
        }

        for (int scale = 1; scale <= MAX_DECIMAL_SCALE; scale++) {
            double factor = POW10_DOUBLE[scale];
            double scaled = value * factor;

            if (!isExactLong(scaled)) {
                continue;
            }

            long mantissa = (long) scaled;
            double reconstructed = mantissa / factor;

            if (sameNormalizedDouble(value, reconstructed)) {
                VarInt.encodeUnsigned(makeHeader(TYPE_DECIMAL, scale), output);
                VarInt.encodeSigned(mantissa, output);
                return;
            }
        }

        VarInt.encodeUnsigned(makeHeader(TYPE_RAW_DOUBLE, 0), output);
        output.putDouble(value);
    }

    private static void writeFloatInternal(float value, ByteBuffer output) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            VarInt.encodeUnsigned(makeHeader(TYPE_RAW_FLOAT, 0), output);
            output.putFloat(value);
            return;
        }

        if (isExactLong(value)) {
            long encoded = zigZagEncode((long) value);
            VarInt.encodeUnsigned(makeHeader(TYPE_INTEGER, encoded), output);
            return;
        }

        for (int scale = 1; scale <= MAX_DECIMAL_SCALE; scale++) {
            float factor = POW10_FLOAT[scale];
            float scaled = value * factor;

            if (!isExactLong(scaled)) {
                continue;
            }

            long mantissa = (long) scaled;
            float reconstructed = mantissa / factor;

            if (sameNormalizedFloat(value, reconstructed)) {
                VarInt.encodeUnsigned(makeHeader(TYPE_DECIMAL, scale), output);
                VarInt.encodeSigned(mantissa, output);
                return;
            }
        }

        VarInt.encodeUnsigned(makeHeader(TYPE_RAW_FLOAT, 0), output);
        output.putFloat(value);
    }

    private static void writeFloatInternal(float value, GrowableByteBuffer output) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            VarInt.encodeUnsigned(makeHeader(TYPE_RAW_FLOAT, 0), output);
            output.putFloat(value);
            return;
        }

        if (isExactLong(value)) {
            long encoded = zigZagEncode((long) value);
            VarInt.encodeUnsigned(makeHeader(TYPE_INTEGER, encoded), output);
            return;
        }

        for (int scale = 1; scale <= MAX_DECIMAL_SCALE; scale++) {
            float factor = POW10_FLOAT[scale];
            float scaled = value * factor;

            if (!isExactLong(scaled)) {
                continue;
            }

            long mantissa = (long) scaled;
            float reconstructed = mantissa / factor;

            if (sameNormalizedFloat(value, reconstructed)) {
                VarInt.encodeUnsigned(makeHeader(TYPE_DECIMAL, scale), output);
                VarInt.encodeSigned(mantissa, output);
                return;
            }
        }

        VarInt.encodeUnsigned(makeHeader(TYPE_RAW_FLOAT, 0), output);
        output.putFloat(value);
    }

    /**
     * Packs the type into the lowest two bits of the header and stores
     * the payload in the remaining upper bits.
     */
    private static long makeHeader(int type, long payload) {
        if ((type & ~0x3) != 0) {
            throw new IllegalArgumentException("Header type must fit in 2 bits: " + type);
        }
        if (payload < 0) {
            throw new IllegalArgumentException("Header payload cannot be negative: " + payload);
        }
        if (payload > (Long.MAX_VALUE >>> 2)) {
            throw new IllegalArgumentException("Header payload too large: " + payload);
        }
        return (payload << 2) | type;
    }

    private static int headerType(long header) {
        return (int) (header & 0x3L);
    }

    private static long headerPayload(long header) {
        return header >>> 2;
    }

    /**
     * This mirrors the ZigZag mapping used by VarInt for signed values,
     * but is kept local because integer-mode stores the encoded signed value
     * directly inside the header payload.
     */
    private static long zigZagEncode(long value) {
        return (value << 1) ^ (value >> 63);
    }

    /**
     * This mirrors the ZigZag inverse used by VarInt for signed values,
     * but is kept local because integer-mode stores the encoded signed value
     * directly inside the header payload.
     */
    private static long zigZagDecode(long value) {
        return (value >>> 1) ^ -(value & 1L);
    }

    private static boolean isExactLong(double value) {
        if (value < Long.MIN_VALUE || value > Long.MAX_VALUE) {
            return false;
        }
        long l = (long) value;
        return (double) l == value;
    }

    private static boolean isExactLong(float value) {
        if (value < Long.MIN_VALUE || value > Long.MAX_VALUE) {
            return false;
        }
        long l = (long) value;
        return (float) l == value;
    }

    private static boolean sameNormalizedDouble(double a, double b) {
        return Double.doubleToRawLongBits(normalizeZero(a))
                == Double.doubleToRawLongBits(normalizeZero(b));
    }

    private static boolean sameNormalizedFloat(float a, float b) {
        return Float.floatToRawIntBits(normalizeZero(a))
                == Float.floatToRawIntBits(normalizeZero(b));
    }

    private static double normalizeZero(double value) {
        return value == 0.0d ? 0.0d : value;
    }

    private static float normalizeZero(float value) {
        return value == 0.0f ? 0.0f : value;
    }
}