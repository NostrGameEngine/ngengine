package org.ngengine.demo.son;

import java.util.function.Consumer;

import org.ngengine.AsyncAssetManager;
import org.ngengine.DevMode;
import org.ngengine.components.initializers.AppComponentInitializer;
import org.ngengine.components.jme3.ComponentManagerAppState;
import org.ngengine.components.updaters.AppComponentUpdater;
import org.ngengine.components.updaters.AppViewPortComponentUpdater;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.svg.SVGLoader;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.simsilica.lemur.GuiGlobals;

public class NGEApplication {
    private final Jme3Application app;
    private static class Jme3Application extends SimpleApplication{
        private Consumer<ComponentManagerAppState> ready;
        
        public Jme3Application(Consumer<ComponentManagerAppState> ready) {
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


            DevMode.registerForReload(rootNode);

            this.ready.accept(cmng);

        }

    }

    public NGEApplication(Consumer<ComponentManagerAppState> onReady){
        this(null, onReady);
    }

    public NGEApplication(AppSettings settings, Consumer<ComponentManagerAppState> onReady) {
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

        app = new Jme3Application(onReady);
        app.setSettings(baseSettings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
    }

    public Jme3Application getJme3App() {
        return app;
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



    
}
