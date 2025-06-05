package org.ngengine.components.fragments;

import com.jme3.renderer.RenderManager;

/**
 * A fragment that can receive a RenderManager instance and is called every frame during the render phase. It
 * is used to perform low-level rendering logic, not to update the scene graph, for that use
 * {@link LogicFragment} or {@link ViewPortFragment}.
 */
public interface RenderFragment extends Fragment{

    /**
     * Receive a RenderManager instance as soon as it is available. The reference to the RenderManager can be
     * stored and used later in the component logic.
     * 
     * @param renderer
     *            the RenderManager instance
     */
    public default void receiveRenderManager(RenderManager renderer) {

    }


    /**
     * Called every frame during the render phase.
     * @param renderer the RenderManager instance
     * @param tpf time per frame
     */
    public void updateRender(RenderManager renderer);
}
