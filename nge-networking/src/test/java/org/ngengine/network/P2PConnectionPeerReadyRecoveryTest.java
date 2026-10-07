package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.RTCSettings;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.rtc.NostrRTCSocket;
import org.ngengine.nostr4j.rtc.NostrTURNPool;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCPeerSocketAvailableListener;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDisconnectListener;
import org.ngengine.nostr4j.rtc.signal.NostrRTCLocalPeer;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.runner.Runner;

final class P2PConnectionPeerReadyRecoveryTest {
    @Test void firstReadyDropRecoversWithoutTreatingTaskCompletionAsAcknowledgement() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = message -> {
                if (message.isAcknowledgement() || pair.a.remote.readySends > 1) pair.b.ready(message, pair.a.local());
                return AsyncTask.completed(null);
            };
            pair.start();
            assertNull(pair.a.server.getConnection(7), "a completed send cannot substitute for a ready ACK");
            assertSame(pair.b.remote, pair.b.server.getConnection(7));
            assertTrue(pair.a.reasons().contains(P2PConnection.HandshakeReason.READY_SEND_COMPLETED));
            pair.a.scheduler.next();
            assertSame(pair.a.remote, pair.a.server.getConnection(7));
            assertEquals(2, pair.a.remote.readySends);
            assertEquals(1, pair.a.promotions);
            pair.a.scheduler.drain(); pair.b.scheduler.drain();
            assertEquals(2, pair.a.remote.readySends, "promotion cancels scheduled retries");
        }
    }

    @Test void firstAckDropAndDuplicateControlsNeverDuplicatePromotion() throws Exception {
        try (Pair pair = new Pair()) {
            pair.b.remote.wire = message -> {
                if (!message.isAcknowledgement() || pair.b.remote.ackSends > 1) pair.a.ready(message, pair.b.local());
                return AsyncTask.completed(null);
            };
            pair.start();
            assertNull(pair.a.server.getConnection(7));
            pair.a.scheduler.next();
            assertSame(pair.a.remote, pair.a.server.getConnection(7));
            pair.b.ready(new P2PConnection.PeerReadyMessage(false), pair.a.local());
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), pair.b.local());
            assertEquals(1, pair.a.promotions); assertEquals(1, pair.b.promotions);
            assertEquals(3, pair.b.remote.ackSends, "a duplicate valid READY receives an idempotent ACK");
        }
    }

    @Test void failedReliableSendIsObservedAndFiniteRetryBudgetDoesNotPromote() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = ignored -> AsyncTask.failed(new IOException("injected send failure"));
            pair.a.begin(); pair.a.scheduler.drain();
            assertEquals(6, pair.a.remote.readySends);
            assertEquals(6, pair.a.reasons().stream().filter(reason -> reason == P2PConnection.HandshakeReason.READY_SEND_FAILED).count());
            assertTrue(pair.a.reasons().contains(P2PConnection.HandshakeReason.READY_RETRY_LIMIT));
            assertNull(pair.a.server.getConnection(7));
            assertTrue(pair.a.scheduler.delays.stream().allMatch(delay -> delay == 10_000L));
            assertEquals("IOException", pair.a.events.get(1).failureClass());
            assertTrue(pair.a.reasons().contains(P2PConnection.HandshakeReason.READY_EXPIRED));
            assertTrue(pair.a.pending().isEmpty());
        }
    }

    @Test void disconnectAndConnectionCloseInvalidateQueuedRetries() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            pair.a.begin();
            pair.a.disconnect();
            pair.a.scheduler.drain();
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), pair.b.local());
            assertEquals(1, pair.a.remote.readySends);
            assertNull(pair.a.server.getConnection(7));
            pair.b.remote.wire = ignored -> AsyncTask.completed(null);
            pair.b.begin(); pair.b.server.close(); pair.b.scheduler.drain();
            assertEquals(1, pair.b.remote.readySends);
            assertEquals(0, pair.b.promotions);
        }
    }

    @Test void newSessionCancelsOldPendingAndRejectsOldSourceAcknowledgement() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            pair.a.begin();
            NostrRTCPeer oldSource = pair.b.local();
            NostrRTCPeer replacement = new NostrRTCPeer(oldSource.getPubkey(), oldSource.getApplicationId(),
                    oldSource.getProtocolId(), oldSource.getSessionId() + "-replacement", oldSource.getRoomPubkey(), null);
            pair.a.socketAvailable(replacement);
            RemotePeer current = pair.a.pending().values().iterator().next();
            assertNotSame(pair.a.remote, current);
            pair.a.ready(current, new P2PConnection.PeerReadyMessage(true), oldSource);
            assertEquals(0, pair.a.promotions);
            assertTrue(pair.a.reasons().contains(P2PConnection.HandshakeReason.READY_STALE_SESSION));
            pair.a.scheduler.next();
            assertEquals(1, pair.a.remote.readySends, "an obsolete scheduled retry cannot resend the old READY");
        }
    }

    @Test void changedAdmissionRejectsRecoveredAckAndLateSendCallbacksCannotReviveOldPeer() throws Exception {
        try (Pair pair = new Pair()) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<Void>> complete = new java.util.concurrent.atomic.AtomicReference<>();
            pair.a.remote.wire = ignored -> NGEPlatform.get().wrapPromise((resolve, reject) -> complete.set(resolve));
            pair.a.begin();
            pair.a.server.setPeerAdmission(ignored -> false);
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), pair.b.local());
            int events = pair.a.events.size();
            complete.get().accept(null); pair.a.scheduler.drain();
            assertEquals(events, pair.a.events.size(), "a late task callback after rejection must be discarded");
            assertEquals(0, pair.a.promotions);
            assertTrue(pair.a.reasons().contains(P2PConnection.HandshakeReason.ADMISSION_REJECTED));
        }
    }

    @Test void acknowledgementAtSixtySecondsIsRejectedEvenBeforeExpiryCallbackRuns() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            pair.a.begin(); pair.a.scheduler.clock.set(60_000_000_000L);
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), pair.b.local());
            assertNull(pair.a.server.getConnection(7)); assertEquals(0, pair.a.promotions);
            assertTrue(pair.a.pending().isEmpty());
            assertTrue(pair.a.reasons().contains(P2PConnection.HandshakeReason.READY_EXPIRED));
            pair.a.scheduler.drain(); assertEquals(1, pair.a.remote.readySends);
        }
    }

    @Test void acknowledgementJustBeforeExpiryStillRequiresCurrentAdmission() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            pair.a.begin(); pair.a.scheduler.clock.set(59_999_999_999L);
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), pair.b.local());
            assertSame(pair.a.remote, pair.a.server.getConnection(7));
            pair.a.scheduler.drain(); assertEquals(1, pair.a.promotions);
            pair.b.remote.wire = ignored -> AsyncTask.completed(null);
            pair.b.begin(); pair.b.server.setPeerAdmission(ignored -> {
                pair.b.scheduler.clock.set(60_000_000_000L); return true;
            });
            pair.b.ready(new P2PConnection.PeerReadyMessage(true), pair.a.local());
            assertNull(pair.b.server.getConnection(7), "expiry during admission must still prevent publication");
        }
    }

    @Test void oldExpiryAndTaskCompletionCannotRemoveReplacementAttempt() throws Exception {
        try (Pair pair = new Pair()) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<Void>> complete = new java.util.concurrent.atomic.AtomicReference<>();
            pair.a.remote.wire = ignored -> NGEPlatform.get().wrapPromise((resolve, reject) -> complete.set(resolve));
            pair.a.begin(); pair.a.scheduler.clock.set(50_000_000_000L);
            NostrRTCPeer old = pair.b.local();
            NostrRTCPeer replacement = new NostrRTCPeer(old.getPubkey(), old.getApplicationId(), old.getProtocolId(),
                    old.getSessionId() + "-replacement", old.getRoomPubkey(), null);
            pair.a.socketAvailable(replacement);
            RemotePeer current = pair.a.pending().values().iterator().next();
            pair.a.scheduler.clock.set(60_000_000_000L);
            int events = pair.a.events.size(); complete.get().accept(null);
            assertEquals(events, pair.a.events.size());
            pair.a.scheduler.next();
            assertSame(current, pair.a.pending().values().iterator().next());
            pair.a.ready(current, new P2PConnection.PeerReadyMessage(true), replacement);
            assertSame(current, pair.a.server.getConnection(current.getId())); assertEquals(1, pair.a.promotions);
        }
    }

    @Test void replacementOrDisconnectDuringAdmissionCannotPublishOldSession() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            pair.a.begin();
            NostrRTCPeer old = pair.b.local();
            NostrRTCPeer replacement = new NostrRTCPeer(old.getPubkey(), old.getApplicationId(), old.getProtocolId(),
                    old.getSessionId() + "-replacement", old.getRoomPubkey(), null);
            pair.a.server.setPeerAdmission(ignored -> {
                try { pair.a.socketAvailable(replacement); }
                catch (Exception failure) { throw new AssertionError(failure); }
                return true;
            });
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), old);
            RemotePeer current = pair.a.pending().values().iterator().next();
            assertNotSame(pair.a.remote, current);
            assertNull(pair.a.server.getConnection(7)); assertEquals(0, pair.a.promotions);
            pair.a.server.setPeerAdmission(ignored -> true);
            pair.a.ready(current, new P2PConnection.PeerReadyMessage(true), replacement);
            assertSame(current, pair.a.server.getConnection(current.getId())); assertEquals(1, pair.a.promotions);

            pair.b.remote.wire = ignored -> AsyncTask.completed(null);
            pair.b.begin(); pair.b.server.setPeerAdmission(ignored -> {
                try { pair.b.disconnect(); }
                catch (Exception failure) { throw new AssertionError(failure); }
                return true;
            });
            pair.b.ready(new P2PConnection.PeerReadyMessage(true), pair.a.local());
            assertNull(pair.b.server.getConnection(7)); assertEquals(0, pair.b.promotions);
            assertTrue(pair.b.pending().isEmpty());
        }
    }

    @Test void expiredSessionCannotRestartItsBudgetOrAdmitDelayedBooleanAck() throws Exception {
        try (Pair pair = new Pair()) {
            pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            pair.a.begin(); pair.a.scheduler.drain();
            assertEquals(6, pair.a.remote.readySends); assertTrue(pair.a.pending().isEmpty());
            NostrRTCPeer expired = pair.b.local();
            pair.a.socketAvailable(expired);
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), expired);
            assertTrue(pair.a.pending().isEmpty()); assertEquals(0, pair.a.promotions);
            assertEquals(6, pair.a.remote.readySends);
            NostrRTCPeer fresh = new NostrRTCPeer(expired.getPubkey(), expired.getApplicationId(), expired.getProtocolId(),
                    expired.getSessionId() + "-fresh", expired.getRoomPubkey(), null);
            pair.a.socketAvailable(fresh);
            RemotePeer current = pair.a.pending().values().iterator().next();
            pair.a.ready(current, new P2PConnection.PeerReadyMessage(true), expired);
            assertNull(pair.a.server.getConnection(current.getId()));
            pair.a.ready(current, new P2PConnection.PeerReadyMessage(true), fresh);
            assertSame(current, pair.a.server.getConnection(current.getId())); assertEquals(1, pair.a.promotions);
        }
    }

    @Test void promotionDiscardsDelayedReadyOutcomeButDuplicateReadyCanObserveItsAckTask() throws Exception {
        try (Pair pair = new Pair()) {
            java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<Void>> complete = new java.util.concurrent.atomic.AtomicReference<>();
            pair.a.remote.wire = message -> message.isAcknowledgement() ? AsyncTask.completed(null)
                    : NGEPlatform.get().wrapPromise((resolve, reject) -> complete.set(resolve));
            pair.a.begin();
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), pair.b.local());
            int count = pair.a.events.size(); complete.get().accept(null);
            assertEquals(count, pair.a.events.size(), "promotion retires READY task diagnostics with the attempt");
            pair.a.ready(new P2PConnection.PeerReadyMessage(false), pair.b.local());
            assertEquals(P2PConnection.HandshakeReason.ACK_SEND_COMPLETED,
                    pair.a.events.get(pair.a.events.size() - 1).reason());
            assertEquals(1, pair.a.promotions);
        }
    }

    @Test @SuppressWarnings("unchecked")
    void expiryTombstoneCapacityFailsClosedWithoutEvictingOldSessions() throws Exception {
        try (Pair pair = new Pair(); NostrKeyPair other = new NostrKeyPair()) {
            java.util.Set<String> expired = (java.util.Set<String>) field(P2PConnection.class,
                    "expiredReadySessions").get(pair.a.server);
            for (int i = 0; i < 4095; i++) expired.add("retired-native-session-" + i);
            pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            WirePeer first = pair.a.remote;
            pair.a.begin();
            NostrRTCPeer source = pair.b.local();
            NostrRTCPeer second = new NostrRTCPeer(other.getPublicKey(), source.getApplicationId(), source.getProtocolId(),
                    "other-native-session", source.getRoomPubkey(), null);
            pair.a.bind(second); pair.a.remote.wire = ignored -> AsyncTask.completed(null);
            pair.a.begin(); pair.a.scheduler.drain();
            assertEquals(6, first.readySends); assertEquals(6, pair.a.remote.readySends);
            assertEquals(4096, expired.size(), "concurrent live attempts cannot grow tombstones beyond the cap");
            assertTrue(expired.contains("retired-native-session-0"));
            assertTrue(expired.contains(source.getPubkey().asHex() + "|" + source.getSessionId()));
            assertTrue(pair.a.pending().isEmpty());
            NostrRTCPeer fresh = new NostrRTCPeer(other.getPublicKey(), source.getApplicationId(), source.getProtocolId(),
                    "fresh-native-session", source.getRoomPubkey(), null);
            pair.a.socketAvailable(fresh);
            pair.a.ready(first, new P2PConnection.PeerReadyMessage(true), source);
            pair.a.ready(new P2PConnection.PeerReadyMessage(true), second);
            assertTrue(pair.a.pending().isEmpty()); assertEquals(0, pair.a.promotions);
            pair.a.server.close(); assertTrue(expired.isEmpty(), "capacity resets only with the connection lifetime");
        }
    }

    private static final class Pair implements AutoCloseable {
        private final NostrKeyPair room = new NostrKeyPair();
        private final Fixture a = new Fixture(room);
        private final Fixture b = new Fixture(room);
        private Pair() throws Exception {
            a.bind(b.local()); b.bind(a.local());
            a.remote.wire = message -> { b.ready(message, a.local()); return AsyncTask.completed(null); };
            b.remote.wire = message -> { a.ready(message, b.local()); return AsyncTask.completed(null); };
        }
        private void start() throws Exception { a.begin(); b.begin(); }
        @Override public void close() { a.close(); b.close(); room.close(); }
    }

    private static final class Fixture implements AutoCloseable {
        private final NostrKeyPair key = new NostrKeyPair();
        private final NostrKeyPair room;
        private final P2PConnection server;
        private final Scheduler scheduler = new Scheduler();
        private final List<P2PConnection.HandshakeDiagnostic> events = new ArrayList<>();
        private int promotions;
        private WirePeer remote;
        private Fixture(NostrKeyPair room) throws Exception {
            this.room = room;
            Runner runner = new Runner() {
                public void run(Runnable task) { task.run(); }
                public void enqueue(Runnable task) { task.run(); }
                public void checkThread() {}
            };
            server = new P2PConnection(new NostrKeyPairSigner(key), "peer-ready-recovery", 1,
                    room.getPrivateKey(), null, new NostrPool(), runner);
            Field executor = field(P2PConnection.class, "readyExecutor");
            ((AsyncExecutor) executor.get(server)).close(); executor.set(server, scheduler);
            field(P2PConnection.class, "readyClock").set(server, (java.util.function.LongSupplier) scheduler.clock::get);
            server.setHandshakeDiagnosticObserver(events::add);
            server.addConnectionListener(new ConnectionListener() {
                public void connectionAdded(Server ignored, HostedConnection connection) { promotions++; }
                public void connectionRemoved(Server ignored, HostedConnection connection) {}
            });
        }
        private NostrRTCLocalPeer local() { return (NostrRTCLocalPeer) server.getRtcRoom().getLocalPeerInfo(); }
        private void bind(NostrRTCPeer peer) throws Exception {
            remote = new WirePeer(server, local(), peer);
            pending().put(peer.getPubkey().asHex() + "|" + peer.getSessionId(), remote);
        }
        @SuppressWarnings("unchecked")
        private Map<String, RemotePeer> pending() throws Exception {
            return (Map<String, RemotePeer>) field(P2PConnection.class, "pendingConnections").get(server);
        }
        private void begin() throws Exception {
            Method begin = P2PConnection.class.getDeclaredMethod("beginPeerReady", RemotePeer.class);
            begin.setAccessible(true); begin.invoke(server, remote);
        }
        private void ready(P2PConnection.PeerReadyMessage message, NostrRTCPeer source) { ready(remote, message, source); }
        private void ready(RemotePeer peer, P2PConnection.PeerReadyMessage message, NostrRTCPeer source) {
            try {
                Method ready = P2PConnection.class.getDeclaredMethod("handlePeerReady", RemotePeer.class,
                        NostrRTCPeer.class, P2PConnection.PeerReadyMessage.class);
                ready.setAccessible(true); ready.invoke(server, peer, source, message);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }
        private List<P2PConnection.HandshakeReason> reasons() { return events.stream().map(P2PConnection.HandshakeDiagnostic::reason).toList(); }
        private NostrRTCSocket socket(NostrRTCPeer peer) throws Exception {
            Constructor<NostrRTCSocket> constructor = NostrRTCSocket.class.getDeclaredConstructor(AsyncExecutor.class,
                    NostrRTCPeer.class, NostrKeyPair.class, NostrRTCLocalPeer.class, RTCSettings.class, NostrTURNPool.class);
            constructor.setAccessible(true);
            return constructor.newInstance(scheduler, peer, room, local(), RTCSettings.getDefault("peer-ready-recovery", "peer-ready-recovery:1"), null);
        }
        @SuppressWarnings("unchecked")
        private void disconnect() throws Exception {
            List<NostrRTCRoomPeerDisconnectListener> listeners = (List<NostrRTCRoomPeerDisconnectListener>) field(
                    server.getRtcRoom().getClass(), "onDisconnectionListeners").get(server.getRtcRoom());
            NostrRTCSocket socket = socket(remote.getRemotePeer());
            for (NostrRTCRoomPeerDisconnectListener listener : listeners) listener.onRoomPeerDisconnected(remote.getRemotePeer(), socket);
        }
        @SuppressWarnings("unchecked")
        private void socketAvailable(NostrRTCPeer peer) throws Exception {
            List<NostrRTCPeerSocketAvailableListener> listeners = (List<NostrRTCPeerSocketAvailableListener>) field(
                    server.getRtcRoom().getClass(), "onSocketAvailable").get(server.getRtcRoom());
            NostrRTCSocket socket = socket(peer);
            for (NostrRTCPeerSocketAvailableListener listener : listeners) listener.onRoomPeerSocketAvailable(peer, socket);
        }
        @Override public void close() { server.close(); key.close(); }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }

    private static final class WirePeer extends RemotePeer {
        private Function<P2PConnection.PeerReadyMessage, AsyncTask<Void>> wire;
        private int readySends;
        private int ackSends;
        private WirePeer(P2PConnection server, NostrRTCPeer local, NostrRTCPeer remote) {
            super(7, server.getRtcRoom(), local, remote, server);
        }
        @Override AsyncTask<Void> sendPeerReady(P2PConnection.PeerReadyMessage message) {
            if (message.isAcknowledgement()) ackSends++; else readySends++;
            return wire.apply(message);
        }
    }

    private static final class Scheduler implements AsyncExecutor {
        private record Scheduled(Callable<?> task, long dueNanos) {}
        private final ArrayDeque<Scheduled> scheduled = new ArrayDeque<>();
        private final List<Long> delays = new ArrayList<>();
        private final AtomicLong clock = new AtomicLong();
        public <T> AsyncTask<T> runLater(Callable<T> task, long delay, TimeUnit unit) {
            delays.add(unit.toMillis(delay)); scheduled.add(new Scheduled(task, clock.get() + unit.toNanos(delay)));
            return AsyncTask.completed(null);
        }
        public <T> AsyncTask<T> run(Callable<T> task) { throw new UnsupportedOperationException(); }
        private void next() throws Exception {
            Scheduled next = scheduled.removeFirst(); clock.set(Math.max(clock.get(), next.dueNanos())); next.task().call();
        }
        private void drain() throws Exception { while (!scheduled.isEmpty()) next(); }
        public void close() {}
    }
}
