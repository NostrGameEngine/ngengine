package org.ngengine.world2d.tiled.components;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.Components;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.network.components.NetcodeFragment;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.network.components.NetcodePartitioning;
import org.ngengine.network.components.NetcodeSpawner;
import org.ngengine.network.components.SnapshotMessage;
import org.ngengine.network.quantization.TransformQuantizer;
import org.ngengine.world2d.TiledWorld2d;
import org.ngengine.world2d.TiledWorld2dManagerComponent;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import org.ngengine.world2d.tiled.components.messages.TiledComponentSnapshotMessage;
import org.ngengine.world2d.tiled.components.messages.TiledObjectSnapshotMessage;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import jakarta.annotation.Nullable;

public class TiledNetcodeSpawner implements NetcodeSpawner {
    private static final Logger log = Logger.getLogger(TiledNetcodeSpawner.class.getName());
    private static final long COMPONENT_SNAPSHOT_BUFFER_TTL_MS = 5000L;
    private final Map<String, List<BufferedComponentSnapshot>> bufferedComponentSnapshots = new HashMap<>();
    private final Map<String, List<BufferedObjectSnapshot>> bufferedObjectSnapshots = new HashMap<>();

    private static final class BufferedComponentSnapshot {
        private final TiledComponentSnapshotMessage snapshot;
        private final Instant createdAt;

        private BufferedComponentSnapshot(TiledComponentSnapshotMessage snapshot, Instant createdAt) {
            this.snapshot = snapshot;
            this.createdAt = createdAt;
        }
    }

    private static final class BufferedObjectSnapshot {
        private final TiledObjectSnapshotMessage snapshot;
        private final Instant createdAt;

        private BufferedObjectSnapshot(TiledObjectSnapshotMessage snapshot, Instant createdAt) {
            this.snapshot = snapshot;
            this.createdAt = createdAt;
        }
    }

    @Override
    public @Nullable NetcodeFragment spawn(NetcodeManagerComponent manager, SnapshotMessage snapshot) {
        if (snapshot instanceof TiledObjectSnapshotMessage) {
            return spawnObjectFromSnapshot(manager, (TiledObjectSnapshotMessage) snapshot);
        }
        if (snapshot instanceof TiledComponentSnapshotMessage) {
            return spawnComponentFromSnapshot(manager, (TiledComponentSnapshotMessage) snapshot);
        }
        return null;
    }

    @Override
    public void despawn(
        NetcodeManagerComponent manager,
        @Nullable NetcodeFragment target
    ) {
        if (target instanceof TiledObjectSyncComponent) {
            TiledObjectSyncComponent sync = (TiledObjectSyncComponent) target;
            TiledObjectEntity entity = sync.getNetworkEntity();
            if (entity != null && entity.getObjectGroup() != null) {
                entity.getObjectGroup().remove(entity);
            }
            return;
        }
        if (target instanceof Component) {
            Component cmp = (Component) target;
            cmp.getComponentManager().removeComponent(cmp);
        }
    }

    private @Nullable NetcodeFragment spawnObjectFromSnapshot(
        NetcodeManagerComponent manager,
        TiledObjectSnapshotMessage snapshot
    ) {
        pruneBufferedSnapshots();
        BigInteger entityId = parseEntityId(snapshot.getEntityId());
        if (entityId == null) return null;

        // Try any buffered object snapshots first (they might succeed now that world loaded)
        trySpawnBufferedObjectSnapshots(manager);

        TiledObjectEntity entity = resolveEntity(manager, snapshot.getMapScope(), snapshot.getLayerName(), entityId);
        if (entity == null) {
            entity = spawnEntityFromSnapshot(manager, snapshot, entityId);
            if (entity == null) {
                // Buffer the object snapshot and retry later
                bufferObjectSnapshot(snapshot, entityId);
                log.log(Level.FINE, "Buffering object snapshot for missing entity id=" + snapshot.getEntityId()
                    + " map=" + snapshot.getMapScope() + " layer=" + snapshot.getLayerName());
                return null;
            }
        }

        applyBufferedComponentSnapshots(manager, entity, snapshot.getMapScope(), snapshot.getLayerName(), entityId);
        return ensureObjectSyncComponent(entity, snapshot.getComponentId());
    }

    private @Nullable NetcodeFragment spawnComponentFromSnapshot(
        NetcodeManagerComponent manager,
        TiledComponentSnapshotMessage snapshot
    ) {
        pruneBufferedSnapshots();
        BigInteger entityId = parseEntityId(snapshot.getEntityId());
        if (entityId == null) return null;

        TiledObjectEntity entity = resolveEntity(manager, snapshot.getMapScope(), snapshot.getLayerName(), entityId);
        if (entity == null) {
            bufferComponentSnapshot(snapshot, entityId);
            log.log(Level.FINE, "Buffering component snapshot for missing entity id=" + snapshot.getEntityId()
                + " map=" + snapshot.getMapScope() + " layer=" + snapshot.getLayerName());
            return null;
        }

        return applyComponentSnapshot(entity, snapshot);
    }

    private @Nullable NetcodeFragment applyComponentSnapshot(
        TiledObjectEntity entity,
        TiledComponentSnapshotMessage snapshot
    ) {
        String componentType = snapshot.getComponentType();
        if (componentType == null || componentType.isEmpty()) {
            return null;
        }
        Component component = mountComponent(entity, componentType);
        if (component == null) return null;
        applyComponentEnabledState(entity, component, snapshot.isEnabled());
        return component instanceof NetcodeFragment ? (NetcodeFragment) component : null;
    }

    private @Nullable NetcodeFragment ensureObjectSyncComponent(TiledObjectEntity entity, @Nullable String requestedComponentId) {
        ComponentManager componentManager = entity.getComponentManager();
        String syncComponentClass = resolveSyncComponentClassName(entity, requestedComponentId);
        if (syncComponentClass != null && !syncComponentClass.isEmpty()) {
            Component mounted = TiledComponentReflectionMounting.mountByClassName(componentManager, syncComponentClass, entity);
            if (mounted instanceof NetcodeFragment) {
                return (NetcodeFragment) mounted;
            }
        }
        TiledObjectSyncComponent sync = componentManager.getComponent(TiledObjectSyncComponent.class);
        if (sync == null) {
            sync = Components.mount(entity, new TiledObjectSyncComponent()).enable().get();
        }
        return sync;
    }

    private @Nullable Component mountComponent(TiledObjectEntity entity, String componentType) {
        return TiledComponentReflectionMounting.mountByClassName(
            entity.getComponentManager(),
            componentType,
            entity
        );
    }

    private String bufferedKey(@Nullable String mapScope, @Nullable String layerName, BigInteger entityId) {
        String scope = mapScope != null ? mapScope : "";
        String layer = layerName != null ? layerName : "";
        return scope + "|" + layer + "|" + entityId.toString();
    }

    private void bufferComponentSnapshot(TiledComponentSnapshotMessage snapshot, BigInteger entityId) {
        String key = bufferedKey(snapshot.getMapScope(), snapshot.getLayerName(), entityId);
        bufferedComponentSnapshots
            .computeIfAbsent(key, ignored -> new ArrayList<>())
            .add(new BufferedComponentSnapshot(snapshot, Instant.now()));
    }

    private void bufferObjectSnapshot(TiledObjectSnapshotMessage snapshot, BigInteger entityId) {
        String key = bufferedKey(snapshot.getMapScope(), snapshot.getLayerName(), entityId);
        bufferedObjectSnapshots
            .computeIfAbsent(key, ignored -> new ArrayList<>())
            .add(new BufferedObjectSnapshot(snapshot, Instant.now()));
    }

    private void applyBufferedComponentSnapshots(
        NetcodeManagerComponent manager,
        TiledObjectEntity entity,
        @Nullable String mapScope,
        @Nullable String layerName,
        BigInteger entityId
    ) {
        String key = bufferedKey(mapScope, layerName, entityId);
        List<BufferedComponentSnapshot> buffered = bufferedComponentSnapshots.remove(key);
        if ((buffered == null || buffered.isEmpty())
                && (NetcodePartitioning.isReservedId(entityId)
                    || NetcodePartitioning.isPersistentId(entityId))) {
            Iterator<Map.Entry<String, List<BufferedComponentSnapshot>>> iterator =
                bufferedComponentSnapshots.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<BufferedComponentSnapshot>> candidate = iterator.next();
                List<BufferedComponentSnapshot> snapshots = candidate.getValue();
                boolean sameGloballyUniqueEntity = snapshots != null && snapshots.stream()
                    .anyMatch(item -> entityId.equals(parseEntityId(item.snapshot.getEntityId())));
                if (sameGloballyUniqueEntity) {
                    buffered = snapshots;
                    iterator.remove();
                    log.log(Level.FINE,
                        "Recovered buffered component snapshots by globally unique entity id=" + entityId
                            + " requestedKey=" + key + " bufferedKey=" + candidate.getKey());
                    break;
                }
            }
        }
        if (buffered == null || buffered.isEmpty()) {
            return;
        }
        for (BufferedComponentSnapshot item : buffered) {
            try {
                NetcodeFragment handler = applyComponentSnapshot(entity, item.snapshot);
                if (handler != null) {
                    if (manager != null) {
                        manager.registerActionHandler(handler);
                    }
                    handler.onSnapshot(item.snapshot);
                }
            } catch (Throwable ex) {
                log.log(Level.WARNING, "Failed applying buffered component snapshot for entity "
                    + entityId + " componentType=" + item.snapshot.getComponentType(), ex);
            }
        }
    }

    private void trySpawnBufferedObjectSnapshots(NetcodeManagerComponent manager) {
        if (bufferedObjectSnapshots.isEmpty()) return;
        Iterator<Map.Entry<String, List<BufferedObjectSnapshot>>> it = bufferedObjectSnapshots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<BufferedObjectSnapshot>> entry = it.next();
            List<BufferedObjectSnapshot> list = entry.getValue();
            Iterator<BufferedObjectSnapshot> sit = list.iterator();
            while (sit.hasNext()) {
                BufferedObjectSnapshot item = sit.next();
                try {
                    BigInteger entityId = parseEntityId(item.snapshot.getEntityId());
                    if (entityId == null) {
                        sit.remove();
                        continue;
                    }
                    TiledObjectEntity entity = resolveEntity(manager, item.snapshot.getMapScope(), item.snapshot.getLayerName(), entityId);
                    if (entity != null) {
                        // entity already exists, apply buffered component snapshots then remove buffer
                        applyBufferedComponentSnapshots(manager, entity, item.snapshot.getMapScope(), item.snapshot.getLayerName(), entityId);
                        sit.remove();
                        continue;
                    }
                    TiledObjectEntity spawned = spawnEntityFromSnapshot(manager, item.snapshot, entityId);
                    if (spawned != null) {
                        applyBufferedComponentSnapshots(manager, spawned, item.snapshot.getMapScope(), item.snapshot.getLayerName(), entityId);
                        sit.remove();
                    }
                } catch (Throwable ex) {
                    log.log(Level.WARNING, "Failed to spawn buffered object snapshot", ex);
                }
            }
            if (list.isEmpty()) {
                it.remove();
            }
        }
    }

    private void pruneBufferedSnapshots() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, List<BufferedComponentSnapshot>>> it = bufferedComponentSnapshots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<BufferedComponentSnapshot>> entry = it.next();
            List<BufferedComponentSnapshot> list = entry.getValue();
            list.removeIf(item -> now.toEpochMilli() - item.createdAt.toEpochMilli() > COMPONENT_SNAPSHOT_BUFFER_TTL_MS);
            if (list.isEmpty()) {
                it.remove();
            }
        }
        Iterator<Map.Entry<String, List<BufferedObjectSnapshot>>> oit = bufferedObjectSnapshots.entrySet().iterator();
        while (oit.hasNext()) {
            Map.Entry<String, List<BufferedObjectSnapshot>> entry = oit.next();
            List<BufferedObjectSnapshot> list = entry.getValue();
            list.removeIf(item -> now.toEpochMilli() - item.createdAt.toEpochMilli() > COMPONENT_SNAPSHOT_BUFFER_TTL_MS);
            if (list.isEmpty()) {
                oit.remove();
            }
        }
    }

    private void applyComponentEnabledState(TiledObjectEntity entity, Component component, boolean enabled) {
        if (enabled) {
            entity.getComponentManager().enableComponent(component);
        } else {
            entity.getComponentManager().disableComponent(component);
        }
    }

    private @Nullable TiledObjectEntity spawnEntityFromSnapshot(
        NetcodeManagerComponent manager,
        TiledObjectSnapshotMessage snapshot,
        BigInteger entityId
    ) {
        TiledObjectLayer layer = resolveObjectLayer(manager, snapshot.getMapScope(), snapshot.getLayerName());
        if (layer == null) {
            log.log(Level.WARNING, "Cannot spawn entity id=" + entityId + ": layer not found map="
                + snapshot.getMapScope() + " layer=" + snapshot.getLayerName());
            return null;
        }
        TransformQuantizer quantizer = resolveQuantizer(manager, layer.getMap());
        if (quantizer == null) {
            log.log(Level.WARNING, "Cannot spawn entity id=" + entityId + ": missing transform quantizer for map="
                + snapshot.getMapScope() + " layer=" + snapshot.getLayerName());
            return null;
        }
        Vector3f decodedPosition = quantizer.dequantizePosition(snapshot.getPackedTranslation());
        Quaternion decodedRotation = quantizer.dequantizeRotation(snapshot.getPackedRotation());
        float[] angles = decodedRotation.toAngles(new float[3]);
        double spawnX = decodedPosition.x;
        double spawnY = decodedPosition.z;
        double spawnRotation = Math.toDegrees(angles[2]);
        TiledObjectEntity entity = new TiledObjectEntity(entityId, spawnX, spawnY, snapshot.getWidth(), snapshot.getHeight());
        entity.setRotation(spawnRotation);
        entity.setName(snapshot.getName());
        entity.setClazz(snapshot.getClazz());
        entity.setVisible(snapshot.isVisible());
        if (snapshot.getShape() != null && !snapshot.getShape().isEmpty()) {
            try {
                entity.setShape(ObjectShape.valueOf(snapshot.getShape()));
            } catch (IllegalArgumentException ex) {
                entity.setShape(ObjectShape.RECTANGLE);
            }
        } else {
            entity.setShape(ObjectShape.RECTANGLE);
        }
        entity.setProperties(snapshot.getProperties() != null ? snapshot.getProperties() : java.util.Map.of());
        applyTileFromSnapshot(entity, layer, snapshot.getGid(), entityId);
        layer.add(entity);
        ensureObjectSyncComponent(entity, snapshot.getComponentId());
        return entity;
    }

    private void applyTileFromSnapshot(
        TiledObjectEntity entity,
        @Nullable TiledObjectLayer layer,
        int gid,
        BigInteger entityId
    ) {
        if (gid <= 0) {
            entity.setGid(gid);
            return;
        }
        if (layer == null || layer.getMap() == null) {
            entity.setGid(gid);
            return;
        }
        Tile tile = null;
        try {
            tile = layer.getMap().getTileForTileGID(gid);
        } catch (Exception ex) {
            log.log(Level.WARNING, "Cannot resolve tile for spawned entity id=" + entityId + " gid=" + gid, ex);
        }
        if (tile != null) {
            entity.setTile(tile);
        } else {
            entity.setGid(gid);
        }
    }

    private @Nullable String resolveSyncComponentClassName(TiledObjectEntity entity, @Nullable String requestedComponentId) {
        Object syncProp = entity.getProperty("net.sync.component");
        if (syncProp != null) {
            String cls = String.valueOf(syncProp).trim();
            if (!cls.isEmpty()) {
                return cls;
            }
        }
        if (requestedComponentId == null || requestedComponentId.isEmpty()) {
            return null;
        }
        int slash = requestedComponentId.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < requestedComponentId.length()) {
            return requestedComponentId.substring(slash + 1);
        }
        return requestedComponentId;
    }

    private @Nullable TiledWorld2d resolveWorld(NetcodeManagerComponent manager, @Nullable String mapScope) {
        TiledWorld2dManagerComponent worlds = manager.getInstanceOf(TiledWorld2dManagerComponent.class);
        if (worlds == null) {
            return null;
        }
        if (mapScope != null && !mapScope.isEmpty()) {
            TiledWorld2d world = worlds.getWorld(mapScope);
            if (world != null) {
                return world;
            }
        }
        return worlds.getDefaultWorld();
    }

    private @Nullable TiledObjectLayer resolveObjectLayer(
        NetcodeManagerComponent manager,
        @Nullable String mapScope,
        @Nullable String layerName
    ) {
        TiledWorld2d world = resolveWorld(manager, mapScope);
        if (world == null) {
            return null;
        }
        TiledMap map = world.getMap();
        if (layerName != null && !layerName.isEmpty()) {
            return map.getLayerByName(layerName, TiledObjectLayer.class);
        }
        for (TiledLayer layer : map.getLayersFlat()) {
            if (layer instanceof TiledObjectLayer) {
                return (TiledObjectLayer) layer;
            }
        }
        return null;
    }

    private @Nullable TiledObjectEntity resolveEntity(
        NetcodeManagerComponent manager,
        @Nullable String mapScope,
        @Nullable String layerName,
        BigInteger entityId
    ) {
        TiledObjectEntity inPreferredLayer = resolveEntityInPreferredLayer(manager, mapScope, layerName, entityId);
        if (inPreferredLayer != null) return inPreferredLayer;

        TiledWorld2d world = resolveWorld(manager, mapScope);
        if (world == null) return null;

        for (TiledLayer l : world.getMap().getLayersFlat()) {
            if (l instanceof TiledObjectLayer) {
                TiledObjectEntity o = ((TiledObjectLayer) l).get(entityId);
                if (o != null) {
                    return o;
                }
            }
        }
        return null;
    }

    private @Nullable TiledObjectEntity resolveEntityInPreferredLayer(
        NetcodeManagerComponent manager,
        @Nullable String mapScope,
        @Nullable String layerName,
        BigInteger entityId
    ) {
        TiledObjectLayer layer = resolveObjectLayer(manager, mapScope, layerName);
        if (layer == null) return null;
        return layer.get(entityId);
    }

    private TransformQuantizer resolveQuantizer(NetcodeManagerComponent manager, @Nullable TiledMap map) {
        if (map == null || manager == null) {
            return null;
        }
        TiledWorld2dManagerComponent worlds = manager.getInstanceOf(TiledWorld2dManagerComponent.class);
        if (worlds == null) {
            return null;
        }
        return worlds.getTransformQuantizer(map);
    }

    private @Nullable BigInteger parseEntityId(@Nullable String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return null;
        }
        try {
            return new BigInteger(entityId);
        } catch (Exception ex) {
            return null;
        }
    }
}
