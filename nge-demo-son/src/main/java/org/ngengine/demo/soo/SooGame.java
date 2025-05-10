package org.ngengine.demo.soo;

import java.lang.reflect.Array;
import java.util.Arrays;

import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
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
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.simsilica.lemur.GuiGlobals;

public class SooGame extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        setLostFocusBehavior(LostFocusBehavior.Disabled);
        // GuiGlobals.initialize(this);
        inputManager.setCursorVisible(false);
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
       

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.setNumSamples(4);
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(2f)));
        viewPort.addProcessor(fpp);

    }

    
    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setGammaCorrection(true);
        settings.setSamples(4);


        SooGame app = new SooGame();
        app.setSettings(settings);
        app.setShowSettings(false);

        app.start();
    }


    
}
