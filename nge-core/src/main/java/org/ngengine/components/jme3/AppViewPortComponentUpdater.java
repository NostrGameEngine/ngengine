package org.ngengine.components.jme3;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentUpdater;
import org.ngengine.components.fragments.GuiViewPortFragment;
import org.ngengine.components.fragments.ViewPortFragment;

import com.jme3.app.Application;

/**
 * Updates viewport components using JME3 application resources.
 */
public class AppViewPortComponentUpdater implements ComponentUpdater {
    
    private final Application app;
    

    public AppViewPortComponentUpdater(Application app){
        this.app = app;
    }

    @Override
    public boolean canUpdate(ComponentManager fragmentManager,Component component) {
        return component instanceof GuiViewPortFragment || 
               component instanceof ViewPortFragment ;
    }

    @Override
    public void update(ComponentManager fragmentManager, Component component, float tpf) {
        if(component instanceof GuiViewPortFragment) {
            GuiViewPortFragment guiFragment = (GuiViewPortFragment) component;
            guiFragment.updateGuiViewPort(app.getGuiViewPort(), tpf);
        } 
         if(component instanceof ViewPortFragment) {
            ViewPortFragment viewFragment = (ViewPortFragment) component;
            viewFragment.updateViewPort(app.getViewPort(), tpf);
        } 
   

    } 

    @Override
    public void render(ComponentManager fragmentManager, Component component) {
     
    }
}
