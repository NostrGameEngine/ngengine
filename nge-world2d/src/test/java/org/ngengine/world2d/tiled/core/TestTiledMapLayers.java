/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.world2d.tiled.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

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
}
