package org.ngengine.world2d.tiled.components.fragments;

import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.Fragment;
import org.ngengine.world2d.PovRenderer;

import org.ngengine.world2d.tiled.components.TiledGuiUpdater.GuiFragmentContext;

public interface TiledGuiFragment extends Fragment{
    public void rebuildGuiFragment(
        ComponentManager mng,
        PovRenderer renderer,       
        GuiFragmentContext data);
    
    public void renderGuiFragmentData(
        ComponentManager mng,
        PovRenderer renderer,       
        GuiFragmentContext data
      
    );

}
