package org.ngengine.components.jme3;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.ngengine.AsyncAssetManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentInitializer;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AppFragment;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.GuiViewPortFragment;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.RenderFragment;
import org.ngengine.components.fragments.ViewPortFragment;

import com.jme3.app.Application;
import com.jme3.post.FilterPostProcessor;

/**
 * Initializes components by connecting them to JME3 application resources.
 */
public class AppComponentInitializer implements ComponentInitializer {
    private static final Logger log = Logger.getLogger(AppComponentInitializer.class.getName());

    private final Application app;
    private final AsyncAssetManager assetManager;
    private final Map<InputHandlerFragment, InputHandlerFragment.Wrapper> inputHandlerWrappers = new HashMap<>();

    public AppComponentInitializer(Application app){
        this.app = app;
        this.assetManager = AsyncAssetManager.of(app.getAssetManager(), app);
    }


    @Override
    public int initialize(ComponentManager mng, Component fragment, Runnable markReady) {
        int i = 0;
        if(fragment instanceof AppFragment){
            i++;
            AppFragment f = (AppFragment) fragment;
            f.receiveApplication(app);
            markReady.run();

        } 
        
        if(fragment instanceof ViewPortFragment){
            i++;
            ViewPortFragment f = (ViewPortFragment) fragment;
            f.receiveViewPort(app.getViewPort());
            FilterPostProcessor fpp = Utils.getFilterPostProcessor(app.getContext().getSettings(),
                    assetManager, app.getViewPort());
            f.receiveViewPortFilterPostProcessor(fpp);
            markReady.run();
        } 
        
        if(fragment instanceof GuiViewPortFragment){
            i++;
            GuiViewPortFragment f = (GuiViewPortFragment) fragment;
            f.receiveGuiViewPort(app.getGuiViewPort());
            markReady.run();
        }

        if(fragment instanceof InputHandlerFragment){
            i++;
            InputHandlerFragment f = (InputHandlerFragment) fragment;
            f.receiveInputManager(app.getInputManager());
            InputHandlerFragment.Wrapper wrapper = new InputHandlerFragment.Wrapper(mng, f);
            inputHandlerWrappers.put(f, wrapper);
            app.getInputManager().addRawInputListener(wrapper);
            markReady.run();
        }

        if(fragment instanceof RenderFragment){
            i++;
            RenderFragment f = (RenderFragment) fragment;
            f.receiveRenderManager(app.getRenderManager());
            markReady.run();
        }

        
        return i;
     
    }

    @Override
    public void cleanup(ComponentManager mng, Component fragment) {
        if(fragment instanceof InputHandlerFragment){
            InputHandlerFragment f = (InputHandlerFragment) fragment;
            InputHandlerFragment.Wrapper wrapper = inputHandlerWrappers.remove(f);
            if(wrapper != null) {
                app.getInputManager().removeRawInputListener(wrapper);
            }
        }        
    }

    @Override
    public boolean canInitialize(ComponentManager mng, Component fragment) {
        return 
            fragment instanceof AppFragment||
            fragment instanceof ViewPortFragment ||
            fragment instanceof GuiViewPortFragment ||
            fragment instanceof InputHandlerFragment ||
            fragment instanceof RenderFragment || fragment instanceof AssetLoadingFragment;
    }

   

    
}
