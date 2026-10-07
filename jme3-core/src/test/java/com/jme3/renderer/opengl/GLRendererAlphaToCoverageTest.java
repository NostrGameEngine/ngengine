package com.jme3.renderer.opengl;

import com.jme3.renderer.Caps;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GLRendererAlphaToCoverageTest {
    @Test void glesCoverageDoesNotRequireDesktopMultisampleEnable() {
        verifyCoverage(Caps.OpenGLES20);
    }

    @Test void desktopAndWebGlCoverageRemainSupported() {
        verifyCoverage(Caps.Multisample);
        verifyCoverage(Caps.WebGL);
    }

    @Test void unsupportedRendererDoesNotIssueGlCalls() {
        Calls calls = new Calls();
        GLRenderer renderer = new GLRenderer(calls.gl(), null, null);
        renderer.setAlphaToCoverage(true);
        assertFalse(renderer.getAlphaToCoverage());
        assertTrue(calls.names.isEmpty());
    }

    private static void verifyCoverage(Caps capability) {
        Calls calls = new Calls();
        GLRenderer renderer = new GLRenderer(calls.gl(), null, null);
        renderer.getCaps().add(capability);
        renderer.setAlphaToCoverage(true);
        assertTrue(renderer.getAlphaToCoverage());
        renderer.setAlphaToCoverage(false);
        assertFalse(renderer.getAlphaToCoverage());
        assertEquals(List.of("glEnable", "glIsEnabled", "glDisable", "glIsEnabled"), calls.names);
    }

    private static final class Calls {
        private final List<String> names = new ArrayList<>();
        private boolean enabled;

        private GL gl() {
            return (GL) Proxy.newProxyInstance(GL.class.getClassLoader(), new Class<?>[]{GL.class},
                    (proxy, method, arguments) -> {
                        assertEquals(GLExt.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB, arguments[0]);
                        names.add(method.getName());
                        switch (method.getName()) {
                            case "glEnable": enabled = true; return null;
                            case "glDisable": enabled = false; return null;
                            case "glIsEnabled": return enabled;
                            default: throw new AssertionError("Unexpected GL call " + method.getName());
                        }
                    });
        }
    }
}
