package org.ngengine.components;

public interface ComponentUpdater {
    public boolean canUpdate(ComponentManager fragmentManager, Component component);
    public void update(ComponentManager fragmentManager, Component component, float tpf);

    public void render(ComponentManager fragmentManager, Component component) ;
}
