package com.jme3.system;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SystemListenerAggregator implements SystemListener{
    private final List<SystemListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(SystemListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(SystemListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public <T> T getListener(Class<T> type) {
        for (SystemListener listener : listeners) {
            if (type.isInstance(listener)) {
                return type.cast(listener);
            }
        }
        return null;
    }

    @Override
    public void initialize() {
        for (SystemListener listener : listeners) {
            listener.initialize();
        }
    }

    @Override
    public void reshape(int width, int height) {
        for (SystemListener listener : listeners) {
            listener.reshape(width, height);
        }
    }

    @Override
    public void reshape(int logicalWidth, int logicalHeight, int framebufferWidth, int framebufferHeight) {
        for (SystemListener listener : listeners) {
            listener.reshape(logicalWidth, logicalHeight, framebufferWidth, framebufferHeight);
        }
    }

    @Override
    public void rescale(float x, float y) {
        for (SystemListener listener : listeners) {
            listener.rescale(x, y);
        }
    }

    @Override
    public void update() {
        for (SystemListener listener : listeners) {
            listener.update();
        }
    }

    @Override
    public void requestClose(boolean esc) {
        for (SystemListener listener : listeners) {
            listener.requestClose(esc);
        }
    }

    @Override
    public void gainFocus() {
        for (SystemListener listener : listeners) {
            listener.gainFocus();
        }
    }

    @Override
    public void loseFocus() {
        for (SystemListener listener : listeners) {
            listener.loseFocus();
        }
    }

    @Override
    public void handleError(String errorMsg, Throwable t) {
        for (SystemListener listener : listeners) {
            listener.handleError(errorMsg, t);
        }
    }

    @Override
    public void destroy() {

        for (SystemListener listener : listeners) {
            listener.destroy();
        }
        listeners.clear();

    }
    
}
