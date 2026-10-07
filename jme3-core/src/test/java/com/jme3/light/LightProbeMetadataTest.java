package com.jme3.light;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LightProbeMetadataTest {
    @Test
    void sphericalMetadataDecodesSmallNormalAndLargeRadii() {
        LightProbe probe = new LightProbe();
        probe.setNbMipMaps(6);
        probe.setPosition(new Vector3f(2, 3, 4));
        for (float radius : new float[]{.5f, .8f, 1f, Math.nextUp(1f), 2f, 3f, 10_000_000f}) {
            probe.getArea().setRadius(radius);
            Matrix4f data = probe.getUniformMatrix();
            float inverseRadius = data.m30 == 0 && data.m32 < 0
                    ? data.m31 : data.m33 - (float) Math.floor(data.m33);
            assertEquals(6f, (float) Math.floor(data.m33), "Mip count for radius " + radius);
            assertEquals(1f / radius, inverseRadius, Math.max(1e-12f, 1e-6f / radius));
            assertEquals(2f, data.m03);
            assertEquals(3f, data.m13);
            assertEquals(4f, data.m23);
            assertEquals(0f, data.m30);
        }
    }

    @Test
    void returningToALegacyRadiusClearsTheAlternateLayout() {
        LightProbe probe = new LightProbe();
        probe.setNbMipMaps(6);
        probe.getArea().setRadius(.5f);
        Matrix4f data = probe.getUniformMatrix();
        assertEquals(-1f, data.m32);
        probe.getArea().setRadius(2f);
        assertSame(data, probe.getUniformMatrix());
        assertEquals(6.5f, data.m33);
        assertEquals(0f, data.m31);
        assertEquals(0f, data.m32);
    }

    @Test
    void boxMetadataPreservesItsExtentsWithoutCorruptingMipCount() {
        LightProbe probe = new LightProbe();
        probe.setAreaType(LightProbe.AreaType.OrientedBox);
        probe.setNbMipMaps(6);
        for (float radius : new float[]{.5f, 1f, 2f}) {
            probe.getArea().setRadius(radius);
            Matrix4f data = probe.getUniformMatrix();
            assertEquals(6f, (float) Math.floor(data.m33));
            assertEquals(radius, data.m30);
            assertEquals(radius, data.m31);
            assertEquals(radius, data.m32);
        }
    }
}
