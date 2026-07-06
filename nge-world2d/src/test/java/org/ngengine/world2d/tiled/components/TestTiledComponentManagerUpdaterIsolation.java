package org.ngengine.world2d.tiled.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.ngengine.components.AbstractComponentManager;
import org.ngengine.components.runners.ComponentInitializer;
import org.ngengine.components.runners.ComponentUpdater;
import org.junit.jupiter.api.Test;

public class TestTiledComponentManagerUpdaterIsolation {

    @Test
    public void childManagersDoNotInheritParentTiledUpdaters() throws Exception {
        TestManager root = new TestManager();
        TiledComponentManager map = new TiledComponentManager();
        TiledComponentManager layer = new TiledComponentManager();
        TiledComponentManager entity = new TiledComponentManager();

        map.setParent(root);
        layer.setParent(map);
        entity.setParent(layer);

        map.onUpdate(0f);
        layer.onUpdate(0f);
        entity.onUpdate(0f);

        assertEquals(3, countTiledUpdaters(map));
        assertEquals(3, countTiledUpdaters(layer));
        assertEquals(3, countTiledUpdaters(entity));
        assertEquals(2, countTiledInitializers(map));
        assertEquals(2, countTiledInitializers(layer));
        assertEquals(2, countTiledInitializers(entity));
        assertEquals(1, countPhysicsManagers(map));
        assertEquals(1, countPhysicsManagers(layer));
        assertEquals(1, countPhysicsManagers(entity));
    }

    private int countTiledUpdaters(TiledComponentManager manager) {
        int count = 0;
        for (ComponentUpdater updater : manager.getUpdaters()) {
            if (updater instanceof TiledLogicUpdater
                    || updater instanceof TiledEntityLifecycleManager
                    || updater instanceof TiledGuiUpdater) {
                count++;
            }
        }
        return count;
    }

    private int countTiledInitializers(TiledComponentManager manager) {
        int count = 0;
        for (ComponentInitializer initializer : manager.getInitializers()) {
            if (initializer instanceof TiledEntityLifecycleManager || initializer instanceof TiledGuiUpdater) {
                count++;
            }
        }
        return count;
    }

    private int countPhysicsManagers(TiledComponentManager manager) throws Exception {
        Field field = TiledComponentManager.class.getDeclaredField("physicsManagers");
        field.setAccessible(true);
        return ((List<?>) field.get(manager)).size();
    }

    private static class TestManager extends AbstractComponentManager {
    }
}
