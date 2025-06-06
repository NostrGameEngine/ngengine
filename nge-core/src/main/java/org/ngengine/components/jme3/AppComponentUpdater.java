package org.ngengine.components.jme3;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentUpdater;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.RenderFragment;
import com.jme3.app.Application;

/**
 * Updates components using JME3 application resources.
 */
public class AppComponentUpdater  implements ComponentUpdater {
    private final Application app;

    public AppComponentUpdater(Application app){
        this.app = app;
    }

    @Override
    public boolean canUpdate(ComponentManager fragmentManager,Component component) {
        return component instanceof LogicFragment||               component instanceof RenderFragment;
 
    }

    @Override
    public void update(ComponentManager fragmentManager, Component component, float tpf) {
        if(component instanceof LogicFragment) {
            LogicFragment appFragment = (LogicFragment) component;
            appFragment.updateAppLogic(tpf);
        }  
    } 

    @Override
    public void render(ComponentManager fragmentManager, Component component) {
        if (component instanceof RenderFragment) {
            RenderFragment renderFragment = (RenderFragment) component;
            renderFragment.updateRender(app.getRenderManager());
        }
    }
}
