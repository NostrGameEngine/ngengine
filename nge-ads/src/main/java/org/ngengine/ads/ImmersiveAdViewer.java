package org.ngengine.ads;


public interface ImmersiveAdViewer {
    default boolean isVisible(ImmersiveAdSpace space){
        return isNear(space);
    }

    boolean isNear(ImmersiveAdSpace space);

    void showInfo(ImmersiveAdSpace space, String description,String callToAction,String link);
    
    void beginUpdate();
    void endUpdate();
}
