package com.jme3.light;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrientedBoxProbeAreaTest {
    @Test
    void defaultAreaPublishesUnitExtentsWithoutNeedingAnotherSetter() {
        Matrix4f matrix = new OrientedBoxProbeArea().getUniformMatrix();
        assertEquals(1f, matrix.m00);
        assertEquals(1f, matrix.m11);
        assertEquals(1f, matrix.m22);
        assertEquals(1f, matrix.m30);
        assertEquals(1f, matrix.m31);
        assertEquals(1f, matrix.m32);
    }

    @Test
    void changingRadiusUpdatesTheSameUniformMatrixAndPreservesTheCenter() {
        OrientedBoxProbeArea area = new OrientedBoxProbeArea();
        area.setCenter(new Vector3f(2, 3, 4));
        Matrix4f matrix = area.getUniformMatrix();
        area.setRadius(7);
        assertSame(matrix, area.getUniformMatrix());
        assertEquals(7f, matrix.m30);
        assertEquals(7f, matrix.m31);
        assertEquals(7f, matrix.m32);
        assertEquals(2f, matrix.m03);
        assertEquals(3f, matrix.m13);
        assertEquals(4f, matrix.m23);
    }
}
