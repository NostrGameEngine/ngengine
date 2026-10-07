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

import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.Server;
import com.jme3.network.base.MessageProtocol;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.network.protocol.messages.ClassRegistrationAckMessage;
import org.ngengine.nostr4j.rtc.NostrRTCChannel;
import org.ngengine.nostr4j.rtc.NostrRTCRoom;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.AsyncExecutor;

public class RemotePeer implements HostedConnection {

    private static final Logger log = Logger.getLogger(RemotePeer.class.getName());

    private final P2PConnection server;
    private final NostrRTCPeer remotePeer;
    private final NostrRTCRoom room;
    private final int id;
    private final Map<String, Object> sessionData = new ConcurrentHashMap<>();
    private final MessageProtocol protocol;
    private final ReliableChannelGate reliableChannels;
    private final java.util.concurrent.atomic.AtomicBoolean retirementReported = new java.util.concurrent.atomic.AtomicBoolean();

    private final java.util.concurrent.atomic.AtomicReference<ReliableFailureDiagnostic> reliableFailure =
            new java.util.concurrent.atomic.AtomicReference<>();

    public enum ReliableFailureOrigin { OUTBOUND_GATE, OPEN_ACK_REPLY }
    /** First failure before teardown. Counters cover the peer; acknowledged describes only OUTBOUND_GATE. */
    public record ReliableFailureDiagnostic(ReliableFailureOrigin origin, String reason, int channel,
            boolean acknowledged, int pendingCount, long pendingBytes, String operation, List<String> causeClasses) {
        public ReliableFailureDiagnostic { causeClasses = List.copyOf(causeClasses); }
        public ReliableFailureDiagnostic(ReliableFailureOrigin origin, String reason, int channel,
                boolean acknowledged, int pendingCount, long pendingBytes) {
            this(origin, reason, channel, acknowledged, pendingCount, pendingBytes,
                    ReliableChannelGate.FailureOperation.NONE.name(), List.of());
        }
    }

    public ReliableFailureDiagnostic reliableFailureDiagnostic() { return reliableFailure.get(); }

    boolean markRetirementReported() { return retirementReported.compareAndSet(false, true); }

 
    RemotePeer(int id, NostrRTCRoom room, NostrRTCPeer localPeer, NostrRTCPeer remotePeer, P2PConnection server) {
        this(id, room, localPeer, remotePeer, server, server.reliableChannelExecutor(), System::nanoTime);
    }

    RemotePeer(int id, NostrRTCRoom room, NostrRTCPeer localPeer, NostrRTCPeer remotePeer, P2PConnection server,
            AsyncExecutor executor, LongSupplier clock) {
        this.room = room;
        this.server = server;
        this.remotePeer = remotePeer;
        this.id = id;
        boolean side = localPeer.getPubkey().asHex().compareTo(remotePeer.getPubkey().asHex()) < 0;
        this.protocol = new DynamicSerializerProtocol(true, this::onClassRegistered, side ? 1L : -1L);
        this.reliableChannels = new ReliableChannelGate(executor, clock, new ReliableChannelGate.Transport() {
            @Override public AsyncTask<Void> open(int channel, ByteBuffer control) {
                return sendPreparedControl(new OpenChannelMessage(channel, false), control);
            }
            @Override public AsyncTask<Void> send(int channel, ByteBuffer frame) {
                return sendReliableFrame(channel, frame);
            }
        }, this::failOutboundReliableChannel);
    }

    private void failOutboundReliableChannel(ReliableChannelGate.Failure failure) {
        ReliableChannelGate.FailureDiagnostic captured = reliableChannels.failureDiagnostic();
        reportReliableFailure(new ReliableFailureDiagnostic(ReliableFailureOrigin.OUTBOUND_GATE, failure.name(),
                captured == null ? -1 : captured.channel(), captured != null && captured.acknowledged(),
                captured == null ? 0 : captured.pendingCount(), captured == null ? 0 : captured.pendingBytes(),
                captured == null ? ReliableChannelGate.FailureOperation.NONE.name() : captured.operation().name(),
                captured == null ? List.of() : captured.causeClasses()));
        server.failReliableChannel(this, failure);
    }

    private void failOpenAckReply(int channel, ReliableChannelGate.FailureOperation operation, Throwable error) {
        ReliableChannelGate.Diagnostic captured = reliableChannels.diagnostic(channel);
        reportReliableFailure(new ReliableFailureDiagnostic(ReliableFailureOrigin.OPEN_ACK_REPLY,
                ReliableChannelGate.Failure.CONTROL_SEND_FAILED.name(), channel,
                false, captured.pendingCount(), captured.pendingBytes(), operation.name(),
                ReliableChannelGate.failureCauseClasses(error)));
        server.failReliableChannel(this, ReliableChannelGate.Failure.CONTROL_SEND_FAILED);
    }

    private void reportReliableFailure(ReliableFailureDiagnostic event) {
        if (!reliableFailure.compareAndSet(null, event)) return;
        try {
            log.info("NGE_RELIABLE_CHANNEL_FAILED peer=" + remotePeer.getPubkey().asHex()
                    + " peerId=" + id + " origin=" + event.origin().name() + " failure=" + event.reason()
                    + " channel=" + event.channel() + " acknowledged=" + event.acknowledged()
                    + " pendingCount=" + event.pendingCount() + " pendingBytes=" + event.pendingBytes()
                    + " operation=" + event.operation() + " causeClasses="
                    + (event.causeClasses().isEmpty() ? "NONE" : String.join(">", event.causeClasses())));
        } catch (Throwable ignored) { /* Diagnostic sinks cannot prevent existing peer retirement. */ }
    }

    /**
     * Channel 2 state, with peer-wide queue totals (including in-flight frames) and saturating counters.
     * nativeSends counts custom reliable native send invocations, never receipt or application acknowledgement.
     * No payload or identifiers.
     */
    public record ReliableChannelDiagnostic(String state, int pendingCount, long pendingBytes,
            int openControlSendFailed, int timeouts, int acknowledgements, int nativeSends) {}

    public ReliableChannelDiagnostic reliableChannelDiagnostic() {
        ReliableChannelGate.Diagnostic value = reliableChannels.diagnostic(2);
        return new ReliableChannelDiagnostic(value.state(), value.pendingCount(), value.pendingBytes(),
                value.openControlSendFailed(), value.timeouts(), value.acknowledgements(), value.nativeSends());
    }

    void retireReliableChannels() { reliableChannels.close(); }

    private void onClassRegistered(long id) {
        log.fine("Send registration ack for class id " + id);
        send(new ClassRegistrationAckMessage(id));
    }


    public MessageProtocol getProtocol() {
        return protocol;
    }

    public NostrRTCPeer getRemotePeer() {
        return remotePeer;
    }

    AsyncTask<Void> sendPeerReady(P2PConnection.PeerReadyMessage message) {
        try {
            if (reliableChannels.isClosed()) return AsyncTask.failed(new IllegalStateException("Peer retired"));
            ByteBuffer buffer = protocol.toByteBuffer(message, null);
            if (buffer == null || !buffer.hasRemaining()) {
                return AsyncTask.failed(new IllegalStateException("Empty peer-ready control message"));
            }
            return room.send(remotePeer, buffer);
        } catch (Throwable error) {
            return AsyncTask.failed(error);
        }
    }

    private static String channelName(int channel, boolean reliable) {
        return "nge-" + Math.max(1, channel) + (reliable ? "-r" : "-u");
    }

 

    AsyncTask<Void> sendControl(Message message) {
        try {
            if (reliableChannels.isClosed()) return AsyncTask.failed(new IllegalStateException("Peer retired"));
            ByteBuffer buffer = protocol.toByteBuffer(message, null);
            if (buffer == null || !buffer.hasRemaining()) {
                return AsyncTask.failed(new IllegalStateException("Empty P2P control message"));
            }
            return room.send(remotePeer, buffer);
        } catch (Throwable error) {
            return AsyncTask.failed(error);
        }
    }

    AsyncTask<Void> sendPreparedControl(Message message, ByteBuffer buffer) {
        if (reliableChannels.isClosed()) return AsyncTask.failed(new IllegalStateException("Peer retired"));
        return room.send(remotePeer, buffer);
    }

    AsyncTask<Void> sendReliableFrame(int channel, ByteBuffer frame) {
        NostrRTCChannel nativeChannel = room.createChannel(remotePeer, channelName(channel, true), true, true,
                Integer.valueOf(0), null);
        return room.send(nativeChannel, frame);
    }

    void handleOpenChannel(OpenChannelMessage message) {
        if (reliableChannels.isClosed()) return;
        int channel = message.getChannel();
        if (message.isAcknowledgement()) {
            reliableChannels.acknowledge(channel);
            return;
        }
        try {
            room.createChannel(remotePeer, channelName(channel, true), true, true, Integer.valueOf(0), null);
            // The receiver may never send an unreliable frame, so register both logical receive paths before ACK.
            room.createChannel(remotePeer, channelName(channel, false), false, false, Integer.valueOf(0), null);
            sendControl(new OpenChannelMessage(channel, true)).catchException(error ->
                    failOpenAckReply(channel, ReliableChannelGate.FailureOperation.ACK_REPLY_ASYNC, error));
        } catch (Throwable error) {
            failOpenAckReply(channel, ReliableChannelGate.FailureOperation.ACK_REPLY_SYNC, error);
        }
    }

    @Override
    public void send(Message message) {
        send(0, message);
    }

    @Override
    public void send(int c, Message message) {
        try{
            if (reliableChannels.isClosed()) return;
            Objects.requireNonNull(message);
            if(c < 0){
                c = 0;
            }

            int channel = c;
            ByteBuffer buffer = protocol.toByteBuffer(message, null);
            if (buffer == null || !buffer.hasRemaining()) {
                return;
            }
            if (channel == 0 && message.isReliable()) {
                room.send(remotePeer, buffer);
                return;
            }

            if(message.isReliable()){
                // Serialization can retire a protocol while holding its own monitor; never acquire it inside the gate.
                ByteBuffer control = reliableChannels.needsOpen(channel)
                        ? protocol.toByteBuffer(new OpenChannelMessage(channel, false), null) : null;
                reliableChannels.submit(channel, buffer, control);
            } else {
                NostrRTCChannel chan = room.createChannel(remotePeer, channelName(channel, false), false, false, Integer.valueOf(0), null);
                room.send(chan, buffer);      
            }
        } catch (Throwable ex) {
            log.log(Level.FINEST, "Failed to send message to peer " + remotePeer, ex);
        }
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getAddress() {
        return remotePeer.getPubkey().asHex();
    }

    @Override
    public void close(String reason) {
        retireReliableChannels();
        room.disconnect(remotePeer);
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) return sessionData.remove(name);
        return sessionData.put(name, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String name) {
        return (T) sessionData.get(name);
    }

    @Override
    public Set<String> attributeNames() {
        return Collections.unmodifiableSet(sessionData.keySet());
    }
}
