package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

class TestTiledModelComponent {
    @Test
    public void objectModelPropertyMountsModelComponent() {
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, 16, 16);
        object.putProperty("model.path", "Models/Test.j3o");

        assertNotNull(object.getComponentManager().getComponent(TiledModelComponent.class));
        assertTrue(TiledModelComponent.hasModel(object));
    }

    @Test
    public void objectTileModelPropertyMountsModelComponent() {
        Tile tile = new Tile(0, 0, 16, 16);
        tile.putProperty("spatial", "Models/FromTile.j3o");
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, tile);

        assertNotNull(object.getComponentManager().getComponent(TiledModelComponent.class));
        assertTrue(TiledModelComponent.hasModel(object));
    }

    @Test
    public void tileLayerModelPropertyMountsModelComponent() {
        Tile tile = new Tile(0, 0, 16, 16);
        tile.putProperty("model", "Models/TileLayer.j3o");
        TiledTileEntity entity = new TiledTileEntity(null, tile, 1, 2);

        assertNotNull(entity.getComponentManager().getComponent(TiledModelComponent.class));
        assertTrue(TiledModelComponent.hasModel(entity));
    }

    @Test
    public void missingModelPropertyDoesNotMountModelComponent() {
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, 16, 16);

        assertNull(object.getComponentManager().getComponent(TiledModelComponent.class));
    }
}
