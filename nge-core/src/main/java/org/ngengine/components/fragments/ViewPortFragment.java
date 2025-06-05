package org.ngengine.components.fragments;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 * A fragment that provides access to a ViewPort. A ViewPort is an object that contains the camera and scene
 * to be rendered.
 */
public interface ViewPortFragment  extends Fragment{
    
    /**
     * Get the first scene node of the ViewPort. This is usually the root node of the scene graph.
     * 
     * @param vp
     *            the ViewPort instance
     * @return the root node of the scene graph
     * @see ViewPort#getScenes()
     */
    public default Node getRootNode(ViewPort vp) {
        return (Node) vp.getScenes().get(0);
    }

    /**
     * Receive a ViewPort instance as soon as it is available. The reference to the ViewPort can be stored and
     * used later in the component logic.
     * 
     * @param viewPort
     *            the ViewPort instance
     */
    public default void receiveViewPort(ViewPort viewPort){

    }

    /**
     * Update the ViewPort with the given time per frame (tpf). This method is called every frame and can be
     * used to get the ViewPort or scene info, the camera, tweak the camera or even update the scene graph.
     * 
     * This method is called before {@link LogicFragment#update(float)}.
     * 
     * @param viewPort
     *            the ViewPort instance
     * @param tpf
     *            time per frame
     */
    public void updateViewPort(ViewPort viewPort,float tpf);
}
