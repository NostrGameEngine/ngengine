package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.network.RemotePeer;
import org.ngengine.network.components.ActionMessage;

class TiledObjectSyncCleanupTest {
    @Test
    void localOnlyObjectsNeverBroadcastTheirNegativeIds() {
        TestSync sync = new TestSync(BigInteger.valueOf(-1));
        sync.onTiledEntityCleanup(null, null);
        assertEquals(0, sync.removals);
    }

    @Test
    void authoritativeNetworkObjectsBroadcastOneTerminalRemoval() {
        TestSync sync = new TestSync(BigInteger.valueOf(42));
        sync.onTiledEntityCleanup(null, null);
        sync.onTiledEntityCleanup(null, null);
        assertEquals(1, sync.removals);
    }

    private static final class TestSync extends TiledObjectSyncComponent {
        private final BigInteger id;
        private int removals;

        TestSync(BigInteger id) { this.id = id; }
        @Override public BigInteger getNetworkId() { return id; }
        @Override public String getComponentId() { return "test/object"; }
        @Override public boolean checkAuthority() { return true; }
        @Override public void invokeAction(RemotePeer target, ActionMessage message) { removals++; }
    }
}
