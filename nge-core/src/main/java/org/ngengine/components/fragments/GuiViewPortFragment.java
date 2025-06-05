package org.ngengine.components.fragments;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 * A fragment that provides access to a GUI ViewPort. A GUI ViewPort is an object that contains the camera and
 * scene to be rendered for the GUI.
 */
public interface GuiViewPortFragment extends Fragment {

    /**
     * Get the first scene node of the GUI ViewPort. This is usually the root node of the GUI scene graph.
     * 
     * @param vp
     *            the ViewPort instance
     * @return the root node of the GUI scene graph
     */
    public default Node getGuiNode(ViewPort vp){
        return (Node) vp.getScenes().get(0);
    }

    /**
     * Receive a GUI ViewPort instance as soon as it is available. The reference to the GUI ViewPort can be
     * stored and
     * 
     * @param vp
     *            the ViewPort instance
     */
    public default void receiveGuiViewPort(ViewPort vp) {

    }

    public void updateGuiViewPort(ViewPort vp, float tpf);

    
}
