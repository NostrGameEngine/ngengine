/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.virtual.VirtualKeyboard;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;

/**
 * Manual test for the desktop on-screen virtual keyboard.
 */
public class TestVirtualKeyboard extends SimpleApplication {

    private static final String TOGGLE_KEYBOARD = "ToggleVirtualKeyboard";
    private static final String TOGGLE_STYLE = "ToggleVirtualKeyboardStyle";

    private final StringBuilder text = new StringBuilder();
    private BitmapText statusText;
    private BitmapText inputText;
    private boolean keyboardVisible;
    private VirtualKeyboard.IconStyle iconStyle = VirtualKeyboard.IconStyle.OUTLINE;

    public static void main(String[] args) {
        TestVirtualKeyboard app = new TestVirtualKeyboard();
        AppSettings settings = new AppSettings(true);
        settings.setUseJoysticks(true);
        settings.setJoysticksMapper(AppSettings.JOYSTICKS_XBOX_MAPPER);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);
        setDisplayFps(false);
        inputManager.setCursorVisible(true);
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(new ColorRGBA(0.07f, 0.09f, 0.12f, 1f));

        statusText = new BitmapText(guiFont);
        statusText.setLocalTranslation(24f, cam.getHeight() - 24f, 0f);
        guiNode.attachChild(statusText);

        inputText = new BitmapText(guiFont);
        inputText.setLocalTranslation(24f, cam.getHeight() - 88f, 0f);
        guiNode.attachChild(inputText);

        inputManager.addMapping(TOGGLE_KEYBOARD, new KeyTrigger(KeyInput.KEY_F1));
        inputManager.addMapping(TOGGLE_STYLE, new KeyTrigger(KeyInput.KEY_F2));
        inputManager.addListener(actionListener, TOGGLE_KEYBOARD, TOGGLE_STYLE);
        inputManager.addRawInputListener(keyListener);

        keyboardVisible = true;
        JmeSystem.showSoftKeyboard(true);
        updateHud();
    }

    @Override
    public void destroy() {
        JmeSystem.showSoftKeyboard(false);
        super.destroy();
    }

    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        if (!isPressed) {
            return;
        }
        if (TOGGLE_KEYBOARD.equals(name)) {
            keyboardVisible = !keyboardVisible;
            JmeSystem.showSoftKeyboard(keyboardVisible);
        } else if (TOGGLE_STYLE.equals(name)) {
            iconStyle = iconStyle == VirtualKeyboard.IconStyle.OUTLINE
                    ? VirtualKeyboard.IconStyle.FULL
                    : VirtualKeyboard.IconStyle.OUTLINE;
            VirtualKeyboard.getInstance().setIconStyle(iconStyle);
        }
        updateHud();
    };

    private final RawInputListenerAdapter keyListener = new RawInputListenerAdapter() {
        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if (!evt.isPressed() || evt.isRepeating()) {
                return;
            }

            int code = evt.getKeyCode();
            if (code == KeyInput.KEY_F1 || code == KeyInput.KEY_F2) {
                return;
            }
            if (code == KeyInput.KEY_BACK) {
                if (text.length() > 0) {
                    text.deleteCharAt(text.length() - 1);
                }
            } else if (code == KeyInput.KEY_RETURN || code == KeyInput.KEY_ESCAPE) {
                keyboardVisible = false;
                JmeSystem.showSoftKeyboard(false);
            } else if (evt.getKeyChar() >= ' ' && !Character.isISOControl(evt.getKeyChar())) {
                text.append(evt.getKeyChar());
            }
            updateHud();
        }
    };

    private void updateHud() {
        statusText.setText("F1: keyboard " + (keyboardVisible ? "on" : "off")
                + " | F2: " + iconStyle
                + " | mouse clicks or controller D-pad/stick + A/B");
        inputText.setText("Input: " + text);
    }
}
