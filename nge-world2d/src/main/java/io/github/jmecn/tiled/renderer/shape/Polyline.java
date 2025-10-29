package io.github.jmecn.tiled.renderer.shape;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class Polyline extends Mesh {

    public Polyline() {
    }

    public Polyline(List<Vector2f> points, boolean closePath) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("An polygon must have 2 points at least.");
        }
        polyline(points, closePath);
    }

    protected void polyline(List<Vector2f> points, boolean closePath) {
        int len = points.size();
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

        this.setMode(Mode.LineStrip);
        this.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertex));
        this.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normal));
        this.setBuffer(VertexBuffer.Type.Index, 2, index);
        this.updateBound();
        this.updateCounts();
        this.setStatic();
    }
}