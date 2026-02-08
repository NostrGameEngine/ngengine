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
import java.util.function.Supplier;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;

import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.AppSettings;

import org.ngengine.web.WebBinds;
import org.ngengine.web.context.WebCanvasElement;

public class WebTouchInput implements TouchInput{
    private Supplier<WebCanvasElement> canvasSupplier;
    private RawInputListener listener;
    private boolean initialized = false;
    private boolean simulateMouse;
    private boolean simulateKeyboard;
    private boolean flipX = false;
    private boolean flipY = false;
    @SuppressWarnings("rawtypes")
    private EventListener webListener = new EventListener() {
        @Override
        public void handleEvent(Event evt) {
            handleWebEvent(evt);
        }
    };
    private static class TouchStatus {
        boolean undefinedPos;
        int xPos, yPos;
    }
    private final List<TouchStatus> touchStatus = new ArrayList<>();
    private final List<MouseMotionEvent> mouseMotionEvents = new ArrayList<>();
    private final List<MouseButtonEvent> mouseButtonEvents = new ArrayList<>();
    
    private final List<TouchEvent> touchEvents = new ArrayList<>();
    public WebTouchInput(Supplier<WebCanvasElement> canvasSupplier, AppSettings settings) {
        this.canvasSupplier = canvasSupplier;
        this.setSimulateMouse(settings.isEmulateMouse());
        this.setSimulateKeyboard(settings.isEmulateKeyboard());
        flipX = settings.isEmulateMouseFlipX();
        flipY = settings.isEmulateMouseFlipY();
        
    }

    @Override
    public void initialize() {
        WebBinds.addInputEventListener("touchstart", webListener);
        WebBinds.addInputEventListener("touchmove", webListener);
        WebBinds.addInputEventListener("touchcancel", webListener);
        WebBinds.addInputEventListener("touchend", webListener);
  
        initialized = true;
    }
 

    @Override
    public void update() {
        for (TouchEvent te : touchEvents) {
            listener.onTouchEvent(te);
        }
        touchEvents.clear();

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
    public void destroy() {
        WebBinds.removeInputEventListener("touchstart", webListener);
        WebBinds.removeInputEventListener("touchmove", webListener);
        WebBinds.removeInputEventListener("touchcancel", webListener);
        WebBinds.removeInputEventListener("touchend", webListener);

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

    @Override
    public void setSimulateMouse(boolean simulate) {
      simulateMouse=simulate;
    }

    @Override
    public boolean isSimulateMouse() {
        return simulateMouse;
    }

    @Override
    public void setSimulateKeyboard(boolean simulate) {
        simulateKeyboard = simulate;
    }

    @Override
    public boolean isSimulateKeyboard() {
        return simulateKeyboard;
    }

    @Override
    public void setOmitHistoricEvents(boolean dontSendHistory) {
    }
          
    private TouchStatus getTouchStatus(int index) {
        while (touchStatus.size() <= index) {
            touchStatus.add(new TouchStatus());
        }
        return touchStatus.get(index);
    }

    private void scheduleEvent(TouchEvent.Type t, JSTouchEvent ev, boolean simulateMouse) {
        int nTouches = ev.getNumChangedTouches();
        WebCanvasElement canvas = canvasSupplier.get();
        for (int i = 0; i < nTouches; i++) {
            JSTouch touch = ev.getChangedTouch(i);
            TouchStatus s = getTouchStatus((int) touch.getIdentifier());
            int x = touch.getClientX();
            int y = touch.getClientY();
                          

            int dX = s.undefinedPos ? 0 : x - s.xPos;
            int dY = s.undefinedPos ? 0 : y - s.yPos;

            s.xPos = x;
            s.yPos = y;
            s.undefinedPos = false;

            long time = getInputTimeNanos();
            TouchEvent te = new TouchEvent(t, x, y, dX, dY);
            te.setTime(time);
            te.setPressure(touch.getForce());
            touchEvents.add(te);

            if (simulateMouse) {
                if(canvas!=null){
                    if (flipX) {
                        x = canvas.getWidth() - x;
                    }
                    if (flipY) {
                        y = canvas.getHeight() - y;
                    }
                }
                if (t == TouchEvent.Type.DOWN) {
                    MouseButtonEvent mev = new MouseButtonEvent(MouseInput.BUTTON_LEFT, true, x, y);
                    mev.setTime(time);
                    mouseButtonEvents.add(mev);
                } else if (t == TouchEvent.Type.UP) {
                    MouseButtonEvent mev=new MouseButtonEvent(MouseInput.BUTTON_LEFT, false, x, y);
                    mev.setTime(time);
                    mouseButtonEvents.add(mev);
                } else if (t == TouchEvent.Type.MOVE) {
                    MouseMotionEvent mev = new MouseMotionEvent(x, y, dX, dY, 0, 0);
                                        mev.setTime(time);

                    mouseMotionEvents.add(mev);
                }
            }

        }
        // ev.preventDefault();     
    }

    private void handleWebEvent(Event evt) {

        if (evt.getType().equals("touchstart")) {
            scheduleEvent(TouchEvent.Type.DOWN, (JSTouchEvent) evt,this.isSimulateMouse());
        } else if (evt.getType().equals("touchcancel")) {
            scheduleEvent(TouchEvent.Type.UP, (JSTouchEvent) evt,this.isSimulateMouse());
        } else if( evt.getType().equals("touchend")) {
            scheduleEvent(TouchEvent.Type.UP, (JSTouchEvent) evt,this.isSimulateMouse());
        } else if (evt.getType().equals("touchmove")) {
            scheduleEvent(TouchEvent.Type.MOVE, (JSTouchEvent) evt,this.isSimulateMouse());
        }
    }

}
