package org.ngengine.network.components;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.platform.NGEPlatform;

import jakarta.annotation.Nullable;

/**
 * Shared peer ownership resolver for partitioned network ids.
 *
 * <p>For reserved ids, ownership is read from the encoded owner key.
 * For non-reserved ids, ownership is selected by stable hash partitioning
 * across known peers.
 */
public final class NetcodeAuthorityAssignment {
    private static final Map<NostrPublicKey, BigInteger> PEER_KEY_CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());

    private NetcodeAuthorityAssignment() {}

    /**
     * Returns true when {@code peer} is authoritative for {@code networkId}.
     */
    public static boolean hasAuthority(
        @Nullable NostrPublicKey peer,
        @Nullable BigInteger networkId,
        Set<NostrPublicKey> knownPeerIds,
        @Nullable NostrPublicKey localPeerId
    ) {
        if (peer == null) {
            return false;
        }
        NostrPublicKey owner = getPeerWithAuthority(networkId, knownPeerIds, localPeerId);
        return owner != null && owner.equals(peer);
    }

    /**
     * Returns the peer that currently has authority over {@code networkId}.
     *
     * <p>If the deterministic owner is currently online, it is returned.
     * For shared and persistent ids whose owner is offline, ownership is
     * reassigned without coordination using rendezvous hashing across online peers.
     */
    public static @Nullable NostrPublicKey getPeerWithAuthority(
        @Nullable BigInteger networkId,
        Collection<NostrPublicKey> knownPeerIds,
        @Nullable NostrPublicKey localPeerId
    ) {
        if (networkId == null || networkId.signum() < 0) {
            return null;
        }
        Collection<NostrPublicKey> known = knownPeerIds != null ? knownPeerIds : Collections.emptyList();
        if (known.isEmpty()) {
            return localPeerId;
        }
        NostrPublicKey preferred = resolveDeterministicOwner(networkId, known, localPeerId);
        if (preferred != null && containsPeer(known, preferred)) {
            return preferred;
        }
        // Reserved ids encode creator ownership and should not be failover-reassigned.
        if (NetcodePartitioning.isReservedId(networkId)) {
            return preferred;
        }
        // Shared ids and persistent ids with offline owner use deterministic failover.
        return resolveByRendezvous(networkId, known);
    }

    private static @Nullable NostrPublicKey resolveDeterministicOwner(
        @Nullable BigInteger networkId,
        Collection<NostrPublicKey> knownPeerIds,
        @Nullable NostrPublicKey localPeerId
    ) {
        if (networkId == null || networkId.signum() < 0) {
            return null;
        }
        if (NetcodePartitioning.isReservedId(networkId)) {
            return resolveReservedOwnerPeerId(networkId, knownPeerIds, localPeerId);
        }
        if (NetcodePartitioning.isPersistentId(networkId)) {
            return resolvePersistentOwnerPeerId(networkId, knownPeerIds, localPeerId);
        }
        List<NostrPublicKey> peers = new ArrayList<>(knownPeerIds != null ? knownPeerIds : Collections.emptyList());
        if (localPeerId != null && !peers.contains(localPeerId)) {
            peers.add(localPeerId);
        }
        if (peers.isEmpty()) {
            return localPeerId;
        }
        peers.sort((a, b) -> a.asHex().compareTo(b.asHex()));
        int idx = Math.floorMod(networkId.hashCode(), peers.size());
        return peers.get(idx);
    }

    /**
     * Resolves owner for reserved ids by decoding embedded owner key and matching it to peers.
     */
    private static @Nullable NostrPublicKey resolveReservedOwnerPeerId(
        @Nullable BigInteger networkId,
        Collection<NostrPublicKey> knownPeerIds,
        @Nullable NostrPublicKey localPeerId
    ) {
        if (!NetcodePartitioning.isReservedId(networkId)) {
            return null;
        }
        BigInteger ownerKey = NetcodePartitioning.decodeReservedOwnerKey(networkId);
        if (ownerKey == null) {
            return null;
        }
        return findPeerByKey(knownPeerIds, localPeerId, ownerKey);
    }

    /**
     * Resolves owner for persistent ids by decoding embedded owner key and matching it to peers.
     */
    private static @Nullable NostrPublicKey resolvePersistentOwnerPeerId(
        @Nullable BigInteger networkId,
        Collection<NostrPublicKey> knownPeerIds,
        @Nullable NostrPublicKey localPeerId
    ) {
        if (!NetcodePartitioning.isPersistentId(networkId)) {
            return null;
        }
        BigInteger ownerKey = NetcodePartitioning.decodePersistentOwnerKey(networkId);
        if (ownerKey == null) {
            return null;
        }
        return findPeerByKey(knownPeerIds, localPeerId, ownerKey);
    }

    /**
     * Converts a peer id to a stable key used by partitioning logic.
     */
    private static @Nullable BigInteger peerKey(@Nullable NostrPublicKey peerId) {
        if (peerId == null) {
            return null;
        }
        synchronized (PEER_KEY_CACHE) {
            BigInteger cached = PEER_KEY_CACHE.get(peerId);
            if (cached == null) {
                cached = new BigInteger(peerId.asHex(), 16);
                PEER_KEY_CACHE.put(peerId, cached);
            }
            return cached;
        }
    }

    private static boolean containsPeer(Collection<NostrPublicKey> peers, @Nullable NostrPublicKey target) {
        return target != null && peers != null && peers.contains(target);
    }

    private static @Nullable NostrPublicKey findPeerByKey(
        @Nullable Collection<NostrPublicKey> knownPeerIds,
        @Nullable NostrPublicKey localPeerId,
        BigInteger ownerKey
    ) {
        if (knownPeerIds != null) {
            for (NostrPublicKey peer : knownPeerIds) {
                BigInteger key = peerKey(peer);
                if (key != null && key.equals(ownerKey)) {
                    return peer;
                }
            }
        }
        BigInteger localKey = peerKey(localPeerId);
        return localKey != null && localKey.equals(ownerKey) ? localPeerId : null;
    }

    private static @Nullable NostrPublicKey resolveByRendezvous(BigInteger networkId, Collection<NostrPublicKey> peers) {
        NostrPublicKey best = null;
        BigInteger bestScore = null;
        for (NostrPublicKey peer : peers) {
            if (peer == null) {
                continue;
            }
            BigInteger score = rendezvousScore(networkId, peer);
            if (best == null || score.compareTo(bestScore) > 0) {
                best = peer;
                bestScore = score;
            }
        }
        return best;
    }

    private static BigInteger rendezvousScore(BigInteger networkId, NostrPublicKey peer) {
        byte[] data = (networkId.toString() + "|" + peer.asHex()).getBytes(StandardCharsets.UTF_8);
        byte[] digest = NGEPlatform.get().sha256(data);
        return new BigInteger(1, digest);
    }
}
