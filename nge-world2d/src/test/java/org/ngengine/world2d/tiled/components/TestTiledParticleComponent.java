package org.ngengine.world2d.tiled.components;

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

public class TestTiledParticleComponent {

    @Test
    public void followCanBeConfiguredBeforeComponentIsAttached() {
        TiledParticleComponent component = new TiledParticleComponent();
        TiledObjectEntity target = new TiledObjectEntity(-1, 10, 20, 32, 32);

        assertSame(component, component.follow(target, 16f, 0f));
    }
}
