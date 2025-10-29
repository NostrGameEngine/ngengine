package io.github.jmecn.tiled.renderer.shape;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a marker to show the position of a point.
 *
 * @author yanmaoyuan
 */
public class Marker extends Polygon {

    public Marker(float radius, int count, boolean fill) {
        if (count < 3) {
            throw new IllegalArgumentException("count must be greater than 3");
        }
        if (fill) {
            fill(radius, count);
        } else {
            border(radius, count);
        }
    }

    /**
     * Make a border for map marker. It's a half circle on the top and a triangle on the bottom, point to (0,0).
     * @param radius the radius of the marker
     * @param count how many points you need?
     */
    private void border(float radius, int count) {
        List<Vector2f> points = new ArrayList<>(count + 1);
        points.add(new Vector2f(0, 0));

        float baseHeight = radius * -2f;
        float baseRadian = FastMath.PI / 3f;
        float radian = (FastMath.PI + baseRadian) / count;
        float r = 0;
        for(int i=0; i<count; i++) {
            float x = FastMath.sin(r + baseRadian) * radius;
            float y = FastMath.cos(r + baseRadian) * radius + baseHeight;
            points.add(new Vector2f(x, y));

            r += radian;
        }

        polyline(points, true);
    }


    /**
     * Make a border for map marker. It's a half circle on the top and a triangle on the bottom, point to (0,0).
     * @param radius the radius of the marker
     * @param count how many points you need?
     */
    private void fill(float radius, int count) {
        float baseHeight = radius * -2f;
        float baseRadian = FastMath.PI / 3f;

        List<Vector2f> points = new ArrayList<>(count + 2);
        points.add(new Vector2f(0, 0));

        float radian = (FastMath.PI + baseRadian) / count;
        float r = 0;
        for(int i=0; i<count; i++) {
            float x = FastMath.sin(r + baseRadian) * radius;
            float y = FastMath.cos(r + baseRadian) * radius + baseHeight;
            points.add(new Vector2f(x, y));

            r += radian;
        }

        super.fill(points);
    }
}