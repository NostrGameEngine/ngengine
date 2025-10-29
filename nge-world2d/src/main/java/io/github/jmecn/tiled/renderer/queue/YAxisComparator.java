package io.github.jmecn.tiled.renderer.queue;

import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.scene.Geometry;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class YAxisComparator implements GeometryComparator {
    @Override
    public int compare(Geometry o1, Geometry o2) {
        float y1 = o1.getWorldTranslation().getY();
        float y2 = o2.getWorldTranslation().getY();
        return Float.compare(y1, y2);
    }
    @Override
    public void setCamera(Camera cam) {
        // nothing
    }
}
