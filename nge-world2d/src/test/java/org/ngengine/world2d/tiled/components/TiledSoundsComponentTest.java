package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

import com.jme3.math.Transform;

class TiledSoundsComponentTest {

    @Test
    void worldTransformIsAvailableBeforeCoordinateSystemIsMounted() {
        TiledObjectEntity entity = new TiledObjectEntity(BigInteger.ONE, 0, 0, 1, 1);
        ExposedTiledSoundsComponent sounds = new ExposedTiledSoundsComponent();
        sounds.onAttached(entity.getComponentManager(), null, null);

        assertNotNull(sounds.worldTransform());
    }

    private static final class ExposedTiledSoundsComponent extends TiledSoundsComponent {
        private Transform worldTransform() {
            return getWorldTransform();
        }
    }
}
