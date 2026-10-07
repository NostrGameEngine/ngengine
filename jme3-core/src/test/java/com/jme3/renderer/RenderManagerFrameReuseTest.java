package com.jme3.renderer;

import com.jme3.renderer.pipeline.PipelineContext;
import com.jme3.renderer.pipeline.RenderPipeline;
import com.jme3.system.NanoTimer;
import com.jme3.system.NullRenderer;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderManagerFrameReuseTest {
    @Test
    void sharedContextsAndPipelinesFinishOncePerFrameInRegistrationOrder() {
        NullRenderer backing = new NullRenderer();
        Renderer renderer = (Renderer) Proxy.newProxyInstance(Renderer.class.getClassLoader(),
                new Class<?>[]{Renderer.class}, (proxy, method, arguments) -> method.invoke(backing, arguments));
        RenderManager manager = new RenderManager(renderer);
        manager.setTimer(new NanoTimer());
        List<String> ends = new ArrayList<>();
        TestContext context = new TestContext(ends);
        TestPipeline first = new TestPipeline("first", context, ends);
        TestPipeline second = new TestPipeline("second", context, ends);
        manager.createMainView("one", new Camera(64, 64)).setPipeline(first);
        manager.createMainView("two", new Camera(64, 64)).setPipeline(first);
        manager.createMainView("three", new Camera(64, 64)).setPipeline(second);
        for (int frame = 1; frame <= 3; frame++) {
            ends.clear();
            manager.render(.016f, true);
            assertEquals(List.of("context", "first", "second"), ends);
            assertEquals(frame, context.ends);
            assertEquals(frame, first.starts);
            assertEquals(frame, second.starts);
            assertEquals(frame * 2, first.renders);
            assertEquals(frame, second.renders);
        }
    }

    private static class TestContext implements PipelineContext {
        private final List<String> events;
        private boolean started;
        private int ends;

        TestContext(List<String> events) { this.events = events; }

        @Override public boolean startViewPortRender(RenderManager manager, ViewPort viewport) {
            boolean previous = started;
            started = true;
            return previous;
        }
        @Override public void endViewPortRender(RenderManager manager, ViewPort viewport) {}
        @Override public void endContextRenderFrame(RenderManager manager) {
            events.add("context");
            ends++;
            started = false;
        }
    }

    private static class TestPipeline implements RenderPipeline<TestContext> {
        private final String name;
        private final TestContext context;
        private final List<String> events;
        private boolean started;
        private int starts;
        private int renders;

        TestPipeline(String name, TestContext context, List<String> events) {
            this.name = name;
            this.context = context;
            this.events = events;
        }
        @Override public TestContext fetchPipelineContext(RenderManager manager) { return context; }
        @Override public boolean hasRenderedThisFrame() { return started; }
        @Override public void startRenderFrame(RenderManager manager) { started = true; starts++; }
        @Override public void pipelineRender(RenderManager manager, TestContext context, ViewPort viewport, float tpf) { renders++; }
        @Override public void endRenderFrame(RenderManager manager) { events.add(name); started = false; }
    }
}
