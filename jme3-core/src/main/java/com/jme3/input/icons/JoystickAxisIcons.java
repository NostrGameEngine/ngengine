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
import com.jme3.input.JoystickAxis;
import com.jme3.texture.Texture;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves controller axis icons from normalized joystick axis ids.
 */
public final class JoystickAxisIcons {

    private static final String BASE = "com/jme3/input/icons/";
    private static final Map<String, Texture> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> XBOX = new HashMap<>();
    private static final Map<String, String> PLAYSTATION = new HashMap<>();
    private static final Map<String, String> SWITCH = new HashMap<>();

    static {
        XBOX.put(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, "xbox_stick_l_horizontal.png");
        XBOX.put(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y, "xbox_stick_l_vertical.png");
        XBOX.put(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X, "xbox_stick_r_horizontal.png");
        XBOX.put(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y, "xbox_stick_r_vertical.png");
        XBOX.put(JoystickAxis.AXIS_XBOX_LEFT_TRIGGER, "xbox_lt.png");
        XBOX.put(JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER, "xbox_rt.png");
        XBOX.put(JoystickAxis.POV_X, "xbox_dpad_horizontal.png");
        XBOX.put(JoystickAxis.POV_Y, "xbox_dpad_vertical.png");

        PLAYSTATION.put(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, "playstation_stick_l_horizontal.png");
        PLAYSTATION.put(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y, "playstation_stick_l_vertical.png");
        PLAYSTATION.put(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X, "playstation_stick_r_horizontal.png");
        PLAYSTATION.put(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y, "playstation_stick_r_vertical.png");
        PLAYSTATION.put(JoystickAxis.AXIS_XBOX_LEFT_TRIGGER, "playstation_trigger_l2.png");
        PLAYSTATION.put(JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER, "playstation_trigger_r2.png");
        PLAYSTATION.put(JoystickAxis.POV_X, "playstation_dpad_horizontal.png");
        PLAYSTATION.put(JoystickAxis.POV_Y, "playstation_dpad_vertical.png");

        SWITCH.put(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, "switch_stick_l_horizontal.png");
        SWITCH.put(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y, "switch_stick_l_vertical.png");
        SWITCH.put(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X, "switch_stick_r_horizontal.png");
        SWITCH.put(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y, "switch_stick_r_vertical.png");
        SWITCH.put(JoystickAxis.AXIS_XBOX_LEFT_TRIGGER, "switch_button_zl.png");
        SWITCH.put(JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER, "switch_button_zr.png");
        SWITCH.put(JoystickAxis.POV_X, "switch_dpad_horizontal.png");
        SWITCH.put(JoystickAxis.POV_Y, "switch_dpad_vertical.png");
    }

    private JoystickAxisIcons() {
    }

    public static String getIconPath(JoystickAxis axis) {
        if (axis == null) return null;
        return getIconPath(axis.getJoystick(), axis.getLogicalId(), axis.getName());
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

    public static Texture getIcon(AssetManager assetManager, JoystickAxis axis) {
        String path = getIconPath(axis);
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
