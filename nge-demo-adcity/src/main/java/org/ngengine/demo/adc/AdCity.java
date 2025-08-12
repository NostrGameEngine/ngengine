package org.ngengine.demo.adc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

import org.ngengine.AsyncAssetManager;
import org.ngengine.ads.ImmersiveAdComponent;
import org.ngengine.ads.ImmersiveAdControl;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AppFragment;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.MainViewPortFragment;
import org.ngengine.gui.components.NLabel;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.gui.win.std.NHud;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStore;
import org.ngengine.store.DataStoreProvider;

import com.jme3.anim.tween.action.Action;
import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.KHRToneMapFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.post.filters.SoftBloomFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.SpotLightShadowFilter;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;

public class AdCity implements Component<Object>, LogicFragment, AsyncAssetLoadingFragment, MainViewPortFragment, AppFragment, InputHandlerFragment {
    private Spatial map;
    private BulletAppState physics;
    private Application app;
    private ViewPort mainViewPort;
    private Spatial sky;
    private AudioNode backgroundMusic;
    private AudioNode footstepSound;
    private AudioNode jumpSound;
    private AssetManager assetManager;
    private InputManager inputManager;
    private float lastFootstepTime = 0f;

    @Override
    public void loadAssetsAsync(AsyncAssetManager assetManager, DataStore cache, Consumer<Object> preload) {
        this.assetManager = assetManager;
    
        
        TextureKey key = new TextureKey("adc/night-sky.png", true);
        key.setGenerateMips(false);
        Texture skyTextyre = assetManager.loadTexture(key);
        sky = SkyFactory.createSky(assetManager, skyTextyre, SkyFactory.EnvMapType.EquirectMap);
        sky.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));
        EnvironmentProbeControl.tagGlobal(sky);

        AudioKey audioKey = new AudioKey("adc/e.q._city.ogg", false, false);
        AudioData audioData = assetManager.loadAudio(audioKey);
        backgroundMusic = new AudioNode(audioData, audioKey);
        backgroundMusic.setLooping(true);
        backgroundMusic.setPositional(false);
        backgroundMusic.setVolume(0.4f);

        AudioKey footstepKey = new AudioKey("adc/footstep.ogg", false, false);
        AudioData footstepData = assetManager.loadAudio(footstepKey);
        footstepSound = new AudioNode(footstepData, footstepKey);
        footstepSound.setLooping(false);
        footstepSound.setPositional(false);
        footstepSound.setVolume(0.5f);

          
        AudioKey jumpKey = new AudioKey("adc/jump.ogg", false, false);
        AudioData jumpData = assetManager.loadAudio(jumpKey);
        jumpSound = new AudioNode(jumpData, jumpKey);
        jumpSound.setLooping(false);
        jumpSound.setPositional(false);
        jumpSound.setVolume(0.5f);

        
        // try{
        //     if(cache.exists("adc/city2")){
        //         map = (Spatial)cache.read("adc/city2");
        //     }
        // } catch(Exception e){
        //     e.printStackTrace();
        // }
        
        if(map==null){
            System.out.println("AdCity: Loading map from assets...");
            map = assetManager.loadModel("adc/city/city.gltf");
            CollisionShape shp = CollisionShapeFactory.createMeshShape(map);
            RigidBodyControl rb = new RigidBodyControl(shp, 0);
            map.addControl(rb);
            rb.setFriction(1f);
            // try {
            //     cache.write("adc/city2", map);
            // } catch (IOException e) {
            //     e.printStackTrace();
            // }
        } else {
            System.out.println("AdCity: Map loaded from cache.");
        }
         
        preload.accept(map);



    }



    


    @Override
    public void receiveInputManager(InputManager inputManager) {
        this.inputManager = inputManager;

    }
  
    @Override
    public void receiveApplication(Application app) {
        this.app = app;
    }

    @Override
    public void receiveMainViewPort(ViewPort viewPort) {
        this.mainViewPort = viewPort;
    }


    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime, Object arg) {
        physics = new BulletAppState();
        physics.setThreadingType(ThreadingType.PARALLEL);
        app.getStateManager().attach(physics);
        app.getRenderManager().setSinglePassLightBatchSize(16);
        Node rootNode = getRootNode(mainViewPort);
        rootNode.attachChild(map);
        rootNode.attachChild(sky);
        rootNode.attachChild(backgroundMusic);
        rootNode.attachChild(footstepSound);
        rootNode.attachChild(jumpSound);
        backgroundMusic.play();

        
        
        int resolution = 1024; // Resolution of the environment probe
        rootNode.addControl(new EnvironmentProbeControl(assetManager, resolution));

        // mainViewPort.getCamera().setFrustumPerspective(
            // 55f, (float)mainViewPort.getCamera().getWidth()/(float)mainViewPort.getCamera().getHeight(),0.001f, 1000f);
        // mainViewPort.getCamera().lookAt(map.getWorldTranslation(), Vector3f.UNIT_Y);
        System.out.println("AdCity: Map loaded and added to the scene.");
        physics.getPhysicsSpace().add(map);       
        System.out.println("AdCity: Physics control added to the map.");

        inputManager.setCursorVisible(false);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.setFrameBufferDepthFormat(Format.Depth24Stencil8);
        fpp.setNumSamples(2);
        mainViewPort.addProcessor(fpp);

        // map.depthFirstTraversal(sx->{
        //     LightList lights = sx.getLocalLightList();
        //     for(Light l:lights){
        //         if(l instanceof SpotLight){
        //             SpotLight sl = (SpotLight) l;
        //             SpotLightShadowFilter slsf = new SpotLightShadowFilter(assetManager, 64);
        //             slsf.setLight(sl);
        //             slsf.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
        //             slsf.setEnabled(true);
        //             slsf.setShadowIntensity(.3f);
        //             slsf.setShadowZFadeLength(21);
        //             slsf.setShadowCompareMode(CompareMode.Hardware);
        //             slsf.setRenderBackFacesShadows(false);
        //             fpp.addFilter(slsf);
        //         }
        //     }
         
        // });

        // map.setShadowMode(ShadowMode.CastAndReceive);
    

    
        fog = new FogFilter();
        fog.setFogDensity(0.4f);
        fog.setFogDistance(20f);
        fog.setFogColor(new ColorRGBA(15.0f / 255.0f, 0.0f, 110f / 255.0f, 1f));
        fpp.addFilter(fog);

        ssaoFilter = new SSAOFilter(2.9299974f,25f,5.8100376f,0.091000035f);
        ssaoFilter.setApproximateNormals(true);
        fpp.addFilter(ssaoFilter);

        tonemap = new KHRToneMapFilter();
        fpp.addFilter(tonemap);
            
        bloom=new BloomFilter();
        bloom.setDownSamplingFactor(2);
        bloom.setBlurScale(1.37f);
        bloom.setExposurePower(3.30f);
        bloom.setExposureCutOff(0.2f);
        bloom.setBloomIntensity(2.45f);
            
        fpp.addFilter(bloom);

         player = new Node("Player");
        player.setLocalTranslation(new Vector3f(-33.665176f, 2.3f, -23.83905f));
        player.setLocalRotation(new Quaternion(-0.10938066f, -0.004970028f, -5.469133f, 0.9939874f));
        characterControl = new CharacterControl(new CapsuleCollisionShape(0.5f, 1.f), .1f);
        player.addControl(characterControl);
        rootNode.attachChild(player);

        physics.getPhysicsSpace().add(characterControl);

        ImmersiveAdControl adControl = map.getControl(ImmersiveAdControl.class);
        if(adControl==null){
            adControl = new ImmersiveAdControl(assetManager);
            map.addControl(adControl);
            mng.getComponent(ImmersiveAdComponent.class).register(adControl);
        }

        NWindowManagerComponent windowManager = mng.getComponent(NWindowManagerComponent.class);

        windowManager.showWindow(
            NHud.class,
            (win, err) -> {     
                win.setFitContent(false);           
                win.setFullscreen(true);
                NLabel crossair = new NLabel("+");
                crossair.setTextVAlignment(VAlignment.Center);
                crossair.setTextHAlignment(HAlignment.Center);
                win.getCenter().addChild(crossair);                
            }
        );

    }
    
    Node player;
    CharacterControl characterControl;
    BloomFilter bloom;
    FogFilter fog;
    SSAOFilter ssaoFilter;
    KHRToneMapFilter tonemap;
    
    @Override
    public void updateMainViewPort(ViewPort viewPort, float tpf)  {
        ssaoFilter.setIntensity(33f);
        ssaoFilter.setScale(1f);
        ssaoFilter.setBias(0.29f);
        ssaoFilter.setApproximateNormals(true);
        ssaoFilter.setSampleRadius(2f);
        fog.setFogDistance(210f);
        fog.setFogDensity(0.9f);
        tonemap.setGamma(new Vector3f(0.9f,0.9f,0.9f));
        tonemap.setExposure(new Vector3f(0.6f,0.6f,1.f));
                mainViewPort.getCamera().setLocation(characterControl.getPhysicsLocation());
        mainViewPort.getCamera().lookAtDirection(characterControl.getViewDirection(), Vector3f.UNIT_Y);

        // mainViewPort.getCamera().setFrustumPerspective(
        //     45f, (float)mainViewPort.getCamera().getWidth()/(float)mainViewPort.getCamera().getHeight(),0.f, 10000f);
    }


    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        map.removeFromParent();
        sky.removeFromParent();
        backgroundMusic.removeFromParent();
        backgroundMusic.stop();
        app.getStateManager().detach(physics);
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if(evt.getKeyCode() == KeyInput.KEY_W) {
            up = evt.isPressed();
        } else if(evt.getKeyCode() == KeyInput.KEY_S) {
            down = evt.isPressed();
        } else if(evt.getKeyCode() == KeyInput.KEY_A) {
            left = evt.isPressed();
        } else if(evt.getKeyCode() == KeyInput.KEY_D) {
            right = evt.isPressed();
        }  else if(evt.getKeyCode() == KeyInput.KEY_SPACE) {
            jump = evt.isPressed();
        }  
    }

    Vector3f walkDirection = new Vector3f();
    boolean left = false;
    boolean right = false;
    boolean up = false;
    boolean down = false;
    boolean jump = false;

    @Override
    public void updateAppLogic(float tpf){
        Camera cam = mainViewPort.getCamera();
           Vector3f camDir = cam.getDirection().clone().multLocal(0.1f);
        Vector3f camLeft = cam.getLeft().clone().multLocal(0.1f);
        camDir.y = 0;
        camLeft.y = 0;
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        if (jump){
            if(characterControl.onGround()){
               jumpSound.playInstance();
            }
            characterControl.jump();
        }
 
        characterControl.setWalkDirection(walkDirection);

        if(walkDirection.length() > 0f&&characterControl.onGround()) {
            lastFootstepTime+= tpf;
            if(lastFootstepTime > 0.3f) {
                System.out.println("Playing footstep sound");
                footstepSound.setPitch(1f-FastMath.nextRandomFloat()*0.1f);
                footstepSound.setVolume(0.3f-FastMath.nextRandomFloat()*0.1f);
                footstepSound.playInstance();
                lastFootstepTime = 0f;
            }
        }
    }

    // Add these as class fields
    private float currentYaw = 0;
    private float currentPitch = 0;
    private static final float MAX_PITCH = FastMath.HALF_PI - 0.1f; // ~89 degrees

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        float sensitivity = 0.0005f;
        
        
        // Update rotation angles
        currentYaw += -(float)evt.getDX() * sensitivity;
        currentPitch += -(float)evt.getDY() * sensitivity;
        
        // Limit pitch to prevent flipping
        currentPitch = FastMath.clamp(currentPitch, -MAX_PITCH, MAX_PITCH);
        
        // Create rotation quaternion from yaw angle (horizontal only)
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(currentPitch, currentYaw, 0);
        
        // Set character direction based on yaw only (horizontal movement)
        Vector3f viewDir = new Vector3f(0, 0, 1); // Forward vector
        viewDir = rotation.mult(viewDir);
        characterControl.setViewDirection(viewDir);
        
        // Camera rotation and positioning happens in updateMainViewPort
    }
}
