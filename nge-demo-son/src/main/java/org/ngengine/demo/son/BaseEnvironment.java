package org.ngengine.demo.son;

import org.ngengine.AsyncAssetManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.ViewPortFragment;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
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
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

public class BaseEnvironment implements Component<Object>, AsyncAssetLoadingFragment, ViewPortFragment {
    private Spatial sky ;
    private AudioNode backgroundMusic;
    private AssetManager assetManager;
    private ViewPort viewPort;

    @Override
    public void loadAssetsAsync(AsyncAssetManager assetManager) {
        this.assetManager = assetManager;

        TextureKey key = new TextureKey("skies/alienSkyLOWEXP.png", true);
        key.setGenerateMips(false);
        Texture skyTextyre = assetManager.loadTexture(key);
        sky = SkyFactory.createSky(assetManager, skyTextyre, SkyFactory.EnvMapType.EquirectMap);
        

        AudioKey audioKey = new AudioKey("Sounds/fato_shadow_-_lunar_strings.ogg", false, false);
        AudioData audioData = assetManager.loadAudio(audioKey);
        backgroundMusic = new AudioNode(audioData, audioKey);
        backgroundMusic.setLooping(true);
        backgroundMusic.setPositional(false);
        backgroundMusic.setVolume(0.4f);
          
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime,
            Object arg) {
        
        // music
        backgroundMusic.play();

        // sky
        Node rootNode = getRootNode(viewPort);
        rootNode.attachChild(backgroundMusic);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tagGlobal(sky);
        rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));

        // light
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.3f, -0.1f, -0.9f).normalizeLocal());
        dl.setColor(ColorRGBA.White.mult(2.3f));
        rootNode.addLight(dl);

        // post processing
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.setFrameBufferDepthFormat(Format.Depth24Stencil8);
        fpp.setNumSamples(4);
        viewPort.addProcessor(fpp);

        ToneMapFilter tonemap = new ToneMapFilter(Vector3f.UNIT_XYZ.mult(4f));
        fpp.addFilter(tonemap);

        LightScatteringFilter lightScattering = new LightScatteringFilter(dl.getDirection().mult(-300));
        lightScattering
                .setLightPosition(viewPort.getCamera().getLocation().add(dl.getDirection().mult(-1000)));
        fpp.addFilter(lightScattering);

        FogFilter fog = new FogFilter();
        fog.setFogDensity(0.4f);
        fog.setFogDistance(200f);
        fog.setFogColor(new ColorRGBA(35.0f / 255.0f, 0.0f, 110f / 255.0f, 1f));
        lightScattering.setLightDensity(4.5f);
        fpp.addFilter(fog);
   
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        
    }

    @Override
    public void receiveViewPort(ViewPort viewPort) {
        this.viewPort = viewPort;        
    }

    @Override
    public void updateViewPort(ViewPort viewPort, float tpf) {
         
    }
    
}
