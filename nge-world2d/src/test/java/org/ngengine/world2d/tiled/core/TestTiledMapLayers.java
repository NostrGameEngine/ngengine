/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.world2d.tiled.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

class TestTiledMapLayers {

    @Test
    void replacingLayerRemovesOldNameLookup() {
        TiledMap map = new TiledMap(10, 10);
        TiledObjectLayer oldLayer = new TiledObjectLayer();
        oldLayer.setName("old");
        TiledObjectLayer newLayer = new TiledObjectLayer();
        newLayer.setName("new");
        map.addLayer(oldLayer);

        map.setLayer(0, newLayer);

        assertNull(map.getLayer("old"));
        assertSame(newLayer, map.getLayer("new"));
        assertSame(newLayer, map.getLayer(0));
    }

    @Test
    void removingForeignObjectDoesNotDetachItFromItsLayer() {
        TiledObjectLayer owner = new TiledObjectLayer();
        TiledObjectLayer other = new TiledObjectLayer();
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, 1, 1);
        owner.add(object);

        other.remove(object);

        assertSame(owner, object.getObjectGroup());
        assertSame(object, owner.getObjects().get(0));
    }

    @Test
    void settingLayersRebuildsOwnershipAndIndexes() {
        TiledMap map = new TiledMap(10, 10);
        TiledObjectLayer first = new TiledObjectLayer();
        first.setName("first");
        TiledObjectLayer second = new TiledObjectLayer();
        second.setName("second");

        map.setLayers(Arrays.asList(first, second));

        assertSame(map, first.getMap());
        assertSame(map, second.getMap());
        assertSame(first, map.getLayer("first"));
        assertSame(second, map.getLayer("second"));
        assertEquals(0, first.getIndex());
        assertEquals(1, second.getIndex());
    }
}
