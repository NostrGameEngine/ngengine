package org.ngengine.network.protocol;

import static org.junit.jupiter.api.Assertions.*;
import com.jme3.network.AbstractMessage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.ngengine.network.protocol.messages.BinaryMessage;
import org.ngengine.network.protocol.messages.ByteDataMessage;
import org.ngengine.network.protocol.messages.CompressedMessage;

/** Checks the enforced serializer resource model, not arbitrary application/native heap allocation. */
final class DynamicSerializerAllowanceTest {
    @NetworkSafe public static final class BoxMessage extends AbstractMessage {
        public Object value;
        public BoxMessage() {}
        BoxMessage(Object value) { this.value = value; }
    }
    @NetworkSafe public static final class ObservedBytesMessage extends AbstractMessage implements ByteDataMessage {
        static int dataAssignments;
        private ByteBuffer data;
        public ObservedBytesMessage() {}
        public ByteBuffer getData() { return data; }
        public void setData(ByteBuffer data) { dataAssignments++; this.data = data; }
    }
    private static DynamicSerializerProtocol protocol(long side) { return new DynamicSerializerProtocol(true, ignored -> {}, side); }

    @Test void bytePayloadAllowanceFailsBeforeCopyAndSetDataThenDefaultDecodeStillWorks() {
        DynamicSerializerProtocol sender = protocol(1), receiver = protocol(-1);
        try {
            ObservedBytesMessage message = new ObservedBytesMessage(); message.setData(ByteBuffer.allocate(1024));
            ByteBuffer frame = sender.toByteBuffer(message, null); ObservedBytesMessage.dataAssignments = 0;
            int wireAndRoot = frame.remaining() * 2 + 128;
            assertThrows(RuntimeException.class, () -> receiver.toMessage(frame, true, wireAndRoot));
            assertEquals(0, ObservedBytesMessage.dataAssignments);
            assertInstanceOf(ObservedBytesMessage.class, receiver.toMessage(frame, true));
            assertEquals(1, ObservedBytesMessage.dataAssignments);
        } finally { sender.retire(); receiver.retire(); }
    }
    @Test void exactMeasuredResourceAllowanceSucceedsAndOneByteLessFails() {
        DynamicSerializerProtocol sender = protocol(1), measuring = protocol(-1), exact = protocol(-1), shortBudget = protocol(-1);
        try {
            ByteBuffer frame = sender.toByteBuffer(new BinaryMessage(ByteBuffer.allocate(128)), null);
            assertNotNull(measuring.toMessage(frame, true)); int required = measuring.getLastDecodeResourceBytes();
            assertNotNull(exact.toMessage(frame, true, required)); assertEquals(required, exact.getLastDecodeResourceBytes());
            assertThrows(RuntimeException.class, () -> shortBudget.toMessage(frame, true, required - 1));
        } finally { sender.retire(); measuring.retire(); exact.retire(); shortBudget.retire(); }
    }
    @Test void collectionAndMapPreflightTheirChildNodesBeforeUnboundedTraversal() {
        ArrayList<Object> values = new ArrayList<>(); HashMap<Integer, Integer> mapping = new HashMap<>();
        for (int index = 0; index < 100; index++) { values.add(Integer.valueOf(index)); mapping.put(index, index); }
        for (Object container : new Object[] {values, mapping}) {
            DynamicSerializerProtocol sender = protocol(1), receiver = protocol(-1);
            try {
                ByteBuffer frame = sender.toByteBuffer(new BoxMessage(container), null);
                int allowance = frame.remaining() * 2 + 4096;
                assertThrows(RuntimeException.class, () -> receiver.toMessage(frame, true, allowance));
                assertTrue(receiver.getLastDecodeResourceBytes() <= allowance,
                        "preflight must fail before charging or traversing all declared child nodes");
            } finally { sender.retire(); receiver.retire(); }
        }
    }
    @Test void compressedScratchAndExpansionConsumeTheSameCallerAllowance() {
        DynamicSerializerProtocol sender = protocol(1), receiver = protocol(-1);
        try {
            ByteBuffer frame = sender.toByteBuffer(new CompressedMessage(new BinaryMessage(ByteBuffer.allocate(32 * 1024))), null);
            assertTrue(frame.remaining() < 4096);
            assertThrows(RuntimeException.class, () -> receiver.toMessage(frame, true, 8192));
            assertInstanceOf(CompressedMessage.class, receiver.toMessage(frame, true));
            assertTrue(receiver.getLastDecodeResourceBytes() > 8192);
        } finally { sender.retire(); receiver.retire(); }
    }
    @Test void impossibleAllowanceIsRejectedWithoutAdvancingInputOrProtocol() {
        DynamicSerializerProtocol sender = protocol(1), receiver = protocol(-1);
        try {
            ByteBuffer frame = sender.toByteBuffer(new BinaryMessage(ByteBuffer.allocate(1)), null);
            int position = frame.position();
            assertThrows(IllegalArgumentException.class, () -> receiver.toMessage(frame, true, 0));
            assertThrows(IllegalArgumentException.class, () -> receiver.toMessage(frame, true, DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES + 1));
            assertEquals(position, frame.position()); assertNotNull(receiver.toMessage(frame, true));
        } finally { sender.retire(); receiver.retire(); }
    }
}
