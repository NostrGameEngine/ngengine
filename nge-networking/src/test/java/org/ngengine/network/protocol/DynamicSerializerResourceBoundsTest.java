package org.ngengine.network.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.network.AbstractMessage;
import com.jme3.network.base.MessageBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;
import org.ngengine.network.protocol.messages.CompressedMessage;
import org.ngengine.network.protocol.serializers.CollectionSerializer;
import org.ngengine.network.protocol.serializers.MapSerializer;
import org.ngengine.network.protocol.serializers.StringSerializer;

/** Source specifications; execution against the locked public96 providers belongs to Root. */
final class DynamicSerializerResourceBoundsTest {
    @NetworkSafe
    public static class StateMessage extends AbstractMessage implements DiffableMessage {
        public long group;
        public String label;
        public int value;
        public StateMessage() {}
        public StateMessage(long group, int value) { this.group = group; this.value = value; this.label = "state"; }
        public long getDiffGroup() { return group; }
    }

    @NetworkSafe
    public static class ContainerMessage extends AbstractMessage {
        public Object content;
        public ContainerMessage() {}
    }

    @NetworkSafe
    public static class EmptyMessage extends AbstractMessage { public EmptyMessage() {} }

    @Test void outboundCapacityAndSerializationFailuresHaveFixedDiagnosticsAndRetireOnlyOnce() {
        ContainerMessage tooLarge = new ContainerMessage();
        tooLarge.content = new int[DynamicSerializerProtocol.MAX_COLLECTION_ITEMS + 1];
        ContainerMessage unsupported = new ContainerMessage(); unsupported.content = new Object();
        ContainerMessage[] messages = {tooLarge, unsupported};
        DynamicSerializerProtocol.OutboundFailure[] expected = {
            DynamicSerializerProtocol.OutboundFailure.CAPACITY, DynamicSerializerProtocol.OutboundFailure.SERIALIZATION
        };
        for (int index = 0; index < messages.length; index++) {
            DynamicSerializerProtocol protocol = new DynamicSerializerProtocol(true, ignored -> {}, 1);
            List<DynamicSerializerProtocol.OutboundFailure> reasons = new ArrayList<>();
            List<String> types = new ArrayList<>();
            try {
                protocol.setDetailedResourceFailureHandler((reason, type) -> {
                    reasons.add(reason); types.add(type);
                    assertThrows(RuntimeException.class, () -> protocol.toByteBuffer(new EmptyMessage(), null),
                            "the protocol fails closed before publishing its reason");
                });
                ContainerMessage message = messages[index];
                assertThrows(RuntimeException.class, () -> protocol.toByteBuffer(message, null));
                assertEquals(List.of(expected[index]), reasons);
                assertTrue(types.get(0).matches("[A-Za-z0-9_$]{1,80}"));
                assertThrows(RuntimeException.class, () -> protocol.toByteBuffer(new EmptyMessage(), null));
                assertEquals(1, reasons.size());
            } finally { protocol.retire(); }
        }
    }

    private static final class ClockProtocol extends DynamicSerializerProtocol {
        private long time = 1000;
        private ClockProtocol() { super(true, ignored -> {}, 1); }
        long nowMillis() { return time; }
    }

    @Test void actualProviderStreamPrefixSupportsSplitHeadersAndCoalescedFrames() {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            ByteBuffer frame = sender.toByteBuffer(new EmptyMessage(), null);
            ByteBuffer wire = ByteBuffer.allocate(2 * (frame.remaining() + 2));
            wire.putShort((short) frame.remaining()).put(frame.duplicate());
            wire.putShort((short) frame.remaining()).put(frame.duplicate()).flip();
            MessageBuffer stream = receiver.createBuffer();
            ByteBuffer firstByte = wire.slice(); firstByte.limit(1);
            assertFalse(stream.addBytes(firstByte)); wire.position(1);
            assertTrue(stream.addBytes(wire));
            assertInstanceOf(EmptyMessage.class, stream.pollMessage());
            assertInstanceOf(EmptyMessage.class, stream.pollMessage());
            assertFalse(stream.hasMessages());
        } finally { sender.retire(); receiver.retire(); }
    }

    @Test void streamFloodFailsClosedBeforeAnotherFrameAllocation() {
        DynamicSerializerProtocol protocol = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        try {
            MessageBuffer stream = protocol.createBuffer();
            ByteBuffer flood = ByteBuffer.allocate(129 * 3);
            for (int index = 0; index < 129; index++) flood.putShort((short) 1).put((byte) 1);
            flood.flip();
            assertThrows(IllegalArgumentException.class, () -> stream.addBytes(flood));
            assertFalse(stream.hasMessages());
            assertThrows(IllegalStateException.class, stream::pollMessage);
        } finally { protocol.retire(); }
    }

    @Test void hostileLengthsCannotWrapIntoSmallAllocationsOrInvokeElementDecoders() throws Exception {
        long[] lengths = {4097, Integer.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
        CollectionSerializer collection = new CollectionSerializer((object, output) -> null,
                (input, type) -> { fail("Hostile collection must fail before element decode"); return null; });
        MapSerializer map = new MapSerializer((object, output) -> null,
                (input, type) -> { fail("Hostile map must fail before entry decode"); return null; });
        for (long length : lengths) {
            ByteBuffer encoded = ByteBuffer.allocate(16);
            if (length < 0) encoded.put(new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                    (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 1});
            else VarInt.encodeUnsigned(length, encoded);
            encoded.flip();
            assertThrows(Exception.class, () -> collection.readObject(encoded.duplicate(), ArrayList.class));
            assertThrows(Exception.class, () -> map.readObject(encoded.duplicate(), java.util.HashMap.class));
            assertThrows(Exception.class, () -> new StringSerializer().readObject(encoded.duplicate(), String.class));
        }
    }

    @Test void oversizedBodyAndClassPathFailBeforeClassRegistrationCallback() {
        int[] registered = {0};
        DynamicSerializerProtocol protocol = new DynamicSerializerProtocol(true, ignored -> registered[0]++, 1);
        try {
            ByteBuffer body = ByteBuffer.allocate(1); body.put((byte) 0).flip();
            ByteBuffer frame = envelope(EmptyMessage.class, 91, body);
            int bodyLengthPosition = frame.limit() - 5;
            frame.putInt(bodyLengthPosition, -1);
            assertThrows(RuntimeException.class, () -> protocol.toMessage(frame));
            assertEquals(0, registered[0]);
            ByteBuffer path = ByteBuffer.allocate(4); VarInt.encodeSigned(1025, path); path.flip();
            assertThrows(RuntimeException.class, () -> protocol.toMessage(path));
            assertEquals(0, registered[0]);
        } finally { protocol.retire(); }
    }

    @Test void repeatedClassIdsAreFiniteEvenWhenTheSameAllowedClassIsReused() {
        DynamicSerializerProtocol protocol = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            for (int index = 1; index < DynamicSerializerProtocol.MAX_CLASSES; index++) {
                // AbstractMessage.reliable is transient in the actual provider: EmptyMessage has no body fields.
                assertNotNull(protocol.toMessage(envelope(EmptyMessage.class,index,ByteBuffer.wrap(new byte[] {0}))));
            }
            assertThrows(RuntimeException.class, () -> protocol.toMessage(envelope(EmptyMessage.class,
                    DynamicSerializerProtocol.MAX_CLASSES,ByteBuffer.wrap(new byte[] {0}))));
        } finally { protocol.retire(); }
    }

    @Test void recursiveWireStructureAndZipBombHaveFiniteDecodeWork() {
        DynamicSerializerProtocol protocol = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            ByteBuffer nested = ByteBuffer.wrap(new byte[] {1}); // signed -1: null envelope
            for (int depth = 0; depth < 40; depth++) {
                ByteBuffer body = ByteBuffer.allocate(nested.remaining() + 2).put((byte) 0).put((byte) 1).put(nested); body.flip();
                nested = envelope(ArrayList.class, 72, body);
            }
            ByteBuffer reliable = envelope(Boolean.class, 73, ByteBuffer.wrap(new byte[] {0, 1}));
            ByteBuffer outer = ByteBuffer.allocate(1 + nested.remaining() + reliable.remaining()).put((byte) 0).put(nested).put(reliable); outer.flip();
            ByteBuffer hostile = envelope(ContainerMessage.class, 74, outer);
            assertThrows(RuntimeException.class, () -> protocol.toMessage(hostile));
            byte[] input = new byte[DynamicSerializerProtocol.MAX_FRAME_BYTES + 1];
            Deflater deflater = new Deflater();
            ByteBuffer compressed = ByteBuffer.allocate(2048);
            try { deflater.setInput(input); deflater.finish(); byte[] output = new byte[1024]; int size = deflater.deflate(output);
                compressed.put((byte) 0); VarInt.encodeUnsigned(size, compressed); compressed.put(output, 0, size); compressed.flip();
            } finally { deflater.end(); }
            assertThrows(RuntimeException.class, () -> protocol.toMessage(envelope(CompressedMessage.class, 75, compressed)));
        } finally { protocol.retire(); }
    }

    @Test void senderIdleExpiryProducesNewReliableFullWithAnIncreasingPacketId() {
        ClockProtocol sender = new ClockProtocol(); ClockProtocol receiver = new ClockProtocol();
        try {
            StateMessage first = new StateMessage(-17, 1); first.setReliable(true);
            assertEquals(1, ((StateMessage) receiver.toMessage(sender.toByteBuffer(first, null))).value);
            sender.time += 16_000;
            StateMessage next = new StateMessage(-17, 2); next.setReliable(true);
            assertEquals(2, ((StateMessage) receiver.toMessage(sender.toByteBuffer(next, null))).value);
        } finally { sender.retire(); receiver.retire(); }
    }

    @Test void unknownUnreliableGroupFloodDoesNotPreventReliableInitialBaseline() {
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        try {
            for (int group = 0; group < 5000; group++) {
                ByteBuffer body = ByteBuffer.allocate(64); VarInt.encodeUnsigned(2, body); VarInt.encodeUnsigned(1, body);
                VarInt.encodeUnsigned(0, body); VarInt.encodeSigned(group, body); VarInt.encodeUnsigned(2, body);
                VarInt.encodeUnsigned(1, body); body.flip();
                assertNull(receiver.toMessage(envelope(StateMessage.class, 97, body), Boolean.FALSE));
            }
            StateMessage baseline = new StateMessage(9000, 42); baseline.setReliable(true);
            assertEquals(42, ((StateMessage) receiver.toMessage(sender.toByteBuffer(baseline, null), Boolean.TRUE)).value);
        } finally { receiver.retire(); sender.retire(); }
    }

    @Test void groupAndByteCapacityFailureKeepAnExistingReliableBaseAndRetirementReleasesQuota() {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            for (int group = 0; group < DiffRuntime.MAX_PEER_GROUPS; group++) {
                StateMessage state = new StateMessage(group, 1); state.setReliable(true);
                assertNotNull(receiver.toMessage(sender.toByteBuffer(state, null)));
            }
            StateMessage overflow = new StateMessage(100_000, 1); overflow.setReliable(true);
            assertThrows(RuntimeException.class, () -> sender.toByteBuffer(overflow, null));
            StateMessage update = new StateMessage(0, 2); update.setReliable(true);
            assertEquals(2, ((StateMessage) receiver.toMessage(sender.toByteBuffer(update, null))).value);
        } finally { sender.retire(); receiver.retire(); }
        List<DynamicSerializerProtocol> receivers = new ArrayList<>();
        try {
            boolean capacityFailed = false;
            for (int peer = 0; peer < 9 && !capacityFailed; peer++) {
                DynamicSerializerProtocol protocol = new DynamicSerializerProtocol(true, ignored -> {}, -1); receivers.add(protocol);
                DynamicSerializerProtocol peerSender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
                try {
                    for (int group = 0; group < DiffRuntime.MAX_PEER_GROUPS; group++) {
                        StateMessage state = new StateMessage(group,group); state.setReliable(true);
                        try { assertNotNull(protocol.toMessage(peerSender.toByteBuffer(state,null))); }
                        catch (RuntimeException failure) { capacityFailed = true; break; }
                    }
                } finally { peerSender.retire(); }
            }
            assertTrue(capacityFailed, "aggregate group admission must be finite even with multiple peers");
            receivers.get(0).retire();
            StateMessage fresh = new StateMessage(1_000_000, 7); fresh.setReliable(true);
            DynamicSerializerProtocol freshSender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
            try { assertEquals(7, ((StateMessage) receivers.get(receivers.size() - 1).toMessage(freshSender.toByteBuffer(fresh, null))).value); }
            finally { freshSender.retire(); }
        } finally { for (DynamicSerializerProtocol protocol : receivers) protocol.retire(); }
    }

    @Test void byteRetentionFailureDoesNotEvictTheBaseNeededByAnUnreliableDiff() {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            boolean failed = false;
            for (int group = 0; group < 200 && !failed; group++) {
                StateMessage state = new StateMessage(group, 1); state.label = "x".repeat(60_000); state.setReliable(true);
                try { assertNotNull(receiver.toMessage(sender.toByteBuffer(state, null))); }
                catch (RuntimeException expectedCapacityFailure) { failed = true; }
            }
            assertTrue(failed, "large live bases must reach a finite byte limit before the group limit");
            StateMessage unreliable = new StateMessage(0, 2); unreliable.label = "x".repeat(60_000); unreliable.setReliable(false);
            assertEquals(2, ((StateMessage) receiver.toMessage(sender.toByteBuffer(unreliable, null))).value);
        } finally { sender.retire(); receiver.retire(); }
    }

    @Test void aReliableRuntimeHeaderCannotBeInjectedOnTheUnreliableNativeLane() {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            StateMessage message = new StateMessage(Long.MIN_VALUE, 1); message.setReliable(true);
            ByteBuffer frame = sender.toByteBuffer(message, null);
            assertThrows(RuntimeException.class, () -> receiver.toMessage(frame, Boolean.FALSE));
            assertEquals(1, ((StateMessage) receiver.toMessage(frame, Boolean.TRUE)).value);
        } finally { sender.retire(); receiver.retire(); }
    }

    private static ByteBuffer envelope(Class<?> type, long id, ByteBuffer body) {
        byte[] path = type.getName().getBytes(StandardCharsets.UTF_8);
        ByteBuffer frame = ByteBuffer.allocate(path.length + body.remaining() + 32);
        VarInt.encodeSigned(path.length, frame); frame.put(path); VarInt.encodeSigned(id, frame);
        frame.putInt(body.remaining()).put(body.duplicate()); frame.flip(); return frame;
    }
}
