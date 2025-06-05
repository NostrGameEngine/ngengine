package org.ngengine.components.fragments;


/**
 * A fragment that with a single update method that is called every frame during the application logic update
 * loop.
 * 
 * This can be used to perform game logic updates including updating the scene graph, processing inputs,
 * attaching and using controls or other components, etc.
 */
public interface LogicFragment extends Fragment {
    /**
     * Called by the application logic update loop.
     * This method is called every frame before the render phase.
     * @param tpf time per frame
     */
    public void updateAppLogic(float tpf);

}
