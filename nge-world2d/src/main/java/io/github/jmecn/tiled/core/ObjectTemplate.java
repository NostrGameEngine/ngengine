package io.github.jmecn.tiled.core;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class ObjectTemplate {
    private String source;
    private Tileset tileset;
    private MapObject object;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Tileset getTileset() {
        return tileset;
    }

    public void setTileset(Tileset tileset) {
        this.tileset = tileset;
    }

    public MapObject getObject() {
        return object;
    }

    public void setObject(MapObject object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "ObjectTemplate{" +
                "source='" + source + '\'' +
                ", tileset=" + tileset +
                ", object=" + object +
                '}';
    }

    public void copyTo(MapObject obj) {
        obj.setName(object.getName());
        obj.setClazz(object.getClazz());
        obj.setWidth(object.getWidth());
        obj.setHeight(object.getHeight());
        obj.setShape(object.getShape());
        obj.setGid(object.getGid());// for Shape.TILE
        obj.setTile(object.getTile());// for Shape.TILE
        obj.setPoints(object.getPoints());// for Shape.POLYLINE, Shape.POLYGON
        obj.setImage(object.getImage());// for Shape.IMAGE
        obj.setTextData(object.getTextData());// for Shape.TEXT
        obj.setVisible(object.isVisible());
        obj.setRotation(object.getRotation());
        obj.setProperties(object.getProperties());
    }
}