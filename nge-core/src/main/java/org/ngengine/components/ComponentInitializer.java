package org.ngengine.components;


public interface ComponentInitializer {

    public void initialize(ComponentManager mng, Component fragment, Runnable markReady);

    public void cleanup(ComponentManager mng, Component fragment);

    public boolean canInitialize(ComponentManager mng, Component fragment);
}
