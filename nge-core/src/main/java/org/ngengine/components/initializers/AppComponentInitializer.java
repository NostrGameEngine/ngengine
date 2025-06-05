package org.ngengine.components.initializers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.AsyncAssetManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentInitializer;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AppFragment;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.GuiViewPortFragment;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.RenderFragment;
import org.ngengine.components.fragments.ViewPortFragment;

import com.jme3.app.Application;

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
    public void initialize(ComponentManager mng, Component fragment, Runnable markReady) {
        if(fragment instanceof AppFragment){
            AppFragment f = (AppFragment) fragment;
            f.receiveApplication(app);
        } 
        
        if(fragment instanceof ViewPortFragment){
            ViewPortFragment f = (ViewPortFragment) fragment;
            f.receiveViewPort(app.getViewPort());
        } 
        
        if(fragment instanceof GuiViewPortFragment){
            GuiViewPortFragment f = (GuiViewPortFragment) fragment;
            f.receiveGuiViewPort(app.getGuiViewPort());
        }

        if(fragment instanceof InputHandlerFragment){
            InputHandlerFragment f = (InputHandlerFragment) fragment;
            f.receiveInputManager(app.getInputManager());
            InputHandlerFragment.Wrapper wrapper = new InputHandlerFragment.Wrapper(mng, f);
            inputHandlerWrappers.put(f, wrapper);
            app.getInputManager().addRawInputListener(wrapper);
        }

        if(fragment instanceof RenderFragment){
            RenderFragment f = (RenderFragment) fragment;
            f.receiveRenderManager(app.getRenderManager());
        }
        
        if(fragment instanceof AssetLoadingFragment){
            AssetLoadingFragment f = (AssetLoadingFragment) fragment;
            f.loadAssets(assetManager);
        }
        if((fragment instanceof AsyncAssetLoadingFragment)){
            AsyncAssetLoadingFragment f = (AsyncAssetLoadingFragment) fragment;
            assetManager.runInLoaderThread((am) -> {
                f.loadAssetsAsync(assetManager);
                return null;
            }, (d, err) -> {
                if(err!=null){
                    log.log(Level.SEVERE, "Error during async asset loading in fragment: " + f.getClass().getSimpleName(), err);                }
                markReady.run();
            });
        } else {
            markReady.run();
        }      
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
            fragment instanceof AsyncAssetLoadingFragment ||
            fragment instanceof ViewPortFragment ||
            fragment instanceof GuiViewPortFragment ||
            fragment instanceof InputHandlerFragment ||
            fragment instanceof RenderFragment || fragment instanceof AssetLoadingFragment;
    }

   

    
}
