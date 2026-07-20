package org.ngengine.components.jme3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.jme3.app.Application;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.NullRenderer;

class Jme3ViewPortManagerTest {

    @Test
    void filterPostProcessorIsCreatedOnRequestedViewport() {
        AppSettings settings = new AppSettings(false);
        RenderManager renderManager = new RenderManager(new NullRenderer());
        ViewPort main = renderManager.createMainView("main", new Camera(1280, 720));
        ViewPort requested = renderManager.createMainView("requested", new Camera(640, 360));
        JmeContext context = proxy(JmeContext.class, (methodName) ->
                "getSettings".equals(methodName) ? settings : null);
        Application application = proxy(Application.class, (methodName) -> switch (methodName) {
            case "getRenderManager" -> renderManager;
            case "getViewPort" -> main;
            case "getContext" -> context;
            default -> null;
        });

        FilterPostProcessor processor = new Jme3ViewPortManager(application)
                .getFilterPostProcessor(requested);

        assertTrue(requested.getProcessors().contains(processor));
        assertFalse(main.getProcessors().contains(processor));
        assertSame(processor, new Jme3ViewPortManager(application).getFilterPostProcessor(requested));
    }

    private static <T> T proxy(Class<T> type, ValueProvider values) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (instance, method, args) -> {
                    Object value = values.value(method.getName());
                    if (value != null || !method.getReturnType().isPrimitive()) {
                        return value;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == char.class) {
                        return '\0';
                    }
                    return 0;
                }));
    }

    @FunctionalInterface
    private interface ValueProvider {
        Object value(String methodName);
    }
}
