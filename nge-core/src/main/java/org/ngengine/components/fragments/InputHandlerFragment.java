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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.components.fragments;

import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;

/**
 * A fragment that intercept raw input events. It extends {@link RawInputListener} and is automatically
 * registered with the {@link InputManager} when the component is initialized.
 *
 * If higher-level input handling is needed, the
 *
 * The onXXEvents are called only if the component is enabled.
 *
 * {@link InputHandlerFragment#receiveInputManager(InputManager)} method can be overridden to receive the
 * {@link InputManager} instance and register additional input listeners.
 */
public interface InputHandlerFragment extends Fragment, RawInputListener {
    class Wrapper implements RawInputListener {

        private final InputHandlerFragment fragment;
        private final ComponentManager fragmentManager;

        public Wrapper(ComponentManager fragmentManager, InputHandlerFragment fragment) {
            this.fragment = fragment;
            this.fragmentManager = fragmentManager;
        }

        @Override
        public void beginInput() {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.beginInput();
            }
        }

        @Override
        public void endInput() {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.endInput();
            }
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.onJoyAxisEvent(evt);
            }
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.onJoyButtonEvent(evt);
            }
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.onMouseMotionEvent(evt);
            }
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.onMouseButtonEvent(evt);
            }
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.onKeyEvent(evt);
            }
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
            if (!(fragment instanceof Component) || fragmentManager.isComponentEnabled((Component) fragment)) {
                fragment.onTouchEvent(evt);
            }
        }
    }

    @Override
    default void beginInput() {}

    @Override
    default void endInput() {}

    /**
     * Receives the InputManager instance when the component is initialized. This method can be overridden to
     * register additional input listeners.
     *
     * The reference to the InputManager can be stored and used later in the component logic.
     */
    default void receiveInputManager(InputManager inputManager) {}

    default void onJoyAxisEvent(JoyAxisEvent evt) {}

    default void onJoyButtonEvent(JoyButtonEvent evt) {}

    default void onMouseMotionEvent(MouseMotionEvent evt) {}

    default void onMouseButtonEvent(MouseButtonEvent evt) {}

    default void onKeyEvent(KeyInputEvent evt) {}

    default void onTouchEvent(TouchEvent evt) {}
}
