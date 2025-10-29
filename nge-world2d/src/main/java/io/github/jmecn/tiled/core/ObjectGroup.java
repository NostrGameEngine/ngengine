package io.github.jmecn.tiled.core;

import java.util.LinkedList;
import java.util.List;

import com.jme3.math.ColorRGBA;
import com.jme3.util.SafeArrayList;

import io.github.jmecn.tiled.enums.DrawOrder;
import io.github.jmecn.tiled.math2d.Bound2D;

/**
 * The object group is in fact a map layer, and is hence called "object layer"
 * in Tiled Qt.
 * 
 * @author yanmaoyuan
 * 
 */
public class ObjectGroup extends Layer {

    /**
     * The color used to display the objects in this group.
     */
    private ColorRGBA color;

    /**
     * Whether the objects are drawn according to the order of appearance
     * ("index") or sorted by their y-coordinate ("topdown"). Defaults to
     * "topdown".
     */
    private DrawOrder drawOrder = DrawOrder.TOPDOWN;

    private final List<MapObject> objects = new SafeArrayList<>(MapObject.class);

    public ObjectGroup() {
        // for serialization
    }

    public ObjectGroup(int width, int height) {
        super(width, height);
    }

    public ColorRGBA getColor() {
        return color;
    }

    public void setColor(ColorRGBA color) {
        this.color = color;
    }
    
    public DrawOrder getDrawOrder() {
        return drawOrder;
    }

    public void setDrawOrder(DrawOrder drawOrder) {
        this.drawOrder = drawOrder;
    }

    public List<MapObject> getObjects() {
        return objects;
    }

    public MapObject get(int id) {
        for (MapObject obj : objects) {
            if (obj.getId() == id) {
                return obj;
            }
        }
        return null;
    }
    
    public void add(MapObject obj) {
        obj.setObjectGroup(this);
        objects.add(obj);
    }

    public void remove(MapObject o) {
        objects.remove(o);
        o.setObjectGroup(null);
    }

    /**
     * <p>
     * getObjectAt.
     * </p>
     * 
     * @param x a double.
     * @param y a double.
     * @return a {@link MapObject} object.
     */
    public MapObject getObjectAt(double x, double y) {
        for (MapObject obj : objects) {
            // Attempt to get an object bordering the point that has no width
            if (obj.getWidth() == 0 && obj.getX() == x) {
                return obj;
            }

            // Attempt to get an object bordering the point that has no height
            if (obj.getHeight() == 0 && obj.getY() == y) {
                return obj;
            }

            // TODO check if the object is a polygon or polyline, handle rotation
            Bound2D rect = new Bound2D(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());
            if (rect.contains(x, y)) {
                return obj;
            }
        }
        return null;
    }

}
