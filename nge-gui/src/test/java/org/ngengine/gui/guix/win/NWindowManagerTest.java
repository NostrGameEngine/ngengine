/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.guix.win;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeSystemDelegate;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NWindowManagerTest {

    @Test
    public void passiveHudStaysAttachedWhenInteractiveWindowsChange() {
        TestWindowManager manager = newManager("gui-window-stack");
        Node guiNode = (Node) manager.getContext().getGuiNode();

        NHud hud = manager.showWindow(NHud.class);
        assertSame(guiNode, hud.getParent());
        assertFalse(manager.hasInteractiveWindows());

        TestWindow first = manager.showWindow(TestWindow.class);
        assertSame(guiNode, hud.getParent());
        assertSame(guiNode, first.getParent());
        assertTrue(manager.hasInteractiveWindows());

        TestWindow second = manager.showWindow(TestWindow.class);
        assertSame(guiNode, hud.getParent());
        assertNull(first.getParent());
        assertSame(guiNode, second.getParent());

        manager.back();
        assertSame(guiNode, hud.getParent());
        assertSame(guiNode, first.getParent());
        assertNull(second.getParent());

        manager.back();
        assertSame(guiNode, hud.getParent());
        assertNull(first.getParent());
        assertFalse(manager.hasInteractiveWindows());
    }

    @Test
    public void backDoesNotClosePassiveHud() {
        TestWindowManager manager = newManager("gui-hud-back");
        Node guiNode = (Node) manager.getContext().getGuiNode();

        NHud hud = manager.showWindow(NHud.class);

        manager.back();
        manager.back();

        assertSame(guiNode, hud.getParent());
        assertFalse(manager.hasInteractiveWindows());
    }

    @Test
    public void interactionRequestBecomesActiveOnlyWhileInteractiveWindowExists() throws Exception {
        initializeGui();

        ViewPort vp = new ViewPort("gui-interaction-intent", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        TestInteractionComponent component = new TestInteractionComponent();
        TestWindowManager manager = new TestWindowManager(component, context);
        setWindowManagers(component, manager);
        setComponentEnabled(component, true);

        component.setInteractionEnabled(true);
        assertTrue(component.isInteractionEnabled());
        assertFalse(component.isInteractionActive());
        assertFalse(component.physicalCursorVisible);
        assertEquals(0, component.releases.get());

        component.updateAppLogic(null, 0);
        assertFalse(component.isInteractionActive());
        assertFalse(component.physicalCursorVisible);
        assertEquals(0, component.releases.get());

        TestWindow first = manager.showWindow(TestWindow.class);
        assertSame(guiNode, first.getParent());
        assertTrue(component.isInteractionEnabled());
        assertTrue(component.isInteractionActive());
        assertTrue(context.getNavigator().isCursorVisible());
        assertEquals(1, component.releases.get());

        manager.back();
        assertTrue(component.isInteractionEnabled());
        assertFalse(component.isInteractionActive());
        assertFalse(context.getNavigator().isCursorVisible());
        assertEquals(1, component.releases.get());

        TestWindow second = manager.showWindow(TestWindow.class);
        assertSame(guiNode, second.getParent());
        assertTrue(component.isInteractionActive());
        assertTrue(context.getNavigator().isCursorVisible());
        assertEquals(2, component.releases.get());

        component.setInteractionEnabled(false);
        assertFalse(component.isInteractionEnabled());
        assertFalse(component.isInteractionActive());
        assertFalse(component.physicalCursorVisible);

        component.updateAppLogic(null, 0);
        assertFalse(component.isInteractionEnabled());
        assertFalse(component.isInteractionActive());
        assertFalse(component.physicalCursorVisible);
        assertEquals(2, component.releases.get());
    }

    private static TestWindowManager newManager(String name) {
        initializeGui();

        ViewPort vp = new ViewPort(name, new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        return new TestWindowManager(new TestWindowManagerComponent(), context);
    }

    private static void initializeGui() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
                NWindowManagerTest.class.getResource("/com/jme3/asset/Desktop.cfg"));
        NGEGui.initialize(assets);
    }

    @SuppressWarnings("unchecked")
    private static void setWindowManagers(NWindowManagerComponent component, NWindowManager manager) throws Exception {
        Field field = NWindowManagerComponent.class.getDeclaredField("windowManagers");
        field.setAccessible(true);
        ArrayList<NWindowManager> managers = (ArrayList<NWindowManager>) field.get(component);
        managers.clear();
        managers.add(manager);
    }

    private static void setComponentEnabled(NWindowManagerComponent component, boolean enabled) throws Exception {
        Field field = NWindowManagerComponent.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.set(component, enabled);
    }

    public static class TestWindow extends NWindow<Void> {
        @Override
        protected void compose(Vector3f size, Void args) throws Throwable {
        }
    }

    private static class TestWindowManager extends NWindowManager {
        TestWindowManager(NWindowManagerComponent mng, GuiContext ctx) {
            super(mng, ctx);
        }

        @Override
        protected void checkThread() {
        }
    }

    private static class TestWindowManagerComponent extends NWindowManagerComponent {
        @Override
        void onWindowStackChanged() {
        }
    }

    private static class TestInteractionComponent extends NWindowManagerComponent {
        private final AtomicInteger releases = new AtomicInteger();
        private boolean physicalCursorVisible;

        @Override
        public <T> T getInstanceOf(Class<T> type) {
            return null;
        }

        @Override
        protected void setPhysicalCursorVisible(boolean visible) {
            physicalCursorVisible = visible;
        }

        @Override
        protected void releaseActiveInputMappings() {
            releases.incrementAndGet();
        }
    }

    private static final class TestSystemDelegate extends JmeSystemDelegate {
        @Override
        public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height)
                throws IOException {
        }

        @Override
        public URL getPlatformAssetConfigURL() {
            return NWindowManagerTest.class.getResource("/com/jme3/asset/Desktop.cfg");
        }

        @Override
        public JmeContext newContext(AppSettings settings, JmeContext.Type contextType) {
            return null;
        }

        @Override
        public AudioRenderer newAudioRenderer(AppSettings settings) {
            return null;
        }

        @Override
        public void initialize(AppSettings settings) {
        }

        @Override
        public void showSoftKeyboard(boolean show) {
        }
    }
}
