package org.ngengine.components.jme3;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.RenderFragment;
import org.ngengine.components.fragments.SpatialLogicFragment;
import org.ngengine.components.fragments.SpatialRenderFragment;
import org.ngengine.components.runners.ComponentUpdater;

import com.jme3.app.Application;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.control.Control;

public class SpatialComponentUpdater implements ComponentUpdater {
    private final ComponentManagerControl control;

    public SpatialComponentUpdater(ComponentManagerControl control) {
        this.control = control;
    }

    @Override
    public boolean canUpdate(ComponentManager fragmentManager, Component component) {
        return component instanceof SpatialLogicFragment || component instanceof SpatialRenderFragment;
    }


    @Override
    public void update(ComponentManager mng, Component component, float tpf) {
        if (component instanceof SpatialLogicFragment) {
            SpatialLogicFragment appFragment = (SpatialLogicFragment) component;
            appFragment.updateSpatialLogic(mng, tpf, control.getSpatial());
        }
    }

    @Override
    public void render(ComponentManager mng, Component component) {
        if (component instanceof SpatialRenderFragment) {
            SpatialRenderFragment renderFragment = (SpatialRenderFragment) component;
            renderFragment.updateSpatialRender(mng, (RenderManager)mng.getGlobalInstance(RenderManager.class),control.getLastViewPort(), control.getSpatial());
        }
    }
}
