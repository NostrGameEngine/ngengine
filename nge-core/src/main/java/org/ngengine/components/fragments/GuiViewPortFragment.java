package org.ngengine.components.fragments;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

public interface GuiViewPortFragment extends Fragment {

    public default Node getGuiNode(ViewPort vp){
        return (Node) vp.getScenes().get(0);
    }

    public void receiveGuiViewPort(ViewPort vp);

    public void updateGuiViewPort(ViewPort vp, float tpf);

    
}
