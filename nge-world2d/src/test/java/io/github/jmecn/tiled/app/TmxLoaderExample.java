package io.github.jmecn.tiled.app;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.system.AppSettings;
import io.github.jmecn.tiled.TmxLoader;
import io.github.jmecn.tiled.core.TiledMap;

/**
 * Test loading tmx assets with TmxLoader.
 * @author yanmaoyuan
 *
 */
public class TmxLoaderExample extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        // register it
        TmxLoader.registerLoader(assetManager);

        // load tmx with it
        TiledMap map = (TiledMap) assetManager.loadAsset("tmx/Desert/desert.tmx");

        // render it with TiledMapAppState
        TiledMapAppState tiledMapState = new TiledMapAppState();
        stateManager.attach(tiledMapState);
        tiledMapState.initialize(stateManager, this);

        tiledMapState.setMap(map);
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setSamples(4);
        settings.setGammaCorrection(false);

        TmxLoaderExample app = new TmxLoaderExample();
        app.setSettings(settings);
        app.start();
    }

}
