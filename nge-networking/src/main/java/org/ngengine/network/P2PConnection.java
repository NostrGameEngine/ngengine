/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.ngengine.network;

import com.jme3.network.AbstractMessage;
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filter;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Server;
import com.jme3.network.base.MessageListenerRegistry;
import com.jme3.network.base.MessageProtocol;
import com.jme3.network.service.HostedServiceManager;
import java.time.Duration;
import java.util.Objects;
import java.util.Collection;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.LongSupplier;
import java.util.logging.Logger;
import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.network.protocol.NetworkSafe;
import org.ngengine.network.protocol.messages.ClassRegistrationAckMessage;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.RTCSettings;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.rtc.NostrRTCRoom;
import org.ngengine.nostr4j.rtc.NostrRTCSocket;
import org.ngengine.nostr4j.rtc.NostrTURNPool;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDiscoveredListener;
import org.ngengine.nostr4j.rtc.signal.NostrRTCLocalPeer;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.runner.Runner;

public class P2PConnection implements Server {

    private static final Logger log = Logger.getLogger(P2PConnection.class.getName());
    public static final Duration DEFAULT_P2P_ATTEMPT_TIMEOUT = Duration.ofSeconds(30);

    /** Millisecond-resolution bounds also keep the provider's fourfold fallback finite. */
    public static Duration validateP2pAttemptTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.compareTo(Duration.ofMillis(1)) < 0 || timeout.compareTo(DEFAULT_P2P_ATTEMPT_TIMEOUT) > 0
                || timeout.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException("P2P attempt timeout must be whole milliseconds between 1 and 30000");
        }
        return timeout;
    }

    private static final int MAX_READY_SENDS = 6;
    private static final int MAX_EXPIRED_READY_SESSIONS = 4096;
    private static final long READY_RETRY_MS = 10_000L;
    private static final long READY_WINDOW_NANOS = MAX_READY_SENDS * READY_RETRY_MS * 1_000_000L;
    private boolean isStarted = false;
    private volatile boolean closed;
    public enum HandshakeReason {
        PENDING_CREATED, READY_SEND_REQUESTED, READY_RECEIVED, ACK_RECEIVED,
        READY_SEND_COMPLETED, READY_SEND_FAILED, ACK_SEND_REQUESTED, ACK_SEND_COMPLETED, ACK_SEND_FAILED,
        READY_RETRY_LIMIT, READY_EXPIRED, READY_STALE_SESSION,
        PROMOTION_STALE_PENDING, ADMISSION_REJECTED, PROMOTED, DISCONNECTED, PARSE_FAILURE
    }
    /** Public peer identifiers only; no handshake payload or credentials. */
    public record HandshakeDiagnostic(long epoch, HandshakeReason reason, String localSession,
            String remoteSession, String peerPublicKey, String failureClass) {}
    private record HandshakeObserver(long epoch, java.util.function.Consumer<HandshakeDiagnostic> callback) {}
    private final java.util.concurrent.atomic.AtomicLong handshakeEpoch = new java.util.concurrent.atomic.AtomicLong();
    private volatile HandshakeObserver handshakeObserver;

    public enum PeerRetirementReason {
        OUTBOUND_CAPACITY, OUTBOUND_SERIALIZATION, RELIABLE_INBOUND_CAPACITY, RELIABLE_NETCODE_CAPACITY,
        RELIABLE_FRAME_OVERSIZED, INVALID_PROTOCOL_FRAME, INBOUND_DISPATCH_FAILURE, APPLICATION_BEFORE_READY,
        READY_EXPIRED, ADMISSION_REJECTED, NATIVE_DISCONNECTED, NATIVE_SESSION_REPLACED, REQUESTED,
        RELIABLE_CHANNEL_CONTROL_FAILED, RELIABLE_CHANNEL_TIMEOUT, RELIABLE_CHANNEL_SEND_FAILED,
        RELIABLE_CHANNEL_CAPACITY, RELIABLE_CHANNEL_TIMER_FAILED
    }
    /** Fixed reasons, bounded public identifiers and quota counters; never includes exception messages or payloads. */
    public record PeerRetirementDiagnostic(PeerRetirementReason reason, String localSession, String remoteSession,
            String peerPublicKey, String failureClass, int inboundCount, long inboundBytes, int peerInboundCount,
            long peerInboundBytes, int decodeReservationBytes, int peerWireFifoCount, int peerDecodedFifoCount,
            int peerExtraReferenceCount, int peerAwaitingDispatchCount, int peerRetainedAfterDispatchCount) {}
    private record RetirementObserver(java.util.function.Consumer<PeerRetirementDiagnostic> callback) {}
    private volatile RetirementObserver retirementObserver;

    /** Synchronous, best-effort callback on the failure thread; observers must only record bounded data without blocking. */
    public void setPeerRetirementDiagnosticObserver(java.util.function.Consumer<PeerRetirementDiagnostic> observer) {
        retirementObserver = observer == null ? null : new RetirementObserver(observer);
    }

    private static String diagnosticIdentifier(String value) {
        if (value == null) return "";
        if (value.length() > 128) return "invalid";
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_' || c == '.' || c == ':' || c == '$')) return "invalid";
        }
        return value;
    }

    // Called under inboundLock so the first cause and its counters cannot be replaced by a later native close.
    private PeerRetirementDiagnostic captureRetirement(RemotePeer peer, PeerRetirementReason reason, String failureClass) {
        if (closed || !peer.markRetirementReported()) return null;
        InboundPeerState state = inboundPeers.get(peer);
        NostrRTCPeer nativePeer = peer.getRemotePeer();
        int decoded = 0, extraReferences = 0, awaitingDispatch = 0, retainedAfterDispatch = 0;
        for (InboundReservation reservation : inbound) if (reservation.state.peer == peer) decoded++;
        for (InboundReservation reservation : inboundReservations.values()) {
            if (reservation.state.peer != peer || reservation.references == 0) continue;
            if (reservation.references > 1) extraReferences++;
            if (reservation.dispatchFinished) retainedAfterDispatch++; else awaitingDispatch++;
        }
        return new PeerRetirementDiagnostic(reason, diagnosticIdentifier(rtcRoom.getLocalPeerInfo().getSessionId()),
                diagnosticIdentifier(nativePeer == null ? null : nativePeer.getSessionId()),
                diagnosticIdentifier(nativePeer == null ? null : nativePeer.getPubkey().asHex()),
                diagnosticIdentifier(failureClass), inboundCount, inboundBytes, state == null ? 0 : state.count,
                state == null ? 0 : state.bytes, DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES,
                state == null ? 0 : state.wire.size(), decoded, extraReferences, awaitingDispatch, retainedAfterDispatch);
    }

    private void reportRetirement(PeerRetirementDiagnostic event) {
        if (event == null) return;
        RetirementObserver observer = retirementObserver;
        try {
            log.info("NGE_PEER_RETIRED reason=" + event.reason() + " peer=" + event.peerPublicKey()
                    + " localSession=" + event.localSession() + " remoteSession=" + event.remoteSession()
                    + " exception=" + event.failureClass() + " inboundCount=" + event.inboundCount()
                    + "/" + MAX_INBOUND_COUNT + " inboundBytes=" + event.inboundBytes() + "/" + MAX_INBOUND_BYTES
                    + " peerInboundCount=" + event.peerInboundCount() + "/" + MAX_PEER_INBOUND_COUNT
                    + " peerInboundBytes=" + event.peerInboundBytes() + "/" + MAX_PEER_INBOUND_BYTES
                    + " decodeReservationBytes=" + event.decodeReservationBytes()
                    + " peerWireFifoCount=" + event.peerWireFifoCount() + " peerDecodedFifoCount=" + event.peerDecodedFifoCount()
                    + " peerExtraReferenceCount=" + event.peerExtraReferenceCount()
                    + " peerAwaitingDispatchCount=" + event.peerAwaitingDispatchCount()
                    + " peerRetainedAfterDispatchCount=" + event.peerRetainedAfterDispatchCount());
        } catch (Throwable ignored) { /* Diagnostics cannot affect retirement. */ }
        if (observer != null && retirementObserver == observer && !closed) {
            try { observer.callback().accept(event); } catch (Throwable ignored) { /* Same contract as handshake observer. */ }
        }
    }

    private void reportRetirement(RemotePeer peer, PeerRetirementReason reason) {
        PeerRetirementDiagnostic event;
        synchronized (inboundLock) { event = captureRetirement(peer, reason, ""); }
        reportRetirement(event);
    }

    public void setHandshakeDiagnosticObserver(java.util.function.Consumer<HandshakeDiagnostic> observer) {
        long epoch = handshakeEpoch.incrementAndGet();
        handshakeObserver = observer == null ? null : new HandshakeObserver(epoch, observer);
    }

    private void observeHandshake(HandshakeReason reason, NostrRTCPeer peer, Throwable failure) {
        HandshakeObserver observer = handshakeObserver;
        if (observer == null) return;
        String errorClass = failure == null ? "" : failure.getClass().getSimpleName();
        HandshakeDiagnostic event = new HandshakeDiagnostic(observer.epoch(), reason,
                rtcRoom.getLocalPeerInfo().getSessionId(), peer == null ? "" : peer.getSessionId(),
                peer == null ? "" : peer.getPubkey().asHex(), errorClass.substring(0, Math.min(80, errorClass.length())));
        if (handshakeObserver != observer) return;
        try { observer.callback().accept(event); } catch (Throwable ignored) { /* Diagnostics cannot affect handshake. */ }
    }

    /**
     * Internal reliable round-trip used before exposing a peer to application
     * code. A socket can exist before its RTC/TURN path is bidirectionally usable.
     */
    @NetworkSafe
    public static final class PeerReadyMessage extends AbstractMessage {

        private boolean acknowledgement;

        public PeerReadyMessage() {
            super(true);
        }

        PeerReadyMessage(boolean acknowledgement) {
            super(true);
            this.acknowledgement = acknowledgement;
        }

        boolean isAcknowledgement() {
            return acknowledgement;
        }
    }

    private final String gameName;
    private final int version;
    private final HostedServiceManager services;
    private final Map<Integer, RemotePeer> connections = new ConcurrentHashMap<>();
    private final Map<String, RemotePeer> pendingConnections = new ConcurrentHashMap<>();
    private final Map<String, PeerReadyAttempt> readyAttempts = new ConcurrentHashMap<>();
    // Boolean ACKs cannot distinguish an expired attempt from a new attempt in the same native session.
    private final java.util.Set<String> expiredReadySessions = ConcurrentHashMap.newKeySet();
    private final AtomicLong readyEpoch = new AtomicLong();
    private final AsyncExecutor readyExecutor;
    private final LongSupplier readyClock = System::nanoTime;
    private final MessageListenerRegistry<HostedConnection> messageListeners = new MessageListenerRegistry<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<NostrRTCRoomPeerDiscoveredListener> peerDiscoveredListeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextConnectionId = new AtomicInteger();
    private volatile Predicate<NostrPublicKey> peerAdmission = ignored -> true;

    private final NostrSigner localSigner;
    private final NostrPool masterServersPool;
    private final NostrRTCRoom rtcRoom;
    private final NostrTURNPool turnPool;

    private Runner dispatcher;

    public static final int MAX_INBOUND_COUNT = 512;
    // Initial scene snapshots and schema ACKs can exceed 64 frames between logic updates.
    public static final int MAX_PEER_INBOUND_COUNT = 256;
    private static final int MAX_WIRE_DECODES_PER_TASK = 64;
    public static final long MAX_INBOUND_BYTES = 32L * 1024 * 1024;
    public static final long MAX_PEER_INBOUND_BYTES = 4L * 1024 * 1024;
    public static final int MAX_NATIVE_PEERS = 128;
    private final Object inboundLock = new Object();
    private final Map<RemotePeer, InboundPeerState> inboundPeers = new IdentityHashMap<>();
    private final Map<Message, InboundReservation> inboundReservations = new IdentityHashMap<>();
    private final ArrayDeque<InboundReservation> inbound = new ArrayDeque<>();
    private int inboundCount;
    private long inboundBytes;
    private long inboundWireBytes;
    private long inboundCallbackBytes;
    private boolean inboundDrainScheduled;

    private static final class InboundPeerState {
        private final RemotePeer peer;
        private int count;
        private long bytes;
        private long wireBytes;
        private long callbackBytes;
        private final ArrayDeque<InboundReservation> wire = new ArrayDeque<>();
        private boolean decoding;
        private volatile boolean retired;
        private String failure;
        private boolean failureHandled;
        private boolean cleanupPending;
        private boolean notifyRemoved;

        private InboundPeerState(RemotePeer peer) { this.peer = peer; }
    }

    /** A reservation follows a decoded message through the runner and the netcode queue. */
    public final class InboundReservation implements AutoCloseable {
        private final InboundPeerState state;
        private int bytes;
        private int references = 1;
        private Message message;
        private boolean dispatchFinished;
        private java.nio.ByteBuffer frame;
        private boolean reliable;
        private boolean startsDecoder;
        private int nativeInputBytes;
        private boolean nativeCallbackOpen = true;
        private boolean counted = true;

        private InboundReservation(InboundPeerState state, int bytes) {
            this.state = state;
            this.bytes = bytes;
        }

        public int getResourceBytes() { synchronized (inboundLock) { return bytes; } }

        // The provider retains its raw input until this callback returns, even after a dropped decode or inline dispatch.
        private void releaseNativeInput() {
            synchronized (inboundLock) {
                if (!nativeCallbackOpen) return;
                nativeCallbackOpen = false;
                inboundBytes -= nativeInputBytes;
                state.bytes -= nativeInputBytes;
                inboundCallbackBytes -= nativeInputBytes;
                state.callbackBytes -= nativeInputBytes;
                nativeInputBytes = 0;
                releaseCountIfFinished();
            }
            if (!Thread.holdsLock(inboundLock)) scheduleWireDecodes();
        }

        private void releaseCountIfFinished() {
            if (counted && references == 0 && !nativeCallbackOpen) {
                counted = false;
                inboundCount--;
                state.count--;
            }
            if (state.retired && state.count == 0 && state.failureHandled && !state.cleanupPending)
                inboundPeers.remove(state.peer);
        }

        public void close() {
            synchronized (inboundLock) {
                if (references == 0 || --references != 0) return;
                inboundBytes -= bytes;
                state.bytes -= bytes;
                if (message != null) inboundReservations.remove(message);
                message = null;
                releaseCountIfFinished();
            }
            if (!Thread.holdsLock(inboundLock)) scheduleWireDecodes();
        }
    }

    public boolean isCurrentPeer(RemotePeer peer) {
        if (closed || peer == null || connections.get(peer.getId()) != peer
                || expiredReadySessions.contains(peerSessionKeyOf(peer.getRemotePeer()))) return false;
        synchronized (inboundLock) {
            InboundPeerState state = inboundPeers.get(peer);
            return state == null || !state.retired;
        }
    }

    public InboundReservation retainInboundMessage(RemotePeer peer, Message message) {
        synchronized (inboundLock) {
            InboundReservation reservation = inboundReservations.get(message);
            if (!isCurrentPeer(peer) || reservation == null || reservation.state.peer != peer
                    || reservation.references == 0 || reservation.references == Integer.MAX_VALUE) return null;
            reservation.references++;
            return reservation;
        }
    }

    /**
     * Acquires an independent owner of a currently admitted message, or returns null if unavailable.
     * Owners share one resource charge; retaining the same graph does not charge it again.
     */
    public InboundMessageLease tryRetainInboundMessage(RemotePeer peer, Message message) {
        synchronized (inboundLock) {
            InboundReservation reservation = inboundReservations.get(message);
            if (!isCurrentPeer(peer) || reservation == null || reservation.state.peer != peer
                    || reservation.references == 0 || reservation.references == Integer.MAX_VALUE) return null;
            InboundMessageLease lease = new InboundMessageLease(this, reservation);
            reservation.references++;
            return lease;
        }
    }

    /** An independent, close-once handle for shared inbound ownership and logical resource accounting. */
    public static final class InboundMessageLease implements AutoCloseable {
        private final Object inboundLock;
        private P2PConnection connection;
        private InboundReservation reservation;

        private InboundMessageLease(P2PConnection connection, InboundReservation reservation) {
            this.inboundLock = connection.inboundLock;
            this.connection = connection;
            this.reservation = reservation;
        }

        /** Returns a new owner, or null after retirement or reference exhaustion. A closed handle throws. */
        public InboundMessageLease retain() {
            synchronized (inboundLock) {
                if (reservation == null) throw new IllegalStateException("Inbound message lease is closed");
                return connection.tryRetainInboundMessage(reservation.state.peer, reservation.message);
            }
        }

        /**
         * Raises the shared total charge before allocating additional resources, without shrinking it.
         * False leaves all counters unchanged: the caller must abort or defer allocation. Retirement
         * refuses even nongrowing requests. Reliable rejection must follow the caller's fail-closed
         * policy. This is a logical budget, not a measured JVM heap size.
         */
        public boolean ensureResourceBytes(int minimumTotalBytes) {
            if (minimumTotalBytes <= 0) throw new IllegalArgumentException("Resource bytes must be positive");
            synchronized (inboundLock) {
                if (reservation == null) throw new IllegalStateException("Inbound message lease is closed");
                if (!connection.isCurrentPeer(reservation.state.peer)) return false;
                int extra = minimumTotalBytes - reservation.bytes;
                if (extra <= 0) return true;
                if (extra > MAX_INBOUND_BYTES - connection.inboundBytes
                        || extra > MAX_PEER_INBOUND_BYTES - reservation.state.bytes) return false;
                reservation.bytes += extra;
                reservation.state.bytes += extra;
                connection.inboundBytes += extra;
                return true;
            }
        }

        /** The shared charge remains readable after retirement, until this handle closes. */
        public int getResourceBytes() {
            synchronized (inboundLock) {
                if (reservation == null) throw new IllegalStateException("Inbound message lease is closed");
                return reservation.bytes;
            }
        }

        @Override public void close() {
            boolean released;
            P2PConnection owner;
            synchronized (inboundLock) {
                if (reservation == null) return;
                InboundReservation held = reservation;
                owner = connection;
                reservation = null;
                connection = null;
                held.close();
                released = held.references == 0;
            }
            if (released && !Thread.holdsLock(inboundLock)) owner.scheduleWireDecodes();
        }
    }

    private InboundReservation reserveInbound(RemotePeer peer, java.nio.ByteBuffer frame, boolean reliable) {
        synchronized (inboundLock) {
            if (closed || connections.get(peer.getId()) != peer
                    && pendingConnections.get(peerSessionKeyOf(peer.getRemotePeer())) != peer) return null;
            InboundPeerState state = inboundPeers.get(peer);
            if (state == null) {
                if (inboundPeers.size() >= MAX_NATIVE_PEERS) return null;
                state = new InboundPeerState(peer);
                inboundPeers.put(peer, state);
            }
            // Account for the native input and our immutable copy before allocation or waiting for the protocol monitor.
            int bytes = frame.limit() * 2;
            int nativeBytes = frame.limit();
            int admissionBytes = bytes + nativeBytes;
            if (state.retired || inboundCount >= MAX_INBOUND_COUNT || state.count >= MAX_PEER_INBOUND_COUNT
                    || admissionBytes > MAX_INBOUND_BYTES - inboundBytes || admissionBytes > MAX_PEER_INBOUND_BYTES - state.bytes
                    // Leave room to promote at least one queued frame to the existing worst-case decode reservation.
                    || admissionBytes > MAX_INBOUND_BYTES - DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES - inboundWireBytes - inboundCallbackBytes
                    || admissionBytes > MAX_PEER_INBOUND_BYTES - DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES - state.wireBytes - state.callbackBytes) return null;
            inboundCount++;
            inboundBytes += admissionBytes;
            state.count++;
            state.bytes += admissionBytes;
            state.callbackBytes += nativeBytes;
            inboundCallbackBytes += nativeBytes;
            InboundReservation reservation = new InboundReservation(state, bytes);
            reservation.nativeInputBytes = nativeBytes;
            try {
                java.nio.ByteBuffer source = frame.asReadOnlyBuffer(); source.position(0);
                java.nio.ByteBuffer copy = java.nio.ByteBuffer.allocate(source.remaining()); copy.put(source).flip();
                reservation.frame = copy.asReadOnlyBuffer();
            } catch (RuntimeException | Error error) {
                reservation.close();
                reservation.releaseNativeInput();
                throw error;
            }
            reservation.reliable = reliable;
            state.wire.add(reservation);
            state.wireBytes += bytes;
            inboundWireBytes += bytes;
            if (!state.decoding) { state.decoding = true; reservation.startsDecoder = true; }
            return reservation;
        }
    }

    private boolean canDecodeWire(InboundPeerState state) {
        InboundReservation head = state.wire.peek();
        if (closed || state.retired || head == null || connections.get(state.peer.getId()) != state.peer
                && pendingConnections.get(peerSessionKeyOf(state.peer.getRemotePeer())) != state.peer) return false;
        int extra = DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES - head.bytes;
        return extra <= MAX_INBOUND_BYTES - inboundBytes && extra <= MAX_PEER_INBOUND_BYTES - state.bytes;
    }

    private void scheduleWireDecodes() {
        List<InboundPeerState> ready = new java.util.ArrayList<>();
        synchronized (inboundLock) {
            if (closed) return;
            for (InboundPeerState state : inboundPeers.values()) {
                if (!state.decoding && canDecodeWire(state)) { state.decoding = true; ready.add(state); }
            }
        }
        // One task per peer, not per packet, on the existing asynchronous executor; never decode inside a lease release.
        for (InboundPeerState state : ready) {
            try {
                readyExecutor.run(() -> { drainWire(state); return null; }).catchException(error ->
                    failInboundPeer(state.peer, "Inbound dispatch failure", PeerRetirementReason.INBOUND_DISPATCH_FAILURE,
                            error.getClass().getSimpleName()));
            } catch (RuntimeException error) {
                failInboundPeer(state.peer, "Inbound dispatch failure", PeerRetirementReason.INBOUND_DISPATCH_FAILURE,
                        error.getClass().getSimpleName());
            }
        }
    }

    private void drainWire(InboundPeerState state) {
        try {
            for (int processed = 0; processed < MAX_WIRE_DECODES_PER_TASK; processed++) {
                MessageProtocol protocol = state.peer.getProtocol();
                synchronized (protocol) {
                    InboundReservation reservation;
                    synchronized (inboundLock) {
                        if (!canDecodeWire(state)) return;
                        reservation = state.wire.remove();
                        state.wireBytes -= reservation.bytes;
                        inboundWireBytes -= reservation.bytes;
                        int extra = DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES - reservation.bytes;
                        reservation.bytes += extra; state.bytes += extra; inboundBytes += extra;
                    }
                    boolean enqueued = false;
                    try {
                        Message message = protocol instanceof DynamicSerializerProtocol
                            ? ((DynamicSerializerProtocol) protocol).toMessage(reservation.frame, Boolean.valueOf(reservation.reliable), reservation.bytes)
                            : protocol.toMessage(reservation.frame);
                        if (message == null) continue;
                        message.setReliable(reservation.reliable);
                        int bytes = protocol instanceof DynamicSerializerProtocol
                            ? ((DynamicSerializerProtocol) protocol).getLastDecodeResourceBytes()
                            : DynamicSerializerProtocol.MAX_DECODE_RESOURCE_BYTES;
                        enqueueInbound(reservation, message, bytes);
                        enqueued = true;
                    } catch (RuntimeException error) {
                        observeHandshake(HandshakeReason.PARSE_FAILURE, state.peer.getRemotePeer(), error);
                        logInboundFailure("Invalid NGE protocol frame", error);
                        failInboundPeer(state.peer, "Invalid protocol frame", PeerRetirementReason.INVALID_PROTOCOL_FRAME,
                                error.getClass().getSimpleName());
                    } finally {
                        reservation.frame = null;
                        if (!enqueued) reservation.close();
                    }
                }
            }
        } finally {
            synchronized (inboundLock) { state.decoding = false; }
            scheduleWireDecodes();
        }
    }

    private void enqueueInbound(InboundReservation reservation, Message message, int bytes) {
        boolean schedule;
        synchronized (inboundLock) {
            if (closed || reservation.state.retired) { reservation.close(); return; }
            if (bytes > reservation.bytes) throw new IllegalStateException("Decode exceeded admission reservation");
            int difference = reservation.bytes - bytes;
            reservation.bytes = bytes;
            inboundBytes -= difference;
            reservation.state.bytes -= difference;
            reservation.message = message;
            inboundReservations.put(message, reservation);
            inbound.add(reservation);
            schedule = !inboundDrainScheduled;
            inboundDrainScheduled = true;
        }
        scheduleWireDecodes();
        if (schedule) dispatcher.run(this::drainInbound);
    }

    /** Marks failure immediately; native disconnect and application callbacks run on the normal runner. */
    public void failInboundPeer(RemotePeer peer, String reason) {
        PeerRetirementReason category = switch (reason == null ? "" : reason) {
            case "Reliable inbound capacity exhausted" -> PeerRetirementReason.RELIABLE_INBOUND_CAPACITY;
            case "Reliable netcode capacity exhausted" -> PeerRetirementReason.RELIABLE_NETCODE_CAPACITY;
            case "Oversized reliable frame" -> PeerRetirementReason.RELIABLE_FRAME_OVERSIZED;
            case "Invalid protocol frame" -> PeerRetirementReason.INVALID_PROTOCOL_FRAME;
            case "Inbound dispatch failure" -> PeerRetirementReason.INBOUND_DISPATCH_FAILURE;
            case "Reliable application frame before peer-ready" -> PeerRetirementReason.APPLICATION_BEFORE_READY;
            default -> PeerRetirementReason.REQUESTED;
        };
        failInboundPeer(peer, reason, category, "");
    }

    AsyncExecutor reliableChannelExecutor() { return readyExecutor; }

    void failReliableChannel(RemotePeer peer, ReliableChannelGate.Failure failure) {
        peer.retireReliableChannels();
        RemotePeer connected;
        RemotePeer pending;
        synchronized (pendingConnections) {
            String sessionKey = peerSessionKeyOf(peer.getRemotePeer());
            if (closed || sessionKey == null) return;
            // This barrier shares the admission/removal monitor: object removal cannot erase epoch failure.
            // At capacity all new admission is already blocked; never evict an earlier failed epoch.
            if (expiredReadySessions.size() < MAX_EXPIRED_READY_SESSIONS) expiredReadySessions.add(sessionKey);
            connected = findConnectionByPeer(peer.getRemotePeer());
            pending = pendingConnections.get(sessionKey);
        }
        PeerRetirementReason reason = switch (failure) {
            case CONTROL_SEND_FAILED -> PeerRetirementReason.RELIABLE_CHANNEL_CONTROL_FAILED;
            case ACK_TIMEOUT, SEND_TIMEOUT -> PeerRetirementReason.RELIABLE_CHANNEL_TIMEOUT;
            case SEND_FAILED -> PeerRetirementReason.RELIABLE_CHANNEL_SEND_FAILED;
            case CAPACITY -> PeerRetirementReason.RELIABLE_CHANNEL_CAPACITY;
            case TIMER_FAILED -> PeerRetirementReason.RELIABLE_CHANNEL_TIMER_FAILED;
        };
        // Gates and runner/native callbacks stay outside the lifecycle monitor. Match epochs, never just pubkeys.
        if (connected != null) failInboundPeer(connected, "Reliable channel failure: " + failure.name(), reason, "");
        if (pending != null && pending != connected)
            failInboundPeer(pending, "Reliable channel failure: " + failure.name(), reason, "");
    }

    private void failInboundPeer(RemotePeer peer, String reason, PeerRetirementReason category, String failureClass) {
        peer.retireReliableChannels();
        boolean schedule;
        PeerRetirementDiagnostic event;
        synchronized (inboundLock) {
            InboundPeerState state = inboundPeers.get(peer);
            if (state == null) {
                if (closed || inboundPeers.size() >= MAX_NATIVE_PEERS) return;
                state = new InboundPeerState(peer);
                inboundPeers.put(peer, state);
            }
            if (state.retired) return;
            event = captureRetirement(peer, category, failureClass);
            state.retired = true;
            state.failure = reason;
            schedule = !inboundDrainScheduled;
            inboundDrainScheduled = true;
        }
        reportRetirement(event);
        if (schedule) dispatcher.run(this::drainInbound);
    }

    private void queuePeerRetirement(RemotePeer peer, boolean notifyRemoved, PeerRetirementReason reason) {
        peer.retireReliableChannels();
        boolean schedule;
        PeerRetirementDiagnostic event;
        synchronized (inboundLock) {
            InboundPeerState state = inboundPeers.get(peer);
            if (state == null) {
                if (closed || inboundPeers.size() >= MAX_NATIVE_PEERS) return;
                state = new InboundPeerState(peer);
                inboundPeers.put(peer, state);
            }
            event = captureRetirement(peer, reason, "");
            state.retired = true;
            state.failureHandled = true;
            state.cleanupPending = true;
            state.notifyRemoved |= notifyRemoved;
            schedule = !inboundDrainScheduled;
            inboundDrainScheduled = true;
        }
        reportRetirement(event);
        if (schedule) dispatcher.run(this::drainInbound);
    }

    private void retireInboundPeer(RemotePeer peer) {
        peer.retireReliableChannels();
        synchronized (inboundLock) {
            InboundPeerState state = inboundPeers.get(peer);
            if (state != null) {
                state.retired = true;
                state.failureHandled = true;
                state.cleanupPending = false;
                while (!state.wire.isEmpty()) {
                    InboundReservation reservation = state.wire.remove();
                    state.wireBytes -= reservation.bytes;
                    inboundWireBytes -= reservation.bytes;
                    reservation.frame = null;
                    reservation.close();
                }
            }
            Iterator<InboundReservation> queued = inbound.iterator();
            while (queued.hasNext()) {
                InboundReservation reservation = queued.next();
                if (reservation.state.peer == peer) { queued.remove(); reservation.close(); }
            }
            if (state != null && state.count == 0) inboundPeers.remove(peer);
        }
        if (peer.getProtocol() instanceof DynamicSerializerProtocol) {
            ((DynamicSerializerProtocol) peer.getProtocol()).retire();
        }
        scheduleWireDecodes();
    }

    private void drainInbound() {
        // One runner task drains the bounded inbox; no packet creates another task queue.
        for (int processed = 0; processed < MAX_INBOUND_COUNT; processed++) {
            InboundPeerState failed = null;
            InboundPeerState cleanup = null;
            InboundReservation reservation = null;
            synchronized (inboundLock) {
                for (InboundPeerState state : inboundPeers.values()) {
                    if (state.cleanupPending) { cleanup = state; state.cleanupPending = false; break; }
                    if (state.failure != null && !state.failureHandled) {
                        state.failureHandled = true;
                        failed = state;
                        break;
                    }
                }
                if (failed == null && cleanup == null) reservation = inbound.poll();
                if (failed == null && cleanup == null && reservation == null) { inboundDrainScheduled = false; return; }
            }
            if (cleanup != null) {
                retireInboundPeer(cleanup.peer);
                if (cleanup.notifyRemoved) {
                    for (ConnectionListener listener : connectionListeners) {
                        try { listener.connectionRemoved(this, cleanup.peer); }
                        catch (RuntimeException error) { log.log(java.util.logging.Level.FINE, "Peer removal listener failed", error); }
                    }
                }
                continue;
            }
            if (failed != null) {
                RemotePeer peer = failed.peer;
                boolean wasConnected;
                synchronized (pendingConnections) {
                    pendingConnections.remove(peerSessionKeyOf(peer.getRemotePeer()), peer);
                    wasConnected = connections.remove(peer.getId(), peer);
                }
                cancelPeerReady(peer);
                retireInboundPeer(peer);
                try {
                    if (wasConnected) for (ConnectionListener listener : connectionListeners) listener.connectionRemoved(this, peer);
                } catch (RuntimeException error) {
                    log.log(java.util.logging.Level.FINE, "Peer removal listener failed", error);
                } finally {
                    try { peer.close(failed.failure); }
                    catch (RuntimeException error) { log.log(java.util.logging.Level.FINE, "Native peer disconnect failed", error); }
                }
                continue;
            }
            try {
                RemotePeer peer = reservation.state.peer;
                Message message = reservation.message;
                if (closed || reservation.state.retired || message == null
                        || expiredReadySessions.contains(peerSessionKeyOf(peer.getRemotePeer()))) continue;
                if (message instanceof ClassRegistrationAckMessage) {
                    if (!message.isReliable()) throw new IllegalArgumentException("Unreliable class registration ACK");
                    ((DynamicSerializerProtocol) peer.getProtocol()).markClassRegistered(((ClassRegistrationAckMessage) message).getClassId());
                } else if (message instanceof OpenChannelMessage) {
                    int channel = ((OpenChannelMessage) message).getChannel();
                    if (!message.isReliable() || channel < 1 || channel > 64) throw new IllegalArgumentException("Invalid bounded channel");
                    peer.handleOpenChannel((OpenChannelMessage) message);
                } else if (message instanceof PeerReadyMessage) {
                    if (!message.isReliable()) throw new IllegalArgumentException("Unreliable peer-ready control");
                    handlePeerReady(peer, peer.getRemotePeer(), (PeerReadyMessage) message);
                } else if (isCurrentPeer(peer)) {
                    messageListeners.messageReceived(peer, message);
                } else if (message.isReliable()) {
                    failInboundPeer(peer, "Reliable application frame before peer-ready");
                }
            } catch (RuntimeException error) {
                logInboundFailure("Invalid NGE inbound dispatch", error);
                failInboundPeer(reservation.state.peer, "Inbound dispatch failure",
                        PeerRetirementReason.INBOUND_DISPATCH_FAILURE, error.getClass().getSimpleName());
            } finally {
                synchronized (inboundLock) {
                    reservation.dispatchFinished = true;
                    reservation.close();
                }
                scheduleWireDecodes();
            }
        }
        dispatcher.enqueue(this::drainInbound);
    }


    private static final class PeerReadyAttempt {
        private final RemotePeer connection;
        private final String sessionKey;
        private final String localSession;
        private final long epoch;
        private final long startedAt;
        private int sends;
        private AsyncTask<?> retry;

        private PeerReadyAttempt(RemotePeer connection, String sessionKey, String localSession, long epoch, long startedAt) {
            this.connection = connection;
            this.sessionKey = sessionKey;
            this.localSession = localSession;
            this.epoch = epoch;
            this.startedAt = startedAt;
        }
    }

    private boolean currentReadyPeer(PeerReadyAttempt attempt) {
        return !closed && readyEpoch.get() == attempt.epoch
                && !expiredReadySessions.contains(attempt.sessionKey)
                && readyClock.getAsLong() - attempt.startedAt < READY_WINDOW_NANOS
                && rtcRoom.getLocalPeerInfo().getSessionId().equals(attempt.localSession)
                && (pendingConnections.get(attempt.sessionKey) == attempt.connection
                    || connections.get(attempt.connection.getId()) == attempt.connection);
    }

    private void beginPeerReady(RemotePeer connection) {
        String sessionKey = peerSessionKeyOf(connection.getRemotePeer());
        PeerReadyAttempt attempt;
        synchronized (pendingConnections) {
            if (closed || sessionKey == null || expiredReadySessions.contains(sessionKey)
                    || expiredReadySessions.size() >= MAX_EXPIRED_READY_SESSIONS
                    || pendingConnections.get(sessionKey) != connection) return;
            attempt = new PeerReadyAttempt(connection, sessionKey,
                    rtcRoom.getLocalPeerInfo().getSessionId(), readyEpoch.get(), readyClock.getAsLong());
            if (readyAttempts.putIfAbsent(sessionKey, attempt) != null) return;
        }
        sendPeerReady(attempt);
    }

    private void expirePeerReady(PeerReadyAttempt attempt) {
        boolean expired;
        synchronized (pendingConnections) {
            expired = !closed && readyEpoch.get() == attempt.epoch && readyAttempts.get(attempt.sessionKey) == attempt
                    && readyClock.getAsLong() - attempt.startedAt >= READY_WINDOW_NANOS
                    && pendingConnections.remove(attempt.sessionKey, attempt.connection);
            if (expired && expiredReadySessions.size() < MAX_EXPIRED_READY_SESSIONS)
                expiredReadySessions.add(attempt.sessionKey);
        }
        if (expired) {
            reportRetirement(attempt.connection, PeerRetirementReason.READY_EXPIRED);
            cancelPeerReady(attempt.connection);
            retireInboundPeer(attempt.connection);
            observeHandshake(HandshakeReason.READY_EXPIRED, attempt.connection.getRemotePeer(), null);
        }
    }

    private void sendPeerReady(PeerReadyAttempt attempt) {
        synchronized (attempt) {
            expirePeerReady(attempt);
            if (!currentReadyPeer(attempt) || readyAttempts.get(attempt.sessionKey) != attempt
                    || pendingConnections.get(attempt.sessionKey) != attempt.connection) return;
            if (attempt.sends < MAX_READY_SENDS) {
                attempt.sends++;
                sendPeerReadyControl(attempt, false);
                if (attempt.sends == MAX_READY_SENDS && currentReadyPeer(attempt)
                        && readyAttempts.get(attempt.sessionKey) == attempt)
                    observeHandshake(HandshakeReason.READY_RETRY_LIMIT, attempt.connection.getRemotePeer(), null);
            }
            if (!currentReadyPeer(attempt) || pendingConnections.get(attempt.sessionKey) != attempt.connection) return;
            long remainingNanos = READY_WINDOW_NANOS - (readyClock.getAsLong() - attempt.startedAt);
            if (remainingNanos > 0) {
                attempt.retry = readyExecutor.runLater(() -> {
                    dispatcher.run(() -> sendPeerReady(attempt));
                    return null;
                }, Math.min(READY_RETRY_MS, (remainingNanos + 999_999L) / 1_000_000L), TimeUnit.MILLISECONDS);
            }
        }
    }

    private void sendPeerReadyControl(PeerReadyAttempt attempt, boolean acknowledgement) {
        if (!currentReadyPeer(attempt) || !acknowledgement && readyAttempts.get(attempt.sessionKey) != attempt) return;
        observeHandshake(acknowledgement ? HandshakeReason.ACK_SEND_REQUESTED : HandshakeReason.READY_SEND_REQUESTED,
                attempt.connection.getRemotePeer(), null);
        attempt.connection.sendPeerReady(new PeerReadyMessage(acknowledgement))
                .then(ignored -> {
                    dispatcher.run(() -> {
                        if (currentReadyPeer(attempt) && (acknowledgement || readyAttempts.get(attempt.sessionKey) == attempt)) observeHandshake(
                                acknowledgement ? HandshakeReason.ACK_SEND_COMPLETED : HandshakeReason.READY_SEND_COMPLETED,
                                attempt.connection.getRemotePeer(), null);
                    });
                    return null;
                })
                .catchException(error -> dispatcher.run(() -> {
                    if (currentReadyPeer(attempt) && (acknowledgement || readyAttempts.get(attempt.sessionKey) == attempt)) observeHandshake(
                            acknowledgement ? HandshakeReason.ACK_SEND_FAILED : HandshakeReason.READY_SEND_FAILED,
                            attempt.connection.getRemotePeer(), error);
                }));
    }

    private void cancelPeerReady(RemotePeer connection) {
        String sessionKey = peerSessionKeyOf(connection.getRemotePeer());
        PeerReadyAttempt attempt = readyAttempts.get(sessionKey);
        if (attempt != null && attempt.connection == connection && readyAttempts.remove(sessionKey, attempt)) {
            synchronized (attempt) {
                if (attempt.retry != null) attempt.retry.cancel();
            }
        }
    }

    private static String peerKeyOf(NostrRTCPeer peer) {
        if (peer == null) {
            return null;
        }
        if (peer.getPubkey() != null) {
            return peer.getPubkey().asHex();
        }
        return peer.toString();
    }

    private RemotePeer findConnectionByPeer(NostrRTCPeer peer) {
        return findConnectionByPeer(connections.values(), peer);
    }

    private RemotePeer findPendingConnectionByPeer(NostrRTCPeer peer) {
        return findConnectionByPeer(pendingConnections.values(), peer);
    }

    private static RemotePeer findConnectionByPeer(Collection<RemotePeer> candidates, NostrRTCPeer peer) {
        String key = peerSessionKeyOf(peer);
        if (key == null) {
            return null;
        }
        for (RemotePeer connection : candidates) {
            if (connection == null) {
                continue;
            }
            String connectionKey = peerSessionKeyOf(connection.getRemotePeer());
            if (key.equals(connectionKey)) {
                return connection;
            }
        }
        return null;
    }

    private static String peerSessionKeyOf(NostrRTCPeer peer) {
        if (peer == null) {
            return null;
        }
        String pub = peer.getPubkey() != null ? peer.getPubkey().asHex() : "null";
        String session = peer.getSessionId() != null ? peer.getSessionId() : "null";
        return pub + "|" + session;
    }

    private RemotePeer findConnectionByPubkey(NostrRTCPeer peer) {
        return findConnectionByPubkey(connections.values(), peer);
    }

    private RemotePeer findPendingConnectionByPubkey(NostrRTCPeer peer) {
        return findConnectionByPubkey(pendingConnections.values(), peer);
    }

    private static RemotePeer findConnectionByPubkey(Collection<RemotePeer> candidates, NostrRTCPeer peer) {
        String key = peerKeyOf(peer);
        if (key == null) {
            return null;
        }
        for (RemotePeer connection : candidates) {
            if (connection == null) {
                continue;
            }
            String connectionKey = peerKeyOf(connection.getRemotePeer());
            if (key.equals(connectionKey)) {
                return connection;
            }
        }
        return null;
    }

    private void promotePendingConnection(RemotePeer connection) {
        String sessionKey = peerSessionKeyOf(connection.getRemotePeer());
        PeerReadyAttempt attempt = sessionKey == null ? null : readyAttempts.get(sessionKey);
        if (attempt != null) expirePeerReady(attempt);
        if (attempt == null || attempt.connection != connection || !currentReadyPeer(attempt)
                || pendingConnections.get(sessionKey) != connection) {
            observeHandshake(HandshakeReason.PROMOTION_STALE_PENDING, connection.getRemotePeer(), null);
            return;
        }
        NostrPublicKey peer = connection.getRemotePeer() != null
            ? connection.getRemotePeer().getPubkey()
            : null;
        boolean admitted = peer != null && peerAdmission.test(peer);
        HandshakeReason reason;
        synchronized (pendingConnections) {
            // Admission may call application code while this session is replaced or disconnected.
            if (closed || expiredReadySessions.contains(sessionKey) || readyEpoch.get() != attempt.epoch
                    || !rtcRoom.getLocalPeerInfo().getSessionId().equals(attempt.localSession)
                    || readyAttempts.get(sessionKey) != attempt || pendingConnections.get(sessionKey) != connection) {
                reason = HandshakeReason.PROMOTION_STALE_PENDING;
            } else {
                pendingConnections.remove(sessionKey, connection);
                if (readyClock.getAsLong() - attempt.startedAt >= READY_WINDOW_NANOS) {
                    if (expiredReadySessions.size() < MAX_EXPIRED_READY_SESSIONS) expiredReadySessions.add(sessionKey);
                    reason = HandshakeReason.READY_EXPIRED;
                } else if (!admitted) {
                    reason = HandshakeReason.ADMISSION_REJECTED;
                } else {
                    connections.put(connection.getId(), connection);
                    reason = HandshakeReason.PROMOTED;
                }
            }
        }
        cancelPeerReady(connection);
        observeHandshake(reason, connection.getRemotePeer(), null);
        if (reason == HandshakeReason.ADMISSION_REJECTED || reason == HandshakeReason.READY_EXPIRED) {
            reportRetirement(connection, reason == HandshakeReason.READY_EXPIRED
                    ? PeerRetirementReason.READY_EXPIRED : PeerRetirementReason.ADMISSION_REJECTED);
            retireInboundPeer(connection);
            rtcRoom.disconnect(connection.getRemotePeer());
            return;
        }
        if (reason != HandshakeReason.PROMOTED) return;
        this.dispatcher.run(() -> {
            if (closed || connections.get(connection.getId()) != connection) return;
            for (ConnectionListener listener : connectionListeners) {
                listener.connectionAdded(this, connection);
            }
        });
    }

    private void handlePeerReady(RemotePeer connection, NostrRTCPeer source, PeerReadyMessage ready) {
        String sessionKey = peerSessionKeyOf(connection.getRemotePeer());
        if (closed) return;
        PeerReadyAttempt attempt = sessionKey == null ? null : readyAttempts.get(sessionKey);
        if (attempt != null) expirePeerReady(attempt);
        if (sessionKey == null || !sessionKey.equals(peerSessionKeyOf(source))
                || pendingConnections.get(sessionKey) != connection && connections.get(connection.getId()) != connection) {
            observeHandshake(HandshakeReason.READY_STALE_SESSION, source, null);
            return;
        }
        if (ready.isAcknowledgement()) {
            observeHandshake(HandshakeReason.ACK_RECEIVED, connection.getRemotePeer(), null);
            promotePendingConnection(connection);
        } else {
            observeHandshake(HandshakeReason.READY_RECEIVED, connection.getRemotePeer(), null);
            PeerReadyAttempt reply = new PeerReadyAttempt(connection, sessionKey,
                    rtcRoom.getLocalPeerInfo().getSessionId(), readyEpoch.get(), readyClock.getAsLong());
            sendPeerReadyControl(reply, true);
        }
    }

    public P2PConnection(
        NostrSigner localSigner,
        String gameName,
        int gameVersion,
        NostrPrivateKey roomKey,
        String turnServer,
        NostrPool masterServer,
        Runner dispatcher
    ) {
        this(localSigner, gameName, gameVersion, roomKey, turnServer, masterServer, dispatcher,
                DEFAULT_P2P_ATTEMPT_TIMEOUT);
    }

    public P2PConnection(
        NostrSigner localSigner,
        String gameName,
        int gameVersion,
        NostrPrivateKey roomKey,
        String turnServer,
        NostrPool masterServer,
        Runner dispatcher,
        Duration p2pAttemptTimeout
    ) {
        validateP2pAttemptTimeout(p2pAttemptTimeout);
        this.dispatcher = dispatcher;
        this.services = new HostedServiceManager(this);
        addStandardServices();
        this.gameName = gameName;
        this.version = gameVersion;
        this.localSigner = localSigner;
        this.masterServersPool = masterServer;
        this.readyExecutor = NGEPlatform.get().newAsyncExecutor(P2PConnection.class.getName() + "-peer-ready");
      

        NostrKeyPair roomKeyPair = new NostrKeyPair(roomKey);
        RTCSettings rtcSettings = RTCSettings.getDefault(gameName, gameName + ":" + gameVersion)
            .withStunServers(RTCSettings.PUBLIC_STUN_SERVERS)
            .withP2pAttemptTimeout(p2pAttemptTimeout)
            .withSignalingRelays(masterServersPool.getRelays().stream().map(relay -> relay.getUrl()).toList());
        NostrRTCLocalPeer localPeer = new NostrRTCLocalPeer(rtcSettings, localSigner, roomKeyPair, turnServer);
 
        this.turnPool = new NostrTURNPool();

        this.rtcRoom = new NostrRTCRoom(rtcSettings, localPeer, roomKeyPair, masterServersPool, turnPool);

        rtcRoom.addPeerDiscoveryListener((var1, var2, var3) -> {
            this.dispatcher.run(() -> {
                for (NostrRTCRoomPeerDiscoveredListener listener : peerDiscoveredListeners) {
                    listener.onRoomPeerDiscovered(var1, var2, var3);
                }
            });
        });

        rtcRoom.addPeerSocketAvailableListener((peerKey, socket) -> {
            log.fine("New connection from: " + peerKey);
            RemotePeer existingPubkeyConnection;
            RemotePeer existingPendingConnection;
            RemotePeer connection;
            synchronized (pendingConnections) {
                if (closed || expiredReadySessions.contains(peerSessionKeyOf(socket.getRemotePeer()))
                        || expiredReadySessions.size() >= MAX_EXPIRED_READY_SESSIONS) return;
                synchronized (inboundLock) {
                    if (inboundPeers.size() >= MAX_NATIVE_PEERS) return;
                }
                RemotePeer existingConnection = findConnectionByPeer(socket.getRemotePeer());
                if (existingConnection == null) {
                    existingConnection = findPendingConnectionByPeer(socket.getRemotePeer());
                }
                if (existingConnection != null) {
                    log.fine("Socket available for existing peer session: " + peerSessionKeyOf(socket.getRemotePeer()));
                    return;
                }
                existingPubkeyConnection = findConnectionByPubkey(socket.getRemotePeer());
                if (existingPubkeyConnection != null) {
                    // Session rollover for same pubkey: replace the old connection with a fresh one.
                    connections.remove(existingPubkeyConnection.getId(), existingPubkeyConnection);
                }
                existingPendingConnection = findPendingConnectionByPubkey(socket.getRemotePeer());
                if (existingPendingConnection != null) {
                    pendingConnections.remove(
                        peerSessionKeyOf(existingPendingConnection.getRemotePeer()),
                        existingPendingConnection
                    );
                }
                if (connections.size() + pendingConnections.size() >= MAX_NATIVE_PEERS) return;
                connection = new RemotePeer(nextConnectionId.getAndIncrement(), rtcRoom, socket.getLocalPeer(), socket.getRemotePeer(), this);
                pendingConnections.put(peerSessionKeyOf(connection.getRemotePeer()), connection);
                synchronized (inboundLock) { inboundPeers.put(connection, new InboundPeerState(connection)); }
                RemotePeer boundedPeer = connection;
                ((DynamicSerializerProtocol) connection.getProtocol()).setDetailedResourceFailureHandler((reason, failureClass) ->
                    failInboundPeer(boundedPeer, "Outbound protocol capacity or serialization failure",
                            reason == DynamicSerializerProtocol.OutboundFailure.CAPACITY
                                ? PeerRetirementReason.OUTBOUND_CAPACITY : PeerRetirementReason.OUTBOUND_SERIALIZATION,
                            failureClass));
            }
            if (existingPubkeyConnection != null) queuePeerRetirement(existingPubkeyConnection, true, PeerRetirementReason.NATIVE_SESSION_REPLACED);
            if (existingPendingConnection != null) {
                cancelPeerReady(existingPendingConnection);
                queuePeerRetirement(existingPendingConnection, false, PeerRetirementReason.NATIVE_SESSION_REPLACED);
            }
            observeHandshake(HandshakeReason.PENDING_CREATED, connection.getRemotePeer(), null);
            // A failed queued send or lost reply must not leave this session pending forever.
            beginPeerReady(connection);
        });

        rtcRoom.addDisconnectionListener((peerKey, socket) -> {
            log.fine("Connection closed: " + peerKey);
            observeHandshake(HandshakeReason.DISCONNECTED, socket.getRemotePeer(), null);
            RemotePeer pendingConnection;
            RemotePeer connection;
            synchronized (pendingConnections) {
                pendingConnection = findPendingConnectionByPeer(socket.getRemotePeer());
                if (pendingConnection != null) {
                    pendingConnections.remove(peerSessionKeyOf(pendingConnection.getRemotePeer()), pendingConnection);
                }
                connection = findConnectionByPeer(socket.getRemotePeer());
                if (connection != null) connections.remove(connection.getId(), connection);
            }
            if (pendingConnection != null) {
                cancelPeerReady(pendingConnection);
                queuePeerRetirement(pendingConnection, false, PeerRetirementReason.NATIVE_DISCONNECTED);
            }
            if (connection == null) {
                return;
            }
            queuePeerRetirement(connection, true, PeerRetirementReason.NATIVE_DISCONNECTED);
        });

        rtcRoom.addMessageListener((peerKey, socket, channel, bbf, isTurn) -> {
            if (!NostrRTCSocket.DEFAULT_CHANNEL_NAME.equals(channel.getName()) && !channel.getName().startsWith("nge-")) return;
            // Never bind a retired native session to a fresh connection by public key alone.
            RemotePeer peer = findConnectionByPeer(socket.getRemotePeer());
            if (peer == null) peer = findPendingConnectionByPeer(socket.getRemotePeer());
            if (peer == null || closed) return;
            boolean reliable = channel.isOrdered() && channel.isReliable();
            if (bbf.limit() > DynamicSerializerProtocol.MAX_FRAME_BYTES) {
                if (reliable) failInboundPeer(peer, "Oversized reliable frame");
                return;
            }
            InboundReservation reservation;
            try { reservation = reserveInbound(peer, bbf, reliable); }
            catch (RuntimeException error) {
                failInboundPeer(peer, "Invalid protocol frame", PeerRetirementReason.INVALID_PROTOCOL_FRAME,
                        error.getClass().getSimpleName());
                return;
            }
            if (reservation == null) {
                // Unreliable diffs never promote a base. The next reliable update still references the retained base.
                if (reliable) failInboundPeer(peer, "Reliable inbound capacity exhausted");
                return;
            }
            try {
                if (reservation.startsDecoder) drainWire(reservation.state);
            } finally {
                reservation.releaseNativeInput();
            }
        });

        NGEPlatform.get().registerFinalizer(
            this,
            () -> {
                close();
            }
        );
    }

    private static void logInboundFailure(String context, RuntimeException error) {
        StackTraceElement[] trace = error.getStackTrace();
        String origin = trace.length == 0 ? "unknown" : trace[0].toString();
        // Exception messages can contain decoded payloads; report only code locations.
        log.warning(context + ": " + error.getClass().getSimpleName()
            + " at " + origin.substring(0, Math.min(240, origin.length())));
    }

   
    public NostrSigner getLocalSigner() {
        return localSigner;
    }

    

    protected void addStandardServices() {
 
    }

    @Override
    public String getGameName() {
        return gameName;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public HostedServiceManager getServices() {
        return services;
    }

    @Override
    public void broadcast(Message message) {
        for (HostedConnection connection : connections.values()) {
            if (connection instanceof RemotePeer) {
                ((RemotePeer) connection).send(message);
            } else {
                connection.send(message);
            }
        }
    }

    /** Stable application payloads can use the RTC broadcast tree without per-peer serializer state. */
    public NostrRTCRoom getRtcRoom() {
        return rtcRoom;
    }

    @Override
    public void broadcast(Filter<? super HostedConnection> filter, Message message) {
        for (HostedConnection connection : connections.values()) {
            if (filter.apply(connection)) {
                if (connection instanceof RemotePeer) {
                    ((RemotePeer) connection).send(message);
                } else {
                    connection.send(message);
                }
            }
        }
    }

    @Override
    public void broadcast(int channel, Filter<? super HostedConnection> filter, Message message) {
         for (HostedConnection connection : connections.values()) {
            if (filter.apply(connection)) {
                if (connection instanceof RemotePeer) {
                    ((RemotePeer) connection).send(channel, message);
                } else {
                    connection.send(message);
                }
            }
        }
    }

    @Override
    public void start() {
        if (isStarted) {
            return;
        }
        rtcRoom.start();
        isStarted = true;
    }

    public void discover() {
        rtcRoom.discover();
    }

    /**
     * Disconnects every active session associated with the supplied public key.
     *
     * @return true when an active or pending session was found
     */
    public boolean disconnectPeer(NostrPublicKey peer) {
        if (peer == null || !hasPeer(peer)) {
            return false;
        }
        rtcRoom.disconnect(peer);
        return true;
    }

    void setPeerAdmission(Predicate<NostrPublicKey> peerAdmission) {
        this.peerAdmission = peerAdmission != null ? peerAdmission : ignored -> true;
    }

    private boolean hasPeer(NostrPublicKey peer) {
        for (RemotePeer connection : connections.values()) {
            if (hasPublicKey(connection, peer)) {
                return true;
            }
        }
        for (RemotePeer connection : pendingConnections.values()) {
            if (hasPublicKey(connection, peer)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPublicKey(RemotePeer connection, NostrPublicKey peer) {
        return connection != null
            && connection.getRemotePeer() != null
            && peer.equals(connection.getRemotePeer().getPubkey());
    }

    @Override
    public int addChannel(int port) {
        // nop
        return 0;
    }

    @Override
    public boolean isRunning() {
        return isStarted;
    }

    @Override
    public void close() {
        List<RemotePeer> pending;
        synchronized (pendingConnections) {
            closed = true;
            readyEpoch.incrementAndGet();
            pending = new java.util.ArrayList<>(pendingConnections.values());
            pendingConnections.clear();
            expiredReadySessions.clear();
        }
        for (RemotePeer connection : pending) { cancelPeerReady(connection); retireInboundPeer(connection); }
        for (RemotePeer connection : connections.values()) retireInboundPeer(connection);
        connections.clear();
        List<RemotePeer> retainedPeers;
        synchronized (inboundLock) { retainedPeers = new java.util.ArrayList<>(inboundPeers.keySet()); }
        for (RemotePeer peer : retainedPeers) retireInboundPeer(peer);
        readyAttempts.clear();
        readyExecutor.close();
        setHandshakeDiagnosticObserver(null);
        setPeerRetirementDiagnosticObserver(null);
        rtcRoom.close();
        isStarted = false;
    }

    @Override
    public HostedConnection getConnection(int id) {
        return connections.get(id);
    }

    @Override
    public boolean hasConnections() {
        return !connections.isEmpty();
    }

    @Override
    public Collection<HostedConnection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    public void addDiscoveryListener(NostrRTCRoomPeerDiscoveredListener listener) {
        peerDiscoveredListeners.add(listener);
    }

    public void removeDiscoveryListener(NostrRTCRoomPeerDiscoveredListener listener) {
        peerDiscoveredListeners.remove(listener);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public void addMessageListener(MessageListener<? super HostedConnection> listener) {
        messageListeners.addMessageListener(listener);
    }

    @Override
    public void addMessageListener(MessageListener<? super HostedConnection> listener, Class... classes) {
        messageListeners.addMessageListener(listener, classes);
    }

    @Override
    public void removeMessageListener(MessageListener<? super HostedConnection> listener) {
        messageListeners.removeMessageListener(listener);
    }

    @Override
    public void removeMessageListener(MessageListener<? super HostedConnection> listener, Class... classes) {
        messageListeners.removeMessageListener(listener, classes);
    }
}
