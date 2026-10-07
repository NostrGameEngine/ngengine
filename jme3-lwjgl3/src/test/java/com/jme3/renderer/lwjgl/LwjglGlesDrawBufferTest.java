package com.jme3.renderer.lwjgl;

import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLFbo;
import java.nio.IntBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LwjglGlesDrawBufferTest {
    @Test
    void depthOnlyFramebufferDisablesColorWrites() {
        CapturingGles gl = new CapturingGles();
        gl.glDrawBuffer(GL.GL_NONE);
        assertArrayEquals(new int[]{GL.GL_NONE}, gl.drawBuffers);
    }

    @Test
    void colorAttachmentsKeepTheirExistingMapping() {
        CapturingGles gl = new CapturingGles();
        gl.glDrawBuffer(GLFbo.GL_COLOR_ATTACHMENT0_EXT + 2);
        assertArrayEquals(new int[]{GL.GL_NONE, GL.GL_NONE, GLFbo.GL_COLOR_ATTACHMENT0_EXT + 2}, gl.drawBuffers);
        assertThrows(IllegalArgumentException.class, () -> gl.glDrawBuffer(-1));
    }

    private static class CapturingGles extends LwjglGLES {
        private int[] drawBuffers;
        @Override public void glDrawBuffers(IntBuffer buffers) {
            drawBuffers = new int[buffers.remaining()];
            buffers.duplicate().get(drawBuffers);
        }
    }
}
