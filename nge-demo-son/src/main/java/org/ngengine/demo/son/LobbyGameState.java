package org.ngengine.demo.son;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AppFragment;
import org.ngengine.components.fragments.ViewPortFragment;
import org.ngengine.demo.son.gui.LobbyManagerWindow;
import org.ngengine.demo.son.gui.LobbyManagerWindowArg;
import org.ngengine.demo.son.ocean.OceanAppState;
import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.win.NWindowListener;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.network.Lobby;
import org.ngengine.network.LobbyManager;
import org.ngengine.network.LocalLobby;
import org.ngengine.network.P2PChannel;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.TempVars;

public class LobbyGameState implements Component<NostrSigner>, ViewPortFragment {
    private static final Logger log = Logger.getLogger(LobbyGameState.class.getName());
    private LobbyManager mng;

    public LobbyGameState() {
    }

    @Override
    public Object getSlot() {
        return "mainState";
    }

    @Override
    public void onEnable(ComponentManager componentMng, Runner runner, DataStoreProvider dataStoreProvider,
            boolean firstTime, NostrSigner signer) {
        mng = new LobbyManager(signer, Settings.GAME_NAME, Settings.GAME_VERSION, Settings.RELAYS,
                Settings.TURN_SERVER, runner);

        NWindowManagerComponent windowManager = componentMng
                .getComponentByType(NWindowManagerComponent.class);
        windowManager.showWindow(LobbyManagerWindow.class, new LobbyManagerWindowArg(mng, (chan) -> {

            componentMng.enableComponent(PlayGameState.class, chan);
        }));
    }

    @Override
    public void updateViewPort(ViewPort vp, float tpf) {
        TempVars vars = TempVars.get();
        try{
            float angles[] = vars.fADdU;
            Camera cam = vp.getCamera();
            cam.getRotation().toAngles(angles);
            angles[1] = FastMath.interpolateLinear(tpf, angles[1], FastMath.PI * -0.9f);
            // angles[0] = FastMath.interpolateLinear(tpf, angles[0], FastMath.PI * 0f);
            cam.setRotation(cam.getRotation().fromAngles(angles));
        } finally {
            vars.release();
        }
    }
 

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {

    }

}
