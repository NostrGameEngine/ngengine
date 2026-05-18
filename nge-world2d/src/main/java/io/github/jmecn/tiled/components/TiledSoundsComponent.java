package io.github.jmecn.tiled.components;

import org.ngengine.components.jme3.audio.AbstractAudioComponent;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;

import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.util.CoordinateSystem;

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

 
   
    
}
