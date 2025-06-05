package org.ngengine.components;

/**
 * An interface to initializing and cleaning up components in the component manager. This is used to perform
 * any necessary setup or teardown for a component when it is added or removed.
 */

public interface ComponentInitializer {

    /**
     * Initialize the component in the given component manager. This method is called when the component is
     * added to the component manager.
     *
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to initialize
     * @param markReady
     *            a runnable that should be called when the initialization is complete
     */
    public void initialize(ComponentManager mng, Component fragment, Runnable markReady);

    /**
     * Cleanup the component in the given component manager. This method is called when the component is
     * removed from the component manager.
     *
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to cleanup
     */
    public void cleanup(ComponentManager mng, Component fragment);

    /**
     * Check if the component can be initialized by this initializer. This is used to determine if the
     * component is compatible with this initializer and can be initialized without issues. If the component
     * is not compatible, it will not be initialized and the component manager will call the next available
     * initializer in the chain.
     * 
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to check
     * @return true if the component can be initialized, false otherwise
     */
    public boolean canInitialize(ComponentManager mng, Component fragment);
}
