package com.jme3.util.mikktspace;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.MorphTarget;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MikktspaceTangentGeneratorTest {
    @Test
    void mirroredUvSeamsKeepBothTangentFramesAndMorphData() {
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, new float[]{0,0,0, 1,0,0, 0,1,0, -1,0,0});
        mesh.setBuffer(Type.Normal, 3, new float[]{0,0,1, 0,0,1, 0,0,1, 0,0,1});
        mesh.setBuffer(Type.TexCoord, 2, new float[]{0,0, 1,0, 0,1, 1,0});
        mesh.setBuffer(Type.Color, 4, new float[]{1,0,0,1, 0,1,0,1, 0,0,1,1, 1,1,1,1});
        mesh.setBuffer(Type.Index, 3, new int[]{0,1,2, 0,2,3});
        MorphTarget morph = new MorphTarget("Move");
        morph.setBuffer(Type.Position, BufferUtils.createFloatBuffer(
                new float[]{0,0,1, 0,0,2, 0,0,3, 0,0,4}));
        mesh.addMorphTarget(morph);
        FloatBuffer positions = mesh.getFloatBuffer(Type.Position);
        positions.position(2);
        FloatBuffer uv = mesh.getFloatBuffer(Type.TexCoord);
        uv.position(1);

        MikktspaceTangentGenerator.generate(mesh);

        assertEquals(2, positions.position());
        assertEquals(1, uv.position());
        assertEquals(6, mesh.getVertexCount());
        assertEquals(2, mesh.getTriangleCount());
        IndexBuffer indices = mesh.getIndicesAsList();
        FloatBuffer tangents = mesh.getFloatBuffer(Type.Tangent);
        for (int corner = 0; corner < 6; corner++) {
            int vertex = indices.get(corner);
            float direction = corner < 3 ? 1f : -1f;
            assertEquals(direction, tangents.get(vertex * 4), 1e-6f);
            assertEquals(-direction, tangents.get(vertex * 4 + 3));
        }
        int splitOrigin = indices.get(3);
        assertNotEquals(indices.get(0), splitOrigin);
        assertEquals(1f, mesh.getFloatBuffer(Type.Color).get(splitOrigin * 4));
        assertEquals(1f, morph.getBuffer(Type.Position).get(splitOrigin * 3 + 2));
        // Re-generation must not keep duplicating already-correct seams.
        MikktspaceTangentGenerator.generate(mesh);
        assertEquals(6, mesh.getVertexCount());
    }

    @Test
    void continuousUvQuadKeepsItsSharedVertices() {
        Mesh mesh = new com.jme3.scene.shape.Quad(1, 1);
        MikktspaceTangentGenerator.generate(mesh);
        assertEquals(4, mesh.getVertexCount());
        FloatBuffer tangent = mesh.getFloatBuffer(Type.Tangent);
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(1f, tangent.get(vertex * 4));
            assertEquals(-1f, tangent.get(vertex * 4 + 3));
        }
    }

    @Test
    void rawMikkConventionIsExplicitAndDoesNotChangeTheLegacyDefault() {
        Mesh mesh = new com.jme3.scene.shape.Quad(1, 1);
        MikktspaceTangentGenerator.generate(mesh, MikktspaceTangentGenerator.TangentConvention.MIKKTSPACE);
        FloatBuffer tangent = mesh.getFloatBuffer(Type.Tangent);
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(1f, tangent.get(vertex * 4));
            assertEquals(1f, tangent.get(vertex * 4 + 3));
        }
        MikktspaceTangentGenerator.generate(mesh);
        tangent = mesh.getFloatBuffer(Type.Tangent);
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(-1f, tangent.get(vertex * 4 + 3));
        }
    }
}
