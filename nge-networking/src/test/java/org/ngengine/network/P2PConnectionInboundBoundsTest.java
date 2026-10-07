package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.base.MessageProtocol;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.network.protocol.DiffableMessage;
import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.network.protocol.NetworkSafe;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.RTCSettings;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.rtc.NostrRTCChannel;
import org.ngengine.nostr4j.rtc.NostrRTCSocket;
import org.ngengine.nostr4j.rtc.NostrTURNPool;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerMessageListener;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDisconnectListener;
import org.ngengine.nostr4j.rtc.signal.NostrRTCLocalPeer;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.runner.Runner;

/** Uses the actual RTC room callback signature; no sockets are started by this fixture. */
final class P2PConnectionInboundBoundsTest {
    @NetworkSafe public static class StateMessage extends AbstractMessage implements DiffableMessage {
        public int value;
        public StateMessage() {}
        public StateMessage(int value) { this.value = value; }
        public long getDiffGroup() { return 41; }
    }
    @NetworkSafe public static class PayloadMessage extends AbstractMessage {
        public ByteBuffer payload;
        public PayloadMessage() {}
    }

    @Test void initialSceneBurstSurvivesUntilTheNextLogicUpdate() throws Exception {
        try (Fixture fixture = new Fixture()) {
            List<P2PConnection.PeerRetirementDiagnostic> retired = new ArrayList<>();
            fixture.server.setPeerRetirementDiagnosticObserver(retired::add);
            for (int index = 0; index < 128; index++) {
                PayloadMessage message = new PayloadMessage();
                message.payload = ByteBuffer.allocate(8);
                message.setReliable(true);
                fixture.receive(fixture.sender.toByteBuffer(message, null), true);
            }
            assertTrue(fixture.server.isCurrentPeer(fixture.peer));
            assertTrue(retired.isEmpty());
            assertEquals(128, field(P2PConnection.class, "inboundCount").getInt(fixture.server));
            fixture.runner.drain();
            fixture.manager.updateAppLogic(null, 0);
            assertEquals(0, field(P2PConnection.class, "inboundCount").getInt(fixture.server));
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
            assertEquals(0, fixture.peer.disconnects);
        }
    }

    @Test void droppedFirstFrameKeepsNativeCallbackChargedDuringNextDecodeAndRetirement() throws Exception {
        assertNativeCallbackLifetime(true, false);
    }

    @Test void inlineDispatchReleaseKeepsNativeCallbackChargedDuringConcurrentClose() throws Exception {
        assertNativeCallbackLifetime(false, true);
    }

    private void assertNativeCallbackLifetime(boolean dropFirst, boolean concurrentClose) throws Exception {
        try (Fixture fixture = new Fixture()) {
            PayloadMessage large = new PayloadMessage(); large.payload = ByteBuffer.allocate(60_000); large.setReliable(true);
            PayloadMessage small = new PayloadMessage(); small.payload = ByteBuffer.allocate(8); small.setReliable(true);
            ByteBuffer firstFrame = fixture.sender.toByteBuffer(large, null), nextFrame = fixture.sender.toByteBuffer(small, null);
            fixture.peer.protocol.decodeEntered = new CountDownLatch(1);
            fixture.peer.protocol.releaseDecode = new CountDownLatch(1);
            fixture.peer.protocol.nextDecodeEntered = new CountDownLatch(1);
            fixture.peer.protocol.releaseNextDecode = new CountDownLatch(1);
            fixture.peer.protocol.dropFirstDecoded = dropFirst;
            if (!dropFirst) {
                @SuppressWarnings("unchecked") MessageListener<HostedConnection> netcode = (MessageListener<HostedConnection>)
                        field(NetcodeManagerComponent.class, "messageListener").get(fixture.manager);
                fixture.server.removeMessageListener(netcode);
                fixture.runner.inline = true;
            }
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread closer = new Thread(() -> {
                try { fixture.server.close(); } catch (Throwable error) { failure.set(error); }
            }, "nge-native-callback-close-test");
            closer.setDaemon(true);
            Thread callback = new Thread(() -> {
                try { fixture.receive(firstFrame, true); } catch (Throwable error) { failure.set(error); }
            }, "nge-native-callback-lifetime-test");
            callback.setDaemon(true);
            try {
                callback.start(); assertTrue(fixture.peer.protocol.decodeEntered.await(2, TimeUnit.SECONDS));
                fixture.receiveOn("nge-2-r", nextFrame, true, fixture.nativePeer);
                fixture.peer.protocol.releaseDecode.countDown();
                assertTrue(fixture.peer.protocol.nextDecodeEntered.await(2, TimeUnit.SECONDS));
                assertTrue(callback.isAlive());
                assertEquals(firstFrame.limit(), field(P2PConnection.class, "inboundCallbackBytes").getLong(fixture.server));
                assertEquals(DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES + (long) firstFrame.limit(),
                        field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
                assertEquals(2, field(P2PConnection.class, "inboundCount").getInt(fixture.server));
                if (concurrentClose) {
                    closer.start();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                    while (!field(P2PConnection.class, "closed").getBoolean(fixture.server) && System.nanoTime() < deadline)
                        Thread.onSpinWait();
                    assertTrue(field(P2PConnection.class, "closed").getBoolean(fixture.server));
                } else fixture.server.failInboundPeer(fixture.peer, "Reliable inbound capacity exhausted");
                assertFalse(fixture.server.isCurrentPeer(fixture.peer));
                assertEquals(firstFrame.limit(), field(P2PConnection.class, "inboundCallbackBytes").getLong(fixture.server));
            } finally {
                fixture.peer.protocol.releaseDecode.countDown(); fixture.peer.protocol.releaseNextDecode.countDown();
                callback.join(3000); if (concurrentClose) closer.join(3000);
            }
            assertFalse(callback.isAlive()); assertFalse(closer.isAlive()); assertNull(failure.get());
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertEquals(0L, field(P2PConnection.class, "inboundCallbackBytes").getLong(fixture.server));
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
            assertEquals(0L, field(P2PConnection.class, "inboundWireBytes").getLong(fixture.server));
            assertEquals(0, field(P2PConnection.class, "inboundCount").getInt(fixture.server));
            fixture.server.close();
            assertEquals(0L, field(P2PConnection.class, "inboundCallbackBytes").getLong(fixture.server));
        }
    }

    @Test void distinctReliableChannelsSerializeValidFramesWithoutFalseReservationRetirement() throws Exception {
        try (Fixture fixture = new Fixture()) {
            List<P2PConnection.PeerRetirementDiagnostic> retired = new ArrayList<>();
            fixture.server.setPeerRetirementDiagnosticObserver(retired::add);
            PayloadMessage message = new PayloadMessage(); message.payload = ByteBuffer.allocate(8); message.setReliable(true);
            ByteBuffer frame = fixture.sender.toByteBuffer(message, null);
            assertTrue(frame.remaining() < 1024);
            fixture.peer.protocol.decodeEntered = new CountDownLatch(1);
            fixture.peer.protocol.releaseDecode = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread first = new Thread(() -> {
                try { fixture.receiveOn(NostrRTCSocket.DEFAULT_CHANNEL_NAME, frame, true, fixture.nativePeer); }
                catch (Throwable error) { failure.set(error); }
            }, "nge-bounded-channel-overlap-test");
            first.setDaemon(true);
            try {
                first.start();
                assertTrue(fixture.peer.protocol.decodeEntered.await(2, TimeUnit.SECONDS));
                fixture.receiveOn("nge-2-r", frame, true, fixture.nativePeer);
                assertTrue(fixture.server.isCurrentPeer(fixture.peer));
                assertTrue(retired.isEmpty(), "a second valid small frame queues within the unchanged quota");
                assertEquals(2, field(P2PConnection.class, "inboundCount").getInt(fixture.server));
                assertTrue(field(P2PConnection.class, "inboundBytes").getLong(fixture.server)
                        <= DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES + 2048);
                // The callback's original mutable input is no longer owned by either queued decode.
                for (int index = 0; index < frame.limit(); index++) frame.put(index, (byte) 0xff);
            } finally {
                fixture.peer.protocol.releaseDecode.countDown();
                first.join(3000);
            }
            assertFalse(first.isAlive()); assertNull(failure.get());
            assertEquals(2, fixture.peer.protocol.decodes);
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertTrue(fixture.server.isCurrentPeer(fixture.peer)); assertTrue(retired.isEmpty());
            assertEquals(0, fixture.peer.disconnects);
            assertEquals(0, field(P2PConnection.class, "inboundCount").getInt(fixture.server));
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
            assertEquals(0L, field(P2PConnection.class, "inboundWireBytes").getLong(fixture.server));
        }
    }

    @Test void blockedDecodeWireQueueKeepsTheOriginalCountBoundAndReportsItsLocation() throws Exception {
        try (Fixture fixture = new Fixture()) {
            List<P2PConnection.PeerRetirementDiagnostic> retired = new ArrayList<>();
            fixture.server.setPeerRetirementDiagnosticObserver(retired::add);
            PayloadMessage message = new PayloadMessage(); message.payload = ByteBuffer.allocate(8); message.setReliable(true);
            ByteBuffer frame = fixture.sender.toByteBuffer(message, null);
            fixture.peer.protocol.decodeEntered = new CountDownLatch(1);
            fixture.peer.protocol.releaseDecode = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread first = new Thread(() -> {
                try { fixture.receiveOn(NostrRTCSocket.DEFAULT_CHANNEL_NAME, frame, true, fixture.nativePeer); }
                catch (Throwable error) { failure.set(error); }
            }, "nge-bounded-wire-count-test");
            first.setDaemon(true);
            try {
                first.start(); assertTrue(fixture.peer.protocol.decodeEntered.await(2, TimeUnit.SECONDS));
                for (int index = 1; index < P2PConnection.MAX_PEER_INBOUND_COUNT; index++)
                    fixture.receiveOn("nge-2-r", frame, true, fixture.nativePeer);
                assertTrue(fixture.server.isCurrentPeer(fixture.peer));
                fixture.receiveOn("nge-2-r", frame, true, fixture.nativePeer);
                assertFalse(fixture.server.isCurrentPeer(fixture.peer)); assertEquals(1, retired.size());
                P2PConnection.PeerRetirementDiagnostic event = retired.get(0);
                assertEquals(P2PConnection.PeerRetirementReason.RELIABLE_INBOUND_CAPACITY, event.reason());
                assertEquals(P2PConnection.MAX_PEER_INBOUND_COUNT, event.peerInboundCount());
                assertEquals(P2PConnection.MAX_PEER_INBOUND_COUNT - 1, event.peerWireFifoCount());
                assertEquals(0, event.peerDecodedFifoCount());
                assertEquals(0, event.peerRetainedAfterDispatchCount());
            } finally {
                fixture.peer.protocol.releaseDecode.countDown(); first.join(3000);
            }
            assertFalse(first.isAlive()); assertNull(failure.get());
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertEquals(1, fixture.peer.disconnects);
            assertEquals(0L, field(P2PConnection.class, "inboundWireBytes").getLong(fixture.server));
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
        }
    }

    @Test void netcodeRetainedReservationsAreReportedAfterRunnerDispatchUntilLogicConsumesThem() throws Exception {
        try (Fixture fixture = new Fixture()) {
            List<P2PConnection.PeerRetirementDiagnostic> retired = new ArrayList<>();
            fixture.server.setPeerRetirementDiagnosticObserver(retired::add);
            for (int index = 0; index < P2PConnection.MAX_PEER_INBOUND_COUNT; index++) {
                PayloadMessage message = new PayloadMessage(); message.payload = ByteBuffer.allocate(8); message.setReliable(false);
                fixture.receive(fixture.sender.toByteBuffer(message, null), false);
            }
            fixture.runner.drain();
            PayloadMessage overflow = new PayloadMessage(); overflow.payload = ByteBuffer.allocate(8); overflow.setReliable(true);
            fixture.receive(fixture.sender.toByteBuffer(overflow, null), true);
            assertEquals(1, retired.size());
            P2PConnection.PeerRetirementDiagnostic event = retired.get(0);
            assertEquals(P2PConnection.MAX_PEER_INBOUND_COUNT, event.peerRetainedAfterDispatchCount());
            assertEquals(0, event.peerWireFifoCount()); assertEquals(0, event.peerDecodedFifoCount());
            assertEquals(0, event.peerExtraReferenceCount());
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
        }
    }

    @Test void retirementObserverFailureDoesNotPreventMalformedFrameTeardown() throws Exception {
        try (Fixture fixture = new Fixture()) {
            List<P2PConnection.PeerRetirementDiagnostic> retired = new ArrayList<>();
            fixture.server.setPeerRetirementDiagnosticObserver(event -> {
                retired.add(event);
                throw new IllegalStateException("observer detail must never enter retirement telemetry");
            });
            fixture.receive(ByteBuffer.allocate(0), true);
            assertEquals(1, retired.size());
            assertEquals(P2PConnection.PeerRetirementReason.INVALID_PROTOCOL_FRAME, retired.get(0).reason());
            assertFalse(retired.get(0).failureClass().isEmpty());
            assertTrue(retired.get(0).failureClass().length() <= 128);
            fixture.runner.drain();
            assertEquals(1, fixture.peer.disconnects);
        }
    }

    @Test void stalledRunnerCapsPerPeerBeforeDecodeAndUnreliableDropPreservesNextReliableBase() throws Exception {
        try (Fixture fixture = new Fixture()) {
            StateMessage initial = new StateMessage(1); initial.setReliable(true);
            fixture.receive(fixture.sender.toByteBuffer(initial, null), true); fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertEquals(List.of(1), fixture.values);
            StateMessage update = new StateMessage(2); update.setReliable(false);
            ByteBuffer repeated = fixture.sender.toByteBuffer(update, null);
            for (int index = 0; index < P2PConnection.MAX_PEER_INBOUND_COUNT * 2; index++) fixture.receive(repeated, false);
            assertEquals(1 + P2PConnection.MAX_PEER_INBOUND_COUNT, fixture.peer.protocol.decodes);
            assertTrue(fixture.runner.tasks.size() <= 1, "packets share one bounded runner drain");
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            StateMessage reliable = new StateMessage(500); reliable.setReliable(true);
            fixture.receive(fixture.sender.toByteBuffer(reliable, null), true); fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertEquals(500, fixture.values.get(fixture.values.size() - 1));
            assertTrue(fixture.server.isCurrentPeer(fixture.peer));
        }
    }

    @Test void reliableOverflowFailsClosedAndDoesNotDispatchQueuedSnapshots() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (int index = 0; index <= P2PConnection.MAX_PEER_INBOUND_COUNT; index++) {
                PayloadMessage message = new PayloadMessage(); message.payload = ByteBuffer.allocate(8); message.setReliable(true);
                fixture.receive(fixture.sender.toByteBuffer(message, null), true);
            }
            assertTrue(fixture.peer.protocol.decodes <= P2PConnection.MAX_PEER_INBOUND_COUNT);
            assertFalse(fixture.server.isCurrentPeer(fixture.peer));
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertEquals(1, fixture.peer.disconnects);
            assertTrue(fixture.values.isEmpty());
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
        }
    }

    @Test void netcodeRetentionStillCountsAgainstAdmissionUntilLogicDispatch() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (int index = 0; index < P2PConnection.MAX_PEER_INBOUND_COUNT; index++) {
                PayloadMessage message = new PayloadMessage(); message.payload = ByteBuffer.allocate(8); message.setReliable(false);
                fixture.receive(fixture.sender.toByteBuffer(message, null), false);
            }
            fixture.runner.drain();
            int before = fixture.peer.protocol.decodes;
            PayloadMessage dropped = new PayloadMessage(); dropped.payload = ByteBuffer.allocate(8); dropped.setReliable(false);
            fixture.receive(fixture.sender.toByteBuffer(dropped, null), false);
            assertEquals(before, fixture.peer.protocol.decodes, "moving to netcode does not free the transport reservation");
            fixture.manager.updateAppLogic(null, 0);
            fixture.receive(fixture.sender.toByteBuffer(dropped, null), false);
            assertEquals(before + 1, fixture.peer.protocol.decodes);
        }
    }

    @Test void byteCapRejectsBeforeDecodeAndRawSignedChunkChannelBypassesMessageProtocol() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ByteBuffer oversized = ByteBuffer.allocate(DynamicSerializerProtocol.MAX_FRAME_BYTES + 1);
            fixture.receive(oversized, false);
            assertEquals(0, fixture.peer.protocol.decodes);
            fixture.receiveOn("tdb-world-chunks-v1", ByteBuffer.allocate(100_000), true, fixture.nativePeer);
            assertEquals(0, fixture.peer.protocol.decodes);
            PayloadMessage payload = new PayloadMessage(); payload.payload = ByteBuffer.allocate(100_000); payload.setReliable(false);
            for (int count = 0; count < 80; count++) fixture.receive(fixture.sender.toByteBuffer(payload, null), false);
            assertTrue(fixture.peer.protocol.decodes < 64, "byte capacity precedes count capacity for large frames");
        }
    }

    @Test void retiredNativeSessionCannotBindByPublicKeyAndDisableDropsNetcodeLeasesBeforeRejoin() throws Exception {
        try (Fixture fixture = new Fixture()) {
            StateMessage initial = new StateMessage(1); initial.setReliable(true);
            fixture.receive(fixture.sender.toByteBuffer(initial, null), true); fixture.runner.drain();
            NostrRTCPeer stale = new NostrRTCPeer(fixture.nativePeer.getPubkey(), fixture.nativePeer.getApplicationId(),
                    fixture.nativePeer.getProtocolId(), "stale-session", fixture.nativePeer.getRoomPubkey(), null);
            int before = fixture.peer.protocol.decodes;
            fixture.receiveOn("nge-1-r", ByteBuffer.allocate(1), true, stale);
            assertEquals(before, fixture.peer.protocol.decodes);
            Method disable = NetcodeManagerComponent.class.getDeclaredMethod("onDisable", org.ngengine.components.ComponentManager.class);
            disable.setAccessible(true); disable.invoke(fixture.manager, new Object[] {null});
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertTrue(fixture.values.isEmpty());
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
            assertThrows(RuntimeException.class, () -> fixture.peer.protocol.toByteBuffer(initial, null));
            try (Fixture fresh = new Fixture()) {
                field(NetcodeManagerComponent.class, "connection").set(fixture.manager, fresh.server);
                @SuppressWarnings("unchecked") MessageListener<HostedConnection> listener = (MessageListener<HostedConnection>)
                    field(NetcodeManagerComponent.class, "messageListener").get(fixture.manager);
                fresh.server.addMessageListener(listener);
                fixture.manager.registerMessageHandler(StateMessage.class, (source, message) -> fixture.values.add(message.value));
                StateMessage joined = new StateMessage(9); joined.setReliable(true);
                fresh.receive(fresh.sender.toByteBuffer(joined, null), true); fresh.runner.drain(); fixture.manager.updateAppLogic(null, 0);
                assertEquals(List.of(9), fixture.values);
                fixture.manager.disconnectFromLobby();
            }
        }
    }

    @Test void actualNativeDisconnectDropsBothQueuesAndRetiresTheProtocol() throws Exception {
        try (Fixture fixture = new Fixture()) {
            StateMessage baseline = new StateMessage(1); baseline.setReliable(true);
            fixture.receive(fixture.sender.toByteBuffer(baseline, null), true); fixture.runner.drain();
            Constructor<NostrRTCSocket> constructor = NostrRTCSocket.class.getDeclaredConstructor(AsyncExecutor.class,
                    NostrRTCPeer.class, NostrKeyPair.class, NostrRTCLocalPeer.class, RTCSettings.class, NostrTURNPool.class);
            constructor.setAccessible(true);
            NostrRTCSocket socket = constructor.newInstance(fixture.executor, fixture.nativePeer, fixture.room,
                    (NostrRTCLocalPeer) fixture.server.getRtcRoom().getLocalPeerInfo(), RTCSettings.getDefault("inbound-bounds", "inbound-bounds:1"), null);
            @SuppressWarnings("unchecked") List<NostrRTCRoomPeerDisconnectListener> listeners =
                (List<NostrRTCRoomPeerDisconnectListener>) field(fixture.server.getRtcRoom().getClass(), "onDisconnectionListeners").get(fixture.server.getRtcRoom());
            for (NostrRTCRoomPeerDisconnectListener listener : listeners) listener.onRoomPeerDisconnected(fixture.nativePeer, socket);
            assertFalse(fixture.server.isCurrentPeer(fixture.peer));
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertTrue(fixture.values.isEmpty());
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
            assertThrows(RuntimeException.class, () -> fixture.peer.protocol.toByteBuffer(baseline, null));
        }
    }

    @Test void aggregateCountCapAppliesAcrossNativePeersBeforeAnyExtraDecode() throws Exception {
        List<NostrKeyPair> keys = new ArrayList<>();
        try (Fixture fixture = new Fixture()) {
            PayloadMessage message = new PayloadMessage(); message.payload = ByteBuffer.allocate(8); message.setReliable(false);
            ByteBuffer frame = fixture.sender.toByteBuffer(message, null);
            NostrRTCLocalPeer local = (NostrRTCLocalPeer) fixture.server.getRtcRoom().getLocalPeerInfo();
            for (int index = 0; index < 9; index++) {
                NostrKeyPair key = new NostrKeyPair(); keys.add(key);
                NostrRTCPeer nativePeer = new NostrRTCPeer(key.getPublicKey(), local.getApplicationId(), local.getProtocolId(),
                        "aggregate-session-" + index, local.getRoomPubkey(), null);
                Peer peer = new Peer(fixture.server, local, nativePeer, index + 20); fixture.connections().put(peer.getId(), peer);
                for (int packet = 0; packet < 64; packet++) fixture.receiveOn("nge-1-u", frame, false, nativePeer);
                assertEquals(index < 8 ? 64 : 0, peer.protocol.decodes);
            }
            assertEquals(1, fixture.runner.tasks.size());
            fixture.runner.drain(); fixture.manager.updateAppLogic(null, 0);
            assertEquals(0L, field(P2PConnection.class, "inboundBytes").getLong(fixture.server));
        } finally { for (NostrKeyPair key : keys) key.close(); }
    }

    @Test void inlineRunnerYieldsAtTheDrainBudgetInsteadOfRecursingIntoAnotherBatch() throws Exception {
        try (Fixture fixture = new Fixture()) {
            @SuppressWarnings("unchecked") MessageListener<HostedConnection> netcode = (MessageListener<HostedConnection>)
                    field(NetcodeManagerComponent.class, "messageListener").get(fixture.manager);
            fixture.server.removeMessageListener(netcode);
            fixture.runner.inline = true;
            int wireBudget = field(P2PConnection.class, "MAX_WIRE_DECODES_PER_TASK").getInt(null);
            fixture.peer.protocol.pauseAfterDecodes = wireBudget;
            fixture.peer.protocol.continuationEntered = new CountDownLatch(1);
            fixture.peer.protocol.releaseContinuation = new CountDownLatch(1);
            PayloadMessage message = new PayloadMessage(); message.payload = ByteBuffer.allocate(8); message.setReliable(false);
            ByteBuffer frame = fixture.sender.toByteBuffer(message, null); int[] delivered = {0};
            MessageListener<HostedConnection> producer = (peer, ignored) -> {
                delivered[0]++;
                if (delivered[0] < 1000) {
                    try { fixture.receive(frame, false); }
                    catch (Exception failure) { throw new RuntimeException(failure); }
                }
            };
            fixture.server.addMessageListener(producer, PayloadMessage.class);
            try {
                fixture.receive(frame, false);
                assertTrue(fixture.peer.protocol.continuationEntered.await(2, TimeUnit.SECONDS));
                assertEquals(wireBudget, delivered[0], "one wire drain has a finite synchronous budget");
                fixture.server.removeMessageListener(producer);
            } finally { fixture.peer.protocol.releaseContinuation.countDown(); }
        }
    }

    private static final class QueuedRunner implements Runner {
        private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private boolean inline;
        public void run(Runnable task) { if (inline) task.run(); else tasks.add(task); }
        public void enqueue(Runnable task) { tasks.add(task); }
        public void checkThread() {}
        private void drain() { Runnable task; while ((task = tasks.poll()) != null) task.run(); }
    }
    private static final class CountingProtocol extends DynamicSerializerProtocol {
        private int decodes;
        private volatile CountDownLatch decodeEntered;
        private volatile CountDownLatch releaseDecode;
        private volatile CountDownLatch nextDecodeEntered, releaseNextDecode, continuationEntered, releaseContinuation;
        private boolean dropFirstDecoded;
        private int pauseAfterDecodes = -1;
        private CountingProtocol() { super(true, ignored -> {}, -1); }
        @Override public synchronized Message toMessage(ByteBuffer frame, Boolean reliable, int resourceAllowance) {
            if (decodes == 1 && nextDecodeEntered != null) waitFor(nextDecodeEntered, releaseNextDecode);
            if (decodes == pauseAfterDecodes && continuationEntered != null) waitFor(continuationEntered, releaseContinuation);
            if (decodeEntered != null) {
                decodeEntered.countDown();
                try {
                    if (!releaseDecode.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("Controlled decode timed out");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Controlled decode interrupted");
                }
            }
            decodes++; Message result = super.toMessage(frame, reliable, resourceAllowance);
            return dropFirstDecoded && decodes == 1 ? null : result;
        }
        private void waitFor(CountDownLatch entered, CountDownLatch release) {
            entered.countDown();
            try { if (!release.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("Controlled continuation timed out"); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException("Controlled continuation interrupted"); }
        }
    }
    private static final class Peer extends RemotePeer {
        private final CountingProtocol protocol = new CountingProtocol();
        private int disconnects;
        private Peer(P2PConnection server, NostrRTCPeer local, NostrRTCPeer remote, int id) { super(id, server.getRtcRoom(), local, remote, server); }
        @Override public MessageProtocol getProtocol() { return protocol; }
        @Override public void send(Message message) {}
        @Override public void send(int channel, Message message) {}
        @Override public void close(String reason) { disconnects++; }
    }
    private static final class Fixture implements AutoCloseable {
        private final NostrKeyPair room = new NostrKeyPair();
        private final NostrKeyPair local = new NostrKeyPair();
        private final NostrKeyPair remote = new NostrKeyPair();
        private final QueuedRunner runner = new QueuedRunner();
        private final P2PConnection server;
        private final Peer peer;
        private final NostrRTCPeer nativePeer;
        private final DynamicSerializerProtocol sender = new DynamicSerializerProtocol(true, ignored -> {}, 1);
        private final NetcodeManagerComponent manager = new NetcodeManagerComponent();
        private final List<Integer> values = new ArrayList<>();
        private final AsyncExecutor executor;
        private Fixture() throws Exception {
            server = new P2PConnection(new NostrKeyPairSigner(local), "inbound-bounds", 1, room.getPrivateKey(), null, new NostrPool(), runner);
            NostrRTCLocalPeer localPeer = (NostrRTCLocalPeer) server.getRtcRoom().getLocalPeerInfo();
            nativePeer = new NostrRTCPeer(remote.getPublicKey(), localPeer.getApplicationId(), localPeer.getProtocolId(), "live-session", localPeer.getRoomPubkey(), null);
            peer = new Peer(server, localPeer, nativePeer, 7);
            connections().put(7, peer);
            field(NetcodeManagerComponent.class, "connection").set(manager, server);
            @SuppressWarnings("unchecked") MessageListener<HostedConnection> listener = (MessageListener<HostedConnection>) field(NetcodeManagerComponent.class, "messageListener").get(manager);
            server.addMessageListener(listener);
            com.jme3.network.ConnectionListener membership = (com.jme3.network.ConnectionListener)
                field(NetcodeManagerComponent.class, "connectionListener").get(manager);
            server.addConnectionListener(membership); membership.connectionAdded(server, peer);
            manager.registerMessageHandler(StateMessage.class, (source, message) -> values.add(message.value));
            executor = (AsyncExecutor) field(P2PConnection.class, "readyExecutor").get(server);
        }
        @SuppressWarnings("unchecked") private Map<Integer, RemotePeer> connections() throws Exception {
            return (Map<Integer, RemotePeer>) field(P2PConnection.class, "connections").get(server);
        }
        private void receive(ByteBuffer frame, boolean reliable) throws Exception { receiveOn(reliable ? "nge-1-r" : "nge-1-u", frame, reliable, nativePeer); }
        @SuppressWarnings("unchecked") private void receiveOn(String name, ByteBuffer frame, boolean reliable, NostrRTCPeer source) throws Exception {
            Constructor<NostrRTCSocket> socketConstructor = NostrRTCSocket.class.getDeclaredConstructor(AsyncExecutor.class,
                    NostrRTCPeer.class, NostrKeyPair.class, NostrRTCLocalPeer.class, RTCSettings.class, NostrTURNPool.class);
            socketConstructor.setAccessible(true);
            NostrRTCSocket socket = socketConstructor.newInstance(executor, source, room,
                    (NostrRTCLocalPeer) server.getRtcRoom().getLocalPeerInfo(), RTCSettings.getDefault("inbound-bounds", "inbound-bounds:1"), null);
            Constructor<NostrRTCChannel> channelConstructor = NostrRTCChannel.class.getDeclaredConstructor(String.class,
                    NostrRTCSocket.class, boolean.class, boolean.class, Number.class, Duration.class);
            channelConstructor.setAccessible(true);
            NostrRTCChannel channel = channelConstructor.newInstance(name, socket, reliable, reliable, Integer.valueOf(0), null);
            List<NostrRTCRoomPeerMessageListener> listeners = (List<NostrRTCRoomPeerMessageListener>) field(server.getRtcRoom().getClass(), "onMessageListeners").get(server.getRtcRoom());
            for (NostrRTCRoomPeerMessageListener listener : listeners) listener.onRoomPeerMessage(source, socket, channel, frame.duplicate(), false);
        }
        public void close() { manager.disconnectFromLobby(); server.close(); sender.retire(); runner.drain(); local.close(); remote.close(); room.close(); }
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name); result.setAccessible(true); return result;
    }
}
