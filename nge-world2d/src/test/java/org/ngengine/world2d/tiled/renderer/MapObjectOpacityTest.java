/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.world2d.tiled.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

class MapObjectOpacityTest {

    @Test
    void objectOpacityDefaultsToOpaqueAndClampsConfiguredValues() {
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, 1, 1);

        assertEquals(1f, MapRenderer.objectOpacity(object));

        object.putProperty("render.opacity", 0.45f);
        assertEquals(0.45f, MapRenderer.objectOpacity(object));

        object.putProperty("render.opacity", 2f);
        assertEquals(1f, MapRenderer.objectOpacity(object));

        object.putProperty("render.opacity", -1f);
        assertEquals(0f, MapRenderer.objectOpacity(object));
    }
}
