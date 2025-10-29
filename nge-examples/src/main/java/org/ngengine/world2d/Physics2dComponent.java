package org.ngengine.world2d;

import org.example.Box2DPhysicsFactory;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.SpatialLogicFragment;
import org.ngengine.platform.NGEUtils;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.scene.Spatial;

import ngetest.tests.world2d.TiledWorld2DComponent;

public class Physics2dComponent implements TiledWorldComponent, SpatialLogicFragment{
    private ComponentManager mng;
    private Body body;

    public TiledWorld2DComponent getTiledWorld(){
        return getTiledWorld(mng);
    }

    @Override
    public void updateSpatialLogic(ComponentManager mng, float tpf, Spatial sp) {
     
        
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime) {
        this.mng = mng;   
        TiledWorld2DComponent world = getTiledWorld();
        World phy = world.getPhysicsWorld();
        if(phy!=null){
            

            PhysicsDef def = Box2DPhysicsFactory.createBody(world, mng, sp);
            
            body = phy.createBody(bodydef);
            
        }
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        TiledWorld2DComponent world = getTiledWorld();
        World phy = world.getPhysicsWorld();
        if(phy!=null && body!=null){
            phy.destroyBody(body);
            body = null;
        }
    }

    @Override
    public Component newInstance() {
        return new Physics2dComponent();
    }

    
}
