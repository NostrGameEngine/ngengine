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
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.store.DataStoreProvider;

public class NWindowManagerComponent extends AbstractComponent implements  LogicFragment, InputHandlerFragment {

    private static final Logger log = Logger.getLogger(NWindowManagerComponent.class.getName());
    private final ArrayList<NWindow<?>> windowsStack = new ArrayList<>();
    private final ArrayList<NToast> toastsStack = new ArrayList<>();

    
    private Container toastContainer;
  
    private int width = 0;
    private int height = 0;

    @Override
    public Component newInstance() {
        return new NWindowManagerComponent();
    }

    public void showCursor(boolean v) {
        getInstanceOf(InputManager.class).setCursorVisible(v);
    }

    private Node getGuiNode(){
        ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
        return vpm.getRootNode(vpm.getGuiViewPort());
    }

    public ViewPort getViewPort(){
        ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
        return vpm.getGuiViewPort();
    }

    public void enqueueInThread(Runnable task) {
        MainThreadRunner r = getInstanceOf(MainThreadRunner.class);
        r.enqueue(task);
    }

    public void runInThread(Runnable task) {
        MainThreadRunner r = getInstanceOf(MainThreadRunner.class);
        r.run(task);
    }

  

    public DataStoreProvider getDataStoreProvider() {
        return getInstanceOf(DataStoreProvider.class);
    }

    

    public int getWidth() {
        ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
        ViewPort guiVp= vpm.getGuiViewPort();
        Camera cam = guiVp.getCamera();
        return cam.getWidth();
    }

    public int getHeight() {
        ViewPortManager vpm = getInstanceOf(ViewPortManager.class);
        ViewPort guiVp= vpm.getGuiViewPort();
        Camera cam = guiVp.getCamera();
        return cam.getHeight();
    }

    protected void checkThread() {
        MainThreadRunner r = getInstanceOf(MainThreadRunner.class);
        r.checkThread();
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
        checkThread();
        return showWindow(windowClass, null);
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
        checkThread();
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Runnable> closer = new AtomicReference<>(() -> {
            closed.set(true);
        });

        try {
            for (NWindow<?> window : windowsStack) {
                window.removeFromParent();
            }

            log.finer("Opening window: " + windowClass.getSimpleName());

            Consumer<NWindow<A>> backAction = null;

            if (windowsStack.size() > 0) {
                backAction = win -> {
                    closeWindow(windowsStack.get(windowsStack.size() - 1));
                };
            }

            T window = (T) windowClass.getDeclaredConstructor().newInstance();
            window.addWindowListener(new NWindowListener() {
                @Override
                public void onShow(NWindow<?> window) {
                    closer.set(() -> {
                        window.close();
                    });
                    if (closed.get()) {
                        window.close();
                    }
                }

                @Override
                public void onHide(NWindow<?> window) {
                }
            });
            window.initialize(this, backAction);
            if (args != null) window.setArgs(args);

            showWindow(window);
            windowsStack.add(window);
            return window;

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to open window: " + windowClass.getSimpleName(), e);
            throw new RuntimeException("Failed to create window", e);
        }

    }

    public <T extends NWindow<?>> T getWindow(Predicate<NWindow<?>> filter){
        for (NWindow<?> window : windowsStack) {
            if(filter.test(window))return (T) window;
        }
        return null;
    }

    public <T extends NWindow<?>> T getWindow(Class<T> windowClass){
        return getWindow(w->{
            return windowClass.isInstance(w);
        });
    }

    public NErrorWindow showFatalError(Throwable exc) {
        checkThread();
        log.log(Level.SEVERE, "Fatal error", exc);
        return (NErrorWindow) showWindow(NErrorWindow.class, exc);
    }

    public NToast showToast(Throwable exc) {
        checkThread();
        return showToast(exc, null);
    }

    public NToast showToast(Throwable exc, Duration duration) {
        checkThread();
        exc.printStackTrace();
        StringBuilder message = new StringBuilder();
        message.append("Error: ");
        message.append(exc.getClass().getSimpleName());
        message.append("\n\t");
        message.append(exc.getMessage());
        log.log(Level.WARNING, "Exception toast " + message.toString(), exc);
        return showToast(ToastType.ERROR, message.toString(), duration);
    }

    public NToast showToast(ToastType type, String message) {
        return showToast(type, message, null);
    }

    public NToast showToast(ToastType type, String message, Duration duration) {
        checkThread();
        Duration finalDuration = duration;
        if (finalDuration == null) {
            if (type != ToastType.INFO) {
                finalDuration = Duration.ofSeconds(10);
            } else {
                finalDuration = Duration.ofSeconds(5);
            }
        }
        NToast toast = new NToast(type, message, finalDuration);
        toastContainer.addChild(toast);
        toastsStack.add(toast);
        return toast;       
    }

    void closeToast(NToast toast) {
        checkThread();
        if (toast.getParent() != null) {
            toast.removeFromParent();
        }
        toastsStack.remove(toast);
    }

    private void showWindow(NWindow<?> window) {
        checkThread();
        if (window.getParent() != null) {
            window.removeFromParent();
        }
        window.invalidate();
        getGuiNode().attachChild(window);
        window.onShow();
    }

    public void closeAllWindows() {
        checkThread();
        NWindow<?>[] windows = windowsStack.toArray(new NWindow[0]);
        for (NWindow<?> window : windows) {
            closeWindow(window);
        }
    }

    public void closeAllToasts() {
        checkThread();
        NToast[] toasts = toastsStack.toArray(new NToast[0]);
        for (NToast toast : toasts) {
            closeToast(toast);
        }
    }

    public void closeAll() {
        checkThread();
        // runInThread(()->{
        closeAllWindows();
        closeAllToasts();
        // });
    }

    void closeWindow(NWindow<?> window) {
        checkThread();
        if (window.getParent() != null) {
            window.removeFromParent();
        }
        window.onHide();

        windowsStack.remove(window);
        if (windowsStack.size() > 0) {
            NWindow<?> lastWindow = windowsStack.get(windowsStack.size() - 1);
            showWindow(lastWindow);
        }
    }

    @Override
    public void onEnable(ComponentManager mng,
            boolean firstTime ) {

         int width = getWidth();
        int height = getHeight();
        NGEStyle.installAndUse(width,height);

        Node guiNode = getGuiNode();
        {
            Container toastParent = new Container(new BorderLayout());
            toastParent.setLocalTranslation(0, height, 10);
            toastParent.setPreferredSize(new Vector3f(width, height, 10));
            toastContainer = new Container(
                    new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
            toastContainer.setInsetsComponent(new DynamicInsetsComponent(0f, 1f, 0f, 0f));
            toastParent.addChild(toastContainer, BorderLayout.Position.South);
            guiNode.attachChild(toastParent);
        }
    }

    @Override
    public void onDisable(ComponentManager mng) {
        closeAll();
        {
            toastContainer.getParent().removeFromParent();
        }
    }
    

    
    @Override
    public void updateAppLogic(ComponentManager mng, float tpf){
        ViewPortManager vpm = mng.getInstanceOf(ViewPortManager.class);
        ViewPort vp= vpm.getGuiViewPort();
        
        if (toastsStack.size() > 0) {
            Instant now = Instant.now();
            Iterator<NToast> it = toastsStack.iterator();
            while (it.hasNext()) {
                NToast toast = it.next();
                boolean expired = toast.getCreationTime().plus(toast.getDuration()).isBefore(now);
                boolean closed = toast.isClosed();
                if (expired) {
                    toast.close();
                }
                if (closed) {
                    it.remove();
                }
            }
        }

        Camera cam = vp.getCamera();
        if (width != cam.getWidth() || height != cam.getHeight()) {
            width = cam.getWidth();
            height = cam.getHeight();
            NGEStyle.installAndUse(width,height);
            for (NWindow<?> window : windowsStack) {
                window.invalidate();
            }
            ((Container) toastContainer.getParent()).setPreferredSize(new Vector3f(width, height, 10));
        }
    }

    public void back() {
        checkThread();
        if (windowsStack.size() > 0) {
            closeWindow(windowsStack.get(windowsStack.size() - 1));
        }
    }

    public void action(int id) {
        checkThread();
        if (windowsStack.size() > 0) {
            NWindow<?> window = windowsStack.get(windowsStack.size() - 1);
            window.onAction(id);
        }
    }

    public void toastAction(int id) {
        checkThread();
        if (toastsStack.size() > 0) {
            NToast toast = toastsStack.get(toastsStack.size() - 1);
            toast.onAction(id);
        }
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
}
