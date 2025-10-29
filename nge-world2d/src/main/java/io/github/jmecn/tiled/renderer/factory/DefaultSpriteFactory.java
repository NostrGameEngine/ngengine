package io.github.jmecn.tiled.renderer.factory;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.*;
import io.github.jmecn.tiled.animation.AnimatedTileControl;
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.enums.ObjectType;


/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class DefaultSpriteFactory implements SpriteFactory {

    private final static Logger logger = Logger.getLogger(DefaultSpriteFactory.class.getName());

    private MeshFactory meshFactory;
    private MaterialFactory materialFactory;

    public DefaultSpriteFactory() {
        this(null, null);
    }

    public DefaultSpriteFactory(MeshFactory meshFactory, MaterialFactory materialFactory) {
        this.meshFactory = meshFactory;
        this.materialFactory = materialFactory;
    }

    /**
     * Get the MaterialFactory for creating tile material.
     * @return the MaterialFactory
     */
    public MaterialFactory getMaterialFactory() {
        return materialFactory;
    }

    /**
     * Set the MaterialFactory for creating tile material.
     * @param materialFactory the MaterialFactory
     */
    public void setMaterialFactory(MaterialFactory materialFactory) {
        this.materialFactory = materialFactory;
    }

    /**
     * Get the MeshFactory for creating tile mesh.
     * @return the MeshFactory
     */
    public MeshFactory getMeshFactory() {
        return meshFactory;
    }

    /**
     * Set the MeshFactory for creating tile mesh.
     * @param meshFactory the MeshFactory
     */
    public void setMeshFactory(MeshFactory meshFactory) {
        this.meshFactory = meshFactory;
    }

  
    public void applyProperties(Base tile, Spatial spatial){
         Properties props = tile.getProperties();
        for(Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            Object value =  entry.getValue();
            if(!UserData.isSupportedType(value)) continue;
            spatial.setUserData(key, value);                   
        }
    }
  
 
    @Override
    public Geometry newTileSprite(Tile tile, Material material) {
        Mesh mesh = meshFactory.getTileMesh(tile);
        String name = "tile#" + tile.getGid();
        Geometry geometry = new Geometry(name, mesh);
        geometry.setMaterial(material);
        if (tile.isAnimated()) {
            geometry.addControl(new AnimatedTileControl(tile));
        }
        return geometry;
    }

    public Geometry newObjectSprite(MapObject obj) {
        Mesh mesh = meshFactory.newObjectMesh(obj);
        if (mesh == null) {
            return null;
        }

        Geometry geometry = new Geometry(obj.getName(), mesh);
        return geometry;
    }

    private Geometry text(MapObject obj) {
        // TODO render text
        ObjectText objectText = obj.getTextData();
        return null;
    }

    @Override
    public void setAnimation(Spatial visual, Tile tile) {
        AnimatedTileControl anim = visual.getControl(AnimatedTileControl.class);
        if(anim == null && tile.isAnimated()){
            visual.addControl(new AnimatedTileControl(tile));
        } else if(anim != null){
            if(!tile.isAnimated()){
                visual.removeControl(anim);
            } else {
                anim.setTile(tile);
            }                    
        }
    }

    @Override
    public void setAnimation(Spatial spatial, MapObject object) {
        if(object.getShape()==ObjectType.TILE){
            Tile tile = object.getTile();
            setAnimation(spatial, tile);
        }
    }

}
