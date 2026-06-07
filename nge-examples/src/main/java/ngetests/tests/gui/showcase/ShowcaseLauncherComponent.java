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

package ngetests.tests.gui.showcase;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.ColorRGBA;
import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.jme3.AppComponentInitializer.InputActions;
import org.ngengine.gui.nav.Navigator;
import org.ngengine.gui.guix.win.NToast.ToastType;
import org.ngengine.gui.guix.win.NWindowManagerComponent;

public class ShowcaseLauncherComponent extends AbstractComponent implements InputHandlerFragment {

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        getInstanceOf(ViewPortManager.class).getMainSceneViewPort()
            .setBackgroundColor(new ColorRGBA(0.34f, 0.37f, 0.41f, 1f));
        NWindowManagerComponent win = getInstanceOf(NWindowManagerComponent.class);
        win.showWindow(ShowcaseLauncherWindow.class);
    }

    @Override
    protected void onDisable(ComponentManager mng) {}

    @Override
    public void onKeyEvent(ComponentManager mng, KeyInputEvent evt) {
        if (!evt.isPressed()) return;

        NWindowManagerComponent win = getInstanceOf(NWindowManagerComponent.class);
        Navigator navigator = win.getManager(null).getContext().getNavigator();
        if (evt.getKeyCode() == KeyInput.KEY_1) {
            navigator.setHardwareCursor(false);
            navigator.setSimulateCursor(false);
            win.showToast(ToastType.INFO, "Mode 1: software cursor");
            evt.setConsumed();
        } else if (evt.getKeyCode() == KeyInput.KEY_2) {
            navigator.setHardwareCursor(true);
            navigator.setSimulateCursor(false);
            win.showToast(ToastType.INFO, "Mode 2: hardware cursor");
            evt.setConsumed();
        } else if (evt.getKeyCode() == KeyInput.KEY_3) {
            navigator.setHardwareCursor(true);
            navigator.setSimulateCursor(true);
            win.showToast(ToastType.INFO, "Mode 3: hardware + controller cursor");
            evt.setConsumed();
        }
    }

    @Override
    public void onInputAction(ComponentManager mng, String action, boolean toggled, float value, InputEvent<?> event,
            float tpf) {}

    @Override
    public void onInputDeviceConnected(ComponentManager mng, InputManager inputManager, InputActions inputActions,
            InputDevice device) {}

    @Override
    public void onInputDeviceDisconnected(ComponentManager mng, InputManager inputManager, InputActions inputActions,
            InputDevice device) {}
}
