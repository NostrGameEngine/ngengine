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

package org.ngengine.web.input;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.KeyboardEvent;

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.virtual.VirtualKeyboard;
import org.ngengine.web.WebBinds;

public class WebKeyInput implements KeyInput {
    private boolean initialized = false;
    private RawInputListener listener;

    private final ArrayDeque<KeyInputEvent> keyEvents = new ArrayDeque<>();
    private final Set<Integer> pressedKeys = new HashSet<>();
    @SuppressWarnings("rawtypes")
    private EventListener webListener = new EventListener() {
        @Override
        public void handleEvent(Event evt) {
            handleWebEvent(evt);
        }
    };
    public WebKeyInput() {
   }

    @Override
    public void initialize() {
       
 
        WebBinds.addInputEventListener("keyup", webListener);
        WebBinds.addInputEventListener("keydown", webListener);
        WebBinds.addInputEventListener("textinput", webListener);
        WebBinds.addInputEventListener("inputreset", webListener);
        initialized = true;
    }

    @Override
    public void update() {
        VirtualKeyboard.getInstance().dispatchEvents(listener);
        KeyInputEvent event;
        while ((event = keyEvents.poll()) != null) {
            if (listener != null) listener.onKeyEvent(event);
        }
    }

    @Override
    public void destroy() {
        WebBinds.removeInputEventListener("keyup", webListener);
        WebBinds.removeInputEventListener("keydown", webListener);
        WebBinds.removeInputEventListener("textinput", webListener);
        WebBinds.removeInputEventListener("inputreset", webListener);
        keyEvents.clear();
        pressedKeys.clear();
        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        
        return System.nanoTime();

    }

    private void handleWebEvent(Event evt) {
        if ("textinput".equals(evt.getType())) {
            handleTextInput((JSWebTextInputEvent) evt);
            return;
        }
        if ("inputreset".equals(evt.getType())) {
            releasePressedKeys();
            return;
        }

        long time = getInputTimeNanos();
        KeyboardEvent ev = (KeyboardEvent) evt;
        String keyCode = ev.getCode();
        String keyCharCode = ev.getKey();
        
        int jmeKeyCode=KeyMapper.jsCodeToJme(keyCode);
        char jmeKeyChar = keyCharCode != null && keyCharCode.length() == 1 ? keyCharCode.charAt(0) : '\0';
        boolean isPressed = ev.getType().equals("keydown");

         
        enqueue(jmeKeyCode, jmeKeyChar, isPressed, ev.isRepeat(), time);
        if (jmeKeyCode != KeyInput.KEY_UNKNOWN) {
            if (isPressed) {
                pressedKeys.add(jmeKeyCode);
            } else {
                pressedKeys.remove(jmeKeyCode);
            }
        }
    }

    private void handleTextInput(JSWebTextInputEvent event) {
        String inputType = event.getInputType();
        long time = getInputTimeNanos();
        if (inputType != null && inputType.startsWith("deleteContentBackward")) {
            enqueuePressAndRelease(KeyInput.KEY_BACK, '\0', time);
            return;
        }
        if (inputType != null && inputType.startsWith("deleteContentForward")) {
            enqueuePressAndRelease(KeyInput.KEY_DELETE, '\0', time);
            return;
        }

        String data = event.getData();
        if (data == null || data.isEmpty()) {
            return;
        }
        for (int i = 0; i < data.length(); i++) {
            char character = data.charAt(i);
            int keyCode = character == '\n' ? KeyInput.KEY_RETURN : KeyInput.KEY_UNKNOWN;
            enqueuePressAndRelease(keyCode, character, time);
        }
    }

    private void releasePressedKeys() {
        long time = getInputTimeNanos();
        for (Integer keyCode : pressedKeys) {
            enqueue(keyCode, '\0', false, false, time);
        }
        pressedKeys.clear();
    }

    private void enqueuePressAndRelease(int keyCode, char character, long time) {
        enqueue(keyCode, character, true, false, time);
        enqueue(keyCode, character, false, false, time);
    }

    private void enqueue(int keyCode, char character, boolean pressed, boolean repeating, long time) {
        KeyInputEvent event = new KeyInputEvent(keyCode, character, pressed, repeating);
        event.setTime(time);
        keyEvents.add(event);
    }

    @Override
    public String getKeyName(int key) {
        return KeyMapper.getKeyNameJme(key);
    }
    
}
