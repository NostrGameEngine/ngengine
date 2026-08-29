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

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ngengine.nostr4j.NostrFilter;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.NostrRelay;
import org.ngengine.nostr4j.event.NostrEvent.TagValue;
import org.ngengine.nostr4j.event.SignedNostrEvent;
import org.ngengine.nostr4j.event.UnsignedNostrEvent;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.nip49.Nip49;
import org.ngengine.nostr4j.nip50.NostrSearchFilter;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;
import org.ngengine.runner.Runner;

import jakarta.annotation.Nullable;

public class LobbyManager implements Closeable {

    private final int KIND = 30078;
    private final NostrPool masterServersPool;
    private final NostrSigner localSigner;
    private final String gameName;
    private final int gameVersion;
    private final AsyncExecutor looper;
    private final ArrayList<WeakReference<Lobby>> trackedLobbies = new ArrayList<>();
    private final Map<Lobby, P2PConnection> connectedLobbies = new WeakHashMap<>();
    private final Set<Lobby> refreshInFlight = Collections.newSetFromMap(new WeakHashMap<Lobby, Boolean>());
    private final Runner dispatcher;
    private volatile boolean closed = false;
    private String turnServer = null;

    private transient Boolean isSearchSupported;

    private static final Logger log = Logger.getLogger(LobbyManager.class.getName());

 
    public LobbyManager(
        Collection<String> relays,
        NostrSigner signer,
        String gameName,
        int gameVersion,
        Runner dispatcher
    ) {
        this.dispatcher = dispatcher;

        this.looper = NGEUtils.getPlatform().newAsyncExecutor(LobbyManager.class);
        this.localSigner = signer;
        this.gameName = gameName;
        this.gameVersion = gameVersion;

 
        this.masterServersPool = new NostrPool();
        for (String server : relays) {
            try {
                this.masterServersPool.connectRelay(new NostrRelay(server));
            } catch (Exception e) {
                log.warning("Failed to add server: " + server);
            }
        }

        update();
    }

 

    protected void update() {
        this.looper.runLater(
                () -> {
                    if (closed) return null;
                    try {
                        synchronized (trackedLobbies) {
                            Iterator<WeakReference<Lobby>> it = trackedLobbies.iterator();
                            while (it.hasNext()) {
                                WeakReference<Lobby> ref = it.next();
                                Lobby lobby = ref.get();
                                if (lobby == null) {
                                    it.remove();
                                    continue;
                                }
                                if (lobby instanceof LocalLobby) {
                                    LocalLobby llobby = (LocalLobby) lobby;
                                    if (llobby.isUpdateNeeded()) {
                                        updateLobby((LocalLobby) lobby);
                                        llobby.clearUpdateNeeded();
                                    }
                                }
                            }
                        }
                        refreshConnectedLobbies();
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error during lobby manager update: " + e.getMessage(), e);
                    }
                    update();

                    return null;
                },
                10000,
                TimeUnit.MILLISECONDS
            );
    }

    public void close() {
        closed = true;
        synchronized (connectedLobbies) {
            for (Lobby lobby : connectedLobbies.keySet()) {
                if (lobby instanceof LocalLobby) {
                    ((LocalLobby) lobby).setBanKickHandler(null);
                }
            }
            connectedLobbies.clear();
            refreshInFlight.clear();
        }
        try {
            looper.close();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to close executor: " + e.getMessage());
        }
    }

    public void listLobbies(NostrFilter filter, int limit, Duration timeout,  BiConsumer<List<Lobby>, Throwable> callback) {
        masterServersPool
            .fetch(filter,
                limit,
                true,
                timeout
            )
            .then(events -> {
                List<Lobby> lobbies = new ArrayList<>();
                NGEPlatform p = NGEUtils.getPlatform();
                for (SignedNostrEvent event : events) {
                    try {
                        String rawData = event.getContent();
                        Map<String, String> data = p.fromJSON(rawData, Map.class);
                        if (data == null) continue;

                        String roomKey = NGEUtils.safeString(data.get("roomKey"));
                        if (roomKey == null) continue;

                        String roomId = event.getFirstTag("d").get(0);
                        Instant expiration = event.getExpiration();
                        Instant creationTime = event.getCreatedAt();

                        NostrPublicKey owner = event.getPubkey();
                        Lobby lobby;
                        if (event.getPubkey().equals(this.localSigner.getPublicKey().await())) {
                            lobby = new LocalLobby(roomId, roomKey, rawData, expiration, creationTime, owner);
                        } else {
                            lobby = new Lobby(roomId, roomKey, rawData, expiration, creationTime, owner);
                        }
                        Map<String, String> snapshot = new HashMap<>();
                        for (String tagKey : event.listTagKeys()) {
                            TagValue tagValue = event.getFirstTag(tagKey);
                            if (tagKey.equals("expiration")) continue; // ignore expiration tag
                            snapshot.put(tagKey, tagValue.get(0));
                        }
                        snapshot.putAll(data);
                        lobby.replaceSnapshot(rawData, snapshot);
                        lobby.recordSnapshotRevision(event.getCreatedAt(), event.getId());

                        lobbies.add(lobby);
                    } catch (Exception e) {
                        log.warning("Failed to parse lobby: " + e.getMessage());
                        continue;
                    }
                }
                this.dispatcher.run(() -> {
                        callback.accept(lobbies, null);
                    });
                return lobbies;
            })
            .catchException(ex -> {
                log.log(Level.WARNING, "Failed to fetch lobbies: " + ex.getMessage(), ex);
                this.dispatcher.run(() -> {
                        callback.accept(null, ex);
                    });
            });
    }

    private boolean isSearchSupported() {
        if (isSearchSupported != null) return isSearchSupported;
        try {
            for (NostrRelay relay : masterServersPool.getRelays()) {
                if (!relay.getInfo().isNipSupported(50)) {
                    isSearchSupported = false;
                    return isSearchSupported;
                }
            }
            return isSearchSupported;
        } catch (Exception e) {
            log.warning("Failed to check search support: " + e.getMessage());
            isSearchSupported = false;
            return isSearchSupported;
        }
    }

    public void listLobbies(
        String words,
        int limit,
        @Nullable Map<String, String> dataFilter,
        BiConsumer<LobbyCursor, Throwable> callback
    ) {
        listLobbies(words,limit,dataFilter, null, callback);
    }

    public void listLobbies(
        String words,
        int limit,
        @Nullable Map<String, String> dataFilter,
        LobbyCursor cursor,
        BiConsumer<LobbyCursor, Throwable> callback
    ) {
        NostrFilter filter = null;
        if (words != null && !words.isEmpty() && isSearchSupported()) {
            filter = new NostrSearchFilter();
            ((NostrSearchFilter) filter).search(words);
        } else {
            filter = new NostrFilter();
        }
        filter.withKind(KIND);
        filter.withTag("t", gameName + "/" + gameVersion);
        if(cursor!=null) {
            if (cursor.direction() == LobbyCursor.Direction.OLDER && cursor.until() != null) {
                filter.until(cursor.until());
            } else if (cursor.direction() == LobbyCursor.Direction.NEWER && cursor.since() != null) {
                filter.since(cursor.since());
            }
        }

        if (dataFilter != null) {
            // relay side filter for 1 letter tags
            for (Map.Entry<String, String> entry : dataFilter.entrySet()) {
                if (entry.getKey().length() > 1) continue;
                filter.withTag(entry.getKey(), entry.getValue());
            }
        }

        listLobbies(
            filter,
            limit,
            Duration.ofSeconds(3),
            (lobbies, err) -> {
                if (err != null) {
                    this.dispatcher.run(() -> {
                            callback.accept(null, err);
                        });
                    return;
                }
                List<Lobby> filteredLobbies = lobbies
                    .stream()
                    .filter(lobby -> {
                        if(cursor!=null && cursor.get().contains(lobby))return false;
                        if (dataFilter != null) {
                            // client side filter by tags > 1 letter
                            for (Entry<String, String> tagFilter : dataFilter.entrySet()) {
                                String key = tagFilter.getKey();
                                if (key.length() == 1) continue; // 1 letter tags are already filtered

                                String value = lobby.getData(key);
                                if (value == null) return false; // lobby doesn't have this tag -> filter it out

                                // lobby has this tag, but the value is not in the filter -> filter it out
                                if (!value.equals(tagFilter.getValue())) return false;
                            }
                        }

                        if (words != null && !words.isEmpty() && !isSearchSupported()) {
                            // client side filter by words
                            return lobby.matches(words.split("[ ,]+"));
                        }

                        return true;
                    })
                    .collect(Collectors.toList());
                    this.dispatcher.run(() -> {
                        Instant until = null;
                        Instant since = null;
                        for (Lobby l : filteredLobbies) {
                            if (until == null || l.getCreationTime().isBefore(until)) {
                                until = l.getCreationTime();
                            }
                            if (since == null || l.getCreationTime().isAfter(since)) {
                                since = l.getCreationTime();
                            }
                        }
                        LobbyCursor newCursor = new LobbyCursor(
                            cursor != null ? cursor.direction() : LobbyCursor.Direction.OLDER,
                            until,
                            since,
                            filteredLobbies
                        );
                        callback.accept(newCursor, null);
                    });
            }
        );
    }

    protected void lobbyToEvent(Lobby lobby, BiConsumer<SignedNostrEvent, Throwable> callback) {
        UnsignedNostrEvent event = new UnsignedNostrEvent().withKind(KIND);
        event.withContent(lobby.getRawData());
        for (Entry<String, String> entry : lobby.getData().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            event.withTag(key, value);
        }
        event.withExpiration(lobby.getExpiration());
        log.info("Signing lobby event: " + event);
        localSigner
            .sign(event)
            .then(signed -> {
                this.dispatcher.run(() -> {
                        callback.accept(signed, null);
                    });
                return null;
            })
            .catchException(err -> {
                log.log(Level.WARNING, "Failed to sign lobby event: " + err.getMessage(), err);
                this.dispatcher.run(() -> {
                        callback.accept(null, err);
                    });
            });
    }

    public void createLobby(
        String passphrase,
        Map<String, String> data,
        Duration expiration,
        BiConsumer<Lobby, Throwable> callback
    ) {
        if (data.containsKey(Lobby.BANNED_PEERS_DATA_KEY)) {
            dispatcher.run(() -> callback.accept(
                null,
                new IllegalArgumentException("The lobby ban list is engine-managed.")
            ));
            return;
        }
        BiConsumer<NostrPrivateKey, String> create = (newPriv, roomKey) -> {
            String roomId = newPriv.getPublicKey().asBech32();

            Map<String, String> fullData = new HashMap<>();
            fullData.put("roomKey", roomKey);
            fullData.put("t", gameName + "/" + gameVersion);
            fullData.put("d", roomId);
            for (Entry<String, String> entry : data.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                fullData.put(key, value);
            }

            String rawData = NGEUtils.getPlatform().toJSON(fullData);

            NostrPublicKey owner;
            try {
                owner = localSigner.getPublicKey().await();
            } catch (Exception error) {
                dispatcher.run(() -> callback.accept(null, error));
                return;
            }
            LocalLobby lobby = new LocalLobby(
                roomId,
                roomKey,
                rawData,
                Instant.now().plus(expiration),
                Instant.now(),
                owner
            );
            for (Entry<String, String> dataEntry : fullData.entrySet()) {
                String key = dataEntry.getKey();
                String value = dataEntry.getValue();
                lobby.setDataSilent(key, value);
            }
            synchronized (trackedLobbies) {
                if (!trackedLobbies.stream().anyMatch(ref -> ref.get() == lobby)) {
                    trackedLobbies.add(new WeakReference<>(lobby));
                }
            }
            lobbyToEvent(
                lobby,
                (signed, error) -> {
                    if (error != null) {
                        log.log(Level.WARNING, "Failed to create lobby: " + error.getMessage(), error);
                        this.dispatcher.run(() -> {
                                callback.accept(null, error);
                            });
                        return;
                    }
                    log.info("Creating lobby with event " + signed.toMap());
                    AsyncTask.any(masterServersPool
                        .publish(signed))
                        .then(acks -> {
                            this.dispatcher.run(() -> {
                                    callback.accept(lobby, null);
                                });
                            return null;
                        })
                        .catchException(err -> {
                            this.dispatcher.run(() -> {
                                    callback.accept(null, error);
                                });
                        });
                }
            );
        };

        NostrPrivateKey newPriv = NostrPrivateKey.generate();
        if (passphrase != null && !passphrase.isEmpty()) {
            Nip49
                .encrypt(newPriv, passphrase)
                .then(roomKey -> {
                    create.accept(newPriv, roomKey);
                    return null;
                })
                .catchException(err -> {
                    log.log(Level.WARNING, "Failed to encrypt private key: " + err.getMessage(), err);
                    this.dispatcher.run(() -> {
                            callback.accept(null, err);
                        });
                });
        } else {
            create.accept(newPriv, newPriv.asBech32());
        }
    }

    void updateLobby(LocalLobby lobby) {
        synchronized (trackedLobbies) {
            if (!trackedLobbies.stream().anyMatch(ref -> ref.get() == lobby)) {
                trackedLobbies.add(new WeakReference<>(lobby));
            }
        }
        lobbyToEvent(
            lobby,
            (signed, err) -> {
                if (err != null) {
                    log.log(Level.WARNING, "Failed to update lobby: " + err.getMessage(), err);
                    return;
                }
                masterServersPool.send(signed);
            }
        );
    }

    public P2PConnection connectToLobby(Lobby lobby, String passphrase) throws Exception {
        NostrPrivateKey privKey = lobby.getKey(passphrase);
        NostrPublicKey localPeer = localSigner.getPublicKey().await();
        if (lobby.isPeerBanned(localPeer)) {
            throw new IllegalStateException("The local peer is banned from this lobby.");
        }
        synchronized (trackedLobbies) {
            if (!trackedLobbies.stream().anyMatch(ref -> ref.get() == lobby)) {
                trackedLobbies.add(new WeakReference<>(lobby));
            }
        }

        P2PConnection conn = new P2PConnection(
            this.localSigner,
            this.gameName,
            this.gameVersion,
            privKey,
            turnServer,
            this.masterServersPool,
            dispatcher
        );
        conn.setPeerAdmission(peer -> !lobby.isPeerBanned(peer));
        conn.start();
        synchronized (connectedLobbies) {
            connectedLobbies.put(lobby, conn);
        }
        if (lobby instanceof LocalLobby) {
            ((LocalLobby) lobby).setBanKickHandler(conn::disconnectPeer);
        }
        kickBannedPeers(lobby, conn);
        return conn;
    }

    private void refreshConnectedLobbies() {
        ArrayList<Entry<Lobby, P2PConnection>> pending = new ArrayList<>();
        synchronized (connectedLobbies) {
            Iterator<Entry<Lobby, P2PConnection>> iterator = connectedLobbies.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Lobby, P2PConnection> entry = iterator.next();
                Lobby lobby = entry.getKey();
                P2PConnection connection = entry.getValue();
                if (lobby == null || connection == null || !connection.isRunning()) {
                    if (lobby instanceof LocalLobby) {
                        ((LocalLobby) lobby).setBanKickHandler(null);
                    }
                    iterator.remove();
                    refreshInFlight.remove(lobby);
                    continue;
                }
                if (!lobby.isOwnedByLocalPeer() && refreshInFlight.add(lobby)) {
                    pending.add(new java.util.AbstractMap.SimpleImmutableEntry<>(lobby, connection));
                }
            }
        }
        for (Entry<Lobby, P2PConnection> entry : pending) {
            refreshRemoteLobby(entry.getKey(), entry.getValue());
        }
    }

    private void refreshRemoteLobby(Lobby lobby, P2PConnection connection) {
        NostrFilter filter = new NostrFilter()
            .withKind(KIND)
            .withAuthor(lobby.getOwner())
            .withTag("d", lobby.getId())
            .withTag("t", gameName + "/" + gameVersion)
            .limit(1);
        masterServersPool
            .fetch(filter, 1, true, Duration.ofSeconds(3))
            .then(events -> {
                SignedNostrEvent event = newestExpectedLobbyEvent(lobby, events);
                if (event == null) {
                    clearRefreshInFlight(lobby);
                    return null;
                }
                try {
                    String rawData = event.getContent();
                    Map<String, String> content = NGEUtils.getPlatform().fromJSON(rawData, Map.class);
                    if (content == null) {
                        clearRefreshInFlight(lobby);
                        return null;
                    }
                    Map<String, String> snapshot = snapshotData(event, content);
                    dispatcher.run(() -> {
                        try {
                            if (isConnected(lobby, connection)) {
                                lobby.refreshSnapshot(
                                    rawData,
                                    snapshot,
                                    event.getCreatedAt(),
                                    event.getId()
                                );
                                kickBannedPeers(lobby, connection);
                            }
                        } finally {
                            clearRefreshInFlight(lobby);
                        }
                    });
                } catch (Exception error) {
                    clearRefreshInFlight(lobby);
                    log.log(Level.FINE, "Failed to refresh connected lobby metadata", error);
                }
                return null;
            })
            .catchException(error -> {
                clearRefreshInFlight(lobby);
                log.log(Level.FINE, "Failed to refresh connected lobby", error);
            });
    }

    private SignedNostrEvent newestExpectedLobbyEvent(Lobby lobby, List<SignedNostrEvent> events) {
        if (events == null) {
            return null;
        }
        for (SignedNostrEvent event : events) {
            if (event == null || !lobby.isOwner(event.getPubkey())) {
                continue;
            }
            try {
                TagValue room = event.getFirstTag("d");
                TagValue game = event.getFirstTag("t");
                if (room != null && game != null
                        && lobby.getId().equals(room.get(0))
                        && (gameName + "/" + gameVersion).equals(game.get(0))) {
                    return event;
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed lobby revisions.
            }
        }
        return null;
    }

    private static Map<String, String> snapshotData(
            SignedNostrEvent event,
            Map<String, String> content) {
        Map<String, String> snapshot = new HashMap<>();
        for (String tagKey : event.listTagKeys()) {
            if ("expiration".equals(tagKey)) {
                continue;
            }
            TagValue tagValue = event.getFirstTag(tagKey);
            if (tagValue != null) {
                snapshot.put(tagKey, tagValue.get(0));
            }
        }
        snapshot.putAll(content);
        return snapshot;
    }

    private boolean isConnected(Lobby lobby, P2PConnection connection) {
        synchronized (connectedLobbies) {
            return connectedLobbies.get(lobby) == connection && connection.isRunning();
        }
    }

    private void clearRefreshInFlight(Lobby lobby) {
        synchronized (connectedLobbies) {
            refreshInFlight.remove(lobby);
        }
    }

    private void kickBannedPeers(Lobby lobby, P2PConnection connection) {
        try {
            if (lobby.isPeerBanned(localSigner.getPublicKey().await())) {
                connection.close();
                return;
            }
        } catch (Exception error) {
            log.log(Level.FINE, "Failed to resolve the local peer while applying lobby bans", error);
        }
        for (NostrPublicKey peer : lobby.getBannedPeersSnapshot()) {
            connection.disconnectPeer(peer);
        }
    }

    public void setTurnServer(String turnServer) {
        this.turnServer = turnServer;
    }
}
