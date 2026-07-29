package org.ngengine.network.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

public class NetcodeAuthorityAssignmentTest {

    @Test
    public void sharedStateHasExactlyOneAuthorityAcrossFourPeers() {
        List<NostrPublicKey> peers = peers(4);
        BigInteger sharedId = BigInteger.valueOf(42);
        NostrPublicKey expected = NetcodeAuthorityAssignment.getPeerWithAuthority(
            sharedId,
            peers,
            peers.get(0)
        );

        long authorities = peers.stream()
            .filter(peer -> NetcodeAuthorityAssignment.hasAuthority(
                peer,
                sharedId,
                new LinkedHashSet<>(peers),
                peer
            ))
            .count();

        assertEquals(1L, authorities);
        assertTrue(peers.contains(expected));
        for (NostrPublicKey localPerspective : peers) {
            assertEquals(
                expected,
                NetcodeAuthorityAssignment.getPeerWithAuthority(
                    sharedId,
                    new LinkedHashSet<>(peers),
                    localPerspective
                )
            );
        }
    }

    @Test
    public void persistentPlayerStateStaysOwnedByItsPlayerAcrossFourPeers() {
        List<NostrPublicKey> peers = peers(4);
        NostrPublicKey playerOwner = peers.get(2);
        BigInteger ownerKey = new BigInteger(playerOwner.asHex(), 16);
        BigInteger playerEntityId = NetcodePartitioning.nextLocalPersistentReservedId(ownerKey, 7L);

        assertEquals(
            playerOwner,
            NetcodeAuthorityAssignment.getPeerWithAuthority(playerEntityId, peers, peers.get(0))
        );
        assertTrue(
            NetcodeAuthorityAssignment.hasAuthority(
                playerOwner,
                playerEntityId,
                new LinkedHashSet<>(peers),
                peers.get(0)
            )
        );
    }

    @Test
    public void persistentSharedStateFailsOverConsistentlyWhenOwnerLeaves() {
        List<NostrPublicKey> peers = peers(4);
        NostrPublicKey originalOwner = peers.get(1);
        BigInteger ownerKey = new BigInteger(originalOwner.asHex(), 16);
        BigInteger persistentId = NetcodePartitioning.nextLocalPersistentReservedId(ownerKey, 11L);
        Set<NostrPublicKey> remaining = new LinkedHashSet<>(peers);
        remaining.remove(originalOwner);

        NostrPublicKey replacement = NetcodeAuthorityAssignment.getPeerWithAuthority(
            persistentId,
            remaining,
            peers.get(0)
        );

        assertTrue(remaining.contains(replacement));
        for (NostrPublicKey localPerspective : remaining) {
            assertEquals(
                replacement,
                NetcodeAuthorityAssignment.getPeerWithAuthority(
                    persistentId,
                    remaining,
                    localPerspective
                )
            );
        }
    }

    @Test
    public void temporaryPlayerOwnedStateDoesNotTransferWhenOwnerLeaves() {
        List<NostrPublicKey> peers = peers(4);
        NostrPublicKey owner = peers.get(3);
        BigInteger ownerKey = new BigInteger(owner.asHex(), 16);
        BigInteger reservedId = NetcodePartitioning.nextLocalReservedId(ownerKey, 3L);
        Set<NostrPublicKey> remaining = new LinkedHashSet<>(peers);
        remaining.remove(owner);

        assertNull(
            NetcodeAuthorityAssignment.getPeerWithAuthority(
                reservedId,
                remaining,
                peers.get(0)
            )
        );
    }

    private static List<NostrPublicKey> peers(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
            .mapToObj(value -> NostrPublicKey.fromHex(String.format("%064x", value)))
            .toList();
    }
}
