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

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;

import org.ngengine.web.WebBinds;
import org.ngengine.web.context.WebCanvasElement;

public class WebMouseInput implements MouseInput {
    private static final int MOUSE_POINTER_ID = -1;
    private boolean cursorVisible = true;
    private RawInputListener listener;
    private final WebJoyInput joyInput;
    private int xPos = 0, yPos = 0, wheelPos;
    private boolean undefinedPos = true;
    private boolean initialized = false;
    private final List<MouseMotionEvent> mouseMotionEvents = new ArrayList<>();
    private final List<MouseButtonEvent> mouseButtonEvents = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    private EventListener webListener = new EventListener() {
        @Override
        public void handleEvent(Event evt) {
            handleWebEvent(evt);
        }
    };
    public WebMouseInput(WebJoyInput joyInput) {
        this.joyInput = joyInput;
    }

    @Override
    public void initialize() {

        WebBinds.addInputEventListener("mousemove", webListener);
        WebBinds.addInputEventListener("wheel", webListener);
        WebBinds.addInputEventListener("mousedown", webListener);
        WebBinds.addInputEventListener("mouseup", webListener); 

        initialized = true;
    }

    @Override
    public void destroy() {

        WebBinds.removeInputEventListener("mousemove", webListener);
        WebBinds.removeInputEventListener("wheel", webListener);
        WebBinds.removeInputEventListener("mousedown", webListener);
        WebBinds.removeInputEventListener("mouseup", webListener);

        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    private void handleWebEvent(Event evt) {
        if (listener == null) return;
        if (evt.getType().equals("mousemove")) {
            MouseEvent ev = (MouseEvent) evt;

            int dX;
            int dY;

            if (isLocked() && !undefinedPos) { // captured pointer mode
                dX = (int) ev.getMovementX();
                dY = (int) ev.getMovementY();
                xPos += dX;
                yPos += dY;
            } else {
                int x = (int) ev.getClientX();
                int y = (int) ev.getClientY();

                dX = undefinedPos ? 0 : x - xPos;
                dY = undefinedPos ? 0 : y - yPos;
                xPos = x;
                yPos = y;
                undefinedPos = false;
            }

            // if (!(xPos >= 0 && yPos >= 0 && xPos <= canvas.getWidth() && yPos <= canvas.getHeight())) return;
            MouseMotionEvent mme = new MouseMotionEvent((int) xPos, (int) yPos, (int) dX, (int) dY, 0, 0);
            mme.setTime(getInputTimeNanos());
            if (joyInput != null && joyInput.onPointerMove(MOUSE_POINTER_ID, xPos, yPos, mme.getTime())) {
                return;
            }
            mouseMotionEvents.add(mme);

        } else if (evt.getType().equals("wheel")) {
            // if (!(xPos >= 0 && yPos >= 0 && xPos <= canvas.getWidth() && yPos <= canvas.getHeight())) return;

            WheelEvent ev = (WheelEvent) evt;
            float dX = 0;
            float dY = 0;
            double wheelDelta = ev.getDeltaY();
            wheelPos += wheelDelta;

            MouseMotionEvent mme = new MouseMotionEvent((int) xPos, (int) yPos, (int) dX, (int) dY,
                    (int) wheelPos, (int) wheelDelta);
            mme.setTime(getInputTimeNanos());

            mouseMotionEvents.add(mme);

        } else if (evt.getType().equals("mousedown")) {
            // if (!(xPos >= 0 && yPos >= 0 && xPos <= canvas.getWidth() && yPos <= canvas.getHeight())) return;

            MouseEvent ev = (MouseEvent) evt;
            int button = ev.getButton();
            boolean isPressed = true;
            int jmeButton = KeyMapper.jsMouseButtonToJme(button);
            if (jmeButton != -1) {
                long time = getInputTimeNanos();
                if (joyInput != null && joyInput.onPointerDown(MOUSE_POINTER_ID, xPos, yPos, time)) {
                    return;
                }
                MouseButtonEvent mbe = new MouseButtonEvent(jmeButton, isPressed, (int) xPos, (int) yPos);
                mbe.setTime(time);

                mouseButtonEvents.add(mbe);

            }
        } else if (evt.getType().equals("mouseup")) {
            // if (!(xPos >= 0 && yPos >= 0 && xPos <= canvas.getWidth() && yPos <= canvas.getHeight())) return;

            MouseEvent ev = (MouseEvent) evt;
            int button = ev.getButton();
            boolean isPressed = false;
            int jmeButton = KeyMapper.jsMouseButtonToJme(button);
            if (jmeButton != -1) {
                long time = getInputTimeNanos();
                if (joyInput != null && joyInput.onPointerUp(MOUSE_POINTER_ID, xPos, yPos, time)) {
                    return;
                }
                MouseButtonEvent mbe = new MouseButtonEvent(jmeButton, isPressed, (int) xPos, (int) yPos);
                mbe.setTime(time);

                mouseButtonEvents.add(mbe);
            }
        }

    }

    private boolean isLocked() {
        return !cursorVisible;
    }

    @Override
    public void update() {
        boolean lock = !cursorVisible;

        WebBinds.togglePointerLock(lock);

        // if (cursorVisible != canvasCursorVisible) {
        //     undefinedPos = true;

            // if (lock) {
            // PointerLockOptions options = PointerLockOptions.create();
            // // options.setUnadjustedMovement(true);
            // canvas.requestPointerLock(options);
            // } else
            // canvas.exitPointerLock();
        // }

        for (MouseMotionEvent mme : mouseMotionEvents) {
            listener.onMouseMotionEvent(mme);
        }
        mouseMotionEvents.clear();

        for (MouseButtonEvent mbe : mouseButtonEvents) {
            listener.onMouseButtonEvent(mbe);
        }
        mouseButtonEvents.clear();
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }

    @Override
    public void setCursorVisible(boolean visible) {
        cursorVisible = visible;
    }

    @Override
    public int getButtonCount() {
        return 3;
    }

    @Override
    public void setNativeCursor(JmeCursor cursor) {
        // unsupported
    }

}
