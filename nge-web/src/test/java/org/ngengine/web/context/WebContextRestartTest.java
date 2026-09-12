/*
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.jme3.system.AppSettings;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class WebContextRestartTest {
    @Test
    void quittingAndRestartingShareOnePageReload() throws Exception {
        RecordingContext context = new RecordingContext();
        context.destroy(false);
        context.destroy(true);
        context.restart();
        assertEquals(1, context.reloads);
        Method loop = WebContext.class.getDeclaredMethod("loop");
        loop.setAccessible(true);
        assertFalse((Boolean) loop.invoke(context));
    }

    @Test
    void restartRequestsExactlyOnePageReload() {
        RecordingContext context = new RecordingContext();
        context.restart();
        context.restart();
        assertEquals(1, context.reloads);
    }

    @Test
    void restartedContextDoesNotRenderAnotherFrame() throws Exception {
        RecordingContext context = new RecordingContext();
        context.restart();
        Method loop = WebContext.class.getDeclaredMethod("loop");
        loop.setAccessible(true);
        assertFalse((Boolean) loop.invoke(context));
    }

    @Test
    void copyingSettingsCannotCreateMultisampledBlitTextures() throws Exception {
        RecordingContext context = new RecordingContext();
        AppSettings settings = new AppSettings(true);
        settings.setSamples(4);
        context.setSettings(settings);
        Method rebuild = WebContext.class.getDeclaredMethod(
                "rebuildAuxiliaryFrameBufferIfNeeded", int.class, int.class);
        rebuild.setAccessible(true);
        rebuild.invoke(context, 640, 360);
        assertEquals(1, context.auxiliaryFrameBuffer.getSamples());
        assertEquals(1, context.auxiliaryFrameBuffer.getColorTarget(0).getTexture().getImage().getMultiSamples());
        settings.setSamples(2);
        context.setSettings(settings);
        rebuild.invoke(context, 1280, 720);
        assertEquals(1, context.auxiliaryFrameBuffer.getSamples());
        assertEquals(1, context.auxiliaryFrameBuffer.getColorTarget(0).getTexture().getImage().getMultiSamples());
        assertEquals(2, settings.getSamples());
    }

    private static class RecordingContext extends WebContext {
        int reloads;

        @Override
        protected void requestPageReload() {
            reloads++;
        }
    }
}
