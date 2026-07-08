/**
 * Copyright (c) 2026, Nostr Game Engine
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
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package org.ngengine.gui.nav;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.ngengine.gui.Axis;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.Slider;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.Keyboard;
import com.jme3.input.Mouse;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.TouchInput;
import com.jme3.input.TouchScreen;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector2f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

public class DefaultNavigatorInputHandler implements NavigatorInputHandler {
    private WeakReference<ViewPort> guiViewPort;
    private boolean consume = true;
    private String bindId = "";
    private InputManager inputManager;
    private final Set<String> mappings = new HashSet<>();
    private InputDevice inputDevice;
    private Runnable primaryAction = () -> {};
    private Runnable secondaryAction = () -> {};
    private Runnable backAction = () -> {};
    private double cursorX, cursorY;
    private double lastMotionX = Double.NaN, lastMotionY = Double.NaN;
    private MouseMotionEvent lastCursorMotionEvent;
    private Spatial pointerActionTarget;
    private boolean pointerActionPressed;
    private boolean navigatorActionPressed = false;
    private boolean consumeNextJoystickPress = false;
    private final Set<String> activeAxisActions = new HashSet<>();
    private static final float AXIS_ACTION_PRESS = 0.45f;
    private static final float AXIS_ACTION_RELEASE = 0.20f;
    private static final float AXIS_DOMINANCE_MARGIN = 0.08f;
    private float leftStickX;
    private float leftStickY;
    private float simulatedCursorX;
    private float simulatedCursorY;
    private final RawInputListenerAdapter rawMouseListener = new RawInputListenerAdapter() {
        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            handleRawMouseMotion(evt);
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            handleRawMouseButton(evt);
        }
    };
    

    public DefaultNavigatorInputHandler(ViewPort guiViewPort) {
        setViewPort(guiViewPort);
    }

    public DefaultNavigatorInputHandler() {
    }

    public void setConsumeEvents(boolean consume) {
        this.consume = consume;
    }

    @Override
    public void setPrimaryAction(Runnable action) {
        this.primaryAction = action;
    }

    @Override
    public void setSecondaryAction(Runnable action) {
        this.secondaryAction = action;
    }

    @Override
    public void setBackAction(Runnable action) {
        this.backAction = action;
    }

    public boolean isConsumeEvents() {
        return consume;
    }

    private void consume(InputEvent<?> event) {
        if (consume && event != null) {
            event.setConsumed();
        }
    }

    private boolean closeOpenIme(InputEvent<?> event) {
        GuiContext ctx = NGEGui.get(getViewPort());
        if (ctx == null || !ctx.getImeComposer().isOpen()) {
            return false;
        }
        ctx.getImeComposer().close();
        consume(event);
        return true;
    }

    /**
     * Returns the ViewPort this input handler is affecting, or null if not registered to any or
     * the ViewPort has been garbage collected.
     */
    @Nullable
    public ViewPort getViewPort() {
        if(guiViewPort==null)return null;
        return guiViewPort.get();
    }

    /**
     * Set the ViewPort this input handler should affect.
     */
    @Override
    public void setViewPort(ViewPort vp) {
        if (this.inputManager != null) {
            InputDevice currentDevice = this.inputDevice;
            clearActiveMappings(this.inputManager);
            this.inputDevice = null;
            this.guiViewPort = new WeakReference<>(vp);
            this.bindId = String.valueOf(vp.hashCode());
            registerListener(this.inputManager);
            if (currentDevice != null) {
                setInputDevice(this.inputManager, currentDevice);
            }
        } else {
            this.guiViewPort = new WeakReference<>(vp);
            this.bindId = String.valueOf(vp.hashCode());
        }
        GuiContext state = getGuiContextIfRegistered(vp);
        if (state != null) {
            cursorX = state.getLogicalWidth() / 2.0;
            cursorY = state.getLogicalHeight() / 2.0;
        } else {
            cursorX = vp.getCamera().getWidth() / 2.0;
            cursorY = vp.getCamera().getHeight() / 2.0;
        }

    }

    private String _p(String action) {
        return "ui:" + action + ":" + bindId;
    }

    private String _ps(String action) {
        String p = _p(action);
        mappings.add(p);
        return p;
    }

    @Override
    public void registerListener(InputManager inputManager) {
        this.inputManager = inputManager;
        inputManager.removeListener(this);
        inputManager.removeRawInputListener(rawMouseListener);
        inputManager.addRawInputListener(rawMouseListener);
        bindActiveMappings();
    }

    @Override
    public void unregisterListener(InputManager inputManager) {
        if (inputManager != null) {
            inputManager.removeListener(this);
            inputManager.removeRawInputListener(rawMouseListener);
            clearActiveMappings(inputManager);
        }
        this.inputManager = null;
        this.inputDevice = null;
        navigatorActionPressed = false;
        consumeNextJoystickPress = false;
        activeAxisActions.clear();
        lastCursorMotionEvent = null;
        pointerActionTarget = null;
        pointerActionPressed = false;
        leftStickX = 0f;
        leftStickY = 0f;
        simulatedCursorX = 0f;
        simulatedCursorY = 0f;
    }

    @Override
    public void setInputDevice(InputManager inputManager, @Nullable InputDevice device) {
        ViewPort vp = getViewPort();
        GuiContext state = vp == null ? null : NGEGui.get(vp);
        if (state != null) {
            state.setInputDevice(device);
        }
        if (this.inputDevice == device) {
            this.inputManager = inputManager;
            if (device == null) {
                navigatorActionPressed = false;
                consumeNextJoystickPress = false;
                activeAxisActions.clear();
                lastCursorMotionEvent = null;
                pointerActionTarget = null;
                pointerActionPressed = false;
                leftStickX = 0f;
                leftStickY = 0f;
                simulatedCursorX = 0f;
                simulatedCursorY = 0f;
                clearActiveMappings(inputManager);
                return;
            } else if (!mappings.isEmpty()) {
                return;
            }
        }
        InputDevice previousDevice = this.inputDevice;
        navigatorActionPressed = false;
        activeAxisActions.clear();
        lastCursorMotionEvent = null;
        pointerActionTarget = null;
        pointerActionPressed = false;
        leftStickX = 0f;
        leftStickY = 0f;
        simulatedCursorX = 0f;
        simulatedCursorY = 0f;
        clearActiveMappings(inputManager);
        
        this.inputManager = inputManager;
        this.inputDevice = device;
        boolean hasFocus = state != null && state.getNavigator() != null && state.getNavigator().peekFocus() != null;
        consumeNextJoystickPress = previousDevice != null && previousDevice != device && device instanceof Joystick && !hasFocus;

        if (device == null) {
            return;
        } else if (device instanceof Keyboard || device instanceof Mouse) {
            // mouse and keyboard
            inputManager.addMapping(_ps("navigateUp"), new KeyTrigger(KeyInput.KEY_UP));
            inputManager.addMapping(_ps("navigateDown"), new KeyTrigger(KeyInput.KEY_DOWN));
            inputManager.addMapping(_ps("navigateLeft"), new KeyTrigger(KeyInput.KEY_LEFT));
            inputManager.addMapping(_ps("navigateRight"), new KeyTrigger(KeyInput.KEY_RIGHT));
            inputManager.addMapping(_ps("navigateNext"), new KeyTrigger(KeyInput.KEY_TAB));
            inputManager.addMapping(_ps("confirm"), new KeyTrigger(KeyInput.KEY_RETURN));

            inputManager.addMapping(_ps("primaryAction"), new KeyTrigger(KeyInput.KEY_RETURN));

            inputManager.addMapping(_ps("secondaryAction"), new KeyTrigger(KeyInput.KEY_P));
            inputManager.addMapping(_ps("back"),
                    new KeyTrigger(KeyInput.KEY_ESCAPE),
                    new KeyTrigger(KeyInput.KEY_BACK));

            if (device instanceof Mouse) {
                if (inputManager.isCursorVisible()) {
                    Vector2f pos = inputManager.getCursorPosition();
                    if (state != null) {
                        cursorX = state.toGuiX(pos.x);
                        cursorY = state.toGuiY(pos.y);
                    } else {
                        cursorX = pos.x;
                        cursorY = pos.y;
                    }
                }
                clampCursorToViewPort();
            } else {
                autofocus();
            }
            bindActiveMappings();
        } else if (device instanceof TouchScreen) {
            inputManager.addMapping(_ps("touch"), new TouchTrigger(TouchInput.ALL));
            bindActiveMappings();
        } else if (device instanceof Joystick) {
            Joystick joy = (Joystick) device;
            inputManager.addMapping(_ps("navigateUp"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_DPAD_UP),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y, true));
            inputManager.addMapping(_ps("navigateDown"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_DPAD_DOWN),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y, false));
            inputManager.addMapping(_ps("navigateLeft"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_DPAD_LEFT),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, true));
            inputManager.addMapping(_ps("navigateRight"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_DPAD_RIGHT),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, false));
            inputManager.addMapping(_ps("navigateNext"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_R3));
            inputManager.addMapping(_ps("navigatePrevious"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_L3));
            inputManager.addMapping(_ps("scrollLeft"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_LB));
            inputManager.addMapping(_ps("scrollRight"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_RB));

            inputManager.addMapping(_ps("scrollUp"),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y, true));
            inputManager.addMapping(_ps("scrollDown"),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y, false));

            inputManager.addMapping(_ps("confirm"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_A),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_RT));

            inputManager.addMapping(_ps("primaryAction"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_A),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_RT));

            inputManager.addMapping(_ps("back"), new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_B));

            inputManager.addMapping(_ps("secondaryAction"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_BACK),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_LT));

            if (!consumeNextJoystickPress) {
                autofocus();
            }
            bindActiveMappings();
        }
    }

    private void bindActiveMappings() {
        if (inputManager == null || mappings.isEmpty()) {
            return;
        }
        inputManager.addListener(this, mappings.toArray(new String[mappings.size()]));
    }

    private void clearActiveMappings(InputManager inputManager) {
        if (inputManager == null || mappings.isEmpty()) {
            return;
        }
        for (String mapping : new HashSet<>(mappings)) {
            try {
                if (inputManager.hasMapping(mapping)) {
                    inputManager.deleteMapping(mapping);
                }
            } catch (Exception e) {
            }
        }
        mappings.clear();
    }

    @Override
    public void update(float tpf) {
        ViewPort vp = getViewPort();
        if (vp == null) return;
        GuiContext state = NGEGui.get(vp);
        if (state == null) return;
        Navigator navigator = state.getNavigator();
        if (navigator == null || !navigator.isSimulateCursor()) return;
        float deadZone = navigator.getSimulatedCursorDeadZone();
        if (Math.abs(simulatedCursorX) < deadZone && Math.abs(simulatedCursorY) < deadZone) {
            return;
        }

        double step = navigator.getSimulatedCursorSpeed() * Math.max(tpf, 0f);
        if (state.isRelativeSize()) {
            step = state.toGuiDistance(step);
        }
        double dx = simulatedCursorX * step;
        double dy = simulatedCursorY * step;
        double threshold = state.toGuiDistance(navigator.getCursorActivityThreshold());
        if (dx * dx + dy * dy < threshold * threshold) {
            return;
        }
        cursorX += dx;
        cursorY += dy;
        clampCursorToViewPort();
        if (navigator.updateSimulatedCursorPosition(cursorX, cursorY)) {
            updatePointerFocusAt(state, navigator, cursorX, cursorY);
        }
    }

    private void autofocus() {
        ViewPort vp = getViewPort();
        if (vp == null) return;
        GuiContext state = NGEGui.get(vp);
        if (state == null || state.getNavigator() == null) return;
        state.getNavigator().autofocus();
    }

    private GuiContext getGuiContextIfRegistered(ViewPort vp) {
        return NGEGui.isRegistered(vp) ? NGEGui.get(vp) : null;
    }

    private void clampCursorToViewPort() {
        ViewPort vp = getViewPort();
        if (vp == null) return;
        GuiContext state = getGuiContextIfRegistered(vp);
        double width = state != null ? state.getLogicalWidth() : vp.getCamera().getWidth();
        double height = state != null ? state.getLogicalHeight() : vp.getCamera().getHeight();
        if (cursorX < 0) cursorX = 0;
        if (cursorY < 0) cursorY = 0;
        if (cursorX > width) cursorX = width;
        if (cursorY > height) cursorY = height;
    }

    @Override
    public void onUnifiedInput(String name, boolean toggled, float value, InputEvent<?> event, float tpf) {
        boolean isPressed = value > 0;
        if(inputDevice == null)return;

        ViewPort vp = getViewPort();
        if(vp==null)return;
        
        GuiContext state = NGEGui.get(vp);
        if (state == null) return;
        
        Navigator navigator = state.getNavigator();
        if (navigator == null) return;

        if (handleTouchInput(state, navigator, name, event)) {
            return;
        }

        if (handleSimulatedCursorAxis(state, navigator, name, value, event, tpf)) {
            return;
        }

        if (event instanceof JoyAxisEvent && isAxisAction(name)) {
            updateStickAxisState(name, value);
            if (value < AXIS_ACTION_RELEASE) {
                activeAxisActions.remove(name);
                return;
            }
            if (value < AXIS_ACTION_PRESS || !isDominantAxisAction(name) || !activeAxisActions.add(name)) {
                consume(event);
                return;
            }
            toggled = true;
            isPressed = true;
        }

        if (_p("back").equals(name) && toggled && isPressed && closeOpenIme(event)) {
            return;
        }

        if (_p("back").equals(name) && toggled && isPressed && event instanceof JoyButtonEvent
                && navigator.getFocus() instanceof Slider) {
            navigator.clearPointerFocus();
            consume(event);
            return;
        }

        if (consumeNextJoystickPress && event != null && event.getDevice() == inputDevice && toggled && isPressed) {
            consumeNextJoystickPress = false;
            navigator.autofocus();
            consume(event);
            return;
        }

        if (_p("confirm").equals(name) && toggled && event instanceof MouseButtonEvent) {
            if (isPressed) {
                MouseButtonEvent me = (MouseButtonEvent) event;
                double x = inputManager != null && !inputManager.isCursorVisible() ? cursorX : state.toGuiX(me.getX());
                double y = inputManager != null && !inputManager.isCursorVisible() ? cursorY : state.toGuiY(me.getY());
                pointerPressed(state, navigator, x, y);
            } else {
                MouseButtonEvent me = (MouseButtonEvent) event;
                double x = inputManager != null && !inputManager.isCursorVisible() ? cursorX : state.toGuiX(me.getX());
                double y = inputManager != null && !inputManager.isCursorVisible() ? cursorY : state.toGuiY(me.getY());
                pointerReleased(state, navigator, x, y);
            }
            consume(event);
            return;
        }

        if (_p("confirm").equals(name) && toggled && event instanceof JoyButtonEvent
                && navigator.isSimulateCursor()
                && isJoystickButton((JoyButtonEvent) event, JoystickButton.BUTTON_XBOX_RT)) {
            if (isPressed) {
                pointerPressed(state, navigator, cursorX, cursorY);
            } else {
                pointerReleased(state, navigator, cursorX, cursorY);
            }
            consume(event);
            return;
        }
        
        if (navigator.getFocus() == null && !name.equals(_p("navigateByCursor"))) {
            navigator.autofocus();
        }

        if(_p("back").equals(name)){
            if(toggled && isPressed){
                backAction.run();
                consume(event);
            }
            return;
        }

        if (_p("secondaryAction").equals(name) && navigator.isSimulateCursor()) {
            if (toggled && isPressed) {
                secondaryAction.run();
                consume(event);
            }
            return;
        }

        if (navigator.getFocus() == null && !name.equals(_p("navigateByCursor"))) {
            if(_p("primaryAction").equals(name)){
                if(toggled && isPressed){
                    primaryAction.run();
                    consume(event);
                }
            } else if(_p("secondaryAction").equals(name)){
                if(toggled && isPressed){
                    secondaryAction.run();
                    consume(event);
                }
            } else {
                consume(event);
            }
            return;
        }

        if (_p("scrollUp").equals(name)) {
            if (toggled && isPressed) {
                navigator.scroll(ScrollDirection.Up, 1);
                consume(event);
            }
        } else if (_p("scrollDown").equals(name)) {
            if (toggled && isPressed) {
                navigator.scroll(ScrollDirection.Down, 1);
                consume(event);
            }
        } else if (_p("scrollLeft").equals(name)) {
            if (toggled && isPressed) {
                navigator.scroll(ScrollDirection.Left, 1);
                consume(event);
            }
        } else if (_p("scrollRight").equals(name)) {
            if (toggled && isPressed) {
                navigator.scroll(ScrollDirection.Right, 1);
                consume(event);
            }
        } else if (_p("confirm").equals(name)) {
            if (toggled) {
                if (isPressed) {
                    navigatorActionPressed = true;
                    navigator.action(true);
                } else if (navigatorActionPressed) {
                    navigatorActionPressed = false;
                    navigator.action(false);
                }
                consume(event);
            }
        } else if (_p("navigateUp").equals(name)) {
            if (toggled && isPressed) {
                navigateOrAdjustSlider(navigator, TraversalDirection.Up, event);
            }
        } else if (_p("navigateDown").equals(name)) {
            if (toggled && isPressed) {
                navigateOrAdjustSlider(navigator, TraversalDirection.Down, event);
            }
        } else if (_p("navigateLeft").equals(name)) {
            if (toggled && isPressed) {
                navigateOrAdjustSlider(navigator, TraversalDirection.Left, event);
            }
        } else if (_p("navigateRight").equals(name)) {
            if (toggled && isPressed) {
                navigateOrAdjustSlider(navigator, TraversalDirection.Right, event);
            }
        } else if (_p("navigateNext").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Next);
                consume(event);
            }
        } else if (_p("navigatePrevious").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Previous);
                consume(event);
            }
        } else if (_p("navigateByCursor").equals(name)) {
            if (event instanceof MouseMotionEvent) {
                MouseMotionEvent mme = (MouseMotionEvent) event;
                if (mme == lastCursorMotionEvent) {
                    consume(event);
                    return;
                }
                lastCursorMotionEvent = mme;
                updateCursorFromMotion(state, mme);
                if (pointerActionPressed) {
                    pointerDragged(cursorX, cursorY);
                }
                if(navigator.updateCursorPosition(cursorX, cursorY)){
                    Spatial picked = state.pick(cursorX, cursorY);
                    if (picked != null) {
                        navigator.focusPointer(picked);
                    } else {
                        navigator.clearPointerFocus();
                    }
                    if (picked != null) {
                        consume(event);
                    }
                }
            }
        }   
    }

    private void handleRawMouseMotion(MouseMotionEvent event) {
        ViewPort vp = getViewPort();
        if (vp == null) return;

        GuiContext state = NGEGui.get(vp);
        if (state == null) return;

        Navigator navigator = state.getNavigator();
        if (navigator == null || (!navigator.isCursorVisible() && !pointerActionPressed)) {
            return;
        }

        updateCursorFromMotion(state, event);
        Spatial picked = null;
        if (navigator.updateCursorPosition(cursorX, cursorY) && !pointerActionPressed) {
            picked = state.pick(cursorX, cursorY);
            if (picked != null) {
                navigator.focusPointer(picked);
            } else {
                navigator.clearPointerFocus();
            }
        }

        if (pointerActionPressed) {
            pointerDragged(cursorX, cursorY);
        }

        if (event.getDeltaWheel() != 0) {
            navigator.scroll(event.getDeltaWheel() < 0 ? ScrollDirection.Down : ScrollDirection.Up, 1);
        }

        lastCursorMotionEvent = event;
        if (pointerActionPressed || event.getDeltaWheel() != 0 || picked != null) {
            consume(event);
        }
    }

    private void handleRawMouseButton(MouseButtonEvent event) {
        if (event.getButtonIndex() != MouseInput.BUTTON_LEFT) {
            return;
        }

        ViewPort vp = getViewPort();
        if (vp == null) return;

        GuiContext state = NGEGui.get(vp);
        if (state == null) return;

        Navigator navigator = state.getNavigator();
        if (navigator == null || (!navigator.isCursorVisible() && !pointerActionPressed)) {
            return;
        }

        double x = hasCursorMotion() ? cursorX : state.toGuiX(event.getX());
        double y = hasCursorMotion() ? cursorY : state.toGuiY(event.getY());
        navigator.updateCursorPosition(x, y);
        if (event.isPressed()) {
            pointerPressed(state, navigator, x, y);
        } else {
            pointerReleased(state, navigator, x, y);
        }
        consume(event);
    }

    private boolean hasCursorMotion() {
        return !Double.isNaN(lastMotionX) && !Double.isNaN(lastMotionY);
    }

    private void updateCursorFromMotion(GuiContext state, MouseMotionEvent event) {
        double rawEventX = event.getX();
        double rawEventY = event.getY();
        double eventX = state.toGuiX(rawEventX);
        double eventY = state.toGuiY(rawEventY);
        boolean fixedAbsolute = eventX == lastMotionX && eventY == lastMotionY
                || (Double.isNaN(lastMotionX) && eventX == cursorX && eventY == cursorY);
        if (inputManager != null && !inputManager.isCursorVisible()
                && fixedAbsolute
                && (event.getDX() != 0 || event.getDY() != 0)) {
            cursorX += state.toGuiDeltaX(event.getDX());
            cursorY += state.toGuiDeltaY(event.getDY());
        } else {
            cursorX = eventX;
            cursorY = eventY;
        }
        lastMotionX = eventX;
        lastMotionY = eventY;
        clampCursorToViewPort();
    }

    private boolean handleTouchInput(GuiContext state, Navigator navigator, String name, InputEvent<?> inputEvent) {
        if (!_p("touch").equals(name) || !(inputEvent instanceof TouchEvent)) {
            return false;
        }
        TouchEvent event = (TouchEvent) inputEvent;
        TouchEvent.Type type = event.getType();
        double x = state.toGuiX(event.getX());
        double y = state.toGuiY(event.getY());
        if (type == TouchEvent.Type.TAP) {
            pointerPressed(state, navigator, x, y);
            pointerReleased(state, navigator, x, y);
            consume(event);
        } else if (type == TouchEvent.Type.DOWN) {
            pointerPressed(state, navigator, x, y);
            consume(event);
        } else if (type == TouchEvent.Type.UP) {
            pointerReleased(state, navigator, x, y);
            consume(event);
        } else if (type == TouchEvent.Type.MOVE && pointerActionPressed) {
            pointerDragged(x, y);
            updatePointerFocusAt(state, navigator, x, y);
            consume(event);
        }
        return true;
    }

    private void navigateOrAdjustSlider(Navigator navigator, TraversalDirection dir, InputEvent<?> event) {
        Spatial focus = navigator.getFocus();
        if (focus instanceof Slider) {
            Slider slider = (Slider) focus;
            ScrollDirection scroll = null;
            if (slider.getAxis() == Axis.X) {
                if (dir == TraversalDirection.Left) scroll = ScrollDirection.Left;
                else if (dir == TraversalDirection.Right) scroll = ScrollDirection.Right;
            } else if (slider.getAxis() == Axis.Y) {
                if (dir == TraversalDirection.Up) scroll = ScrollDirection.Up;
                else if (dir == TraversalDirection.Down) scroll = ScrollDirection.Down;
            }
            if (scroll != null) {
                navigator.scroll(scroll, 1);
                consume(event);
                return;
            }
        }
        navigator.navigate(dir);
        consume(event);
    }

    private boolean handleSimulatedCursorAxis(GuiContext state, Navigator navigator, String name, float value,
            InputEvent<?> event, float tpf) {
        if (!(event instanceof JoyAxisEvent) || !navigator.isSimulateCursor()) {
            return false;
        }
        JoyAxisEvent axisEvent = (JoyAxisEvent) event;
        String axis = axisEvent.getAxis().getLogicalId();
        if (!JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X.equals(axis)
                && !JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y.equals(axis)) {
            return false;
        }

        float axisValue = Math.abs(value) < navigator.getSimulatedCursorDeadZone() ? 0f : value;
        if (_p("navigateLeft").equals(name)) simulatedCursorX = -axisValue;
        else if (_p("navigateRight").equals(name)) simulatedCursorX = axisValue;
        else if (_p("navigateUp").equals(name)) simulatedCursorY = axisValue;
        else if (_p("navigateDown").equals(name)) simulatedCursorY = -axisValue;

        update(tpf);
        consume(event);
        return true;
    }

    private boolean isJoystickButton(JoyButtonEvent event, String logicalId) {
        return event.getButton() != null && logicalId.equals(event.getButton().getLogicalId());
    }

    private void updatePointerFocusAt(GuiContext state, Navigator navigator, double x, double y) {
        Spatial picked = state.pick(x, y);
        if (picked != null) {
            navigator.focusPointer(picked);
        } else {
            navigator.clearPointerFocus();
        }
    }

    private void pointerPressed(GuiContext state, Navigator navigator, double x, double y) {
        pointerActionTarget = state.pick(x, y);
        if (pointerActionTarget != null) {
            navigator.focusPointer(pointerActionTarget);
            FocusTarget target = NGEGui.findFocusTarget(pointerActionTarget);
            if (target != null) {
                target.focusAction(pointerActionTarget, true, (float) x, (float) y);
                pointerActionPressed = true;
            }
        } else {
            navigator.clearPointerFocus();
            pointerActionPressed = false;
        }
    }

    private void pointerDragged(double x, double y) {
        if (pointerActionTarget == null || !pointerActionPressed) {
            return;
        }
        FocusTarget target = NGEGui.findFocusTarget(pointerActionTarget);
        if (target != null) {
            target.focusDrag(pointerActionTarget, (float) x, (float) y);
        }
    }

    private void pointerReleased(GuiContext state, Navigator navigator, double x, double y) {
        Spatial pressedTarget = pointerActionTarget;
        Spatial releaseTarget = state.pick(x, y);
        pointerActionTarget = null;
        if (pressedTarget != null && pointerActionPressed) {
            FocusTarget target = NGEGui.findFocusTarget(pressedTarget);
            if (target != null) {
                target.focusAction(pressedTarget, false, (float) x, (float) y);
            }
            if (pressedTarget == releaseTarget) {
                navigator.focusPointer(pressedTarget);
            }
        }
        pointerActionPressed = false;
        if (releaseTarget != null) {
            navigator.focusPointer(releaseTarget);
        } else {
            navigator.clearPointerFocus();
        }
    }

    private boolean isAxisAction(String name) {
        return _p("navigateUp").equals(name)
                || _p("navigateDown").equals(name)
                || _p("navigateLeft").equals(name)
                || _p("navigateRight").equals(name)
                || _p("scrollUp").equals(name)
                || _p("scrollDown").equals(name);
    }

    private void updateStickAxisState(String name, float value) {
        if (_p("navigateLeft").equals(name)) {
            leftStickX = -value;
        } else if (_p("navigateRight").equals(name)) {
            leftStickX = value;
        } else if (_p("navigateUp").equals(name)) {
            leftStickY = -value;
        } else if (_p("navigateDown").equals(name)) {
            leftStickY = value;
        }
    }

    private boolean isDominantAxisAction(String name) {
        if (_p("navigateLeft").equals(name) || _p("navigateRight").equals(name)) {
            return Math.abs(leftStickX) >= Math.abs(leftStickY) + AXIS_DOMINANCE_MARGIN;
        } else if (_p("navigateUp").equals(name) || _p("navigateDown").equals(name)) {
            return Math.abs(leftStickY) >= Math.abs(leftStickX) + AXIS_DOMINANCE_MARGIN;
        }
        return true;
    }
}
