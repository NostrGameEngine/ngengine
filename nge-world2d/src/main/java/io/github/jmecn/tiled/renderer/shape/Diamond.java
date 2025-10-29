package io.github.jmecn.tiled.renderer.shape;

import com.jme3.math.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the basic grid shape in isometric map.
 *
 * @author yanmaoyuan
 */
public class Diamond extends Polygon {

    public Diamond(int tileWidth, int tileHeight, boolean fill) {
        List<Vector2f> polygon = new ArrayList<>(4);
        polygon.add(new Vector2f(0, tileHeight * 0.5f));
        polygon.add(new Vector2f(tileWidth * 0.5f, tileHeight));
        polygon.add(new Vector2f(tileWidth, tileHeight * 0.5f));
        polygon.add(new Vector2f(tileWidth * 0.5f, 0));
        if (fill) {
            fill(polygon);
        } else {
            polyline(polygon, true);
        }
    }
}
