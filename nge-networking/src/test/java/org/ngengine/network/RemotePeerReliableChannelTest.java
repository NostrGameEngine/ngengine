package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.network.Message;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.RTCSettings;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.nostr4j.rtc.signal.NostrRTCLocalPeer;
import org.ngengine.nostr4j.rtc.NostrRTCChannel;
import org.ngengine.nostr4j.rtc.NostrRTCRoom;
import org.ngengine.nostr4j.rtc.NostrRTCSocket;
import org.ngengine.nostr4j.rtc.NostrTURNPool;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCChannelListener;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCPeerSocketAvailableListener;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDisconnectListener;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.transport.RTCDataChannel;
import org.ngengine.platform.transport.RTCTransportListener;
import org.ngengine.runner.Runner;

final class RemotePeerReliableChannelTest {
    @Test void ackTimeoutDiagnosticRetainsExactChannelAndPreTeardownQueue() {
        Harness h = new Harness();
        h.submit(193, frame(1)); h.submit(193, frame(2));
        assertNull(h.gate.failureDiagnostic()); h.scheduler.advance(29);
        assertTrue(h.failures.isEmpty()); h.scheduler.advance(1);
        ReliableChannelGate.FailureDiagnostic failure = h.gate.failureDiagnostic();
        assertEquals(ReliableChannelGate.Failure.ACK_TIMEOUT, failure.reason());
        assertEquals(193, failure.channel()); assertFalse(failure.acknowledged());
        assertEquals(ReliableChannelGate.FailureOperation.NONE, failure.operation()); assertTrue(failure.causeClasses().isEmpty());
        assertEquals(2, failure.pendingCount()); assertEquals(8L, failure.pendingBytes());
        assertEquals(0, h.gate.diagnostic(193).pendingCount());
        h.gate.acknowledge(193); h.submit(193, frame(3)); h.scheduler.advance(30);
        assertSame(failure, h.gate.failureDiagnostic()); assertEquals(1, h.failures.size());
    }

    @Test void sendTimeoutDiagnosticRetainsAcknowledgedInFlightAndQueuedFrames() {
        Harness h = new Harness(); Deferred<Void> sending = new Deferred<>();
        h.send = ignored -> sending.task;
        h.submit(2, frame(1)); h.submit(2, frame(2)); h.gate.acknowledge(2);
        h.scheduler.advance(29); assertNull(h.gate.failureDiagnostic()); h.scheduler.advance(1);
        ReliableChannelGate.FailureDiagnostic failure = h.gate.failureDiagnostic();
        assertEquals(ReliableChannelGate.Failure.SEND_TIMEOUT, failure.reason());
        assertEquals(2, failure.channel()); assertTrue(failure.acknowledged());
        assertEquals(ReliableChannelGate.FailureOperation.NONE, failure.operation()); assertTrue(failure.causeClasses().isEmpty());
        assertEquals(2, failure.pendingCount()); assertEquals(8L, failure.pendingBytes());
        sending.resolve.accept(null); h.scheduler.advance(30);
        assertSame(failure, h.gate.failureDiagnostic()); assertEquals(1, h.failures.size());
        assertEquals(List.of(1), h.sent); assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
    }

    @Test void outboundTimeoutDiagnosticSurvivesActualPeerRetirement() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.peer.send(2, new OpenChannelMessage(41));
            f.scheduler.advance(30);
            RemotePeer.ReliableFailureDiagnostic failure = f.peer.reliableFailureDiagnostic();
            assertNotNull(failure); assertEquals(RemotePeer.ReliableFailureOrigin.OUTBOUND_GATE, failure.origin());
            assertEquals("ACK_TIMEOUT", failure.reason()); assertEquals(2, failure.channel());
            assertFalse(failure.acknowledged()); assertEquals(1, failure.pendingCount());
            assertTrue(failure.pendingBytes() > 0L); assertNull(f.server.getConnection(7));
            assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_TIMEOUT), f.reasons);
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true)); f.scheduler.advance(30);
            assertSame(failure, f.peer.reliableFailureDiagnostic()); assertEquals(1, f.reasons.size());
        }
    }

    @Test void receiverAckReplyFailureDiagnosticIsDistinctAndContainsNoExceptionPayload() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.registerSocket(f.peer.getRemotePeer());
            Deferred<Void> acknowledgement = new Deferred<>(); f.peer.control = ignored -> acknowledgement.task;
            f.peer.handleOpenChannel(new OpenChannelMessage(2, false));
            assertEquals(List.of(new OpenChannelMessage(2, true)), f.peer.controls);
            acknowledgement.reject.accept(new IOException("PRIVATE-FIXTURE-PAYLOAD"));
            RemotePeer.ReliableFailureDiagnostic failure = f.peer.reliableFailureDiagnostic();
            assertEquals(RemotePeer.ReliableFailureOrigin.OPEN_ACK_REPLY, failure.origin());
            assertEquals("CONTROL_SEND_FAILED", failure.reason()); assertEquals(2, failure.channel());
            assertEquals("ACK_REPLY_ASYNC", failure.operation());
            assertEquals(List.of(IOException.class.getName()), failure.causeClasses());
            assertFalse(failure.toString().contains("PRIVATE-FIXTURE-PAYLOAD"));
            assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED), f.reasons);
            assertNull(f.server.getConnection(7));
        }
    }

    @Test void outboundSendFailuresKeepSafeClassesAndFirstFailureAcrossAckReplyTeardown() throws Exception {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(RemotePeer.class.getName());
        java.util.logging.Level previousLevel = logger.getLevel();
        List<java.util.logging.LogRecord> records = new ArrayList<>();
        java.util.logging.Handler capture = new java.util.logging.Handler() {
            public void publish(java.util.logging.LogRecord record) {
                if (record.getMessage().startsWith("NGE_RELIABLE_CHANNEL_FAILED")) records.add(record);
            }
            public void flush() {}
            public void close() {}
        };
        logger.setLevel(java.util.logging.Level.INFO); logger.addHandler(capture);
        try {
            for (boolean asynchronous : new boolean[] {false, true}) {
                records.clear();
                try (PeerFixture fixture = new PeerFixture()) {
                    fixture.registerSocket(fixture.peer.getRemotePeer());
                    Deferred<Void> acknowledgement = new Deferred<>();
                    fixture.peer.control = message -> message.isAcknowledgement() ? acknowledgement.task : AsyncTask.completed(null);
                    fixture.peer.handleOpenChannel(new OpenChannelMessage(2, false));
                    Deferred<Void> sending = new Deferred<>();
                    IllegalStateException privateFailure = new IllegalStateException("PRIVATE-FIXTURE-PAYLOAD\ncredential",
                            new IOException("PRIVATE-LOWER-CAUSE"));
                    fixture.peer.reliableSend = ignored -> {
                        if (asynchronous) return sending.task;
                        throw privateFailure;
                    };
                    fixture.peer.send(2, new OpenChannelMessage(41));
                    fixture.peer.handleOpenChannel(new OpenChannelMessage(2, true));
                    if (asynchronous) {
                        assertNull(fixture.peer.reliableFailureDiagnostic()); sending.reject.accept(privateFailure);
                    }
                    RemotePeer.ReliableFailureDiagnostic first = fixture.peer.reliableFailureDiagnostic();
                    assertEquals(RemotePeer.ReliableFailureOrigin.OUTBOUND_GATE, first.origin());
                    assertEquals("SEND_FAILED", first.reason()); assertEquals(2, first.channel()); assertTrue(first.acknowledged());
                    assertEquals(asynchronous ? "SEND_ASYNC" : "SEND_SYNC", first.operation());
                    assertEquals(List.of(IllegalStateException.class.getName()), first.causeClasses());
                    assertEquals(1, first.pendingCount()); assertTrue(first.pendingBytes() > 0L);
                    assertThrows(UnsupportedOperationException.class, () -> first.causeClasses().clear());
                    acknowledgement.reject.accept(new UnsupportedOperationException("PRIVATE-LATER-ACK"));
                    fixture.scheduler.advance(60);
                    assertSame(first, fixture.peer.reliableFailureDiagnostic());
                    assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_SEND_FAILED), fixture.reasons);
                    assertNull(fixture.server.getConnection(7)); assertEquals(1, fixture.peer.frames.size());
                    assertEquals(1, records.size()); java.util.logging.LogRecord logged = records.getFirst();
                    assertNull(logged.getThrown()); assertNull(logged.getParameters());
                    assertTrue(logged.getMessage().contains("operation=" + first.operation()));
                    assertTrue(logged.getMessage().contains("causeClasses=java.lang.IllegalStateException"));
                    assertFalse(logged.getMessage().contains("PRIVATE")); assertFalse(logged.getMessage().contains("credential"));
                    assertFalse(first.toString().contains("PRIVATE")); assertFalse(first.toString().contains("credential"));
                }
            }
        } finally { logger.removeHandler(capture); logger.setLevel(previousLevel); }
    }

    @Test void synchronousAckReplyFailureKeepsCauseWithoutChangingControlRetirement() throws Exception {
        try (PeerFixture fixture = new PeerFixture()) {
            fixture.registerSocket(fixture.peer.getRemotePeer());
            fixture.peer.control = ignored -> { throw new IllegalStateException("PRIVATE-ACK", new IOException("PRIVATE-CAUSE")); };
            fixture.peer.handleOpenChannel(new OpenChannelMessage(2, false));
            RemotePeer.ReliableFailureDiagnostic failure = fixture.peer.reliableFailureDiagnostic();
            assertEquals(RemotePeer.ReliableFailureOrigin.OPEN_ACK_REPLY, failure.origin());
            assertEquals("ACK_REPLY_SYNC", failure.operation());
            assertEquals(List.of(IllegalStateException.class.getName()), failure.causeClasses());
            assertFalse(failure.toString().contains("PRIVATE")); assertFalse(failure.acknowledged());
            assertEquals(0, failure.pendingCount()); assertEquals(0L, failure.pendingBytes());
            assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED), fixture.reasons);
            assertNull(fixture.server.getConnection(7));
        }
    }

    @Test void customAckCauseAccessorCannotPublishAnotherFailureOrExposePrivateText() throws Exception {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(RemotePeer.class.getName());
        java.util.logging.Level previousLevel = logger.getLevel();
        List<java.util.logging.LogRecord> records = new ArrayList<>();
        java.util.logging.Handler capture = new java.util.logging.Handler() {
            public void publish(java.util.logging.LogRecord record) {
                if (record.getMessage().startsWith("NGE_RELIABLE_CHANNEL_FAILED")) records.add(record);
            }
            public void flush() {}
            public void close() {}
        };
        try (PeerFixture fixture = new PeerFixture()) {
            logger.setLevel(java.util.logging.Level.INFO); logger.addHandler(capture);
            try {
                fixture.registerSocket(fixture.peer.getRemotePeer());
                Deferred<Void> firstAck = new Deferred<>(); Deferred<Void> laterAck = new Deferred<>();
                fixture.peer.control = message -> message.getChannel() == 2 ? firstAck.task : laterAck.task;
                fixture.peer.handleOpenChannel(new OpenChannelMessage(2, false));
                fixture.peer.handleOpenChannel(new OpenChannelMessage(3, false));
                java.util.concurrent.atomic.AtomicInteger accessorCalls = new java.util.concurrent.atomic.AtomicInteger();
                RuntimeException privateFailure = new IllegalStateException("PRIVATE-ACK-MESSAGE") {
                    @Override public synchronized Throwable getCause() {
                        accessorCalls.incrementAndGet(); laterAck.reject.accept(new IOException("PRIVATE-REENTRANT-ACK"));
                        return new IOException("PRIVATE-CUSTOM-CAUSE");
                    }
                    @Override public String getMessage() { throw new AssertionError("Exception messages must not be read"); }
                    @Override public String toString() { throw new AssertionError("Exception text must not be read"); }
                };
                firstAck.reject.accept(privateFailure);
                RemotePeer.ReliableFailureDiagnostic first = fixture.peer.reliableFailureDiagnostic();
                assertEquals(0, accessorCalls.get()); assertEquals(2, first.channel()); assertEquals("ACK_REPLY_ASYNC", first.operation());
                assertEquals(RemotePeer.ReliableFailureOrigin.OPEN_ACK_REPLY, first.origin());
                assertEquals(List.of(privateFailure.getClass().getName()), first.causeClasses());
                laterAck.reject.accept(new IOException("PRIVATE-LATER-ACK")); fixture.scheduler.advance(60);
                assertSame(first, fixture.peer.reliableFailureDiagnostic()); assertEquals(0, accessorCalls.get());
                assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED), fixture.reasons);
                assertNull(fixture.server.getConnection(7)); assertEquals(1, records.size());
                java.util.logging.LogRecord logged = records.getFirst(); assertNull(logged.getThrown()); assertNull(logged.getParameters());
                assertTrue(logged.getMessage().contains("operation=ACK_REPLY_ASYNC"));
                assertFalse(logged.getMessage().contains("PRIVATE")); assertFalse(first.toString().contains("PRIVATE"));
            } finally { logger.removeHandler(capture); logger.setLevel(previousLevel); }
        }
    }

    @Test void causeMetadataBoundsAndSanitizationDoNotRetainDeepPrivateOrCyclicCauses() {
        Harness bounded = new Harness();
        RuntimeException deep = new DïagnosticExceptionWithAClassNameThatExceedsTheDiagnosticNameLimit(
                new IllegalStateException("PRIVATE-2", new IllegalArgumentException("PRIVATE-3",
                        new IOException("PRIVATE-4", new UnsupportedOperationException("PRIVATE-5")))));
        bounded.send = ignored -> AsyncTask.failed(deep);
        bounded.submit(2, frame(1)); bounded.gate.acknowledge(2);
        ReliableChannelGate.FailureDiagnostic failure = bounded.gate.failureDiagnostic();
        assertEquals(ReliableChannelGate.FailureOperation.SEND_ASYNC, failure.operation());
        assertEquals(1, failure.causeClasses().size()); assertEquals(96, failure.causeClasses().getFirst().length());
        assertTrue(failure.causeClasses().getFirst().contains("D_agnostic"));
        assertTrue(failure.causeClasses().stream().allMatch(name -> name.length() <= 96 && name.matches("[A-Za-z0-9_.$]+")));
        assertFalse(failure.causeClasses().contains(IOException.class.getName()));
        assertFalse(failure.toString().contains("PRIVATE"));
        assertThrows(UnsupportedOperationException.class, () -> failure.causeClasses().add("unexpected"));
        assertEquals(List.of(ReliableChannelGate.Failure.SEND_FAILED), bounded.failures);
        Harness cyclic = new Harness(); RuntimeException first = new RuntimeException("PRIVATE-CYCLE");
        RuntimeException second = new RuntimeException("PRIVATE-CYCLE"); first.initCause(second); second.initCause(first);
        cyclic.send = ignored -> { throw first; }; cyclic.submit(2, frame(1)); cyclic.gate.acknowledge(2);
        assertEquals(List.of(RuntimeException.class.getName()), cyclic.gate.failureDiagnostic().causeClasses());
        assertEquals(List.of(ReliableChannelGate.Failure.SEND_FAILED), cyclic.failures);
    }

    @Test void customCauseAccessorCannotReenterTheGateOrPreventTeardown() {
        Harness harness = new Harness();
        java.util.concurrent.atomic.AtomicInteger accessorCalls = new java.util.concurrent.atomic.AtomicInteger();
        RuntimeException failure = new RuntimeException("PRIVATE-ACCESSOR") {
            @Override public synchronized Throwable getCause() {
                accessorCalls.incrementAndGet();
                harness.submit(3, frame(3)); harness.gate.acknowledge(3);
                throw new UnsupportedOperationException("PRIVATE-ACCESSOR-FAILURE");
            }
            @Override public String getMessage() { throw new AssertionError("Exception messages must not be read"); }
            @Override public String toString() { throw new AssertionError("Exception text must not be read"); }
        };
        harness.send = value -> { throw value == 1 ? failure : new IllegalArgumentException("PRIVATE-REENTRANT"); };
        harness.submit(2, frame(1)); harness.gate.acknowledge(2);
        ReliableChannelGate.FailureDiagnostic first = harness.gate.failureDiagnostic();
        assertEquals(2, first.channel()); assertEquals(1, first.pendingCount()); assertEquals(4L, first.pendingBytes());
        assertEquals(List.of(failure.getClass().getName()), first.causeClasses());
        harness.scheduler.advance(60); harness.submit(2, frame(9));
        assertSame(first, harness.gate.failureDiagnostic()); assertEquals(List.of(ReliableChannelGate.Failure.SEND_FAILED), harness.failures);
        assertEquals(0, accessorCalls.get());
        assertEquals(List.of(1), harness.sent);
        assertTrue(harness.gate.isClosed()); assertEquals(0, harness.gate.diagnostic(2).pendingCount());
    }

    private static final class DïagnosticExceptionWithAClassNameThatExceedsTheDiagnosticNameLimit extends RuntimeException {
        DïagnosticExceptionWithAClassNameThatExceedsTheDiagnosticNameLimit(Throwable cause) { super("PRIVATE-1", cause); }
    }

    @Test void failingDiagnosticSinkCannotSuppressExistingRetirement() throws Exception {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(RemotePeer.class.getName());
        java.util.logging.Level previousLevel = logger.getLevel();
        java.util.concurrent.atomic.AtomicInteger publications = new java.util.concurrent.atomic.AtomicInteger();
        java.util.logging.Handler broken = new java.util.logging.Handler() {
            public void publish(java.util.logging.LogRecord record) {
                if (record.getMessage().startsWith("NGE_RELIABLE_CHANNEL_FAILED")) {
                    publications.incrementAndGet(); throw new IllegalStateException("fixture sink");
                }
            }
            public void flush() {}
            public void close() {}
        };
        try (PeerFixture f = new PeerFixture()) {
            logger.setLevel(java.util.logging.Level.INFO); logger.addHandler(broken);
            try {
                f.peer.send(2, new OpenChannelMessage(41)); f.scheduler.advance(30);
                assertNotNull(f.peer.reliableFailureDiagnostic()); assertNull(f.server.getConnection(7));
                assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_TIMEOUT), f.reasons);
                f.peer.handleOpenChannel(new OpenChannelMessage(2, true)); f.scheduler.advance(30);
                assertEquals(1, publications.get());
            } finally { logger.removeHandler(broken); logger.setLevel(previousLevel); }
        }
    }

    @Test void completedControlWithoutAckExpiresAndCannotBeReopenedByLateAck() throws Exception {
        Harness h = new Harness();
        h.submit(2, frame(1)); h.submit(2, frame(2));
        assertEquals(1, h.opens); assertTrue(h.sent.isEmpty());
        assertEquals(2, h.gate.diagnostic(2).pendingCount());
        h.scheduler.advance(30);
        assertEquals(List.of(ReliableChannelGate.Failure.ACK_TIMEOUT), h.failures);
        h.gate.acknowledge(2); h.submit(2, frame(3)); h.scheduler.advance(30);
        assertEquals(1, h.opens); assertTrue(h.sent.isEmpty());
        assertEquals(0, h.gate.diagnostic(2).pendingCount());
        assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
    }

    @Test void synchronousControlFailurePublishesNoZombieGateOrSuccessor() throws Exception {
        Harness h = new Harness();
        h.open = ignored -> AsyncTask.failed(new IOException("fixture"));
        h.submit(2, frame(1)); h.submit(2, frame(2)); h.gate.acknowledge(2);
        h.scheduler.advance(30);
        assertEquals(List.of(ReliableChannelGate.Failure.CONTROL_SEND_FAILED), h.failures);
        assertEquals(ReliableChannelGate.FailureOperation.OPEN_ASYNC, h.gate.failureDiagnostic().operation());
        assertEquals(List.of(IOException.class.getName()), h.gate.failureDiagnostic().causeClasses());
        assertEquals(1, h.opens); assertTrue(h.sent.isEmpty());
        assertEquals(1, h.gate.diagnostic(2).openControlSendFailed());
        assertEquals(0, h.gate.diagnostic(2).pendingCount());
    }

    @Test void asynchronousControlFailureDiscardsEveryQueuedFrame() throws Exception {
        Harness h = new Harness(); Deferred<Void> opening = new Deferred<>();
        h.open = ignored -> opening.task;
        h.submit(2, frame(1)); h.submit(2, frame(2));
        opening.reject.accept(new IOException("fixture"));
        h.gate.acknowledge(2); h.scheduler.advance(30);
        assertEquals(List.of(ReliableChannelGate.Failure.CONTROL_SEND_FAILED), h.failures);
        assertEquals(ReliableChannelGate.FailureOperation.OPEN_ASYNC, h.gate.failureDiagnostic().operation());
        assertEquals(List.of(IOException.class.getName()), h.gate.failureDiagnostic().causeClasses());
        assertTrue(h.sent.isEmpty()); assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
    }

    @Test void thrownControlFailureKeepsSynchronousOperationAndPreTeardownTotals() {
        Harness harness = new Harness();
        harness.open = ignored -> { throw new IllegalStateException("PRIVATE-OPEN", new IOException("PRIVATE-CAUSE")); };
        harness.submit(2, frame(1));
        ReliableChannelGate.FailureDiagnostic failure = harness.gate.failureDiagnostic();
        assertEquals(ReliableChannelGate.FailureOperation.OPEN_SYNC, failure.operation());
        assertEquals(List.of(IllegalStateException.class.getName()), failure.causeClasses());
        assertFalse(failure.acknowledged()); assertEquals(1, failure.pendingCount()); assertEquals(4L, failure.pendingBytes());
        assertFalse(failure.toString().contains("PRIVATE"));
        assertEquals(List.of(ReliableChannelGate.Failure.CONTROL_SEND_FAILED), harness.failures);
        assertTrue(harness.sent.isEmpty()); assertEquals(0, harness.gate.diagnostic(2).pendingCount());
    }

    @Test void validAckPreservesOrderAndWaitsForEachAsynchronousNativeSend() throws Exception {
        Harness h = new Harness(); Deferred<Void> first = new Deferred<>();
        h.send = value -> value == 1 ? first.task : AsyncTask.completed(null);
        h.submit(2, frame(1)); h.submit(2, frame(2)); h.submit(2, frame(3));
        h.gate.acknowledge(2); h.gate.acknowledge(2);
        assertEquals(List.of(1), h.sent); assertEquals(3, h.gate.diagnostic(2).pendingCount());
        first.resolve.accept(null);
        assertEquals(List.of(1, 2, 3), h.sent);
        assertEquals(0, h.gate.diagnostic(2).pendingCount());
        assertEquals(1, h.gate.diagnostic(2).acknowledgements());
        h.scheduler.advance(60); assertTrue(h.failures.isEmpty());
    }

    @Test void validLogicalAckSurvivesLateControlFailureWhileNativeFramesRemainOrdered() {
        Harness h = new Harness(); Deferred<Void> opening = new Deferred<>(); Deferred<Void> firstSend = new Deferred<>();
        h.open = ignored -> opening.task;
        h.send = value -> value == 1 ? firstSend.task : AsyncTask.completed(null);
        h.submit(2, frame(1)); h.submit(2, frame(2)); h.submit(2, frame(3));
        h.scheduler.now = TimeUnit.SECONDS.toNanos(1);
        h.gate.acknowledge(2);
        assertEquals(List.of(1), h.sent); assertEquals(3, h.gate.diagnostic(2).pendingCount());
        h.scheduler.now = TimeUnit.SECONDS.toNanos(12);
        opening.reject.accept(new IOException("lost control delivery acknowledgement"));
        assertEquals("OPEN", h.gate.diagnostic(2).state()); assertTrue(h.failures.isEmpty());
        assertEquals(0, h.gate.diagnostic(2).openControlSendFailed());
        assertEquals(3, h.gate.diagnostic(2).pendingCount());
        firstSend.resolve.accept(null);
        assertEquals(List.of(1, 2, 3), h.sent);
        assertEquals(0, h.gate.diagnostic(2).pendingCount()); assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
        h.submit(2, frame(4)); h.scheduler.advance(60);
        assertEquals(List.of(1, 2, 3, 4), h.sent); assertEquals(1, h.opens);
        assertEquals("OPEN", h.gate.diagnostic(2).state()); assertTrue(h.failures.isEmpty());
        h.gate.close(); assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
    }

    @Test void ackDeliveredSynchronouslyInsideOpenCannotMissPublishedQueue() throws Exception {
        Harness h = new Harness();
        h.open = channel -> { h.gate.acknowledge(channel); return AsyncTask.completed(null); };
        h.submit(2, frame(7)); h.submit(2, frame(8)); h.scheduler.advance(60);
        assertEquals(1, h.opens); assertEquals(List.of(7, 8), h.sent); assertTrue(h.failures.isEmpty());
    }

    @Test void nativeFailureAndStalledSendBothRetireWithoutSendingQueuedSuccessors() throws Exception {
        Harness failed = new Harness(); Deferred<Void> nativeSend = new Deferred<>();
        failed.send = ignored -> nativeSend.task;
        failed.submit(2, frame(1)); failed.submit(2, frame(2)); failed.gate.acknowledge(2);
        nativeSend.reject.accept(new IOException("fixture")); failed.scheduler.advance(30);
        assertEquals(List.of(1), failed.sent);
        assertEquals(List.of(ReliableChannelGate.Failure.SEND_FAILED), failed.failures);
        Harness stalled = new Harness(); stalled.send = ignored -> new Deferred<Void>().task;
        stalled.submit(2, frame(1)); stalled.submit(2, frame(2)); stalled.gate.acknowledge(2);
        stalled.scheduler.advance(30);
        assertEquals(List.of(1), stalled.sent);
        assertEquals(List.of(ReliableChannelGate.Failure.SEND_TIMEOUT), stalled.failures);
        assertEquals(0, stalled.gate.diagnostic(2).pendingCount());
    }

    @Test void openingWatchdogDoesNotExpireANewerNativeSendDeadline() {
        Harness h = new Harness(); h.send = ignored -> new Deferred<Void>().task;
        h.submit(2, frame(1)); h.scheduler.now = TimeUnit.SECONDS.toNanos(12);
        h.gate.acknowledge(2); h.scheduler.advance(18);
        assertTrue(h.failures.isEmpty()); assertEquals(1, h.scheduler.scheduled.size());
        h.scheduler.advance(12);
        assertEquals(List.of(ReliableChannelGate.Failure.SEND_TIMEOUT), h.failures);
        assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
    }

    @Test void ackAtDeadlineIsRejectedBeforeDelayedTimerCallbackRuns() {
        Harness h = new Harness(); h.submit(2, frame(1));
        h.scheduler.now = TimeUnit.SECONDS.toNanos(30);
        h.gate.acknowledge(2);
        assertEquals(List.of(ReliableChannelGate.Failure.ACK_TIMEOUT), h.failures);
        assertTrue(h.sent.isEmpty());
    }

    @Test void expiredLogicalAckAndLateOpeningOutcomeCannotReviveClosedGate() {
        Harness h = new Harness(); Deferred<Void> opening = new Deferred<>(); h.open = ignored -> opening.task;
        h.submit(2, frame(1)); h.scheduler.now = TimeUnit.SECONDS.toNanos(30);
        h.gate.acknowledge(2);
        opening.reject.accept(new IOException("late delivery failure"));
        h.submit(2, frame(2)); h.gate.acknowledge(2); h.scheduler.advance(60);
        assertEquals(List.of(ReliableChannelGate.Failure.ACK_TIMEOUT), h.failures);
        assertEquals("CLOSED", h.gate.diagnostic(2).state()); assertTrue(h.sent.isEmpty());
        assertEquals(1, h.opens); assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
    }

    @Test void teardownIgnoresLateCompletionAckAndCancelledTimerWithoutClosingSharedExecutor() throws Exception {
        Harness h = new Harness(); Deferred<Void> first = new Deferred<>();
        h.send = ignored -> first.task;
        h.submit(2, frame(1)); h.submit(2, frame(2)); h.gate.acknowledge(2);
        h.gate.close(); h.gate.close(); first.resolve.accept(null);
        h.gate.acknowledge(2); h.submit(2, frame(3)); h.scheduler.advance(60);
        assertEquals(List.of(1), h.sent); assertTrue(h.failures.isEmpty());
        assertEquals(0, h.gate.diagnostic(2).pendingCount());
        assertEquals(0L, h.gate.diagnostic(2).pendingBytes()); assertFalse(h.scheduler.closed);
    }

    @Test void globalCountAndByteBoundsFailClosedWithoutEvictingPendingFrames() {
        Harness count = new Harness();
        for (int i = 0; i < 256; i++) count.submit(2, frame(i));
        assertEquals(256, count.gate.diagnostic(2).pendingCount());
        count.submit(2, frame(257)); count.gate.acknowledge(2);
        assertEquals(List.of(ReliableChannelGate.Failure.CAPACITY), count.failures);
        assertTrue(count.sent.isEmpty()); assertEquals(1, count.opens);
        Harness bytes = new Harness();
        bytes.submit(2, ByteBuffer.allocate(4 * 1024 * 1024));
        assertEquals(4L * 1024 * 1024, bytes.gate.diagnostic(2).pendingBytes());
        bytes.submit(3, ByteBuffer.allocate(1));
        assertEquals(List.of(ReliableChannelGate.Failure.CAPACITY), bytes.failures);
        assertEquals(0L, bytes.gate.diagnostic(2).pendingBytes());
    }

    @Test void openingCanRetainMoreThanSixtyFourSmallFramesAndFlushAllInOrder() throws Exception {
        Harness h = new Harness(); List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 256; i++) { h.submit(2, frame(i)); expected.add(i); }
        assertTrue(h.failures.isEmpty()); assertTrue(h.sent.isEmpty());
        assertEquals(256, h.gate.diagnostic(2).pendingCount());
        assertEquals(1, h.scheduler.scheduled.size(), "opening has only one watchdog");
        h.scheduler.now = TimeUnit.SECONDS.toNanos(12);
        h.gate.acknowledge(2);
        assertEquals(expected, h.sent); assertEquals(1, h.opens);
        assertEquals(0, h.gate.diagnostic(2).pendingCount());
        assertEquals(0L, h.gate.diagnostic(2).pendingBytes());
        assertEquals(1, h.scheduler.scheduled.size(), "synchronous sends reuse the watchdog rather than adding 256 timers");
        h.scheduler.advance(60); assertTrue(h.failures.isEmpty());
    }

    @Test void acknowledgedEmptyChannelsStillCountTowardLifetimeChannelBound() {
        Harness h = new Harness();
        for (int channel = 1; channel <= 64; channel++) {
            h.submit(channel, frame(channel)); h.gate.acknowledge(channel);
        }
        assertEquals(64, h.sent.size()); assertEquals(0, h.gate.diagnostic(2).pendingCount());
        h.submit(65, frame(65)); h.gate.acknowledge(65);
        assertEquals(64, h.opens); assertEquals(64, h.sent.size());
        assertEquals(List.of(ReliableChannelGate.Failure.CAPACITY), h.failures);
    }

    @Test void actualRemotePeerChannelTwoUsesGateAndFixedRetirementOnLostAck() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.peer.send(2, new OpenChannelMessage(11)); f.peer.send(2, new OpenChannelMessage(12));
            assertEquals(List.of(new OpenChannelMessage(2, false)), f.peer.controls);
            assertEquals("WAITING_ACK", f.peer.reliableChannelDiagnostic().state());
            assertTrue(f.peer.frames.isEmpty()); f.scheduler.advance(30);
            assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_TIMEOUT), f.reasons);
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true));
            f.peer.send(2, new OpenChannelMessage(13));
            assertTrue(f.peer.frames.isEmpty());
            assertEquals("CLOSED", f.peer.reliableChannelDiagnostic().state());
            assertEquals(0, f.peer.reliableChannelDiagnostic().pendingCount());
        }
    }

    @Test void lowerKeyReceiverRegistersUnreliableReceivePathBeforeReliableAck() throws Exception {
        receiverRegistersBothPathsBeforeAck(true);
    }

    @Test void higherKeyReceiverRegistersUnreliableReceivePathBeforeReliableAck() throws Exception {
        receiverRegistersBothPathsBeforeAck(false);
    }

    private void receiverRegistersBothPathsBeforeAck(boolean localLower) throws Exception {
        try (PeerFixture f = new PeerFixture(localLower)) {
            assertEquals(localLower, f.local.getPublicKey().asHex().compareTo(f.other.getPublicKey().asHex()) < 0);
            NostrRTCSocket socket = f.registerSocket(f.peer.getRemotePeer());
            Map<String, NostrRTCChannel> channels = f.channels(socket);
            RTCTransportListener nativeListener = f.nativeListener(socket);
            NativeChannel unknown = new NativeChannel("nge-3-u", false);
            nativeListener.onRTCChannelReady(unknown);
            nativeListener.onRTCBinaryMessage(unknown, ByteBuffer.allocate(16).putLong(1L).putShort((short) 0)
                    .putShort((short) 1).putInt(99).flip());
            assertNull(channels.get("nge-3-u"), "unknown native application channels remain unregistered");
            f.peer.control = acknowledgement -> {
                assertEquals(new OpenChannelMessage(2, true), acknowledgement);
                assertTrue(acknowledgement.isReliable());
                NostrRTCChannel reliable = channels.get("nge-2-r");
                NostrRTCChannel unreliable = channels.get("nge-2-u");
                assertNotNull(reliable, "reliable receive path must exist before ACK");
                assertNotNull(unreliable, "one-way unreliable receive path must exist before ACK");
                assertTrue(reliable.isOrdered()); assertTrue(reliable.isReliable());
                assertFalse(unreliable.isOrdered()); assertFalse(unreliable.isReliable());
                assertEquals(0, reliable.getMaxRetransmits()); assertNull(reliable.getMaxPacketLifeTime());
                assertEquals(0, unreliable.getMaxRetransmits()); assertNull(unreliable.getMaxPacketLifeTime());
                return AsyncTask.completed(null);
            };
            f.peer.handleOpenChannel(new OpenChannelMessage(2, false));
            NostrRTCChannel reliable = channels.get("nge-2-r");
            NostrRTCChannel unreliable = channels.get("nge-2-u");
            List<byte[]> received = new ArrayList<>(); f.listen(unreliable, received);
            List<byte[]> reliableReceived = new ArrayList<>(); f.listen(reliable, reliableReceived);
            NativeChannel nativeReliable = new NativeChannel("nge-2-r", true);
            nativeListener.onRTCChannelReady(nativeReliable);
            nativeListener.onRTCBinaryMessage(nativeReliable, ByteBuffer.allocate(16).putLong(1L).putShort((short) 0)
                    .putShort((short) 1).putInt(72).flip());
            NativeChannel nativeUnreliable = new NativeChannel("nge-2-u", false);
            nativeListener.onRTCChannelReady(nativeUnreliable);
            nativeListener.onRTCBinaryMessage(nativeUnreliable, ByteBuffer.allocate(16).putLong(2L).putShort((short) 0)
                    .putShort((short) 1).putInt(73).flip());
            assertEquals(1, received.size()); assertArrayEquals(copy(frame(73)), received.get(0));
            assertEquals(1, reliableReceived.size()); assertArrayEquals(copy(frame(72)), reliableReceived.get(0));
            int channelCount = channels.size();
            f.peer.handleOpenChannel(new OpenChannelMessage(2, false));
            assertSame(reliable, channels.get("nge-2-r")); assertSame(unreliable, channels.get("nge-2-u"));
            assertEquals(channelCount, channels.size());
            assertEquals(List.of(new OpenChannelMessage(2, true), new OpenChannelMessage(2, true)), f.peer.controls);
            assertEquals("UNUSED", f.peer.reliableChannelDiagnostic().state());
            assertEquals(0, f.peer.reliableChannelDiagnostic().acknowledgements());
            assertEquals(0, f.peer.reliableChannelDiagnostic().nativeSends());
            assertEquals(0, f.peer.reliableChannelDiagnostic().pendingCount());
            assertEquals(0L, f.peer.reliableChannelDiagnostic().pendingBytes());
            assertTrue(f.peer.frames.isEmpty()); assertTrue(f.reasons.isEmpty());
        }
    }

    @Test void retiredReceiverCreatesNoLogicalChannelsOrAck() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            NostrRTCSocket socket = f.registerSocket(f.peer.getRemotePeer());
            f.peer.retireReliableChannels();
            f.peer.handleOpenChannel(new OpenChannelMessage(2, false));
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true));
            assertTrue(f.channels(socket).isEmpty()); assertTrue(f.peer.controls.isEmpty());
            assertEquals("CLOSED", f.peer.reliableChannelDiagnostic().state());
        }
    }

    @Test void receiverAckFailureRetiresExactSessionAndLateOpenCannotReanimateIt() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            NostrRTCPeer old = f.peer.getRemotePeer(); NostrRTCSocket socket = f.registerSocket(old);
            Deferred<Void> acknowledgement = new Deferred<>(); f.peer.control = ignored -> acknowledgement.task;
            f.peer.handleOpenChannel(new OpenChannelMessage(2, false));
            assertNotNull(f.channels(socket).get("nge-2-u"));
            acknowledgement.reject.accept(new IOException("receiver ACK failed"));
            assertNull(f.server.getConnection(7));
            assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED), f.reasons);
            assertEquals("CLOSED", f.peer.reliableChannelDiagnostic().state());
            f.socketAvailable(old); assertTrue(f.pending().isEmpty());
            NostrRTCPeer fresh = new NostrRTCPeer(old.getPubkey(), old.getApplicationId(), old.getProtocolId(),
                    old.getSessionId() + "-fresh-receive", old.getRoomPubkey(), null);
            f.socketAvailable(fresh); assertEquals(1, f.pending().size());
            RemotePeer successor = f.pending().values().iterator().next();
            int channelCount = f.channels(socket).size();
            f.peer.handleOpenChannel(new OpenChannelMessage(3, false));
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true));
            assertEquals(channelCount, f.channels(socket).size());
            assertEquals(List.of(new OpenChannelMessage(2, true)), f.peer.controls);
            assertSame(successor, f.pending().values().iterator().next());
            assertTrue(f.peer.frames.isEmpty()); assertEquals(0L, f.peer.reliableChannelDiagnostic().pendingBytes());
        }
    }

    @Test void actualRemotePeerObservesAsyncControlFailureBeforeAnyChannelTwoFrame() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            Deferred<Void> control = new Deferred<>(); f.peer.control = ignored -> control.task;
            f.peer.send(2, new OpenChannelMessage(41)); f.peer.send(2, new OpenChannelMessage(42));
            assertEquals(2, f.peer.reliableChannelDiagnostic().pendingCount());
            control.reject.accept(new IOException("fixture"));
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true)); f.scheduler.advance(30);
            assertTrue(f.peer.frames.isEmpty());
            assertEquals(List.of(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED), f.reasons);
            assertEquals(1, f.peer.reliableChannelDiagnostic().openControlSendFailed());
            assertEquals(0L, f.peer.reliableChannelDiagnostic().pendingBytes());
        }
    }

    @Test void actualRemotePeerRemainsCurrentWhenOpenControlFailsAfterValidLogicalAck() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            Deferred<Void> control = new Deferred<>(); f.peer.control = ignored -> control.task;
            Message first = new OpenChannelMessage(61); Message second = new OpenChannelMessage(62);
            byte[] expectedFirst = copy(f.peer.getProtocol().toByteBuffer(first, null));
            byte[] expectedSecond = copy(f.peer.getProtocol().toByteBuffer(second, null));
            f.peer.send(2, first); f.peer.send(2, second);
            f.scheduler.now = TimeUnit.SECONDS.toNanos(1);
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true));
            f.scheduler.now = TimeUnit.SECONDS.toNanos(12);
            control.reject.accept(new IOException("lost control delivery acknowledgement"));
            assertSame(f.peer, f.server.getConnection(7)); assertTrue(f.reasons.isEmpty());
            assertEquals("OPEN", f.peer.reliableChannelDiagnostic().state());
            assertEquals(0, f.peer.reliableChannelDiagnostic().openControlSendFailed());
            assertArrayEquals(expectedFirst, f.peer.frames.get(0)); assertArrayEquals(expectedSecond, f.peer.frames.get(1));
            f.peer.send(2, new OpenChannelMessage(63)); f.scheduler.advance(60);
            assertEquals(3, f.peer.frames.size()); assertEquals(1, f.peer.controls.size());
            assertTrue(f.reasons.isEmpty()); assertEquals(0L, f.peer.reliableChannelDiagnostic().pendingBytes());
            f.server.close(); assertEquals("CLOSED", f.peer.reliableChannelDiagnostic().state());
            assertEquals(0, f.peer.reliableChannelDiagnostic().pendingCount());
        }
    }

    @Test void actualRemotePeerPreservesEncodedFrameOrderAndP2PRetirementClosesGate() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            Message first = new OpenChannelMessage(21); Message second = new OpenChannelMessage(22);
            byte[] expectedFirst = copy(f.peer.getProtocol().toByteBuffer(first, null));
            byte[] expectedSecond = copy(f.peer.getProtocol().toByteBuffer(second, null));
            f.peer.send(2, first); f.peer.send(2, second);
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true));
            assertEquals(2, f.peer.frames.size());
            assertArrayEquals(expectedFirst, f.peer.frames.get(0)); assertArrayEquals(expectedSecond, f.peer.frames.get(1));
            f.server.failInboundPeer(f.peer, "Fixture retirement");
            f.peer.send(2, first); f.peer.handleOpenChannel(new OpenChannelMessage(2, true)); f.scheduler.advance(60);
            assertEquals(2, f.peer.frames.size());
            assertEquals("CLOSED", f.peer.reliableChannelDiagnostic().state());
        }
    }

    @Test void failedNativeSessionCannotRebindAnOldAckButFreshSessionMayRejoin() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.peer.control = ignored -> AsyncTask.failed(new IOException("fixture"));
            NostrRTCPeer old = f.peer.getRemotePeer();
            f.peer.send(2, new OpenChannelMessage(51));
            assertNull(f.server.getConnection(7));
            f.socketAvailable(old); assertTrue(f.pending().isEmpty());
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true));
            assertTrue(f.peer.frames.isEmpty());
            NostrRTCPeer fresh = new NostrRTCPeer(old.getPubkey(), old.getApplicationId(), old.getProtocolId(),
                    old.getSessionId() + "-fresh", old.getRoomPubkey(), null);
            f.socketAvailable(fresh); assertEquals(1, f.pending().size());
            assertNotSame(f.peer, f.pending().values().iterator().next());
        }
    }

    @Test void delayedReliableFailureAfterNativeRemovalStillTombstonesExactSession() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.server.deferReliableFailure = true;
            f.peer.control = ignored -> AsyncTask.failed(new IOException("fixture"));
            f.peer.send(2, new OpenChannelMessage(71));
            assertEquals("CLOSED", f.peer.reliableChannelDiagnostic().state());
            assertNotNull(f.server.delayedFailure);
            NostrRTCPeer old = f.peer.getRemotePeer(); f.disconnect(old);
            assertNull(f.server.getConnection(7)); assertTrue(f.pending().isEmpty());
            f.server.deliverFailure();
            f.socketAvailable(old); assertTrue(f.pending().isEmpty());
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true));
            assertTrue(f.peer.frames.isEmpty()); assertEquals(0L, f.peer.reliableChannelDiagnostic().pendingBytes());
        }
    }

    @Test void delayedReliableFailureRetiresAlreadyAdmittedSameSessionSuccessorBeforeLateAck() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.server.deferReliableFailure = true;
            f.peer.control = ignored -> AsyncTask.failed(new IOException("fixture"));
            f.peer.send(2, new OpenChannelMessage(72));
            NostrRTCPeer old = f.peer.getRemotePeer(); f.disconnect(old);
            f.socketAvailable(old);
            RemotePeer successor = f.pending().values().iterator().next(); f.ready(successor, old);
            assertTrue(f.server.isCurrentPeer(successor)); assertNotSame(f.peer, successor);
            CapturedTransport transport = f.captureTransport(successor);
            successor.send(2, new OpenChannelMessage(73));
            assertEquals("WAITING_ACK", successor.reliableChannelDiagnostic().state());
            assertEquals(1, successor.reliableChannelDiagnostic().pendingCount());
            f.server.deliverFailure();
            assertFalse(f.server.isCurrentPeer(successor)); assertNull(f.server.getConnection(successor.getId()));
            assertEquals("CLOSED", successor.reliableChannelDiagnostic().state());
            successor.handleOpenChannel(new OpenChannelMessage(2, true)); successor.send(2, new OpenChannelMessage(74));
            f.socketAvailable(old); f.scheduler.advance(60);
            assertEquals(1, transport.opens); assertTrue(transport.frames.isEmpty()); assertTrue(f.pending().isEmpty());
            assertEquals(0, successor.reliableChannelDiagnostic().pendingCount());
            assertEquals(0L, successor.reliableChannelDiagnostic().pendingBytes());
            assertTrue(f.reasons.contains(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED));
        }
    }

    @Test void delayedReliableFailureRetiresPendingSameSessionSuccessorBeforeLateReadyAck() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.server.deferReliableFailure = true;
            f.peer.control = ignored -> AsyncTask.failed(new IOException("fixture"));
            f.peer.send(2, new OpenChannelMessage(78));
            NostrRTCPeer old = f.peer.getRemotePeer(); f.disconnect(old); f.socketAvailable(old);
            RemotePeer successor = f.pending().values().iterator().next();
            CapturedTransport transport = f.captureTransport(successor);
            successor.send(2, new OpenChannelMessage(79));
            assertEquals(1, successor.reliableChannelDiagnostic().pendingCount());
            f.server.deliverFailure(); f.ready(successor, old);
            successor.handleOpenChannel(new OpenChannelMessage(2, true)); f.scheduler.advance(60);
            assertTrue(f.pending().isEmpty()); assertNull(f.server.getConnection(successor.getId()));
            assertFalse(f.server.isCurrentPeer(successor)); assertEquals("CLOSED", successor.reliableChannelDiagnostic().state());
            assertTrue(transport.frames.isEmpty()); assertEquals(0L, successor.reliableChannelDiagnostic().pendingBytes());
        }
    }

    @Test void delayedReliableFailureLeavesAlreadyAdmittedFreshSessionSuccessorUsable() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.server.deferReliableFailure = true;
            f.peer.control = ignored -> AsyncTask.failed(new IOException("fixture"));
            f.peer.send(2, new OpenChannelMessage(75));
            NostrRTCPeer old = f.peer.getRemotePeer(); f.disconnect(old);
            NostrRTCPeer fresh = new NostrRTCPeer(old.getPubkey(), old.getApplicationId(), old.getProtocolId(),
                    old.getSessionId() + "-fresh-race", old.getRoomPubkey(), null);
            f.socketAvailable(fresh);
            RemotePeer successor = f.pending().values().iterator().next(); f.ready(successor, fresh);
            CapturedTransport transport = f.captureTransport(successor);
            Message first = new OpenChannelMessage(76);
            byte[] expected = copy(successor.getProtocol().toByteBuffer(first, null));
            successor.send(2, first);
            f.server.deliverFailure();
            assertTrue(f.server.isCurrentPeer(successor)); assertSame(successor, f.server.getConnection(successor.getId()));
            assertEquals("WAITING_ACK", successor.reliableChannelDiagnostic().state());
            successor.handleOpenChannel(new OpenChannelMessage(2, true));
            assertEquals(1, transport.frames.size()); assertArrayEquals(expected, transport.frames.get(0));
            successor.send(2, new OpenChannelMessage(77)); f.scheduler.advance(60);
            assertEquals(2, transport.frames.size()); assertEquals(1, transport.opens);
            assertTrue(f.server.isCurrentPeer(successor)); assertEquals("OPEN", successor.reliableChannelDiagnostic().state());
            assertEquals(0L, successor.reliableChannelDiagnostic().pendingBytes());
            assertFalse(f.reasons.contains(P2PConnection.PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED));
        }
    }

    @Test void actualP2PCloseRetiresPendingGateAndOwnsExecutorLifetime() throws Exception {
        try (PeerFixture f = new PeerFixture()) {
            f.peer.send(2, new OpenChannelMessage(31)); f.server.close();
            f.peer.handleOpenChannel(new OpenChannelMessage(2, true)); f.peer.send(2, new OpenChannelMessage(32));
            assertTrue(f.scheduler.closed); assertTrue(f.peer.frames.isEmpty());
            assertEquals(0L, f.peer.reliableChannelDiagnostic().pendingBytes());
        }
    }

    private static ByteBuffer frame(int value) { return ByteBuffer.allocate(4).putInt(value).flip(); }
    private static byte[] copy(ByteBuffer buffer) {
        byte[] result = new byte[buffer.remaining()]; buffer.duplicate().get(result); return result;
    }
    private static final class Deferred<T> {
        Consumer<T> resolve;
        Consumer<Throwable> reject;
        final AsyncTask<T> task = AsyncTask.create((resolve, reject) -> { this.resolve = resolve; this.reject = reject; });
    }
    private static final class Harness {
        final Scheduler scheduler = new Scheduler();
        final List<Integer> sent = new ArrayList<>();
        final List<ReliableChannelGate.Failure> failures = new ArrayList<>();
        Function<Integer, AsyncTask<Void>> open = ignored -> AsyncTask.completed(null);
        Function<Integer, AsyncTask<Void>> send = ignored -> AsyncTask.completed(null);
        int opens;
        final ReliableChannelGate gate = new ReliableChannelGate(scheduler, () -> scheduler.now,
                new ReliableChannelGate.Transport() {
                    public AsyncTask<Void> open(int channel, ByteBuffer control) { opens++; return Harness.this.open.apply(channel); }
                    public AsyncTask<Void> send(int channel, ByteBuffer buffer) {
                        int value = buffer.getInt(); sent.add(value); return Harness.this.send.apply(value);
                    }
                }, this::failed);
        void submit(int channel, ByteBuffer buffer) { gate.submit(channel, buffer, ByteBuffer.allocate(0)); }
        private void failed(ReliableChannelGate.Failure failure) {
            assertFalse(Thread.holdsLock(gate), "retirement callbacks must run after releasing the gate monitor");
            failures.add(failure);
        }
    }
    private static final class Scheduler implements AsyncExecutor {
        private record Scheduled(long due, Runnable action) {}
        final List<Scheduled> scheduled = new ArrayList<>();
        long now;
        boolean closed;
        public <T> AsyncTask<T> runLater(Callable<T> task, long delay, TimeUnit unit) {
            return AsyncTask.create((resolve, reject) -> scheduled.add(new Scheduled(now + unit.toNanos(delay), () -> {
                try { resolve.accept(task.call()); } catch (Throwable error) { reject.accept(error); }
            })));
        }
        public <T> AsyncTask<T> run(Callable<T> task) { throw new UnsupportedOperationException(); }
        void advance(long seconds) {
            now += TimeUnit.SECONDS.toNanos(seconds);
            for (;;) {
                Scheduled next = null;
                for (Scheduled candidate : scheduled) {
                    if (candidate.due() <= now && (next == null || candidate.due() < next.due())) next = candidate;
                }
                if (next == null) return;
                scheduled.remove(next); next.action().run(); // Deliberately exercises callbacks even after cancellation.
            }
        }
        public void close() { closed = true; scheduled.clear(); }
    }
    private static final class PeerFixture implements AutoCloseable {
        final NostrKeyPair local;
        final NostrKeyPair other;
        final NostrKeyPair room = new NostrKeyPair();
        final Scheduler scheduler = new Scheduler();
        final ControlledConnection server;
        final WirePeer peer;
        final List<P2PConnection.PeerRetirementReason> reasons = new ArrayList<>();
        PeerFixture() throws Exception { this(null); }
        @SuppressWarnings("unchecked") PeerFixture(Boolean localLower) throws Exception {
            NostrKeyPair first = new NostrKeyPair(); NostrKeyPair second = new NostrKeyPair();
            boolean firstLower = first.getPublicKey().asHex().compareTo(second.getPublicKey().asHex()) < 0;
            boolean swap = localLower != null && firstLower != localLower.booleanValue();
            local = swap ? second : first;
            other = swap ? first : second;
            Runner runner = new Runner() {
                public void run(Runnable task) { task.run(); }
                public void enqueue(Runnable task) { task.run(); }
                public void checkThread() {}
            };
            server = new ControlledConnection(local, room, runner);
            Field executor = P2PConnection.class.getDeclaredField("readyExecutor"); executor.setAccessible(true);
            ((AsyncExecutor) executor.get(server)).close(); executor.set(server, scheduler);
            NostrRTCPeer self = server.getRtcRoom().getLocalPeerInfo();
            NostrRTCPeer remote = new NostrRTCPeer(other.getPublicKey(), self.getApplicationId(), self.getProtocolId(),
                    "fixture-session", self.getRoomPubkey(), null);
            peer = new WirePeer(server, self, remote, scheduler);
            Field connections = P2PConnection.class.getDeclaredField("connections"); connections.setAccessible(true);
            ((Map<Integer, RemotePeer>) connections.get(server)).put(7, peer);
            server.setPeerRetirementDiagnosticObserver(event -> reasons.add(event.reason()));
        }
        @SuppressWarnings("unchecked") Map<String, RemotePeer> pending() throws Exception {
            Field pending = P2PConnection.class.getDeclaredField("pendingConnections"); pending.setAccessible(true);
            return (Map<String, RemotePeer>) pending.get(server);
        }
        NostrRTCSocket socket(NostrRTCPeer remote) throws Exception {
            Constructor<NostrRTCSocket> constructor = NostrRTCSocket.class.getDeclaredConstructor(AsyncExecutor.class,
                    NostrRTCPeer.class, NostrKeyPair.class, NostrRTCLocalPeer.class, RTCSettings.class, NostrTURNPool.class);
            constructor.setAccessible(true);
            return constructor.newInstance(scheduler, remote, room,
                    (NostrRTCLocalPeer) server.getRtcRoom().getLocalPeerInfo(),
                    RTCSettings.getDefault("reliable-channel", "reliable-channel:1"), null);
        }
        @SuppressWarnings("unchecked") NostrRTCSocket registerSocket(NostrRTCPeer remote) throws Exception {
            NostrRTCSocket socket = socket(remote);
            Field connections = NostrRTCRoom.class.getDeclaredField("connections"); connections.setAccessible(true);
            ((Map<NostrRTCPeer, NostrRTCSocket>) connections.get(server.getRtcRoom())).put(remote, socket);
            return socket;
        }
        @SuppressWarnings("unchecked") Map<String, NostrRTCChannel> channels(NostrRTCSocket socket) throws Exception {
            Field channels = NostrRTCSocket.class.getDeclaredField("channels"); channels.setAccessible(true);
            return (Map<String, NostrRTCChannel>) channels.get(socket);
        }
        RTCTransportListener nativeListener(NostrRTCSocket socket) throws Exception {
            Field listener = NostrRTCSocket.class.getDeclaredField("rtcListener"); listener.setAccessible(true);
            return (RTCTransportListener) listener.get(socket);
        }
        void listen(NostrRTCChannel channel, List<byte[]> received) throws Exception {
            Method add = NostrRTCChannel.class.getDeclaredMethod("addListener", NostrRTCChannelListener.class);
            add.setAccessible(true);
            add.invoke(channel, new NostrRTCChannelListener() {
                public void onRTCSocketMessage(NostrRTCChannel source, ByteBuffer buffer, boolean isTurn) {
                    assertSame(channel, source); assertFalse(isTurn); received.add(copy(buffer));
                }
                public void onRTCChannelError(NostrRTCChannel source, Throwable error) { fail(error); }
                public void onRTCChannelClosed(NostrRTCChannel source) {}
                public void onRTCBufferedAmountLow(NostrRTCChannel source) {}
            });
        }
        @SuppressWarnings("unchecked") void socketAvailable(NostrRTCPeer remote) throws Exception {
            NostrRTCSocket socket = socket(remote);
            Field available = server.getRtcRoom().getClass().getDeclaredField("onSocketAvailable"); available.setAccessible(true);
            List<NostrRTCPeerSocketAvailableListener> listeners =
                    (List<NostrRTCPeerSocketAvailableListener>) available.get(server.getRtcRoom());
            for (NostrRTCPeerSocketAvailableListener listener : listeners) listener.onRoomPeerSocketAvailable(remote, socket);
        }
        @SuppressWarnings("unchecked") void disconnect(NostrRTCPeer remote) throws Exception {
            NostrRTCSocket socket = socket(remote);
            Field disconnected = server.getRtcRoom().getClass().getDeclaredField("onDisconnectionListeners");
            disconnected.setAccessible(true);
            List<NostrRTCRoomPeerDisconnectListener> listeners =
                    (List<NostrRTCRoomPeerDisconnectListener>) disconnected.get(server.getRtcRoom());
            for (NostrRTCRoomPeerDisconnectListener listener : listeners) listener.onRoomPeerDisconnected(remote, socket);
        }
        void ready(RemotePeer peer, NostrRTCPeer source) throws Exception {
            Method ready = P2PConnection.class.getDeclaredMethod("handlePeerReady", RemotePeer.class,
                    NostrRTCPeer.class, P2PConnection.PeerReadyMessage.class);
            ready.setAccessible(true); ready.invoke(server, peer, source, new P2PConnection.PeerReadyMessage(true));
        }
        CapturedTransport captureTransport(RemotePeer peer) throws Exception {
            peer.retireReliableChannels();
            CapturedTransport transport = new CapturedTransport();
            ReliableChannelGate gate = new ReliableChannelGate(scheduler, () -> scheduler.now, transport,
                    failure -> server.failReliableChannel(peer, failure));
            Field channels = RemotePeer.class.getDeclaredField("reliableChannels"); channels.setAccessible(true);
            channels.set(peer, gate);
            return transport;
        }
        public void close() { server.close(); local.close(); other.close(); room.close(); }
    }
    private static final class ControlledConnection extends P2PConnection {
        boolean deferReliableFailure;
        Runnable delayedFailure;
        ControlledConnection(NostrKeyPair local, NostrKeyPair room, Runner runner) {
            super(new NostrKeyPairSigner(local), "reliable-channel", 1,
                    room.getPrivateKey(), null, new NostrPool(), runner);
        }
        @Override void failReliableChannel(RemotePeer peer, ReliableChannelGate.Failure failure) {
            if (!deferReliableFailure) { super.failReliableChannel(peer, failure); return; }
            assertNull(delayedFailure, "only the old peer failure is delayed");
            delayedFailure = () -> super.failReliableChannel(peer, failure);
        }
        void deliverFailure() {
            assertNotNull(delayedFailure); Runnable delivery = delayedFailure; delayedFailure = null;
            deferReliableFailure = false; delivery.run();
        }
    }
    private static final class CapturedTransport implements ReliableChannelGate.Transport {
        int opens;
        final List<byte[]> frames = new ArrayList<>();
        public AsyncTask<Void> open(int channel, ByteBuffer control) { opens++; return AsyncTask.completed(null); }
        public AsyncTask<Void> send(int channel, ByteBuffer frame) {
            frames.add(copy(frame)); return AsyncTask.completed(null);
        }
    }
    private static final class NativeChannel extends RTCDataChannel {
        NativeChannel(String name, boolean reliable) { super(name, "reliable-channel:1", reliable, reliable, 0, null); }
        public AsyncTask<RTCDataChannel> ready() { return AsyncTask.completed(this); }
        public AsyncTask<Void> write(ByteBuffer buffer) { throw new UnsupportedOperationException("receive-only fixture"); }
        public AsyncTask<Number> getMaxMessageSize() { return AsyncTask.completed(Integer.valueOf(65536)); }
        public AsyncTask<Number> getAvailableAmount() { return AsyncTask.completed(Integer.valueOf(65536)); }
        public AsyncTask<Number> getBufferedAmount() { return AsyncTask.completed(Integer.valueOf(0)); }
        public AsyncTask<Void> setBufferedAmountLowThreshold(int threshold) { return AsyncTask.completed(null); }
        public AsyncTask<Void> close() { return AsyncTask.completed(null); }
    }
    private static final class WirePeer extends RemotePeer {
        final List<OpenChannelMessage> controls = new ArrayList<>();
        final List<byte[]> frames = new ArrayList<>();
        Function<OpenChannelMessage, AsyncTask<Void>> control = ignored -> AsyncTask.completed(null);
        Function<Integer, AsyncTask<Void>> reliableSend = ignored -> AsyncTask.completed(null);
        WirePeer(P2PConnection server, NostrRTCPeer local, NostrRTCPeer remote, Scheduler scheduler) {
            super(7, server.getRtcRoom(), local, remote, server, scheduler, () -> scheduler.now);
        }
        @Override AsyncTask<Void> sendPreparedControl(Message message, ByteBuffer buffer) {
            OpenChannelMessage open = (OpenChannelMessage) message; controls.add(open); return control.apply(open);
        }
        @Override AsyncTask<Void> sendControl(Message message) {
            OpenChannelMessage open = (OpenChannelMessage) message; controls.add(open); return control.apply(open);
        }
        @Override AsyncTask<Void> sendReliableFrame(int channel, ByteBuffer buffer) {
            assertEquals(2, channel); frames.add(copy(buffer)); return reliableSend.apply(channel);
        }
    }
}
