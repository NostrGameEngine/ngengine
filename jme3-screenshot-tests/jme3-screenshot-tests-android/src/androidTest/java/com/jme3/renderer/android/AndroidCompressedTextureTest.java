package com.jme3.renderer.android;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLES30;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AndroidCompressedTextureTest {
    @Test
    public void compressedUploadsUseTheMipRangeInsteadOfTheCumulativeLimit() {
        EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        assertTrue(EGL14.eglInitialize(display, new int[1], 0, new int[1], 0));
        EGLConfig[] configs = new EGLConfig[1];
        int[] count = new int[1];
        assertTrue(EGL14.eglChooseConfig(display, new int[] {
                EGL14.EGL_RENDERABLE_TYPE, 0x40, // EGL_OPENGL_ES3_BIT_KHR
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_NONE
        }, 0, configs, 0, configs.length, count, 0));
        assertTrue(count[0] > 0);
        EGLSurface surface = EGL14.eglCreatePbufferSurface(display, configs[0], new int[] {
                EGL14.EGL_WIDTH, 4, EGL14.EGL_HEIGHT, 4, EGL14.EGL_NONE
        }, 0);
        EGLContext context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                new int[] {EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE}, 0);
        try {
            assertTrue(EGL14.eglMakeCurrent(display, surface, surface, context));
            ByteBuffer data = ByteBuffer.allocateDirect(80);
            AndroidGL gl = new AndroidGL();
            int tex2d = GLES20.GL_TEXTURE_2D;
            int array = GLES30.GL_TEXTURE_2D_ARRAY;
            int format = GLES30.GL_COMPRESSED_RGBA8_ETC2_EAC;
            int[] textures = new int[2];
            GLES20.glGenTextures(textures.length, textures, 0);

            GLES20.glBindTexture(tex2d, textures[0]);
            data.limit(64);
            gl.glCompressedTexImage2D(tex2d, 0, format, 8, 8, 0, data);
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError());
            data.limit(80).position(64);
            gl.glCompressedTexImage2D(tex2d, 1, format, 4, 4, 0, data);
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError());
            gl.glCompressedTexSubImage2D(tex2d, 1, 0, 0, 4, 4, format, data);
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError());

            GLES20.glBindTexture(array, textures[1]);
            data.position(0).limit(64);
            gl.glCompressedTexImage3D(array, 0, format, 8, 8, 1, 0, data);
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError());
            data.limit(80).position(64);
            gl.glCompressedTexImage3D(array, 1, format, 4, 4, 1, 0, data);
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError());
            gl.glCompressedTexSubImage3D(array, 1, 0, 0, 0, 4, 4, 1, format, data);
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError());
            assertEquals(64, data.position());
            assertEquals(80, data.limit());
        } finally {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(display, context);
            EGL14.eglDestroySurface(display, surface);
            EGL14.eglTerminate(display);
        }
    }
}
