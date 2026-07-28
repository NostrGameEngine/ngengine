package org.ngengine.world2d.tiled.components;

import java.util.ArrayList;
import java.util.Collection;
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
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.util.CoordinateSystem;
import org.ngengine.world2d.tiled.util.TiledAnchorResolver;

public class TiledParticlesSystem extends AbstractComponent implements ReloadableComponent {
    private static final float PARTICLE_ANCHOR_EPSILON_SQUARED = 0.0001f;
    private final Logger logger = Logger.getLogger(TiledParticlesSystem.class.getName());
    private Map<String, Map<String,Tile>> particles = new HashMap<>();
    private List<TiledObjectEntity> foundObjects= new ArrayList<>();
    private final Vector2f foundParticleAnchor = new Vector2f();
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

            TiledMap tiledMap = layer.getComponentManager().getInstanceOf(TiledMap.class);
            Orientation orientation = tiledMap != null ? tiledMap.getOrientation() : null;
            if (onlyIfEmpty && hasParticleAt(layer, map.values(), pos, cs, orientation)) {
                return null;
            }


            TiledObjectEntity obj = new TiledObjectEntity(nextObjectId(source, networkSync), pos.x, pos.y, tile.getWidth(), tile.getHeight());
            obj.setVisible(true);
            obj.setShape(ObjectShape.TILE);
            obj.setTile(tile);
            applyParticleRenderProperties(tile, obj);
            if(baseWidth>0){
                obj.setWidth(baseWidth);
            }
            if(baseHeight>0){
                obj.setHeight(baseHeight);
            }
            obj.setWidth(obj.getWidth()*scale);
            obj.setHeight(obj.getHeight()*scale);
            TiledParticleOrigin.alignToGridAnchor(
                obj,
                pos,
                cs,
                tiledMap != null ? tiledMap.getOrientation() : null
            );
            layer.add(obj);
            Components.mount(obj, new TiledParticleComponent()).enable();
            return obj;

        }


    }

    static void applyParticleRenderProperties(Tile tile, TiledObjectEntity object) {
        if (tile == null || object == null) {
            return;
        }
        Object particleOpacity = tile.getProperty("particle.opacity");
        if (particleOpacity instanceof Number) {
            object.putProperty(
                "render.opacity",
                Math.max(0f, Math.min(1f, ((Number) particleOpacity).floatValue()))
            );
        }
    }

    boolean hasParticleAt(TiledObjectLayer layer, Collection<Tile> particleTiles, Vector2f anchorGrid,
            CoordinateSystem coordinates, Orientation orientation) {
        foundObjects.clear();
        layer.getObjectsAt(anchorGrid.x, anchorGrid.y, foundObjects);
        for (TiledObjectEntity object : foundObjects) {
            if (particleTiles.contains(object.getTile())) {
                return true;
            }
        }

        for (TiledObjectEntity object : layer.getObjects()) {
            if (!particleTiles.contains(object.getTile())) {
                continue;
            }
            if (TiledParticleOrigin.getGridAnchor(object, coordinates, orientation, foundParticleAnchor)
                    && foundParticleAnchor.distanceSquared(anchorGrid) <= PARTICLE_ANCHOR_EPSILON_SQUARED) {
                return true;
            }
        }
        return false;
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
        } else if (source instanceof ComponentManagerProvider) {
            manager = ((ComponentManagerProvider) source).getComponentManager();
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
    /**
     * Spawns a local particle at explicit world coordinates.
     *
     * @param onlyIfEmpty when true, skips an occupied particle anchor
     */
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
        return spawnInternal(
            null,
            tileset,
            name,
            layer,
            screenX,
            screenY,
            baseWidth,
            baseHeight,
            scale,
            onlyIfEmpty,
            false
        );
    }

    /**
     * Spawns a network-synchronized particle at explicit world coordinates.
     *
     * @param onlyIfEmpty when true, skips an occupied particle anchor
     */
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
        return spawnInternal(
            null,
            tileset,
            name,
            layer,
            screenX,
            screenY,
            baseWidth,
            baseHeight,
            scale,
            onlyIfEmpty,
            true
        );
    }

    /**
     * Spawns at explicit world coordinates and derives network ownership from
     * {@code source}.
     *
     * <p>When {@code followSource} is true, the initial world position becomes a
     * persistent offset from the source anchor.
     *
     * @param source tiled entity, component, or component-manager provider used
     *               for authority and optional following
     * @param onlyIfEmpty when true, skips an occupied particle anchor
     * @param followSource whether the particle should continue following the source
     */
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
        boolean followSource
    ) {
        BigInteger sourceId = getSourceNetworkId(source);
        boolean networkSync = sourceId != null && sourceId.signum() >= 0;
        if (networkSync && !isSourceLocallyAuthoritative(source)) {
            return null;
        }
        TiledEntity sourceEntity = getSourceEntity(source);
        if (followSource && (sourceEntity == null || layer == null)) {
            return null;
        }
        TiledObjectEntity particle = spawnInternal(
            source,
            tileset,
            name,
            layer,
            screenX,
            screenY,
            baseWidth,
            baseHeight,
            scale,
            onlyIfEmpty,
            networkSync
        );
        if (!followSource || particle == null) {
            return particle;
        }

        CoordinateSystem cs = layer.getComponentManager().getInstanceOf(CoordinateSystem.class);
        if (cs == null) {
            particle.removeFromLayer();
            return null;
        }
        try (TempVars vars = TempVars.get()) {
            Vector2f sourceGrid = vars.vect2d;
            if (sourceEntity instanceof TiledObjectEntity) {
                TiledObjectEntity sourceObject = (TiledObjectEntity) sourceEntity;
                TiledAnchorResolver.resolve(sourceObject, sourceObject.getTile(), null, null, cs, sourceGrid);
            } else {
                cs.getCenterInGridSpace(sourceEntity, sourceGrid);
            }
            Vector2f particleGrid = vars.vect2d2;
            cs.worldToGridSpace(screenX, screenY, particleGrid);
            TiledParticleComponent component = Components.get(particle, TiledParticleComponent.class).get();
            if (component != null) {
                component.follow(
                    sourceEntity,
                    particleGrid.x - sourceGrid.x,
                    particleGrid.y - sourceGrid.y
                );
            }
            return particle;
        }
    }

    /**
     * Spawns at a named emitter and derives network ownership from {@code source}.
     *
     * @param source tiled object or owning component containing the emitter
     * @param emitterId value of the emitter marker's {@code particles.emitter} property
     * @param layer target layer, or {@code null} to resolve it from the emitter owner
     * @param onlyIfEmpty when true, skips an occupied particle anchor
     * @param followSource whether the particle should continue following the emitter
     */
    public TiledObjectEntity spawnFromEmitter(
        Object source,
        String emitterId,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean onlyIfEmpty,
        boolean followSource
    ) {
        BigInteger sourceId = getSourceNetworkId(source);
        boolean networkSync = sourceId != null && sourceId.signum() >= 0;
        TiledEntity sourceEntity = getSourceEntity(source);
        TiledObjectLayer resolvedLayer = layer != null
            ? layer
            : resolveParticleLayer(source, emitterId);
        if (!(sourceEntity instanceof TiledObjectEntity) || resolvedLayer == null) {
            return null;
        }
        if (networkSync && !isSourceLocallyAuthoritative(source)) {
            return null;
        }
        CoordinateSystem cs = resolvedLayer.getComponentManager().getInstanceOf(CoordinateSystem.class);
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
            TiledObjectEntity particle = spawnInternal(
                source,
                tileset,
                name,
                resolvedLayer,
                world.x,
                world.y,
                baseWidth,
                baseHeight,
                scale,
                onlyIfEmpty,
                networkSync
            );
            TiledParticleComponent component = followSource && particle != null
                ? Components.get(particle, TiledParticleComponent.class).get()
                : null;
            if (component != null) {
                component.followEmitter(sourceEntity, emitterId);
            }
            return particle;
        }
    }

    /**
     * Resolves the default layer for a particle emitter from its configured marker
     * and tiled owner.
     *
     * @param source emitter owner or component
     * @param emitterId optional emitter marker identifier
     * @param fallbackLayerNames optional ordered map-level fallback layer names
     * @return resolved object layer, or {@code null} when none exists
     */
    public TiledObjectLayer resolveParticleLayer(
            Object source,
            String emitterId,
            String... fallbackLayerNames) {
        return TiledParticleLayerResolver.resolve(
            source,
            emitterId,
            getInstanceOf(TiledMap.class),
            fallbackLayerNames
        );
    }

    /** Spawns a local particle only when its resolved anchor is empty. */
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

    /** Spawns a network-synchronized particle only when its resolved anchor is empty. */
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
        return spawnNetworked(tileset, name, layer, screenX, screenY, baseWidth, baseHeight, scale, true);
    }

    /**
     * Spawns from a source only when the resolved anchor is empty.
     *
     * @param followSource whether the particle should continue following the source
     */
    public TiledObjectEntity spawnIfEmptyFrom(
        Object source,
        String tileset,
        String name,
        TiledObjectLayer layer,
        float screenX,
        float screenY,
        float baseWidth,
        float baseHeight,
        float scale,
        boolean followSource
    ) {
        return spawnFrom(
            source,
            tileset,
            name,
            layer,
            screenX,
            screenY,
            baseWidth,
            baseHeight,
            scale,
            true,
            followSource
        );
    }

    /** Preloads and registers all particle tiles from a tileset. */
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
