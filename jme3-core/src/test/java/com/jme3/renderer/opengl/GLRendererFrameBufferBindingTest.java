package com.jme3.renderer.opengl;

import com.jme3.renderer.RenderContext;
import com.jme3.texture.FrameBuffer;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GLRendererFrameBufferBindingTest {
    @Test
    void mainOverrideDoesNotHideTheBoundMultisampleSceneTarget() throws ReflectiveOperationException {
        GLRenderer renderer = new GLRenderer(null, null, null);
        FrameBuffer window = new FrameBuffer(1280, 720, 1);
        FrameBuffer scene = new FrameBuffer(1280, 720, 4);
        Field override = GLRenderer.class.getDeclaredField("mainFbOverride");
        override.setAccessible(true);
        override.set(renderer, window);
        Field contextField = GLRenderer.class.getDeclaredField("context");
        contextField.setAccessible(true);
        RenderContext context = (RenderContext) contextField.get(renderer);
        context.boundFB = scene;
        assertSame(scene, renderer.getCurrentFrameBuffer());
        assertSame(window, renderer.getMainFrameBufferOverride());
        assertEquals(4, renderer.getCurrentFrameBuffer().getSamples());
        context.boundFB = window;
        assertSame(window, renderer.getCurrentFrameBuffer());
        assertSame(window, renderer.getMainFrameBufferOverride());
    }

    @Test
    void defaultTargetWithoutAnOverrideRemainsNull() {
        GLRenderer renderer = new GLRenderer(null, null, null);
        assertNull(renderer.getCurrentFrameBuffer());
        assertNull(renderer.getMainFrameBufferOverride());
    }
}
