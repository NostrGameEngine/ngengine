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

import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JoystickAxisIconsTest {

    @Test
    void resolvesFamilySpecificAxisIconPaths() {
        assertEquals(
                "com/jme3/input/icons/xbox/xbox_stick_l_horizontal.png",
                JoystickAxisIcons.getIconPath(null, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, null));
        assertResource(JoystickAxisIcons.getIconPath(null, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, null));
        assertEquals(
                "com/jme3/input/icons/playstation/playstation_trigger_r2.png",
                JoystickAxisIcons.getIconPath(new TestJoystick("DualSense Wireless Controller"),
                        JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER, null));
        assertResource(JoystickAxisIcons.getIconPath(new TestJoystick("DualSense Wireless Controller"),
                JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER, null));
        assertEquals(
                "com/jme3/input/icons/switch/switch_dpad_vertical.png",
                JoystickAxisIcons.getIconPath(new TestJoystick("Nintendo Switch Pro Controller"),
                        JoystickAxis.POV_Y, null));
        assertResource(JoystickAxisIcons.getIconPath(new TestJoystick("Nintendo Switch Pro Controller"),
                JoystickAxis.POV_Y, null));
        assertNull(JoystickAxisIcons.getIconPath(null, "unknown", null));
    }

    private static void assertResource(String path) {
        assertNotNull(JoystickAxisIconsTest.class.getClassLoader().getResource(path), path);
    }

    private static final class TestJoystick implements Joystick {
        private final String name;

        private TestJoystick(String name) {
            this.name = name;
        }

        @Override
        public JoystickAxis getAxis(String logicalId) {
            return null;
        }

        @Override
        public java.util.List<JoystickAxis> getAxes() {
            return java.util.Collections.emptyList();
        }

        @Override
        public JoystickButton getButton(String logicalId) {
            return null;
        }

        @Override
        public java.util.List<JoystickButton> getButtons() {
            return java.util.Collections.emptyList();
        }

        @Override
        public JoystickAxis getXAxis() {
            return null;
        }

        @Override
        public JoystickAxis getYAxis() {
            return null;
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return null;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return null;
        }

        @Override
        public int getXAxisIndex() {
            return -1;
        }

        @Override
        public int getYAxisIndex() {
            return -1;
        }

        @Override
        public int getAxisCount() {
            return 0;
        }

        @Override
        public int getButtonCount() {
            return 0;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getJoyId() {
            return 0;
        }

        @Override
        public void assignButton(String mappingName, int buttonId) {
        }

        @Override
        public void assignAxis(String positiveMapping, String negativeMapping, int axisId) {
        }

        @Override
        public void rumble(float amountHigh, float amountLow, float duration) {
        }
    }
}
