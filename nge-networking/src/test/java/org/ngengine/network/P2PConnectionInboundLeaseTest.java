package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;

/** Exercises real admission, decoding and Netcode dispatch using the existing no-socket fixture. */
final class P2PConnectionInboundLeaseTest {
    @Test void isolatedRunLoadsCandidateProductionClassesFirst() throws Exception {
        String expected = System.getProperty("nge.inboundLease.classes");
        if (expected != null) {
            assertEquals(Path.of(expected).toRealPath(), Path.of(P2PConnection.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toRealPath());
            assertEquals(Path.of(expected).toRealPath(), Path.of(NetcodeManagerComponent.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toRealPath());
        }
        try (var bytecode = P2PConnection.class.getResourceAsStream("P2PConnection.class")) {
            assertEquals(65, bytecode.readAllBytes()[7] & 0xff, "the production candidate targets Java 21");
        }
    }

    @Test void independentHandlesCannotDoubleReleaseOriginalDispatchOrEachOther() throws Exception {
        try (Fixture f = new Fixture()) {
            f.receive(f.peer);
            Message message = f.onlyMessage();
            P2PConnection.InboundMessageLease first = f.server.tryRetainInboundMessage(f.peer, message);
            P2PConnection.InboundMessageLease second = f.server.tryRetainInboundMessage(f.peer, message);
            assertNotNull(first); assertNotNull(second); assertNotSame(first, second);
            long bytes = f.bytes();
            first.close(); first.close();
            assertNull(field(first.getClass(), "reservation").get(first));
            assertNull(field(first.getClass(), "connection").get(first));
            assertTrue(java.lang.reflect.Modifier.isStatic(first.getClass().getModifiers()),
                    "a closed lease must not retain the enclosing connection implicitly");
            assertEquals(bytes, f.bytes()); assertEquals(1, f.count());
            f.drain(); f.manager.updateAppLogic(null, 0);
            assertEquals(bytes, f.bytes()); assertEquals(bytes, second.getResourceBytes());
            assertEquals(1, f.count());
            second.close(); second.close(); f.assertEmpty();
            assertNull(f.server.tryRetainInboundMessage(f.peer, message));
        }
    }

    @Test void retainedHandleAndBalancedLegacyOwnerShareOneCharge() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease first = f.acquire(f.peer);
            Message message = f.onlyMessage();
            P2PConnection.InboundMessageLease second = first.retain();
            P2PConnection.InboundReservation legacy = f.server.retainInboundMessage(f.peer, message);
            assertNotNull(second); assertNotSame(first, second); assertNotNull(legacy);
            assertTrue(second.ensureResourceBytes(1_000_000));
            first.close(); first.close();
            assertEquals(1_000_000, legacy.getResourceBytes()); assertEquals(1_000_000, f.bytes());
            legacy.close(); assertEquals(1_000_000, f.bytes());
            second.close(); f.assertEmpty();
        }
    }

    @Test void absoluteGrowthIsMonotonicAndSharedAcrossConcurrentOwners() throws Exception {
        try (Fixture f = new Fixture(); ExecutorService pool = Executors.newFixedThreadPool(8)) {
            P2PConnection.InboundMessageLease first = f.acquire(f.peer), second = first.retain();
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                int total = (i % 4 + 1) * 512_000;
                P2PConnection.InboundMessageLease owner = i % 2 == 0 ? first : second;
                results.add(pool.submit(() -> { start.await(); return owner.ensureResourceBytes(total); }));
            }
            start.countDown();
            for (Future<Boolean> result : results) assertTrue(result.get(3, TimeUnit.SECONDS));
            assertEquals(2_048_000, f.bytes()); assertEquals(2_048_000, first.getResourceBytes());
            assertTrue(second.ensureResourceBytes(1)); assertEquals(2_048_000, second.getResourceBytes());
            first.close(); assertEquals(2_048_000, f.bytes()); assertEquals(1, f.count());
            second.close(); f.assertEmpty();
        }
    }

    @Test void perPeerBoundaryInvalidInputsAndOverflowRefuseWithoutMutation() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease lease = f.acquire(f.peer);
            assertTrue(lease.ensureResourceBytes((int) P2PConnection.MAX_PEER_INBOUND_BYTES));
            List<Long> before = f.counters();
            assertTrue(lease.ensureResourceBytes((int) P2PConnection.MAX_PEER_INBOUND_BYTES));
            assertFalse(lease.ensureResourceBytes((int) P2PConnection.MAX_PEER_INBOUND_BYTES + 1));
            assertFalse(lease.ensureResourceBytes(Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> lease.ensureResourceBytes(0));
            assertThrows(IllegalArgumentException.class, () -> lease.ensureResourceBytes(-1));
            assertThrows(IllegalArgumentException.class, () -> lease.ensureResourceBytes(Integer.MIN_VALUE));
            assertEquals(before, f.counters());
            lease.close(); f.assertEmpty();
            assertThrows(IllegalStateException.class, lease::getResourceBytes);
            assertThrows(IllegalStateException.class, lease::retain);
            assertThrows(IllegalStateException.class, () -> lease.ensureResourceBytes(1));
        }
    }

    @Test void globalBoundaryAndCrossPeerReleasePreserveEveryCounterOnRefusal() throws Exception {
        try (Fixture f = new Fixture()) {
            List<P2PConnection.InboundMessageLease> leases = new ArrayList<>();
            leases.add(f.acquire(f.peer));
            for (int i = 1; i < 9; i++) leases.add(f.acquire(f.addPeer(20 + i, false)));
            int ninthBytes = leases.get(8).getResourceBytes();
            for (int i = 0; i < 7; i++)
                assertTrue(leases.get(i).ensureResourceBytes((int) P2PConnection.MAX_PEER_INBOUND_BYTES));
            assertTrue(leases.get(7).ensureResourceBytes((int) P2PConnection.MAX_PEER_INBOUND_BYTES - ninthBytes));
            assertEquals(P2PConnection.MAX_INBOUND_BYTES, f.bytes()); assertEquals(9, f.count());
            List<Long> before = f.counters();
            assertFalse(leases.get(8).ensureResourceBytes(ninthBytes + 1));
            assertFalse(leases.get(7).ensureResourceBytes((int) P2PConnection.MAX_PEER_INBOUND_BYTES));
            assertEquals(before, f.counters());
            leases.get(8).close();
            assertTrue(leases.get(7).ensureResourceBytes((int) P2PConnection.MAX_PEER_INBOUND_BYTES));
            assertEquals(P2PConnection.MAX_INBOUND_BYTES, f.bytes()); assertEquals(8, f.count());
            for (P2PConnection.InboundMessageLease lease : leases) lease.close();
            f.assertEmpty();
        }
    }

    @Test void referenceSaturationDoesNotWrapOrAcquireAnOwner() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease lease = f.acquire(f.peer);
            Message message = f.onlyMessage();
            Object reservation = field(lease.getClass(), "reservation").get(lease);
            Field references = field(reservation.getClass(), "references");
            int original = references.getInt(reservation);
            references.setInt(reservation, Integer.MAX_VALUE);
            try {
                List<Long> before = f.counters();
                assertNull(lease.retain());
                assertNull(f.server.tryRetainInboundMessage(f.peer, message));
                assertNull(f.server.retainInboundMessage(f.peer, message));
                assertEquals(Integer.MAX_VALUE, references.getInt(reservation));
                assertEquals(before, f.counters());
            } finally { references.setInt(reservation, original); lease.close(); }
            f.assertEmpty();
        }
    }

    @Test void factoryRequiresExactCurrentPeerAndMessageIdentity() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease lease = f.acquire(f.peer);
            RemotePeer other = f.addPeer(44, false);
            List<Long> before = f.counters();
            assertNull(f.server.tryRetainInboundMessage(other, f.onlyMessage()));
            assertNull(f.server.tryRetainInboundMessage(null, f.onlyMessage()));
            assertNull(f.server.tryRetainInboundMessage(f.peer, new P2PConnectionInboundBoundsTest.PayloadMessage()));
            assertNull(f.server.tryRetainInboundMessage(f.peer, null));
            assertEquals(before, f.counters());
            lease.close(); f.assertEmpty();
        }
    }

    @Test void retirementKeepsHeldChargeButRefusesAllNewWork() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease lease = f.acquire(f.peer), second = lease.retain();
            Message message = f.onlyMessage();
            assertTrue(lease.ensureResourceBytes(900_000));
            f.server.failInboundPeer(f.peer, "fixture retirement"); f.drain();
            assertFalse(f.server.isCurrentPeer(f.peer));
            assertEquals(900_000, f.bytes()); assertEquals(1, f.count());
            assertEquals(900_000, lease.getResourceBytes());
            List<Long> before = f.counters();
            assertFalse(lease.ensureResourceBytes(1)); assertFalse(lease.ensureResourceBytes(900_001));
            assertNull(lease.retain()); assertNull(f.server.tryRetainInboundMessage(f.peer, message));
            assertEquals(before, f.counters());
            lease.close(); assertEquals(900_000, f.bytes()); second.close(); f.assertEmpty();
            assertTrue(((Map<?, ?>) field(P2PConnection.class, "inboundPeers").get(f.server)).isEmpty());
        }
    }

    @Test void closingConnectionKeepsOutstandingChargeUntilItsOwnerCloses() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease lease = f.acquire(f.peer);
            Message message = f.onlyMessage();
            assertTrue(lease.ensureResourceBytes(1_200_000));
            f.server.close();
            assertEquals(1_200_000, f.bytes()); assertEquals(1, f.count());
            assertFalse(lease.ensureResourceBytes(1)); assertNull(lease.retain());
            assertNull(f.server.tryRetainInboundMessage(f.peer, message));
            assertEquals(1_200_000, lease.getResourceBytes());
            lease.close(); lease.close(); f.assertEmpty();
        }
    }

    @Test void replacementNativeSessionCannotAcquireOldMessageOrGrowOldLease() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease old = f.acquire(f.peer);
            Message stale = f.onlyMessage();
            int oldBytes = old.getResourceBytes();
            RemotePeer replacement = f.addPeer(f.peer.getId(), true);
            assertEquals(f.peer.getRemotePeer().getPubkey(), replacement.getRemotePeer().getPubkey());
            assertNotEquals(f.peer.getRemotePeer().getSessionId(), replacement.getRemotePeer().getSessionId());
            assertFalse(old.ensureResourceBytes(1)); assertNull(old.retain());
            assertNull(f.server.tryRetainInboundMessage(f.peer, stale));
            assertNull(f.server.tryRetainInboundMessage(replacement, stale));
            invoke(P2PConnection.class, f.server, "queuePeerRetirement",
                    new Class<?>[] {RemotePeer.class, boolean.class, P2PConnection.PeerRetirementReason.class},
                    f.peer, true, P2PConnection.PeerRetirementReason.NATIVE_SESSION_REPLACED);
            f.drain();
            P2PConnection.InboundMessageLease current = f.acquire(replacement);
            assertTrue(current.ensureResourceBytes(500_000));
            assertEquals(oldBytes + 500_000L, f.bytes());
            old.close(); assertEquals(500_000, f.bytes()); current.close(); f.assertEmpty();
        }
    }

    @Test void ensureAndDuplicateCloseAreAtomicUnderContention() throws Exception {
        try (Fixture f = new Fixture(); ExecutorService pool = Executors.newFixedThreadPool(3)) {
            P2PConnection.InboundMessageLease keeper = f.acquire(f.peer);
            for (int i = 0; i < 100; i++) {
                P2PConnection.InboundMessageLease owner = keeper.retain();
                int target = 10_000 + i * 1_000;
                CountDownLatch start = new CountDownLatch(1);
                Future<?> ensure = pool.submit(() -> {
                    start.await();
                    try { assertTrue(owner.ensureResourceBytes(target)); }
                    catch (IllegalStateException expected) { /* Close won the lock. */ }
                    return null;
                });
                Future<?> close = pool.submit(() -> { start.await(); owner.close(); return null; });
                Future<?> duplicate = pool.submit(() -> { start.await(); owner.close(); return null; });
                start.countDown(); ensure.get(3, TimeUnit.SECONDS); close.get(3, TimeUnit.SECONDS); duplicate.get(3, TimeUnit.SECONDS);
                assertNull(field(owner.getClass(), "reservation").get(owner));
                assertEquals(1, f.count()); assertEquals(keeper.getResourceBytes(), f.bytes());
                assertTrue(f.bytes() >= 0 && f.bytes() <= target);
            }
            keeper.close(); f.assertEmpty();
        }
    }

    @Test void finalOwnerCloseRacingGrowthReleasesExactlyOnce() throws Exception {
        try (Fixture f = new Fixture(); ExecutorService pool = Executors.newFixedThreadPool(3)) {
            for (int i = 0; i < 100; i++) {
                P2PConnection.InboundMessageLease lease = f.acquire(f.peer);
                CountDownLatch start = new CountDownLatch(1);
                Future<?> ensure = pool.submit(() -> {
                    start.await();
                    try { assertTrue(lease.ensureResourceBytes(2_000_000)); }
                    catch (IllegalStateException expected) { /* Final close won the lock. */ }
                    return null;
                });
                Future<?> close = pool.submit(() -> { start.await(); lease.close(); return null; });
                Future<?> duplicate = pool.submit(() -> { start.await(); lease.close(); return null; });
                start.countDown(); ensure.get(3, TimeUnit.SECONDS); close.get(3, TimeUnit.SECONDS); duplicate.get(3, TimeUnit.SECONDS);
                f.assertEmpty();
                assertThrows(IllegalStateException.class, () -> lease.ensureResourceBytes(1));
            }
        }
    }

    @Test void validationAndAllocationFailureReleaseTheirAcquiredOwners() throws Exception {
        try (Fixture f = new Fixture()) {
            f.manager.registerMessageHandler(P2PConnectionInboundBoundsTest.PayloadMessage.class, (peer, message) -> {
                assertThrows(IllegalArgumentException.class, () -> {
                    try (P2PConnection.InboundMessageLease lease = f.server.tryRetainInboundMessage(peer, message)) {
                        assertNotNull(lease); assertTrue(lease.ensureResourceBytes(500_000));
                        throw new IllegalArgumentException("simulated validation rejection");
                    }
                });
                assertThrows(OutOfMemoryError.class, () -> {
                    try (P2PConnection.InboundMessageLease lease = f.server.tryRetainInboundMessage(peer, message)) {
                        assertNotNull(lease); assertTrue(lease.ensureResourceBytes(700_000));
                        throw new OutOfMemoryError("simulated allocation failure, no heap exhaustion");
                    }
                });
            });
            f.receive(f.peer); f.drain(); f.manager.updateAppLogic(null, 0); f.assertEmpty();
            assertEquals(0L, field(NetcodeManagerComponent.class, "inboundBytes").getLong(f.manager));
        }
    }

    @Test void netcodeCapturedAdmissionCountersBalanceAfterSharedLeaseGrowth() throws Exception {
        try (Fixture f = new Fixture()) {
            f.receive(f.peer); f.drain();
            Message message = f.onlyMessage();
            long admittedBytes = field(NetcodeManagerComponent.class, "inboundBytes").getLong(f.manager);
            P2PConnection.InboundMessageLease lease = f.server.tryRetainInboundMessage(f.peer, message);
            assertTrue(lease.ensureResourceBytes(2_000_000));
            assertEquals(admittedBytes, field(NetcodeManagerComponent.class, "inboundBytes").getLong(f.manager));
            f.manager.updateAppLogic(null, 0);
            assertEquals(0L, field(NetcodeManagerComponent.class, "inboundBytes").getLong(f.manager));
            assertTrue(((Map<?, ?>) field(NetcodeManagerComponent.class, "inboundPeerUsage").get(f.manager)).isEmpty());
            assertEquals(2_000_000, f.bytes()); assertEquals(1, f.count());
            lease.close(); f.assertEmpty();
        }
    }

    @Test void netcodeAdmissionUsesOneSnapshotWhenGrowthOccursBetweenReadAndEnqueue() throws Exception {
        try (Fixture f = new Fixture()) {
            f.receive(f.peer);
            try (P2PConnection.InboundMessageLease lease = f.server.tryRetainInboundMessage(f.peer, f.onlyMessage())) {
                int admissionBytes = lease.getResourceBytes();
                // size() is evaluated after the admission read but before constructing the queued record.
                java.util.ArrayDeque<Object> queue = new java.util.ArrayDeque<>() {
                    private boolean grew;
                    @Override public int size() {
                        if (!grew) { grew = true; assertTrue(lease.ensureResourceBytes(2_000_000)); }
                        return super.size();
                    }
                };
                field(NetcodeManagerComponent.class, "inboundMessages").set(f.manager, queue);
                f.drain();
                assertEquals(2_000_000, lease.getResourceBytes());
                assertEquals(admissionBytes, field(NetcodeManagerComponent.class, "inboundBytes").getLong(f.manager));
                int removalBytes = field(queue.peek().getClass(), "bytes").getInt(queue.peek());
                f.manager.updateAppLogic(null, 0);
                long remainingBytes = field(NetcodeManagerComponent.class, "inboundBytes").getLong(f.manager);
                System.out.println("ADMISSION_SNAPSHOT admissionBytes=" + admissionBytes + " removalBytes=" + removalBytes
                        + " postDispatchQueueBytes=" + remainingBytes);
                assertAll(
                        () -> assertEquals(admissionBytes, removalBytes, "removal must use the exact admission snapshot"),
                        () -> assertEquals(0L, remainingBytes, "queue accounting must remain balanced after growth"));
                assertTrue(((Map<?, ?>) field(NetcodeManagerComponent.class, "inboundPeerUsage").get(f.manager)).isEmpty());
                assertEquals(2_000_000, f.bytes());
            }
            f.assertEmpty();
        }
    }

    @Test void finalMessageReleasePreservesNativeInputAndCallbackReleaseWakesWaitingDecode() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease keeper = f.acquire(f.peer);
            f.removeNetcodeListener(); field(f.runner.getClass(), "inline").setBoolean(f.runner, true);
            AtomicReference<P2PConnection.InboundMessageLease> first = new AtomicReference<>();
            CountDownLatch nextDispatched = new CountDownLatch(1);
            f.server.addMessageListener((peer, message) -> {
                if (first.get() == null) first.set(f.server.tryRetainInboundMessage((RemotePeer) peer, message));
                else nextDispatched.countDown();
            }, P2PConnectionInboundBoundsTest.PayloadMessage.class);
            // Drive actual callback stages separately so native ownership can outlive synchronous dispatch.
            P2PConnection.InboundReservation callback = f.reserve(f.frame());
            Object state = field(callback.getClass(), "state").get(callback);
            invoke(P2PConnection.class, f.server, "drainWire", new Class<?>[] {state.getClass()}, state);
            assertNotNull(first.get());
            int nativeBytes = field(callback.getClass(), "nativeInputBytes").getInt(callback);
            first.get().close(); first.get().close();
            assertEquals(keeper.getResourceBytes() + nativeBytes, f.bytes()); assertEquals(2, f.count());
            assertEquals(nativeBytes, field(P2PConnection.class, "inboundCallbackBytes").getLong(f.server));
            assertTrue(keeper.ensureResourceBytes((int) (P2PConnection.MAX_PEER_INBOUND_BYTES
                    - DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES)));
            P2PConnection.InboundReservation queued = f.reserve(f.frame());
            invoke(queued.getClass(), queued, "releaseNativeInput", new Class<?>[0]);
            int decoded = field(f.peer.getProtocol().getClass(), "decodes").getInt(f.peer.getProtocol());
            invoke(P2PConnection.class, f.server, "drainWire", new Class<?>[] {state.getClass()}, state);
            assertEquals(decoded, field(f.peer.getProtocol().getClass(), "decodes").getInt(f.peer.getProtocol()));
            assertEquals(1, ((java.util.Collection<?>) field(state.getClass(), "wire").get(state)).size());
            invoke(callback.getClass(), callback, "releaseNativeInput", new Class<?>[0]);
            assertTrue(nextDispatched.await(3, TimeUnit.SECONDS), "final native callback release must wake blocked wire decode");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (f.count() != 1 && System.nanoTime() < deadline) Thread.sleep(1);
            assertEquals(keeper.getResourceBytes(), f.bytes()); assertEquals(1, f.count());
            assertEquals(0, field(P2PConnection.class, "inboundCallbackBytes").getLong(f.server));
            keeper.close(); f.assertEmpty();
        }
    }

    @Test void finalLeaseReleaseWakesDecodeOutsideInboundLock() throws Exception {
        try (Fixture f = new Fixture()) {
            P2PConnection.InboundMessageLease lease = f.acquire(f.peer);
            assertTrue(lease.ensureResourceBytes(2_000_000));
            f.removeNetcodeListener(); field(f.runner.getClass(), "inline").setBoolean(f.runner, true);
            CountDownLatch dispatched = new CountDownLatch(1);
            AtomicReference<Boolean> heldLock = new AtomicReference<>();
            Object lock = field(P2PConnection.class, "inboundLock").get(f.server);
            f.server.addMessageListener((peer, message) -> {
                heldLock.set(Thread.holdsLock(lock)); dispatched.countDown();
            }, P2PConnectionInboundBoundsTest.PayloadMessage.class);
            f.receive(f.peer);
            assertEquals(2, f.count()); assertNull(heldLock.get());
            lease.close(); lease.close();
            assertTrue(dispatched.await(3, TimeUnit.SECONDS)); assertEquals(Boolean.FALSE, heldLock.get());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (f.count() != 0 && System.nanoTime() < deadline) Thread.sleep(1);
            f.assertEmpty();
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final Class<?> type = Class.forName("org.ngengine.network.P2PConnectionInboundBoundsTest$Fixture");
        private final Object delegate;
        private final P2PConnection server;
        private final RemotePeer peer;
        private final Object runner;
        private final NetcodeManagerComponent manager;
        private final DynamicSerializerProtocol sender;
        private final List<NostrKeyPair> keys = new ArrayList<>();
        private Message last;

        private Fixture() throws Exception {
            Constructor<?> constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
            delegate = constructor.newInstance();
            server = (P2PConnection) field(type, "server").get(delegate);
            peer = (RemotePeer) field(type, "peer").get(delegate);
            runner = field(type, "runner").get(delegate);
            manager = (NetcodeManagerComponent) field(type, "manager").get(delegate);
            sender = (DynamicSerializerProtocol) field(type, "sender").get(delegate);
            manager.registerMessageHandler(P2PConnectionInboundBoundsTest.PayloadMessage.class, (source, message) -> {});
            server.addMessageListener((source, message) -> last = message);
        }

        private ByteBuffer frame() {
            P2PConnectionInboundBoundsTest.PayloadMessage message = new P2PConnectionInboundBoundsTest.PayloadMessage();
            message.payload = ByteBuffer.allocate(8); message.setReliable(true);
            return sender.toByteBuffer(message, null);
        }

        private void receive(RemotePeer source) throws Exception {
            invoke(type, delegate, "receiveOn", new Class<?>[] {String.class, ByteBuffer.class, boolean.class, NostrRTCPeer.class},
                    "nge-1-r", frame(), true, source.getRemotePeer());
        }

        private void drain() throws Exception { invoke(runner.getClass(), runner, "drain", new Class<?>[0]); }

        private P2PConnection.InboundMessageLease acquire(RemotePeer source) throws Exception {
            receive(source); drain();
            P2PConnection.InboundMessageLease lease = server.tryRetainInboundMessage(source, last);
            assertNotNull(lease); manager.updateAppLogic(null, 0); return lease;
        }

        private Message onlyMessage() throws Exception {
            Map<?, ?> reservations = (Map<?, ?>) field(P2PConnection.class, "inboundReservations").get(server);
            assertEquals(1, reservations.size()); return (Message) reservations.keySet().iterator().next();
        }

        @SuppressWarnings("unchecked") private RemotePeer addPeer(int id, boolean sameIdentity) throws Exception {
            NostrRTCPeer original = peer.getRemotePeer();
            NostrKeyPair key = new NostrKeyPair(); keys.add(key);
            NostrRTCPeer nativePeer = new NostrRTCPeer(sameIdentity ? original.getPubkey() : key.getPublicKey(),
                    original.getApplicationId(), original.getProtocolId(), "lease-session-" + id, original.getRoomPubkey(), null);
            Class<?> peerType = peer.getClass();
            Constructor<?> constructor = peerType.getDeclaredConstructor(P2PConnection.class, NostrRTCPeer.class,
                    NostrRTCPeer.class, int.class); constructor.setAccessible(true);
            RemotePeer added = (RemotePeer) constructor.newInstance(server, server.getRtcRoom().getLocalPeerInfo(), nativePeer, id);
            ((Map<Integer, RemotePeer>) field(P2PConnection.class, "connections").get(server)).put(id, added);
            return added;
        }

        @SuppressWarnings("unchecked") private void removeNetcodeListener() throws Exception {
            server.removeMessageListener((MessageListener<HostedConnection>) field(NetcodeManagerComponent.class,
                    "messageListener").get(manager));
        }

        private P2PConnection.InboundReservation reserve(ByteBuffer frame) throws Exception {
            P2PConnection.InboundReservation result = (P2PConnection.InboundReservation) invoke(P2PConnection.class, server,
                    "reserveInbound", new Class<?>[] {RemotePeer.class, ByteBuffer.class, boolean.class}, peer, frame, true);
            assertNotNull(result); return result;
        }

        private long bytes() throws Exception {
            synchronized (field(P2PConnection.class, "inboundLock").get(server)) {
                return field(P2PConnection.class, "inboundBytes").getLong(server);
            }
        }
        private int count() throws Exception {
            synchronized (field(P2PConnection.class, "inboundLock").get(server)) {
                return field(P2PConnection.class, "inboundCount").getInt(server);
            }
        }
        private List<Long> counters() throws Exception {
            List<Long> values = new ArrayList<>();
            synchronized (field(P2PConnection.class, "inboundLock").get(server)) {
                for (String name : List.of("inboundCount", "inboundBytes", "inboundWireBytes", "inboundCallbackBytes"))
                    values.add(((Number) field(P2PConnection.class, name).get(server)).longValue());
                Map<?, ?> peers = (Map<?, ?>) field(P2PConnection.class, "inboundPeers").get(server);
                List<RemotePeer> ordered = peers.keySet().stream().map(RemotePeer.class::cast)
                        .sorted(java.util.Comparator.comparingInt(RemotePeer::getId)).toList();
                for (RemotePeer peer : ordered) {
                    Object state = peers.get(peer);
                    for (String name : List.of("count", "bytes", "wireBytes", "callbackBytes"))
                        values.add(((Number) field(state.getClass(), name).get(state)).longValue());
                }
            }
            return values;
        }
        private void assertEmpty() throws Exception {
            assertEquals(0, count());
            for (long value : counters()) assertEquals(0, value);
            assertTrue(((Map<?, ?>) field(P2PConnection.class, "inboundReservations").get(server)).isEmpty());
        }
        @Override public void close() throws Exception {
            ((AutoCloseable) delegate).close(); for (NostrKeyPair key : keys) key.close();
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Object invoke(Class<?> type, Object receiver, String name, Class<?>[] signature, Object... args) throws Exception {
        Method method = type.getDeclaredMethod(name, signature); method.setAccessible(true); return method.invoke(receiver, args);
    }
}
