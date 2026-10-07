package com.jme3.renderer.android;

import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLFbo;
import java.nio.IntBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AndroidDrawBufferTest {
    @Test
    void depthOnlyAndDefaultTargetsUseTheirExactDrawBuffer() {
        CapturingGl gl = new CapturingGl();
        gl.glDrawBuffer(GL.GL_NONE);
        assertArrayEquals(new int[]{GL.GL_NONE}, gl.drawBuffers);
        gl.glDrawBuffer(GL.GL_BACK);
        assertArrayEquals(new int[]{GL.GL_BACK}, gl.drawBuffers);
    }

    @Test
    void colorAttachmentsKeepTheirExistingMapping() {
        CapturingGl gl = new CapturingGl();
        gl.glDrawBuffer(GLFbo.GL_COLOR_ATTACHMENT0_EXT + 1);
        assertArrayEquals(new int[]{GL.GL_NONE, GLFbo.GL_COLOR_ATTACHMENT0_EXT + 1}, gl.drawBuffers);
        assertThrows(IllegalArgumentException.class, () -> gl.glDrawBuffer(-1));
    }

    private static class CapturingGl extends AndroidGL {
        private int[] drawBuffers;
        @Override public void glDrawBuffers(IntBuffer buffers) {
            drawBuffers = new int[buffers.remaining()];
            buffers.duplicate().get(drawBuffers);
        }
    }
}
