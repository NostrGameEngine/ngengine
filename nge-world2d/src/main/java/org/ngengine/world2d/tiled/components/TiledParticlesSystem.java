package org.ngengine.world2d.tiled.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigInteger;

import org.ngengine.AsyncAssetManager;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ReloadableComponent;
import org.ngengine.network.components.NetcodeManagerComponent;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

public class TiledParticlesSystem extends AbstractComponent implements ReloadableComponent {
    private final Logger logger = Logger.getLogger(TiledParticlesSystem.class.getName());
    private Map<String, Map<String,Tile>> particles = new HashMap<>();
    private List<TiledObjectEntity> foundObjects= new ArrayList<>();
    private static final AtomicLong LOCAL_PARTICLE_ID = new AtomicLong(-1L);

    

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {

    }

    @Override
    protected void onDisable(ComponentManager mng) {

    }

    @Override
    public void reload() {
        particles.clear();
        foundObjects.clear();
    }

    private void loadParticles(String tileset, Tileset ts) {
        for (Tile tile : ts.getTiles()) {
            String cls = tile.getClazz();
            if (cls != null && cls.startsWith("particle_")) {
                String name = cls.substring("particle_".length());
                particles.computeIfAbsent(tileset, k -> new HashMap<>()).put(name, tile);
            }

            String[] tags = getTags(tile);
            for (String tag : tags) {
                tag = tag.trim();
                if (tag.startsWith("particle_")) {
                    String name = tag.substring("particle_".length());
                    particles.computeIfAbsent(tileset, k -> new HashMap<>()).put(name, tile);
                }
            }
        }

    }

    public TiledObjectEntity spawn(
        String tileset, 
        String name, 
        TiledObjectLayer layer,
        float screenX, 
        float screenY, 
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty
    ) {
        try(TempVars vars = TempVars.get()){
            Map<String, Tile> map = particles.get(tileset);
            if (map == null) {
                AssetManager am = getInstanceOf(AssetManager.class);
                Tileset ts = (Tileset) am.loadAsset(tileset);
                loadParticles(tileset, ts);
                map = particles.get(tileset);
            }

            if (map == null) {
                logger.log(Level.WARNING, "No particles loaded for tileset: " + tileset);
                return null;
            }

            Tile tile = map.get(name);
            if (tile == null) {
                logger.log(Level.WARNING, "No particle named " + name + " in tileset: " + tileset);
                return null;
            }   

            Vector2f pos = vars.vect2d;
            CoordinateSystem cs = layer.getComponentManager().getInstanceOf(CoordinateSystem.class);
            cs.worldToGridSpace(screenX, screenY, pos);
            

            
            if(onlyIfEmpty){
                foundObjects.clear();
                layer.getObjectsAt(pos.x, pos.y, foundObjects);

                // check if any of the found objects use the same tile
                for(TiledObjectEntity o : foundObjects){
                    Tile t1 = o.getTile();
                    if(t1 == null) continue;
                    // Tileset ts1 = t1.getTileset();
                    for(Tile t2:map.values()){
                        // Tileset ts2 = t2.getTileset();
                        if(t1==t2){
                            // found existing particle of the same type
                            return null;
                        }
                    }
                }
            } 

            
            TiledObjectEntity obj = new TiledObjectEntity(nextObjectId(false), pos.x, pos.y, tile.getWidth(), tile.getHeight());
            obj.setVisible(true);
            obj.setShape(ObjectShape.TILE);
            obj.setTile(tile);
             if(baseWidth>0){
                obj.setWidth(baseWidth);
            }
            if(baseHeight>0){
                obj.setHeight(baseHeight);
            }
            obj.setWidth(obj.getWidth()*scale);
            obj.setHeight(obj.getHeight()*scale);
            layer.add(obj);
            Components.mount(obj, new TiledParticleComponent()).enable();
            return obj;

        }
   

    }

    private BigInteger nextObjectId(boolean networkSync) {
        if (networkSync) {
            NetcodeManagerComponent net = getInstanceOf(NetcodeManagerComponent.class);
            if (net != null) {
                return net.getNextTemporaryNetworkUID();
            }
        }
        return BigInteger.valueOf(LOCAL_PARTICLE_ID.getAndDecrement());
    }

  
    public TiledObjectEntity spawn(
        String tileset, 
        String name, 
        TiledObjectLayer layer,
        float screenX, 
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale
    ) {
        return spawn(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, false);
    }
    public TiledObjectEntity spawnIfEmpty(
        String tileset, 
        String name, 
        TiledObjectLayer layer,
        float screenX, 
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale
    ) {
        return spawn(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, true);
   

    }

    public void load(String tileset) {
        if(particles.containsKey(tileset))return;
        AsyncAssetManager am = getInstanceOf(AsyncAssetManager.class);
        am.loadAssetAsync(tileset, (t, exc) -> {
            if (exc != null) {
                logger.log(Level.WARNING, "Error loading tileset for particles: " + tileset, exc);
                return;
            }
            loadParticles(tileset, (Tileset) t);
        });
    }

        private static final String empty[] = new String[0];
    private static String[] getTags(TiledBase entity) {
        String tagsS = (String) entity.getProperty("tags");
        if(tagsS!=null){
            String tags[] = tagsS.split("[\\n|,]+");
            for(int i=0;i<tags.length;i++){
                tags[i]=tags[i].trim();
            }
            return tags;
        }
        return empty;

    }

}
