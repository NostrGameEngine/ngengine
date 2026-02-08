/**
 * Copyright (c) 2025, Nostr Game Engine
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

package org.ngengine.components.fragments;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickConnectionListener;
import com.jme3.input.RawInputListener;

import com.jme3.input.controls.UnifiedInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

import java.util.HashSet;
import java.util.Set;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.AppComponentInitializer.InputActions;

/**
 * A fragment that intercept raw input events. It extends {@link RawInputListener} and is automatically
 * registered with the {@link InputManager} when the component is initialized.
 *
 * If higher-level input handling is needed, the
 *
 * The onXXEvents are called only if the component is enabled.
 *
 * {@link InputManager} instance and register additional input listeners.
 */
public interface InputHandlerFragment extends Fragment {
    class Wrapper implements RawInputListener, UnifiedInputListener, JoystickConnectionListener{

        private final InputHandlerFragment fragment;
        private final ComponentManager mng;
        private final Set<InputDevice> seenDevices = new HashSet<>();
        private final InputActions binder;

        public Wrapper(ComponentManager fragmentManager, InputHandlerFragment fragment, InputActions binder) {
            this.fragment = fragment;
            this.mng = fragmentManager;
            this.binder = binder;   
            this.binder.setListener(this);
        }

        @Override
        public void beginInput() {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                fragment.beginInput(mng);
            }
        }

        private void deviceConnect(InputEvent<?> evt) {
            InputDevice device = evt.getDevice();
            if (seenDevices.add(device)) {                
                fragment.onInputDeviceConnected(mng, mng.getInstanceOf(InputManager.class), binder, device);
            }
        }

 
        private void deviceDisconnect(InputDevice device) {
            if (seenDevices.remove(device)) {
                fragment.onInputDeviceDisconnected(mng, mng.getInstanceOf(InputManager.class), binder,device);
            }
        }


        @Override
        public void endInput() {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                fragment.endInput(mng);
            }
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                deviceConnect(evt);
                fragment.onJoyAxisEvent(mng, evt);
            }
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                deviceConnect(evt);
                fragment.onJoyButtonEvent(mng, evt);
            }
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                deviceConnect(evt);
                fragment.onMouseMotionEvent(mng, evt);
            }
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                deviceConnect(evt);
                fragment.onMouseButtonEvent(mng, evt);
            }
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                deviceConnect(evt);
                fragment.onKeyEvent(mng, evt);
            }
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                deviceConnect(evt);
                fragment.onTouchEvent(mng, evt);
            }
        }


        @Override
        public void onUnifiedInput(String name, boolean toggled, float value, InputEvent<?>  event, float tpf) {
            if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                fragment.onInputAction(mng, name, toggled, value, event, tpf);
            }
        }

        @Override
        public void onConnected(Joystick joystick) {
           
        }

        @Override
        public void onDisconnected(Joystick joystick) {
             if (!(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment)) {
                deviceDisconnect(joystick);
            }
        }

     
    }
 
    @Deprecated
    default void receiveInputManager(InputManager inputManager) {}

    default void beginInput(ComponentManager mng) {}

    default void endInput(ComponentManager mng) {}

    default void onJoyAxisEvent(ComponentManager mng, JoyAxisEvent evt) {}

    default void onJoyButtonEvent(ComponentManager mng, JoyButtonEvent evt) {}

    default void onMouseMotionEvent(ComponentManager mng, MouseMotionEvent evt) {}

    default void onMouseButtonEvent(ComponentManager mng, MouseButtonEvent evt) {}

    default void onKeyEvent(ComponentManager mng, KeyInputEvent evt) {}

    default void onTouchEvent(ComponentManager mng, TouchEvent evt) {}

    void onInputAction(ComponentManager mng, String action, boolean toggled, float value, InputEvent<?>  event, float tpf) ;

    void onInputDeviceConnected(ComponentManager mng, InputManager inputManager, InputActions inputActions, InputDevice device) ;

    void onInputDeviceDisconnected(ComponentManager mng, InputManager inputManager, InputActions inputActions, InputDevice device) ;

 
}
