package org.ngengine.player;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.nip01.Nip01UserMetadata;
import org.ngengine.nostr4j.nip24.Nip24ExtraMetadata;

public class PlayerTest {

    @Test
    public void missingGamerTagReturnsNull() {
        Player player = new Player(null, null) {
            private final Nip24ExtraMetadata metadata = new Nip24ExtraMetadata(new Nip01UserMetadata());

            @Override
            public Nip24ExtraMetadata getMetatada() {
                return metadata;
            }
        };

        assertNull(player.getGamerTag());
    }
}
