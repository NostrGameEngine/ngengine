package org.ngengine.demo.son;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.demo.son.gui.LobbyManagerWindow;
import org.ngengine.demo.son.gui.LobbyManagerWindowArg;
import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.win.NWindowListener;
import org.ngengine.gui.win.NWindowManagerAppState;
import org.ngengine.network.Lobby;
import org.ngengine.network.LobbyManager;
import org.ngengine.network.LocalLobby;
import org.ngengine.network.P2PChannel;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.runner.MainThreadRunner;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.TempVars;

public class LobbyAppState extends NGEAppState{
    private static final Logger log = Logger.getLogger(LobbyAppState.class.getName());
    private LobbyManager mng;

    public LobbyAppState() {
        setUnit("mainState");
        setEnabled(false);
    }

    
    public void show(NostrSigner signer) {
        log.info("Showing LobbyAppState");

        mng = new LobbyManager(
            signer, Settings.GAME_NAME, Settings.GAME_VERSION, Settings.RELAYS,
            Settings.TURN_SERVER,
            MainThreadRunner.of(getApplication())        
        );

        setEnabled(false);

        setEnabled(true);
        
    }

 

    public void join(Lobby lobby, String passphrase) throws Exception {
        P2PChannel chan = mng.connectToLobby(lobby, passphrase);
        GameAppState game = getStateManager().getState(GameAppState.class);
        game.show(chan);
         
    }

   

    @Override
    protected void cleanup(Application app) {
       
    }

    @Override
    protected void onEnable() {
        NWindowManagerAppState windowManager = getStateManager().getState(NWindowManagerAppState.class);
        LobbyManagerWindowArg arg = new LobbyManagerWindowArg(mng, (chan)->{
            getStateManager().getState(GameAppState.class).show(chan);
        });

        windowManager.showWindow(LobbyManagerWindow.class, arg);

       
    }

    @Override
    protected void onDisable() {
        
    }
    
    @Override 
    public void update(float tpf) {
        super.update(tpf);
        TempVars vars = TempVars.get();
        try{
            float angles[] = vars.fADdU;
            Camera cam = getApplication().getCamera();
            cam.getRotation().toAngles(angles);
            angles[1] = FastMath.interpolateLinear(tpf, angles[1], FastMath.PI * -0.9f);
            // angles[0] = FastMath.interpolateLinear(tpf, angles[0], FastMath.PI * 0f);
            cam.setRotation(cam.getRotation().fromAngles(angles));
        } finally {
            vars.release();
        }
        

    }
}
