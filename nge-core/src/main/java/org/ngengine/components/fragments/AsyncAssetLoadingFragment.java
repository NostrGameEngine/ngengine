package org.ngengine.components.fragments;

import org.ngengine.AsyncAssetManager;

import com.jme3.asset.AssetManager;

/**
 * A fragment that can load assets asynchronously using the provided AssetManager. This is useful for
 * components that need to load assets without blocking the main thread.
 */
public interface AsyncAssetLoadingFragment extends Fragment {
    
    /**
     * This method is called from the asset loader thread and can be used to load and transform assets
     * asynchronously, without blocking the main thread.
     * 
     * A component implementing this fragment will stay in "pending" state until the
     * {@link #loadAssetsAsync(AsyncAssetManager)} method completes its execution and they are subsequently
     * enabled by the component manager.
     *
     * <p>
     * <strong>Thread Safety Warning:</strong> This method executes on a background thread. You must not
     * modify the scene graph or access other thread-unsafe objects from this method. Instead, as a rule of
     * thumb, you should only load assets here and pass them to the
     * {@link org.ngengine.components.Component#onEnable(org.ngengine.components.ComponentManager, org.ngengine.components.Runner, org.ngengine.components.DataStoreProvider, boolean, Object)}
     * method using class fields.
     * </p>
     * 
     * @param assetManager
     *            the AsyncAssetManager instance to use for loading assets
     */
    public void loadAssetsAsync(
            AsyncAssetManager assetManager
    );

}
