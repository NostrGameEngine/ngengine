package org.ngengine.network.components;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.ngengine.network.RemotePeer;
import org.ngengine.network.protocol.NetworkSafe;

public class TestNetcodeManagerSnapshotFlow {

    @NetworkSafe
    public static class TestSnapshotMessage extends SnapshotMessage {
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
            if (!(obj instanceof TestSnapshotMessage)) return false;
            if (!super.equals(obj)) return false;
            TestSnapshotMessage other = (TestSnapshotMessage) obj;
            return value == other.value;
        }

        @Override
        public long getDiffGroup() {
            return 0;
        }
    }

    private static final class TestHandler implements NetcodeFragment {
        private int value;
        private final BigInteger id = BigInteger.valueOf(42);
        private boolean authoritative = true;

        @Override
        public BigInteger getNetworkId() {
            return id;
        }

        @Override
        public String getComponentId() {
            return "test/component";
        }

        @Override
        public NetcodeBehavior getNetworkBehavior() {
            return new NetcodeBehavior(Duration.ofMillis(0));
        }

        @Override
        public boolean checkAuthority() {
            return authoritative;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends SnapshotMessage> T requestSnapshot(RemotePeer target) {
            TestSnapshotMessage msg = new TestSnapshotMessage();
            msg.setValue(value);
            msg.setReliable(true);
            return (T) msg;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void setAuthoritative(boolean authoritative) {
            this.authoritative = authoritative;
        }
    }

    private static final class TestManager extends NetcodeManagerComponent {
        private int sentCount;

        @Override
        public void sendMessageToPeer(RemotePeer peer, com.jme3.network.Message message, int channel, boolean reliable) {
            sentCount++;
        }
    }

    private TestManager manager;
    private TestHandler handler;

    @Before
    public void setUp() throws Exception {
        manager = new TestManager();
        handler = new TestHandler();
        manager.registerActionHandler(handler);
        setField(manager, "connectedPeers", Collections.singletonList((RemotePeer) null));
        setField(manager, "connectedPeersRO", Collections.unmodifiableList(Collections.singletonList((RemotePeer) null)));
    }

    @Test
    public void managerSendsSnapshotOnEveryTickWhenAuthoritative() {
        handler.setValue(10);
        manager.updateAppLogic(null, 0f);
        assertEquals(1, manager.sentCount);

        manager.updateAppLogic(null, 0f);
        assertEquals(2, manager.sentCount);

        handler.setValue(11);
        manager.updateAppLogic(null, 0f);
        assertEquals(3, manager.sentCount);
    }

    @Test
    public void managerSkipsSnapshotsWithoutAuthorityAndSendsImmediatelyWhenAuthorityReturns() {
        handler.setAuthoritative(false);
        manager.updateAppLogic(null, 0f);
        manager.updateAppLogic(null, 0f);
        assertEquals(0, manager.sentCount);

        handler.setAuthoritative(true);
        manager.updateAppLogic(null, 0f);
        assertEquals(1, manager.sentCount);
    }

    private static void setField(Object target, String field, Object value) throws Exception {
        Field f = findField(target.getClass(), field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Field findField(Class<?> type, String name) throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
