package org.ngengine.components.fragments;

import com.jme3.app.Application;

public interface LogicFragment extends Fragment {
    /**
     * Called by the application logic update loop.
     * This method is called every frame before the render phase.
     * @param tpf time per frame
     */
    public void onLogicUpdate( float tpf);

}
