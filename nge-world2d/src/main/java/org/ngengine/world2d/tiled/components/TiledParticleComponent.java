package org.ngengine.world2d.tiled.components;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;

import com.jme3.math.Vector2f;

import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledNetcodeFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

public class TiledParticleComponent extends AbstractComponent implements TiledEntityLogicFragment, TiledNetcodeFragment{
    private float lifespan = -1f;
    private float age = 0f;
    private TiledEntity followTarget;
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

    public TiledParticleComponent follow(TiledEntity target, float offsetX, float offsetY) {
        this.followTarget = target;
        this.followOffsetX = offsetX;
        this.followOffsetY = offsetY;
        updateFollowTarget();
        return this;
    }

    public void clearFollowTarget() {
        this.followTarget = null;
        this.followOffsetX = 0f;
        this.followOffsetY = 0f;
    }

    private void updateFollowTarget() {
        if (followTarget == null) {
            return;
        }
        CoordinateSystem cs = getInstanceOf(CoordinateSystem.class);
        TiledObjectEntity particle = getInstanceOf(TiledObjectEntity.class);
        if (cs == null || particle == null) {
            return;
        }
        cs.getCenterInGridSpace(followTarget, tmpFollowGrid);
        particle.setX(tmpFollowGrid.x + followOffsetX);
        particle.setY(tmpFollowGrid.y + followOffsetY);
    }
    
}
