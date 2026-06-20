/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.components.fragments;

import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.virtual.VirtualJoystick;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.AppComponentInitializer.InputActions;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputHandlerFragmentWrapperTest {

    @Test
    public void wrapperBindsKeyboardOnRawDeviceConnectAndDispatchesPressRelease() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys, null);
        RecordingInputComponent component = new RecordingInputComponent();
        ComponentManager manager = componentManager(inputManager, component, true);
        InputHandlerFragment.Wrapper wrapper = new InputHandlerFragment.Wrapper(
                manager,
                component,
                new InputActions(inputManager));
        inputManager.addRawInputListener(wrapper);

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);
        keys.queue(key(KeyInput.KEY_D, false));
        inputManager.update(0.016f);

        assertEquals(1, component.connectedDevices);
        assertEquals(2, component.rawKeyEvents);
        assertEquals(List.of("walk_right:true", "walk_right:false"), component.actions);
    }

    @Test
    public void wrapperDoesNotDispatchWhenComponentIsDisabled() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys, null);
        RecordingInputComponent component = new RecordingInputComponent();
        ComponentManager manager = componentManager(inputManager, component, false);
        InputHandlerFragment.Wrapper wrapper = new InputHandlerFragment.Wrapper(
                manager,
                component,
                new InputActions(inputManager));
        inputManager.addRawInputListener(wrapper);

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);

        assertEquals(0, component.connectedDevices);
        assertEquals(0, component.rawKeyEvents);
        assertEquals(List.of(), component.actions);
        assertFalse(inputManager.hasMapping("walk_right"));
    }

    @Test
    public void joystickAxisOnlyConnectsDeviceWhenMovedPastIntentThreshold() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys, null);
        RecordingInputComponent component = new RecordingInputComponent();
        ComponentManager manager = componentManager(inputManager, component, true);
        InputHandlerFragment.Wrapper wrapper = new InputHandlerFragment.Wrapper(
                manager,
                component,
                new InputActions(inputManager));
        TestJoystick joystick = new TestJoystick();

        wrapper.onJoyAxisEvent(new JoyAxisEvent(joystick.getXAxis(), 0.4f));

        assertEquals(0, component.connectedDevices);
        assertEquals(0, component.rawAxisEvents);

        wrapper.onJoyAxisEvent(new JoyAxisEvent(joystick.getXAxis(), 0.91f));

        assertEquals(1, component.connectedDevices);
        assertEquals(1, component.rawAxisEvents);

        wrapper.onJoyAxisEvent(new JoyAxisEvent(joystick.getXAxis(), 0.2f));

        assertEquals(1, component.connectedDevices);
        assertEquals(2, component.rawAxisEvents);
    }

    @Test
    public void virtualJoystickConnectsBeforeCheckingInputBindings() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys, null);
        VirtualJoystick joystick = new VirtualJoystick(inputManager, new TestJoyInput(), 0);
        inputManager.setJoysticks(new Joystick[] { joystick });
        VirtualJoystickBindingComponent component = new VirtualJoystickBindingComponent();
        ComponentManager manager = componentManager(inputManager, component, true);
        InputHandlerFragment.Wrapper wrapper = new InputHandlerFragment.Wrapper(
                manager,
                component,
                new InputActions(inputManager));

        assertFalse(joystick.hasInputBindings());

        wrapper.beginInput();

        assertEquals(1, component.connectedDevices);
        assertTrue(joystick.hasInputBindings());
        assertTrue(joystick.isEnabled());
    }

    @Test
    public void nonControllingFragmentsDoNotDisableVirtualJoystick() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys, null);
        VirtualJoystick joystick = new VirtualJoystick(inputManager, new TestJoyInput(), 0);
        inputManager.setJoysticks(new Joystick[] { joystick });
        VirtualJoystickBindingComponent gameplay = new VirtualJoystickBindingComponent();
        NonControllingInputComponent overlay = new NonControllingInputComponent();
        InputHandlerFragment.Wrapper gameplayWrapper = new InputHandlerFragment.Wrapper(
                componentManager(inputManager, gameplay, true),
                gameplay,
                new InputActions(inputManager));
        InputHandlerFragment.Wrapper overlayWrapper = new InputHandlerFragment.Wrapper(
                componentManager(inputManager, overlay, true),
                overlay,
                new InputActions(inputManager));

        gameplayWrapper.beginInput();
        assertTrue(joystick.isEnabled());

        overlayWrapper.beginInput();

        assertTrue(joystick.isEnabled());
        assertEquals(0, overlay.connectedDevices);
    }

    private static ComponentManager componentManager(InputManager inputManager, Component component, boolean enabled) {
        return (ComponentManager) Proxy.newProxyInstance(
                InputHandlerFragmentWrapperTest.class.getClassLoader(),
                new Class<?>[] { ComponentManager.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getInstanceOf".equals(name) && args != null && args.length == 1 && args[0] == InputManager.class) {
                        return inputManager;
                    }
                    if ("isComponentEnabled".equals(name) && args != null && args.length == 1 && args[0] == component) {
                        return enabled;
                    }
                    if ("getComponent".equals(name) && args != null && args.length == 1 && args[0] == component.getClass()) {
                        return component;
                    }
                    if ("hasComponent".equals(name)) {
                        return false;
                    }
                    if ("getAllComponents".equals(name) || "getComponentsBySlot".equals(name)
                            || "getUpdaters".equals(name) || "getInitializers".equals(name)
                            || "getLoaders".equals(name)) {
                        return List.of();
                    }
                    if ("getParent".equals(name) || "getDataStoreProvider".equals(name)
                            || "getSettings".equals(name) || "getRunner".equals(name)
                            || "getComponentById".equals(name)) {
                        return null;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == void.class) {
                        return null;
                    }
                    return null;
                });
    }

    private static InputManager newInputManager(TestKeyInput keys, JoyInput joyInput) {
        DummyMouseInput mouse = new DummyMouseInput();
        keys.initialize();
        mouse.initialize();
        if (joyInput != null) {
            joyInput.initialize();
        }
        return new InputManager(mouse, keys, joyInput, null);
    }

    private static KeyInputEvent key(int keyCode, boolean pressed) {
        KeyInputEvent event = new KeyInputEvent(keyCode, '\0', pressed, false);
        event.setTime(System.nanoTime());
        return event;
    }

    private static class RecordingInputComponent implements Component, InputHandlerFragment {
        protected final ArrayList<String> actions = new ArrayList<>();
        protected int connectedDevices;
        protected int rawKeyEvents;
        protected int rawAxisEvents;

        @Override
        public void onInputDeviceConnected(ComponentManager mng, InputManager inputManager,
                InputActions inputActions, com.jme3.input.InputDevice device) {
            connectedDevices++;
            inputActions.bind("walk_right", new KeyTrigger(KeyInput.KEY_D));
        }

        @Override
        public void onInputDeviceDisconnected(ComponentManager mng, InputManager inputManager,
                InputActions inputActions, com.jme3.input.InputDevice device) {
        }

        @Override
        public void onKeyEvent(ComponentManager mng, KeyInputEvent evt) {
            rawKeyEvents++;
        }

        @Override
        public void onJoyAxisEvent(ComponentManager mng, JoyAxisEvent evt) {
            rawAxisEvents++;
        }

        @Override
        public void onInputAction(ComponentManager mng, String action, boolean toggled,
                float value, InputEvent<?> event, float tpf) {
            if (toggled) {
                actions.add(action + ":" + (value > 0f));
            }
        }

        @Override
        public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime) {
        }

        @Override
        public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        }

        @Override
        public Component newInstance() {
            return new RecordingInputComponent();
        }

        @Override
        public ComponentManager getComponentManager() {
            return null;
        }
    }

    private static final class VirtualJoystickBindingComponent extends RecordingInputComponent {

        @Override
        public boolean showOnScreenJoystick(ComponentManager mng, Joystick[] joysticks) {
            return true;
        }

        @Override
        public void onInputDeviceConnected(ComponentManager mng, InputManager inputManager,
                InputActions inputActions, com.jme3.input.InputDevice device) {
            super.onInputDeviceConnected(mng, inputManager, inputActions, device);
            if (device instanceof VirtualJoystick) {
                Joystick joystick = (Joystick) device;
                inputActions.bind("move_right", new JoyAxisTrigger(
                        joystick.getJoyId(),
                        joystick.getXAxis().getAxisId(),
                        false));
            }
        }
    }

    private static final class NonControllingInputComponent extends RecordingInputComponent {

        @Override
        public boolean controlsOnScreenJoystick(ComponentManager mng, Joystick[] joysticks) {
            return false;
        }

        @Override
        public boolean showOnScreenJoystick(ComponentManager mng, Joystick[] joysticks) {
            return false;
        }
    }

    private static final class TestJoyInput implements JoyInput {
        private boolean initialized;

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void update() {
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
            return new Joystick[0];
        }
    }

    private static final class TestKeyInput extends DummyKeyInput {
        private final Queue<KeyInputEvent> events = new ArrayDeque<>();
        private RawInputListener listener;

        private void queue(KeyInputEvent event) {
            events.add(event);
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        @Override
        public void update() {
            super.update();
            assertTrue(isInitialized());
            while (!events.isEmpty()) {
                InputEvent<?> event = events.remove();
                assertFalse(event.isConsumed());
                listener.onKeyEvent((KeyInputEvent) event);
            }
        }
    }

    private static final class TestJoystick implements Joystick {
        private final TestJoystickAxis xAxis = new TestJoystickAxis(this);

        @Override
        public int getJoyId() {
            return 0;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public JoystickAxis getXAxis() {
            return xAxis;
        }

        @Override
        public JoystickAxis getYAxis() {
            return xAxis;
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return xAxis;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return xAxis;
        }

        @Override
        public JoystickAxis getAxis(String logicalId) {
            return xAxis;
        }

        @Override
        public List<JoystickAxis> getAxes() {
            return List.of(xAxis);
        }

        @Override
        public JoystickButton getButton(String logicalId) {
            return null;
        }

        @Override
        public List<JoystickButton> getButtons() {
            return List.of();
        }

        @Override
        public int getXAxisIndex() {
            return 0;
        }

        @Override
        public int getYAxisIndex() {
            return 0;
        }

        @Override
        public int getAxisCount() {
            return 1;
        }

        @Override
        public int getButtonCount() {
            return 0;
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
    }

    private static final class TestJoystickAxis implements JoystickAxis {
        private final Joystick joystick;

        private TestJoystickAxis(Joystick joystick) {
            this.joystick = joystick;
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
            return "x";
        }

        @Override
        public String getLogicalId() {
            return JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X;
        }

        @Override
        public int getAxisId() {
            return 0;
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
}
