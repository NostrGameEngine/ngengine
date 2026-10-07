/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 */
package jme3tools.optimize;

import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryBatchFactoryTest {

    @Test
    void mergeStandardMeshesIgnoresCustomTypeSentinel() {
        Geometry first = new Geometry("first", new Box(1f, 1f, 1f));
        Geometry second = new Geometry("second", new Box(1f, 1f, 1f));
        second.setLocalTranslation(3f, 0f, 0f);
        Mesh merged = new Mesh();

        GeometryBatchFactory.mergeGeometries(Arrays.asList(first, second), merged);

        assertEquals(first.getVertexCount() + second.getVertexCount(), merged.getVertexCount());
        assertEquals(first.getTriangleCount() + second.getTriangleCount(), merged.getTriangleCount());
    }
}
