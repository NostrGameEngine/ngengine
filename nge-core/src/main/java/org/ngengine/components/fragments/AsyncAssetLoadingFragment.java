package org.ngengine.components.fragments;

import com.jme3.asset.AssetManager;

public interface AsyncAssetLoadingFragment extends Fragment {
    
    public void loadAssetsAsync(
        AssetManager assetManager
    );

}
