package io.github.jmecn.tiled.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Objects;

import org.junit.Test;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.network.components.SnapshotMessage;
import org.ngengine.network.protocol.NetworkSafe;

import io.github.jmecn.tiled.components.fragments.TiledNetcodeFragment;
import io.github.jmecn.tiled.components.messages.TiledComponentSnapshotMessage;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;

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
