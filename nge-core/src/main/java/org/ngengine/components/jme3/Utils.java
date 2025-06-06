package org.ngengine.components.jme3;

import com.jme3.asset.AssetManager;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;

public class Utils {
    public static FilterPostProcessor getFilterPostProcessor(AppSettings settings, AssetManager am, ViewPort vp) {
        for (SceneProcessor sp : vp.getProcessors()) {
            if (sp instanceof FilterPostProcessor) {
                return (FilterPostProcessor) sp;
            }
        }
        FilterPostProcessor fpp = new FilterPostProcessor(am);
        if (settings.getSamples() > 1) {
            fpp.setNumSamples(settings.getSamples());
        }
        vp.addProcessor(fpp);
        return fpp;
    }
}
