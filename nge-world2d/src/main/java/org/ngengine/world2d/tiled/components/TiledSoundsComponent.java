package org.ngengine.world2d.tiled.components;

import org.ngengine.components.jme3.audio.AbstractAudioComponent;
import org.ngengine.components.jme3.audio.Sound;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;

import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

public class TiledSoundsComponent extends AbstractAudioComponent {
    private final Transform worldTransform = new Transform();
    private final Vector2f worldPosition = new Vector2f();

   
    @Override
    protected Transform getWorldTransform() {
        TiledObjectEntity entry = getInstanceOf(TiledObjectEntity.class);
        CoordinateSystem cs = getInstanceOf(CoordinateSystem.class);
        if (entry != null) {
            cs.getCenterInGridSpace(entry, worldPosition);
            cs.gridToWorldSpace(worldPosition.x, worldPosition.y, worldPosition);
            cs.worldToPhysicsSpace(worldPosition.x, worldPosition.y, worldPosition);
            worldTransform.setTranslation(worldPosition.x, 0, worldPosition.y);
        }
        return worldTransform;
    }

    /**
     * Configures positional attenuation in map-cell units instead of raw
     * physics meters.
     *
     * @param sound sound to configure
     * @param referenceTiles reference scale used by inverse distance attenuation
     * @param maximumTiles distance where inverse attenuation is clamped
     * @return the supplied sound
     */
    public Sound setDistanceInTiles(
            Sound sound,
            float referenceTiles,
            float maximumTiles) {
        if (sound == null) {
            return null;
        }
        CoordinateSystem coordinates = getInstanceOf(CoordinateSystem.class);
        if (coordinates == null) {
            return sound;
        }
        float referenceDistance = coordinates.tileDistanceToPhysics(referenceTiles);
        float maximumDistance = coordinates.tileDistanceToPhysics(maximumTiles);
        sound.setRefDistance(referenceDistance);
        sound.setMaxDistance(Math.max(referenceDistance, maximumDistance));
        return sound;
    }
}
