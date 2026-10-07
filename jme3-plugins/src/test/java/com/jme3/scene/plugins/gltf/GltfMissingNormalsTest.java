package com.jme3.scene.plugins.gltf;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.MorphTarget;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GltfMissingNormalsTest {
    @Test
    void missingNormalsAreFlatAndPreserveMorphAndUvCorners() {
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, new float[]{0,0,0, 1,0,0, 0,1,0, 0,0,1});
        mesh.setBuffer(Type.TexCoord, 2, new float[]{0,0, 1,0, 0,1, 1,1});
        mesh.setBuffer(Type.Index, 3, new int[]{0,1,2, 0,3,1});
        mesh.setBuffer(Type.Tangent, 4, new float[16]);
        MorphTarget morph = new MorphTarget();
        morph.setBuffer(Type.Position, BufferUtils.createFloatBuffer(new float[]{0,0,1, 0,0,2, 0,0,3, 0,0,4}));
        mesh.addMorphTarget(morph);
        GltfUtils.generateMissingNormals(mesh);
        assertEquals(6, mesh.getVertexCount());
        assertEquals(2, mesh.getTriangleCount());
        assertNull(mesh.getBuffer(Type.Tangent));
        FloatBuffer normal = mesh.getFloatBuffer(Type.Normal);
        for (int i = 0; i < 6; i++) {
            assertEquals(0f, normal.get(i * 3));
            assertEquals(i < 3 ? 0f : 1f, normal.get(i * 3 + 1));
            assertEquals(i < 3 ? 1f : 0f, normal.get(i * 3 + 2));
        }
        assertEquals(4f, morph.getBuffer(Type.Position).get(4 * 3 + 2));
        assertEquals(1f, mesh.getFloatBuffer(Type.TexCoord).get(4 * 2));
        GltfUtils.generateMissingNormals(mesh);
        assertEquals(6, mesh.getVertexCount());
    }
}
