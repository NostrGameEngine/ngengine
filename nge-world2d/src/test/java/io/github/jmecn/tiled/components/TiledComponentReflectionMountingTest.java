package io.github.jmecn.tiled.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.AbstractComponentManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;

/**
 * Unit tests for shared reflection-based tiled component mounting helpers.
 */
public class TiledComponentReflectionMountingTest {

    @Test
    public void parseComponentClassNamesSupportsNewLinesAndCommas() {
        List<String> names = TiledComponentReflectionMounting.parseComponentClassNames(
            "a.b.C1\n a.b.C2, a.b.C3  | a.b.C4"
        );
        assertEquals(4, names.size());
        assertEquals("a.b.C1", names.get(0));
        assertEquals("a.b.C2", names.get(1));
        assertEquals("a.b.C3", names.get(2));
        assertEquals("a.b.C4", names.get(3));
    }

    @Test
    public void mountByClassNameAddsAndEnablesOnlyOnce() {
        TestManager manager = new TestManager();

        Component first = TiledComponentReflectionMounting.mountByClassName(
            manager,
            TestComponent.class.getName(),
            "owner"
        );
        assertNotNull(first);
        manager.runLifecycle();
        assertTrue(manager.isComponentEnabled(first));
        assertEquals(1, manager.getAllComponents().size());

        Component second = TiledComponentReflectionMounting.mountByClassName(
            manager,
            TestComponent.class.getName(),
            "owner"
        );
        assertNotNull(second);
        assertSame(first, second);
        assertEquals(1, manager.getAllComponents().size());
    }

    @Test
    public void mountByClassNameReturnsNullForInvalidClass() {
        TestManager manager = new TestManager();
        Component out = TiledComponentReflectionMounting.mountByClassName(manager, "not.a.RealClass", "owner");
        assertNull(out);
        assertEquals(0, manager.getAllComponents().size());
    }

    /** Minimal manager used by mount tests. */
    public static class TestManager extends AbstractComponentManager {
        void runLifecycle() {
            runMountUpdate();
        }
    }

    /** Minimal concrete component used to verify reflection mount behavior. */
    public static class TestComponent extends AbstractComponent {
        @Override
        protected void onEnable(ComponentManager mng, boolean firstTime) {}

        @Override
        protected void onDisable(ComponentManager mng) {}
    }
}
