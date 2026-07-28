/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.world2d;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.components.TiledComponentManager;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;

class TiledWorldTeardownTest {

    @Test
    void disablingWorldReachesMapLayersAndEveryEntityKind() {
        TrackingMap map = new TrackingMap();
        TrackingObjectLayer objectLayer = new TrackingObjectLayer();
        TrackingObjectEntity object = new TrackingObjectEntity();
        objectLayer.add(object);
        map.addLayer(objectLayer);

        TrackingTileLayer tileLayer = new TrackingTileLayer();
        map.addLayer(tileLayer);

        TiledWorld2dManagerComponent.disableWorldComponentManagers(map);

        assertEquals(1, map.manager.disableCalls);
        assertEquals(1, objectLayer.manager.disableCalls);
        assertEquals(1, object.manager.disableCalls);
        assertEquals(1, tileLayer.manager.disableCalls);
        assertEquals(1, tileLayer.entity.manager.disableCalls);
    }

    private static final class TrackingManager extends TiledComponentManager {
        private int disableCalls;

        @Override
        public void setEnabled(boolean enabled) {
            if (!enabled) {
                disableCalls++;
            }
        }
    }

    private static final class TrackingMap extends TiledMap {
        private final TrackingManager manager = new TrackingManager();

        private TrackingMap() {
            super(1, 1);
        }

        @Override
        public TiledComponentManager getComponentManager() {
            return manager;
        }
    }

    private static final class TrackingObjectLayer extends TiledObjectLayer {
        private final TrackingManager manager = new TrackingManager();

        private TrackingObjectLayer() {
            super(1, 1);
        }

        @Override
        public TiledComponentManager getComponentManager() {
            return manager;
        }
    }

    private static final class TrackingObjectEntity extends TiledObjectEntity {
        private final TrackingManager manager = new TrackingManager();

        private TrackingObjectEntity() {
            super(BigInteger.ONE, 0, 0, 1, 1);
        }

        @Override
        public TiledComponentManager getComponentManager() {
            return manager;
        }
    }

    private static final class TrackingTileLayer extends TiledTileLayer {
        private final TrackingManager manager = new TrackingManager();
        private final TrackingTileEntity entity = new TrackingTileEntity();

        private TrackingTileLayer() {
            super(1, 1);
        }

        @Override
        public TiledComponentManager getComponentManager() {
            return manager;
        }

        @Override
        public TiledTileEntity getTileAt(int x, int y) {
            return entity;
        }
    }

    private static final class TrackingTileEntity extends TiledTileEntity {
        private final TrackingManager manager = new TrackingManager();

        private TrackingTileEntity() {
            super(null, null, 0, 0);
        }

        @Override
        public TiledComponentManager getComponentManager() {
            return manager;
        }
    }
}
