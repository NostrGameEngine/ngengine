package org.ngengine.demo.son;

import org.ngengine.auth.AuthSelectionWindow;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.win.NWindowManagerAppState;
import org.ngengine.player.PlayerManagerAppState;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.jme3.util.TempVars;

public class HelloAppState extends NGEAppState{
    
    public HelloAppState(){
        setUnit("mainState");
        setEnabled(false);
    }

   
    public void show() {
        setEnabled(true);
    }

    @Override
    protected void onEnable() {

        AppSettings settings = getApplication().getContext().getSettings();
        NWindowManagerAppState windowManager = getStateManager().getState(NWindowManagerAppState.class, true);
        LobbyAppState lobbyAppState = getStateManager().getState(LobbyAppState.class, true);
        PlayerManagerAppState playerManager = getStateManager().getState(PlayerManagerAppState.class, true);

        // create an authentication strategy that toggles the lobby app state when a signer is available
        AuthStrategy authStrategy = Defaults.authStrategy(settings, (signer) -> {
            lobbyAppState.show(signer);
        },playerManager);
        

        // open auth window
        windowManager.showWindow(AuthSelectionWindow.class, authStrategy);
        
 
        
      
 

        

    }

    @Override
    protected void onDisable() {
        // getStateManager().detach(ocean);
        // backgroundNode.removeFromParent();
    }

    @Override 
    public void update(float tpf) {
        super.update(tpf);
        TempVars vars = TempVars.get();
        try{
            float angles[] = vars.fADdU;
            Camera cam = getApplication().getCamera();
            cam.getRotation().toAngles(angles);
            angles[1] = FastMath.interpolateLinear(tpf, angles[1], FastMath.PI*0.1f );
            angles[0] = FastMath.interpolateLinear(tpf, angles[0], 0f);

            cam.setRotation(cam.getRotation().fromAngles(angles));
        } finally {
            vars.release();
        }
        

    }
    
}
