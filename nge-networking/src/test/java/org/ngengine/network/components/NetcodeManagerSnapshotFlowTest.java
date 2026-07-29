package org.ngengine.network.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngengine.components.actions.ComponentAction;
import org.ngengine.components.actions.ComponentActionFilter;
import org.ngengine.network.RemotePeer;
import org.ngengine.network.protocol.NetworkSafe;

import com.jme3.network.Message;

public class NetcodeManagerSnapshotFlowTest {

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

    @NetworkSafe
    public static class TestAuthorityActionMessage extends ActionMessage {
    }

    @NetworkSafe
    public static class TestRemoteAuthorityActionMessage extends ActionMessage {
    }

    private static final class TestHandler implements NetcodeFragment {
        private int value;
        private int authorityActionCount;
        private int remoteAuthorityActionCount;
        private final BigInteger id = BigInteger.valueOf(42);
        private boolean authoritative = true;
        private boolean remoteAuthoritative;

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
        public boolean checkAuthority(RemotePeer peer) {
            return remoteAuthoritative;
        }

        @ComponentAction(
            type = TestAuthorityActionMessage.class,
            filter = ComponentActionFilter.REMOTE | ComponentActionFilter.LOCAL_PEER_HAS_AUTHORITY
        )
        private void onAuthorityAction(TestAuthorityActionMessage action) {
            authorityActionCount++;
        }

        @ComponentAction(
            type = TestRemoteAuthorityActionMessage.class,
            filter = ComponentActionFilter.REMOTE | ComponentActionFilter.REMOTE_PEER_HAS_AUTHORITY
        )
        private void onRemoteAuthorityAction(TestRemoteAuthorityActionMessage action) {
            remoteAuthorityActionCount++;
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

    @BeforeEach
    public void setUp() throws Exception {
        manager = new TestManager();
        handler = new TestHandler();
        manager.registerActionHandler(handler);
        List<RemotePeer> replicas = Arrays.asList(
            (RemotePeer) null,
            (RemotePeer) null,
            (RemotePeer) null
        );
        setField(manager, "connectedPeers", replicas);
        setField(manager, "connectedPeersRO", Collections.unmodifiableList(replicas));
    }

    @Test
    public void managerSendsSnapshotToAllThreeReplicasOnEveryAuthoritativeTick() {
        handler.setValue(10);
        manager.updateAppLogic(null, 0f);
        assertEquals(3, manager.sentCount);

        manager.updateAppLogic(null, 0f);
        assertEquals(6, manager.sentCount);

        handler.setValue(11);
        manager.updateAppLogic(null, 0f);
        assertEquals(9, manager.sentCount);
    }

    @Test
    public void managerSkipsSnapshotsWithoutAuthorityAndSendsImmediatelyWhenAuthorityReturns() {
        handler.setAuthoritative(false);
        manager.updateAppLogic(null, 0f);
        manager.updateAppLogic(null, 0f);
        assertEquals(0, manager.sentCount);

        handler.setAuthoritative(true);
        manager.updateAppLogic(null, 0f);
        assertEquals(3, manager.sentCount);
    }

    @Test
    public void remoteNonAuthorityRequestExecutesOnTheLocalAuthority() throws Exception {
        TestAuthorityActionMessage action = new TestAuthorityActionMessage();
        action.setComponentId(handler.getComponentId());
        action.setNetworkId(handler.getNetworkId());
        enqueueInbound(manager, action);

        manager.updateAppLogic(null, 0f);

        assertEquals(1, handler.authorityActionCount);
    }

    @Test
    public void localAuthorityConstraintRejectsOnNonAuthorityReplica() throws Exception {
        TestAuthorityActionMessage action = new TestAuthorityActionMessage();
        action.setComponentId(handler.getComponentId());
        action.setNetworkId(handler.getNetworkId());
        handler.authoritative = false;

        enqueueInbound(manager, action);
        manager.updateAppLogic(null, 0f);

        assertEquals(0, handler.authorityActionCount);
    }

    @Test
    public void remoteAuthorityConstraintRejectsAndThenAcceptsBySenderAuthority() throws Exception {
        TestRemoteAuthorityActionMessage action = new TestRemoteAuthorityActionMessage();
        action.setComponentId(handler.getComponentId());
        action.setNetworkId(handler.getNetworkId());
        handler.authoritative = false;

        enqueueInbound(manager, action);
        manager.updateAppLogic(null, 0f);
        assertEquals(0, handler.remoteAuthorityActionCount);

        handler.remoteAuthoritative = true;
        enqueueInbound(manager, action);
        manager.updateAppLogic(null, 0f);
        assertEquals(1, handler.remoteAuthorityActionCount);
    }

    @SuppressWarnings("unchecked")
    private static void enqueueInbound(NetcodeManagerComponent manager, Message message) throws Exception {
        Class<?> inboundType = Arrays.stream(NetcodeManagerComponent.class.getDeclaredClasses())
            .filter(type -> "InboundMessage".equals(type.getSimpleName()))
            .findFirst()
            .orElseThrow();
        Constructor<?> constructor = inboundType.getDeclaredConstructor(RemotePeer.class, Message.class);
        constructor.setAccessible(true);
        Object inbound = constructor.newInstance(null, message);
        Field field = findField(manager.getClass(), "inboundMessages");
        field.setAccessible(true);
        ((ArrayDeque<Object>) field.get(manager)).addLast(inbound);
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
