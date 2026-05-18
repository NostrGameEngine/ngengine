package org.ngengine.network.quantization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import org.junit.Test;

public class TestTransformQuantizer {

    private static final TransformQuantizer QUANTIZER = new TransformQuantizer(
        new Vector3f(-512f, -16f, -512f),
        new Vector3f(1024f, 64f, 1024f),
        0.02f
    );

    @Test
    public void positionRoundTripStaysWithinConfiguredError() {
        Vector3f original = new Vector3f(123.456f, 1.234f, -98.765f);
        long packed = QUANTIZER.quantizePosition(original);
        Vector3f decoded = QUANTIZER.dequantizePosition(packed);

        assertTrue(Math.abs(original.x - decoded.x) <= QUANTIZER.getActualMaxPositionErrorX());
        assertTrue(Math.abs(original.y - decoded.y) <= QUANTIZER.getActualMaxPositionErrorY());
        assertTrue(Math.abs(original.z - decoded.z) <= QUANTIZER.getActualMaxPositionErrorZ());
    }

    @Test
    public void rotationRoundTripKeepsHighSimilarity() {
        Quaternion original = new Quaternion().fromAngles(0.15f, 1.4f, -0.5f);
        long packed = QUANTIZER.quantizeRotation(original);
        Quaternion decoded = QUANTIZER.dequantizeRotation(packed);

        float similarity = Math.abs(original.clone().normalizeLocal().dot(decoded.clone().normalizeLocal()));
        assertTrue("rotation similarity too low: " + similarity, similarity > 0.999f);
    }

    @Test
    public void specialScaleModesAreDetected() {
        long one = QUANTIZER.quantizeScale(new Vector3f(1f, 1f, 1f));
        long half = QUANTIZER.quantizeScale(new Vector3f(0.5f, 0.5f, 0.5f));

        assertEquals(TransformQuantizer.TYPE_SCALE, QUANTIZER.getType(one));
        assertEquals(TransformQuantizer.TYPE_SCALE, QUANTIZER.getType(half));
        assertEquals(0, QUANTIZER.getScaleMode(one));
        assertEquals(1, QUANTIZER.getScaleMode(half));
    }

    @Test
    public void transformRoundTripWorks() {
        Transform original = new Transform(
            new Vector3f(45.12f, 2.5f, -301.8f),
            new Quaternion().fromAngles(0f, 0.9f, 0f),
            new Vector3f(1f, 1f, 1f)
        );

        long[] packed = QUANTIZER.quantizeTransform(original);
        Transform decoded = QUANTIZER.dequantizeTransform(packed);

        assertTrue(original.getTranslation().distance(decoded.getTranslation()) <= 0.05f);
        float similarity = Math.abs(
            original.getRotation().clone().normalizeLocal().dot(decoded.getRotation().clone().normalizeLocal())
        );
        assertTrue(similarity > 0.999f);
        assertEquals(1f, decoded.getScale().x, 1e-4f);
        assertEquals(1f, decoded.getScale().y, 1e-4f);
        assertEquals(1f, decoded.getScale().z, 1e-4f);
    }
}
