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
 * the BSD 3-Clause License. 
 */
package org.ngengine;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetConfig;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import com.jme3.util.res.Resources;
import com.simsilica.lemur.GuiGlobals;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.ads.ImmersiveAdComponent;
import org.ngengine.auth.AuthSelectionWindow;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.components.ComponentManager;

import org.ngengine.components.jme3.ComponentManagerAppState;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

public class NGEApplication {

    private static final Logger logger = Logger.getLogger(NGEApplication.class.getName());

    private final Jme3Application app;
    private final AppSettings settings;
    private final NGEAppSettings ngeSettings;
    // private final String defaultAppId = "npub146wutmuxfmnlx9fcty0lkns2rpwhnl57kpes26mmt4hygalsakrsdllryz";
 

    public NGEAppSettings getAppSettings() {
        return ngeSettings;
    }

    public static class Jme3Application extends SimpleApplication {

        private final Runnable ready;
        private final NGEApplication ngeapp;

        public Jme3Application(NGEApplication ngeapp, Runnable ready) {
            super();
            this.ready = ready;
            this.ngeapp = ngeapp;
        }

        public void setFlyCamEnabled(boolean enabled) {
            flyCam.setMoveSpeed(200);
            flyCam.setEnabled(enabled);
        }

        @Override
        public void simpleInitApp() {
            getRenderManager().setSinglePassLightBatchSize(16);

            flyCam.setEnabled(false);

            ComponentManagerAppState cmng = new ComponentManagerAppState(ngeapp.getAppSettings());

            AsyncAssetManager assetManager = AsyncAssetManager.of(this.assetManager, this);

            try {
                if (
                    JmeSystem.getPlatform().getOs() != Platform.Os.Web
                    && (JmeSystem.getPlatform().getOs() != Platform.Os.MacOS || !JmeSystem.getPlatform().isGraalVM())
                ) {
                    AssetConfig.loadText(assetManager, Resources.getResource("com/jme3/asset/Awt.cfg"));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load NGE configuration file", e);
            }

            GuiGlobals.initialize(this);
            stateManager.attach(new DevMode());

            getStateManager().attach(cmng);

            DevMode.registerForReload(rootNode);

            this.ready.run();
        }
    }

    NGEApplication(NGEAppSettings settings, Consumer<NGEApplication> onReady) {
        if(settings==null){
            settings = new NGEAppSettings();
        }
        this.ngeSettings= settings;

        AppSettings baseSettings = new AppSettings(true);
        baseSettings.setRenderer(AppSettings.LWJGL_OPENGL32);
        baseSettings.copyFrom(settings.getJmeAppSettings());
        



        this.settings = baseSettings;
        logger.info("Starting app " + settings.getAppId() + " with settings " + baseSettings);

        app =
            new Jme3Application(
                this,
                () -> {
                    onReady.accept(this);
                }
            );
        app.setSettings(baseSettings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
    }

    public AppSettings getSettings() {
        return settings;
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

    public ImmersiveAdComponent enableAds() {
        return enableAds(null, null);
    }

    public ImmersiveAdComponent enableAds(List<String> relays) {
        return enableAds(null, relays);
    }

    public ImmersiveAdComponent enableAds(NostrPrivateKey userAdsKey) {
        return enableAds(userAdsKey, null);
    }

    public ImmersiveAdComponent enableAds(NostrPrivateKey userAdsKey, Collection<String> relays) {
        NostrPublicKey appKey = ngeSettings.getAppId();
        ImmersiveAdComponent ads = getComponentManager().getComponent(ImmersiveAdComponent.class);
        if (ads == null) {
            getComponentManager()
                .addAndEnableComponent(
                    ads =
                        new ImmersiveAdComponent(
                            relays != null && !relays.isEmpty() ? relays : ngeSettings.getNostrRelays().get("ads"),
                            appKey,
                            userAdsKey
                        )
                );
        }
        return ads;
    }

    public void disableAds() {
        getComponentManager().removeComponent(getComponentManager().getComponent(ImmersiveAdComponent.class));
    }

    public interface NGEAppRunner extends Runnable {
        NGEApplication app();

        NGEApplication start();

        default void run() {
            start();
        }
    }

    public static NGEAppRunner createApp(NGEAppSettings settings, Consumer<NGEApplication> onReady) {
        NGEApplication app = new NGEApplication(settings, onReady);
        return new NGEAppRunner() {
            @Override
            public NGEApplication app() {
                return app;
            }

            @Override
            public NGEApplication start() {
                app.start();
                return app;
            }
        };
    }

    

 
    public static NGEAppRunner createApp(Consumer<NGEApplication> onReady) {
        return createApp( null, onReady);
    }
}
