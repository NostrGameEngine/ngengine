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
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

package org.ngengine.gui.win;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.renderer.ViewPort;
import com.simsilica.lemur.GuiContext;
import com.simsilica.lemur.NGEGui;
import com.simsilica.lemur.nav.DefaultNavigatorInputHandler;
import com.simsilica.lemur.nav.NavigatorInputHandler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.jme3.AppComponentInitializer.InputActions;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.win.NToast.ToastType;
import org.ngengine.gui.win.std.NErrorWindow;
import org.ngengine.store.DataStoreProvider;

public class NWindowManagerComponent extends AbstractComponent implements LogicFragment, InputHandlerFragment {

    private static final Logger log = Logger.getLogger(NWindowManagerComponent.class.getName());
    private final ArrayList<NWindowManager> windowManagers = new ArrayList<>();
    private Class<? extends NavigatorInputHandler> defaultInputHandlerClass = DefaultNavigatorInputHandler.class;
    private boolean enabled = false;

    public NWindowManager getManager(ViewPort vp){
        return getManager(vp, null);
    }

    public NWindowManager getManager(ViewPort vp, Class<? extends NavigatorInputHandler> inputHandlerClass) {
         if(vp==null){
            ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
            vp= vpm.getGuiViewPort();
        }
        for(NWindowManager manager : windowManagers){
            if(manager.getViewPort() == vp){
                return manager;
            }
        }
        NGEGui.register(vp, true);
        GuiContext ctx = NGEGui.get(vp);
        NWindowManager newmanager = new NWindowManager(this,ctx); 
        windowManagers.add(newmanager);

        if(inputHandlerClass==null){
            inputHandlerClass = defaultInputHandlerClass;
        }

        if(inputHandlerClass!=null){
            try {
                NavigatorInputHandler inputHandler = inputHandlerClass.getConstructor().newInstance();
                newmanager.setInputHandler(inputHandler);

                inputHandler.setPrimaryAction(()->{
                    newmanager.action(0);
                });
                inputHandler.setSecondaryAction(()->{
                    newmanager.toastAction(0);
                });
                inputHandler.setBackAction(()->{
                    newmanager.back();
                });
            } catch (Exception e) {
                log.log(Level.SEVERE,"Failed to create input handler",e);
            }
        }

   
        

        return newmanager;
    }

    public void setDefaultInputHandler(Class<? extends NavigatorInputHandler> inputHandler) {
   
        this.defaultInputHandlerClass = inputHandler;
    }

  
    @Override
    public Component newInstance() {
        return new NWindowManagerComponent();
    }

    public void showCursor(boolean v) {
        getInstanceOf(InputManager.class).setCursorVisible(v);
    }


    public void enqueueInThread(Runnable task) {
        getManager(null).enqueueInThread(task);
    }

    public void runInThread(Runnable task) {
        getManager(null).runInThread(task);
    }

    public DataStoreProvider getDataStoreProvider() {
        return getManager(null).getDataStoreProvider();
    }

    

    public int getWidth() {
        return getManager(null).getWidth();
    }

    public int getHeight() {
        return getManager(null).getHeight();
    }

    




    /**
     * Shows a window of the specified class with the given arguments and a callback and returns a closer
     * function.
     * 
     * @param <T>
     *            the class of the window
     * @param windowClass
     *            the class of the window to show
     * @return an instance of the window
     */
    public  <T extends NWindow<A>, A> T  showWindow(Class<T> windowClass) {
        return getManager(null).showWindow(windowClass);
    }



    /**
     * Shows a window of the specified class with the given arguments and a callback and returns a closer
     * function.
     * 
     * @param <T>
     *            the class of the window
     * @param windowClass
     *            the class of the window to show
     * @param args
     *            the arguments to pass to the window, can be null
     * @return an instance of the window
     */
    public <T extends NWindow<A>, A> T showWindow(Class<T> windowClass, A args) {
        return getManager(null).showWindow(windowClass, args);
    }

    public <T extends NWindow<?>> T getWindow(Class<T> windowClass){
        return getManager(null).getWindow(windowClass);
    }

    public <T extends NWindow<?>> T getWindow(Predicate<NWindow<?>> filter){
        return getManager(null).getWindow(filter);
    }

    public NErrorWindow showFatalError(Throwable exc) {
        return getManager(null).showFatalError(exc);
    }

    public NToast showToast(Throwable exc) {
        return getManager(null).showToast(exc);
    }

    public NToast showToast(Throwable exc, Duration duration) {
        return getManager(null).showToast(exc, duration);
    }

    public NToast showToast(ToastType type, String message) {
        return getManager(null).showToast(type, message);
    }

    public NToast showToast(ToastType type, String message, Duration duration) {
        return getManager(null).showToast(type, message, duration);
    }

    public void closeAllWindows() {
        getManager(null).closeAllWindows();
    }

    public void closeAllToasts() {
        getManager(null).closeAllToasts();
    }

    public void closeAll() {
        getManager(null).closeAll();
    }

    @Override
    public void onEnable(ComponentManager mng,
            boolean firstTime ) {
        enabled = true;
        int width = getWidth();
        int height = getHeight();
        NGEStyle.installAndUse(width,height);

        InputManager inputManager = getInstanceOf(InputManager.class);
        NWindowManager m = getManager(null);
    }

    @Override
    public void onDisable(ComponentManager mng) {
        enabled = false;
        for(NWindowManager manager : windowManagers) {
            manager.closeAll();
            manager.setInputHandler(null);
        }
 
    }

    public void setInputDevice(InputDevice device) {
        getManager(null).setInputDevice(device);
    }

    
    @Override
    public void updateAppLogic(ComponentManager mng, float tpf){
        for(NWindowManager manager : windowManagers){
            manager.update(tpf);
        }
    }

    public void back() {
        getManager(null).back();
    }

    public void action(int id) {
        getManager(null).action(id);
    }

    public void toastAction(int id) {
        getManager(null).toastAction(id);
    }

 
    @Override
    public void onInputDeviceConnected(ComponentManager mng, InputManager inputManager, InputActions actions, InputDevice device) {
    }

    @Override
    public void onInputDeviceDisconnected(ComponentManager mng, InputManager inputManager, InputActions actions, InputDevice device) {
    
    }

    @Override
    public void onInputAction(ComponentManager mng, String action, boolean toggled,  float value, InputEvent<?> event,
            float tpf) {
         
    }

    // any input action will set the input device as 
    // current for the main window manager
    public void onJoyAxisEvent(ComponentManager mng, JoyAxisEvent evt) {
        setInputDevice(evt.getDevice());
    }

    public void onJoyButtonEvent(ComponentManager mng, JoyButtonEvent evt) {
        setInputDevice(evt.getDevice());
    }

    public void onMouseMotionEvent(ComponentManager mng, MouseMotionEvent evt) {
        setInputDevice(evt.getDevice());
    }

    public void onMouseButtonEvent(ComponentManager mng, MouseButtonEvent evt) {
        setInputDevice(evt.getDevice());
    }

    public void onKeyEvent(ComponentManager mng, KeyInputEvent evt) {
        setInputDevice(evt.getDevice());
        
    }

    public void onTouchEvent(ComponentManager mng, TouchEvent evt) {
        setInputDevice(evt.getDevice());
    }
}
