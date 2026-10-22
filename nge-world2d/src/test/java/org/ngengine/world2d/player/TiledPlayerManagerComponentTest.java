package org.ngengine.world2d.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.math.Vector2f;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.player.Player;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

class TiledPlayerManagerComponentTest {
    @Test
    void occupancyUsesTheSameAnchorAsSpawningRatherThanTheSpriteCenter() throws Exception {
        TiledMap map = new TiledMap(4, 4);
        map.setTileWidth(256);
        map.setTileHeight(128);
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        map.addLayer(layer);
        TiledObjectEntity player = new TiledObjectEntity(1, 100, 100, new Tile(0, 0, 256, 256));
        player.putProperty("net.sync.component", TiledPlayerSyncComponent.class.getName());
        layer.add(player);
        TiledPlayerManagerComponent manager = new TiledPlayerManagerComponent() {
            @Override
            protected TiledObjectEntity createPlayerEntity(Player info, BigInteger id, Vector2f position) {
                return null;
            }
        };
        Method occupied = TiledPlayerManagerComponent.class.getDeclaredMethod(
            "isOccupiedByPlayer", TiledMap.class, Vector2f.class);
        occupied.setAccessible(true);

        assertTrue((Boolean) occupied.invoke(manager, map, new Vector2f(100, 100)));
        assertFalse((Boolean) occupied.invoke(manager, map, new Vector2f(-28, -28)));
    }

    @Test
    void simultaneousOwnersReserveTheSameDistinctSlotsRegardlessOfConnectionOrder() {
        NostrPublicKey first = key(1);
        NostrPublicKey second = key(2);
        NostrPublicKey third = key(3);
        Set<NostrPublicKey> firstView = new LinkedHashSet<>(List.of(third, first, second));
        Set<NostrPublicKey> secondView = new LinkedHashSet<>(List.of(second, third, first));

        for (NostrPublicKey peer : firstView) {
            assertEquals(TiledPlayerManagerComponent.playerSpawnSlot(peer, firstView),
                TiledPlayerManagerComponent.playerSpawnSlot(peer, secondView));
        }
        assertEquals(0, TiledPlayerManagerComponent.playerSpawnSlot(first, firstView));
        assertEquals(1, TiledPlayerManagerComponent.playerSpawnSlot(second, firstView));
        assertEquals(2, TiledPlayerManagerComponent.playerSpawnSlot(third, firstView));
        assertEquals(2, TiledPlayerManagerComponent.playerSpawnSlot(third, Set.of(first, second)));
    }

    @Test
    void singleMarkerSupportsMoreThanThreeNonOverlappingPlayers() {
        List<Vector2f> positions = new ArrayList<>();
        for (int slot = 0; slot < 25; slot++) {
            Vector2f position = new Vector2f(235f, 252f);
            TiledPlayerManagerComponent.offsetPlayerSpawn(position, slot, 72f);
            for (Vector2f existing : positions) {
                assertTrue(existing.distance(position) >= 72f);
            }
            positions.add(position);
        }
        assertEquals(new Vector2f(235f, 252f), positions.get(0));
    }

    private static NostrPublicKey key(int value) {
        return NostrPublicKey.fromHex(String.format("%064x", value));
    }
}
