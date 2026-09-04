/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License.
 */
package org.ngengine.web.input;

import com.jme3.input.AbstractJoystick;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.DefaultJoystickButton;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.virtual.VirtualJoystick;
import com.jme3.input.virtual.VirtualKeyboard;
import com.jme3.math.FastMath;
import com.jme3.system.AppSettings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ngengine.web.WebBinds;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

/** Web joystick backend for both the browser Gamepad API and NGE controls. */
public class WebJoyInput implements JoyInput {
    private static final int VIRTUAL_JOYSTICK_ID = 255;
    private static final int STANDARD_AXIS_COUNT = 6;
    private static final int POV_X_AXIS_ID = 6;
    private static final int POV_Y_AXIS_ID = 7;

    private final AppSettings settings;
    private final Map<Integer, WebJoystick> gamepads = new HashMap<>();
    private final ArrayDeque<InputEvent<?>> events = new ArrayDeque<>();
    private RawInputListener listener;
    private InputManager inputManager;
    private VirtualJoystick virtualJoystick;
    private boolean initialized;

    @SuppressWarnings("rawtypes")
    private final EventListener webListener = new EventListener() {
        @Override
        public void handleEvent(Event event) {
            handleWebEvent(event);
        }
    };

    public WebJoyInput(AppSettings settings) {
        this.settings = settings;
    }

    @Override
    public void initialize() {
        WebBinds.addInputEventListener("gamepadconnected", webListener);
        WebBinds.addInputEventListener("gamepaddisconnected", webListener);
        WebBinds.addInputEventListener("gamepadstate", webListener);
        WebBinds.addInputEventListener("inputreset", webListener);
        WebBinds.refreshGamepads();
        initialized = true;
    }

    @Override
    public void update() {
        InputEvent<?> event;
        while ((event = events.poll()) != null) {
            if (listener == null || handleVirtualKeyboardEvent(event)) {
                continue;
            }
            if (event instanceof JoyAxisEvent) {
                listener.onJoyAxisEvent((JoyAxisEvent) event);
            } else if (event instanceof JoyButtonEvent) {
                listener.onJoyButtonEvent((JoyButtonEvent) event);
            }
        }
        if (virtualJoystick != null) {
            virtualJoystick.dispatchEvents(listener);
        }
    }

    @Override
    public void destroy() {
        WebBinds.removeInputEventListener("gamepadconnected", webListener);
        WebBinds.removeInputEventListener("gamepaddisconnected", webListener);
        WebBinds.removeInputEventListener("gamepadstate", webListener);
        WebBinds.removeInputEventListener("inputreset", webListener);
        if (virtualJoystick != null) {
            virtualJoystick.onPointerCancel(getInputTimeNanos());
        }
        events.clear();
        gamepads.clear();
        inputManager = null;
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
        if (!gamepads.containsKey(joyId)) {
            return;
        }
        int durationMillis;
        if (duration == Float.POSITIVE_INFINITY) {
            durationMillis = 60_000;
        } else if (duration <= 0f) {
            durationMillis = 0;
        } else {
            durationMillis = Math.max(1, Math.round(duration * 1000f));
        }
        WebBinds.setGamepadRumble(joyId, FastMath.clamp(amountHigh, 0f, 1f),
                FastMath.clamp(amountLow, 0f, 1f), durationMillis);
    }

    @Override
    public void stopJoyRumble(int joyId) {
        setJoyRumble(joyId, 0f, 0f, 0f);
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager) {
        this.inputManager = inputManager;
        if (shouldCreateVirtualJoystick()) {
            virtualJoystick = new VirtualJoystick(inputManager, this, VIRTUAL_JOYSTICK_ID);
            virtualJoystick.setLayout(VirtualJoystick.createLayout(settings.getVirtualJoystickDefaultLayout()));
            virtualJoystick.setEnabled(false);
        } else {
            virtualJoystick = null;
        }
        WebBinds.refreshGamepads();
        return currentJoysticks();
    }

    public boolean onPointerDown(int pointerId, float x, float y, long time) {
        return virtualJoystick != null && virtualJoystick.onPointerDown(pointerId, x, y, time);
    }

    public boolean onPointerMove(int pointerId, float x, float y, long time) {
        return virtualJoystick != null && virtualJoystick.onPointerMove(pointerId, x, y, time);
    }

    public boolean onPointerUp(int pointerId, float x, float y, long time) {
        return virtualJoystick != null && virtualJoystick.onPointerUp(pointerId, x, y, time);
    }

    public boolean onPointerCancel(long time) {
        return virtualJoystick != null && virtualJoystick.onPointerCancel(time);
    }

    private void handleWebEvent(Event event) {
        String type = event.getType();
        if ("inputreset".equals(type)) {
            releaseGamepadState();
            return;
        }

        JSGamepadEvent gamepadEvent = (JSGamepadEvent) event;
        int gamepadIndex = gamepadEvent.getGamepadIndex();
        if ("gamepaddisconnected".equals(type)) {
            disconnect(gamepadIndex);
            return;
        }

        WebJoystick joystick = gamepads.get(gamepadIndex);
        if (joystick == null) {
            joystick = connect(gamepadEvent);
        }
        if (joystick != null && "gamepadstate".equals(type)) {
            joystick.applyState(gamepadEvent, getInputTimeNanos(), events);
        }
    }

    private WebJoystick connect(JSGamepadEvent event) {
        if (inputManager == null || gamepads.containsKey(event.getGamepadIndex())) {
            return gamepads.get(event.getGamepadIndex());
        }
        boolean standard = "standard".equals(event.getMapping());
        WebJoystick joystick = new WebJoystick(inputManager, this, event.getGamepadIndex(),
                displayName(event.getGamepadId(), event.getGamepadIndex()), standard,
                event.getAxisCount(), event.getButtonCount());
        gamepads.put(joystick.getJoyId(), joystick);
        inputManager.setJoysticks(currentJoysticks());
        inputManager.fireJoystickConnectedEvent(joystick);
        return joystick;
    }

    private void disconnect(int gamepadIndex) {
        WebJoystick joystick = gamepads.remove(gamepadIndex);
        if (joystick == null) {
            return;
        }
        joystick.releaseAll(getInputTimeNanos(), events);
        if (inputManager != null) {
            inputManager.setJoysticks(currentJoysticks());
            inputManager.fireJoystickDisconnectedEvent(joystick);
        }
    }

    private void releaseGamepadState() {
        long time = getInputTimeNanos();
        for (WebJoystick joystick : gamepads.values()) {
            joystick.releaseAll(time, events);
        }
        if (virtualJoystick != null) {
            virtualJoystick.onPointerCancel(time);
        }
    }

    private boolean handleVirtualKeyboardEvent(InputEvent<?> event) {
        VirtualKeyboard keyboard = VirtualKeyboard.getInstance();
        if (!keyboard.isVisible()) {
            return false;
        }
        if (!(event instanceof JoyButtonEvent)) {
            return true;
        }
        JoyButtonEvent buttonEvent = (JoyButtonEvent) event;
        String id = buttonEvent.getButton().getLogicalId();
        boolean pressed = buttonEvent.isPressed();
        long time = event.getTime();
        if (JoystickButton.BUTTON_XBOX_A.equals(id)) {
            keyboard.action(pressed, time);
        } else if (JoystickButton.BUTTON_XBOX_B.equals(id)) {
            keyboard.cancel(pressed, time);
        } else if (pressed && JoystickButton.BUTTON_XBOX_DPAD_LEFT.equals(id)) {
            keyboard.navigate(-1, 0, time);
        } else if (pressed && JoystickButton.BUTTON_XBOX_DPAD_RIGHT.equals(id)) {
            keyboard.navigate(1, 0, time);
        } else if (pressed && JoystickButton.BUTTON_XBOX_DPAD_UP.equals(id)) {
            keyboard.navigate(0, 1, time);
        } else if (pressed && JoystickButton.BUTTON_XBOX_DPAD_DOWN.equals(id)) {
            keyboard.navigate(0, -1, time);
        }
        return true;
    }

    private Joystick[] currentJoysticks() {
        List<Joystick> current = new ArrayList<>(gamepads.values());
        current.sort(Comparator.comparingInt(Joystick::getJoyId));
        if (virtualJoystick != null) {
            current.add(virtualJoystick);
        }
        return current.toArray(new Joystick[0]);
    }

    private boolean shouldCreateVirtualJoystick() {
        return settings.useJoysticks()
                && AppSettings.VIRTUAL_JOYSTICK_ENABLED.equals(settings.getVirtualJoystickMode());
    }

    private static String displayName(String name, int index) {
        return name == null || name.isEmpty() ? "Web Gamepad " + index : name;
    }

    static String standardButtonLogicalId(int browserButton) {
        switch (browserButton) {
            case 0: return JoystickButton.BUTTON_XBOX_A;
            case 1: return JoystickButton.BUTTON_XBOX_B;
            case 2: return JoystickButton.BUTTON_XBOX_X;
            case 3: return JoystickButton.BUTTON_XBOX_Y;
            case 4: return JoystickButton.BUTTON_XBOX_LB;
            case 5: return JoystickButton.BUTTON_XBOX_RB;
            case 6: return JoystickButton.BUTTON_XBOX_LT;
            case 7: return JoystickButton.BUTTON_XBOX_RT;
            case 8: return JoystickButton.BUTTON_XBOX_BACK;
            case 9: return JoystickButton.BUTTON_XBOX_START;
            case 10: return JoystickButton.BUTTON_XBOX_L3;
            case 11: return JoystickButton.BUTTON_XBOX_R3;
            case 12: return JoystickButton.BUTTON_XBOX_DPAD_UP;
            case 13: return JoystickButton.BUTTON_XBOX_DPAD_DOWN;
            case 14: return JoystickButton.BUTTON_XBOX_DPAD_LEFT;
            case 15: return JoystickButton.BUTTON_XBOX_DPAD_RIGHT;
            default: return String.valueOf(browserButton);
        }
    }

    private static final class WebJoystick extends AbstractJoystick {
        private final Map<Integer, JoystickAxis> axes = new HashMap<>();
        private final Map<Integer, JoystickButton> buttons = new HashMap<>();
        private final float[] axisValues;
        private final float[] buttonValues;
        private final boolean[] buttonPressed;
        private final boolean standard;
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povXAxis;
        private JoystickAxis povYAxis;

        WebJoystick(InputManager inputManager, JoyInput joyInput, int id, String name, boolean standard,
                int browserAxisCount, int browserButtonCount) {
            super(inputManager, joyInput, id, name);
            this.standard = standard;
            int axisCount = standard ? Math.max(STANDARD_AXIS_COUNT, browserAxisCount) : browserAxisCount;
            int buttonCount = standard ? Math.max(16, browserButtonCount) : browserButtonCount;
            axisValues = new float[Math.max(axisCount, POV_Y_AXIS_ID + 1)];
            buttonValues = new float[Math.max(0, buttonCount)];
            buttonPressed = new boolean[Math.max(0, buttonCount)];
            addAxes(inputManager, axisCount);
            addButtons(inputManager, buttonCount);
        }

        void applyState(JSGamepadEvent state, long time, ArrayDeque<InputEvent<?>> target) {
            int directAxisCount = Math.min(state.getAxisCount(), standard ? 4 : axisValues.length);
            for (int axisId = 0; axisId < directAxisCount; axisId++) {
                updateAxis(axisId, FastMath.clamp(state.getAxis(axisId), -1f, 1f), time, target);
            }

            int count = Math.min(state.getButtonCount(), buttonValues.length);
            for (int buttonId = 0; buttonId < count; buttonId++) {
                float value = FastMath.clamp(state.getButtonValue(buttonId), 0f, 1f);
                boolean pressed = state.isButtonPressed(buttonId) || value >= 0.5f;
                updateButton(buttonId, value, pressed, time, target);
            }
            if (standard) {
                updateAxis(4, buttonValues.length > 6 ? buttonValues[6] : 0f, time, target);
                updateAxis(5, buttonValues.length > 7 ? buttonValues[7] : 0f, time, target);
                updatePov(time, target);
            }
        }

        void releaseAll(long time, ArrayDeque<InputEvent<?>> target) {
            for (int axisId = 0; axisId < axisValues.length; axisId++) {
                updateAxis(axisId, 0f, time, target);
            }
            for (int buttonId = 0; buttonId < buttonPressed.length; buttonId++) {
                updateButton(buttonId, 0f, false, time, target);
            }
        }

        private void updateAxis(int axisId, float value, long time, ArrayDeque<InputEvent<?>> target) {
            JoystickAxis axis = axes.get(axisId);
            if (axis == null || Float.compare(axisValues[axisId], value) == 0) {
                return;
            }
            axisValues[axisId] = value;
            JoyAxisEvent event = new JoyAxisEvent(axis, value, value);
            event.setTime(time);
            target.add(event);
        }

        private void updateButton(int buttonId, float value, boolean pressed, long time,
                ArrayDeque<InputEvent<?>> target) {
            if (buttonId < 0 || buttonId >= buttonValues.length) {
                return;
            }
            buttonValues[buttonId] = value;
            if (buttonPressed[buttonId] == pressed) {
                return;
            }
            buttonPressed[buttonId] = pressed;
            JoystickButton button = buttons.get(buttonId);
            if (button != null) {
                JoyButtonEvent event = new JoyButtonEvent(button, pressed);
                event.setTime(time);
                target.add(event);
            }
        }

        private void updatePov(long time, ArrayDeque<InputEvent<?>> target) {
            float x = pressed(15) - pressed(14);
            float y = pressed(12) - pressed(13);
            updateAxis(POV_X_AXIS_ID, x, time, target);
            updateAxis(POV_Y_AXIS_ID, y, time, target);
        }

        private float pressed(int buttonId) {
            return buttonId < buttonPressed.length && buttonPressed[buttonId] ? 1f : 0f;
        }

        private void addAxes(InputManager inputManager, int count) {
            for (int id = 0; id < count; id++) {
                String logicalId = standardAxisLogicalId(id);
                addAxis(inputManager, id, logicalId, logicalId);
            }
            if (standard) {
                addAxis(inputManager, POV_X_AXIS_ID, JoystickAxis.POV_X, JoystickAxis.POV_X);
                addAxis(inputManager, POV_Y_AXIS_ID, JoystickAxis.POV_Y, JoystickAxis.POV_Y);
            }
        }

        private void addButtons(InputManager inputManager, int count) {
            for (int id = 0; id < count; id++) {
                String logicalId = standard ? standardButtonLogicalId(id) : String.valueOf(id);
                JoystickButton button = new DefaultJoystickButton(inputManager, this, id,
                        "Button " + id, logicalId);
                buttons.put(id, button);
                super.addButton(button);
            }
        }

        private void addAxis(InputManager inputManager, int id, String name, String logicalId) {
            if (axes.containsKey(id)) {
                return;
            }
            JoystickAxis axis = new DefaultJoystickAxis(inputManager, this, id, name, logicalId,
                    true, false, 0f);
            axes.put(id, axis);
            super.addAxis(axis);
            if (id == 0) xAxis = axis;
            if (id == 1) yAxis = axis;
            if (id == POV_X_AXIS_ID) povXAxis = axis;
            if (id == POV_Y_AXIS_ID) povYAxis = axis;
        }

        private String standardAxisLogicalId(int id) {
            if (!standard) return String.valueOf(id);
            switch (id) {
                case 0: return JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X;
                case 1: return JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y;
                case 2: return JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X;
                case 3: return JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y;
                case 4: return JoystickAxis.AXIS_XBOX_LEFT_TRIGGER;
                case 5: return JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER;
                default: return String.valueOf(id);
            }
        }

        @Override public JoystickAxis getXAxis() { return xAxis; }
        @Override public JoystickAxis getYAxis() { return yAxis; }
        @Override public JoystickAxis getPovXAxis() { return povXAxis; }
        @Override public JoystickAxis getPovYAxis() { return povYAxis; }
    }
}
