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
package org.ngengine.world2d.player;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.network.P2PConnection;
import org.ngengine.network.RemotePeer;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.network.components.NetcodePartitioning;
import org.ngengine.network.components.NetworkMessageHandler;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.player.Player;
import org.ngengine.player.PlayerManagerComponent;
import org.ngengine.world2d.TiledWorld2dManagerComponent;
import org.ngengine.world2d.player.messages.TiledPlayerSpawnMessage;
import org.ngengine.world2d.spawn.TiledSpawnPoint;
import org.ngengine.world2d.spawn.TiledSpawnPoints;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

import com.jme3.math.Vector2f;
import com.jme3.network.HostedConnection;

/**
 * Generic multiplayer lifecycle for player objects in a Tiled world.
 *
 * <p>Each peer owns one transient network ID. The manager announces that ID,
 * creates the local or remote player through {@link #createPlayerEntity}, keeps
 * player-to-entity lookup tables, chooses tagged spawn markers, and avoids
 * occupied player spawns. Games only provide the visual entity and may extend
 * {@link TiledPlayerSyncComponent} for game-specific state.</p>
 */
public abstract class TiledPlayerManagerComponent extends PlayerManagerComponent
        implements LogicFragment {
    public static final String DEFAULT_PLAYER_SPAWN_TAG = "player";
    private static final int DEFAULT_MESSAGE_CHANNEL = 1;
    private static final int[][] NEARBY_SPAWN_OFFSET_MULTIPLIERS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
        {2, 0}, {-2, 0}, {0, 2}, {0, -2}
    };
    private static final Logger logger =
        Logger.getLogger(TiledPlayerManagerComponent.class.getName());

    private final Map<NostrPublicKey, TiledObjectEntity> playerEntities = new HashMap<>();
    private final Map<TiledObjectEntity, NostrPublicKey> entityOwners = new HashMap<>();
    private final Map<NostrPublicKey, BigInteger> pendingPlayerIds = new HashMap<>();
    private final Set<NostrPublicKey> knownRemotePeers = new HashSet<>();
    private final Set<NostrPublicKey> currentRemotePeers = new HashSet<>();
    private final Vector2f spawnPosition = new Vector2f();
    private final Vector2f candidatePosition = new Vector2f();
    private final Vector2f existingPosition = new Vector2f();
    private final NetworkMessageHandler<TiledPlayerSpawnMessage> spawnHandler =
        this::onPlayerSpawnMessage;
    private BigInteger localPlayerEntityId;

    @Override
    protected void onEnable(ComponentManager manager, boolean firstTime) {
        super.onEnable(manager, firstTime);
        NetcodeManagerComponent netcode = manager.getInstanceOf(NetcodeManagerComponent.class);
        if (netcode != null) {
            netcode.registerMessageHandler(TiledPlayerSpawnMessage.class, spawnHandler);
        }
    }

    @Override
    protected void onDisable(ComponentManager manager) {
        NetcodeManagerComponent netcode = manager.getInstanceOf(NetcodeManagerComponent.class);
        if (netcode != null) {
            netcode.unregisterMessageHandler(TiledPlayerSpawnMessage.class, spawnHandler);
        }
        playerEntities.clear();
        entityOwners.clear();
        pendingPlayerIds.clear();
        knownRemotePeers.clear();
        currentRemotePeers.clear();
        localPlayerEntityId = null;
        super.onDisable(manager);
    }

    public final TiledObjectEntity getPlayerEntity(Player player) {
        if (player == null || player.getPublicKey() == null) {
            return null;
        }
        NostrPublicKey playerKey = player.getPublicKey();
        TiledObjectEntity entity = playerEntities.get(playerKey);
        if (entity != null) {
            return entity;
        }
        TiledMap map = getPlayerMap();
        if (map == null) {
            return null;
        }
        entity = findPlayerEntity(map, playerKey);
        if (entity != null) {
            rememberPlayerEntity(playerKey, entity);
        }
        return entity;
    }

    public final TiledObjectEntity getPlayerEntity(NostrPublicKey key) {
        return key != null ? getPlayerEntity(getPlayer(key)) : null;
    }

    public final TiledObjectEntity getPlayerEntity(NostrSigner signer) {
        return getPlayerEntity(getPlayer(signer));
    }

    public final TiledObjectEntity getPlayerEntity(RemotePeer peer) {
        return getPlayerEntity(getPlayer(peer));
    }

    public final TiledObjectEntity getPlayerEntity(P2PConnection connection) {
        return getPlayerEntity(getPlayer(connection));
    }

    public final TiledObjectEntity getPlayerEntity(HostedConnection connection) {
        return getPlayerEntity(getPlayer(connection));
    }

    public final NostrPublicKey getPlayerPublicKey(TiledObjectEntity entity) {
        return entityOwners.get(entity);
    }

    /** Ensures that the local player has one transient object in the active map. */
    public final TiledObjectEntity ensurePlayerSpawned(Player player) {
        if (player == null || player.getPublicKey() == null) {
            return null;
        }
        TiledObjectEntity existing = getPlayerEntity(player);
        if (existing != null) {
            return existing;
        }
        NetcodeManagerComponent netcode = getInstanceOf(NetcodeManagerComponent.class);
        if (netcode == null || !netcode.isNetworkSessionActive()
                || !player.getPublicKey().equals(netcode.getLocalPeerPublicKey())) {
            return null;
        }
        if (localPlayerEntityId == null) {
            localPlayerEntityId = netcode.getNextTemporaryNetworkUID();
        }
        queuePlayerSpawn(player.getPublicKey(), localPlayerEntityId);
        return getPlayerEntity(player);
    }

    @Override
    public final void updateAppLogic(ComponentManager manager, float tpf) {
        forgetDetachedPlayerEntities();
        processPendingPlayerSpawns();
        NetcodeManagerComponent netcode = getInstanceOf(NetcodeManagerComponent.class);
        if (netcode == null || !netcode.isNetworkSessionActive()) {
            knownRemotePeers.clear();
            return;
        }

        NostrPublicKey localKey = netcode.getLocalPeerPublicKey();
        TiledObjectEntity localEntity = localKey != null ? playerEntities.get(localKey) : null;
        currentRemotePeers.clear();
        for (RemotePeer peer : netcode.getRemotePeers()) {
            NostrPublicKey remoteKey = peer != null && peer.getRemotePeer() != null
                ? peer.getRemotePeer().getPubkey() : null;
            if (remoteKey == null) {
                continue;
            }
            currentRemotePeers.add(remoteKey);
            if (localEntity != null && !knownRemotePeers.contains(remoteKey)) {
                TiledPlayerSpawnMessage message = new TiledPlayerSpawnMessage();
                message.setPlayerPublicKey(localKey.asHex());
                message.setNetworkId(localEntity.getId());
                message.setReliable(true);
                netcode.sendMessageToPeer(peer, message, getPlayerMessageChannel(), true);
            }
        }
        knownRemotePeers.retainAll(currentRemotePeers);
        if (localEntity != null) {
            knownRemotePeers.addAll(currentRemotePeers);
        }
    }

    /** Creates the game's player object at the selected grid-space position. */
    protected abstract TiledObjectEntity createPlayerEntity(
        Player player,
        BigInteger networkId,
        Vector2f position
    );

    /** Returns the sync component mounted on every player entity. */
    protected Class<? extends TiledPlayerSyncComponent> getPlayerSyncComponentType() {
        return TiledPlayerSyncComponent.class;
    }

    protected String getPlayerSpawnTag() {
        return DEFAULT_PLAYER_SPAWN_TAG;
    }

    protected float getPlayerSpawnClearance(TiledMap map) {
        return map != null ? Math.max(map.getTileWidth(), map.getTileHeight()) * 0.25f : 1f;
    }

    protected int getPlayerMessageChannel() {
        return DEFAULT_MESSAGE_CHANNEL;
    }

    /** Called after the player has been attached to its object layer. */
    protected void onPlayerEntitySpawned(Player player, TiledObjectEntity entity) {
    }

    final void rememberPlayerEntity(NostrPublicKey playerKey, TiledObjectEntity entity) {
        if (playerKey == null || entity == null) {
            return;
        }
        playerEntities.put(playerKey, entity);
        entityOwners.put(entity, playerKey);
    }

    final void forgetPlayerEntity(TiledObjectEntity entity) {
        NostrPublicKey rememberedKey = entityOwners.remove(entity);
        if (rememberedKey != null) {
            playerEntities.remove(rememberedKey, entity);
        } else {
            playerEntities.values().removeIf(value -> value == entity);
        }
    }

    private void onPlayerSpawnMessage(RemotePeer source, TiledPlayerSpawnMessage message) {
        if (message == null || message.getNetworkId() == null
                || !NetcodePartitioning.isReservedId(message.getNetworkId())) {
            return;
        }
        String publicKeyHex = message.getPlayerPublicKey();
        if (publicKeyHex == null || publicKeyHex.isBlank()) {
            return;
        }
        NostrPublicKey playerKey;
        try {
            playerKey = NostrPublicKey.fromHex(publicKeyHex);
        } catch (RuntimeException invalidKey) {
            return;
        }
        NetcodeManagerComponent netcode = getInstanceOf(NetcodeManagerComponent.class);
        NostrPublicKey sourceKey = source != null && source.getRemotePeer() != null
            ? source.getRemotePeer().getPubkey()
            : netcode != null ? netcode.getLocalPeerPublicKey() : null;
        BigInteger encodedOwner =
            NetcodePartitioning.decodeReservedOwnerKey(message.getNetworkId());
        if (sourceKey == null || !sourceKey.equals(playerKey)
                || !new BigInteger(playerKey.asHex(), 16).equals(encodedOwner)) {
            logger.warning("Ignoring player spawn with mismatched identity or network ID owner.");
            return;
        }
        queuePlayerSpawn(playerKey, message.getNetworkId());
    }

    private void queuePlayerSpawn(NostrPublicKey playerKey, BigInteger entityId) {
        BigInteger pendingId = pendingPlayerIds.putIfAbsent(playerKey, entityId);
        if (pendingId != null && !pendingId.equals(entityId)) {
            logger.warning("Ignoring a second pending network ID for player " + playerKey.asHex());
            return;
        }
        if (spawnPlayerIfMissing(playerKey, entityId) != null) {
            pendingPlayerIds.remove(playerKey, entityId);
        }
    }

    private void processPendingPlayerSpawns() {
        if (pendingPlayerIds.isEmpty() || getPlayerMap() == null) {
            return;
        }
        Iterator<Map.Entry<NostrPublicKey, BigInteger>> iterator =
            pendingPlayerIds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<NostrPublicKey, BigInteger> pending = iterator.next();
            if (spawnPlayerIfMissing(pending.getKey(), pending.getValue()) != null) {
                iterator.remove();
            }
        }
    }

    private TiledObjectEntity spawnPlayerIfMissing(
            NostrPublicKey playerKey,
            BigInteger entityId) {
        TiledObjectEntity existing = playerEntities.get(playerKey);
        if (existing != null) {
            if (!entityId.equals(existing.getId())) {
                logger.warning("Ignoring a second network ID for player " + playerKey.asHex());
            }
            return existing;
        }
        TiledMap map = getPlayerMap();
        if (map == null) {
            return null;
        }
        TiledObjectEntity entity = findObjectById(map, entityId);
        if (entity != null) {
            if (!isPlayerEntity(entity)) {
                logger.warning("Cannot spawn player: network ID " + entityId + " is already in use.");
                return null;
            }
            rememberPlayerEntity(playerKey, entity);
            return entity;
        }

        TiledObjectLayer fallbackLayer = firstObjectLayer(map);
        TiledSpawnPoint target = TiledSpawnPoints.pickDeterministic(
            map, getPlayerSpawnTag(), entityId
        );
        if (target == null && fallbackLayer == null) {
            logger.warning("Cannot spawn player " + playerKey.asHex()
                + ": no object layer is available.");
            return null;
        }
        CoordinateSystem coordinates = getInstanceOf(CoordinateSystem.class);
        if (target != null) {
            target.getPosition(coordinates, spawnPosition);
        } else {
            spawnPosition.set(0f, 0f);
        }
        findFreePlayerSpawn(map, coordinates, target, spawnPosition, spawnPosition);
        Player player = getPlayer(playerKey);
        entity = createPlayerEntity(player, entityId, spawnPosition);
        if (entity == null) {
            throw new IllegalStateException("createPlayerEntity returned null.");
        }
        entity.putProperty("net.sync.component", getPlayerSyncComponentType().getName());
        TiledObjectLayer spawnLayer = target != null ? target.getLayer() : fallbackLayer;
        spawnLayer.add(entity);
        rememberPlayerEntity(playerKey, entity);
        entity.getComponentManager();
        onPlayerEntitySpawned(player, entity);
        return entity;
    }

    private void findFreePlayerSpawn(
            TiledMap map,
            CoordinateSystem coordinates,
            TiledSpawnPoint preferred,
            Vector2f fallback,
            Vector2f out) {
        out.set(fallback);
        if (!isOccupiedByPlayer(map, coordinates, out)) {
            return;
        }
        for (TiledSpawnPoint point : TiledSpawnPoints.find(map, getPlayerSpawnTag())) {
            if (preferred != null && point.getObject() == preferred.getObject()) {
                continue;
            }
            point.getPosition(coordinates, candidatePosition);
            if (!isOccupiedByPlayer(map, coordinates, candidatePosition)) {
                out.set(candidatePosition);
                return;
            }
        }
        float clearance = Math.max(1f, getPlayerSpawnClearance(map));
        for (int[] offset : NEARBY_SPAWN_OFFSET_MULTIPLIERS) {
            candidatePosition.set(
                fallback.x + offset[0] * clearance,
                fallback.y + offset[1] * clearance
            );
            if (!isOccupiedByPlayer(map, coordinates, candidatePosition)) {
                out.set(candidatePosition);
                return;
            }
        }
    }

    private boolean isOccupiedByPlayer(
            TiledMap map,
            CoordinateSystem coordinates,
            Vector2f candidate) {
        float clearance = getPlayerSpawnClearance(map);
        float clearanceSquared = clearance * clearance;
        for (TiledLayer layer : map.getLayersFlat()) {
            if (!(layer instanceof TiledObjectLayer)) {
                continue;
            }
            for (TiledObjectEntity entity : ((TiledObjectLayer) layer).getObjects()) {
                if (!isPlayerEntity(entity)) {
                    continue;
                }
                if (coordinates != null) {
                    coordinates.getCenterInGridSpace(entity, existingPosition);
                } else {
                    existingPosition.set((float) entity.getX(), (float) entity.getY());
                }
                if (existingPosition.distanceSquared(candidate) < clearanceSquared) {
                    return true;
                }
            }
        }
        return false;
    }

    private TiledObjectEntity findPlayerEntity(TiledMap map, NostrPublicKey playerKey) {
        BigInteger ownerKey = new BigInteger(playerKey.asHex(), 16);
        for (TiledLayer layer : map.getLayersFlat()) {
            if (!(layer instanceof TiledObjectLayer)) {
                continue;
            }
            for (TiledObjectEntity entity : ((TiledObjectLayer) layer).getObjects()) {
                if (isPlayerEntity(entity)
                        && ownerKey.equals(NetcodePartitioning.decodeReservedOwnerKey(entity.getId()))) {
                    return entity;
                }
            }
        }
        return null;
    }

    private boolean isPlayerEntity(TiledObjectEntity entity) {
        Object syncType = entity != null ? entity.getProperty("net.sync.component") : null;
        return getPlayerSyncComponentType().getName().equals(String.valueOf(syncType));
    }

    private void forgetDetachedPlayerEntities() {
        Iterator<Map.Entry<NostrPublicKey, TiledObjectEntity>> iterator =
            playerEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            TiledObjectEntity entity = iterator.next().getValue();
            if (entity == null || entity.getObjectGroup() == null) {
                entityOwners.remove(entity);
                iterator.remove();
            }
        }
    }

    private TiledObjectEntity findObjectById(TiledMap map, BigInteger id) {
        for (TiledLayer layer : map.getLayersFlat()) {
            if (!(layer instanceof TiledObjectLayer)) {
                continue;
            }
            TiledObjectEntity entity = ((TiledObjectLayer) layer).get(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    /** Returns the map in which player objects should be resolved and spawned. */
    protected TiledMap getPlayerMap() {
        TiledWorld2dManagerComponent worlds = getInstanceOf(TiledWorld2dManagerComponent.class);
        if (worlds == null || worlds.getDefaultWorld() == null) {
            return null;
        }
        return worlds.getDefaultWorld().getMap();
    }

    private TiledObjectLayer firstObjectLayer(TiledMap map) {
        for (TiledLayer layer : map.getLayersFlat()) {
            if (layer instanceof TiledObjectLayer) {
                return (TiledObjectLayer) layer;
            }
        }
        return null;
    }
}
