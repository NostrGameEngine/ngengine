package org.ngengine.components;

import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

public interface Component<T> {
    public default void onAttached(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {}
    public default void onDetached(ComponentManager mng, Runner runner,
            DataStoreProvider dataStore) {

    }
    
    /**
     * Returns a unique identifier for this fragment.
     * <p>
     * The default implementation returns the simple name of the class. This can be overridden to provide a
     * custom identifier if needed.
     * <p>
     *
     * @return a unique identifier for this fragment, defaulting to the class name
     */
    public default String getId() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the slot associated with this fragment.
     * <p>
     * A slot represents a mutually exclusive container for fragments—only one fragment can be active in a
     * given slot at any time. When a fragment with a defined slot is enabled, any other active fragment
     * occupying the same slot will be automatically disabled.
     * <p>
     * If {@code getSlot()} returns {@code null}, the fragment is excluded from slot-based management and can
     * coexist with other fragments.
     *
     * @return the name of the slot this fragment belongs to, or {@code null} if it does not use a slot
     */
    public default Object getSlot() {
        return null;
    }
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime, T arg);
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore);

    public default void onNudge(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime, T arg) {
    }
}
