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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.gui.Axis;
import org.ngengine.gui.Container;
import org.ngengine.gui.FillMode;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.Insets3f;
import org.ngengine.gui.Label;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.component.QuadBackgroundComponent;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.Keyboard;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial.CullHint;
import org.ngengine.gui.component.BorderLayout;
import org.ngengine.gui.component.SpringGridLayout;
import org.ngengine.gui.guix.win.NToast.ToastType;
import org.ngengine.gui.ime.ImeComposer;
import org.ngengine.gui.ime.JmeSoftKeyboardImeComposer;
import org.ngengine.gui.ime.PhysicalKeyboardImeComposer;
import org.ngengine.gui.nav.NavigatorInputHandler;

public class NWindowManager {
    private static final Logger log = Logger.getLogger(NWindowManager.class.getName());
    private static final float TOAST_LAYER_Z = 2f;

    private final GuiContext ctx;
    private final NWindowManagerComponent mng;
    private final ArrayList<NWindow<?>> windows = new ArrayList<>();
    private final ArrayList<NToast> toastsStack = new ArrayList<>();
    private Container containerToast;
    private float oldWidth, oldHeight;
    private NavigatorInputHandler inputHandler;
    private InputDevice inputDevice;
    private Label controllerBackHint;


    NWindowManager(
        NWindowManagerComponent mng,
        GuiContext ctx
    ) {
        this.mng = mng;
        this.ctx = ctx;
        
        oldWidth = getWidth();
        oldHeight = getHeight();
    }

    public void setInputHandler(NavigatorInputHandler inputHandler){
        InputManager inputManager = mng.getInstanceOf(InputManager.class);
        ensureKeyboardImeComposer(inputManager);
        if(this.inputHandler!=null){
            this.inputHandler.unregisterListener(inputManager);
            this.inputHandler = null;
        }
        if(inputHandler!=null){
            this.inputHandler = inputHandler;
            if(this.inputHandler.getViewPort()==null){
                this.inputHandler.setViewPort(ctx.getViewPort());
            }
        }
        if( this.inputHandler!=null){
            this.inputHandler.registerListener(inputManager);
            if (this.inputDevice != null) {
                this.inputHandler.setInputDevice(inputManager, this.inputDevice);
            }
        }
    }

    public void setInputDevice(InputDevice device){
        if(inputDevice == device)return;
        this.inputDevice = device;
        ctx.setInputDevice(device);
        InputManager inputManager = mng.getInstanceOf(InputManager.class);
        ensureKeyboardImeComposer(inputManager);
         
        if(inputHandler!=null){
            inputHandler.setInputDevice(inputManager, device);
        } 
    }

    private void ensureKeyboardImeComposer(InputManager inputManager) {
        if (inputManager == null) {
            return;
        }
        ImeComposer ime = ctx.getImeComposer();
        if(!(ime instanceof PhysicalKeyboardImeComposer)){
            ctx.setImeComposer(new JmeSoftKeyboardImeComposer(inputManager));
        } else if (ime.getClass() == PhysicalKeyboardImeComposer.class) {
            ctx.setImeComposer(new JmeSoftKeyboardImeComposer(inputManager));
        }
    }

    public boolean hasOpenWindows() {
        return !windows.isEmpty();
    }

    public boolean hasInteractiveWindows() {
        for (NWindow<?> window : windows) {
            if (window.capturesInput()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPointerInteractiveWindows() {
        for (NWindow<?> window : windows) {
            if (window.receivesPointerInput()) {
                return true;
            }
        }
        return false;
    }

    public NavigatorInputHandler getInputHandler(){
        return inputHandler;
    }

    public InputDevice getInputDevice() {
        return inputDevice;
    }
  
    public GuiContext getContext(){
        return ctx;
    }

    public ViewPort getViewPort(){
        return ctx.getViewPort();
    }

    public void update(float tpf){    
        if(ctx.getViewPort() == null){
            closeAll();
            return;
        }    
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

        Camera cam = ctx.getViewPort().getCamera();
        if (oldWidth != getLogicalWidth() || oldHeight != getLogicalHeight()) {
            oldWidth = getLogicalWidth();
            oldHeight = getLogicalHeight();
            for (NWindow<?> window : windows) {
                window.invalidate();
            }

            if (containerToast != null) {
                updateToastStackLayout();
            }
        }

        updateControllerBackHint();
        if (inputHandler != null) {
            inputHandler.update(tpf);
        }
        ctx.update(tpf);
    }

    private void updateControllerBackHint() {
        boolean show = inputDevice instanceof Joystick && hasControllerBackAction();
        if (!show) {
            if (controllerBackHint != null) {
                controllerBackHint.setCullHint(CullHint.Always);
            }
            return;
        }

        if (controllerBackHint == null) {
            controllerBackHint = new Label("B Back");
            controllerBackHint.setFontSize(NGEStyle.vmin(2.2f));
            controllerBackHint.setColor(new ColorRGBA(1f, 1f, 1f, 0.72f));
            controllerBackHint.setShadowColor(new ColorRGBA(0f, 0f, 0f, 0.85f));
            controllerBackHint.setInsets(new Insets3f(
                NGEStyle.px(5),
                NGEStyle.px(9),
                NGEStyle.px(5),
                NGEStyle.px(9)
            ));
            controllerBackHint.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0f, 0.58f)));
        }

        if (controllerBackHint.getParent() != ctx.getGuiNode()) {
            controllerBackHint.removeFromParent();
            ctx.getGuiNode().attachChild(controllerBackHint);
        }
        controllerBackHint.setCullHint(CullHint.Inherit);
        Vector3f size = controllerBackHint.getPreferredSize();
        float margin = NGEStyle.vmin(1.3f);
        controllerBackHint.setLocalTranslation(margin, size.y + margin, 100);
    }

    private boolean hasControllerBackAction() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            NWindow<?> window = windows.get(i);
            if (window.capturesInput()) {
                return window.hasBackAction();
            }
        }
        return false;
    }
 

    public void enqueueInThread(Runnable task) {
        MainThreadRunner r = mng.getInstanceOf(MainThreadRunner.class);
        r.enqueue(task);
    }

    public void runInThread(Runnable task) {
        MainThreadRunner r = mng.getInstanceOf(MainThreadRunner.class);
        r.run(task);
    }

    public DataStoreProvider getDataStoreProvider() {
        return mng.getInstanceOf(DataStoreProvider.class);
    }


    public int getWidth() {
        Camera cam = ctx.getViewPort().getCamera();
        return cam.getWidth();
    }

    public int getHeight() {
        Camera cam = ctx.getViewPort().getCamera();
        return cam.getHeight();
    }

    public float getLogicalWidth() {
        return ctx.getLogicalWidth();
    }

    public float getLogicalHeight() {
        return ctx.getLogicalHeight();
    }

    void invalidateAll() {
        for (NWindow<?> window : windows) {
            window.invalidate();
        }
    }
  
    protected void checkThread() {
        MainThreadRunner r = mng.getInstanceOf(MainThreadRunner.class);
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
            log.finer("Opening window: " + windowClass.getSimpleName());

            T window = (T) windowClass.getDeclaredConstructor().newInstance();
            if (window.capturesInput()) {
                hideInteractiveWindows();
            }
            Consumer<NWindow<A>> backAction = window.capturesInput() ? win -> closeWindow(win, true) : null;
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
            windows.add(window);

            if (window.capturesInput()) {
                ctx.getNavigator().pushLayer(window);
                if (shouldAutofocusForInputDevice()) {
                    ctx.getNavigator().autofocus();
                }
            } else if (window.receivesPointerInput()) {
                ctx.getNavigator().pushPointerLayer(window);
            }
            mng.onWindowStackChanged();
            return window;

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to open window: " + windowClass.getSimpleName(), e);
            throw new RuntimeException("Failed to create window", e);
        }

    }

    private boolean shouldAutofocusForInputDevice() {
        return inputDevice instanceof Keyboard || inputDevice instanceof Joystick;
    }

    private void showWindow(NWindow<?> window) {
        checkThread();
        if (window.getParent() != null) {
            window.removeFromParent();
        }
        window.invalidate();
        ctx.getGuiNode().attachChild(window);
        window.onShow();
    }

    private void hideInteractiveWindows() {
        for (NWindow<?> window : windows) {
            if (window.capturesInput() && window.getParent() != null) {
                window.removeFromParent();
            }
        }
    }

    private NWindow<?> getLastInteractiveWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            NWindow<?> candidate = windows.get(i);
            if (candidate.capturesInput()) {
                return candidate;
            }
        }
        return null;
    }


 

    public NErrorWindow showFatalError(Throwable exc) {
        checkThread();
        log.log(Level.SEVERE, "Fatal error", exc);
        return (NErrorWindow) showWindow(NErrorWindow.class, exc);
    }


    public <T extends NWindow<?>> T getWindow(Class<T> windowClass){
        return getWindow(w->{
            return windowClass.isInstance(w);
        });
    }

    public <T extends NWindow<?>> T getWindow(Predicate<NWindow<?>> filter){
        for (NWindow<?> window : windows) {
            if(filter.test(window))return (T) window;
        }
        return null;
    }

    Collection<NWindow<?>> getWindows() {
        return windows;
    }

    public void closeAllWindows() {
        checkThread();
        NWindow<?>[] windows = this.windows.toArray(new NWindow[0]);
        for (NWindow<?> window : windows) {
            closeWindow(window, false);
        }
    }
    void closeWindow(NWindow<?> window) {
        closeWindow(window, false);
    }

    void closeWindow(NWindow<?> window, boolean showPrevious) {
        checkThread();
        boolean wasInteractive = window.capturesInput();
        if (wasInteractive || window.receivesPointerInput()) {
            ctx.getNavigator().popLayer(window);
        }
        if (window.getParent() != null) {
            window.removeFromParent();
        }
        window.onHide();
        windows.remove(window);
        
        if (showPrevious && wasInteractive) {
            NWindow<?> lastWindow = getLastInteractiveWindow();
            if (lastWindow != null && lastWindow.getParent() == null) {
                showWindow(lastWindow);
                if (shouldAutofocusForInputDevice()) {
                    ctx.getNavigator().autofocus();
                }
            }
        }
        mng.onWindowStackChanged();
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

        if(containerToast == null){
            containerToast = new Container( new SpringGridLayout(Axis.Y, Axis.X, FillMode.ForcedEven, FillMode.Even));
            Container toastParent = new Container(new BorderLayout());
            toastParent.addChild(containerToast, BorderLayout.Position.South);
            ctx.getGuiNode().attachChild(toastParent);
        }
        containerToast.addChild(toast);

        toastsStack.add(toast);
        updateToastStackLayout();
        return toast;       
    }

    private void updateToastStackLayout() {
        if (containerToast == null) {
            return;
        }

        float logicalWidth = getLogicalWidth();
        float toastWidth = getToastStackWidth(logicalWidth);
        float stackHeight = 0f;
        for (NToast toast : toastsStack) {
            stackHeight += getToastHeight(toast);
        }

        float margin = NGEStyle.vmin(1f);
        containerToast.setPreferredSize(new Vector3f(toastWidth, stackHeight, TOAST_LAYER_Z));
        Container toastParent = (Container) containerToast.getParent();
        toastParent.setPreferredSize(new Vector3f(toastWidth, stackHeight, TOAST_LAYER_Z));
        toastParent.setLocalTranslation(getToastStackX(logicalWidth, toastWidth), stackHeight + margin, TOAST_LAYER_Z);
    }

    private float getToastStackWidth(float logicalWidth) {
        return Math.min(logicalWidth, NGEStyle.vmin(55f));
    }

    private float getToastStackX(float logicalWidth, float toastWidth) {
        return Math.max(0f, logicalWidth - toastWidth - NGEStyle.vmin(1f));
    }

    private float getToastHeight(NToast toast) {
        int lines = Math.max(1, toast.getMessage().split("\\R", -1).length);
        return Math.max(NGEStyle.vmin(9.6f), NGEStyle.vmin(5.4f) * lines);
    }

    public void closeAllToasts() {
        checkThread();
        NToast[] toasts = toastsStack.toArray(new NToast[0]);
        for (NToast toast : toasts) {
            closeToast(toast);
        }
    }

    void closeToast(NToast toast) {
        checkThread();
        if (toast.getParent() != null) {
            toast.removeFromParent();
        }
        toastsStack.remove(toast);
        if(toastsStack.size() == 0){
            Container toastParent = containerToast != null ? (Container) containerToast.getParent() : null;
            if (toastParent != null) {
                toastParent.removeFromParent();
            } else if (containerToast != null) {
                containerToast.removeFromParent();
            }
            containerToast = null;
        } else {
            updateToastStackLayout();
        }
    }

    public void closeAll() {
        checkThread();
        closeAllWindows();
        closeAllToasts();
    }
    public void back() {
        checkThread();
        for (int i = windows.size() - 1; i >= 0; i--) {
            NWindow<?> window = windows.get(i);
            if (window.capturesInput()) {
                window.back();
                return;
            }
        }
    }

    public void action(int id) {
        checkThread();
        for (int i = windows.size() - 1; i >= 0; i--) {
            NWindow<?> window = windows.get(i);
            if (window.capturesInput()) {
                window.onAction(id);
                return;
            }
        }
    }

    public void toastAction(int id) {
        checkThread();
        if (toastsStack.size() > 0) {
            NToast toast = toastsStack.get(toastsStack.size() - 1);
            toast.onAction(id);
        }
    }
}
