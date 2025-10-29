package io.github.jmecn.tiled.renderer.shape;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import io.github.jmecn.tiled.math2d.Triangulation;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class Polygon extends Polyline {

    public Polygon() {
    }

    public Polygon(List<Vector2f> points, boolean fill) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("An polygon must have 2 points at least.");
        }
        if (fill) {
            fill(points);
        } else {
            polyline(points, true);
        }
    }

    protected void fill(List<Vector2f> points) {
        int len = points.size();
        List<Integer> result = new ArrayList<>(len);

        Triangulation.process(points, result);

        Vector3f[] vertex = new Vector3f[len];
        Vector3f[] normal = new Vector3f[len];
        Vector2f[] texCoord = new Vector2f[len];
        for(int i=0; i<len; i++) {
            Vector2f p = points.get(i);
            vertex[i] = new Vector3f(p.x, 0f, p.y);
            normal[i] = new Vector3f(0, 1, 0);
            texCoord[i] = new Vector2f(0, 0);
        }

        len = result.size();
        int[] index = new int[len];
        for(int i=0; i<len; i++) {
            index[i] = result.get(i);
        }

        this.setMode(Mode.Triangles);
        this.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertex));
        this.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normal));
        this.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        this.setBuffer(VertexBuffer.Type.Index, 3, index);
        this.updateBound();
        this.updateCounts();
        this.setStatic();
    }
}
