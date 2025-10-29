package org.ngengine.components.fragments;

import org.ngengine.components.ComponentManager;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

public interface SpatialRenderFragment  extends Fragment {
    void updateSpatialRender(ComponentManager mng, RenderManager rm, ViewPort vp, Spatial sp);
}
