package org.ngengine.components.fragments;

import org.ngengine.components.ComponentManager;

import com.jme3.scene.Spatial;

public interface SpatialLogicFragment  extends Fragment {
    void updateSpatialLogic(ComponentManager mng, float tpf, Spatial sp);

}
