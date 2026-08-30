package org.ngengine.network.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

public class NetcodeManagerOrphanLifecycleTest {
    private static final NostrPublicKey LOCAL = key(1);
    private static final NostrPublicKey OWNER = key(2);
    private static final NostrPublicKey REPLACEMENT = key(3);
    private static final BigInteger RESERVED_ID = NetcodePartitioning.nextLocalReservedId(
        new BigInteger(OWNER.asHex(), 16),
        42L
    );

    private TestManager manager;

    @BeforeEach
    public void setUp() {
        manager = new TestManager();
    }

    @Test
    public void defaultsToOneMinuteGracePeriod() {
        assertEquals(Duration.ofMinutes(1L), NetcodeFragment.DEFAULT_ORPHAN_GRACE_PERIOD);
        assertEquals(Duration.ofMinutes(1L), manager.getNetworkOrphanGracePeriod());
    }

    @Test
    public void managerGracePeriodAppliesWhenHandlerDoesNotOverrideIt() throws Exception {
        TestHandler handler = new TestHandler(null);
        manager.setNetworkOrphanGracePeriod(Duration.ZERO);
        manager.owner = OWNER;
        manager.registerActionHandler(handler);
        manager.updateAppLogic(null, 0f);

        manager.owner = null;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);

        assertEquals(1, handler.orphanCount);
    }

    @Test
    public void notifiesExactlyOnceWhenOwnerDisappears() throws Exception {
        TestHandler handler = new TestHandler(Duration.ZERO);
        manager.owner = OWNER;
        manager.registerActionHandler(handler);
        manager.updateAppLogic(null, 0f);

        manager.owner = null;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);
        manager.updateAppLogic(null, 0f);

        assertEquals(1, handler.orphanCount);
        assertSame(OWNER, handler.context.getPreviousOwner());
        assertEquals(handler.getNetworkId(), handler.context.getNetworkId());
        assertEquals(handler.getComponentId(), handler.context.getComponentId());
        assertTrue(handler.context.isCurrentPeerCleanupCoordinator());
    }

    @Test
    public void directAuthorityReassignmentDoesNotReportAnOrphan() throws Exception {
        TestHandler handler = new TestHandler(Duration.ZERO);
        manager.owner = OWNER;
        manager.registerActionHandler(handler);
        manager.updateAppLogic(null, 0f);

        manager.owner = REPLACEMENT;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);

        assertEquals(0, handler.orphanCount);
    }

    @Test
    public void reassignableIdsNeverEnterOrphanLifecycleWhenOwnerResolutionFails() {
        BigInteger persistentId = NetcodePartitioning.nextLocalPersistentReservedId(
            new BigInteger(OWNER.asHex(), 16),
            7L
        );
        TestHandler shared = new TestHandler(Duration.ZERO, BigInteger.valueOf(42L));
        TestHandler persistent = new TestHandler(Duration.ZERO, persistentId);
        manager.owner = null;
        manager.registerActionHandler(shared);
        manager.registerActionHandler(persistent);

        manager.updateAppLogic(null, 0f);

        assertEquals(0, manager.ownerResolutionCount);
        assertEquals(0, shared.orphanCount);
        assertEquals(0, persistent.orphanCount);
    }

    @Test
    public void restoredOwnerAllowsASecondOrphanEpisode() throws Exception {
        TestHandler handler = new TestHandler(Duration.ZERO);
        manager.owner = OWNER;
        manager.registerActionHandler(handler);
        manager.updateAppLogic(null, 0f);

        manager.owner = null;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);
        manager.owner = REPLACEMENT;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);
        manager.owner = null;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);

        assertEquals(2, handler.orphanCount);
        assertSame(REPLACEMENT, handler.context.getPreviousOwner());
    }

    @Test
    public void restoredOwnerCancelsPendingOrphanCleanup() throws Exception {
        TestHandler handler = new TestHandler(Duration.ofHours(1L));
        manager.owner = OWNER;
        manager.registerActionHandler(handler);
        manager.updateAppLogic(null, 0f);

        manager.owner = null;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);

        manager.owner = OWNER;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);

        assertEquals(0, handler.orphanCount);
    }

    @Test
    public void orphanCallbackMayUnregisterItsHandler() throws Exception {
        TestHandler handler = new TestHandler(Duration.ZERO);
        handler.manager = manager;
        handler.unregisterOnOrphan = true;
        manager.owner = OWNER;
        manager.registerActionHandler(handler);
        manager.updateAppLogic(null, 0f);

        manager.owner = null;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);

        assertEquals(1, handler.orphanCount);
    }

    @Test
    public void inactiveSessionNeverReportsOrphans() throws Exception {
        TestHandler handler = new TestHandler(Duration.ZERO);
        manager.owner = OWNER;
        manager.registerActionHandler(handler);
        manager.updateAppLogic(null, 0f);

        manager.active = false;
        manager.owner = null;
        advanceTopology(manager);
        manager.updateAppLogic(null, 0f);

        assertEquals(0, handler.orphanCount);
        assertNull(handler.context);
    }

    @Test
    public void unchangedTopologyDoesNotRescanStableHandlers() {
        TestHandler handler = new TestHandler(Duration.ZERO);
        manager.owner = OWNER;
        manager.registerActionHandler(handler);

        manager.updateAppLogic(null, 0f);
        manager.updateAppLogic(null, 0f);

        assertEquals(1, manager.ownerResolutionCount);
        assertEquals(0, handler.orphanCount);
    }

    private static void advanceTopology(NetcodeManagerComponent target) throws Exception {
        Field field = NetcodeManagerComponent.class.getDeclaredField("connectedPeerSetVersion");
        field.setAccessible(true);
        field.setLong(target, field.getLong(target) + 1L);
    }

    private static NostrPublicKey key(int value) {
        return NostrPublicKey.fromHex(String.format("%064x", value));
    }

    private static final class TestManager extends NetcodeManagerComponent {
        private final Set<NostrPublicKey> peers = new LinkedHashSet<>();
        private boolean active = true;
        private NostrPublicKey owner;
        private int ownerResolutionCount;

        private TestManager() {
            peers.add(LOCAL);
        }

        @Override
        public boolean isNetworkSessionActive() {
            return active;
        }

        @Override
        public NostrPublicKey getLocalPeerPublicKey() {
            return LOCAL;
        }

        @Override
        public Set<NostrPublicKey> getKnownPeerPublicKeys() {
            return peers;
        }

        @Override
        public NostrPublicKey resolveActiveOwnerPeerPublicKey(BigInteger networkId) {
            ownerResolutionCount++;
            return owner;
        }
    }

    private static final class TestHandler implements NetcodeFragment {
        private final Duration gracePeriod;
        private final BigInteger networkId;
        private int orphanCount;
        private NetcodeOrphanContext context;
        private TestManager manager;
        private boolean unregisterOnOrphan;

        private TestHandler(Duration gracePeriod) {
            this(gracePeriod, RESERVED_ID);
        }

        private TestHandler(Duration gracePeriod, BigInteger networkId) {
            this.gracePeriod = gracePeriod;
            this.networkId = networkId;
        }

        @Override
        public BigInteger getNetworkId() {
            return networkId;
        }

        @Override
        public String getComponentId() {
            return "test/orphan";
        }

        @Override
        public Duration getNetworkOrphanGracePeriod() {
            return gracePeriod;
        }

        @Override
        public void onNetworkOrphaned(NetcodeOrphanContext context) {
            orphanCount++;
            this.context = context;
            if (unregisterOnOrphan) {
                manager.unregisterActionHandler(this);
            }
        }
    }
}
