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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.ngengine.nostr4j.rtc.NostrTURNPool;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDiscoveredListener;
import org.ngengine.nostr4j.rtc.signal.NostrRTCLocalPeer;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.runner.Runner;

public class P2PConnection implements Server {

    private static final Logger log = Logger.getLogger(P2PConnection.class.getName());
    private boolean isStarted = false;

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
    private final MessageListenerRegistry<HostedConnection> messageListeners = new MessageListenerRegistry<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<NostrRTCRoomPeerDiscoveredListener> peerDiscoveredListeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextConnectionId = new AtomicInteger();

    private final NostrSigner localSigner;
    private final NostrPool masterServersPool;
    private final NostrRTCRoom rtcRoom;
    private final NostrTURNPool turnPool;

    private Runner dispatcher;

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
        if (sessionKey == null || !pendingConnections.remove(sessionKey, connection)) {
            return;
        }
        connections.put(connection.getId(), connection);
        this.dispatcher.run(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.connectionAdded(this, connection);
            }
        });
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
        this.dispatcher = dispatcher;
        this.services = new HostedServiceManager(this);
        addStandardServices();
        this.gameName = gameName;
        this.version = gameVersion;
        this.localSigner = localSigner;
        this.masterServersPool = masterServer;
      

        NostrKeyPair roomKeyPair = new NostrKeyPair(roomKey);
        NostrRTCLocalPeer localPeer = new NostrRTCLocalPeer(
            localSigner, 
            RTCSettings.PUBLIC_STUN_SERVERS, 
            gameName,
            gameName+":"+gameVersion,
            roomKeyPair,
            turnServer
        );
 
        this.turnPool = new NostrTURNPool();

        this.rtcRoom = new NostrRTCRoom(
            RTCSettings.DEFAULT,
            localPeer,
            roomKeyPair,
            masterServersPool,
            turnServer,
            turnPool
        );

        rtcRoom.addPeerDiscoveryListener((var1, var2, var3) -> {
            this.dispatcher.run(() -> {
                for (NostrRTCRoomPeerDiscoveredListener listener : peerDiscoveredListeners) {
                    listener.onRoomPeerDiscovered(var1, var2, var3);
                }
            });
        });

        rtcRoom.addPeerSocketAvailableListener((peerKey, socket) -> {
      
            log.fine("New connection from: " + peerKey);
            RemotePeer existingConnection = findConnectionByPeer(socket.getRemotePeer());
            if (existingConnection == null) {
                existingConnection = findPendingConnectionByPeer(socket.getRemotePeer());
            }
            if (existingConnection != null) {
                log.fine("Socket available for existing peer session: " + peerSessionKeyOf(socket.getRemotePeer()));
                return;
            }
            RemotePeer existingPubkeyConnection = findConnectionByPubkey(socket.getRemotePeer());
            if (existingPubkeyConnection != null) {
                // Session rollover for same pubkey: replace the old connection with a fresh one.
                connections.remove(existingPubkeyConnection.getId());
                this.dispatcher.run(() -> {
                    for (ConnectionListener listener : connectionListeners) {
                        listener.connectionRemoved(this, existingPubkeyConnection);
                    }
                });
            }
            RemotePeer existingPendingConnection = findPendingConnectionByPubkey(socket.getRemotePeer());
            if (existingPendingConnection != null) {
                pendingConnections.remove(
                    peerSessionKeyOf(existingPendingConnection.getRemotePeer()),
                    existingPendingConnection
                );
            }
            RemotePeer connection = new RemotePeer(nextConnectionId.getAndIncrement(), rtcRoom, socket.getLocalPeer(), socket.getRemotePeer(), this);
            pendingConnections.put(peerSessionKeyOf(connection.getRemotePeer()), connection);
            // The room queues default-channel messages until the channel is ready.
            connection.send(new PeerReadyMessage(false));
        });

        rtcRoom.addDisconnectionListener((peerKey, socket) -> {
            log.fine("Connection closed: " + peerKey);
            RemotePeer pendingConnection = findPendingConnectionByPeer(socket.getRemotePeer());
            if (pendingConnection != null) {
                pendingConnections.remove(peerSessionKeyOf(pendingConnection.getRemotePeer()), pendingConnection);
            }
            RemotePeer connection = findConnectionByPeer(socket.getRemotePeer());
            if (connection == null) {
                return;
            }
            connections.remove(connection.getId());
            this.dispatcher.run(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.connectionRemoved(this, connection);
                }
            });
        });

        rtcRoom.addMessageListener((peerKey, socket, channel, bbf, isTurn) -> {
            try{
                RemotePeer connection = findConnectionByPeer(socket.getRemotePeer());
                if (connection == null) {
                    connection = findPendingConnectionByPeer(socket.getRemotePeer());
                }
                if (connection == null) {
                    connection = findConnectionByPubkey(socket.getRemotePeer());
                }
                if (connection == null) {
                    connection = findPendingConnectionByPubkey(socket.getRemotePeer());
                }
                if (connection == null) {
                    log.finer("Message received for unknown peer: " + peerKeyOf(socket.getRemotePeer()));
                    return;
                }
                MessageProtocol protocol = connection.getProtocol();
                Message message = protocol.toMessage(bbf);
                if(message instanceof ClassRegistrationAckMessage && protocol instanceof DynamicSerializerProtocol){
                    int id = (int)((ClassRegistrationAckMessage)message).getClassId();
                    log.finer("Class registration acknowledged by remote peer for id: " + id);
                    DynamicSerializerProtocol dyn = (DynamicSerializerProtocol)protocol;
                    dyn.markClassRegistered(id);
                    return;
                }
                if (message instanceof OpenChannelMessage) {
                    connection.handleOpenChannel((OpenChannelMessage) message);
                    return;
                }
                if (message instanceof PeerReadyMessage) {
                    PeerReadyMessage ready = (PeerReadyMessage) message;
                    if (ready.isAcknowledgement()) {
                        promotePendingConnection(connection);
                    } else {
                        connection.send(new PeerReadyMessage(true));
                    }
                    return;
                }
                if (message == null) {
                    log.warning("Received null message from: " + peerKey);
                    return;
                }
                if (findPendingConnectionByPeer(connection.getRemotePeer()) != null) {
                    log.finer("Dropping application message until peer-ready round-trip completes: " + peerKey);
                    return;
                }
            
                message.setReliable(channel.isOrdered()&&channel.isReliable());
                RemotePeer finalConnection = connection;
                this.dispatcher.run(() -> {
                    messageListeners.messageReceived(finalConnection, message);
                });
            } catch (Throwable e) {
                log.log(java.util.logging.Level.FINER, "Error processing message", e);
            }
        });

        NGEPlatform.get().registerFinalizer(
            this,
            () -> {
                rtcRoom.close();
            }
        );
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

    /** Disconnects every active session associated with the supplied public key. */
    public void disconnectPeer(NostrPublicKey peer) {
        if (peer != null) {
            rtcRoom.disconnect(peer);
        }
    }

    /** Bans a public key for this room and closes all of its active sessions. */
    public void banPeer(NostrPublicKey peer) {
        if (peer != null) {
            rtcRoom.ban(peer);
        }
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
        rtcRoom.close();
        pendingConnections.clear();
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
