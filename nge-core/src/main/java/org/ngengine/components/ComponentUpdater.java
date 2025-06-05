package org.ngengine.components;

/**
 * An interface to update components in the component manager.
 */
public interface ComponentUpdater {
    /**
     * Check if the component can be updated by this updater. This is used to determine if the component is
     * compatible with this updater and can be updated without issues. If the component is not compatible, it
     * will not be updated and the component manager will call the next available updater in the chain.
     * 
     * @param componentManager
     *            the component manager
     * @param component
     *            the component to check
     * @return true if the component can be updated, false otherwise
     */
    public boolean canUpdate(ComponentManager componentManager, Component component);

    /**
     * Update the component in the given component manager. This method is called every frame to update the
     * component's state.
     *
     * @param componentManager
     *            the component manager
     * @param component
     *            the component to update
     * @param tpf
     *            time per frame, used for time-based updates
     */

    public void update(ComponentManager componentManager, Component component, float tpf);

    /**
     * Call the render method for the component in the given component manager. This method is called during
     * the render phase to perform low-level rendering logic for the component.
     *
     * @param componentManager
     *            the component manager
     * @param component
     *            the component to render
     */
    public void render(ComponentManager componentManager, Component component);
}
