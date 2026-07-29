package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.network.Message;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.ngengine.network.protocol.DynamicSerializerProtocol;

public class P2PConnectionPeerReadyMessageTest {

    @Test
    public void peerReadyRoundTripStaysReliableAndPreservesAcknowledgement() {
        DynamicSerializerProtocol initiator = new DynamicSerializerProtocol(true, ignored -> {}, 1L);
        DynamicSerializerProtocol responder = new DynamicSerializerProtocol(true, ignored -> {}, -1L);

        ByteBuffer probeBytes = initiator.toByteBuffer(new P2PConnection.PeerReadyMessage(false), null);
        Message decodedProbe = responder.toMessage(probeBytes);

        P2PConnection.PeerReadyMessage probe = assertInstanceOf(
            P2PConnection.PeerReadyMessage.class,
            decodedProbe
        );
        assertTrue(probe.isReliable());
        assertFalse(probe.isAcknowledgement());

        ByteBuffer acknowledgementBytes = responder.toByteBuffer(new P2PConnection.PeerReadyMessage(true), null);
        Message decodedAcknowledgement = initiator.toMessage(acknowledgementBytes);

        P2PConnection.PeerReadyMessage acknowledgement = assertInstanceOf(
            P2PConnection.PeerReadyMessage.class,
            decodedAcknowledgement
        );
        assertTrue(acknowledgement.isReliable());
        assertTrue(acknowledgement.isAcknowledgement());
    }

    @Test
    public void openChannelRequestAndAcknowledgementAreDistinctReliableMessages() {
        DynamicSerializerProtocol initiator = new DynamicSerializerProtocol(true, ignored -> {}, 1L);
        DynamicSerializerProtocol responder = new DynamicSerializerProtocol(true, ignored -> {}, -1L);

        OpenChannelMessage request = assertInstanceOf(
            OpenChannelMessage.class,
            responder.toMessage(initiator.toByteBuffer(new OpenChannelMessage(2, false), null))
        );
        assertTrue(request.isReliable());
        assertFalse(request.isAcknowledgement());

        OpenChannelMessage acknowledgement = assertInstanceOf(
            OpenChannelMessage.class,
            initiator.toMessage(responder.toByteBuffer(new OpenChannelMessage(2, true), null))
        );
        assertTrue(acknowledgement.isReliable());
        assertTrue(acknowledgement.isAcknowledgement());
    }
}
