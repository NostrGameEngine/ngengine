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

import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.RawInputListener;
import com.jme3.input.virtual.VirtualJoystick;
import com.jme3.system.AppSettings;

public class WebJoyInput implements JoyInput {
    private final AppSettings settings;
    private RawInputListener listener;
    private VirtualJoystick virtualJoystick;
    private boolean initialized;

    public WebJoyInput(AppSettings settings) {
        this.settings = settings;
    }

    @Override
    public void initialize() {
        initialized = true;
    }

    @Override
    public void update() {
        updateVirtualJoystickAutoVisibility();
        if (virtualJoystick != null) {
            virtualJoystick.dispatchEvents(listener);
        }
    }

    @Override
    public void destroy() {
        if (virtualJoystick != null) {
            virtualJoystick.onPointerCancel(getInputTimeNanos());
        }
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
        // Browser haptics are not wired into this backend yet.
    }

    @Override
    public void stopJoyRumble(int joyId) {
        setJoyRumble(joyId, 0f, 0f, 0f);
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager) {
        if (shouldCreateVirtualJoystick()) {
            virtualJoystick = new VirtualJoystick(inputManager, this, 0);
            virtualJoystick.setLayout(VirtualJoystick.createLayout(settings.getVirtualJoystickDefaultLayout()));
            virtualJoystick.setEnabled(false);
            updateVirtualJoystickAutoVisibility();
            return new Joystick[]{virtualJoystick};
        }

        virtualJoystick = null;
        return new Joystick[0];
    }

    public boolean onPointerDown(int pointerId, float x, float y, long time) {
        updateVirtualJoystickAutoVisibility();
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

    private boolean shouldCreateVirtualJoystick() {
        return settings.useJoysticks()
                && !AppSettings.VIRTUAL_JOYSTICK_DISABLED.equals(settings.getVirtualJoystickMode());
    }

    private void updateVirtualJoystickAutoVisibility() {
        if (virtualJoystick == null) {
            return;
        }
        String mode = settings.getVirtualJoystickMode();
        boolean active = AppSettings.VIRTUAL_JOYSTICK_ENABLED.equals(mode)
                || (AppSettings.VIRTUAL_JOYSTICK_AUTO.equals(mode) && virtualJoystick.hasInputBindings());
        if (virtualJoystick.isEnabled() != active) {
            virtualJoystick.setEnabled(active);
        }
    }
}
