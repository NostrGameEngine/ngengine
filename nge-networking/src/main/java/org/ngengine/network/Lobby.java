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

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.nip49.Nip49;
import org.ngengine.nostr4j.nip49.Nip49FailedException;

public class Lobby implements Cloneable, Serializable {
    static final String BANNED_PEERS_DATA_KEY = "nge.bannedPeers";

    protected final String id;
    protected final String key;
    private final NostrPublicKey owner;
    protected String roomRawData; // used for filtering
    protected final Map<String, String> data = new HashMap<>();
    private final Set<NostrPublicKey> bannedPeers = new CopyOnWriteArraySet<>();
    protected final Instant expiration;
    protected final Instant creationTime;
    private volatile Instant latestSnapshotTime;
    private volatile String latestSnapshotId;

    Lobby(
            String roomId,
            String roomKey,
            String roomRawData,
            Instant expiration,
            Instant creationTime,
            NostrPublicKey owner) {
        this.key = Objects.requireNonNull(roomKey);
        this.roomRawData = Objects.requireNonNull(roomRawData);
        this.id = roomId;
        this.expiration = expiration;
        this.creationTime = creationTime;
        this.latestSnapshotTime = creationTime;
        this.owner = Objects.requireNonNull(owner);
    }
    
    public Instant getCreationTime() {
        return creationTime;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public String getId() {
        return id;
    }

    /** Returns the public key that signed and owns this lobby. */
    public NostrPublicKey getOwner() {
        return owner;
    }

    /** Returns whether the supplied public key owns this lobby. */
    public boolean isOwner(NostrPublicKey peer) {
        return owner.equals(peer);
    }

    public boolean isOwnedByLocalPeer() {
        return false;
    }

    public NostrPrivateKey getKey(String passphrase) throws Nip49FailedException {
        if (Nip49.isEncrypted(key)) {
            return Nip49.decryptSync(key, passphrase);
        }
        return NostrPrivateKey.fromBech32(key);
    }

    public NostrPrivateKey getKey() {
        if (Nip49.isEncrypted(key)) {
            throw new IllegalArgumentException("Key is encrypted, please provide a passphrase");
        }
        return NostrPrivateKey.fromBech32(key);
    }

    public boolean matches(String words[]) {
        if (words == null) return true;
        for (String word : words) {
            if (roomRawData.contains(word)) return true;
        }
        return false;
    }

    @Override
    public Lobby clone() {
        try {
            return (Lobby) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setData(String key, String value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
        if (BANNED_PEERS_DATA_KEY.equals(key)) {
            loadBannedPeers(value);
        }
    }

    public String getData(String key) {
        return data.get(key);
    }

    public String getDataOrDefault(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public Collection<String> getDataKeys() {
        return data.keySet();
    }

    protected Map<String, String> getData() {
        return data;
    }

    protected String getRawData() {
        return roomRawData;
    }

    protected void setRawData(String rawData) {
        this.roomRawData = rawData;
    }

    final boolean isPeerBanned(NostrPublicKey peer) {
        return peer != null && bannedPeers.contains(peer);
    }

    final Collection<NostrPublicKey> getBannedPeersSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(bannedPeers));
    }

    protected final boolean addBannedPeer(NostrPublicKey peer) {
        return peer != null && bannedPeers.add(peer);
    }

    protected final boolean removeBannedPeer(NostrPublicKey peer) {
        return peer != null && bannedPeers.remove(peer);
    }

    protected final String serializeBannedPeers() {
        ArrayList<String> encoded = new ArrayList<>(bannedPeers.size());
        for (NostrPublicKey peer : bannedPeers) {
            encoded.add(peer.asHex());
        }
        Collections.sort(encoded);
        return String.join(",", encoded);
    }

    final void replaceSnapshot(String rawData, Map<String, String> nextData) {
        roomRawData = Objects.requireNonNull(rawData);
        data.clear();
        data.putAll(nextData);
        loadBannedPeers(data.get(BANNED_PEERS_DATA_KEY));
    }

    final boolean refreshSnapshot(
            String rawData,
            Map<String, String> nextData,
            Instant snapshotTime,
            String snapshotId) {
        if (!isNewerSnapshot(snapshotTime, snapshotId)) {
            return false;
        }
        replaceSnapshot(rawData, nextData);
        latestSnapshotTime = snapshotTime;
        latestSnapshotId = snapshotId;
        return true;
    }

    final void recordSnapshotRevision(Instant snapshotTime, String snapshotId) {
        latestSnapshotTime = snapshotTime;
        latestSnapshotId = snapshotId;
    }

    private boolean isNewerSnapshot(Instant snapshotTime, String snapshotId) {
        if (snapshotTime == null) {
            return false;
        }
        int timeOrder = snapshotTime.compareTo(latestSnapshotTime);
        if (timeOrder != 0) {
            return timeOrder > 0;
        }
        if (latestSnapshotId == null) {
            return snapshotId != null;
        }
        return snapshotId != null
            && !snapshotId.equals(latestSnapshotId)
            && snapshotId.compareTo(latestSnapshotId) < 0;
    }

    private void loadBannedPeers(String encoded) {
        bannedPeers.clear();
        if (encoded == null || encoded.trim().isEmpty()) {
            return;
        }
        String[] peers = encoded.split(",");
        for (String peer : peers) {
            String normalized = peer.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            try {
                bannedPeers.add(NostrPublicKey.fromHex(normalized));
            } catch (RuntimeException ignored) {
                // Ignore malformed entries received from remote lobby metadata.
            }
        }
    }

    // }

    @Override
    public String toString() {
        return (
            "Lobby ["+
            "roomId=" +
            id +
            ", roomKey=" +
            key +
            ", roomRawData=" +
            roomRawData +
            ", data=" +
            data +
            ", expiration=" +
            expiration.getEpochSecond() +
            "]"
        );
    }

    public boolean isLocked() {
        return Nip49.isEncrypted(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lobby lobby = (Lobby) o;
        return Objects.equals(id, lobby.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
