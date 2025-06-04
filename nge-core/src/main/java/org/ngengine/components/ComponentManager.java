package org.ngengine.components;

import java.util.List;

 
public interface ComponentManager {

    public <T extends Component> T getComponentByType(Class<T> type);

    public Component getComponentById(String id);

    public List<Component> getComponentBySlot(Object slot);

    public default Component getCurrentComponentInSlot(Object slot){
        List<Component> fragments = getComponentBySlot(slot);
        for (int i = 0; i < fragments.size(); i++) {
            Component fragment = fragments.get(i);
            if (isComponentEnabled(fragment)) {
                return fragment;
            }
        }
        return null;
    }

    public List<Component> getComponent();

    public void addComponent(Component fragment, Object... deps);

    public void removeComponent(Component fragment);

    
    public default void enableComponent(Component fragment) {
        enableComponent(fragment, null);
    }

    public <T> void enableComponent(Component fragment, T arg);

 

    public default void enableComponent(String id, Object arg) {
        enableComponent(getComponentById(id), arg);
    }

    public default void enableFragment(String id) {
        enableComponent(id, null);
    }

    public default void disableComponent(String id) {
        disableComponent(getComponentById(id));
    }

    public default void enableComponent(Class<? extends Component> type, Object arg) {
        enableComponent(getComponentByType(type), arg);
    }
    public default void enableComponent(Class<? extends Component> type) {
        enableComponent(type, null);
    }

    public default void disableComponent(Class<? extends Component> type) {
        disableComponent(getComponentByType(type));
    }


    public void disableComponent(Component fragment);

    public boolean isComponentEnabled(Component fragment);
    
    
    public default void addAndEnableComponent(Component fragment, Object... deps) {
        addComponent(fragment, deps);
        enableComponent(fragment);
    }

    public default void addAndEnableComponent(Component fragment, Object arg, Object... deps) {
        addComponent(fragment, deps);
        enableComponent(fragment, arg);
    }
    
}

