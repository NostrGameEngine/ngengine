package org.ngengine.network.components;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.network.protocol.NetworkSafe;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotResourceCompatibilityTest {
    @NetworkSafe
    public static class StateSnapshotMessage extends SnapshotMessage {
        public float health;
        public boolean playerSpawned;
        public float[] areas;
    }

    @Test
    void entitySnapshotsRoundTripAcrossIndependentGroupsAndReliableDiffUpdates() {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            for (int update = 0; update < 3; update++) {
                for (int entity = 0; entity < 128; entity++) {
                    StateSnapshotMessage source = new StateSnapshotMessage();
                    source.setNetworkId(BigInteger.ONE.shiftLeft(80).add(BigInteger.valueOf(entity)));
                    source.setComponentId("device");
                    source.setReliable(true);
                    source.health = 100f - update;
                    source.playerSpawned = entity % 2 == 0;
                    source.areas = new float[] {entity, update, 7, entity + 1, update + 1, 5, entity + 2, update + 2, 3};
                    ByteBuffer frame = sender.toByteBuffer(source, null);
                    assertNotNull(frame);
                    assertTrue(frame.remaining() < DynamicSerializerProtocol.MAX_FRAME_BYTES);
                    StateSnapshotMessage decoded = (StateSnapshotMessage) receiver.toMessage(frame, true);
                    assertEquals(source.getNetworkId(), decoded.getNetworkId());
                    assertEquals("device", decoded.getComponentId());
                    assertEquals(source.health, decoded.health);
                    assertEquals(source.playerSpawned, decoded.playerSpawned);
                    assertArrayEquals(source.areas, decoded.areas);
                }
            }
        } finally {
            sender.retire();
            receiver.retire();
        }
    }

    @Test
    void maximumSupportedPrimitiveArrayStillFitsTheDecodeResourceAllowance() {
        DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        DynamicSerializerProtocol receiver = new DynamicSerializerProtocol(true, ignored -> {}, -1);
        try {
            StateSnapshotMessage source = new StateSnapshotMessage();
            source.setNetworkId(BigInteger.ONE);
            source.setComponentId("areas");
            source.setReliable(true);
            source.areas = new float[DynamicSerializerProtocol.MAX_COLLECTION_ITEMS];
            source.areas[source.areas.length - 1] = 42;
            StateSnapshotMessage decoded = (StateSnapshotMessage) receiver.toMessage(sender.toByteBuffer(source, null), true);
            assertArrayEquals(source.areas, decoded.areas);
            assertTrue(receiver.getLastDecodeResourceBytes() <= DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES);
        } finally {
            sender.retire();
            receiver.retire();
        }
    }
}
