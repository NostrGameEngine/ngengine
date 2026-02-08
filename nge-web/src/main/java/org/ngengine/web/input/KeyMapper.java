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

package org.ngengine.web.input;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;

public class KeyMapper {

    private static final Map<String, Integer> KEY_MAP = new HashMap<>();

    static {
        // Control / navigation
        KEY_MAP.put("Escape", KeyInput.KEY_ESCAPE);
        KEY_MAP.put("Backspace", KeyInput.KEY_BACK);
        KEY_MAP.put("Tab", KeyInput.KEY_TAB);
        KEY_MAP.put("Enter", KeyInput.KEY_RETURN);
        KEY_MAP.put("Space", KeyInput.KEY_SPACE);
        KEY_MAP.put("CapsLock", KeyInput.KEY_CAPITAL);
        KEY_MAP.put("Insert", KeyInput.KEY_INSERT);
        KEY_MAP.put("Delete", KeyInput.KEY_DELETE);
        KEY_MAP.put("Home", KeyInput.KEY_HOME);
        KEY_MAP.put("End", KeyInput.KEY_END);
        KEY_MAP.put("PageUp", KeyInput.KEY_PGUP);
        KEY_MAP.put("PageDown", KeyInput.KEY_PGDN);

        // Arrow keys
        KEY_MAP.put("ArrowLeft", KeyInput.KEY_LEFT);
        KEY_MAP.put("ArrowRight", KeyInput.KEY_RIGHT);
        KEY_MAP.put("ArrowUp", KeyInput.KEY_UP);
        KEY_MAP.put("ArrowDown", KeyInput.KEY_DOWN);

        // Modifier keys
        KEY_MAP.put("ShiftLeft", KeyInput.KEY_LSHIFT);
        KEY_MAP.put("ShiftRight", KeyInput.KEY_RSHIFT);
        KEY_MAP.put("ControlLeft", KeyInput.KEY_LCONTROL);
        KEY_MAP.put("ControlRight", KeyInput.KEY_RCONTROL);
        KEY_MAP.put("AltLeft", KeyInput.KEY_LMENU);
        KEY_MAP.put("AltRight", KeyInput.KEY_RMENU);
        KEY_MAP.put("MetaLeft", KeyInput.KEY_LMETA);
        KEY_MAP.put("MetaRight", KeyInput.KEY_RMETA);

        // Letters
        KEY_MAP.put("KeyA", KeyInput.KEY_A);
        KEY_MAP.put("KeyB", KeyInput.KEY_B);
        KEY_MAP.put("KeyC", KeyInput.KEY_C);
        KEY_MAP.put("KeyD", KeyInput.KEY_D);
        KEY_MAP.put("KeyE", KeyInput.KEY_E);
        KEY_MAP.put("KeyF", KeyInput.KEY_F);
        KEY_MAP.put("KeyG", KeyInput.KEY_G);
        KEY_MAP.put("KeyH", KeyInput.KEY_H);
        KEY_MAP.put("KeyI", KeyInput.KEY_I);
        KEY_MAP.put("KeyJ", KeyInput.KEY_J);
        KEY_MAP.put("KeyK", KeyInput.KEY_K);
        KEY_MAP.put("KeyL", KeyInput.KEY_L);
        KEY_MAP.put("KeyM", KeyInput.KEY_M);
        KEY_MAP.put("KeyN", KeyInput.KEY_N);
        KEY_MAP.put("KeyO", KeyInput.KEY_O);
        KEY_MAP.put("KeyP", KeyInput.KEY_P);
        KEY_MAP.put("KeyQ", KeyInput.KEY_Q);
        KEY_MAP.put("KeyR", KeyInput.KEY_R);
        KEY_MAP.put("KeyS", KeyInput.KEY_S);
        KEY_MAP.put("KeyT", KeyInput.KEY_T);
        KEY_MAP.put("KeyU", KeyInput.KEY_U);
        KEY_MAP.put("KeyV", KeyInput.KEY_V);
        KEY_MAP.put("KeyW", KeyInput.KEY_W);
        KEY_MAP.put("KeyX", KeyInput.KEY_X);
        KEY_MAP.put("KeyY", KeyInput.KEY_Y);
        KEY_MAP.put("KeyZ", KeyInput.KEY_Z);

        // Digits (top row)
        KEY_MAP.put("Digit0", KeyInput.KEY_0);
        KEY_MAP.put("Digit1", KeyInput.KEY_1);
        KEY_MAP.put("Digit2", KeyInput.KEY_2);
        KEY_MAP.put("Digit3", KeyInput.KEY_3);
        KEY_MAP.put("Digit4", KeyInput.KEY_4);
        KEY_MAP.put("Digit5", KeyInput.KEY_5);
        KEY_MAP.put("Digit6", KeyInput.KEY_6);
        KEY_MAP.put("Digit7", KeyInput.KEY_7);
        KEY_MAP.put("Digit8", KeyInput.KEY_8);
        KEY_MAP.put("Digit9", KeyInput.KEY_9);

        // Punctuation
        KEY_MAP.put("Minus", KeyInput.KEY_MINUS);
        KEY_MAP.put("Equal", KeyInput.KEY_EQUALS);
        KEY_MAP.put("BracketLeft", KeyInput.KEY_LBRACKET);
        KEY_MAP.put("BracketRight", KeyInput.KEY_RBRACKET);
        KEY_MAP.put("Backslash", KeyInput.KEY_BACKSLASH);
        KEY_MAP.put("Semicolon", KeyInput.KEY_SEMICOLON);
        KEY_MAP.put("Quote", KeyInput.KEY_APOSTROPHE);
        KEY_MAP.put("Backquote", KeyInput.KEY_GRAVE);
        KEY_MAP.put("Comma", KeyInput.KEY_COMMA);
        KEY_MAP.put("Period", KeyInput.KEY_PERIOD);
        KEY_MAP.put("Slash", KeyInput.KEY_SLASH);

        // Function keys
        KEY_MAP.put("F1", KeyInput.KEY_F1);
        KEY_MAP.put("F2", KeyInput.KEY_F2);
        KEY_MAP.put("F3", KeyInput.KEY_F3);
        KEY_MAP.put("F4", KeyInput.KEY_F4);
        KEY_MAP.put("F5", KeyInput.KEY_F5);
        KEY_MAP.put("F6", KeyInput.KEY_F6);
        KEY_MAP.put("F7", KeyInput.KEY_F7);
        KEY_MAP.put("F8", KeyInput.KEY_F8);
        KEY_MAP.put("F9", KeyInput.KEY_F9);
        KEY_MAP.put("F10", KeyInput.KEY_F10);
        KEY_MAP.put("F11", KeyInput.KEY_F11);
        KEY_MAP.put("F12", KeyInput.KEY_F12);

        // Numpad
        KEY_MAP.put("Numpad0", KeyInput.KEY_NUMPAD0);
        KEY_MAP.put("Numpad1", KeyInput.KEY_NUMPAD1);
        KEY_MAP.put("Numpad2", KeyInput.KEY_NUMPAD2);
        KEY_MAP.put("Numpad3", KeyInput.KEY_NUMPAD3);
        KEY_MAP.put("Numpad4", KeyInput.KEY_NUMPAD4);
        KEY_MAP.put("Numpad5", KeyInput.KEY_NUMPAD5);
        KEY_MAP.put("Numpad6", KeyInput.KEY_NUMPAD6);
        KEY_MAP.put("Numpad7", KeyInput.KEY_NUMPAD7);
        KEY_MAP.put("Numpad8", KeyInput.KEY_NUMPAD8);
        KEY_MAP.put("Numpad9", KeyInput.KEY_NUMPAD9);
        KEY_MAP.put("NumpadAdd", KeyInput.KEY_ADD);
        KEY_MAP.put("NumpadSubtract", KeyInput.KEY_SUBTRACT);
        KEY_MAP.put("NumpadMultiply", KeyInput.KEY_MULTIPLY);
        KEY_MAP.put("NumpadDivide", KeyInput.KEY_DIVIDE);
        KEY_MAP.put("NumpadDecimal", KeyInput.KEY_DECIMAL);
        KEY_MAP.put("NumpadEnter", KeyInput.KEY_NUMPADENTER);
    }

    public static int jsCodeToJme(String code) {
        return KEY_MAP.getOrDefault(code, KeyInput.KEY_UNKNOWN);
    }

    public static String getKeyNameJme(int jmeKeyCode) {
        for (Map.Entry<String, Integer> entry : KEY_MAP.entrySet()) {
            if (entry.getValue() == jmeKeyCode) {
                return entry.getKey();
            }
        }
        return "UNKNOWN";
    }

    public static int jsMouseButtonToJme(int mouseButton) {
        switch (mouseButton) {
            case 0:
                return MouseInput.BUTTON_LEFT;
            case 1:
                return MouseInput.BUTTON_MIDDLE;
            case 2:
                return MouseInput.BUTTON_RIGHT;
            default:
                return -1;
        }
    }
}
