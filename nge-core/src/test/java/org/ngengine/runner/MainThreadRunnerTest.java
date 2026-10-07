package org.ngengine.runner;

import com.jme3.app.LegacyApplication;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.ngengine.platform.NGEPlatform;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainThreadRunnerTest {
    @Test
    void preInitializationCallbacksUseBoundApplicationAndRemainQueued() {
        QueueApplication app = new QueueApplication();
        MainThreadRunner runner = new MainThreadRunner(app);
        AtomicInteger calls = new AtomicInteger();
        assertFalse(runner.isInitialized());
        assertThrows(IllegalStateException.class, runner::checkThread);
        runner.run(calls::incrementAndGet);
        runner.enqueue(calls::incrementAndGet);
        assertEquals(0, calls.get());
        runner.initialize(app.getStateManager(), app);
        app.drain();
        assertEquals(2, calls.get());
        runner.checkThread();
    }

    @Test
    void initializedOwnerRunsImmediatelyAndForeignCallbacksStayQueued() throws Exception {
        QueueApplication app = new QueueApplication();
        MainThreadRunner runner = new MainThreadRunner(app);
        runner.initialize(app.getStateManager(), app);
        NGEPlatform platform = mock(NGEPlatform.class, CALLS_REAL_METHODS);
        AtomicInteger calls = new AtomicInteger();
        try (MockedStatic<NGEPlatform> current = mockStatic(NGEPlatform.class)) {
            current.when(NGEPlatform::get).thenReturn(platform);
            runner.run(calls::incrementAndGet);
            assertEquals(1, calls.get());
            runner.enqueue(calls::incrementAndGet);
            assertEquals(1, calls.get());
            app.drain();
            assertEquals(2, calls.get());
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread foreign = new Thread(() -> {
            try (MockedStatic<NGEPlatform> current = mockStatic(NGEPlatform.class)) {
                current.when(NGEPlatform::get).thenReturn(platform);
                assertThrows(IllegalStateException.class, runner::checkThread);
                runner.run(calls::incrementAndGet);
            } catch (Throwable problem) { failure.set(problem); }
        });
        foreign.start(); foreign.join();
        assertNull(failure.get());
        assertEquals(2, calls.get());
        app.drain();
        assertEquals(3, calls.get());
    }

    private static final class QueueApplication extends LegacyApplication {
        void drain() { runQueuedTasks(); }
    }
}
