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

package org.ngengine.gui.guix.win;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.TouchScreen;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

import org.ngengine.gui.nav.DefaultNavigatorInputHandler;
import org.ngengine.gui.nav.NavigatorInputHandler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ReloadableComponent;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.jme3.AppComponentInitializer.InputActions;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.guix.win.NToast.ToastType;
import org.ngengine.store.DataStoreProvider;

public class NWindowManagerComponent extends AbstractComponent implements LogicFragment, InputHandlerFragment, ReloadableComponent {

    private static final Logger log = Logger.getLogger(NWindowManagerComponent.class.getName());
    public static final String RELATIVE_CAMERA_NAME = "NGE GUI Relative";
    public static final int RELATIVE_CAMERA_SCALE = 1000;
    private final ArrayList<NWindowManager> windowManagers = new ArrayList<>();
    private Class<? extends NavigatorInputHandler> defaultInputHandlerClass = DefaultNavigatorInputHandler.class;
    private boolean enabled = false;
    private boolean interactionEnabled = false;
    private boolean interactionActive = false;
    private boolean physicalCursorVisible = false;
    private boolean appliedPhysicalCursorVisible = false;
    private boolean physicalCursorVisibleDirty = true;
    private InputDevice lastInputDevice;
    private final boolean relativeSize;
    private ViewPort defaultGuiViewPort;
    private Node defaultGuiNode;
    private int physicalWidth = 1;
    private int physicalHeight = 1;

    


    public NWindowManagerComponent() {
        this(false);
    }

    public NWindowManagerComponent(boolean relativeSize) {
        this.relativeSize = relativeSize;
    }

    public boolean isRelativeSize() {
        return relativeSize;
    }

    public static boolean isRelativeSize(Camera camera) {
        return camera != null && RELATIVE_CAMERA_NAME.equals(camera.getName());
    }

    public NWindowManager getManager(ViewPort vp){
        return getManager(vp, defaultInputHandlerClass);
    }

    public NWindowManager getManager(ViewPort vp, Class<? extends NavigatorInputHandler> inputHandlerClass) {
         if(vp==null){
            vp = getDefaultGuiViewPort();
        }
        configureDefaultGuiViewPort();
        for(NWindowManager manager : windowManagers){
            if(manager.getViewPort() == vp){
                return manager;
            }
        }
        NGEGui.register(vp, true);
        GuiContext ctx = NGEGui.get(vp);
        NWindowManager newmanager = new NWindowManager(this,ctx); 
        windowManagers.add(newmanager);
 
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

                AppSettings settings = getInstanceOf(AppSettings.class);
                if (settings != null) {
                    boolean useHardwareCursor = settings.isHardwareCursor();
                    newmanager.getContext().getNavigator().setHardwareCursor(useHardwareCursor);
                }

            } catch (Exception e) {
                log.log(Level.SEVERE,"Failed to create input handler",e);
            }
        }

   
        

        return newmanager;
    }

    public ViewPort getDefaultGuiViewPort() {
        ensureDefaultGuiViewPort();
        return defaultGuiViewPort;
    }

    private void ensureDefaultGuiViewPort() {
        if (defaultGuiViewPort != null) {
            configureDefaultGuiViewPort();
            return;
        }
        ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
        if (vpm == null) {
            throw new IllegalStateException("ViewPortManager is required before creating the NGE GUI viewport.");
        }
        updatePhysicalSize(vpm);
        int targetWidth = getTargetCameraWidth();
        int targetHeight = getTargetCameraHeight();
        Camera cam = new Camera(targetWidth, targetHeight);
        if (relativeSize) {
            cam.setName(RELATIVE_CAMERA_NAME);
        }
        configureGuiCamera(cam, targetWidth, targetHeight);
        defaultGuiViewPort = vpm.createNewGuiViewPort("NGE GUI", cam);
        defaultGuiViewPort.setClearFlags(false, false, false);
        defaultGuiNode = new Node("NGE GuiNode");
        defaultGuiNode.setQueueBucket(Bucket.Gui);
        configureGuiNodeScale();
        defaultGuiViewPort.attachScene(defaultGuiNode);
        configureDefaultGuiViewPort();
    }

    private void configureDefaultGuiViewPort() {
        if (defaultGuiViewPort == null) {
            return;
        }
        ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
        if (vpm != null) {
            updatePhysicalSize(vpm);
        }
        defaultGuiViewPort.setRenderTargetSize(physicalWidth, physicalHeight);
        Camera cam = defaultGuiViewPort.getCamera();
        int targetWidth = getTargetCameraWidth();
        int targetHeight = getTargetCameraHeight();
        if (relativeSize) {
            cam.setName(RELATIVE_CAMERA_NAME);
        }
        if (cam.getWidth() != targetWidth || cam.getHeight() != targetHeight) {
            cam.resize(targetWidth, targetHeight, !relativeSize);
            configureGuiCamera(cam, targetWidth, targetHeight);
            configureGuiNodeScale();
            for (NWindowManager manager : windowManagers) {
                if (manager.getViewPort() == defaultGuiViewPort) {
                    manager.invalidateAll();
                }
            }
        } else {
            configureGuiCamera(cam, targetWidth, targetHeight);
            configureGuiNodeScale();
        }
    }

    private int getTargetCameraWidth() {
        if (!relativeSize) {
            return physicalWidth;
        }
        return Math.max(1, Math.round(getRelativeLogicalWidth() * RELATIVE_CAMERA_SCALE));
    }

    private int getTargetCameraHeight() {
        return relativeSize ? RELATIVE_CAMERA_SCALE : physicalHeight;
    }

    private float getRelativeLogicalWidth() {
        return physicalWidth / (float) Math.max(physicalHeight, 1);
    }

    private void configureGuiNodeScale() {
        if (defaultGuiNode == null) {
            return;
        }
        float scale = relativeSize ? RELATIVE_CAMERA_SCALE : 1f;
        defaultGuiNode.setLocalScale(scale, scale, 1f);
    }

    private void configureGuiCamera(Camera cam, int logicalWidth, int logicalHeight) {
        cam.setParallelProjection(true);
        cam.setFrustum(-1000f, 1000f, 0f, logicalWidth, logicalHeight, 0f);
    }

    private void updatePhysicalSize(ViewPortManager vpm) {
        ViewPort main = vpm.getMainSceneViewPort();
        if (main != null) {
            physicalWidth = Math.max(main.getRenderTargetWidth(), 1);
            physicalHeight = Math.max(main.getRenderTargetHeight(), 1);
        } else if (defaultGuiViewPort != null) {
            physicalWidth = Math.max(defaultGuiViewPort.getRenderTargetWidth(), 1);
            physicalHeight = Math.max(defaultGuiViewPort.getRenderTargetHeight(), 1);
        }
    }


    public NWindowManager closeManager(NWindowManager manager) {
        manager.closeAll();
        NGEGui.unregister(manager.getViewPort());
        manager.setInputHandler(null);
        windowManagers.remove(manager);
        return manager;
    }
    

    public void setDefaultInputHandler(Class<? extends NavigatorInputHandler> inputHandler) {
   
        this.defaultInputHandlerClass = inputHandler;
    }

  
    @Override
    public Component newInstance() {
        return new NWindowManagerComponent(relativeSize);
    }

    /**
     * @deprecated use {@link #setInteractionEnabled(boolean)} to enable UI input and cursor together.
     */
    @Deprecated
    public void showCursor(boolean v) {
        setInteractionEnabled(v);
    }

    public void setInteractionEnabled(boolean enabled) {
        boolean wasActive = interactionActive;
        interactionEnabled = enabled;
        if (!enabled) {
            interactionActive = false;
        }
        applyInteractionState();
        if (!wasActive && interactionActive) {
            releaseActiveInputMappings();
        }
    }

    public boolean isInteractionEnabled() {
        return interactionEnabled;
    }

    public boolean isInteractionActive() {
        return interactionActive;
    }

    public boolean hasOpenWindows() {
        for (NWindowManager manager : windowManagers) {
            if (manager.hasOpenWindows()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInteractiveWindows() {
        for (NWindowManager manager : windowManagers) {
            if (manager.hasInteractiveWindows()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPointerInteractiveWindows() {
        for (NWindowManager manager : windowManagers) {
            if (manager.hasPointerInteractiveWindows()) {
                return true;
            }
        }
        return false;
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

    public float getLogicalWidth() {
        return getManager(null).getLogicalWidth();
    }

    public float getLogicalHeight() {
        return getManager(null).getLogicalHeight();
    }

    public int getPhysicalWidth() {
        return physicalWidth;
    }

    public int getPhysicalHeight() {
        return physicalHeight;
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
        applyInteractionState();
    }

    public void closeAllToasts() {
        getManager(null).closeAllToasts();
    }

    public void closeAll() {
        getManager(null).closeAll();
        applyInteractionState();
    }

    @Override
    public void onEnable(ComponentManager mng,
            boolean firstTime ) {
        enabled = true;
        NWindowManager m = getManager(null);
        NGEStyle.installAndUse(m.getLogicalWidth(), m.getLogicalHeight());

        setPhysicalCursorVisible(false);
    }

    @Override
    public void onDisable(ComponentManager mng) {
        enabled = false;
        interactionEnabled = false;
        interactionActive = false;
        lastInputDevice = null;
        setPhysicalCursorVisible(false);
        for(NWindowManager manager : windowManagers) {
            manager.closeAll();
            manager.setInputHandler(null);
            NGEGui.unregister(manager.getViewPort());
        }
        windowManagers.clear();
        if (defaultGuiViewPort != null) {
            ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
            if (vpm != null) {
                vpm.removeGuiViewPort(defaultGuiViewPort);
            }
            defaultGuiViewPort = null;
            defaultGuiNode = null;
        }
 
    }

    public void setInputDevice(InputDevice device) {
        lastInputDevice = device;
        applyInputDeviceToManagers();
    }

    private void applyInputDeviceToManagers() {
        for (NWindowManager manager : windowManagers) {
            InputDevice device = inputDeviceFor(manager);
            if (device != null) {
                manager.setInputDevice(device);
            } else {
                manager.setInputDevice(null);
            }
        }
    }

    private InputDevice inputDeviceFor(NWindowManager manager) {
        if (interactionActive && canInteractWith(manager)) {
            return lastInputDevice;
        }
        if (canUseDirectTouch(manager)) {
            return lastInputDevice instanceof TouchScreen ? lastInputDevice : new TouchScreen();
        }
        return null;
    }

    
    @Override
    public void updateAppLogic(ComponentManager mng, float tpf){
        configureDefaultGuiViewPort();
        if (enabled) {
            boolean wasActive = interactionActive;
            applyInteractionState();
            if (!wasActive && interactionActive) {
                releaseActiveInputMappings();
            }
        }
        for(NWindowManager manager : windowManagers){
            manager.update(tpf);
        }
        if (enabled) {
            applyInteractionState();
        }
    }

    public void back() {
        getManager(null).back();
        applyInteractionState();
    }

    public void action(int id) {
        getManager(null).action(id);
    }

    public void toastAction(int id) {
        getManager(null).toastAction(id);
    }

 
    @Override
    public void onInputDeviceConnected(ComponentManager mng, InputManager inputManager, InputActions actions, InputDevice device) {
        if (device instanceof TouchScreen && hasPointerInteractiveWindows()) {
            setInputDevice(device);
        }
        if (!interactionActive) setPhysicalCursorVisible(false);
    }

    @Override
    public void onInputDeviceDisconnected(ComponentManager mng, InputManager inputManager, InputActions actions, InputDevice device) {
        if (lastInputDevice == device) {
            lastInputDevice = null;
            applyInputDeviceToManagers();
        }
        if (!interactionActive) setPhysicalCursorVisible(false);
    }

    @Override
    public void onInputAction(ComponentManager mng, String action, boolean toggled,  float value, InputEvent<?> event,
            float tpf) {
        if (!interactionActive) setPhysicalCursorVisible(false);
    }

    @Override
    public boolean controlsOnScreenJoystick(ComponentManager mng, Joystick[] joysticks) {
        return false;
    }

    // any input action will set the input device as 
    // current for the main window manager
    public void onJoyAxisEvent(ComponentManager mng, JoyAxisEvent evt) {
        if (!interactionActive) setPhysicalCursorVisible(false);
        setInputDevice(evt.getDevice());
    }

    public void onJoyButtonEvent(ComponentManager mng, JoyButtonEvent evt) {
        if (!interactionActive) setPhysicalCursorVisible(false);
        setInputDevice(evt.getDevice());
    }

    public void onMouseMotionEvent(ComponentManager mng, MouseMotionEvent evt) {
        if (!interactionActive) setPhysicalCursorVisible(false);
        setInputDevice(evt.getDevice());
    }

    public void onMouseButtonEvent(ComponentManager mng, MouseButtonEvent evt) {
        if (!interactionActive) setPhysicalCursorVisible(false);
        setInputDevice(evt.getDevice());
    }

    public void onKeyEvent(ComponentManager mng, KeyInputEvent evt) {
        if (!interactionActive) setPhysicalCursorVisible(false);
        setInputDevice(evt.getDevice());
        
    }

    public void onTouchEvent(ComponentManager mng, TouchEvent evt) {
        if (!interactionActive) setPhysicalCursorVisible(false);
        setInputDevice(evt.getDevice());
    }

    protected void setPhysicalCursorVisible(boolean visible) {
        if (physicalCursorVisible != visible) {
            physicalCursorVisibleDirty = true;
            physicalCursorVisible = visible;
        }
        applyPhysicalCursorVisible();
    }

    private void applyPhysicalCursorVisible() {
        if (getComponentManager() == null) {
            return;
        }
        InputManager inputManager = getInstanceOf(InputManager.class);
        if (inputManager != null
                && (physicalCursorVisibleDirty
                    || appliedPhysicalCursorVisible != physicalCursorVisible
                    || inputManager.isCursorVisible() != physicalCursorVisible)) {
            inputManager.setCursorVisible(physicalCursorVisible);
            appliedPhysicalCursorVisible = physicalCursorVisible;
            physicalCursorVisibleDirty = false;
        }
    }

    private boolean canInteractWith(NWindowManager manager) {
        return interactionEnabled && manager != null && manager.hasInteractiveWindows();
    }

    private boolean canUseDirectTouch(NWindowManager manager) {
        if (manager == null || !manager.hasPointerInteractiveWindows()) {
            return false;
        }
        return lastInputDevice instanceof TouchScreen
                || InputHandlerFragment.isMobilePlatform()
                || InputHandlerFragment.isMobileWebView();
    }

    void onWindowStackChanged() {
        boolean wasActive = interactionActive;
        applyInteractionState();
        if (!wasActive && interactionActive) {
            releaseActiveInputMappings();
        }
    }

    private void applyInteractionState() {
        interactionActive = false;
        boolean hardwareCursorVisible = false;
        boolean directTouchUi = InputHandlerFragment.isMobilePlatform() || InputHandlerFragment.isMobileWebView();
        for (NWindowManager manager : windowManagers) {
            boolean managerCanInteract = canInteractWith(manager);
            interactionActive |= managerCanInteract;
            manager.getContext().getNavigator().setSimulateCursor(!directTouchUi);
            boolean cursorVisible = managerCanInteract && !directTouchUi;
            if (manager.getContext().getNavigator().isCursorVisible() != cursorVisible) {
                manager.getContext().getNavigator().setCursor(cursorVisible);
            }
            hardwareCursorVisible |= !directTouchUi
                    && managerCanInteract
                    && manager.getContext().getNavigator().isHardwareCursor()
                    && manager.getContext().getNavigator().isCursorActive();
        }
        setPhysicalCursorVisible(hardwareCursorVisible);
        applyInputDeviceToManagers();
    }

    protected void releaseActiveInputMappings() {
        InputManager inputManager = getInstanceOf(InputManager.class);
        if (inputManager != null) {
            inputManager.releaseActiveMappings();
        }
    }

    @Override
    public void reload() {
        for(NWindowManager manager : windowManagers) {
            NWindow<?>[] windows = manager.getWindows().toArray(new NWindow[0]);
            for(NWindow<?> window : windows) {
                window.reloadNow();
            }
        }
    }
}
