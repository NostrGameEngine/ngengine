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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.components;

import org.ngengine.components.fragments.Fragment;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

/**
 * A component is a reusable piece of functionality that can be attached to a {@link ComponentManager}.
 * Components are composed of various fragments that define their behavior and capabilities.
 * <p>
 * Components can be attached and detached from a {@link ComponentManager} and can be enabled or disabled with
 * specific arguments. They can also define a slot for mutually exclusive components.
 *
 * <h3>Component Lifecycle</h3>
 * <ol>
 * <li><strong>Attached:</strong> {@link #onAttached(ComponentManager, Runner, DataStoreProvider)} is called
 * when the component is first added to the manager</li>
 * <li><strong>Enabled:</strong>
 * {@link #onEnable(ComponentManager, Runner, DataStoreProvider, boolean, Object)} is called when the
 * component and all its dependencies are ready</li>
 * <li><strong>Nudged:</strong> {@link #onNudge(ComponentManager, Runner, DataStoreProvider, boolean, Object)}
 * is called when the component is scheduled to be enabled but has pending dependencies</li>
 * <li><strong>Disabled:</strong> {@link #onDisable(ComponentManager, Runner, DataStoreProvider)} is called
 * when the component is explicitly disabled or when one of its dependencies is disabled</li>
 * <li><strong>Detached:</strong> {@link #onDetached(ComponentManager, Runner, DataStoreProvider)} is called
 * when the component is removed from the manager</li>
 * </ol>
 *
 * <h3>Dependency System</h3>
 * <p>
 * Components can depend on other components and will only be enabled when all their dependencies are enabled.
 * Dependencies are specified when adding a component to the manager:
 * </p>
 *
 * <pre>
 * manager.addComponent(myComponent, dependency1, dependency2, ...);
 * </pre>
 * <p>
 * If any dependency is disabled, components that depend on it will automatically be disabled as well. When
 * dependencies become available again, dependent components will not automatically re-enable; they must be
 * explicitly enabled again.
 * </p>
 *
 * <h3>Best Practices</h3>
 * <ul>
 * <li>Implement primary component logic in {@link #onEnable} and fragment update methods</li>
 * <li>Use {@link #onAttached} only for initialization that must happen before enabling</li>
 * <li>The receiveXXX methods from fragments should be used only to store instance fields</li>
 * <li>The loadXXX methods should be used only for resource creation or loading, or to configure what is
 * directly passed to them. This will avoid concurrency issues, as some of these methods may run on different
 * threads than the rest of the component.</li>
 * <li>{@link #onDisable} should reset state to allow future re-enabling. ${@link #onDisable} is always called
 * automatically before an enabled component is detached</li>
 * <li>{@link #onDetached} must thoroughly clean up all resources to prevent memory leaks</li>
 * </ul>
 *
 * <p>
 * For a detailed explaination of the component lifecycle and methods execution order, see the
 * {@link Fragment} class.
 * </p>
 *
 * <p>
 * It is usually a good approach to ignore the {@link #onAttached} and {@link #onDetached} methods and
 * implement all the logic in {@link #onEnable} and {@link #onDisable}, so that when the component is
 * disabled, everything that was set up in {@link #onEnable} is cleaned up in {@link #onDisable}. Unless you
 * are dealing with heavy initialization logic, this will help keeping the code clean and concise.
 * </p>
 *
 *
 * @param <T>
 *            The type of argument that can be passed when enabling this component
 */
public interface Component<T> {
    /**
     * Called immediately when the component is attached to a {@link ComponentManager} and a {@link Runner}.
     * <p>
     * This method can be overridden to perform initialization tasks when the component is attached.
     * </p>
     *
     * @param mng
     *            the ComponentManager to which this component is attached
     * @param runner
     *            the Runner associated with this component
     * @param dataStore
     *            the DataStoreProvider for accessing shared data
     */

    default void onAttached(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {}

    /**
     * Called when the component is detached from the {@link ComponentManager}.
     * <p>
     * This method can be overridden to perform cleanup tasks when the component is detached.
     * </p>
     *
     * @param mng
     * @param runner
     * @param dataStore
     */
    default void onDetached(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {}

    /**
     * Returns an identifier for this component, multiple component can have the same identifier.
     * <p>
     * The default implementation returns the simple name of the class. This can be overridden to provide a
     * custom identifier if needed.
     * </p>
     *
     * @return an identifier for this component, default is the simple class name
     */
    default String getId() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the slot associated with this component.
     * <p>
     * A slot represents a mutually exclusive container for components. Only one component can be active in a
     * given slot at any time. When a component with a defined slot is enabled, any other active component
     * occupying the same slot will be automatically disabled.
     * </p>
     * If {@code getSlot()} returns {@code null}, the component is excluded from slot-based management and can
     * coexist with other components.
     *
     * @return the name of the slot this component belongs to, or {@code null} if it does not use a slot
     */
    default Object getSlot() {
        return null;
    }

    /**
     * Called when the component is enabled. This is the most important method of the component lifecycle,
     * where the component's main logic is implemented.
     * <p>
     * This method is called when the component is enabled, all its dependencies are satisfied, the fragments
     * are initialized, and the resources are loaded.
     * </p>
     *
     * @param mng
     *            - the ComponentManager to which this component is attached
     * @param runner
     *            - the Runner that executes the logic
     * @param dataStore
     *            - the DataStoreProvider for storing and retrieving data and caches
     * @param firstTime
     *            - whether this is the first time the component is being enabled
     * @param arg
     *            - an argument that can be passed when enabling this component, can be null
     */
    void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime, T arg);

    /**
     * Called when the component is disabled.
     * <p>
     * This should undo anything that was done in {@link #onEnable} and reset the component state to allow
     * future re-enabling.
     * </p>
     * <p>
     * This method is called when the component is explicitly disabled or when one of its dependencies is
     * disabled.
     * </p>
     * <p>
     * If ${@link #onDetach} is not implemented, this method should take care of cleaning up all resources to
     * prevent leaks.
     * </p>
     *
     * <p>
     * It is always called before an enabled component is detached from the {@link ComponentManager}.
     * </p>
     *
     * @param mng
     * @param runner
     * @param dataStore
     */
    void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore);

    /**
     * Called when the component is scheduled to be enabled but cannot be due to unsatisfied dependencies.
     * <p>
     * This method provides an opportunity for the component to take action when it's waiting on dependencies.
     * Unlike {@link #onEnable}, which is only called when all dependencies are satisfied, {@code onNudge} is
     * called when the component manager attempts to enable the component but one or more dependencies are not
     * yet enabled.
     * </p>
     * <p>
     * This can be used to implement "smart" components that can enable their own dependencies or adapt to
     * missing dependencies. For example, a GameMapComponent might depend on a GameTerrainComponent. When a
     * user tries to enable the GameMapComponent, it could use this method to detect and enable its terrain
     * dependency automatically.
     * </p>
     * <p>
     * The method receives the same parameters as {@link #onEnable}, allowing components to perform partial
     * initialization or setup temporary alternatives while waiting for dependencies.
     * </p>
     *
     * @param mng
     *            the ComponentManager that manages this component
     * @param runner
     *            the Runner that provides execution context
     * @param dataStore
     *            the DataStoreProvider for accessing persistent data
     * @param firstTime
     *            whether this is the first attempt to enable this component
     * @param arg
     *            an optional argument passed when enabling this component
     */
    default void onNudge(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime, T arg) {}
}
