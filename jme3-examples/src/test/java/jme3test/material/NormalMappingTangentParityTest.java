package jme3test.material;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.system.JmeSystem;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NormalMappingTangentParityTest {
    @Test
    void regeneratedGltfTangentsMatchTheExportedFramesAtEveryCorner() {
        AssetManager assets = JmeSystem.newAssetManager(
                getClass().getResource("/com/jme3/asset/Desktop.cfg"));
        Spatial model = assets.loadModel("jme3test/normalmapCompare/NormalTangentMirrorTest.gltf");
        int[] corners = {0};
        model.depthFirstTraversal(spatial -> {
            if (!(spatial instanceof Geometry)) {
                return;
            }
            Mesh mesh = ((Geometry) spatial).getMesh();
            IndexBuffer originalIndices = mesh.getIndicesAsList();
            FloatBuffer originalTangents = mesh.getFloatBuffer(Type.Tangent);
            float[] expected = new float[originalIndices.size() * 4];
            for (int i = 0; i < originalIndices.size(); i++) {
                for (int c = 0; c < 4; c++) {
                    expected[i * 4 + c] = originalTangents.get(originalIndices.get(i) * 4 + c);
                }
            }
            MikktspaceTangentGenerator.generate(mesh);
            IndexBuffer indices = mesh.getIndicesAsList();
            FloatBuffer tangents = mesh.getFloatBuffer(Type.Tangent);
            assertEquals(expected.length, indices.size() * 4);
            for (int i = 0; i < indices.size(); i++) {
                int actual = indices.get(i) * 4;
                float dot = 0;
                for (int c = 0; c < 3; c++) {
                    dot += expected[i * 4 + c] * tangents.get(actual + c);
                }
                assertTrue(dot > 0.999f, "Tangent direction at corner " + i + ": " + dot);
                assertEquals(expected[i * 4 + 3], tangents.get(actual + 3),
                        "Handedness at corner " + i);
            }
            corners[0] += indices.size();
        });
        assertEquals(15720, corners[0]);
    }
}
