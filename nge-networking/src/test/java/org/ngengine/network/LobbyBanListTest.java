package org.ngengine.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

public class LobbyBanListTest {

    private static final NostrPublicKey OWNER = peer(1);
    private static final NostrPublicKey GUEST = peer(2);

    @Test
    public void lobbyExposesTheVerifiedEventOwner() {
        Lobby lobby = remoteLobby();

        assertEquals(OWNER, lobby.getOwner());
        assertTrue(lobby.isOwner(OWNER));
        assertFalse(lobby.isOwner(GUEST));
        assertFalse(lobby.isOwnedByLocalPeer());
    }

    @Test
    public void localBanPersistsAndKicksImmediately() {
        LocalLobby lobby = localLobby();
        AtomicReference<NostrPublicKey> kicked = new AtomicReference<>();
        lobby.setBanKickHandler(kicked::set);

        assertFalse(lobby.banPeer(OWNER));
        assertTrue(lobby.banPeer(GUEST));
        assertEquals(GUEST, kicked.get());
        assertTrue(lobby.isPeerBanned(GUEST));
        assertEquals(GUEST.asHex(), lobby.getData(Lobby.BANNED_PEERS_DATA_KEY));
        assertTrue(lobby.getRawData().contains(GUEST.asHex()));
        assertTrue(lobby.isUpdateNeeded());

        kicked.set(null);
        assertFalse(lobby.banPeer(GUEST));
        assertNull(kicked.get());
    }

    @Test
    public void applicationDataCannotOverwriteTheEngineManagedBanList() {
        LocalLobby lobby = localLobby();

        assertThrows(
            IllegalArgumentException.class,
            () -> lobby.setData(Lobby.BANNED_PEERS_DATA_KEY, GUEST.asHex())
        );
    }

    @Test
    public void localUnbanRemovesThePersistentEntry() {
        LocalLobby lobby = localLobby();
        assertTrue(lobby.banPeer(GUEST));

        assertTrue(lobby.unbanPeer(GUEST));
        assertFalse(lobby.isPeerBanned(GUEST));
        assertNull(lobby.getData(Lobby.BANNED_PEERS_DATA_KEY));
        assertFalse(lobby.unbanPeer(GUEST));
    }

    @Test
    public void refreshedRemoteSnapshotReplacesTheCachedBanList() {
        Lobby lobby = remoteLobby();
        Map<String, String> banned = new HashMap<>();
        banned.put(Lobby.BANNED_PEERS_DATA_KEY, GUEST.asHex());

        lobby.replaceSnapshot("{\"nge.bannedPeers\":\"" + GUEST.asHex() + "\"}", banned);
        assertTrue(lobby.isPeerBanned(GUEST));

        lobby.replaceSnapshot("{}", Map.of());
        assertFalse(lobby.isPeerBanned(GUEST));
    }

    @Test
    public void staleRemoteSnapshotCannotRollBackTheBanList() {
        Instant initial = Instant.parse("2026-08-29T10:00:00Z");
        Lobby lobby = new Lobby(
            "room",
            "key",
            "{}",
            initial.plusSeconds(600),
            initial,
            OWNER
        );
        Map<String, String> banned = Map.of(Lobby.BANNED_PEERS_DATA_KEY, GUEST.asHex());

        assertTrue(lobby.refreshSnapshot("banned", banned, initial.plusSeconds(20), "b"));
        assertTrue(lobby.isPeerBanned(GUEST));
        assertFalse(lobby.refreshSnapshot("stale", Map.of(), initial.plusSeconds(10), "a"));
        assertTrue(lobby.isPeerBanned(GUEST));
        assertTrue(lobby.refreshSnapshot("unbanned", Map.of(), initial.plusSeconds(30), "c"));
        assertFalse(lobby.isPeerBanned(GUEST));
    }

    @Test
    public void malformedRemoteBanEntriesAreIgnored() {
        Lobby lobby = remoteLobby();
        lobby.replaceSnapshot(
            "{\"nge.bannedPeers\":\"invalid\"}",
            Map.of(Lobby.BANNED_PEERS_DATA_KEY, "invalid")
        );

        assertTrue(lobby.getBannedPeersSnapshot().isEmpty());
    }

    private static LocalLobby localLobby() {
        return new LocalLobby(
            "room",
            "key",
            "{}",
            Instant.now().plusSeconds(60),
            Instant.now(),
            OWNER
        );
    }

    private static Lobby remoteLobby() {
        return new Lobby(
            "room",
            "key",
            "{}",
            Instant.now().plusSeconds(60),
            Instant.now(),
            OWNER
        );
    }

    private static NostrPublicKey peer(int value) {
        return NostrPublicKey.fromHex(String.format("%064x", value));
    }
}
