package org.ngengine.network.components;

import java.math.BigInteger;

import org.ngengine.nostr4j.keypair.NostrPublicKey;

import jakarta.annotation.Nullable;

/**
 * Immutable metadata passed to
 * {@link NetcodeFragment#onNetworkOrphaned(NetcodeOrphanContext)}.
 *
 * <p>An orphan is a fragment with a reserved network ID whose original owner
 * stayed offline for the complete grace period. Reserved IDs are deliberately
 * not reassigned. Instead, every surviving replica receives this context and
 * performs its own local cleanup.</p>
 *
 * <p>The cleanup coordinator is a deterministic peer selected from the peers
 * currently known by each replica. It is not the new owner of the orphaned
 * fragment. It only coordinates optional network-visible effects that must be
 * produced once. Local cleanup must never be conditional on coordinator
 * status.</p>
 */
public final class NetcodeOrphanContext {
    private final BigInteger networkId;
    private final String componentId;
    private final @Nullable NostrPublicKey previousOwner;
    private final @Nullable NostrPublicKey cleanupCoordinator;
    private final @Nullable NostrPublicKey currentPeer;

    NetcodeOrphanContext(
        BigInteger networkId,
        String componentId,
        @Nullable NostrPublicKey previousOwner,
        @Nullable NostrPublicKey cleanupCoordinator,
        @Nullable NostrPublicKey currentPeer
    ) {
        this.networkId = networkId;
        this.componentId = componentId;
        this.previousOwner = previousOwner;
        this.cleanupCoordinator = cleanupCoordinator;
        this.currentPeer = currentPeer;
    }

    /**
     * Returns the reserved network ID of the orphaned fragment.
     */
    public BigInteger getNetworkId() {
        return networkId;
    }

    /**
     * Returns the component ID used to address the orphaned fragment.
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * Returns the disconnected peer that previously owned the fragment, when
     * that peer was observed before it disconnected.
     */
    public @Nullable NostrPublicKey getPreviousOwner() {
        return previousOwner;
    }

    /**
     * Returns the peer selected to produce optional network-visible one-shot
     * cleanup effects. This peer does not become the fragment's owner.
     */
    public @Nullable NostrPublicKey getCleanupCoordinator() {
        return cleanupCoordinator;
    }

    /**
     * Returns whether the current peer is the deterministic cleanup
     * coordinator.
     *
     * <p>Use this only to guard network-visible effects that must be produced
     * once. Do not use it to guard removal or cleanup of the local replica,
     * because every peer must clean its own copy.</p>
     *
     * @return {@code true} when the current peer should produce coordinated
     *         one-shot effects; {@code false} when another peer was selected
     *         or when either peer identity is unavailable
     */
    public boolean isCurrentPeerCleanupCoordinator() {
        return currentPeer != null && currentPeer.equals(cleanupCoordinator);
    }
}
