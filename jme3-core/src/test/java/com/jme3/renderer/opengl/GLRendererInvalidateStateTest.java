package com.jme3.renderer.opengl;

import com.jme3.renderer.Caps;
import com.jme3.material.RenderState;
import com.jme3.renderer.RenderContext;
import com.jme3.texture.FrameBuffer;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GLRendererInvalidateStateTest {
    @Test void invalidationRestoresNativeMasksAndBlendStateBeforeDefaultRendering() {
        boolean[] depthWrite = {false};
        boolean[] colorWrite = {false};
        boolean[] blending = {true};
        int[] activeTexture = {GL.GL_TEXTURE0 + 4};
        GL gl = (GL) Proxy.newProxyInstance(GL.class.getClassLoader(), new Class<?>[]{GL.class},
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "glDepthMask": depthWrite[0] = (boolean) arguments[0]; break;
                        case "glColorMask": colorWrite[0] = (boolean) arguments[0] && (boolean) arguments[1]
                                && (boolean) arguments[2] && (boolean) arguments[3]; break;
                        case "glDisable": if ((int) arguments[0] == GL.GL_BLEND) blending[0] = false; break;
                        case "glActiveTexture": activeTexture[0] = (int) arguments[0]; break;
                    }
                    return null;
                });
        GLRenderer renderer = new GLRenderer(gl, null, null);
        renderer.invalidateState();
        renderer.applyRenderState(RenderState.DEFAULT);
        assertTrue(depthWrite[0]);
        assertTrue(colorWrite[0]);
        assertFalse(blending[0]);
        assertEquals(GL.GL_TEXTURE0, activeTexture[0]);
    }

    @Test void es3CapturesItsDrawAndReadBuffersAndResetsTheCache() throws ReflectiveOperationException {
        Queries queries = new Queries(0x8825, 0x8CE0, 0x8CE1);
        GLRenderer renderer = new GLRenderer(queries.gl(GL2.class, GLES_30.class), null, null);
        renderer.getCaps().add(Caps.OpenGLES20);
        renderer.getCaps().add(Caps.OpenGLES30);
        RenderContext context = context(renderer);
        prime(context);

        renderer.invalidateState();

        assertEquals(List.of(0x8825, GL2.GL_READ_BUFFER), queries.names);
        assertEquals(0x8CE0, context.initialDrawBuf);
        assertEquals(0x8CE1, context.initialReadBuf);
        assertReset(context);
    }

    @Test void desktopKeepsItsDrawAndReadQueriesAndResetsTheCache() throws ReflectiveOperationException {
        Queries queries = new Queries(GL2.GL_DRAW_BUFFER, GL.GL_FRONT, GL.GL_BACK);
        GLRenderer renderer = new GLRenderer(queries.gl(GL2.class), null, null);
        renderer.getCaps().add(Caps.OpenGL20);
        RenderContext context = context(renderer);
        prime(context);

        renderer.invalidateState();

        assertEquals(List.of(GL2.GL_DRAW_BUFFER, GL2.GL_READ_BUFFER), queries.names);
        assertEquals(GL.GL_FRONT, context.initialDrawBuf);
        assertEquals(GL.GL_BACK, context.initialReadBuf);
        assertReset(context);
    }

    @Test void plainGlDoesNotQueryBuffersButStillResetsTheCache() throws ReflectiveOperationException {
        Queries queries = new Queries(null, 0, 0);
        GLRenderer renderer = new GLRenderer(queries.gl(GL.class), null, null);
        RenderContext context = context(renderer);
        prime(context);

        renderer.invalidateState();

        assertTrue(queries.names.isEmpty());
        assertReset(context);
    }

    @Test void es2OrUninitializedEsProviderDoesNotQueryEs3Buffers() throws ReflectiveOperationException {
        for (boolean initialized : new boolean[]{false, true}) {
            Queries queries = new Queries(null, 0, 0);
            GLRenderer renderer = new GLRenderer(queries.gl(GL2.class, GLES_30.class), null, null);
            if (initialized) renderer.getCaps().add(Caps.OpenGLES20);
            RenderContext context = context(renderer);
            prime(context);

            renderer.invalidateState();

            assertTrue(queries.names.isEmpty());
            assertReset(context);
        }
    }

    private static RenderContext context(GLRenderer renderer) throws ReflectiveOperationException {
        Field field = GLRenderer.class.getDeclaredField("context");
        field.setAccessible(true);
        return (RenderContext) field.get(renderer);
    }

    private static void prime(RenderContext context) {
        context.boundShaderProgram = 91;
        context.boundFBO = 92;
        context.boundFB = new FrameBuffer(16, 16, 1);
        context.boundArrayVBO = 93;
        context.boundTextureUnit = 4;
        context.initialDrawBuf = 94;
        context.initialReadBuf = 95;
    }

    private static void assertReset(RenderContext context) {
        assertEquals(0, context.boundShaderProgram);
        assertEquals(0, context.boundFBO);
        assertNull(context.boundFB);
        assertEquals(0, context.boundArrayVBO);
        assertEquals(0, context.boundTextureUnit);
    }

    private static final class Queries {
        private final Integer drawName;
        private final int drawValue;
        private final int readValue;
        private final List<Integer> names = new ArrayList<>();

        private Queries(Integer drawName, int drawValue, int readValue) {
            this.drawName = drawName;
            this.drawValue = drawValue;
            this.readValue = readValue;
        }

        private GL gl(Class<?>... interfaces) {
            return (GL) Proxy.newProxyInstance(GL.class.getClassLoader(), interfaces, (proxy, method, arguments) -> {
                if (!method.getName().equals("glGetInteger")) return null;
                int name = (Integer) arguments[0];
                names.add(name);
                assertNotNull(drawName, "This provider does not support draw/read buffer queries");
                assertTrue(name == drawName || name == GL2.GL_READ_BUFFER,
                        "Unsupported draw-buffer query enum: 0x" + Integer.toHexString(name));
                ((IntBuffer) arguments[1]).put(0, name == drawName ? drawValue : readValue);
                return null;
            });
        }
    }
}
