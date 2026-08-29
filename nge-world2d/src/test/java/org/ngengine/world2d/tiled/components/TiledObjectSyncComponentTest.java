package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponentManager;
import org.ngengine.network.interpolation.TransformInterpolatorAndPredictor;
import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.network.quantization.TransformQuantizer;
import org.ngengine.world2d.TiledWorld2dManagerComponent;
import org.ngengine.world2d.tiled.components.messages.TiledObjectSnapshotMessage;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

public class TiledObjectSyncComponentTest {
    private static final float EPSILON = 0.03f;

    @Test
    public void runtimeTilesetReferenceWinsOverCollidingMapGid() {
        TiledMap map = new TiledMap(4, 4);
        Tileset terrain = tileset("tilesets/terrain.tsx", "terrain", 1, "floor");
        Tileset particles = tileset("tilesets/spray.tsx", "spray", 0, "particle_extinguisher");
        map.addTileset(terrain);
        map.addTileset(particles);

        Tile resolved = TiledTileReferenceResolver.resolve(
            null,
            map,
            1,
            "tilesets/spray.tsx",
            "particle_extinguisher",
            0
        );

        assertEquals(particles.getTile(0), resolved);
    }

    @Test
    public void runtimeTileReferenceRoundTripsThroughTheNetworkProtocol() {
        TiledObjectSnapshotMessage snapshot = new TiledObjectSnapshotMessage();
        snapshot.setTileSource("tilesets/spray.tsx");
        snapshot.setTileClass("particle_extinguisher");
        snapshot.setTileId(7);

        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> { }, 1L);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> { }, -1L);
        TiledObjectSnapshotMessage decoded = (TiledObjectSnapshotMessage) receiver.toMessage(
            sender.toByteBuffer(snapshot, null)
        );

        assertEquals("tilesets/spray.tsx", decoded.getTileSource());
        assertEquals("particle_extinguisher", decoded.getTileClass());
        assertEquals(7, decoded.getTileId());
    }

    @Test
    public void lagAfterSnapshotGapSnapsToLatestAuthoritativeTransformAndResetsInterpolator()
            throws Exception {
        TiledMap map = new TiledMap(16, 16);
        map.setTileWidth(256);
        map.setTileHeight(128);
        TiledObjectLayer players = new TiledObjectLayer(map.getWidth(), map.getHeight());
        players.setName("players");
        map.addLayer(players);

        TiledObjectEntity entity =
            new TiledObjectEntity(BigInteger.valueOf(77), 0, 0, 32, 32);
        entity.putProperty("net.sync.component", ReplicaObjectSyncComponent.class.getName());
        players.add(entity);

        TiledWorld2dManagerComponent worlds =
            new TiledWorld2dManagerComponent((String) null);
        Components.mount(entity, worlds);
        entity.getComponentManager().setParent(new TestComponentManager());
        entity.getComponentManager().update(null, map, players, entity, 0f);
        ReplicaObjectSyncComponent sync =
            entity.getComponentManager().getComponent(ReplicaObjectSyncComponent.class);

        TransformQuantizer quantizer = worlds.getTransformQuantizer(map);
        Vector3f initial = new Vector3f(36f, 0f, 36f);
        Vector3f latest = new Vector3f(1408f, 0f, 1152f);
        sync.onSnapshot(snapshot(quantizer, initial));
        assertEquals(1, sync.getInterpolatorFactoryInvocationCount());

        Vector3f expectedInitial =
            quantizer.dequantizePosition(quantizer.quantizePosition(initial));
        Vector3f expectedLatest =
            quantizer.dequantizePosition(quantizer.quantizePosition(latest));
        assertEquals(expectedInitial.x, entity.getX(), EPSILON);
        assertEquals(expectedInitial.z, entity.getY(), EPSILON);

        TransformInterpolatorAndPredictor interpolator =
            field(sync, "transformInterpolator", TransformInterpolatorAndPredictor.class);
        long staleSnapshotTime = System.currentTimeMillis() - 2_000L;
        interpolator.clear();
        interpolator.addTransformPoint(
            staleSnapshotTime,
            new Transform(
                expectedInitial,
                new Quaternion(),
                new Vector3f(1f, 1f, 1f)
            )
        );
        setField(sync, "lastSnapshotPointMillis", staleSnapshotTime);

        sync.onSnapshot(snapshot(quantizer, latest));
        assertEquals(1, sync.getInterpolatorFactoryInvocationCount());
        assertEquals(2, interpolator.size());
        assertEquals(expectedInitial.x, entity.getX(), EPSILON);
        assertEquals(expectedInitial.z, entity.getY(), EPSILON);

        sync.onTiledEntityLogicUpdate(entity.getComponentManager(), 0f, entity);

        assertEquals(expectedLatest.x, entity.getX(), EPSILON);
        assertEquals(expectedLatest.z, entity.getY(), EPSILON);
        assertEquals(0, interpolator.size());
        assertEquals(
            Long.MIN_VALUE,
            field(sync, "lastSnapshotPointMillis", Long.class).longValue()
        );
    }

    private static TiledObjectSnapshotMessage snapshot(
            TransformQuantizer quantizer,
            Vector3f position) {
        TiledObjectSnapshotMessage snapshot = new TiledObjectSnapshotMessage();
        snapshot.setPackedTranslation(quantizer.quantizePosition(position));
        snapshot.setPackedRotation(quantizer.quantizeRotation(new Quaternion()));
        snapshot.setProperties(Map.of());
        snapshot.setWidth(32);
        snapshot.setHeight(32);
        return snapshot;
    }

    private static Tileset tileset(
            String source,
            String name,
            int firstGid,
            String tileClass) {
        Tileset tileset = new Tileset();
        tileset.setSource(source);
        tileset.setName(name);
        tileset.setFirstGid(firstGid);
        Tile tile = new Tile(0, firstGid, 32, 32);
        tile.setClazz(tileClass);
        tile.setTileset(tileset);
        tileset.addTile(tile);
        return tileset;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = TiledObjectSyncComponent.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T field(Object target, String name, Class<T> type)
            throws Exception {
        Field field = TiledObjectSyncComponent.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    public static class ReplicaObjectSyncComponent extends TiledObjectSyncComponent {
        private int interpolatorFactoryInvocationCount;

        @Override
        public boolean checkAuthority() {
            return false;
        }

        @Override
        protected TransformInterpolatorAndPredictor createTransformInterpolator(float worldScale) {
            interpolatorFactoryInvocationCount++;
            return super.createTransformInterpolator(worldScale);
        }

        int getInterpolatorFactoryInvocationCount() {
            return interpolatorFactoryInvocationCount;
        }
    }

    private static class TestComponentManager extends AbstractComponentManager {
    }
}
