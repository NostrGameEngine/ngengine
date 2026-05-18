package io.github.jmecn.tiled.components.messages;

import java.util.Objects;

import org.ngengine.network.components.SnapshotMessage;
import org.ngengine.network.protocol.NetworkSafe;

@NetworkSafe
public class TiledComponentSnapshotMessage extends SnapshotMessage {
    private String mapScope;
    private String layerName;
    private String entityId;
    private String componentType;
    private boolean enabled = true;

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

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(),
            mapScope,
            layerName,
            entityId,
            componentType,
            Boolean.valueOf(enabled)
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TiledComponentSnapshotMessage)) return false;
        if (!super.equals(obj)) return false;
        TiledComponentSnapshotMessage other = (TiledComponentSnapshotMessage) obj;
        return enabled == other.enabled
            && Objects.equals(mapScope, other.mapScope)
            && Objects.equals(layerName, other.layerName)
            && Objects.equals(entityId, other.entityId)
            && Objects.equals(componentType, other.componentType);
    }
}
