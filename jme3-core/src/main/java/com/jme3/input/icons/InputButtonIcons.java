/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 */
package com.jme3.input.icons;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.input.KeyNames;
import com.jme3.input.MouseInput;
import com.jme3.texture.Texture;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InputButtonIcons {

    private static final String BASE = "com/jme3/input/icons/keyboard-mouse/";
    private static final ConcurrentMap<String, Texture> TEXTURE_CACHE = new ConcurrentHashMap<>();

    private InputButtonIcons() {
    }

    public static String getKeyLabel(int keyId) {
        return KeyNames.getName(keyId);
    }

    public static String getKeyIconPath(int keyId) {
        String label = getKeyLabel(keyId);
        if (label == null) return null;
        String file = keyFileName(label);
        return file == null ? null : BASE + file;
    }

    public static Texture getKeyIcon(AssetManager assetManager, int keyId) {
        return load(assetManager, getKeyIconPath(keyId));
    }

    public static String getMouseButtonLabel(int buttonId) {
        switch (buttonId) {
            case MouseInput.BUTTON_LEFT: return "Left Mouse";
            case MouseInput.BUTTON_RIGHT: return "Right Mouse";
            case MouseInput.BUTTON_MIDDLE: return "Middle Mouse";
            default: return "Mouse " + buttonId;
        }
    }

    public static String getMouseButtonIconPath(int buttonId) {
        switch (buttonId) {
            case MouseInput.BUTTON_LEFT: return BASE + "mouse_left.png";
            case MouseInput.BUTTON_RIGHT: return BASE + "mouse_right.png";
            case MouseInput.BUTTON_MIDDLE: return BASE + "mouse_scroll.png";
            default: return BASE + "mouse.png";
        }
    }

    public static Texture getMouseButtonIcon(AssetManager assetManager, int buttonId) {
        return load(assetManager, getMouseButtonIconPath(buttonId));
    }

    private static Texture load(AssetManager assetManager, String path) {
        if (assetManager == null || path == null) return null;
        try {
            return TEXTURE_CACHE.computeIfAbsent(path, assetManager::loadTexture);
        } catch (AssetNotFoundException e) {
            return null;
        }
    }

    private static String keyFileName(String label) {
        String key = label.toLowerCase(Locale.ROOT).replace(" ", "");
        if ("esc".equals(key)) key = "escape";
        if ("leftctrl".equals(key) || "rightctrl".equals(key)) key = "ctrl";
        if ("leftshift".equals(key) || "rightshift".equals(key)) key = "shift";
        if ("leftalt".equals(key) || "rightalt".equals(key)) key = "alt";
        if ("leftoption".equals(key) || "rightoption".equals(key)) key = "option";
        if ("pageup".equals(key)) key = "page_up";
        if ("pagedown".equals(key)) key = "page_down";
        if ("scrolllock".equals(key)) key = "scroll_lock";
        if ("sysrq".equals(key)) key = "printscreen";
        if ("numpadenter".equals(key)) key = "numpad_enter";
        if ("numpad=".equals(key)) key = "equals";
        if ("numpad,".equals(key)) key = "comma";
        if ("numpad/".equals(key)) key = "slash_forward";
        if ("numpad-".equals(key)) key = "minus";
        if ("numpad.".equals(key)) key = "period";
        if ("-".equals(key)) key = "minus";
        if ("=".equals(key)) key = "equals";
        if ("[".equals(key)) key = "bracket_open";
        if ("]".equals(key)) key = "bracket_close";
        if (";".equals(key)) key = "semicolon";
        if ("'".equals(key)) key = "apostrophe";
        if ("`".equals(key)) key = "tilde";
        if ("\\".equals(key)) key = "slash_back";
        if (",".equals(key)) key = "comma";
        if (".".equals(key)) key = "period";
        if ("/".equals(key)) key = "slash_forward";
        if ("*".equals(key)) key = "asterisk";
        if ("+".equals(key)) key = "plus";
        if (":".equals(key)) key = "colon";
        if ("_".equals(key)) key = "underscore";
        if ("up".equals(key) || "down".equals(key) || "left".equals(key) || "right".equals(key)) {
            key = "arrow_" + key;
        }
        return "keyboard_" + key + ".png";
    }
}
