package org.ngengine.demo.son;

import org.ngengine.auth.AuthSelectionWindow;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AppFragment;
import org.ngengine.components.fragments.ViewPortFragment;
import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.player.PlayerManagerComponent;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.jme3.util.TempVars;

public class HelloGameState implements Component<Object>, AppFragment, ViewPortFragment {
    private AppSettings settings;
    public HelloGameState(){

    }

    @Override
    public Object getSlot() {
        return "mainState";
    }

    @Override
    public void receiveApplication(Application app) {
        settings = app.getContext().getSettings();
    }


    @Override
    public void onEnable(ComponentManager fragmentManager, Runner runner, DataStoreProvider dataStoreProvider,
            boolean firstTime, Object slot) {

        PlayerManagerComponent playerManager = fragmentManager
                .getComponent(PlayerManagerComponent.class);

        // create an authentication strategy that toggles the lobby app state when a signer is available
        AuthStrategy authStrategy = Defaults.authStrategy(settings, (signer) -> {
            fragmentManager.enableComponent(LobbyGameState.class, signer);
        },playerManager);
        

        // open auth window
        NWindowManagerComponent windowManager = fragmentManager
                .getComponent(NWindowManagerComponent.class);
        windowManager.showWindow(AuthSelectionWindow.class, authStrategy);

    }

    @Override
    public void updateViewPort(ViewPort viewPort, float tpf) {
        TempVars vars = TempVars.get();
        try{
            float angles[] = vars.fADdU;
            Camera cam = viewPort.getCamera();
            cam.getRotation().toAngles(angles);
            angles[1] = FastMath.interpolateLinear(tpf, angles[1], FastMath.PI*0.1f );
            angles[0] = FastMath.interpolateLinear(tpf, angles[0], 0f);

            cam.setRotation(cam.getRotation().fromAngles(angles));
        } finally {
            vars.release();
        }
        

    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStoreProvider) {
    }

    @Override
    public void loadViewPortFilterPostprocessor(AssetManager assetManager, FilterPostProcessor fpp) {

    }
  
    
    
}
