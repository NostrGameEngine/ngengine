package org.ngengine.gui.win;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.DevMode;
import org.ngengine.gui.win.Toast.ToastType;
import org.ngengine.gui.win.std.ErrorWindow;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.noise.Color;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.GuiControl;

public class WindowManagerAppState extends BaseAppState {
    private static final Logger log = Logger.getLogger(WindowManagerAppState.class.getName());
    private final Node guiNode;
    private final int width;
    private final int height;
    private final ArrayList<Window<?>> windowsStack = new ArrayList<>();

    private final ArrayList<Toast> toastsStack = new ArrayList<>();
    private Thread renderThread;
    private Container toastContainer;

    public WindowManagerAppState(Node guiNode, int width, int height) {
        this.guiNode = guiNode;
        this.width = width;
        this.height = height;
        DevMode.registerReloadCallback(this, () -> {
            setEnabled(false);
            setEnabled(true);
            for (Window<?> window : windowsStack) {
                window.invalidate();
            }
        });
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public <T> AsyncTask<T> runInThread(Callable<T> r) {
        NGEPlatform platform = NGEPlatform.get();
        return platform.wrapPromise((Consumer<  T> res, Consumer<Throwable> rej) -> {
            if (getApplication() != null
                    && (renderThread == null || Thread.currentThread() != renderThread)) {
                getApplication().enqueue(() -> {
                    try {
                        T out = r.call();
                        res.accept(out);
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Failed to run in thread", e);
                        rej.accept(e);
                    }
                });
            } else {
                try {
                    T out = r.call();
                    res.accept(out);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Failed to run in thread", e);
                    rej.accept(e);
                }
            }
        });
    }
    public AsyncTask<Void> runInThread(Runnable r) {
        Callable<Void> callable = () -> {
            r.run();
            return null;
        };
        return runInThread(callable);
    }

    public <T extends Window<?>> AsyncTask<T> showWindow(Class<T> windowClass) {
        return showWindow(windowClass, null,null);
    }

    public <T extends Window<?>> AsyncTask<T> showWindow(Class<T> windowClass, WindowListener listener) {
        return showWindow(windowClass, null, listener);
    }

    public <T extends Window<?>> AsyncTask<T> showWindow(Class<T> windowClass, Object args ) {
        return showWindow(windowClass, args, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Window> AsyncTask<T> showWindow(Class<T> windowClass, Object args, WindowListener listener) {
        try {
            return runInThread(()->{

                for (Window window : windowsStack) {
                    window.removeFromParent();
                }
                int windowWidth = (int) ((float) width * 0.6f);
                int windowHeight = (int) ((float) height * 0.6f);
                if (windowWidth < 800) {
                    windowWidth = 800;
                }
                if (windowHeight < 600) {
                    windowHeight = 600;
                }
                if (windowWidth > width) {
                    windowWidth = width;
                }
                if (windowHeight > height) {
                    windowHeight = height;
                }

                Consumer<Window<?>> backAction = null;

                if (windowsStack.size() > 0) {
                    backAction = (win) -> {
                        closeWindow(windowsStack.get(windowsStack.size() - 1));
                    };
                }

                T window = windowClass.getDeclaredConstructor().newInstance();
                window.addWindowListener(listener);
                window.initialize(this, backAction);
                if (args != null) window.setArgs(args);

                showWindow(window);
                windowsStack.add(window);
                return window;
            });
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to open window: " + windowClass.getSimpleName(), e);
            throw new RuntimeException("Failed to create window", e);
        }
    }

    public AsyncTask<ErrorWindow> showFatalError(Throwable exc) {
        log.log(Level.SEVERE, "Fatal error", exc);
        return showWindow(ErrorWindow.class, exc, null);
    }

    public AsyncTask<Toast> showToast(Throwable exc) {
        return showToast(  exc, null);
    }

    public AsyncTask<Toast> showToast(Throwable exc, Duration duration) {
        log.log(Level.WARNING, "Exception toast", exc);
        StringBuilder message = new StringBuilder();
        message.append("Error: ");
        message.append(exc.getClass().getSimpleName());
        message.append("\n\t");
        message.append(exc.getMessage());
        return showToast(ToastType.ERROR, message.toString(), duration);
    }

    public AsyncTask<Toast> showToast(ToastType type, String message) {    
        return showToast(type, message, null);
    }

    public AsyncTask<Toast> showToast(ToastType type, String message, Duration duration) {
        try{
            return runInThread(()->{
                Duration finalDuration = duration;
                if(finalDuration==null){
                    if (type != ToastType.INFO) {
                        finalDuration = Duration.ofSeconds(10);
                    } else{
                        finalDuration = Duration.ofSeconds(5);
                    }
                }
                Toast toast = new Toast(type, message, finalDuration);
                toastContainer.addChild(toast);
                toastsStack.add(toast);
                return toast;
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to create toast", e);
        }
    }

    void closeToast(Toast toast) {
        runInThread(()->{
            if (toast.getParent() != null) {
                toast.removeFromParent();
            }
            toastsStack.remove(toast);
        });
    }

    private void showWindow(Window<?> window) {
        runInThread(()->{
            if (window.getParent() != null) {
                window.removeFromParent();
            }
            window.invalidate();
            guiNode.attachChild(window);
            window.onShow();
        });
    }

    public void closeAllWindows() {
        runInThread(()->{
            Window<?>[] windows = windowsStack.toArray(new Window[0]);
            for (Window<?> window : windows) {
                closeWindow(window);
            }              
        });
    }

    public void closeAllToasts() {
        runInThread(()->{
            Toast[] toasts = toastsStack.toArray(new Toast[0]);
            for (Toast toast : toasts) {
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

    void closeWindow(Window<?> window) {
        runInThread(()->{

            if (window.getParent() != null) {
                window.removeFromParent();
            }
            window.onHide();

            windowsStack.remove(window);
            if (windowsStack.size() > 0) {
                Window<?> lastWindow = windowsStack.get(windowsStack.size() - 1);
                showWindow(lastWindow);
            }
        });
    }

    @Override
    protected void initialize(Application app) {
        if (renderThread == null) {
            renderThread = Thread.currentThread();
        }
    }

    @Override
    protected void cleanup(Application app) {

    }


    @Override
    protected void onEnable() {
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
    protected void onDisable() {
        closeAll();
        {
            toastContainer.getParent().removeFromParent();
        }
        

    }

    @Override
    public void update(float tpf) {
        if(renderThread==null){
            renderThread = Thread.currentThread();
        }
        if (toastsStack.size() > 0) {
            Instant now = Instant.now();
            Iterator<Toast> it = toastsStack.iterator();
            while (it.hasNext()) {
                Toast toast = it.next();
                boolean expired = toast.getCreationTime().plus(toast.getDuration()).isBefore(now);
                if (expired) {
                    it.remove();
                    toast.close();
                }
            }
        }
    }

}
