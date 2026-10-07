package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.jme3.system.JmeContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jmonkeyengine.screenshottests.testframework.TestContainingApp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DesktopRunnerLifecycleTest {
    @Test
    void completedMacCaptureStopsTheBlockingFirstThreadLoop() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.MACOSX);
        CountDownLatch done = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        TestContainingApp app = new TestContainingApp() {
            @Override public void start(JmeContext.Type type) {
                done.countDown();
                try {
                    if (!stopped.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Completed capture did not stop the event loop");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(error);
                }
            }
            @Override public void stop(boolean waitFor) { stopped.countDown(); }
        };
        new DesktopRunner().runApplicationUntilScenarioCompletes(app, done);
        assertEquals(0, stopped.getCount());
    }

    @Test
    void startupFailureIsReportedInsteadOfPassing() {
        CountDownLatch done = new CountDownLatch(1);
        RuntimeException failure = new RuntimeException("startup failed");
        TestContainingApp app = new TestContainingApp() {
            @Override public void start(JmeContext.Type type) { throw failure; }
            @Override public void stop(boolean waitFor) {}
        };
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> new DesktopRunner().runApplicationUntilScenarioCompletes(app, done));
        assertSame(failure, error.getCause());
        assertEquals(0, done.getCount());
    }

    @Test
    void asynchronousApplicationFailureIsReportedInsteadOfPassing() {
        CountDownLatch done = new CountDownLatch(1);
        RuntimeException failure = new RuntimeException("render failed");
        TestContainingApp app = new TestContainingApp() {
            @Override public void start(JmeContext.Type type) { onError.accept(failure); }
            @Override public void stop(boolean waitFor) {}
        };
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> new DesktopRunner().runApplicationUntilScenarioCompletes(app, done));
        assertSame(failure, error.getCause());
        assertEquals(0, done.getCount());
    }

    @Test
    void successfulMacStartupUsesTheCallingThread() {
        CountDownLatch done = new CountDownLatch(1);
        Thread caller = Thread.currentThread();
        Thread[] launchThread = new Thread[1];
        TestContainingApp app = new TestContainingApp() {
            @Override public void start(JmeContext.Type type) {
                launchThread[0] = Thread.currentThread();
                done.countDown();
            }
            @Override public void stop(boolean waitFor) {}
        };
        new DesktopRunner().runApplicationUntilScenarioCompletes(app, done);
        assertNotNull(launchThread[0]);
        if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.MACOSX) {
            assertSame(caller, launchThread[0]);
        }
    }
}
