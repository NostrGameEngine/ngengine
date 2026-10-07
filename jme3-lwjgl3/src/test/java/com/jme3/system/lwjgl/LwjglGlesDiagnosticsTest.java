/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.system.lwjgl;

import com.jme3.renderer.lwjgl.LwjglGLES;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GLES_30;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.system.AppSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LwjglGlesDiagnosticsTest {
    @Test
    void diagnosticCombinationsPreserveAllBackendInterfaces() {
        for (int flags = 0; flags < 8; flags++) {
            AppSettings settings = new AppSettings(true);
            settings.setGraphicsDebug((flags & 1) != 0);
            settings.setGraphicsTiming((flags & 2) != 0);
            settings.setGraphicsTrace((flags & 4) != 0);
            LwjglGLES nativeGl = mock(LwjglGLES.class);
            GL decorated = LwjglContext.wrapGles(nativeGl, settings);
            for (Class<?> type : new Class<?>[]{GL.class, GL2.class, GLES_30.class,
                    GLExt.class, GLFbo.class}) {
                assertTrue(type.isInstance(decorated), type.getSimpleName() + " flags=" + flags);
            }
            assertEquals(GL.GL_NO_ERROR, decorated.glGetError());
            verify(nativeGl).glGetError();
        }
    }
}
