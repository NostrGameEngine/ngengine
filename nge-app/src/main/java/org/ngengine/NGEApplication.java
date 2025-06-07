package org.ngengine;

import java.util.function.Consumer;

import org.ngengine.auth.AuthSelectionWindow;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.AppComponentInitializer;
import org.ngengine.components.jme3.AppComponentLoader;
import org.ngengine.components.jme3.AppComponentUpdater;
import org.ngengine.components.jme3.AppViewPortComponentUpdater;
import org.ngengine.components.jme3.ComponentManagerAppState;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.svg.SVGLoader;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.player.PlayerManagerComponent;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;

public class NGEApplication {
    private final Jme3Application app;
    private static class Jme3Application extends SimpleApplication{
        private final Runnable ready;
        
        public Jme3Application(Runnable ready) {
            super();
            this.ready = ready;
        }
        @Override
        public void simpleInitApp() {
            flyCam.setEnabled(false);
            
            ComponentManagerAppState cmng = new ComponentManagerAppState(this);

            AsyncAssetManager assetManager = AsyncAssetManager.of(this.assetManager, this);
            assetManager.registerLoader(WebpImageLoader.class, "webp");
            assetManager.registerLoader(SVGLoader.class, "svg");

            GuiGlobals.initialize(this);
            NGEStyle.installAndUse();
            stateManager.attach(new DevMode());

            getStateManager().attach(cmng);
            cmng.addInitializer(new AppComponentInitializer(this));
            cmng.addUpdater(new AppViewPortComponentUpdater(this));
            cmng.addUpdater(new AppComponentUpdater(this));
            cmng.addLoader(new AppComponentLoader(this));

            DevMode.registerForReload(rootNode);

            this.ready.run();

        }

    }

    NGEApplication(Consumer<NGEApplication> onReady) {
        this(null, onReady);
    }

    NGEApplication(AppSettings settings, Consumer<NGEApplication> onReady) {
        AppSettings baseSettings = new AppSettings(true);
        baseSettings.setRenderer(AppSettings.LWJGL_OPENGL32);
        baseSettings.setWidth(1280);
        baseSettings.setHeight(720);
        baseSettings.setGammaCorrection(true);
        baseSettings.setSamples(4);
        baseSettings.setStencilBits(8);
        baseSettings.setDepthBits(24);
        baseSettings.setVSync(true);
        baseSettings.setTitle("Nostr Game Engine");

        if(settings!=null){
            baseSettings.copyFrom(settings);
        }

        app = new Jme3Application(() -> {
            onReady.accept(this);
        });
        app.setSettings(baseSettings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
    }

    public Jme3Application getJme3App() {
        return app;
    }

    public ComponentManager getComponentManager() {
        return app.getStateManager().getState(ComponentManagerAppState.class);
    }

    public void requestAuth(AuthStrategy stategy) {
        app.enqueue(() -> {
            ComponentManager componentManager = getComponentManager();
            NWindowManagerComponent windowManager = componentManager
                    .getComponent(NWindowManagerComponent.class);
            if (windowManager == null || !componentManager.isComponentEnabled(windowManager)) {
                requestAuth(stategy);
            } else {
                windowManager.showWindow(AuthSelectionWindow.class, stategy);
            }
        });
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public void start() {
        if (app != null) {
            app.start();
        }
    }



    public static Runnable createApp(AppSettings settings, Consumer<NGEApplication> onReady) {

        NGEApplication app = new NGEApplication(settings, onReady);
        return () -> app.start();
    }

    public static Runnable createApp(Consumer<NGEApplication> onReady) {

        NGEApplication app = new NGEApplication(onReady);
        return () -> app.start();
    }
    
}
