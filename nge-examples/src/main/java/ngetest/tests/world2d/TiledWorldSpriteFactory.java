package ngetest.tests.world2d;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.Components;
import org.ngengine.components.Component;
import org.ngengine.platform.NGEUtils;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.renderer.factory.DefaultSpriteFactory;
import io.github.jmecn.tiled.renderer.factory.MaterialFactory;
import io.github.jmecn.tiled.renderer.factory.MeshFactory;

public class TiledWorldSpriteFactory extends DefaultSpriteFactory {
    private final static Logger logger = Logger.getLogger(TiledWorldSpriteFactory.class.getName());
     public TiledWorldSpriteFactory() {
        super();
    }

    public TiledWorldSpriteFactory(MeshFactory meshFactory, MaterialFactory materialFactory) {
        super(meshFactory, materialFactory);
    }

    @Override
    public Geometry newTileSprite(Tile tile){
        Geometry geom = super.newTileSprite(tile);    
        loadProperties(geom);
        return geom;
    }

    @Override
    public Geometry newTileSprite(Tile tile, Material material){
        Geometry geom = super.newTileSprite(tile, material);
        loadProperties(geom);
        return geom;        
    }

    @Override
    public Spatial newObjectSprite(MapObject object, Material material){
        Spatial sp = super.newObjectSprite(object, material);
        loadProperties(sp);
        return sp;
    }


    public static void loadProperties(Spatial root) {
        root.breadthFirstTraversal(sp -> {
            for (String k : sp.getUserDataKeys()) {
                if (k.equalsIgnoreCase("Components.mount")) {
                    String mounts[] = NGEUtils.safeString(sp.getUserData(k)).trim().split("\n");
                    for (String mount : mounts) {
                        try {
                            @SuppressWarnings("unchecked")
                            Class<? extends Component> cls = (Class<? extends Component>) Class.forName(mount);
                          
                            if(!Components.has(sp, cls)){
                                Object obj = cls.getDeclaredConstructor().newInstance();
                                if (!(obj instanceof Component)) {
                                    throw new IllegalArgumentException("Class " + mount + " is not a Component");
                                }

                                Components.mount(sp, (Component) obj).enable();
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Cannot find component class to mount: " + mount, e);
                        }
                    }
                }
            }
        });
   
    }
}
