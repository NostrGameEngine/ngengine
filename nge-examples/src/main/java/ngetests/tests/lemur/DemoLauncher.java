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

package ngetests.tests.lemur;

import org.ngengine.gui.NGEGui;
import org.ngengine.gui.NGEStyle;

import com.jme3.app.*;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.Keyboard;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import org.ngengine.gui.ime.PhysicalKeyboardImeComposer;
import org.ngengine.gui.nav.DefaultNavigatorInputHandler;
import org.ngengine.gui.nav.Navigator;

public class DemoLauncher extends SimpleApplication implements RawInputListener {

        public static void main(String... args) throws Exception {
                DemoLauncher main = new DemoLauncher();
                AppSettings settings = new AppSettings(true);

                settings.setRenderer(AppSettings.LWJGL_OPENGL40);
                settings.setWidth(1280);
                settings.setHeight(720);
                settings.setVSync(true);
                settings.setResizable(true);
                settings.setTitle("Lemur Demos");

                main.setSettings(settings);

                main.start();
        }

        public DemoLauncher() {
                super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false),
                                new MainMenuState(), new ScreenshotAppState("", System.currentTimeMillis()));

        }

        DefaultNavigatorInputHandler inputHandler;

        public void simpleInitApp() {
                viewPort.setBackgroundColor(ColorRGBA.Gray);
                setPauseOnLostFocus(false);
                setDisplayFps(false);
                setDisplayStatView(false);

                inputManager.addRawInputListener(this);
                NGEGui.initialize(assetManager);

                NGEStyle.installAndUse();

                NGEGui.register(getGuiViewPort(), true);
                // GuiGlobals.initialize(this);

                // NGEGui.register(getGuiViewPort());
                // BaseStyles.loadGlassStyle();

                inputHandler = new DefaultNavigatorInputHandler(getGuiViewPort());
                inputHandler.registerListener(inputManager);

                NGEGui.get(getGuiViewPort()).setImeComposer(new PhysicalKeyboardImeComposer(inputManager));
        }

        @Override
        public void simpleUpdate(float tpf) {
                NGEGui.update(guiViewPort, tpf);

        }

        @Override
        public void beginInput() {

        }

        @Override
        public void endInput() {

        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
                inputHandler.setInputDevice(inputManager, evt.getDevice());
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
                // System.out.println("Set input device "+evt.getDevice());
                inputHandler.setInputDevice(inputManager, evt.getDevice());
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
                // System.out.println("Set input device "+evt.getDevice());
                inputHandler.setInputDevice(inputManager, evt.getDevice());
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
                // System.out.println("Set input device "+evt.getDevice());
                inputHandler.setInputDevice(inputManager, evt.getDevice());
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
                // System.out.println("Set input device "+evt.getDevice());
                inputHandler.setInputDevice(inputManager, evt.getDevice());
                if (evt.isPressed() && evt.getKeyCode() == KeyInput.KEY_RCONTROL) {
                        Navigator nav = NGEGui.get(getGuiViewPort()).getNavigator();
                        if (!nav.isCursorVisible()) {
                                nav.setCursor(true);

                        } else {
                                nav.setCursor(false);
                        }

                }
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
                // System.out.println("Set input device "+evt.getDevice());
                inputHandler.setInputDevice(inputManager, evt.getDevice());
        }

}
