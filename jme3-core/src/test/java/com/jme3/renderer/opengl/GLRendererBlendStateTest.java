package com.jme3.renderer.opengl;

import com.jme3.material.RenderState;
import com.jme3.renderer.RenderContext;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GLRendererBlendStateTest {
    @Test
    void cachedDefaultsMatchNativeBlendFactors() {
        RenderContext context = new RenderContext();
        assertEquals(RenderState.BlendFunc.One, context.sfactorRGB);
        assertEquals(RenderState.BlendFunc.Zero, context.dfactorRGB);
        assertEquals(RenderState.BlendFunc.One, context.sfactorAlpha);
        assertEquals(RenderState.BlendFunc.Zero, context.dfactorAlpha);
    }

    @Test
    void firstAdditiveDrawSetsFactorsBeforeCachingThem() {
        List<int[]> factors = new ArrayList<>();
        GL gl = (GL) Proxy.newProxyInstance(GL.class.getClassLoader(), new Class<?>[]{GL.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("glBlendFunc")) {
                        factors.add(new int[]{(int) arguments[0], (int) arguments[1]});
                    }
                    return null;
                });
        GLRenderer renderer = new GLRenderer(gl, null, null);
        RenderState state = new RenderState();
        state.setBlendMode(RenderState.BlendMode.Additive);
        renderer.applyRenderState(state);
        assertEquals(1, factors.size(), "Fresh GL contexts use ONE,ZERO, not ONE,ONE");
        assertArrayEquals(new int[]{GL.GL_ONE, GL.GL_ONE}, factors.get(0));
        renderer.applyRenderState(state);
        assertEquals(1, factors.size(), "Confirmed state should not issue redundant calls");
    }
}
