package org.ngengine.components.jme3;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentInitializer;
import org.ngengine.components.ComponentLoader;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentUpdater;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.RenderManager;

/**
 * A component manager that manages components in a JME3 application.
 */
public class ComponentManagerAppState extends BaseAppState implements ComponentManager {
    private static final Logger log = Logger.getLogger(ComponentManagerAppState.class.getName());

    private static class ComponentMount {
        
        Component component;
        boolean enabled;
        boolean desiredEnabledState;
        boolean ready;
        boolean markForRemoval;
        boolean isNew = true;
        Object[] deps;
        Object arg;
        final AtomicInteger initialized = new AtomicInteger(Integer.MIN_VALUE); // Integer.MIN_VALUE means not
                                                                                // initialized, 0 means ready,
                                                                                // >0 means pending
        final AtomicInteger loaded = new AtomicInteger(Integer.MIN_VALUE); // Integer.MIN_VALUE means not
        // loaded, 0 means ready,
        // >0 means pending

    }

    private static class ComponentSlot {
        final List<Component> components = new CopyOnWriteArrayList<>();
        final List<Component> componentsRO = Collections.unmodifiableList(components);
    }

    private final List<ComponentMount> componentMounts = new CopyOnWriteArrayList<>();
    private final Map<Object, ComponentSlot> slotComponent = new ConcurrentHashMap<>();
    private final List<Component> components = new CopyOnWriteArrayList<>();
    private final List<Component> componentsRO = Collections.unmodifiableList(components);
    private final List<ComponentInitializer> initializers = new CopyOnWriteArrayList<>();
    private final List<ComponentLoader> loaders = new CopyOnWriteArrayList<>();
    private final List<ComponentUpdater> updaters = new CopyOnWriteArrayList<>();

    private DataStoreProvider dataStoreProvider;
    private Application app;
    public ComponentManagerAppState(Application app) {
        super();
        this.app = app;
    }

    public void addInitializer(ComponentInitializer initializer) {
        if (initializer == null) {
            throw new IllegalArgumentException("Initializer cannot be null");
        }
        initializers.add(initializer);
    }

    public void removeInitializer(ComponentInitializer initializer) {
        if (initializer == null) {
            throw new IllegalArgumentException("Initializer cannot be null");
        }
        initializers.remove(initializer);
    }

    public void addUpdater(ComponentUpdater updater) {
        if (updater == null) {
            throw new IllegalArgumentException("Updater cannot be null");
        }
        updaters.add(updater);
    }

    public void addLoader(ComponentLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("Loader cannot be null");
        }
        loaders.add(loader);
    }

    public void removeLoader(ComponentLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("Loader cannot be null");
        }
        loaders.remove(loader);
    }

    public void removeUpdater(ComponentUpdater updater) {
        if (updater == null) {
            throw new IllegalArgumentException("Updater cannot be null");
        }
        updaters.remove(updater);
    }

    private DataStoreProvider getDataStoreProvider() {
        if (dataStoreProvider == null) {
            dataStoreProvider = new DataStoreProvider(this.app.getContext().getSettings().getTitle(), this.app.getAssetManager());
        }
        return dataStoreProvider;
    }

    @Override
    public <T extends Component> T getComponentByType(Class<T> type) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component.getClass().equals(type)) {
                return type.cast(mount.component);
            }
        }
        return null;
    }

    @Override
    public Component getComponentById(String id) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component.getId().equals(id)) {
                return mount.component;
            }
        }
        return null;
    }

    @Override
    public List<Component> getComponentBySlot(Object slot) {
        ComponentSlot fragmentsInSlot = slotComponent.get(slot);
        if (fragmentsInSlot == null) {
            return Collections.emptyList();
        }
        return fragmentsInSlot.componentsRO;
    }

    @Override
    public List<Component> getComponent() {
        return componentsRO;
    }

    private ComponentMount getMount(Component fragment) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component == fragment) {
                return mount;
            }
        }
        return null;
    }

    @Override
    public void addComponent(Component component, Object... deps) {
        boolean hasCycle = hasCircularDependency(component, deps, new HashSet<>());
        if (hasCycle) {
            throw new IllegalArgumentException(
                    "Circular dependency detected for fragment: " + component.getId());
        }
        ComponentMount mount = new ComponentMount();
        mount.component = component;
        mount.deps = deps;

        componentMounts.add(mount);
        components.add(component);

        // Add to slot if defined
        Object slot = component.getSlot();
        if (slot != null) {
            ComponentSlot cslot = this.slotComponent.computeIfAbsent(slot, k -> new ComponentSlot());
            cslot.components.add(component);
        }
        component.onAttached(this, MainThreadRunner.of(this.app), getDataStoreProvider());

    }
    

    @Override
    public void removeComponent(Component component) {
        ComponentMount mount = getMount(component);
        if (mount == null) {
            log.warning("Attempted to remove non-existent component: " + component.getId());

            return;
        }
        disableComponent(component); // Ensure the fragment is disabled before removal
        mount.markForRemoval = true;
    }

    @Override
    public <T> void enableComponent(Component component, T arg) {
        ComponentMount mount = getMount(component);
        if (mount == null) {
            throw new IllegalArgumentException("Component not found: " + component.getId());
        }
        mount.arg = arg;
        mount.desiredEnabledState = true;
    }


    @Override
    public void disableComponent(Component component) {
        ComponentMount mount = getMount(component);
        if (mount == null) {
            throw new IllegalArgumentException("Fragment not found: " + component.getId());
        }
        // disable all depdendencies
        for (ComponentMount m : componentMounts) {
            Object deps[] = m.deps;
            if (deps != null) {
                for (Object d : deps) {
                    Component depFragment = getFragment(d);
                    if (depFragment != null && depFragment == mount.component) {
                        m.desiredEnabledState = false;
                    }
                }
            }
        }

        // Disable the fragment itself
        mount.desiredEnabledState = false;
    }

    @Override
    public boolean isComponentEnabled(Component fragment) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component == fragment) {
                return mount.enabled;
            }
        }
        return false;
    }

    @Override
    protected void initialize(Application app) {

    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

    @SuppressWarnings("unchecked")
    private Component getFragment(Object d) {
        if (d instanceof Component) {
            return (Component) d;
        } else if (d instanceof String) {
            return getComponentById((String) d);
        } else if (d instanceof Class<?>) {
            return getComponentByType((Class<? extends Component>) d);

        }
        return null;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        for (ComponentMount mount : componentMounts) {
            if (mount.ready) continue;
            if (mount.initialized.get() == Integer.MIN_VALUE) {
                mount.initialized.set(0); // if no initializer is registered, we assume it is initialized
                for (ComponentInitializer initializer : initializers) {
                    if (initializer.canInitialize(this, mount.component)) {
                        int n = initializer.initialize(this, mount.component, () -> {
                            mount.initialized.decrementAndGet();
                        });
                        mount.initialized.addAndGet(n);
                    }
                }
            }
            if (mount.initialized.get() > 0) {
                log.fine("Component " + mount.component.getId() + " is not ready it still initializing. "
                        + mount.initialized.get() + " left");
            }
        }

        for (ComponentMount mount : componentMounts) {
            if (mount.ready || mount.initialized.get() > 0) continue;
            if (mount.loaded.get() == Integer.MIN_VALUE) {
                mount.loaded.set(0); // if no loader is registered, we assume it is loaded
                for (ComponentLoader loader : loaders) {
                    if (loader.canLoad(this, mount.component)) {
                        int n = loader.load(this, mount.component, () -> {
                            mount.loaded.decrementAndGet();
                        });
                        mount.loaded.addAndGet(n);
                    }
                }
            }
            if (mount.loaded.get() > 0) {
                log.fine("Component " + mount.component.getId() + " is not ready it still loading. "
                        + mount.loaded.get() + " left");
            }
        }

        for (ComponentMount mount : componentMounts) {
            if (!mount.ready) {
                if (mount.initialized.get() == 0 && mount.loaded.get() == 0) {
                    mount.ready = true;
                } else {
                    continue;
                }
            }

            if (mount.enabled != mount.desiredEnabledState) {
                if (mount.desiredEnabledState) {
                    Object deps[] = mount.deps;

                    // Check if all dependencies are enabled
                    if (deps == null || Arrays.stream(deps).allMatch(d -> {
                        Component depFragment = getFragment(d);
                        if (depFragment == null) {
                            return true; // if dependency is not a fragment, we assume it is always
                                         // enabled
                        }
                        boolean ready = isComponentEnabled(depFragment);
                        if (!ready) {
                            log.fine("Component " + mount.component.getId()
                                    + " is not ready because dependency " + depFragment.getId()
                                    + " is not ready.");
                        }
                        return ready;
                    })) {

                        // Disable any other fragment in the same slot
                        Object slot = mount.component.getSlot();
                        if (slot != null) {
                            ComponentSlot slotFragments = this.slotComponent.get(slot);
                            if (slotFragments != null) {
                                for (Component otherFragment : slotFragments.components) {
                                    if (otherFragment != mount.component) {
                                        ComponentMount otherMount = getMount(otherFragment);
                                        if (otherMount.enabled) {
                                            otherMount.component.onDisable(this,
                                                    MainThreadRunner.of(this.app), getDataStoreProvider());
                                            otherMount.enabled = false;
                                            otherMount.desiredEnabledState = false;
                                        }

                                    }
                                }
                            }
                        }

                        // Enable the fragment
                        mount.component.onEnable(this, MainThreadRunner.of(this.app), getDataStoreProvider(),
                                mount.isNew, mount.arg);
                        mount.isNew = false;
                        mount.enabled = mount.desiredEnabledState;
                    } else {
                        mount.component.onNudge(this, MainThreadRunner.of(this.app), getDataStoreProvider(),
                                mount.isNew, mount.arg);
                    }
                } else {

                    // Disable the fragment
                    mount.component.onDisable(this, MainThreadRunner.of(this.app), getDataStoreProvider());
                    mount.enabled = mount.desiredEnabledState;
                }
            }
            if (mount.markForRemoval) {
                mount.component.onDetached(this, MainThreadRunner.of(this.app), getDataStoreProvider());
                for (ComponentInitializer initializer : initializers) {
                    if (initializer.canInitialize(this, mount.component)) {
                        initializer.cleanup(this, mount.component);
                    }
                }
                for (ComponentLoader loader : loaders) {
                    if (loader.canLoad(this, mount.component)) {
                        loader.unload(this, mount.component);
                    }
                }
                components.remove(mount.component);
                componentMounts.remove(mount);
                Object slot = mount.component.getSlot();
                if (slot != null) {
                    ComponentSlot slotFragments = this.slotComponent.get(slot);
                    if (slotFragments != null) {
                        slotFragments.components.remove(mount.component);
                        if (slotFragments.components.isEmpty()) {
                            this.slotComponent.remove(slot);
                        }
                    }
                }
            }


               

        }

        for (ComponentUpdater updater: updaters) {
            for (ComponentMount mount : componentMounts) {
                if (!mount.enabled) continue;
                if (updater.canUpdate(this, mount.component)) {
                    updater.update(this, mount.component, tpf);
                }
            }
        }
    }

    
    @Override
    public void render(RenderManager rm) {
        for (ComponentUpdater updater : updaters) {
            for (ComponentMount mount : componentMounts) {
                if (!mount.enabled) continue;
                if (updater.canUpdate(this, mount.component)) {
                    updater.render(this, mount.component);
                }
            }
        }
    }

    private boolean hasCircularDependency(Component fragment, Object[] deps, Set<Component> visited) {
        if (deps == null || deps.length == 0) {
            return false;
        }

        // If we've already visited this fragment in this path, we have a cycle
        if (visited.contains(fragment)) {
            return true;
        }

        // Add current fragment to visited set
        visited.add(fragment);

        for (Object dep : deps) {
            Component depFragment = getFragment(dep);
            if (depFragment == null) {
                continue; // Skip non-fragment dependencies
            }

            ComponentMount depMount = getMount(depFragment);
            if (depMount != null && depMount.deps != null) {
                // Recursively check this dependency's dependencies
                if (hasCircularDependency(depFragment, depMount.deps, new HashSet<>(visited))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void updateComponentDependencies(Component fragment, Object... deps) {
        ComponentMount mount = getMount(fragment);
        if (mount == null) {
            throw new IllegalArgumentException("Fragment not found: " + fragment.getId());
        }
        boolean hasCycle = hasCircularDependency(fragment, deps, new HashSet<>());
        if (hasCycle) {
            throw new IllegalArgumentException(
                    "Circular dependency detected for fragment: " + fragment.getId());
        }
        mount.deps = deps;
    }
}
