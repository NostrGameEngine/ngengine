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
 * SERVICES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package org.ngengine.gui.ime;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.gui.GuiContext;

import com.jme3.input.Joystick;
import com.jme3.input.InputManager;
import com.jme3.system.JmeSystem;

/**
 * IME composer that asks the active JmeSystem backend to show the native software keyboard.
 */
public class JmeSoftKeyboardImeComposer extends PhysicalKeyboardImeComposer {

    private static final Logger log = Logger.getLogger(JmeSoftKeyboardImeComposer.class.getName());
    private boolean softKeyboardVisible;

    public JmeSoftKeyboardImeComposer(InputManager inputManager) {
        super(inputManager);
    }

    @Override
    public void open(GuiContext ctx, Consumer<ImeCompositionEvent> listener, ImeCompositionEvent event,
            Function<Character, Character> inputFilter, Function<String, Float> getLineWidth) {
        super.open(ctx, listener, event, inputFilter, getLineWidth);
        if (ctx.getInputDevice() instanceof Joystick) {
            setSoftKeyboardVisible(true);
            softKeyboardVisible = true;
        }
    }

    @Override
    public void close() {
        boolean wasOpen = isOpen();
        super.close();
        if (wasOpen && softKeyboardVisible) {
            setSoftKeyboardVisible(false);
            softKeyboardVisible = false;
        }
    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }

    private void setSoftKeyboardVisible(boolean visible) {
        try {
            JmeSystem.showSoftKeyboard(visible);
        } catch (RuntimeException | LinkageError e) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Unable to change software keyboard visibility", e);
            }
        }
    }
}
