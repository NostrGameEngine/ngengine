/*
 * Copyright (c) 2026, Nostr Game Engine
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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.input.icons;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickButton;
import com.jme3.texture.Texture;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves controller button icons from normalized joystick button ids.
 */
public final class JoystickButtonIcons {

    private static final String BASE = "com/jme3/input/icons/";
    private static final Map<String, Texture> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> XBOX = new HashMap<>();
    private static final Map<String, String> PLAYSTATION = new HashMap<>();
    private static final Map<String, String> SWITCH = new HashMap<>();

    static {
        XBOX.put(JoystickButton.BUTTON_XBOX_A, "xbox_button_color_a.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_B, "xbox_button_color_b.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_X, "xbox_button_color_x.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_Y, "xbox_button_color_y.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_LB, "xbox_lb.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_RB, "xbox_rb.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_LT, "xbox_lt.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_RT, "xbox_rt.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_BACK, "xbox_button_view.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_START, "xbox_button_menu.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_L3, "xbox_ls.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_R3, "xbox_rs.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_DPAD_UP, "xbox_dpad_up.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_DPAD_DOWN, "xbox_dpad_down.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_DPAD_LEFT, "xbox_dpad_left.png");
        XBOX.put(JoystickButton.BUTTON_XBOX_DPAD_RIGHT, "xbox_dpad_right.png");

        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_A, "playstation_button_color_cross.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_B, "playstation_button_color_circle.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_X, "playstation_button_color_square.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_Y, "playstation_button_color_triangle.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_LB, "playstation_trigger_l1.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_RB, "playstation_trigger_r1.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_LT, "playstation_trigger_l2.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_RT, "playstation_trigger_r2.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_BACK, "playstation4_button_share.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_START, "playstation4_button_options.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_L3, "playstation_button_l3.png");
        PLAYSTATION.put(JoystickButton.BUTTON_XBOX_R3, "playstation_button_r3.png");

        SWITCH.put(JoystickButton.BUTTON_XBOX_A, "switch_button_b.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_B, "switch_button_a.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_X, "switch_button_y.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_Y, "switch_button_x.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_LB, "switch_button_l.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_RB, "switch_button_r.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_LT, "switch_button_zl.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_RT, "switch_button_zr.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_BACK, "switch_button_minus.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_START, "switch_button_plus.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_L3, "switch_stick_l_press.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_R3, "switch_stick_r_press.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_DPAD_UP, "switch_dpad_up.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_DPAD_DOWN, "switch_dpad_down.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_DPAD_LEFT, "switch_dpad_left.png");
        SWITCH.put(JoystickButton.BUTTON_XBOX_DPAD_RIGHT, "switch_dpad_right.png");
    }

    private JoystickButtonIcons() {
    }

    public static String getIconPath(JoystickButton button) {
        if (button == null) return null;
        return getIconPath(button.getJoystick(), button.getLogicalId(), button.getName());
    }

    public static String getIconPath(Joystick joystick, String logicalId, String label) {
        if (logicalId == null) return null;

        String family = family(joystick, label);
        Map<String, String> icons;
        String dir;
        if ("playstation".equals(family)) {
            icons = PLAYSTATION;
            dir = "playstation/";
        } else if ("switch".equals(family)) {
            icons = SWITCH;
            dir = "switch/";
        } else {
            icons = XBOX;
            dir = "xbox/";
        }

        String file = icons.get(logicalId);
        if (file == null) {
            file = XBOX.get(logicalId);
            dir = "xbox/";
        }
        return file == null ? null : BASE + dir + file;
    }

    public static Texture getIcon(AssetManager assetManager, JoystickButton button) {
        String path = getIconPath(button);
        return getIcon(assetManager, path);
    }

    public static Texture getIcon(AssetManager assetManager, Joystick joystick, String logicalId, String label) {
        String path = getIconPath(joystick, logicalId, label);
        return getIcon(assetManager, path);
    }

    private static Texture getIcon(AssetManager assetManager, String path) {
        if (assetManager == null || path == null) return null;
        try {
            return TEXTURE_CACHE.computeIfAbsent(path, assetManager::loadTexture);
        } catch (AssetNotFoundException e) {
            return null;
        }
    }

    private static String family(Joystick joystick, String label) {
        String name = joystick == null ? "" : joystick.getName();
        String haystack = (name + " " + (label == null ? "" : label)).toLowerCase(Locale.ROOT);
        if (haystack.contains("playstation") || haystack.contains("dualshock")
                || haystack.contains("dualsense") || haystack.contains("cross")
                || haystack.contains("circle") || haystack.contains("square")
                || haystack.contains("triangle")) {
            return "playstation";
        }
        if (haystack.contains("switch") || haystack.contains("joy-con")
                || haystack.contains("joycon") || haystack.contains("nintendo")) {
            return "switch";
        }
        return "xbox";
    }
}
