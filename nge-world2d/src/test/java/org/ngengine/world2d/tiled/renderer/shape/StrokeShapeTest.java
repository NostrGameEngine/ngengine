package org.ngengine.world2d.tiled.renderer.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.FloatBuffer;
import java.util.List;

import org.ngengine.world2d.tiled.util.ObjectMesh;
import org.ngengine.world2d.util.LineMeshTriangulator;
import org.junit.jupiter.api.Test;

import com.jme3.math.Vector2f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

class StrokeShapeTest {

    @Test
    void openPolylineUsesTwoTrianglesPerSegment() {
        Mesh source = new Mesh();
        source.setMode(Mesh.Mode.Lines);
        source.setBuffer(VertexBuffer.Type.Position, 3, new float[] {
            0f, 0f, 0f,
            10f, 0f, 0f
        });
        source.updateCounts();

        Mesh stroke = LineMeshTriangulator.triangulate(source, 2f);

        assertEquals(Mesh.Mode.Lines, source.getMode());
        assertEquals(Mesh.Mode.Triangles, stroke.getMode());
        assertEquals(4, stroke.getVertexCount());
        assertEquals(2, stroke.getTriangleCount());

        FloatBuffer positions = (FloatBuffer) stroke.getBuffer(VertexBuffer.Type.Position).getDataReadOnly();
        float minimumZ = Float.POSITIVE_INFINITY;
        float maximumZ = Float.NEGATIVE_INFINITY;
        while (positions.hasRemaining()) {
            positions.get();
            positions.get();
            float z = positions.get();
            minimumZ = Math.min(minimumZ, z);
            maximumZ = Math.max(maximumZ, z);
        }
        assertEquals(-1f, minimumZ);
        assertEquals(1f, maximumZ);
    }

    @Test
    void lineLoopProducesAClosedTriangleStroke() {
        Mesh source = new Mesh();
        source.setMode(Mesh.Mode.LineLoop);
        source.setBuffer(VertexBuffer.Type.Position, 3, new float[] {
            0f, 0f, 0f,
            10f, 0f, 0f,
            10f, 0f, 10f,
            0f, 0f, 10f
        });
        source.updateCounts();

        Mesh stroke = LineMeshTriangulator.triangulate(source, 1f);

        assertEquals(Mesh.Mode.Triangles, stroke.getMode());
        assertEquals(8, stroke.getVertexCount());
        assertEquals(8, stroke.getTriangleCount());
    }

    @Test
    void everyOutlinedShapeUsesPortableTriangleGeometry() {
        List<Mesh> outlines = List.of(
            new Polyline(List.of(new Vector2f(0f, 0f), new Vector2f(10f, 5f)), false),
            new Polygon(List.of(
                new Vector2f(0f, 0f),
                new Vector2f(10f, 0f),
                new Vector2f(10f, 10f),
                new Vector2f(0f, 10f)
            ), false),
            new Rect(10f, 5f, false),
            new Ellipse(10f, 5f, 32, false),
            new Marker(5f, 16, false)
        );

        for (Mesh outline : outlines) {
            assertEquals(Mesh.Mode.Triangles, outline.getMode());
            assertTrue(outline.getTriangleCount() > 0);
        }

        assertEquals(Mesh.Mode.Triangles, ObjectMesh.makeRectangleBorder(10f, 5f).getMode());
    }

    @Test
    void sharpAndRepeatedPointsStillProduceFiniteVertices() {
        Polyline stroke = new Polyline(List.of(
            new Vector2f(0f, 0f),
            new Vector2f(10f, 0f),
            new Vector2f(10f, 0f),
            new Vector2f(10.01f, 10f),
            new Vector2f(0f, 10f)
        ), true, 2f);

        FloatBuffer positions = (FloatBuffer) stroke.getBuffer(VertexBuffer.Type.Position).getDataReadOnly();
        while (positions.hasRemaining()) {
            assertTrue(Float.isFinite(positions.get()));
        }
    }
}
