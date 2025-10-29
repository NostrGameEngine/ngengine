/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */
package org.ngengine.components.jme3;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.ngengine.AsyncAssetManager;
import org.ngengine.ViewPortManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.StallingComponent;
import org.ngengine.components.runners.ComponentInitializer;
import org.ngengine.components.runners.ComponentLoader;
import org.ngengine.components.runners.ComponentUpdater;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.store.DataStoreProvider;

/**
 * A component manager that manages components in a JME3 application.
 */
public abstract class Jme3ComponentManager  implements ComponentManager, Savable{

    private static final Logger log = Logger.getLogger(ComponentManagerAppState.class.getName());

    protected final List<ComponentMount> componentMounts = new CopyOnWriteArrayList<>();
    protected final Map<Object, ComponentSlot> slotComponent = new ConcurrentHashMap<>();
    protected final List<ComponentInitializer> initializers = new CopyOnWriteArrayList<>();
    protected final List<ComponentLoader> loaders = new CopyOnWriteArrayList<>();
    protected final List<ComponentUpdater> updaters = new CopyOnWriteArrayList<>();

    private DataStoreProvider dataStoreProvider;
    private NGEAppSettings settings;
    private Application app;
    private ComponentManager parent;


    @Override
    public ComponentManager getParent(){
        return parent;
    }

    public void setParent(ComponentManager parent){
        this.parent = parent;
    }

    @Override
    public List<ComponentUpdater> getUpdaters() {
        return updaters;
    }

    @Override
    public List<ComponentInitializer> getInitializers() {
        return initializers;
    }

    @Override
    public List<ComponentLoader> getLoaders() {
        return loaders;
    }
 
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.writeSavableArrayList(new ArrayList<>(componentMounts),"mounts", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException{
        InputCapsule capsule = im.getCapsule(this);
        @SuppressWarnings("unchecked")
        List<ComponentMount> mounts = capsule.readSavableArrayList("mounts", null);
        if(mounts!=null){
            for(ComponentMount mount: mounts){
                addMount(mount);
            }
        }
    }
   

    protected void initializeComponentManager(Application app, NGEAppSettings settings){
        this.app = app;
        this.settings = settings;

        initializers.add(new AppComponentInitializer(app));
        updaters.add(new AppViewPortComponentUpdater(app));
        updaters.add(new AppComponentUpdater(app));
        loaders.add(new AppComponentLoader(app));
    }


    protected Application getApplication() {
        if(parent != null && parent instanceof Jme3ComponentManager){
            return ((Jme3ComponentManager)parent).getApplication();
        }
        return app;
    }

    @Override
    public NGEAppSettings getSettings() {
        if(parent != null){
            return parent.getSettings();
        }
        return this.settings;
    }
   

    public DataStoreProvider getDataStoreProvider() {
        if (dataStoreProvider == null) {
            String id = getSettings().getAppId().asBech32();
            if (id == null || id.isEmpty()) {
                id = this.getApplication().getContext().getSettings().getTitle();
            }
            dataStoreProvider = new DataStoreProvider(id, this.getApplication().getAssetManager());
        }
        return dataStoreProvider;
    }

    @Override
    public <T extends Component> T getComponent(Class<T> type) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component.getClass().equals(type)) {
                return type.cast(mount.component);
            }
        }
        return null;
    }

    @Override
    public boolean hasComponent(Component component) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component == component) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasComponent(Class<? extends Component> cls) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component.getClass().equals(cls)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasComponent(String id) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Component> T getComponentById(String id) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component.getId().equals(id)) {
                return (T) mount.component;
            }
        }
        return null;
    }

    @Override
    public List<Component> getComponentsBySlot(Object slot) {
        ComponentSlot fragmentsInSlot = slotComponent.get(slot);
        if (fragmentsInSlot == null) {
            return Collections.emptyList();
        }
        return fragmentsInSlot.getComponents();
    }

   
    private ComponentMount getMount(Component fragment) {
        for (ComponentMount mount : componentMounts) {
            if (mount.component == fragment) {
                return mount;
            }
        }
        return null;
    }

    private void addMount(ComponentMount mount) {
        mount.manager = this;
        componentMounts.add(mount);
        // Add to slot if defined
        Object slot = mount.component.getSlot();
        if (slot != null) {
            ComponentSlot cslot = this.slotComponent.computeIfAbsent(slot, k -> new ComponentSlot());
            cslot.addComponent(mount.component);
        }
        mount.newlyAttached = true;
    }


    @Override
    public void addComponent(Component component, Object... deps) {
        boolean hasCycle = hasCircularDependency(component, deps, new HashSet<>());
        if (hasCycle) {
            throw new IllegalArgumentException("Circular dependency detected for fragment: " + component.getId());
        }
        ComponentMount mount = new ComponentMount();
        mount.component = component;
        mount.deps = deps;

        addMount(mount);
    
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
    public void enableComponent(Component component) {
        ComponentMount mount = getMount(component);
        if (mount == null) {
            throw new IllegalArgumentException("Component not found: " + component.getId());
        }
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
                    Component depFragment = resolveDependency(d);
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
        if (fragment instanceof StallingComponent) {
            return false; // StallingComponent is never enabled
        }
        for (ComponentMount mount : componentMounts) {
            if (mount.component == fragment) {
                return mount.enabled;
            }
        }
        return false;
    }


    protected void runMountUpdate(){
        for(ComponentMount mount : componentMounts){
            if(mount.newlyAttached){
                mount.newlyAttached = false;
                mount.component.onAttached(this, MainThreadRunner.of(this.getApplication()), getDataStoreProvider());
            }
        }
         for (ComponentMount mount : componentMounts) {
            if (mount.ready) continue;
            if (mount.initialized.get() == Integer.MIN_VALUE) {
                mount.initialized.set(0); // if no initializer is registered, we assume it is initialized
                for (ComponentInitializer initializer : initializers) {
                    if (initializer.canInitialize(this, mount.component)) {
                        int n = initializer.initialize(
                            this,
                            mount.component,
                            () -> {
                                mount.initialized.decrementAndGet();
                            }
                        );
                        mount.initialized.addAndGet(n);
                    }
                }
            }
            if (mount.initialized.get() > 0) {
                log.fine(
                    "Component " +
                    mount.component.getId() +
                    " is not ready it still initializing. " +
                    mount.initialized.get() +
                    " left"
                );
            }
        }

        for (ComponentMount mount : componentMounts) {
            if (mount.ready || mount.initialized.get() > 0) continue;
            if (mount.loaded.get() == Integer.MIN_VALUE) {
                mount.loaded.set(0); // if no loader is registered, we assume it is loaded
                for (ComponentLoader loader : loaders) {
                    if (loader.canLoad(this, mount.component)) {
                        int n = loader.load(
                            this,
                            mount.component,
                            () -> {
                                mount.loaded.decrementAndGet();
                            }
                        );
                        mount.loaded.addAndGet(n);
                    }
                }
            }
            if (mount.loaded.get() > 0) {
                log.fine(
                    "Component " + mount.component.getId() + " is not ready it still loading. " + mount.loaded.get() + " left"
                );
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
                    if (
                        deps == null ||
                        Arrays
                            .stream(deps)
                            .allMatch(d -> {
                                if (d instanceof Class && d == StallingComponent.class) {
                                    return false; // StallingComponent is never enabled
                                }
                                Component depFragment = resolveDependency(d);
                                if (depFragment == null) {
                                    return true; // if dependency is not a fragment, we assume it is always
                                    // enabled
                                }
                                boolean ready = isComponentEnabled(depFragment);
                                if (!ready) {
                                    log.fine(
                                        "Component " +
                                        mount.component.getId() +
                                        " is not ready because dependency " +
                                        depFragment.getId() +
                                        " is not ready."
                                    );
                                }
                                return ready;
                            })
                    ) {
                        // Disable any other fragment in the same slot
                        Object slot = mount.component.getSlot();
                        if (slot != null) {
                            ComponentSlot slotFragments = this.slotComponent.get(slot);
                            if (slotFragments != null) {
                                for (Component otherFragment : slotFragments.getComponents()) {
                                    if (otherFragment != mount.component) {
                                        ComponentMount otherMount = getMount(otherFragment);
                                        if (otherMount.enabled) {
                                            otherMount.component.onDisable(
                                                this,
                                                MainThreadRunner.of(this.getApplication()),
                                                getDataStoreProvider()
                                            );
                                            otherMount.enabled = false;
                                            otherMount.desiredEnabledState = false;
                                        }
                                    }
                                }
                            }
                        }

                        // Enable the fragment
                        mount.component.onEnable(
                            this,
                            MainThreadRunner.of(this.getApplication()),
                            getDataStoreProvider(),
                            mount.isNew
                        );
                        mount.isNew = false;
                        mount.enabled = mount.desiredEnabledState;
                    } else {
                        mount.component.onNudge(
                            this,
                            MainThreadRunner.of(this.getApplication()),
                            getDataStoreProvider(),
                            mount.isNew
                        );
                    }
                } else {
                    // Disable the fragment
                    mount.component.onDisable(this, MainThreadRunner.of(this.getApplication()), getDataStoreProvider());
                    mount.enabled = mount.desiredEnabledState;
                }
            }
            if (mount.markForRemoval) {
                mount.component.onDetached(this, MainThreadRunner.of(this.getApplication()), getDataStoreProvider());
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
                componentMounts.remove(mount);
                Object slot = mount.component.getSlot();
                if (slot != null) {
                    ComponentSlot slotFragments = this.slotComponent.get(slot);
                    if (slotFragments != null) {
                        slotFragments.removeComponent(mount.component);
                        if (slotFragments.isEmpty()) {
                            this.slotComponent.remove(slot);
                        }
                    }
                }
            }
        }
    }
    
    protected void onUpdate(float tpf) {
        runMountUpdate();

        for (ComponentUpdater updater : updaters) {
            for (ComponentMount mount : componentMounts) {
                if (!mount.enabled) continue;
                if (updater.canUpdate(this, mount.component)) {
                    updater.update(this, mount.component, tpf);
                }
            }
        }
    }

    protected void onRender(RenderManager rm) {
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
            Component depFragment = resolveDependency(dep);
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
            throw new IllegalArgumentException("Circular dependency detected for fragment: " + fragment.getId());
        }
        mount.deps = deps;
    }

    @Override
    public <T> T getGlobalInstance(Class<T> type) {
        if (type == InputManager.class) {
            return type.cast(this.getApplication().getInputManager());
        }
        if (type == Application.class) {
            return type.cast(this.getApplication());
        }
        if (type == ComponentManager.class) {
            return type.cast(this);
        }
        if (type == Camera.class) {
            return type.cast(this.getApplication().getCamera());
        }
        if (type == RenderManager.class) {
            return type.cast(this.getApplication().getRenderManager());
        }
        if (type == AppStateManager.class) {
            return type.cast(this.getApplication().getStateManager());
        }
        if (type == DataStoreProvider.class) {
            return type.cast(getDataStoreProvider());
        }
        if (type == ViewPort.class) {
            return type.cast(this.getApplication().getViewPort());
        }
        if (type == AssetManager.class) {
            return type.cast(this.getApplication().getAssetManager());
        }
        if (type == AsyncAssetManager.class) {
            return type.cast(AsyncAssetManager.of(this.getApplication().getAssetManager(), this.getApplication()));
        }
        if (type == ViewPortManager.class) {
            return type.cast( new Jme3ViewPortManager(this.getApplication()));
        }
        if (type==MainThreadRunner.class){
            return type.cast(MainThreadRunner.of(this.getApplication()));
        }

        return null;
    }
}
