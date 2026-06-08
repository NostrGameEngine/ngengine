package org.ngengine.world2d.tiled.components.fragments;

import java.math.BigInteger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.ActionBasedFragment;
import org.ngengine.network.RemotePeer;
import org.ngengine.network.components.NetcodeFragment;
import org.ngengine.network.components.SnapshotMessage;

import org.ngengine.world2d.tiled.components.messages.TiledComponentSnapshotMessage;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import jakarta.annotation.Nullable;

/**
 * Optional network extension on top of {@link ActionBasedFragment}.
 *
 * <p>Components can implement this interface when they need authority checks,
 * periodic snapshots, player list updates, or typed network message callbacks.
 */
public interface TiledNetcodeFragment extends NetcodeFragment {

    default @Nullable TiledObjectEntity getNetworkEntity() {
        if (!(this instanceof Component)) {
            return null;
        }
        Component cmp = (Component) this;
        ComponentManager manager = cmp.getComponentManager();
        if (manager == null) {
            return null;
        }
        Object instance = manager.getInstanceOf(TiledObjectEntity.class);
        return instance instanceof TiledObjectEntity ? (TiledObjectEntity) instance : null;
    }

    default String getBaseComponentId() {
        return this.getClass().getName();
    }

    default String getMapScope() {
        TiledObjectEntity entity = getNetworkEntity();
        if (entity == null || entity.getObjectGroup() == null) {
            return "";
        }
        TiledMap map = entity.getObjectGroup().getMap();
        if (map == null || map.getName() == null) {
            return "";
        }
        return map.getName();
    }

    @Override
    default String getComponentId() {
        String scope = getMapScope();
        if (scope == null || scope.isEmpty()) {
            return getBaseComponentId();
        }
        return scope + "/" + getBaseComponentId();
    }

    @Override
    default BigInteger getNetworkId() {
        TiledObjectEntity entity = getNetworkEntity();
        if (entity == null) {
            return null;
        }
        return entity.getId();
    }

    @Override
    default <T extends SnapshotMessage> void onSnapshot(T actionMessage) {
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T extends SnapshotMessage> T requestSnapshot(RemotePeer target) {
        BigInteger networkId = getNetworkId();
        if (networkId == null) {
            return null;
        }

        TiledObjectEntity entity = getNetworkEntity();
        boolean enabled = true;
        if (this instanceof Component) {
            Component cmp = (Component) this;
            if (cmp.getComponentManager() != null) {
                enabled = cmp.getComponentManager().isComponentEnabled(cmp);
            }
        }

        TiledComponentSnapshotMessage snapshot = new TiledComponentSnapshotMessage();
        snapshot.setMapScope(getMapScope());
        snapshot.setLayerName(entity != null && entity.getObjectGroup() != null ? entity.getObjectGroup().getName() : null);
        snapshot.setEntityId(networkId.toString());
        snapshot.setComponentType(getClass().getName());
        snapshot.setEnabled(enabled);
        snapshot.setReliable(true);
        return (T) snapshot;
    }
}
