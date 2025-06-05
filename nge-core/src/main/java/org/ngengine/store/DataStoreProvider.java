package org.ngengine.store;

import com.jme3.asset.AssetManager;

/**
 * Provides data stores bound to a specific application name and asset manager.
 */
public class DataStoreProvider {
    private final String appName;
    private final AssetManager assetManager;
    public DataStoreProvider(String appName, AssetManager assetManager) {
        this.appName = appName;
        this.assetManager = assetManager;
    }

    public DataStore getDataStore(String storeName){
        return new DataStore(appName, assetManager, storeName, false);
    }

    public DataStore getCacheStore(String storeName) {
        return new DataStore( appName, assetManager, storeName, true);
    }
  
}
