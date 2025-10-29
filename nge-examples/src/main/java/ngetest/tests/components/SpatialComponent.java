package ngetest.tests.components;

import org.ngengine.ViewPortManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.SpatialLogicFragment;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

public class SpatialComponent implements Component, SpatialLogicFragment, InputHandlerFragment{
    private Spatial spatial;

    @Override
    public void updateSpatialLogic(ComponentManager mng, float tpf, Spatial sp) {
        spatial = sp;
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime) {
        System.out.println("SpatialComponent enabled");
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
       System.out.println("SpatialComponent disabled");
    }

    @Override
    public Component newInstance() {
       return new SpatialComponent();
    }
 

    @Override
    public void onMouseButtonEvent(ComponentManager mng, MouseButtonEvent evt) {
        System.out.println("Mouse button event: "+evt);
        if (spatial == null)
            return;

        int x = evt.getX();
        int y = evt.getY();
        ViewPortManager vpm = mng.getGlobalInstance(ViewPortManager.class);
        ViewPort vp = vpm.getMainSceneViewPort();
        
        // move toward click
        Vector2f center = new Vector2f(vp.getCamera().getWidth()/2, vp.getCamera().getHeight()/2);
        center.subtractLocal(x, y).normalizeLocal().multLocal(-0.1f);
        spatial.move(center.x, center.y, 0);
        
        
    }

    
}
