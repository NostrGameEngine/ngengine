package org.ngengine.network.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    public void sharedAuthorityConvergesWhenMembershipChangesFromThreeToFourPeers() {
        List<NostrPublicKey> allPeers = peers(4);
        Set<NostrPublicKey> threePeers = new LinkedHashSet<>(allPeers.subList(0, 3));
        Set<NostrPublicKey> fourPeers = new LinkedHashSet<>(allPeers);
        BigInteger sharedId = new BigInteger("484C434F4F5052454C4159", 16);

        NostrPublicKey ownerWithThree = NetcodeAuthorityAssignment.getPeerWithAuthority(
            sharedId,
            threePeers,
            allPeers.get(0)
        );
        NostrPublicKey ownerWithFour = NetcodeAuthorityAssignment.getPeerWithAuthority(
            sharedId,
            fourPeers,
            allPeers.get(0)
        );

        assertNotEquals(ownerWithThree, ownerWithFour);
        for (NostrPublicKey localPerspective : allPeers) {
            assertEquals(
                ownerWithFour,
                NetcodeAuthorityAssignment.getPeerWithAuthority(
                    sharedId,
                    fourPeers,
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

    @Test
    public void threePeerTopologyReassignsOnlySharedAndPersistentIds() {
        List<NostrPublicKey> peers = peers(3);
        Set<NostrPublicKey> allPeers = new LinkedHashSet<>(peers);

        BigInteger sharedId = BigInteger.valueOf(42L);
        NostrPublicKey sharedOwner = NetcodeAuthorityAssignment.getPeerWithAuthority(
            sharedId,
            allPeers,
            peers.get(0)
        );
        Set<NostrPublicKey> sharedRemaining = new LinkedHashSet<>(peers);
        sharedRemaining.remove(sharedOwner);
        NostrPublicKey sharedReplacement = NetcodeAuthorityAssignment.getPeerWithAuthority(
            sharedId,
            sharedRemaining,
            sharedRemaining.iterator().next()
        );
        assertNotNull(sharedReplacement);
        assertNotEquals(sharedOwner, sharedReplacement);
        for (NostrPublicKey localPerspective : sharedRemaining) {
            assertEquals(
                sharedReplacement,
                NetcodeAuthorityAssignment.getPeerWithAuthority(
                    sharedId,
                    sharedRemaining,
                    localPerspective
                )
            );
        }

        NostrPublicKey encodedOwner = peers.get(1);
        BigInteger ownerKey = new BigInteger(encodedOwner.asHex(), 16);
        Set<NostrPublicKey> ownerRemaining = new LinkedHashSet<>(peers);
        ownerRemaining.remove(encodedOwner);
        BigInteger persistentId = NetcodePartitioning.nextLocalPersistentReservedId(ownerKey, 8L);
        NostrPublicKey persistentReplacement = NetcodeAuthorityAssignment.getPeerWithAuthority(
            persistentId,
            ownerRemaining,
            ownerRemaining.iterator().next()
        );
        assertNotNull(persistentReplacement);
        for (NostrPublicKey localPerspective : ownerRemaining) {
            assertEquals(
                persistentReplacement,
                NetcodeAuthorityAssignment.getPeerWithAuthority(
                    persistentId,
                    ownerRemaining,
                    localPerspective
                )
            );
        }

        BigInteger reservedId = NetcodePartitioning.nextLocalReservedId(ownerKey, 9L);
        for (NostrPublicKey localPerspective : ownerRemaining) {
            assertNull(
                NetcodeAuthorityAssignment.getPeerWithAuthority(
                    reservedId,
                    ownerRemaining,
                    localPerspective
                )
            );
        }
    }

    @Test
    public void orphanCleanupCoordinatorIsDeterministicAcrossRemainingPeers() {
        List<NostrPublicKey> peers = peers(4);
        BigInteger networkId = NetcodePartitioning.nextLocalReservedId(
            new BigInteger(peers.get(3).asHex(), 16),
            5L
        );
        Set<NostrPublicKey> remaining = new LinkedHashSet<>(peers.subList(0, 3));
        NostrPublicKey coordinator = NetcodeAuthorityAssignment.getOrphanCleanupCoordinator(
            networkId,
            remaining
        );

        assertTrue(remaining.contains(coordinator));
        for (int i = 0; i < 4; i++) {
            assertEquals(
                coordinator,
                NetcodeAuthorityAssignment.getOrphanCleanupCoordinator(networkId, remaining)
            );
        }
    }

    @Test
    public void preSortedFastPathMatchesGeneralAuthorityResolution() {
        List<NostrPublicKey> sortedPeers = peers(4).stream()
            .sorted((left, right) -> left.asHex().compareTo(right.asHex()))
            .toList();
        NostrPublicKey owner = sortedPeers.get(2);
        BigInteger ownerKey = new BigInteger(owner.asHex(), 16);
        List<BigInteger> networkIds = List.of(
            BigInteger.valueOf(42),
            NetcodePartitioning.nextLocalReservedId(ownerKey, 3L),
            NetcodePartitioning.nextLocalPersistentReservedId(ownerKey, 7L)
        );

        for (BigInteger networkId : networkIds) {
            assertEquals(
                NetcodeAuthorityAssignment.getPeerWithAuthority(
                    networkId,
                    sortedPeers,
                    sortedPeers.get(0)
                ),
                NetcodeAuthorityAssignment.getPeerWithAuthorityFromSortedPeers(
                    networkId,
                    sortedPeers
                )
            );
        }
    }

    private static List<NostrPublicKey> peers(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
            .mapToObj(value -> NostrPublicKey.fromHex(String.format("%064x", value)))
            .toList();
    }
}
