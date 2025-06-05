package org.ngengine.components.fragments;

import com.jme3.asset.AssetManager;

/**
 * A fragment that loads assets using the provided AssetManager. This is like the
 * {@link AsyncAssetLoadingFragment} but it is called from the main thread.
 * 
 * It is easier to use than the {@link AsyncAssetLoadingFragment} for simple asset loading tasks but risks
 * blocking the main thread if the asset loading takes too long.
 */
public interface AssetLoadingFragment extends Fragment {

    /**
     * Load assets using the provided AssetManager.
     * 
     * The reference to the AssetManager can be stored and used later in the component logic.
     * 
     * @param assetManager
     *            the AssetManager instance
     */
    public void loadAssets(AssetManager assetManager);

}
