package org.ngengine.components.fragments;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 * A fragment that provides access to a ViewPort.
 */
public interface ViewPortFragment  extends Fragment{
    
    public default Node getRootNode(ViewPort vp) {
        return (Node) vp.getScenes().get(0);
    }

    /**
     * Receive a ViewPort instance as soon as it is available.
     * This can be stored and used later in the component logic.
     * @param viewPort the ViewPort instance
     */
    public default void receiveViewPort(ViewPort viewPort){

    }

    /**
     * Update the ViewPort with the given time per frame (tpf).
     * This method is called every frame to allow the component to update its logic.
     * @param viewPort the ViewPort instance
     * @param tpf time per frame
     */
    public void updateViewPort(ViewPort viewPort,float tpf);
}
