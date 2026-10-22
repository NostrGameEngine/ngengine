package org.ngengine.network.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NetcodeDespawnActionMessageTest {
    @Test
    public void despawnMessagesAreReliableByDefault() {
        assertTrue(new NetcodeDespawnActionMessage().isReliable());
        org.junit.jupiter.api.Assertions.assertEquals(2, new NetcodeDespawnActionMessage().getChannel());
    }
}
