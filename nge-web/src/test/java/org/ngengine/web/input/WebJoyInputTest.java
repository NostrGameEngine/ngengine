/*
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.input.JoystickButton;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WebJoyInputTest {

    @Test
    void mapsTheBrowserStandardGamepadButtonsToJmeLogicalIds() {
        String[] expected = {
                JoystickButton.BUTTON_XBOX_A,
                JoystickButton.BUTTON_XBOX_B,
                JoystickButton.BUTTON_XBOX_X,
                JoystickButton.BUTTON_XBOX_Y,
                JoystickButton.BUTTON_XBOX_LB,
                JoystickButton.BUTTON_XBOX_RB,
                JoystickButton.BUTTON_XBOX_LT,
                JoystickButton.BUTTON_XBOX_RT,
                JoystickButton.BUTTON_XBOX_BACK,
                JoystickButton.BUTTON_XBOX_START,
                JoystickButton.BUTTON_XBOX_L3,
                JoystickButton.BUTTON_XBOX_R3,
                JoystickButton.BUTTON_XBOX_DPAD_UP,
                JoystickButton.BUTTON_XBOX_DPAD_DOWN,
                JoystickButton.BUTTON_XBOX_DPAD_LEFT,
                JoystickButton.BUTTON_XBOX_DPAD_RIGHT
        };

        Set<String> logicalIds = new HashSet<>();
        for (int browserButton = 0; browserButton < expected.length; browserButton++) {
            String actual = WebJoyInput.standardButtonLogicalId(browserButton);
            assertEquals(expected[browserButton], actual);
            assertTrue(logicalIds.add(actual), "standard logical IDs must be unique");
        }
        assertEquals("16", WebJoyInput.standardButtonLogicalId(16));
    }
}
