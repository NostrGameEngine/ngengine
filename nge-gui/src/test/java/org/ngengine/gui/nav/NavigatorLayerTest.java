/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.ngengine.gui.nav;

import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.RawInputListener;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.audio.AudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeSystemDelegate;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.core.GuiControl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class NavigatorLayerTest {

    @Test
    public void autofocusSelectsTopLeftFocusableElementAndActionsIt() {
        Node root = new Node("root");
        Node right = focusable("right", 20f, 40f);
        Node topLeft = focusable("topLeft", 5f, 40f);
        Node lowerLeft = focusable("lowerLeft", 0f, 10f);
        List<String> events = new ArrayList<>();
        addRecorder(topLeft, events);

        root.attachChild(right);
        root.attachChild(topLeft);
        root.attachChild(lowerLeft);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.updateFocus(true);
        layer.action(true);
        layer.action(false);

        assertSame(topLeft, layer.getFocus());
        assertEquals(List.of(
            "gained:topLeft",
            "action:topLeft:true",
            "action:topLeft:false"
        ), events);
    }

    @Test
    public void directionalNavigationSkipsNonFocusableElements() {
        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node nonFocusable = new Node("nonFocusable");
        nonFocusable.setLocalTranslation(new Vector3f(20f, 0f, 0f));
        Node target = focusable("target", 40f, 0f);

        root.attachChild(start);
        root.attachChild(nonFocusable);
        root.attachChild(target);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.focus(start);

        assertSame(target, layer.navigate(TraversalDirection.Right));
        assertSame(target, layer.getFocus());
    }

    @Test
    public void joystickHandlerConfirmsFocusedElementAndBacksOutWithFocusPresent() {
        initializeGui();

        ViewPort vp = new ViewPort("gui", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        Node root = new Node("root");
        Node target = focusable("target", 0f, 0f);
        List<String> events = new ArrayList<>();
        addRecorder(target, events);
        root.attachChild(target);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        AtomicInteger backActions = new AtomicInteger();
        AtomicInteger primaryActions = new AtomicInteger();
        handler.setBackAction(backActions::incrementAndGet);
        handler.setPrimaryAction(primaryActions::incrementAndGet);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        assertSame(target, context.getNavigator().autofocus());

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 2), true));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 2), false));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 1), true));
        inputManager.update(0.016f);

        assertEquals(List.of(
            "gained:target",
            "action:target:true",
            "action:target:false"
        ), events);
        assertEquals(0, primaryActions.get());
        assertEquals(1, backActions.get());
    }

    @Test
    public void disabledCursorDoesNotBecomeActiveWhenPositionUpdates() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-cursor", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        Navigator navigator = NGEGui.register(vp, true).getNavigator();

        assertEquals(true, navigator.updateCursorPosition(10, 20));
        assertEquals(true, navigator.isCursorVisible());
        assertEquals(true, navigator.isCursorActive());

        navigator.setCursor(false);

        assertEquals(false, navigator.updateCursorPosition(30, 40));
        assertEquals(false, navigator.isCursorVisible());
        assertEquals(false, navigator.isCursorActive());
    }

    private static void initializeGui() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
                NavigatorLayerTest.class.getResource("/com/jme3/asset/Desktop.cfg"));
        NGEGui.initialize(assets);
    }

    private static Node focusable(String name, float x, float y) {
        Node node = new Node(name);
        node.setLocalTranslation(new Vector3f(x, y, 0f));
        GuiControl control = new GuiControl("focus");
        control.setFocusable(true);
        node.addControl(control);
        return node;
    }

    private static void addRecorder(Node node, List<String> events) {
        node.getControl(GuiControl.class).addFocusChangeListener(new FocusListener() {
            @Override
            public void focusGained(Spatial target) {
                events.add("gained:" + target.getName());
            }

            @Override
            public void focusLost(Spatial target) {
                events.add("lost:" + target.getName());
            }

            @Override
            public void focusAction(Spatial target, boolean pressed) {
                events.add("action:" + target.getName() + ":" + pressed);
            }

            @Override
            public void focusScrollUpdate(Spatial target, ScrollDirection dir, double value) {
            }
        });
    }

    private static InputManager newInputManager(JoyInput joyInput) {
        DummyMouseInput mouse = new DummyMouseInput();
        DummyKeyInput keys = new DummyKeyInput();
        mouse.initialize();
        keys.initialize();
        joyInput.initialize();
        return new InputManager(mouse, keys, joyInput, null);
    }

    private static final class TestJoyInput implements JoyInput {
        private final Queue<InputEvent<?>> events = new ArrayDeque<>();
        private final Joystick joystick;
        private RawInputListener listener;
        private boolean initialized;

        private TestJoyInput(Joystick joystick) {
            this.joystick = joystick;
        }

        private void queue(InputEvent<?> event) {
            events.add(event);
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void update() {
            while (!events.isEmpty()) {
                InputEvent<?> event = events.remove();
                if (event instanceof JoyButtonEvent) {
                    listener.onJoyButtonEvent((JoyButtonEvent) event);
                } else {
                    throw new IllegalArgumentException("Unsupported event: " + event);
                }
            }
        }

        @Override
        public void destroy() {
            initialized = false;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        @Override
        public long getInputTimeNanos() {
            return System.nanoTime();
        }

        @Override
        public void setJoyRumble(int joyId, float amountHigh, float amountLow, float duration) {
        }

        @Override
        public Joystick[] loadJoysticks(InputManager inputManager) {
            return new Joystick[] { joystick };
        }
    }

    private static final class TestJoystick implements Joystick {
        @Override
        public int getJoyId() {
            return 0;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public List<JoystickAxis> getAxes() {
            return List.of();
        }

        @Override
        public List<JoystickButton> getButtons() {
            return List.of();
        }

        @Override
        public JoystickAxis getAxis(String logicalId) {
            return new TestJoystickAxis(this, axisId(logicalId), logicalId);
        }

        @Override
        public JoystickButton getButton(String logicalId) {
            return new TestJoystickButton(this, Integer.parseInt(logicalId));
        }

        @Override
        public JoystickAxis getXAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X);
        }

        @Override
        public JoystickAxis getYAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y);
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X);
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y);
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
            return 4;
        }

        @Override
        public int getButtonCount() {
            return 16;
        }

        @Override
        public void assignButton(String mappingName, int buttonId) {
        }

        @Override
        public void assignAxis(String positiveMapping, String negativeMapping, int axisId) {
        }

        @Override
        public void rumble(float amountHigh, float amountLow, float duration) {
        }

        private int axisId(String logicalId) {
            if (JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X.equals(logicalId)) return 0;
            if (JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y.equals(logicalId)) return 1;
            if (JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X.equals(logicalId)) return 2;
            if (JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y.equals(logicalId)) return 3;
            return 0;
        }
    }

    private static final class TestJoystickAxis implements JoystickAxis {
        private final Joystick joystick;
        private final int axisId;
        private final String logicalId;

        private TestJoystickAxis(Joystick joystick, int axisId, String logicalId) {
            this.joystick = joystick;
            this.axisId = axisId;
            this.logicalId = logicalId;
        }

        @Override
        public void assignAxis(String positiveMapping, String negativeMapping) {
        }

        @Override
        public Joystick getJoystick() {
            return joystick;
        }

        @Override
        public String getName() {
            return logicalId;
        }

        @Override
        public String getLogicalId() {
            return logicalId;
        }

        @Override
        public int getAxisId() {
            return axisId;
        }

        @Override
        public boolean isAnalog() {
            return true;
        }

        @Override
        public boolean isRelative() {
            return false;
        }

        @Override
        public float getDeadZone() {
            return 0f;
        }
    }

    private static final class TestJoystickButton implements JoystickButton {
        private final Joystick joystick;
        private final int buttonId;

        private TestJoystickButton(Joystick joystick, int buttonId) {
            this.joystick = joystick;
            this.buttonId = buttonId;
        }

        @Override
        public void assignButton(String mappingName) {
        }

        @Override
        public Joystick getJoystick() {
            return joystick;
        }

        @Override
        public String getName() {
            return String.valueOf(buttonId);
        }

        @Override
        public String getLogicalId() {
            return String.valueOf(buttonId);
        }

        @Override
        public int getButtonId() {
            return buttonId;
        }
    }

    private static final class TestSystemDelegate extends JmeSystemDelegate {
        @Override
        public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height)
                throws IOException {
        }

        @Override
        public URL getPlatformAssetConfigURL() {
            return NavigatorLayerTest.class.getResource("/com/jme3/asset/Desktop.cfg");
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
