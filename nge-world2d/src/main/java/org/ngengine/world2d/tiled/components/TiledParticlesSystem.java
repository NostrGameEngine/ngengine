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
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentManagerProvider;
import org.ngengine.components.ReloadableComponent;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.network.components.NetcodeFragment;
import org.ngengine.network.components.NetcodePartitioning;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledMap;
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

    private TiledObjectEntity spawnInternal(
        Object source,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float screenX,
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty,
        boolean networkSync
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


            TiledObjectEntity obj = new TiledObjectEntity(nextObjectId(source, networkSync), pos.x, pos.y, tile.getWidth(), tile.getHeight());
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

    private BigInteger nextObjectId(Object source, boolean networkSync) {
        if (!networkSync) {
            return nextLocalObjectId();
        }

        BigInteger sourceId = source != null ? getSourceNetworkId(source) : null;
        if (source != null) {
            if (sourceId == null || sourceId.signum() < 0) {
                return nextLocalObjectId();
            }
            if (!isSourceLocallyAuthoritative(source)) {
                return nextLocalObjectId();
            }

            if (!NetcodePartitioning.isReservedId(sourceId) && !NetcodePartitioning.isPersistentId(sourceId)) {
                BigInteger sharedId = nextSharedObjectId(sourceId);
                if (sharedId != null) {
                    return sharedId;
                }
            }
        }

        NetcodeManagerComponent net = getInstanceOf(NetcodeManagerComponent.class);
        if (net != null) {
            try {
                return net.getNextTemporaryNetworkUID();
            } catch (IllegalStateException ex) {
                logger.log(Level.FINE, "Cannot allocate networked particle id without a local peer key.", ex);
            }
        }
        return nextLocalObjectId();
    }

    private BigInteger nextLocalObjectId() {
        return BigInteger.valueOf(LOCAL_PARTICLE_ID.getAndDecrement());
    }

    private BigInteger nextSharedObjectId(BigInteger sourceId) {
        NetcodeManagerComponent net = getInstanceOf(NetcodeManagerComponent.class);
        TiledMap map = getInstanceOf(TiledMap.class);
        if (map == null) {
            return null;
        }

        NostrPublicKey sourceOwner = net != null ? net.resolveActiveOwnerPeerPublicKey(sourceId) : null;
        int candidate = Math.max(1, map.getNextObjectId());
        int attempts = 0;
        while (candidate > 0 && candidate <= NetcodePartitioning.SHARED_MAX.intValue() && attempts < 4096) {
            BigInteger candidateId = BigInteger.valueOf(candidate);
            map.setNextObjectId(candidate + 1);
            candidate++;
            attempts++;

            if (sourceOwner == null || net == null) {
                return candidateId;
            }
            NostrPublicKey candidateOwner = net.resolveActiveOwnerPeerPublicKey(candidateId);
            if (sourceOwner.equals(candidateOwner)) {
                return candidateId;
            }
        }
        return null;
    }

    private boolean isSourceLocallyAuthoritative(Object source) {
        NetcodeFragment fragment = getSourceNetcodeFragment(source);
        if (fragment != null) {
            return fragment.checkAuthority();
        }
        return true;
    }

    private BigInteger getSourceNetworkId(Object source) {
        NetcodeFragment fragment = getSourceNetcodeFragment(source);
        if (fragment != null) {
            return fragment.getNetworkId();
        }
        if (source instanceof TiledObjectEntity) {
            return ((TiledObjectEntity) source).getId();
        }
        return null;
    }

    private TiledEntity getSourceEntity(Object source) {
        if (source instanceof TiledEntity) {
            return (TiledEntity) source;
        }
        ComponentManager manager = null;
        if (source instanceof TiledObjectEntity) {
            manager = ((TiledObjectEntity) source).getComponentManager();
        } else if (source instanceof Component) {
            manager = ((Component) source).getComponentManager();
        } else if (source instanceof ComponentManagerProvider) {
            manager = ((ComponentManagerProvider) source).getComponentManager();
        }
        if (manager != null) {
            TiledEntity entity = manager.getInstanceOf(TiledEntity.class);
            if (entity != null) {
                return entity;
            }
        }
        if (source instanceof Component) {
            Object entity = ((Component) source).getInstanceOf(TiledObjectEntity.class);
            if (entity instanceof TiledEntity) {
                return (TiledEntity) entity;
            }
        }
        return null;
    }

    private NetcodeFragment getSourceNetcodeFragment(Object source) {
        if (source instanceof NetcodeFragment) {
            return (NetcodeFragment) source;
        }
        ComponentManager manager = null;
        if (source instanceof TiledObjectEntity) {
            manager = ((TiledObjectEntity) source).getComponentManager();
        } else if (source instanceof Component) {
            manager = ((Component) source).getComponentManager();
        }
        if (manager != null) {
            NetcodeFragment fragment = manager.getComponent(TiledObjectSyncComponent.class);
            if (fragment != null) {
                return fragment;
            }
            fragment = manager.getComponent(TiledParticleComponent.class);
            if (fragment != null) {
                return fragment;
            }
        }
        if (source instanceof Component) {
            Object entity = ((Component) source).getInstanceOf(TiledObjectEntity.class);
            if (entity instanceof TiledObjectEntity) {
                ComponentManager entityManager = ((TiledObjectEntity) entity).getComponentManager();
                NetcodeFragment fragment = entityManager.getComponent(TiledObjectSyncComponent.class);
                if (fragment != null) {
                    return fragment;
                }
                fragment = entityManager.getComponent(TiledParticleComponent.class);
                if (fragment != null) {
                    return fragment;
                }
            }
        }
        return null;
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
        return spawn(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, false, false);
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
        return spawn(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, onlyIfEmpty, false);
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
        boolean onlyIfEmpty,
        boolean networkSync
    ) {
        return spawnInternal(null, tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, onlyIfEmpty, networkSync);
    }

    public TiledObjectEntity spawnNetworked(
        String tileset,
        String name,
        TiledObjectLayer layer,
        float screenX,
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale
    ) {
        return spawnNetworked(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, false);
    }

    public TiledObjectEntity spawnNetworked(
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
        return spawn(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, onlyIfEmpty, true);
    }

    public TiledObjectEntity spawnFrom(
        Object source,
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
        BigInteger sourceId = getSourceNetworkId(source);
        boolean networkSync = sourceId != null && sourceId.signum() >= 0;
        return spawnFrom(source, tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, onlyIfEmpty, networkSync);
    }

    public TiledObjectEntity spawnFrom(
        Object source,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float screenX,
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty,
        boolean networkSync
    ) {
        if (networkSync && !isSourceLocallyAuthoritative(source)) {
            return null;
        }
        return spawnInternal(source, tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, onlyIfEmpty, networkSync);
    }

    public TiledObjectEntity spawnFollowingFrom(
        Object source,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float offsetX,
        float offsetY,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty
    ) {
        BigInteger sourceId = getSourceNetworkId(source);
        boolean networkSync = sourceId != null && sourceId.signum() >= 0;
        return spawnFollowingFrom(source, tileset, name, layer, offsetX, offsetY, baseWidth, baseHeight, scale,
            onlyIfEmpty, networkSync);
    }

    public TiledObjectEntity spawnFollowingFrom(
        Object source,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float offsetX,
        float offsetY,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty,
        boolean networkSync
    ) {
        TiledEntity sourceEntity = getSourceEntity(source);
        if (sourceEntity == null || layer == null) {
            return null;
        }
        if (networkSync && !isSourceLocallyAuthoritative(source)) {
            return null;
        }
        CoordinateSystem cs = layer.getComponentManager().getInstanceOf(CoordinateSystem.class);
        if (cs == null) {
            return null;
        }
        try (TempVars vars = TempVars.get()) {
            Vector2f grid = vars.vect2d;
            cs.getCenterInGridSpace(sourceEntity, grid);
            grid.x += offsetX;
            grid.y += offsetY;

            Vector2f world = vars.vect2d2;
            cs.gridToWorldSpace(grid.x, grid.y, world);
            TiledObjectEntity particle = spawnInternal(source, tileset, name, layer, world.x, world.y, baseWidth,
                baseHeight, scale, onlyIfEmpty, networkSync);
            TiledParticleComponent component = particle != null
                ? Components.get(particle, TiledParticleComponent.class).get()
                : null;
            if (component != null) {
                component.follow(sourceEntity, offsetX, offsetY);
            }
            return particle;
        }
    }

    public TiledObjectEntity spawnFollowingFromEmitter(
        Object source,
        String emitterId,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty
    ) {
        BigInteger sourceId = getSourceNetworkId(source);
        boolean networkSync = sourceId != null && sourceId.signum() >= 0;
        return spawnFollowingFromEmitter(source, emitterId, tileset, name, layer, baseWidth, baseHeight, scale,
            onlyIfEmpty, networkSync);
    }

    public TiledObjectEntity spawnFollowingFromEmitter(
        Object source,
        String emitterId,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty,
        boolean networkSync
    ) {
        TiledEntity sourceEntity = getSourceEntity(source);
        if (!(sourceEntity instanceof TiledObjectEntity) || layer == null) {
            return null;
        }
        if (networkSync && !isSourceLocallyAuthoritative(source)) {
            return null;
        }
        CoordinateSystem cs = layer.getComponentManager().getInstanceOf(CoordinateSystem.class);
        if (cs == null) {
            return null;
        }
        try (TempVars vars = TempVars.get()) {
            Vector2f grid = vars.vect2d;
            if (!TiledParticleEmitter.getPosition((TiledObjectEntity) sourceEntity, emitterId, cs, grid)) {
                return null;
            }
            Vector2f world = vars.vect2d2;
            cs.gridToWorldSpace(grid.x, grid.y, world);
            TiledObjectEntity particle = spawnInternal(source, tileset, name, layer, world.x, world.y, baseWidth,
                baseHeight, scale, onlyIfEmpty, networkSync);
            TiledParticleComponent component = particle != null
                ? Components.get(particle, TiledParticleComponent.class).get()
                : null;
            if (component != null) {
                component.followEmitter(sourceEntity, emitterId);
            }
            return particle;
        }
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
        return spawn(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, true, false);
    }

    public TiledObjectEntity spawnIfEmptyNetworked(
        String tileset,
        String name,
        TiledObjectLayer layer,
        float screenX,
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale
    ) {
        return spawn(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, true, true);
    }

    public TiledObjectEntity spawnIfEmptyFrom(
        Object source,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float screenX,
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale
    ) {
        return spawnFrom(source, tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, true);
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
