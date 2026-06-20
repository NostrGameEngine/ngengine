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

package org.ngengine.components.fragments;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickConnectionListener;
import com.jme3.input.RawInputListener;
import com.jme3.input.virtual.VirtualJoystick;

import com.jme3.input.controls.UnifiedInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;

import java.lang.reflect.Method;
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
    static final float JOYSTICK_AXIS_DEVICE_SWITCH_THRESHOLD = 0.9f;
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

        private boolean isFragmentEnabled() {
            return !(fragment instanceof Component) || mng.isComponentEnabled((Component) fragment);
        }

        @Override
        public void beginInput() {
            if (isFragmentEnabled()) {
                syncVirtualJoystick();
                fragment.beginInput(mng);
            }
        }

        private void deviceConnect(InputEvent<?> evt) {
            deviceConnect(evt.getDevice());
        }

        private void deviceConnect(InputDevice device) {
            if (seenDevices.add(device)) {                
                fragment.onInputDeviceConnected(mng, mng.getInstanceOf(InputManager.class), binder, device);
            }
        }

        private void syncVirtualJoystick() {
            InputManager inputManager = mng.getInstanceOf(InputManager.class);
            Joystick[] joysticks = inputManager.getJoysticks();
            if (joysticks == null) {
                return;
            }
            for (Joystick joystick : joysticks) {
                if (joystick instanceof VirtualJoystick) {
                    updateVirtualJoystick((VirtualJoystick) joystick, joysticks);
                    return;
                }
            }
        }

        private void updateVirtualJoystick(VirtualJoystick joystick, Joystick[] joysticks) {
            if (!fragment.controlsOnScreenJoystick(mng, joysticks)) {
                return;
            }
            boolean wantsVisible = fragment.showOnScreenJoystick(mng, joysticks);
            if (wantsVisible) {
                deviceConnect(joystick);
            }
            boolean visible = wantsVisible && joystick.hasInputBindings();
            joystick.setEnabled(visible);
            if (!wantsVisible) {
                deviceDisconnect(joystick);
            }
        }

        private void deviceDisconnect(InputDevice device) {
            if (seenDevices.remove(device)) {
                fragment.onInputDeviceDisconnected(mng, mng.getInstanceOf(InputManager.class), binder,device);
            }
        }


        @Override
        public void endInput() {
            if (isFragmentEnabled()) {
                fragment.endInput(mng);
            }
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            if (isFragmentEnabled()) {
                // axis drift caused by normal wear can cause this event to be fired unintentionally.
                // as a failsafe measure we avoid triggering deviceConnect events in this case
                if (Math.abs(evt.getValue()) >= JOYSTICK_AXIS_DEVICE_SWITCH_THRESHOLD) {
                    deviceConnect(evt);
                }
                if(seenDevices.contains(evt.getDevice())) {
                    fragment.onJoyAxisEvent(mng, evt);
                }
            }
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            if (isFragmentEnabled()) {
                if (evt.getDevice() instanceof VirtualJoystick || evt.isPressed()) {
                    deviceConnect(evt);
                }
                fragment.onJoyButtonEvent(mng, evt);
            }
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            if (isFragmentEnabled()) {
                deviceConnect(evt);
                fragment.onMouseMotionEvent(mng, evt);
            }
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if (isFragmentEnabled()) {
                deviceConnect(evt);
                fragment.onMouseButtonEvent(mng, evt);
            }
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if (isFragmentEnabled()) {
                deviceConnect(evt);
                fragment.onKeyEvent(mng, evt);
            }
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
            if (isFragmentEnabled()) {
                deviceConnect(evt);
                fragment.onTouchEvent(mng, evt);
            }
        }


        @Override
        public void onUnifiedInput(String name, boolean toggled, float value, InputEvent<?>  event, float tpf) {
            if (isFragmentEnabled()) {
                fragment.onInputAction(mng, name, toggled, value, event, tpf);
            }
        }

        @Override
        public void onConnected(Joystick joystick) {
            if (isFragmentEnabled()) {
                if (joystick instanceof VirtualJoystick) {
                    updateVirtualJoystick((VirtualJoystick) joystick, mng.getInstanceOf(InputManager.class).getJoysticks());
                } else {
                    syncVirtualJoystick();
                }
            }
        }

        @Override
        public void onDisconnected(Joystick joystick) {
             if (isFragmentEnabled()) {
                deviceDisconnect(joystick);
                syncVirtualJoystick();
            }
        }

     
    }

    static boolean hasPhysicalJoystick(Joystick[] joysticks) {
        if (joysticks == null) {
            return false;
        }
        for (Joystick joystick : joysticks) {
            if (!(joystick instanceof VirtualJoystick)) {
                return true;
            }
        }
        return false;
    }

    static boolean isMobilePlatform() {
        Platform.Os os = JmeSystem.getPlatform().getOs();
        return os == Platform.Os.Android || os == Platform.Os.iOS;
    }

    static boolean isMobileWebView() {
        if (JmeSystem.getPlatform().getOs() != Platform.Os.Web) {
            return false;
        }
        try {
            Class<?> webInfo = Class.forName("org.ngengine.web.WebPlatformInfo");
            Method method = webInfo.getMethod("isMobileView");
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (Throwable ignored) {
            return false;
        }
    }

    default boolean showOnScreenJoystick(ComponentManager mng, Joystick[] joysticks) {
        if (hasPhysicalJoystick(joysticks)) {
            return false;
        }
        return isMobilePlatform() || isMobileWebView();
    }

    default boolean controlsOnScreenJoystick(ComponentManager mng, Joystick[] joysticks) {
        return true;
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
