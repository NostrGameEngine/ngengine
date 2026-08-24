/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.world2d.tiled.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.animation.Frame;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

public class TiledEntityRenderedTileTest {

    @Test
    public void animationChangesOnlyTheRenderedTile() {
        Tileset tileset = new Tileset(16, 16, 0, 0);
        Tile firstFrame = tile(0, 0);
        Tile logicalTile = tile(1, 16);
        logicalTile.putProperty("role", "logical");
        firstFrame.putProperty("role", "frame");
        logicalTile.addAnimation("Default", Arrays.asList(
                new Frame(0, 100), new Frame(1, 100)));
        tileset.addTile(firstFrame);
        tileset.addTile(logicalTile);
        TiledObjectEntity entity = new TiledObjectEntity(1, 0, 0, logicalTile);
        int logicalUpdateId = entity.getUpdateId();
        int logicalGid = entity.getGid();

        entity.updateTileAnimation(0f);

        assertSame(logicalTile, entity.getTile());
        assertSame(firstFrame, entity.getRenderedTile());
        assertEquals(logicalGid, entity.getGid());
        assertEquals(logicalUpdateId, entity.getUpdateId());
        assertEquals("logical", entity.getProperty("role"));

        int firstRenderedUpdateId = entity.getRenderedTileUpdateId();
        entity.updateTileAnimation(0.101f);

        assertSame(logicalTile, entity.getTile());
        assertSame(logicalTile, entity.getRenderedTile());
        assertEquals(logicalUpdateId, entity.getUpdateId());
        assertNotEquals(firstRenderedUpdateId, entity.getRenderedTileUpdateId());
    }

    @Test
    public void logicalTileSwapClearsTheVisualOverride() {
        Tile logicalTile = tile(0, 0);
        Tile visualTile = tile(1, 16);
        Tile replacement = tile(2, 32);
        TiledObjectEntity entity = new TiledObjectEntity(1, 0, 0, logicalTile);
        entity.setRenderedTile(visualTile);

        entity.setTile(replacement);

        assertSame(replacement, entity.getTile());
        assertSame(replacement, entity.getRenderedTile());
    }

    @Test
    public void explicitVisualOverrideSurvivesAutomaticAnimationUpdate() {
        Tile logicalTile = tile(0, 0);
        Tile visualTile = tile(1, 16);
        TiledObjectEntity entity = new TiledObjectEntity(1, 0, 0, logicalTile);
        entity.setRenderedTile(visualTile);

        entity.updateTileAnimation(1f);

        assertSame(logicalTile, entity.getTile());
        assertSame(visualTile, entity.getRenderedTile());
    }

    private static Tile tile(int id, int x) {
        Tile tile = new Tile(x, 0, 16, 16);
        tile.setId(id);
        return tile;
    }
}
