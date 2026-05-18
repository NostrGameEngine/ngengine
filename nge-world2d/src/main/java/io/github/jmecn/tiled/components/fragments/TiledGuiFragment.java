package io.github.jmecn.tiled.components.fragments;

import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.Fragment;
import org.ngengine.world2d.PovRenderer;

import io.github.jmecn.tiled.components.TiledGuiUpdater.GuiFragmentContext;

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
