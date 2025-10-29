package io.github.jmecn.tiled;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import io.github.jmecn.tiled.loader.MapLoader;
import io.github.jmecn.tiled.loader.TilesetLoader;
import io.github.jmecn.tiled.loader.layer.ObjectLayerLoader;

import java.io.IOException;

/**
 * Tiled map loader.
 *
 * @author yanmaoyuan
 */
public class TmxLoader implements AssetLoader {

    public static void registerLoader(AssetManager assetManager) {
        assetManager.registerLoader(TmxLoader.class, TiledConst.TMX_EXTENSION, TiledConst.TSX_EXTENSION, TiledConst.TX_EXTENSION);
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        AssetManager assetManager = assetInfo.getManager();

        String extension = key.getExtension();

        switch (extension) {
            case TiledConst.TMX_EXTENSION:
                MapLoader mapLoader = new MapLoader(assetManager, key);
                return mapLoader.load(assetInfo.openStream());
            case TiledConst.TSX_EXTENSION:
                TilesetLoader tilesetLoader = new TilesetLoader(assetManager, key);
                return tilesetLoader.load(assetInfo.openStream());
            case TiledConst.TX_EXTENSION:
                ObjectLayerLoader objectLayerLoader = new ObjectLayerLoader(assetManager, key, null);
                return objectLayerLoader.loadObjectTemplate(assetInfo.openStream());
            default:
                return null;
        }
    }

}
