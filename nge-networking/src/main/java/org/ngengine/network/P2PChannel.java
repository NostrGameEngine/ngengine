package org.ngengine.network;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.rtc.NostrRTCRoom;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDiscoveredListener;
import org.ngengine.nostr4j.rtc.signal.NostrRTCLocalPeer;
import org.ngengine.nostr4j.rtc.turn.NostrTURNSettings;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.RTCSettings;

import com.jme3.network.ConnectionListener;
import com.jme3.network.Filter;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Server;
import com.jme3.network.base.MessageListenerRegistry;
import com.jme3.network.base.MessageProtocol;
import com.jme3.network.base.protocol.SerializerMessageProtocol;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.serializer.ServerSerializerRegistrationsService;

public class P2PChannel implements Server {
    private static final Logger log = Logger.getLogger(P2PChannel.class.getName());
    private boolean isStarted = false;    

    

    private final String gameName;
    private final int version;
    private final HostedServiceManager services;
    private final Map<Integer, RemotePeer> connections = new ConcurrentHashMap<>();
    private final MessageListenerRegistry<HostedConnection> messageListeners = new MessageListenerRegistry<>();                        
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<NostrRTCRoomPeerDiscoveredListener> peerDiscoveredListeners = new CopyOnWriteArrayList<>();
    private final MessageProtocol protocol = new SerializerMessageProtocol();

    private final NostrSigner localSigner;
    private final NostrPool masterServersPool;
    private final NostrRTCRoom rtcRoom;

    private final Lobby lobby;

 
    public P2PChannel(
        NostrSigner localSigner,

        String gameName,
        int gameVersion,

        NostrPrivateKey roomKey,
        String turnServer,
        NostrPool masterServer,
        Lobby lobby
    ){
        this.services = new HostedServiceManager(this);
        addStandardServices();
        this.gameName = gameName;
        this.version = gameVersion;
        this.localSigner=localSigner;
        this.masterServersPool = masterServer;
        this.lobby=lobby;

        this.rtcRoom = new NostrRTCRoom(
                RTCSettings.DEFAULT,
                    NostrTURNSettings.DEFAULT,
            new NostrRTCLocalPeer(
                localSigner, 
                        RTCSettings.PUBLIC_STUN_SERVERS,
                        turnServer, 
                new HashMap<String, Object>()
            ),
            new NostrKeyPair(roomKey),
            masterServersPool
        );

        rtcRoom.addPeerDiscoveryListener((var1, var2, var3) -> {
            for (NostrRTCRoomPeerDiscoveredListener listener : peerDiscoveredListeners) {
                listener.onRoomPeerDiscovered(var1, var2, var3);
            }
        });

        rtcRoom.addConnectionListener((peerKey, socket) -> {
            log.fine("New connection from: " + peerKey);
            RemotePeer connection = new RemotePeer(connections.size(), socket, this, protocol);
            connections.put(connection.getId(), connection);

            for (ConnectionListener listener : connectionListeners) {
                listener.connectionAdded(this, connection);
            }

            updatePlayerCount();
        });

        rtcRoom.addDisconnectionListener((peerKey, socket) -> {
            log.fine("Connection closed: " + peerKey);
            for (Entry<Integer, RemotePeer> entry : connections.entrySet()) {
                if (entry.getValue().getSocket() == socket) {
                    RemotePeer connection = entry.getValue();
                    connections.remove(connection.getId());
                    for (ConnectionListener listener : connectionListeners) {
                        listener.connectionRemoved(this, connection);
                    }
                    break;
                }
            }
            updatePlayerCount();

        });

        rtcRoom.addMessageListener((peerKey, socket, bbf, isTurn) -> {
            log.fine("Message from: " + peerKey);
            for (Entry<Integer, RemotePeer> entry : connections.entrySet()) {
                if (entry.getValue().getSocket() == socket) {
                    RemotePeer connection = entry.getValue();
                    Message message = protocol.toMessage(bbf);
                    if (message == null) {
                        log.warning("Received null message from: " + peerKey);
                        return;
                    }
                    message.setReliable(true);
                    messageListeners.messageReceived(connection, message);
                    break;
                }
            }

        });
    }

    protected void updatePlayerCount() {     
        if (lobby instanceof LocalLobby) {
            int playerCount = connections.size();
            LocalLobby localLobby = (LocalLobby) lobby;
            try{
                localLobby.setData("numPeers", String.valueOf(playerCount));
            } catch (Exception e) {
                log.warning("Failed to update player count: " + e.getMessage());
            }
        }
    }
 

    protected void addStandardServices() {
        log.fine("Adding standard services...");
        services.addService(new ServerSerializerRegistrationsService());
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
            connection.send(message);
        }
    }

    @Override
    public void broadcast(Filter<? super HostedConnection> filter, Message message) {
        for (HostedConnection connection : connections.values()) {
            if(filter.apply(connection)) {
                connection.send(message);
            }
        }
    }

    @Override
    public void broadcast(int channel, Filter<? super HostedConnection> filter, Message message) {
        broadcast(filter, message);
    }

    @Override
    public void start() {
        rtcRoom.start();
        isStarted = true;
    }

    public void discover(){
        rtcRoom.discover();
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
        isStarted = false;
    }

    @Override
    public HostedConnection getConnection( int id )
    {
        return connections.get(id);
    }     
 
    @Override
    public boolean hasConnections()
    {
        return !connections.isEmpty();
    }
 
    @Override
    public Collection<HostedConnection> getConnections()
    {
        return Collections.unmodifiableCollection(connections.values());
    } 


    public void addDiscoveryListener(NostrRTCRoomPeerDiscoveredListener listener) {
        peerDiscoveredListeners.add(listener);
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
