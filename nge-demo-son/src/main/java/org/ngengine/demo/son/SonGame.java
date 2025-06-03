package org.ngengine.demo.son;

import static org.lwjgl.glfw.GLFW.glfwGetMonitors;

import org.ngengine.AsyncAssetManager;
import org.ngengine.DevMode;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.win.NWindowManagerAppState;
import org.ngengine.gui.svg.SVGLoader;
import org.ngengine.network.LobbyManager;
import org.ngengine.player.PlayerManagerAppState;
import org.ngengine.runner.MainThreadRunner;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.SystemListener;
import com.jme3.system.JmeContext.Type;
import com.jme3.texture.Image.Format;
import com.jme3.util.SkyFactory;
import com.simsilica.lemur.GuiGlobals;
import com.jme3.input.ChaseCamera;

public class SonGame extends SimpleApplication {
    Spatial playerSpatial;
    ChaseCamera chaseCam;
    AudioNode backgroundMusic;

    LobbyManager lobbyManager;
    DirectionalLight dl;
    ToneMapFilter tonemap;
    LightScatteringFilter lightScattering;
    FogFilter fog;

    @Override
    public void simpleInitApp() {
        AsyncAssetManager assetManager = AsyncAssetManager.of(this.assetManager, this);
        assetManager.registerLoader(WebpImageLoader.class, "webp");

        flyCam.setEnabled(false);
        setLostFocusBehavior(LostFocusBehavior.Disabled);

        assetManager.registerLoader(SVGLoader.class, "svg");
        GuiGlobals.initialize(this);
        NGEStyle.installAndUse();
        stateManager.attach(new DevMode());

        DevMode.registerForReload(rootNode);

        ViewPort guiViewport = getGuiViewPort();
        Node guiNode = (Node) guiViewport.getScenes().get(0);

        int width = guiViewport.getCamera().getWidth();
        int height = guiViewport.getCamera().getHeight();

        // stateManager.attach(new FocusGuardAppState());
        stateManager.attach(new PlayerManagerAppState());
        stateManager.attach(new NWindowManagerAppState(guiNode, width, height, MainThreadRunner.of(this)));
        stateManager.attach(new LobbyAppState());
        stateManager.attach(new HelloAppState());
        stateManager.attach(new GameAppState());

        stateManager.getState(HelloAppState.class).show();

        inputManager.setCursorVisible(false);
        // flyCam.setEnabled(true);
        // flyCam.setMoveSpeed(1000f);

        // stateManager.attach(new OceanAppState());
        //

        {
            TextureKey key = new TextureKey("skies/alienSkyLOWEXP.png", true);
            key.setGenerateMips(false);
            assetManager.loadTextureAsync(key, (skyTextyre, err) -> {
                Spatial sky = SkyFactory.createSky(assetManager, skyTextyre,
                        SkyFactory.EnvMapType.EquirectMap);
                EnvironmentProbeControl.tagGlobal(sky);
                rootNode.attachChild(sky);

                EnvironmentProbeControl envProbe = new EnvironmentProbeControl(assetManager, 256);
                rootNode.addControl(envProbe);
            });
        }

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.3f, -0.1f, -0.9f).normalizeLocal());
        dl.setColor(ColorRGBA.White.mult(2.3f));
        rootNode.addLight(dl);
        tonemap = new ToneMapFilter(Vector3f.UNIT_XYZ.mult(1f));
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.setFrameBufferDepthFormat(Format.Depth24Stencil8);
        fpp.setNumSamples(4);
        fpp.addFilter(tonemap);
        viewPort.addProcessor(fpp);

        // DirectionalLightShadowFilter shadowFilter = new DirectionalLightShadowFilter(assetManager, 1024,
        // 3);
        // shadowFilter.setLight(dl);
        // shadowFilter.setEnabled(true);
        // shadowFilter.setShadowIntensity(0.5f);
        // shadowFilter.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        // fpp.addFilter(shadowFilter);

        lightScattering = new LightScatteringFilter(dl.getDirection().mult(-300));

        fog = new FogFilter();
        fog.setFogDensity(0.4f);
        fog.setFogDistance(200f);
        fog.setFogColor(new ColorRGBA(35.0f / 255.0f, 0.0f, 110f / 255.0f, 1f));
        tonemap.setWhitePoint(Vector3f.UNIT_XYZ.mult(4f));
        lightScattering.setLightDensity(4.5f);
        fpp.addFilter(fog);

        fpp.addFilter(lightScattering);

        // // --- Input mappings ---

        // //
        {
            AudioKey audioKey = new AudioKey("Sounds/fato_shadow_-_lunar_strings.ogg", false, false);
            assetManager.loadAudioAsync(audioKey, (audioData, err) -> {
                backgroundMusic = new AudioNode(audioData, audioKey);
                backgroundMusic.setLooping(true);
                backgroundMusic.setPositional(false);
                backgroundMusic.setVolume(0.4f);
                backgroundMusic.play();
                rootNode.attachChild(backgroundMusic);
            });
        }

    }

    @Override
    public void simpleUpdate(float tpf) {


        lightScattering.setLightPosition(getCamera().getLocation().add(dl.getDirection().mult(-1000)));
    }

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
        // settings.setFrameRate(60);
        settings.setGraphicsDebug(false);
        settings.setTitle("Nostr Game Engine Demo");

        SonGame app = new SonGame();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

}
