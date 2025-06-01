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
