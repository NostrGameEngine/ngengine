package io.github.jmecn.tiled.renderer.shape;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class Ellipse extends Polyline {
    public Ellipse(float width, float height, int count, boolean fill) {
        if (count < 3) {
            throw new IllegalArgumentException("count must be greater than 3");
        }
        if (fill) {
            fill(width, height, count);
        } else {
            border(width, height, count);
        }
    }


    /**
     * Make an ellipse mesh.
     *
     *
     * @param width the ellipse width
     * @param height the ellipse height
     * @param count how many points you need?
     */
    protected void fill(float width, float height, int count) {
        // the uv center
        float uc = 0.5f;
        float vc = 0.5f;

        // the ellipse center
        float xc = width * uc;
        float yc = height * vc;

        // add two for center vertex and last triangle
        Vector3f[] vertex = new Vector3f[count+2];
        Vector3f[] normal = new Vector3f[count+2];
        Vector2f[] texCoord = new Vector2f[count+2];
        int[] index = new int[count+2];

        // the center
        vertex[0] = new Vector3f(xc, 0, yc);
        normal[0] = new Vector3f(0, 1, 0);
        texCoord[0] = new Vector2f(uc, vc);
        index[0] = 0;

        float radian = FastMath.TWO_PI / count;
        float r = 0;
        for(int i=0; i<count; i++) {
            float x = FastMath.sin(r);
            float y = FastMath.cos(r);
            r += radian;

            vertex[i+1] = new Vector3f(x*xc+xc, 0, y*yc+yc);
            normal[i+1] = new Vector3f(0, 1, 0);
            texCoord[i+1] = new Vector2f(x*uc+uc, y*vc+vc);
            index[i+1] = i+1;
        }
        vertex[count+1] = vertex[1];
        normal[count+1] = normal[1];
        texCoord[count+1] = texCoord[1];
        index[count+1] = count+1;

        setMode(Mode.TriangleFan);
        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertex));
        setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normal));
        setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        setBuffer(VertexBuffer.Type.Index, 3, index);
        setStatic();
        updateBound();
        updateCounts();
    }

    /**
     * Make a border for Ellipse.
     *
     *
     * @param width the ellipse width
     * @param height the ellipse height
     * @param count How many points you need?
     */
    public void border(double width, double height, int count) {
        float xc = (float) (width * 0.5);
        float yc = (float) (height * 0.5);

        List<Vector2f> points = new ArrayList<>(count);
        float radian = FastMath.TWO_PI / count;
        float r = 0;
        for(int i=0; i<count; i++) {
            float x = FastMath.sin(r) * xc + xc;
            float y = FastMath.cos(r) * yc + yc;
            points.add(new Vector2f(x, y));

            r += radian;
        }

        polyline(points, true);
    }
}