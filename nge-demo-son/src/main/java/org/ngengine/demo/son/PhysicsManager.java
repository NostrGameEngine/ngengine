package org.ngengine.demo.son;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AppFragment;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;

public class PhysicsManager implements Component<Object>, AppFragment {
    private Application app;
    private BulletAppState physics;
 
    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime,
            Object arg) {
         physics = new BulletAppState();
        app.getStateManager().attach(physics);

    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        if (physics != null) {
            app.getStateManager().detach(physics);
            physics = null;
        }
    }

    public BulletAppState getPhysics() {
        return physics;
    }

    @Override
    public void receiveApplication(Application app) {
        this.app = app;
    }
    
}
