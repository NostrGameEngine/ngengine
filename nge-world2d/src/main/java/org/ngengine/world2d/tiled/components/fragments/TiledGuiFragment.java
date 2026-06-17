package org.ngengine.world2d.tiled.components.fragments;

import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.Fragment;
import org.ngengine.world2d.PovRenderer;

import org.ngengine.world2d.tiled.components.TiledGuiUpdater.GuiFragmentContext;

/**
 * Fragment that renders GUI content anchored to a tiled world entity.
 */
public interface TiledGuiFragment extends Fragment{
    /**
     * Builds the persistent GUI controls for this fragment and POV.
     * <p>
     * This method is called when the fragment first becomes visible for a
     * renderer. Expensive control creation belongs here, not in
     * {@link #renderGuiFragmentData(ComponentManager, PovRenderer, GuiFragmentContext)}.
     * </p>
     *
     * @param mng the component manager running the fragment
     * @param renderer the point of view that will display the GUI
     * @param data per-POV GUI state and root content panel
     */
    public void rebuildGuiFragment(
        ComponentManager mng,
        PovRenderer renderer,       
        GuiFragmentContext data);

    /**
     * Updates this fragment's GUI state for a render frame.
     * <p>
     * Implementations should keep this method cheap and update only values that
     * actually changed, because it is called every frame while the fragment is
     * active.
     * </p>
     *
     * @param mng the component manager running the fragment
     * @param renderer the point of view being rendered
     * @param data per-POV GUI state and root content panel
     */
    public void renderGuiFragmentData(
        ComponentManager mng,
        PovRenderer renderer,       
        GuiFragmentContext data
      
    );

}
