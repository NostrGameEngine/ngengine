package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.runner.Runner;

final class P2PConnectionHandshakeDiagnosticsTest {
    @Test void actualAcknowledgementPromotionRejectsStalePendingAndObserverFailureCannotBlockPromotion() throws Exception {
        try (Fixture fixture = new Fixture()) {
            List<P2PConnection.HandshakeDiagnostic> events = new ArrayList<>();
            fixture.server.setHandshakeDiagnosticObserver(events::add);
            fixture.pending();
            events.clear();
            fixture.ready(true);
            assertSame(fixture.remote, fixture.server.getConnection(7));
            assertEquals(List.of(P2PConnection.HandshakeReason.ACK_RECEIVED, P2PConnection.HandshakeReason.PROMOTED),
                    events.stream().map(P2PConnection.HandshakeDiagnostic::reason).toList());
            fixture.ready(true);
            assertEquals(P2PConnection.HandshakeReason.PROMOTION_STALE_PENDING, events.get(events.size() - 1).reason());
            long oldEpoch = events.get(0).epoch();
            fixture.server.setHandshakeDiagnosticObserver(event -> { throw new IllegalStateException("observer error"); });
            fixture.pending();
            assertDoesNotThrow(() -> fixture.ready(true));
            assertSame(fixture.remote, fixture.server.getConnection(7));
            fixture.server.setHandshakeDiagnosticObserver(events::add);
            fixture.ready(true);
            assertTrue(events.get(events.size() - 1).epoch() > oldEpoch);
            int count = events.size();
            fixture.server.close();
            fixture.ready(true);
            assertEquals(count, events.size(), "close must detach the observer");
        }
    }

    @Test void admissionRejectionIsObservedAtTheRealPromotionGateWithoutExposingPeer() throws Exception {
        try (Fixture fixture = new Fixture()) {
            List<P2PConnection.HandshakeDiagnostic> events = new ArrayList<>();
            fixture.server.setHandshakeDiagnosticObserver(events::add);
            fixture.server.setPeerAdmission(ignored -> false);
            fixture.pending();
            fixture.ready(true);
            assertNull(fixture.server.getConnection(7));
            assertEquals(P2PConnection.HandshakeReason.ADMISSION_REJECTED, events.get(events.size() - 1).reason());
            assertEquals(fixture.peer.getSessionId(), events.get(0).remoteSession());
            assertEquals(fixture.peer.getPubkey().asHex(), events.get(0).peerPublicKey());
            assertEquals("", events.get(0).failureClass());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final NostrKeyPair local = new NostrKeyPair();
        private final NostrKeyPair remoteKey = new NostrKeyPair();
        private final NostrKeyPair roomKey = new NostrKeyPair();
        private final P2PConnection server;
        private final NostrRTCPeer peer;
        private final RemotePeer remote;
        private Fixture() {
            Runner runner = new Runner() {
                public void run(Runnable task) { task.run(); }
                public void enqueue(Runnable task) { task.run(); }
                public void checkThread() {}
            };
            server = new P2PConnection(new NostrKeyPairSigner(local), "handshake-test", 1,
                    roomKey.getPrivateKey(), null, new NostrPool(), runner);
            NostrRTCPeer own = server.getRtcRoom().getLocalPeerInfo();
            peer = new NostrRTCPeer(remoteKey.getPublicKey(), own.getApplicationId(), own.getProtocolId(),
                    "remote-test-session", roomKey.getPublicKey(), null);
            remote = new RemotePeer(7, server.getRtcRoom(), own, peer, server);
        }
        @SuppressWarnings("unchecked")
        private void pending() throws Exception {
            Field field = P2PConnection.class.getDeclaredField("pendingConnections"); field.setAccessible(true);
            Method key = P2PConnection.class.getDeclaredMethod("peerSessionKeyOf", NostrRTCPeer.class); key.setAccessible(true);
            ((Map<String, RemotePeer>) field.get(server)).put((String) key.invoke(server, peer), remote);
            Method begin = P2PConnection.class.getDeclaredMethod("beginPeerReady", RemotePeer.class);
            begin.setAccessible(true); begin.invoke(server, remote);
        }
        private void ready(boolean acknowledgment) throws Exception {
            Method method = P2PConnection.class.getDeclaredMethod("handlePeerReady", RemotePeer.class, NostrRTCPeer.class, P2PConnection.PeerReadyMessage.class);
            method.setAccessible(true); method.invoke(server, remote, peer, new P2PConnection.PeerReadyMessage(acknowledgment));
        }
        public void close() { server.close(); local.close(); remoteKey.close(); roomKey.close(); }
    }
}
