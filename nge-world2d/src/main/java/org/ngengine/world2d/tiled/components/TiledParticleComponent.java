package org.ngengine.world2d.tiled.components;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.network.components.NetcodeOrphanContext;

import com.jme3.math.Vector2f;

import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledNetcodeFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.util.CoordinateSystem;
import org.ngengine.world2d.tiled.util.TiledAnchorResolver;

public class TiledParticleComponent extends AbstractComponent implements TiledEntityLogicFragment, TiledNetcodeFragment{
    private float lifespan = -1f;
    private float age = 0f;
    private TiledEntity followTarget;
    private String followEmitter;
    private float followOffsetX;
    private float followOffsetY;
    private final Vector2f tmpFollowGrid = new Vector2f();

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        TiledBase entity = getInstanceOf(TiledEntity.class);
        Number lifeSpan = (Number) entity.getProperty("particle.lifespan");
        if(lifeSpan!=null){
            lifespan = lifeSpan.floatValue();
        }
        
    }

    @Override
    protected void onDisable(ComponentManager mng) {
       
    }

    @Override
    public void onTiledEntityLogicUpdate(ComponentManager mng, float tpf, TiledBase entry) {
        if (!checkAuthority()) {
            return;
        }
        updateFollowTarget();
        age+=tpf;
        if(isDead()){
            TiledObjectEntity entity = getInstanceOf(TiledObjectEntity.class);
            entity.getObjectGroup().remove(entity);
        }
        
    }

    public boolean isDead(){
        return lifespan>0 && age>=lifespan;
    }

    @Override
    public void onNetworkOrphaned(NetcodeOrphanContext context) {
        TiledObjectEntity entity = getInstanceOf(TiledObjectEntity.class);
        if (entity != null && entity.getObjectGroup() != null) {
            entity.removeFromLayer();
        }
    }

    public TiledParticleComponent follow(TiledEntity target, float offsetX, float offsetY) {
        this.followTarget = target;
        this.followEmitter = null;
        this.followOffsetX = offsetX;
        this.followOffsetY = offsetY;
        updateFollowTarget();
        return this;
    }

    public TiledParticleComponent followEmitter(TiledEntity target, String emitterId) {
        this.followTarget = target;
        this.followEmitter = emitterId;
        this.followOffsetX = 0f;
        this.followOffsetY = 0f;
        updateFollowTarget();
        return this;
    }

    public void clearFollowTarget() {
        this.followTarget = null;
        this.followEmitter = null;
        this.followOffsetX = 0f;
        this.followOffsetY = 0f;
    }

    /** Re-applies the current follow target after changing particle rotation or scale. */
    public void refreshFollowPosition() {
        updateFollowTarget();
    }

    private void updateFollowTarget() {
        if (followTarget == null) {
            return;
        }
        ComponentManager manager = getComponentManager();
        if (manager == null) {
            return;
        }
        CoordinateSystem cs = manager.getInstanceOf(CoordinateSystem.class);
        TiledObjectEntity particle = manager.getInstanceOf(TiledObjectEntity.class);
        if (cs == null || particle == null) {
            return;
        }
        if (followEmitter != null && followTarget instanceof TiledObjectEntity
                && TiledParticleEmitter.getPosition((TiledObjectEntity) followTarget, followEmitter, cs,
                    tmpFollowGrid)) {
            setAtAnchor(particle, cs, tmpFollowGrid);
            return;
        }
        if (followTarget instanceof TiledObjectEntity) {
            TiledObjectEntity source = (TiledObjectEntity) followTarget;
            TiledAnchorResolver.resolve(source, source.getTile(), null, null, cs, tmpFollowGrid);
        } else {
            cs.getCenterInGridSpace(followTarget, tmpFollowGrid);
        }
        tmpFollowGrid.addLocal(followOffsetX, followOffsetY);
        setAtAnchor(particle, cs, tmpFollowGrid);
    }

    private void setAtAnchor(TiledObjectEntity particle, CoordinateSystem cs, Vector2f anchor) {
        TiledMap map = getInstanceOf(TiledMap.class);
        if (!TiledParticleOrigin.alignToGridAnchor(particle, anchor, cs,
                map != null ? map.getOrientation() : null)) {
            particle.setX(anchor.x);
            particle.setY(anchor.y);
        }
    }
    
}
