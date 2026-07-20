package org.ngengine.world2d.tiled.components;

import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import org.ngengine.components.ComponentManager;
import org.ngengine.world2d.tiled.animation.AnimatedTileControl;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.util.CoordinateSystem;
import org.ngengine.world2d.tiled.util.TiledAnchorResolver;

public final class TiledParticleEmitter {
    public static final String PROPERTY_EMITTER = "particles.emitter";

    private TiledParticleEmitter() {
    }

    public static boolean getPosition(TiledObjectEntity source, String emitterId, CoordinateSystem coordinates,
            Vector2f out) {
        if (source == null || coordinates == null || out == null) {
            return false;
        }
        Tile sourceTile = source.getTile();
        Tile currentTile = getCurrentTile(source);
        String expectedEmitter = emitterId == null || emitterId.isBlank() ? null : emitterId;
        return TiledAnchorResolver.resolve(
            source,
            currentTile != null ? currentTile : sourceTile,
            PROPERTY_EMITTER,
            expectedEmitter,
            coordinates,
            out
        );
    }

    public static TiledObjectEntity findEmitter(Tile tile, String emitterId) {
        String expectedEmitter = emitterId == null || emitterId.isBlank() ? null : emitterId;
        return TiledAnchorResolver.findMarker(tile, PROPERTY_EMITTER, expectedEmitter);
    }

    public static Tile getCurrentTile(TiledObjectEntity source) {
        if (source == null) {
            return null;
        }
        ComponentManager manager = source.getComponentManager();
        Spatial visual = manager != null ? manager.getInstanceOf(Spatial.class) : null;
        AnimatedTileControl animation = findAnimation(visual);
        Tile current = animation != null ? animation.getCurrentTile() : null;
        return current != null ? current : source.getTile();
    }

    private static AnimatedTileControl findAnimation(Spatial visual) {
        if (visual == null) {
            return null;
        }
        AnimatedTileControl control = visual.getControl(AnimatedTileControl.class);
        if (control != null) {
            return control;
        }
        if (visual instanceof Node) {
            for (Spatial child : ((Node) visual).getChildren()) {
                control = findAnimation(child);
                if (control != null) {
                    return control;
                }
            }
        }
        return null;
    }
}
