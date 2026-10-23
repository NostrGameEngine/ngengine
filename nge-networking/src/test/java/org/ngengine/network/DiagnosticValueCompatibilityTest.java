package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticValueCompatibilityTest {
    @Test
    void handshakeDiagnosticsRetainTheirValueAndTextContracts() {
        P2PConnection.HandshakeDiagnostic first = new P2PConnection.HandshakeDiagnostic(
            17L, P2PConnection.HandshakeReason.PROMOTED, "local", "remote", "public", null);
        P2PConnection.HandshakeDiagnostic same = new P2PConnection.HandshakeDiagnostic(
            17L, P2PConnection.HandshakeReason.PROMOTED, "local", "remote", "public", null);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, new P2PConnection.HandshakeDiagnostic(
            18L, P2PConnection.HandshakeReason.PROMOTED, "local", "remote", "public", null));
        assertEquals(17L, first.epoch());
        assertEquals("HandshakeDiagnostic[epoch=17, reason=PROMOTED, localSession=local, "
            + "remoteSession=remote, peerPublicKey=public, failureClass=null]", first.toString());
    }

    @Test
    void retirementDiagnosticsKeepTheCompleteCounterSnapshot() {
        P2PConnection.PeerRetirementDiagnostic first = new P2PConnection.PeerRetirementDiagnostic(
            P2PConnection.PeerRetirementReason.REQUESTED, "local", "remote", "public", "failure",
            1, 2L, 3, 4L, 5, 6, 7, 8, 9, 10);
        P2PConnection.PeerRetirementDiagnostic same = new P2PConnection.PeerRetirementDiagnostic(
            P2PConnection.PeerRetirementReason.REQUESTED, "local", "remote", "public", "failure",
            1, 2L, 3, 4L, 5, 6, 7, 8, 9, 10);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertEquals(2L, first.inboundBytes());
        assertEquals(4L, first.peerInboundBytes());
        assertEquals(5, first.decodeReservationBytes());
        assertEquals(10, first.peerRetainedAfterDispatchCount());
        assertNotEquals(first, new P2PConnection.PeerRetirementDiagnostic(
            P2PConnection.PeerRetirementReason.REQUESTED, "local", "remote", "public", "failure",
            1, 2L, 3, 4L, 5, 6, 7, 8, 9, 11));
    }

    @Test
    void failureDiagnosticsRetainDefensiveCopiesAndTheConvenienceConstructor() {
        List<String> causes = new ArrayList<>(List.of("Failure"));
        ReliableChannelGate.FailureDiagnostic gate = new ReliableChannelGate.FailureDiagnostic(
            ReliableChannelGate.Failure.SEND_FAILED, 2, true, 3, 4L,
            ReliableChannelGate.FailureOperation.SEND_ASYNC, causes);
        RemotePeer.ReliableFailureDiagnostic peer = new RemotePeer.ReliableFailureDiagnostic(
            RemotePeer.ReliableFailureOrigin.OUTBOUND_GATE, "SEND_FAILED", 2, true, 3, 4L,
            "SEND_ASYNC", causes);
        causes.add("LaterFailure");
        assertEquals(List.of("Failure"), gate.causeClasses());
        assertEquals(List.of("Failure"), peer.causeClasses());
        assertThrows(UnsupportedOperationException.class, () -> gate.causeClasses().clear());
        assertThrows(UnsupportedOperationException.class, () -> peer.causeClasses().clear());
        assertEquals(gate, new ReliableChannelGate.FailureDiagnostic(
            ReliableChannelGate.Failure.SEND_FAILED, 2, true, 3, 4L,
            ReliableChannelGate.FailureOperation.SEND_ASYNC, List.of("Failure")));
        assertEquals(peer, new RemotePeer.ReliableFailureDiagnostic(
            RemotePeer.ReliableFailureOrigin.OUTBOUND_GATE, "SEND_FAILED", 2, true, 3, 4L,
            "SEND_ASYNC", List.of("Failure")));
        RemotePeer.ReliableFailureDiagnostic defaults = new RemotePeer.ReliableFailureDiagnostic(
            RemotePeer.ReliableFailureOrigin.OUTBOUND_GATE, "SEND_FAILED", 2, true, 3, 4L);
        assertEquals("NONE", defaults.operation());
        assertEquals(List.of(), defaults.causeClasses());
    }

    @Test
    void channelDiagnosticsRemainComparableValues() {
        ReliableChannelGate.Diagnostic gate = new ReliableChannelGate.Diagnostic("READY", 1, 2L, 3, 4, 5, 6);
        ReliableChannelGate.Diagnostic sameGate = new ReliableChannelGate.Diagnostic("READY", 1, 2L, 3, 4, 5, 6);
        assertEquals(gate, sameGate);
        assertEquals(gate.hashCode(), sameGate.hashCode());
        RemotePeer.ReliableChannelDiagnostic peer = new RemotePeer.ReliableChannelDiagnostic(
            "READY", 1, 2L, 3, 4, 5, 6);
        RemotePeer.ReliableChannelDiagnostic samePeer = new RemotePeer.ReliableChannelDiagnostic(
            "READY", 1, 2L, 3, 4, 5, 6);
        assertEquals(peer, samePeer);
        assertEquals(peer.hashCode(), samePeer.hashCode());
        assertEquals("ReliableChannelDiagnostic[state=READY, pendingCount=1, pendingBytes=2, "
            + "openControlSendFailed=3, timeouts=4, acknowledgements=5, nativeSends=6]", peer.toString());
    }
}
