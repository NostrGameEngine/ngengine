package org.ngengine.network.components;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.network.Message;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import org.ngengine.network.P2PConnection;
import org.ngengine.network.RemotePeer;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.rtc.NostrRTCRoom;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;

class NetcodeManagerDespawnOrderingTest {
    private static final NostrPublicKey LOCAL = key(1);
    private static final NostrPublicKey OWNER = key(2);
    private static final NostrPublicKey ATTACKER = key(3);
    private static final String COMPONENT = "test/object";
    private static final BigInteger ID = id(0);

    @Test
    void authenticatedRemovalBeforeCreationRejectsTheLateSnapshot() throws Exception {
        Fixture fixture = new Fixture();
        enqueue(fixture.manager, remote(OWNER), removal());
        enqueue(fixture.manager, remote(OWNER), snapshot());
        fixture.manager.updateAppLogic(null, 0f);
        assertEquals(0, fixture.spawns);
        assertTrue(fixture.manager.wasReservedTargetDespawned(ID, COMPONENT, System.nanoTime()));
    }

    @Test
    void unauthorizedRemovalCannotSuppressAnOwnersCreation() throws Exception {
        Fixture fixture = new Fixture();
        enqueue(fixture.manager, remote(ATTACKER), removal());
        enqueue(fixture.manager, remote(OWNER), snapshot());
        fixture.manager.updateAppLogic(null, 0f);
        assertEquals(1, fixture.spawns);
        assertFalse(fixture.manager.wasReservedTargetDespawned(ID, COMPONENT, System.nanoTime()));
    }

    @Test
    void deferredComponentRegistrationStillReceivesTheTerminalRemoval() throws Exception {
        Fixture fixture = new Fixture();
        enqueue(fixture.manager, remote(OWNER), removal());
        fixture.manager.updateAppLogic(null, 0f);
        TestHandler handler = new TestHandler();
        fixture.manager.registerActionHandler(handler);
        fixture.manager.updateAppLogic(null, 0f);
        fixture.manager.updateAppLogic(null, 0f);
        assertEquals(1, fixture.despawns);
    }

    @Test
    void existingHandlerRemovalAlsoRejectsLateSnapshots() throws Exception {
        Fixture fixture = new Fixture();
        fixture.manager.registerActionHandler(new TestHandler());
        enqueue(fixture.manager, remote(OWNER), removal());
        fixture.manager.updateAppLogic(null, 0f);
        enqueue(fixture.manager, remote(OWNER), snapshot());
        fixture.manager.updateAppLogic(null, 0f);
        assertEquals(1, fixture.despawns);
        assertEquals(0, fixture.spawns);
    }

    @Test
    void terminalStateIsBoundedExpiresAndIsComponentSpecific() {
        Fixture fixture = new Fixture();
        long now = System.nanoTime();
        for (int i = 0; i <= 4096; i++) fixture.manager.rememberReservedDespawn(id(i), COMPONENT, now);
        assertFalse(fixture.manager.wasReservedTargetDespawned(ID, COMPONENT, now));
        assertTrue(fixture.manager.wasReservedTargetDespawned(id(4096), COMPONENT, now));
        assertFalse(fixture.manager.wasReservedTargetDespawned(id(4096), "another/component", now));
        assertFalse(fixture.manager.wasReservedTargetDespawned(id(4096), COMPONENT,
            now + Duration.ofSeconds(31).toNanos()));
        String oversizedComponent = "x".repeat(513);
        fixture.manager.rememberReservedDespawn(id(4097), oversizedComponent, now);
        assertFalse(fixture.manager.wasReservedTargetDespawned(id(4097), oversizedComponent, now));
    }

    @Test
    void reusableIdsAndNewSessionsDoNotInheritTerminalState() {
        Fixture fixture = new Fixture();
        long now = System.nanoTime();
        BigInteger persistent = NetcodePartitioning.nextLocalPersistentReservedId(new BigInteger(OWNER.asHex(), 16), 0);
        fixture.manager.rememberReservedDespawn(BigInteger.valueOf(42), COMPONENT, now);
        fixture.manager.rememberReservedDespawn(persistent, COMPONENT, now);
        assertFalse(fixture.manager.wasReservedTargetDespawned(BigInteger.valueOf(42), COMPONENT, now));
        assertFalse(fixture.manager.wasReservedTargetDespawned(persistent, COMPONENT, now));
        fixture.manager.rememberReservedDespawn(ID, COMPONENT, now);
        fixture.manager.disconnectFromLobby();
        assertFalse(fixture.manager.wasReservedTargetDespawned(ID, COMPONENT, now));
    }

    private static final class Fixture implements NetcodeSpawner {
        int spawns;
        int despawns;
        final NetcodeManagerComponent manager = new NetcodeManagerComponent(this) {
            @Override public Set<NostrPublicKey> getKnownPeerPublicKeys() { return Set.of(LOCAL, OWNER, ATTACKER); }
            @Override public NostrPublicKey getLocalPeerPublicKey() { return LOCAL; }
        };
        public NetcodeFragment spawn(NetcodeManagerComponent manager, SnapshotMessage snapshot) {
            spawns++;
            return null;
        }
        public void despawn(NetcodeManagerComponent manager, NetcodeFragment target) {
            despawns++;
            manager.unregisterActionHandler(target);
        }
    }

    private static final class TestHandler implements NetcodeFragment {
        public BigInteger getNetworkId() { return ID; }
        public String getComponentId() { return COMPONENT; }
        public boolean checkAuthority() { return false; }
        public boolean checkAuthority(RemotePeer source) { return OWNER.equals(source.getRemotePeer().getPubkey()); }
        public <T extends SnapshotMessage> T requestSnapshot(RemotePeer target) { return null; }
    }

    private static NetcodeDespawnActionMessage removal() {
        NetcodeDespawnActionMessage message = new NetcodeDespawnActionMessage();
        message.setComponentId(COMPONENT);
        message.setNetworkId(ID);
        return message;
    }

    private static SnapshotMessage snapshot() {
        SnapshotMessage message = new SnapshotMessage() {};
        message.setComponentId(COMPONENT);
        message.setNetworkId(ID);
        return message;
    }

    private static BigInteger id(int sequence) {
        return NetcodePartitioning.nextLocalReservedId(new BigInteger(OWNER.asHex(), 16), sequence);
    }

    private static NostrPublicKey key(int value) {
        return NostrPublicKey.fromHex(String.format("%064x", value));
    }

    private static RemotePeer remote(NostrPublicKey key) throws Exception {
        NostrRTCPeer self = new NostrRTCPeer(LOCAL, "test", "test", "local", LOCAL, null);
        NostrRTCPeer other = new NostrRTCPeer(key, "test", "test", "remote", LOCAL, null);
        AsyncExecutor unused = new AsyncExecutor() {
            public <T> AsyncTask<T> runLater(Callable<T> task, long delay, TimeUnit unit) { throw new AssertionError(); }
            public <T> AsyncTask<T> run(Callable<T> task) { throw new AssertionError(); }
            public void close() {}
        };
        Constructor<RemotePeer> constructor = RemotePeer.class.getDeclaredConstructor(int.class, NostrRTCRoom.class,
            NostrRTCPeer.class, NostrRTCPeer.class, P2PConnection.class, AsyncExecutor.class, LongSupplier.class);
        constructor.setAccessible(true);
        return constructor.newInstance(1, null, self, other, null, unused, (LongSupplier) System::nanoTime);
    }

    @SuppressWarnings("unchecked")
    private static void enqueue(NetcodeManagerComponent manager, RemotePeer source, Message message) throws Exception {
        Class<?> type = Arrays.stream(NetcodeManagerComponent.class.getDeclaredClasses())
            .filter(candidate -> "InboundMessage".equals(candidate.getSimpleName())).findFirst().orElseThrow();
        Constructor<?> constructor = type.getDeclaredConstructor(RemotePeer.class, Message.class);
        constructor.setAccessible(true);
        Field queue = NetcodeManagerComponent.class.getDeclaredField("inboundMessages");
        queue.setAccessible(true);
        ((Queue<Object>) queue.get(manager)).add(constructor.newInstance(source, message));
    }
}
