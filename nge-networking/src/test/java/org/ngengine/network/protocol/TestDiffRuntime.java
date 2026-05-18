package org.ngengine.network.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.jme3.network.AbstractMessage;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;
import org.junit.Test;

public class TestDiffRuntime {

    @NetworkSafe
    public static class RuntimeProbeMessage extends AbstractMessage implements DiffableMessage {
        public String componentId;
        public int value;

        public RuntimeProbeMessage() {
        }

        public RuntimeProbeMessage(String componentId, int value) {
            this.componentId = componentId;
            this.value = value;
        }

        @Override
        public long getDiffGroup() {
            byte[] bytes = componentId.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            long hash = 0xcbf29ce484222325L;
            for (byte b : bytes) {
                hash ^= (b & 0xff);
                hash *= 0x100000001b3L;
            }
            return hash;
        }
    }

    @Test
    public void reliableUnreliablePromotionRulesHold() throws Exception {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, id -> {}, 0);
        RuntimeProbeMessage base = new RuntimeProbeMessage("group-A", 1);
        sender.toByteBuffer(base, null);

        Object groupState = senderGroupState(sender, base.getDiffGroup());
        long reliableBaseBefore = longField(groupState, "reliableBasePacketId");

        RuntimeProbeMessage unrel = new RuntimeProbeMessage("group-A", 2);
        unrel.setReliable(false);
        sender.toByteBuffer(unrel, null);
        long reliableBaseAfterUnrel = longField(groupState, "reliableBasePacketId");
        assertEquals(reliableBaseBefore, reliableBaseAfterUnrel);

        RuntimeProbeMessage reliable = new RuntimeProbeMessage("group-A", 3);
        sender.toByteBuffer(reliable, null);
        long reliableBaseAfterReliable = longField(groupState, "reliableBasePacketId");
        assertTrue(reliableBaseAfterReliable > reliableBaseAfterUnrel);
    }

    @Test
    public void senderHistoryIsCappedAndKeepsBase() throws Exception {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, id -> {}, 0);
        RuntimeProbeMessage msg = new RuntimeProbeMessage("group-B", 0);
        sender.toByteBuffer(msg, null);

        for (int i = 1; i <= 120; i++) {
            RuntimeProbeMessage next = new RuntimeProbeMessage("group-B", i);
            sender.toByteBuffer(next, null);
        }

        Object groupState = senderGroupState(sender, msg.getDiffGroup());
        @SuppressWarnings("unchecked")
        Map<Long, ?> history = (Map<Long, ?>) objectField(groupState, "reliableHistory");
        long basePacketId = longField(groupState, "reliableBasePacketId");
        assertTrue("history cap must be enforced", history.size() <= 32);
        assertTrue("current base must be retained while group is alive", history.containsKey(basePacketId));
    }

    @Test
    public void senderGroupEvictionRemovesIdleGroup() throws Exception {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, id -> {}, 0);
        RuntimeProbeMessage a0 = new RuntimeProbeMessage("group-C", 1);
        sender.toByteBuffer(a0, null);
        Object aState = senderGroupState(sender, a0.getDiffGroup());
        setLongField(aState, "lastTouchedAt", System.currentTimeMillis() - 20_000L);

        RuntimeProbeMessage b0 = new RuntimeProbeMessage("group-D", 1);
        sender.toByteBuffer(b0, null); // triggers sender eviction pass

        Map<?, ?> senderGroups = senderGroupsMap(sender);
        assertFalse(senderGroups.containsKey(a0.getDiffGroup()));
        assertTrue(senderGroups.containsKey(b0.getDiffGroup()));
    }

    @Test
    public void receiverGroupEvictionRemovesIdleGroup() throws Exception {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, id -> {}, 0);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, id -> {}, 0);

        RuntimeProbeMessage a0 = new RuntimeProbeMessage("group-E", 1);
        receiver.toMessage(sender.toByteBuffer(a0, null).duplicate());

        Object aState = receiverGroupState(receiver, a0.getDiffGroup());
        setLongField(aState, "lastTouchedAt", System.currentTimeMillis() - 40_000L);

        RuntimeProbeMessage b0 = new RuntimeProbeMessage("group-F", 1);
        receiver.toMessage(sender.toByteBuffer(b0, null).duplicate()); // triggers receiver eviction pass

        Map<?, ?> receiverGroups = receiverGroupsMap(receiver);
        assertFalse(receiverGroups.containsKey(a0.getDiffGroup()));
        assertTrue(receiverGroups.containsKey(b0.getDiffGroup()));
    }

    @Test
    public void runtimeHeaderUsesVarintsAndCarriesReferences() {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, id -> {}, 0);
        RuntimeProbeMessage first = new RuntimeProbeMessage("group-G", 1);
        ByteBuffer full = sender.toByteBuffer(first, null);
        RuntimeHeader fullHeader = parseRuntimeHeader(full);
        assertEquals(DiffRuntime.MODE_FULL, fullHeader.mode);
        assertEquals(DiffRuntime.LANE_RELIABLE, fullHeader.lane);
        assertEquals(1L, fullHeader.packetId);

        RuntimeProbeMessage second = new RuntimeProbeMessage("group-G", 2);
        ByteBuffer diff = sender.toByteBuffer(second, null);
        RuntimeHeader diffHeader = parseRuntimeHeader(diff);
        assertEquals(DiffRuntime.MODE_DIFF, diffHeader.mode);
        assertEquals(Long.valueOf(1L), diffHeader.basePacketId);
        assertTrue(diffHeader.packetId > diffHeader.basePacketId.longValue());
    }

    private static final class RuntimeHeader {
        private final long mode;
        private final long lane;
        private final long packetId;
        private final Long basePacketId;

        private RuntimeHeader(long mode, long lane, long packetId, Long basePacketId) {
            this.mode = mode;
            this.lane = lane;
            this.packetId = packetId;
            this.basePacketId = basePacketId;
        }
    }

    private static RuntimeHeader parseRuntimeHeader(ByteBuffer packet) {
        ByteBuffer body = body(packet);
        long marker = VarInt.decodeUnsigned(body);
        assertEquals(DiffRuntime.DIFF_RUNTIME_MARKER, marker);
        long mode = VarInt.decodeUnsigned(body);
        long lane = VarInt.decodeUnsigned(body);
        VarInt.decodeSigned(body); // group
        long packetId = VarInt.decodeUnsigned(body);
        Long basePacketId = mode == DiffRuntime.MODE_DIFF ? Long.valueOf(VarInt.decodeUnsigned(body)) : null;
        return new RuntimeHeader(mode, lane, packetId, basePacketId);
    }

    private static ByteBuffer body(ByteBuffer packet) {
        ByteBuffer bb = packet.duplicate();
        long classPathLength = VarInt.decodeSigned(bb);
        if (classPathLength > 0) {
            bb.position(bb.position() + (int) classPathLength);
        }
        VarInt.decodeSigned(bb); // class id
        long bodyLength = (long) bb.getInt() & 0xFFFFFFFFL;
        ByteBuffer body = bb.slice();
        body.limit((int) bodyLength);
        return body;
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Object> senderGroupsMap(DynamicSerializerProtocol protocol) throws Exception {
        Object runtime = objectField(protocol, "diffRuntime");
        return (Map<Long, Object>) objectField(runtime, "senderGroups");
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Object> receiverGroupsMap(DynamicSerializerProtocol protocol) throws Exception {
        Object runtime = objectField(protocol, "diffRuntime");
        return (Map<Long, Object>) objectField(runtime, "receiverGroups");
    }

    private static Object senderGroupState(DynamicSerializerProtocol protocol, long group) throws Exception {
        return senderGroupsMap(protocol).get(group);
    }

    private static Object receiverGroupState(DynamicSerializerProtocol protocol, long group) throws Exception {
        return receiverGroupsMap(protocol).get(group);
    }

    private static Object objectField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static long longField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.getLong(target);
    }

    private static void setLongField(Object target, String name, long value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.setLong(target, value);
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
