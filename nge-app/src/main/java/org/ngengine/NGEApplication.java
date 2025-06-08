/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetConfig;
import com.jme3.system.AppSettings;
import com.jme3.util.res.Resources;
import com.simsilica.lemur.GuiGlobals;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.auth.AuthSelectionWindow;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.jme3.AppComponentInitializer;
import org.ngengine.components.jme3.AppComponentLoader;
import org.ngengine.components.jme3.AppComponentUpdater;
import org.ngengine.components.jme3.AppViewPortComponentUpdater;
import org.ngengine.components.jme3.ComponentManagerAppState;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.win.NWindowManagerComponent;

public class NGEApplication {

    private static final Logger logger = Logger.getLogger(NGEApplication.class.getName());

    private final Jme3Application app;

    private static class Jme3Application extends SimpleApplication {

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

            try {
                AssetConfig.loadText(assetManager, Resources.getResource("org/ngengine/NGE.cfg"));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load NGE configuration file", e);
            }

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

        if (settings != null) {
            baseSettings.copyFrom(settings);
        }

        app =
            new Jme3Application(() -> {
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
            NWindowManagerComponent windowManager = componentManager.getComponent(NWindowManagerComponent.class);
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
