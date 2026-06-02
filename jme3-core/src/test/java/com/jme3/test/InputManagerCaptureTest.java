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
package com.jme3.test;

import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.RawInputListener;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.UnifiedInputListener;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputManagerCaptureTest {

    @Test
    public void ordinaryKeyPressAndReleaseReachUnifiedListener() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> calls = new ArrayList<>();

        inputManager.addMapping("walkRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                calls.add(name + ":" + (value > 0f));
            }
        }, "walkRight");

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);
        keys.queue(key(KeyInput.KEY_D, false));
        inputManager.update(0.016f);

        assertEquals(List.of("walkRight:true", "walkRight:false"), calls);
    }

    @Test
    public void ordinaryKeyPressAndReleaseReachActionListener() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> calls = new ArrayList<>();

        inputManager.addMapping("walkRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener((ActionListener) (name, pressed, tpf) ->
                calls.add(name + ":" + pressed), "walkRight");

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);
        keys.queue(key(KeyInput.KEY_D, false));
        inputManager.update(0.016f);

        assertEquals(List.of("walkRight:true", "walkRight:false"), calls);
    }

    @Test
    public void consumedUnifiedEventStopsLaterMappings() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> calls = new ArrayList<>();

        inputManager.addMapping("game", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                calls.add("game:" + name);
            }
        }, "game");

        inputManager.addMapping("ui", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                calls.add("ui:" + name);
                event.setConsumed();
            }
        }, "ui");

        keys.queue(key(KeyInput.KEY_A, true));
        inputManager.update(0.016f);

        assertEquals(List.of("ui:ui"), calls);
    }

    @Test
    public void consumedDigitalInputDoesNotEmulateAnalogToLaterMappings() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> calls = new ArrayList<>();

        inputManager.addMapping("game", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name + ":" + toggled), "game");

        inputManager.addMapping("ui", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            calls.add("ui:" + name + ":" + toggled);
            event.setConsumed();
        }, "ui");

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);
        inputManager.update(0.016f);

        assertEquals(List.of("ui:ui:true"), calls);
    }

    @Test
    public void consumedDigitalInputStillReturnsReleaseToConsumerOnly() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> calls = new ArrayList<>();

        inputManager.addMapping("game", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name + ":" + toggled + ":" + (value > 0f)), "game");

        inputManager.addMapping("ui", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            calls.add("ui:" + name + ":" + toggled + ":" + (value > 0f));
            event.setConsumed();
        }, "ui");

        keys.queue(key(KeyInput.KEY_SPACE, true));
        inputManager.update(0.016f);
        keys.queue(key(KeyInput.KEY_SPACE, false));
        inputManager.update(0.016f);

        assertEquals(List.of(
            "ui:ui:true:true",
            "ui:ui:true:false"
        ), calls);
    }

    @Test
    public void consumedJoyButtonInputStillReturnsReleaseToConsumerOnly() {
        TestKeyInput keys = new TestKeyInput();
        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(keys, joyInput);
        List<String> calls = new ArrayList<>();

        TestJoystickButton button = new TestJoystickButton(joystick, 1);
        JoyButtonTrigger trigger = new JoyButtonTrigger(joystick.getJoyId(), button.getButtonId());
        inputManager.addMapping("game", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name + ":" + toggled + ":" + (value > 0f)), "game");

        inputManager.addMapping("ui", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            calls.add("ui:" + name + ":" + toggled + ":" + (value > 0f));
            event.setConsumed();
        }, "ui");

        joyInput.queue(new JoyButtonEvent(button, true));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(button, false));
        inputManager.update(0.016f);

        assertEquals(List.of(
            "ui:ui:true:true",
            "ui:ui:true:false"
        ), calls);
    }

    @Test
    public void joyButtonReleaseAfterUiMappingDeletedDoesNotReachGameMapping() {
        TestKeyInput keys = new TestKeyInput();
        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(keys, joyInput);
        List<String> calls = new ArrayList<>();

        TestJoystickButton button = new TestJoystickButton(joystick, 1);
        JoyButtonTrigger trigger = new JoyButtonTrigger(joystick.getJoyId(), button.getButtonId());
        inputManager.addMapping("game", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name + ":" + toggled + ":" + (value > 0f)), "game");

        inputManager.addMapping("ui", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            calls.add("ui:" + name + ":" + toggled + ":" + (value > 0f));
            event.setConsumed();
        }, "ui");

        joyInput.queue(new JoyButtonEvent(button, true));
        inputManager.update(0.016f);
        inputManager.deleteMapping("ui");
        joyInput.queue(new JoyButtonEvent(button, false));
        inputManager.update(0.016f);

        assertEquals(List.of("ui:ui:true:true"), calls);
    }

    @Test
    public void consumedAxisEventStopsLaterMappings() {
        TestKeyInput keys = new TestKeyInput();
        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(keys, joyInput);
        List<String> calls = new ArrayList<>();

        TestJoystickAxis axis = new TestJoystickAxis(joystick, 0);
        JoyAxisTrigger trigger = new JoyAxisTrigger(joystick.getJoyId(), axis.getAxisId(), false);
        inputManager.addMapping("game", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name), "game");

        inputManager.addMapping("ui", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            calls.add("ui:" + name);
            event.setConsumed();
        }, "ui");

        joyInput.queue(new JoyAxisEvent(axis, 1f));
        inputManager.update(0.016f);

        assertEquals(List.of("ui:ui"), calls);
    }

    @Test
    public void gameReceivesAxisAgainAfterUiMappingIsDeletedWithoutFalseRelease() {
        TestKeyInput keys = new TestKeyInput();
        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(keys, joyInput);
        List<String> calls = new ArrayList<>();

        TestJoystickAxis axis = new TestJoystickAxis(joystick, 0);
        JoyAxisTrigger trigger = new JoyAxisTrigger(joystick.getJoyId(), axis.getAxisId(), false);
        inputManager.addMapping("game", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name + ":" + value), "game");

        inputManager.addMapping("ui", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            calls.add("ui:" + name + ":" + value);
            event.setConsumed();
        }, "ui");

        joyInput.queue(new JoyAxisEvent(axis, 1f));
        inputManager.update(0.016f);
        inputManager.deleteMapping("ui");
        inputManager.releaseActiveMappings();
        joyInput.queue(new JoyAxisEvent(axis, 1f));
        inputManager.update(0.016f);

        assertEquals(List.of(
            "ui:ui:1.0",
            "game:game:1.0"
        ), calls);
    }

    @Test
    public void rawAxisDeviceSwitchCanCaptureSameQueuedAxisEvent() {
        TestKeyInput keys = new TestKeyInput();
        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(keys, joyInput);
        List<String> calls = new ArrayList<>();

        TestJoystickAxis axis = new TestJoystickAxis(joystick, 0);
        JoyAxisTrigger trigger = new JoyAxisTrigger(joystick.getJoyId(), axis.getAxisId(), false);
        inputManager.addMapping("game", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name + ":" + value), "game");

        inputManager.addRawInputListener(new RawInputListenerAdapter() {
            private boolean uiRegistered;

            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {
                if (uiRegistered) {
                    return;
                }
                uiRegistered = true;
                inputManager.addMapping("ui", trigger);
                inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
                    calls.add("ui:" + name + ":" + value);
                    event.setConsumed();
                }, "ui");
            }
        });

        joyInput.queue(new JoyAxisEvent(axis, 1f));
        inputManager.update(0.016f);

        assertEquals(List.of("ui:ui:1.0"), calls);
    }

    @Test
    public void releaseActiveMappingsReleasesGameMappingThatWasActiveBeforeUiCaptured() {
        TestKeyInput keys = new TestKeyInput();
        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(keys, joyInput);
        List<String> calls = new ArrayList<>();

        TestJoystickAxis axis = new TestJoystickAxis(joystick, 0);
        JoyAxisTrigger trigger = new JoyAxisTrigger(joystick.getJoyId(), axis.getAxisId(), false);
        inputManager.addMapping("game", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) ->
                calls.add("game:" + name + ":" + value), "game");

        joyInput.queue(new JoyAxisEvent(axis, 1f));
        inputManager.update(0.016f);

        inputManager.addMapping("ui", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            calls.add("ui:" + name + ":" + value);
            event.setConsumed();
        }, "ui");
        inputManager.releaseActiveMappings();

        assertEquals(List.of(
            "game:game:1.0",
            "game:game:0.0"
        ), calls);
    }

    @Test
    public void deleteMappingRemovesMappingName() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);

        inputManager.addMapping("ui", new KeyTrigger(KeyInput.KEY_SPACE));
        assertTrue(inputManager.hasMapping("ui"));

        inputManager.deleteMapping("ui");

        assertFalse(inputManager.hasMapping("ui"));
    }

    @Test
    public void deleteMappingClearsStaleActiveInputWhenNoMappingsRemain() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> calls = new ArrayList<>();

        inputManager.addMapping("walk", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                calls.add("old:" + (value > 0f));
            }
        }, "walk");

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);
        inputManager.deleteMapping("walk");

        inputManager.addMapping("walk", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                calls.add("new:" + (value > 0f));
            }
        }, "walk");
        inputManager.releaseActiveMappings();

        assertEquals(List.of("old:true"), calls);
    }

    @Test
    public void deleteTriggerClearsStaleActiveInputWhenNoMappingsRemain() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> calls = new ArrayList<>();

        KeyTrigger trigger = new KeyTrigger(KeyInput.KEY_D);
        inputManager.addMapping("walk", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                calls.add("old:" + (value > 0f));
            }
        }, "walk");

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);
        inputManager.deleteTrigger("walk", trigger);

        inputManager.addMapping("walk", trigger);
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                calls.add("new:" + (value > 0f));
            }
        }, "walk");
        inputManager.releaseActiveMappings();

        assertEquals(List.of("old:true"), calls);
    }

    @Test
    public void releaseActiveMappingsDispatchesReleaseForPressedMappings() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<Boolean> pressedStates = new ArrayList<>();

        inputManager.addMapping("walk", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                pressedStates.add(value > 0f);
            }
        }, "walk");

        keys.queue(key(KeyInput.KEY_D, true));
        inputManager.update(0.016f);

        inputManager.releaseActiveMappings();

        assertEquals(List.of(true, false), pressedStates);
    }

    @Test
    public void releaseActiveMappingsDispatchesReleaseForAllPressedMappings() {
        TestKeyInput keys = new TestKeyInput();
        InputManager inputManager = newInputManager(keys);
        List<String> states = new ArrayList<>();

        inputManager.addMapping("walkRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("walkDown", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                states.add(name + ":" + (value > 0f));
            }
        }, "walkRight", "walkDown");

        keys.queue(key(KeyInput.KEY_D, true));
        keys.queue(key(KeyInput.KEY_S, true));
        inputManager.update(0.016f);

        inputManager.releaseActiveMappings();

        assertEquals(List.of(
            "walkRight:true",
            "walkDown:true",
            "walkRight:false",
            "walkDown:false"
        ), states);
    }

    @Test
    public void releaseActiveMappingsDispatchesReleaseForAxisMappings() {
        TestKeyInput keys = new TestKeyInput();
        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(keys, joyInput);
        List<Float> values = new ArrayList<>();

        TestJoystickAxis axis = new TestJoystickAxis(joystick, 0);
        inputManager.addMapping("walkRight", new JoyAxisTrigger(joystick.getJoyId(), axis.getAxisId(), false));
        inputManager.addListener((UnifiedInputListener) (name, toggled, value, event, tpf) -> {
            if (toggled) {
                values.add(value);
            }
        }, "walkRight");

        joyInput.queue(new JoyAxisEvent(axis, 1f));
        inputManager.update(0.016f);
        inputManager.releaseActiveMappings();

        assertEquals(List.of(1f, 0f), values);
    }

    private static InputManager newInputManager(TestKeyInput keys) {
        return newInputManager(keys, null);
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
                if (event instanceof JoyAxisEvent) {
                    listener.onJoyAxisEvent((JoyAxisEvent) event);
                } else if (event instanceof JoyButtonEvent) {
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
            return null;
        }

        @Override
        public JoystickButton getButton(String logicalId) {
            return null;
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
        private final int axisId;

        private TestJoystickAxis(Joystick joystick, int axisId) {
            this.joystick = joystick;
            this.axisId = axisId;
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
            return "button-" + buttonId;
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
}
