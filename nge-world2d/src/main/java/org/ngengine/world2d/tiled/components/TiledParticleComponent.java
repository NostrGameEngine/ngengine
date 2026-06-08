package org.ngengine.world2d.tiled.components;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;

import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledNetcodeFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

public class TiledParticleComponent extends AbstractComponent implements TiledEntityLogicFragment, TiledNetcodeFragment{
    private float lifespan = -1f;
    private float age = 0f;

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
        age+=tpf;
        if(isDead()){
            TiledObjectEntity entity = getInstanceOf(TiledObjectEntity.class);
            entity.getObjectGroup().remove(entity);
        }
        
    }

    public boolean isDead(){
        return lifespan>0 && age>=lifespan;
    }
    
}
