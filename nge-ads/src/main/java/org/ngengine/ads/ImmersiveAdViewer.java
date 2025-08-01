package org.ngengine.ads;


public interface ImmersiveAdViewer {
    default boolean isVisible(ImmersiveAdSpace space){
        return isNear(space);
    }

    boolean isNear(ImmersiveAdSpace space);
}
