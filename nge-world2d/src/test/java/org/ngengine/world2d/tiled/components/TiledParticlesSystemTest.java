/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

class TiledParticlesSystemTest {

    @Test
    void particleOpacityBecomesAnObjectRenderProperty() {
        Tile tile = new Tile(0, 0, 32, 32);
        tile.putProperty("particle.opacity", 0.5f);
        TiledObjectEntity particle =
            new TiledObjectEntity(BigInteger.ONE, 0, 0, 32, 32);

        TiledParticlesSystem.applyParticleRenderProperties(tile, particle);

        assertEquals(0.5f, ((Number) particle.getProperty("render.opacity")).floatValue());
    }

    @Test
    void particleOpacityIsClampedToTheMaterialRange() {
        Tile tile = new Tile(0, 0, 32, 32);
        tile.putProperty("particle.opacity", 4f);
        TiledObjectEntity particle =
            new TiledObjectEntity(BigInteger.ONE, 0, 0, 32, 32);

        TiledParticlesSystem.applyParticleRenderProperties(tile, particle);

        assertEquals(1f, ((Number) particle.getProperty("render.opacity")).floatValue());
    }
}
