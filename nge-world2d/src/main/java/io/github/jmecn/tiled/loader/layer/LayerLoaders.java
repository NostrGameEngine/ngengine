package io.github.jmecn.tiled.loader.layer;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.loader.LayerLoader;
import static io.github.jmecn.tiled.TiledConst.*;

import java.util.logging.Logger;

/**
 * @author yanmaoyuan
 */
public class LayerLoaders {

    private final static Logger logger = Logger.getLogger(LayerLoaders.class.getName());

    private final AssetManager assetManager;

    private final AssetKey<?> assetKey;

    private final TiledMap map;

    public LayerLoaders(AssetManager assetManager, AssetKey<?> assetKey, TiledMap map) {
        this.assetManager = assetManager;
        this.assetKey = assetKey;
        this.map = map;
    }

    public LayerLoader create(String layerType) {
        switch (layerType) {
            case LAYER:
                return new TileLayerLoader(assetManager, assetKey, map);
            case OBJECTGROUP:
                return new ObjectLayerLoader(assetManager, assetKey, map);
            case IMAGELAYER:
                return new ImageLayerLoader(assetManager, assetKey, map);
            case GROUP:
                return new GroupLayerLoader(assetManager, assetKey, map);
            default:
                if (!TILESET.equals(layerType) && !PROPERTIES.equals(layerType) && !TEXT_EMPTY.equals(layerType)) {
                    logger.warning("Unsupported layer type: " + layerType);
                }
                return null;
        }
    }

}
