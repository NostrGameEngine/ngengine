package org.ngengine.world2d.tiled.components;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.network.RemotePeer;
import org.ngengine.network.components.NetcodeDespawnActionMessage;
import org.ngengine.network.components.SnapshotMessage;
import org.ngengine.network.interpolation.TransformInterpolatorAndPredictor;
import org.ngengine.network.quantization.TransformQuantizer;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import org.ngengine.world2d.tiled.components.fragments.TiledEntityLifecycleFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledNetcodeFragment;
import org.ngengine.world2d.tiled.components.messages.TiledObjectSnapshotMessage;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.TiledWorld2dManagerComponent;

/**
 * Single authoritative entity-level snapshot producer/consumer.
 */
public class TiledObjectSyncComponent extends AbstractComponent implements TiledNetcodeFragment, TiledEntityLifecycleFragment, TiledEntityLogicFragment {
    private static final float FALLBACK_WORLD_SCALE = 1f;

    private boolean despawnRequested = false;
    private transient TransformInterpolatorAndPredictor transformInterpolator;
    private transient Transform snapshotTransformScratch;
    private transient Transform sampledTransformScratch;
    private transient float[] sampledAnglesScratch;
    private transient long lastSnapshotPointMillis = Long.MIN_VALUE;
    private transient String lastAuthorityPeer = null;

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        despawnRequested = false;
        transformInterpolator = null;
        snapshotTransformScratch = null;
        sampledTransformScratch = null;
        sampledAnglesScratch = null;
        lastSnapshotPointMillis = Long.MIN_VALUE;
        lastAuthorityPeer = null;
    }

    @Override
    protected void onDisable(ComponentManager mng) {
        if (transformInterpolator != null) {
            transformInterpolator.clear();
        }
        transformInterpolator = null;
        snapshotTransformScratch = null;
        sampledTransformScratch = null;
        sampledAnglesScratch = null;
        lastSnapshotPointMillis = Long.MIN_VALUE;
        lastAuthorityPeer = null;
    }

    @Override
    public BigInteger getNetworkId() {
        TiledObjectEntity entity = getNetworkEntity();
        return entity != null ? entity.getId() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SnapshotMessage> T requestSnapshot(RemotePeer target) {
        TiledObjectEntity entity = getNetworkEntity();
        if (entity == null || entity.getId() == null || entity.getId().signum() < 0) {
            return null;
        }
        TiledObjectSnapshotMessage snapshot = new TiledObjectSnapshotMessage();
        snapshot.setReliable(true);
        snapshot.setMapScope(getMapScope());
        TiledObjectLayer layer = entity.getObjectGroup();
        TiledMap map = layer != null ? layer.getMap() : null;
        if (map == null) {
            return null;
        }
        snapshot.setLayerName(layer != null ? layer.getName() : null);
        snapshot.setEntityId(entity.getId().toString());
        TransformQuantizer quantizer = requireQuantizer(map);
        Transform transform = new Transform(
            new Vector3f((float) entity.getX(), 0f, (float) entity.getY()),
            new Quaternion().fromAngles(0f, 0f, (float) Math.toRadians(entity.getRotation())),
            new Vector3f(1f, 1f, 1f)
        );
        long[] packed = quantizer.quantizeTransform(transform);
        snapshot.setPackedTranslation(packed[0]);
        snapshot.setPackedRotation(packed[1]);
        snapshot.setName(entity.getName());
        snapshot.setClazz(entity.getClazz());
        snapshot.setVisible(entity.isVisible());
        snapshot.setWidth(entity.getWidth());
        snapshot.setHeight(entity.getHeight());
        snapshot.setShape(entity.getShape() != null ? entity.getShape().name() : null);
        snapshot.setGid(entity.getGid());
        Map<String, Object> properties = sanitizeProperties(entity.getAllProperties());
        snapshot.setProperties(properties != null ? properties : Map.of());
        return (T) snapshot;
    }

    @Override
    public <T extends SnapshotMessage> void onSnapshot(T actionMessage) {
        if (!(actionMessage instanceof TiledObjectSnapshotMessage)) {
            return;
        }
        TiledObjectEntity entity = getNetworkEntity();
        if (entity == null) {
            return;
        }
        TiledObjectSnapshotMessage snapshot = (TiledObjectSnapshotMessage) actionMessage;
        TiledMap map = entity.getObjectGroup() != null ? entity.getObjectGroup().getMap() : null;
        if (map == null) {
            return;
        }
        TransformQuantizer quantizer = requireQuantizer(map);
        Vector3f decodedPosition = quantizer.dequantizePosition(snapshot.getPackedTranslation());
        Quaternion decodedRotation = quantizer.dequantizeRotation(snapshot.getPackedRotation());
        ensureInterpolatorState(map);
        String currentAuthorityPeer = snapshot.getSource() != null
            && snapshot.getSource().getRemotePeer() != null
            && snapshot.getSource().getRemotePeer().getPubkey() != null
            ? snapshot.getSource().getRemotePeer().getPubkey().asHex()
            : null;
        if ((lastAuthorityPeer == null && currentAuthorityPeer != null)
            || (lastAuthorityPeer != null && !lastAuthorityPeer.equals(currentAuthorityPeer))) {
            transformInterpolator.clear();
            lastSnapshotPointMillis = Long.MIN_VALUE;
        }
        lastAuthorityPeer = currentAuthorityPeer;

        // 2D tiled coordinates are mapped into 3D transforms as X/Z, keeping Y flat.
        snapshotTransformScratch.getTranslation().set(decodedPosition.x, 0f, decodedPosition.z);
        snapshotTransformScratch.getRotation().set(decodedRotation).normalizeLocal();
        snapshotTransformScratch.getScale().set(1f, 1f, 1f);

        long nowMillis = System.currentTimeMillis();
        if (nowMillis <= lastSnapshotPointMillis) {
            nowMillis = lastSnapshotPointMillis + 1L;
        }
        lastSnapshotPointMillis = nowMillis;
        transformInterpolator.addTransformPoint(nowMillis, snapshotTransformScratch);

        if (transformInterpolator.size() == 1) {
            applyTransform(entity, snapshotTransformScratch);
        }
        entity.setName(snapshot.getName());
        entity.setClazz(snapshot.getClazz());
        entity.setVisible(snapshot.isVisible());
        entity.setWidth(snapshot.getWidth());
        entity.setHeight(snapshot.getHeight());
        if (snapshot.getShape() != null && !snapshot.getShape().isEmpty()) {
            try {
                entity.setShape(ObjectShape.valueOf(snapshot.getShape()));
            } catch (IllegalArgumentException ex) {
                // Ignore unknown shape names from remote peers.
            }
        }
        applyTileFromSnapshot(entity, snapshot.getGid());
        Map<String, Object> props = snapshot.getProperties();
        entity.setProperties(props != null ? props : Map.of());
    }

    @Override
    public void onTiledEntityLogicUpdate(ComponentManager mng, float tpf, TiledBase entry) {
        if (!(entry instanceof TiledObjectEntity)) {
            return;
        }
        if (checkAuthority()) {
            if (transformInterpolator != null) {
                transformInterpolator.clear();
            }
            lastSnapshotPointMillis = Long.MIN_VALUE;
            lastAuthorityPeer = null;
            return;
        }
        if (transformInterpolator == null || transformInterpolator.size() == 0) {
            return;
        }
        TiledObjectEntity entity = (TiledObjectEntity) entry;
        ensureInterpolatorState(entity.getObjectGroup() != null ? entity.getObjectGroup().getMap() : null);
        TransformInterpolatorAndPredictor.SampleResult sample = transformInterpolator.sample(System.currentTimeMillis(), sampledTransformScratch);
        if (sample.status == TransformInterpolatorAndPredictor.Status.EMPTY) {
            return;
        }
        if (sample.status == TransformInterpolatorAndPredictor.Status.LAG) {
            // Reliable transform snapshots may be suppressed while an entity is idle.
            // If the next authoritative update arrives beyond the interpolation gap,
            // recover to that latest absolute transform instead of freezing forever.
            applyTransform(entity, snapshotTransformScratch);
            transformInterpolator.clear();
            lastSnapshotPointMillis = Long.MIN_VALUE;
            return;
        }

        applyTransform(entity, sampledTransformScratch);
    }

    private void applyTransform(TiledObjectEntity entity, Transform transform) {
        entity.setX(transform.getTranslation().x);
        entity.setY(transform.getTranslation().z);
        sampledAnglesScratch = transform.getRotation().toAngles(sampledAnglesScratch);
        entity.setRotation(Math.toDegrees(sampledAnglesScratch[2]));
    }

    private void applyTileFromSnapshot(TiledObjectEntity entity, int gid) {
        if (gid <= 0) {
            entity.setGid(gid);
            return;
        }
        TiledObjectLayer layer = entity.getObjectGroup();
        if (layer == null || layer.getMap() == null) {
            entity.setGid(gid);
            return;
        }
        Tile tile = null;
        try {
            tile = layer.getMap().getTileForTileGID(gid);
        } catch (Exception ignored) {
            tile = null;
        }
        if (tile != null) {
            entity.setTile(tile);
        } else {
            entity.setGid(gid);
        }
    }

    @Override
    public void onTiledEntityInitialize(ComponentManager mng, TiledBase entry) {
        despawnRequested = false;
        if (transformInterpolator != null) {
            transformInterpolator.clear();
        }
        lastSnapshotPointMillis = Long.MIN_VALUE;
        lastAuthorityPeer = null;
    }

    @Override
    public void onTiledEntityCleanup(ComponentManager mng, TiledBase entry) {
        if (transformInterpolator != null) {
            transformInterpolator.clear();
        }
        lastSnapshotPointMillis = Long.MIN_VALUE;
        lastAuthorityPeer = null;
        if (despawnRequested || getComponentId() == null || getNetworkId() == null) {
            return;
        }
        if (!checkAuthority()) {
            return;
        }
        despawnRequested = true;
        requestRemoteDespawn();
    }

    private Map<String, Object> sanitizeProperties(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            safe.put(entry.getKey(), sanitizeValue(entry.getValue()));
        }
        return safe;
    }

    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String
            || value instanceof Boolean
            || value instanceof Byte
            || value instanceof Short
            || value instanceof Integer
            || value instanceof Long
            || value instanceof Float
            || value instanceof Double
            || value instanceof Character
            || value instanceof BigInteger
            || value instanceof BigDecimal) {
            return value;
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, Object> safeMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                safeMap.put(String.valueOf(entry.getKey()), sanitizeValue(entry.getValue()));
            }
            return safeMap;
        }
        if (value instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) value;
            List<Object> safeList = new ArrayList<>(col.size());
            for (Object item : col) {
                safeList.add(sanitizeValue(item));
            }
            return safeList;
        }
        return String.valueOf(value);
    }

    private TransformQuantizer requireQuantizer(TiledMap map) {
        if (map == null) {
            throw new IllegalStateException("Cannot build tiled snapshot without map context.");
        }
        TiledWorld2dManagerComponent worlds = getInstanceOf(TiledWorld2dManagerComponent.class);
        if (worlds == null) {
            throw new IllegalStateException("Missing " + TiledWorld2dManagerComponent.class.getSimpleName() + " while syncing object snapshot.");
        }
        TransformQuantizer quantizer = worlds.getTransformQuantizer(map);
        if (quantizer == null) {
            throw new IllegalStateException("Missing transform quantizer for map " + map.getName());
        }
        return quantizer;
    }

    private void ensureInterpolatorState(TiledMap map) {
        if (transformInterpolator == null) {
            transformInterpolator = new TransformInterpolatorAndPredictor(resolveWorldScale(map));
        }
        if (snapshotTransformScratch == null) {
            snapshotTransformScratch = new Transform(
                new Vector3f(),
                new Quaternion(),
                new Vector3f(1f, 1f, 1f)
            );
        }
        if (sampledTransformScratch == null) {
            sampledTransformScratch = new Transform(
                new Vector3f(),
                new Quaternion(),
                new Vector3f(1f, 1f, 1f)
            );
        }
        if (sampledAnglesScratch == null) {
            sampledAnglesScratch = new float[3];
        }
    }

    private float resolveWorldScale(TiledMap map) {
        if (map == null) {
            return FALLBACK_WORLD_SCALE;
        }
        Float explicitScale = parsePositiveFloat(map.getProperty("worldScale"));
        if (explicitScale != null) {
            return explicitScale;
        }
        Float ppmScale = parsePositiveFloat(map.getPropertyOrDefault("ppm", null));
        if (ppmScale != null) {
            return ppmScale;
        }
        int tileWidth = Math.max(1, map.getTileWidth());
        int tileHeight = Math.max(1, map.getTileHeight());
        return Math.max(tileWidth, tileHeight);
    }

    private Float parsePositiveFloat(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            float n = ((Number) value).floatValue();
            return Float.isFinite(n) && n > 0f ? n : null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            float n = Float.parseFloat(raw);
            return Float.isFinite(n) && n > 0f ? n : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void requestRemoteDespawn() {
        invokeAction((RemotePeer) null, new NetcodeDespawnActionMessage());
    }
}
