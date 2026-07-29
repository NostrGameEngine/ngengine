package org.ngengine.network.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;

public class TestDynamicSerializerProtocolDiff {

    private static final class MutableClockProtocol extends DynamicSerializerProtocol {
        private long nowMillis = 1_000L;

        private MutableClockProtocol() {
            super(true, id -> {}, 0, true);
        }

        @Override
        long nowMillis() {
            return nowMillis;
        }

        private void advance(long millis) {
            nowMillis += millis;
        }
    }

    @NetworkSafe
    public static class DiffableStateMessage extends AbstractMessage implements DiffableMessage {
        public String componentId;
        public String mapId;
        public String label;
        public int value;

        public DiffableStateMessage() {
        }

        public DiffableStateMessage(String componentId, String mapId, String label, int value) {
            this.componentId = componentId;
            this.mapId = mapId;
            this.label = label;
            this.value = value;
        }

        @Override
        public long getDiffGroup() {
            return hashGroup(componentId + "|" + mapId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(componentId, mapId, label, Integer.valueOf(value));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DiffableStateMessage)) return false;
            DiffableStateMessage other = (DiffableStateMessage) obj;
            return value == other.value
                && Objects.equals(componentId, other.componentId)
                && Objects.equals(mapId, other.mapId)
                && Objects.equals(label, other.label);
        }
    }

    @NetworkSafe
    public static class DiffableNestedMessage extends AbstractMessage implements DiffableMessage {
        public String componentId;
        public String mapId;
        public int hp;
        public List<Integer> values;

        public DiffableNestedMessage() {
        }

        public DiffableNestedMessage(String componentId, String mapId, int hp, List<Integer> values) {
            this.componentId = componentId;
            this.mapId = mapId;
            this.hp = hp;
            this.values = values;
        }

        @Override
        public long getDiffGroup() {
            return hashGroup(componentId + "|" + mapId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(componentId, mapId, Integer.valueOf(hp), values);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DiffableNestedMessage)) return false;
            DiffableNestedMessage other = (DiffableNestedMessage) obj;
            return hp == other.hp
                && Objects.equals(componentId, other.componentId)
                && Objects.equals(mapId, other.mapId)
                && Objects.equals(values, other.values);
        }
    }

    @Serializable
    public static class PlainMessage extends AbstractMessage {
        public int hp;
        public String name;

        public PlainMessage() {
        }

        public PlainMessage(int hp, String name) {
            this.hp = hp;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Integer.valueOf(hp), name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PlainMessage)) return false;
            PlainMessage other = (PlainMessage) obj;
            return hp == other.hp && Objects.equals(name, other.name);
        }
    }

    private DynamicSerializerProtocol sender;
    private DynamicSerializerProtocol receiver;

    private static final class RuntimeHeader {
        private final long marker;
        private final long mode;
        private final long lane;
        private final long group;
        private final long packetId;
        private final Long basePacketId;

        private RuntimeHeader(long marker, long mode, long lane, long group, long packetId, Long basePacketId) {
            this.marker = marker;
            this.mode = mode;
            this.lane = lane;
            this.group = group;
            this.packetId = packetId;
            this.basePacketId = basePacketId;
        }
    }

    @Before
    public void setUp() {
        sender = new DynamicSerializerProtocol(true, id -> {}, 0);
        receiver = new DynamicSerializerProtocol(true, id -> {}, 0);
    }

    @Test
    public void nonDiffableStillUsesRegularFullBodyMode() {
        PlainMessage original = new PlainMessage(10, "alpha");
        ByteBuffer packet = sender.toByteBuffer(original, null);
        assertEquals(0L, firstBodyVarint(packet));
        PlainMessage decoded = (PlainMessage) receiver.toMessage(packet.duplicate());
        assertEquals(original, decoded);
    }

    @Test
    public void firstDiffableSendUsesRuntimeFull() {
        DiffableStateMessage msg = new DiffableStateMessage("cmp", "mapA", "a", 1);
        RuntimeHeader header = parseRuntimeHeader(sender.toByteBuffer(msg, null));
        assertEquals(DiffRuntime.DIFF_RUNTIME_MARKER, header.marker);
        assertEquals(DiffRuntime.MODE_FULL, header.mode);
        assertEquals(DiffRuntime.LANE_RELIABLE, header.lane);
        assertEquals(1L, header.packetId);
    }

    @Test
    public void secondReliableChangedSendUsesRuntimeDiff() {
        DiffableStateMessage first = new DiffableStateMessage("cmp", "mapA", "a", 10);
        ByteBuffer full = sender.toByteBuffer(first, null);
        receiver.toMessage(full.duplicate());

        DiffableStateMessage changed = new DiffableStateMessage("cmp", "mapA", "a", 11);
        ByteBuffer diff = sender.toByteBuffer(changed, null);
        RuntimeHeader header = parseRuntimeHeader(diff);
        assertEquals(DiffRuntime.MODE_DIFF, header.mode);
        assertEquals(DiffRuntime.LANE_RELIABLE, header.lane);
        assertEquals(Long.valueOf(1L), header.basePacketId);

        DiffableStateMessage decoded = (DiffableStateMessage) receiver.toMessage(diff.duplicate());
        assertEquals(changed, decoded);
    }

    @Test
    public void unchangedDiffablePacketIsSkipped() {
        DiffableStateMessage first = new DiffableStateMessage("cmp", "mapA", "same", 10);
        ByteBuffer a = sender.toByteBuffer(first, null);
        assertNotNull(a);

        DiffableStateMessage same = new DiffableStateMessage("cmp", "mapA", "same", 10);
        ByteBuffer b = sender.toByteBuffer(same, null);
        assertNotNull(b);
        assertFalse(b.hasRemaining());
    }

    @Test
    public void unchangedReliableStateRecoversAfterApplicationRejectsFirstFull() {
        MutableClockProtocol recoveringSender = new MutableClockProtocol();
        DynamicSerializerProtocol transportReceiver =
            new DynamicSerializerProtocol(true, id -> {}, 0, true);
        DiffableStateMessage authoritative =
            new DiffableStateMessage("coop-relay", "mapA", "players", 4);

        ByteBuffer rejectedByApplication = recoveringSender.toByteBuffer(authoritative, null);
        RuntimeHeader firstHeader = parseRuntimeHeader(rejectedByApplication);
        assertEquals(DiffRuntime.MODE_FULL, firstHeader.mode);
        DiffableStateMessage decodedButRejected =
            (DiffableStateMessage) transportReceiver.toMessage(rejectedByApplication.duplicate());
        assertEquals(authoritative, decodedButRejected);
        // The transport decoded and cached the FULL, but the application-level
        // authority gate intentionally rejects it during peer-set churn.

        ByteBuffer immediateRetry = recoveringSender.toByteBuffer(
            new DiffableStateMessage("coop-relay", "mapA", "players", 4),
            null
        );
        assertFalse("unchanged snapshots stay suppressed before the recovery TTL", immediateRetry.hasRemaining());

        recoveringSender.advance(DiffRuntime.RELIABLE_FULL_RECOVERY_INTERVAL_MS + 1L);
        ByteBuffer recovery = recoveringSender.toByteBuffer(
            new DiffableStateMessage("coop-relay", "mapA", "players", 4),
            null
        );
        RuntimeHeader recoveryHeader = parseRuntimeHeader(recovery);
        assertEquals(DiffRuntime.MODE_FULL, recoveryHeader.mode);
        assertTrue(recoveryHeader.packetId > firstHeader.packetId);

        DiffableStateMessage acceptedAfterMembershipConverges =
            (DiffableStateMessage) transportReceiver.toMessage(recovery.duplicate());
        assertEquals(authoritative, acceptedAfterMembershipConverges);
    }

    @Test
    public void unreliableWithoutReliableBaseBypassesRuntime() {
        DiffableStateMessage first = new DiffableStateMessage("cmp", "mapA", "u0", 10);
        first.setReliable(false);
        ByteBuffer packet = sender.toByteBuffer(first, null);
        assertEquals(0L, firstBodyVarint(packet));
        DiffableStateMessage decoded = (DiffableStateMessage) receiver.toMessage(packet.duplicate());
        assertEquals(first, decoded);
    }

    @Test
    public void unreliableWithReliableBaseUsesRuntimeDiff() {
        DiffableStateMessage base = new DiffableStateMessage("cmp", "mapA", "base", 10);
        sender.toByteBuffer(base, null);

        DiffableStateMessage unrel = new DiffableStateMessage("cmp", "mapA", "base", 11);
        unrel.setReliable(false);
        ByteBuffer packet = sender.toByteBuffer(unrel, null);
        RuntimeHeader header = parseRuntimeHeader(packet);
        assertEquals(DiffRuntime.MODE_DIFF, header.mode);
        assertEquals(DiffRuntime.LANE_UNRELIABLE, header.lane);
        assertEquals(Long.valueOf(1L), header.basePacketId);
    }

    @Test
    public void missingBaseDropsUnreliableDiffWithoutStall() {
        DynamicSerializerProtocol isolatedReceiver = new DynamicSerializerProtocol(true, id -> {}, 0);
        DiffableStateMessage base = new DiffableStateMessage("cmp", "mapA", "a", 10);
        sender.toByteBuffer(base, null); // sender base only

        DiffableStateMessage unrel = new DiffableStateMessage("cmp", "mapA", "a", 11);
        unrel.setReliable(false);
        ByteBuffer packet = sender.toByteBuffer(unrel, null);
        assertNull(isolatedReceiver.toMessage(packet.duplicate()));
    }

    @Test(expected = RuntimeException.class)
    public void missingBaseFailsReliableDiff() {
        DynamicSerializerProtocol isolatedReceiver = new DynamicSerializerProtocol(true, id -> {}, 0);

        DiffableStateMessage first = new DiffableStateMessage("cmp", "mapA", "a", 10);
        sender.toByteBuffer(first, null); // do not deliver to isolated receiver

        DiffableStateMessage second = new DiffableStateMessage("cmp", "mapA", "a", 11);
        ByteBuffer diff = sender.toByteBuffer(second, null);
        isolatedReceiver.toMessage(diff.duplicate());
    }

    @Test
    public void recoverableReliableStreamSelfRecoversWithCheckpointFull() {
        DynamicSerializerProtocol recoveringSender = new DynamicSerializerProtocol(true, id -> {}, 0, true);
        DynamicSerializerProtocol recoveringReceiver = new DynamicSerializerProtocol(true, id -> {}, 0, true);

        DiffableStateMessage base = new DiffableStateMessage("cmp", "mapA", "a", 10);
        recoveringReceiver.toMessage(recoveringSender.toByteBuffer(base, null).duplicate());

        DiffableStateMessage dropped = new DiffableStateMessage("cmp", "mapA", "a", 11);
        ByteBuffer droppedPacket = recoveringSender.toByteBuffer(dropped, null);
        RuntimeHeader droppedHeader = parseRuntimeHeader(droppedPacket);
        assertEquals(DiffRuntime.MODE_DIFF, droppedHeader.mode);
        assertEquals(DiffRuntime.LANE_RELIABLE, droppedHeader.lane);
        // Packet intentionally dropped to create a missing base on receiver.

        DiffableStateMessage missingBase = new DiffableStateMessage("cmp", "mapA", "a", 12);
        ByteBuffer missingBasePacket = recoveringSender.toByteBuffer(missingBase, null);
        try {
            recoveringReceiver.toMessage(missingBasePacket.duplicate());
            fail("Reliable diff should fail when base packet is missing");
        } catch (RuntimeException expected) {
            // expected
        }

        boolean recoveredByFull = false;
        for (int value = 13; value < 80; value++) {
            DiffableStateMessage next = new DiffableStateMessage("cmp", "mapA", "a", value);
            ByteBuffer packet = recoveringSender.toByteBuffer(next, null);
            RuntimeHeader header = parseRuntimeHeader(packet);
            try {
                DiffableStateMessage decoded = (DiffableStateMessage) recoveringReceiver.toMessage(packet.duplicate());
                if (header.mode == DiffRuntime.MODE_FULL) {
                    assertEquals(next, decoded);
                    recoveredByFull = true;
                    break;
                }
            } catch (RuntimeException ignored) {
                // Continue until sender emits the checkpoint FULL.
            }
        }

        assertTrue("Recoverable mode must eventually send a FULL checkpoint and recover", recoveredByFull);

        DiffableStateMessage afterRecovery = new DiffableStateMessage("cmp", "mapA", "a", 81);
        DiffableStateMessage decodedAfterRecovery = (DiffableStateMessage) recoveringReceiver.toMessage(
            recoveringSender.toByteBuffer(afterRecovery, null).duplicate()
        );
        assertEquals(afterRecovery, decodedAfterRecovery);
    }

    @Test
    public void nonRecoverableReliableStreamDoesNotSelfRecoverAfterMissingBase() {
        DynamicSerializerProtocol noRecoverySender = new DynamicSerializerProtocol(true, id -> {}, 0, false);
        DynamicSerializerProtocol noRecoveryReceiver = new DynamicSerializerProtocol(true, id -> {}, 0, false);

        DiffableStateMessage base = new DiffableStateMessage("cmp", "mapA", "a", 10);
        noRecoveryReceiver.toMessage(noRecoverySender.toByteBuffer(base, null).duplicate());

        DiffableStateMessage dropped = new DiffableStateMessage("cmp", "mapA", "a", 11);
        noRecoverySender.toByteBuffer(dropped, null); // intentionally dropped

        boolean sawRuntimeFull = false;
        int decodeFailures = 0;
        for (int value = 12; value < 80; value++) {
            DiffableStateMessage next = new DiffableStateMessage("cmp", "mapA", "a", value);
            ByteBuffer packet = noRecoverySender.toByteBuffer(next, null);
            RuntimeHeader header = parseRuntimeHeader(packet);
            if (header.mode == DiffRuntime.MODE_FULL) {
                sawRuntimeFull = true;
            }
            try {
                noRecoveryReceiver.toMessage(packet.duplicate());
                fail("Non-recoverable mode should not self-heal after base loss");
            } catch (RuntimeException expected) {
                decodeFailures++;
            }
        }

        assertFalse("Non-recoverable mode should not inject FULL recovery checkpoints", sawRuntimeFull);
        assertTrue("Non-recoverable mode should keep failing after base loss", decodeFailures > 0);
    }

    @Test
    public void droppedUnreliableDiffDoesNotBlockFollowingReliableDiff() {
        DiffableStateMessage base = new DiffableStateMessage("cmp", "mapA", "a", 10);
        ByteBuffer basePacket = sender.toByteBuffer(base, null);
        receiver.toMessage(basePacket.duplicate());

        DiffableStateMessage unrel = new DiffableStateMessage("cmp", "mapA", "a", 11);
        unrel.setReliable(false);
        sender.toByteBuffer(unrel, null); // intentionally dropped

        DiffableStateMessage reliable = new DiffableStateMessage("cmp", "mapA", "a", 12);
        ByteBuffer reliablePacket = sender.toByteBuffer(reliable, null);
        DiffableStateMessage decoded = (DiffableStateMessage) receiver.toMessage(reliablePacket.duplicate());
        assertEquals(reliable, decoded);
    }

    @Test
    public void groupInterleavingKeepsStreamsIndependent() {
        DiffableStateMessage a0 = new DiffableStateMessage("cmpA", "map", "a0", 0);
        DiffableStateMessage b0 = new DiffableStateMessage("cmpB", "map", "b0", 0);
        receiver.toMessage(sender.toByteBuffer(a0, null).duplicate());
        receiver.toMessage(sender.toByteBuffer(b0, null).duplicate());

        DiffableStateMessage a1 = new DiffableStateMessage("cmpA", "map", "a1", 1);
        DiffableStateMessage b1 = new DiffableStateMessage("cmpB", "map", "b1", 1);
        DiffableStateMessage decodedA1 = (DiffableStateMessage) receiver.toMessage(sender.toByteBuffer(a1, null).duplicate());
        DiffableStateMessage decodedB1 = (DiffableStateMessage) receiver.toMessage(sender.toByteBuffer(b1, null).duplicate());
        assertEquals(a1, decodedA1);
        assertEquals(b1, decodedB1);
    }

    @Test
    public void diffApplyDoesNotShareReferencesWithPrevious() {
        DiffableNestedMessage first = new DiffableNestedMessage("cmp", "mapA", 10, new ArrayList<>(Arrays.asList(1, 2)));
        ByteBuffer firstPacket = sender.toByteBuffer(first, null);
        DiffableNestedMessage decodedFirst = (DiffableNestedMessage) receiver.toMessage(firstPacket.duplicate());

        DiffableNestedMessage second = new DiffableNestedMessage("cmp", "mapA", 11, new ArrayList<>(Arrays.asList(1, 2)));
        ByteBuffer secondPacket = sender.toByteBuffer(second, null);
        DiffableNestedMessage decodedSecond = (DiffableNestedMessage) receiver.toMessage(secondPacket.duplicate());
        decodedSecond.values.add(3);

        assertEquals(2, decodedFirst.values.size());
        assertEquals(3, decodedSecond.values.size());
    }

    @Test
    public void classRegistrationRemainsCompatibleAcrossProtocols() {
        DynamicSerializerProtocol senderA = new DynamicSerializerProtocol(true, id -> {}, 0);
        DynamicSerializerProtocol receiverB = new DynamicSerializerProtocol(true, id -> {}, 0);

        DiffableStateMessage first = new DiffableStateMessage("cmp", "mapA", "a", 10);
        receiverB.toMessage(senderA.toByteBuffer(first, null).duplicate());

        DiffableStateMessage second = new DiffableStateMessage("cmp", "mapA", "b", 20);
        DiffableStateMessage decoded = (DiffableStateMessage) receiverB.toMessage(senderA.toByteBuffer(second, null).duplicate());
        assertEquals(second, decoded);
    }

    private static long firstBodyVarint(ByteBuffer packet) {
        ByteBuffer body = body(packet);
        return VarInt.decodeUnsigned(body);
    }

    private static RuntimeHeader parseRuntimeHeader(ByteBuffer packet) {
        ByteBuffer body = body(packet);
        long marker = VarInt.decodeUnsigned(body);
        assertEquals(DiffRuntime.DIFF_RUNTIME_MARKER, marker);
        long mode = VarInt.decodeUnsigned(body);
        long lane = VarInt.decodeUnsigned(body);
        long group = VarInt.decodeSigned(body);
        long packetId = VarInt.decodeUnsigned(body);
        Long base = mode == DiffRuntime.MODE_DIFF ? Long.valueOf(VarInt.decodeUnsigned(body)) : null;
        return new RuntimeHeader(marker, mode, lane, group, packetId, base);
    }

    private static ByteBuffer body(ByteBuffer packet) {
        ByteBuffer bb = packet.duplicate();
        long classPathLength = VarInt.decodeSigned(bb);
        assertTrue("packet must be non-null envelope", classPathLength != -1);
        if (classPathLength > 0) {
            bb.position(bb.position() + (int) classPathLength);
        }
        VarInt.decodeSigned(bb); // class id
        long bodyLength = (long) bb.getInt() & 0xFFFFFFFFL;
        assertTrue("invalid body length", bodyLength <= bb.remaining());
        ByteBuffer body = bb.slice();
        body.limit((int) bodyLength);
        return body;
    }

    private static long hashGroup(String key) {
        byte[] bytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long hash = 0xcbf29ce484222325L;
        for (byte b : bytes) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
