package org.ngengine.world2d.tiled.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.animation.Frame;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.Orientation;

import com.jme3.math.Vector2f;

public class TiledAnchorResolverTest {
    @Test
    public void markerTakesPriorityOverPhysicalAndGeometricCenters() {
        Fixture fixture = fixture();
        TiledObjectEntity marker = new TiledObjectEntity(-2, 44, 60, 8, 10);
        marker.putProperty("anchor", "heat");
        fixture.collisions.add(marker);
        Vector2f expected = new Vector2f();
        fixture.coordinates.getTileObjectCenterInGridSpace(fixture.source, fixture.tile, marker, expected);

        Vector2f actual = new Vector2f();
        TiledAnchorResolver.resolve(
            fixture.source, fixture.tile, "anchor", "heat", fixture.coordinates, actual);

        assertVectorEquals(expected, actual);
        assertSame(marker, TiledAnchorResolver.findMarker(fixture.tile, "anchor", "heat"));
    }

    @Test
    public void physicalFallbackUsesTheUnionOfEnabledCollisionBounds() {
        Fixture fixture = fixture();
        fixture.collisions.add(new TiledObjectEntity(-2, 90, 160, 60, 50));
        fixture.collisions.add(new TiledObjectEntity(-3, 130, 190, 50, 45));
        TiledObjectEntity ignored = new TiledObjectEntity(-4, 0, 0, 256, 20);
        ignored.putProperty("physics", false);
        fixture.collisions.add(ignored);

        TiledObjectEntity union = new TiledObjectEntity(-5, 90, 160, 90, 75);
        Vector2f expected = new Vector2f();
        fixture.coordinates.getTileObjectCenterInGridSpace(fixture.source, fixture.tile, union, expected);

        Vector2f actual = new Vector2f();
        TiledAnchorResolver.resolve(
            fixture.source, fixture.tile, "anchor", "missing", fixture.coordinates, actual);

        assertVectorEquals(expected, actual);
    }

    @Test
    public void geometricCenterIsUsedWhenTheTileHasNoPhysicalShapes() {
        Fixture fixture = fixture();
        TiledObjectEntity ignored = new TiledObjectEntity(-2, 90, 160, 60, 50);
        ignored.putProperty("physics", false);
        fixture.collisions.add(ignored);
        Vector2f expected = new Vector2f();
        fixture.coordinates.getCenterInGridSpace(fixture.source, expected);

        Vector2f actual = new Vector2f();
        TiledAnchorResolver.resolve(
            fixture.source, fixture.tile, "anchor", "missing", fixture.coordinates, actual);

        assertVectorEquals(expected, actual);
    }

    @Test
    public void animatedTileUsesPhysicalBoundsFromOneOfItsFrames() {
        Fixture fixture = fixture();
        Tile physicalFrame = new Tile(0, 0, 256, 256);
        TiledObjectLayer frameCollisions = new TiledObjectLayer();
        physicalFrame.setCollisions(frameCollisions);
        frameCollisions.add(new TiledObjectEntity(-2, 80, 170, 100, 60));
        Tileset tileset = new Tileset();
        tileset.addTile(physicalFrame);
        tileset.addTile(fixture.tile);
        fixture.tile.addAnimation(List.of(new Frame(physicalFrame.getId(), 100)));

        TiledObjectEntity bounds = new TiledObjectEntity(-3, 80, 170, 100, 60);
        Vector2f expected = new Vector2f();
        fixture.coordinates.getTileObjectCenterInGridSpace(
            fixture.source, physicalFrame, bounds, expected);

        Vector2f actual = new Vector2f();
        TiledAnchorResolver.resolve(
            fixture.source, fixture.tile, "anchor", "missing", fixture.coordinates, actual);

        assertVectorEquals(expected, actual);
    }

    private static Fixture fixture() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ISOMETRIC);
        map.setTileWidth(128);
        map.setTileHeight(64);
        Tile tile = new Tile(0, 0, 256, 256);
        TiledObjectLayer collisions = new TiledObjectLayer();
        tile.setCollisions(collisions);
        TiledObjectEntity source = new TiledObjectEntity(-1, 320, 420, 256, 256);
        source.setTile(tile);
        return new Fixture(tile, collisions, source, TiledCoordinateSystem.create(map, 64));
    }

    private static void assertVectorEquals(Vector2f expected, Vector2f actual) {
        assertEquals(expected.x, actual.x, 0.001f);
        assertEquals(expected.y, actual.y, 0.001f);
    }

    private record Fixture(
        Tile tile,
        TiledObjectLayer collisions,
        TiledObjectEntity source,
        TiledCoordinateSystem coordinates
    ) {
    }
}
