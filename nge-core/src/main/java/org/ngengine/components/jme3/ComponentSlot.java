package org.ngengine.components.jme3;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ngengine.components.Component;

public class ComponentSlot {
    private final List<Component> components = new CopyOnWriteArrayList<>();
    private transient List<Component> componentsRO; 
    public List<Component> getComponents() {
        if (componentsRO == null) {
            componentsRO = Collections.unmodifiableList(components);
        }
        return componentsRO;
    }

    public void addComponent(Component component) {
        components.add(component);
    }

    public void removeComponent(Component component) {
        components.remove(component);
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }
}