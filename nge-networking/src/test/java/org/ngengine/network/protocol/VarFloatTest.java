package org.ngengine.network.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;

public class VarFloatTest {

    @Test
    public void floatRoundTripRepresentativeValues() {
        float[] values = new float[] {
            0f,
            -0f,
            1f,
            -1f,
            1.25f,
            -123.5f,
            9999999f,
            Float.MIN_NORMAL,
            Float.MAX_VALUE
        };

        for (float value : values) {
            ByteBuffer out = ByteBuffer.allocate(32);
            VarFloat.encodeFloat(value, out);
            out.flip();
            float decoded = VarFloat.decodeFloat(out);
            assertEquals("Float round-trip mismatch for value=" + value, value == 0f ? 0f : value, decoded, 0f);
        }
    }

    @Test
    public void doubleRoundTripRepresentativeValues() {
        double[] values = new double[] {
            0d,
            -0d,
            1d,
            -1d,
            1.25d,
            -123.5d,
            9_999_999_999d,
            Double.MIN_NORMAL,
            Double.MAX_VALUE
        };

        for (double value : values) {
            ByteBuffer out = ByteBuffer.allocate(32);
            VarFloat.encodeDouble(value, out);
            out.flip();
            double decoded = VarFloat.decodeDouble(out);
            assertEquals("Double round-trip mismatch for value=" + value, value == 0d ? 0d : value, decoded, 0d);
        }
    }

    @Test
    public void specialValuesRoundTrip() {
        ByteBuffer fb = ByteBuffer.allocate(32);
        VarFloat.encodeFloat(Float.NaN, fb);
        fb.flip();
        assertTrue(Float.isNaN(VarFloat.decodeFloat(fb)));

        ByteBuffer fi = ByteBuffer.allocate(32);
        VarFloat.encodeFloat(Float.POSITIVE_INFINITY, fi);
        fi.flip();
        assertEquals(Float.POSITIVE_INFINITY, VarFloat.decodeFloat(fi), 0f);

        ByteBuffer db = ByteBuffer.allocate(32);
        VarFloat.encodeDouble(Double.NaN, db);
        db.flip();
        assertTrue(Double.isNaN(VarFloat.decodeDouble(db)));

        ByteBuffer di = ByteBuffer.allocate(32);
        VarFloat.encodeDouble(Double.NEGATIVE_INFINITY, di);
        di.flip();
        assertEquals(Double.NEGATIVE_INFINITY, VarFloat.decodeDouble(di), 0d);
    }

    @Test
    public void negativeZeroIsNormalized() {
        ByteBuffer fb = ByteBuffer.allocate(16);
        VarFloat.encodeFloat(-0f, fb);
        fb.flip();
        float f = VarFloat.decodeFloat(fb);
        assertEquals(Float.floatToRawIntBits(0f), Float.floatToRawIntBits(f));

        ByteBuffer db = ByteBuffer.allocate(16);
        VarFloat.encodeDouble(-0d, db);
        db.flip();
        double d = VarFloat.decodeDouble(db);
        assertEquals(Double.doubleToRawLongBits(0d), Double.doubleToRawLongBits(d));
    }

    @Test
    public void decodeFromGrowableBufferConsumesPosition() {
        GrowableByteBuffer gb = new GrowableByteBuffer(ByteBuffer.allocate(64), 64);
        VarFloat.encodeFloat(12.5f, gb);
        VarFloat.encodeDouble(-42.25d, gb);
        gb.flip();

        int before = gb.position();
        float fv = VarFloat.decodeFloat(gb);
        int afterFloat = gb.position();
        double dv = VarFloat.decodeDouble(gb);
        int afterDouble = gb.position();

        assertEquals(12.5f, fv, 0f);
        assertEquals(-42.25d, dv, 0d);
        assertTrue(afterFloat > before);
        assertTrue(afterDouble > afterFloat);
    }

    @Test
    public void integerAndDecimalEncodingCanBeSmallerThanRaw() {
        ByteBuffer intFloat = ByteBuffer.allocate(16);
        VarFloat.encodeFloat(5f, intFloat);
        assertTrue(intFloat.position() < Float.BYTES);

        ByteBuffer decimalFloat = ByteBuffer.allocate(16);
        VarFloat.encodeFloat(1.25f, decimalFloat);
        assertTrue(decimalFloat.position() < Float.BYTES);

        ByteBuffer intDouble = ByteBuffer.allocate(16);
        VarFloat.encodeDouble(5d, intDouble);
        assertTrue(intDouble.position() < Double.BYTES);
    }
}
