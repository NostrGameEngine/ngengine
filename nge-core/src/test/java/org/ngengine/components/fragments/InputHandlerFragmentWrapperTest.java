/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.components.fragments;

import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
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

    private static final class RecordingInputComponent implements Component, InputHandlerFragment {
        private final ArrayList<String> actions = new ArrayList<>();
        private int connectedDevices;
        private int rawKeyEvents;

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
}
