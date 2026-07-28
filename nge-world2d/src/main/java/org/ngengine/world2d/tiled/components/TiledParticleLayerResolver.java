package org.ngengine.world2d.tiled.components;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentManagerProvider;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledLayerGroup;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledTileContainer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;

/**
 * Resolves the object layer that should own a particle emitted by a tiled source.
 */
public final class TiledParticleLayerResolver {
    public static final String PROPERTY_LAYER = "particles.layer";
    private static final String[] DEFAULT_MAP_LAYERS = {"objects", "particles"};

    private TiledParticleLayerResolver() {
    }

    /**
     * Resolves a particle layer using the emitter configuration and its tiled owner.
     *
     * <p>The resolution order is:</p>
     * <ol>
     *   <li>the layer named on the emitter marker with {@value #PROPERTY_LAYER};</li>
     *   <li>the layer named on the source itself with {@value #PROPERTY_LAYER};</li>
     *   <li>the source object's own object layer;</li>
     *   <li>the object layer on which the source component is mounted;</li>
     *   <li>the supplied map fallback names, or {@code objects}, then {@code particles};</li>
     *   <li>the first object layer in the map.</li>
     * </ol>
     *
     * @param source emitter owner, tiled entry, layer, map, component, or manager provider
     * @param emitterId optional emitter marker identifier
     * @param fallbackMap map to use when it cannot be recovered from {@code source}
     * @param fallbackLayerNames optional ordered map-level fallback names
     * @return the resolved object layer, or {@code null} if the map has none
     */
    public static TiledObjectLayer resolve(
            Object source,
            String emitterId,
            TiledMap fallbackMap,
            String... fallbackLayerNames) {
        ComponentManager manager = componentManager(source);
        TiledBase owner = tiledOwner(source, manager);
        TiledLayer mountedLayer = tiledLayer(source, owner, manager);
        TiledMap map = tiledMap(source, owner, mountedLayer, manager, fallbackMap);

        TiledObjectEntity sourceObject = owner instanceof TiledObjectEntity
            ? (TiledObjectEntity) owner
            : null;
        TiledObjectLayer configured = findNamedLayer(
            map,
            TiledParticleEmitter.getLayerName(sourceObject, emitterId)
        );
        if (configured != null) {
            return configured;
        }

        configured = findNamedLayer(map, propertyLayerName(owner));
        if (configured != null) {
            return configured;
        }

        if (sourceObject != null && sourceObject.getObjectGroup() != null) {
            return sourceObject.getObjectGroup();
        }
        if (owner instanceof TiledObjectLayer) {
            return (TiledObjectLayer) owner;
        }
        if (mountedLayer instanceof TiledObjectLayer) {
            return (TiledObjectLayer) mountedLayer;
        }
        if (mountedLayer instanceof TiledLayerGroup) {
            TiledObjectLayer child = firstObjectLayer((TiledLayerGroup) mountedLayer);
            if (child != null) {
                return child;
            }
        }

        String[] fallbackNames = fallbackLayerNames != null && fallbackLayerNames.length > 0
            ? fallbackLayerNames
            : DEFAULT_MAP_LAYERS;
        if (map != null) {
            for (String fallbackName : fallbackNames) {
                TiledObjectLayer layer = findNamedLayer(map, fallbackName);
                if (layer != null) {
                    return layer;
                }
            }
            for (TiledLayer layer : map.getLayersFlat()) {
                if (layer instanceof TiledObjectLayer) {
                    return (TiledObjectLayer) layer;
                }
            }
        }
        return null;
    }

    private static ComponentManager componentManager(Object source) {
        if (source instanceof Component) {
            return ((Component) source).getComponentManager();
        }
        if (source instanceof ComponentManagerProvider) {
            return ((ComponentManagerProvider) source).getComponentManager();
        }
        return null;
    }

    private static TiledBase tiledOwner(Object source, ComponentManager manager) {
        if (source instanceof TiledBase) {
            return (TiledBase) source;
        }
        return manager != null ? manager.getInstanceOf(TiledBase.class) : null;
    }

    private static TiledLayer tiledLayer(Object source, TiledBase owner, ComponentManager manager) {
        if (source instanceof TiledLayer) {
            return (TiledLayer) source;
        }
        if (owner instanceof TiledLayer) {
            return (TiledLayer) owner;
        }
        if (owner instanceof TiledObjectEntity) {
            return ((TiledObjectEntity) owner).getObjectGroup();
        }
        if (owner instanceof TiledTileEntity) {
            TiledTileContainer container = ((TiledTileEntity) owner).getContainer();
            if (container instanceof TiledLayer) {
                return (TiledLayer) container;
            }
        }
        return manager != null ? manager.getInstanceOf(TiledLayer.class) : null;
    }

    private static TiledMap tiledMap(
            Object source,
            TiledBase owner,
            TiledLayer layer,
            ComponentManager manager,
            TiledMap fallbackMap) {
        if (source instanceof TiledMap) {
            return (TiledMap) source;
        }
        if (owner instanceof TiledMap) {
            return (TiledMap) owner;
        }
        if (layer != null && layer.getMap() != null) {
            return layer.getMap();
        }
        TiledMap managerMap = manager != null ? manager.getInstanceOf(TiledMap.class) : null;
        return managerMap != null ? managerMap : fallbackMap;
    }

    private static String propertyLayerName(TiledBase owner) {
        Object value = owner != null ? owner.getProperty(PROPERTY_LAYER) : null;
        return value != null ? String.valueOf(value).trim() : null;
    }

    private static TiledObjectLayer findNamedLayer(TiledMap map, String names) {
        if (map == null || names == null || names.isBlank()) {
            return null;
        }
        for (String name : names.split("[\\n,|]+")) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            TiledObjectLayer layer = map.getLayerByName(trimmed, TiledObjectLayer.class);
            if (layer != null) {
                return layer;
            }
        }
        return null;
    }

    private static TiledObjectLayer firstObjectLayer(TiledLayerGroup group) {
        for (TiledLayer layer : group.getLayers()) {
            if (layer instanceof TiledObjectLayer) {
                return (TiledObjectLayer) layer;
            }
            if (layer instanceof TiledLayerGroup) {
                TiledObjectLayer nested = firstObjectLayer((TiledLayerGroup) layer);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
