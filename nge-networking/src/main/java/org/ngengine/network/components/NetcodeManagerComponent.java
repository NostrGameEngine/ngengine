package org.ngengine.network.components;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Duration;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.actions.ComponentActionHandler;
import org.ngengine.components.actions.ComponentActionOrigin;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.config.RelayList;
import org.ngengine.network.Lobby;
import org.ngengine.network.LobbyCursor;
import org.ngengine.network.LobbyManager;
import org.ngengine.network.P2PConnection;
import org.ngengine.network.RemotePeer;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.player.Player;
import org.ngengine.player.PlayerManagerComponent;
import org.ngengine.runner.Runner;

import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;

import jakarta.annotation.Nullable;

/**
 * Main tiled networking component.
 *
 * <p>Owns lobby/session lifecycle, peer discovery, typed message transport and
 * bridges inbound action envelopes into the action runtime.
 */
public class NetcodeManagerComponent extends AbstractComponent implements LogicFragment {
    private static final Logger log = Logger.getLogger(NetcodeManagerComponent.class.getName());

    private final @Nullable NostrSigner signer;
    private @Nullable String turnServer;
    private LobbyManager lobbyManager;
    private P2PConnection connection;
    private Lobby connectedLobby;
    

    private final ArrayDeque<InboundMessage> inboundMessages = new ArrayDeque<>();
    private final Map<NetcodeFragment, RegisteredHandler> registeredActionHandlers = new HashMap<>();
    private final @Nullable NetcodeSpawner spawner;
    private final AtomicLong localReservedCounter = new AtomicLong();
    private final AtomicLong localPersistentCounter = new AtomicLong();
    

    private List<RemotePeer> connectedPeers = new ArrayList<>();
    private List<RemotePeer> connectedPeersRO = Collections.unmodifiableList(connectedPeers);
    private long connectedPeerSetVersion;
    private @Nullable NostrSigner cachedLocalPeerSigner;
    private @Nullable NostrPublicKey cachedLocalPeerPublicKey;
    private boolean cachedLocalPeerPublicKeyResolved;
    private long cachedKnownPeerSetVersion = -1;
    private @Nullable NostrPublicKey cachedKnownPeerLocalPublicKey;
    private @Nullable Set<NostrPublicKey> cachedKnownPeerPublicKeys;

    public void registerActionHandler(NetcodeFragment handler) {
        registeredActionHandlers.putIfAbsent(handler, new RegisteredHandler());
    }

    public void unregisterActionHandler(NetcodeFragment handler) {
        registeredActionHandlers.remove(handler);
    }

    private static final class RegisteredHandler {
        public Instant lastSnapshot = Instant.MIN;
    }

   

    private static final class InboundMessage {
        private final RemotePeer fromPeer;
        private final Message message;

        InboundMessage( RemotePeer fromPeer, Message message) {
            this.fromPeer = fromPeer;
            this.message = message;
        }

    

        RemotePeer getFromPeer() {
            return fromPeer;
        }

        Message getMessage() {
            return message;
        }
    }

    private final MessageListener<HostedConnection> messageListener = new MessageListener<HostedConnection>() {
        @Override
        public void messageReceived(HostedConnection conn, Message message) {
            if(!(conn instanceof RemotePeer)){
                throw new IllegalStateException("Expected RemotePeer connection, got " + conn.getClass().getName());
            }
            RemotePeer remotePeer = (RemotePeer) conn;
            inboundMessages.addLast(new InboundMessage(remotePeer, message));
        }
    };

    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connectionAdded(com.jme3.network.Server server, HostedConnection conn) {
            if(!(conn instanceof RemotePeer)){
                throw new IllegalStateException("Expected RemotePeer connection, got " + conn.getClass().getName());
            }
            RemotePeer remotePeer = (RemotePeer) conn;
            connectedPeers.add(remotePeer);
            invalidateKnownPeerPublicKeys();
            String pub = remotePeer.getRemotePeer() != null && remotePeer.getRemotePeer().getPubkey() != null
                ? remotePeer.getRemotePeer().getPubkey().asHex()
                : "unknown";
            log.info("Netcode peer connected: " + pub);
        }

        @Override
        public void connectionRemoved(com.jme3.network.Server server, HostedConnection conn) {
            if(!(conn instanceof RemotePeer)){
                throw new IllegalStateException("Expected RemotePeer connection, got " + conn.getClass().getName());
            }
            RemotePeer remotePeer = (RemotePeer) conn;
            connectedPeers.remove(remotePeer);
            invalidateKnownPeerPublicKeys();
            String pub = remotePeer.getRemotePeer() != null && remotePeer.getRemotePeer().getPubkey() != null
                ? remotePeer.getRemotePeer().getPubkey().asHex()
                : "unknown";
            log.info("Netcode peer disconnected: " + pub);
        }
    };


    public NetcodeManagerComponent() {
        this(null, null);
    }

    public NetcodeManagerComponent(@Nullable NetcodeSpawner spawner) {
        this(spawner, null);
    }

    public NetcodeManagerComponent(
        @Nullable NetcodeSpawner spawner,
        @Nullable NostrSigner signer
    ) {
        this.spawner = spawner;
        this.signer = signer;
    }

    public void setTurnServer(@Nullable String turnServer) {
        this.turnServer = turnServer;
        if (lobbyManager != null) {
            lobbyManager.setTurnServer(turnServer);
        }
    }

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        NostrPublicKey appId = mng.getSettings().getAppId();
        Number version = mng.getSettings().getNumber("VersionCode", 0);
        RelayList relays = mng.getSettings().getNostrRelays();
        Runner runner = mng.getRunner();
        NostrSigner s = this.signer != null ? this.signer : new NostrKeyPairSigner(new NostrKeyPair());
        lobbyManager = new LobbyManager(relays.get("lobby"), s, appId.asHex(), version.intValue(), runner);
        if (turnServer != null && !turnServer.isEmpty()) {
            lobbyManager.setTurnServer(turnServer);
        }

    }

    @Override
    protected void onDisable(ComponentManager mng) {
        disconnectFromLobby();
        if (lobbyManager != null) {
            lobbyManager.close();
            lobbyManager = null;
        }
        inboundMessages.clear();
        registeredActionHandlers.clear();
    }

    public void listLobbies(
        String words,
        int limit,
        @Nullable Map<String, String> dataFilter,
        BiConsumer<LobbyCursor, Throwable> callback
    ) {
        if (lobbyManager == null) {
            callback.accept(null, new IllegalStateException("TiledNetworkComponent is not enabled"));
            return;
        }
        lobbyManager.listLobbies(words, limit, dataFilter, callback);
    }

    public void listLobbies(BiConsumer<LobbyCursor, Throwable> callback) {
        listLobbies("", 24, null, callback);
    }

    public void connectToLobby(Lobby lobby, String passphrase) throws Exception {
        Objects.requireNonNull(lobby, "lobby");
        if (lobbyManager == null) {
            throw new IllegalStateException(getClass().getSimpleName()+" is not enabled");
        }
        // disconnect first (just make sure the state is clean)
        disconnectFromLobby();

        // connect!
        P2PConnection conn = lobbyManager.connectToLobby(lobby, passphrase);
        conn.addConnectionListener(connectionListener);
        conn.addMessageListener(messageListener);

        connection = conn;
        invalidateLocalPeerPublicKey();
        connectedLobby = lobby;
    }

    public void createLobby(
        String passphrase,
        Map<String, String> data,
        Duration expiration,
        BiConsumer<Lobby, Throwable> callback
    ) {
        if (lobbyManager == null) {
            callback.accept(null, new IllegalStateException(getClass().getSimpleName()+" is not enabled"));
            return;
        }
        Map<String, String> safeData = data != null ? data : Collections.emptyMap();
        Duration safeExpiration = expiration != null ? expiration : Duration.ofHours(12);
        lobbyManager.createLobby(passphrase, safeData, safeExpiration, callback);
    }

    public void disconnectFromLobby() {
        if (connection != null) {
            try {
                connection.removeMessageListener(messageListener);
                connection.removeConnectionListener(connectionListener);
                connection.close();
            } catch (Exception ex) {
                log.log(Level.FINE, "Error while disconnecting lobby", ex);
            }
        }
        connection = null;
        connectedLobby = null;
        connectedPeers.clear();
        inboundMessages.clear();
        invalidateLocalPeerPublicKey();
        invalidateKnownPeerPublicKeys();
    }

    public Lobby getLobby() {
        return connectedLobby;
    }

    public boolean isNetworkSessionActive() {
        return connection != null;
    }
  
    public Player getLocalPlayerInfo(){
        PlayerManagerComponent players = getInstanceOf(PlayerManagerComponent.class);
        if(players == null || connection == null){
            return null;
        }
        try {
            return players.getPlayer(connection.getLocalSigner());
        } catch (Exception ex) {
            return null;
        }
    }

    public @Nullable Player getLocalPlayer() {
        return getLocalPlayerInfo();
    }

    public List<Player> getDiscoveredPlayers() {
        List<Player> discovered = new ArrayList<>();
        for (RemotePeer peer : connectedPeersRO) {
            Player p = getRemotePlayerInfo(peer);
            if (p != null) {
                discovered.add(p);
            }
        }
        return Collections.unmodifiableList(discovered);
    }

    public Player getRemotePlayerInfo(RemotePeer peer){
        PlayerManagerComponent players = getInstanceOf(PlayerManagerComponent.class);
        if(players == null || connection == null || peer == null){
            return null;
        }
        try {
            return players.getPlayer(peer);
        } catch (Exception ex) {
            return null;
        }
    }

    public List<RemotePeer> getRemotePeers() {
        return connectedPeersRO;
    }

    public @Nullable NostrPublicKey getLocalPeerPublicKey() {
        NostrSigner localSigner = connection != null ? connection.getLocalSigner() : null;
        if (localSigner == null) {
            if (cachedLocalPeerSigner != null || cachedLocalPeerPublicKey != null || cachedLocalPeerPublicKeyResolved) {
                invalidateLocalPeerPublicKey();
            }
            return null;
        }
        if (localSigner != cachedLocalPeerSigner) {
            cachedLocalPeerSigner = localSigner;
            cachedLocalPeerPublicKey = null;
            cachedLocalPeerPublicKeyResolved = false;
            invalidateKnownPeerPublicKeys();
        }
        if (cachedLocalPeerPublicKeyResolved) {
            return cachedLocalPeerPublicKey;
        }
        try {
            var publicKeyTask = localSigner.getPublicKey();
            if (publicKeyTask == null) {
                return null;
            }
            NostrPublicKey publicKey = publicKeyTask.await();
            cachedLocalPeerPublicKey = publicKey;
            cachedLocalPeerPublicKeyResolved = publicKey != null;
            invalidateKnownPeerPublicKeys();
            return publicKey;
        } catch (Exception ex) {
            return null;
        }
    }

    public @Nullable String getLocalPeerId() {
        NostrPublicKey key = getLocalPeerPublicKey();
        return key != null ? key.asHex() : null;
    }

    public Set<NostrPublicKey> getKnownPeerPublicKeys() {
        NostrPublicKey local = getLocalPeerPublicKey();
        if (
            cachedKnownPeerPublicKeys != null
                && cachedKnownPeerSetVersion == connectedPeerSetVersion
                && Objects.equals(cachedKnownPeerLocalPublicKey, local)
        ) {
            return cachedKnownPeerPublicKeys;
        }
        Set<NostrPublicKey> peers = new LinkedHashSet<>();
        for (RemotePeer peer : connectedPeersRO) {
            if (peer == null || peer.getRemotePeer() == null || peer.getRemotePeer().getPubkey() == null) {
                continue;
            }
            peers.add(peer.getRemotePeer().getPubkey());
        }
        if (local != null) {
            peers.add(local);
        }
        cachedKnownPeerSetVersion = connectedPeerSetVersion;
        cachedKnownPeerLocalPublicKey = local;
        cachedKnownPeerPublicKeys = Collections.unmodifiableSet(peers);
        return cachedKnownPeerPublicKeys;
    }

 

    public boolean isPeerKnown(@Nullable String peerId) {
        if (peerId == null || peerId.isEmpty()) {
            return false;
        }
        try {
            NostrPublicKey key = peerId.startsWith("npub")
                ? NostrPublicKey.fromBech32(peerId)
                : NostrPublicKey.fromHex(peerId);
            return getKnownPeerPublicKeys().contains(key);
        } catch (Exception ex) {
            return false;
        }
    }

    public @Nullable String resolveActiveOwnerPeerId(@Nullable BigInteger networkId) {
        NostrPublicKey owner = resolveActiveOwnerPeerPublicKey(networkId);
        return owner != null ? owner.asHex() : null;
    }

    public @Nullable NostrPublicKey resolveActiveOwnerPeerPublicKey(@Nullable BigInteger networkId) {
        return NetcodeAuthorityAssignment.getPeerWithAuthority(networkId, getKnownPeerPublicKeys(), getLocalPeerPublicKey());
    }

    public BigInteger getNextTemporaryNetworkUID() {
        BigInteger ownerKey = requireLocalPeerKey();
        long seq = localReservedCounter.getAndIncrement();
        if (seq > NetcodePartitioning.RESERVED_SEQ_MASK.longValue()) {
            throw new IllegalStateException("Temporary network id range exhausted for local peer.");
        }
        return NetcodePartitioning.nextLocalReservedId(ownerKey, seq);
    }

    public BigInteger getNextPersistentNetworkUID() {
        BigInteger ownerKey = requireLocalPeerKey();
        long seq = localPersistentCounter.getAndIncrement();
        if (seq >= NetcodePartitioning.PERSISTENT_BLOCK_SIZE.longValue()) {
            throw new IllegalStateException("Persistent network id range exhausted for local peer.");
        }
        return NetcodePartitioning.nextLocalPersistentReservedId(ownerKey, seq);
    }

   

    private BigInteger requireLocalPeerKey() {
        NostrPublicKey local = getLocalPeerPublicKey();
        if (local == null) {
            throw new IllegalStateException("Local peer public key is not available.");
        }
        return new BigInteger(local.asHex(), 16);
    }


    public void sendMessageBroadcast(Message message, int channel, boolean reliable) {
        message.setReliable(reliable);
        for (RemotePeer conn : connectedPeersRO){
            conn.send(channel, message);
        }
    }

    public void sendMessageToPeer(RemotePeer peer, Message message, int channel, boolean reliable) {
        if (peer == null) {
            return;
        }
        message.setReliable(reliable);
        peer.send(channel, message);
    }

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        cleanupDetachedHandlers();
        while (!inboundMessages.isEmpty()) {
            InboundMessage inbound = inboundMessages.pollFirst();
            if (inbound == null || inbound.getMessage() == null) {
                continue;
            }
            dispatchMessage(inbound);
        }

        for(Entry<NetcodeFragment, RegisteredHandler> entry : registeredActionHandlers.entrySet()){
            NetcodeFragment handler = entry.getKey();
            RegisteredHandler data = entry.getValue();
            NetcodeBehavior behavior = handler.getNetworkBehavior();
            Instant now = Instant.now();
            boolean needsSnapshot = now.isAfter(data.lastSnapshot.plus(behavior.getSnapshotInterval()));
            boolean hasAuthority = handler.checkAuthority();

            if (!needsSnapshot || !hasAuthority) {
                continue;
            }

            for(RemotePeer peer : connectedPeersRO){
                SnapshotMessage snapshot = handler.requestSnapshot(peer);
                if(snapshot!=null){
                    snapshot.setComponentId(handler.getComponentId());
                    snapshot.setSource(ActionMessage.LOCAL_PEER);          
                    snapshot.setNetworkId(handler.getNetworkId());
                    sendMessageToPeer(peer, snapshot, snapshot.getChannel(), snapshot.isReliable());
                }
            }
            data.lastSnapshot = now;
        }

    }

    private void dispatchMessage(InboundMessage inbound) {
        Message message = inbound.getMessage();
        if(!(message instanceof ActionMessage)){
            log.log(Level.FINEST, "Received unknown message type: " + message.getClass().getName());
            return;
        }
        ActionMessage actionMessage = (ActionMessage) message;
        actionMessage.setSource(inbound.getFromPeer());
        NetcodeFragment handler = findRegisteredHandler(actionMessage);
        if (handler != null && !isHandlerAttached(handler)) {
            unregisterActionHandler(handler);
            handler = null;
        }
        if (actionMessage instanceof NetcodeDespawnActionMessage) {
            if (handler == null) {
                log.log(Level.WARNING,
                    "Dropping despawn message: target handler not found for componentId="
                        + actionMessage.getComponentId()
                        + ", networkId=" + actionMessage.getNetworkId()
                        + ", fromPeer="
                        + (inbound.getFromPeer() != null && inbound.getFromPeer().getRemotePeer() != null
                            && inbound.getFromPeer().getRemotePeer().getPubkey() != null
                            ? inbound.getFromPeer().getRemotePeer().getPubkey().asHex()
                            : "unknown")
                );
                return;
            }
            if (handler.checkAuthority(inbound.getFromPeer())) {            
                if (spawner != null) {
                    spawner.despawn(this, handler);
                }
            }
            return;
        }
        if(actionMessage instanceof SnapshotMessage){
            SnapshotMessage snapshotMessage = (SnapshotMessage) actionMessage;
            if(handler!=null){
                if(handler.checkAuthority(inbound.getFromPeer())){
                    try {
                        handler.onSnapshot(snapshotMessage);
                    } catch (Throwable ex) {
                        log.log(Level.WARNING, "Error applying snapshot to existing handler. componentId="
                            + actionMessage.getComponentId() + ", networkId=" + actionMessage.getNetworkId(), ex);
                        unregisterActionHandler(handler);
                    }
                }
                return;
            }

     
            // Handler not found. Validate authority first.
            BigInteger networkId = snapshotMessage.getNetworkId();
            java.util.Set<NostrPublicKey> knownPeers = new java.util.LinkedHashSet<>(getKnownPeerPublicKeys());
            NostrPublicKey senderKey = inbound.getFromPeer() != null
                && inbound.getFromPeer().getRemotePeer() != null
                ? inbound.getFromPeer().getRemotePeer().getPubkey()
                : null;
            if (senderKey != null) {
                // The sender is an active connection even if connectionAdded callback has not been processed yet.
                knownPeers.add(senderKey);
            }
            boolean hasAuthority = NetcodeAuthorityAssignment.hasAuthority(
                senderKey,
                networkId,
                knownPeers,
                getLocalPeerPublicKey()
            );

            if(!hasAuthority){
                log.log(Level.WARNING, "Received snapshot message for networkId " + networkId + " from peer " + inbound.getFromPeer().getRemotePeer().getPubkey().asHex() + " without authority");
                return;
            }

            if(spawner==null){
                return;
            }

            handler = spawner.spawn(this, snapshotMessage);
            if(handler==null || !isHandlerAttached(handler)){
                return;
            }
            this.registerActionHandler(handler);
            try {
                handler.onSnapshot(snapshotMessage);
            } catch (Throwable ex) {
                log.log(Level.WARNING, "Error applying snapshot to spawned handler. componentId="
                    + actionMessage.getComponentId() + ", networkId=" + actionMessage.getNetworkId(), ex);
                unregisterActionHandler(handler);
            }
        }else{
            Iterator<NetcodeFragment> it = registeredActionHandlers.keySet().iterator();
            while (it.hasNext()) {
                NetcodeFragment candidate = it.next();
                if (!isHandlerAttached(candidate)) {
                    it.remove();
                    continue;
                }
                if(
                    !Objects.equals(candidate.getNetworkId(), actionMessage.getNetworkId()) 
                    || !Objects.equals(candidate.getComponentId(), actionMessage.getComponentId())
                ){
                    continue;
                }

                ComponentActionHandler.Selection best = ComponentActionHandler.selectBest(
                    candidate,
                    actionMessage.getComponentId(),
                    actionMessage,
                    ComponentActionOrigin.REMOTE,
                    candidate::checkAuthority
                );
                if(best!=null){
                    best.getHandler().invoke(candidate, actionMessage);
                }
            }
        }
    }

    private @Nullable NetcodeFragment findRegisteredHandler(ActionMessage actionMessage) {
        Iterator<NetcodeFragment> it = registeredActionHandlers.keySet().iterator();
        while (it.hasNext()) {
            NetcodeFragment h = it.next();
            if (!isHandlerAttached(h)) {
                it.remove();
                continue;
            }
            if (
                Objects.equals(h.getComponentId(), actionMessage.getComponentId())
                && Objects.equals(h.getNetworkId(), actionMessage.getNetworkId())
            ) {
                return h;
            }
        }
        return null;
    }

    private void cleanupDetachedHandlers() {
        Iterator<NetcodeFragment> it = registeredActionHandlers.keySet().iterator();
        while (it.hasNext()) {
            NetcodeFragment h = it.next();
            if (!isHandlerAttached(h)) {
                it.remove();
            }
        }
    }

    private boolean isHandlerAttached(@Nullable NetcodeFragment handler) {
        if (!(handler instanceof Component)) {
            return true;
        }
        Component cmp = (Component) handler;
        return cmp.getComponentManager() != null;
    }

    public void invalidateLocalPeerPublicKey() {
        cachedLocalPeerSigner = null;
        cachedLocalPeerPublicKey = null;
        cachedLocalPeerPublicKeyResolved = false;
        invalidateKnownPeerPublicKeys();
    }

    private void invalidateKnownPeerPublicKeys() {
        connectedPeerSetVersion++;
        cachedKnownPeerSetVersion = -1;
        cachedKnownPeerLocalPublicKey = null;
        cachedKnownPeerPublicKeys = null;
    }

   
 
}
