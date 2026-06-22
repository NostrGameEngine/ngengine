package org.ngengine.world2d.tiled.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;

import org.ngengine.world2d.tiled.core.TiledBase;
import jakarta.annotation.Nullable;

/**
 * Shared reflection-based component mounting utility for tiled owners.
 *
 * <p>This is used by map/layer/entity loading and by snapshot-driven auto-mount.
 */
public final class TiledComponentReflectionMounting {
    private static final Logger log = Logger.getLogger(TiledComponentReflectionMounting.class.getName());

    private TiledComponentReflectionMounting() {}

    public static List<String> parseComponentClassNames(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = raw.split("[\\n|,]+");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String cls = p.trim();
            if (!cls.isEmpty()) {
                out.add(cls);
            }
        }
        return out;
    }

    public static void mountFromProperty(@Nullable TiledBase owner, @Nullable ComponentManager manager) {
        if (owner == null || manager == null) {
            return;
        }
        Object raw = owner.getProperty("components");
        if (!(raw instanceof String)) {
            return;
        }
        for (String className : parseComponentClassNames((String) raw)) {
            mountByClassName(manager, className, owner);
        }
    }

    public static void mountBuiltInsFromProperties(@Nullable TiledBase owner, @Nullable ComponentManager manager) {
        if (owner == null || manager == null) {
            return;
        }
        if (TiledModelComponent.hasModel(owner) && manager.getComponent(TiledModelComponent.class) == null) {
            manager.addComponent(new TiledModelComponent());
            manager.enableComponent(TiledModelComponent.class);
        }
    }

    public static @Nullable Component mountByClassName(
        @Nullable ComponentManager manager,
        @Nullable String componentClassName,
        @Nullable Object ownerContext
    ) {
        if (manager == null || componentClassName == null || componentClassName.trim().isEmpty()) {
            return null;
        }
        String className = componentClassName.trim();
        try {
            Class<?> clazz = Class.forName(className);
            if (!Component.class.isAssignableFrom(clazz)) {
                log.warning("Cannot mount " + className + " for " + ownerContext + ": class does not implement Component.");
                return null;
            }
            Component existing = manager.getComponent((Class<? extends Component>) clazz);
            if (existing != null) {
                manager.enableComponent(existing);
                return existing;
            }
            Component component = (Component) clazz.getDeclaredConstructor().newInstance();
            manager.addComponent(component);
            manager.enableComponent(component);
            return component;
        } catch (Exception e) {
            log.log(
                Level.WARNING,
                "Failed to mount component class: " + className + " for owner " + ownerContext +
                ". The classpath may be incorrect or constructor may be invalid.",
                e
            );
            return null;
        }
    }
}
