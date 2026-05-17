package org.ngengine.components.actions;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.ActionBasedFragment;
import org.ngengine.components.runners.ComponentInitializer;

public class ActionsComponentInitializer  implements ComponentInitializer {

    @Override
    public int initialize(ComponentManager mng, Component fragment, Runnable markReady) {
        if(fragment instanceof ActionBasedFragment) {
            ActionBasedFragment<?> f = (ActionBasedFragment<?>) fragment;
            f.loadActions(mng);
            markReady.run();
            return 1;
        }
        return 0;
    }

    @Override
    public void cleanup(ComponentManager mng, Component fragment) {
        if(fragment instanceof ActionBasedFragment) {
            ActionBasedFragment<?> f = (ActionBasedFragment<?>) fragment;
            f.unloadActions(mng);
        }
    }

    @Override
    public boolean canInitialize(ComponentManager mng, Component fragment) {
        return (
            fragment instanceof ActionBasedFragment
        );
    }
    
}
