package io.github.jmecn.tiled.renderer.factory;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.core.TiledImage;
import io.github.jmecn.tiled.core.Tileset;
import io.github.jmecn.tiled.core.Base;
/**
 * @author yanmaoyuan
 */
public interface SpriteFactory {

   
   

    void applyProperties(Base tile, Spatial spatial);

    Geometry newTileSprite(Tile tile, Material material);

    Geometry newObjectSprite(MapObject object );

    MaterialFactory getMaterialFactory();

    void setMaterialFactory(MaterialFactory materialFactory);

    MeshFactory getMeshFactory();

    void setMeshFactory(MeshFactory meshFactory);

    void setAnimation(Spatial spatial, Tile tile);

    void setAnimation(Spatial spatial, MapObject object);
}