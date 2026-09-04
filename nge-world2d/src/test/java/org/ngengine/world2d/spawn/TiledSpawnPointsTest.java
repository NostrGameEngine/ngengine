package org.ngengine.world2d.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.enums.ObjectShape;

import com.jme3.math.Vector2f;

public class TiledSpawnPointsTest {
    @Test
    public void findsMarkersByCaseInsensitiveTag() {
        TiledMap map = new TiledMap(4, 4);
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        map.addLayer(layer);
        TiledObjectEntity player = marker(1, "Player, coop");
        TiledObjectEntity delivery = marker(2, "delivery_package");
        layer.add(player);
        layer.add(delivery);

        List<TiledSpawnPoint> found = TiledSpawnPoints.find(map, "PLAYER");

        assertEquals(1, found.size());
        assertSame(player, found.get(0).getObject());
        assertTrue(found.get(0).hasTag("coop"));
        assertTrue(TiledSpawnPoints.isSpawnPoint(delivery));
    }

    @Test
    public void deterministicSelectionUsesTheStableKey() {
        TiledMap map = new TiledMap(4, 4);
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        map.addLayer(layer);
        layer.add(marker(1, "player"));
        TiledObjectEntity second = marker(2, "player");
        layer.add(second);

        TiledSpawnPoint selected =
            TiledSpawnPoints.pickDeterministic(map, "player", BigInteger.valueOf(3));

        assertSame(second, selected.getObject());
    }

    @Test
    public void rectangleAreaSamplingStaysInsideItsRotatedBounds() {
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        TiledObjectEntity area = new TiledObjectEntity(1, 10, 20, 30, 40);
        area.setShape(ObjectShape.RECTANGLE);
        area.putProperty(TiledSpawnPoints.TAGS_PROPERTY, "enemy");
        layer.add(area);
        TiledSpawnPoint point = new TiledSpawnPoint(area, layer, "enemy");
        Vector2f sampled = new Vector2f();

        point.samplePosition(new Random(42L), null, sampled);

        assertTrue(sampled.x >= 10f && sampled.x <= 40f);
        assertTrue(sampled.y >= 20f && sampled.y <= 60f);
    }

    @Test
    public void polygonAreaSamplingUsesItsLocalPoints() {
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        TiledObjectEntity area = new TiledObjectEntity(1, 10, 20, 0, 0);
        area.setShape(ObjectShape.POLYGON);
        area.setPoints(List.of(
            new Vector2f(0f, 0f),
            new Vector2f(20f, 0f),
            new Vector2f(0f, 20f)
        ));
        layer.add(area);
        TiledSpawnPoint point = new TiledSpawnPoint(area, layer, "enemy");
        Vector2f sampled = new Vector2f();

        point.samplePosition(new Random(7L), null, sampled);

        assertTrue(sampled.x >= 10f && sampled.x <= 30f);
        assertTrue(sampled.y >= 20f && sampled.y <= 40f);
        assertTrue((sampled.x - 10f) + (sampled.y - 20f) <= 20f);
    }

    private static TiledObjectEntity marker(int id, String tags) {
        TiledObjectEntity marker = new TiledObjectEntity(id, id * 10, id * 20, 0, 0);
        marker.setShape(ObjectShape.POINT);
        marker.putProperty(TiledSpawnPoints.TAGS_PROPERTY, tags);
        return marker;
    }
}
