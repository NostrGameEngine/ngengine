package io.github.jmecn.tiled.util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

/**
 * Create mesh for the visual part of an ObjectNode.
 * Bound2D, Ellipse, Polygon, Polyline
 * 
 * @author yanmaoyuan
 *
 */
public final class ObjectMesh {

    private ObjectMesh() {}

    public static void toIsometric(Mesh mesh, int tileWidth, int tileHeight) {
        float ratio = (float) tileHeight / tileWidth;
        Matrix3f mat3 = new Matrix3f(
                1f, 0f, -1f,
                0f, 1f, 0f,
                ratio, 0f, ratio);

        VertexBuffer vb = mesh.getBuffer(VertexBuffer.Type.Position);
        FloatBuffer fb = (FloatBuffer) vb.getData();
        for (int i = 0; i < fb.capacity(); i += 3) {
            Vector3f v = new Vector3f(fb.get(i), 0f, fb.get(i + 2));
            mat3.multLocal(v);
            fb.put(i, v.x);
            fb.put(i + 2, v.z);
        }
        mesh.updateBound();
    }

    public static Mesh makeRectangleBorder(double width, double height) {
        List<Vector2f> points = new ArrayList<>();
        points.add(new Vector2f(0,0));
        points.add(new Vector2f((float) width,0));
        points.add(new Vector2f((float)width, (float)height));
        points.add(new Vector2f(0, (float) height));
        
        return makePolyline(points, true);
    }

    public static Mesh makePolyline(List<Vector2f> points, boolean closePath) {
        int len = points.size();
        if (len < 2) {
            throw new IllegalArgumentException("An polygon must have 2 points at least.");
        }
        
        Vector3f[] vertex = new Vector3f[len];
        Vector3f[] normal = new Vector3f[len];
        short[] index = new short[closePath?len+1:len];
        
        // first one
        Vector2f point = new Vector2f();
        for(int i=0; i<len; i++) {
            point.set(points.get(i));
            vertex[i] = new Vector3f(point.x, 0, point.y);
            normal[i] = new Vector3f(0f, 1f, 0f);
            index[i] = (short) i;
        }
        
        if (closePath) {
            index[len] = 0;
        }
        
        Mesh mesh = new Mesh();
        mesh.setMode(Mode.LineStrip);
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertex));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normal));
        mesh.setBuffer(Type.Index, 2, index);
        mesh.setStatic();
        mesh.updateBound();
        mesh.updateCounts();
        
        return mesh;
    }
}
