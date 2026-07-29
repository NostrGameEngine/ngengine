package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.network.components.NetcodePartitioning;
import org.ngengine.network.components.SnapshotMessage;
import org.ngengine.network.protocol.NetworkSafe;

import org.ngengine.world2d.tiled.components.fragments.TiledNetcodeFragment;
import org.ngengine.world2d.tiled.components.messages.TiledComponentSnapshotMessage;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

public class TiledNetcodeSpawnerTest {

    @Test
    public void bufferedComponentSnapshotsApplyCustomPayloadAfterMount() throws Exception {
        TiledNetcodeSpawner spawner = new TiledNetcodeSpawner();
        TiledObjectEntity entity = new TiledObjectEntity(BigInteger.valueOf(77), 0, 0, 16, 16);
        RecordingSnapshotMessage snapshot = new RecordingSnapshotMessage();
        snapshot.setEntityId("77");
        snapshot.setComponentType(RecordingNetcodeComponent.class.getName());
        snapshot.setValue(123);

        invoke(
            spawner,
            "bufferComponentSnapshot",
            new Class<?>[] { TiledComponentSnapshotMessage.class, BigInteger.class },
            snapshot,
            BigInteger.valueOf(77)
        );
        invoke(
            spawner,
            "applyBufferedComponentSnapshots",
            new Class<?>[] {
                org.ngengine.network.components.NetcodeManagerComponent.class,
                TiledObjectEntity.class,
                String.class,
                String.class,
                BigInteger.class
            },
            null,
            entity,
            null,
            null,
            BigInteger.valueOf(77)
        );

        RecordingNetcodeComponent component = entity.getComponentManager().getComponent(RecordingNetcodeComponent.class);
        assertNotNull(component);
        assertEquals(123, component.getLastValue());
    }

    @Test
    public void globallyUniqueDynamicEntityRecoversBufferedPayloadAcrossScopeKeyRace() throws Exception {
        TiledNetcodeSpawner spawner = new TiledNetcodeSpawner();
        BigInteger entityId = NetcodePartitioning.RESERVED_BASE.add(BigInteger.valueOf(77));
        TiledObjectEntity entity = new TiledObjectEntity(entityId, 0, 0, 16, 16);
        RecordingSnapshotMessage snapshot = new RecordingSnapshotMessage();
        snapshot.setMapScope("scope-before-object");
        snapshot.setLayerName("objects");
        snapshot.setEntityId(entityId.toString());
        snapshot.setComponentType(RecordingNetcodeComponent.class.getName());
        snapshot.setValue(456);

        invoke(
            spawner,
            "bufferComponentSnapshot",
            new Class<?>[] { TiledComponentSnapshotMessage.class, BigInteger.class },
            snapshot,
            entityId
        );
        invoke(
            spawner,
            "applyBufferedComponentSnapshots",
            new Class<?>[] {
                org.ngengine.network.components.NetcodeManagerComponent.class,
                TiledObjectEntity.class,
                String.class,
                String.class,
                BigInteger.class
            },
            null,
            entity,
            "scope-after-object",
            "objects",
            entityId
        );

        RecordingNetcodeComponent component = entity.getComponentManager().getComponent(RecordingNetcodeComponent.class);
        assertNotNull(component);
        assertEquals(456, component.getLastValue());
    }

    @Test
    public void localOnlyEntitiesKeepLocalAuthorityWhenNetcodeManagerExists() {
        TiledObjectEntity entity = new TiledObjectEntity(BigInteger.valueOf(-1), 0, 0, 16, 16);
        Components.mount(entity, new NetcodeManagerComponent());
        RecordingNetcodeComponent component = Components.mount(entity, new RecordingNetcodeComponent()).get();

        assertEquals(true, component.checkAuthority());
    }

    @Test
    public void localOnlyObjectSyncDoesNotPublishSnapshots() {
        TiledObjectEntity entity = new TiledObjectEntity(BigInteger.valueOf(-1), 0, 0, 16, 16);
        TiledObjectSyncComponent sync = entity.getComponentManager().getComponent(TiledObjectSyncComponent.class);

        assertNotNull(sync);
        assertNull(sync.requestSnapshot(null));
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    @NetworkSafe
    public static class RecordingSnapshotMessage extends TiledComponentSnapshotMessage {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), Integer.valueOf(value));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RecordingSnapshotMessage)) return false;
            if (!super.equals(obj)) return false;
            RecordingSnapshotMessage other = (RecordingSnapshotMessage) obj;
            return value == other.value;
        }
    }

    public static class RecordingNetcodeComponent extends AbstractComponent implements TiledNetcodeFragment {
        private int lastValue = -1;

        public int getLastValue() {
            return lastValue;
        }

        @Override
        public <T extends SnapshotMessage> void onSnapshot(T actionMessage) {
            if (actionMessage instanceof RecordingSnapshotMessage) {
                lastValue = ((RecordingSnapshotMessage) actionMessage).getValue();
            }
        }

        @Override
        protected void onEnable(ComponentManager mng, boolean firstTime) {}

        @Override
        protected void onDisable(ComponentManager mng) {}
    }
}
