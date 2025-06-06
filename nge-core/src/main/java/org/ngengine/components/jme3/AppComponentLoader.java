package org.ngengine.components.jme3;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.AsyncAssetManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentLoader;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.ViewPortFragment;

import com.jme3.app.Application;
import com.jme3.post.FilterPostProcessor;

/**
 * load components by connecting them to JME3 application resources.
 */
public class AppComponentLoader implements ComponentLoader {
    private static final Logger log = Logger.getLogger(AppComponentInitializer.class.getName());

    private final Application app;
    private final AsyncAssetManager assetManager;

    public AppComponentLoader(Application app){
        this.app = app;
        this.assetManager = AsyncAssetManager.of(app.getAssetManager(), app);
    }


    @Override
    public int load(ComponentManager mng, Component fragment, Runnable markReady) {
       
        int i = 0;
                
        if(fragment instanceof AssetLoadingFragment){
            i++;
            AssetLoadingFragment f = (AssetLoadingFragment) fragment;
            f.loadAssets(assetManager);
            markReady.run();
        }
        if((fragment instanceof AsyncAssetLoadingFragment)){
            i++;
            AsyncAssetLoadingFragment f = (AsyncAssetLoadingFragment) fragment;
            assetManager.runInLoaderThread((am) -> {
                f.loadAssetsAsync(assetManager);
                return null;
            }, (d, err) -> {
                if(err!=null){
                    log.log(Level.SEVERE, "Error during async asset loading in fragment: " + f.getClass().getSimpleName(), err);                }
                markReady.run();
            });            
        }      

        if (fragment instanceof ViewPortFragment) {
            i++;
            ViewPortFragment f = (ViewPortFragment) fragment;
            FilterPostProcessor fpp = Utils.getFilterPostProcessor(app.getContext().getSettings(),
                    assetManager, app.getViewPort());
            f.loadViewPortFilterPostprocessor(assetManager, fpp);
            markReady.run();

        }

        
        return i;
    }

   

    @Override
    public boolean canLoad(ComponentManager mng, Component fragment) {
        return 
            fragment instanceof AsyncAssetLoadingFragment || fragment instanceof AssetLoadingFragment ||
            fragment instanceof ViewPortFragment ;
    }


    @Override
    public void unload(ComponentManager mng, Component fragment) {
       
    }

   

    
}
