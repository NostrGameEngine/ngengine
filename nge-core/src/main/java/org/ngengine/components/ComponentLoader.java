package org.ngengine.components;

/**
 * An interface to load resources and configurations for components in a component manager.
 */

public interface ComponentLoader {

    /**
    * Load the component in the given component manager. This method is called after the component is
    * initialized
     *
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to load
     * @param markReady
     *            a runnable that should be called when the loading is complete
     * @return how many times the markReady callback is expected to be called to mark the complete
     *         loading.
     */
    public int load(ComponentManager mng, Component fragment, Runnable markReady);

    /**
     * Frees up resources and configurations for the component in the given component manager.
     *
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to cleanup
     */
    public void unload(ComponentManager mng, Component fragment);

    /**
     * Check if the component can be loaded by this loader. This is used to determine if the component is
     * compatible with this loader and can be initialized without issues. If the component is not compatible,
     *  the next available loader in the chain.
     * 
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to check
     * @return true if the component can be initialized, false otherwise
     */
    public boolean canLoad(ComponentManager mng, Component fragment);
}
