/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.ngengine.components.runners.ComponentUpdater;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.renderer.RenderManager;

class AbstractComponentManagerRemovalTest {

    @Test
    void componentRemovedDuringUpdateReceivesNoLaterCallbacks() {
        TestManager manager = new TestManager();
        TestComponent component = new TestComponent();
        RemovingUpdater remover = new RemovingUpdater();
        TrackingUpdater tracker = new TrackingUpdater();
        manager.getUpdaters().add(remover);
        manager.getUpdaters().add(tracker);
        manager.addComponent(component);
        manager.enableComponent(component);

        manager.update(1f / 60f);
        manager.render(null);

        assertEquals(0, tracker.updates);
        assertEquals(0, tracker.afterUpdates);
        assertEquals(0, tracker.renders);
        assertEquals(0, tracker.afterRenders);
    }

    private static final class TestManager extends AbstractComponentManager {
        private void update(float tpf) {
            onUpdate(tpf);
        }

        private void render(RenderManager renderManager) {
            onRender(renderManager);
        }
    }

    private static final class TestComponent implements Component {
        @Override
        public void onEnable(ComponentManager manager, Runner runner, DataStoreProvider dataStore, boolean firstTime) {
        }

        @Override
        public void onDisable(ComponentManager manager, Runner runner, DataStoreProvider dataStore) {
        }

        @Override
        public Component newInstance() {
            return new TestComponent();
        }

        @Override
        public ComponentManager getComponentManager() {
            return null;
        }
    }

    private static final class RemovingUpdater implements ComponentUpdater {
        @Override
        public boolean canUpdate(ComponentManager manager, Component component) {
            return true;
        }

        @Override
        public void update(ComponentManager manager, Component component, float tpf) {
            manager.removeComponent(component);
        }

        @Override
        public void render(ComponentManager manager, Component component) {
        }
    }

    private static final class TrackingUpdater implements ComponentUpdater {
        private int updates;
        private int afterUpdates;
        private int renders;
        private int afterRenders;

        @Override
        public boolean canUpdate(ComponentManager manager, Component component) {
            return true;
        }

        @Override
        public void update(ComponentManager manager, Component component, float tpf) {
            updates++;
        }

        @Override
        public void afterUpdate(ComponentManager manager, Component component) {
            afterUpdates++;
        }

        @Override
        public void render(ComponentManager manager, Component component) {
            renders++;
        }

        @Override
        public void afterRender(ComponentManager manager, Component component) {
            afterRenders++;
        }
    }
}
