/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.gui.win;

import com.jme3.math.Vector3f;
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
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.GuiViewPortFragment;
import org.ngengine.gui.win.NToast.ToastType;
import org.ngengine.gui.win.std.NErrorWindow;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

public class NWindowManagerComponent implements Component<Object>, GuiViewPortFragment {

    private static final Logger log = Logger.getLogger(NWindowManagerComponent.class.getName());
    private final ArrayList<NWindow<?>> windowsStack = new ArrayList<>();
    private final ArrayList<NToast> toastsStack = new ArrayList<>();

    private Node guiNode;
    private int width;
    private int height;
    private Container toastContainer;
    private Runner dispatcher;
    private DataStoreProvider dataStoreProvider;

    @Override
    public void onAttached(ComponentManager mng, Runner runner, DataStoreProvider dataStoreProvider) {
        this.dispatcher = runner;
        this.dataStoreProvider = dataStoreProvider;
    }

    public DataStoreProvider getDataStoreProvider() {
        return dataStoreProvider;
    }

    @Override
    public void receiveGuiViewPort(ViewPort vp) {
        this.guiNode = getGuiNode(vp);
        this.width = vp.getCamera().getWidth();
        this.height = vp.getCamera().getHeight();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public <T> void runInThread(Callable<T> r, BiConsumer<T, Throwable> callback) {
        dispatcher.run(() -> {
            try {
                T out = r.call();
                if (callback != null) callback.accept(out, null);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to run in thread", e);
                if (callback != null) callback.accept(null, e);
            }
        });
    }

    public void runInThread(Runnable r) {
        dispatcher.run(r);
    }

    public <T extends NWindow<?>> void showWindow(Class<T> windowClass) {
        showWindow(windowClass, null, null);
    }

    public <T extends NWindow<?>> void showWindow(Class<T> windowClass, BiConsumer<T, Throwable> callback) {
        showWindow(windowClass, null, callback);
    }

    public <T extends NWindow<?>> void showWindow(Class<T> windowClass, Object args) {
        showWindow(windowClass, args, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends NWindow> void showWindow(Class<T> windowClass, Object args, BiConsumer<T, Throwable> callback) {
        try {
            runInThread(
                () -> {
                    for (NWindow window : windowsStack) {
                        window.removeFromParent();
                    }

                    log.finer("Opening window: " + windowClass.getSimpleName());

                    Consumer<NWindow<?>> backAction = null;

                    if (windowsStack.size() > 0) {
                        backAction =
                            win -> {
                                closeWindow(windowsStack.get(windowsStack.size() - 1));
                            };
                    }

                    T window = windowClass.getDeclaredConstructor().newInstance();
                    window.initialize(this, backAction);
                    if (args != null) window.setArgs(args);

                    showWindow(window);
                    windowsStack.add(window);
                    return window;
                },
                callback
            );
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to open window: " + windowClass.getSimpleName(), e);
            throw new RuntimeException("Failed to create window", e);
        }
    }

    public void showFatalError(Throwable exc) {
        showFatalError(exc, null);
    }

    public void showFatalError(Throwable exc, BiConsumer<NErrorWindow, Throwable> callback) {
        log.log(Level.SEVERE, "Fatal error", exc);
        showWindow(NErrorWindow.class, exc, callback);
    }

    public void showToast(Throwable exc) {
        showToast(exc, null, null);
    }

    public void showToast(Throwable exc, BiConsumer<NToast, Throwable> callback) {
        showToast(exc, null, callback);
    }

    public void showToast(Throwable exc, Duration duration) {
        showToast(exc, duration, null);
    }

    public void showToast(Throwable exc, Duration duration, BiConsumer<NToast, Throwable> callback) {
        log.log(Level.WARNING, "Exception toast", exc);
        StringBuilder message = new StringBuilder();
        message.append("Error: ");
        message.append(exc.getClass().getSimpleName());
        message.append("\n\t");
        message.append(exc.getMessage());
        showToast(ToastType.ERROR, message.toString(), duration, callback);
    }

    public void showToast(ToastType type, String message) {
        showToast(type, message, null, null);
    }

    public void showToast(ToastType type, String message, BiConsumer<NToast, Throwable> callback) {
        showToast(type, message, null, callback);
    }

    public void showToast(ToastType type, String message, Duration duration) {
        showToast(type, message, duration, null);
    }

    public void showToast(ToastType type, String message, Duration duration, BiConsumer<NToast, Throwable> callback) {
        try {
            runInThread(
                () -> {
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
                },
                callback
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create toast", e);
        }
    }

    void closeToast(NToast toast) {
        runInThread(() -> {
            if (toast.getParent() != null) {
                toast.removeFromParent();
            }
            toastsStack.remove(toast);
        });
    }

    private void showWindow(NWindow<?> window) {
        runInThread(() -> {
            if (window.getParent() != null) {
                window.removeFromParent();
            }
            window.invalidate();
            guiNode.attachChild(window);
            window.onShow();
        });
    }

    public void closeAllWindows() {
        runInThread(() -> {
            NWindow<?>[] windows = windowsStack.toArray(new NWindow[0]);
            for (NWindow<?> window : windows) {
                closeWindow(window);
            }
        });
    }

    public void closeAllToasts() {
        runInThread(() -> {
            NToast[] toasts = toastsStack.toArray(new NToast[0]);
            for (NToast toast : toasts) {
                closeToast(toast);
            }
        });
    }

    public void closeAll() {
        // runInThread(()->{
        closeAllWindows();
        closeAllToasts();
        // });
    }

    void closeWindow(NWindow<?> window) {
        runInThread(() -> {
            if (window.getParent() != null) {
                window.removeFromParent();
            }
            window.onHide();

            windowsStack.remove(window);
            if (windowsStack.size() > 0) {
                NWindow<?> lastWindow = windowsStack.get(windowsStack.size() - 1);
                showWindow(lastWindow);
            }
        });
    }

    @Override
    public void onEnable(
        ComponentManager mng,
        Runner runner,
        DataStoreProvider dataStoreProvider,
        boolean firstTime,
        Object slot
    ) {
        {
            Container toastParent = new Container(new BorderLayout());
            toastParent.setLocalTranslation(0, height, 10);
            toastParent.setPreferredSize(new Vector3f(width, height, 10));
            toastContainer = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
            toastContainer.setInsetsComponent(new DynamicInsetsComponent(0f, 1f, 0f, 0f));
            toastParent.addChild(toastContainer, BorderLayout.Position.South);
            guiNode.attachChild(toastParent);
        }
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStoreProvider) {
        closeAll();
        {
            toastContainer.getParent().removeFromParent();
        }
    }

    @Override
    public void updateGuiViewPort(ViewPort vp, float tpf) {
        if (toastsStack.size() > 0) {
            Instant now = Instant.now();
            Iterator<NToast> it = toastsStack.iterator();
            while (it.hasNext()) {
                NToast toast = it.next();
                boolean expired = toast.getCreationTime().plus(toast.getDuration()).isBefore(now);
                if (expired) {
                    it.remove();
                    toast.close();
                }
            }
        }
    }
}
