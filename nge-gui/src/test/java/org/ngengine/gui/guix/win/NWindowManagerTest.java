/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.guix.win;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.Mouse;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.ngengine.ViewPortManager;
import org.ngengine.components.ComponentManager;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.Panel;
import org.ngengine.gui.ime.ImeCompositionEvent;
import org.ngengine.gui.ime.JmeSoftKeyboardImeComposer;
import org.ngengine.gui.ime.PhysicalKeyboardImeComposer;
import org.ngengine.gui.nav.FocusTarget;
import org.ngengine.gui.nav.DefaultNavigatorInputHandler;
import org.ngengine.gui.nav.Navigator;

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
    public void passiveHudCanReceivePointerFocusWithoutBecomingInteractive() throws Throwable {
        TestWindowManager manager = newManager("gui-hud-pointer");

        NHud hud = manager.showWindow(NHud.class);
        hud.preCompose(new Vector3f(800, 600, 0), null);
        Panel target = new Panel();
        target.getControl(org.ngengine.gui.core.GuiControl.class).setFocusable(FocusTarget.FOCUS_POINTER);
        hud.getBottomRight().addChild(target);

        assertFalse(manager.hasInteractiveWindows());
        assertTrue(manager.hasPointerInteractiveWindows());

        assertTrue(manager.getContext().getNavigator().focusPointer(target));
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

    @Test
    public void physicalCursorRequestBeforeMountIsAppliedWhenInputManagerIsAvailable() throws Exception {
        initializeGui();

        ViewPort vp = new ViewPort("gui-physical-cursor-pending", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        TestInputComponent component = new TestInputComponent();
        TestWindowManager manager = new TestWindowManager(component, context);
        setWindowManagers(component, manager);

        component.inputManager.setCursorVisible(true);
        assertTrue(component.inputManager.isCursorVisible());

        component.requestPhysicalCursorVisible(false);
        assertTrue(component.inputManager.isCursorVisible());

        component.onAttached(emptyComponentManager(), null, null);
        setComponentEnabled(component, true);
        component.updateAppLogic(null, 0);

        assertFalse(component.inputManager.isCursorVisible());
    }

    @Test
    public void hardwareCursorVisibilityFollowsNavigatorActivity() throws Exception {
        initializeGui();

        ViewPort vp = new ViewPort("gui-hardware-cursor-autohide", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        TestInteractionComponent component = new TestInteractionComponent();
        TestWindowManager manager = new TestWindowManager(component, context);
        setWindowManagers(component, manager);
        setComponentEnabled(component, true);

        Navigator navigator = context.getNavigator();
        navigator.setHardwareCursor(true);
        component.setInteractionEnabled(true);
        manager.showWindow(TestWindow.class);

        assertFalse(component.physicalCursorVisible);

        assertTrue(navigator.updateCursorPosition(10, 20));
        component.updateAppLogic(null, 0);
        assertTrue(component.physicalCursorVisible);

        component.updateAppLogic(null, 15.1f);
        assertFalse(component.physicalCursorVisible);

        assertTrue(navigator.updateCursorPosition(30, 40));
        component.updateAppLogic(null, 0);
        assertTrue(component.physicalCursorVisible);
    }

    @Test
    public void inputHandlerInstallsSoftKeyboardImeBeforeKeyboardDeviceIsSelected() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-ime", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        TestInputComponent component = new TestInputComponent();
        TestWindowManager manager = new TestWindowManager(component, context);

        manager.setInputHandler(new DefaultNavigatorInputHandler(vp));

        assertTrue(context.getImeComposer() instanceof PhysicalKeyboardImeComposer);
        assertTrue(context.getImeComposer() instanceof JmeSoftKeyboardImeComposer);
    }

    @Test
    public void softKeyboardImeComposerDoesNotShowJmeSystemKeyboardForMouseInput() {
        TestSystemDelegate system = initializeGui();

        ViewPort vp = new ViewPort("gui-soft-keyboard-ime", new Camera(800, 600));
        GuiContext context = NGEGui.register(vp, true);
        context.setInputDevice(new Mouse());
        InputManager inputManager = new InputManager(new DummyMouseInput(), new DummyKeyInput(), null, null);
        JmeSoftKeyboardImeComposer composer = new JmeSoftKeyboardImeComposer(inputManager);

        composer.open(context, ev -> {}, new ImeCompositionEvent("typing"), c -> c, s -> 0f);
        assertEquals(0, system.showSoftKeyboardCalls.get());
        assertEquals(0, system.hideSoftKeyboardCalls.get());

        composer.close();
        assertEquals(0, system.showSoftKeyboardCalls.get());
        assertEquals(0, system.hideSoftKeyboardCalls.get());
    }

    @Test
    public void softKeyboardImeComposerUsesJmeSystemHookForJoystickInput() {
        TestSystemDelegate system = initializeGui();

        ViewPort vp = new ViewPort("gui-joystick-keyboard-ime", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        context.setInputDevice(new TestJoystick());
        InputManager inputManager = new InputManager(new DummyMouseInput(), new DummyKeyInput(), null, null);
        JmeSoftKeyboardImeComposer composer = new JmeSoftKeyboardImeComposer(inputManager);
        ImeCompositionEvent event = new ImeCompositionEvent("");

        composer.open(context, ev -> {}, event, c -> c, s -> 0f);

        assertEquals("", event.getText());
        assertEquals(1, system.showSoftKeyboardCalls.get());
        assertEquals(0, system.hideSoftKeyboardCalls.get());
        assertTrue(composer.isOpen());

        composer.close();
        assertFalse(composer.isOpen());
        assertEquals(1, system.hideSoftKeyboardCalls.get());
        assertEquals(0, guiNode.getQuantity());
    }

    @Test
    public void relativeManagerCreatesDedicatedAspectGuiCameraWithPhysicalRenderTarget() {
        initializeGui();

        TestViewportComponent component = new TestViewportComponent(true, 1280, 720);
        NWindowManager manager = component.getManager(null, null);
        ViewPort vp = manager.getViewPort();

        assertTrue(component.isRelativeSize());
        assertTrue(NWindowManagerComponent.isRelativeSize(vp.getCamera()));
        assertEquals(1778, vp.getCamera().getWidth());
        assertEquals(NWindowManagerComponent.RELATIVE_CAMERA_SCALE, vp.getCamera().getHeight());
        assertEquals(1280f / 720f, manager.getLogicalWidth(), 0.001f);
        assertEquals(1f, manager.getLogicalHeight(), 0.001f);
        assertEquals(1280, vp.getRenderTargetWidth());
        assertEquals(720, vp.getRenderTargetHeight());
        assertSame(component.createdGuiViewPort, vp);
    }

    @Test
    public void relativeManagerUsesPhysicalRenderTargetWhenLogicalCameraIsScaled() {
        initializeGui();

        TestViewportComponent component = new TestViewportComponent(true, 1280, 720, 2560, 1440);
        NWindowManager manager = component.getManager(null, null);
        ViewPort vp = manager.getViewPort();

        assertTrue(component.isRelativeSize());
        assertEquals(1778, vp.getCamera().getWidth());
        assertEquals(NWindowManagerComponent.RELATIVE_CAMERA_SCALE, vp.getCamera().getHeight());
        assertEquals(2560, vp.getRenderTargetWidth());
        assertEquals(1440, vp.getRenderTargetHeight());
        assertEquals(2560, component.getPhysicalWidth());
        assertEquals(1440, component.getPhysicalHeight());
    }

    @Test
    public void relativeManagerConvertsInputUsingLogicalCoordinatesWhenRenderTargetIsScaled() {
        initializeGui();

        TestViewportComponent component = new TestViewportComponent(true, 1280, 720, 2560, 1440);
        NWindowManager manager = component.getManager(null, null);

        assertEquals(0.5f, manager.getContext().toGuiX(360), 0.001f);
        assertEquals(0.5f, manager.getContext().toGuiY(360), 0.001f);
        assertEquals(0.5f, manager.getContext().toGuiDeltaX(360), 0.001f);
        assertEquals(0.5f, manager.getContext().toGuiDistance(360), 0.001f);
    }

    @Test
    public void pixelManagerCreatesDedicatedPixelGuiCamera() {
        initializeGui();

        TestViewportComponent component = new TestViewportComponent(false, 1024, 768);
        NWindowManager manager = component.getManager(null, null);
        ViewPort vp = manager.getViewPort();

        assertFalse(component.isRelativeSize());
        assertFalse(NWindowManagerComponent.isRelativeSize(vp.getCamera()));
        assertEquals(1024, vp.getCamera().getWidth());
        assertEquals(768, vp.getCamera().getHeight());
        assertEquals(1024, vp.getRenderTargetWidth());
        assertEquals(768, vp.getRenderTargetHeight());
        assertSame(component.createdGuiViewPort, vp);
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

    private static TestSystemDelegate initializeGui() {
        TestSystemDelegate system = new TestSystemDelegate();
        JmeSystem.setSystemDelegate(system);
        AssetManager assets = JmeSystem.newAssetManager(
                NWindowManagerTest.class.getResource("/com/jme3/asset/Desktop.cfg"));
        NGEGui.initialize(assets);
        return system;
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

    private static ComponentManager emptyComponentManager() {
        return (ComponentManager) Proxy.newProxyInstance(
                ComponentManager.class.getClassLoader(),
                new Class<?>[] { ComponentManager.class },
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == List.class) return Collections.emptyList();
                    return null;
                });
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

    private static class TestInputComponent extends TestWindowManagerComponent {
        private final DummyMouseInput mouse = new DummyMouseInput();
        private final DummyKeyInput keys = new DummyKeyInput();
        private final InputManager inputManager = new InputManager(mouse, keys, null, null);

        TestInputComponent() {
            mouse.initialize();
            keys.initialize();
        }

        void requestPhysicalCursorVisible(boolean visible) {
            setPhysicalCursorVisible(visible);
        }

        @Override
        public <T> T getInstanceOf(Class<T> type) {
            if (type == InputManager.class) {
                return type.cast(inputManager);
            }
            return null;
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

    private static class TestViewportComponent extends NWindowManagerComponent {
        private final TestViewPortManager viewPortManager;
        private ViewPort createdGuiViewPort;

        TestViewportComponent(boolean relativeSize, int width, int height) {
            this(relativeSize, width, height, width, height);
        }

        TestViewportComponent(boolean relativeSize, int width, int height, int renderTargetWidth, int renderTargetHeight) {
            super(relativeSize);
            this.viewPortManager = new TestViewPortManager(width, height, renderTargetWidth, renderTargetHeight);
        }

        @Override
        public <T> T getInstanceOf(Class<T> type) {
            if (type == ViewPortManager.class) {
                return type.cast(viewPortManager);
            }
            return null;
        }

        private class TestViewPortManager implements ViewPortManager {
            private final ViewPort mainViewPort;

            TestViewPortManager(int width, int height, int renderTargetWidth, int renderTargetHeight) {
                mainViewPort = new ViewPort("main", new Camera(width, height));
                mainViewPort.setRenderTargetSize(renderTargetWidth, renderTargetHeight);
            }

            @Override
            public ViewPort getMainSceneViewPort() {
                return mainViewPort;
            }

            @Override
            public ViewPort getGuiViewPort() {
                return null;
            }

            @Override
            public ViewPort createNewGuiViewPort(String name, Camera cam) {
                createdGuiViewPort = new ViewPort(name, cam);
                createdGuiViewPort.setRenderTargetSize(
                        mainViewPort.getRenderTargetWidth(),
                        mainViewPort.getRenderTargetHeight());
                return createdGuiViewPort;
            }

            @Override
            public boolean removeGuiViewPort(ViewPort vp) {
                if (createdGuiViewPort == vp) {
                    createdGuiViewPort = null;
                    return true;
                }
                return false;
            }

            @Override
            public List<ViewPort> getSceneViewPorts() {
                return Collections.singletonList(mainViewPort);
            }

            @Override
            public ViewPort createNewSceneViewPort(String name, Camera cam) {
                return new ViewPort(name, cam);
            }

            @Override
            public FilterPostProcessor getFilterPostProcessor(ViewPort vp) {
                return null;
            }
        }
    }

    private static final class TestJoystick implements Joystick {
        @Override
        public void assignButton(String mappingName, int buttonId) {
        }

        @Override
        public void assignAxis(String positiveMapping, String negativeMapping, int axisId) {
        }

        @Override
        public JoystickAxis getAxis(String logicalId) {
            return null;
        }

        @Override
        public List<JoystickAxis> getAxes() {
            return Collections.emptyList();
        }

        @Override
        public JoystickButton getButton(String logicalId) {
            return null;
        }

        @Override
        public List<JoystickButton> getButtons() {
            return Collections.emptyList();
        }

        @Override
        public JoystickAxis getXAxis() {
            return null;
        }

        @Override
        public JoystickAxis getYAxis() {
            return null;
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return null;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return null;
        }

        @Override
        public int getXAxisIndex() {
            return 0;
        }

        @Override
        public int getYAxisIndex() {
            return 1;
        }

        @Override
        public int getAxisCount() {
            return 0;
        }

        @Override
        public int getButtonCount() {
            return 0;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public int getJoyId() {
            return 0;
        }

        @Override
        public void rumble(float amountHigh, float amountLow, float duration) {
        }
    }

    private static final class TestSystemDelegate extends JmeSystemDelegate {
        private final AtomicInteger showSoftKeyboardCalls = new AtomicInteger();
        private final AtomicInteger hideSoftKeyboardCalls = new AtomicInteger();

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
            if (show) {
                showSoftKeyboardCalls.incrementAndGet();
            } else {
                hideSoftKeyboardCalls.incrementAndGet();
            }
        }
    }
}
