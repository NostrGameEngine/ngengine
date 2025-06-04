package org.ngengine.components.updaters;

import java.util.HashMap;
import java.util.Map;

import org.ngengine.AsyncAssetManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentUpdater;
import org.ngengine.components.fragments.AppFragment;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.GuiViewPortFragment;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.RenderFragment;
import org.ngengine.components.fragments.ViewPortFragment;

import com.jme3.app.Application;

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
   

        // for (ComponentMount mount : componentMounts) {
        //     if (!mount.enabled) continue;
        //     if (mount.component instanceof GuiViewPortFragment) {
        //         GuiViewPortFragment appFragment = (GuiViewPortFragment) mount.component;
        //         appFragment.onGuiViewPortUpdate(getApplication().getGuiViewPort(), tpf);
        //     }
        //     if (mount.component instanceof ViewPortFragment) {
        //         ViewPortFragment appFragment = (ViewPortFragment) mount.component;
        //         appFragment.onViewPortUpdate(getApplication().getViewPort(), tpf);
        //     }
        // }

        // for (ComponentMount mount : componentMounts) {
        //     if (!mount.enabled) continue;
        //     if (mount.component instanceof AppFragment) {
        //         AppFragment appFragment = (AppFragment) mount.component;
        //         appFragment.onApplicationLogicUpdate(getApplication(), tpf);
        //     }
        // }
    } 

    @Override
    public void render(ComponentManager fragmentManager, Component component) {
     
    }
}
