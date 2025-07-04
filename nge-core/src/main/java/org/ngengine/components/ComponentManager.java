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

import java.util.List;
import org.ngengine.components.fragments.Fragment;

/**
 * The ComponentManager is responsible for managing the lifecycle of components within the application.
 * <p>
 * This interface provides methods to add, remove, enable, disable, and retrieve components. It also handles
 * component dependencies and slots (for mutually exclusive components).
 * <p>
 * <h3>Key Concepts:</h3>
 * <ul>
 * <li><strong>Components</strong> - Reusable pieces of functionality</li>
 * <li><strong>Dependencies</strong> - Components a component depends on to function</li>
 * <li><strong>Slots</strong> - Groups of mutually exclusive components (only one active at a time)</li>
 * </ul>
 * See the {@link Component} and {@link Fragment} classes for more details on component lifecycle and
 * behavior.
 * </p>
 *
 */
public interface ComponentManager {
    /**
     * Retrieves a component by its type.
     *
     * @param <T>
     *            The component type
     * @param type
     *            The class of the component to retrieve
     * @return The component of the specified type, or null if not found
     */
    <T extends Component> T getComponent(Class<T> type);

    /**
     * Retrieves a component by its ID.
     *
     * @param id
     *            The ID of the component to retrieve
     * @return The component with the specified ID, or null if not found
     */
    Component getComponentById(String id);

    /**
     * Retrieves all components assigned to a specific slot.
     *
     * @param slot
     *            The slot to get components from
     * @return A list of components in the specified slot
     */
    List<Component> getComponentBySlot(Object slot);

    /**
     * Gets the currently enabled component in a slot.
     * <p>
     * Since slots are designed for mutually exclusive components, there should be only one enabled component
     * per slot at any given time.
     *
     * @param slot
     *            The slot to check
     * @return The currently enabled component in the slot, or null if none is enabled
     */
    default Component getCurrentComponentInSlot(Object slot) {
        List<Component> components = getComponentBySlot(slot);
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            if (isComponentEnabled(component)) {
                return component;
            }
        }
        return null;
    }

    /**
     * Gets all registered components.
     *
     * @return A list of all components managed by this ComponentManager
     */
    List<Component> getComponent();

    /**
     * Adds a component to the manager with optional dependencies.
     * <p>
     * The component will be initialized but not enabled. Call {@link #enableComponent(Component)} to enable
     * it after adding.
     *
     * @param component
     *            The component to add
     * @param deps
     *            Zero or more dependencies for the component
     */
    void addComponent(Component component, Object... deps);

    /**
     * Removes a component from the manager.
     * <p>
     * If the component is enabled, it will be disabled before removal.
     *
     * @param component
     *            The component to remove
     */
    void removeComponent(Component component);

    /**
     * Enables a component without any specific arguments.
     * <p>
     * This is a convenience method equivalent to {@code enableComponent(component, null)}.
     *
     * @param component
     *            The component to enable
     */
    default void enableComponent(Component component) {
        enableComponent(component, null);
    }

    /**
     * Enables a component with the specified argument.
     * <p>
     * The component will only be enabled if all its dependencies are already enabled.
     *
     * @param <T>
     *            The type of argument to pass to the component
     * @param component
     *            The component to enable
     * @param arg
     *            The argument to pass to the component's onEnable method
     */
    <T> void enableComponent(Component component, T arg);

    /**
     * Disables a component.
     * <p>
     * This will also disable any components that depend on this component.
     *
     * @param component
     *            The component to disable
     */
    void disableComponent(Component component);

    /**
     * Updates the dependencies of a component.
     * <p>
     * This allows changing what components a particular component depends on at runtime.
     *
     * @param component
     *            The component to update dependencies for
     * @param deps
     *            The new dependencies for the component
     */
    void updateComponentDependencies(Component component, Object... deps);

    /**
     * Checks if a component is currently enabled.
     *
     * @param component
     *            The component to check
     * @return true if the component is enabled, false otherwise
     */
    boolean isComponentEnabled(Component component);

    // --------------------------------------------------------------------------
    // Convenience Methods
    // --------------------------------------------------------------------------

    /**
     * Enables a component by its ID.
     *
     * @param id
     *            The ID of the component to enable
     * @param arg
     *            The argument to pass to the component's onEnable method
     */
    default void enableComponent(String id, Object arg) {
        enableComponent(getComponentById(id), arg);
    }

    /**
     * Enables a component by its ID without arguments.
     *
     * @param id
     *            The ID of the component to enable
     */
    default void enableComponent(String id) {
        enableComponent(id, null);
    }

    /**
     * Disables a component by its ID.
     *
     * @param id
     *            The ID of the component to disable
     */
    default void disableComponent(String id) {
        disableComponent(getComponentById(id));
    }

    /**
     * Enables a component by its type with the specified argument.
     *
     * @param <T>
     *            The component type
     * @param type
     *            The class of the component to enable
     * @param arg
     *            The argument to pass to the component's onEnable method
     */
    default void enableComponent(Class<? extends Component> type, Object arg) {
        enableComponent(getComponent(type), arg);
    }

    /**
     * Enables a component by its type without arguments.
     *
     * @param <T>
     *            The component type
     * @param type
     *            The class of the component to enable
     */
    default void enableComponent(Class<? extends Component> type) {
        enableComponent(type, null);
    }

    /**
     * Disables a component by its type.
     *
     * @param <T>
     *            The component type
     * @param type
     *            The class of the component to disable
     */
    default void disableComponent(Class<? extends Component> type) {
        disableComponent(getComponent(type));
    }

    /**
     * Adds and immediately enables a component.
     *
     * @param component
     *            The component to add and enable
     * @param deps
     *            Zero or more dependencies for the component
     */
    default void addAndEnableComponent(Component component, Object... deps) {
        addComponent(component, deps);
        enableComponent(component);
    }

    /**
     * Adds and immediately enables a component with the specified argument.
     *
     * @param component
     *            The component to add and enable
     * @param arg
     *            The argument to pass to the component's onEnable method
     * @param deps
     *            Zero or more dependencies for the component
     */
    default void addAndEnableComponent(Component component, Object arg, Object... deps) {
        addComponent(component, deps);
        enableComponent(component, arg);
    }

    default Component resolveDependency(Object d) {
        if (d instanceof Component) {
            return (Component) d;
        } else if (d instanceof String) {
            return getComponentById((String) d);
        } else if (d instanceof Class<?>) {
            return getComponent((Class<? extends Component>) d);
        }
        return null;
    }
}
