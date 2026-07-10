package org.ngengine.world2d.tiled.components;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.jme3.math.Vector2f;

import org.junit.Test;
import org.ngengine.world2d.tiled.animation.AnimatedTileControl;
import org.ngengine.world2d.tiled.animation.Frame;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

public class TestTiledParticleComponent {

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
}
