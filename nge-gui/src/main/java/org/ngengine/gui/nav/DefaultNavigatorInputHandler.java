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
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.Keyboard;
import com.jme3.input.Mouse;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchScreen;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
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
        this.guiViewPort = new WeakReference<>(vp);
        this.bindId = String.valueOf(vp.hashCode());
        if (this.inputManager != null) {
            registerListener(this.inputManager);
            if (this.inputDevice != null) {
                setInputDevice(this.inputManager, this.inputDevice);
            }
        }
        cursorX = vp.getCamera().getWidth() / 2;
        cursorY = vp.getCamera().getHeight() / 2;

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
        inputManager.addListener(this, _p("navigateUp"), _p("navigateDown"), _p("navigateLeft"),
                _p("navigateRight"), _p("navigateNext"), _p("navigatePrevious"), _p("scrollUp"),
                _p("scrollDown"), _p("confirm"), _p("navigateByCursor"),
        _p("primaryAction"), _p("secondaryAction"), _p("back")
            );
    }

    @Override
    public void unregisterListener(InputManager inputManager) {
        if (inputManager != null) {
            inputManager.removeListener(this);
        }
    }

    @Override
    public void setInputDevice(InputManager inputManager, @Nullable InputDevice device) {
        if(this.inputDevice == device) return;
        if(this.inputManager!=null){
            try{
                for (String mapping : mappings) {
                    inputManager.deleteMapping(mapping);
                }
            } catch (Exception e) {
            }
        }
        mappings.clear();
        
        this.inputManager = inputManager;
        this.inputDevice = device;

        if (device == null) {
            return;
        } else if (device instanceof Keyboard || device instanceof Mouse) {
            // mouse and keyboard
            inputManager.addMapping(_ps("navigateUp"), new KeyTrigger(KeyInput.KEY_UP));
            inputManager.addMapping(_ps("navigateDown"), new KeyTrigger(KeyInput.KEY_DOWN));
            inputManager.addMapping(_ps("navigateLeft"), new KeyTrigger(KeyInput.KEY_LEFT));
            inputManager.addMapping(_ps("navigateRight"), new KeyTrigger(KeyInput.KEY_RIGHT));
            inputManager.addMapping(_ps("navigateNext"), new KeyTrigger(KeyInput.KEY_TAB));
            inputManager.addMapping(_ps("navigateByCursor"), new MouseAxisTrigger(MouseInput.AXIS_X, true),
                    new MouseAxisTrigger(MouseInput.AXIS_X, false),
                    new MouseAxisTrigger(MouseInput.AXIS_Y, true),
                    new MouseAxisTrigger(MouseInput.AXIS_Y, false));

            inputManager.addMapping(_ps("scrollUp"), new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));

            inputManager.addMapping(_ps("scrollDown"), new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

            inputManager.addMapping(_ps("confirm"), new KeyTrigger(KeyInput.KEY_RETURN),
                    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

            inputManager.addMapping(_ps("primaryAction"), new KeyTrigger(KeyInput.KEY_RETURN));

            inputManager.addMapping(_ps("secondaryAction"), new KeyTrigger(KeyInput.KEY_P));
            inputManager.addMapping(_ps("back"), new KeyTrigger(KeyInput.KEY_BACK));
        } else if (device instanceof TouchScreen) {
            // TODO
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
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_RB));
            inputManager.addMapping(_ps("navigatePrevious"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_LB));

            inputManager.addMapping(_ps("scrollUp"),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y, true));
            inputManager.addMapping(_ps("scrollDown"),
                    new JoyAxisTrigger(joy, JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y, false));

            inputManager.addMapping(_ps("confirm"), new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_A));

            inputManager.addMapping(_ps("primaryAction"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_A));

            inputManager.addMapping(_ps("back"), new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_B));

            inputManager.addMapping(_ps("secondaryAction"),
                    new JoyButtonTrigger(joy, JoystickButton.BUTTON_XBOX_BACK));

        }
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
        
        if (navigator.getFocus() == null && !name.equals(_p("navigateByCursor"))) {
            if(_p("primaryAction").equals(name)){
                if(toggled && isPressed){
                    primaryAction.run();
                    if(consume)event.setConsumed();
                }
            } else if(_p("secondaryAction").equals(name)){
                if(toggled && isPressed){
                    secondaryAction.run();
                    if(consume)event.setConsumed();
                }
            } else if(_p("back").equals(name)){
                if(toggled && isPressed){
                    backAction.run();
                    if(consume)event.setConsumed();
                }
            }
            return;
        }

        if (_p("scrollUp").equals(name)) {
            if (toggled && isPressed) {
                navigator.scroll(ScrollDirection.Up, 1);
                if (consume) event.setConsumed();
            }
        } else if (_p("scrollDown").equals(name)) {
            if (toggled && isPressed) {
                navigator.scroll(ScrollDirection.Down, 1);
                if (consume) event.setConsumed();
            }
        } else if (_p("confirm").equals(name)) {

            if (toggled) {
                if (event instanceof TouchEvent) {
                    // if touch event we do a pick before action
                    TouchEvent te = (TouchEvent) event;
                    Spatial picked = state.pick((int) te.getX(), (int) te.getY());
                    navigator.focus(picked);
                } else if (event instanceof MouseButtonEvent) {
                    MouseButtonEvent mbe = (MouseButtonEvent) event;
                    cursorX = mbe.getX();
                    cursorY = mbe.getY();
                    navigator.updateCursorPosition(cursorX, cursorY);
                }
                navigator.action(isPressed);
                if (consume) event.setConsumed();
            }
        } else if (_p("navigateUp").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Up);
                if (consume) event.setConsumed();
            }
        } else if (_p("navigateDown").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Down);
                if (consume) event.setConsumed();
            }
        } else if (_p("navigateLeft").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Left);
                if (consume) event.setConsumed();
            }
        } else if (_p("navigateRight").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Right);
                if (consume) event.setConsumed();
            }
        } else if (_p("navigateNext").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Next);
                if (consume) event.setConsumed();
            }
        } else if (_p("navigatePrevious").equals(name)) {
            if (toggled && isPressed) {
                navigator.navigate(TraversalDirection.Previous);
                if (consume) event.setConsumed();
            }
        } else if (_p("navigateByCursor").equals(name)) {
            if (event instanceof MouseMotionEvent) {
                MouseMotionEvent mme = (MouseMotionEvent) event;
                cursorX += mme.getDX();
                cursorY += mme.getDY();
                if(cursorX < 0) cursorX = 0;
                if(cursorY < 0) cursorY = 0;
                if(cursorX > vp.getCamera().getWidth()) cursorX = vp.getCamera().getWidth();
                if(cursorY > vp.getCamera().getHeight()) cursorY = vp.getCamera().getHeight();
                if(navigator.updateCursorPosition(cursorX, cursorY)){
                    Spatial picked = state.pick(cursorX, cursorY);
                    navigator.focus(picked);
                    if (picked != null) {
                        if (consume) event.setConsumed();
                    }
                }
            }
        }   
    }
}
