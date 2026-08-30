package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.jme3.math.Vector2f;

import org.junit.jupiter.api.Test;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponentManager;
import org.ngengine.world2d.tiled.animation.AnimatedTileControl;
import org.ngengine.world2d.tiled.animation.Frame;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

public class TiledParticleComponentTest {

    @Test
    public void orphanedParticleRemovesItsLocalReplica() {
        TiledMap map = new TiledMap(4, 4);
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        map.addLayer(layer);
        TiledObjectEntity particle = new TiledObjectEntity(1, 0, 0, 32, 32);
        layer.add(particle);
        Components.mount(particle, new TiledParticleComponent());
        particle.getComponentManager().setParent(new TestComponentManager());
        particle.getComponentManager().update(null, map, layer, particle, 0f);
        TiledParticleComponent component = particle.getComponentManager().getComponent(
            TiledParticleComponent.class
        );

        component.onNetworkOrphaned(null);

        assertFalse(layer.getObjects().contains(particle));
        assertNull(particle.getObjectGroup());
    }

    @Test
    public void followCanBeConfiguredBeforeComponentIsAttached() {
        TiledParticleComponent component = new TiledParticleComponent();
        TiledObjectEntity target = new TiledObjectEntity(-1, 10, 20, 32, 32);

        assertSame(component, component.follow(target, 16f, 0f));
    }

    @Test
    public void emitterLookupSelectsTheNamedTileMarker() {
        Tile tile = new Tile(0, 0, 256, 256);
        TiledObjectLayer markers = new TiledObjectLayer();
        TiledObjectEntity spray = marker(180, 90, "spray");
        TiledObjectEntity leak = marker(150, 120, "leak");
        markers.add(spray);
        markers.add(leak);
        tile.setCollisions(markers);

        assertSame(spray, TiledParticleEmitter.findEmitter(tile, "spray"));
        assertSame(leak, TiledParticleEmitter.findEmitter(tile, "leak"));
        assertNull(TiledParticleEmitter.findEmitter(tile, "missing"));
    }

    @Test
    public void particleOriginAlignsAnArbitraryTilePointWithTheEmitter() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ORTHOGONAL);
        map.setTileWidth(64);
        map.setTileHeight(64);
        TiledCoordinateSystem coordinates = TiledCoordinateSystem.create(map, 64);

        Tileset tileset = new Tileset(100, 100, 0, 0);
        tileset.setObjectAlignment("left");
        Tile tile = new Tile(0, 0, 100, 100);
        tile.setId(0);
        TiledObjectLayer markers = new TiledObjectLayer();
        TiledObjectEntity origin = new TiledObjectEntity(-1, 20, 50, 0, 0);
        origin.putProperty(TiledParticleOrigin.PROPERTY_ORIGIN, true);
        markers.add(origin);
        tile.setCollisions(markers);
        tileset.addTile(tile);

        TiledObjectEntity particle = new TiledObjectEntity(-2, 0, 0, 100, 100);
        particle.setTile(tile);
        Vector2f anchor = new Vector2f(200, 300);

        assertTrue(TiledParticleOrigin.alignToGridAnchor(
            particle, anchor, coordinates, Orientation.ORTHOGONAL));
        assertEquals(180f, particle.getX(), 0.001f);
        assertEquals(300f, particle.getY(), 0.001f);
        Vector2f resolvedAnchor = new Vector2f();
        assertTrue(TiledParticleOrigin.getGridAnchor(
            particle, coordinates, Orientation.ORTHOGONAL, resolvedAnchor));
        assertEquals(anchor.x, resolvedAnchor.x, 0.001f);
        assertEquals(anchor.y, resolvedAnchor.y, 0.001f);

        particle.setRotation(90d);
        assertTrue(TiledParticleOrigin.alignToGridAnchor(
            particle, anchor, coordinates, Orientation.ORTHOGONAL));
        assertEquals(200f, particle.getX(), 0.001f);
        assertEquals(280f, particle.getY(), 0.001f);
        assertTrue(TiledParticleOrigin.getGridAnchor(
            particle, coordinates, Orientation.ORTHOGONAL, resolvedAnchor));
        assertEquals(anchor.x, resolvedAnchor.x, 0.001f);
        assertEquals(anchor.y, resolvedAnchor.y, 0.001f);
    }

    @Test
    public void onlyIfEmptyFindsParticleWhoseCustomOriginLiesOnExcludedBottomEdge() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ORTHOGONAL);
        map.setTileWidth(64);
        map.setTileHeight(64);
        TiledCoordinateSystem coordinates = TiledCoordinateSystem.create(map, 64);

        Tileset tileset = new Tileset(100, 100, 0, 0);
        tileset.setObjectAlignment("left");
        Tile tile = new Tile(0, 0, 100, 100);
        TiledObjectLayer markers = new TiledObjectLayer();
        TiledObjectEntity origin = new TiledObjectEntity(-1, 50, 100, 0, 0);
        origin.putProperty(TiledParticleOrigin.PROPERTY_ORIGIN, true);
        markers.add(origin);
        tile.setCollisions(markers);
        tileset.addTile(tile);

        Vector2f anchor = new Vector2f(200, 300);
        TiledObjectEntity particle = new TiledObjectEntity(-2, 0, 0, 100, 100);
        particle.setTile(tile);
        assertTrue(TiledParticleOrigin.alignToGridAnchor(
            particle, anchor, coordinates, Orientation.ORTHOGONAL));
        TiledObjectLayer layer = new TiledObjectLayer();
        layer.getObjects().add(particle);

        assertTrue(new TiledParticlesSystem().hasParticleAt(
            layer, List.of(tile), anchor, coordinates, Orientation.ORTHOGONAL));
    }

    @Test
    public void particleOriginFallsBackWhenNoMarkerIsDeclared() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ORTHOGONAL);
        map.setTileWidth(64);
        map.setTileHeight(64);
        TiledObjectEntity particle = new TiledObjectEntity(-2, 0, 0, new Tile(0, 0, 100, 100));

        assertFalse(TiledParticleOrigin.alignToGridAnchor(
            particle, new Vector2f(200, 300), TiledCoordinateSystem.create(map, 64), Orientation.ORTHOGONAL));
        assertEquals(0f, particle.getX(), 0.001f);
        assertEquals(0f, particle.getY(), 0.001f);
    }

    @Test
    public void animatedTileControlExposesTheCurrentFrameTile() {
        Tileset tileset = new Tileset(256, 256, 0, 0);
        Tile frame = new Tile(0, 0, 256, 256);
        frame.setId(0);
        tileset.addTile(frame);
        Tile animated = new Tile(256, 0, 256, 256);
        animated.setId(1);
        animated.addAnimation("Default", java.util.List.of(new Frame(0, 100)));
        tileset.addTile(animated);

        assertSame(frame, new AnimatedTileControl(animated).getCurrentTile());
    }

    @Test
    public void emitterCoordinatesUseTheDisplayedFrameDimensions() {
        TiledMap map = new TiledMap(10, 10);
        map.setOrientation(Orientation.ORTHOGONAL);
        map.setTileWidth(64);
        map.setTileHeight(64);
        TiledCoordinateSystem coordinates = TiledCoordinateSystem.create(map, 64);

        Tile sourceTile = new Tile(0, 0, 128, 128);
        Tile displayedFrame = new Tile(0, 0, 256, 256);
        TiledObjectEntity source = new TiledObjectEntity(-1, 10, 20, 256, 256);
        source.setTile(sourceTile);
        TiledObjectEntity emitter = marker(128, 128, "spray");
        Vector2f position = new Vector2f();

        coordinates.getTileObjectCenterInGridSpace(source, displayedFrame, emitter, position);

        assertEquals(138f, position.x, 0.001f);
        assertEquals(128f, position.y, 0.001f);
    }

    private TiledObjectEntity marker(float x, float y, String emitterId) {
        TiledObjectEntity marker = new TiledObjectEntity(-1, x, y, 0, 0);
        marker.putProperty(TiledParticleEmitter.PROPERTY_EMITTER, emitterId);
        return marker;
    }

    private static final class TestComponentManager extends AbstractComponentManager {
    }
}
