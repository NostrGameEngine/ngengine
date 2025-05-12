package org.ngengine.demo.soo;

import java.lang.reflect.Array;
import java.util.Arrays;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.environment.FastLightProbeFactory;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.simsilica.lemur.GuiGlobals;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;

public class SooGame extends SimpleApplication {
    Spatial playerSpatial;
    float steer = 0;
    float throttle = 0;
    float mouseSensitivity = 0.015f;
    float cameraDistance = 18f;
    float cameraHeight = 6f;
    float cameraYaw = 0;
    float cameraPitch = 0.2f;

    @Override
    public void simpleInitApp() {
        setLostFocusBehavior(LostFocusBehavior.Disabled);
        inputManager.setCursorVisible(false);
        stateManager.attach(new OceanAppState());

        // GuiGlobals.initialize(this);
        // com.simsilica.lemur.style.base.styles.GlassStyle.installAndUse();
        // stateManager.attach(new LobbyAppState(guiNode));
        stateManager.attach(new OceanAppState());

        // 2) Ensure camera sees it
        flyCam.setMoveSpeed(20);
        cam.setLocation(new Vector3f(0,10,20));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y); 


        // simple cube
        // Box b = new Box(1, 1, 1);
        // Geometry geom = new Geometry("Box", b);
        // Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        // mat.setColor("Color", ColorRGBA.Blue);
        // geom.setMaterial(mat);
        // geom.setLocalTranslation(0, 0, 0);
        // rootNode.attachChild(geom);

        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(true);

        Spatial sky = SkyFactory.createSky(assetManager, "quarry_01_puresky_4k.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        // // Create baker control
        EnvironmentProbeControl envProbe=new EnvironmentProbeControl(assetManager,1024);
        rootNode.addControl(envProbe);
       
        EnvironmentProbeControl.tagGlobal(sky);
        // Tag the sky, only the tagged spatials will be rendered in the env map
        // envProbe.tag(sky);
        // final EnvironmentCamera envCam = new EnvironmentCamera(256, new Vector3f(0, 0, 0));
        // stateManager.attach(envCam);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.setNumSamples(4);
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(10f)));
        viewPort.addProcessor(fpp);

        Geometry testCube = new Geometry("Boat", new Box(2f, 1.2f, 6f)); // length=12, height=2.4, width=4
        // (approximate pirate boat)
        Material testMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        testMat.setColor("Color", ColorRGBA.Red);
        testCube.setMaterial(testMat);
        testCube.setLocalTranslation(0, 10, 0);

        playerSpatial = testCube;

        RigidBodyControl playerPhysics = new RigidBodyControl(0.1f);
        playerSpatial.addControl(playerPhysics);
        // BoatControl boatControl = new BoatControl(false, 200f);
        // playerSpatial.addControl(boatControl);
        stateManager.getState(BulletAppState.class).getPhysicsSpace().add(playerSpatial);

        stateManager.getState(OceanAppState.class).add(playerSpatial);
        rootNode.attachChild(playerSpatial);

        // --- Input mappings ---
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("SteerLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("SteerRight", new KeyTrigger(KeyInput.KEY_D));

        // inputManager.addListener(boatControl, "Forward", "Backward", "SteerLeft", "SteerRight");

    }

    @Override
    public void simpleUpdate(float tpf) {

        // --- Camera follow ---
        Vector3f boatPos = playerSpatial.getWorldTranslation();
        Vector3f camDir = new Vector3f((float) Math.sin(cameraYaw) * (float) Math.cos(cameraPitch),
                (float) Math.sin(cameraPitch), (float) Math.cos(cameraYaw) * (float) Math.cos(cameraPitch));
        Vector3f camPos = boatPos.add(camDir.mult(-cameraDistance)).add(0, cameraHeight, 0);
        cam.setLocation(camPos);
        cam.lookAt(boatPos.add(0, 2f, 0), Vector3f.UNIT_Y);
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setGammaCorrection(true);
        settings.setSamples(4);
        settings.setGraphicsDebug(true);

        SooGame app = new SooGame();
        app.setSettings(settings);
        app.setShowSettings(false);

        app.start();
    }


    
}
