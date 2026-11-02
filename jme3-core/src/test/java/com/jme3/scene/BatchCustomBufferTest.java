package com.jme3.scene;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BatchCustomBufferTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void namedAttributesSurviveBatchExtensionAndTransforms(boolean simple) {
        BatchNode node = simple ? new SimpleBatchNode("batch") : new BatchNode("batch");
        Material material = new Material(new MaterialDef(new DesktopAssetManager(), "test"));
        Geometry first = geometry(material, 1);
        FloatBuffer values = (FloatBuffer) first.getMesh().getBuffer("inValues").getData();
        values.position(3).mark();
        ByteBuffer tint = (ByteBuffer) first.getMesh().getBuffer("inTint").getData();
        tint.position(2).mark();
        node.attachChild(first);
        node.batch();

        Geometry second = geometry(material, 20);
        node.attachChild(second);
        node.batch();
        assertEquals(1, node.batches.size());
        first.move(3, 4, 5);
        second.move(-2, 1, 3);
        node.updateGeometricState();

        Mesh merged = node.batches.get(0).getGeometry().getMesh();
        VertexBuffer mergedValues = merged.getBuffer("inValues");
        assertEquals(VertexBuffer.Type.Custom, mergedValues.getBufferType());
        assertEquals("inValues", mergedValues.getAttributeName());
        assertEquals(2, mergedValues.getNumComponents());
        FloatBuffer actual = (FloatBuffer) mergedValues.getData();
        for (int i = 0; i < 8; i++) {
            assertEquals(1 + i, actual.get(i));
            assertEquals(20 + i, actual.get(8 + i));
        }
        VertexBuffer mergedTint = merged.getBuffer("inTint");
        assertTrue(mergedTint.isNormalized());
        assertEquals(VertexBuffer.Format.UnsignedByte, mergedTint.getFormat());
        assertEquals(8, mergedTint.getNumElements());
        ByteBuffer colors = (ByteBuffer) mergedTint.getData();
        for (int i = 0; i < 16; i++) {
            assertEquals((byte) (1 + i), colors.get(i));
            assertEquals((byte) (20 + i), colors.get(16 + i));
        }
        assertEquals(3, values.position());
        assertEquals(8, values.limit());
        assertDoesNotThrow(values::reset);
        assertEquals(2, tint.position());
        assertEquals(16, tint.limit());
        assertDoesNotThrow(tint::reset);

        node.batch();
        assertEquals(1, node.batches.size());
        assertSame(mergedValues, node.batches.get(0).getGeometry().getMesh().getBuffer("inValues"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void incompatibleAttributesAreRejectedWithoutDetachingExistingBatch(boolean simple) {
        for (int mismatch = 0; mismatch < 5; mismatch++) {
            BatchNode node = simple ? new SimpleBatchNode("batch") : new BatchNode("batch");
            Material material = new Material(new MaterialDef(new DesktopAssetManager(), "test"));
            node.attachChild(geometry(material, 1));
            node.batch();
            Geometry batch = node.batches.get(0).getGeometry();
            Geometry added = geometry(material, 20);
            Mesh mesh = added.getMesh();
            if (mismatch == 0) {
                mesh.clearBuffer("inValues");
            } else if (mismatch == 1) {
                mesh.clearBuffer("inValues");
                mesh.setBuffer("inValues", 1, VertexBuffer.Format.Float, BufferUtils.createFloatBuffer(4));
            } else if (mismatch == 2) {
                mesh.clearBuffer("inValues");
                mesh.setBuffer("inValues", 2, VertexBuffer.Format.Int, BufferUtils.createIntBuffer(8));
            } else if (mismatch == 3) {
                mesh.getBuffer("inTint").setNormalized(false);
            } else {
                mesh.clearBuffer("inValues");
                mesh.setBuffer("inValues", 2, VertexBuffer.Format.Float, BufferUtils.createFloatBuffer(6));
            }
            node.attachChild(added);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, node::batch);
            assertTrue(error.getMessage().contains("inValues") || error.getMessage().contains("inTint"));
            assertSame(node, batch.getParent());
            assertSame(batch, node.batches.get(0).getGeometry());
            assertFalse(added.isGrouped());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void namedDoubleAttributesAreCopied(boolean simple) {
        BatchNode node = simple ? new SimpleBatchNode("batch") : new BatchNode("batch");
        Material material = new Material(new MaterialDef(new DesktopAssetManager(), "test"));
        for (int i = 0; i < 2; i++) {
            Geometry geometry = geometry(material, i);
            geometry.getMesh().setBuffer("inDouble", 1, VertexBuffer.Format.Double,
                    BufferUtils.createDoubleBuffer(4).put(new double[]{i, i + 1, i + 2, i + 3}).flip());
            node.attachChild(geometry);
        }
        node.batch();
        java.nio.DoubleBuffer data = (java.nio.DoubleBuffer) node.batches.get(0).getGeometry()
                .getMesh().getBuffer("inDouble").getData();
        assertEquals(8, data.limit());
        assertEquals(0, data.get(0));
        assertEquals(4, data.get(7));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void instanceAttributesAreNotSilentlyConvertedToVertexAttributes(boolean simple) {
        BatchNode node = simple ? new SimpleBatchNode("batch") : new BatchNode("batch");
        Material material = new Material(new MaterialDef(new DesktopAssetManager(), "test"));
        Geometry geometry = geometry(material, 1);
        geometry.getMesh().getBuffer("inValues").setInstanceSpan(1);
        node.attachChild(geometry);
        UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class, node::batch);
        assertTrue(error.getMessage().contains("inValues"));
        assertFalse(geometry.isGrouped());
    }

    private Geometry geometry(Material material, int start) {
        Mesh mesh = new Quad(1, 1);
        FloatBuffer values = BufferUtils.createFloatBuffer(8);
        ByteBuffer tint = BufferUtils.createByteBuffer(16);
        for (int i = 0; i < 8; i++) values.put(start + i);
        for (int i = 0; i < 16; i++) tint.put((byte) (start + i));
        values.flip();
        tint.flip();
        mesh.setBuffer("inValues", 2, VertexBuffer.Format.Float, values);
        mesh.setBuffer("inTint", 4, VertexBuffer.Format.UnsignedByte, tint);
        mesh.getBuffer("inTint").setNormalized(true);
        Geometry geometry = new Geometry("source", mesh);
        geometry.setMaterial(material);
        return geometry;
    }
}
