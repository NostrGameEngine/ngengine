package org.ngengine.network;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import static org.junit.jupiter.api.Assertions.*;

class LocalLobbyPublicationTest {
    private static LocalLobby lobby() {
        return new LocalLobby("room", "key", "{}", Instant.now().plusSeconds(60), Instant.now(),
                NostrPublicKey.fromHex(String.format("%064x", 1)));
    }

    @Test
    void signingOwnsOneImmutableRevisionAndConcurrentEditsNeedAnotherPublication() {
        LocalLobby lobby = lobby();
        lobby.setData(Map.of("name", "original", "players", "2"));
        LocalLobby.Update first = lobby.beginUpdate();
        assertNotNull(first);
        assertNull(lobby.beginUpdate());
        lobby.setData("name", "changed");
        assertEquals("original", first.snapshot.getData("name"));
        assertFalse(lobby.currentUpdate(first));
        assertNull(lobby.handoffUpdate(first, () -> "should not publish"));
        lobby.finishUpdate(first, true);
        LocalLobby.Update second = lobby.beginUpdate();
        assertEquals("changed", second.snapshot.getData("name"));
        assertTrue(lobby.currentUpdate(second));
        assertEquals("published", lobby.handoffUpdate(second, () -> "published"));
        lobby.finishUpdate(first, true);
        assertNull(lobby.beginUpdate(), "an old completion cannot release the new revision");
        lobby.finishUpdate(second, false);
        assertNull(lobby.beginUpdate());
    }

    @Test
    void guardIsCheckedAgainAtHandoffAndCannotRecursivelyAcquireTheLane() {
        LocalLobby lobby = lobby();
        AtomicInteger checks = new AtomicInteger();
        lobby.setPublicationGuard(() -> {
            assertNull(lobby.beginUpdate());
            return checks.incrementAndGet() == 1;
        });
        LocalLobby.Update update = lobby.beginUpdate();
        assertNotNull(update);
        assertNull(lobby.handoffUpdate(update, () -> "should not publish"));
        assertEquals(2, checks.get());
        lobby.finishUpdate(update, true);
        lobby.setPublicationGuard(() -> true);
        assertNotNull(lobby.beginUpdate());
    }

    @Test
    void bulkDataSupportsRemovalButCannotOverwriteTheEngineBanList() {
        LocalLobby lobby = lobby();
        lobby.setData(Map.of("name", "old", "players", "2"));
        Map<String, String> values = new HashMap<>();
        values.put("name", null);
        values.put("players", "3");
        lobby.setData(values);
        LocalLobby.Update update = lobby.beginUpdate();
        assertNull(update.snapshot.getData("name"));
        assertEquals("3", update.snapshot.getData("players"));
        assertThrows(IllegalArgumentException.class,
                () -> lobby.setData(Map.of(Lobby.BANNED_PEERS_DATA_KEY, "untrusted")));
    }

    @Test
    void unchangedBulkDataDoesNotInvalidateTheSignedRevision() {
        LocalLobby lobby = lobby();
        lobby.setData(Map.of("name", "stable"));
        LocalLobby.Update update = lobby.beginUpdate();
        lobby.setData(Map.of("name", "stable"));
        assertTrue(lobby.currentUpdate(update));
        lobby.finishUpdate(update, false);
        assertNull(lobby.beginUpdate());
    }
}
