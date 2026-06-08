package org.ngengine.world2d.tiled.components.messages;

import java.util.Map;
import java.util.Objects;

import org.ngengine.network.components.SnapshotMessage;
import org.ngengine.network.protocol.NetworkSafe;

@NetworkSafe
public class TiledObjectSnapshotMessage extends SnapshotMessage {
    private String mapScope;
    private String layerName;
    private String entityId;
    private String name;
    private String clazz;
    private boolean visible;
    private double width;
    private double height;
    private String shape;
    private int gid;
    private long packedTranslation;
    private long packedRotation;
    private Map<String, Object> properties;

    public String getMapScope() {
        return mapScope;
    }

    public void setMapScope(String mapScope) {
        this.mapScope = mapScope;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public long getPackedTranslation() {
        return packedTranslation;
    }

    public void setPackedTranslation(long packedTranslation) {
        this.packedTranslation = packedTranslation;
    }

    public long getPackedRotation() {
        return packedRotation;
    }

    public void setPackedRotation(long packedRotation) {
        this.packedRotation = packedRotation;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(),
            mapScope,
            layerName,
            entityId,
            name,
            clazz,
            Boolean.valueOf(visible),
            Double.valueOf(width),
            Double.valueOf(height),
            shape,
            Integer.valueOf(gid),
            Long.valueOf(packedTranslation),
            Long.valueOf(packedRotation),
            properties
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TiledObjectSnapshotMessage)) return false;
        if (!super.equals(obj)) return false;
        TiledObjectSnapshotMessage other = (TiledObjectSnapshotMessage) obj;
        return visible == other.visible
            && Double.compare(width, other.width) == 0
            && Double.compare(height, other.height) == 0
            && gid == other.gid
            && packedTranslation == other.packedTranslation
            && packedRotation == other.packedRotation
            && Objects.equals(mapScope, other.mapScope)
            && Objects.equals(layerName, other.layerName)
            && Objects.equals(entityId, other.entityId)
            && Objects.equals(name, other.name)
            && Objects.equals(clazz, other.clazz)
            && Objects.equals(shape, other.shape)
            && Objects.equals(properties, other.properties);
    }
}
