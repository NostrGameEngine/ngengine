package org.ngengine;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;

public class ComponentRef {
    private final ComponentManager mng;
    private final Component component;
    ComponentRef(ComponentManager mng, Component component){
        this.mng = mng;
        this.component = component;
    }
    public ComponentRef enable(){
        this.mng.enableComponent(get());
        return this;
    }

    public ComponentRef disable(){
        this.mng.disableComponent(get());
        return this;
    }
    public Component get(){
        return this.component;
    }
}
