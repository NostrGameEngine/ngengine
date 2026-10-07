package com.jme3.renderer.opengl;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderContext;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GLRendererCopyFrameBufferStateTest {
    @Test
    void copiesIgnoreScissorAndRestoreItAfterSuccessOrFailure() throws ReflectiveOperationException {
        for (boolean fail : new boolean[]{false, true}) {
            List<String> events = new ArrayList<>();
            Object driver = Proxy.newProxyInstance(GL.class.getClassLoader(),
                    new Class<?>[]{GL.class, GLFbo.class, GLExt.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("glDisable") && (int) arguments[0] == GL.GL_SCISSOR_TEST) events.add("disable");
                        if (method.getName().equals("glEnable") && (int) arguments[0] == GL.GL_SCISSOR_TEST) events.add("enable");
                        if (method.getName().equals("glBindFramebufferEXT") && (int) arguments[0] == GLFbo.GL_FRAMEBUFFER_EXT) {
                            assertEquals(7, arguments[1]);
                            events.add("restore");
                        }
                        if (method.getName().equals("glBlitFramebufferEXT")) {
                            events.add("blit");
                            if (fail) throw new IllegalStateException("copy failed");
                        }
                        return null;
                    });
            GLRenderer renderer = new GLRenderer((GL) driver, (GLExt) driver, (GLFbo) driver);
            renderer.getCaps().add(Caps.FrameBufferBlit);
            Field field = GLRenderer.class.getDeclaredField("context");
            field.setAccessible(true);
            RenderContext context = (RenderContext) field.get(renderer);
            context.boundFBO = 7;
            context.clipRectEnabled = true;
            if (fail) assertThrows(IllegalStateException.class, () -> renderer.copyFrameBuffer(null, null, true, false));
            else renderer.copyFrameBuffer(null, null, true, false);
            assertEquals(List.of("disable", "blit", "enable", "restore"), events);
            assertTrue(context.clipRectEnabled);
            assertEquals(7, context.boundFBO);
        }
    }
}
