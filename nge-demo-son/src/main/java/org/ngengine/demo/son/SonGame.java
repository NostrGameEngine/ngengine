package org.ngengine.demo.son;

import org.ngengine.NGEApplication;
import org.ngengine.demo.son.ocean.OceanAppState;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.player.PlayerManagerComponent;
import com.jme3.system.AppSettings;

public class SonGame {

    // mac needs to start with -XstartOnFirstThread -Djava.awt.headless=true
    public static void main(String[] args) throws InterruptedException {

        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setGammaCorrection(true);
        settings.setSamples(4);
        settings.setStencilBits(8);
        settings.setDepthBits(24);
        settings.setVSync(true);
        settings.setGraphicsDebug(false);
        settings.setTitle("Nostr Game Engine Demo");

        NGEApplication app = new NGEApplication(settings, mng -> {

            mng.addAndEnableComponent(new BaseEnvironment());
            mng.addAndEnableComponent(new NWindowManagerComponent());
            mng.addAndEnableComponent(new PlayerManagerComponent());
            mng.addAndEnableComponent(new PhysicsManager());

            mng.addComponent(new OceanAppState());
            mng.addComponent(new LobbyGameState(), NWindowManagerComponent.class,
                    PlayerManagerComponent.class);
            mng.addComponent(new PlayGameState(), NWindowManagerComponent.class, PlayerManagerComponent.class,
                    OceanAppState.class, PhysicsManager.class);
            mng.addComponent(new HelloGameState(), BaseEnvironment.class, NWindowManagerComponent.class,
                    PlayerManagerComponent.class);

            mng.enableComponent(HelloGameState.class);
        });

        app.start();
    }

}
