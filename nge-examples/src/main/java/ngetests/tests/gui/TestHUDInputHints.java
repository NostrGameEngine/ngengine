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
 */

package ngetests.tests.gui;

import com.jme3.input.KeyInput;
import com.jme3.input.Keyboard;
import com.jme3.input.Mouse;
import com.jme3.input.MouseInput;
import com.jme3.math.ColorRGBA;
import org.ngengine.Components;
import org.ngengine.NGEApplication;
import org.ngengine.NGEApplication.NGEAppRunner;
import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.gui.guix.win.HUDInputHintComponent;
import org.ngengine.gui.guix.win.NHud;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.jvm.JVMAsyncPlatform;

public class TestHUDInputHints {

    public static void main(String[] args) {
        NGEPlatform.set(new JVMAsyncPlatform());

        NGEAppSettings settings = new NGEAppSettings();
        settings.setInt("Width", 1280);
        settings.setInt("Height", 720);
        settings.setString("Title", "NGE HUD Input Hints");
        settings.setBoolean("DisplayFps", false);
        settings.setBoolean("DisplayStats", false);

        NGEAppRunner app = NGEApplication.createApp(settings, a -> {
            NWindowManagerComponent win = new NWindowManagerComponent();
            Components.mount(a, win).enable();
            win.setInteractionEnabled(true);

            Components.mount(a, new HUDInputHintComponent(), NWindowManagerComponent.class).enable();
            Components.mount(a, new HudInputHintDemoComponent(), NWindowManagerComponent.class, HUDInputHintComponent.class).enable();
        });
        app.run();
    }

    private static final class HudInputHintDemoComponent extends AbstractComponent {
        @Override
        protected void onEnable(ComponentManager mng, boolean firstTime) {
            getInstanceOf(ViewPortManager.class).getMainSceneViewPort()
                .setBackgroundColor(new ColorRGBA(0.34f, 0.37f, 0.41f, 1f));

            NWindowManagerComponent win = getInstanceOf(NWindowManagerComponent.class);
            win.showWindow(NHud.class);

            HUDInputHintComponent hints = getInstanceOf(HUDInputHintComponent.class);
            Keyboard keyboard = new Keyboard();
            Mouse mouse = new Mouse();
            hints.setInputHint(keyboard, KeyInput.KEY_E, "pick up");
            hints.setInputHint(mouse, MouseInput.BUTTON_MIDDLE, "select");
            hints.setInputHint(keyboard, KeyInput.KEY_X, "hold detach mode");
            hints.showInputHintsForDevice(keyboard);
        }

        @Override
        protected void onDisable(ComponentManager mng) {
            HUDInputHintComponent hints = getInstanceOf(HUDInputHintComponent.class);
            if (hints != null) {
                hints.clearInputHints();
            }
        }
    }
}
