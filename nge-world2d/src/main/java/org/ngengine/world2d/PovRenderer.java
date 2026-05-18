package org.ngengine.world2d;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

public interface PovRenderer {
    public ViewPort getSceneViewPort();
    public ViewPort getGuiViewPort();



    public Node getGuiNode(int i);
    public Node getSceneNode(int i);
}
