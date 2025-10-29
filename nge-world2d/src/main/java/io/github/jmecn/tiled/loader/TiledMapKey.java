package io.github.jmecn.tiled.loader;

import com.jme3.asset.AssetKey;
import io.github.jmecn.tiled.core.TiledMap;

/**
 * When loading separate tileset or template, the loaded TiledMap object can be passed to the constructor.
 * The {@link io.github.jmecn.tiled.loader.TilesetLoader} and {@link io.github.jmecn.tiled.loader.layer.ObjectLayerLoader}
 * can use this key to get the TiledMap object.
 * @author yanmaoyuan
 */
public class TiledMapKey<T> extends AssetKey<T> {

    private final TiledMap tiledMap;

    public TiledMapKey(String name, TiledMap tiledMap) {
        super(name);
        this.tiledMap = tiledMap;
    }

    public TiledMap getTiledMap() {
        return tiledMap;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
