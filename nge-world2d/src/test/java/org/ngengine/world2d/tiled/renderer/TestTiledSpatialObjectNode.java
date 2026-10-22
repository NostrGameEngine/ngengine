package org.ngengine.world2d.tiled.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.Orientation;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Geometry;

class TestTiledSpatialObjectNode {
    @Test
    void modelSurvivesSpriteStateChangesButNotModelPathChanges() {
        Tile first = new Tile(0, 0, 16, 16);
        first.setId(1);
        first.setGid(1);
        Tile second = new Tile(0, 0, 16, 16);
        second.setId(2);
        second.setGid(2);
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, first);
        object.putProperty("model.path", "Models/Character.gltf");
        TiledSpatialObjectNode node = new TiledSpatialObjectNode(object, "Models/Character.gltf", new Node());
        node.setUserData("ngengine.world2d.gid", first.getGid());
        object.setTile(second);
        assertNotEquals(first.getGid(), second.getGid());
        assertTrue(MapRenderer.filterByShape(object, node));
        object.putProperty("model.path", "Models/Other.gltf");
        assertFalse(MapRenderer.filterByShape(object, node));
        object.putProperty("model.path", null);
        assertFalse(MapRenderer.filterByShape(object, node));
    }

    @Test
    void ordinarySpritesStillRebuildWhenTheirTileChanges() {
        Tile tile = new Tile(0, 0, 16, 16);
        tile.setId(2);
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, tile);
        Geometry sprite = new Geometry();
        sprite.setUserData("ngengine.world2d.gid", 1);
        assertFalse(MapRenderer.filterByShape(object, sprite));
    }

    @Test
    void objectSpatialPropertyDoesNotMountComponent() {
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, 16, 16);
        object.putProperty("model.path", "Models/Test.j3o");

        assertTrue(TiledSpatialObjectNode.hasSpatial(object));
        assertEquals("Models/Test.j3o", TiledSpatialObjectNode.spatialPath(object));
    }

    @Test
    void tileSpatialPropertyIsInheritedByTileObject() {
        Tile tile = new Tile(0, 0, 16, 16);
        tile.putProperty("spatial", "Models/FromTile.j3o");
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, tile);

        assertTrue(TiledSpatialObjectNode.hasSpatial(object));
        assertEquals("Models/FromTile.j3o", TiledSpatialObjectNode.spatialPath(object));
    }

    @Test
    void spatialObjectParentUsesMapOrientationAndAuthoredTransform() {
        TiledMap map = new TiledMap(4, 4);
        map.setOrientation(Orientation.ISOMETRIC);
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, 16, 16);
        object.putProperty("model.path", "Models/Test.j3o");
        object.putProperty("model.offsetX", 1f);
        object.putProperty("model.offsetY", 2f);
        object.putProperty("model.offsetZ", 3f);
        object.putProperty("model.scale", 2f);
        object.putProperty("model.scaleZ", 0.5f);

        TiledSpatialObjectNode node = new TiledSpatialObjectNode(object, "Models/Test.j3o", new Node("model"));
        node.configure(map, 10f, 2f);

        assertEquals(new Vector3f(1f, 2f, 3f), node.getSpatialParent().getLocalTranslation());
        assertEquals(new Vector3f(2f, 2f, 1f), node.getSpatialParent().getLocalScale());
        float[] angles = node.getSpatialParent().getLocalRotation().toAngles(null);
        assertEquals(-FastMath.QUARTER_PI, angles[1], 0.0001f);
    }
}
