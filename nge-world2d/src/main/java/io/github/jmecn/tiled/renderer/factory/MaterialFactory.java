package io.github.jmecn.tiled.renderer.factory;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.core.TiledImage;
import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.Tileset;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public interface MaterialFactory {
    Material newMaterial();

    void setTile(Material mat, Tile tile);

    void setTileset(Material mat, Tileset tileset);

    void setTiledImage(Material mat, TiledImage image);

    void setColor(Material mat, ColorRGBA color);

    void setTintColor(Material mat, ColorRGBA tintColor);

    void setTintColor(Spatial spatial, ColorRGBA tintColor);

    void setLayerOpacity(Material material, float opacity);

    void setLayerOpacity(Spatial spatial, float opacity);

    void setOpacity(Material material, float opacity);

    void setOpacity(Spatial spatial, float opacity);

    void setMapObject(Material mat, MapObject obj);
}