/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.world2d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

class TiledObjectLayerUpdateTest {

    @Test
    void entityRemovedEarlierInStableTraversalIsNotUpdatedOrReenabled() {
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        TiledObjectEntity remover = object(1);
        TiledObjectEntity removed = object(2);
        layer.add(remover);
        layer.add(removed);

        TrackingManager manager = new TrackingManager(remover, removed);
        manager.updateObjectLayer(1f / 60f, null, layer);

        assertNull(removed.getObjectGroup());
        assertEquals(0, manager.removedUpdates);
    }

    @Test
    void addingAnObjectToAnotherLayerMovesItWithoutLeavingAStaleEntry() {
        TiledObjectLayer first = new TiledObjectLayer(4, 4);
        TiledObjectLayer second = new TiledObjectLayer(4, 4);
        TiledObjectEntity object = object(1);
        first.add(object);

        second.add(object);

        assertEquals(0, first.getObjects().size());
        assertEquals(1, second.getObjects().size());
        assertSame(second, object.getObjectGroup());
    }

    @Test
    void addingAnObjectToItsCurrentLayerDoesNotDuplicateIt() {
        TiledObjectLayer layer = new TiledObjectLayer(4, 4);
        TiledObjectEntity object = object(1);

        layer.add(object);
        layer.add(object);

        assertEquals(1, layer.getObjects().size());
    }

    @Test
    void objectMovedDuringUpdateIsNotUpdatedAgainInItsNewLayer() {
        TiledObjectLayer first = new TiledObjectLayer(4, 4);
        TiledObjectLayer second = new TiledObjectLayer(4, 4);
        TiledObjectEntity object = object(1);
        first.add(object);
        MovingManager manager = new MovingManager(object, second);

        manager.updateObjectLayer(1f / 60f, null, first);
        manager.updateObjectLayer(1f / 60f, null, second);

        assertSame(second, object.getObjectGroup());
        assertEquals(1, manager.updates);
    }

    @Test
    void detachCallbackCanReattachObjectWithoutOldLayerClearingNewOwner() {
        TiledObjectLayer first = new TiledObjectLayer(4, 4);
        TiledObjectLayer second = new TiledObjectLayer(4, 4);
        ReattachingObject object = new ReattachingObject(second);
        first.add(object);

        first.remove(object);

        assertEquals(0, first.getObjects().size());
        assertEquals(1, second.getObjects().size());
        assertSame(second, object.getObjectGroup());
    }

    private static TiledObjectEntity object(long id) {
        return new TiledObjectEntity(BigInteger.valueOf(id), 0, 0, 16, 16);
    }

    private static final class TrackingManager extends TiledWorld2dManagerComponent {
        private final TiledObjectEntity remover;
        private final TiledObjectEntity removed;
        private int removedUpdates;

        private TrackingManager(TiledObjectEntity remover, TiledObjectEntity removed) {
            super((String) null);
            this.remover = remover;
            this.removed = removed;
        }

        @Override
        protected void onEntityUpdate(
                float tpf,
                TiledWorld2d world,
                TiledLayer layer,
                TiledEntity entity) {
            if (entity == remover) {
                removed.removeFromLayer();
            } else if (entity == removed) {
                removedUpdates++;
            }
        }
    }

    private static final class MovingManager extends TiledWorld2dManagerComponent {
        private final TiledObjectEntity object;
        private final TiledObjectLayer destination;
        private int updates;

        private MovingManager(TiledObjectEntity object, TiledObjectLayer destination) {
            super((String) null);
            this.object = object;
            this.destination = destination;
        }

        @Override
        protected void onEntityUpdate(
                float tpf,
                TiledWorld2d world,
                TiledLayer layer,
                TiledEntity entity) {
            updates++;
            if (entity == object && object.getObjectGroup() != destination) {
                destination.add(object);
            }
        }
    }

    private static final class ReattachingObject extends TiledObjectEntity {
        private final TiledObjectLayer destination;

        private ReattachingObject(TiledObjectLayer destination) {
            super(BigInteger.ONE, 0, 0, 16, 16);
            this.destination = destination;
        }

        @Override
        protected void detached() {
            super.detached();
            destination.add(this);
        }
    }
}
