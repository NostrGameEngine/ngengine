package org.ngengine.world2d.tiled.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

public class TiledObjectLayerTest {

    @Test
    public void removalKeepsLayerScopeAvailableDuringDetachCleanup() {
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        TrackingObjectEntity entity = new TrackingObjectEntity();
        layer.add(entity);

        layer.remove(entity);

        assertSame(layer, entity.groupDuringDetach);
        assertNull(entity.getObjectGroup());
    }

    private static final class TrackingObjectEntity extends TiledObjectEntity {
        private TiledObjectLayer groupDuringDetach;

        private TrackingObjectEntity() {
            super(BigInteger.valueOf(77), 0, 0, 16, 16);
        }

        @Override
        protected void detached() {
            groupDuringDetach = getObjectGroup();
            super.detached();
        }
    }
}
