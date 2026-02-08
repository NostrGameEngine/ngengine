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

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.KeyboardEvent;

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import org.ngengine.web.WebBinds;

public class WebKeyInput implements KeyInput {
    private boolean initialized = false;
    private RawInputListener listener;

    private final List<KeyInputEvent> keyEvents = new ArrayList<>();
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
        initialized = true;
    }

    @Override
    public void update() {
        for (KeyInputEvent evt : keyEvents) {
            if (listener != null) {
                listener.onKeyEvent(evt);
            }
        }
        keyEvents.clear();
    }

    @Override
    public void destroy() {
        WebBinds.removeInputEventListener("keyup", webListener);
        WebBinds.removeInputEventListener("keydown", webListener);      
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
        long time = getInputTimeNanos();
        KeyboardEvent ev = (KeyboardEvent) evt;
        String keyCode = ev.getCode();
        String keyCharCode = ev.getKey();
        
        int jmeKeyCode=KeyMapper.jsCodeToJme(keyCode);
        char jmeKeyChar = keyCharCode.length() == 1 ? keyCharCode.charAt(0) : '\0';
        boolean isPressed = ev.getType().equals("keydown");

         
        KeyInputEvent jmeEvent=new KeyInputEvent(jmeKeyCode, jmeKeyChar, isPressed, false);
        jmeEvent.setTime(time);
        keyEvents.add(jmeEvent);
    }

    @Override
    public String getKeyName(int key) {
        return KeyMapper.getKeyNameJme(key);
    }
    
}
