package org.ngengine.components;

import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

/**
 * A component that is never enabled and does nothing.
 */
public class StallingComponent implements Component<Object> {

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime,
            Object arg) {
        
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
     
    }
 
}
