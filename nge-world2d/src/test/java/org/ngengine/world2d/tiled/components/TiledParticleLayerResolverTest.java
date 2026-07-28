package org.ngengine.world2d.tiled.components;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

import static org.junit.jupiter.api.Assertions.assertSame;

class TiledParticleLayerResolverTest {

    @Test
    void emitterMarkerLayerOverrideTakesPriorityOverOwnerLayer() {
        Fixture fixture = fixture();
        TiledObjectEntity source = fixture.source(fixture.objects);
        TiledObjectEntity marker = emitterMarker("spray");
        marker.putProperty(TiledParticleLayerResolver.PROPERTY_LAYER, "effects");
        source.getTile().getCollisions().add(marker);

        assertSame(
            fixture.effects,
            TiledParticleLayerResolver.resolve(source, "spray", fixture.map)
        );
    }

    @Test
    void objectEmitterUsesItsOwnLayerByDefault() {
        Fixture fixture = fixture();
        TiledObjectEntity source = fixture.source(fixture.effects);
        source.getTile().getCollisions().add(emitterMarker("spray"));

        assertSame(
            fixture.effects,
            TiledParticleLayerResolver.resolve(source, "spray", fixture.map)
        );
    }

    @Test
    void layerMountedEmitterUsesThatObjectLayer() {
        Fixture fixture = fixture();

        assertSame(
            fixture.effects,
            TiledParticleLayerResolver.resolve(fixture.effects, null, fixture.map)
        );
    }

    @Test
    void mapMountedEmitterPrefersObjectsThenLegacyParticles() {
        Fixture fixture = fixture();

        assertSame(
            fixture.objects,
            TiledParticleLayerResolver.resolve(fixture.map, null, fixture.map)
        );

        fixture.map.removeLayer(fixture.map.getLayers().indexOf(fixture.objects));
        assertSame(
            fixture.particles,
            TiledParticleLayerResolver.resolve(fixture.map, null, fixture.map)
        );
    }

    @Test
    void customFallbackOrderCanBeProvided() {
        Fixture fixture = fixture();

        assertSame(
            fixture.effects,
            TiledParticleLayerResolver.resolve(
                fixture.map,
                null,
                fixture.map,
                "effects",
                "objects"
            )
        );
    }

    private static TiledObjectEntity emitterMarker(String id) {
        TiledObjectEntity marker = new TiledObjectEntity(-2, 10, 10, 0, 0);
        marker.putProperty(TiledParticleEmitter.PROPERTY_EMITTER, id);
        return marker;
    }

    private static Fixture fixture() {
        TiledMap map = new TiledMap(10, 10);
        TiledObjectLayer particles = layer("particles");
        TiledObjectLayer objects = layer("objects");
        TiledObjectLayer effects = layer("effects");
        map.addLayer(particles);
        map.addLayer(objects);
        map.addLayer(effects);
        return new Fixture(map, particles, objects, effects);
    }

    private static TiledObjectLayer layer(String name) {
        TiledObjectLayer layer = new TiledObjectLayer();
        layer.setName(name);
        return layer;
    }

    private record Fixture(
        TiledMap map,
        TiledObjectLayer particles,
        TiledObjectLayer objects,
        TiledObjectLayer effects
    ) {
        private TiledObjectEntity source(TiledObjectLayer layer) {
            Tile tile = new Tile(0, 0, 64, 64);
            tile.setCollisions(new TiledObjectLayer());
            TiledObjectEntity source = new TiledObjectEntity(-1, 20, 20, tile);
            layer.add(source);
            return source;
        }
    }
}
